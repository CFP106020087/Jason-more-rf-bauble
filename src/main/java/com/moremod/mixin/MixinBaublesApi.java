package com.moremod.mixin;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.accessorybox.SmartBaublesHandlerWrapper;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 将 getBaublesHandler(player) 的返回值包装成 SmartBaublesHandlerWrapper
 */
@Mixin(value = BaublesApi.class,remap = false)
public class MixinBaublesApi {

    @Inject(
            method = "getBaublesHandler",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void wrapHandler(EntityPlayer player, CallbackInfoReturnable<IBaublesItemHandler> cir) {

        IBaublesItemHandler original = cir.getReturnValue();

        if (original == null) {
            return;
        }

        // ⭐ 用我们的 wrapper 替换所有调用方得到的 handler
        cir.setReturnValue(new SmartBaublesHandlerWrapper(original));
    }
}
