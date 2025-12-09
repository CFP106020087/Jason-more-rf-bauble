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
 * 无限卡 - 放入无线发射器使其获得无限范围
 * 
 * 注意：如果继承 RS 的 ItemUpgrade 会自动被升级槽接受，
 * 但这需要硬依赖 RS。当前使用普通 Item + ASM 补丁。
 */
public class ItemInfinityCard extends Item {
    
    public ItemInfinityCard() {
        setRegistryName("moremod", "infinity_card");
        setTranslationKey("moremod.infinity_card");
        setMaxStackSize(1);
        
        // 尝试使用 RS 的 CreativeTab
        try {
            Class<?> rsClass = Class.forName("com.raoulvdberge.refinedstorage.RS");
            java.lang.reflect.Field tabField = rsClass.getField("INSTANCE");
            Object tab = tabField.get(null);
            if (tab instanceof CreativeTabs) {
                setCreativeTab((CreativeTabs) tab);
            } else {
                // RS.INSTANCE 可能是 mod 实例，尝试获取 tab
                try {
                    java.lang.reflect.Field creativeTabField = rsClass.getDeclaredField("creativeTab");
                    creativeTabField.setAccessible(true);
                    Object creativeTab = creativeTabField.get(tab);
                    if (creativeTab instanceof CreativeTabs) {
                        setCreativeTab((CreativeTabs) creativeTab);
                    } else {
                        setCreativeTab(CreativeTabs.MISC);
                    }
                } catch (Exception e2) {
                    setCreativeTab(CreativeTabs.MISC);
                }
            }
        } catch (Exception e) {
            setCreativeTab(CreativeTabs.MISC);
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18n.format("item.moremod.infinity_card.tooltip"));
        tooltip.add(I18n.format("item.moremod.rs_card.energy", RSConfig.infinityCardEnergyUsage));
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true; // 附魔光效
    }
}
