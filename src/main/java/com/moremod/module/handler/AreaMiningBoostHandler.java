package com.moremod.module.handler;

import com.moremod.module.effect.EventContext;
import com.moremod.module.effect.IModuleEventHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

/**
 * 周围挖掘增幅模块处理器
 *
 * 效果: 为周围玩家提供急迫效果，加快挖掘速度
 *
 * Lv1: 4格范围, 急迫 I
 * Lv2: 6格范围, 急迫 II
 * Lv3: 8格范围, 急迫 III
 */
public class AreaMiningBoostHandler implements IModuleEventHandler {

    // 每级增加的范围
    private static final int BASE_RANGE = 2;
    private static final int RANGE_PER_LEVEL = 2;

    // 效果持续时间 (tick) - 稍长于刷新间隔，避免闪烁
    private static final int EFFECT_DURATION = 45; // 2.25秒

    // 能耗: 每次刷新
    private static final int ENERGY_PER_TICK = 5;

    @Override
    public void onSecondTick(EventContext ctx) {
        // 检查能量
        if (!ctx.consumeEnergy(ENERGY_PER_TICK * 20)) {
            return;
        }

        // 计算范围和效果等级
        int range = BASE_RANGE + (ctx.level * RANGE_PER_LEVEL);
        int amplifier = ctx.level - 1; // Lv1 = 急迫I (amplifier 0)

        // 获取范围内的所有玩家
        AxisAlignedBB aabb = new AxisAlignedBB(
            ctx.player.posX - range, ctx.player.posY - range, ctx.player.posZ - range,
            ctx.player.posX + range, ctx.player.posY + range, ctx.player.posZ + range
        );

        List<EntityPlayer> nearbyPlayers = ctx.player.world.getEntitiesWithinAABB(
            EntityPlayer.class, aabb
        );

        // 给所有周围玩家添加急迫效果
        for (EntityPlayer player : nearbyPlayers) {
            player.addPotionEffect(new PotionEffect(
                MobEffects.HASTE,
                EFFECT_DURATION,
                amplifier,
                true,  // ambient (环境效果，粒子减少)
                true   // show particles
            ));
        }
    }

    @Override
    public int getPassiveEnergyCost() {
        return 0; // 使用 onSecondTick 中的主动消耗
    }

    @Override
    public String getDescription() {
        return "周围挖掘增幅 - 为附近玩家提供急迫效果";
    }
}
