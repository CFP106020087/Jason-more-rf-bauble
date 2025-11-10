package com.moremod.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * 超简化的宝石词条API
 * 
 * 核心理念: 一行代码注册一个词条
 * 
 * 使用示例:
 * ```zenscript
 * import mods.moremod.GemAffix;
 * 
 * // 转换词条: 名称, 伤害类型, 最小值, 最大值, 稀有度
 * GemAffix.conversion("火焰转换", "fire", 0.15, 0.35, 100);
 * GemAffix.conversion("冰霜转换", "ice", 0.15, 0.35, 100);
 * GemAffix.conversion("神圣转换", "holy", 0.20, 0.40, 50);  // 自定义类型
 * 
 * // 增伤词条: 名称, 伤害类型, 最小倍率, 最大倍率, 稀有度
 * GemAffix.boost("火焰增伤", "fire", 0.3, 0.8, 80);
 * GemAffix.boost("暗影增伤", "shadow", 0.5, 1.2, 60);  // 自定义类型
 * 
 * // 攻速词条: 名称, 最小值, 最大值, 稀有度
 * GemAffix.speed("迅捷", 0.1, 0.3, 120);
 * 
 * // 固定伤害: 名称, 伤害类型, 最小值, 最大值, 稀有度
 * GemAffix.flat("火焰附伤", "fire", 3.0, 8.0, 90);
 * 
 * // 属性加成: 名称, 属性名, 最小值, 最大值, 稀有度
 * GemAffix.attribute("力量加成", "strength", 5.0, 15.0, 100);
 * 
 * // 特殊效果: 名称, 效果类型, 最小值, 最大值, 稀有度
 * GemAffix.special("生命偷取", "lifesteal", 0.02, 0.08, 70);
 * ```
 */
@ZenRegister
@ZenClass("mods.moremod.GemAffix")
public class CTGemAffixSimple {
    
    // 自动生成的ID计数器
    private static int idCounter = 0;
    
    // ==========================================
    // ⭐ 核心方法: 一行注册词条
    // ==========================================
    
