package com.moremod.mixin;

import c4.champions.common.EventHandlerCommon;
import com.moremod.item.causal.CausalFieldManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.living.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 攔截 Champions 的事件處理器
 * 在沉默場內直接取消處理，防止任何效果觸發
 */
@Mixin(value = EventHandlerCommon.class, remap = false)
public class MixinEventHandlerCommon {

    @Inject(method = "livingDamage", at = @At("HEAD"), cancellable = true, remap = false)
    private void livingDamage(LivingHurtEvent evt, CallbackInfo ci) {
        EntityLivingBase entity = evt.getEntityLiving();
        if (entity != null && CausalFieldManager.isInField(entity.world, entity.posX, entity.posY, entity.posZ)) {
            ci.cancel();  // 取消處理
        }
    }

    // 其他事件方法如果需要也可以攔截
    // livingDeath, livingDrops 等通常不需要攔截，因為怪物死亡時已經離開場了
}