package com.moremod.mixin;

import atomicstryker.infernalmobs.common.MobModifier;
import com.moremod.causal.CausalFieldManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 攔截 MobModifier 的所有效果方法
 * 在沉默場內完全禁用詞條效果
 */
@Mixin(value = MobModifier.class, remap = false)
public abstract class MixinMobModifier {

    /**
     * 攔截 onUpdate
     * 方法簽名: public boolean onUpdate(EntityLivingBase mob)
     */
    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private void onUpdate(EntityLivingBase mob, CallbackInfoReturnable<Boolean> cir) {
        if (mob != null && !mob.world.isRemote
                && CausalFieldManager.isInField(mob.world, mob.posX, mob.posY, mob.posZ)) {
            cir.setReturnValue(false);  // 返回 false，取消所有 tick 效果
        }
    }

    /**
     * 攔截 onAttack - 當這個精英攻擊別人時
     * 方法簽名: public float onAttack(EntityLivingBase entity, DamageSource source, float amount)
     *
     * 注意：這裡的 entity 是被攻擊者，source 包含攻擊者信息
     * 我們需要從 source 獲取攻擊者（精英）的位置
     */
    @Inject(method = "onAttack", at = @At("HEAD"), cancellable = true, remap = false)
    private void onAttack(EntityLivingBase entity, DamageSource source, float amount,
                          CallbackInfoReturnable<Float> cir) {
        // 檢查攻擊來源
        if (source != null && source.getTrueSource() instanceof EntityLivingBase) {
            EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();
            if (!attacker.world.isRemote
                    && CausalFieldManager.isInField(attacker.world, attacker.posX, attacker.posY, attacker.posZ)) {
                cir.setReturnValue(amount);  // 返回原始傷害，不修改
            }
        }
    }

    /**
     * 攔截 onHurt - 當這個精英受傷時
     * 方法簽名: public float onHurt(EntityLivingBase mob, DamageSource source, float amount)
     */
    @Inject(method = "onHurt", at = @At("HEAD"), cancellable = true, remap = false)
    private void onHurt(EntityLivingBase mob, DamageSource source, float amount,
                        CallbackInfoReturnable<Float> cir) {
        if (mob != null && !mob.world.isRemote
                && CausalFieldManager.isInField(mob.world, mob.posX, mob.posY, mob.posZ)) {
            cir.setReturnValue(amount);  // 返回原始傷害
        }
    }

    /**
     * 攔截 onSetAttackTarget
     * 方法簽名: public void onSetAttackTarget(EntityLivingBase target)
     *
     * 注意：這個方法沒有傳入 mob 參數，我們無法直接獲取精英位置
     * 但是當場內的精英調用這個方法時，onUpdate 已經被攔截了，所以不會有攻擊
     */
    @Inject(method = "onSetAttackTarget", at = @At("HEAD"), cancellable = true, remap = false)
    private void onSetAttackTarget(EntityLivingBase target, CallbackInfo ci) {
        // 由於無法獲取 mob 實例，這個方法可以不攔截
        // onUpdate 已經阻止了攻擊邏輯
    }

    /**
     * 攔截 onJump
     * 方法簽名: public void onJump(EntityLivingBase entityLiving)
     */
    @Inject(method = "onJump", at = @At("HEAD"), cancellable = true, remap = false)
    private void onJump(EntityLivingBase entityLiving, CallbackInfo ci) {
        if (entityLiving != null && !entityLiving.world.isRemote
                && CausalFieldManager.isInField(entityLiving.world, entityLiving.posX, entityLiving.posY, entityLiving.posZ)) {
            ci.cancel();
        }
    }

    /**
     * 攔截 onFall
     * 方法簽名: public boolean onFall(float distance)
     *
     * 注意：這個方法也沒有傳入 mob，無法檢查位置
     * 但摔落傷害通常不是重要效果，可以不攔截
     */
    @Inject(method = "onFall", at = @At("HEAD"), cancellable = true, remap = false)
    private void onFall(float distance, CallbackInfoReturnable<Boolean> cir) {
        // 無法獲取 mob 位置，跳過
        // 如果需要，可以通過其他方式追蹤
    }

    /**
     * 攔截 onDeath - 返回 false 允許正常死亡
     * 方法簽名: public boolean onDeath()
     */
    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true, remap = false)
    private void onDeath(CallbackInfoReturnable<Boolean> cir) {
        // 死亡時不攔截，讓掉落正常進行
    }

    /**
     * 攔截 onDropItems
     * 方法簽名: public void onDropItems(EntityLivingBase moddedMob, DamageSource killSource,
     *                                    List<EntityItem> drops, int lootingLevel,
     *                                    boolean recentlyHit, int specialDropValue)
     */
    @Inject(method = "onDropItems", at = @At("HEAD"), cancellable = true, remap = false)
    private void onDropItems(EntityLivingBase moddedMob, DamageSource killSource, List<EntityItem> drops,
                             int lootingLevel, boolean recentlyHit, int specialDropValue, CallbackInfo ci) {
        // 掉落時不攔截
    }
}