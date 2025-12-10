package com.moremod.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * 附魔增强方块的物品形式 - 支持多个子类型
 */
public class ItemBlockEnchantingBooster extends ItemBlock {

    public ItemBlockEnchantingBooster(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        BlockEnchantingBooster.BoosterType type = BlockEnchantingBooster.BoosterType.byMeta(stack.getMetadata());
        return getUnlocalizedName() + "." + type.getName();
    }
}
