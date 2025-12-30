package com.moremod.block;

import com.moremod.block.entity.ChargingStationBlockEntity;
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
 * 充电站方块 - 1.20 Forge版本
 *
 * 功能：
 * - 为玩家饰品充能
 * - RF能量存储
 * - 无线充电范围
 */
public class ChargingStationBlock extends BaseMachineBlock {

    public ChargingStationBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargingStationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHARGING_STATION.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ChargingStationBlockEntity station) {
            // Shift+右键显示状态
            if (player.isShiftKeyDown()) {
                int energy = station.getEnergyStored();
                int maxEnergy = station.getMaxEnergyStored();
                int range = station.getChargeRange();

                player.sendSystemMessage(Component.literal(
                        ChatFormatting.GOLD + "=== 充電站狀態 ==="
                ));
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.YELLOW + "能量: " + energy + " / " + maxEnergy + " RF"
                ));
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.AQUA + "充電範圍: " + range + " 格"
                ));
                return InteractionResult.SUCCESS;
            }

            // 打开GUI
            station.openMenu(player);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GRAY + "為範圍內玩家的饰品充能"));
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "Shift+右鍵查看狀態"));
    }
}
