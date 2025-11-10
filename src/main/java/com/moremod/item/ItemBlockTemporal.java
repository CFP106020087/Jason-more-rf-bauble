package com.moremod.item;

import com.moremod.block.BlockTemporalAccelerator;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 时间加速器的物品方块 - 适配RegisterItem框架
 *
 * 方案1: 无参构造函数（推荐）
 * 在构造函数内部创建或获取方块实例
 */
public class ItemBlockTemporal extends ItemBlock {

    // 方案1: 无参构造函数 - 内部创建方块
    public ItemBlockTemporal() {
        super(createOrGetBlock());
        setRegistryName( "temporal_accelerator");
        setTranslationKey("temporal_accelerator");
        setCreativeTab(moremodCreativeTab.moremod_TAB);

    }

    // 辅助方法：创建或获取方块实例
    private static Block createOrGetBlock() {
        // 如果ModBlocks已经初始化了方块，使用它
        if (ModBlocks.TEMPORAL_ACCELERATOR != null) {
            return ModBlocks.TEMPORAL_ACCELERATOR;
        }
        // 否则创建新的方块实例
        Block block = new BlockTemporalAccelerator();
        ModBlocks.TEMPORAL_ACCELERATOR = block;
        return block;
    }

    // 方案2: 带参构造函数（备用）
    public ItemBlockTemporal(Block block) {
        super(block);
        setRegistryName(block.getRegistryName());
        setTranslationKey(block.getTranslationKey());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        // 添加提示信息
        tooltip.add(TextFormatting.GOLD + "时间加速器");
        tooltip.add(TextFormatting.GRAY + "加速16x16范围内的时间流动");
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "能量需求:");
        tooltip.add(TextFormatting.WHITE + "  存储: " + TextFormatting.AQUA + "100,000 FE");
        tooltip.add(TextFormatting.WHITE + "  消耗: " + TextFormatting.RED + "20-25 FE/tick");
        tooltip.add("");
        tooltip.add(TextFormatting.GREEN + "效果:");
        tooltip.add(TextFormatting.WHITE + "  • 熔炉: 3x速度 + 25%双倍产出");
        tooltip.add(TextFormatting.WHITE + "  • 酿造台: 3x速度");
        tooltip.add(TextFormatting.WHITE + "  • 作物: 加速生长");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "右键激活/关闭");
    }
}