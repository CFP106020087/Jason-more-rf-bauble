package com.moremod.item;

import com.moremod.client.gui.GuiHandler;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
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
import java.util.List;

/**
 * MoreMod 綜合指南書
 * 整合所有系統的說明文檔
 */
public class ItemModGuide extends Item {

    public ItemModGuide() {
        setTranslationKey("moremod_guide");
        setRegistryName("moremod_guide");
        setMaxStackSize(1);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (world.isRemote) {
            openGuiClient(player);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @SideOnly(Side.CLIENT)
    private void openGuiClient(EntityPlayer player) {
        net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
            new com.moremod.gui.GuiModGuide(player)
        );
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "MoreMod 綜合指南");
        tooltip.add(TextFormatting.GRAY + "包含所有系統的說明文檔");
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "右鍵打開指南");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
