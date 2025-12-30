package com.moremod.block;

import com.moremod.block.entity.PedestalBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * 展示台方块 - 1.20 Forge版本
 *
 * 功能：
 * - 展示物品
 * - 多方块结构组件
 * - 物品交互
 */
public class PedestalBlock extends BaseMachineBlock {

    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    public PedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PedestalBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PedestalBlockEntity pedestal) {
            ItemStack heldItem = player.getItemInHand(hand);
            ItemStack displayItem = pedestal.getDisplayItem();

            // 取出物品
            if (!displayItem.isEmpty()) {
                if (!player.getInventory().add(displayItem.copy())) {
                    player.drop(displayItem.copy(), false);
                }
                pedestal.setDisplayItem(ItemStack.EMPTY);
                return InteractionResult.SUCCESS;
            }

            // 放入物品
            if (!heldItem.isEmpty()) {
                ItemStack toPlace = heldItem.copy();
                toPlace.setCount(1);
                pedestal.setDisplayItem(toPlace);
                heldItem.shrink(1);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PedestalBlockEntity pedestal) {
                ItemStack item = pedestal.getDisplayItem();
                if (!item.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), item);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
