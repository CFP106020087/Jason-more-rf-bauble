package com.moremod.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemAxe;

/**
 * 真伤附魔 - 将武器伤害转换为真实伤害（无视护甲）
 * 非常稀有但可从附魔台获得
 */
public class EnchantmentTrueDamage extends Enchantment {

    public static final String NAME = "true_damage";

    public EnchantmentTrueDamage() {
        super(Rarity.VERY_RARE, EnumEnchantmentType.WEAPON, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND});
        setName("moremod." + NAME);
        setRegistryName("moremod", NAME);
    }

    @Override
    public int getMinEnchantability(int level) {
        // 非常高的附魔等级要求：30级起步
        return 30 + (level - 1) * 15;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 3; // 最高3级
    }

    @Override
    public boolean canApply(ItemStack stack) {
        // 只能应用于剑和斧
        return stack.getItem() instanceof ItemSword || stack.getItem() instanceof ItemAxe;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return canApply(stack);
    }

    @Override
    public boolean isTreasureEnchantment() {
        return false; // 非宝藏附魔，可以从附魔台获得
    }

    @Override
    public boolean isCurse() {
        return false;
    }

    /**
     * 获取真伤转换比例
     * @param level 附魔等级
     * @return 转换比例 (0.0 - 1.0)
     */
    public static float getTrueDamageRatio(int level) {
        switch (level) {
            case 1: return 0.25f;  // 25% 伤害转真伤
            case 2: return 0.50f;  // 50% 伤害转真伤
            case 3: return 0.75f;  // 75% 伤害转真伤
            default: return 0f;
        }
    }
}
