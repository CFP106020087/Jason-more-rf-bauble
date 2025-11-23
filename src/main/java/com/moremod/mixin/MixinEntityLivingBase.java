package com.moremod.mixin;

import com.moremod.compat.crafttweaker.CustomElementType;
import com.moremod.compat.crafttweaker.ElementalConversionData;
import com.moremod.compat.crafttweaker.ElementTypeRegistry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 普通生物元素伤害 Mixin（转换率+增伤分离版本）
 *
 * 计算公式：
 * - 元素伤害 = 原始伤害 × 转换率 × 增伤倍率
 * - 物理伤害 = 原始伤害 × (1 - 转换率)
 * - 最终伤害 = 元素伤害 + 物理伤害
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

    private static final boolean DEBUG = true;

    @ModifyVariable(
            method = "func_70665_d",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float moremod$applyElementalDamage(float originalDamage, DamageSource source) {
        EntityLivingBase victim = (EntityLivingBase) (Object) this;

        if (!(source.getTrueSource() instanceof EntityPlayer)) {
            return originalDamage;
        }

        EntityPlayer attacker = (EntityPlayer) source.getTrueSource();
        ItemStack weapon = attacker.getHeldItemMainhand();

        if (weapon.isEmpty() || !ElementTypeRegistry.hasElementalGems(weapon)) {
            return originalDamage;
        }

        ElementalConversionData conversion = ElementTypeRegistry.calculateConversion(weapon);
        if (conversion == null || conversion.dominantType == null) {
            return originalDamage;
        }

        // ✅ 新计算方式
        float conversionRate = conversion.conversionRatio;        // 0.0 - 1.0
        float damageMultiplier = conversion.damageMultiplier;     // ≥1.0

        // 计算元素伤害和物理伤害
        float elementalDamage = originalDamage * conversionRate * damageMultiplier;
        float physicalDamage = originalDamage * (1.0f - conversionRate);
        float finalDamage = elementalDamage + physicalDamage;

        CustomElementType elementType = conversion.dominantType;

        if (DEBUG) {
            System.out.println("╔════════════════════════════════════════════════════════╗");
            System.out.println("║        元素伤害系统 - 转换率+增伤分离                 ║");
            System.out.println("╠════════════════════════════════════════════════════════╣");
            System.out.println("║ 攻击者: " + attacker.getName());
            System.out.println("║ 受害者: " + victim.getName() + " (" + victim.getClass().getSimpleName() + ")");
            System.out.println("║ 武器: " + weapon.getDisplayName());
            System.out.println("║ ════════════════════════════════════════════════════");
            System.out.println("║ 主导元素: " + elementType.getDisplayName());
            System.out.println("║ 宝石数量: " + conversion.totalGemCount + " 个");
            System.out.println("║ 混合元素: " + (conversion.isMixed ? "是" : "否"));
            System.out.println("║ ════════════════════════════════════════════════════");
            System.out.println("║ 转换率: " + String.format("%.0f%%", conversionRate * 100));
            System.out.println("║ 增伤倍率: ×" + String.format("%.1f", damageMultiplier));
            System.out.println("║ ════════════════════════════════════════════════════");
            System.out.println("║ 原始伤害: " + String.format("%.2f", originalDamage));
            System.out.println("║ 元素伤害: " + String.format("%.2f", elementalDamage) +
                    " (" + String.format("%.2f", originalDamage) +
                    " × " + String.format("%.0f%%", conversionRate * 100) +
                    " × " + String.format("%.1f", damageMultiplier) + ")");
            System.out.println("║ 物理伤害: " + String.format("%.2f", physicalDamage) +
                    " (" + String.format("%.2f", originalDamage) +
                    " × " + String.format("%.0f%%", (1 - conversionRate) * 100) + ")");
            System.out.println("║ ════════════════════════════════════════════════════");
            System.out.println("║ 最终伤害: " + String.format("%.2f", finalDamage));

            // 显示增伤百分比
            if (finalDamage > originalDamage) {
                float increase = (finalDamage / originalDamage - 1) * 100;
                System.out.println("║ 总增伤: +" + String.format("%.0f%%", increase));
            }

            System.out.println("╚════════════════════════════════════════════════════════╝");
        }

        return finalDamage;
    }
}