    /**
     * 转换词条 - 将物理伤害转为元素伤害
     * 
     * @param name 显示名称 (例: "火焰转换")
     * @param damageType 伤害类型 (例: "fire", "holy", "chaos" - 任意自定义)
     * @param minRate 最小转换率 (0.0-1.0, 例: 0.15 = 15%)
     * @param maxRate 最大转换率 (0.0-1.0, 例: 0.35 = 35%)
     * @param rarity 稀有度/权重 (越高越常见, 建议: 50-150)
     * 
     * 示例: GemAffix.conversion("火焰转换", "fire", 0.15, 0.35, 100);
     */
    @ZenMethod
    public static void conversion(String name, String damageType, double minRate, double maxRate, int rarity) {
        try {
            String id = generateId("conv", damageType);
            
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(formatName(name, "转换", "§6"))
                    .setType(GemAffix.AffixType.DAMAGE_CONVERSION)
                    .setValueRange((float)minRate, (float)maxRate)
                    .setWeight(rarity)
                    .setLevelRequirement(1)
                    .setParameter("damageType", damageType);
            
            AffixPoolRegistry.registerAffix(affix);
            
            CraftTweakerAPI.logInfo(String.format(
                "[GemAffix] ✅ 转换词条: %s (%s, %.0f%%-%.0f%%, 稀有度:%d)",
                name, damageType, minRate*100, maxRate*100, rarity
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffix] 注册失败: " + name + " - " + e.getMessage());
        }
    }
    
    /**
     * 增伤词条 - 增加该元素的伤害倍率
     * 
     * @param name 显示名称 (例: "火焰增伤")
     * @param damageType 伤害类型
     * @param minBonus 最小增伤 (例: 0.3 = +30%)
     * @param maxBonus 最大增伤 (例: 0.8 = +80%)
     * @param rarity 稀有度
     * 
     * 示例: GemAffix.boost("火焰增伤", "fire", 0.3, 0.8, 80);
     */
    @ZenMethod
    public static void boost(String name, String damageType, double minBonus, double maxBonus, int rarity) {
        try {
            String id = generateId("boost", damageType);
            
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(formatName(name, "增伤", "§c"))
                    .setType(GemAffix.AffixType.DAMAGE_MULTIPLIER)
                    .setValueRange((float)(1.0 + minBonus), (float)(1.0 + maxBonus))
                    .setWeight(rarity)
                    .setLevelRequirement(1)
                    .setParameter("damageType", damageType);
            
            AffixPoolRegistry.registerAffix(affix);
            
            CraftTweakerAPI.logInfo(String.format(
                "[GemAffix] ✅ 增伤词条: %s (%s, +%.0f%%-+%.0f%%, 稀有度:%d)",
                name, damageType, minBonus*100, maxBonus*100, rarity
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffix] 注册失败: " + name + " - " + e.getMessage());
        }
    }
    
    /**
     * 攻速词条 - 增加攻击速度
     * 
     * @param name 显示名称 (例: "迅捷")
     * @param minSpeed 最小攻速 (例: 0.1)
     * @param maxSpeed 最大攻速 (例: 0.3)
     * @param rarity 稀有度
     * 
     * 示例: GemAffix.speed("迅捷", 0.1, 0.3, 120);
     */
    @ZenMethod
    public static void speed(String name, double minSpeed, double maxSpeed, int rarity) {
        try {
            String id = generateId("speed", null);
            
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(formatName(name, "攻速", "§a"))
                    .setType(GemAffix.AffixType.ATTACK_SPEED)
                    .setValueRange((float)minSpeed, (float)maxSpeed)
                    .setWeight(rarity)
                    .setLevelRequirement(1);
            
            AffixPoolRegistry.registerAffix(affix);
            
            CraftTweakerAPI.logInfo(String.format(
                "[GemAffix] ✅ 攻速词条: %s (%.2f-%.2f, 稀有度:%d)",
                name, minSpeed, maxSpeed, rarity
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffix] 注册失败: " + name + " - " + e.getMessage());
        }
    }
    
    /**
     * 固定伤害词条 - 附加固定元素伤害
     * 
     * @param name 显示名称 (例: "火焰附伤")
     * @param damageType 伤害类型
     * @param minDamage 最小伤害
     * @param maxDamage 最大伤害
     * @param rarity 稀有度
     * 
     * 示例: GemAffix.flat("火焰附伤", "fire", 3.0, 8.0, 90);
     */
    @ZenMethod
    public static void flat(String name, String damageType, double minDamage, double maxDamage, int rarity) {
        try {
            String id = generateId("flat", damageType);
            
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(formatName(name, "附伤", "§e"))
                    .setType(GemAffix.AffixType.FLAT_DAMAGE)
                    .setValueRange((float)minDamage, (float)maxDamage)
                    .setWeight(rarity)
                    .setLevelRequirement(1)
                    .setParameter("damageType", damageType);
            
            AffixPoolRegistry.registerAffix(affix);
            
            CraftTweakerAPI.logInfo(String.format(
                "[GemAffix] ✅ 附伤词条: %s (%s, %.1f-%.1f, 稀有度:%d)",
                name, damageType, minDamage, maxDamage, rarity
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffix] 注册失败: " + name + " - " + e.getMessage());
        }
    }
    
    /**
     * 属性词条 - 增加角色属性
     * 
     * @param name 显示名称 (例: "力量加成")
     * @param attribute 属性名 (例: "strength", "agility" - 自定义)
     * @param minValue 最小值
     * @param maxValue 最大值
     * @param rarity 稀有度
     * 
     * 示例: GemAffix.attribute("力量加成", "strength", 5.0, 15.0, 100);
     */
    @ZenMethod
    public static void attribute(String name, String attribute, double minValue, double maxValue, int rarity) {
        try {
            String id = generateId("attr", attribute);
            
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(formatName(name, "属性", "§b"))
                    .setType(GemAffix.AffixType.ATTRIBUTE_BONUS)
                    .setValueRange((float)minValue, (float)maxValue)
                    .setWeight(rarity)
                    .setLevelRequirement(1)
                    .setParameter("attribute", attribute);
            
            AffixPoolRegistry.registerAffix(affix);
            
            CraftTweakerAPI.logInfo(String.format(
                "[GemAffix] ✅ 属性词条: %s (%s, %.0f-%.0f, 稀有度:%d)",
                name, attribute, minValue, maxValue, rarity
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffix] 注册失败: " + name + " - " + e.getMessage());
        }
    }
    
    /**
     * 特殊效果词条 - 特殊机制
     * 
     * @param name 显示名称 (例: "生命偷取")
     * @param effectType 效果类型 (例: "lifesteal", "crit" - 自定义)
     * @param minValue 最小值
     * @param maxValue 最大值
     * @param rarity 稀有度
     * 
     * 示例: GemAffix.special("生命偷取", "lifesteal", 0.02, 0.08, 70);
     */
    @ZenMethod
    public static void special(String name, String effectType, double minValue, double maxValue, int rarity) {
        try {
            String id = generateId("special", effectType);
            
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(formatName(name, "特效", "§d"))
                    .setType(GemAffix.AffixType.SPECIAL_EFFECT)
                    .setValueRange((float)minValue, (float)maxValue)
                    .setWeight(rarity)
                    .setLevelRequirement(1)
                    .setParameter("effectType", effectType);
            
            AffixPoolRegistry.registerAffix(affix);
            
            CraftTweakerAPI.logInfo(String.format(
                "[GemAffix] ✅ 特效词条: %s (%s, 稀有度:%d)",
                name, effectType, rarity
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffix] 注册失败: " + name + " - " + e.getMessage());
        }
    }
    
    // ==========================================
    // 等级限制版本 (高级用法)
    // ==========================================
    
    /**
     * 转换词条 (带等级限制)
     */
    @ZenMethod
    public static void conversionLv(String name, String damageType, double minRate, double maxRate, int rarity, int levelReq) {
        try {
            String id = generateId("conv", damageType);
            
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(formatName(name, "转换", "§6"))
                    .setType(GemAffix.AffixType.DAMAGE_CONVERSION)
                    .setValueRange((float)minRate, (float)maxRate)
                    .setWeight(rarity)
                    .setLevelRequirement(levelReq)
                    .setParameter("damageType", damageType);
            
            AffixPoolRegistry.registerAffix(affix);
            
            CraftTweakerAPI.logInfo(String.format(
                "[GemAffix] ✅ 转换词条[Lv%d]: %s (%s, %.0f%%-%.0f%%)",
                levelReq, name, damageType, minRate*100, maxRate*100
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffix] 注册失败: " + name);
        }
    }
    
    @ZenMethod
    public static void boostLv(String name, String damageType, double minBonus, double maxBonus, int rarity, int levelReq) {
        try {
            String id = generateId("boost", damageType);
            
            GemAffix affix = new GemAffix(id)
                    .setDisplayName(formatName(name, "增伤", "§c"))
                    .setType(GemAffix.AffixType.DAMAGE_MULTIPLIER)
                    .setValueRange((float)(1.0 + minBonus), (float)(1.0 + maxBonus))
                    .setWeight(rarity)
                    .setLevelRequirement(levelReq)
                    .setParameter("damageType", damageType);
            
            AffixPoolRegistry.registerAffix(affix);
            
            CraftTweakerAPI.logInfo(String.format(
                "[GemAffix] ✅ 增伤词条[Lv%d]: %s (%s, +%.0f%%-+%.0f%%)",
                levelReq, name, damageType, minBonus*100, maxBonus*100
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemAffix] 注册失败: " + name);
        }
    }
    
    // ==========================================
    // 工具方法
    // ==========================================
    
    /**
     * 生成唯一ID
     */
    public static String generateId(String prefix, String type) {
        idCounter++;
        return prefix + "_" + (type != null ? type : "generic") + "_" + idCounter;
    }
    
    /**
     * 格式化显示名称
     */
    public static String formatName(String name, String category, String color) {
        return color + name + " {value}";
    }
    
    // ==========================================
    // 管理方法
    // ==========================================
    
    /**
     * 清空所有词条
     */
    @ZenMethod
    public static void clear() {
        AffixPoolRegistry.clearAll();
        idCounter = 0;
        CraftTweakerAPI.logInfo("[GemAffix] 已清空所有词条");
    }
    
    /**
     * 输出所有词条
     */
    @ZenMethod
    public static void dump() {
        AffixPoolRegistry.debugPrintAll();
    }
    
    /**
     * 启用调试模式
     */
    @ZenMethod
    public static void debug(boolean enable) {
        AffixPoolRegistry.setDebugMode(enable);
        CraftTweakerAPI.logInfo("[GemAffix] 调试模式: " + (enable ? "开启" : "关闭"));
    }
}
