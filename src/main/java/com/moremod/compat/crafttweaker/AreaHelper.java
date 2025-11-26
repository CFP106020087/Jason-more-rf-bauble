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
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.init.MobEffects;

/**
 * 区域效果助手类
 * 提供 AOE（Area of Effect）范围效果功能
 */
@ZenRegister
@ZenClass("mods.moremod.AreaHelper")
public class AreaHelper {

    /**
     * 对范围内所有实体造成伤害
     * 
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param damage 伤害数值
     * @param attacker 攻击者（可为 null）
     */
    @ZenMethod
    public static void damageArea(IWorld world,
                                 double x, double y, double z,
                                 double radius, float damage,
                                 IEntityLivingBase attacker) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        Entity mcAttacker = attacker != null ? CraftTweakerMC.getEntityLivingBase(attacker) : null;
        DamageSource source = mcAttacker != null ? 
            new net.minecraft.util.EntityDamageSource("aoe", mcAttacker) :
            new DamageSource("aoe");
        
        AxisAlignedBB box = new AxisAlignedBB(
            x - radius, y - radius, z - radius,
            x + radius, y + radius, z + radius
        );
        
        for (EntityLivingBase entity : mcWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (entity == mcAttacker) continue;
            
            double distance = entity.getDistance(x, y, z);
            if (distance <= radius) {
                // 根据距离衰减伤害
                float damageMultiplier = (float) (1.0 - (distance / radius) * 0.5);
                float finalDamage = damage * damageMultiplier;
                entity.attackEntityFrom(source, finalDamage);
            }
        }
    }
    
    /**
     * 对范围内所有敌对生物造成伤害
     * 
     * @param world 世界
     * @param player 玩家
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param damage 伤害数值
     */
    @ZenMethod
    public static void damageHostileArea(IWorld world, IPlayer player,
                                        double x, double y, double z,
                                        double radius, float damage) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        DamageSource source = new net.minecraft.util.EntityDamageSource("player", mcPlayer);
        
        AxisAlignedBB box = new AxisAlignedBB(
            x - radius, y - radius, z - radius,
            x + radius, y + radius, z + radius
        );
        
        for (EntityLivingBase entity : mcWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (entity == mcPlayer) continue;
            
            // 只攻击敌对生物
            if (entity instanceof EntityMob) {
                double distance = entity.getDistance(x, y, z);
                if (distance <= radius) {
                    float damageMultiplier = (float) (1.0 - (distance / radius) * 0.5);
                    float finalDamage = damage * damageMultiplier;
                    entity.attackEntityFrom(source, finalDamage);
                }
            }
        }
    }
    
    /**
     * 治疗范围内所有实体
     * 
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param healAmount 治疗数值
     */
    @ZenMethod
    public static void healArea(IWorld world,
                               double x, double y, double z,
                               double radius, float healAmount) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        AxisAlignedBB box = new AxisAlignedBB(
            x - radius, y - radius, z - radius,
            x + radius, y + radius, z + radius
        );
        
        for (EntityLivingBase entity : mcWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            double distance = entity.getDistance(x, y, z);
            if (distance <= radius) {
                entity.heal(healAmount);
            }
        }
    }
    
    /**
     * 对范围内所有实体施加药水效果
     * 
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param effectId 药水效果 ID
     * @param duration 持续时间（tick）
     * @param amplifier 效果等级
     */
    @ZenMethod
    public static void applyEffectInArea(IWorld world,
                                        double x, double y, double z,
                                        double radius, String effectId,
                                        int duration, int amplifier) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        // 获取药水效果
        Potion potion = Potion.getPotionFromResourceLocation(effectId);
        if (potion == null) return;
        
        AxisAlignedBB box = new AxisAlignedBB(
            x - radius, y - radius, z - radius,
            x + radius, y + radius, z + radius
        );
        
        for (EntityLivingBase entity : mcWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            double distance = entity.getDistance(x, y, z);
            if (distance <= radius) {
                entity.addPotionEffect(new PotionEffect(potion, duration, amplifier));
            }
        }
    }
    
    /**
     * 显示范围圈效果（粒子）
     * 
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param particleType 粒子类型
     */
    @ZenMethod
    public static void showAreaCircle(IWorld world,
                                     double x, double y, double z,
                                     double radius, String particleType) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        EnumParticleTypes particle = EnumParticleTypes.getByName(particleType);
        if (particle == null) return;
        
        WorldServer serverWorld = (WorldServer) mcWorld;
        int particleCount = (int) (radius * 32); // 根据半径调整粒子数量
        
        for (int i = 0; i < particleCount; i++) {
            double angle = 2 * Math.PI * i / particleCount;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            
            serverWorld.spawnParticle(particle,
                x + offsetX, y, z + offsetZ,
                1, 0.0, 0.0, 0.0, 0.0);
        }
    }
    
    /**
     * 显示范围球体效果（粒子）
     * 
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param particleType 粒子类型
     */
    @ZenMethod
    public static void showAreaSphere(IWorld world,
                                     double x, double y, double z,
                                     double radius, String particleType) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        EnumParticleTypes particle = EnumParticleTypes.getByName(particleType);
        if (particle == null) return;
        
        WorldServer serverWorld = (WorldServer) mcWorld;
        int particleCount = (int) (radius * radius * 16);
        
        for (int i = 0; i < particleCount; i++) {
            double theta = mcWorld.rand.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * mcWorld.rand.nextDouble() - 1);
            
            double offsetX = radius * Math.sin(phi) * Math.cos(theta);
            double offsetY = radius * Math.sin(phi) * Math.sin(theta);
            double offsetZ = radius * Math.cos(phi);
            
            serverWorld.spawnParticle(particle,
                x + offsetX, y + offsetY, z + offsetZ,
                1, 0.0, 0.0, 0.0, 0.0);
        }
    }
    
    /**
     * 击退范围内所有实体
     * 
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param strength 击退力度
     */
    @ZenMethod
    public static void knockbackArea(IWorld world,
                                    double x, double y, double z,
                                    double radius, double strength) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        AxisAlignedBB box = new AxisAlignedBB(
            x - radius, y - radius, z - radius,
            x + radius, y + radius, z + radius
        );
        
        for (EntityLivingBase entity : mcWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            double distance = entity.getDistance(x, y, z);
            if (distance <= radius && distance > 0) {
                double dx = entity.posX - x;
                double dy = entity.posY - y;
                double dz = entity.posZ - z;
                
                double knockbackStrength = strength * (1.0 - distance / radius);
                
                entity.motionX += (dx / distance) * knockbackStrength;
                entity.motionY += 0.4 * knockbackStrength;
                entity.motionZ += (dz / distance) * knockbackStrength;
                entity.velocityChanged = true;
            }
        }
    }
    
    /**
     * 吸引范围内所有实体
     * 
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param strength 吸引力度
     */
    @ZenMethod
    public static void pullArea(IWorld world,
                               double x, double y, double z,
                               double radius, double strength) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        AxisAlignedBB box = new AxisAlignedBB(
            x - radius, y - radius, z - radius,
            x + radius, y + radius, z + radius
        );
        
        for (EntityLivingBase entity : mcWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            double distance = entity.getDistance(x, y, z);
            if (distance <= radius && distance > 0) {
                double dx = x - entity.posX;
                double dy = y - entity.posY;
                double dz = z - entity.posZ;
                
                double pullStrength = strength * (1.0 - distance / radius);
                
                entity.motionX += (dx / distance) * pullStrength;
                entity.motionY += (dy / distance) * pullStrength;
                entity.motionZ += (dz / distance) * pullStrength;
                entity.velocityChanged = true;
            }
        }
    }
    
    /**
     * 冻结范围内所有实体
     * 
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param duration 持续时间（tick）
     */
    @ZenMethod
    public static void freezeArea(IWorld world,
                                 double x, double y, double z,
                                 double radius, int duration) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        AxisAlignedBB box = new AxisAlignedBB(
            x - radius, y - radius, z - radius,
            x + radius, y + radius, z + radius
        );
        
        for (EntityLivingBase entity : mcWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            double distance = entity.getDistance(x, y, z);
            if (distance <= radius) {
                // 使用缓慢效果模拟冻结
                entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, duration, 10));
                entity.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, duration, 10));
                
                // 停止移动
                entity.motionX = 0;
                entity.motionY = 0;
                entity.motionZ = 0;
                entity.velocityChanged = true;
            }
        }
    }
    
    /**
     * 在范围内生成多个粒子效果
     * 
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param particleType 粒子类型
     * @param count 粒子数量
     */
    @ZenMethod
    public static void fillAreaWithParticles(IWorld world,
                                            double x, double y, double z,
                                            double radius, String particleType,
                                            int count) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        EnumParticleTypes particle = EnumParticleTypes.getByName(particleType);
        if (particle == null) return;
        
        WorldServer serverWorld = (WorldServer) mcWorld;
        
        for (int i = 0; i < count; i++) {
            double offsetX = (mcWorld.rand.nextDouble() - 0.5) * radius * 2;
            double offsetY = (mcWorld.rand.nextDouble() - 0.5) * radius * 2;
            double offsetZ = (mcWorld.rand.nextDouble() - 0.5) * radius * 2;
            
            double distance = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
            if (distance <= radius) {
                serverWorld.spawnParticle(particle,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }
    
    /**
     * 获取范围内的实体数量
     * 
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @return 实体数量
     */
    @ZenMethod
    public static int getEntityCountInArea(IWorld world,
                                          double x, double y, double z,
                                          double radius) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        int count = 0;
        
        AxisAlignedBB box = new AxisAlignedBB(
            x - radius, y - radius, z - radius,
            x + radius, y + radius, z + radius
        );
        
        for (EntityLivingBase entity : mcWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            double distance = entity.getDistance(x, y, z);
            if (distance <= radius) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * 清除范围内所有敌对生物
     * 
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @return 清除的数量
     */
    @ZenMethod
    public static int clearHostileInArea(IWorld world,
                                        double x, double y, double z,
                                        double radius) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return 0;
        
        int count = 0;
        AxisAlignedBB box = new AxisAlignedBB(
            x - radius, y - radius, z - radius,
            x + radius, y + radius, z + radius
        );
        
        for (EntityLivingBase entity : mcWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (entity instanceof EntityMob) {
                double distance = entity.getDistance(x, y, z);
                if (distance <= radius) {
                    entity.setDead();
                    count++;
                }
            }
        }
        
        return count;
    }
}
