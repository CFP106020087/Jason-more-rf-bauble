package com.moremod.item;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.entity.curse.CurseManager;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;

public class ItemHolyWater extends Item {

    public ItemHolyWater() {
        this.setMaxStackSize(16);
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
        this.setTranslationKey("holy_water");
        this.setRegistryName("holy_water");
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, EntityLivingBase entityLiving) {
        EntityPlayer player = entityLiving instanceof EntityPlayer ? (EntityPlayer) entityLiving : null;

        if (player == null) {
            return stack;
        }

        if (!worldIn.isRemote) {
            // 解除诅咒
            CurseManager.removeCurse(player);

            // 播放音效
            worldIn.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS,
                    0.5F, worldIn.rand.nextFloat() * 0.1F + 0.9F);

            // 恢复一些生命值作为额外效果
            player.heal(4.0F);
        }

        if (player != null) {
            player.addStat(StatList.getObjectUseStats(this));

            if (player instanceof EntityPlayerMP) {
                CriteriaTriggers.CONSUME_ITEM.trigger((EntityPlayerMP) player, stack);
            }

            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);

                // 返还空瓶
                if (stack.isEmpty()) {
                    return new ItemStack(Items.GLASS_BOTTLE);
                } else {
                    player.inventory.addItemStackToInventory(new ItemStack(Items.GLASS_BOTTLE));
                }
            }
        }

        return stack;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 32; // 饮用时间（单位：tick）
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.DRINK;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        playerIn.setActiveHand(handIn);
        return new ActionResult<>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 添加附魔光效
    }
}