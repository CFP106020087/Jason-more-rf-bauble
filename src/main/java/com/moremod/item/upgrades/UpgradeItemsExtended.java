package com.moremod.item.upgrades;

import com.moremod.item.UpgradeType;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.util.text.TextFormatting;
import com.moremod.item.upgrades.ItemUpgradeComponent;/**
 * 扩展升级组件定义类
 * 包含所有新的升级类型
 */
public class UpgradeItemsExtended {

    // ===== 生存类升级 =====

    public static final ItemUpgradeComponent YELLOW_SHIELD_MODULE = createUpgrade(
            UpgradeType.YELLOW_SHIELD,
            "yellow_shield_module",
            new String[]{
                    "黄条护盾发生器",
                    TextFormatting.YELLOW + "生成吸收伤害的能量护盾",
                    TextFormatting.GRAY + "护盾容量:",
                    TextFormatting.GRAY + "Lv.1: 4点 | Lv.2: 8点 | Lv.3: 12点",
                    TextFormatting.AQUA + "护盾会缓慢自动恢复",
                    TextFormatting.RED + "破碎后需要30秒冷却",
                    TextFormatting.DARK_GRAY + "不占用生命值上限"
            },
            1, 16
    );

    public static final ItemUpgradeComponent NANO_REPAIR_SYSTEM = createUpgrade(
            UpgradeType.HEALTH_REGEN,
            "nano_repair_system",
            new String[]{
                    "纳米修复系统",
                    TextFormatting.RED + "直接修复身体损伤",
                    TextFormatting.GRAY + "恢复速度:",
                    TextFormatting.GRAY + "Lv.1: 每4秒 0.5心",
                    TextFormatting.GRAY + "Lv.2: 每2秒 1.0心",
                    TextFormatting.GRAY + "Lv.3: 每1秒 1.5心",
                    TextFormatting.GREEN + "不依赖药水效果"
            },
            1, 16
    );

    public static final ItemUpgradeComponent METABOLIC_REGULATOR = createUpgrade(
            UpgradeType.HUNGER_THIRST,
            "metabolic_regulator",
            new String[]{
                    "代谢调节器",
                    TextFormatting.GREEN + "优化身体能量消耗",
                    TextFormatting.GRAY + "效果:",
                    TextFormatting.GRAY + "• 定期恢复饱食度",
                    TextFormatting.GRAY + "• 减缓饥饿消耗",
                    TextFormatting.GRAY + "• 提高食物效率",
                    TextFormatting.AQUA + "兼容口渴模组"
            },
            1, 16
    );

    public static final ItemUpgradeComponent REACTIVE_ARMOR = createUpgrade(
            UpgradeType.THORNS,
            "reactive_armor",
            new String[]{
                    "反应装甲",
                    TextFormatting.DARK_RED + "将受到的伤害反弹给攻击者",
                    TextFormatting.GRAY + "反伤比例:",
                    TextFormatting.GRAY + "Lv.1: 15% | Lv.2: 30% | Lv.3: 45%",
                    TextFormatting.YELLOW + "包含魔法伤害效果",
                    TextFormatting.DARK_GRAY + "不会反伤自己"
            },
            1, 16
    );

    public static final ItemUpgradeComponent FIRE_SUPPRESSION = createUpgrade(
            UpgradeType.FIRE_EXTINGUISH,
            "fire_suppression_system",
            new String[]{
                    "自动灭火系统",
                    TextFormatting.BLUE + "检测并扑灭身上的火焰",
                    TextFormatting.GRAY + "冷却时间:",
                    TextFormatting.GRAY + "Lv.1: 4秒 | Lv.2: 2秒 | Lv.3: 1秒",
                    TextFormatting.AQUA + "喷洒冷却剂扑灭火焰",
                    TextFormatting.DARK_GRAY + "对岩浆无效"
            },
            1, 16
    );

