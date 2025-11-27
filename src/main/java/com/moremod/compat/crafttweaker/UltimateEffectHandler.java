package com.moremod.compat.crafttweaker;

import com.moremod.entity.SwordBeamType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.util.math.AxisAlignedBB;
import java.util.*;

/**
 * 终极版特殊效果处理器
 * 完全可配置,零硬编码
 *
 * 整合Helper:
 * - CombatHelper
 * - SwordBeamHelper
 * - SoundHelper
 * - TeleportHelper
 * - AreaHelper
 * - EntityHelper
 * - LoveHelper
 * - DamageHelper
 */
public class UltimateEffectHandler {

    private static final Random RANDOM = new Random();

    /**
     * 完全可配置的效果配置类
     */
    public static class EffectConfig {
        // 基础属性
        public final String effectType;
        public float value;
        public final Map<String, Object> params;

        public EffectConfig(String effectType, float value) {
            this.effectType = effectType;
            this.value = value;
            this.params = new HashMap<>();
        }

        // 流式设置参数
        public EffectConfig param(String key, Object val) {
            params.put(key, val);
            return this;
        }

        public EffectConfig damage(float dmg) {
            return param("damage", dmg);
        }

        public EffectConfig damageType(String type) {
            return param("damageType", type);
        }

        public EffectConfig radius(double r) {
            return param("radius", r);
        }

        public EffectConfig duration(int ticks) {
            return param("duration", ticks);
        }

        public EffectConfig sound(String soundId) {
            return param("sound", soundId);
        }

        public EffectConfig particle(String particleType) {
            return param("particle", particleType);
        }

        public EffectConfig beamType(String type) {
            return param("beamType", type);
        }

        // 获取参数
        public Object get(String key) {
            return params.get(key);
        }

        public String getString(String key, String def) {
            Object o = params.get(key);
            return o != null ? o.toString() : def;
        }

        public float getFloat(String key, float def) {
            Object o = params.get(key);
            if (o instanceof Number) return ((Number) o).floatValue();
            return def;
        }

        public int getInt(String key, int def) {
            Object o = params.get(key);
            if (o instanceof Number) return ((Number) o).intValue();
            return def;
        }

        public double getDouble(String key, double def) {
            Object o = params.get(key);
            if (o instanceof Number) return ((Number) o).doubleValue();
            return def;
        }

        public boolean getBoolean(String key, boolean def) {
            Object o = params.get(key);
            if (o instanceof Boolean) return (Boolean) o;
            return def;
        }
    }

