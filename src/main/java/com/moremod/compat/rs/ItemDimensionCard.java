package com.moremod.compat.rs;

import com.moremod.compat.rs.RSConfig;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 维度卡 - 放入无线发射器使其支持跨维度访问
 */
public class ItemDimensionCard extends Item {
    
    public ItemDimensionCard() {
        setRegistryName("moremod", "dimension_card");
        setTranslationKey("moremod.dimension_card");
        setMaxStackSize(1);
        
        // 尝试使用 RS 的 CreativeTab
        try {
            Class<?> rsClass = Class.forName("com.raoulvdberge.refinedstorage.RS");
            Object rsInstance = rsClass.getField("INSTANCE").get(null);
            if (rsInstance instanceof CreativeTabs) {
                setCreativeTab((CreativeTabs) rsInstance);
            } else {
                setCreativeTab(CreativeTabs.MISC);
            }
        } catch (Exception e) {
            setCreativeTab(CreativeTabs.MISC);
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18n.format("item.moremod.dimension_card.tooltip"));
        tooltip.add(I18n.format("item.moremod.rs_card.energy", RSConfig.dimensionCardEnergyUsage));
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true; // 附魔光效
    }
}
