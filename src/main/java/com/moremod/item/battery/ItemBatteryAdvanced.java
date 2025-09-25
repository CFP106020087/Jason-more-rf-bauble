package com.moremod.item.battery;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import java.util.List;

public class ItemBatteryAdvanced extends ItemBatteryBase {

    public ItemBatteryAdvanced() {
        super("battery_advanced", 2,
                1000000,     // 1M RF容量
                5000,        // 5K RF/t 输出
                5000,        // 5K RF/t 输入
                "高级",
                TextFormatting.YELLOW);        // 100 RF/t 额外发电
    }

    @Override
    protected void handleBatteryLogic(ItemStack stack, EntityPlayer player) {
        super.handleBatteryLogic(stack, player);

    }

    @Override
    protected void addSpecialTooltip(ItemStack stack,List<String> tooltip) {
        tooltip.add(TextFormatting.YELLOW + "具有自充能功能");
    }

    @Override
    protected void addDetailedTooltip(ItemStack stack,List<String> tooltip) {
        tooltip.add(TextFormatting.GRAY + "• 缓慢自动充电");
        tooltip.add(TextFormatting.GRAY + "• 适合中期使用");
        tooltip.add(TextFormatting.GRAY + "• 10倍于基础电池的容量");
    }
}