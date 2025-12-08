package com.moremod.item;

import com.moremod.creativetab.MoremodMaterialsTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 基础织布物品 - 显示描述和织入效果
 */
@SuppressWarnings("deprecation")
public class ItemBasicFabric extends Item {

    private final EnumRarity rarity;
    private final String descKey;    // item.moremod.xxx.desc
    private final String effectKey;  // item.moremod.xxx.effect

    public ItemBasicFabric(String registryName, EnumRarity rarity) {
        this.rarity = rarity == null ? EnumRarity.COMMON : rarity;
        this.descKey = "item.moremod." + registryName + ".desc";
        this.effectKey = "item.moremod." + registryName + ".effect";

        setRegistryName(new ResourceLocation("moremod", registryName));
        setTranslationKey("moremod." + registryName);
        setMaxStackSize(64);
        setCreativeTab(MoremodMaterialsTab.MATERIALS_TAB);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return rarity;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        // 显示描述
        if (I18n.canTranslate(descKey)) {
            tooltip.add(TextFormatting.GRAY + I18n.translateToLocal(descKey));
        }

        // 显示织入效果
        if (I18n.canTranslate(effectKey)) {
            tooltip.add("");
            tooltip.add(I18n.translateToLocal(effectKey));
        }
    }
}
