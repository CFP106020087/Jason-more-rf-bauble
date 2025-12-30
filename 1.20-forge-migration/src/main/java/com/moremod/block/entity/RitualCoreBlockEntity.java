package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

/**
 * 祭坛核心BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 嵌入仪式的核心
 * - 物品处理（输入/输出）
 * - 玩家坐入功能
 */
public class RitualCoreBlockEntity extends BaseEnergyBlockEntity {

    private static final int CAPACITY = 100000;
    private static final int RITUAL_TICKS = 200;

    private int ritualProgress = 0;
    private boolean isPerformingRitual = false;
    private Player seatedPlayer = null;

    public RitualCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RITUAL_CORE.get(), pos, state,
                CAPACITY, 5000, 0, 2); // 2 slots: input and output
    }

    @Override
    protected ItemStackHandler createInventory(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                syncToClient();
            }
        };
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        if (isPerformingRitual) {
            processRitual();
        }
    }

    /**
     * 尝试让玩家坐上祭坛
     */
    public boolean seatPlayer(Player player) {
        if (seatedPlayer != null) {
            return false;
        }

        // TODO: 实现骑乘实体或坐标锁定逻辑
        seatedPlayer = player;
        return true;
    }

    private void processRitual() {
        ritualProgress++;

        if (ritualProgress >= RITUAL_TICKS) {
            completeRitual();
        }
    }

    private void completeRitual() {
        // TODO: 实现仪式完成逻辑
        isPerformingRitual = false;
        ritualProgress = 0;
        seatedPlayer = null;
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("RitualProgress", ritualProgress);
        tag.putBoolean("IsPerformingRitual", isPerformingRitual);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ritualProgress = tag.getInt("RitualProgress");
        isPerformingRitual = tag.getBoolean("IsPerformingRitual");
    }
}
