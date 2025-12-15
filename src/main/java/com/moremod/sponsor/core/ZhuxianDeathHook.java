package com.moremod.sponsor.core;

import com.moremod.sponsor.item.ZhuxianSword;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

/**
 * 诛仙剑死亡钩子
 * Zhuxian Death Hook
 *
 * 此类由 ASM Transformer 调用，提供"为生民立命"技能的死亡保护
 * 参考香巴拉实现，但无需能量消耗
 *
 * 与香巴拉的区别：
 * - 香巴拉：消耗能量阻止死亡，没能量则死亡
 * - 诛仙剑：只要技能激活，无条件锁血20%
 *
 * 集成方式：在 moremodTransformer 的 onDeath 注入中调用
 */
public class ZhuxianDeathHook {

    /** 血量保护阈值：20% */
    private static final float HEALTH_THRESHOLD = 0.2f;

    /**
     * 检查是否应该拦截死亡（最终防线）
     * Called by ASM at HEAD of EntityLivingBase.onDeath
     *
     * 参考香巴拉 ShambhalaDeathHook.shouldPreventDeath
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

            // 检查是否激活"为生民立命"技能
            if (!ZhuxianSword.isPlayerSkillActive(player, ZhuxianSword.NBT_SKILL_LIMING)) {
                return false;
            }

            // 恢复到最低血量（20%）
            float minHealth = Math.max(1.0f, player.getMaxHealth() * HEALTH_THRESHOLD);
            player.setHealth(minHealth);

            // 粒子效果（参考香巴拉）
            if (player.world instanceof WorldServer) {
                WorldServer ws = (WorldServer) player.world;
                ws.spawnParticle(EnumParticleTypes.TOTEM,
                        player.posX, player.posY + player.height / 2, player.posZ,
                        30, 0.5, 0.5, 0.5, 0.2);
            }

            // 提示玩家
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GOLD + "【为生民立命】" + TextFormatting.WHITE + " 阻止了死亡！"
            ), true);

            return true;

        } catch (Throwable t) {
            System.err.println("[ZhuxianDeathHook] Error in shouldPreventDeath: " + t.getMessage());
            return false;
        }
    }

    /**
     * 兼容方法（旧API）
     */
    public static boolean onPreLivingDeath(EntityLivingBase entity, DamageSource source) {
        return shouldPreventDeath(entity, source);
    }

    /**
     * 保留空方法以兼容旧的ASM调用（damageEntity不再使用）
     * @deprecated 不再使用，为生民立命改为只在onDeath拦截
     */
    @Deprecated
    public static boolean checkAndLimitDamage(EntityLivingBase entity, DamageSource source, float damage) {
        return false; // 不再拦截伤害，只拦截死亡
    }
}
