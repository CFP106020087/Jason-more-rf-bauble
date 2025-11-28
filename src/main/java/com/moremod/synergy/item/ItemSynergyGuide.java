package com.moremod.synergy.item;

import com.moremod.client.gui.GuiHandler;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.moremod;
import com.moremod.synergy.core.SynergyManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 协同效应手册 - Synergy Guide
 *
 * 一本记录所有协同效应组合的手册。
 * 右键打开 GUI 查看所有可用的 Synergy 组合。
 */
public class ItemSynergyGuide extends Item {

    public ItemSynergyGuide() {
        this.setRegistryName("synergy_guide");
        this.setTranslationKey("moremod.synergy_guide");
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
        this.setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        // 在客户端打开GUI
        if (world.isRemote) {
            player.openGui(moremod.instance, GuiHandler.SYNERGY_GUIDE_GUI, world, 0, 0, 0);
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.LIGHT_PURPLE + "『协同效应完全指南』");
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "记录了所有已知的模块协同组合。");
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "右键" + TextFormatting.WHITE + " 打开协同手册");
        tooltip.add("");

        // 显示统计
        SynergyManager manager = SynergyManager.getInstance();
        if (manager != null) {
            int total = manager.getAllSynergies().size();
            tooltip.add(TextFormatting.DARK_GRAY + "共计 " + total + " 种协同效应");
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 附魔光效
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return TextFormatting.LIGHT_PURPLE + "" + TextFormatting.BOLD +
               super.getItemStackDisplayName(stack);
    }
}
