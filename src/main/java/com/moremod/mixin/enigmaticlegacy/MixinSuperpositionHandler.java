package com.moremod.mixin.enigmaticlegacy;

import com.moremod.entity.curse.EmbeddedCurseEffectHandler;
import com.moremod.entity.curse.EmbeddedCurseManager;
import com.moremod.entity.curse.EmbeddedCurseManager.EmbeddedRelicType;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 拦截 SuperpositionHandler 中的灵魂破碎效果
 *
 * 当玩家嵌入了灵魂锚点 (SOUL_ANCHOR) 时，阻止 loseSoul 方法执行
 */
@Mixin(targets = "keletu.enigmaticlegacy.event.SuperpositionHandler", remap = false)
public class MixinSuperpositionHandler {

    /**
     * 拦截 loseSoul 方法
     * 当玩家嵌入了灵魂锚点时，取消灵魂破碎效果
     */
    @Inject(method = "loseSoul", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onLoseSoul(EntityPlayer player, CallbackInfo ci) {
        if (player == null || player.world.isRemote) return;

        // 检查是否嵌入了灵魂锚点
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SOUL_ANCHOR)) {
            System.out.println("[SacredRelic] 灵魂锚点抵消了灵魂破碎效果: " + player.getName());
            ci.cancel();
        }
    }
}
