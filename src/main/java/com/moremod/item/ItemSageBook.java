package com.moremod.item;

import com.moremod.moremod;
import com.moremod.client.gui.GuiHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class ItemSageBook extends Item {

    public ItemSageBook() {
        this.setRegistryName("sage_book");
        this.setTranslationKey("moremod.sage_book");
        this.setCreativeTab(CreativeTabs.MISC);
        this.setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!world.isRemote) {
            // 打开GUI
            player.openGui(moremod.instance, GuiHandler.GUI_SAGE_BOOK, world,
                    (int)player.posX, (int)player.posY, (int)player.posZ);

            // 消耗道具
            ItemStack stack = player.getHeldItem(hand);
            stack.shrink(1);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }
}