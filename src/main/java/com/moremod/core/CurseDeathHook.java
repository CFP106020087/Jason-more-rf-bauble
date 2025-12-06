package com.moremod.core;

import baubles.api.BaublesApi;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 七咒死亡钩子
 * Curse Death Hook
 *
 * 此类由 ASM Transformer 调用，提供虚无之眸的死亡保护：
 * - damageEntity HEAD - 检测致命伤害，消耗经验阻止
 * - onDeath HEAD - 最终防线
 *
 * 虚无之眸效果：
 * - 致命伤害时消耗经验抵御死亡
 * - 每次触发消耗 10 级经验
 * - 触发后有 60 秒冷却时间
 */
public class CurseDeathHook {

    // 经验消耗量（级数）
    private static final int XP_LEVEL_COST = 10;
    // 冷却时间（毫秒）
    private static final long COOLDOWN_MS = 60000; // 60秒
    // 触发后恢复的血量
    private static final float RECOVERY_HEALTH = 4.0f;

    // 冷却时间记录
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    // ========== Hook 1: damageEntity ==========

    /**
     * 检查并尝试用经验阻止致命伤害
     * Called by ASM at HEAD of EntityLivingBase.damageEntity
     *
     * @param entity 受伤的实体
     * @param source 伤害来源
     * @param damage 最终伤害量（护甲后）
     * @return true = 取消伤害（用经验抵消了）
     */
    public static boolean checkAndPreventFatalDamage(EntityLivingBase entity, DamageSource source, float damage) {
        try {
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            // 检查是否有七咒之戒和虚无之眸
            if (!hasVoidGaze(player)) {
                return false;
            }

            if (!hasCursedRing(player)) {
                return false;
            }

            // 检测致命伤害：当前血量 - 伤害 <= 0
            float currentHealth = player.getHealth();
            if (currentHealth - damage <= 0) {
                // 致命伤害！尝试用经验阻止
                return tryPreventDeathWithXP(player, source);
            }

            return false;

        } catch (Throwable t) {
            System.err.println("[CurseDeathHook] Error in checkAndPreventFatalDamage: " + t.getMessage());
            return false;
        }
    }

    // ========== Hook 2: onDeath（最终防线） ==========

    /**
     * 检查是否应该拦截死亡
     * Called by ASM at HEAD of EntityLivingBase.onDeath
     *
     * @param entity 将要死亡的实体
     * @param source 伤害来源
     * @return true = 阻止死亡
     */
    public static boolean shouldPreventDeath(EntityLivingBase entity, DamageSource source) {
        try {
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            // 检查是否有七咒之戒和虚无之眸
            if (!hasVoidGaze(player)) {
                return false;
            }

            if (!hasCursedRing(player)) {
                return false;
            }

            // 最终防线：尝试用经验阻止死亡
            return tryPreventDeathWithXP(player, source);

        } catch (Throwable t) {
            System.err.println("[CurseDeathHook] Error in shouldPreventDeath: " + t.getMessage());
            return false;
        }
    }

    /**
     * 尝试用经验阻止死亡
     */
    private static boolean tryPreventDeathWithXP(EntityPlayer player, DamageSource source) {
        UUID playerId = player.getUniqueID();

        // 检查冷却
        Long cooldownEnd = COOLDOWNS.get(playerId);
        if (cooldownEnd != null && System.currentTimeMillis() < cooldownEnd) {
            // 仍在冷却中
            return false;
        }

        // 检查经验是否足够
        if (player.experienceLevel < XP_LEVEL_COST) {
            // 经验不足，无法阻止死亡
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "虚无之眸低语：" +
                    TextFormatting.GRAY + "经验不足，无法凝视深渊..."
            ));
            return false;
        }

        // 消耗经验
        player.addExperienceLevel(-XP_LEVEL_COST);

        // 恢复血量
        player.setHealth(RECOVERY_HEALTH);

        // 设置冷却
        COOLDOWNS.put(playerId, System.currentTimeMillis() + COOLDOWN_MS);

        // 效果提示
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "☠ 虚无之眸凝视深渊！" +
                TextFormatting.GRAY + " 消耗 " +
                TextFormatting.GREEN + XP_LEVEL_COST + " 级经验" +
                TextFormatting.GRAY + " 抵御了死亡"
        ));

        // 粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            // 黑色/紫色粒子环绕
            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    player.posX, player.posY + 1.0, player.posZ,
                    50, 0.5, 1.0, 0.5, 0.1);
            ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                    player.posX, player.posY + 1.0, player.posZ,
                    30, 0.3, 0.8, 0.3, 0.0);
        }

        return true;
    }

    // ========== 辅助方法 ==========

    /**
     * 检查玩家是否佩戴七咒之戒
     */
    public static boolean hasCursedRing(EntityPlayer player) {
        try {
            for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
                ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
                if (!bauble.isEmpty() &&
                    bauble.getItem().getRegistryName() != null &&
                    bauble.getItem().getRegistryName().toString().equals("enigmaticlegacy:cursed_ring")) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * 检查玩家是否佩戴虚无之眸
     */
    public static boolean hasVoidGaze(EntityPlayer player) {
        try {
            for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
                ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
                if (!bauble.isEmpty() &&
                    bauble.getItem().getRegistryName() != null &&
                    bauble.getItem().getRegistryName().toString().equals("moremod:void_gaze")) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * 检查是否在冷却中
     */
    public static boolean isOnCooldown(EntityPlayer player) {
        Long cooldownEnd = COOLDOWNS.get(player.getUniqueID());
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }

    /**
     * 获取剩余冷却时间（秒）
     */
    public static int getRemainingCooldown(EntityPlayer player) {
        Long cooldownEnd = COOLDOWNS.get(player.getUniqueID());
        if (cooldownEnd == null) return 0;
        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }

    /**
     * 兼容方法
     */
    public static boolean onPreLivingDeath(EntityLivingBase entity, DamageSource source) {
        return shouldPreventDeath(entity, source);
    }

    /**
     * 清理玩家状态（玩家退出时调用）
     */
    public static void cleanupPlayer(UUID playerId) {
        COOLDOWNS.remove(playerId);
    }

    /**
     * 清空所有状态（世界卸载时调用）
     */
    public static void clearAllState() {
        COOLDOWNS.clear();
    }
}
