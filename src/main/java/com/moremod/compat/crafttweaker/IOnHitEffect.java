package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.entity.IEntityLivingBase;
import crafttweaker.api.player.IPlayer;
import crafttweaker.api.item.IItemStack;
import stanhebben.zenscript.annotations.ZenClass;

@ZenRegister
@ZenClass("mods.moremod.IOnHitEffect")
public interface IOnHitEffect extends IUpgradeEffect {
    void onHit(IPlayer attacker, IEntityLivingBase target, IItemStack sword, float damage);
}