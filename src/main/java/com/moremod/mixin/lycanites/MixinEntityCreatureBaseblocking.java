package com.moremod.mixin.lycanites;

import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for 1.12.2 Lycanites Mobs
 * 注意：1.12.2中类名是EntityCreatureBase，不是BaseCreatureEntity
 */
@Pseudo
@Mixin(targets = "com.lycanitesmobs.core.entity.BaseCreatureEntity", remap = false)
public class MixinEntityCreatureBaseblocking {

    /**
     * 让所有Lycanites生物都不无敌
     */
    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void bypassAllInvulnerability(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        // 直接返回false，没有任何生物是无敌的
        cir.setReturnValue(false);
    }

    /**
     * 禁用所有格挡
     */
    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void disableAllBlocking(CallbackInfoReturnable<Boolean> cir) {
        // 永远不格挡
        cir.setReturnValue(false);
    }

    /**
     * 如果有isDamageTypeApplicable方法（1.12.2版本可能有）
     */
    @Inject(method = "isDamageTypeApplicable", at = @At("HEAD"), cancellable = true, require = 0)
    private void allowAllDamageTypes(String type, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        // 所有伤害类型都有效
        cir.setReturnValue(true);
    }
}