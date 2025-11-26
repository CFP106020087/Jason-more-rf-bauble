package com.moremod.compat.crafttweaker;

/**
 * 特殊效果触发时机
 */
public enum SpecialEffectTrigger {
    /** 攻击命中时 */
    ON_HIT,
    
    /** 暴击时 */
    ON_CRIT,
    
    /** 击杀时 */
    ON_KILL,
    
    /** 受到攻击时 */
    ON_HIT_TAKEN,
    
    /** 闪避攻击时 */
    ON_DODGE,
    
    /** 格挡攻击时 */
    ON_BLOCK,
    
    /** 生命值低于阈值时 */
    ON_LOW_HEALTH,
    
    /** 攻击特定类型敌人时 */
    ON_HIT_TYPE,
    
    /** 持续效果(每秒触发) */
    PASSIVE,
    
    /** 使用物品时 */
    ON_USE,
    
    /** 跳跃时 */
    ON_JUMP,
    
    /** 冲刺时 */
    ON_SPRINT,
    
    /** 受到致命伤害时(一次性) */
    ON_LETHAL_DAMAGE,
    
    /** 生命值满时 */
    ON_FULL_HEALTH,
    
    /** 连击时(多次命中) */
    ON_COMBO
}
