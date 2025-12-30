package com.moremod.mixin.parasites;

import com.moremod.item.ItemArcGaze;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 防止寄生虫对ArcGaze电弧伤害产生抗性
 */
@Pseudo
@Mixin(
        targets = "com.dhanantry.scapeandrunparasites.entity.ai.misc.EntityPMalleable",
        remap = false,
        priority = 2000
)
public class MixinEntityPMalleableArcGaze {

    @Unique
    private static final ThreadLocal<Boolean> moremod$isArcDamage =
            ThreadLocal.withInitial(() -> false);

    /**
     * 检测是否为电弧伤害
     */
    @Inject(
            method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
            at = @At("HEAD"),
            require = 0
    )
    private void moremod$detectArcDamage(DamageSource source, float amount,
                                         CallbackInfoReturnable<Boolean> cir) {
        boolean isArc = source instanceof ItemArcGaze.ArcPiercingDamage ||
                "arc_piercing".equals(source.getDamageType()) ||
                "moremod.arc_piercing".equals(source.getDamageType());
        moremod$isArcDamage.set(isArc);
    }

    /**
     * 如果是电弧伤害，将bonus设为0（取消伤害减免）
     */
    @ModifyVariable(
            method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
            at = @At(value = "STORE", ordinal = 0),
            name = "bonus",
            require = 0
    )
    private float moremod$nullifyArcBonus(float bonus) {
        return moremod$isArcDamage.get() ? 0.0f : bonus;
    }

    /**
     * 阻止电弧伤害被学习
     */
    @Inject(
            method = "hasResistance(Ljava/lang/String;B)I",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void moremod$preventArcLearning(String damage, byte type,
                                            CallbackInfoReturnable<Integer> cir) {
        if ("arc_piercing".equals(damage) ||
                "moremod.arc_piercing".equals(damage)) {
            cir.setReturnValue(0);  // 永远没有抗性
        }
    }

    /**
     * 阻止添加电弧抗性
     */
    @Inject(
            method = "addResistance(Ljava/lang/String;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void moremod$blockArcResistance(String damage,
                                            CallbackInfoReturnable<Void> cir) {
        if ("arc_piercing".equals(damage) ||
                "moremod.arc_piercing".equals(damage)) {
            cir.cancel();  // 阻止添加
        }
    }

    /**
     * 可选：阻止获取电弧伤害抗性
     */
    @Inject(
            method = "getResistance(Ljava/lang/String;)B",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void moremod$getArcResistance(String damage,
                                          CallbackInfoReturnable<Byte> cir) {
        if ("arc_piercing".equals(damage) ||
                "moremod.arc_piercing".equals(damage)) {
            cir.setReturnValue((byte)0);  // 返回0抗性
        }
    }

    /**
     * 清理ThreadLocal避免内存泄漏
     */
    @Inject(
            method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
            at = @At("RETURN"),
            require = 0
    )
    private void moremod$cleanup(DamageSource source, float amount,
                                 CallbackInfoReturnable<Boolean> cir) {
        moremod$isArcDamage.remove();
    }

    /**
     * 可选：确保电弧伤害总是全额
     */
    @ModifyVariable(
            method = "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private float moremod$ensureArcDamage(float amount, DamageSource source) {
        if (source instanceof ItemArcGaze.ArcPiercingDamage) {
            // 可以在这里增加伤害倍率
            return amount * 1.5F;  // 对寄生虫150%伤害
        }
        return amount;
    }
}