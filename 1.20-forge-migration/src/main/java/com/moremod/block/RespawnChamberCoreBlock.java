package com.moremod.block;

import com.moremod.block.entity.RespawnChamberCoreBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.ChatFormatting;
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
 * 重生仓核心方块 - 1.20 Forge版本
 *
 * 功能：
 * - 多方块结构的控制中心
 * - 玩家右键绑定重生点
 * - 支持多种框架等级
 */
public class RespawnChamberCoreBlock extends Block implements EntityBlock {

    public RespawnChamberCoreBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(5.0F, 15.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> 8)); // 0.5F * 15 ≈ 8
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RespawnChamberCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == ModBlockEntities.RESPAWN_CHAMBER_CORE.get()
                ? (lvl, pos, st, be) -> ((RespawnChamberCoreBlockEntity) be).serverTick()
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RespawnChamberCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }

        // 蹲下右键：显示状态
        if (player.isShiftKeyDown()) {
            showStatus(player, core, pos, level);
            return InteractionResult.CONSUME;
        }

        // 普通右键：绑定重生点
        core.bindPlayer(player);
        player.displayClientMessage(Component.literal(
                ChatFormatting.GREEN + "✓ 已绑定重生点到此重生仓"
        ), true);
        return InteractionResult.CONSUME;
    }

    private void showStatus(Player player, RespawnChamberCoreBlockEntity core, BlockPos pos, Level level) {
        boolean structureValid = core.isStructureValid();
        int tier = core.getStructureTier();

        player.displayClientMessage(Component.literal(
                ChatFormatting.DARK_PURPLE + "=== 重生仓状态 ==="
        ), false);

        // 结构状态
        if (structureValid) {
            player.displayClientMessage(Component.literal(
                    ChatFormatting.GREEN + "✓ 结构完整 " + ChatFormatting.GRAY + "(等级: " + getTierName(tier) + ")"
            ), false);
        } else {
            player.displayClientMessage(Component.literal(
                    ChatFormatting.RED + "✗ 结构不完整！"
            ), false);
            player.displayClientMessage(Component.literal(
                    ChatFormatting.GRAY + "需要 3x3x3 结构："
            ), false);
            player.displayClientMessage(Component.literal(
                    ChatFormatting.GRAY + "  - 地板: 8个框架方块环绕核心"
            ), false);
            player.displayClientMessage(Component.literal(
                    ChatFormatting.GRAY + "  - 中层: 4角框架，中间空气"
            ), false);
            player.displayClientMessage(Component.literal(
                    ChatFormatting.GRAY + "  - 天花板: 8个框架 + 中心光源"
            ), false);
        }

        // 绑定状态
        if (core.hasBoundPlayer()) {
            player.displayClientMessage(Component.literal(
                    ChatFormatting.AQUA + "绑定玩家: " + ChatFormatting.WHITE + core.getBoundPlayerName()
            ), false);
        } else {
            player.displayClientMessage(Component.literal(
                    ChatFormatting.GRAY + "绑定玩家: 无"
            ), false);
        }

        // 位置信息
        player.displayClientMessage(Component.literal(
                ChatFormatting.GRAY + "位置: " + ChatFormatting.WHITE +
                        String.format("X:%d Y:%d Z:%d (维度:%s)",
                                pos.getX(), pos.getY(), pos.getZ(),
                                level.dimension().location().toString())
        ), false);

        // 使用提示
        player.displayClientMessage(Component.literal(
                ChatFormatting.DARK_GRAY + "右键核心绑定重生点"
        ), false);
    }

    private String getTierName(int tier) {
        return switch (tier) {
            case 4 -> ChatFormatting.GREEN + "绿宝石框架";
            case 3 -> ChatFormatting.AQUA + "钻石/黑曜石框架";
            case 2 -> ChatFormatting.GOLD + "金框架";
            default -> ChatFormatting.WHITE + "铁框架";
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
