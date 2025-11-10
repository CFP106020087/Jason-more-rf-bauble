package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.player.IPlayer;
import crafttweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.moremod.IOnTickEffect")
public interface IOnTickEffect extends IUpgradeEffect {
    void onTick(IPlayer player, IItemStack sword, int tickCount);
}