    // ===== 辅助类升级 =====
    public static final ItemUpgradeComponent WATERPROOF_MODULE = createUpgrade(
            UpgradeType.WATERPROOF_MODULE,
            "waterproof_module",
            new String[]{
                    "防水模块",
                    TextFormatting.AQUA + "保护核心免受水体损害",
                    TextFormatting.GRAY + "防护等级:",
                    TextFormatting.GRAY + "Lv.1: 基础防水涂层",
                    TextFormatting.GRAY + "Lv.2: +水下呼吸",
                    TextFormatting.GRAY + "Lv.3: +夜视 +游泳加速",
                    TextFormatting.YELLOW + "防止故障效果触发",
                    TextFormatting.DARK_GRAY + "最高3级"
            },
            1, 16
    );    public static final ItemUpgradeComponent ORE_SCANNER = createUpgrade(
            UpgradeType.ORE_VISION,
            "ore_scanner_module",
            new String[]{
                    "矿物扫描仪",
                    TextFormatting.GOLD + "透视显示附近矿物",
                    TextFormatting.GRAY + "扫描范围:",
                    TextFormatting.GRAY + "Lv.1: 8格 | Lv.2: 16格 | Lv.3: 24格",
                    TextFormatting.YELLOW + "按V键切换显示",
                    TextFormatting.GREEN + "支持模组矿物",
                    TextFormatting.DARK_GRAY + "消耗能量维持"
            },
            1, 16
    );

    public static final ItemUpgradeComponent SERVO_MOTORS = createUpgrade(
            UpgradeType.MOVEMENT_SPEED,
            "servo_motor_upgrade",
            new String[]{
                    "速度强化",
                    TextFormatting.AQUA + "增强腿部机械性能",
                    TextFormatting.GRAY + "速度提升:",
                    TextFormatting.GRAY + "Lv.1: +20% | Lv.2: +40% | Lv.3: +60%",
                    TextFormatting.GREEN + "直接修改移动属性",
                    TextFormatting.DARK_GRAY + "不使用药水效果"
            },
            1, 16
    );

    public static final ItemUpgradeComponent OPTICAL_CAMOUFLAGE = createUpgrade(
            UpgradeType.STEALTH,
            "optical_camouflage",
            new String[]{
                    "光学迷彩系统",
                    TextFormatting.DARK_GRAY + "降低生物的感知范围",
                    TextFormatting.GRAY + "效果:",
                    TextFormatting.GRAY + "• 半透明化",
                    TextFormatting.GRAY + "• 仇恨范围 -30%/-50%/-70%",
                    TextFormatting.YELLOW + "消耗: 40/30/20 RF/tick",
                    TextFormatting.RED + "攻击会暂时失效"
            },
            1, 16
    );

    public static final ItemUpgradeComponent EXP_COLLECTOR = createUpgrade(
            UpgradeType.EXP_AMPLIFIER,
            "exp_collection_matrix",
            new String[]{
                    "经验收集矩阵",
                    TextFormatting.GREEN + "增幅获得的击杀经验值",
                    TextFormatting.GRAY + "增幅倍率:",
                    TextFormatting.GRAY + "Lv.1: 1.5x | Lv.2: 2.0x | Lv.3: 2.5x",
                    TextFormatting.LIGHT_PURPLE + "额外效果:",
                    TextFormatting.GRAY + "• 短时间连续击杀增加获得经验量",
                    TextFormatting.GRAY + "• 附魔台等级 +5/+10/+15"
            },
            1, 16
    );

    // ===== 战斗类升级 =====

    public static final ItemUpgradeComponent STRENGTH_AMPLIFIER = createUpgrade(
            UpgradeType.DAMAGE_BOOST,
            "strength_amplifier",
            new String[]{
                    "力量增幅器",
                    TextFormatting.DARK_RED + "提升物理攻击伤害",
                    TextFormatting.GRAY + "伤害加成:",
                    TextFormatting.GRAY + "Lv.1-5: +25%/级",
                    TextFormatting.GOLD + "暴击几率: 10%/级",
                    TextFormatting.YELLOW + "暴击伤害: 2.0x",
                    TextFormatting.DARK_GRAY + "最高5级"
            },
            1, 16
    );

