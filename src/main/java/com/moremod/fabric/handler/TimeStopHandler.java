package com.moremod.fabric.handler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class TimeStopHandler {

    // 存储冻结的实体
    private static final Set<UUID> FROZEN_ENTITIES = Collections.synchronizedSet(new HashSet<>());
    private static final Map<Integer, List<TimeStopZone>> ACTIVE_ZONES = new ConcurrentHashMap<>();

    /**
     * 时停区域类
     */
    public static class TimeStopZone {
        public final World world;
        public final BlockPos center;
        public final double range;
        public final EntityPlayer caster;
        public final int maxDuration;
        public int currentAge = 0;
        public boolean active = true;
        private final Set<UUID> affectedEntities = new HashSet<>();

        public TimeStopZone(World world, BlockPos center, double range, EntityPlayer caster, int duration) {
            this.world = world;
            this.center = center;
            this.range = range;
            this.caster = caster;
            this.maxDuration = duration;
        }

        public void tick() {
            if (!active) return;

            currentAge++;
            if (currentAge >= maxDuration) {
                deactivate();
                return;
            }

            // 查找范围内的实体
            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                    EntityLivingBase.class,
                    new AxisAlignedBB(center).grow(range),
                    e -> e != caster && e != null && !e.isDead &&
                            e.getDistanceSq(center) <= range * range
            );

            // 冻结新进入的实体
            for (EntityLivingBase entity : entities) {
                if (!affectedEntities.contains(entity.getUniqueID())) {
                    freezeEntity(entity);
                    affectedEntities.add(entity.getUniqueID());
                }
            }

            // 检查离开范围的实体
            Iterator<UUID> it = affectedEntities.iterator();
            while (it.hasNext()) {
                UUID id = it.next();
                Entity entity = world.getMinecraftServer().getEntityFromUuid(id);
                if (entity == null || entity.isDead ||
                        entity.getDistanceSq(center) > range * range) {
                    unfreezeEntity(id);
                    it.remove();
                }
            }
        }

        private void freezeEntity(EntityLivingBase entity) {
            FROZEN_ENTITIES.add(entity.getUniqueID());
        }

        private void unfreezeEntity(UUID id) {
            FROZEN_ENTITIES.remove(id);
        }

        public void deactivate() {
            active = false;

            // 解冻所有实体
            for (UUID id : affectedEntities) {
                unfreezeEntity(id);
            }
            affectedEntities.clear();
        }
    }

    /**
     * 创建时停区域
     */
    public static TimeStopZone createTimeStop(World world, BlockPos center, double range,
                                              EntityPlayer caster, int duration) {
        if (world.isRemote) return null;

        TimeStopZone zone = new TimeStopZone(world, center, range, caster, duration);
        int dimId = world.provider.getDimension();
        List<TimeStopZone> zones = ACTIVE_ZONES.computeIfAbsent(dimId, k -> new ArrayList<>());
        zones.add(zone);

        return zone;
    }

    /**
     * 核心：拦截实体更新（模仿AS的做法）
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();

        // 检查是否被冻结
        if (FROZEN_ENTITIES.contains(entity.getUniqueID())) {
            // 特殊情况：实体死亡或者是末影龙死亡动画
            if (entity.isDead || entity.getHealth() <= 0) {
                FROZEN_ENTITIES.remove(entity.getUniqueID());
                return;
            }

            if (entity instanceof EntityDragon) {
                EntityDragon dragon = (EntityDragon) entity;
                // 让末影龙死亡动画继续
                if (dragon.getHealth() <= 0) {
                    FROZEN_ENTITIES.remove(entity.getUniqueID());
                    return;
                }
            }

            // 取消实体更新
            event.setCanceled(true);

            // 处理必要的tick（模仿AS的handleImportantEntityTicks）
            handleFrozenEntityTicks(entity);
        }
    }

    /**
     * 处理冻结实体的必要更新（防止插值抖动）
     */
    private static void handleFrozenEntityTicks(EntityLivingBase entity) {
        // 处理伤害时间（允许受伤动画）
        if (entity.hurtTime > 0) {
            entity.hurtTime--;
        }
        if (entity.hurtResistantTime > 0) {
            entity.hurtResistantTime--;
        }

        // 关键：保持prev值等于当前值，防止客户端插值
        entity.prevPosX = entity.posX;
        entity.prevPosY = entity.posY;
        entity.prevPosZ = entity.posZ;
        entity.prevRotationYaw = entity.rotationYaw;
        entity.prevRotationPitch = entity.rotationPitch;
        entity.prevRotationYawHead = entity.rotationYawHead;
        entity.prevRenderYawOffset = entity.renderYawOffset;

        // 动画相关
        entity.prevLimbSwingAmount = entity.limbSwingAmount;
        entity.prevSwingProgress = entity.swingProgress;
        entity.prevDistanceWalkedModified = entity.distanceWalkedModified;
        entity.prevCameraPitch = entity.cameraPitch;

        // 清除运动（但不强制设置位置）
        entity.motionX = 0;
        entity.motionY = 0;
        entity.motionZ = 0;
        entity.velocityChanged = true;

        // 保持实体不窒息
        entity.setAir(300);

        // 粒子效果（可选）
        if (entity.world instanceof WorldServer && entity.ticksExisted % 5 == 0) {
            spawnFrozenParticles((WorldServer)entity.world, entity);
        }
    }

    /**
     * 阻止冻结实体跳跃
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (FROZEN_ENTITIES.contains(event.getEntityLiving().getUniqueID())) {
            // 阻止跳跃
            event.getEntityLiving().motionY = 0;
        }
    }

    /**
     * 服务器tick - 更新时停区域
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 更新所有时停区域
        ACTIVE_ZONES.forEach((dimId, zones) -> {
            zones.removeIf(zone -> {
                zone.tick();
                return !zone.active;
            });
        });
    }

    /**
     * 世界卸载时清理
     */
    @SubscribeEvent
    public static void onWorldUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        World world = event.getWorld();
        if (world != null && world.provider != null && !world.isRemote) {
            int dimId = world.provider.getDimension();
            List<TimeStopZone> zones = ACTIVE_ZONES.get(dimId);
            if (zones != null) {
                zones.forEach(TimeStopZone::deactivate);
                zones.clear();
            }
        }
    }

    /**
     * 生成冻结粒子效果
     */
    private static void spawnFrozenParticles(WorldServer world, EntityLivingBase entity) {
        for (int i = 0; i < 3; i++) {
            double x = entity.posX + (world.rand.nextDouble() - 0.5) * entity.width * 2;
            double y = entity.posY + world.rand.nextDouble() * entity.height;
            double z = entity.posZ + (world.rand.nextDouble() - 0.5) * entity.width * 2;

            world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.END_ROD,
                    x, y, z,
                    1, 0, 0, 0.0, 0
            );
        }
    }

    /**
     * 检查实体是否被冻结
     */
    public static boolean isFrozen(EntityLivingBase entity) {
        return FROZEN_ENTITIES.contains(entity.getUniqueID());
    }

    /**
     * 手动解冻实体
     */
    public static void unfreezeEntity(EntityLivingBase entity) {
        FROZEN_ENTITIES.remove(entity.getUniqueID());
    }
}