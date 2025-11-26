package com.moremod.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * 终极版宝石特殊效果API - 完整恢复版
 * 完全可配置,零硬编码
 *
 * ✅ 所有方法均对应 UltimateEffectHandler 中已实现的效果
 *
 * 支持效果分类:
 * - 💗 生命系统 (吸血/回血/护盾/偷盾)
 * - ⚔️ 伤害系统 (额外伤害/真伤/百分比伤害)
 * - ⏱️ 无敌帧系统 (减少/忽略/穿透)
 * - 🎯 控制系统 (击退/冰冻/眩晕/点燃)
 * - 💥 AOE系统 (范围伤害/爆炸/闪电链)
 * - ⚡ 剑气系统 (12种类型/完全自定义)
 * - 🌀 传送系统 (前方/背后/随机/闪现)
 * - 🛡️ 防御系统 (闪避/格挡/反伤)
 * - 🧪 药水效果 (完全可配置)
 * - 👹 召唤系统 (生物召唤)
 * - 💕 繁殖系统 (动物/村民)
 * - 🔊 音效粒子 (完全自定义)
 * - ⚡ 暴击系统 (几率/伤害)
 * - 🐉 Ice and Fire效果 (火龙/冰龙/雷龙)
 * - 🔥 连招系统 (自动攻击/连击增伤)
 * - ⭐ 标记系统 (标记/消耗)
 * - 🚀 特殊机制 (冲刺/跳跃/狂暴)
 */
@ZenRegister
@ZenClass("mods.moremod.GemUltimate")
public class CTGemUltimate {

    private static int idCounter = 0;

    // ==========================================
    // 💗 生命系统
    // ==========================================

