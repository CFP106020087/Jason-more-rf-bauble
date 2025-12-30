package com.moremod.block;

import com.moremod.block.entity.TemporalAcceleratorBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 时间加速器方块 - 1.20 Forge版本
 *
 * 功能：
 * - 加速周围方块的tick
 * - 消耗RF能量
 * - 可调节加速倍率
 */
public class TemporalAcceleratorBlock extends BaseMachineBlock {

    public TemporalAcceleratorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TemporalAcceleratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.TEMPORAL_ACCELERATOR.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TemporalAcceleratorBlockEntity accelerator) {
            if (player.isShiftKeyDown()) {
                // 切换倍率
                accelerator.cycleMultiplier();
                int multiplier = accelerator.getMultiplier();
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.AQUA + "加速倍率: " + multiplier + "x"
                ));
            } else {
                // 显示状态
                int energy = accelerator.getEnergyStored();
                int maxEnergy = accelerator.getMaxEnergyStored();
                int multiplier = accelerator.getMultiplier();
                boolean active = accelerator.isActive();

                player.sendSystemMessage(Component.literal(
                        ChatFormatting.GOLD + "=== 時間加速器 ==="
                ));
                player.sendSystemMessage(Component.literal(
                        (active ? ChatFormatting.GREEN + "✓ 運行中" : ChatFormatting.RED + "✗ 已停止")
                ));
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.YELLOW + "能量: " + energy + " / " + maxEnergy + " RF"
                ));
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.AQUA + "加速倍率: " + multiplier + "x"
                ));
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.GRAY + "Shift+右鍵切換倍率"
                ));
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GRAY + "加速周圍方塊的時間流逝"));
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "右鍵查看狀態"));
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "Shift+右鍵切換倍率"));
    }
}
