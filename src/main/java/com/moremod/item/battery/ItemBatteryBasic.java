package com.moremod.item.battery;

import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public class ItemBatteryBasic extends ItemBatteryBase {

    public ItemBatteryBasic() {
        super("battery_basic", 1,
                100_000,     // 100K RF 容量
                1_000,       // 1K RF/t 输出
                1_000,       // 1K RF/t 输入
                "基础",
                TextFormatting.GRAY);
    }

    @Override
    protected void addSpecialTooltip(ItemStack stack, List<String> tooltip) {
        tooltip.add(TextFormatting.GRAY + "初级储能设备");
    }

    @Override
    protected void addDetailedTooltip(ItemStack stack, List<String> tooltip) {
        tooltip.add(TextFormatting.GRAY + "• 适合初期使用");
        tooltip.add(TextFormatting.GRAY + "• 容量有限，需要频繁充电");
    }
}