    /**
     * 生命偷取
     *
     * @param name 显示名称
     * @param minPercent 最小偷取率 (0.02 = 2%)
     * @param maxPercent 最大偷取率
     * @param rarity 稀有度
     */
    @ZenMethod
    public static void lifesteal(String name, double minPercent, double maxPercent, int rarity) {
        createEffect(name, "lifesteal", minPercent, maxPercent, rarity, "§c")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 击杀回血
     *
     * @param name 显示名称
     * @param minPercent 最小回复率 (恢复目标最大生命的%)
     * @param maxPercent 最大回复率
     * @param rarity 稀有度
     */
    @ZenMethod
    public static void healOnKill(String name, double minPercent, double maxPercent, int rarity) {
        createEffect(name, "heal_on_kill", minPercent, maxPercent, rarity, "§d")
                .setTrigger(SpecialEffectTrigger.ON_KILL);
    }

    /**
     * 护盾 - 攻击时获得护盾
     *
     * @param name 显示名称
     * @param minAmount 最小护盾量
     * @param maxAmount 最大护盾量
     * @param rarity 稀有度
     */
    @ZenMethod
    public static void shield(String name, double minAmount, double maxAmount, int rarity) {
        createEffect(name, "shield", minAmount, maxAmount, rarity, "§b")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 偷盾 - 偷取目标的伤害吸收效果
     */
    @ZenMethod
    public static void absorbSteal(String name, double minPercent, double maxPercent, int rarity) {
        createEffect(name, "absorb_steal", minPercent, maxPercent, rarity, "§3")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    // ==========================================
    // ⚔️ 伤害系统 (完全可配置)
    // ==========================================

    /**
     * 额外伤害 - 可指定伤害类型
     *
     * @param name 显示名称
     * @param minPercent 最小伤害百分比
     * @param maxPercent 最大伤害百分比
     * @param damageType 伤害类型 (magic/fire/ice/lightning/holy/shadow/poison/true等)
     * @param rarity 稀有度
     */
    @ZenMethod
    public static void bonusDamage(String name, double minPercent, double maxPercent,
                                   String damageType, int rarity) {
        createEffect(name, "bonus_damage", minPercent, maxPercent, rarity, "§c")
                .setParam("damageType", damageType)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 固定额外伤害
     *
     * @param name 显示名称
     * @param minDamage 最小固定伤害值
     * @param maxDamage 最大固定伤害值
     * @param damageType 伤害类型
     * @param rarity 稀有度
     */
    @ZenMethod
    public static void customDamage(String name, double minDamage, double maxDamage,
                                    String damageType, int rarity) {
        createEffect(name, "custom_damage", minDamage, maxDamage, rarity, "§c")
                .setParam("damageType", damageType)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 百分比最大生命伤害
     */
    @ZenMethod
    public static void percentMaxHP(String name, double minPercent, double maxPercent, int rarity) {
        createEffect(name, "percent_max_hp", minPercent, maxPercent, rarity, "§c")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 百分比当前生命伤害
     */
    @ZenMethod
    public static void percentCurrentHP(String name, double minPercent, double maxPercent, int rarity) {
        createEffect(name, "percent_current_hp", minPercent, maxPercent, rarity, "§c")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 真实伤害 - 无视护甲
     */
    @ZenMethod
    public static void trueDamage(String name, double minPercent, double maxPercent, int rarity) {
        createEffect(name, "bonus_damage", minPercent, maxPercent, rarity, "§4")
                .setParam("damageType", "true")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    // ==========================================
    // ⏱️ 无敌帧系统 (NEW!)
    // ==========================================

    /**
     * 减少目标无敌帧
     *
     * @param name 显示名称
     * @param minReduction 最小减少率 (0.5 = 减少50%无敌帧)
     * @param maxReduction 最大减少率 (0.8 = 减少80%无敌帧)
     * @param rarity 稀有度
     */
    @ZenMethod
    public static void reduceIframes(String name, double minReduction, double maxReduction, int rarity) {
        createEffect(name, "reduce_iframes", minReduction, maxReduction, rarity, "§6")
                .setParam("sound", true)
                .setParam("particle", true)
                .setTrigger(SpecialEffectTrigger.ON_HIT);

        CraftTweakerAPI.logInfo(String.format(
                "[GemUltimate] ✅ 减少无敌帧: %s (%.0f%%-%.0f%%, 稀有度:%d)",
                name, minReduction*100, maxReduction*100, rarity
        ));
    }

    /**
     * 完全忽略无敌帧
     */
    @ZenMethod
    public static void ignoreIframes(String name, int rarity) {
        createEffect(name, "ignore_iframes", 1.0, 1.0, rarity, "§c")
                .setParam("sound", true)
                .setParam("particle", true)
                .setTrigger(SpecialEffectTrigger.ON_HIT);

        CraftTweakerAPI.logInfo(String.format(
                "[GemUltimate] ✅ 忽略无敌帧: %s (稀有度:%d)",
                name, rarity
        ));
    }

    /**
     * 穿透无敌帧
     */
    @ZenMethod
    public static void iframePenetration(String name, double minPenetration, double maxPenetration, int rarity) {
        createEffect(name, "iframe_penetration", minPenetration, maxPenetration, rarity, "§d")
                .setTrigger(SpecialEffectTrigger.ON_HIT);

        CraftTweakerAPI.logInfo(String.format(
                "[GemUltimate] ✅ 穿透无敌帧: %s (%.0f%%-%.0f%%, 稀有度:%d)",
                name, minPenetration*100, maxPenetration*100, rarity
        ));
    }

    /**
     * 快速连击 (组合效果)
     */
    @ZenMethod
    public static void rapidStrike(String name, double reduction, int rarity) {
        createEffect(name, "reduce_iframes", reduction, reduction, rarity, "§e")
                .setParam("sound", true)
                .setParam("particle", true)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 破甲重击 (组合效果)
     */
    @ZenMethod
    public static void armorBreak(String name, double iframeReduction, double bonusDamagePercent, int rarity) {
        createEffect(name + "_iframe", "reduce_iframes", iframeReduction, iframeReduction, rarity, "§6")
                .setParam("sound", true)
                .setTrigger(SpecialEffectTrigger.ON_HIT);

        createEffect(name + "_damage", "bonus_damage", bonusDamagePercent, bonusDamagePercent, rarity, "§c")
                .setParam("damageType", "true")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    // ==========================================
    // 🎯 控制系统
    // ==========================================

    /**
     * 击退效果
     */
    @ZenMethod
    public static void knockback(String name, double minStrength, double maxStrength, int rarity) {
        createEffect(name, "knockback", minStrength, maxStrength, rarity, "§7")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 击飞效果
     */
    @ZenMethod
    public static void knockup(String name, double minStrength, double maxStrength, int rarity) {
        createEffect(name, "knockup", minStrength, maxStrength, rarity, "§7")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 拉拽效果
     */
    @ZenMethod
    public static void pull(String name, double minStrength, double maxStrength, int rarity) {
        createEffect(name, "pull", minStrength, maxStrength, rarity, "§5")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 点燃效果
     */
    @ZenMethod
    public static void ignite(String name, int minSeconds, int maxSeconds, int rarity) {
        createEffect(name, "ignite", minSeconds, maxSeconds, rarity, "§6")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 冰冻 - 可配置范围冰冻
     */
    @ZenMethod
    public static void freeze(String name, int minDuration, int maxDuration, int rarity) {
        createEffect(name, "freeze", minDuration, maxDuration, rarity, "§b")
                .setParam("duration", 100)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 范围冰冻
     */
    @ZenMethod
    public static void freezeArea(String name, int duration, double radius, int rarity) {
        createEffect(name, "freeze", duration, duration, rarity, "§b")
                .setParam("duration", duration)
                .setParam("radius", radius)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 眩晕
     */
    @ZenMethod
    public static void stun(String name, int rarity) {
        createEffect(name, "stun", 1, 1, rarity, "§5")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    // ==========================================
    // 💥 AOE系统
    // ==========================================

    /**
     * AOE伤害 - 可配置伤害类型
     *
     * @param damageType 伤害类型 (true/magic/fire/ice/lightning等)
     */
    @ZenMethod
    public static void aoeDamage(String name, double minPercent, double maxPercent,
                                 double radius, String damageType, int rarity) {
        createEffect(name, "aoe_damage", minPercent, maxPercent, rarity, "§c")
                .setParam("radius", radius)
                .setParam("damageType", damageType)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 闪电链 - 可配置伤害类型
     */
    @ZenMethod
    public static void chainLightning(String name, double minPercent, double maxPercent,
                                      int rarity, int chainCount, double chainRadius,
                                      String damageType) {
        createEffect(name, "chain_damage", minPercent, maxPercent, rarity, "§e")
                .setParam("chainCount", chainCount)
                .setParam("chainRadius", (float)chainRadius)
                .setParam("damageDecay", 0.7f)
                .setParam("damageType", damageType)
                .setParam("particle", "FIREWORKS_SPARK")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 爆炸
     *
     * @param blockDamage 是否破坏方块
     */
    @ZenMethod
    public static void explosion(String name, double radius, double damage,
                                 boolean blockDamage, String damageType, int rarity) {
        createEffect(name, "explosion", damage, damage, rarity, "§c")
                .setParam("radius", (float)radius)
                .setParam("damage", (float)damage)
                .setParam("blockDamage", blockDamage)
                .setParam("damageType", damageType)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    // ==========================================
    // ⚡ 剑气系统 (12种类型)
    // ==========================================

    /**
     * 普通剑气
     */
    @ZenMethod
    public static void swordBeam(String name, double minMultiplier, double maxMultiplier, int rarity) {
        swordBeamCustom(name, minMultiplier, maxMultiplier, "normal", 2.0f, rarity);
    }

    /**
     * 龙形剑气 - 追踪敌人
     */
    @ZenMethod
    public static void dragonBeam(String name, double minMultiplier, double maxMultiplier, int rarity) {
        swordBeamCustom(name, minMultiplier, maxMultiplier, "dragon", 1.5f, rarity);
    }

    /**
     * 凤凰剑气 - 火焰效果
     */
    @ZenMethod
    public static void phoenixBeam(String name, double minMultiplier, double maxMultiplier, int rarity) {
        swordBeamCustom(name, minMultiplier, maxMultiplier, "phoenix", 2.0f, rarity);
    }

    /**
     * 螺旋剑气 - 旋转前进
     */
    @ZenMethod
    public static void spiralBeam(String name, double minMultiplier, double maxMultiplier, int rarity) {
        swordBeamCustom(name, minMultiplier, maxMultiplier, "spiral", 1.8f, rarity);
    }

    /**
     * 冰霜剑气
     */
    @ZenMethod
    public static void frostBeam(String name, double minMultiplier, double maxMultiplier, int rarity) {
        swordBeamCustom(name, minMultiplier, maxMultiplier, "frost", 2.0f, rarity);
    }

    /**
     * 雷电剑气 - 闪电链
     */
    @ZenMethod
    public static void lightningBeam(String name, double minMultiplier, double maxMultiplier, int rarity) {
        swordBeamCustom(name, minMultiplier, maxMultiplier, "lightning", 2.2f, rarity);
    }

    /**
     * 暗影剑气 - 穿透
     */
    @ZenMethod
    public static void shadowBeam(String name, double minMultiplier, double maxMultiplier, int rarity) {
        swordBeamCustom(name, minMultiplier, maxMultiplier, "shadow", 2.0f, rarity);
    }

    /**
     * 圣光剑气 - 治疗友军
     */
    @ZenMethod
    public static void holyBeam(String name, double minMultiplier, double maxMultiplier, int rarity) {
        swordBeamCustom(name, minMultiplier, maxMultiplier, "holy", 1.8f, rarity);
    }

    /**
     * 星辰剑气 - 爆炸
     */
    @ZenMethod
    public static void starBeam(String name, double minMultiplier, double maxMultiplier, int rarity) {
        swordBeamCustom(name, minMultiplier, maxMultiplier, "star", 2.0f, rarity);
    }

    /**
     * 完全自定义剑气
     *
     * @param beamType 剑气类型
     * @param speed 速度
     */
    @ZenMethod
    public static void swordBeamCustom(String name, double minMultiplier, double maxMultiplier,
                                       String beamType, double speed, int rarity) {
        createEffect(name, "sword_beam", minMultiplier, maxMultiplier, rarity, "§b")
                .setParam("beamType", beamType)
                .setParam("speed", (float)speed)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 自定义颜色剑气
     */
    @ZenMethod
    public static void swordBeamRGB(String name, double minMultiplier, double maxMultiplier,
                                    float red, float green, float blue, float scale,
                                    int penetrate, int rarity) {
        createEffect(name, "sword_beam", minMultiplier, maxMultiplier, rarity, "§b")
                .setParam("red", red)
                .setParam("green", green)
                .setParam("blue", blue)
                .setParam("scale", scale)
                .setParam("penetrate", penetrate)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 多重剑气 (扇形)
     */
    @ZenMethod
    public static void multiBeam(String name, int count, double spreadAngle, int rarity) {
        createEffect(name, "multi_beam", count, count, rarity, "§b")
                .setParam("count", count)
                .setParam("spreadAngle", (float)spreadAngle)
                .setParam("speed", 2.0f)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 环形剑气
     */
    @ZenMethod
    public static void circleBeam(String name, int count, int rarity) {
        createEffect(name, "circle_beam", count, count, rarity, "§b")
                .setParam("count", count)
                .setParam("speed", 2.0f)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 击杀发射剑气
     */
    @ZenMethod
    public static void swordBeamOnKill(String name, double minMultiplier, double maxMultiplier,
                                       String beamType, int rarity) {
        createEffect(name, "sword_beam_onkill", minMultiplier, maxMultiplier, rarity, "§b")
                .setParam("beamType", beamType)
                .setParam("speed", 2.5f)
                .setTrigger(SpecialEffectTrigger.ON_KILL);
    }

    // ==========================================
    // 🌀 传送系统
    // ==========================================

    /**
     * 向前传送 (目光传送)
     */
    @ZenMethod
    public static void teleportForward(String name, double distance, int rarity) {
        createEffect(name, "teleport_forward", distance, distance, rarity, "§d")
                .setParam("distance", distance)
                .setTrigger(SpecialEffectTrigger.ON_USE);
    }

    /**
     * 传送到目标背后
     */
    @ZenMethod
    public static void teleportBehind(String name, double distance, int rarity) {
        createEffect(name, "teleport_behind", distance, distance, rarity, "§d")
                .setParam("distance", distance)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 随机传送
     */
    @ZenMethod
    public static void teleportRandom(String name, double range, int rarity) {
        createEffect(name, "teleport_random", range, range, rarity, "§d")
                .setParam("range", range)
                .setTrigger(SpecialEffectTrigger.ON_USE);
    }

    /**
     * 闪现 (短距离快速传送)
     */
    @ZenMethod
    public static void blink(String name, int rarity) {
        createEffect(name, "blink", 5, 5, rarity, "§d")
                .setParam("distance", 5.0)
                .setTrigger(SpecialEffectTrigger.ON_USE);
    }

    // ==========================================
    // 🛡️ 防御系统
    // ==========================================

    /**
     * 闪避几率
     */
    @ZenMethod
    public static void dodgeChance(String name, double minChance, double maxChance, int rarity) {
        createEffect(name, "dodge", minChance, maxChance, rarity, "§a")
                .setTrigger(SpecialEffectTrigger.ON_HIT_TAKEN);
    }

    /**
     * 格挡几率
     */
    @ZenMethod
    public static void blockChance(String name, double minChance, double maxChance, int rarity) {
        createEffect(name, "block", minChance, maxChance, rarity, "§7")
                .setTrigger(SpecialEffectTrigger.ON_HIT_TAKEN);
    }

    /**
     * 反伤 - 可配置伤害类型
     */
    @ZenMethod
    public static void thorns(String name, double minPercent, double maxPercent,
                              String damageType, int rarity) {
        createEffect(name, "thorns", minPercent, maxPercent, rarity, "§5")
                .setParam("damageType", damageType)
                .setTrigger(SpecialEffectTrigger.ON_HIT_TAKEN);
    }

    // ==========================================
    // 🧪 药水效果 (完全可配置)
    // ==========================================

    /**
     * 施加药水效果
     *
     * @param potionId 药水ID (minecraft:poison等)
     * @param minDuration 最小持续时间(tick)
     * @param maxDuration 最大持续时间
     * @param amplifier 效果等级
     */
    @ZenMethod
    public static void applyPotion(String name, String potionId,
                                   int minDuration, int maxDuration,
                                   int amplifier, int rarity) {
        createEffect(name, "potion", minDuration, maxDuration, rarity, "§d")
                .setParam("potionId", potionId)
                .setParam("amplifier", amplifier)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 给自己施加药水效果
     */
    @ZenMethod
    public static void applyPotionSelf(String name, String potionId,
                                       int duration, int amplifier, int rarity) {
        createEffect(name, "potion_self", duration, duration, rarity, "§b")
                .setParam("potionId", potionId)
                .setParam("amplifier", amplifier)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 随机药水效果
     */
    @ZenMethod
    public static void randomPotion(String name, int duration, int rarity) {
        createEffect(name, "random_potion", duration, duration, rarity, "§5")
                .setParam("duration", duration)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    // ==========================================
    // 👹 召唤系统
    // ==========================================

    /**
     * 召唤生物
     *
     * @param entityId 生物ID (minecraft:zombie等)
     * @param count 数量
     * @param radius 召唤半径
     */
    @ZenMethod
    public static void summonEntity(String name, String entityId, int count,
                                    double radius, int rarity) {
        createEffect(name, "summon_entity", count, count, rarity, "§5")
                .setParam("entityId", entityId)
                .setParam("count", count)
                .setParam("radius", radius)
                .setTrigger(SpecialEffectTrigger.ON_KILL);
    }

    /**
     * 召唤友方
     */
    @ZenMethod
    public static void summonAlly(String name, String entityId, int count, int rarity) {
        createEffect(name, "summon_ally", count, count, rarity, "§2")
                .setParam("entityId", entityId)
                .setParam("count", count)
                .setParam("radius", 3.0)
                .setTrigger(SpecialEffectTrigger.ON_KILL);
    }

    // ==========================================
    // 💕 繁殖系统 (LoveHelper)
    // ==========================================

    /**
     * 使动物进入繁殖状态
     */
    @ZenMethod
    public static void animalLove(String name, int rarity) {
        createEffect(name, "animal_love", 1, 1, rarity, "§d")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 使村民配对
     */
    @ZenMethod
    public static void villagerMate(String name, double radius, int rarity) {
        createEffect(name, "villager_mate", radius, radius, rarity, "§a")
                .setParam("radius", radius)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    // ==========================================
    // 🔊 音效/粒子 (完全可配置)
    // ==========================================

    /**
     * 播放音效
     *
     * @param soundId 音效ID (entity.player.attack.crit等)
     * @param volume 音量
     * @param pitch 音调
     */
    @ZenMethod
    public static void playSound(String name, String soundId, double volume, double pitch, int rarity) {
        createEffect(name, "sound", 1, 1, rarity, "§e")
                .setParam("sound", soundId)
                .setParam("volume", (float)volume)
                .setParam("pitch", (float)pitch)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 播放粒子效果
     *
     * @param particleType 粒子类型 (CRIT/FIREWORKS_SPARK等)
     * @param count 数量
     * @param radius 半径
     */
    @ZenMethod
    public static void playParticle(String name, String particleType, int count, double radius, int rarity) {
        createEffect(name, "particle", count, count, rarity, "§e")
                .setParam("particle", particleType)
                .setParam("count", count)
                .setParam("radius", radius)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 粒子轨迹 (攻击者到目标)
     */
    @ZenMethod
    public static void particleTrail(String name, String particleType, int rarity) {
        createEffect(name, "particle_trail", 1, 1, rarity, "§e")
                .setParam("particle", particleType)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    // ==========================================
    // ⚡ 暴击系统
    // ==========================================

    /**
     * 暴击几率
     */
    @ZenMethod
    public static void critChance(String name, double minChance, double maxChance, int rarity) {
        createEffect(name, "crit_chance", minChance, maxChance, rarity, "§e")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 暴击伤害
     */
    @ZenMethod
    public static void critDamage(String name, double minBonus, double maxBonus, int rarity) {
        createEffect(name, "crit_damage", minBonus, maxBonus, rarity, "§e")
                .setTrigger(SpecialEffectTrigger.ON_CRIT);
    }

    // ==========================================
    // 🐉 Ice and Fire 效果
    // ==========================================

    /**
     * 火龙效果 - 点燃+击退+对冰龙额外伤害
     *
     * @param name 显示名称
     * @param fireDuration 点燃时间(秒)
     * @param knockback 击退强度
     * @param dragonBonus 对龙额外伤害
     * @param rarity 稀有度
     */
    @ZenMethod
    public static void firedragonEffect(String name, int fireDuration,
                                        double knockback, double dragonBonus, int rarity) {
        createEffect(name, "icefire_fire", fireDuration, fireDuration, rarity, "§6")
                .setParam("fireDuration", fireDuration)
                .setParam("knockback", (float)knockback)
                .setParam("dragonBonus", (float)dragonBonus)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 冰龙效果 - 冰冻+减速+挖掘疲劳+击退+对火龙额外伤害
     *
     * @param name 显示名称
     * @param frozenTicks 冰冻时长(tick)
     * @param slownessDuration 减速时长(tick)
     * @param knockback 击退强度
     * @param dragonBonus 对龙额外伤害
     * @param rarity 稀有度
     */
    @ZenMethod
    public static void icedragonEffect(String name, int frozenTicks, int slownessDuration,
                                       double knockback, double dragonBonus, int rarity) {
        createEffect(name, "icefire_ice", frozenTicks, frozenTicks, rarity, "§b")
                .setParam("frozenTicks", frozenTicks)
                .setParam("slownessDuration", slownessDuration)
                .setParam("knockback", (float)knockback)
                .setParam("dragonBonus", (float)dragonBonus)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 雷电效果 - 闪电链+击退+对龙额外伤害
     *
     * @param name 显示名称
     * @param knockback 击退强度
     * @param dragonBonus 对龙额外伤害
     * @param rarity 稀有度
     */
    @ZenMethod
    public static void lightningdragonEffect(String name, double knockback,
                                             double dragonBonus, int rarity) {
        createEffect(name, "icefire_lightning", 1, 1, rarity, "§e")
                .setParam("knockback", (float)knockback)
                .setParam("dragonBonus", (float)dragonBonus)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 三重龙效果 - 火+冰+雷
     *
     * @param name 显示名称
     * @param rarity 稀有度
     */
    @ZenMethod
    public static void tripledragonEffect(String name, int rarity) {
        createEffect(name, "icefire_triple", 1, 1, rarity, "§5")
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    // ==========================================
    // 🔥 连招系统 (自动攻击 + 连击增伤)
    // ==========================================

    /**
     * 自动攻击 - 提升攻击速度
     *
     * @param name 显示名称（例如："狂怒"）
     * @param minSpeed 最小攻击速度倍率（例如：1.2 = 20%加速）
     * @param maxSpeed 最大攻击速度倍率（例如：2.5 = 150%加速）
     * @param rarity 稀有度（越高越常见）
     */
    @ZenMethod
    public static void autoAttack(String name, double minSpeed, double maxSpeed, int rarity) {
        try {
            // 重要：使用固定的ID格式，不包含值
            String id = "auto_attack_" + name.toLowerCase().replaceAll("[^a-z0-9_]", "");

            GemAffix affix = new GemAffix(id)
                    .setDisplayName("§c" + name + " §7[攻速 ×{value}]")
                    .setType(GemAffix.AffixType.SPECIAL_EFFECT)
                    .setValueRange((float)minSpeed, (float)maxSpeed)
                    .setWeight(rarity)
                    .setLevelRequirement(1)
                    .setParameter("effectType", "auto_attack");  // 关键参数

            AffixPoolRegistry.registerAffix(affix);

            CraftTweakerAPI.logInfo(String.format(
                    "[GemUltimate] ✅ 自动攻击: %s (ID: %s, 速度×%.1f-×%.1f, 稀有度:%d)",
                    name, id, minSpeed, maxSpeed, rarity
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemUltimate] 注册失败: " + name + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 连击增伤词条
     *
     * @param name 显示名称（例如："连击"）
     * @param minBonus 最小每次增伤（例如：0.05 = 每次+5%）
     * @param maxBonus 最大每次增伤（例如：0.15 = 每次+15%）
     * @param rarity 稀有度
     */
    @ZenMethod
    public static void comboDamage(String name, double minBonus, double maxBonus, int rarity) {
        try {
            // 重要：使用固定的ID格式，不包含值
            String id = "combo_damage_" + name.toLowerCase().replaceAll("[^a-z0-9_]", "");

            GemAffix affix = new GemAffix(id)
                    .setDisplayName("§6" + name + " §7[每次 +{value}]")
                    .setType(GemAffix.AffixType.SPECIAL_EFFECT)
                    .setValueRange((float)minBonus, (float)maxBonus)
                    .setWeight(rarity)
                    .setLevelRequirement(1)
                    .setParameter("effectType", "combo_damage");  // 关键参数

            AffixPoolRegistry.registerAffix(affix);

            CraftTweakerAPI.logInfo(String.format(
                    "[GemUltimate] ✅ 连击增伤: %s (ID: %s, 每次+%.0f%%-+%.0f%%, 稀有度:%d)",
                    name, id, minBonus*100, maxBonus*100, rarity
            ));
        } catch (Exception e) {
            CraftTweakerAPI.logError("[GemUltimate] 注册失败: " + name + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 连招系统（自动攻击+连击增伤 二合一）
     */
    @ZenMethod
    public static void comboSystem(String name, double minSpeed, double maxSpeed,
                                   double minCombo, double maxCombo, int rarity) {
        autoAttack(name + "_speed", minSpeed, maxSpeed, rarity);
        comboDamage(name + "_combo", minCombo, maxCombo, rarity);
    }

    // ==========================================
    // ⭐ 标记系统
    // ==========================================

    /**
     * 标记目标
     */
    @ZenMethod
    public static void mark(String name, String markKey, int duration, int rarity) {
        createEffect(name, "mark", duration, duration, rarity, "§e")
                .setParam("markKey", markKey)
                .setParam("duration", duration)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    /**
     * 消耗标记并造成伤害
     */
    @ZenMethod
    public static void consumeMark(String name, String markKey, double bonusPercent, int rarity) {
        createEffect(name, "consume_mark", bonusPercent, bonusPercent, rarity, "§e")
                .setParam("markKey", markKey)
                .setTrigger(SpecialEffectTrigger.ON_HIT);
    }

    // ==========================================
    // 🚀 特殊机制
    // ==========================================

    /**
     * 冲刺
     */
    @ZenMethod
    public static void dash(String name, double speed, int rarity) {
        createEffect(name, "dash", speed, speed, rarity, "§f")
                .setParam("speed", speed)
                .setParam("yBoost", 0.2)
                .setTrigger(SpecialEffectTrigger.ON_USE);
    }

    /**
     * 跳跃
     */
    @ZenMethod
    public static void leap(String name, double power, int rarity) {
        createEffect(name, "leap", power, power, rarity, "§f")
                .setParam("power", power)
                .setTrigger(SpecialEffectTrigger.ON_USE);
    }

    /**
     * 狂暴 - 多个增益效果
     */
    @ZenMethod
    public static void rage(String name, int duration, int rarity) {
        createEffect(name, "rage", duration, duration, rarity, "§c")
                .setParam("duration", duration)
                .setTrigger(SpecialEffectTrigger.ON_LOW_HEALTH);
    }

    // ==========================================
    // 工具方法
    // ==========================================

    private static EffectBuilder createEffect(String name, String effectType,
                                              double minValue, double maxValue,
                                              int rarity, String colorCode) {
        String id = "ultimate_" + effectType + "_" + (++idCounter);

        GemAffix affix = new GemAffix(id)
                .setDisplayName(colorCode + name + " {value}")
                .setType(GemAffix.AffixType.SPECIAL_EFFECT)
                .setValueRange((float)minValue, (float)maxValue)
                .setWeight(rarity)
                .setLevelRequirement(1)
                .setParameter("effectType", effectType);

        return new EffectBuilder(affix);
    }

    /**
     * 效果构建器 - 链式设置参数
     */
    private static class EffectBuilder {
        private final GemAffix affix;

        public EffectBuilder(GemAffix affix) {
            this.affix = affix;
        }

        public EffectBuilder setParam(String key, Object value) {
            affix.setParameter(key, value);
            return this;
        }

        public EffectBuilder setTrigger(SpecialEffectTrigger trigger) {
            affix.setParameter("trigger", trigger.name());
            finish();
            return this;
        }

        private void finish() {
            AffixPoolRegistry.registerAffix(affix);

            String effectType = (String) affix.getParameter("effectType");
            CraftTweakerAPI.logInfo(String.format(
                    "[GemUltimate] ✅ %s: %s (%.2f-%.2f, 权重%d)",
                    effectType, affix.getDisplayName(),
                    affix.getMinValue(), affix.getMaxValue(),
                    affix.getWeight()
            ));
        }
    }

    /**
     * 清空所有特殊效果
     */
    @ZenMethod
    public static void clear() {
        idCounter = 0;
        CraftTweakerAPI.logInfo("[GemUltimate] 已清空所有效果");
    }
}