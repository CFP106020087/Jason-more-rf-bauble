package com.moremod.synergy.condition;

import com.moremod.synergy.api.IInstalledModuleView;
import com.moremod.synergy.api.ISynergyCondition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;
import java.util.function.Predicate;

/**
 * 玩家状态条件 - 检查玩家的状态
 *
 * 说明：
 * - 灵活的条件类型，使用 Predicate 自定义检查逻辑
 * - 例如：检查玩家生命值、能量、位置等
 */
public class PlayerStateCondition implements ISynergyCondition {

    private final Predicate<EntityPlayer> statePredicate;
    private final String description;

    public PlayerStateCondition(Predicate<EntityPlayer> predicate, String description) {
        this.statePredicate = predicate;
        this.description = description;
    }

    @Override
    public boolean test(EntityPlayer player, List<IInstalledModuleView> modules, Event event) {
        if (player == null || statePredicate == null) {
            return false;
        }

        return statePredicate.test(player);
    }

    @Override
    public String getDescription() {
        return "PlayerState[" + description + "]";
    }

    // 便捷工厂方法

    /**
     * 检查玩家生命值百分比
     *
     * @param minHealthPercent 最小生命值百分比（0.0 ~ 1.0）
     * @return 条件对象
     */
    public static PlayerStateCondition healthAbove(float minHealthPercent) {
        return new PlayerStateCondition(
                p -> (p.getHealth() / p.getMaxHealth()) >= minHealthPercent,
                "Health>=" + (int)(minHealthPercent * 100) + "%"
        );
    }

    /**
     * 检查玩家生命值百分比
     *
     * @param maxHealthPercent 最大生命值百分比（0.0 ~ 1.0）
     * @return 条件对象
     */
    public static PlayerStateCondition healthBelow(float maxHealthPercent) {
        return new PlayerStateCondition(
                p -> (p.getHealth() / p.getMaxHealth()) <= maxHealthPercent,
                "Health<=" + (int)(maxHealthPercent * 100) + "%"
        );
    }

    /**
     * 检查玩家是否在地面上
     *
     * @return 条件对象
     */
    public static PlayerStateCondition onGround() {
        return new PlayerStateCondition(
                EntityPlayer::onGround,
                "OnGround"
        );
    }

    /**
     * 检查玩家是否在飞行
     *
     * @return 条件对象
     */
    public static PlayerStateCondition flying() {
        return new PlayerStateCondition(
                p -> p.capabilities.isFlying || p.isElytraFlying(),
                "Flying"
        );
    }

    /**
     * 检查玩家是否在潜行
     *
     * @return 条件对象
     */
    public static PlayerStateCondition sneaking() {
        return new PlayerStateCondition(
                EntityPlayer::isSneaking,
                "Sneaking"
        );
    }

    /**
     * 检查玩家是否在疾跑
     *
     * @return 条件对象
     */
    public static PlayerStateCondition sprinting() {
        return new PlayerStateCondition(
                EntityPlayer::isSprinting,
                "Sprinting"
        );
    }

    /**
     * 检查玩家是否在水中
     *
     * @return 条件对象
     */
    public static PlayerStateCondition inWater() {
        return new PlayerStateCondition(
                EntityPlayer::isInWater,
                "InWater"
        );
    }
}
