package com.moremod.entity;

/**
 * 剑气类型枚举 - 对应 EntitySwordBeam.BeamType
 *
 * 只保留实际需要的类型
 */
public enum SwordBeamType {
    /** 普通剑气 */
    NORMAL("normal", 1.0f, 1.0f, 0xFFFFFF),

    /** 螺旋剑气 - 旋转前进 */
    SPIRAL("spiral", 1.0f, 0.9f, 0x00FFFF),

    /** 月牙剑气 */
    CRESCENT("crescent", 1.1f, 1.0f, 0xCCCCFF),

    /** 十字剑气 */
    CROSS("cross", 1.2f, 1.0f, 0xFFFF00),

    /** 龙形剑气 - 追踪+龙息效果 */
    DRAGON("dragon", 1.5f, 1.1f, 0xFF0000),

    /** 凤凰剑气 - 火焰尾迹 */
    PHOENIX("phoenix", 1.3f, 1.0f, 0xFF6600);

    private final String id;
    private final float damageMultiplier;
    private final float speedMultiplier;
    private final int color;

    SwordBeamType(String id, float damageMultiplier, float speedMultiplier, int color) {
        this.id = id;
        this.damageMultiplier = damageMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.color = color;
    }

    public String getId() { return id; }
    public float getDamageMultiplier() { return damageMultiplier; }
    public float getSpeedMultiplier() { return speedMultiplier; }
    public int getColor() { return color; }

    /**
     * 通过ID查找剑气类型
     */
    public static SwordBeamType fromString(String id) {
        for (SwordBeamType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return NORMAL;
    }

    /**
     * 转换为EntitySwordBeam.BeamType
     */
    public com.moremod.entity.EntitySwordBeam.BeamType toEntityBeamType() {
        switch (this) {
            case SPIRAL: return com.moremod.entity.EntitySwordBeam.BeamType.SPIRAL;
            case CRESCENT: return com.moremod.entity.EntitySwordBeam.BeamType.CRESCENT;
            case CROSS: return com.moremod.entity.EntitySwordBeam.BeamType.CROSS;
            case DRAGON: return com.moremod.entity.EntitySwordBeam.BeamType.DRAGON;
            case PHOENIX: return com.moremod.entity.EntitySwordBeam.BeamType.PHOENIX;
            default: return com.moremod.entity.EntitySwordBeam.BeamType.NORMAL;
        }
    }
}