package com.moremod.block;

import com.moremod.block.entity.PlantOilPressBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.Nullable;

/**
 * 植物油压榨机方块 - 1.20 Forge版本
 *
 * 功能：
 * - 消耗RF能量
 * - 将农作物压榨成植物油
 */
public class PlantOilPressBlock extends Block implements EntityBlock {

    public PlantOilPressBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(4.0F, 10.0F)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlantOilPressBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == ModBlockEntities.PLANT_OIL_PRESS.get()
                ? (lvl, pos, st, be) -> ((PlantOilPressBlockEntity) be).serverTick()
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PlantOilPressBlockEntity press)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);

        // 蹲下右键：显示状态
        if (player.isShiftKeyDown()) {
            showStatus(player, press);
            return InteractionResult.CONSUME;
        }

        // 空手：取出物品
        if (heldItem.isEmpty()) {
            press.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                // 先尝试取出输出
                ItemStack output = handler.extractItem(1, 64, false);
                if (!output.isEmpty()) {
                    if (!player.addItem(output)) {
                        player.drop(output, false);
                    }
                    player.displayClientMessage(Component.literal(
                            "§a取出: " + output.getHoverName().getString() + " x" + output.getCount()
                    ), true);
                    return;
                }

                // 再尝试取出输入
                ItemStack input = handler.extractItem(0, 64, false);
                if (!input.isEmpty()) {
                    if (!player.addItem(input)) {
                        player.drop(input, false);
                    }
                    player.displayClientMessage(Component.literal(
                            "§e取出: " + input.getHoverName().getString() + " x" + input.getCount()
                    ), true);
                }
            });
            return InteractionResult.CONSUME;
        }

        // 放入农作物
        if (PlantOilPressBlockEntity.isValidInput(heldItem)) {
            press.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                ItemStack toInsert = heldItem.copy();
                ItemStack remainder = handler.insertItem(0, toInsert, false);
                int inserted = toInsert.getCount() - remainder.getCount();
                if (inserted > 0) {
                    heldItem.shrink(inserted);
                    player.displayClientMessage(Component.literal(
                            "§a放入: " + toInsert.getHoverName().getString() + " x" + inserted
                    ), true);
                }
            });
            return InteractionResult.CONSUME;
        } else {
            player.displayClientMessage(Component.literal(
                    "§c此物品无法压榨成植物油！"
            ), true);
        }

        return InteractionResult.CONSUME;
    }

    private void showStatus(Player player, PlantOilPressBlockEntity press) {
        int energy = press.getEnergyStored();
        int maxEnergy = press.getMaxEnergyStored();
        int progress = press.getProgress();
        int maxProgress = press.getMaxProgress();

        player.displayClientMessage(Component.literal(
                "§6=== 压榨机状态 ==="
        ), false);

        float percentage = maxEnergy > 0 ? (energy * 100.0f / maxEnergy) : 0;
        String energyColor = percentage >= 50 ? "§a" : percentage >= 20 ? "§e" : "§c";
        player.displayClientMessage(Component.literal(
                energyColor + "能量: " + formatAmount(energy) + " / " + formatAmount(maxEnergy) + " RF"
        ), false);

        if (press.isProcessing()) {
            float progressPercent = maxProgress > 0 ? (progress * 100.0f / maxProgress) : 0;
            player.displayClientMessage(Component.literal(
                    "§d⚡ 压榨中... " + String.format("%.1f", progressPercent) + "%"
            ), false);
        } else {
            player.displayClientMessage(Component.literal(
                    "§7待机中 (放入农作物开始压榨)"
            ), false);
        }

        player.displayClientMessage(Component.literal(
                "§8可压榨: 小麦、马铃薯、胡萝卜、甜菜根、南瓜、西瓜等"
        ), false);
    }

    private String formatAmount(int amount) {
        if (amount >= 1000000) {
            return String.format("%.1fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("%.1fk", amount / 1000.0);
        }
        return String.valueOf(amount);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PlantOilPressBlockEntity press) {
                press.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                        }
                    }
                });
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