    /**
     * 触发特殊效果
     */
    public static boolean triggerEffect(EffectConfig config,
                                        SpecialEffectTrigger trigger,
                                        EntityLivingBase attacker,
                                        EntityLivingBase victim,
                                        float damage) {
        if (config == null || attacker == null) return false;

        try {
            switch (config.effectType) {
                // ========== 生命相关 ==========
                case "lifesteal": return effectLifesteal(attacker, damage, config);
                case "lifesteal_onkill": return trigger == SpecialEffectTrigger.ON_KILL && effectLifesteal(attacker, damage, config);
                case "heal_on_kill": return trigger == SpecialEffectTrigger.ON_KILL && effectHealOnKill(attacker, victim, config);

                // ========== 暴击系统 ==========
                case "crit_chance": return RANDOM.nextFloat() < config.value;
                case "crit_damage": return true;

                // ========== 伤害效果 (完全可配置) ==========
                case "bonus_damage": return effectBonusDamage(victim, attacker, damage, config);
                case "custom_damage": return effectCustomDamage(victim, attacker, config);
                case "percent_max_hp": return effectPercentMaxHP(victim, attacker, config);
                case "percent_current_hp": return effectPercentCurrentHP(victim, attacker, config);

                // ========== 控制效果 ==========
                case "knockback": return effectKnockback(victim, attacker, config);
                case "knockup": return effectKnockup(victim, config);
                case "pull": return effectPull(victim, attacker, config);
                case "ignite": return effectIgnite(victim, config);
                case "freeze": return effectFreeze(victim, config);
                case "stun": return effectStun(victim, config);

                case "icefire_fire": return effectIceFireFire(victim, attacker, damage, config);
                case "icefire_ice": return effectIceFireIce(victim, attacker, damage, config);
                case "icefire_lightning": return effectIceFireLightning(victim, attacker, damage, config);
                case "icefire_triple": return effectIceFireTriple(victim, attacker, damage, config);

                // ========== AOE效果 ==========
                case "aoe_damage": return effectAOEDamage(attacker, damage, config);
                case "chain_damage": return effectChainDamage(attacker, victim, damage, config);
                case "explosion": return effectExplosion(attacker, victim, config);
//=====================================
                case "reduce_iframes": return effectReduceIframes(victim, config);
                case "ignore_iframes": return effectIgnoreIframes(victim, config);
                case "iframe_penetration": return effectIframePenetration(victim, config);



                // ========== 剑气系统 (完全可配置) ==========
                case "sword_beam": return effectSwordBeam(attacker, damage, config);
                case "sword_beam_onkill": return trigger == SpecialEffectTrigger.ON_KILL && effectSwordBeam(attacker, damage, config);
                case "multi_beam": return effectMultiBeam(attacker, damage, config);
                case "circle_beam": return effectCircleBeam(attacker, damage, config);

                // ========== 传送系统 ==========
                case "teleport_forward": return effectTeleportForward(attacker, config);
                case "teleport_behind": return effectTeleportBehind(attacker, victim, config);
                case "teleport_random": return effectTeleportRandom(attacker, config);
                case "blink": return effectBlink(attacker, config);

                // ========== 防御系统 ==========
                case "dodge": return RANDOM.nextFloat() < config.value;
                case "block": return RANDOM.nextFloat() < config.value;
                case "thorns": return trigger == SpecialEffectTrigger.ON_HIT_TAKEN && effectThorns(victim, attacker, damage, config);
                case "absorb_steal": return effectAbsorbSteal(attacker, victim, damage, config);

                // ========== 药水效果 (完全可配置) ==========
                case "potion": return effectPotion(victim, config);
                case "potion_self": return effectPotion(attacker, config);
                case "random_potion": return effectRandomPotion(victim, config);

                // ========== 召唤系统 ==========
                case "summon_entity": return effectSummonEntity(attacker, config);
                case "summon_ally": return effectSummonAlly(attacker, config);

                // ========== 繁殖系统 (LoveHelper) ==========
                case "animal_love": return effectAnimalLove(victim, config);
                case "villager_mate": return effectVillagerMate(victim, config);

                // ========== 音效/粒子 ==========
                case "sound": return effectSound(attacker, config);
                case "particle": return effectParticle(attacker, config);
                case "particle_trail": return effectParticleTrail(attacker, victim, config);

                // ========== 标记系统 ==========
                case "mark": return effectMark(victim, config);
                case "consume_mark": return effectConsumeMark(victim, damage, config);

                // ========== 特殊效果 ==========
                case "dash": return effectDash(attacker, config);
                case "leap": return effectLeap(attacker, config);
                case "rage": return effectRage(attacker, config);
                case "shield": return effectShield(attacker, config);
            }
        } catch (Exception e) {
            System.err.println("[UltimateEffect] 触发效果失败: " + config.effectType);
            e.printStackTrace();
        }

        return false;
    }

    // ==========================================
    // 生命相关效果
    // ==========================================

    private static boolean effectLifesteal(EntityLivingBase attacker, float damage, EffectConfig config) {
        float healAmount = damage * config.value;
        return CombatHelper.heal(wrap(attacker), healAmount) > 0;
    }

    private static boolean effectHealOnKill(EntityLivingBase attacker, EntityLivingBase victim, EffectConfig config) {
        if (victim == null) return false;
        float healAmount = victim.getMaxHealth() * config.value;
        return CombatHelper.heal(wrap(attacker), healAmount) > 0;
    }

    // ==========================================
    // 伤害效果 (完全可配置)
    // ==========================================

    private static boolean effectBonusDamage(EntityLivingBase victim, EntityLivingBase attacker,
                                             float damage, EffectConfig config) {
        if (victim == null) return false;
        float bonusDamage = damage * config.value;

        // 可配置伤害类型
        String damageType = config.getString("damageType", "magic");

        if ("true".equals(damageType)) {
            return CombatHelper.trueDamage(wrap(victim), bonusDamage, wrap(attacker)) > 0;
        } else {
            DamageHelper.dealCustomDamage(
                    wrapLiving(victim),
                    wrapLiving(attacker),
                    bonusDamage,
                    damageType
            );
            return true;
        }
    }

