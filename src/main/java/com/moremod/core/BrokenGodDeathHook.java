package com.moremod.core;

import com.moremod.system.ascension.BrokenGodHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

/**
 * 破碎之神死亡钩子（增强版）
 * Broken God Death Hook (Enhanced)
 *
 * 此类由 ASM Transformer 调用，提供多层死亡保护：
 * 1. attackEntityFrom - 停机期间完全免疫攻击
 * 2. damageEntity - 检测致命伤害并提前触发停机
 * 3. onDeath - 最终防线
 *
 * 必须在游戏早期加载，不依赖任何延迟初始化的类
 */
public class BrokenGodDeathHook {

    // ========== Hook 1: attackEntityFrom ==========

    /**
     * 检查是否应该取消攻击
     * Called by ASM at HEAD of EntityLivingBase.attackEntityFrom
     *
     * @param entity 被攻击的实体
     * @param source 伤害来源
     * @param amount 伤害量
     * @return true = 取消攻击（return false 给调用者）
     */
    public static boolean shouldCancelAttack(EntityLivingBase entity, DamageSource source, float amount) {
        try {
            // 快速路径：非玩家直接放行
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            // 非破碎之神放行
            if (!BrokenGodHandler.isBrokenGod(player)) {
                return false;
            }

            // 停机期间：完全免疫所有攻击
            if (BrokenGodHandler.isInShutdown(player)) {
                return true;
            }

            return false;

        } catch (Throwable t) {
            System.err.println("[BrokenGodDeathHook] Error in shouldCancelAttack: " + t.getMessage());
            return false;
        }
    }

    // ========== Hook 2: damageEntity ==========

    /**
     * 检查并触发停机模式
     * Called by ASM at HEAD of EntityLivingBase.damageEntity
     *
     * @param entity 受伤的实体
     * @param source 伤害来源
     * @param damage 最终伤害量（护甲后）
     * @return true = 取消伤害并进入停机
     */
    public static boolean checkAndTriggerShutdown(EntityLivingBase entity, DamageSource source, float damage) {
        try {
            // 快速路径：非玩家直接放行
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            // 非破碎之神放行
            if (!BrokenGodHandler.isBrokenGod(player)) {
                return false;
            }

            // 已在停机：取消伤害
            if (BrokenGodHandler.isInShutdown(player)) {
                return true;
            }

            // 检测致命伤害：当前血量 - 伤害 <= 0
            float currentHealth = player.getHealth();
            if (currentHealth - damage <= 0) {
                // 致命伤害！进入停机模式
                BrokenGodHandler.enterShutdown(player);
                System.out.println("[BrokenGodDeathHook] Fatal damage detected, entering shutdown: " + player.getName());
                return true;
            }

            return false;

        } catch (Throwable t) {
            System.err.println("[BrokenGodDeathHook] Error in checkAndTriggerShutdown: " + t.getMessage());
            return false;
        }
    }

    // ========== Hook 3: onDeath（最终防线） ==========

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

            // 非破碎之神放行
            if (!BrokenGodHandler.isBrokenGod(player)) {
                return false;
            }

            // 如果已经在停机状态，阻止死亡并确保安全血量
            if (BrokenGodHandler.isInShutdown(player)) {
                if (player.getHealth() < 0.5f) {
                    player.setHealth(0.5f);
                }
                return true;
            }

            // 最终防线：进入停机模式
            BrokenGodHandler.enterShutdown(player);
            System.out.println("[BrokenGodDeathHook] Final defense: Intercepted death for " + player.getName());

            return true;

        } catch (Throwable t) {
            System.err.println("[BrokenGodDeathHook] Error in shouldPreventDeath: " + t.getMessage());
            return false;
        }
    }

    /**
     * 兼容方法
     */
    public static boolean onPreLivingDeath(EntityLivingBase entity, DamageSource source) {
        return shouldPreventDeath(entity, source);
    }

    // ========== Hook 4: setDead（终极防线） ==========

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

            // 非破碎之神放行
            if (!BrokenGodHandler.isBrokenGod(player)) {
                return false;
            }

            // 停机期间：阻止 setDead
            if (BrokenGodHandler.isInShutdown(player)) {
                if (player.getHealth() < 0.5f) {
                    player.setHealth(0.5f);
                }
                return true;
            }

            // 最终防线：进入停机模式
            BrokenGodHandler.enterShutdown(player);
            System.out.println("[BrokenGodDeathHook] setDead intercepted, entering shutdown: " + player.getName());
            return true;

        } catch (Throwable t) {
            System.err.println("[BrokenGodDeathHook] Error in shouldPreventSetDead: " + t.getMessage());
            return false;
        }
    }
}
