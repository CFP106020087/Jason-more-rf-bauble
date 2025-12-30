package com.moremod.block;

import com.moremod.block.entity.OilGeneratorBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * 石油发电机方块 - 1.20 Forge版本
 *
 * 功能：
 * - 燃烧石油或植物油发电
 * - 输出RF能量到相邻机器
 * - 支持增速插件
 */
public class OilGeneratorBlock extends BaseMachineBlock {

    public OilGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OilGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.OIL_GENERATOR.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof OilGeneratorBlockEntity generator)) {
            return InteractionResult.FAIL;
        }

        ItemStack heldItem = player.getItemInHand(hand);

        // 蹲下右键：显示状态
        if (player.isShiftKeyDown()) {
            showStatus(player, generator);
            return InteractionResult.SUCCESS;
        }

        IItemHandler handler = level.getCapability(ForgeCapabilities.ITEM_HANDLER, pos, hit.getDirection());
        if (handler == null) return InteractionResult.FAIL;

        // 空手：取出燃料
        if (heldItem.isEmpty()) {
            ItemStack fuel = handler.extractItem(0, 64, false);
            if (!fuel.isEmpty()) {
                if (!player.getInventory().add(fuel)) {
                    player.drop(fuel, false);
                }
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.YELLOW + "取出: " + fuel.getHoverName().getString() + " x" + fuel.getCount()
                ));
                return InteractionResult.SUCCESS;
            }
        } else {
            // 放入燃料
            if (OilGeneratorBlockEntity.isValidFuel(heldItem)) {
                ItemStack toInsert = heldItem.copy();
                ItemStack remainder = handler.insertItem(0, toInsert, false);
                int inserted = toInsert.getCount() - remainder.getCount();
                if (inserted > 0) {
                    heldItem.shrink(inserted);
                    player.sendSystemMessage(Component.literal(
                            ChatFormatting.GREEN + "放入燃料: " + toInsert.getHoverName().getString() + " x" + inserted
                    ));
                    return InteractionResult.SUCCESS;
                }
            }
            // 放入增速插件 (槽位1-4)
            else if (OilGeneratorBlockEntity.isValidUpgrade(heldItem)) {
                for (int slot = 1; slot <= 4; slot++) {
                    if (handler.getStackInSlot(slot).isEmpty()) {
                        ItemStack toInsert = heldItem.copy();
                        toInsert.setCount(1);
                        ItemStack remainder = handler.insertItem(slot, toInsert, false);
                        if (remainder.isEmpty()) {
                            heldItem.shrink(1);
                            int upgradeCount = 0;
                            for (int i = 1; i <= 4; i++) {
                                if (!handler.getStackInSlot(i).isEmpty()) upgradeCount++;
                            }
                            player.sendSystemMessage(Component.literal(
                                    ChatFormatting.AQUA + "安装增速插件! " + ChatFormatting.YELLOW +
                                            "(" + upgradeCount + "/4) " + ChatFormatting.GREEN +
                                            "+" + (upgradeCount * 50) + "% 发电速度"
                            ));
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.RED + "增速插件槽已满! (最多4个)"
                ));
            } else {
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.RED + "此物品不是有效的燃料或增速插件！"
                ));
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.GRAY + "有效燃料: 原油桶、植物油桶"
                ));
            }
        }

        return InteractionResult.PASS;
    }

    private void showStatus(Player player, OilGeneratorBlockEntity generator) {
        int energy = generator.getEnergyStored();
        int maxEnergy = generator.getMaxEnergyStored();
        int burnTime = generator.getBurnTime();
        int maxBurnTime = generator.getMaxBurnTime();

        player.sendSystemMessage(Component.literal(
                ChatFormatting.GOLD + "=== 石油发电机状态 ==="
        ));

        // 能量
        float percentage = maxEnergy > 0 ? (energy * 100.0f / maxEnergy) : 0;
        ChatFormatting energyColor = percentage >= 80 ? ChatFormatting.GREEN :
                percentage >= 50 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        player.sendSystemMessage(Component.literal(
                energyColor + "储能: " + formatAmount(energy) + " / " + formatAmount(maxEnergy) + " RF"
        ));

        // 发电速率
        player.sendSystemMessage(Component.literal(
                ChatFormatting.AQUA + "发电速率: " + generator.getRFPerTick() + " RF/t"
        ));

        // 燃烧状态
        if (generator.isBurning()) {
            float burnPercent = maxBurnTime > 0 ? (burnTime * 100.0f / maxBurnTime) : 0;
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.LIGHT_PURPLE + "⚡ 燃烧中... " + String.format("%.1f", burnPercent) + "% 剩余"
            ));
        } else {
            if (energy >= maxEnergy) {
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.GREEN + "✓ 电量已满"
                ));
            } else {
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.GRAY + "待机中 (放入燃料开始发电)"
                ));
            }
        }

        player.sendSystemMessage(Component.literal(
                ChatFormatting.DARK_GRAY + "自动向相邻机器输出能量"
        ));
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
            if (be != null) {
                be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                        }
                    }
                });
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
