package com.moremod.mixin.parasites;

import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "com.dhanantry.scapeandrunparasites.entity.ai.misc.EntityPMalleable",
        remap = false,
        priority = 2000
)
public class MixinEntityPMalleableVoidLaser {

    @Unique
    private static final ThreadLocal<Boolean> moremod$isVoidLaser =
            ThreadLocal.withInitial(() -> false);

    @Inject(
            method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
            at = @At("HEAD"),
            require = 0
    )
    private void moremod$detectVoidLaser(DamageSource source, float amount,
                                         CallbackInfoReturnable<Boolean> cir) {
        moremod$isVoidLaser.set(
                source.getDamageType().equals("moremod.void_laser")
        );
    }

    @ModifyVariable(
            method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
            at = @At(value = "STORE", ordinal = 0),
            name = "bonus",
            require = 0
    )
    private float moremod$nullifyBonus(float bonus) {
        return moremod$isVoidLaser.get() ? 0.0f : bonus;
    }

    // 阻止虚空镭射被学习
    @Inject(
            method = "hasResistance(Ljava/lang/String;B)I",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void moremod$preventVoidLaserLearning(String damage, byte type,
                                                  CallbackInfoReturnable<Integer> cir) {
        if ("moremod.void_laser".equals(damage)) {
            cir.setReturnValue(0);
        }
    }

    // 阻止添加虚空镭射抗性
    @Inject(
            method = "addResistance(Ljava/lang/String;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void moremod$blockVoidLaserResistance(String damage,
                                                  CallbackInfoReturnable<Void> cir) {
        if ("moremod.void_laser".equals(damage)) {
            cir.cancel();
        }
    }

    // 清理ThreadLocal
    @Inject(
            method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
            at = @At("RETURN"),
            require = 0
    )
    private void moremod$cleanup(DamageSource source, float amount,
                                 CallbackInfoReturnable<Boolean> cir) {
        moremod$isVoidLaser.remove();
    }
}