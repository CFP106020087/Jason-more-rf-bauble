package com.moremod.synergy.condition;

import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.core.SynergyContext;
import com.moremod.synergy.core.SynergyEventType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * 事件类型条件
 *
 * 检查当前事件是否为指定类型。
 */
public class EventTypeCondition implements ISynergyCondition {

    private final Set<SynergyEventType> allowedTypes;

    public EventTypeCondition(SynergyEventType... types) {
        this.allowedTypes = EnumSet.copyOf(Arrays.asList(types));
    }

    @Override
    public boolean test(SynergyContext context) {
        SynergyEventType currentType = context.getEventType();
        for (SynergyEventType allowed : allowedTypes) {
            if (allowed.matches(currentType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Event is " + allowedTypes;
    }

    // ==================== 静态工厂方法 ====================

    public static EventTypeCondition ofType(SynergyEventType... types) {
        return new EventTypeCondition(types);
    }

    public static EventTypeCondition attack() {
        return new EventTypeCondition(SynergyEventType.ATTACK);
    }

    public static EventTypeCondition hurt() {
        return new EventTypeCondition(SynergyEventType.HURT);
    }

    public static EventTypeCondition tick() {
        return new EventTypeCondition(SynergyEventType.TICK);
    }

    public static EventTypeCondition kill() {
        return new EventTypeCondition(SynergyEventType.KILL);
    }

    public static EventTypeCondition combat() {
        return new EventTypeCondition(
                SynergyEventType.ATTACK,
                SynergyEventType.HURT,
                SynergyEventType.KILL,
                SynergyEventType.CRITICAL_HIT
        );
    }

    public static EventTypeCondition environmental() {
        return new EventTypeCondition(SynergyEventType.ENVIRONMENTAL_DAMAGE);
    }
}
