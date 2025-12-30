package com.moremod.mixin.lycanites;

import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "com.lycanitesmobs.core.entity.BaseCreatureEntity", remap = false)
public class MixinBaseCreatureEntity {

    // 重定向damageLimit检查
    @Redirect(method = "isEntityInvulnerable", at = @At(value = "FIELD", target = "Lcom/lycanitesmobs/core/entity/BaseCreatureEntity;damageLimit:F"))
    private float redirectDamageLimit(BaseCreatureEntity entity) {
        return 0; // 返回0让检查永远不通过
    }

    // 修改getDamageAfterDefense的返回值
    @ModifyVariable(method = "getDamageAfterDefense", at = @At("RETURN"), ordinal = 0)
    private float modifyDamageReturn(float damage) {
        return damage; // 返回原始伤害值
    }

    // 重定向boss范围检查
    @Redirect(method = "isDamageEntityApplicable", at = @At(value = "INVOKE", target = "Lcom/lycanitesmobs/core/entity/BaseCreatureEntity;getDistance(Lnet/minecraft/entity/Entity;)F"))
    private float redirectBossRangeCheck(BaseCreatureEntity entity, net.minecraft.entity.Entity target) {
        return 0; // 总是返回0距离，通过范围检查
    }
}