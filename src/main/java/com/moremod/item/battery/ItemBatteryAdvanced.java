package com.moremod.item.battery;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import java.util.List;

public class ItemBatteryAdvanced extends ItemBatteryBase {

    private static final int SELF_CHARGE_RATE = 100; // 100 RF/t 自充能

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

        // 自充能：每 10 tick 结算一次 (0.5秒)
        if (player.world.getTotalWorldTime() % 10 == 0) {
            IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
            if (energy != null && energy.canReceive()) {
                // 10 tick 充 1000 RF (100 RF/t)
                energy.receiveEnergy(SELF_CHARGE_RATE * 10, false);
            }
        }
    }

    @Override
    protected void addSpecialTooltip(ItemStack stack, List<String> tooltip) {
        tooltip.add(TextFormatting.YELLOW + "具有自充能功能");
        tooltip.add(TextFormatting.GOLD + "⚡ +" + SELF_CHARGE_RATE + " RF/t 缓慢充电");
    }

    @Override
    protected void addDetailedTooltip(ItemStack stack, List<String> tooltip) {
        tooltip.add(TextFormatting.GRAY + "• 自动缓慢充电，无需外部能源");
        tooltip.add(TextFormatting.GRAY + "• 适合中期使用");
        tooltip.add(TextFormatting.GRAY + "• 10倍于基础电池的容量");
        tooltip.add(TextFormatting.GRAY + "• 充满需要约 2.8 小时游戏时间");
    }
}