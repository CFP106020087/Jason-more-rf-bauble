package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.entity.IEntity;
import crafttweaker.api.entity.IEntityLivingBase;
import crafttweaker.api.player.IPlayer;
import crafttweaker.api.world.IWorld;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.world.World;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体助手类
 * 提供生成、查找、操作实体的功能
 */
@ZenRegister
@ZenClass("mods.moremod.EntityHelper")
public class EntityHelper {

    /**
     * 在指定位置生成实体
     *
     * @param world 世界
     * @param entityId 实体 ID（如 "minecraft:zombie"）
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return 是否成功生成
     */
    @ZenMethod
    public static boolean spawnEntity(IWorld world, String entityId,
                                      double x, double y, double z) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return false;

        try {
            ResourceLocation location = new ResourceLocation(entityId);
            Entity entity = net.minecraft.entity.EntityList.createEntityByIDFromName(location, mcWorld);

            if (entity != null) {
                entity.setPosition(x, y, z);
                return mcWorld.spawnEntity(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 生成自定义实体（带 NBT 数据）
     *
     * @param world 世界
     * @param entityId 实体 ID
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @param customName 自定义名称（可为 null）
     * @param health 生命值（-1 表示默认）
     * @return 是否成功生成
     */
    @ZenMethod
    public static boolean spawnCustomEntity(IWorld world, String entityId,
                                            double x, double y, double z,
                                            String customName, float health) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        if (mcWorld.isRemote) return false;

        try {
            ResourceLocation location = new ResourceLocation(entityId);
            Entity entity = net.minecraft.entity.EntityList.createEntityByIDFromName(location, mcWorld);

            if (entity != null) {
                entity.setPosition(x, y, z);

                // 设置自定义名称
                if (customName != null && !customName.isEmpty()) {
                    entity.setCustomNameTag(customName);
                    entity.setAlwaysRenderNameTag(true);
                }

                // 设置生命值
                if (health > 0 && entity instanceof EntityLivingBase) {
                    EntityLivingBase living = (EntityLivingBase) entity;
                    living.setHealth(health);
                }

                return mcWorld.spawnEntity(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 获取范围内的所有实体
     *
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @return 实体数组
     */
    @ZenMethod
    public static IEntityLivingBase[] getEntitiesInRadius(IWorld world,
                                                          double x, double y, double z,
                                                          double radius) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        List<IEntityLivingBase> result = new ArrayList<>();

        AxisAlignedBB box = new AxisAlignedBB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius
        );

        for (EntityLivingBase entity : mcWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            double distance = entity.getDistance(x, y, z);
            if (distance <= radius) {
                result.add(CraftTweakerMC.getIEntityLivingBase(entity));
            }
        }

        return result.toArray(new IEntityLivingBase[0]);
    }

    /**
     * 获取范围内的所有敌对生物
     *
     * @param world 世界
     * @param player 玩家（用于判断敌对）
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @return 敌对生物数组
     */
    @ZenMethod
    public static IEntityLivingBase[] getHostileInRadius(IWorld world, IPlayer player,
                                                         double x, double y, double z,
                                                         double radius) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        EntityPlayer mcPlayer = CraftTweakerMC.getPlayer(player);
        List<IEntityLivingBase> result = new ArrayList<>();

        AxisAlignedBB box = new AxisAlignedBB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius
        );

        for (EntityLivingBase entity : mcWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (entity == mcPlayer) continue;

            double distance = entity.getDistance(x, y, z);
            if (distance <= radius) {
                // 检查是否为敌对生物
                if (entity instanceof EntityMob ||
                        (entity instanceof EntityLiving && ((EntityLiving)entity).getAttackTarget() == mcPlayer)) {
                    result.add(CraftTweakerMC.getIEntityLivingBase(entity));
                }
            }
        }

        return result.toArray(new IEntityLivingBase[0]);
    }

    /**
     * 获取范围内的所有玩家
     *
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param radius 半径
     * @return 玩家数组
     */
    @ZenMethod
    public static IPlayer[] getPlayersInRadius(IWorld world,
                                               double x, double y, double z,
                                               double radius) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        List<IPlayer> result = new ArrayList<>();

        for (EntityPlayer player : mcWorld.playerEntities) {
            double distance = player.getDistance(x, y, z);
            if (distance <= radius) {
                result.add(CraftTweakerMC.getIPlayer(player));
            }
        }

        return result.toArray(new IPlayer[0]);
    }

    /**
     * 获取最近的实体
     *
     * @param world 世界
     * @param x 中心 X 坐标
     * @param y 中心 Y 坐标
     * @param z 中心 Z 坐标
     * @param maxRadius 最大搜索半径
     * @return 最近的实体（可能为 null）
     */
    @ZenMethod
    public static IEntityLivingBase getNearestEntity(IWorld world,
                                                     double x, double y, double z,
                                                     double maxRadius) {
        World mcWorld = CraftTweakerMC.getWorld(world);
        EntityLivingBase nearest = null;
        double nearestDistance = maxRadius;

        AxisAlignedBB box = new AxisAlignedBB(
                x - maxRadius, y - maxRadius, z - maxRadius,
                x + maxRadius, y + maxRadius, z + maxRadius
        );

        for (EntityLivingBase entity : mcWorld.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            double distance = entity.getDistance(x, y, z);
            if (distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }

        return nearest != null ? CraftTweakerMC.getIEntityLivingBase(nearest) : null;
    }

    /**
     * 将实体击退
     *
     * @param entity 目标实体
     * @param source 击退来源实体
     * @param strength 击退力度
     */
    @ZenMethod
    public static void knockback(IEntityLivingBase entity,
                                 IEntityLivingBase source,
                                 double strength) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
        EntityLivingBase mcSource = CraftTweakerMC.getEntityLivingBase(source);

        double dx = mcEntity.posX - mcSource.posX;
        double dz = mcEntity.posZ - mcSource.posZ;
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance > 0) {
            mcEntity.motionX += (dx / distance) * strength;
            mcEntity.motionY += 0.4 * strength;
            mcEntity.motionZ += (dz / distance) * strength;
            mcEntity.velocityChanged = true;
        }
    }

