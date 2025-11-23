package com.moremod.item;

import com.moremod.item.upgrades.ItemUpgradeComponent;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.util.text.TextFormatting;
import com.moremod.item.UpgradeType;

/**
 * 升级组件定义类 - 分级模块系统
 * 每个等级需要对应等级的模块
 */
public class UpgradeItems {

    // ===== 能量容量升级（10级） =====
    public static final ItemUpgradeComponent ENERGY_CAPACITY_LV1 = createUpgrade(
            UpgradeType.ENERGY_CAPACITY, "energy_capacity_lv1",
            new String[]{
                    TextFormatting.GOLD + "能量电池 I",
                    TextFormatting.GRAY + "将能量容量提升至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 容量: 60,000 RF",
                    TextFormatting.GRAY + "  基础: 10,000 RF",
                    TextFormatting.DARK_GRAY + "基础级能量扩展"
            }, 1, 16
    );

    public static final ItemUpgradeComponent ENERGY_CAPACITY_LV2 = createUpgrade(
            UpgradeType.ENERGY_CAPACITY, "energy_capacity_lv2",
            new String[]{
                    TextFormatting.GOLD + "能量电池 II",
                    TextFormatting.GRAY + "将能量容量提升至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 容量: 110,000 RF",
                    TextFormatting.DARK_GRAY + "标准级能量扩展"
            }, 2, 16
    );

    public static final ItemUpgradeComponent ENERGY_CAPACITY_LV3 = createUpgrade(
            UpgradeType.ENERGY_CAPACITY, "energy_capacity_lv3",
            new String[]{
                    TextFormatting.GOLD + "能量电池 III",
                    TextFormatting.GRAY + "将能量容量提升至 Lv.3",
                    "",
                    TextFormatting.YELLOW + "▶ 容量: 160,000 RF",
                    TextFormatting.DARK_GRAY + "改良级能量扩展"
            }, 3, 8
    );

    public static final ItemUpgradeComponent ENERGY_CAPACITY_LV4 = createUpgrade(
            UpgradeType.ENERGY_CAPACITY, "energy_capacity_lv4",
            new String[]{
                    TextFormatting.GOLD + "能量电池 IV",
                    TextFormatting.GRAY + "将能量容量提升至 Lv.4",
                    "",
                    TextFormatting.YELLOW + "▶ 容量: 210,000 RF",
                    TextFormatting.DARK_GRAY + "高级能量扩展"
            }, 4, 8
    );

    public static final ItemUpgradeComponent ENERGY_CAPACITY_LV5 = createUpgrade(
            UpgradeType.ENERGY_CAPACITY, "energy_capacity_lv5",
            new String[]{
                    TextFormatting.GOLD + "能量电池 V",
                    TextFormatting.GRAY + "将能量容量提升至 Lv.5",
                    "",
                    TextFormatting.YELLOW + "▶ 容量: 260,000 RF",
                    TextFormatting.BLUE + "精英级能量扩展"
            }, 5, 4
    );

    public static final ItemUpgradeComponent ENERGY_CAPACITY_LV6 = createUpgrade(
            UpgradeType.ENERGY_CAPACITY, "energy_capacity_lv6",
            new String[]{
                    TextFormatting.GOLD + "量子电池 VI",
                    TextFormatting.GRAY + "将能量容量提升至 Lv.6",
                    "",
                    TextFormatting.YELLOW + "▶ 容量: 310,000 RF",
                    TextFormatting.BLUE + "量子级存储"
            }, 6, 4
    );

    public static final ItemUpgradeComponent ENERGY_CAPACITY_LV7 = createUpgrade(
            UpgradeType.ENERGY_CAPACITY, "energy_capacity_lv7",
            new String[]{
                    TextFormatting.GOLD + "量子电池 VII",
                    TextFormatting.GRAY + "将能量容量提升至 Lv.7",
                    "",
                    TextFormatting.YELLOW + "▶ 容量: 360,000 RF",
                    TextFormatting.LIGHT_PURPLE + "超量子存储"
            }, 7, 2
    );

    public static final ItemUpgradeComponent ENERGY_CAPACITY_LV8 = createUpgrade(
            UpgradeType.ENERGY_CAPACITY, "energy_capacity_lv8",
            new String[]{
                    TextFormatting.GOLD + "虚空电池 VIII",
                    TextFormatting.GRAY + "将能量容量提升至 Lv.8",
                    "",
                    TextFormatting.YELLOW + "▶ 容量: 410,000 RF",
                    TextFormatting.LIGHT_PURPLE + "虚空压缩技术"
            }, 8, 2
    );

