package com.moremod.tile;

import com.moremod.compat.crafttweaker.GemExtractionHelper;
import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.compat.crafttweaker.IdentifiedAffix;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 提取台 TileEntity - 改造版
 * 槽位分配：
 * 0-5: 右侧6个输出槽（精炼宝石）
 * 6:   左侧输入槽（源宝石）
 */
public class TileEntityExtractionStation extends TileEntity implements ITickable {

    // 物品处理器（7个槽位）
    private final ItemStackHandler inventory = new ItemStackHandler(7) {
        @Override
        protected void onContentsChanged(int slot) {
            TileEntityExtractionStation.this.markDirty();
            // 同步到客户端
            if (!world.isRemote) {
                world.notifyBlockUpdate(pos, world.getBlockState(pos),
                        world.getBlockState(pos), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 6) {
                // 输入槽：必须是已鉴定的宝石
                return GemNBTHelper.isGem(stack) && GemNBTHelper.isIdentified(stack);
            }
            // 输出槽：不允许放入
            return false;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 输出槽（0-5）可以正常取出
            return super.extractItem(slot, amount, simulate);
        }
    };

    private int workTicks = 0;
    private boolean isWorking = false;

    // 基础经验消耗
    private static final int BASE_XP_COST = 5;  // 单个词条消耗

    @Override
    public void update() {
        if (world.isRemote) {
            if (isWorking) {
                spawnParticles();
            }
            return;
        }

        if (isWorking) {
            workTicks++;
            if (workTicks >= 40) {
                isWorking = false;
                workTicks = 0;
            }
        }
    }

    /**
     * 生成粒子效果
     */
    private void spawnParticles() {
        if (world.rand.nextInt(3) == 0) {
            double x = pos.getX() + 0.5 + (world.rand.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5 + (world.rand.nextDouble() - 0.5) * 0.5;

            world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                    x, y, z,
                    (world.rand.nextDouble() - 0.5) * 0.1,
                    0.1,
                    (world.rand.nextDouble() - 0.5) * 0.1
            );
        }
    }

    // ==========================================
    // 槽位访问方法
    // ==========================================

    /**
     * 获取输入槽的宝石
     */
    public ItemStack getInputStack() {
        return inventory.getStackInSlot(6);
    }

    /**
     * 设置输入槽
     */
    public void setInputStack(ItemStack stack) {
        inventory.setStackInSlot(6, stack);
    }

    /**
     * 获取输出槽物品（0-5）
     */
    public ItemStack getOutputStack(int index) {
        if (index < 0 || index > 5) return ItemStack.EMPTY;
        return inventory.getStackInSlot(index);
    }

    /**
     * 设置输出槽
     */
    public void setOutputStack(int index, ItemStack stack) {
        if (index >= 0 && index <= 5) {
            inventory.setStackInSlot(index, stack);
        }
    }

    /**
     * 获取物品处理器
     */
    public ItemStackHandler getInventory() {
        return inventory;
    }

    // ==========================================
    // 业务逻辑
    // ==========================================

    /**
     * 检查是否可以提取
     */
    public boolean canExtract() {
        ItemStack input = getInputStack();
        if (input.isEmpty() || !GemNBTHelper.isIdentified(input)) {
            return false;
        }

        // 检查是否所有输出槽都为空
        for (int i = 0; i < 6; i++) {
            if (!getOutputStack(i).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取输入宝石的词条列表
     */
    public List<IdentifiedAffix> getAffixes() {
        ItemStack input = getInputStack();
        if (input.isEmpty() || !GemNBTHelper.isIdentified(input)) {
            return null;
        }
        return GemNBTHelper.getAffixes(input);
    }

    /**
     * 提取单个词条
     *
     * @param index 词条索引（0-5）
     * @param player 玩家
     * @return 是否成功
     */
    public boolean extractAffix(int index, EntityPlayer player) {
        if (!canExtract()) return false;

        List<IdentifiedAffix> affixes = getAffixes();
        if (affixes == null || index < 0 || index >= affixes.size()) {
            return false;
        }

        // 检查经验（单个提取：1倍消耗）
        int xpCost = BASE_XP_COST;
        if (player.experienceLevel < xpCost && !player.capabilities.isCreativeMode) {
            return false;
        }

        // 执行提取
        ItemStack input = getInputStack().copy();
        ItemStack refined = GemExtractionHelper.extractAffix(input, index);

        if (refined.isEmpty()) return false;

        // 扣除经验
        if (!player.capabilities.isCreativeMode) {
            player.addExperienceLevel(-xpCost);
        }

        // 输出到对应槽位
        setOutputStack(index, refined);

        // 清空输入
        setInputStack(ItemStack.EMPTY);

        // 触发工作动画
        isWorking = true;
        workTicks = 0;
        markDirty();

        return true;
    }

    /**
     * 完全分解宝石（所有词条）
     *
     * @param player 玩家
     * @return 是否成功
     */
    public boolean decomposeGem(EntityPlayer player) {
        if (!canExtract()) return false;

        List<IdentifiedAffix> affixes = getAffixes();
        if (affixes == null || affixes.isEmpty()) {
            return false;
        }

        // 检查经验（完全分解：6倍消耗）
        int xpCost = BASE_XP_COST * 6;
        if (player.experienceLevel < xpCost && !player.capabilities.isCreativeMode) {
            return false;
        }

        // 执行完全分解
        ItemStack input = getInputStack().copy();
        List<ItemStack> refinedGems = GemExtractionHelper.decomposeGem(input);

        if (refinedGems.isEmpty()) return false;

        // 扣除经验
        if (!player.capabilities.isCreativeMode) {
            player.addExperienceLevel(-xpCost);
        }

        // 将所有精炼宝石放入输出槽（最多6个）
        for (int i = 0; i < Math.min(refinedGems.size(), 6); i++) {
            setOutputStack(i, refinedGems.get(i));
        }

        // 如果超过6个，剩余的掉落地上
        if (refinedGems.size() > 6) {
            for (int i = 6; i < refinedGems.size(); i++) {
                if (!world.isRemote) {
                    net.minecraft.entity.item.EntityItem entityItem =
                            new net.minecraft.entity.item.EntityItem(world,
                                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                                    refinedGems.get(i));
                    world.spawnEntity(entityItem);
                }
            }
        }

        // 清空输入
        setInputStack(ItemStack.EMPTY);

        // 触发工作动画
        isWorking = true;
        workTicks = 0;
        markDirty();

        return true;
    }

    /**
     * 获取单次提取经验消耗
     */
    public int getSingleExtractXpCost() {
        return BASE_XP_COST;
    }

    /**
     * 获取完全分解经验消耗
     */
    public int getFullDecomposeXpCost() {
        return BASE_XP_COST * 6;
    }

    // ==========================================
    // NBT保存/加载
    // ==========================================

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("inventory", inventory.serializeNBT());
        compound.setInteger("workTicks", workTicks);
        compound.setBoolean("isWorking", isWorking);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("inventory"));
        workTicks = compound.getInteger("workTicks");
        isWorking = compound.getBoolean("isWorking");
    }

    // ==========================================
    // 网络同步
    // ==========================================

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setTag("inventory", inventory.serializeNBT());
        tag.setBoolean("isWorking", isWorking);
        return tag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound tag = pkt.getNbtCompound();
        readFromNBT(tag);
        isWorking = tag.getBoolean("isWorking");
    }

    // ==========================================
    // Capability支持
    // ==========================================

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }
}