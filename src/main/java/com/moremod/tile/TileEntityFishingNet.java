package com.moremod.tile;

import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 渔网TileEntity - 自动钓鱼逻辑
 */
public class TileEntityFishingNet extends TileEntity implements ITickable {

    // 内部物品存储 (9格)
    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    // 钓鱼计时器
    private int fishingTimer = 0;

    // 基础钓鱼时间 (tick) - 平均约30秒一次
    private static final int BASE_FISHING_TIME = 600;
    // 随机波动范围
    private static final int FISHING_TIME_VARIANCE = 400;

    // 下次钓鱼的目标时间
    private int targetFishingTime = 0;

    public TileEntityFishingNet() {
        resetFishingTimer();
    }

    private void resetFishingTimer() {
        fishingTimer = 0;
        if (world != null) {
            targetFishingTime = BASE_FISHING_TIME + world.rand.nextInt(FISHING_TIME_VARIANCE);
        } else {
            targetFishingTime = BASE_FISHING_TIME;
        }
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        // 检查下方是否有水
        if (!hasWaterBelow()) {
            return;
        }

        fishingTimer++;

        if (fishingTimer >= targetFishingTime) {
            doFishing();
            resetFishingTimer();
        }
    }

    /**
     * 检查下方是否有水源
     */
    public boolean hasWaterBelow() {
        if (world == null) return false;
        BlockPos below = pos.down();
        return world.getBlockState(below).getMaterial() == Material.WATER;
    }

    /**
     * 执行钓鱼，从战利品表获取物品
     */
    private void doFishing() {
        if (!(world instanceof WorldServer)) return;

        WorldServer ws = (WorldServer) world;

        // 使用原版钓鱼战利品表
        LootContext.Builder builder = new LootContext.Builder(ws);
        builder.withLuck(0); // 无幸运加成

        List<ItemStack> loot = ws.getLootTableManager()
            .getLootTableFromLocation(LootTableList.GAMEPLAY_FISHING)
            .generateLootForPools(ws.rand, builder.build());

        // 尝试将战利品放入库存
        for (ItemStack stack : loot) {
            ItemStack remaining = insertItem(stack);
            // 如果库存满了，掉落到世界
            if (!remaining.isEmpty()) {
                EntityItem entityItem = new EntityItem(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    remaining);
                world.spawnEntity(entityItem);
            }
        }

        // 生成粒子效果
        spawnFishingParticles();
    }

    /**
     * 尝试将物品插入库存
     */
    private ItemStack insertItem(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < inventory.getSlots() && !remaining.isEmpty(); i++) {
            remaining = inventory.insertItem(i, remaining, false);
        }
        return remaining;
    }

    /**
     * 生成钓鱼粒子效果
     */
    private void spawnFishingParticles() {
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(net.minecraft.util.EnumParticleTypes.WATER_SPLASH,
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                10, 0.3, 0.1, 0.3, 0.0);
        }
    }

    /**
     * 掉落所有库存物品
     */
    public void dropInventory() {
        if (world == null || world.isRemote) return;

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                EntityItem entityItem = new EntityItem(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    stack.copy());
                world.spawnEntity(entityItem);
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    // ========== NBT 读写 ==========

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("FishingTimer", fishingTimer);
        compound.setInteger("TargetTime", targetFishingTime);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        fishingTimer = compound.getInteger("FishingTimer");
        targetFishingTime = compound.getInteger("TargetTime");
        if (targetFishingTime <= 0) {
            targetFishingTime = BASE_FISHING_TIME;
        }
    }

    // ========== Capability (漏斗/管道支持) ==========

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }

    // ========== Getter ==========

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getFishingProgress() {
        return fishingTimer;
    }

    public int getTargetTime() {
        return targetFishingTime;
    }
}
