package com.moremod.item.herosword;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.List;

public class HeroSwordSkill {

    // ========== 成长系统 ==========

    public static float getThreshold(ItemStack stack) {
        int lv = HeroSwordNBT.getLevel(stack);

        float base = 0.12f;
        float max = 0.30f;
        return base + (max - base) * (1.0f - (float)Math.exp(-lv / 20f));
    }

    public static float getRadius(ItemStack stack) {
        int lv = HeroSwordNBT.getLevel(stack);
        return 4.5f + (lv / 60f) * 2.5f;
    }

    public static int getCooldownTicks(ItemStack stack) {
        int lv = HeroSwordNBT.getLevel(stack);
        int ticks = (int)(600 - lv * 4);
        return Math.max(360, ticks);
    }

    // ========== 主动技能执行 ==========

    public static boolean tryExecuteSkill(ItemStack stack, EntityPlayer player) {
        World world = player.world;

        float radius = getRadius(stack);
        float thresh = getThreshold(stack);

        List<EntityLivingBase> list = world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                new AxisAlignedBB(
                        player.posX - radius, player.posY - 2, player.posZ - radius,
                        player.posX + radius, player.posY + 3, player.posZ + radius
                ),
                e -> e != player && e.isEntityAlive()
        );

        boolean foundOne = false;

        for (EntityLivingBase t : list) {
            float hp = t.getHealth();
            float max = t.getMaxHealth();

            if (hp <= max * thresh) {
                foundOne = true;

                // 设置到1滴血（最大兼容）
                t.setHealth(1.0f);

                // 正式击杀调用
                t.attackEntityFrom(DamageSource.causePlayerDamage(player), 99999f);
            }
        }

        return foundOne;
    }
}
