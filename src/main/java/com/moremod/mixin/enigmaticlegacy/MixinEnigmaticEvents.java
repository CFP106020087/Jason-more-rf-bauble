package com.moremod.mixin.enigmaticlegacy;

import com.moremod.entity.curse.EmbeddedCurseManager;
import com.moremod.entity.curse.EmbeddedCurseManager.EmbeddedRelicType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 拦截 EnigmaticEvents 中的诅咒效果，并转化为祝福
 *
 * 目标类: keletu.enigmaticlegacy.event.EnigmaticEvents
 *
 * 祝福效果：
 * 1. 霜华之露 (FROST_DEW) - 火焰抗性：立即灭火 + 短暂火焰抗性
 * 2. 安眠香囊 (SLUMBER_SACHET) - 安眠祝福：睡眠时获得生命恢复
 * 3. 守护鳞片 (GUARDIAN_SCALE) - 护甲强化：减伤30%而非增伤
 * 4. 和平徽章 (PEACE_EMBLEM) - 和平光环：取消所有仇恨锁定
 */
@Pseudo
@Mixin(targets = "keletu.enigmaticlegacy.event.EnigmaticEvents", remap = false)
public class MixinEnigmaticEvents {

    // ═══════════════════════════════════════════════════════════════
    // 霜华之露 - 火焰抗性祝福
    // 原诅咒：着火永燃
    // 祝福：立即灭火 + 给予火焰抗性 buff
    // ═══════════════════════════════════════════════════════════════

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
            // 祝福效果：立即灭火
            player.extinguish();
            // 给予 5 秒火焰抗性
            if (!player.isPotionActive(MobEffects.FIRE_RESISTANCE)) {
                player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 100, 0, false, true));
            }
            return;
        }
        // 正常执行诅咒 setFire
        player.setFire(seconds);
    }

    // ═══════════════════════════════════════════════════════════════
    // 安眠香囊 - 安眠祝福
    // 原诅咒：失眠症（无法睡觉）
    // 祝福：睡眠时获得生命恢复
    // ═══════════════════════════════════════════════════════════════

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
            // 祝福效果：允许睡觉，并给予睡眠时的生命恢复
            if (player.isPlayerSleeping()) {
                // 睡眠中给予再生效果
                if (!player.isPotionActive(MobEffects.REGENERATION)) {
                    player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 100, 1, false, false));
                }
            }
            // 返回 false 跳过失眠诅咒检查
            return false;
        }
        // 正常返回睡眠状态（让诅咒生效）
        return player.isPlayerSleeping();
    }

    // ═══════════════════════════════════════════════════════════════
    // 守护鳞片 - 护甲强化祝福
    // 原诅咒：护甲效力降低30%（受伤+30%）
    // 祝福：护甲效力提升（减伤30%）
    // ═══════════════════════════════════════════════════════════════

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
                // 祝福效果：减伤30%（而非增伤30%）
                float originalAmount = event.getAmount();
                float blessedAmount = originalAmount * 0.70f;
                event.setAmount(blessedAmount);
                return;
            }
        }
        // 正常执行诅咒 setAmount（增伤）
        event.setAmount(amount);
    }

    // ═══════════════════════════════════════════════════════════════
    // 和平徽章 - 和平光环祝福
    // 原诅咒：中立生物主动攻击
    // 祝福：取消所有诅咒引起的仇恨锁定
    // ═══════════════════════════════════════════════════════════════

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
                    // 祝福效果：和平光环 - 取消诅咒戒指引起的仇恨
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
            Class<?> superpositionClass = Class.forName("keletu.enigmaticlegacy.event.SuperpositionHandler");
            java.lang.reflect.Method hasCursed = superpositionClass.getMethod("hasCursed", EntityPlayer.class);
            return (Boolean) hasCursed.invoke(null, player);
        } catch (Exception e) {
            return false;
        }
    }
}