    private static boolean effectCustomDamage(EntityLivingBase victim, EntityLivingBase attacker,
                                              EffectConfig config) {
        if (victim == null) return false;

        float damage = config.getFloat("damage", 5.0f);
        String damageType = config.getString("damageType", "magic");

        DamageHelper.dealCustomDamage(
                wrapLiving(victim),
                wrapLiving(attacker),
                damage,
                damageType
        );
        return true;
    }

    private static boolean effectPercentMaxHP(EntityLivingBase victim, EntityLivingBase attacker,
                                              EffectConfig config) {
        if (victim == null) return false;
        return CombatHelper.percentDamageMax(wrap(victim), config.value * 100, wrap(attacker)) > 0;
    }

    private static boolean effectPercentCurrentHP(EntityLivingBase victim, EntityLivingBase attacker,
                                                  EffectConfig config) {
        if (victim == null) return false;
        return CombatHelper.percentDamageCurrent(wrap(victim), config.value * 100, wrap(attacker)) > 0;
    }

    // ==========================================
    // 控制效果
    // ==========================================

    private static boolean effectKnockback(EntityLivingBase victim, EntityLivingBase attacker,
                                           EffectConfig config) {
        if (victim == null) return false;
        return CombatHelper.knockback(wrap(victim), wrap(attacker), config.value);
    }

    private static boolean effectKnockup(EntityLivingBase victim, EffectConfig config) {
        if (victim == null) return false;
        return CombatHelper.knockUp(wrap(victim), config.value);
    }

    private static boolean effectPull(EntityLivingBase victim, EntityLivingBase attacker,
                                      EffectConfig config) {
        if (victim == null) return false;
        float yBoost = config.getFloat("yBoost", 0.1f);
        return CombatHelper.pullTowards(wrap(victim), wrap(attacker), config.value, yBoost);
    }

    private static boolean effectIgnite(EntityLivingBase victim, EffectConfig config) {
        if (victim == null) return false;
        int seconds = Math.round(config.value);
        return CombatHelper.ignite(wrap(victim), seconds);
    }

    private static boolean effectFreeze(EntityLivingBase victim, EffectConfig config) {
        if (victim == null) return false;
        int duration = config.getInt("duration", 100);
        double radius = config.getDouble("radius", 0);

        if (radius > 0) {
            // 区域冰冻
            AreaHelper.freezeArea(
                    wrap(victim.world),
                    victim.posX, victim.posY, victim.posZ,
                    radius, duration
            );
        } else {
            // 单体冰冻
            CombatHelper.addEffect(wrap(victim), "minecraft:slowness", duration, 10, false, true);
            CombatHelper.addEffect(wrap(victim), "minecraft:mining_fatigue", duration, 10, false, true);
        }
        return true;
    }

    private static boolean effectStun(EntityLivingBase victim, EffectConfig config) {
        if (victim == null) return false;
        victim.motionX = 0;
        victim.motionY = 0;
        victim.motionZ = 0;
        victim.velocityChanged = true;
        return true;
    }

    // ==========================================
    // AOE效果
    // ==========================================

    private static boolean effectAOEDamage(EntityLivingBase attacker, float damage, EffectConfig config) {
        double radius = config.getDouble("radius", 5.0);
        float aoeDamage = damage * config.value;
        String damageType = config.getString("damageType", "magic");

        if ("true".equals(damageType)) {
            return CombatHelper.sweepTrue(wrap(attacker), radius, aoeDamage, true) > 0;
        } else {
            // 使用AreaHelper造成可配置伤害
            AreaHelper.damageArea(
                    wrap(attacker.world),
                    attacker.posX, attacker.posY, attacker.posZ,
                    radius, aoeDamage,
                    wrapLiving(attacker)
            );
            return true;
        }
    }

