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

            // 清除备份状态，下次死亡需要重新检测剑
            // 防止玩家在没有剑的情况下继续受保护
            ZhuxianSword.updateSkillBackup(player, ZhuxianSword.NBT_SKILL_LIMING, false);

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
     * 检查并限制伤害，确保血量不低于20%
     * Called by ASM at HEAD of EntityLivingBase.damageEntity
     *
     * @param entity 受伤的实体
     * @param source 伤害来源
     * @param damage 伤害量
     * @return true = 取消伤害（已锁血）
     */
    public static boolean checkAndLimitDamage(EntityLivingBase entity, DamageSource source, float damage) {
        try {
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            // 检查是否激活"为生民立命"技能
            if (!ZhuxianSword.isPlayerSkillActive(player, ZhuxianSword.NBT_SKILL_LIMING)) {
                return false;
            }

            // 计算最低血量阈值（20%）
            float maxHealth = player.getMaxHealth();
            float minHealth = Math.max(1.0f, maxHealth * HEALTH_THRESHOLD);
            float currentHealth = player.getHealth();

            // 如果伤害会导致血量低于阈值
            if (currentHealth - damage < minHealth) {
                // 计算允许的最大伤害
                float allowedDamage = currentHealth - minHealth;

                if (allowedDamage <= 0) {
                    // 已经在最低血量或更低，完全阻止伤害
                    return true;
                }

                // 允许部分伤害，但确保血量不低于阈值
                // 由于我们不能修改damage参数，需要手动设置血量
                player.setHealth(minHealth);

                // 显示保护提示（每5秒最多显示一次）
                long worldTime = player.world.getTotalWorldTime();
                long lastNotify = player.getEntityData().getLong("ZhuxianLimingLastNotify");
                if (worldTime - lastNotify > 100) { // 5秒
                    player.getEntityData().setLong("ZhuxianLimingLastNotify", worldTime);
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.GOLD + "【为生民立命】" + TextFormatting.WHITE + " 锁血保护！"
                    ), true);
                }

                return true; // 阻止原始伤害，血量已设置
            }

            return false; // 允许伤害
        } catch (Throwable t) {
            System.err.println("[ZhuxianDeathHook] Error in checkAndLimitDamage: " + t.getMessage());
            return false;
        }
    }

    // ========== Hook 3: setDead（终极防线） ==========

    /**
     * 检查是否应该阻止 setDead() 调用
     * Called by ASM at HEAD of Entity.setDead
     *
     * @param entity 将要被标记为死亡的实体
     * @return true = 阻止 setDead
     */
    public static boolean shouldPreventSetDead(net.minecraft.entity.Entity entity) {
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

            // 粒子效果
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

            // 清除备份状态
            ZhuxianSword.updateSkillBackup(player, ZhuxianSword.NBT_SKILL_LIMING, false);

            System.out.println("[ZhuxianDeathHook] setDead intercepted for " + player.getName());
            return true;

        } catch (Throwable t) {
            System.err.println("[ZhuxianDeathHook] Error in shouldPreventSetDead: " + t.getMessage());
            return false;
        }
    }
}
