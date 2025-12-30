package com.moremod.block;

import com.moremod.block.entity.ExtractionStationBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * 提取站方块 - 1.20 Forge版本
 *
 * 功能：
 * - 从物品中提取特殊成分
 * - 消耗RF能量
 * - 输出提取物
 */
public class ExtractionStationBlock extends Block implements EntityBlock {

    public ExtractionStationBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.0F, 10.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExtractionStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == ModBlockEntities.EXTRACTION_STATION.get()
                ? (lvl, pos, st, be) -> ((ExtractionStationBlockEntity) be).serverTick()
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ExtractionStationBlockEntity station)) {
            return InteractionResult.PASS;
        }

        // TODO: 打开GUI
        // 目前显示状态信息
        if (player.isShiftKeyDown()) {
            int energy = station.getEnergyStored();
            int maxEnergy = station.getMaxEnergyStored();
            int progress = station.getProgress();
            int maxProgress = station.getMaxProgress();

            player.displayClientMessage(Component.literal(
                    "§6=== 提取站状态 ==="
            ), false);
            player.displayClientMessage(Component.literal(
                    "§e能量: " + energy + " / " + maxEnergy + " RF"
            ), false);
            if (station.isRunning()) {
                player.displayClientMessage(Component.literal(
                        "§a进度: " + (progress * 100 / maxProgress) + "%"
                ), false);
            }
        } else {
            player.displayClientMessage(Component.literal(
                    "§6【提取站】§r 蹲下右键查看状态"
            ), true);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ExtractionStationBlockEntity station) {
                IItemHandler handler = station.getItemHandler();
                if (handler != null) {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                        }
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