    public static final ItemUpgradeComponent ENERGY_CAPACITY_LV9 = createUpgrade(
            UpgradeType.ENERGY_CAPACITY, "energy_capacity_lv9",
            new String[]{
                    TextFormatting.GOLD + "虚空电池 IX",
                    TextFormatting.GRAY + "将能量容量提升至 Lv.9",
                    "",
                    TextFormatting.YELLOW + "▶ 容量: 460,000 RF",
                    TextFormatting.DARK_PURPLE + "深渊级存储"
            }, 9, 1
    );

    public static final ItemUpgradeComponent ENERGY_CAPACITY_LV10 = createUpgrade(
            UpgradeType.ENERGY_CAPACITY, "energy_capacity_lv10",
            new String[]{
                    TextFormatting.GOLD + "✦ 无限电池 X ✦",
                    TextFormatting.GRAY + "将能量容量提升至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 容量: 510,000 RF",
                    TextFormatting.DARK_PURPLE + "传说级能量核心",
                    TextFormatting.RED + "已达最高等级"
            }, 10, 1
    );

    // ===== 能量效率升级（5级） =====
    public static final ItemUpgradeComponent ENERGY_EFFICIENCY_LV1 = createUpgrade(
            UpgradeType.ENERGY_EFFICIENCY, "energy_efficiency_lv1",
            new String[]{
                    TextFormatting.GREEN + "效率芯片 I",
                    TextFormatting.GRAY + "将能量效率提升至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 消耗减少: 15%",
                    TextFormatting.DARK_GRAY + "基础优化"
            }, 1, 16
    );

    public static final ItemUpgradeComponent ENERGY_EFFICIENCY_LV2 = createUpgrade(
            UpgradeType.ENERGY_EFFICIENCY, "energy_efficiency_lv2",
            new String[]{
                    TextFormatting.GREEN + "效率芯片 II",
                    TextFormatting.GRAY + "将能量效率提升至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 消耗减少: 30%",
                    TextFormatting.DARK_GRAY + "标准优化"
            }, 2, 8
    );

    public static final ItemUpgradeComponent ENERGY_EFFICIENCY_LV3 = createUpgrade(
            UpgradeType.ENERGY_EFFICIENCY, "energy_efficiency_lv3",
            new String[]{
                    TextFormatting.GREEN + "效率芯片 III",
                    TextFormatting.GRAY + "将能量效率提升至 Lv.3",
                    "",
                    TextFormatting.YELLOW + "▶ 消耗减少: 45%",
                    TextFormatting.BLUE + "高级优化"
            }, 3, 4
    );

    public static final ItemUpgradeComponent ENERGY_EFFICIENCY_LV4 = createUpgrade(
            UpgradeType.ENERGY_EFFICIENCY, "energy_efficiency_lv4",
            new String[]{
                    TextFormatting.GREEN + "量子芯片 IV",
                    TextFormatting.GRAY + "将能量效率提升至 Lv.4",
                    "",
                    TextFormatting.YELLOW + "▶ 消耗减少: 60%",
                    TextFormatting.LIGHT_PURPLE + "量子优化"
            }, 4, 2
    );

    public static final ItemUpgradeComponent ENERGY_EFFICIENCY_LV5 = createUpgrade(
            UpgradeType.ENERGY_EFFICIENCY, "energy_efficiency_lv5",
            new String[]{
                    TextFormatting.GREEN + "✦ 永动芯片 V ✦",
                    TextFormatting.GRAY + "将能量效率提升至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 消耗减少: 75%",
                    TextFormatting.DARK_PURPLE + "接近永动机",
                    TextFormatting.RED + "已达最高等级"
            }, 5, 1
    );

    // ===== 护甲强化升级（5级） =====
    public static final ItemUpgradeComponent ARMOR_ENHANCEMENT_LV1 = createUpgrade(
            UpgradeType.ARMOR_ENHANCEMENT, "armor_enhancement_lv1",
            new String[]{
                    TextFormatting.BLUE + "装甲板 I",
                    TextFormatting.GRAY + "将护甲强化至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 减伤: 6%",
                    TextFormatting.DARK_GRAY + "基础防护"
            }, 1, 16
    );

