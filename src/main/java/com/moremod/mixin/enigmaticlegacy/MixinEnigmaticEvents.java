package com.moremod.mixin.enigmaticlegacy;

import com.moremod.entity.curse.EmbeddedCurseManager;
import com.moremod.entity.curse.EmbeddedCurseManager.EmbeddedRelicType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 拦截 EnigmaticEvents 中的各种诅咒效果
 *
 * 目标类: keletu.enigmaticlegacy.event.EnigmaticEvents
 *
 * 拦截效果：
 * 1. 永燃 (FROST_DEW) - 拦截 setFire 调用
 * 2. 失眠症 (SLUMBER_SACHET) - 拦截 sleepTimer 修改
 * 3. 护甲降低30% (GUARDIAN_SCALE) - 拦截伤害倍率
 * 4. 中立生物攻击 (PEACE_EMBLEM) - 通过 onEntityHurt 处理
 */
@Pseudo
@Mixin(targets = "keletu.enigmaticlegacy.event.EnigmaticEvents", remap = false)
public class MixinEnigmaticEvents {

    /**
     * 拦截永燃效果 - 重定向 player.setFire() 调用
     * 当玩家嵌入了霜华之露时，不执行 setFire
     */
    @Redirect(
            method = "tickHandler(Lnet/minecraftforge/fml/common/gameevent/TickEvent$PlayerTickEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;setFire(I)V"
            ),
            require = 0
    )
    private void moremod$redirect_setFire(EntityPlayer player, int seconds) {
        // 检查是否嵌入了霜华之露
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.FROST_DEW)) {
            // 霜华之露抵消永燃效果，不执行 setFire
            return;
        }
        // 正常执行 setFire
        player.setFire(seconds);
    }

    /**
     * 拦截失眠症效果 - 重定向 isPlayerSleeping() 检查
     * 当玩家嵌入了安眠香囊时，让 EnigmaticEvents 认为玩家没在睡觉
     */
    @Redirect(
            method = "tickHandler(Lnet/minecraftforge/fml/common/gameevent/TickEvent$PlayerTickEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;isPlayerSleeping()Z"
            ),
            require = 0
    )
    private boolean moremod$redirect_isPlayerSleeping(EntityPlayer player) {
        // 检查是否嵌入了安眠香囊
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SLUMBER_SACHET)) {
            // 安眠香囊抵消失眠症，返回 false 让 EnigmaticEvents 跳过失眠处理
            return false;
        }
        // 正常返回睡眠状态
        return player.isPlayerSleeping();
    }

    /**
     * 拦截护甲降低效果 - 重定向 event.setAmount() 调用
     * 当玩家嵌入了守护鳞片时，不增加受到的伤害
     *
     * 注意：这个拦截点针对 onEntityHurt 中的 setAmount 调用
     */
    @Redirect(
            method = "onEntityHurt(Lnet/minecraftforge/event/entity/living/LivingHurtEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/event/entity/living/LivingHurtEvent;setAmount(F)V"
            ),
            require = 0
    )
    private void moremod$redirect_setAmount(LivingHurtEvent event, float amount) {
        Entity entity = event.getEntity();
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;

            // 检查是否嵌入了守护鳞片
            if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.GUARDIAN_SCALE)) {
                // 守护鳞片抵消护甲降低效果，不修改伤害
                return;
            }
        }
        // 正常执行 setAmount
        event.setAmount(amount);
    }

    /**
     * 拦截中立生物攻击效果
     * 当玩家嵌入了和平徽章时，取消诅咒戒指引起的额外仇恨
     *
     * 注意：中立生物攻击的处理在 onEntityTarget 中
     */
    @Inject(
            method = "onEntityTarget(Lnet/minecraftforge/event/entity/living/LivingSetAttackTargetEvent;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void moremod$onEntityTarget_head(Object event, CallbackInfo ci) {
        try {
            // 使用反射获取目标
            java.lang.reflect.Method getTarget = event.getClass().getMethod("getTarget");
            Object target = getTarget.invoke(event);

            if (target instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) target;

                // 检查是否嵌入了和平徽章
                if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.PEACE_EMBLEM)) {
                    // 和平徽章：检查这是否是诅咒戒指引起的仇恨
                    // 如果玩家有诅咒戒指，取消这次目标设置
                    if (hasCursedRing(player)) {
                        ci.cancel();
                    }
                }
            }
        } catch (Exception ignored) {
            // 反射失败时忽略
        }
    }

    /**
     * 检查玩家是否有诅咒戒指
     */
    private boolean hasCursedRing(EntityPlayer player) {
        try {
            // 使用反射检查 SuperpositionHandler.hasCursed
            Class<?> superpositionClass = Class.forName("keletu.enigmaticlegacy.event.SuperpositionHandler");
            java.lang.reflect.Method hasCursed = superpositionClass.getMethod("hasCursed", EntityPlayer.class);
            return (Boolean) hasCursed.invoke(null, player);
        } catch (Exception e) {
            return false;
        }
    }
}
