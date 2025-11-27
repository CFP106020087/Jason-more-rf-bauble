package com.moremod.synergy.effect;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.core.SynergyContext;
import com.moremod.synergy.core.SynergyPlayerState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;

/**
 * 传送效果
 *
 * 传送玩家到指定位置或随机位置。
 */
public class TeleportEffect implements ISynergyEffect {

    public enum TeleportType {
        TO_HISTORY,      // 回到历史位置
        RANDOM_NEARBY,   // 随机传送到附近
        FORWARD,         // 向前传送
        BACKWARD,        // 向后传送
        BLINK            // 朝视线方向传送
    }

    private final TeleportType type;
    private final int ticksAgo;        // 用于 TO_HISTORY
    private final int distance;         // 用于距离相关传送
    private final boolean spawnParticles;

    private static final Random RANDOM = new Random();

    public TeleportEffect(TeleportType type, int param, boolean spawnParticles) {
        this.type = type;
        this.ticksAgo = param;
        this.distance = param;
        this.spawnParticles = spawnParticles;
    }

    @Override
    public void apply(SynergyContext context) {
        EntityPlayer player = context.getPlayer();
        World world = player.world;
        Vec3d originalPos = player.getPositionVector();
        Vec3d targetPos = null;

        switch (type) {
            case TO_HISTORY:
                SynergyPlayerState state = SynergyPlayerState.get(player);
                SynergyPlayerState.PositionSnapshot snapshot = state.getPositionAt(ticksAgo);
                if (snapshot != null) {
                    targetPos = snapshot.toVec3d();
                    // 同时恢复 HP
                    player.setHealth(snapshot.health);
                }
                break;

            case RANDOM_NEARBY:
                targetPos = findRandomPosition(player, distance);
                break;

            case FORWARD:
                Vec3d look = player.getLookVec();
                targetPos = originalPos.add(look.scale(distance));
                break;

            case BACKWARD:
                Vec3d lookBack = player.getLookVec().scale(-1);
                targetPos = originalPos.add(lookBack.scale(distance));
                break;

            case BLINK:
                targetPos = raycastPosition(player, distance);
                break;
        }

        if (targetPos != null) {
            // 粒子效果 - 出发位置
            if (spawnParticles) {
                spawnTeleportParticles(world, originalPos);
            }

            // 执行传送
            player.setPositionAndUpdate(targetPos.x, targetPos.y, targetPos.z);

            // 粒子效果 - 到达位置
            if (spawnParticles) {
                spawnTeleportParticles(world, targetPos);
            }
        }
    }

    private Vec3d findRandomPosition(EntityPlayer player, int range) {
        World world = player.world;
        int attempts = 20;

        for (int i = 0; i < attempts; i++) {
            double x = player.posX + (RANDOM.nextDouble() - 0.5) * 2 * range;
            double z = player.posZ + (RANDOM.nextDouble() - 0.5) * 2 * range;
            double y = player.posY + (RANDOM.nextDouble() - 0.5) * range;

            BlockPos pos = new BlockPos(x, y, z);

            // 检查是否是有效的传送位置
            if (isValidTeleportPosition(world, pos)) {
                return new Vec3d(x, pos.getY() + 0.5, z);
            }
        }

        return null;  // 找不到有效位置
    }

    private Vec3d raycastPosition(EntityPlayer player, int maxDistance) {
        Vec3d start = player.getPositionEyes(1.0f);
        Vec3d look = player.getLookVec();
        World world = player.world;

        for (int i = 1; i <= maxDistance; i++) {
            Vec3d check = start.add(look.scale(i));
            BlockPos pos = new BlockPos(check);

            if (!world.isAirBlock(pos)) {
                // 返回撞墙前的位置
                Vec3d safePos = start.add(look.scale(Math.max(1, i - 1)));
                BlockPos safeBlockPos = new BlockPos(safePos);
                if (isValidTeleportPosition(world, safeBlockPos)) {
                    return safePos;
                }
                break;
            }
        }

        // 没有撞墙，传送到最大距离
        Vec3d targetPos = start.add(look.scale(maxDistance));
        BlockPos targetBlockPos = new BlockPos(targetPos);
        if (isValidTeleportPosition(world, targetBlockPos)) {
            return targetPos;
        }

        return null;
    }

    private boolean isValidTeleportPosition(World world, BlockPos pos) {
        // 检查脚下和头部位置
        return world.isAirBlock(pos) && world.isAirBlock(pos.up()) &&
               !world.isAirBlock(pos.down());  // 有地面
    }

    private void spawnTeleportParticles(World world, Vec3d pos) {
        for (int i = 0; i < 32; i++) {
            double dx = RANDOM.nextGaussian() * 0.2;
            double dy = RANDOM.nextGaussian() * 0.2;
            double dz = RANDOM.nextGaussian() * 0.2;

            world.spawnParticle(EnumParticleTypes.PORTAL,
                    pos.x + dx, pos.y + 1.0 + dy, pos.z + dz,
                    dx * 5, dy * 5, dz * 5);
        }
    }

    @Override
    public String getDescription() {
        switch (type) {
            case TO_HISTORY:
                return "Teleport to position " + (ticksAgo / 20f) + "s ago";
            case RANDOM_NEARBY:
                return "Random teleport within " + distance + " blocks";
            case FORWARD:
                return "Teleport forward " + distance + " blocks";
            case BACKWARD:
                return "Teleport backward " + distance + " blocks";
            case BLINK:
                return "Blink up to " + distance + " blocks";
            default:
                return "Teleport";
        }
    }

    // ==================== 静态工厂方法 ====================

    public static TeleportEffect toHistory(int ticksAgo) {
        return new TeleportEffect(TeleportType.TO_HISTORY, ticksAgo, true);
    }

    public static TeleportEffect randomNearby(int range) {
        return new TeleportEffect(TeleportType.RANDOM_NEARBY, range, true);
    }

    public static TeleportEffect forward(int distance) {
        return new TeleportEffect(TeleportType.FORWARD, distance, true);
    }

    public static TeleportEffect blink(int maxDistance) {
        return new TeleportEffect(TeleportType.BLINK, maxDistance, true);
    }
}
