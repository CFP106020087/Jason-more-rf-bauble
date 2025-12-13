package com.moremod.printer;

import com.moremod.multiblock.MultiblockPrinter;
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
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 打印机TileEntity
 *
 * 多方块结构核心，消耗能量和材料打印物品
 * 使用GeckoLib实现机械臂动画
 *
 * 槽位布局:
 * - 槽位 0: 模版槽
 * - 槽位 1-9: 材料槽 (3x3)
 * - 槽位 10: 输出槽
 */
public class TileEntityPrinter extends TileEntity implements ITickable, IAnimatable {

    // GeckoLib动画工厂
    private final AnimationFactory factory = new AnimationFactory(this);

    // 配置
    private static final int ENERGY_CAPACITY = 10000000;      // 10M RF
    private static final int MAX_RECEIVE = 1000000;           // 每tick最多接收 1M RF
    private static final int TEMPLATE_SLOT = 0;
    private static final int MATERIAL_SLOT_START = 1;
    private static final int MATERIAL_SLOT_COUNT = 9;
    private static final int OUTPUT_SLOT = 10;
    private static final int TOTAL_SLOTS = 11;

    // 能量存储
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                markDirty();
            }
            return received;
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
    private boolean multiblockFormed = false;
    private PrinterRecipe currentRecipe = null;
    private int progress = 0;
    private int maxProgress = 0;
    private boolean isProcessing = false;

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        // 每20tick检查一次多方块结构
        if (world.getTotalWorldTime() % 20 == 0) {
            boolean wasFormed = multiblockFormed;
            multiblockFormed = MultiblockPrinter.checkStructure(world, pos);
            if (wasFormed != multiblockFormed) {
                markDirty();
                sendUpdatePacket();
            }
        }

        // 如果多方块没有形成，不进行处理
        if (!multiblockFormed) {
            if (isProcessing) {
                isProcessing = false;
                progress = 0;
                markDirty();
            }
            return;
        }

        // 检查配方
        if (currentRecipe == null) {
            checkRecipe();
        }

        // 处理打印
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
        int stored = energy.getEnergyStored();
        int toExtract = Math.min(amount, stored);
        if (toExtract > 0) {
            try {
                java.lang.reflect.Field field = EnergyStorage.class.getDeclaredField("energy");
                field.setAccessible(true);
                field.setInt(energy, stored - toExtract);
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

    public boolean isMultiblockFormed() {
        return multiblockFormed;
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
        compound.setInteger("Energy", energy.getEnergyStored());
        compound.setBoolean("MultiblockFormed", multiblockFormed);
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
        while (energy.getEnergyStored() < fe && energy.receiveEnergy(Integer.MAX_VALUE, false) > 0) {}
        multiblockFormed = compound.getBoolean("MultiblockFormed");
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

    // ===== GeckoLib动画 =====

    /**
     * 动画控制器 - 根据状态播放不同动画
     * 多方块形成时播放 idle 动画，否则停止
     */
    private <E extends IAnimatable> PlayState animationPredicate(AnimationEvent<E> event) {
        // 只有多方块形成时才播放动画
        if (multiblockFormed) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.3d_printer.idle", true));
            return PlayState.CONTINUE;
        }
        // 多方块未形成时停止动画
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}
