package com.moremod.mixin.infernal;

import com.moremod.item.causal.CausalFieldManager;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 攔截 InfernalMobsCore 的生成處理
 * 在沉默場內阻止生物被轉成精英
 *
 * 使用 @Pseudo 避免 InfernalMobs 未安裝時崩潰
 */
@Pseudo
@Mixin(targets = "atomicstryker.infernalmobs.common.InfernalMobsCore", remap = false)
public abstract class MixinInfernalMobsCore_SpawnGate {

    @Inject(method = "processEntitySpawn", at = @At("HEAD"), cancellable = true, require = 0)
    private void moremod$gateband_blockAffixOnSpawn(EntityLivingBase entity, CallbackInfo ci) {
        if (entity.world.isRemote) return;
        if (CausalFieldManager.isInField(entity.world, entity.posX, entity.posY, entity.posZ)) {
            ci.cancel();
        }
    }
}
