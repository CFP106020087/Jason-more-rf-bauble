package com.moremod.mixin.lycanites;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.12.2：在 BaseCreatureEntity#attackEntityFrom 中放行玩家来源，
 * 防止因 isEntityInvulnerable / isDamageTypeApplicable 提前 return false。
 */
@Pseudo
@Mixin(targets = "com.lycanitesmobs.core.entity.BaseCreatureEntity", remap = false)
public abstract class MixinBaseCreatureEntity_AttackRedirect_Min {

    /** 无敌检查：isEntityInvulnerable(DamageSource) */
    @Redirect(
            method = {
                    "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
                    "func_70097_a(Lnet/minecraft/util/DamageSource;F)Z"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/lycanitesmobs/core/entity/BaseCreatureEntity;isEntityInvulnerable(Lnet/minecraft/util/DamageSource;)Z"
            ),
            require = 0
    )
    private boolean moremod$redirect_isEntityInvulnerable(Object self, DamageSource source) {
        if (isFromPlayer(source)) return false; // 玩家来源：不无敌
        return ((com.lycanitesmobs.core.entity.BaseCreatureEntity) self).isEntityInvulnerable(source);
    }

    /** 类型白名单：isDamageTypeApplicable(String, DamageSource, float) */
    @Redirect(
            method = {
                    "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
                    "func_70097_a(Lnet/minecraft/util/DamageSource;F)Z"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/lycanitesmobs/core/entity/BaseCreatureEntity;isDamageTypeApplicable(Ljava/lang/String;Lnet/minecraft/util/DamageSource;F)Z"
            ),
            require = 0
    )
    private boolean moremod$redirect_isDamageTypeApplicable(Object self, String type, DamageSource source, float amount) {
        if (isFromPlayer(source)) return true; // 玩家来源：一律视为可伤害
        return ((com.lycanitesmobs.core.entity.BaseCreatureEntity) self).isDamageTypeApplicable(type, source, amount);
    }

    private static boolean isFromPlayer(DamageSource src) {
        return src.getTrueSource() instanceof EntityPlayer
                || src.getImmediateSource() instanceof EntityPlayer;
    }
}
