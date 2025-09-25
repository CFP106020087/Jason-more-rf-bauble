package com.moremod.dimension;

import com.moremod.entity.boss.EntityRiftwarden;
import com.moremod.entity.boss.EntityStoneSentinel;
import com.moremod.entity.EntityCursedKnight;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 个人维度生成处理器
 * 控制个人维度中的实体生成规则
 */
public class PersonalDimensionSpawnHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.getWorld().provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) {
            return;
        }

        // 刷怪笼生成的实体允许
        if (event.isSpawner()) {
            event.setResult(Event.Result.ALLOW);
            return;
        }

        // 拒绝其他自然生成
        event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        if (event.getWorld().provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) {
            return;
        }
        // 不取消，让刷怪笼工作
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) {
            return;
        }

        Entity entity = event.getEntity();

        // 玩家总是允许
        if (entity instanceof EntityPlayer) {
            return;
        }

        // 处理虚空守望者Boss
        if (entity instanceof EntityRiftwarden) {
            entity.addTag("boss_entity");
            entity.addTag("riftwarden_boss");
            return;
        }

        // 处理石像哨兵Boss
        if (entity instanceof EntityStoneSentinel) {
            entity.addTag("boss_entity");
            entity.addTag("stone_sentinel_boss");
            return;
        }

        // 处理诅咒骑士（守卫怪物）
        if (entity instanceof EntityCursedKnight) {
            // 检查是否靠近刷怪笼
            if (hasNearbySpawner(event.getWorld(), entity.getPosition())) {
                entity.addTag("spawner_spawned");
                return;
            }

            // 检查是否靠近Boss（Boss召唤的守卫）
            if (hasNearbyBoss(event.getWorld(), entity.getPosition())) {
                entity.addTag("boss_summoned");
                return;
            }

            // 如果已经有标签，允许存在
            if (entity.getTags().contains("spawner_spawned") ||
                    entity.getTags().contains("boss_summoned") ||
                    entity.getTags().contains("player_summoned")) {
                return;
            }
        }

        // 处理哭泣天使（特殊怪物）
        if (entity.getClass().getName().contains("WeepingAngel") ||
                entity.getClass().getSimpleName().equals("EntityWeepingAngel")) {
            // 检查是否靠近刷怪笼
            if (hasNearbySpawner(event.getWorld(), entity.getPosition())) {
                entity.addTag("spawner_spawned");
                return;
            }

            // 检查是否靠近Boss（可能是Boss召唤的）
            if (hasNearbyBoss(event.getWorld(), entity.getPosition())) {
                entity.addTag("boss_summoned");
                return;
            }

            // 如果已经有标签，允许存在
            if (entity.getTags().contains("spawner_spawned") ||
                    entity.getTags().contains("boss_summoned") ||
                    entity.getTags().contains("player_summoned")) {
                return;
            }
        }



        // 跳过非生物实体
        if (!(entity instanceof EntityLivingBase)) {
            return;
        }

        // 处理其他生物实体
        if (entity instanceof EntityLiving) {
            // 检查实体标签
            if (entity.getTags().contains("spawner_spawned") ||
                    entity.getTags().contains("player_summoned") ||
                    entity.getTags().contains("boss_summoned")) {
                return;
            }

            // 检查是否靠近刷怪笼
            if (hasNearbySpawner(event.getWorld(), entity.getPosition())) {
                entity.addTag("spawner_spawned");
                return;
            }

            // 检查是否靠近Boss
            if (hasNearbyBoss(event.getWorld(), entity.getPosition())) {
                entity.addTag("boss_summoned");
                return;
            }

            // 取消其他未授权的生成
            event.setCanceled(true);
        }
    }

    /**
     * 检查附近是否有刷怪笼
     */
    private boolean hasNearbySpawner(World world, BlockPos pos) {
        final int RADIUS = 6;

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    BlockPos checkPos = pos.add(x, y, z);

                    // 边界检查
                    if (checkPos.getY() < 0 || checkPos.getY() > 255) {
                        continue;
                    }

                    TileEntity te = world.getTileEntity(checkPos);
                    if (te instanceof TileEntityMobSpawner) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查附近是否有Boss实体
     * 支持虚空守望者和石像哨兵
     */
    private boolean hasNearbyBoss(World world, BlockPos pos) {
        final int RADIUS = 20;

        for (Entity entity : world.loadedEntityList) {
            // 检查虚空守望者
            if (entity instanceof EntityRiftwarden) {
                double distance = entity.getDistance(pos.getX(), pos.getY(), pos.getZ());
                if (distance <= RADIUS) {
                    return true;
                }
            }

            // 检查石像哨兵
            if (entity instanceof EntityStoneSentinel) {
                double distance = entity.getDistance(pos.getX(), pos.getY(), pos.getZ());
                if (distance <= RADIUS) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查实体是否是Boss
     */
    private boolean isBossEntity(Entity entity) {
        return entity instanceof EntityRiftwarden ||
                entity instanceof EntityStoneSentinel;
    }

    /**
     * 获取Boss类型名称（用于调试）
     */
    private String getBossTypeName(Entity entity) {
        if (entity instanceof EntityRiftwarden) {
            return "虚空守望者";
        } else if (entity instanceof EntityStoneSentinel) {
            return "石像哨兵";
        }
        return "未知Boss";
    }
}