package com.moremod.upgrades;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import com.lycanitesmobs.core.entity.creature.EntityAmalgalich;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 能量消耗管理系統
 * 根據安裝的升級計算能量消耗
 */
public class EnergyConsumptionManager {

    // 各類升級的基礎能量消耗 (RF/秒)
    private static final Map<String, Integer> UPGRADE_CONSUMPTION = new HashMap<>();

    static {
        // 基礎升級消耗
        UPGRADE_CONSUMPTION.put("ARMOR_ENHANCEMENT", 20);      // 護甲強化：每級 20 RF/s
        UPGRADE_CONSUMPTION.put("SPEED_BOOST", 30);           // 速度提升：每級 30 RF/s
        UPGRADE_CONSUMPTION.put("REGENERATION", 50);          // 生命恢復：每級 50 RF/s
        UPGRADE_CONSUMPTION.put("FLIGHT_MODULE", 100);        // 飛行模塊：每級 100 RF/s (飛行時額外)
        UPGRADE_CONSUMPTION.put("SHIELD_GENERATOR", 40);      // 護盾生成：每級 40 RF/s
        UPGRADE_CONSUMPTION.put("TEMPERATURE_CONTROL", 30);   // 溫度調節：每級 30 RF/s

        // 生存類升級消耗
        UPGRADE_CONSUMPTION.put("YELLOW_SHIELD", 40);         // 黃條護盾：每級 40 RF/s
        UPGRADE_CONSUMPTION.put("HEALTH_REGEN", 50);          // 生命恢復：每級 50 RF/s
        UPGRADE_CONSUMPTION.put("HUNGER_THIRST", 20);         // 飢餓口渴：每級 20 RF/s
        UPGRADE_CONSUMPTION.put("THORNS", 30);                // 反傷荊棘：每級 30 RF/s
        UPGRADE_CONSUMPTION.put("FIRE_EXTINGUISH", 20);       // 自動滅火：每級 20 RF/s

        // 輔助類升級消耗
        UPGRADE_CONSUMPTION.put("ORE_VISION", 80);            // 礦物透視：每級 80 RF/s (使用時)
        UPGRADE_CONSUMPTION.put("MOVEMENT_SPEED", 40);        // 移動加速：每級 40 RF/s
        UPGRADE_CONSUMPTION.put("STEALTH", 60);               // 隱身潛行：每級 60 RF/s (激活時)
        UPGRADE_CONSUMPTION.put("EXP_AMPLIFIER", 30);         // 經驗增幅：每級 30 RF/s

        // 戰鬥類升級消耗
        UPGRADE_CONSUMPTION.put("DAMAGE_BOOST", 50);          // 傷害提升：每級 50 RF/s
        UPGRADE_CONSUMPTION.put("ATTACK_SPEED", 40);          // 攻擊速度：每級 40 RF/s
        UPGRADE_CONSUMPTION.put("RANGE_EXTENSION", 30);       // 範圍拓展：每級 30 RF/s
        UPGRADE_CONSUMPTION.put("PURSUIT", 40);               // 追擊打擊：每級 40 RF/s
    }

    /**
     * 計算總能量消耗
     */
    public static int calculateTotalConsumption(ItemStack coreStack, EntityPlayer player) {
        int totalConsumption = 0;
        int nonEnergyUpgrades = 0;

        // 計算基礎升級消耗
        for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
            String key = type.name();
            if (UPGRADE_CONSUMPTION.containsKey(key)) {
                int level = ItemMechanicalCore.getUpgradeLevel(coreStack, type);
                if (level > 0) {
                    totalConsumption += UPGRADE_CONSUMPTION.get(key) * level;
                    nonEnergyUpgrades += level;
                }
            }
        }

