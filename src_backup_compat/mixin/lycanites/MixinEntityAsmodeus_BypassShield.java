package com.moremod.mixin.lycanites;

import com.moremod.event.eventHandler.EnergySwordAttackHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.12.2：阿斯摩德专用。只有能量剑才能关掉"第二阶段小弟护盾"的前置挡伤。
 */
@Mixin(targets = "com.lycanitesmobs.core.entity.creature.EntityAsmodeus", remap = false)
public class MixinEntityAsmodeus_BypassShield {

    @Inject(
            method = "isInvulnerableTo(Lnet/minecraft/util/DamageSource;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void moremod$playerPenetrates(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (EnergySwordAttackHandler.isEnergySwordAttackActive()) {
            Entity attacker = source.getTrueSource();
            if (attacker == null) attacker = source.getImmediateSource();
            if (attacker instanceof EntityPlayer) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "isBlocking()Z", at = @At("HEAD"), cancellable = true)
    private void moremod$disableBlocking(CallbackInfoReturnable<Boolean> cir) {
        if (EnergySwordAttackHandler.isEnergySwordAttackActive()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "isVulnerableTo(Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void moremod$allowPlayer(Entity attacker, CallbackInfoReturnable<Boolean> cir) {
        if (EnergySwordAttackHandler.isEnergySwordAttackActive() && attacker instanceof EntityPlayer) {
            cir.setReturnValue(true);
        }
    }
}