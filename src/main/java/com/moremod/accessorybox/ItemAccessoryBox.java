package com.moremod.accessorybox;

import com.moremod.config.MoreModConfig;
import com.moremod.client.gui.GuiHandler;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.moremod;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemAccessoryBox extends Item {

    public ItemAccessoryBox() {
        this.setTranslationKey("accessory_box_t3");
        this.setRegistryName("accessory_box_t3");
        this.setMaxStackSize(1);
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);

        if (!worldIn.isRemote && handIn == EnumHand.MAIN_HAND) {

            // 标记使用箱子
            playerIn.getEntityData().setBoolean("UsingAccessoryBox", true);

            // 打开GUI
            playerIn.openGui(moremod.INSTANCE, 1,
                    worldIn, 0, 0, 0);

            // 播放音效
            worldIn.playSound(null, playerIn.posX, playerIn.posY, playerIn.posZ,
                    SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.PLAYERS,
                    0.5F, worldIn.rand.nextFloat() * 0.1F + 0.9F);
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, itemstack);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.LIGHT_PURPLE + "從盒子的鎖眼中看去，隱約能見到平行世界中配戴不同飾品套裝的你");
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "讓你配戴:");
        tooltip.add(TextFormatting.YELLOW + " • 額外1個項鍊");
        tooltip.add(TextFormatting.YELLOW + " • 額外2個戒指");
        tooltip.add(TextFormatting.YELLOW + " • 額外1個腰帶");
        tooltip.add(TextFormatting.YELLOW + " • 額外1個頭部");
        tooltip.add(TextFormatting.YELLOW + " • 額外1個身體");
        tooltip.add(TextFormatting.YELLOW + " • 額外1個護符");
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "必須時刻觀測才能拓展!");
    }
}
