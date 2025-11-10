package com.moremod.item.battery;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.List;

public class ItemBatteryUltimate extends ItemBatteryBase {

    private static final int BASE_GENERATION = 2000; // 基础发电 2000 RF/t

    public ItemBatteryUltimate() {
        super("battery_ultimate", 4,
                50_000_000,   // 50M RF 容量
                100_000,      // 100K RF/t 输出
                100_000,      // 100K RF/t 输入
                "终极",
                TextFormatting.LIGHT_PURPLE);
    }

    @Override
    protected void handleBatteryLogic(ItemStack stack, EntityPlayer player) {
        super.handleBatteryLogic(stack, player);

        // 每 5 tick 结算一次环境发电
        World world = player.world;
        if (world.getTotalWorldTime() % 5 == 0) {
            IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
            if (energy != null && energy.canReceive()) {
                int generation = calculateGeneration(world, player);
                energy.receiveEnergy(generation, false);
            }
        }
    }

    private int calculateGeneration(World world, EntityPlayer player) {
        // 基础发电：2000 RF/t × 5 tick = 10000 RF
        int generation = BASE_GENERATION * 5;

        float multiplier = 1.0f;

        // 白天且可见天空：+50%
        if (world.isDaytime() && world.canSeeSky(player.getPosition())) {
            multiplier += 0.5f;
        }

        // 夜晚且可见天空（吸收月光）：+25%
        else if (!world.isDaytime() && world.canSeeSky(player.getPosition())) {
            multiplier += 0.25f;
        }

        // 维度加成
        switch (player.dimension) {
            case -1: // 下界：岩浆能量，×2
                multiplier *= 2.0f;
                break;
            case 1:  // 末地：虚空能量，×1.5
                multiplier *= 1.5f;
                break;
            case 0:  // 主世界
                // 在特定生物群系额外加成
                if (isBiomeBonus(world, player)) {
                    multiplier += 0.3f; // 特殊生物群系 +30%
                }
                break;
        }

        // 高度加成：Y > 200 时，+20%（接近天空）
        if (player.posY > 200) {
            multiplier += 0.2f;
        }
        // 深度加成：Y < 16 时，+30%（接近基岩层，地热能量）
        else if (player.posY < 16) {
            multiplier += 0.3f;
        }

        return (int) (generation * multiplier);
    }

    private boolean isBiomeBonus(World world, EntityPlayer player) {
        // 使用 getRegistryName() 替代 getBiomeName() - 更稳定且兼容所有版本
        String biomeName = world.getBiome(player.getPosition()).getRegistryName().toString().toLowerCase();
        return biomeName.contains("desert") ||
                biomeName.contains("jungle") ||
                biomeName.contains("ocean") ||
                biomeName.contains("mesa") ||
                biomeName.contains("savanna");
    }

    @Override
    protected void addSpecialTooltip(ItemStack stack, List<String> tooltip) {
        tooltip.add(TextFormatting.LIGHT_PURPLE + "环境能量收集");
        tooltip.add(TextFormatting.DARK_PURPLE + "⚡ 基础 " + BASE_GENERATION + " RF/t");
        tooltip.add(TextFormatting.DARK_PURPLE + "🌍 自适应多维度发电");
    }

    @Override
    protected void addDetailedTooltip(ItemStack stack, List<String> tooltip) {
        tooltip.add(TextFormatting.GRAY + "环境加成:");
        tooltip.add(TextFormatting.YELLOW + "  • 白天+天空: +50% | 夜晚+天空: +25%");
        tooltip.add(TextFormatting.RED + "  • 下界: ×2 | " + TextFormatting.DARK_PURPLE + "末地: ×1.5");
        tooltip.add(TextFormatting.AQUA + "  • Y>200: +20% | " + TextFormatting.GOLD + "Y<16: +30%");
        tooltip.add(TextFormatting.GREEN + "  • 特殊生物群系: +30%");
        tooltip.add(TextFormatting.GRAY + "");
        tooltip.add(TextFormatting.GRAY + "• 最高可达约 8000 RF/t");
        tooltip.add(TextFormatting.GRAY + "• 充满需要约 1.7 小时游戏时间");
        tooltip.add(TextFormatting.GRAY + "• 超大储能容量与极限充放速率");
    }
}