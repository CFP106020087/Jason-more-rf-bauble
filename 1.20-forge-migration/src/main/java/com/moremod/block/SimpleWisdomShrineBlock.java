package com.moremod.block;

import com.moremod.block.entity.SimpleWisdomShrineBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * 简易智慧之泉方块 - 1.20 Forge版本
 *
 * 功能：
 * - 简化版的智慧之泉
 * - 多方块结构
 * - 解锁村民交易 + 加速成长
 */
public class SimpleWisdomShrineBlock extends Block implements EntityBlock {

    public SimpleWisdomShrineBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.0F, 10.0F)
                .sound(SoundType.STONE)
                .lightLevel(state -> 8)); // 0.5F * 15 ≈ 8
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleWisdomShrineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == ModBlockEntities.SIMPLE_WISDOM_SHRINE.get()
                ? (lvl, pos, st, be) -> ((SimpleWisdomShrineBlockEntity) be).serverTick()
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SimpleWisdomShrineBlockEntity shrine)) {
            return InteractionResult.PASS;
        }

        if (shrine.isFormed()) {
            player.displayClientMessage(Component.literal("§a✓ 简易智慧之泉已激活"), false);
            player.displayClientMessage(Component.literal("§7范围: " + shrine.getRange() + "格"), false);
            player.displayClientMessage(Component.literal("§7效果: 解锁交易 + 加速成长"), false);
        } else {
            player.displayClientMessage(Component.literal("§c✗ 结构未完成"), false);
            player.displayClientMessage(Component.literal("§7需要搭建3x3x3结构"), false);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SimpleWisdomShrineBlockEntity shrine) {
                shrine.checkStructure();
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SimpleWisdomShrineBlockEntity shrine) {
                shrine.onBroken();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
