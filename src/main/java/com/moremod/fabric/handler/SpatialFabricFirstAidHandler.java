package com.moremod.fabric.handler;

import ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart;
import ichttt.mods.firstaid.api.event.FirstAidLivingDamageEvent;
import ichttt.mods.firstaid.common.network.MessageUpdatePart;
import ichttt.mods.firstaid.FirstAid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;

import java.util.*;
@Mod.EventBusSubscriber(modid = "moremod")

public class SpatialFabricFirstAidHandler {

    private static final Random RANDOM = new Random();
    private static final Map<UUID, Long> RESCUE_COOLDOWNS = new HashMap<>();
    private static final long COOLDOWN_TIME = 60000; // 60秒冷却

    /**
     * FirstAid致命伤害处理 - 虚空传送救援
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFirstAidDamage(FirstAidLivingDamageEvent event) {
        if(event.getEntityPlayer() == null || event.getEntityPlayer().world.isRemote) return;

        // 超高伤害不触发（防止秒杀）
        if(event.getUndistributedDamage() > 1000) return;

        EntityPlayer player = event.getEntityPlayer();
        FabricEventHandler.PlayerFabricData data = FabricEventHandler.getPlayerData(player);

        // 需要至少2件时空布料
        if(data.spatialCount < 2) return;

        // 检查冷却
        Long lastRescue = RESCUE_COOLDOWNS.get(player.getUniqueID());
        if(lastRescue != null && System.currentTimeMillis() - lastRescue < COOLDOWN_TIME) {
            long remaining = (COOLDOWN_TIME - (System.currentTimeMillis() - lastRescue)) / 1000;
            player.sendStatusMessage(new TextComponentString(
                    String.format("§c转移救援冷却中... 剩余%d秒", remaining)), true);
            return;
        }

        // 检查是否有致命部位
        boolean failed = false;
        List<AbstractDamageablePart> partsToHeal = new ArrayList<>();

        for(AbstractDamageablePart part : event.getAfterDamage()) {
            if(part.canCauseDeath && part.currentHealth <= 0.0F) {
                if(part.getMaxHealth() >= 4) {  // 只保护重要部位
                    partsToHeal.add(part);
                } else {
                    // 小部位致命不救援
                    failed = true;
                }
            }
        }

        // 如果有小部位致命，不触发救援
        if(failed || partsToHeal.isEmpty()) return;

        // 计算触发概率
        float baseChance = 0.3f + data.spatialCount * 0.2f; // 2件50%，3件70%，4件90%

        // 如果有大量存储伤害，增加概率
        if(data.storedDamage > 100) {
            baseChance = Math.min(1.0f, baseChance + 0.2f);
        }

        if(RANDOM.nextFloat() > baseChance) {
            player.sendStatusMessage(new TextComponentString(
                    "§c转移救援未触发！"), true);
            return;
        }

        // 立即恢复部位生命值，使用heal方法
        for(AbstractDamageablePart part : partsToHeal) {
            // 先恢复少量生命值以阻止死亡判定
            part.heal(1.0F, null, false);  // 先恢复1点，防止死亡

            // 然后恢复到更高的生命值
            float additionalHeal = part.getMaxHealth() * 0.5f - 1.0f;
            if(additionalHeal > 0) {
                part.heal(additionalHeal, null, false);
            }

            // 立即同步到客户端
            if(player instanceof EntityPlayerMP) {
                FirstAid.NETWORKING.sendTo(new MessageUpdatePart(part), (EntityPlayerMP)player);
            }
        }

        // 记录救援时间（在传送前记录，以便死亡事件能识别）
        RESCUE_COOLDOWNS.put(player.getUniqueID(), System.currentTimeMillis());

        // 执行完整的救援流程
        performVoidRescue(player, partsToHeal, data);
    }

    /**
     * 死亡事件备用保护
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if(!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        FabricEventHandler.PlayerFabricData data = FabricEventHandler.getPlayerData(player);

        // 如果有时空布料且刚刚触发了救援
        if(data.spatialCount >= 2) {
            Long lastRescue = RESCUE_COOLDOWNS.get(player.getUniqueID());
            // 如果2秒内触发过救援，取消死亡
            if(lastRescue != null && System.currentTimeMillis() - lastRescue < 2000) {
                event.setCanceled(true);
                player.setHealth(Math.max(1.0f, player.getMaxHealth() * 0.3f));

                player.sendStatusMessage(new TextComponentString(
                        "§5转移救援保护生效！"), true);
            }
        }
    }

    private static void performVoidRescue(EntityPlayer player, List<AbstractDamageablePart> healedParts,
                                          FabricEventHandler.PlayerFabricData data) {
        // 1. 传送玩家
        BlockPos safePos = findSafeLocation(player, data);

        // 传送前的虚空裂隙效果
        spawnVoidRiftEffect(player);

        // 执行传送
        player.setPositionAndUpdate(safePos.getX() + 0.5,
                safePos.getY() + 0.1,
                safePos.getZ() + 0.5);

        // 2. 应用强力保护
        player.hurtResistantTime = 200;  // 10秒受伤无敌
        player.setEntityInvulnerable(true);  // 临时完全无敌

        // 使用基于Tick的延迟任务替代 Thread.sleep（3秒后取消无敌）
        data.protectiveInvulEndTime = System.currentTimeMillis() + 3000;

        // 传送后效果
        spawnArrivalEffect(player);

        // 3. 消耗资源
        float energyCost = 50 - data.spatialCount * 5;  // 35-50能量
        data.dimensionalEnergy = Math.max(0, data.dimensionalEnergy - energyCost);

        // 消耗部分存储伤害
        if(data.storedDamage > 50) {
            data.storedDamage *= 0.7f;  // 保留70%
        }

        // 4. 通知玩家
        player.sendStatusMessage(new TextComponentString(
                String.format("§5§l⟐ 转移救援启动！⟐\n§d传送%d格，恢复%d个致命部位",
                        (int)player.getDistance(safePos.getX(), safePos.getY(), safePos.getZ()),
                        healedParts.size())), false);

        // 音效
        player.world.playSound(null, safePos, SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                SoundCategory.PLAYERS, 1.5F, 0.5F);
        player.world.playSound(null, safePos, SoundEvents.ENTITY_SHULKER_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.2F);

        // 同步数据到护甲
        FabricEventHandler.syncAllFabricDataToArmor(player, data);
    }

    private static BlockPos findSafeLocation(EntityPlayer player,
                                             FabricEventHandler.PlayerFabricData data) {
        // 传送距离基于装备数量
        int minDistance = 30 + data.spatialCount * 10;  // 50-70格
        int maxDistance = 60 + data.spatialCount * 15;  // 90-120格

        // 尝试找到安全位置
        for(int attempts = 0; attempts < 20; attempts++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            int distance = minDistance + RANDOM.nextInt(maxDistance - minDistance);

            int x = (int)(player.posX + Math.cos(angle) * distance);
            int z = (int)(player.posZ + Math.sin(angle) * distance);

            // 从天空向下寻找安全位置
            for(int y = 200; y > 10; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockPos below = pos.down();

                // 找到实心地面且上方两格为空
                if(!player.world.isAirBlock(below) &&
                        player.world.isAirBlock(pos) &&
                        player.world.isAirBlock(pos.up()) &&
                        player.world.getBlockState(below).isFullBlock()) {
                    return pos;
                }
            }
        }

        // 备用方案：传送到当前位置上方
        return player.getPosition().up(100);
    }

    private static void spawnVoidRiftEffect(EntityPlayer player) {
        if(!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        // 螺旋上升的传送门粒子
        for(int i = 0; i < 50; i++) {
            double angle = (Math.PI * 2 * i) / 10;
            double radius = 2.0 - (i / 50.0) * 1.5;
            double height = (i / 50.0) * 3;

            double x = player.posX + Math.cos(angle * i) * radius;
            double z = player.posZ + Math.sin(angle * i) * radius;
            double y = player.posY + height;

            world.spawnParticle(EnumParticleTypes.PORTAL,
                    x, y, z,
                    1, 0, 0.1, 0, 0.05);
        }

        // 中心爆发
        world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE,
                player.posX, player.posY + 1, player.posZ,
                1, 0, 0, 0.0, 0);

        // 额外的末影粒子环
        for(int ring = 0; ring < 3; ring++) {
            for(int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20;
                double r = (ring + 1) * 0.8;
                double x = player.posX + Math.cos(angle) * r;
                double z = player.posZ + Math.sin(angle) * r;

                world.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                        x, player.posY + ring * 0.5, z,
                        1, 0, 0.1, 0, 0.02);
            }
        }
    }

    private static void spawnArrivalEffect(EntityPlayer player) {
        if(!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        // 落地冲击波
        for(int ring = 1; ring <= 3; ring++) {
            int particles = 12 * ring;
            for(int i = 0; i < particles; i++) {
                double angle = (Math.PI * 2 * i) / particles;
                double radius = ring * 1.5;

                double x = player.posX + Math.cos(angle) * radius;
                double z = player.posZ + Math.sin(angle) * radius;

                world.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                        x, player.posY, z,
                        1, 0, 0.2, 0, 0.02);
            }
        }

        // 垂直光柱
        for(int y = 0; y < 10; y++) {
            world.spawnParticle(EnumParticleTypes.END_ROD,
                    player.posX, player.posY + y * 0.5, player.posZ,
                    3, 0, 0, 0, 0.05);
        }
    }
}