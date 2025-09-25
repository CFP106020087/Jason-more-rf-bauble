package com.moremod.item;

import com.moremod.item.upgrades.ItemUpgradeComponent;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.util.text.TextFormatting;
import com.moremod.item.UpgradeType;

/**
 * 升级组件定义类 - 使用統一的 UpgradeType
 */
public class UpgradeItems {

    // ===== 能量容量升级 =====
    public static final ItemUpgradeComponent ENERGY_CELL_BASIC = createUpgrade(
            UpgradeType.ENERGY_CAPACITY,
            "energy_cell_basic",
            new String[]{
                    TextFormatting.GOLD + "扩展核心能量储备",
                    "",
                    TextFormatting.YELLOW + "▶ 容量提升:",
                    TextFormatting.GRAY + "  每级 +50,000 RF",
                    TextFormatting.GRAY + "  基础: 10,000 RF",
                    TextFormatting.GRAY + "  满级: 510,000 RF (10级)",
                    "",
                    TextFormatting.DARK_GRAY + "更大的能量储备支持更长时间的战斗"
            },
            1,
            16
    );

    // ===== 能量效率升级 =====
    public static final ItemUpgradeComponent EFFICIENCY_CHIP = createUpgrade(
            UpgradeType.ENERGY_EFFICIENCY,
            "efficiency_chip",
            new String[]{
                    TextFormatting.GREEN + "优化能量利用效率",
                    "",
                    TextFormatting.YELLOW + "▶ 消耗减少:",
                    TextFormatting.GRAY + "  Lv.1: -15% | Lv.2: -30%",
                    TextFormatting.GRAY + "  Lv.3: -45% | Lv.4: -60%",
                    TextFormatting.GRAY + "  Lv.5: -75% (满级)",
                    "",
                    TextFormatting.DARK_GRAY + "影响所有RF/FE设备的能量消耗"
            },
            1,
            16
    );

    // ===== 护甲强化升级 =====
    public static final ItemUpgradeComponent ARMOR_PLATING = createUpgrade(
            UpgradeType.ARMOR_ENHANCEMENT,
            "armor_plating",
            new String[]{
                    TextFormatting.BLUE + "强化装甲防护系统",
                    "",
                    TextFormatting.YELLOW + "▶ 基础减伤 (递减):",
                    TextFormatting.GRAY + "  Lv.1: 6% | Lv.2: 10.6% | Lv.3: 14.1%",
                    TextFormatting.GRAY + "  Lv.4: 16.9% | Lv.5: 19.2%",
                    "",
                    TextFormatting.LIGHT_PURPLE + "▶ 特殊能力:",
                    TextFormatting.GRAY + "  Lv.3: " + TextFormatting.DARK_PURPLE + "能量爆发 " + TextFormatting.GRAY + "(15+伤害触发)",
                    TextFormatting.GRAY + "  Lv.4: " + TextFormatting.GOLD + "致命保护 " + TextFormatting.GRAY + "(10分钟CD)",
                    TextFormatting.GRAY + "  Lv.5: " + TextFormatting.LIGHT_PURPLE + "无敌护盾 " + TextFormatting.GRAY + "(3秒/60秒CD)",
                    "",
                    TextFormatting.DARK_GRAY + "高级防护需要充足能量支持"
            },
            1,
            16
    );

    // ===== 飞行模块 - 三个等级 =====
    public static final ItemUpgradeComponent FLIGHT_MODULE_BASIC = createUpgrade(
            UpgradeType.FLIGHT_MODULE,
            "flight_module_basic",
            new String[]{
                    TextFormatting.AQUA + "基础飞行推进系统",
                    "",
                    TextFormatting.YELLOW + "▶ 飞行性能:",
                    TextFormatting.GRAY + "  上升: 0.08 方块/tick",
                    TextFormatting.GRAY + "  下降: 0.15 方块/tick",
                    TextFormatting.GRAY + "  推进: 0.03 方块/tick",
                    "",
                    TextFormatting.RED + "▶ 能耗: 50 RF/tick",
                    "",
                    TextFormatting.GREEN + "按V键切换飞行",
                    TextFormatting.DARK_GRAY + "空格上升 | Shift下降"
            },
            1,
            1
    );

    public static final ItemUpgradeComponent FLIGHT_MODULE_ADVANCED = createUpgrade(
            UpgradeType.FLIGHT_MODULE,
            "flight_module_advanced",
            new String[]{
                    TextFormatting.GOLD + "✦ 高级飞行推进系统 ✦",
                    "",
                    TextFormatting.YELLOW + "▶ 性能提升:",
                    TextFormatting.GRAY + "  • 速度提升 50%",
                    TextFormatting.GRAY + "  • 解锁悬停模式",
                    TextFormatting.GRAY + "  • 更精准的控制",
                    "",
                    TextFormatting.RED + "▶ 悬停能耗: 25 RF/tick",
                    "",
                    TextFormatting.DARK_GREEN + "按H键切换悬停",
                    TextFormatting.YELLOW + "⚠ 需要: 基础飞行模块"
            },
            1,
            1
    );

