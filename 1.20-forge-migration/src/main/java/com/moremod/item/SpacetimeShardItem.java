package com.moremod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 时空碎片物品 - 1.20 Forge版本
 *
 * 从时空碎片矿石中挖掘获得的材料
 * 用于高级合成配方
 */
public class SpacetimeShardItem extends Item {

    public SpacetimeShardItem() {
        super(new Item.Properties().rarity(Rarity.RARE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // 附魔光效
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.LIGHT_PURPLE + "蕴含时空之力的神秘碎片"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal(ChatFormatting.GRAY + "用途："));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "  • 高级机械核心升级"));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "  • 维度钥匙制作"));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "  • 时间加速器核心"));
    }
}
