package com.moremod.item.shambhala;

import baubles.api.BaubleType;
import com.moremod.config.ShambhalaConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.ShambhalaHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 香巴拉_业刺 (Shambhala Karma Thorns) - 业力放大
 *
 * 能力：
 * - 装备时提供反伤倍率加成（默认×5）
 * - 未装备时反伤倍率为×1.0
 */
public class ItemShambhalaThorns extends ItemShambhalaBaubleBase {

    public ItemShambhalaThorns() {
        setRegistryName("shambhala_thorns");
        setTranslationKey("shambhala_thorns");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.RING;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        // 反伤逻辑在 ShambhalaHandler.reflectDamage 中处理
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        // 反伤逻辑在事件处理器中
    }

    /**
     * 获取反伤倍率
     */
    public static float getReflectMultiplier() {
        return (float) ShambhalaConfig.thornsReflectMultiplier;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.AQUA + "" + TextFormatting.BOLD + "香巴拉_业刺");
        tooltip.add(TextFormatting.DARK_GRAY + "Shambhala Karma Thorns");
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "◆ 业力放大");
        tooltip.add(TextFormatting.GRAY + "  反伤倍率: ×" + String.format("%.1f", ShambhalaConfig.thornsReflectMultiplier));
        tooltip.add(TextFormatting.DARK_GRAY + "  （未装备时反伤倍率为×1.0）");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"承受的痛苦，终将返还\"");
        tooltip.add(TextFormatting.GOLD + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
