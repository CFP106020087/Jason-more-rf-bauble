package com.moremod.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.Optional;

import java.util.Arrays;
import java.util.List;

/**
 * CraftTweaker API - 宝石词条系统 (完整版)
 *
 * 使用示例:
 * ```zenscript
 * import mods.moremod.GemAffixes;
 *
 * // 基础注册
 * GemAffixes.registerConversion("fire_conv", "§6物理转火焰 {value}", "fire", 0.25, 0.50, 100, 1);
 * GemAffixes.registerMultiplier("fire_boost", "§6火焰伤害 {value}", "fire", 1.3, 1.8, 80, 10);
 *
 * // 批量注册元素
 * GemAffixes.registerElement("fire", "火焰", 0.25, 0.50, 1.3, 1.8, 100);
 *
 * // 快捷注册
 * GemAffixes.addFireConversion("fire_1", "§6火焰转换 {value}");
 *
 * // 查询和管理
 * GemAffixes.list();
 * GemAffixes.count();
 * GemAffixes.exists("fire_1");
 * ```
 */
@ZenRegister
@ZenClass("mods.moremod.GemAffixes")
public class CTGemAffixes {

    // ==========================================
    // ⭐ 核心注册方法
    // ==========================================

    /**
     * 注册伤害转换词条
     *
     * @param id 词条ID
     * @param displayName 显示名称 (使用 {value} 作为数值占位符)
     * @param damageType 伤害类型 (fire/ice/lightning/poison等)
     * @param minConversion 最小转换率 (0.0-1.0)
     * @param maxConversion 最大转换率 (0.0-1.0)
     * @param weight 权重 (越高越容易抽到)
     * @param levelReq 等级要求
     */
    @ZenMethod
    public static void registerConversion(String id, String displayName, String damageType,
                                          double minConversion, double maxConversion,
                                          int weight, int levelReq) {
        try {
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(displayName)
                    .setType(GemAffix.AffixType.DAMAGE_CONVERSION)
                    .setValueRange((float)minConversion, (float)maxConversion)
                    .setWeight(weight)
                    .setLevelRequirement(levelReq)
                    .setParameter("damageType", damageType);

            AffixPoolRegistry.registerAffix(affix);

            CraftTweakerAPI.logInfo(String.format(
                    "[GemAffixes] ✅ 注册转换词条: %s (%s, %.0f%%-%.0f%%)",
                    id, damageType, minConversion * 100, maxConversion * 100
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 注册失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 注册伤害倍率词条
     *
     * @param id 词条ID
     * @param displayName 显示名称
     * @param damageType 伤害类型
     * @param minMultiplier 最小倍率 (≥1.0)
     * @param maxMultiplier 最大倍率 (≥1.0)
     * @param weight 权重
     * @param levelReq 等级要求
     */
    @ZenMethod
    public static void registerMultiplier(String id, String displayName, String damageType,
                                          double minMultiplier, double maxMultiplier,
                                          int weight, int levelReq) {
        try {
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(displayName)
                    .setType(GemAffix.AffixType.DAMAGE_MULTIPLIER)
                    .setValueRange(
                            Math.max(1.0f, (float)minMultiplier),
                            Math.max(1.0f, (float)maxMultiplier)
                    )
                    .setWeight(weight)
                    .setLevelRequirement(levelReq)
                    .setParameter("damageType", damageType);

            AffixPoolRegistry.registerAffix(affix);

            CraftTweakerAPI.logInfo(String.format(
                    "[GemAffixes] ✅ 注册增伤词条: %s (%s, ×%.1f-×%.1f)",
                    id, damageType, minMultiplier, maxMultiplier
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 注册失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 注册攻击速度词条
     *
     * @param id 词条ID
     * @param displayName 显示名称
     * @param minSpeed 最小速度加成
     * @param maxSpeed 最大速度加成
     * @param weight 权重
     * @param levelReq 等级要求
     */
    @ZenMethod
    public static void registerAttackSpeed(String id, String displayName,
                                           double minSpeed, double maxSpeed,
                                           int weight, int levelReq) {
        try {
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(displayName)
                    .setType(GemAffix.AffixType.ATTACK_SPEED)
                    .setValueRange((float)minSpeed, (float)maxSpeed)
                    .setWeight(weight)
                    .setLevelRequirement(levelReq);

            AffixPoolRegistry.registerAffix(affix);

            CraftTweakerAPI.logInfo(String.format(
                    "[GemAffixes] ✅ 注册攻速词条: %s (%.2f-%.2f)",
                    id, minSpeed, maxSpeed
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 注册失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 注册固定伤害词条
     *
     * @param id 词条ID
     * @param displayName 显示名称
     * @param damageType 伤害类型
     * @param minDamage 最小伤害
     * @param maxDamage 最大伤害
     * @param weight 权重
     * @param levelReq 等级要求
     */
    @ZenMethod
    public static void registerFlatDamage(String id, String displayName, String damageType,
                                          double minDamage, double maxDamage,
                                          int weight, int levelReq) {
        try {
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(displayName)
                    .setType(GemAffix.AffixType.FLAT_DAMAGE)
                    .setValueRange((float)minDamage, (float)maxDamage)
                    .setWeight(weight)
                    .setLevelRequirement(levelReq)
                    .setParameter("damageType", damageType);

            AffixPoolRegistry.registerAffix(affix);

            CraftTweakerAPI.logInfo(String.format(
                    "[GemAffixes] ✅ 注册固定伤害: %s (%s, %.1f-%.1f)",
                    id, damageType, minDamage, maxDamage
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 注册失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 注册属性加成词条 (力量/敏捷/智力等)
     *
     * @param id 词条ID
     * @param displayName 显示名称
     * @param attributeName 属性名称
     * @param minValue 最小值
     * @param maxValue 最大值
     * @param weight 权重
     * @param levelReq 等级要求
     */
    @ZenMethod
    public static void registerAttribute(String id, String displayName, String attributeName,
                                         double minValue, double maxValue,
                                         int weight, int levelReq) {
        try {
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(displayName)
                    .setType(GemAffix.AffixType.ATTRIBUTE_BONUS)
                    .setValueRange((float)minValue, (float)maxValue)
                    .setWeight(weight)
                    .setLevelRequirement(levelReq)
                    .setParameter("attribute", attributeName);

            AffixPoolRegistry.registerAffix(affix);

            CraftTweakerAPI.logInfo(String.format(
                    "[GemAffixes] ✅ 注册属性词条: %s (%s, %.0f-%.0f)",
                    id, attributeName, minValue, maxValue
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 注册失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 注册特殊效果词条 (吸血/暴击/击杀回血等)
     *
     * @param id 词条ID
     * @param displayName 显示名称
     * @param effectType 效果类型 (lifesteal/crit/onkill等)
     * @param minValue 最小值
     * @param maxValue 最大值
     * @param weight 权重
     * @param levelReq 等级要求
     */
    @ZenMethod
    public static void registerSpecialEffect(String id, String displayName, String effectType,
                                             double minValue, double maxValue,
                                             int weight, int levelReq) {
        try {
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(displayName)
                    .setType(GemAffix.AffixType.SPECIAL_EFFECT)
                    .setValueRange((float)minValue, (float)maxValue)
                    .setWeight(weight)
                    .setLevelRequirement(levelReq)
                    .setParameter("effectType", effectType);

            AffixPoolRegistry.registerAffix(affix);

            CraftTweakerAPI.logInfo(String.format(
                    "[GemAffixes] ✅ 注册特效词条: %s (%s)",
                    id, effectType
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 注册失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==========================================
    // 🎯 批量注册方法
    // ==========================================

    /**
     * 批量注册元素 (同时注册转换和增伤)
     *
     * @param elementType 元素类型 (fire/ice/lightning等)
     * @param displayName 显示名称 (例如: "火焰")
     * @param minConv 最小转换率
     * @param maxConv 最大转换率
     * @param minMult 最小伤害倍率
     * @param maxMult 最大伤害倍率
     * @param weight 权重
     */
    @ZenMethod
    public static void registerElement(String elementType, String displayName,
                                       double minConv, double maxConv,
                                       double minMult, double maxMult,
                                       int weight) {
        // 转换词条
        registerConversion(
                elementType + "_conversion",
                "§6" + displayName + "转换 {value}",
                elementType,
                minConv, maxConv,
                weight, 1
        );

        // 增伤词条
        registerMultiplier(
                elementType + "_damage",
                "§6" + displayName + "伤害 {value}",
                elementType,
                minMult, maxMult,
                (int)(weight * 0.8), 5
        );

        CraftTweakerAPI.logInfo(String.format(
                "[GemAffixes] 批量注册元素: %s", displayName
        ));
    }

    /**
     * 批量注册多个元素
     */
    @ZenMethod
    public static void registerElements(String[] elementTypes, String[] displayNames,
                                        double minConv, double maxConv,
                                        double minMult, double maxMult,
                                        int weight) {
        if (elementTypes.length != displayNames.length) {
            CraftTweakerAPI.logError("[GemAffixes] 元素类型和显示名称数量不匹配!");
            return;
        }

        for (int i = 0; i < elementTypes.length; i++) {
            registerElement(
                    elementTypes[i], displayNames[i],
                    minConv, maxConv, minMult, maxMult, weight
            );
        }
    }

    /**
     * 快速注册基础元素 (火/冰/雷/毒)
     */
    @ZenMethod
    public static void registerBasicElements() {
        registerElement("fire", "火焰", 0.15, 0.35, 1.2, 1.8, 100);
        registerElement("ice", "冰霜", 0.15, 0.35, 1.2, 1.8, 100);
        registerElement("lightning", "雷电", 0.15, 0.35, 1.2, 1.8, 100);
        registerElement("poison", "剧毒", 0.10, 0.30, 1.15, 1.6, 80);

        CraftTweakerAPI.logInfo("[GemAffixes] ✅ 已注册基础元素(火/冰/雷/毒)");
    }

    /**
     * 快速注册高级元素 (神圣/暗影/混沌/奥术)
     */
    @ZenMethod
    public static void registerAdvancedElements() {
        registerElement("holy", "神圣", 0.10, 0.25, 1.3, 2.0, 60);
        registerElement("shadow", "暗影", 0.10, 0.25, 1.3, 2.0, 60);
        registerElement("chaos", "混沌", 0.08, 0.20, 1.4, 2.2, 40);
        registerElement("arcane", "奥术", 0.08, 0.20, 1.4, 2.2, 40);

        CraftTweakerAPI.logInfo("[GemAffixes] ✅ 已注册高级元素(神圣/暗影/混沌/奥术)");
    }

    // ==========================================
    // 🔧 修改方法
    // ==========================================

    /**
     * 修改词条权重
     */
    @ZenMethod
    public static void setWeight(String id, int newWeight) {
        try {
            GemAffix affix = AffixPoolRegistry.getAffix(id);
            if (affix != null) {
                affix.setWeight(newWeight);
                CraftTweakerAPI.logInfo(String.format(
                        "[GemAffixes] 修改权重: %s → %d", id, newWeight
                ));
            } else {
                CraftTweakerAPI.logError("[GemAffixes] 词条不存在: " + id);
            }
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 修改失败: " + e.getMessage());
        }
    }

    /**
     * 修改词条等级要求
     */
    @ZenMethod
    public static void setLevelRequirement(String id, int newLevel) {
        try {
            GemAffix affix = AffixPoolRegistry.getAffix(id);
            if (affix != null) {
                affix.setLevelRequirement(newLevel);
                CraftTweakerAPI.logInfo(String.format(
                        "[GemAffixes] 修改等级要求: %s → %d", id, newLevel
                ));
            } else {
                CraftTweakerAPI.logError("[GemAffixes] 词条不存在: " + id);
            }
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 修改失败: " + e.getMessage());
        }
    }

    /**
     * 修改词条数值范围
     */
    @ZenMethod
    public static void setValueRange(String id, double minValue, double maxValue) {
        try {
            GemAffix affix = AffixPoolRegistry.getAffix(id);
            if (affix != null) {
                affix.setValueRange((float)minValue, (float)maxValue);
                CraftTweakerAPI.logInfo(String.format(
                        "[GemAffixes] 修改数值范围: %s → %.2f-%.2f",
                        id, minValue, maxValue
                ));
            } else {
                CraftTweakerAPI.logError("[GemAffixes] 词条不存在: " + id);
            }
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 修改失败: " + e.getMessage());
        }
    }

    /**
     * 修改显示名称
     */
    @ZenMethod
    public static void setDisplayName(String id, String newName) {
        try {
            GemAffix affix = AffixPoolRegistry.getAffix(id);
            if (affix != null) {
                affix.setDisplayName(newName);
                CraftTweakerAPI.logInfo(String.format(
                        "[GemAffixes] 修改显示名称: %s → %s", id, newName
                ));
            } else {
                CraftTweakerAPI.logError("[GemAffixes] 词条不存在: " + id);
            }
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 修改失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用词条
     */
    @ZenMethod
    public static void setEnabled(String id, boolean enabled) {
        try {
            GemAffix affix = AffixPoolRegistry.getAffix(id);
            if (affix != null) {
                affix.setEnabled(enabled);
                CraftTweakerAPI.logInfo(String.format(
                        "[GemAffixes] %s词条: %s",
                        enabled ? "启用" : "禁用", id
                ));
            } else {
                CraftTweakerAPI.logError("[GemAffixes] 词条不存在: " + id);
            }
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 修改失败: " + e.getMessage());
        }
    }

    // ==========================================
    // 📊 查询方法
    // ==========================================

    /**
     * 检查词条是否存在
     */
    @ZenMethod
    public static boolean exists(String id) {
        return AffixPoolRegistry.getAffix(id) != null;
    }

    /**
     * 获取词条数量
     */
    @ZenMethod
    public static int count() {
        return AffixPoolRegistry.getAffixCount();
    }

    /**
     * 获取总权重
     */
    @ZenMethod
    public static int getTotalWeight() {
        return AffixPoolRegistry.getTotalWeight();
    }

    /**
     * 列出所有词条ID
     */
    @ZenMethod
    public static void list() {
        List<String> ids = AffixPoolRegistry.getAllAffixIds();
        CraftTweakerAPI.logInfo("========================================");
        CraftTweakerAPI.logInfo("  已注册的宝石词条 (" + ids.size() + "个)");
        CraftTweakerAPI.logInfo("========================================");
        for (String id : ids) {
            GemAffix affix = AffixPoolRegistry.getAffix(id);
            CraftTweakerAPI.logInfo(String.format(
                    "  %s - %s (权重:%d, 等级:%d)",
                    id,
                    affix.getType().name(),
                    affix.getWeight(),
                    affix.getLevelRequirement()
            ));
        }
        CraftTweakerAPI.logInfo("========================================");
    }

    /**
     * 按类型列出词条
     */
    @ZenMethod
    public static void listByType(String typeName) {
        try {
            GemAffix.AffixType type = GemAffix.AffixType.valueOf(typeName.toUpperCase());
            List<GemAffix> affixes = AffixPoolRegistry.getAffixesByType(type);

            CraftTweakerAPI.logInfo("========================================");
            CraftTweakerAPI.logInfo("  " + typeName + " 类型词条 (" + affixes.size() + "个)");
            CraftTweakerAPI.logInfo("========================================");
            for (GemAffix affix : affixes) {
                CraftTweakerAPI.logInfo(String.format(
                        "  %s - 权重:%d, 等级:%d, 范围:%.2f-%.2f",
                        affix.getId(),
                        affix.getWeight(),
                        affix.getLevelRequirement(),
                        affix.getMinValue(),
                        affix.getMaxValue()
                ));
            }
            CraftTweakerAPI.logInfo("========================================");
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 无效的类型: " + typeName);
        }
    }

    /**
     * 获取词条详细信息
     */
    @ZenMethod
    public static void info(String id) {
        GemAffix affix = AffixPoolRegistry.getAffix(id);
        if (affix == null) {
            CraftTweakerAPI.logError("[GemAffixes] 词条不存在: " + id);
            return;
        }

        CraftTweakerAPI.logInfo("========================================");
        CraftTweakerAPI.logInfo("  词条详情: " + id);
        CraftTweakerAPI.logInfo("========================================");
        CraftTweakerAPI.logInfo("  显示名称: " + affix.getDisplayName());
        CraftTweakerAPI.logInfo("  类型: " + affix.getType().name());
        CraftTweakerAPI.logInfo("  权重: " + affix.getWeight());
        CraftTweakerAPI.logInfo("  等级要求: " + affix.getLevelRequirement());
        CraftTweakerAPI.logInfo("  数值范围: " + affix.getMinValue() + " - " + affix.getMaxValue());
        CraftTweakerAPI.logInfo("  是否启用: " + affix.isEnabled());

        // 显示参数
        if (affix.hasParameter("damageType")) {
            CraftTweakerAPI.logInfo("  伤害类型: " + affix.getParameter("damageType"));
        }
        if (affix.hasParameter("effectType")) {
            CraftTweakerAPI.logInfo("  效果类型: " + affix.getParameter("effectType"));
        }
        if (affix.hasParameter("attribute")) {
            CraftTweakerAPI.logInfo("  属性: " + affix.getParameter("attribute"));
        }

        CraftTweakerAPI.logInfo("========================================");
    }

    // ==========================================
    // 快捷注册方法
    // ==========================================

    /**
     * 快捷注册: 火焰转换 (默认25-50%, 权重100, 等级1)
     */
    @ZenMethod
    public static void addFireConversion(String id, String displayName) {
        registerConversion(id, displayName, "fire", 0.25, 0.50, 100, 1);
    }

    /**
     * 快捷注册: 火焰增伤 (默认+30-80%, 权重80, 等级5)
     */
    @ZenMethod
    public static void addFireBoost(String id, String displayName) {
        registerMultiplier(id, displayName, "fire", 1.3, 1.8, 80, 5);
    }

    /**
     * 快捷注册: 冰霜转换
     */
    @ZenMethod
    public static void addIceConversion(String id, String displayName) {
        registerConversion(id, displayName, "ice", 0.25, 0.50, 100, 1);
    }

    /**
     * 快捷注册: 冰霜增伤
     */
    @ZenMethod
    public static void addIceBoost(String id, String displayName) {
        registerMultiplier(id, displayName, "ice", 1.3, 1.8, 80, 5);
    }

    /**
     * 快捷注册: 雷电转换
     */
    @ZenMethod
    public static void addLightningConversion(String id, String displayName) {
        registerConversion(id, displayName, "lightning", 0.25, 0.50, 100, 1);
    }

    /**
     * 快捷注册: 雷电增伤
     */
    @ZenMethod
    public static void addLightningBoost(String id, String displayName) {
        registerMultiplier(id, displayName, "lightning", 1.3, 1.8, 80, 5);
    }

    /**
     * 快捷注册: 毒素转换
     */
    @ZenMethod
    public static void addPoisonConversion(String id, String displayName) {
        registerConversion(id, displayName, "poison", 0.20, 0.40, 80, 1);
    }

    /**
     * 快捷注册: 毒素增伤
     */
    @ZenMethod
    public static void addPoisonBoost(String id, String displayName) {
        registerMultiplier(id, displayName, "poison", 1.2, 1.6, 70, 5);
    }

    // ==========================================
    // 管理方法
    // ==========================================

    /**
     * 移除词条
     */
    @ZenMethod
    public static void remove(String id) {
        if (AffixPoolRegistry.removeAffix(id)) {
            CraftTweakerAPI.logInfo("[GemAffixes] 已移除词条: " + id);
        } else {
            CraftTweakerAPI.logError("[GemAffixes] 词条不存在: " + id);
        }
    }

    /**
     * 移除多个词条
     */
    @ZenMethod
    public static void removeMultiple(String[] ids) {
        int count = 0;
        for (String id : ids) {
            if (AffixPoolRegistry.removeAffix(id)) {
                count++;
            }
        }
        CraftTweakerAPI.logInfo(String.format(
                "[GemAffixes] 已移除 %d/%d 个词条", count, ids.length
        ));
    }

    /**
     * 按类型移除词条
     */
    @ZenMethod
    public static void removeByType(String typeName) {
        try {
            GemAffix.AffixType type = GemAffix.AffixType.valueOf(typeName.toUpperCase());
            int count = AffixPoolRegistry.removeAffixesByType(type);
            CraftTweakerAPI.logInfo(String.format(
                    "[GemAffixes] 已移除 %d 个 %s 类型词条", count, typeName
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 无效的类型: " + typeName);
        }
    }

    /**
     * 清空所有词条
     */
    @ZenMethod
    public static void clear() {
        AffixPoolRegistry.clearAll();
        CraftTweakerAPI.logInfo("[GemAffixes] 已清空所有词条");
    }

    /**
     * 重新加载词条
     */
    @ZenMethod
    public static void reload() {
        AffixPoolRegistry.reload();
        CraftTweakerAPI.logInfo("[GemAffixes] 已重新加载词条池");
    }

    /**
     * 导出配置到文件
     */
    @ZenMethod
    public static void export(String filename) {
        try {
            AffixPoolRegistry.exportToFile(filename);
            CraftTweakerAPI.logInfo("[GemAffixes] 已导出配置到: " + filename);
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 导出失败: " + e.getMessage());
        }
    }

    /**
     * 从文件导入配置
     */
    @ZenMethod
    public static void importConfig(String filename) {
        try {
            AffixPoolRegistry.importFromFile(filename);
            CraftTweakerAPI.logInfo("[GemAffixes] 已从文件导入配置: " + filename);
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffixes] 导入失败: " + e.getMessage());
        }
    }

    /**
     * 调试输出所有词条
     */
    @ZenMethod
    public static void dump() {
        AffixPoolRegistry.debugPrintAll();
    }

    /**
     * 启用调试模式
     */
    @ZenMethod
    public static void setDebugMode(boolean enable) {
        AffixPoolRegistry.setDebugMode(enable);
        CraftTweakerAPI.logInfo("[GemAffixes] 调试模式: " + (enable ? "启用" : "禁用"));
    }

    /**
     * 获取统计信息
     */
    @ZenMethod
    public static void stats() {
        int total = AffixPoolRegistry.getAffixCount();
        int totalWeight = AffixPoolRegistry.getTotalWeight();

        CraftTweakerAPI.logInfo("========================================");
        CraftTweakerAPI.logInfo("  宝石词条统计");
        CraftTweakerAPI.logInfo("========================================");
        CraftTweakerAPI.logInfo("  总词条数: " + total);
        CraftTweakerAPI.logInfo("  总权重: " + totalWeight);

        // 按类型统计
        for (GemAffix.AffixType type : GemAffix.AffixType.values()) {
            List<GemAffix> affixes = AffixPoolRegistry.getAffixesByType(type);
            if (!affixes.isEmpty()) {
                int typeWeight = affixes.stream()
                        .mapToInt(GemAffix::getWeight)
                        .sum();
                CraftTweakerAPI.logInfo(String.format(
                        "  %s: %d个 (权重:%d, %.1f%%)",
                        type.name(),
                        affixes.size(),
                        typeWeight,
                        (typeWeight * 100.0 / totalWeight)
                ));
            }
        }

        CraftTweakerAPI.logInfo("========================================");
    }
}