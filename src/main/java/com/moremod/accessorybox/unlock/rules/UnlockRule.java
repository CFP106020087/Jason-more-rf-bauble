package com.moremod.accessorybox.unlock.rules;

/**
 * 解锁规则
 * 将槽位目标和解锁条件关联
 */
public class UnlockRule {
    private final SlotTarget target;
    private final UnlockCondition condition;

    public UnlockRule(SlotTarget target, UnlockCondition condition) {
        this.target = target;
        this.condition = condition;
    }

    public SlotTarget getTarget() {
        return target;
    }

    public UnlockCondition getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return target + " -> " + condition;
    }
}
