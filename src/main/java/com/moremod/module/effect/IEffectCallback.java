package com.moremod.module.effect;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * 效果回调接口 - 用于自定义效果逻辑
 */
@FunctionalInterface
public interface IEffectCallback {

    /**
     * 执行效果
     *
     * @param player 玩家
     * @param coreStack 机械核心物品
     * @param moduleId 模块ID
     * @param level 模块等级
     * @param context 上下文对象 (可能是事件对象或其他数据)
     */
    void execute(EntityPlayer player, ItemStack coreStack, String moduleId, int level, Object context);
}
