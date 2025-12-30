package com.moremod.block;

import com.moremod.block.entity.AnimalFeederBlockEntity;
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
 * 动物喂食器方块 - 1.20 Forge版本
 *
 * 功能：
 * - 自动喂养周围的动物使其繁殖
 * - 放入小麦、胡萝卜、种子等
 * - 范围8格
 */
public class AnimalFeederBlock extends BaseMachineBlock {

    public AnimalFeederBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnimalFeederBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.ANIMAL_FEEDER.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof AnimalFeederBlockEntity feeder) {
            ItemStack heldItem = player.getItemInHand(hand);

            // 尝试放入食物
            if (!heldItem.isEmpty()) {
                ItemStack remaining = feeder.addFood(heldItem);
                if (remaining.getCount() != heldItem.getCount()) {
                    if (!player.isCreative()) {
                        player.setItemInHand(hand, remaining);
                    }
                    return InteractionResult.SUCCESS;
                }
            }

            // 显示状态
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.GOLD + "喂食器状态:" +
                            ChatFormatting.GRAY + " 食物: " + ChatFormatting.YELLOW + feeder.getFoodCount() +
                            ChatFormatting.GRAY + " | 范围: " + ChatFormatting.AQUA + "8格" +
                            ChatFormatting.GRAY + " | 冷却: " + ChatFormatting.GREEN + feeder.getCooldownSeconds() + "秒"
            ));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AnimalFeederBlockEntity feeder) {
                feeder.dropInventory();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GRAY + "自动喂养周围的动物使其繁殖"));
        tooltip.add(Component.literal(ChatFormatting.GOLD + "范围: 8格"));
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "接受: 小麦、胡萝卜、种子等"));
    }
}
