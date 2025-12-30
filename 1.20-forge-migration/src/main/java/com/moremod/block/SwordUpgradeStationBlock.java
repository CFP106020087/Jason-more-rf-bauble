package com.moremod.block;

import com.moremod.block.entity.SwordUpgradeStationBlockEntity;
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
 * 剑升级工作站方块 - 1.20 Forge版本
 *
 * 功能：
 * - 用于升级武器属性
 * - 消耗特定材料提升武器等级
 * - 支持多种升级类型
 */
public class SwordUpgradeStationBlock extends Block implements EntityBlock {

    public SwordUpgradeStationBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.5F, 10.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SwordUpgradeStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == ModBlockEntities.SWORD_UPGRADE_STATION.get()
                ? (lvl, pos, st, be) -> ((SwordUpgradeStationBlockEntity) be).serverTick()
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SwordUpgradeStationBlockEntity station)) {
            return InteractionResult.PASS;
        }

        // TODO: 打开GUI
        // 目前显示状态信息
        if (player.isShiftKeyDown()) {
            int energy = station.getEnergyStored();
            int maxEnergy = station.getMaxEnergyStored();

            player.displayClientMessage(Component.literal(
                    "§6=== 剑升级工作站 ==="
            ), false);
            player.displayClientMessage(Component.literal(
                    "§e能量: " + energy + " / " + maxEnergy + " RF"
            ), false);
            if (station.isUpgrading()) {
                player.displayClientMessage(Component.literal(
                        "§d⚡ 正在升级..."
                ), false);
            }
        } else {
            player.displayClientMessage(Component.literal(
                    "§6【剑升级工作站】§r 蹲下右键查看状态"
            ), true);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SwordUpgradeStationBlockEntity station) {
                // 只掉落输入槽物品，避免预览槽物品复制
                IItemHandler handler = station.getItemHandler();
                if (handler != null) {
                    // 假设前几个槽是输入槽
                    for (int i = 0; i < Math.min(3, handler.getSlots()); i++) {
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
