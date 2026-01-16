package com.moremod.item.battery;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import java.util.List;

public class ItemBatteryAdvanced extends ItemBatteryBase {

    public ItemBatteryAdvanced() {
        super("battery_advanced", 2,
                1000000,     // 1M RF容量
                5000,        // 5K RF/t 输出
                5000,        // 5K RF/t 输入
                "高级",
                TextFormatting.YELLOW);
    }

    @Override
    protected void handleBatteryLogic(ItemStack stack, EntityPlayer player) {
        super.handleBatteryLogic(stack, player);
        // 自充电功能已移除
    }

    @Override
    protected void addSpecialTooltip(ItemStack stack, List<String> tooltip) {
        tooltip.add(TextFormatting.YELLOW + "高容量储能电池");
        tooltip.add(TextFormatting.GOLD + "⚡ 10倍于基础电池");
    }

    @Override
    protected void addDetailedTooltip(ItemStack stack, List<String> tooltip) {
        tooltip.add(TextFormatting.GRAY + "• 大容量电池，适合中期使用");
        tooltip.add(TextFormatting.GRAY + "• 高速充电/放电");
        tooltip.add(TextFormatting.GRAY + "• 10倍于基础电池的容量");
    }
}