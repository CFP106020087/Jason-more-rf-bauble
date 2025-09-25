package com.moremod.item.battery;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.List;

public class ItemBatteryUltimate extends ItemBatteryBase {

    public ItemBatteryUltimate() {
        super("battery_ultimate", 4,
                50_000_000,   // 50M RF 容量
                100_000,      // 100K RF/t 输出
                100_000,      // 100K RF/t 输入
                "终极",
                TextFormatting.LIGHT_PURPLE);       // bonusOutput: 2K RF/t 额外发电（基础值）
    }
    int bonusOutput = 50;
    @Override
    protected void handleBatteryLogic(ItemStack stack, EntityPlayer player) {
        super.handleBatteryLogic(stack, player);

        // 每 5 tick 结算一次环境发电
        World world = player.world;
        if (world.getTotalWorldTime() % 5 == 0) {
            IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
            if (energy != null) {
                // 基础：bonusOutput * 5（因为 5tick 结算一次，等价于每tick bonusOutput）
                int generation = bonusOutput * 5;

                // 白天且可见天空：+50%
                if (world.isDaytime() && world.canSeeSky(player.getPosition())) {
                    generation = generation * 3 / 2; // 1.5x，整型安全
                }

                // 下界：x2
                if (player.dimension == -1) {
                    generation *= 2;
                }

                // 末地：稍微加成（如果你也想加成，可以解开下面这行）
                // if (player.dimension == 1) generation = generation * 6 / 5; // +20%

                // 注：receiveEnergy 内部会自己 clamp 到 maxReceive/容量，不用手动截断
                energy.receiveEnergy(generation, false);
            }
        }
    }

    @Override
    protected void addSpecialTooltip(ItemStack stack, List<String> tooltip) {
        tooltip.add(TextFormatting.LIGHT_PURPLE + "环境能量收集");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "自适应充电系统");
    }

    @Override
    protected void addDetailedTooltip(ItemStack stack, List<String> tooltip) {
        tooltip.add(TextFormatting.GRAY + "• 根据环境条件自动调整发电");
        tooltip.add(TextFormatting.GRAY + "• 阳光下发电增加 50%");
        tooltip.add(TextFormatting.GRAY + "• 下界发电翻倍");
        tooltip.add(TextFormatting.GRAY + "• 超大储能容量与高充放速率");
    }
}
