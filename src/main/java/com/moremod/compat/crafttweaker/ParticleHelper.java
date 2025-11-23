package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.world.IWorld;
import crafttweaker.api.player.IPlayer;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;

/**
 * 粒子效果助手类
 * 提供在 ZenScript 中生成各种粒子效果的功能
 */
@ZenRegister
@ZenClass("mods.moremod.ParticleHelper")
public class ParticleHelper {

    /**
     * 在指定位置生成粒子
     * 
     * @param world 世界
     * @param particleType 粒子类型（如 "portal", "flame", "heart" 等）
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @param count 粒子数量
     */
    @ZenMethod
    public static void spawnParticle(IWorld world, String particleType, 
                                     double x, double y, double z, int count) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        EnumParticleTypes particle = getParticleType(particleType);
        if (particle == null) return;
        
        WorldServer serverWorld = (WorldServer) mcWorld;
        for (int i = 0; i < count; i++) {
            double offsetX = (mcWorld.rand.nextDouble() - 0.5) * 0.5;
            double offsetY = mcWorld.rand.nextDouble() * 0.5;
            double offsetZ = (mcWorld.rand.nextDouble() - 0.5) * 0.5;
            
            serverWorld.spawnParticle(particle, 
                x + offsetX, y + offsetY, z + offsetZ, 
                1, 0.0, 0.0, 0.0, 0.0);
        }
    }
    
    /**
     * 在玩家位置生成粒子
     * 
     * @param player 玩家
     * @param particleType 粒子类型
     * @param count 粒子数量
     */
    @ZenMethod
    public static void spawnParticleAtPlayer(IPlayer player, String particleType, int count) {
        spawnParticle(player.getWorld(), particleType, player.getX(), player.getY(), player.getZ(), count);
    }
    
    /**
     * 生成圆形粒子效果
     * 
     * @param world 世界
     * @param particleType 粒子类型
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param count 粒子数量
     */
    @ZenMethod
    public static void spawnCircle(IWorld world, String particleType,
                                   double x, double y, double z, 
                                   double radius, int count) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        EnumParticleTypes particle = getParticleType(particleType);
        if (particle == null) return;
        
        WorldServer serverWorld = (WorldServer) mcWorld;
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            
            serverWorld.spawnParticle(particle,
                x + offsetX, y, z + offsetZ,
                1, 0.0, 0.0, 0.0, 0.0);
        }
    }
    
    /**
     * 生成粒子线（从点 A 到点 B）
     * 
     * @param world 世界
     * @param particleType 粒子类型
     * @param x1 起点 X
     * @param y1 起点 Y
     * @param z1 起点 Z
     * @param x2 终点 X
     * @param y2 终点 Y
     * @param z2 终点 Z
     * @param count 粒子数量
     */
    @ZenMethod
    public static void spawnLine(IWorld world, String particleType,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 int count) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        EnumParticleTypes particle = getParticleType(particleType);
        if (particle == null) return;
        
        WorldServer serverWorld = (WorldServer) mcWorld;
        for (int i = 0; i <= count; i++) {
            double progress = (double) i / count;
            double x = x1 + (x2 - x1) * progress;
            double y = y1 + (y2 - y1) * progress;
            double z = z1 + (z2 - z1) * progress;
            
            serverWorld.spawnParticle(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
    
    /**
     * 生成螺旋粒子效果
     * 
     * @param world 世界
     * @param particleType 粒子类型
     * @param x 中心 X 坐标
     * @param y 底部 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param height 高度
     * @param count 粒子数量
     */
    @ZenMethod
    public static void spawnHelix(IWorld world, String particleType,
                                  double x, double y, double z,
                                  double radius, double height, int count) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        EnumParticleTypes particle = getParticleType(particleType);
        if (particle == null) return;
        
        WorldServer serverWorld = (WorldServer) mcWorld;
        for (int i = 0; i < count; i++) {
            double progress = (double) i / count;
            double angle = progress * Math.PI * 4; // 2圈
            double currentRadius = radius * (1 - progress * 0.3); // 逐渐缩小
            
            double offsetX = Math.cos(angle) * currentRadius;
            double offsetZ = Math.sin(angle) * currentRadius;
            double offsetY = height * progress;
            
            serverWorld.spawnParticle(particle,
                x + offsetX, y + offsetY, z + offsetZ,
                1, 0.0, 0.0, 0.0, 0.0);
        }
    }
    
    /**
     * 生成球形粒子效果
     * 
     * @param world 世界
     * @param particleType 粒子类型
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param count 粒子数量
     */
    @ZenMethod
    public static void spawnSphere(IWorld world, String particleType,
                                   double x, double y, double z,
                                   double radius, int count) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        EnumParticleTypes particle = getParticleType(particleType);
        if (particle == null) return;
        
        WorldServer serverWorld = (WorldServer) mcWorld;
        for (int i = 0; i < count; i++) {
            // 使用球坐标生成均匀分布的点
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
     * 生成爆炸粒子效果（向外扩散）
     * 
     * @param world 世界
     * @param particleType 粒子类型
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param power 威力（影响扩散速度和范围）
     * @param count 粒子数量
     */
    @ZenMethod
    public static void spawnExplosion(IWorld world, String particleType,
                                      double x, double y, double z,
                                      double power, int count) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        EnumParticleTypes particle = getParticleType(particleType);
        if (particle == null) return;
        
        WorldServer serverWorld = (WorldServer) mcWorld;
        for (int i = 0; i < count; i++) {
            double velocityX = (mcWorld.rand.nextDouble() - 0.5) * power;
            double velocityY = (mcWorld.rand.nextDouble() - 0.5) * power;
            double velocityZ = (mcWorld.rand.nextDouble() - 0.5) * power;
            
            serverWorld.spawnParticle(particle, x, y, z, 1,
                velocityX, velocityY, velocityZ, 1.0);
        }
    }
    
    /**
     * 生成彩色粒子（redstone 粒子）
     * 
     * @param world 世界
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @param red 红色值 (0.0 - 1.0)
     * @param green 绿色值 (0.0 - 1.0)
     * @param blue 蓝色值 (0.0 - 1.0)
     * @param count 粒子数量
     */
    @ZenMethod
    public static void spawnColoredParticle(IWorld world, 
                                           double x, double y, double z,
                                           float red, float green, float blue,
                                           int count) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        WorldServer serverWorld = (WorldServer) mcWorld;
        for (int i = 0; i < count; i++) {
            double offsetX = (mcWorld.rand.nextDouble() - 0.5) * 0.5;
            double offsetY = mcWorld.rand.nextDouble() * 0.5;
            double offsetZ = (mcWorld.rand.nextDouble() - 0.5) * 0.5;
            
            serverWorld.spawnParticle(EnumParticleTypes.REDSTONE,
                x + offsetX, y + offsetY, z + offsetZ,
                1, red, green, blue, 1.0);
        }
    }
    
    /**
     * 生成彩色粒子圈
     * 
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @param red 红色值
     * @param green 绿色值
     * @param blue 蓝色值
     * @param count 粒子数量
     */
    @ZenMethod
    public static void spawnColoredCircle(IWorld world,
                                         double x, double y, double z,
                                         double radius,
                                         float red, float green, float blue,
                                         int count) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return;
        
        WorldServer serverWorld = (WorldServer) mcWorld;
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            
            serverWorld.spawnParticle(EnumParticleTypes.REDSTONE,
                x + offsetX, y, z + offsetZ,
                1, red, green, blue, 1.0);
        }
    }
    
    /**
     * 将字符串转换为粒子类型
     */
    private static EnumParticleTypes getParticleType(String name) {
        try {
            return EnumParticleTypes.getByName(name.toLowerCase());
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取所有可用的粒子类型列表
     * 
     * @return 粒子类型名称数组
     */
    @ZenMethod
    public static String[] getAvailableParticles() {
        EnumParticleTypes[] types = EnumParticleTypes.values();
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].getParticleName();
        }
        return names;
    }
}
