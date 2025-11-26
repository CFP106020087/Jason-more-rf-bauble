package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.entity.IEntityLivingBase;
import crafttweaker.api.player.IPlayer;
import crafttweaker.api.world.IWorld;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.world.World;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;

/**
 * 伤害助手类
 * 提供创建自定义伤害、造成各种类型伤害的功能
 */
@ZenRegister
@ZenClass("mods.moremod.DamageHelper")
public class DamageHelper {

    /**
     * 造成自定义伤害
     * 
     * @param target 目标实体
     * @param attacker 攻击者（可为 null）
     * @param amount 伤害数值
     * @param damageType 伤害类型标识符
     */
    @ZenMethod
    public static void dealCustomDamage(IEntityLivingBase target, 
                                       IEntityLivingBase attacker,
                                       float amount, String damageType) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        Entity mcAttacker = attacker != null ? CraftTweakerMC.getEntityLivingBase(attacker) : null;
        
        DamageSource source;
        if (mcAttacker != null) {
            source = new EntityDamageSource(damageType, mcAttacker);
        } else {
            source = new DamageSource(damageType);
        }
        
        mcTarget.attackEntityFrom(source, amount);
    }
    
    /**
     * 造成真实伤害（无视护甲）
     * 
     * @param target 目标实体
     * @param amount 伤害数值
     */
    @ZenMethod
    public static void dealTrueDamage(IEntityLivingBase target, float amount) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        DamageSource source = DamageSource.OUT_OF_WORLD.setDamageBypassesArmor();
        mcTarget.attackEntityFrom(source, amount);
    }
    
    /**
     * 造成魔法伤害
     * 
     * @param target 目标实体
     * @param attacker 攻击者
     * @param amount 伤害数值
     */
    @ZenMethod
    public static void dealMagicDamage(IEntityLivingBase target,
                                      IEntityLivingBase attacker,
                                      float amount) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        Entity mcAttacker = attacker != null ? CraftTweakerMC.getEntityLivingBase(attacker) : null;
        
        DamageSource source;
        if (mcAttacker != null) {
            source = DamageSource.causeIndirectMagicDamage(mcAttacker, mcAttacker);
        } else {
            source = DamageSource.MAGIC;
        }
        
        mcTarget.attackEntityFrom(source, amount);
    }
    
    /**
     * 造成火焰伤害
     * 
     * @param target 目标实体
     * @param attacker 攻击者
     * @param amount 伤害数值
     */
    @ZenMethod
    public static void dealFireDamage(IEntityLivingBase target,
                                     IEntityLivingBase attacker,
                                     float amount) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        Entity mcAttacker = attacker != null ? CraftTweakerMC.getEntityLivingBase(attacker) : null;
        
        DamageSource source;
        if (mcAttacker != null) {
            source = (new EntityDamageSource("onFire", mcAttacker)).setFireDamage();
        } else {
            source = DamageSource.IN_FIRE;
        }
        
        mcTarget.attackEntityFrom(source, amount);
    }
    
    /**
     * 造成爆炸伤害
     * 
     * @param target 目标实体
     * @param attacker 攻击者
     * @param amount 伤害数值
     */
    @ZenMethod
    public static void dealExplosionDamage(IEntityLivingBase target,
                                          IEntityLivingBase attacker,
                                          float amount) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        EntityLivingBase mcAttacker = attacker != null ? CraftTweakerMC.getEntityLivingBase(attacker) : null;
        
        DamageSource source = DamageSource.causeExplosionDamage(mcAttacker);
        mcTarget.attackEntityFrom(source, amount);
    }
    
    /**
     * 造成投射物伤害
     * 
     * @param target 目标实体
     * @param attacker 攻击者
     * @param amount 伤害数值
     */
    @ZenMethod
    public static void dealProjectileDamage(IEntityLivingBase target,
                                           IEntityLivingBase attacker,
                                           float amount) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        Entity mcAttacker = attacker != null ? CraftTweakerMC.getEntityLivingBase(attacker) : null;
        
        DamageSource source;
        if (mcAttacker != null) {
            source = new EntityDamageSourceIndirect("arrow", mcAttacker, mcAttacker);
        } else {
            source = new DamageSource("arrow");
        }
        
        mcTarget.attackEntityFrom(source, amount);
    }
    
    /**
     * 造成范围伤害（AOE）
     * 
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param amount 伤害数值
     * @param attacker 攻击者
     */
    @ZenMethod
    public static void dealAOEDamage(IWorld world,
                                    double x, double y, double z,
                                    double radius, float amount,
                                    IEntityLivingBase attacker) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        Entity mcAttacker = attacker != null ? CraftTweakerMC.getEntityLivingBase(attacker) : null;
        
        DamageSource source;
        if (mcAttacker != null) {
            source = new EntityDamageSource("aoe", mcAttacker);
        } else {
            source = new DamageSource("aoe");
        }
        
        for (EntityLivingBase entity : mcWorld.getEntitiesWithinAABB(EntityLivingBase.class,
                new net.minecraft.util.math.AxisAlignedBB(
                    x - radius, y - radius, z - radius,
                    x + radius, y + radius, z + radius))) {
            
            double distance = entity.getDistance(x, y, z);
            if (distance <= radius) {
                // 根据距离衰减伤害
                float damageMultiplier = (float) (1.0 - (distance / radius));
                float finalDamage = amount * damageMultiplier;
                entity.attackEntityFrom(source, finalDamage);
            }
        }
    }
    
    /**
     * 造成持续伤害（DoT - Damage over Time）
     * 
     * @param target 目标实体
     * @param damagePerTick 每次伤害数值
     * @param duration 持续时间（tick）
     * @param interval 间隔时间（tick）
     */
    @ZenMethod
    public static void dealDamageOverTime(IEntityLivingBase target,
                                         float damagePerTick,
                                         int duration, int interval) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        
        // 使用凋零效果模拟 DoT
        // 注意：这是简化实现，完整实现需要自定义 TickHandler
        int amplifier = Math.max(0, (int)(damagePerTick / 0.5f) - 1);
        mcTarget.addPotionEffect(new PotionEffect(MobEffects.WITHER, duration, amplifier));
    }
    
    /**
     * 造成流血伤害（持续伤害，带视觉效果）
     * 
     * @param target 目标实体
     * @param damagePerSecond 每秒伤害
     * @param durationSeconds 持续秒数
     */
    @ZenMethod
    public static void dealBleedDamage(IEntityLivingBase target,
                                      float damagePerSecond,
                                      int durationSeconds) {
        // 使用凋零效果 + 自定义标记
        int duration = durationSeconds * 20;
        int amplifier = Math.max(0, (int)(damagePerSecond / 0.5f) - 1);
        
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        mcTarget.addPotionEffect(new PotionEffect(MobEffects.WITHER, duration, amplifier));
    }
    
    /**
     * 造成中毒伤害
     * 
     * @param target 目标实体
     * @param damagePerSecond 每秒伤害
     * @param durationSeconds 持续秒数
     */
    @ZenMethod
    public static void dealPoisonDamage(IEntityLivingBase target,
                                       float damagePerSecond,
                                       int durationSeconds) {
        int duration = durationSeconds * 20;
        int amplifier = Math.max(0, (int)(damagePerSecond / 0.375f) - 1);
        
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        mcTarget.addPotionEffect(new PotionEffect(MobEffects.POISON, duration, amplifier));
    }
    
    /**
     * 造成无法防御的伤害（绕过所有防御）
     * 
     * @param target 目标实体
     * @param amount 伤害数值
     */
    @ZenMethod
    public static void dealAbsoluteDamage(IEntityLivingBase target, float amount) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        DamageSource source = new DamageSource("absolute")
            .setDamageBypassesArmor()
            .setDamageIsAbsolute();
        mcTarget.attackEntityFrom(source, amount);
    }
    
    /**
     * 造成基于最大生命值的伤害
     * 
     * @param target 目标实体
     * @param attacker 攻击者
     * @param percentage 最大生命值百分比（0.0 - 1.0）
     */
    @ZenMethod
    public static void dealPercentDamage(IEntityLivingBase target,
                                        IEntityLivingBase attacker,
                                        float percentage) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        float maxHealth = mcTarget.getMaxHealth();
        float damage = maxHealth * percentage;
        
        Entity mcAttacker = attacker != null ? CraftTweakerMC.getEntityLivingBase(attacker) : null;
        DamageSource source;
        if (mcAttacker != null) {
            source = new EntityDamageSource("percent", mcAttacker);
        } else {
            source = new DamageSource("percent");
        }
        
        mcTarget.attackEntityFrom(source, damage);
    }
    
    /**
     * 造成基于当前生命值的伤害
     * 
     * @param target 目标实体
     * @param attacker 攻击者
     * @param percentage 当前生命值百分比
     */
    @ZenMethod
    public static void dealCurrentHealthDamage(IEntityLivingBase target,
                                               IEntityLivingBase attacker,
                                               float percentage) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        float currentHealth = mcTarget.getHealth();
        float damage = currentHealth * percentage;
        
        Entity mcAttacker = attacker != null ? CraftTweakerMC.getEntityLivingBase(attacker) : null;
        DamageSource source;
        if (mcAttacker != null) {
            source = new EntityDamageSource("currentHealth", mcAttacker);
        } else {
            source = new DamageSource("currentHealth");
        }
        
        mcTarget.attackEntityFrom(source, damage);
    }
    
    /**
     * 检查伤害来源类型
     * 
     * @param damageSource 伤害来源字符串
     * @param type 类型（"fire", "magic", "explosion", "projectile" 等）
     * @return 是否匹配
     */
    @ZenMethod
    public static boolean isDamageType(String damageSource, String type) {
        type = type.toLowerCase();
        damageSource = damageSource.toLowerCase();
        
        switch (type) {
            case "fire":
                return damageSource.contains("fire") || damageSource.contains("lava");
            case "magic":
                return damageSource.contains("magic") || damageSource.contains("indirect");
            case "explosion":
                return damageSource.contains("explosion");
            case "projectile":
                return damageSource.contains("arrow") || damageSource.contains("projectile");
            case "fall":
                return damageSource.contains("fall");
            case "drowning":
                return damageSource.contains("drown");
            default:
                return damageSource.contains(type);
        }
    }
    
    /**
     * 设置目标着火
     * 
     * @param target 目标实体
     * @param seconds 着火秒数
     */
    @ZenMethod
    public static void setOnFire(IEntityLivingBase target, int seconds) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        mcTarget.setFire(seconds);
    }
    
    /**
     * 治疗实体
     * 
     * @param target 目标实体
     * @param amount 治疗数值
     */
    @ZenMethod
    public static void heal(IEntityLivingBase target, float amount) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        mcTarget.heal(amount);
    }
    
    /**
     * 获取实体当前生命值
     * 
     * @param entity 实体
     * @return 当前生命值
     */
    @ZenMethod
    public static float getHealth(IEntityLivingBase entity) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
        return mcEntity.getHealth();
    }
    
    /**
     * 获取实体最大生命值
     * 
     * @param entity 实体
     * @return 最大生命值
     */
    @ZenMethod
    public static float getMaxHealth(IEntityLivingBase entity) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
        return mcEntity.getMaxHealth();
    }
    
    /**
     * 设置实体生命值
     * 
     * @param entity 实体
     * @param health 生命值
     */
    @ZenMethod
    public static void setHealth(IEntityLivingBase entity, float health) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
        mcEntity.setHealth(Math.min(health, mcEntity.getMaxHealth()));
    }
    
    /**
     * 检查实体是否无敌
     * 
     * @param entity 实体
     * @return 是否无敌
     */
    @ZenMethod
    public static boolean isInvulnerable(IEntityLivingBase entity) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
        return mcEntity.isEntityInvulnerable(DamageSource.GENERIC);
    }
}
