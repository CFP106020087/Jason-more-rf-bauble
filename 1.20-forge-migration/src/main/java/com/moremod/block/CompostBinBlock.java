package com.moremod.block;

import com.moremod.block.entity.CompostBinBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 堆肥桶方块 - 1.20 Forge版本
 *
 * 功能：
 * - 将有机物转化为骨粉
 * - 放入树叶、种子、腐肉等有机物，自动转化为骨粉
 */
public class CompostBinBlock extends Block implements EntityBlock {

    // 桶形碰撞箱
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

    public CompostBinBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(1.5F, 5.0F)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompostBinBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == ModBlockEntities.COMPOST_BIN.get()
                ? (lvl, pos, st, be) -> ((CompostBinBlockEntity) be).serverTick()
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CompostBinBlockEntity compost) {
            ItemStack heldItem = player.getItemInHand(hand);

            // 尝试放入有机物
            if (!heldItem.isEmpty() && compost.isValidInput(heldItem)) {
                ItemStack remaining = compost.addCompostMaterial(heldItem);
                if (!player.isCreative()) {
                    player.setItemInHand(hand, remaining);
                }
                return InteractionResult.CONSUME;
            }

            // 尝试取出骨粉
            ItemStack output = compost.extractOutput();
            if (!output.isEmpty()) {
                if (!player.addItem(output)) {
                    player.drop(output, false);
                }
                return InteractionResult.CONSUME;
            }

            // 显示状态
            int progress = compost.getCompostProgress();
            int stored = compost.getStoredAmount();
            int outputCount = compost.getOutputCount();

            player.displayClientMessage(Component.literal(
                    "§a堆肥桶状态:§7 有机物: §e" + stored + "/64" +
                    "§7 | 进度: §b" + progress + "%" +
                    "§7 | 骨粉: §f" + outputCount
            ), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CompostBinBlockEntity compost) {
                compost.dropInventory();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.literal("§7将有机物转化为骨粉"));
        tooltip.add(Component.literal("§2接受: 树叶、种子、腐肉、蜘蛛眼等"));
        tooltip.add(Component.literal("§e右键放入/取出物品"));
    }
}
