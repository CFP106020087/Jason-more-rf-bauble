// src/main/java/com/moremod/mixin/infernal/MixinInfernalMobsCore_SpawnGate.java
package com.moremod.mixin.infernal;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InfernalMobsCore.class, remap = false)
public abstract class MixinInfernalMobsCore_SpawnGate {

    @Inject(method = "processEntitySpawn", at = @At("HEAD"), cancellable = true)
    private void moremod$gateband_blockAffixOnSpawn(EntityLivingBase entity, CallbackInfo ci) {
        if (entity.world.isRemote) return;
        if (com.moremod.causal.CausalFieldManager.isInField(entity.world, entity.posX, entity.posY, entity.posZ)) {
            // 阻止這隻在此刻被轉成菁英/超/煉獄
            ci.cancel();
        }
    }
}
