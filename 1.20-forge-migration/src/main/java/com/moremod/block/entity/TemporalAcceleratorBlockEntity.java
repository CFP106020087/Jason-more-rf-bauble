package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 时间加速器BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 加速周围方块的tick
 * - 消耗RF能量
 * - 可调节加速倍率
 */
public class TemporalAcceleratorBlockEntity extends BaseEnergyBlockEntity {

    private static final int CAPACITY = 500000;
    private static final int[] MULTIPLIERS = {2, 4, 8, 16};
    private static final int BASE_ENERGY_COST = 100; // RF per accelerated tick

    private int multiplierIndex = 0;
    private boolean active = false;

    public TemporalAcceleratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEMPORAL_ACCELERATOR.get(), pos, state,
                CAPACITY, 20000, 0, 0);
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        int multiplier = getMultiplier();
        int energyCost = BASE_ENERGY_COST * multiplier;

        // 检查能量是否足够
        if (getEnergyStored() < energyCost) {
            if (active) {
                active = false;
                syncToClient();
            }
            return;
        }

        active = true;

        // 消耗能量
        extractEnergy(energyCost, false);

        // 加速周围方块
        accelerateNearbyBlocks(multiplier);
    }

    private void accelerateNearbyBlocks(int multiplier) {
        // 获取周围6个方向的方块
        BlockPos[] neighbors = {
                getBlockPos().above(),
                getBlockPos().below(),
                getBlockPos().north(),
                getBlockPos().south(),
                getBlockPos().east(),
                getBlockPos().west()
        };

        for (BlockPos neighborPos : neighbors) {
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (be != null) {
                // 尝试获取BlockEntityTicker并额外调用
                // 注意：这种方式可能不适用于所有BlockEntity
                // 更好的方式是使用Mixin或直接调用tick方法
                for (int i = 1; i < multiplier; i++) {
                    tickBlockEntity(be);
                }
            }
        }
    }

    private void tickBlockEntity(BlockEntity be) {
        // TODO: 实现安全的额外tick调用
        // 这需要访问BlockEntity的ticker，可能需要反射或其他方式
    }

    public int getMultiplier() {
        return MULTIPLIERS[multiplierIndex];
    }

    public void cycleMultiplier() {
        multiplierIndex = (multiplierIndex + 1) % MULTIPLIERS.length;
        setChanged();
        syncToClient();
    }

    public boolean isActive() {
        return active;
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("MultiplierIndex", multiplierIndex);
        tag.putBoolean("Active", active);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        multiplierIndex = tag.getInt("MultiplierIndex") % MULTIPLIERS.length;
        active = tag.getBoolean("Active");
    }
}
