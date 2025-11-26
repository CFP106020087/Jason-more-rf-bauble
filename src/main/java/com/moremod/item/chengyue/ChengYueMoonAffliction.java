package com.moremod.item.chengyue;

import com.moremod.potion.ModPotions;
import com.moremod.potion.PotionMoonAffliction;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 月殇系统 - 概率叠加易伤
 */
public class ChengYueMoonAffliction {

    public static final int UNLOCK_LEVEL = 61;
    public static final int MAX_LEVEL = 30;
    public static final int DURATION = 200;

    /**
     * 应用月殇效果
     */
    public static void applyAffliction(EntityLivingBase target, EntityPlayer attacker, ItemStack weapon) {
        int swordLevel = ChengYueNBT.getLevel(weapon);
        if (swordLevel < UNLOCK_LEVEL) {
            return;
        }

        PotionEffect currentEffect = target.getActivePotionEffect(ModPotions.MOON_AFFLICTION);

        int newAmplifier;
        boolean shouldApply = false;

        if (currentEffect == null) {
            newAmplifier = 0;
            shouldApply = true;
        } else {
            int currentAmplifier = currentEffect.getAmplifier();
            int currentLevel = currentAmplifier + 1;

            if (currentLevel >= MAX_LEVEL) {
                newAmplifier = currentAmplifier;
                shouldApply = true;
            } else {
                float stackChance = 0.8f - (currentLevel * 0.02f);

                if (attacker.world.rand.nextFloat() < stackChance) {
                    newAmplifier = currentAmplifier + 1;
                    shouldApply = true;

                    int newLevel = newAmplifier + 1;
                    if (newLevel % 5 == 0) {
                        attacker.sendStatusMessage(new TextComponentString(
                            TextFormatting.DARK_PURPLE + "【月殇 " + newLevel + "】"
                        ), true);
                    }
                } else {
                    newAmplifier = currentAmplifier;
                    shouldApply = true;
                }
            }
        }

        if (shouldApply) {
            PotionEffect newEffect = new PotionEffect(
                    ModPotions.MOON_AFFLICTION,
                DURATION,
                newAmplifier,
                false,
                true
            );
            target.addPotionEffect(newEffect);
        }

        if (newAmplifier + 1 == MAX_LEVEL && (currentEffect == null ||
            currentEffect.getAmplifier() < newAmplifier)) {
            attacker.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "★ 【月殇满层】易伤 +90%！ ★"
            ));
        }
    }

    /**
     * 获取目标的月殇等级
     */
    public static int getAfflictionLevel(EntityLivingBase target) {
        PotionEffect effect = target.getActivePotionEffect(ModPotions.MOON_AFFLICTION);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    /**
     * 计算易伤倍率
     */
    public static float getDamageMultiplier(EntityLivingBase target) {
        PotionEffect effect = target.getActivePotionEffect(ModPotions.MOON_AFFLICTION);
        if (effect == null) {
            return 1.0f;
        }
        return PotionMoonAffliction.getDamageMultiplier(effect.getAmplifier());
    }
}