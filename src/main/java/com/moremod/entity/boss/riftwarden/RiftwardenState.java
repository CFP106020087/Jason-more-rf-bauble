package com.moremod.entity.boss.riftwarden;

/**
 * Riftwarden 主状态机枚举
 * 互斥设计：同一时间只能存在一种主状态
 */
public enum RiftwardenState {
    IDLE(0, true, true),           // 待机：可移动，可转向
    WALKING(1, true, true),        // 行走：可移动，可转向
    PURSUING(2, true, true),       // 追击：可移动，可转向（速度更快）
    
    CASTING_PREPARE(10, false, true),   // 施法准备：不可移动，可缓慢转向
    CASTING_LOCKED(11, false, false),   // 施法锁定：不可移动，不可转向
    CASTING_EXECUTE(12, false, false),  // 施法执行：不可移动，不可转向
    CASTING_RECOVER(13, false, true),   // 施法恢复：不可移动，可缓慢转向
    
    MELEE_WINDUP(20, false, true),      // 近战蓄力：不可移动，可缓慢转向
    MELEE_STRIKE(21, false, false),     // 近战挥击：不可移动，不可转向
    MELEE_RECOVER(22, false, true),     // 近战恢复：不可移动，可转向
    
    EXHAUSTED(30, false, false),        // 虚弱：不可移动，不可转向
    TELEPORTING(40, false, false),      // 传送中：不可移动，不可转向
    
    GATE_ACTIVE(50, true, true),        // 锁血激活：可移动，可转向（特殊状态）
    
    STAGGERED(60, false, false),        // 硬直：不可移动，不可转向
    DEAD(99, false, false);             // 死亡
    
    private final int id;
    private final boolean allowsMovement;
    private final boolean allowsRotation;
    
    RiftwardenState(int id, boolean allowsMovement, boolean allowsRotation) {
        this.id = id;
        this.allowsMovement = allowsMovement;
        this.allowsRotation = allowsRotation;
    }
    
    public int getId() { return id; }
    public boolean allowsMovement() { return allowsMovement; }
    public boolean allowsRotation() { return allowsRotation; }
    
    public boolean isCasting() {
        return this == CASTING_PREPARE || this == CASTING_LOCKED || 
               this == CASTING_EXECUTE || this == CASTING_RECOVER;
    }
    
    public boolean isMelee() {
        return this == MELEE_WINDUP || this == MELEE_STRIKE || this == MELEE_RECOVER;
    }
    
    public boolean isInterruptible() {
        return this == IDLE || this == WALKING || this == PURSUING || 
               this == CASTING_PREPARE || this == MELEE_WINDUP ||
               this == GATE_ACTIVE ||      // 锁血状态可以被中断
               this == MELEE_RECOVER ||    // 近战恢复可以被中断
               this == CASTING_RECOVER;    // 施法恢复可以被中断
    }
    
    public static RiftwardenState fromId(int id) {
        for (RiftwardenState state : values()) {
            if (state.id == id) return state;
        }
        return IDLE;
    }
}