package com.moremod.mixin;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.accessorybox.SmartBaublesHandlerWrapper;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.WeakHashMap;

/**
 * 将 getBaublesHandler(player) 的返回值包装成 SmartBaublesHandlerWrapper
 *
 * 修复：缓存 wrapper 实例，避免每次创建新实例导致的兼容性问题
 */
@Mixin(value = BaublesApi.class, remap = false)
public class MixinBaublesApi {

    // 缓存 wrapper 实例，避免每次创建新的导致状态不一致
    // 使用 WeakHashMap 避免内存泄漏
    private static final WeakHashMap<IBaublesItemHandler, SmartBaublesHandlerWrapper> wrapperCache =
        new WeakHashMap<>();

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

        // ✅ 修复：使用缓存的 wrapper，避免每次创建新实例
        SmartBaublesHandlerWrapper wrapper = wrapperCache.get(original);
        if (wrapper == null) {
            wrapper = new SmartBaublesHandlerWrapper(original);
            wrapperCache.put(original, wrapper);
        }

        cir.setReturnValue(wrapper);
    }
}