    public static final ItemUpgradeComponent ARMOR_ENHANCEMENT_LV2 = createUpgrade(
            UpgradeType.ARMOR_ENHANCEMENT, "armor_enhancement_lv2",
            new String[]{
                    TextFormatting.BLUE + "装甲板 II",
                    TextFormatting.GRAY + "将护甲强化至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 减伤: 10.6%",
                    TextFormatting.DARK_GRAY + "强化防护"
            }, 2, 8
    );

    public static final ItemUpgradeComponent ARMOR_ENHANCEMENT_LV3 = createUpgrade(
            UpgradeType.ARMOR_ENHANCEMENT, "armor_enhancement_lv3",
            new String[]{
                    TextFormatting.BLUE + "合金装甲 III",
                    TextFormatting.GRAY + "将护甲强化至 Lv.3",
                    "",
                    TextFormatting.YELLOW + "▶ 减伤: 14.1%",
                    TextFormatting.LIGHT_PURPLE + "▶ 解锁: 能量爆发",
                    TextFormatting.GRAY + "  15+伤害时触发"
            }, 3, 4
    );

    public static final ItemUpgradeComponent ARMOR_ENHANCEMENT_LV4 = createUpgrade(
            UpgradeType.ARMOR_ENHANCEMENT, "armor_enhancement_lv4",
            new String[]{
                    TextFormatting.BLUE + "纳米装甲 IV",
                    TextFormatting.GRAY + "将护甲强化至 Lv.4",
                    "",
                    TextFormatting.YELLOW + "▶ 减伤: 16.9%",
                    TextFormatting.GOLD + "▶ 解锁: 致命保护",
                    TextFormatting.GRAY + "  防止致命伤害(10分钟CD)"
            }, 4, 2
    );

    public static final ItemUpgradeComponent ARMOR_ENHANCEMENT_LV5 = createUpgrade(
            UpgradeType.ARMOR_ENHANCEMENT, "armor_enhancement_lv5",
            new String[]{
                    TextFormatting.BLUE + "✦ 虚空装甲 V ✦",
                    TextFormatting.GRAY + "将护甲强化至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 减伤: 19.2%",
                    TextFormatting.LIGHT_PURPLE + "▶ 解锁: 无敌护盾",
                    TextFormatting.GRAY + "  3秒无敌(60秒CD)",
                    TextFormatting.RED + "已达最高等级"
            }, 5, 1
    );

    // ===== 速度提升（3级） =====


