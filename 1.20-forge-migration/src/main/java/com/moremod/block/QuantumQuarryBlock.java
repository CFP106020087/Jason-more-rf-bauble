package com.moremod.block;

import com.moremod.block.entity.QuantumQuarryBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 量子采石场方块 - 1.20 Forge版本
 *
 * 功能：
 * - 虚拟维度采矿
 * - 需要六面量子驱动器
 * - 多种模式（采矿/怪物掉落/战利品）
 */
public class QuantumQuarryBlock extends BaseMachineBlock {

    public QuantumQuarryBlock(Properties properties) {
        super(properties.lightLevel(state -> 8)); // 0.5F * 15 ≈ 8
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuantumQuarryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.QUANTUM_QUARRY.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof QuantumQuarryBlockEntity quarry) {
            // 检查结构是否完整
            if (!quarry.isStructureValid()) {
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.RED + "⚠ 結構不完整！需要在六面放置量子驅動器"
                ));
                return InteractionResult.SUCCESS;
            }

            // TODO: 打开GUI
            // 1.20使用MenuProvider和NetworkHooks.openScreen
            quarry.openMenu(player);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof QuantumQuarryBlockEntity quarry) {
                // 掉落缓冲区中的物品
                quarry.dropContents(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof QuantumQuarryBlockEntity quarry) {
            quarry.updateRedstoneState();
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GRAY + "從虛擬維度中採集方塊"));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "需要在六面放置量子驅動器"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal(ChatFormatting.AQUA + "模式:"));
        tooltip.add(Component.literal(ChatFormatting.WHITE + "  採礦 - 根據生態系生成礦物"));
        tooltip.add(Component.literal(ChatFormatting.WHITE + "  怪物掉落 - 模擬擊殺怪物"));
        tooltip.add(Component.literal(ChatFormatting.WHITE + "  戰利品 - 生成寶藏物品"));
    }
}
