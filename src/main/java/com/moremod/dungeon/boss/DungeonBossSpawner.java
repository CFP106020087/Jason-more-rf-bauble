package com.moremod.dungeon.boss;

import com.moremod.entity.boss.EntityRiftwarden;
import com.moremod.entity.boss.EntityStoneSentinel;
import com.moremod.entity.EntityCursedKnight;
import com.moremod.init.ModBlocks;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;

public class DungeonBossSpawner {

    // Boss类型枚举
    public enum BossType {
        RIFTWARDEN("虚空守望者", 0.5),
        STONE_SENTINEL("石像哨兵", 0.5);

        private final String displayName;
        private final double spawnWeight;

        BossType(String displayName, double spawnWeight) {
            this.displayName = displayName;
            this.spawnWeight = spawnWeight;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static boolean trySpawnBoss(World world, BlockPos altarPos, EntityPlayer player) {
        try {
            if (world.isRemote) return false;

            if (world.getBlockState(altarPos).getBlock() != ModBlocks.UNBREAKABLE_BARRIER_QUANTUM) {
                return false;
            }

            if (hasBossNearby(world, altarPos, 50)) {
                player.sendMessage(new TextComponentString("§c强大的Boss已经存在于此区域！"));
                return false;
            }

            int playerCount = countPlayersNearby(world, altarPos, 30);
            if (playerCount < 1) {
                player.sendMessage(new TextComponentString("§c需要至少1名玩家在场才能召唤Boss！"));
                return false;
            }

            // 随机选择要召唤的Boss类型（使用world.rand）
            BossType bossType = selectRandomBossType(world);
            return spawnBoss(world, altarPos, player, bossType);
        } catch (Exception e) {
            System.err.println("[Boss召唤] trySpawnBoss异常: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(new TextComponentString("§c[错误] Boss召唤过程中发生异常，请查看日志"));
            return false;
        }
    }

    private static BossType selectRandomBossType(World world) {
        // 使用 world.rand 進行50/50隨機選擇
        return world.rand.nextBoolean() ? BossType.RIFTWARDEN : BossType.STONE_SENTINEL;
    }

    private static boolean spawnBoss(World world, BlockPos altarPos, EntityPlayer activator, BossType bossType) {
        try {
            if (!(world instanceof WorldServer)) {
                System.out.println("[Boss召唤] 失败: world不是WorldServer");
                return false;
            }

            WorldServer ws = (WorldServer) world;
            EntityLiving boss = null;

            switch (bossType) {
                case RIFTWARDEN:
                    boss = createRiftwarden(ws, altarPos, activator);
                    break;
                case STONE_SENTINEL:
                    boss = createStoneSentinel(ws, altarPos, activator);
                    break;
            }

            if (boss == null) {
                System.out.println("[Boss召唤] 失败: boss实体为null (可能实体类初始化异常)");
                activator.sendMessage(new TextComponentString("§c[错误] Boss实体创建失败，请查看日志"));
                return false;
            }

            // ★ 添加Boss标记，确保不被维度地牢生成处理器拦截
            boss.addTag("boss_summoned");
            boss.addTag("altar_spawned");

            System.out.println("[Boss召唤] 准备生成 " + bossType.getDisplayName() + " @ " + boss.getPosition());

            // 生成特效
            spawnSummonEffects(ws, altarPos, bossType);

            // 生成Boss
            boolean spawned = ws.spawnEntity(boss);
            System.out.println("[Boss召唤] spawnEntity结果: " + spawned + ", isAddedToWorld: " + boss.isAddedToWorld());

            if (!spawned || !boss.isAddedToWorld()) {
                activator.sendMessage(new TextComponentString("§c[调试] Boss生成失败，可能被其他系统拦截"));
                return false;
            }

            // 广播Boss生成消息
            broadcastBossSpawn(ws, altarPos, activator, bossType);

            // 将祭坛变为空气
            world.setBlockState(altarPos, Blocks.AIR.getDefaultState());

            return true;
        } catch (Exception e) {
            System.err.println("[Boss召唤] spawnBoss异常: " + e.getMessage());
            e.printStackTrace();
            activator.sendMessage(new TextComponentString("§c[错误] Boss生成过程中发生异常: " + e.getClass().getSimpleName()));
            return false;
        }
    }

    private static EntityRiftwarden createRiftwarden(WorldServer world, BlockPos altarPos, EntityPlayer activator) {
        try {
            EntityRiftwarden boss = new EntityRiftwarden(world);

            boss.setPosition(
                    altarPos.getX() + 0.5,
                    altarPos.getY() ,  // 降低一格
                    altarPos.getZ() + 0.5
            );

            boss.setCustomNameTag("§5虚空守望者");
            boss.setAttackTarget(activator);

            return boss;
        } catch (Exception e) {
            System.err.println("[Boss召唤] 创建Riftwarden失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static EntityStoneSentinel createStoneSentinel(WorldServer world, BlockPos altarPos, EntityPlayer activator) {
        try {
            EntityStoneSentinel boss = new EntityStoneSentinel(world);

            boss.setPosition(
                    altarPos.getX() + 0.5,
                    altarPos.getY() ,  // 降低一格
                    altarPos.getZ() + 0.5
            );

            boss.setCustomNameTag("§7石像哨兵");
            boss.setAttackTarget(activator);

            return boss;
        } catch (Exception e) {
            System.err.println("[Boss召唤] 创建StoneSentinel失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void spawnGuards(WorldServer world, BlockPos center, BossType bossType) {
        int guardCount = bossType == BossType.STONE_SENTINEL ? 3 : 4; // 石像哨兵召唤3个守卫，虚空守望者召唤4个

        for (int i = 0; i < guardCount; i++) {
            double angle = (Math.PI * 2 * i) / guardCount;
            int radius = bossType == BossType.STONE_SENTINEL ? 6 : 5;
            int x = center.getX() + (int)(Math.cos(angle) * radius);
            int z = center.getZ() + (int)(Math.sin(angle) * radius);
            int y = center.getY();

            BlockPos spawnPos = new BlockPos(x, y, z);
            for (int dy = 0; dy < 5; dy++) {
                BlockPos checkPos = spawnPos.up(dy);
                if (world.isAirBlock(checkPos) && world.isAirBlock(checkPos.up())) {
                    spawnPos = checkPos;
                    break;
                }
            }

            EntityCursedKnight knight = new EntityCursedKnight(world);
            knight.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

            world.spawnEntity(knight);

            // 根据Boss类型使用不同的粒子效果
            EnumParticleTypes particleType = bossType == BossType.STONE_SENTINEL ?
                    EnumParticleTypes.BLOCK_CRACK : EnumParticleTypes.SPELL_MOB;

            for (int j = 0; j < 20; j++) {
                world.spawnParticle(
                        particleType,
                        spawnPos.getX() + 0.5 + (Math.random() - 0.5),
                        spawnPos.getY() + Math.random() * 2,
                        spawnPos.getZ() + 0.5 + (Math.random() - 0.5),
                        1, 0, 0, 0, 0.0
                );
            }
        }
    }

    private static void spawnSummonEffects(WorldServer world, BlockPos pos, BossType bossType) {
        // 通用音效
        world.playSound(null, pos, SoundEvents.ENTITY_WITHER_SPAWN,
                SoundCategory.HOSTILE, 2.0F, 0.5F);

        // 根据Boss类型播放不同的音效
        if (bossType == BossType.RIFTWARDEN) {
            world.playSound(null, pos, SoundEvents.ENTITY_ENDERDRAGON_GROWL,
                    SoundCategory.HOSTILE, 1.5F, 0.8F);
        } else if (bossType == BossType.STONE_SENTINEL) {
            world.playSound(null, pos, SoundEvents.BLOCK_STONE_BREAK,
                    SoundCategory.HOSTILE, 2.0F, 0.3F);
            world.playSound(null, pos, SoundEvents.ENTITY_IRONGOLEM_HURT,
                    SoundCategory.HOSTILE, 1.5F, 0.5F);
        }

        // 螺旋上升粒子效果
        for (int i = 0; i < 100; i++) {
            double angle = (i * 0.1) * Math.PI;
            double radius = 3.0 * (1.0 - i / 100.0);
            double height = i * 0.1;

            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            double y = pos.getY() + height;

            if (bossType == BossType.RIFTWARDEN) {
                world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, 1, 0, 0, 0, 0.0);
                world.spawnParticle(EnumParticleTypes.SPELL_WITCH, x, y, z, 1, 0, 0.1, 0, 0.0);
            } else if (bossType == BossType.STONE_SENTINEL) {
                world.spawnParticle(EnumParticleTypes.BLOCK_DUST, x, y, z, 1, 0, 0, 0, 0.0);
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z, 1, 0, 0.05, 0, 0.0);
            }
        }

        // 爆炸环效果
        for (int ring = 0; ring < 3; ring++) {
            double ringRadius = 2 + ring * 2;
            for (int i = 0; i < 32; i++) {
                double angle = (Math.PI * 2 * i) / 32;
                double x = pos.getX() + 0.5 + Math.cos(angle) * ringRadius;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * ringRadius;

                EnumParticleTypes particle = bossType == BossType.STONE_SENTINEL ?
                        EnumParticleTypes.EXPLOSION_NORMAL : EnumParticleTypes.EXPLOSION_LARGE;

                world.spawnParticle(
                        particle,
                        x, pos.getY() + 1, z,
                        1, 0, 0, 0, 0.0
                );
            }
        }
    }

    private static void broadcastBossSpawn(WorldServer world, BlockPos pos, EntityPlayer activator, BossType bossType) {
        List<EntityPlayer> players = world.getEntitiesWithinAABB(
                EntityPlayer.class,
                new AxisAlignedBB(
                        pos.getX() - 100, pos.getY() - 50, pos.getZ() - 100,
                        pos.getX() + 100, pos.getY() + 50, pos.getZ() + 100
                )
        );

        String colorCode = bossType == BossType.RIFTWARDEN ? "§5" : "§7";
        String message = String.format(
                "%s§l%s已被 §e%s %s§l召唤！准备战斗！",
                colorCode,
                bossType.getDisplayName(),
                activator.getName(),
                colorCode
        );

        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(message));

            world.playSound(null, player.getPosition(),
                    SoundEvents.ENTITY_WITHER_SPAWN,
                    SoundCategory.HOSTILE, 0.5F, 0.5F);
        }
    }

    private static boolean hasBossNearby(World world, BlockPos pos, double range) {
        // 检查虚空守望者
        List<EntityRiftwarden> riftwardens = world.getEntitiesWithinAABB(
                EntityRiftwarden.class,
                new AxisAlignedBB(
                        pos.getX() - range, pos.getY() - range, pos.getZ() - range,
                        pos.getX() + range, pos.getY() + range, pos.getZ() + range
                )
        );

        for (EntityRiftwarden boss : riftwardens) {
            if (boss.isEntityAlive()) {
                return true;
            }
        }

        // 检查石像哨兵
        List<EntityStoneSentinel> sentinels = world.getEntitiesWithinAABB(
                EntityStoneSentinel.class,
                new AxisAlignedBB(
                        pos.getX() - range, pos.getY() - range, pos.getZ() - range,
                        pos.getX() + range, pos.getY() + range, pos.getZ() + range
                )
        );

        for (EntityStoneSentinel boss : sentinels) {
            if (boss.isEntityAlive()) {
                return true;
            }
        }

        return false;
    }

    private static int countPlayersNearby(World world, BlockPos pos, double range) {
        List<EntityPlayer> players = world.getEntitiesWithinAABB(
                EntityPlayer.class,
                new AxisAlignedBB(
                        pos.getX() - range, pos.getY() - range, pos.getZ() - range,
                        pos.getX() + range, pos.getY() + range, pos.getZ() + range
                ),
                player -> player != null && player.isEntityAlive() && !player.isSpectator()
        );

        return players.size();
    }
}