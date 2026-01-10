package com.moremod.core;

import com.moremod.item.curse.ItemScriptOfFifthAct;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;

import java.util.UUID;

/**
 * 第五幕剧本死亡钩子
 * Script of the Fifth Act Death Hook
 *
 * 此类由 ASM Transformer 调用，提供剧本的死亡拦截：
 * - damageEntity HEAD - 检测致命伤害，拦截并进入落幕状态
 * - onDeath HEAD - 最终防线
 *
 * 落幕状态效果（30秒）：
 * - 留下1血
 * - 受伤×2
 * - 禁止治疗
 * - 再次死亡无法阻止
 */
public class ScriptDeathHook {

    // ========== Hook 1: damageEntity ==========

    /**
     * 检查并尝试阻止致命伤害
     * Called by ASM at HEAD of EntityLivingBase.damageEntity
     *
     * @param entity 受伤的实体
     * @param source 伤害来源
     * @param damage 最终伤害量（护甲后）
     * @return true = 取消伤害（拦截死亡）
     */
    public static boolean checkAndPreventFatalDamage(EntityLivingBase entity, DamageSource source, float damage) {
        try {
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            // 检查是否佩戴剧本
            if (!ItemScriptOfFifthAct.hasScript(player)) {
                return false;
            }

            // 检查是否佩戴七咒之戒
            if (!CurseDeathHook.hasCursedRing(player)) {
                return false;
            }

            // 检查是否在落幕状态（落幕中无法再次拦截）
            if (ItemScriptOfFifthAct.isInCurtainFall(player)) {
                return false;
            }

            // 检测致命伤害：当前血量 - 伤害 <= 0
            float currentHealth = player.getHealth();
            if (currentHealth - damage <= 0) {
                // 致命伤害！拦截并进入落幕状态
                return preventDeathAndEnterCurtainFall(player);
            }

            return false;

        } catch (Throwable t) {
            System.err.println("[ScriptDeathHook] Error in checkAndPreventFatalDamage: " + t.getMessage());
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

            // 检查是否佩戴剧本
            if (!ItemScriptOfFifthAct.hasScript(player)) {
                return false;
            }

            // 检查是否佩戴七咒之戒
            if (!CurseDeathHook.hasCursedRing(player)) {
                return false;
            }

            // 检查是否在落幕状态（落幕中无法再次拦截）
            if (ItemScriptOfFifthAct.isInCurtainFall(player)) {
                // 落幕中死亡 = 真正死亡
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.DARK_RED + "「落幕...剧终」"
                ));
                return false;
            }

            // 最终防线：拦截死亡并进入落幕状态
            return preventDeathAndEnterCurtainFall(player);

        } catch (Throwable t) {
            System.err.println("[ScriptDeathHook] Error in shouldPreventDeath: " + t.getMessage());
            return false;
        }
    }

    /**
     * 拦截死亡并进入落幕状态
     */
    private static boolean preventDeathAndEnterCurtainFall(EntityPlayer player) {
        // 设置血量为1
        player.setHealth(1.0f);

        // 进入落幕状态
        ItemScriptOfFifthAct.enterCurtainFall(player);

        // 粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) player.world;
            // 黑暗粒子
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    player.posX, player.posY + 1.0, player.posZ,
                    50, 0.5, 1.0, 0.5, 0.1);
            ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                    player.posX, player.posY + 1.0, player.posZ,
                    30, 0.3, 0.8, 0.3, 0.0);
        }

        return true;
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
        ItemScriptOfFifthAct.cleanupPlayer(playerId);
    }

    /**
     * 清空所有状态（世界卸载时调用）
     */
    public static void clearAllState() {
        ItemScriptOfFifthAct.clearAllState();
    }
}
