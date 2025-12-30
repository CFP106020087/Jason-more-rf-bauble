package com.moremod.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 量子驱动器方块 - 1.20 Forge版本
 *
 * 功能：
 * - 量子采石场的组件
 * - 需要放置在采石场六面
 */
public class QuarryActuatorBlock extends Block {

    public QuarryActuatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GRAY + "量子采石场的驱动组件"));
        tooltip.add(Component.literal(ChatFormatting.AQUA + "放置在采石场六个面上"));
    }
}
