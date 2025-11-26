package com.moremod.synergy.condition;

import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.core.SynergyContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 目标条件
 *
 * 检查目标实体是否满足特定条件。
 */
public class TargetCondition implements ISynergyCondition {

    public enum TargetType {
        EXISTS,          // 目标存在
        IS_MONSTER,      // 目标是怪物
        IS_PLAYER,       // 目标是玩家
        IS_NOT_PLAYER,   // 目标不是玩家
        IS_LOW_HEALTH,   // 目标低血量（<30%）
        IS_FULL_HEALTH   // 目标满血
    }

    private final TargetType type;
    private final float threshold;

    public TargetCondition(TargetType type) {
        this(type, 0.3f);
    }

    public TargetCondition(TargetType type, float threshold) {
        this.type = type;
        this.threshold = threshold;
    }

    @Override
    public boolean test(SynergyContext context) {
        EntityLivingBase target = context.getTarget();

        switch (type) {
            case EXISTS:
                return target != null;

            case IS_MONSTER:
                return target instanceof EntityMob;

            case IS_PLAYER:
                return target instanceof EntityPlayer;

            case IS_NOT_PLAYER:
                return target != null && !(target instanceof EntityPlayer);

            case IS_LOW_HEALTH:
                if (target == null) return false;
                return target.getHealth() / target.getMaxHealth() < threshold;

            case IS_FULL_HEALTH:
                if (target == null) return false;
                return target.getHealth() >= target.getMaxHealth();

            default:
                return false;
        }
    }

    @Override
    public String getDescription() {
        return "Target " + type.name().toLowerCase().replace('_', ' ');
    }

    // ==================== 静态工厂方法 ====================

    public static TargetCondition exists() {
        return new TargetCondition(TargetType.EXISTS);
    }

    public static TargetCondition isMonster() {
        return new TargetCondition(TargetType.IS_MONSTER);
    }

    public static TargetCondition isPlayer() {
        return new TargetCondition(TargetType.IS_PLAYER);
    }

    public static TargetCondition isNotPlayer() {
        return new TargetCondition(TargetType.IS_NOT_PLAYER);
    }

    public static TargetCondition lowHealth() {
        return new TargetCondition(TargetType.IS_LOW_HEALTH);
    }

    public static TargetCondition lowHealth(float threshold) {
        return new TargetCondition(TargetType.IS_LOW_HEALTH, threshold);
    }

    public static TargetCondition fullHealth() {
        return new TargetCondition(TargetType.IS_FULL_HEALTH);
    }
}
