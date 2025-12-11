package com.moremod.item.energy;

import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 石油桶 - 原油
 *
 * 用途：
 * - 放入石油發電機發電
 * - 可精煉成更高效的燃料
 */
public class ItemOilBucket extends Item {

    // 每桶石油的發電量
    public static final int RF_PER_BUCKET = 500000;  // 500k RF (大幅提升)
    public static final int BURN_TIME = 400;         // 20秒 (400 ticks, 加快处理)

    public ItemOilBucket() {
        setMaxStackSize(16);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("crude_oil_bucket");
        setTranslationKey("crude_oil_bucket");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.DARK_GRAY + "從地下石油礦脈中提取的原油");
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "發電效率: " + TextFormatting.WHITE + formatRF(RF_PER_BUCKET) + " RF/桶");
        tooltip.add(TextFormatting.YELLOW + "燃燒時間: " + TextFormatting.WHITE + (BURN_TIME / 20) + " 秒");
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "放入石油發電機使用");
    }

    private String formatRF(int rf) {
        if (rf >= 1000000) {
            return String.format("%.1fM", rf / 1000000.0);
        } else if (rf >= 1000) {
            return String.format("%.0fk", rf / 1000.0);
        }
        return String.valueOf(rf);
    }
}
