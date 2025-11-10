package com.moremod.mixin.enchantments;

import com.shultrea.rin.enchantments.weapon.subject.EnchantmentSubjectEnchantments;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 修正版 - 使用正确的混淆映射
 */
@Mixin(value = EnchantmentSubjectEnchantments.class, remap = false)
public class FixedEnchantmentSubjectPEMixin {

    @Shadow(remap = false)
    private int damageType;

    /**
     * 使用SRG名称 func_70690_d 替代 addPotionEffect
     */
    @Redirect(
            method = "onEntityDamagedAlt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;func_70690_d(Lnet/minecraft/potion/PotionEffect;)V"
            ),
            require = 0
    )
    private void redirectPotionEffect(EntityLivingBase entity, PotionEffect potionEffect) {
        // 只处理体育附魔（damageType == 5）
        if (this.damageType == 5) {
            // 检查是否是抗性提升效果
            if (potionEffect.getPotion() == MobEffects.RESISTANCE) {
                // 限制抗性提升等级
                int originalAmplifier = potionEffect.getAmplifier();
                int limitedAmplifier = Math.min(originalAmplifier, 4); // 最高显示为抗性提升 V

                // 如果等级被限制了，创建新的药水效果
                if (originalAmplifier != limitedAmplifier) {
                    PotionEffect limitedEffect = new PotionEffect(
                            MobEffects.RESISTANCE,
                            potionEffect.getDuration(),
                            limitedAmplifier,
                            potionEffect.getIsAmbient(),
                            potionEffect.doesShowParticles()
                    );
                    entity.addPotionEffect(limitedEffect);
                    return;
                }
            }
        }

        // 其他情况正常应用
        entity.addPotionEffect(potionEffect);
    }
}