    /**
     * 将实体吸引到目标位置
     *
     * @param entity 目标实体
     * @param targetX 目标 X 坐标
     * @param targetY 目标 Y 坐标
     * @param targetZ 目标 Z 坐标
     * @param strength 吸引力度
     */
    @ZenMethod
    public static void pullTowards(IEntityLivingBase entity,
                                   double targetX, double targetY, double targetZ,
                                   double strength) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);

        double dx = targetX - mcEntity.posX;
        double dy = targetY - mcEntity.posY;
        double dz = targetZ - mcEntity.posZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > 0) {
            mcEntity.motionX += (dx / distance) * strength;
            mcEntity.motionY += (dy / distance) * strength;
            mcEntity.motionZ += (dz / distance) * strength;
            mcEntity.velocityChanged = true;
        }
    }

    /**
     * 将实体推开
     *
     * @param entity 目标实体
     * @param sourceX 推力来源 X 坐标
     * @param sourceY 推力来源 Y 坐标
     * @param sourceZ 推力来源 Z 坐标
     * @param strength 推力强度
     */
    @ZenMethod
    public static void pushAway(IEntityLivingBase entity,
                                double sourceX, double sourceY, double sourceZ,
                                double strength) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);

        double dx = mcEntity.posX - sourceX;
        double dy = mcEntity.posY - sourceY;
        double dz = mcEntity.posZ - sourceZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > 0) {
            mcEntity.motionX += (dx / distance) * strength;
            mcEntity.motionY += 0.4 * strength; // 添加向上的力
            mcEntity.motionZ += (dz / distance) * strength;
            mcEntity.velocityChanged = true;
        }
    }

    /**
     * 传送实体到指定位置
     *
     * @param entity 实体
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     */
    @ZenMethod
    public static void teleportEntity(IEntityLivingBase entity,
                                      double x, double y, double z) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
        mcEntity.setPositionAndUpdate(x, y, z);
    }

    /**
     * 设置实体的移动速度
     *
     * @param entity 实体
     * @param velocityX X 方向速度
     * @param velocityY Y 方向速度
     * @param velocityZ Z 方向速度
     */
    @ZenMethod
    public static void setVelocity(IEntityLivingBase entity,
                                   double velocityX, double velocityY, double velocityZ) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
        mcEntity.motionX = velocityX;
        mcEntity.motionY = velocityY;
        mcEntity.motionZ = velocityZ;
        mcEntity.velocityChanged = true;
    }

    /**
     * 向上发射实体
     *
     * @param entity 实体
     * @param strength 发射力度
     */
    @ZenMethod
    public static void launchUp(IEntityLivingBase entity, double strength) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
        mcEntity.motionY = strength;
        mcEntity.velocityChanged = true;
    }

    /**
     * 将实体发射向特定方向
     *
     * @param entity 实体
     * @param yaw 水平角度（度）
     * @param pitch 垂直角度（度）
     * @param strength 力度
     */
    @ZenMethod
    public static void launchInDirection(IEntityLivingBase entity,
                                         float yaw, float pitch, double strength) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);

        // 转换角度为弧度
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        // 计算方向向量
        double motionX = -Math.sin(yawRad) * Math.cos(pitchRad) * strength;
        double motionY = -Math.sin(pitchRad) * strength;
        double motionZ = Math.cos(yawRad) * Math.cos(pitchRad) * strength;

        mcEntity.motionX = motionX;
        mcEntity.motionY = motionY;
        mcEntity.motionZ = motionZ;
        mcEntity.velocityChanged = true;
    }

    /**
     * 冻结实体（停止移动）
     *
     * @param entity 实体
     */
    @ZenMethod
    public static void freezeEntity(IEntityLivingBase entity) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
        mcEntity.motionX = 0;
        mcEntity.motionY = 0;
        mcEntity.motionZ = 0;
        mcEntity.velocityChanged = true;
    }

    /**
     * 获取实体之间的距离
     *
     * @param entity1 实体 1
     * @param entity2 实体 2
     * @return 距离
     */
    @ZenMethod
    public static double getDistance(IEntityLivingBase entity1, IEntityLivingBase entity2) {
        EntityLivingBase mc1 = CraftTweakerMC.getEntityLivingBase(entity1);
        EntityLivingBase mc2 = CraftTweakerMC.getEntityLivingBase(entity2);
        return mc1.getDistance(mc2);
    }

    /**
     * 移除实体
     *
     * @param entity 实体
     */
    @ZenMethod
    public static void removeEntity(IEntityLivingBase entity) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
        mcEntity.setDead();
    }

    /**
     * 获取实体的唯一 ID
     *
     * @param entity 实体
     * @return UUID 字符串
     */
    @ZenMethod
    public static String getEntityUUID(IEntityLivingBase entity) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
        return mcEntity.getUniqueID().toString();
    }

    /**
     * 检查实体是否在地面上
     *
     * @param entity 实体
     * @return 是否在地面
     */
    @ZenMethod
    public static boolean isOnGround(IEntityLivingBase entity) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
        return mcEntity.onGround;
    }

    /**
     * 检查实体是否在水中
     *
     * @param entity 实体
     * @return 是否在水中
     */
    @ZenMethod
    public static boolean isInWater(IEntityLivingBase entity) {
        EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
        return mcEntity.isInWater();
    }
}