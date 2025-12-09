package com.moremod.item.energy;

import com.moremod.moremod;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 發電機增速插件
 *
 * 放入石油發電機的增速槽可增加發電效率
 * 每個插件增加50%發電速度，最多4個
 */
public class ItemSpeedUpgrade extends Item {

    public ItemSpeedUpgrade() {
        setRegistryName(moremod.MODID, "speed_upgrade");
        setTranslationKey(moremod.MODID + ".speed_upgrade");
        setCreativeTab(CreativeTabs.REDSTONE);
        setMaxStackSize(16);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true; // 發光效果
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add("§e發電機增速插件");
        tooltip.add("§7放入石油發電機的增速槽");
        tooltip.add("");
        tooltip.add("§a效果: §f+50% 發電速度");
        tooltip.add("§b最多: §f4個 (300%總效率)");
    }
}
