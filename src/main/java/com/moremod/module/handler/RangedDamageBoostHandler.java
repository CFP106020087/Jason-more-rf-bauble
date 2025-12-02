package com.moremod.module.handler;

import com.moremod.module.effect.EventContext;
import com.moremod.module.effect.IModuleEventHandler;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 远程伤害增幅模块
 *
 * 增加玩家远程攻击（弓箭、投掷物等）造成的伤害
 *
 * Lv1: +15% 远程伤害
 * Lv2: +30% 远程伤害
 * Lv3: +50% 远程伤害
 */
public class RangedDamageBoostHandler implements IModuleEventHandler {

    // 每级伤害加成
    private static final float[] DAMAGE_MULTIPLIERS = {0f, 0.15f, 0.30f, 0.50f};

    @Override
    public float onPlayerAttack(EventContext ctx, EntityLivingBase target, float damage) {
        // 这个方法只处理近战，远程需要在 onPlayerHitEntity 中通过检查 DamageSource 判断
        return damage;
    }

    @Override
    public void onPlayerHitEntity(EventContext ctx, EntityLivingBase target, LivingHurtEvent event) {
        DamageSource source = event.getSource();

        // 检查是否为远程伤害 (弓箭、投掷物等)
        if (!isRangedDamage(source)) {
            return;
        }

        // 获取伤害加成
        float multiplier = getMultiplier(ctx.level);
        if (multiplier <= 0) return;

        // 计算新伤害
        float currentDamage = event.getAmount();
        float bonusDamage = currentDamage * multiplier;
        float newDamage = currentDamage + bonusDamage;

        // 设置新伤害
        event.setAmount(newDamage);
    }

    /**
     * 判断是否为远程伤害
     */
    private boolean isRangedDamage(DamageSource source) {
        // isProjectile() 检查箭、雪球、火焰弹等投射物
        if (source.isProjectile()) {
            return true;
        }

        // 额外检查伤害类型名称
        String type = source.getDamageType();
        if (type != null) {
            // arrow = 箭, thrown = 投掷物, trident = 三叉戟 (1.13+)
            if (type.equals("arrow") || type.equals("thrown") || type.equals("trident")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取对应等级的伤害加成倍率
     */
    private float getMultiplier(int level) {
        if (level < 0 || level >= DAMAGE_MULTIPLIERS.length) {
            return DAMAGE_MULTIPLIERS[DAMAGE_MULTIPLIERS.length - 1];
        }
        return DAMAGE_MULTIPLIERS[level];
    }
}
