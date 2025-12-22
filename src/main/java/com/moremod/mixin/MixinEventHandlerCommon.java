package com.moremod.mixin;

import com.moremod.item.causal.CausalFieldManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 攔截 Champions 的事件處理器
 * 在沉默場內直接取消處理，防止任何效果觸發
 *
 * 使用 @Pseudo 避免 Champions 未安裝時崩潰
 */
@Pseudo
@Mixin(targets = "c4.champions.common.EventHandlerCommon", remap = false)
public class MixinEventHandlerCommon {

    @Inject(method = "livingDamage", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void moremod$livingDamage(LivingHurtEvent evt, CallbackInfo ci) {
        EntityLivingBase entity = evt.getEntityLiving();
        if (entity != null && CausalFieldManager.isInField(entity.world, entity.posX, entity.posY, entity.posZ)) {
            ci.cancel();
        }
    }
}