    public static final ItemUpgradeComponent FLIGHT_MODULE_ULTIMATE = createUpgrade(
            UpgradeType.FLIGHT_MODULE,
            "flight_module_ultimate",
            new String[]{
                    TextFormatting.LIGHT_PURPLE + "✦✦ 终极飞行推进系统 ✦✦",
                    "",
                    TextFormatting.YELLOW + "▶ 终极能力:",
                    TextFormatting.GRAY + "  • 三档速度模式",
                    TextFormatting.WHITE + "    标准 1.0x | 快速 1.5x | 极速 2.0x",
                    TextFormatting.GRAY + "  • 高空保护 (Y>200)",
                    TextFormatting.GRAY + "  • 能量尾迹特效",
                    "",
                    TextFormatting.RED + "▶ 能耗随速度提升",
                    "",
                    TextFormatting.DARK_GREEN + "按C键切换速度模式",
                    TextFormatting.YELLOW + "⚠ 需要: 高级飞行模块"
            },
            1,
            1
    );

    // ===== 温度调节升级 =====
    public static final ItemUpgradeComponent TEMPERATURE_UPGRADE = createUpgrade(
            UpgradeType.TEMPERATURE_CONTROL,
            "temperature_control_upgrade",
            new String[]{
                    TextFormatting.DARK_AQUA + "恒温调节系统",
                    "",
                    TextFormatting.YELLOW + "▶ 自动调节体温:",
                    TextFormatting.GRAY + "  • 抵御极端温度",
                    TextFormatting.GRAY + "  • 减少温度伤害",
                    TextFormatting.GRAY + "  • 加快温度恢复",
                    "",
                    TextFormatting.GREEN + "兼容 SimpleDifficulty",
                    TextFormatting.DARK_GRAY + "最高5级，每级提升效果"
            },
            1,
            16
    );

    // ===== 特殊组合升级 =====
    public static final ItemUpgradeComponent OMNIPOTENT_PACKAGE = createUpgrade(
            UpgradeType.OMNIPOTENT_PACKAGE,
            "omnipotent_package_chip",
            new String[]{
                    TextFormatting.LIGHT_PURPLE + "✦ 全能强化芯片 ✦",
                    TextFormatting.DARK_PURPLE + "传说级综合升级模块",
                    "",
                    TextFormatting.YELLOW + "▶ 同时提升:",
                    TextFormatting.GOLD + "  • 能量容量 +1级",
                    TextFormatting.GREEN + "  • 能量效率 +1级",
                    TextFormatting.BLUE + "  • 护甲强化 +1级",
                    "",
                    TextFormatting.RED + "✦ 极其稀有 ✦",
                    TextFormatting.DARK_GRAY + "一次升级，三重强化"
            },
            1,
            1
    );

    /**
     * 创建升级组件的辅助方法 - 使用 UpgradeType 枚举
     */
    private static ItemUpgradeComponent createUpgrade(UpgradeType type, String registryName,
                                                      String[] descriptions, int upgradeValue, int maxStackSize) {
        ItemUpgradeComponent upgrade = new ItemUpgradeComponent(type, descriptions, upgradeValue);

        upgrade.setRegistryName("moremod", registryName);
        upgrade.setTranslationKey(registryName);
        upgrade.setCreativeTab(moremodCreativeTab.moremod_TAB);
        upgrade.setMaxStackSize(maxStackSize);

        return upgrade;
    }

    /**
     * 获取所有升级组件的数组（用于批量注册）
     */
    public static ItemUpgradeComponent[] getAllUpgrades() {
        return new ItemUpgradeComponent[]{
                ENERGY_CELL_BASIC,
                EFFICIENCY_CHIP,
                ARMOR_PLATING,
                FLIGHT_MODULE_BASIC,
                FLIGHT_MODULE_ADVANCED,
                FLIGHT_MODULE_ULTIMATE,
                TEMPERATURE_UPGRADE,
                OMNIPOTENT_PACKAGE
        };
    }

    /**
     * 打印所有升级组件信息（调试用）
     */
    public static void printUpgradeInfo() {
        System.out.println("[moremod] ⚙️ 升级组件信息:");
        for (ItemUpgradeComponent upgrade : getAllUpgrades()) {
            if (upgrade.getRegistryName() != null) {
                System.out.println("  - " + upgrade.getRegistryName() + " (类型: " +
                        upgrade.getUpgradeType() + ", 堆叠:" + upgrade.getItemStackLimit() + ")");
            }
        }
    }
}