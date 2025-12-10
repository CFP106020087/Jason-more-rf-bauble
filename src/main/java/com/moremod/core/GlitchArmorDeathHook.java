package com.moremod.core;

import com.moremod.item.armor.ItemGlitchArmor;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 故障盔甲死亡鉤子 - ASM注入版
 * Glitch Armor Death Hook - ASM Injected
 *
 * 此類由 ASM Transformer 調用，提供多層死亡保護：
 * 1. damageEntity - 檢測致命傷害並觸發NULL異常
 * 2. onDeath - 最終防線
 *
 * NULL異常效果：
 * - 穿戴胸甲時，致命傷害有20%機率被完全消除
 * - 全套增加10%觸發機率（共30%）
 * - 觸發後獲得1秒閃爍無敵
 * - 30秒冷卻時間
 */
public class GlitchArmorDeathHook {

    private static final Random RANDOM = new Random();

    // NULL觸發冷卻
    private static final Map<UUID, Long> NULL_COOLDOWNS = new HashMap<>();
    private static final long NULL_COOLDOWN_MS = 30000; // 30秒冷卻

    // 閃爍無敵狀態
    private static final Map<UUID, Long> FLICKER_END_TIMES = new HashMap<>();

    // ========== Hook 1: damageEntity ==========

    /**
     * 檢查並觸發NULL異常
     * Called by ASM at HEAD of EntityLivingBase.damageEntity
     *
     * @param entity 受傷的實體
     * @param source 傷害來源
     * @param damage 最終傷害量（護甲後）
     * @return true = 取消傷害（NULL觸發）
     */
    public static boolean checkAndTriggerNull(EntityLivingBase entity, DamageSource source, float damage) {
        try {
            // 快速路徑：非玩家直接放行
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            // 檢查是否穿戴胸甲
            if (!ItemGlitchArmor.hasArmorPiece(player, EntityEquipmentSlot.CHEST)) {
                return false;
            }

            // 檢查是否在閃爍無敵狀態
            Long flickerEnd = FLICKER_END_TIMES.get(player.getUniqueID());
            if (flickerEnd != null && System.currentTimeMillis() < flickerEnd) {
                // 閃爍無敵中，取消傷害
                spawnGlitchParticles(player, 5);
                return true;
            }

            // 檢測致命傷害：當前血量 - 傷害 <= 0
            float currentHealth = player.getHealth();
            if (currentHealth - damage > 0) {
                return false; // 不是致命傷害
            }

            // 檢查冷卻
            Long lastNull = NULL_COOLDOWNS.get(player.getUniqueID());
            if (lastNull != null && System.currentTimeMillis() - lastNull < NULL_COOLDOWN_MS) {
                return false; // 冷卻中
            }

            // 計算觸發機率
            float nullChance = 0.20f;
            if (ItemGlitchArmor.hasFullSet(player)) {
                nullChance += 0.10f; // 全套30%
            }

            // 嘗試觸發NULL異常
            if (RANDOM.nextFloat() < nullChance) {
                triggerNullException(player);
                return true;
            }

            return false;

        } catch (Throwable t) {
            System.err.println("[GlitchArmorDeathHook] Error in checkAndTriggerNull: " + t.getMessage());
            return false;
        }
    }

    // ========== Hook 2: onDeath（最終防線） ==========

    /**
     * 檢查是否應該攔截死亡
     * Called by ASM at HEAD of EntityLivingBase.onDeath
     *
     * @param entity 將要死亡的實體
     * @param source 傷害來源
     * @return true = 阻止死亡
     */
    public static boolean shouldPreventDeath(EntityLivingBase entity, DamageSource source) {
        try {
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            // 檢查是否穿戴胸甲
            if (!ItemGlitchArmor.hasArmorPiece(player, EntityEquipmentSlot.CHEST)) {
                return false;
            }

            // 如果在閃爍狀態，阻止死亡
            Long flickerEnd = FLICKER_END_TIMES.get(player.getUniqueID());
            if (flickerEnd != null && System.currentTimeMillis() < flickerEnd) {
                if (player.getHealth() < 0.5f) {
                    player.setHealth(2.0f);
                }
                return true;
            }

            // 檢查冷卻
            Long lastNull = NULL_COOLDOWNS.get(player.getUniqueID());
            if (lastNull != null && System.currentTimeMillis() - lastNull < NULL_COOLDOWN_MS) {
                return false; // 冷卻中，允許死亡
            }

            // 最終防線：觸發NULL異常
            float nullChance = 0.20f;
            if (ItemGlitchArmor.hasFullSet(player)) {
                nullChance += 0.10f;
            }

            if (RANDOM.nextFloat() < nullChance) {
                triggerNullException(player);
                player.setHealth(2.0f);
                System.out.println("[GlitchArmorDeathHook] Final defense: NULL Exception for " + player.getName());
                return true;
            }

            return false;

        } catch (Throwable t) {
            System.err.println("[GlitchArmorDeathHook] Error in shouldPreventDeath: " + t.getMessage());
            return false;
        }
    }

    // ========== 內部方法 ==========

    /**
     * 觸發NULL異常效果
     */
    private static void triggerNullException(EntityPlayer player) {
        // 記錄冷卻
        NULL_COOLDOWNS.put(player.getUniqueID(), System.currentTimeMillis());

        // 設置1秒閃爍無敵
        FLICKER_END_TIMES.put(player.getUniqueID(), System.currentTimeMillis() + 1000);

        // 恢復血量
        player.setHealth(Math.max(player.getHealth(), 4.0f));

        // 視覺和音效
        spawnNullEffect(player);

        // 訊息
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "\u00a7l[NULL] " +
                TextFormatting.GRAY + "NullPointerException: damage == null"), true);
    }

    private static void spawnNullEffect(EntityPlayer player) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        // 播放音效
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 2.0f);
        world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.5f, 0.5f);

        // 故障粒子效果
        for (int i = 0; i < 40; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 3;
            double offsetY = RANDOM.nextDouble() * 2.5;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 3;

            world.spawnParticle(EnumParticleTypes.PORTAL,
                    player.posX + offsetX, player.posY + offsetY, player.posZ + offsetZ,
                    1, 0, 0, 0, 0.1);
        }

        // 中心爆發效果
        world.spawnParticle(EnumParticleTypes.END_ROD,
                player.posX, player.posY + 1, player.posZ,
                30, 0.8, 0.8, 0.8, 0.15);
    }

    private static void spawnGlitchParticles(EntityPlayer player, int count) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        for (int i = 0; i < count; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 1.5;
            double offsetY = RANDOM.nextDouble() * 2;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 1.5;

            world.spawnParticle(EnumParticleTypes.PORTAL,
                    player.posX + offsetX, player.posY + offsetY, player.posZ + offsetZ,
                    1, 0, 0, 0.0, 0);
        }
    }

    /**
     * 檢查閃爍無敵狀態（供外部查詢）
     */
    public static boolean isInFlickerState(UUID playerId) {
        Long flickerEnd = FLICKER_END_TIMES.get(playerId);
        return flickerEnd != null && System.currentTimeMillis() < flickerEnd;
    }

    /**
     * 設置閃爍狀態（供FirstAid兼容層調用）
     */
    public static void setFlickerState(UUID playerId, long endTime) {
        FLICKER_END_TIMES.put(playerId, endTime);
    }

    /**
     * 清理玩家數據
     */
    public static void cleanupPlayer(UUID playerId) {
        NULL_COOLDOWNS.remove(playerId);
        FLICKER_END_TIMES.remove(playerId);
    }
}
