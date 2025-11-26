package com.moremod.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.EntityDamageSourceIndirect;

/**
 * 剑气专用伤害源
 */
public class DamageSourceSwordBeam extends EntityDamageSourceIndirect {
    
    public DamageSourceSwordBeam(Entity source, Entity indirectEntity) {
        super("swordbeam", source, indirectEntity);
        this.setProjectile();
        this.setMagicDamage();  // 标记为魔法伤害
    }
    

    public boolean isSwordBeamDamage() {
        return true;
    }
}