    private static boolean effectChainDamage(EntityLivingBase attacker, EntityLivingBase victim,
                                             float damage, EffectConfig config) {
        if (victim == null) return false;

        int chainCount = config.getInt("chainCount", 3);
        float chainRadius = config.getFloat("chainRadius", 8.0f);
        float damageDecay = config.getFloat("damageDecay", 0.7f);
        String damageType = config.getString("damageType", "lightning");

        World world = attacker.world;
        EntityLivingBase current = victim;
        float currentDamage = damage * config.value;
        final EntityLivingBase finalAttacker = attacker;

        for (int i = 0; i < chainCount; i++) {
            // 寻找下一个目标
            final EntityLivingBase finalCurrent = current;
            AxisAlignedBB box = current.getEntityBoundingBox().grow(chainRadius);
            List<EntityLivingBase> nearby = world.getEntitiesWithinAABB(
                    EntityLivingBase.class, box,
                    e -> e != finalCurrent && e != finalAttacker && e.isEntityAlive()
            );

            if (nearby.isEmpty()) break;

            EntityLivingBase next = nearby.get(0);

            // 造成可配置伤害类型
            DamageHelper.dealCustomDamage(
                    wrapLiving(next),
                    wrapLiving(attacker),
                    currentDamage,
                    damageType
            );

            // 粒子效果
            String particle = config.getString("particle", "FIREWORKS_SPARK");
            drawLightningBetween(current, next, particle);

            current = next;
            currentDamage *= damageDecay;
        }

        return true;
    }

    private static boolean effectExplosion(EntityLivingBase attacker, EntityLivingBase victim,
                                           EffectConfig config) {
        if (victim == null) return false;

        float radius = config.getFloat("radius", 3.0f);
        float damage = config.getFloat("damage", 10.0f);
        String damageType = config.getString("damageType", "explosion");
        boolean blockDamage = config.getBoolean("blockDamage", false);

        World world = victim.world;
        world.createExplosion(
                attacker,
                victim.posX, victim.posY, victim.posZ,
                radius,
                blockDamage
        );

        // AOE伤害
        AreaHelper.damageArea(
                wrap(world),
                victim.posX, victim.posY, victim.posZ,
                radius, damage,
                wrapLiving(attacker)
        );

        return true;
    }

    // ==========================================
    // 剑气系统 (完全可配置)
    // ==========================================

    private static boolean effectSwordBeam(EntityLivingBase attacker, float damage, EffectConfig config) {
        if (!(attacker instanceof EntityPlayer)) return false;

        float multiplier = config.value;
        float speed = config.getFloat("speed", 2.0f);
        String beamType = config.getString("beamType", "normal");

        // 解析剑气类型
        SwordBeamType type = SwordBeamType.fromString(beamType);

        // 自定义参数
        float red = config.getFloat("red", -1);
        float green = config.getFloat("green", -1);
        float blue = config.getFloat("blue", -1);
        float scale = config.getFloat("scale", 1.0f);
        int penetrate = config.getInt("penetrate", 0);

        if (red >= 0 && green >= 0 && blue >= 0) {
            // 完全自定义剑气
            SwordBeamHelper.shootCustomBeam(
                    wrap((EntityPlayer) attacker),
                    beamType,
                    0, // baseDamage会自动加武器伤害
                    speed,
                    red, green, blue,
                    scale, penetrate
            );
        } else {
            // 使用倍率
            SwordBeamHelper.shootBeamWithMultiplier(
                    wrap((EntityPlayer) attacker),
                    multiplier,
                    speed
            );
        }

        return true;
    }

    private static boolean effectMultiBeam(EntityLivingBase attacker, float damage, EffectConfig config) {
        if (!(attacker instanceof EntityPlayer)) return false;

        int count = config.getInt("count", 3);
        float spreadAngle = config.getFloat("spreadAngle", 15.0f);
        float speed = config.getFloat("speed", 2.0f);

        SwordBeamHelper.shootMultiBeam(
                wrap((EntityPlayer) attacker),
                count,
                spreadAngle,
                0, // baseDamage会自动加武器伤害
                speed
        );

        return true;
    }

    private static boolean effectCircleBeam(EntityLivingBase attacker, float damage, EffectConfig config) {
        if (!(attacker instanceof EntityPlayer)) return false;

        int count = config.getInt("count", 8);
        float speed = config.getFloat("speed", 2.0f);

        SwordBeamHelper.shootCircleBeam(
                wrap((EntityPlayer) attacker),
                count,
                0, // baseDamage会自动加武器伤害
                speed
        );

        return true;
    }

    // ==========================================
    // 传送系统
    // ==========================================

    private static boolean effectTeleportForward(EntityLivingBase attacker, EffectConfig config) {
        if (!(attacker instanceof EntityPlayer)) return false;

        double distance = config.getDouble("distance", 10.0);

        return TeleportHelper.teleportToLookPos(
                wrap((EntityPlayer) attacker),
                wrap(attacker.world),
                distance
        );
    }

