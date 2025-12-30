package com.moremod.compat.crafttweaker;

import com.github.alexthe666.iceandfire.api.IEntityEffectCapability;
import com.github.alexthe666.iceandfire.api.InFCapabilities;
import com.github.alexthe666.iceandfire.entity.EntityFireDragon;
import com.github.alexthe666.iceandfire.entity.EntityIceDragon;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.entity.IEntityLivingBase;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.lang.reflect.Method;

/**
 * Ice and Fire 模组效果整合
 * 提供冰冻、火焰、闪电链等特效
 */
@ZenRegister
@ZenClass("mods.moremod.IceAndFireHelper")
public class IceAndFireHelper {

    // 缓存反射方法
    private static Method chainLightningMethod = null;
    private static boolean chainLightningMethodChecked = false;

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

            // 对冰龙额外伤害
            if (mcTarget instanceof EntityIceDragon) {
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

            // Ice and Fire 冰冻效果
            if (!mcTarget.world.isRemote) {
                try {
                    IEntityEffectCapability capability = InFCapabilities.getEntityEffectCapability(mcTarget);
                    if (capability != null) {
                        capability.setFrozen(frozenTicks);
                    }
                } catch (Exception e) {
                    // 如果Ice and Fire不存在,使用原版效果
                }
            }

            // 减速+挖掘疲劳
            mcTarget.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, slownessDuration, 2));
            mcTarget.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, slownessDuration, 2));

            // 击退
            mcTarget.knockBack(mcTarget, knockbackStrength,
                    mcAttacker.posX - mcTarget.posX,
                    mcAttacker.posZ - mcTarget.posZ);

            // 对火龙额外伤害
            if (mcTarget instanceof EntityFireDragon) {
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

            // Ice and Fire 闪电链 (通过反射调用，兼容不同版本)
            boolean chainLightningSuccess = false;
            try {
                chainLightningSuccess = invokeChainLightning(mcTarget.world, mcTarget, mcAttacker);
            } catch (Exception e) {
                // 反射调用失败
            }

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

            // 对龙额外伤害
            if (mcTarget instanceof EntityFireDragon || mcTarget instanceof EntityIceDragon) {
                mcTarget.attackEntityFrom(DamageSource.LIGHTNING_BOLT, dragonBonus);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 通过反射调用 ChainLightningUtils 的方法
     * 兼容不同版本的 Ice and Fire
     */
    private static boolean invokeChainLightning(World world, EntityLivingBase target, EntityLivingBase source) {
        if (!chainLightningMethodChecked) {
            chainLightningMethodChecked = true;
            try {
                Class<?> clazz = Class.forName("com.github.alexthe666.iceandfire.api.ChainLightningUtils");
                // 尝试不同的方法签名（Ice and Fire 有两个重载版本）
                // 1. createChainLightningFromTarget(World, EntityLivingBase, EntityLivingBase)
                // 2. createChainLightningFromTarget(World, EntityLivingBase, Entity)
                try {
                    chainLightningMethod = clazz.getMethod("createChainLightningFromTarget",
                            World.class, EntityLivingBase.class, EntityLivingBase.class);
                } catch (NoSuchMethodException e1) {
                    try {
                        chainLightningMethod = clazz.getMethod("createChainLightningFromTarget",
                                World.class, EntityLivingBase.class, net.minecraft.entity.Entity.class);
                    } catch (NoSuchMethodException e2) {
                        // 方法不存在
                    }
                }
            } catch (ClassNotFoundException e) {
                // ChainLightningUtils 类不存在
            }
        }

        if (chainLightningMethod != null) {
            try {
                chainLightningMethod.invoke(null, world, target, source);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
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
            return mcEntity instanceof EntityFireDragon || mcEntity instanceof EntityIceDragon;
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
            return mcEntity instanceof EntityFireDragon;
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
            return mcEntity instanceof EntityIceDragon;
        } catch (Exception e) {
            return false;
        }
    }
}