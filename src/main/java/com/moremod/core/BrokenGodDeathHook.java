package com.moremod.core;

import com.moremod.system.ascension.BrokenGodHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

/**
 * 破碎之神死亡钩子
 * Broken God Death Hook
 *
 * 此类由 ASM Transformer 调用，用于在 EntityLivingBase.onDeath 之前拦截死亡
 * 必须在游戏早期加载，不依赖任何延迟初始化的类
 */
public class BrokenGodDeathHook {

    /**
     * 检查是否应该拦截死亡
     * Called by ASM before EntityLivingBase.onDeath
     *
     * @param entity 将要死亡的实体
     * @param source 伤害来源
     * @return true 表示应该阻止死亡（跳过原始 onDeath 方法）
     */
    public static boolean shouldPreventDeath(EntityLivingBase entity, DamageSource source) {
        try {
            if (!(entity instanceof EntityPlayer)) {
                return false;
            }

            EntityPlayer player = (EntityPlayer) entity;

            // 检查是否是破碎之神
            if (!BrokenGodHandler.isBrokenGod(player)) {
                return false;
            }

            // 如果已经在停机状态，阻止死亡
            if (BrokenGodHandler.isInShutdown(player)) {
                return true;
            }

            // 进入停机模式
            BrokenGodHandler.enterShutdown(player);

            System.out.println("[BrokenGodDeathHook] Intercepted death for Broken God player: " + player.getName());

            return true;

        } catch (Throwable t) {
            // 捕获所有异常，确保游戏不崩溃
            System.err.println("[BrokenGodDeathHook] Error in death hook: " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    /**
     * 替代 ForgeHooks.onLivingDeath 的前置检查
     * Called by ASM before ForgeHooks.onLivingDeath
     *
     * @param entity 将要死亡的实体
     * @param source 伤害来源
     * @return true 表示应该跳过死亡处理
     */
    public static boolean onPreLivingDeath(EntityLivingBase entity, DamageSource source) {
        return shouldPreventDeath(entity, source);
    }
}
