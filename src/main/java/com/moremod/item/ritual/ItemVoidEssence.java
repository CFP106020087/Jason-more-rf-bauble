package com.moremod.item.ritual;

import com.moremod.MoreMod;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 虚空精华 (Void Essence)
 *
 * 用于各种高级仪式的催化剂
 * 特别是复制仪式需要大量此物品
 */
public class ItemVoidEssence extends Item {

    public ItemVoidEssence() {
        setTranslationKey("moremod.void_essence");
        setRegistryName("void_essence");
        setCreativeTab(MoreMod.CREATIVE_TAB);
        setMaxStackSize(64);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_PURPLE + "从虚空中凝聚的纯粹能量");
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "用途:");
        tooltip.add(TextFormatting.DARK_GRAY + "- 复制仪式 (需要8个)");
        tooltip.add(TextFormatting.DARK_GRAY + "- 觉醒仪式");
        tooltip.add(TextFormatting.DARK_GRAY + "- 高级祭坛配方");
    }
}