    public static final ItemUpgradeComponent REFLEX_ENHANCER = createUpgrade(
            UpgradeType.ATTACK_SPEED,
            "reflex_enhancement",
            new String[]{
                    "反应增强器",
                    TextFormatting.YELLOW + "提升攻击速度",
                    TextFormatting.GRAY + "攻速加成:",
                    TextFormatting.GRAY + "Lv.1: +20% | Lv.2: +40% | Lv.3: +60%",
                    TextFormatting.AQUA + "连击系统:",
                    TextFormatting.GRAY + "• 2秒内连续攻击减少消耗",
                    TextFormatting.GRAY + "• 显示连击提示"
            },
            1, 16
    );

    public static final ItemUpgradeComponent SWEEP_MODULE = createUpgrade(
            UpgradeType.RANGE_EXTENSION,
            "sweep_attack_module",
            new String[]{
                    "範圍拓展模块",
                    TextFormatting.BLUE + "範圍拓展",
                    TextFormatting.GRAY + "增長觸及距離:",
                    TextFormatting.GRAY + "Lv.1: 3格 | Lv.2: 6格 | Lv.3: 9格",

            },
            1, 16
    );

    public static final ItemUpgradeComponent PURSUIT_SYSTEM = createUpgrade(
            UpgradeType.PURSUIT,
            "pursuit_targeting_system",
            new String[]{
                    "追击定位系统",
                    TextFormatting.LIGHT_PURPLE + "标记并追击目标",
                    TextFormatting.GRAY + "效果:",
                    TextFormatting.GRAY + "• 攻击叠加追击层数",
                    TextFormatting.GRAY + "• 每层 +10% 伤害",
                    TextFormatting.GRAY + "• 最大层数: 2/4/6",
                    TextFormatting.YELLOW + "Lv.2+: 潜行时冲刺攻击",
                    TextFormatting.DARK_GRAY + "切换目标重置层数"
            },
            1, 16
    );

    // ===== 能源类升级 =====

    public static final ItemUpgradeComponent KINETIC_DYNAMO = createUpgrade(
            UpgradeType.KINETIC_GENERATOR,
            "kinetic_energy_dynamo",
            new String[]{
                    "动能发电机",
                    TextFormatting.GRAY + "将运动转化为能量",
                    TextFormatting.GRAY + "发电效率:",
                    TextFormatting.GRAY + "• 行走: 5/10/15 RF/格",
                    TextFormatting.GRAY + "• 疾跑: x1.5",
                    TextFormatting.GRAY + "• 飞行: x2.0",
                    TextFormatting.AQUA + "挖掘也能产生能量",
                    TextFormatting.DARK_GRAY + "基于方块硬度"
            },
            1, 16
    );

    public static final ItemUpgradeComponent SOLAR_PANEL = createUpgrade(
            UpgradeType.SOLAR_GENERATOR,
            "solar_collection_panel",
            new String[]{
                    "太阳能收集板",
                    TextFormatting.YELLOW + "吸收阳光产生能量",
                    TextFormatting.GRAY + "基础产能: 10/20/30 RF/秒",
                    TextFormatting.GRAY + "条件:",
                    TextFormatting.GRAY + "• 需要直接阳光",
                    TextFormatting.GRAY + "• 高度加成 (Y>100)",
                    TextFormatting.RED + "雨天-50% 雷暴-80%",
                    TextFormatting.DARK_GRAY + "夜晚无效"
            },
            1, 16
    );

    public static final ItemUpgradeComponent VOID_RESONATOR = createUpgrade(
            UpgradeType.VOID_ENERGY,
            "void_energy_resonator",
            new String[]{
                    "虚空共振器",
                    TextFormatting.DARK_PURPLE + "从虚空中汲取能量",
                    TextFormatting.GRAY + "充能地点:",
                    TextFormatting.GRAY + "• Y<30: 缓慢充能",
                    TextFormatting.GRAY + "• Y<15: 3倍速率",
                    TextFormatting.GRAY + "• 末地: 2倍速率",
                    TextFormatting.LIGHT_PURPLE + "每100充能 = 500 RF",
                    TextFormatting.DARK_GRAY + "神秘的能量来源"
            },
            1, 16
    );

