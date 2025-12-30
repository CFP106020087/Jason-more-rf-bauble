package com.moremod.mixin;

import com.moremod.item.causal.CausalFieldManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 攔截 MobModifier 的所有效果方法
 * 在沉默場內完全禁用詞條效果
 *
 * 使用 @Pseudo 避免 InfernalMobs 未安裝時崩潰
 */
@Pseudo
@Mixin(targets = "atomicstryker.infernalmobs.common.MobModifier", remap = false)
public abstract class MixinMobModifier {

    /**
     * 攔截 onUpdate
     * 方法簽名: public boolean onUpdate(EntityLivingBase mob)
     */
    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void moremod$onUpdate(EntityLivingBase mob, CallbackInfoReturnable<Boolean> cir) {
        if (mob != null && !mob.world.isRemote
                && CausalFieldManager.isInField(mob.world, mob.posX, mob.posY, mob.posZ)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 攔截 onAttack - 當這個精英攻擊別人時
     */
    @Inject(method = "onAttack", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void moremod$onAttack(EntityLivingBase entity, DamageSource source, float amount,
                          CallbackInfoReturnable<Float> cir) {
        if (source != null && source.getTrueSource() instanceof EntityLivingBase) {
            EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();
            if (!attacker.world.isRemote
                    && CausalFieldManager.isInField(attacker.world, attacker.posX, attacker.posY, attacker.posZ)) {
                cir.setReturnValue(amount);
            }
        }
    }

    /**
     * 攔截 onHurt - 當這個精英受傷時
     */
    @Inject(method = "onHurt", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void moremod$onHurt(EntityLivingBase mob, DamageSource source, float amount,
                        CallbackInfoReturnable<Float> cir) {
        if (mob != null && !mob.world.isRemote
                && CausalFieldManager.isInField(mob.world, mob.posX, mob.posY, mob.posZ)) {
            cir.setReturnValue(amount);
        }
    }

    /**
     * 攔截 onSetAttackTarget
     */
    @Inject(method = "onSetAttackTarget", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void moremod$onSetAttackTarget(EntityLivingBase target, CallbackInfo ci) {
        // 由於無法獲取 mob 實例，這個方法可以不攔截
    }

    /**
     * 攔截 onJump
     */
    @Inject(method = "onJump", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void moremod$onJump(EntityLivingBase entityLiving, CallbackInfo ci) {
        if (entityLiving != null && !entityLiving.world.isRemote
                && CausalFieldManager.isInField(entityLiving.world, entityLiving.posX, entityLiving.posY, entityLiving.posZ)) {
            ci.cancel();
        }
    }

    /**
     * 攔截 onFall
     */
    @Inject(method = "onFall", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void moremod$onFall(float distance, CallbackInfoReturnable<Boolean> cir) {
        // 無法獲取 mob 位置，跳過
    }

    /**
     * 攔截 onDeath
     */
    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void moremod$onDeath(CallbackInfoReturnable<Boolean> cir) {
        // 死亡時不攔截
    }

    /**
     * 攔截 onDropItems
     */
    @Inject(method = "onDropItems", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void moremod$onDropItems(EntityLivingBase moddedMob, DamageSource killSource, List<EntityItem> drops,
                             int lootingLevel, boolean recentlyHit, int specialDropValue, CallbackInfo ci) {
        // 掉落時不攔截
    }
}
