package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.entity.IEntityLivingBase;
import crafttweaker.api.player.IPlayer;
import crafttweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.moremod.IOnKillEffect")
public interface IOnKillEffect extends IUpgradeEffect {
    void onKill(IPlayer player, IEntityLivingBase killed, IItemStack sword);
}