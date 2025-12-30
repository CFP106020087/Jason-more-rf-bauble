package com.moremod.block.entity;

import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 智慧之泉BlockEntity - 1.20 Forge版本
 *
 * 功能：
 * - 多方块结构检测
 * - 村民转化功能
 */
public class WisdomFountainBlockEntity extends BaseEnergyBlockEntity {

    private static final int CAPACITY = 200000;

    private boolean formed = false;

    public WisdomFountainBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WISDOM_FOUNTAIN.get(), pos, state,
                CAPACITY, 10000, 0, 0);
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        // 每20tick检查一次结构
        if (level.getGameTime() % 20 == 0) {
            checkStructure();
        }
    }

    private void checkStructure() {
        // TODO: 实现多方块结构检查
        // 需要检查守护者方块和符文虚空石位置
        formed = true; // 暂时返回true
    }

    public boolean isFormed() {
        return formed;
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Formed", formed);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        formed = tag.getBoolean("Formed");
    }
}
