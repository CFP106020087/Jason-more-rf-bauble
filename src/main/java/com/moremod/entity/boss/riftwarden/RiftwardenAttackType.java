package com.moremod.entity.boss.riftwarden;

/**
 * 子状态/攻击类型枚举
 */
public enum RiftwardenAttackType {
    NONE(0),
    
    // 近战
    MELEE_RIGHT(10),
    MELEE_LEFT(11),
    MELEE_SLAM(12),
    MELEE_COMBO(13),
    
    // 远程 - 第一阶段
    BULLET_BARRAGE(20),
    SPIRAL_BULLETS(21),
    BURST_BULLETS(22),
    WAVE_BULLETS(23),
    PREDICTIVE_SHOT(24),
    CHAIN_LIGHTNING(25),
    
    // 远程 - 第二阶段+
    LIGHTNING_STRIKE(30),
    LIGHTNING_ARC(31),
    CHARGE_SHOOT(32),
    
    // 大招
    LASER_BEAM(40),
    
    // 特殊
    COUNTER_ATTACK(50),
    RECOVERY_BURST(51),
    TELEPORT_STRIKE(52);
    
    private final int id;
    
    RiftwardenAttackType(int id) {
        this.id = id;
    }
    
    public int getId() { return id; }
    
    public static RiftwardenAttackType fromId(int id) {
        for (RiftwardenAttackType type : values()) {
            if (type.id == id) return type;
        }
        return NONE;
    }
}