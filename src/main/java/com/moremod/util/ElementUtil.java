package com.moremod.util;

import com.moremod.compat.crafttweaker.GemAffix;
import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.compat.crafttweaker.GemSocketHelper;
import com.moremod.compat.crafttweaker.IdentifiedAffix;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * 元素相关工具函数
 * 判定武器是否存在“元素转换（DAMAGE_CONVERSION）”词条。
 */
public class ElementUtil {

    /**
     * 检查武器是否具有宝石“伤害转换（物理→元素）”词条
     */
    public static boolean isElementalWeapon(ItemStack stack) {

        if (stack.isEmpty()) return false;

        // 1) 武器自身附带元素标签
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("mm_element_type")) {
            return true;
        }

        // 2) 你的宝石系统转换词条（已有）
        if (hasElementConversion(stack)) {
            return true;
        }

        // 3) 未来可扩展（例如元素武器 class instanceof IElementalWeapon）

        return false;
    }
    public static boolean hasElementConversion(ItemStack weapon) {
        if (weapon.isEmpty() || !GemSocketHelper.hasSocketedGems(weapon)) {
            return false;
        }

        ItemStack[] gems = GemSocketHelper.getAllSocketedGems(weapon);
        if (gems == null || gems.length == 0) {
            return false;
        }

        for (ItemStack gem : gems) {
            if (gem.isEmpty() || !GemNBTHelper.isIdentified(gem)) continue;

            List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
            if (affixes == null) continue;

            for (IdentifiedAffix affix : affixes) {
                if (affix.getAffix().getType() == GemAffix.AffixType.DAMAGE_CONVERSION) {
                    return true; // ⭐ 找到元素转换
                }
            }
        }

        return false;
    }
}
