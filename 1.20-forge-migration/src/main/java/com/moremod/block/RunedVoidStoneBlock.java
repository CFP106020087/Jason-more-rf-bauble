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
 * 符文虚空石方块 - 1.20 Forge版本
 *
 * 功能：
 * - 多方块结构组件
 * - 蕴含虚空能量
 */
public class RunedVoidStoneBlock extends Block {

    public RunedVoidStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GRAY + "蕴含虚空能量的符文石"));
        tooltip.add(Component.literal(ChatFormatting.DARK_PURPLE + "多方塊結構組件"));
    }
}
