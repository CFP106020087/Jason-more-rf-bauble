package com.moremod.item.battery;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.List;

public class ItemBatteryElite extends ItemBatteryBase {

    private static final int WIRELESS_RANGE = 5; // 5格范围

    public ItemBatteryElite() {
        super("battery_elite", 3,
                10000000,    // 10M RF容量
                20000,       // 20K RF/t 输出
                20000,       // 20K RF/t 输入
                "精英",
                TextFormatting.AQUA);        // 500 RF/t 额外发电
    }

    @Override
    protected void handleBatteryLogic(ItemStack stack, EntityPlayer player) {
        super.handleBatteryLogic(stack, player);

        // 自动发电


        // 无线充电附近的其他电池
        if (player.world.getTotalWorldTime() % 20 == 0) {
            wirelessChargeNearbyBatteries(stack, player);
        }
    }

    private void wirelessChargeNearbyBatteries(ItemStack stack, EntityPlayer player) {
        IEnergyStorage thisEnergy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (thisEnergy == null || thisEnergy.getEnergyStored() < 10000) return;

        // 检查背包中的其他电池
        for (ItemStack item : player.inventory.mainInventory) {
            if (item.getItem() instanceof ItemBatteryBase && item != stack) {
                IEnergyStorage otherEnergy = item.getCapability(CapabilityEnergy.ENERGY, null);
                if (otherEnergy != null && otherEnergy.canReceive()) {
                    int transferred = thisEnergy.extractEnergy(1000, true);
                    if (transferred > 0) {
                        int received = otherEnergy.receiveEnergy(transferred, false);
                        thisEnergy.extractEnergy(received, false);
                    }
                }
            }
        }
    }

    @Override
    protected void addSpecialTooltip(ItemStack stack,List<String> tooltip) {
        tooltip.add(TextFormatting.AQUA + "无线充电功能");
        tooltip.add(TextFormatting.AQUA + "自动平衡能量");
    }

    @Override
    protected void addDetailedTooltip(ItemStack stack,List<String> tooltip) {
        tooltip.add(TextFormatting.GRAY + "• 自动为背包中其他电池充电");
        tooltip.add(TextFormatting.GRAY + "• 高速自动发电");
        tooltip.add(TextFormatting.GRAY + "• 大容量储能");
    }
}