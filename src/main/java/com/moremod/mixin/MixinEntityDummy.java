package com.moremod.mixin;

import com.moremod.compat.crafttweaker.CustomElementType;
import com.moremod.compat.crafttweaker.ElementalConversionData;
import com.moremod.compat.crafttweaker.ElementTypeRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 训练假人元素伤害 Mixin（转换率+增伤分离版本）
 */
@Pseudo
@Mixin(targets = "testdummy.entity.EntityDummy", remap = false)
public abstract class MixinEntityDummy {

    private static final boolean DEBUG = true;

    @ModifyVariable(
            method = {
                    "func_70097_a"
            },
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private float moremod$applyElementalDamageToDummy(float originalDamage, DamageSource source) {
        if (source == null || source.getTrueSource() == null) {
            return originalDamage;
        }

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

        // ✅ 计算最终伤害
        float conversionRate = conversion.conversionRatio;
        float damageMultiplier = conversion.damageMultiplier;

        float elementalDamage = originalDamage * conversionRate * damageMultiplier;
        float physicalDamage = originalDamage * (1.0f - conversionRate);
        float finalDamage = elementalDamage + physicalDamage;

        CustomElementType elementType = conversion.dominantType;

        if (DEBUG) {
            System.out.println("╔═══════════════════════════════════════════════════════╗");
            System.out.println("║         训练假人元素伤害 - 转换率+增伤分离           ║");
            System.out.println("╠═══════════════════════════════════════════════════════╣");
            System.out.println("║ ⭐ 训练假人测试");
            System.out.println("║ 攻击者: " + attacker.getName());
            System.out.println("║ 武器: " + weapon.getDisplayName());
            System.out.println("║ ═════════════════════════════════════════════════════");
            System.out.println("║ 主导元素: " + elementType.getDisplayName());
            System.out.println("║ 宝石数量: " + conversion.totalGemCount + " 个");
            System.out.println("║ 转换率: " + String.format("%.0f%%", conversionRate * 100));
            System.out.println("║ 增伤倍率: ×" + String.format("%.1f", damageMultiplier));
            System.out.println("║ ═════════════════════════════════════════════════════");
            System.out.println("║ 原始伤害: " + String.format("%.2f", originalDamage));
            System.out.println("║ → 元素部分: " + String.format("%.2f", originalDamage * conversionRate) +
                    " (" + String.format("%.0f%%", conversionRate * 100) + ")");
            System.out.println("║   × 增伤: " + String.format("%.2f", elementalDamage));
            System.out.println("║ → 物理部分: " + String.format("%.2f", physicalDamage) +
                    " (" + String.format("%.0f%%", (1 - conversionRate) * 100) + ")");
            System.out.println("║ ═════════════════════════════════════════════════════");
            System.out.println("║ ✅ 假人将显示: " + String.format("%.2f", finalDamage));
            System.out.println("╚═══════════════════════════════════════════════════════╝");
        }

        return finalDamage;
    }
}