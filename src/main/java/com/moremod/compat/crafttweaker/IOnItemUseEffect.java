package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.player.IPlayer;
import crafttweaker.api.world.IWorld;
import crafttweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.ZenClass;

/**
 * 对空右键效果接口（不点击方块，直接右键使用物品）
 * 对应 PlayerInteractEvent.RightClickItem
 */
@ZenRegister
@ZenClass("mods.moremod.IOnItemUseEffect")
public interface IOnItemUseEffect extends IUpgradeEffect {
    /**
     * 当玩家对空右键使用剑时触发
     * @param player 玩家
     * @param world 世界
     * @param sword 剑物品
     */
    void onItemUse(IPlayer player, IWorld world, IItemStack sword);
}