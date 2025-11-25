package com.moremod.synergy.core;

/**
 * Synergy 事件类型枚举
 *
 * 定义 Synergy 可以响应的事件类型。
 * 用于条件匹配和效果触发时机判断。
 */
public enum SynergyEventType {

    /**
     * 玩家 tick 事件（每 tick 触发）
     * 适用于：持续效果、周期性检测
     */
    TICK("tick", "每Tick"),

    /**
     * 玩家攻击事件（造成伤害前）
     * 适用于：攻击增强、额外效果
     */
    ATTACK("attack", "攻击"),

    /**
     * 玩家受到伤害事件
     * 适用于：防御效果、反击
     */
    HURT("hurt", "受伤"),

    /**
     * 玩家击杀实体事件
     * 适用于：击杀奖励、能量回复
     */
    KILL("kill", "击杀"),

    /**
     * 玩家死亡事件
     * 适用于：死亡保护、复活
     */
    DEATH("death", "死亡"),

    /**
     * 能量消耗事件
     * 适用于：能量效率、返还
     */
    ENERGY_CONSUME("energy_consume", "能量消耗"),

    /**
     * 能量恢复事件
     * 适用于：充能加速
     */
    ENERGY_RECHARGE("energy_recharge", "能量恢复"),

    /**
     * 暴击事件
     * 适用于：暴击增强
     */
    CRITICAL_HIT("critical_hit", "暴击"),

    /**
     * 环境伤害事件（火、溺水、摔落等）
     * 适用于：环境防护
     */
    ENVIRONMENTAL_DAMAGE("environmental", "环境伤害"),

    /**
     * 状态效果变化事件
     * 适用于：状态效果增强/抵抗
     */
    POTION_EFFECT("potion", "状态效果"),

    /**
     * 手动触发（由其他系统或玩家操作触发）
     */
    MANUAL("manual", "手动"),

    /**
     * 通用事件（匹配所有）
     */
    ANY("any", "任意");

    private final String id;
    private final String displayName;

    SynergyEventType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 检查是否匹配指定的事件类型
     *
     * @param other 要匹配的类型
     * @return true 如果匹配（ANY 匹配所有）
     */
    public boolean matches(SynergyEventType other) {
        if (this == ANY || other == ANY) {
            return true;
        }
        return this == other;
    }

    /**
     * 根据 ID 查找事件类型
     *
     * @param id 事件类型 ID
     * @return 对应的枚举值，未找到返回 null
     */
    public static SynergyEventType fromId(String id) {
        if (id == null) return null;
        for (SynergyEventType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 是否为战斗相关事件
     */
    public boolean isCombatEvent() {
        return this == ATTACK || this == HURT || this == KILL || this == CRITICAL_HIT;
    }

    /**
     * 是否为能量相关事件
     */
    public boolean isEnergyEvent() {
        return this == ENERGY_CONSUME || this == ENERGY_RECHARGE;
    }
}
