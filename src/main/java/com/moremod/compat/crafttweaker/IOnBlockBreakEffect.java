package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.player.IPlayer;
import crafttweaker.api.world.IWorld;
import crafttweaker.api.world.IBlockPos;
import crafttweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.moremod.IOnBlockBreakEffect")
public interface IOnBlockBreakEffect extends IUpgradeEffect {
    void onBlockBreak(IPlayer player, IWorld world, IBlockPos pos, IItemStack sword);
}