        // 計算擴展升級消耗
        for (Map.Entry<String, ItemMechanicalCoreExtended.UpgradeInfo> entry :
                ItemMechanicalCoreExtended.getAllUpgrades().entrySet()) {
            String key = entry.getKey();
            if (UPGRADE_CONSUMPTION.containsKey(key)) {
                int level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, key);
                if (level > 0) {
                    totalConsumption += UPGRADE_CONSUMPTION.get(key) * level;
                    nonEnergyUpgrades += level;
                }
            }
        }

        // 特殊情況額外消耗
        totalConsumption += calculateSpecialConsumption(coreStack, player);

        // 非線性增長：每5個非能量升級增加10%消耗
        float multiplier = 1.0f + (nonEnergyUpgrades / 5) * 0.1f;
        totalConsumption = (int)(totalConsumption * multiplier);

        // 應用能量效率減免
        double efficiency = EnergyEfficiencyManager.getEfficiencyMultiplier(player);
        totalConsumption = (int)(totalConsumption * efficiency);

        return totalConsumption;
    }

    /**
     * 計算特殊情況的額外消耗
     */
    private static int calculateSpecialConsumption(ItemStack coreStack, EntityPlayer player) {
        int extra = 0;

        // 飛行額外消耗
        if (player.capabilities.isFlying) {
            int flightLevel = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE);
            if (flightLevel > 0) {
                ItemMechanicalCore.SpeedMode mode = ItemMechanicalCore.getSpeedMode(coreStack);
                extra += 200 * flightLevel * mode.getMultiplier(); // 200/400/600 RF/s 根據速度模式
            }
        }

        // 礦物透視激活消耗
        if (player.getEntityData().getBoolean("MechanicalCoreOreVision")) {
            int oreLevel = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, "ORE_VISION");
            extra += 100 * oreLevel; // 額外 100/200/300 RF/s
        }

        // 隱身激活消耗
        if (player.getEntityData().getBoolean("MechanicalCoreStealth")) {
            int stealthLevel = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, "STEALTH");
            extra += 150 * stealthLevel; // 額外 150/300/450 RF/s
        }

        // 戰鬥中額外消耗
        if (player.getLastAttackedEntityTime() < 100) { // 5秒內有攻擊
            extra += 100; // 戰鬥狀態額外 100 RF/s
        }

        return extra;
    }

    /**
     * 計算能量平衡（產出 - 消耗）
     */
    public static int calculateEnergyBalance(ItemStack coreStack, EntityPlayer player) {
        int production = calculateTotalProduction(coreStack, player);
        int consumption = calculateTotalConsumption(coreStack, player);
        return production - consumption;
    }

    /**
     * 計算總能量產出（估算）
     */
    private static int calculateTotalProduction(ItemStack coreStack, EntityPlayer player) {
        int totalProduction = 0;

        // 動能發電（移動時平均）
        int kineticLevel = ItemMechanicalCore.getUpgradeLevel(coreStack, "KINETIC_GENERATOR");
        if (kineticLevel > 0 && player.getEntityData().getInteger("MechanicalCoreKineticBuffer") > 0) {
            totalProduction += 150 * kineticLevel; // 平均 150/300/450 RF/s
        }

        // 太陽能（白天）
        int solarLevel = ItemMechanicalCore.getUpgradeLevel(coreStack, "SOLAR_GENERATOR");
        if (solarLevel > 0 && player.world.isDaytime() && player.world.canSeeSky(player.getPosition())) {
            totalProduction += 100 * solarLevel; // 100/200/300 RF/s
        }

        // 虛空能量
        int voidLevel = ItemMechanicalCore.getUpgradeLevel(coreStack, "VOID_ENERGY");
        if (voidLevel > 0 && (player.dimension == 1 || player.posY < 30)) {
            totalProduction += 250 * voidLevel; // 平均 250/500/750 RF/s
        }

        // 應用效率加成（產能加成）
        double efficiencyBonus = 2.0 - EnergyEfficiencyManager.getEfficiencyMultiplier(player);
        totalProduction = (int)(totalProduction * efficiencyBonus);

        return totalProduction;
    }

    /**
     * 應用能量消耗（每秒調用）
     */
    public static void applyEnergyConsumption(EntityPlayer player, ItemStack coreStack) {
        int consumption = calculateTotalConsumption(coreStack, player);

        if (consumption > 0) {
            // 嘗試消耗能量
            boolean consumed = ItemMechanicalCore.consumeEnergy(coreStack, consumption);

            // 能量不足警告
            if (!consumed && player.world.getTotalWorldTime() % 100 == 0) { // 每5秒警告一次
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚡ 能量不足！消耗: " + consumption + " RF/s"
                ), true);
            }
        }
    }

    /**
     * 獲取消耗明細（用於顯示）
     */
    public static ConsumptionBreakdown getConsumptionBreakdown(ItemStack coreStack, EntityPlayer player) {
        ConsumptionBreakdown breakdown = new ConsumptionBreakdown();

        // 收集各類升級消耗
        for (ItemMechanicalCore.UpgradeType type : ItemMechanicalCore.UpgradeType.values()) {
            String key = type.name();
            if (UPGRADE_CONSUMPTION.containsKey(key)) {
                int level = ItemMechanicalCore.getUpgradeLevel(coreStack, type);
                if (level > 0) {
                    int cost = UPGRADE_CONSUMPTION.get(key) * level;
                    breakdown.addItem(type.getDisplayName(), level, cost);
                }
            }
        }

        // 收集擴展升級消耗
        for (Map.Entry<String, ItemMechanicalCoreExtended.UpgradeInfo> entry :
                ItemMechanicalCoreExtended.getAllUpgrades().entrySet()) {
            String key = entry.getKey();
            if (UPGRADE_CONSUMPTION.containsKey(key)) {
                int level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, key);
                if (level > 0) {
                    int cost = UPGRADE_CONSUMPTION.get(key) * level;
                    breakdown.addItem(entry.getValue().displayName, level, cost);
                }
            }
        }

        // 計算總消耗
        breakdown.totalBase = breakdown.items.stream().mapToInt(i -> i.cost).sum();
        breakdown.specialConsumption = calculateSpecialConsumption(coreStack, player);
        breakdown.efficiency = EnergyEfficiencyManager.getEfficiencyMultiplier(player);
        breakdown.totalFinal = calculateTotalConsumption(coreStack, player);

        return breakdown;
    }

    /**
     * 消耗明細類
     */

    public static class ConsumptionBreakdown {
        public List<ConsumptionItem> items = new java.util.ArrayList<>();
        public int totalBase;
        public int specialConsumption;
        public double efficiency;
        public int totalFinal;

        public void addItem(String name, int level, int cost) {
            items.add(new ConsumptionItem(name, level, cost));
        }

        public static class ConsumptionItem {
            public String name;
            public int level;
            public int cost;

            public ConsumptionItem(String name, int level, int cost) {
                this.name = name;
                this.level = level;
                this.cost = cost;
            }
        }
    }
}