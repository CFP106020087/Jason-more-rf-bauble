package com.moremod.compat.crafttweaker;

import com.moremod.compat.IceAndFireReflectionHelper;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.entity.IEntityLivingBase;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * Ice and Fire 模组效果整合 (软依赖版)
 * 提供冰冻、火焰、闪电链等特效
 * 所有 Ice and Fire 功能通过反射调用，模组不存在时安全降级
 */
@ZenRegister
@ZenClass("mods.moremod.IceAndFireHelper")
public class IceAndFireHelper {

    /**
     * 火龙效果 - 点燃+击退+对冰龙额外伤害
     */
    @ZenMethod
    public static boolean applyFireEffect(IEntityLivingBase target, IEntityLivingBase attacker,
                                         int fireDuration, float knockbackStrength, float dragonBonus) {
        try {
            EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
            EntityLivingBase mcAttacker = CraftTweakerMC.getEntityLivingBase(attacker);

            if (mcTarget == null || mcAttacker == null) return false;

            // 点燃
            mcTarget.setFire(fireDuration);

            // 击退
            mcTarget.knockBack(mcTarget, knockbackStrength,
                    mcAttacker.posX - mcTarget.posX,
                    mcAttacker.posZ - mcTarget.posZ);

            // 对冰龙额外伤害 (使用反射检查)
            if (IceAndFireReflectionHelper.isIceDragon(mcTarget)) {
                mcTarget.attackEntityFrom(DamageSource.IN_FIRE, dragonBonus);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 冰龙效果 - 冰冻+减速+挖掘疲劳+击退+对火龙额外伤害
     */
    @ZenMethod
    public static boolean applyIceEffect(IEntityLivingBase target, IEntityLivingBase attacker,
                                        int frozenTicks, int slownessDuration, float knockbackStrength,
                                        float dragonBonus) {
        try {
            EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
            EntityLivingBase mcAttacker = CraftTweakerMC.getEntityLivingBase(attacker);

            if (mcTarget == null || mcAttacker == null) return false;

            // Ice and Fire 冰冻效果 (使用反射调用)
            if (!mcTarget.world.isRemote) {
                IceAndFireReflectionHelper.setFrozen(mcTarget, frozenTicks);
            }

            // 减速+挖掘疲劳
            mcTarget.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, slownessDuration, 2));
            mcTarget.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, slownessDuration, 2));

            // 击退
            mcTarget.knockBack(mcTarget, knockbackStrength,
                    mcAttacker.posX - mcTarget.posX,
                    mcAttacker.posZ - mcTarget.posZ);

            // 对火龙额外伤害 (使用反射检查)
            if (IceAndFireReflectionHelper.isFireDragon(mcTarget)) {
                mcTarget.attackEntityFrom(DamageSource.DROWN, dragonBonus);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 雷电效果 - 闪电链+击退+对龙额外伤害
     */
    @ZenMethod
    public static boolean applyLightningEffect(IEntityLivingBase target, IEntityLivingBase attacker,
                                               float knockbackStrength, float dragonBonus) {
        try {
            EntityLivingBase mcTarget = CraftTweakerMC.getEntityLivingBase(target);
            EntityLivingBase mcAttacker = CraftTweakerMC.getEntityLivingBase(attacker);

            if (mcTarget == null || mcAttacker == null) return false;

            // Ice and Fire 闪电链 (使用反射帮助类)
            boolean chainLightningSuccess = IceAndFireReflectionHelper.createChainLightningFromTarget(
                    mcTarget.world, mcTarget, mcAttacker,
                    new float[]{8.0f, 6.0f, 4.0f, 2.0f}, 4, false
            );

            if (!chainLightningSuccess) {
                // 如果Ice and Fire方法不存在或失败，使用原版闪电
                mcTarget.world.addWeatherEffect(new net.minecraft.entity.effect.EntityLightningBolt(
                    mcTarget.world, mcTarget.posX, mcTarget.posY, mcTarget.posZ, false
                ));
            }

            // 击退
            mcTarget.knockBack(mcTarget, knockbackStrength,
                    mcAttacker.posX - mcTarget.posX,
                    mcAttacker.posZ - mcTarget.posZ);

            // 对龙额外伤害 (使用反射检查)
            if (IceAndFireReflectionHelper.isDragon(mcTarget)) {
                mcTarget.attackEntityFrom(DamageSource.LIGHTNING_BOLT, dragonBonus);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 三重效果 - 火+冰+雷
     */
    @ZenMethod
    public static boolean applyTripleEffect(IEntityLivingBase target, IEntityLivingBase attacker) {
        boolean result = true;
        result &= applyFireEffect(target, attacker, 5, 1.0f, 8.0f);
        result &= applyIceEffect(target, attacker, 200, 100, 1.0f, 8.0f);
        result &= applyLightningEffect(target, attacker, 1.0f, 4.0f);
        return result;
    }

    /**
     * 检查是否为龙
     */
    @ZenMethod
    public static boolean isDragon(IEntityLivingBase entity) {
        try {
            EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
            return IceAndFireReflectionHelper.isDragon(mcEntity);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否为火龙
     */
    @ZenMethod
    public static boolean isFireDragon(IEntityLivingBase entity) {
        try {
            EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
            return IceAndFireReflectionHelper.isFireDragon(mcEntity);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否为冰龙
     */
    @ZenMethod
    public static boolean isIceDragon(IEntityLivingBase entity) {
        try {
            EntityLivingBase mcEntity = CraftTweakerMC.getEntityLivingBase(entity);
            return IceAndFireReflectionHelper.isIceDragon(mcEntity);
        } catch (Exception e) {
            return false;
        }
    }
}
