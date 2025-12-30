package com.moremod.block;

import com.moremod.block.entity.UpgradeChamberCoreBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * 升級艙核心方塊 - 1.20 Forge版本
 *
 * 功能：
 * - 多方塊結構的控制中心
 * - 存儲RF能量
 * - 接受升級模組
 * - 檢測玩家進入並執行升級
 *
 * 1.12 -> 1.20 API变更:
 * - BlockContainer -> BaseEntityBlock
 * - onBlockActivated -> use
 * - TileEntity -> BlockEntity
 * - TextComponentString -> Component.literal
 * - player.sendMessage -> player.sendSystemMessage
 */
public class UpgradeChamberCoreBlock extends BaseMachineBlock {

    public UpgradeChamberCoreBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UpgradeChamberCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.UPGRADE_CHAMBER_CORE.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof UpgradeChamberCoreBlockEntity core)) {
            return InteractionResult.FAIL;
        }

        // 蹲下右鍵：顯示結構檢查和狀態
        if (player.isShiftKeyDown()) {
            showStatus(player, core, pos, level);
            return InteractionResult.SUCCESS;
        }

        // 處理物品交互
        IItemHandler handler = level.getCapability(ForgeCapabilities.ITEM_HANDLER, pos, hit.getDirection());
        if (handler == null) {
            return InteractionResult.FAIL;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        ItemStack currentModule = handler.getStackInSlot(0);

        // 空手取出模組
        if (heldItem.isEmpty() && !currentModule.isEmpty()) {
            ItemStack extracted = handler.extractItem(0, 64, false);
            if (!player.getInventory().add(extracted)) {
                player.drop(extracted, false);
            }
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.YELLOW + "已取出升級模組: " + extracted.getHoverName().getString()
            ));
            core.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            return InteractionResult.SUCCESS;
        }

        // 放入模組
        if (!heldItem.isEmpty() && currentModule.isEmpty()) {
            ItemStack toInsert = heldItem.copy();
            toInsert.setCount(1);
            ItemStack remainder = handler.insertItem(0, toInsert, false);
            if (remainder.isEmpty()) {
                heldItem.shrink(1);
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.GREEN + "已放入升級模組: " + toInsert.getHoverName().getString()
                ));
                core.setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                return InteractionResult.SUCCESS;
            } else {
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.RED + "此物品不是有效的升級模組！"
                ));
            }
        }

        // 替換模組
        if (!heldItem.isEmpty() && !currentModule.isEmpty()) {
            ItemStack toInsert = heldItem.copy();
            toInsert.setCount(1);

            // 先取出舊的
            ItemStack extracted = handler.extractItem(0, 64, false);
            // 放入新的
            ItemStack remainder = handler.insertItem(0, toInsert, false);

            if (remainder.isEmpty()) {
                heldItem.shrink(1);
                if (!player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.GREEN + "已替換升級模組: " + toInsert.getHoverName().getString()
                ));
                core.setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                return InteractionResult.SUCCESS;
            } else {
                // 放回舊的
                handler.insertItem(0, extracted, false);
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.RED + "此物品不是有效的升級模組！"
                ));
            }
        }

        return InteractionResult.PASS;
    }

    private void showStatus(Player player, UpgradeChamberCoreBlockEntity core, BlockPos pos, Level level) {
        // TODO: 移植MultiblockUpgradeChamber检查逻辑
        boolean structureValid = core.isStructureValid();
        int tier = core.getFrameTier();
        int energy = core.getEnergyStored();
        int maxEnergy = core.getMaxEnergyStored();
        int requiredEnergy = core.getRequiredEnergy();
        ItemStack module = core.getModuleStack();
        boolean isRunning = core.isUpgrading();

        player.sendSystemMessage(Component.literal(
                ChatFormatting.GOLD + "=== 升級艙狀態 ==="
        ));

        // 結構狀態
        if (structureValid) {
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.GREEN + "✓ 結構完整 " + ChatFormatting.GRAY + "(等級: " + getTierName(tier) + ")"
            ));
        } else {
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.RED + "✗ 結構不完整！請檢查多方塊結構"
            ));
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.GRAY + "使用說明書查看建造指南"
            ));
        }

        // 能量狀態
        float percentage = maxEnergy > 0 ? (energy * 100.0f / maxEnergy) : 0;
        ChatFormatting energyColor = percentage >= 100 ? ChatFormatting.GREEN :
                percentage >= 50 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        player.sendSystemMessage(Component.literal(
                energyColor + "能量: " + energy + " / " + maxEnergy + " RF (" + String.format("%.1f", percentage) + "%)"
        ));

        // 模組狀態
        if (!module.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.AQUA + "模組: " + module.getHoverName().getString()
            ));
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.GRAY + "所需能量: " + requiredEnergy + " RF"
            ));
        } else {
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.GRAY + "模組: 無 (放入升級模組開始升級)"
            ));
            int repairEnergy = (int)(energy * 0.5f);
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.AQUA + "修復模式: 走進升級艙可修復損壞的模組"
            ));
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.GRAY + "修復消耗: " + repairEnergy + " RF (50%當前能量)"
            ));
        }

        // 運行狀態
        if (isRunning) {
            int progress = core.getProgress();
            int maxProgress = core.getMaxProgress();
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.LIGHT_PURPLE + "⚡ 升級中... " + progress + "/" + maxProgress
            ));
        }

        // 修復狀態
        if (core.isRepairing()) {
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.AQUA + "⚡ 修復中..."
            ));
        }
    }

    private String getTierName(int tier) {
        return switch (tier) {
            case 4 -> ChatFormatting.GREEN + "綠寶石框架";
            case 3 -> ChatFormatting.AQUA + "鑽石框架";
            case 2 -> ChatFormatting.GOLD + "金框架";
            default -> ChatFormatting.WHITE + "鐵框架";
        };
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof UpgradeChamberCoreBlockEntity core) {
                ItemStack module = core.getModuleStack();
                if (!module.isEmpty()) {
                    popResource(level, pos, module);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
