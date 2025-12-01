package com.moremod.item.upgrades;

import com.moremod.item.UpgradeType;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.util.text.TextFormatting;
import com.moremod.item.upgrades.ItemUpgradeComponent;

/**
 * 扩展升级组件定义类 - 分级模块系统
 * 包含所有新的升级类型的分级版本
 *
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                        模块注册完整链路说明                                    ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║                                                                              ║
 * ║  1. ItemMechanicalCoreExtended.registerUpgrades()                            ║
 * ║     └─ 注册模块元数据 (ID, 显示名, 颜色, 最大等级, 类别)                        ║
 * ║     └─ 位置: ItemMechanicalCoreExtended.java                                 ║
 * ║                                                                              ║
 * ║  2. UpgradeType 枚举                                                         ║
 * ║     └─ 添加枚举值 (用于物品创建和类型识别)                                      ║
 * ║     └─ 位置: com.moremod.item.UpgradeType                                    ║
 * ║                                                                              ║
 * ║  3. UpgradeItemsExtended (本类)                                              ║
 * ║     └─ 定义物品实例 (使用 createUpgrade 方法)                                  ║
 * ║     └─ 添加到 getAllExtendedUpgrades() 返回数组                               ║
 * ║                                                                              ║
 * ║  4. RegisterItem.java                                                        ║
 * ║     └─ 从本类获取物品引用并注册到 Forge Registry                               ║
 * ║     └─ 调用 event.getRegistry().register(item)                               ║
 * ║                                                                              ║
 * ║  5. ItemMechanicalCore.EXTENDED_UPGRADE_IDS                                  ║
 * ║     └─ 添加模块ID字符串 (用于tooltip激活/已安装计数)                            ║
 * ║     └─ 位置: ItemMechanicalCore.java 顶部                                    ║
 * ║                                                                              ║
 * ║  6. 语言文件 (en_us.lang / zh_cn.lang)                                       ║
 * ║     └─ 添加 item.registryName.name=显示名称                                   ║
 * ║     └─ 位置: resources/assets/moremod/lang/                                  ║
 * ║                                                                              ║
 * ║  7. 效果实现                                                                  ║
 * ║     └─ 事件处理器 / Mixin / Tick处理                                          ║
 * ║     └─ 检查 isUpgradeActive() 并实现具体效果                                   ║
 * ║                                                                              ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
public class UpgradeItemsExtended {

    // ===== 生存类升级 =====

    // 黄条护盾（3级）
    public static final com.moremod.item.upgrades.ItemUpgradeComponent YELLOW_SHIELD_LV1 = createUpgrade(
            UpgradeType.YELLOW_SHIELD, "yellow_shield_lv1",
            new String[]{
                    TextFormatting.YELLOW + "护盾发生器 I",
                    TextFormatting.GRAY + "将护盾系统升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 护盾容量: 7点（3.5心）",
                    TextFormatting.AQUA + "▶ 自动恢复: 0.5点/秒",
                    TextFormatting.RED + "破碎冷却: 30秒",
                    TextFormatting.DARK_GRAY + "基础能量护盾"
            }, 1, 16
    );

    public static final com.moremod.item.upgrades.ItemUpgradeComponent YELLOW_SHIELD_LV2 = createUpgrade(
            UpgradeType.YELLOW_SHIELD, "yellow_shield_lv2",
            new String[]{
                    TextFormatting.YELLOW + "护盾发生器 II",
                    TextFormatting.GRAY + "将护盾系统升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 护盾容量: 14点（7心）",
                    TextFormatting.AQUA + "▶ 恢复速度提升",
                    TextFormatting.BLUE + "强化护盾矩阵"
            }, 2, 8
    );

    public static final ItemUpgradeComponent YELLOW_SHIELD_LV3 = createUpgrade(
            UpgradeType.YELLOW_SHIELD, "yellow_shield_lv3",
            new String[]{
                    TextFormatting.YELLOW + "✦ 护盾发生器 III ✦",
                    TextFormatting.GRAY + "将护盾系统升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 护盾容量: 21点（10.5心）",
                    TextFormatting.LIGHT_PURPLE + "量子护盾技术",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // 生命恢复（3级）
    public static final ItemUpgradeComponent HEALTH_REGEN_LV1 = createUpgrade(
            UpgradeType.HEALTH_REGEN, "health_regen_lv1",
            new String[]{
                    TextFormatting.RED + "纳米修复 I",
                    TextFormatting.GRAY + "将生命恢复升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 恢复: 每3秒 0.5心",
                    TextFormatting.DARK_GRAY + "基础纳米修复"
            }, 1, 16
    );

    public static final ItemUpgradeComponent HEALTH_REGEN_LV2 = createUpgrade(
            UpgradeType.HEALTH_REGEN, "health_regen_lv2",
            new String[]{
                    TextFormatting.RED + "纳米修复 II",
                    TextFormatting.GRAY + "将生命恢复升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 恢复: 每2秒 1.0心",
                    TextFormatting.BLUE + "高速修复系统"
            }, 2, 8
    );

    public static final ItemUpgradeComponent HEALTH_REGEN_LV3 = createUpgrade(
            UpgradeType.HEALTH_REGEN, "health_regen_lv3",
            new String[]{
                    TextFormatting.RED + "✦ 纳米修复 III ✦",
                    TextFormatting.GRAY + "将生命恢复升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 恢复: 每1秒 1.5心",
                    TextFormatting.LIGHT_PURPLE + "瞬间再生技术",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // 饥饿管理（3级）- 强化补水版本
    public static final ItemUpgradeComponent HUNGER_THIRST_LV1 = createUpgrade(
            UpgradeType.HUNGER_THIRST, "hunger_thirst_lv1",
            new String[]{
                    TextFormatting.GREEN + "代谢调节 I",
                    TextFormatting.GRAY + "将代谢系统升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 饱食: 每2分钟 +1",
                    TextFormatting.AQUA + "▶ 水分: 维持18点以上",
                    TextFormatting.GRAY + "• 补水间隔: 3秒",
                    TextFormatting.GRAY + "• 智能补水: 2-5点",
                    TextFormatting.DARK_GRAY + "基础代谢优化"
            }, 1, 16
    );

    public static final ItemUpgradeComponent HUNGER_THIRST_LV2 = createUpgrade(
            UpgradeType.HUNGER_THIRST, "hunger_thirst_lv2",
            new String[]{
                    TextFormatting.GREEN + "代谢调节 II",
                    TextFormatting.GRAY + "将代谢系统升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 饱食: 每80秒 +2",
                    TextFormatting.AQUA + "▶ 水分: 维持19点以上",
                    TextFormatting.BLUE + "• 补水间隔: 2秒",
                    TextFormatting.BLUE + "• 减缓口渴速度",
                    TextFormatting.BLUE + "高效代谢系统"
            }, 2, 8
    );

    public static final ItemUpgradeComponent HUNGER_THIRST_LV3 = createUpgrade(
            UpgradeType.HUNGER_THIRST, "hunger_thirst_lv3",
            new String[]{
                    TextFormatting.GREEN + "✦ 代谢调节 III ✦",
                    TextFormatting.GRAY + "将代谢系统升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 饱食: 每40秒 +3",
                    TextFormatting.DARK_AQUA + "▶ 水分: 始终满值20",
                    TextFormatting.DARK_AQUA + "• 完全免疫口渴",
                    TextFormatting.DARK_AQUA + "• 炎热环境保护",
                    TextFormatting.LIGHT_PURPLE + "完美代谢控制",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // 反伤荆棘（3级）
    public static final ItemUpgradeComponent THORNS_LV1 = createUpgrade(
            UpgradeType.THORNS, "thorns_lv1",
            new String[]{
                    TextFormatting.DARK_RED + "反应装甲 I",
                    TextFormatting.GRAY + "将反伤系统升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 反伤: 15%",
                    TextFormatting.DARK_GRAY + "基础反击系统"
            }, 1, 16
    );

    public static final ItemUpgradeComponent THORNS_LV2 = createUpgrade(
            UpgradeType.THORNS, "thorns_lv2",
            new String[]{
                    TextFormatting.DARK_RED + "反应装甲 II",
                    TextFormatting.GRAY + "将反伤系统升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 反伤: 30%",
                    TextFormatting.BLUE + "改良反击系统"
            }, 2, 8
    );

    public static final ItemUpgradeComponent THORNS_LV3 = createUpgrade(
            UpgradeType.THORNS, "thorns_lv3",
            new String[]{
                    TextFormatting.DARK_RED + "✦ 反应装甲 III ✦",
                    TextFormatting.GRAY + "将反伤系统升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 反伤: 45%",
                    TextFormatting.LIGHT_PURPLE + "镜像反击",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // 自动灭火（3级）
    public static final ItemUpgradeComponent FIRE_EXTINGUISH_LV1 = createUpgrade(
            UpgradeType.FIRE_EXTINGUISH, "fire_extinguish_lv1",
            new String[]{
                    TextFormatting.BLUE + "灭火系统 I",
                    TextFormatting.GRAY + "将灭火系统升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 冷却: 4秒",
                    TextFormatting.DARK_GRAY + "基础灭火装置"
            }, 1, 16
    );

    public static final ItemUpgradeComponent FIRE_EXTINGUISH_LV2 = createUpgrade(
            UpgradeType.FIRE_EXTINGUISH, "fire_extinguish_lv2",
            new String[]{
                    TextFormatting.BLUE + "灭火系统 II",
                    TextFormatting.GRAY + "将灭火系统升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 冷却: 2秒",
                    TextFormatting.BLUE + "快速灭火系统"
            }, 2, 8
    );

    public static final ItemUpgradeComponent FIRE_EXTINGUISH_LV3 = createUpgrade(
            UpgradeType.FIRE_EXTINGUISH, "fire_extinguish_lv3",
            new String[]{
                    TextFormatting.BLUE + "✦ 灭火系统 III ✦",
                    TextFormatting.GRAY + "将灭火系统升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 冷却: 1秒",
                    TextFormatting.LIGHT_PURPLE + "瞬间灭火",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // ===== 辅助类升级 =====

    // 防水模块（特殊3级）- 更新版本
    public static final ItemUpgradeComponent WATERPROOF_MODULE_BASIC = createUpgrade(
            UpgradeType.WATERPROOF_MODULE, "waterproof_module_basic",
            new String[]{
                    TextFormatting.AQUA + "防水涂层 I",
                    TextFormatting.GRAY + "基础防水保护",
                    "",
                    TextFormatting.GREEN + "▶ 免疫: 浅水接触",
                    TextFormatting.GRAY + "✓ 脚踩水无损害",
                    TextFormatting.RED + "✗ 不防雨水",
                    TextFormatting.RED + "✗ 不防深水浸没",
                    TextFormatting.DARK_GRAY + "纳米防水涂层"
            }, 1, 16
    );

    public static final ItemUpgradeComponent WATERPROOF_MODULE_ADVANCED = createUpgrade(
            UpgradeType.WATERPROOF_MODULE, "waterproof_module_advanced",
            new String[]{
                    TextFormatting.AQUA + "防水系统 II",
                    TextFormatting.GRAY + "高级防水保护",
                    "",
                    TextFormatting.GREEN + "▶ 免疫: 浅水+雨水+腰部",
                    TextFormatting.GRAY + "✓ 可涉水过河",
                    TextFormatting.GRAY + "✓ 雨天作业无损",
                    TextFormatting.RED + "✗ 不防头部浸没",
                    TextFormatting.DARK_GRAY + "需要: 防水涂层 I"
            }, 2, 8
    );

    public static final ItemUpgradeComponent WATERPROOF_MODULE_DEEP_SEA = createUpgrade(
            UpgradeType.WATERPROOF_MODULE, "waterproof_module_deep_sea",
            new String[]{
                    TextFormatting.DARK_AQUA + "✦ 深海适应 III ✦",
                    TextFormatting.GRAY + "完整水下作业系统",
                    "",
                    TextFormatting.DARK_AQUA + "▶ 免疫: 所有水体",
                    TextFormatting.GREEN + "✓ 完全防水保护",
                    TextFormatting.BLUE + "• 水下呼吸",
                    TextFormatting.LIGHT_PURPLE + "• 夜视+急迫II+速度II",
                    TextFormatting.DARK_GRAY + "需要: 防水系统 II"
            }, 3, 4
    );

    // 矿物透视（3级）
    public static final ItemUpgradeComponent ORE_VISION_LV1 = createUpgrade(
            UpgradeType.ORE_VISION, "ore_vision_lv1",
            new String[]{
                    TextFormatting.GOLD + "矿物扫描 I",
                    TextFormatting.GRAY + "将扫描系统升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 范围: 8格",
                    TextFormatting.DARK_GRAY + "基础扫描仪"
            }, 1, 16
    );

    public static final ItemUpgradeComponent ORE_VISION_LV2 = createUpgrade(
            UpgradeType.ORE_VISION, "ore_vision_lv2",
            new String[]{
                    TextFormatting.GOLD + "矿物扫描 II",
                    TextFormatting.GRAY + "将扫描系统升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 范围: 16格",
                    TextFormatting.BLUE + "改良扫描仪"
            }, 2, 8
    );

    public static final ItemUpgradeComponent ORE_VISION_LV3 = createUpgrade(
            UpgradeType.ORE_VISION, "ore_vision_lv3",
            new String[]{
                    TextFormatting.GOLD + "✦ 矿物扫描 III ✦",
                    TextFormatting.GRAY + "将扫描系统升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 范围: 24格",
                    TextFormatting.GREEN + "支持模组矿物",
                    TextFormatting.LIGHT_PURPLE + "量子扫描",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // 移动速度（3级）
    public static final ItemUpgradeComponent MOVEMENT_SPEED_LV1 = createUpgrade(
            UpgradeType.MOVEMENT_SPEED, "movement_speed_lv1",
            new String[]{
                    TextFormatting.AQUA + "伺服马达 I",
                    TextFormatting.GRAY + "将移动系统升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 速度: +20%",
                    TextFormatting.DARK_GRAY + "基础动力增强"
            }, 1, 16
    );

    public static final ItemUpgradeComponent MOVEMENT_SPEED_LV2 = createUpgrade(
            UpgradeType.MOVEMENT_SPEED, "movement_speed_lv2",
            new String[]{
                    TextFormatting.AQUA + "伺服马达 II",
                    TextFormatting.GRAY + "将移动系统升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 速度: +40%",
                    TextFormatting.BLUE + "高速移动系统"
            }, 2, 8
    );

    public static final ItemUpgradeComponent MOVEMENT_SPEED_LV3 = createUpgrade(
            UpgradeType.MOVEMENT_SPEED, "movement_speed_lv3",
            new String[]{
                    TextFormatting.AQUA + "✦ 伺服马达 III ✦",
                    TextFormatting.GRAY + "将移动系统升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 速度: +60%",
                    TextFormatting.LIGHT_PURPLE + "超音速移动",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // 隐身潜行（3级）
    public static final ItemUpgradeComponent STEALTH_LV1 = createUpgrade(
            UpgradeType.STEALTH, "stealth_lv1",
            new String[]{
                    TextFormatting.DARK_GRAY + "光学迷彩 I",
                    TextFormatting.GRAY + "将隐身系统升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 仇恨范围: -30%",
                    TextFormatting.RED + "能耗: 40 RF/tick",
                    TextFormatting.DARK_GRAY + "基础光学伪装"
            }, 1, 16
    );

    public static final ItemUpgradeComponent STEALTH_LV2 = createUpgrade(
            UpgradeType.STEALTH, "stealth_lv2",
            new String[]{
                    TextFormatting.DARK_GRAY + "光学迷彩 II",
                    TextFormatting.GRAY + "将隐身系统升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 仇恨范围: -50%",
                    TextFormatting.RED + "能耗: 30 RF/tick",
                    TextFormatting.BLUE + "改良光学伪装"
            }, 2, 8
    );

    public static final ItemUpgradeComponent STEALTH_LV3 = createUpgrade(
            UpgradeType.STEALTH, "stealth_lv3",
            new String[]{
                    TextFormatting.DARK_GRAY + "✦ 光学迷彩 III ✦",
                    TextFormatting.GRAY + "将隐身系统升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 仇恨范围: -70%",
                    TextFormatting.RED + "能耗: 20 RF/tick",
                    TextFormatting.LIGHT_PURPLE + "完美隐身",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // 经验增幅（3级）
    public static final ItemUpgradeComponent EXP_AMPLIFIER_LV1 = createUpgrade(
            UpgradeType.EXP_AMPLIFIER, "exp_amplifier_lv1",
            new String[]{
                    TextFormatting.GREEN + "经验矩阵 I",
                    TextFormatting.GRAY + "将经验系统升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 经验: 1.5倍",
                    TextFormatting.LIGHT_PURPLE + "▶ 附魔台: +5级",
                    TextFormatting.DARK_GRAY + "基础经验增幅"
            }, 1, 16
    );

    public static final ItemUpgradeComponent EXP_AMPLIFIER_LV2 = createUpgrade(
            UpgradeType.EXP_AMPLIFIER, "exp_amplifier_lv2",
            new String[]{
                    TextFormatting.GREEN + "经验矩阵 II",
                    TextFormatting.GRAY + "将经验系统升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 经验: 2.0倍",
                    TextFormatting.LIGHT_PURPLE + "▶ 附魔台: +10级",
                    TextFormatting.BLUE + "高效经验收集"
            }, 2, 8
    );

    public static final ItemUpgradeComponent EXP_AMPLIFIER_LV3 = createUpgrade(
            UpgradeType.EXP_AMPLIFIER, "exp_amplifier_lv3",
            new String[]{
                    TextFormatting.GREEN + "✦ 经验矩阵 III ✦",
                    TextFormatting.GRAY + "将经验系统升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 经验: 2.5倍",
                    TextFormatting.LIGHT_PURPLE + "▶ 附魔台: +15级",
                    TextFormatting.DARK_PURPLE + "经验虹吸",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // ===== 战斗类升级（5级） =====

    // 伤害提升
    public static final ItemUpgradeComponent DAMAGE_BOOST_LV1 = createUpgrade(
            UpgradeType.DAMAGE_BOOST, "damage_boost_lv1",
            new String[]{
                    TextFormatting.DARK_RED + "力量增幅 I",
                    TextFormatting.GRAY + "将攻击力提升至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 伤害: +25%",
                    TextFormatting.GOLD + "▶ 暴击率: 10%",
                    TextFormatting.DARK_GRAY + "基础力量提升"
            }, 1, 16
    );

    public static final ItemUpgradeComponent DAMAGE_BOOST_LV2 = createUpgrade(
            UpgradeType.DAMAGE_BOOST, "damage_boost_lv2",
            new String[]{
                    TextFormatting.DARK_RED + "力量增幅 II",
                    TextFormatting.GRAY + "将攻击力提升至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 伤害: +50%",
                    TextFormatting.GOLD + "▶ 暴击率: 20%"
            }, 2, 8
    );

    public static final ItemUpgradeComponent DAMAGE_BOOST_LV3 = createUpgrade(
            UpgradeType.DAMAGE_BOOST, "damage_boost_lv3",
            new String[]{
                    TextFormatting.DARK_RED + "力量增幅 III",
                    TextFormatting.GRAY + "将攻击力提升至 Lv.3",
                    "",
                    TextFormatting.YELLOW + "▶ 伤害: +75%",
                    TextFormatting.GOLD + "▶ 暴击率: 30%",
                    TextFormatting.BLUE + "高级力量增幅"
            }, 3, 4
    );

    public static final ItemUpgradeComponent DAMAGE_BOOST_LV4 = createUpgrade(
            UpgradeType.DAMAGE_BOOST, "damage_boost_lv4",
            new String[]{
                    TextFormatting.DARK_RED + "力量增幅 IV",
                    TextFormatting.GRAY + "将攻击力提升至 Lv.4",
                    "",
                    TextFormatting.YELLOW + "▶ 伤害: +100%",
                    TextFormatting.GOLD + "▶ 暴击率: 40%",
                    TextFormatting.LIGHT_PURPLE + "超人力量"
            }, 4, 2
    );

    public static final ItemUpgradeComponent DAMAGE_BOOST_LV5 = createUpgrade(
            UpgradeType.DAMAGE_BOOST, "damage_boost_lv5",
            new String[]{
                    TextFormatting.DARK_RED + "✦ 毁灭之力 V ✦",
                    TextFormatting.GRAY + "将攻击力提升至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 伤害: +125%",
                    TextFormatting.GOLD + "▶ 暴击率: 50%",
                    TextFormatting.DARK_PURPLE + "毁灭一切",
                    TextFormatting.RED + "已达最高等级"
            }, 5, 1
    );

    // 攻击速度（3级）
    public static final ItemUpgradeComponent ATTACK_SPEED_LV1 = createUpgrade(
            UpgradeType.ATTACK_SPEED, "attack_speed_lv1",
            new String[]{
                    TextFormatting.YELLOW + "反应增强 I",
                    TextFormatting.GRAY + "将攻速提升至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 攻速: +20%",
                    TextFormatting.DARK_GRAY + "基础反应提升"
            }, 1, 16
    );

    public static final ItemUpgradeComponent ATTACK_SPEED_LV2 = createUpgrade(
            UpgradeType.ATTACK_SPEED, "attack_speed_lv2",
            new String[]{
                    TextFormatting.YELLOW + "反应增强 II",
                    TextFormatting.GRAY + "将攻速提升至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 攻速: +40%",
                    TextFormatting.BLUE + "连击系统激活"
            }, 2, 8
    );

    public static final ItemUpgradeComponent ATTACK_SPEED_LV3 = createUpgrade(
            UpgradeType.ATTACK_SPEED, "attack_speed_lv3",
            new String[]{
                    TextFormatting.YELLOW + "✦ 闪电反射 III ✦",
                    TextFormatting.GRAY + "将攻速提升至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 攻速: +60%",
                    TextFormatting.LIGHT_PURPLE + "超高速连击",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // 范围拓展（3级）
    public static final ItemUpgradeComponent RANGE_EXTENSION_LV1 = createUpgrade(
            UpgradeType.RANGE_EXTENSION, "range_extension_lv1",
            new String[]{
                    TextFormatting.BLUE + "范围模块 I",
                    TextFormatting.GRAY + "将攻击范围提升至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 范围: +3格",
                    TextFormatting.DARK_GRAY + "基础范围扩展"
            }, 1, 16
    );

    public static final ItemUpgradeComponent RANGE_EXTENSION_LV2 = createUpgrade(
            UpgradeType.RANGE_EXTENSION, "range_extension_lv2",
            new String[]{
                    TextFormatting.BLUE + "范围模块 II",
                    TextFormatting.GRAY + "将攻击范围提升至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 范围: +6格",
                    TextFormatting.BLUE + "远程打击"
            }, 2, 8
    );

    public static final ItemUpgradeComponent RANGE_EXTENSION_LV3 = createUpgrade(
            UpgradeType.RANGE_EXTENSION, "range_extension_lv3",
            new String[]{
                    TextFormatting.BLUE + "✦ 范围模块 III ✦",
                    TextFormatting.GRAY + "将攻击范围提升至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 范围: +9格",
                    TextFormatting.LIGHT_PURPLE + "超远程打击",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // 追击系统（3级）
    public static final ItemUpgradeComponent PURSUIT_LV1 = createUpgrade(
            UpgradeType.PURSUIT, "pursuit_lv1",
            new String[]{
                    TextFormatting.LIGHT_PURPLE + "追击系统 I",
                    TextFormatting.GRAY + "将追击系统升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 最大层数: 2",
                    TextFormatting.GRAY + "每层 +10% 伤害",
                    TextFormatting.DARK_GRAY + "基础追击标记"
            }, 1, 16
    );

    public static final ItemUpgradeComponent PURSUIT_LV2 = createUpgrade(
            UpgradeType.PURSUIT, "pursuit_lv2",
            new String[]{
                    TextFormatting.LIGHT_PURPLE + "追击系统 II",
                    TextFormatting.GRAY + "将追击系统升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 最大层数: 4",
                    TextFormatting.GREEN + "▶ 解锁: 潜行冲刺",
                    TextFormatting.BLUE + "改良追击"
            }, 2, 8
    );

    public static final ItemUpgradeComponent PURSUIT_LV3 = createUpgrade(
            UpgradeType.PURSUIT, "pursuit_lv3",
            new String[]{
                    TextFormatting.LIGHT_PURPLE + "✦ 追击系统 III ✦",
                    TextFormatting.GRAY + "将追击系统升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 最大层数: 6",
                    TextFormatting.DARK_PURPLE + "必杀追击",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // 魔力吸收（3级）
    public static final ItemUpgradeComponent MAGIC_ABSORB_LV1 = createUpgrade(
            UpgradeType.MAGIC_ABSORB, "magic_absorb_lv1",
            new String[]{
                    TextFormatting.DARK_PURPLE + "魔力吸收 I",
                    TextFormatting.GRAY + "将魔力吸收升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 吸收少量法伤",
                    TextFormatting.GRAY + "并转化为物理力量",
                    TextFormatting.RED + "叠加少量余灼",
                    TextFormatting.DARK_GRAY + "基础魔力转换"
            }, 1, 16
    );

    public static final ItemUpgradeComponent MAGIC_ABSORB_LV2 = createUpgrade(
            UpgradeType.MAGIC_ABSORB, "magic_absorb_lv2",
            new String[]{
                    TextFormatting.DARK_PURPLE + "魔力吸收 II",
                    TextFormatting.GRAY + "将魔力吸收升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 更高法伤吸收率",
                    TextFormatting.GOLD + "▶ 更快余灼累积",
                    TextFormatting.BLUE + "强化魔力转换"
            }, 2, 8
    );

    public static final ItemUpgradeComponent MAGIC_ABSORB_LV3 = createUpgrade(
            UpgradeType.MAGIC_ABSORB, "magic_absorb_lv3",
            new String[]{
                    TextFormatting.DARK_PURPLE + "✦ 魔力吸收 III ✦",
                    TextFormatting.GRAY + "将魔力吸收升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 强化吸收倍率",
                    TextFormatting.RED + "▶ 余灼满载触发『魔力爆心』",
                    TextFormatting.LIGHT_PURPLE + "造成一次强力爆发伤害",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // ===== 能源类升级（3级） =====

    // 动能发电
    public static final ItemUpgradeComponent KINETIC_GENERATOR_LV1 = createUpgrade(
            UpgradeType.KINETIC_GENERATOR, "kinetic_generator_lv1",
            new String[]{
                    TextFormatting.GRAY + "动能发电 I",
                    TextFormatting.GRAY + "将动能系统升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 行走: 5 RF/格",
                    TextFormatting.GRAY + "疾跑×1.5 飞行×2.0",
                    TextFormatting.DARK_GRAY + "基础动能转换"
            }, 1, 16
    );

    public static final ItemUpgradeComponent KINETIC_GENERATOR_LV2 = createUpgrade(
            UpgradeType.KINETIC_GENERATOR, "kinetic_generator_lv2",
            new String[]{
                    TextFormatting.GRAY + "动能发电 II",
                    TextFormatting.GRAY + "将动能系统升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 行走: 10 RF/格",
                    TextFormatting.BLUE + "高效动能转换"
            }, 2, 8
    );

    public static final ItemUpgradeComponent KINETIC_GENERATOR_LV3 = createUpgrade(
            UpgradeType.KINETIC_GENERATOR, "kinetic_generator_lv3",
            new String[]{
                    TextFormatting.GRAY + "✦ 动能发电 III ✦",
                    TextFormatting.GRAY + "将动能系统升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 行走: 15 RF/格",
                    TextFormatting.AQUA + "挖掘也产生能量",
                    TextFormatting.LIGHT_PURPLE + "永动机",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // 太阳能发电
    public static final ItemUpgradeComponent SOLAR_GENERATOR_LV1 = createUpgrade(
            UpgradeType.SOLAR_GENERATOR, "solar_generator_lv1",
            new String[]{
                    TextFormatting.YELLOW + "太阳能板 I",
                    TextFormatting.GRAY + "将太阳能系统升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 产能: 10 RF/秒",
                    TextFormatting.GRAY + "需要直接阳光",
                    TextFormatting.DARK_GRAY + "基础光能转换"
            }, 1, 16
    );

    public static final ItemUpgradeComponent SOLAR_GENERATOR_LV2 = createUpgrade(
            UpgradeType.SOLAR_GENERATOR, "solar_generator_lv2",
            new String[]{
                    TextFormatting.YELLOW + "太阳能板 II",
                    TextFormatting.GRAY + "将太阳能系统升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 产能: 20 RF/秒",
                    TextFormatting.BLUE + "高效光能转换"
            }, 2, 8
    );

    public static final ItemUpgradeComponent SOLAR_GENERATOR_LV3 = createUpgrade(
            UpgradeType.SOLAR_GENERATOR, "solar_generator_lv3",
            new String[]{
                    TextFormatting.YELLOW + "✦ 太阳能板 III ✦",
                    TextFormatting.GRAY + "将太阳能系统升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 产能: 30 RF/秒",
                    TextFormatting.GREEN + "高度加成(Y>100)",
                    TextFormatting.LIGHT_PURPLE + "量子光能",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // 虚空能量
    public static final ItemUpgradeComponent VOID_ENERGY_LV1 = createUpgrade(
            UpgradeType.VOID_ENERGY, "void_energy_lv1",
            new String[]{
                    TextFormatting.DARK_PURPLE + "虚空共振 I",
                    TextFormatting.GRAY + "将虚空系统升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ Y<30: 缓慢充能",
                    TextFormatting.LIGHT_PURPLE + "100充能 = 500 RF",
                    TextFormatting.DARK_GRAY + "基础虚空汲取"
            }, 1, 16
    );

    public static final ItemUpgradeComponent VOID_ENERGY_LV2 = createUpgrade(
            UpgradeType.VOID_ENERGY, "void_energy_lv2",
            new String[]{
                    TextFormatting.DARK_PURPLE + "虚空共振 II",
                    TextFormatting.GRAY + "将虚空系统升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ Y<15: 3倍速率",
                    TextFormatting.BLUE + "改良虚空汲取"
            }, 2, 8
    );

    public static final ItemUpgradeComponent VOID_ENERGY_LV3 = createUpgrade(
            UpgradeType.VOID_ENERGY, "void_energy_lv3",
            new String[]{
                    TextFormatting.DARK_PURPLE + "✦ 虚空共振 III ✦",
                    TextFormatting.GRAY + "将虚空系统升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 末地: 2倍速率",
                    TextFormatting.LIGHT_PURPLE + "深渊能量",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // 战斗充能
    public static final ItemUpgradeComponent COMBAT_CHARGER_LV1 = createUpgrade(
            UpgradeType.COMBAT_CHARGER, "combat_charger_lv1",
            new String[]{
                    TextFormatting.RED + "战斗收割 I",
                    TextFormatting.GRAY + "将战斗充能升级至 Lv.1",
                    "",
                    TextFormatting.YELLOW + "▶ 充能: HP×5 RF",
                    TextFormatting.DARK_GRAY + "基础生命汲取"
            }, 1, 16
    );

    public static final ItemUpgradeComponent COMBAT_CHARGER_LV2 = createUpgrade(
            UpgradeType.COMBAT_CHARGER, "combat_charger_lv2",
            new String[]{
                    TextFormatting.RED + "战斗收割 II",
                    TextFormatting.GRAY + "将战斗充能升级至 Lv.2",
                    "",
                    TextFormatting.YELLOW + "▶ 充能: HP×10 RF",
                    TextFormatting.GRAY + "连杀加成: +10%/杀",
                    TextFormatting.BLUE + "高效生命汲取"
            }, 2, 8
    );

    public static final ItemUpgradeComponent COMBAT_CHARGER_LV3 = createUpgrade(
            UpgradeType.COMBAT_CHARGER, "combat_charger_lv3",
            new String[]{
                    TextFormatting.RED + "✦ 战斗收割 III ✦",
                    TextFormatting.GRAY + "将战斗充能升级至最高等级",
                    "",
                    TextFormatting.YELLOW + "▶ 充能: HP×15 RF",
                    TextFormatting.GOLD + "▶ Boss: 10倍",
                    TextFormatting.LIGHT_PURPLE + "灵魂收割",
                    TextFormatting.RED + "已达最高等级"
            }, 3, 4
    );

    // ===== 特殊组合升级（保持原样） =====
    public static final ItemUpgradeComponent SURVIVAL_PACKAGE = createUpgrade(
            UpgradeType.SURVIVAL_PACKAGE, "survival_enhancement_package",
            new String[]{
                    TextFormatting.GREEN + "✦ 生存强化套装 ✦",
                    TextFormatting.GOLD + "综合生存能力提升:",
                    TextFormatting.GRAY + "• 黄条护盾 +1级",
                    TextFormatting.GRAY + "• 生命恢复 +1级",
                    TextFormatting.GRAY + "• 饥饿管理 +1级",
                    TextFormatting.DARK_GREEN + "一次性提升多项生存能力",
                    TextFormatting.DARK_PURPLE + "稀有的综合升级"
            }, 1, 4
    );

    public static final ItemUpgradeComponent COMBAT_PACKAGE = createUpgrade(
            UpgradeType.COMBAT_PACKAGE, "combat_enhancement_package",
            new String[]{
                    TextFormatting.RED + "✦ 战斗强化套装 ✦",
                    TextFormatting.GOLD + "全面战斗能力提升:",
                    TextFormatting.GRAY + "• 伤害提升 +1级",
                    TextFormatting.GRAY + "• 攻击速度 +1级",
                    TextFormatting.GRAY + "• 范围拓展 +1级",
                    TextFormatting.DARK_RED + "成为战斗大师",
                    TextFormatting.DARK_PURPLE + "传说级战斗升级"
            }, 1, 4
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

        return upgrade;
    }

    /**
     * 获取所有新升级组件
     */
    public static ItemUpgradeComponent[] getAllExtendedUpgrades() {
        return new ItemUpgradeComponent[]{
                // 生存类
                YELLOW_SHIELD_LV1, YELLOW_SHIELD_LV2, YELLOW_SHIELD_LV3,
                HEALTH_REGEN_LV1, HEALTH_REGEN_LV2, HEALTH_REGEN_LV3,
                HUNGER_THIRST_LV1, HUNGER_THIRST_LV2, HUNGER_THIRST_LV3,
                THORNS_LV1, THORNS_LV2, THORNS_LV3,
                FIRE_EXTINGUISH_LV1, FIRE_EXTINGUISH_LV2, FIRE_EXTINGUISH_LV3,

                // 辅助类
                WATERPROOF_MODULE_BASIC, WATERPROOF_MODULE_ADVANCED, WATERPROOF_MODULE_DEEP_SEA,
                ORE_VISION_LV1, ORE_VISION_LV2, ORE_VISION_LV3,
                MOVEMENT_SPEED_LV1, MOVEMENT_SPEED_LV2, MOVEMENT_SPEED_LV3,
                STEALTH_LV1, STEALTH_LV2, STEALTH_LV3,
                EXP_AMPLIFIER_LV1, EXP_AMPLIFIER_LV2, EXP_AMPLIFIER_LV3,

                // 战斗类
                DAMAGE_BOOST_LV1, DAMAGE_BOOST_LV2, DAMAGE_BOOST_LV3, DAMAGE_BOOST_LV4, DAMAGE_BOOST_LV5,
                ATTACK_SPEED_LV1, ATTACK_SPEED_LV2, ATTACK_SPEED_LV3,
                RANGE_EXTENSION_LV1, RANGE_EXTENSION_LV2, RANGE_EXTENSION_LV3,
                PURSUIT_LV1, PURSUIT_LV2, PURSUIT_LV3,
                MAGIC_ABSORB_LV1, MAGIC_ABSORB_LV2, MAGIC_ABSORB_LV3,

                // 能源类
                KINETIC_GENERATOR_LV1, KINETIC_GENERATOR_LV2, KINETIC_GENERATOR_LV3,
                SOLAR_GENERATOR_LV1, SOLAR_GENERATOR_LV2, SOLAR_GENERATOR_LV3,
                VOID_ENERGY_LV1, VOID_ENERGY_LV2, VOID_ENERGY_LV3,
                COMBAT_CHARGER_LV1, COMBAT_CHARGER_LV2, COMBAT_CHARGER_LV3,

                // 特殊套装
                SURVIVAL_PACKAGE,
                COMBAT_PACKAGE
        };
    }
}