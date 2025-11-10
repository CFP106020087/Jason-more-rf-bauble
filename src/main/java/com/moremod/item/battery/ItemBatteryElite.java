package com.moremod.item.battery;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.List;

public class ItemBatteryElite extends ItemBatteryBase {

    private static final int SELF_CHARGE_RATE = 500; // 500 RF/t 自动发电
    private static final int WIRELESS_RANGE = 5; // 5格无线充电范围
    private static final int WIRELESS_TRANSFER_RATE = 2000; // 每次传输2000 RF

    public ItemBatteryElite() {
        super("battery_elite", 3,
                10000000,    // 10M RF容量
                20000,       // 20K RF/t 输出
                20000,       // 20K RF/t 输入
                "精英",
                TextFormatting.AQUA);
    }

    @Override
    protected void handleBatteryLogic(ItemStack stack, EntityPlayer player) {
        super.handleBatteryLogic(stack, player);

        // 自动发电：每 5 tick 结算一次
        if (player.world.getTotalWorldTime() % 5 == 0) {
            IEnergyStorage energy = stack.getCapability(CapabilityEnergy.ENERGY, null);
            if (energy != null && energy.canReceive()) {
                // 5 tick 充 2500 RF (500 RF/t)
                energy.receiveEnergy(SELF_CHARGE_RATE * 5, false);
            }
        }

        // 无线充电：每 20 tick (1秒) 检测一次
        if (player.world.getTotalWorldTime() % 20 == 0) {
            wirelessChargeNearby(stack, player);
        }
    }

    private void wirelessChargeNearby(ItemStack stack, EntityPlayer player) {
        IEnergyStorage thisEnergy = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (thisEnergy == null || thisEnergy.getEnergyStored() < WIRELESS_TRANSFER_RATE) return;

        // 1. 先充电背包中的其他电池
        chargeInventoryBatteries(stack, player, thisEnergy);

        // 2. 充电附近玩家的电池（5格范围内）
        chargeNearbyPlayerBatteries(stack, player, thisEnergy);
    }

    private void chargeInventoryBatteries(ItemStack stack, EntityPlayer player, IEnergyStorage thisEnergy) {
        for (ItemStack item : player.inventory.mainInventory) {
            if (item.isEmpty() || item == stack) continue;
            if (item.getItem() instanceof ItemBatteryBase) {
                transferEnergy(thisEnergy, item);
            }
        }
    }

    private void chargeNearbyPlayerBatteries(ItemStack stack, EntityPlayer player, IEnergyStorage thisEnergy) {
        // 获取范围内的其他玩家
        AxisAlignedBB range = new AxisAlignedBB(
                player.posX - WIRELESS_RANGE, player.posY - WIRELESS_RANGE, player.posZ - WIRELESS_RANGE,
                player.posX + WIRELESS_RANGE, player.posY + WIRELESS_RANGE, player.posZ + WIRELESS_RANGE
        );

        List<EntityPlayer> nearbyPlayers = player.world.getEntitiesWithinAABB(EntityPlayer.class, range);
        for (EntityPlayer otherPlayer : nearbyPlayers) {
            if (otherPlayer == player) continue; // 跳过自己

            // 充电其他玩家背包中的电池
            for (ItemStack item : otherPlayer.inventory.mainInventory) {
                if (item.isEmpty()) continue;
                if (item.getItem() instanceof ItemBatteryBase) {
                    transferEnergy(thisEnergy, item);
                }
            }
        }
    }

    private void transferEnergy(IEnergyStorage source, ItemStack target) {
        IEnergyStorage targetEnergy = target.getCapability(CapabilityEnergy.ENERGY, null);
        if (targetEnergy != null && targetEnergy.canReceive()) {
            // 尝试传输能量
            int extracted = source.extractEnergy(WIRELESS_TRANSFER_RATE, true);
            if (extracted > 0) {
                int received = targetEnergy.receiveEnergy(extracted, false);
                source.extractEnergy(received, false);
            }
        }
    }

    @Override
    protected void addSpecialTooltip(ItemStack stack, List<String> tooltip) {
        tooltip.add(TextFormatting.AQUA + "无线充电功能");
        tooltip.add(TextFormatting.DARK_AQUA + "⚡ +" + SELF_CHARGE_RATE + " RF/t 自动发电");
        tooltip.add(TextFormatting.DARK_AQUA + "📡 " + WIRELESS_RANGE + "格范围无线充电");
    }

    @Override
    protected void addDetailedTooltip(ItemStack stack, List<String> tooltip) {
        tooltip.add(TextFormatting.GRAY + "• 自动为" + WIRELESS_RANGE + "格内所有玩家的电池充电");
        tooltip.add(TextFormatting.GRAY + "• 每秒传输最多 " + WIRELESS_TRANSFER_RATE + " RF");
        tooltip.add(TextFormatting.GRAY + "• 高速自动发电 (充满约 5.6 小时)");
        tooltip.add(TextFormatting.GRAY + "• 大容量储能与能量平衡系统");
    }
}