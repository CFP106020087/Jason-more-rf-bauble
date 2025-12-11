package com.moremod.mixin.enigmaticlegacy;

import com.moremod.entity.curse.EmbeddedCurseEffectHandler;
import com.moremod.entity.curse.EmbeddedCurseManager;
import com.moremod.entity.curse.EmbeddedCurseManager.EmbeddedRelicType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
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
 * 2. 安眠香囊 (SLUMBER_SACHET) - 安眠祝福：允许睡觉 + 睡眠时生命恢复
 * 3. 守护鳞片 (GUARDIAN_SCALE) - 护甲强化：减伤30%而非增伤
 * 4. 和平徽章 (PEACE_EMBLEM) - 和平光环：取消所有仇恨锁定
 */
@Pseudo
@Mixin(targets = "keletu.enigmaticlegacy.event.EnigmaticEvents", remap = false)
public class MixinEnigmaticEvents {

    // ═══════════════════════════════════════════════════════════════
    // 霜华之露 - 火焰抗性祝福
    // 原诅咒：着火永燃（永远在燃烧）
    // 祝福：立即灭火 + 给予火焰抗性 buff
    // ═══════════════════════════════════════════════════════════════

    /**
     * 拦截 setFire 调用，当有霜华之露时立即灭火
     */
    @Redirect(
            method = "tickHandler(Lnet/minecraftforge/fml/common/gameevent/TickEvent$PlayerTickEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;setFire(I)V"
            ),
            require = 0
    )
    private static void moremod$redirect_setFire(EntityPlayer player, int seconds) {
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

    /**
     * 拦截 isBurning 检查，当有霜华之露时假装不在燃烧
     * 这可以防止永燃诅咒的持续检查
     */
    @Redirect(
            method = "tickHandler(Lnet/minecraftforge/fml/common/gameevent/TickEvent$PlayerTickEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;isBurning()Z"
            ),
            require = 0
    )
    private static boolean moremod$redirect_isBurning(EntityPlayer player) {
        // 检查是否嵌入了霜华之露
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.FROST_DEW)) {
            // 如果正在燃烧，立即灭火并给予抗性
            if (player.isBurning()) {
                player.extinguish();
                if (!player.isPotionActive(MobEffects.FIRE_RESISTANCE)) {
                    player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 100, 0, false, true));
                }
            }
            // 返回 false 跳过诅咒的永燃检查
            return false;
        }
        return player.isBurning();
    }

    // ═══════════════════════════════════════════════════════════════
    // 安眠香囊 - 安眠祝福
    // 原诅咒：失眠症（无法睡觉）
    // 祝福：允许睡觉 + 睡眠时获得生命恢复
    // ═══════════════════════════════════════════════════════════════

    /**
     * 拦截 tickHandler 中的 isPlayerSleeping 检查
     * 当有安眠香囊时，返回 false 阻止诅咒踢出玩家
     */
    @Redirect(
            method = "tickHandler(Lnet/minecraftforge/fml/common/gameevent/TickEvent$PlayerTickEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;isPlayerSleeping()Z"
            ),
            require = 0
    )
    private static boolean moremod$redirect_isPlayerSleeping(EntityPlayer player) {
        // 检查是否应该绕过睡眠诅咒（使用统一的检查方法）
        if (EmbeddedCurseEffectHandler.shouldBypassSleepCurse(player)) {
            // 如果正在睡觉，给予再生效果（祝福）
            if (player.isPlayerSleeping()) {
                if (!player.isPotionActive(MobEffects.REGENERATION)) {
                    player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 100, 1, false, false));
                }
            }
            // 返回 false 阻止诅咒的睡眠检查（让诅咒认为玩家没在睡觉）
            return false;
        }
        return player.isPlayerSleeping();
    }

    // 注意：EnigmaticLegacy 没有 onSleepEnter 方法
    // 睡眠诅咒是通过 tickHandler 中设置 sleepTimer = 90 实现的
    // 我们通过 isPlayerSleeping 重定向 + EmbeddedCurseEffectHandler 中的 sleepTimer 推进来绕过

    // ═══════════════════════════════════════════════════════════════
    // 守护鳞片 - 护甲强化祝福
    // 原诅咒：护甲效力降低30%（受伤+30%）
    // 祝福：护甲效力提升（减伤30%）
    // ═══════════════════════════════════════════════════════════════

    /**
     * 拦截伤害事件中的 setAmount 调用
     * 诅咒会调用 setAmount(damage * 1.3) 增加伤害
     * 守护鳞片祝福将其改为 setAmount(damage * 0.7) 减少伤害
     */
    @Redirect(
            method = "onEntityHurt(Lnet/minecraftforge/event/entity/living/LivingHurtEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/event/entity/living/LivingHurtEvent;setAmount(F)V"
            ),
            require = 0
    )
    private static void moremod$redirect_setAmount(LivingHurtEvent event, float amount) {
        Entity entity = event.getEntity();
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;

            // 检查是否嵌入了守护鳞片
            if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.GUARDIAN_SCALE)) {
                // 诅咒会设置 amount = originalDamage * 1.3
                // 我们需要还原 originalDamage = amount / 1.3
                // 然后应用祝福 blessedAmount = originalDamage * 0.7
                // 最终效果：amount / 1.3 * 0.7 ≈ amount * 0.538
                // 相对于诅咒伤害减少约 46%
                float blessedAmount = amount * 0.538f;
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

    /**
     * 检查生物是否是条件攻击型（中立/被动攻击）
     * 这些生物正常情况下不会主动攻击，但七咒会让它们主动攻击
     */
    private static boolean isConditionallyHostile(EntityLivingBase entity) {
        String className = entity.getClass().getSimpleName();

        // 末影人 - 只有被看时才攻击
        if (className.contains("Enderman")) return true;
        // 僵尸猪人 - 只有被攻击时才攻击
        if (className.contains("PigZombie") || className.contains("ZombiePigman")) return true;
        // 狼（未驯服）- 只有被攻击时才攻击
        if (entity instanceof EntityWolf) return true;
        // 北极熊 - 只有有幼崽或被攻击时才攻击
        if (entity instanceof EntityPolarBear) return true;
        // 铁傀儡 - 正常不攻击玩家（除非玩家攻击村民）
        if (className.contains("IronGolem")) return true;
        // 雪傀儡 - 正常不攻击玩家
        if (className.contains("SnowMan") || className.contains("Snowman")) return true;
        // 蜘蛛 - 只在黑暗中攻击
        if (className.contains("Spider") && !className.contains("CaveSpider")) return true;
        // 所有非EntityMob的生物（如动物）
        if (!(entity instanceof EntityMob)) return true;

        return false;
    }

    /**
     * 拦截 onLivingChangeTarget 事件处理
     * 当有和平徽章时，取消中立生物的攻击目标设置
     * 注意：EnigmaticLegacy 的方法名是 onLivingChangeTarget，不是 onEntityTarget
     */
    @Inject(
            method = "onLivingChangeTarget(Lnet/minecraftforge/event/entity/living/LivingSetAttackTargetEvent;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void moremod$onLivingChangeTarget_head(LivingSetAttackTargetEvent event, CallbackInfo ci) {
        if (!(event.getTarget() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getTarget();

        // 检查是否嵌入了和平徽章
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.PEACE_EMBLEM)) {
            // 检查是否是条件攻击型生物（中立/被动）
            if (isConditionallyHostile(event.getEntityLiving())) {
                // 祝福效果：取消诅咒引起的仇恨
                ci.cancel();

                // 清除攻击者的目标
                if (event.getEntityLiving() instanceof EntityCreature) {
                    ((EntityCreature) event.getEntityLiving()).setAttackTarget(null);
                } else if (event.getEntityLiving() instanceof EntityLiving) {
                    ((EntityLiving) event.getEntityLiving()).setAttackTarget(null);
                }
            }
        }
    }
}
