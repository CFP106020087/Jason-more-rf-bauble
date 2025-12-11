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
 * 植物油桶 - 生物燃料
 *
 * 用途：
 * - 放入石油發電機發電
 * - 由壓榨機從農作物中提取
 * - 比原油更環保但產量較低
 */
public class ItemPlantOilBucket extends Item {

    // 每桶植物油的發電量（比原油低）
    public static final int RF_PER_BUCKET = 60000;   // 60k RF
    public static final int BURN_TIME = 1200;        // 60秒 (1200 ticks)

    public ItemPlantOilBucket() {
        setMaxStackSize(16);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("plant_oil_bucket");
        setTranslationKey("plant_oil_bucket");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GREEN + "從農作物中壓榨出的生物燃料");
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "發電效率: " + TextFormatting.WHITE + formatRF(RF_PER_BUCKET) + " RF/桶");
        tooltip.add(TextFormatting.YELLOW + "燃燒時間: " + TextFormatting.WHITE + (BURN_TIME / 20) + " 秒");
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "放入石油發電機使用");
        tooltip.add(TextFormatting.DARK_GREEN + "✓ 可再生能源");
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
