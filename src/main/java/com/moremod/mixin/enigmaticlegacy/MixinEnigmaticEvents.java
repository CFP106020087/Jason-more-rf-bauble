package com.moremod.mixin.enigmaticlegacy;

import com.moremod.entity.curse.EmbeddedCurseManager;
import com.moremod.entity.curse.EmbeddedCurseManager.EmbeddedRelicType;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin 拦截 EnigmaticEvents 中的各种诅咒效果
 *
 * 目标类: keletu.enigmaticlegacy.event.EnigmaticEvents
 */
@Mixin(targets = "keletu.enigmaticlegacy.event.EnigmaticEvents", remap = false, expected = 0)
public class MixinEnigmaticEvents {

    /**
     * 拦截失眠症检查
     * 当玩家嵌入了安眠香囊时，返回 false 允许睡觉
     *
     * 注意：这个注入点需要根据实际的 EnigmaticEvents 代码调整
     */
    @Inject(method = "shouldPreventSleep", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void onShouldPreventSleep(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (player == null || player.world.isRemote) return;

        // 检查是否嵌入了安眠香囊
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SLUMBER_SACHET)) {
            System.out.println("[SacredRelic] 安眠香囊抵消了失眠症: " + player.getName());
            cir.setReturnValue(false);
        }
    }

    /**
     * 拦截永燃效果检查
     * 当玩家嵌入了霜华之露时，返回 false 允许火焰熄灭
     */
    @Inject(method = "shouldPreventFireExtinguish", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void onShouldPreventFireExtinguish(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (player == null || player.world.isRemote) return;

        // 检查是否嵌入了霜华之露
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.FROST_DEW)) {
            System.out.println("[SacredRelic] 霜华之露抵消了永燃效果: " + player.getName());
            cir.setReturnValue(false);
        }
    }

    /**
     * 拦截护甲降低效果检查
     * 当玩家嵌入了守护鳞片时，返回 false 不降低护甲
     */
    @Inject(method = "shouldReduceArmor", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void onShouldReduceArmor(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (player == null || player.world.isRemote) return;

        // 检查是否嵌入了守护鳞片
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.GUARDIAN_SCALE)) {
            System.out.println("[SacredRelic] 守护鳞片抵消了护甲降低效果: " + player.getName());
            cir.setReturnValue(false);
        }
    }

    /**
     * 拦截中立生物攻击检查
     * 当玩家嵌入了和平徽章时，返回 false 不让中立生物攻击
     */
    @Inject(method = "shouldNeutralMobsAttack", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void onShouldNeutralMobsAttack(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (player == null || player.world.isRemote) return;

        // 检查是否嵌入了和平徽章
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.PEACE_EMBLEM)) {
            System.out.println("[SacredRelic] 和平徽章抵消了中立生物攻击: " + player.getName());
            cir.setReturnValue(false);
        }
    }
}
