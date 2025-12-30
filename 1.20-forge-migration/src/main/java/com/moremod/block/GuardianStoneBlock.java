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
 * 守护者方块 - 1.20 Forge版本
 *
 * 功能：
 * - 多方块结构组件
 * - 用于智慧之泉等结构
 */
public class GuardianStoneBlock extends Block {

    public GuardianStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GRAY + "多方塊結構組件"));
        tooltip.add(Component.literal(ChatFormatting.AQUA + "用於智慧之泉等結構"));
    }
}