    public static final ItemUpgradeComponent COMBAT_HARVESTER = createUpgrade(
            UpgradeType.COMBAT_CHARGER,
            "combat_energy_harvester",
            new String[]{
                    "战斗能量收割器",
                    TextFormatting.RED + "从战斗中获取能量",
                    TextFormatting.GRAY + "基础充能:",
                    TextFormatting.GRAY + "• 生物HP x5 x等级 RF",
                    TextFormatting.GRAY + "• 连杀加成: +10%/杀",
                    TextFormatting.GRAY + "• Boss: 10倍",
                    TextFormatting.GOLD + "掉落能量精华",
                    TextFormatting.DARK_GRAY + "最多3倍连杀加成"
            },
            1, 16
    );

    // ===== 特殊组合升级 =====

    public static final ItemUpgradeComponent SURVIVAL_PACKAGE = createUpgrade(
            UpgradeType.SURVIVAL_PACKAGE,  // 使用正确的套装类型
            "survival_enhancement_package",
            new String[]{
                    TextFormatting.GREEN + "✦ 生存强化套装 ✦",
                    TextFormatting.GOLD + "综合生存能力提升:",
                    TextFormatting.GRAY + "• 黄条护盾 +1级",
                    TextFormatting.GRAY + "• 生命恢复 +1级",
                    TextFormatting.GRAY + "• 饥饿管理 +1级",
                    TextFormatting.DARK_GREEN + "一次性提升多项生存能力",
                    TextFormatting.DARK_PURPLE + "稀有的综合升级"
            },
            1, 4
    );

    public static final ItemUpgradeComponent COMBAT_PACKAGE = createUpgrade(
            UpgradeType.COMBAT_PACKAGE,  // 使用正确的套装类型
            "combat_enhancement_package",
            new String[]{
                    TextFormatting.RED + "✦ 战斗强化套装 ✦",
                    TextFormatting.GOLD + "全面战斗能力提升:",
                    TextFormatting.GRAY + "• 伤害提升 +1级",
                    TextFormatting.GRAY + "• 攻击速度 +1级",
                    TextFormatting.GRAY + "• 范围拓展 +1级",
                    TextFormatting.DARK_RED + "成为战斗大师",
                    TextFormatting.DARK_PURPLE + "传说级战斗升级"
            },
            1, 4
    );

    /**
     * 创建升级组件的辅助方法 - 使用 UpgradeType 枚举
     */
    private static com.moremod.item.upgrades.ItemUpgradeComponent createUpgrade(UpgradeType type, String registryName,
                                                                                String[] descriptions, int upgradeValue, int maxStackSize) {
        ItemUpgradeComponent upgrade = new ItemUpgradeComponent(type, descriptions, upgradeValue);

        upgrade.setRegistryName("moremod", registryName);
        upgrade.setTranslationKey(registryName);
        upgrade.setCreativeTab(moremodCreativeTab.moremod_TAB);
        upgrade.setMaxStackSize(maxStackSize);

        return upgrade;
    }

    /**
     * 获取所有新升级组件
     */
    public static ItemUpgradeComponent[] getAllExtendedUpgrades() {
        return new ItemUpgradeComponent[]{
                // 生存类
                YELLOW_SHIELD_MODULE,
                NANO_REPAIR_SYSTEM,
                METABOLIC_REGULATOR,
                REACTIVE_ARMOR,
                FIRE_SUPPRESSION,

                // 辅助类
                ORE_SCANNER,
                SERVO_MOTORS,
                OPTICAL_CAMOUFLAGE,
                EXP_COLLECTOR,
                WATERPROOF_MODULE,
                // 战斗类
                STRENGTH_AMPLIFIER,
                REFLEX_ENHANCER,
                SWEEP_MODULE,
                PURSUIT_SYSTEM,

                // 能源类
                KINETIC_DYNAMO,
                SOLAR_PANEL,
                VOID_RESONATOR,
                COMBAT_HARVESTER,

                // 特殊套装
                SURVIVAL_PACKAGE,
                COMBAT_PACKAGE
        };
    }
}