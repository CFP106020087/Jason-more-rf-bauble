package com.moremod.compat.iceandfire;

import com.github.alexthe666.iceandfire.api.ChainLightningUtils;
import com.github.alexthe666.iceandfire.api.IEntityEffectCapability;
import com.github.alexthe666.iceandfire.api.InFCapabilities;
import com.github.alexthe666.iceandfire.entity.EntityFireDragon;
import com.github.alexthe666.iceandfire.entity.EntityIceDragon;
import com.moremod.compat.crafttweaker.IOnHitEffect;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.entity.IEntityLivingBase;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.player.IPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("mods.moremod.IceAndFireEffects")
public class IceAndFireEffects {

    // ═══════════════════════════════════════════════════════
    // 静态工厂方法 - 供ZenScript调用
    // ═══════════════════════════════════════════════════════

    @ZenMethod
    public static IOnHitEffect createFireEffect() {
        return new FireEffect();
    }

    @ZenMethod
    public static IOnHitEffect createIceEffect() {
        return new IceEffect();
    }

    @ZenMethod
    public static IOnHitEffect createLightningEffect() {
        return new LightningEffect();
    }

    @ZenMethod
    public static IOnHitEffect createTripleEffect() {
        return new TripleEffect();
    }

    // ═══════════════════════════════════════════════════════
    // 火龙效果
    // ═══════════════════════════════════════════════════════

    @ZenMethod
    public static void applyFireEffect(IEntityLivingBase target, IEntityLivingBase attacker) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        EntityLivingBase mcAttacker = CraftTweakerMC.getEntityLivingBase(attacker);

        mcTarget.setFire(5);
        mcTarget.knockBack(mcTarget, 1F,
                mcAttacker.posX - mcTarget.posX,
                mcAttacker.posZ - mcTarget.posZ);

        if (mcTarget instanceof EntityIceDragon) {
            mcTarget.attackEntityFrom(DamageSource.IN_FIRE, 8F);
        }
    }

    // ═══════════════════════════════════════════════════════
    // 冰龙效果
    // ═══════════════════════════════════════════════════════

    @ZenMethod
    public static void applyIceEffect(IEntityLivingBase target, IEntityLivingBase attacker) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        EntityLivingBase mcAttacker = CraftTweakerMC.getEntityLivingBase(attacker);

        if (!mcTarget.world.isRemote) {
            IEntityEffectCapability capability = InFCapabilities.getEntityEffectCapability(mcTarget);
            if (capability != null) {
                capability.setFrozen(200);
            }
        }

        mcTarget.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 100, 2));
        mcTarget.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 100, 2));
        mcTarget.knockBack(mcTarget, 1F,
                mcAttacker.posX - mcTarget.posX,
                mcAttacker.posZ - mcTarget.posZ);

        if (mcTarget instanceof EntityFireDragon) {
            mcTarget.attackEntityFrom(DamageSource.DROWN, 8F);
        }
    }

    // ═══════════════════════════════════════════════════════
    // 雷电效果
    // ═══════════════════════════════════════════════════════

    @ZenMethod
    public static void applyLightningEffect(IEntityLivingBase target, IEntityLivingBase attacker) {
        EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
        EntityLivingBase mcAttacker = CraftTweakerMC.getEntityLivingBase(attacker);

        ChainLightningUtils.createChainLightningFromTarget(
                mcTarget.world,
                mcTarget,
                mcAttacker
        );

        mcTarget.knockBack(mcTarget, 1F,
                mcAttacker.posX - mcTarget.posX,
                mcAttacker.posZ - mcTarget.posZ);

        if (mcTarget instanceof EntityFireDragon || mcTarget instanceof EntityIceDragon) {
            mcTarget.attackEntityFrom(DamageSource.LIGHTNING_BOLT, 4F);
        }
    }

    // ═══════════════════════════════════════════════════════
    // 组合效果
    // ═══════════════════════════════════════════════════════

    @ZenMethod
    public static void applyTripleEffect(IEntityLivingBase target, IEntityLivingBase attacker) {
        applyFireEffect(target, attacker);
        applyIceEffect(target, attacker);
        applyLightningEffect(target, attacker);
    }

    // ═══════════════════════════════════════════════════════
    // 内部类实现
    // ═══════════════════════════════════════════════════════

    public static class FireEffect implements IOnHitEffect {
        @Override
        public void onHit(IPlayer attacker, IEntityLivingBase target, IItemStack sword, float damage) {
            applyFireEffect(target, attacker);
        }
    }

    public static class IceEffect implements IOnHitEffect {
        @Override
        public void onHit(IPlayer attacker, IEntityLivingBase target, IItemStack sword, float damage) {
            applyIceEffect(target, attacker);
        }
    }

    public static class LightningEffect implements IOnHitEffect {
        @Override
        public void onHit(IPlayer attacker, IEntityLivingBase target, IItemStack sword, float damage) {
            applyLightningEffect(target, attacker);
        }
    }

    public static class TripleEffect implements IOnHitEffect {
        @Override
        public void onHit(IPlayer attacker, IEntityLivingBase target, IItemStack sword, float damage) {
            applyTripleEffect(target, attacker);
        }
    }
}