    // ===== 飞行模块（保持原有的3个独立等级） =====
    public static final ItemUpgradeComponent FLIGHT_MODULE_BASIC = createUpgrade(
            UpgradeType.FLIGHT_MODULE, "flight_module_basic",
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
            }, 1, 1
    );

    public static final ItemUpgradeComponent FLIGHT_MODULE_ADVANCED = createUpgrade(
            UpgradeType.FLIGHT_MODULE, "flight_module_advanced",
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
                    TextFormatting.YELLOW + "⚠ 需要: 基础飞行模块 Lv.1"
            }, 2, 1
    );

    public static final ItemUpgradeComponent FLIGHT_MODULE_ULTIMATE = createUpgrade(
            UpgradeType.FLIGHT_MODULE, "flight_module_ultimate",
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
                    TextFormatting.DARK_GREEN + "按G键切换速度模式",
                    TextFormatting.YELLOW + "⚠ 需要: 高级飞行模块 Lv.2"
            }, 3, 1
    );

    // ===== 温度调节（5级） =====
    public static final ItemUpgradeComponent TEMPERATURE_CONTROL_LV1 = createUpgrade(
            UpgradeType.TEMPERATURE_CONTROL, "temperature_control_lv1",
            new String[]{
                    TextFormatting.DARK_AQUA + "温控系统 I",
                    TextFormatting.GRAY + "将温度调节提升至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 抗性: 轻度",
                    TextFormatting.DARK_GRAY + "基础温度调节"
            }, 1, 16
    );

    public static final ItemUpgradeComponent TEMPERATURE_CONTROL_LV2 = createUpgrade(
            UpgradeType.TEMPERATURE_CONTROL, "temperature_control_lv2",
            new String[]{
                    TextFormatting.DARK_AQUA + "温控系统 II",
                    TextFormatting.GRAY + "将温度调节提升至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 抗性: 中度",
                    TextFormatting.DARK_GRAY + "改良温度调节"
            }, 2, 8
    );

    public static final ItemUpgradeComponent TEMPERATURE_CONTROL_LV3 = createUpgrade(
            UpgradeType.TEMPERATURE_CONTROL, "temperature_control_lv3",
            new String[]{
                    TextFormatting.DARK_AQUA + "温控系统 III",
                    TextFormatting.GRAY + "将温度调节提升至 Lv.3",
                    "",
                    TextFormatting.YELLOW + "▶ 抗性: 高度",
                    TextFormatting.BLUE + "高级恒温"
            }, 3, 4
    );

    public static final ItemUpgradeComponent TEMPERATURE_CONTROL_LV4 = createUpgrade(
            UpgradeType.TEMPERATURE_CONTROL, "temperature_control_lv4",
            new String[]{
                    TextFormatting.DARK_AQUA + "恒温系统 IV",
                    TextFormatting.GRAY + "将温度调节提升至 Lv.4",
                    "",
                    TextFormatting.YELLOW + "▶ 抗性: 极高",
                    TextFormatting.LIGHT_PURPLE + "量子恒温"
            }, 4, 2
    );

    public static final ItemUpgradeComponent TEMPERATURE_CONTROL_LV5 = createUpgrade(
            UpgradeType.TEMPERATURE_CONTROL, "temperature_control_lv5",
            new String[]{
                    TextFormatting.DARK_AQUA + "✦ 绝对零度 V ✦",
                    TextFormatting.GRAY + "将温度调节提升至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 抗性: 完全免疫",
                    TextFormatting.DARK_PURPLE + "无视极端温度",
                    TextFormatting.RED + "已达最高等级"
            }, 5, 1
    );

    // ===== 特殊组合升级（保持原样，因为是套装） =====
    public static final ItemUpgradeComponent OMNIPOTENT_PACKAGE = createUpgrade(
            UpgradeType.OMNIPOTENT_PACKAGE, "omnipotent_package_chip",
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
            }, 1, 1
    );

    /**
     * 创建升级组件的辅助方法
     */
    private static ItemUpgradeComponent createUpgrade(UpgradeType type, String registryName,
                                                      String[] descriptions, int upgradeValue, int maxStackSize) {
        ItemUpgradeComponent upgrade = new ItemUpgradeComponent(type, descriptions, upgradeValue);

        upgrade.setRegistryName("moremod", registryName);
        upgrade.setTranslationKey(registryName);
        upgrade.setCreativeTab(moremodCreativeTab.moremod_TAB);
        upgrade.setMaxStackSize(maxStackSize);

        // 如果注册名包含_lv，设置NBT中的模块等级
        if (registryName.contains("_lv")) {
            String[] parts = registryName.split("_lv");
            if (parts.length > 1) {
                try {
                    int level = Integer.parseInt(parts[1]);
                    // 这里可以在物品创建时设置NBT标签
                    // upgrade.setModuleLevel(level);
                } catch (NumberFormatException ignored) {}
            }
        }

        return upgrade;
    }

    /**
     * 获取所有升级组件的数组（用于批量注册）
     */
    public static ItemUpgradeComponent[] getAllUpgrades() {
        return new ItemUpgradeComponent[]{
                // 能量容量（10级）
                ENERGY_CAPACITY_LV1, ENERGY_CAPACITY_LV2, ENERGY_CAPACITY_LV3,
                ENERGY_CAPACITY_LV4, ENERGY_CAPACITY_LV5, ENERGY_CAPACITY_LV6,
                ENERGY_CAPACITY_LV7, ENERGY_CAPACITY_LV8, ENERGY_CAPACITY_LV9,
                ENERGY_CAPACITY_LV10,

                // 能量效率（5级）
                ENERGY_EFFICIENCY_LV1, ENERGY_EFFICIENCY_LV2, ENERGY_EFFICIENCY_LV3,
                ENERGY_EFFICIENCY_LV4, ENERGY_EFFICIENCY_LV5,

                // 护甲强化（5级）
                ARMOR_ENHANCEMENT_LV1, ARMOR_ENHANCEMENT_LV2, ARMOR_ENHANCEMENT_LV3,
                ARMOR_ENHANCEMENT_LV4, ARMOR_ENHANCEMENT_LV5,

                // 速度提升（3级）

                // 飞行模块（3个独立等级）
                FLIGHT_MODULE_BASIC, FLIGHT_MODULE_ADVANCED, FLIGHT_MODULE_ULTIMATE,

                // 温度调节（5级）
                TEMPERATURE_CONTROL_LV1, TEMPERATURE_CONTROL_LV2, TEMPERATURE_CONTROL_LV3,
                TEMPERATURE_CONTROL_LV4, TEMPERATURE_CONTROL_LV5,

                // 特殊套装
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