package com.moremod.printer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 打印机TileEntity
 *
 * 单方块机器，接电即可工作
 * 消耗能量和材料打印物品
 *
 * 槽位布局:
 * - 槽位 0: 模版槽
 * - 槽位 1-9: 材料槽 (3x3)
 * - 槽位 10: 输出槽
 */
public class TileEntityPrinter extends TileEntity implements ITickable {

    // 配置
    private static final int ENERGY_CAPACITY = 10000000;      // 10M RF
    private static final int MAX_RECEIVE = 1000000;           // 每tick最多接收 1M RF
    private static final int TEMPLATE_SLOT = 0;
    private static final int MATERIAL_SLOT_START = 1;
    private static final int MATERIAL_SLOT_COUNT = 9;
    private static final int OUTPUT_SLOT = 10;
    private static final int TOTAL_SLOTS = 11;

    // 能量存储 - 使用自定义包装器以支持管线输入
    private final EnergyStorage energyInternal = new EnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE, 0);

    // 能量接收器包装器 - 允许外部管线推送能量
    private final net.minecraftforge.energy.IEnergyStorage energy = new net.minecraftforge.energy.IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = energyInternal.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                markDirty();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0; // 不允许提取
        }

        @Override
        public int getEnergyStored() {
            return energyInternal.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energyInternal.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    // 物品槽
    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
            if (slot == TEMPLATE_SLOT) {
                // 模版改变，重新检查配方
                checkRecipe();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == TEMPLATE_SLOT) {
                // 支持普通模版和自定义模版
                return stack.getItem() instanceof ItemPrintTemplate ||
                       stack.getItem() instanceof ItemCustomPrintTemplate;
            }
            if (slot == OUTPUT_SLOT) {
                return false;  // 输出槽不接受放入
            }
            return true;  // 材料槽接受任何物品
        }
    };

    // 处理状态
    private PrinterRecipe currentRecipe = null;
    private int progress = 0;
    private int maxProgress = 0;
    private boolean isProcessing = false;

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        // 检查配方
        if (currentRecipe == null) {
            checkRecipe();
        }

        // 处理打印 (只需要有电即可工作)
        if (currentRecipe != null && canProcess()) {
            if (!isProcessing) {
                startProcessing();
            }

            // 消耗能量并推进进度
            int energyPerTick = currentRecipe.getEnergyCost() / currentRecipe.getProcessingTime();
            if (energy.getEnergyStored() >= energyPerTick) {
                extractEnergyInternal(energyPerTick);
                progress++;

                if (progress >= maxProgress) {
                    finishProcessing();
                }
                markDirty();
            }
        } else if (isProcessing) {
            // 无法继续处理
            isProcessing = false;
            progress = 0;
            markDirty();
        }
    }

    /**
     * 检查当前配方
     */
    private void checkRecipe() {
        ItemStack templateStack = inventory.getStackInSlot(TEMPLATE_SLOT);
        if (templateStack.isEmpty()) {
            currentRecipe = null;
            return;
        }

        // 处理自定义模版
        if (templateStack.getItem() instanceof ItemCustomPrintTemplate) {
            if (ItemCustomPrintTemplate.isValidTemplate(templateStack)) {
                // 从NBT创建虚拟配方
                currentRecipe = createRecipeFromCustomTemplate(templateStack);
            } else {
                currentRecipe = null;
            }
            return;
        }

        // 处理普通模版
        if (templateStack.getItem() instanceof ItemPrintTemplate) {
            String templateId = ItemPrintTemplate.getTemplateId(templateStack);
            currentRecipe = PrinterRecipeRegistry.getRecipe(templateId);
            return;
        }

        currentRecipe = null;
    }

    /**
     * 从自定义模版创建虚拟配方
     */
    private PrinterRecipe createRecipeFromCustomTemplate(ItemStack customTemplate) {
        String displayName = ItemCustomPrintTemplate.getDisplayName(customTemplate);
        ItemStack output = ItemCustomPrintTemplate.getOutput(customTemplate);
        int energyCost = ItemCustomPrintTemplate.getEnergyCost(customTemplate);
        int processingTime = ItemCustomPrintTemplate.getProcessingTime(customTemplate);
        List<ItemStack> materials = ItemCustomPrintTemplate.getMaterials(customTemplate);
        String rarity = ItemCustomPrintTemplate.getRarityString(customTemplate);

        // 使用Builder创建虚拟配方
        PrinterRecipe.Builder builder = new PrinterRecipe.Builder()
            .setTemplateId("custom_" + System.identityHashCode(customTemplate))
            .setDisplayName(displayName.isEmpty() ? "自定义模版" : displayName)
            .setRarity(rarity)
            .setOutput(output)
            .setEnergyCost(energyCost)
            .setProcessingTime(processingTime);

        for (ItemStack material : materials) {
            builder.addMaterial(material);
        }

        return builder.build();
    }

    /**
     * 检查是否可以处理
     */
    private boolean canProcess() {
        if (currentRecipe == null) return false;

        // 检查材料
        List<ItemStack> materials = getMaterialStacks();
        if (!currentRecipe.matchesMaterials(materials)) return false;

        // 检查能量
        if (energy.getEnergyStored() < currentRecipe.getEnergyCost()) return false;

        // 检查输出槽
        ItemStack outputSlot = inventory.getStackInSlot(OUTPUT_SLOT);
        if (outputSlot.isEmpty()) return true;

        ItemStack recipeOutput = currentRecipe.getOutput();
        if (!ItemStack.areItemsEqual(outputSlot, recipeOutput)) return false;
        if (!ItemStack.areItemStackTagsEqual(outputSlot, recipeOutput)) return false;
        return outputSlot.getCount() + recipeOutput.getCount() <= outputSlot.getMaxStackSize();
    }

    /**
     * 获取所有材料槽的物品
     */
    private List<ItemStack> getMaterialStacks() {
        List<ItemStack> materials = new ArrayList<>();
        for (int i = MATERIAL_SLOT_START; i < MATERIAL_SLOT_START + MATERIAL_SLOT_COUNT; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                materials.add(stack);
            }
        }
        return materials;
    }

    /**
     * 开始处理
     */
    private void startProcessing() {
        isProcessing = true;
        progress = 0;
        maxProgress = currentRecipe.getProcessingTime();
    }

    /**
     * 完成处理
     */
    private void finishProcessing() {
        if (currentRecipe == null) return;

        // 消耗材料
        consumeMaterials();

        // 添加输出
        ItemStack output = currentRecipe.getOutput();
        ItemStack outputSlot = inventory.getStackInSlot(OUTPUT_SLOT);
        if (outputSlot.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, output.copy());
        } else {
            outputSlot.grow(output.getCount());
        }

        // 重置状态
        isProcessing = false;
        progress = 0;
        maxProgress = 0;

        // 重新检查配方（可能可以继续处理）
        checkRecipe();
    }

    /**
     * 消耗材料
     */
    private void consumeMaterials() {
        if (currentRecipe == null) return;

        for (ItemStack required : currentRecipe.getMaterials()) {
            int toConsume = required.getCount();

            for (int i = MATERIAL_SLOT_START; i < MATERIAL_SLOT_START + MATERIAL_SLOT_COUNT && toConsume > 0; i++) {
                ItemStack slot = inventory.getStackInSlot(i);
                if (!slot.isEmpty() && ItemStack.areItemsEqual(required, slot) &&
                    ItemStack.areItemStackTagsEqual(required, slot)) {
                    int consume = Math.min(toConsume, slot.getCount());
                    slot.shrink(consume);
                    toConsume -= consume;
                    if (slot.isEmpty()) {
                        inventory.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    /**
     * 内部提取能量
     */
    private void extractEnergyInternal(int amount) {
        int stored = energyInternal.getEnergyStored();
        int toExtract = Math.min(amount, stored);
        if (toExtract > 0) {
            try {
                java.lang.reflect.Field field = EnergyStorage.class.getDeclaredField("energy");
                field.setAccessible(true);
                field.setInt(energyInternal, stored - toExtract);
            } catch (Exception e) {
                // 反射失败的备用方法
            }
        }
    }

    /**
     * 发送更新包
     */
    private void sendUpdatePacket() {
        if (world != null && !world.isRemote) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        }
    }

    /**
     * 掉落物品
     */
    public void dropItems() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    /**
     * 检查玩家是否可以使用
     */
    public boolean canPlayerUse(EntityPlayer player) {
        return player.getDistanceSq(pos) <= 64;
    }

    // ===== Getters =====

    public int getEnergyStored() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergyStored() {
        return energy.getMaxEnergyStored();
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    /**
     * 单方块机器，始终返回true
     * 保留此方法以兼容GUI
     */
    public boolean isMultiblockFormed() {
        return true;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public PrinterRecipe getCurrentRecipe() {
        return currentRecipe;
    }

    // ===== 客户端同步方法 =====

    /**
     * 客户端设置能量值（用于GUI同步）
     */
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public void setClientEnergy(int energy) {
        try {
            java.lang.reflect.Field field = net.minecraftforge.energy.EnergyStorage.class.getDeclaredField("energy");
            field.setAccessible(true);
            field.setInt(energyInternal, energy);
        } catch (Exception e) {
            // 忽略
        }
    }

    /**
     * 客户端设置进度（用于GUI同步）
     */
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public void setClientProgress(int progress) {
        this.progress = progress;
    }

    /**
     * 客户端设置最大进度（用于GUI同步）
     */
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public void setClientMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
    }

    /**
     * 客户端设置处理状态（用于GUI同步）
     */
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public void setClientProcessing(boolean isProcessing) {
        this.isProcessing = isProcessing;
    }

    // ===== Capabilities =====

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY ||
               capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ||
               super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) energy;
        }
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) inventory;
        }
        return super.getCapability(capability, facing);
    }

    // ===== NBT =====

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("Energy", energyInternal.getEnergyStored());
        compound.setBoolean("IsProcessing", isProcessing);
        compound.setInteger("Progress", progress);
        compound.setInteger("MaxProgress", maxProgress);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Inventory")) {
            inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        }
        int fe = compound.getInteger("Energy");
        while (energyInternal.getEnergyStored() < fe && energyInternal.receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        isProcessing = compound.getBoolean("IsProcessing");
        progress = compound.getInteger("Progress");
        maxProgress = compound.getInteger("MaxProgress");

        // 重新检查配方
        checkRecipe();
    }

    // ===== 网络同步 =====

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }
}