    private static boolean effectTeleportBehind(EntityLivingBase attacker, EntityLivingBase victim,
                                                EffectConfig config) {
        if (victim == null || !(attacker instanceof EntityPlayer)) return false;

        // 传送到目标背后
        double angle = Math.toRadians(victim.rotationYaw);
        double distance = config.getDouble("distance", 2.0);

        double x = victim.posX - Math.sin(angle) * distance;
        double z = victim.posZ + Math.cos(angle) * distance;

        TeleportHelper.teleportTo(
                wrap((EntityPlayer) attacker),
                x, victim.posY, z
        );

        return true;
    }

    private static boolean effectTeleportRandom(EntityLivingBase attacker, EffectConfig config) {
        if (!(attacker instanceof EntityPlayer)) return false;

        double range = config.getDouble("range", 10.0);

        double angle = RANDOM.nextDouble() * Math.PI * 2;
        double distance = RANDOM.nextDouble() * range;

        double x = attacker.posX + Math.cos(angle) * distance;
        double z = attacker.posZ + Math.sin(angle) * distance;

        TeleportHelper.teleportTo(
                wrap((EntityPlayer) attacker),
                x, attacker.posY, z
        );

        return true;
    }

    private static boolean effectBlink(EntityLivingBase attacker, EffectConfig config) {
        // 短距离快速传送
        return effectTeleportForward(attacker, config.param("distance", 5.0));
    }

    // ==========================================
    // 防御效果
    // ==========================================

    private static boolean effectThorns(EntityLivingBase victim, EntityLivingBase attacker,
                                        float damage, EffectConfig config) {
        if (attacker == null) return false;

        float thornsDamage = damage * config.value;
        String damageType = config.getString("damageType", "thorns");

        DamageHelper.dealCustomDamage(
                wrapLiving(attacker),
                wrapLiving(victim),
                thornsDamage,
                damageType
        );
        return true;
    }

    private static boolean effectAbsorbSteal(EntityLivingBase attacker, EntityLivingBase victim,
                                             float damage, EffectConfig config) {
        if (victim == null) return false;

        float stealAmount = damage * config.value;
        float stolen = CombatHelper.damageAbsorbOnly(wrap(victim), stealAmount);

        if (stolen > 0) {
            float current = CombatHelper.getAbsorption(wrap(attacker));
            CombatHelper.setAbsorption(wrap(attacker), current + stolen);
            return true;
        }

        return false;
    }

    // ==========================================
    // 药水效果 (完全可配置)
    // ==========================================

    private static boolean effectPotion(EntityLivingBase target, EffectConfig config) {
        String potionId = config.getString("potionId", "minecraft:speed");
        int duration = config.getInt("duration", 100);
        int amplifier = config.getInt("amplifier", 0);

        return CombatHelper.addEffect(wrap(target), potionId, duration, amplifier, false, true);
    }

    private static boolean effectRandomPotion(EntityLivingBase target, EffectConfig config) {
        String[] potions = {
                "minecraft:speed", "minecraft:strength", "minecraft:regeneration",
                "minecraft:resistance", "minecraft:fire_resistance"
        };

        String randomPotion = potions[RANDOM.nextInt(potions.length)];
        int duration = config.getInt("duration", 100);

        return CombatHelper.addEffect(wrap(target), randomPotion, duration, 0, false, true);
    }

    // ==========================================
    // 召唤系统
    // ==========================================

    private static boolean effectSummonEntity(EntityLivingBase attacker, EffectConfig config) {
        String entityId = config.getString("entityId", "minecraft:zombie");
        int count = config.getInt("count", 1);
        double radius = config.getDouble("radius", 3.0);

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            double x = attacker.posX + Math.cos(angle) * radius;
            double z = attacker.posZ + Math.sin(angle) * radius;

            EntityHelper.spawnEntity(
                    wrap(attacker.world),
                    entityId,
                    x, attacker.posY, z
            );
        }

