package com.moremod.mixin;

import com.moremod.logic.NarrativeLogicHandler;
import com.moremod.client.gui.PlayerNarrativeState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.enhancedvisuals.client.render.EVRenderer;

// @Pseudo 防止没装 EnhancedVisuals 时崩端
@Pseudo
@Mixin(value = EVRenderer.class, remap = false)
public class MixinEVRenderer {

    /**
     * 拦截整个 EnhancedVisuals 的渲染循环
     * 逻辑：如果玩家处于“香巴拉”或“破碎之神”状态，直接跳过这个模组的所有视觉渲染。
     */
    @Inject(
        method = "render",
        at = @At("HEAD"), // 在一开始就拦截
        cancellable = true
    )
    private static void blockRedScreenInDivineState(TickEvent.RenderTickEvent event, CallbackInfo ci) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        // 获取当前玩家的剧情状态
        // (假设你有 NarrativeLogicHandler 来判断状态，如果没有，换成你的判断逻辑)
        PlayerNarrativeState state = NarrativeLogicHandler.determineState(player);

        // 判断是否需要“视觉净化”
        if (shouldBlockEnhancedVisuals(state)) {
            // ❌ 彻底取消渲染！
            // 这样 EVRenderer 里的 VisualHandlers.DAMAGE.clientHurt() 不会被调用
            // renderVisuals() 也不会被调用
            // 结果 = 屏幕干干净净，没有红屏，没有血污
            ci.cancel();
        }
    }

    /**
     * 辅助方法：定义哪些状态下需要屏蔽红屏
     */
    private static boolean shouldBlockEnhancedVisuals(PlayerNarrativeState state) {
        // 比如：在香巴拉状态下，你是完美的，不需要血污
        if (state == PlayerNarrativeState.SHAMBHALA) return true;
        
        // 在破碎之神状态下，你可能希望保留你的故障Glitch效果，而不要 EV 的红屏
        if (state == PlayerNarrativeState.BROKEN_GOD) return true;

        return false;
    }
}