package com.moremod.item;

import com.moremod.creativetab.MoremodMaterialsTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 通用材料物品基类（可选发光、稀有度、tooltip）。
 * 1.12.2 注意：EnumRarity 仅作显示使用。
 */
@SuppressWarnings("deprecation")
public class ItemMaterial extends Item {

    private final EnumRarity rarity;
    private final boolean glows;
    private final String tooltipKey; // lang key: item.moremod.xxx.desc

    public ItemMaterial(String registryName, EnumRarity rarity, boolean glows, @Nullable String tooltipKey) {
        this.rarity = rarity == null ? EnumRarity.COMMON : rarity;
        this.glows = glows;
        this.tooltipKey = tooltipKey;
        setRegistryName(new ResourceLocation("moremod", registryName));
        setTranslationKey("moremod." + registryName);
        setMaxStackSize(64);
        setHasSubtypes(false);
        setCreativeTab(MoremodMaterialsTab.MATERIALS_TAB);

    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return glows || super.hasEffect(stack);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return rarity;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (tooltipKey != null && I18n.canTranslate(tooltipKey)) {
            tooltip.add(TextFormatting.DARK_PURPLE + I18n.translateToLocal(tooltipKey));
        }
    }

    // 方便后续批量填充创造标签用
    public static void fillAll(NonNullList<ItemStack> items, Item item) {
        items.add(new ItemStack(item));
    }
}