        return true;
    }

    private static boolean effectSummonAlly(EntityLivingBase attacker, EffectConfig config) {
        // 召唤友方生物(如狼、铁傀儡等)
        return effectSummonEntity(attacker, config.param("entityId", "minecraft:wolf"));
    }

    // ==========================================
    // 繁殖系统 (LoveHelper)
    // ==========================================

    private static boolean effectAnimalLove(EntityLivingBase target, EffectConfig config) {
        if (!LoveHelper.isAnimal(wrap(target))) return false;

        return LoveHelper.setAnimalInLoveEntity(wrap(target));
    }

    private static boolean effectVillagerMate(EntityLivingBase target, EffectConfig config) {
        if (!LoveHelper.isVillager(wrap(target))) return false;

        double radius = config.getDouble("radius", 10.0);
        return LoveHelper.pairVillagersEntity(wrap(target), radius);
    }

    // ==========================================
    // 音效/粒子
    // ==========================================

    private static boolean effectSound(EntityLivingBase entity, EffectConfig config) {
        String soundId = config.getString("sound", "entity.player.attack.crit");
        float volume = config.getFloat("volume", 1.0f);
        float pitch = config.getFloat("pitch", 1.0f);

        if (entity instanceof EntityPlayer) {
            SoundHelper.playSound(
                    wrap(entity.world),
                    wrap((EntityPlayer) entity),
                    soundId, volume, pitch
            );
        } else {
            SoundHelper.playSoundAt(
                    wrap(entity.world),
                    entity.posX, entity.posY, entity.posZ,
                    soundId, volume, pitch
            );
        }

        return true;
    }

    private static boolean effectParticle(EntityLivingBase entity, EffectConfig config) {
        String particleType = config.getString("particle", "CRIT");
        int count = config.getInt("count", 20);
        double radius = config.getDouble("radius", 1.0);

        AreaHelper.fillAreaWithParticles(
                wrap(entity.world),
                entity.posX, entity.posY + entity.height / 2, entity.posZ,
                radius, particleType, count
        );

        return true;
    }

    private static boolean effectParticleTrail(EntityLivingBase attacker, EntityLivingBase victim,
                                               EffectConfig config) {
        if (victim == null) return false;

        String particleType = config.getString("particle", "FIREWORKS_SPARK");
        drawLightningBetween(attacker, victim, particleType);

        return true;
    }

    // ==========================================
    // 标记系统
    // ==========================================

    private static boolean effectMark(EntityLivingBase target, EffectConfig config) {
        String markKey = config.getString("markKey", "default");
        int duration = config.getInt("duration", 100);

        return CombatHelper.mark(wrap(target), markKey, duration);
    }

    private static boolean effectConsumeMark(EntityLivingBase target, float damage, EffectConfig config) {
        String markKey = config.getString("markKey", "default");

        if (CombatHelper.consumeMark(wrap(target), markKey)) {
            float bonusDamage = damage * config.value;
            return CombatHelper.trueDamage(wrap(target), bonusDamage, null) > 0;
        }

        return false;
    }

    // ==========================================
    // 特殊效果
    // ==========================================

    private static boolean effectDash(EntityLivingBase attacker, EffectConfig config) {
        double speed = config.getDouble("speed", 1.0);
        double yBoost = config.getDouble("yBoost", 0.2);

        return CombatHelper.dashForward(wrap(attacker), speed, yBoost);
    }

    private static boolean effectLeap(EntityLivingBase attacker, EffectConfig config) {
        double power = config.getDouble("power", 1.0);

        attacker.motionY += power;
        attacker.velocityChanged = true;

        return true;
    }

    private static boolean effectRage(EntityLivingBase attacker, EffectConfig config) {
        int duration = config.getInt("duration", 200);

        // 给予多个增益
        CombatHelper.addEffect(wrap(attacker), "minecraft:strength", duration, 1, false, false);
        CombatHelper.addEffect(wrap(attacker), "minecraft:speed", duration, 1, false, false);
        CombatHelper.addEffect(wrap(attacker), "minecraft:resistance", duration, 0, false, false);

        return true;
    }

    private static boolean effectShield(EntityLivingBase attacker, EffectConfig config) {
        float amount = config.getFloat("amount", 4.0f);

        float current = CombatHelper.getAbsorption(wrap(attacker));
        CombatHelper.setAbsorption(wrap(attacker), current + amount);

        return true;
    }
    private static boolean effectIceFireFire(EntityLivingBase victim, EntityLivingBase attacker,
                                             float damage, EffectConfig config) {
        if (victim == null || attacker == null) return false;

        int fireDuration = config.getInt("fireDuration", 5);
        float knockback = config.getFloat("knockback", 1.0f);
        float dragonBonus = config.getFloat("dragonBonus", 8.0f);

        try {
            return IceAndFireHelper.applyFireEffect(
                    wrapLiving(victim),
                    wrapLiving(attacker),
                    fireDuration,
                    knockback,
                    dragonBonus
            );
        } catch (Exception e) {
            // Ice and Fire 不存在时的降级方案
            victim.setFire(fireDuration);
            victim.knockBack(victim, knockback,
                    attacker.posX - victim.posX,
                    attacker.posZ - victim.posZ);
            return true;
        }
    }

    /**
     * 冰龙效果 - 冰冻+减速+挖掘疲劳+击退+对火龙额外伤害
     */
    private static boolean effectIceFireIce(EntityLivingBase victim, EntityLivingBase attacker,
                                            float damage, EffectConfig config) {
        if (victim == null || attacker == null) return false;

        int frozenTicks = config.getInt("frozenTicks", 200);
        int slowDuration = config.getInt("slowDuration", 100);
        float knockback = config.getFloat("knockback", 1.0f);
        float dragonBonus = config.getFloat("dragonBonus", 8.0f);

        try {
            return IceAndFireHelper.applyIceEffect(
                    wrapLiving(victim),
                    wrapLiving(attacker),
                    frozenTicks,
                    slowDuration,
                    knockback,
                    dragonBonus
            );
        } catch (Exception e) {
            // Ice and Fire 不存在时的降级方案
            CombatHelper.addEffect(wrapLiving(victim), "minecraft:slowness", slowDuration, 2, false, false);
            CombatHelper.addEffect(wrapLiving(victim), "minecraft:mining_fatigue", slowDuration, 2, false, false);
            victim.knockBack(victim, knockback,
                    attacker.posX - victim.posX,
                    attacker.posZ - victim.posZ);
            return true;
        }
    }

    /**
     * 雷电效果 - 闪电链+击退+对龙额外伤害
     */
    private static boolean effectIceFireLightning(EntityLivingBase victim, EntityLivingBase attacker,
                                                  float damage, EffectConfig config) {
        if (victim == null || attacker == null) return false;

        float knockback = config.getFloat("knockback", 1.0f);
        float dragonBonus = config.getFloat("dragonBonus", 4.0f);

        try {
            return IceAndFireHelper.applyLightningEffect(
                    wrapLiving(victim),
                    wrapLiving(attacker),
                    knockback,
                    dragonBonus
            );
        } catch (Exception e) {
            // Ice and Fire 不存在时的降级方案
            if (!victim.world.isRemote) {
                victim.world.addWeatherEffect(new net.minecraft.entity.effect.EntityLightningBolt(
                        victim.world, victim.posX, victim.posY, victim.posZ, false
                ));
            }
            victim.knockBack(victim, knockback,
                    attacker.posX - victim.posX,
                    attacker.posZ - victim.posZ);
            return true;
        }
    }

    /**
     * 三重元素效果 - 火+冰+雷
     */
    private static boolean effectIceFireTriple(EntityLivingBase victim, EntityLivingBase attacker,
                                               float damage, EffectConfig config) {
        if (victim == null || attacker == null) return false;

        try {
            return IceAndFireHelper.applyTripleEffect(
                    wrapLiving(victim),
                    wrapLiving(attacker)
            );
        } catch (Exception e) {
            // 降级方案：简化版
            victim.setFire(3);
            CombatHelper.addEffect(wrapLiving(victim), "minecraft:slowness", 60, 1, false, false);
            if (!victim.world.isRemote && RANDOM.nextFloat() < 0.3f) {
                victim.world.addWeatherEffect(new net.minecraft.entity.effect.EntityLightningBolt(
                        victim.world, victim.posX, victim.posY, victim.posZ, false
                ));
            }
            return true;
        }
    }





































    // ==========================================
    // 工具方法
    // ==========================================

    private static void drawLightningBetween(EntityLivingBase from, EntityLivingBase to, String particleType) {
        World world = from.world;
        if (world.isRemote) return;

        double dx = to.posX - from.posX;
        double dy = to.posY - from.posY;
        double dz = to.posZ - from.posZ;
        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);

        int steps = (int) (distance * 2);
        for (int i = 0; i < steps; i++) {
            double t = i / (double) steps;
            double x = from.posX + dx * t;
            double y = from.posY + dy * t + from.height / 2;
            double z = from.posZ + dz * t;

            AreaHelper.fillAreaWithParticles(
                    wrap(world),
                    x, y, z,
                    0.2, particleType, 2
            );
        }
    }

    private static crafttweaker.api.entity.IEntity wrap(Entity entity) {
        return crafttweaker.api.minecraft.CraftTweakerMC.getIEntity(entity);
    }

    private static crafttweaker.api.entity.IEntityLivingBase wrapLiving(EntityLivingBase entity) {
        return crafttweaker.api.minecraft.CraftTweakerMC.getIEntityLivingBase(entity);
    }

    private static crafttweaker.api.player.IPlayer wrap(EntityPlayer player) {
        return crafttweaker.api.minecraft.CraftTweakerMC.getIPlayer(player);
    }

    private static crafttweaker.api.world.IWorld wrap(World world) {
        return crafttweaker.api.minecraft.CraftTweakerMC.getIWorld(world);
    }private static boolean effectReduceIframes(EntityLivingBase victim, EffectConfig config) {
        if (victim == null) return false;

        float reductionPercent = config.value; // 0.0-1.0

        // 获取当前无敌时间
        int currentIframes = victim.hurtResistantTime;

        // 计算减少后的无敌时间
        int reducedIframes = (int) (currentIframes * (1.0f - reductionPercent));

        // 设置新的无敌时间（至少保留1 tick以防止同一tick多次伤害）
        victim.hurtResistantTime = Math.max(1, reducedIframes);

        // 可选: 播放音效
        if (config.getBoolean("sound", true)) {
            victim.world.playSound(null, victim.posX, victim.posY, victim.posZ,
                    net.minecraft.init.SoundEvents.BLOCK_GLASS_BREAK,
                    net.minecraft.util.SoundCategory.PLAYERS, 0.3f, 1.5f);
        }

        // 可选: 播放粒子效果
        if (config.getBoolean("particle", true) && victim.world instanceof net.minecraft.world.WorldServer) {
            ((net.minecraft.world.WorldServer) victim.world).spawnParticle(
                    net.minecraft.util.EnumParticleTypes.CRIT_MAGIC,
                    victim.posX, victim.posY + victim.height / 2, victim.posZ,
                    3, 0.2, 0.2, 0.2, 0.05
            );
        }

        return true;
    }

    /**
     * 完全忽略无敌帧
     *
     * 直接将目标无敌时间设为0,允许立即再次攻击
     */
    private static boolean effectIgnoreIframes(EntityLivingBase victim, EffectConfig config) {
        if (victim == null) return false;

        // 直接清除无敌帧
        victim.hurtResistantTime = 0;
        victim.hurtTime = 0;

        // 播放特殊音效
        if (config.getBoolean("sound", true)) {
            victim.world.playSound(null, victim.posX, victim.posY, victim.posZ,
                    net.minecraft.init.SoundEvents.ENTITY_ITEM_BREAK,
                    net.minecraft.util.SoundCategory.PLAYERS, 0.5f, 2.0f);
        }

        // 播放特殊粒子效果
        if (config.getBoolean("particle", true) && victim.world instanceof net.minecraft.world.WorldServer) {
            ((net.minecraft.world.WorldServer) victim.world).spawnParticle(
                    net.minecraft.util.EnumParticleTypes.CRIT,
                    victim.posX, victim.posY + victim.height / 2, victim.posZ,
                    8, 0.3, 0.3, 0.3, 0.1
            );
        }

        return true;
    }

    /**
     * 穿透无敌帧
     *
     * 允许在无敌帧期间造成部分伤害
     * value: 穿透百分比 (0.0-1.0)
     *
     * 注意: 这个效果需要配合伤害事件使用
     */
    private static boolean effectIframePenetration(EntityLivingBase victim, EffectConfig config) {
        if (victim == null) return false;

        float penetrationPercent = config.value; // 0.0-1.0

        // 在实体NBT中标记穿透百分比，供伤害处理时读取
        victim.getEntityData().setFloat("moremod$iframe_penetration", penetrationPercent);

        // 设置过期时间（1秒后清除标记）
        victim.getEntityData().setLong("moremod$iframe_penetration_expire",
                victim.world.getTotalWorldTime() + 20);

        return true;
    }

}