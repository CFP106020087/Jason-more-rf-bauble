package com.moremod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * 机器方块基类 - 1.20 Forge版本
 *
 * 1.12 BlockContainer/ITileEntityProvider -> 1.20 BaseEntityBlock
 * 1.12 ITickable -> 1.20 BlockEntityTicker
 */
public abstract class BaseMachineBlock extends BaseEntityBlock {

    protected BaseMachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * 创建BlockEntity的ticker
     * 子类需要覆盖此方法来提供tick功能
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    /**
     * 辅助方法：创建通用的BlockEntityTicker
     *
     * @param givenType 给定的BlockEntityType
     * @param expectedType 期望的BlockEntityType
     * @param ticker 实际的ticker实现
     */
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
    }
}
