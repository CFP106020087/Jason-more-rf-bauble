package com.moremod.block;

import com.moremod.block.entity.OilExtractorCoreBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

import javax.annotation.Nullable;

/**
 * 抽油机核心方块 - 1.20 Forge版本
 *
 * 功能：
 * - 多方块结构的控制中心
 * - 消耗RF能量从地下提取石油
 * - 输出石油桶
 */
public class OilExtractorCoreBlock extends Block implements EntityBlock {

    public OilExtractorCoreBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(5.0F, 15.0F)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OilExtractorCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == ModBlockEntities.OIL_EXTRACTOR_CORE.get()
                ? (lvl, pos, st, be) -> ((OilExtractorCoreBlockEntity) be).serverTick()
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof OilExtractorCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);

        // 蹲下右键：显示状态
        if (player.isShiftKeyDown()) {
            showStatus(player, core);
            return InteractionResult.CONSUME;
        }

        // 空手右键：提取石油桶
        if (heldItem.isEmpty()) {
            ItemStack extracted = core.extractOilBucket();
            if (!extracted.isEmpty()) {
                if (!player.addItem(extracted)) {
                    player.drop(extracted, false);
                }
                player.displayClientMessage(Component.literal(
                        "§a取出石油桶 x1"
                ), true);
                return InteractionResult.CONSUME;
            } else {
                player.displayClientMessage(Component.literal(
                        "§e没有可提取的石油"
                ), true);
            }
        }

        return InteractionResult.CONSUME;
    }

    private void showStatus(Player player, OilExtractorCoreBlockEntity core) {
        int energy = core.getEnergyStored();
        int maxEnergy = core.getMaxEnergyStored();
        int storedOil = core.getStoredOil();
        int maxOil = core.getMaxOilStorage();
        int buckets = core.getAvailableBuckets();

        player.displayClientMessage(Component.literal(
                "§6=== 抽油机状态 ==="
        ), false);

        // 能量状态
        float percentage = maxEnergy > 0 ? (energy * 100.0f / maxEnergy) : 0;
        String energyColor = percentage >= 50 ? "§a" : percentage >= 20 ? "§e" : "§c";
        player.displayClientMessage(Component.literal(
                energyColor + "能量: " + formatAmount(energy) + " / " + formatAmount(maxEnergy) + " RF"
        ), false);

        // 储油罐状态
        player.displayClientMessage(Component.literal(
                "§8内部储油: " + formatAmount(storedOil) + " / " + formatAmount(maxOil) + " mB"
        ), false);

        // 可提取桶数
        if (buckets > 0) {
            player.displayClientMessage(Component.literal(
                    "§a可提取: " + buckets + " 桶石油"
            ), false);
        }

        // 运行状态
        if (core.isRunning()) {
            player.displayClientMessage(Component.literal(
                    "§d⚡ 正在抽取石油..."
            ), false);
        }
    }

    private String formatAmount(int amount) {
        if (amount >= 1000000) {
            return String.format("%.1fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("%.1fk", amount / 1000.0);
        }
        return String.valueOf(amount);
    }
}
