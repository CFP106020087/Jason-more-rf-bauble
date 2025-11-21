package com.moremod.mixin;

import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 防止玩家经验值/等级变成负数
 *
 * 问题：当 experienceTotal 或 experienceLevel 变成负数时，
 * Minecraft 的经验计算会导致整数溢出成 Integer.MIN_VALUE（负的最大整数）
 *
 * 解决方案：
 * 1. 拦截 addExperience 方法，防止经验值（XP）降到 0 以下
 * 2. 拦截 addExperienceLevel 方法，防止经验等级降到 0 以下
 */
@Mixin(EntityPlayer.class)
public abstract class MixinPlayerExperienceProtection {

    @Shadow
    public int experienceLevel;

    @Shadow
    public int experienceTotal;

    @Shadow
    public float experience;

    /**
     * 拦截 addExperience 方法，防止经验值（XP）变成负数
     * 这是最常见的溢出原因
     */
    @Inject(
            method = "func_195068_e",  // addExperience 的 SRG 名
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventNegativeExperience(int xp, CallbackInfo ci) {
        // 如果是增加经验，不需要特殊处理
        if (xp >= 0) {
            return;
        }

        // 如果扣除后会让 experienceTotal 变成负数，只扣到 0
        int newTotal = this.experienceTotal + xp;
        if (newTotal < 0) {
            // 取消原方法执行
            ci.cancel();

            // 将所有经验设置为 0
            this.experienceLevel = 0;
            this.experienceTotal = 0;
            this.experience = 0.0F;
        }
    }

    /**
     * 拦截 addExperienceLevel 方法，防止经验等级变成负数
     */
    @Inject(
            method = "func_82242_a",  // addExperienceLevel 的 SRG 名
            at = @At("HEAD"),
            cancellable = true
    )
    private void preventNegativeExperienceLevel(int levels, CallbackInfo ci) {
        // 如果是增加等级，不需要特殊处理
        if (levels >= 0) {
            return;
        }

        // 如果扣除后会变成负数，只扣到 0
        int newLevel = this.experienceLevel + levels;
        if (newLevel < 0) {
            // 取消原方法执行
            ci.cancel();

            // 将等级设置为 0
            this.experienceLevel = 0;
            this.experienceTotal = 0;
            this.experience = 0.0F;
        }
    }
}
