package com.moremod.item;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.upgrades.WetnessSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.SoundEvents;

import java.util.List;

/**
 * 毛巾 - 用於降低潮濕值
 */
public class ItemTowel extends Item {
    
    private static final int DRY_AMOUNT = 30; // 每次使用減少30%潮濕度
    private static final int MAX_USES = 5;    // 最大使用次數
    
    public ItemTowel() {
        setRegistryName("towel");
        setTranslationKey("towel");
        setMaxStackSize(1);
        setMaxDamage(MAX_USES);
        setNoRepair();
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }
    
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        
        if (!world.isRemote) {
            // 檢查是否可以使用
            if (stack.getItemDamage() >= MAX_USES - 1) {
                player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "毛巾已經太髒了，無法使用！"
                ), true);
                return new ActionResult<>(EnumActionResult.FAIL, stack);
            }
            
            // 使用毛巾
            if (WetnessSystem.useTowel(player, DRY_AMOUNT)) {
                // 增加損壞值
                stack.damageItem(1, player);
                
                // 播放音效
                world.playSound(null, player.getPosition(), 
                    SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
                
                // 如果毛巾用完了
                if (stack.getItemDamage() >= MAX_USES - 1) {
                    player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "毛巾已經完全濕透了"
                    ));
                }
                
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
        }
        
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }
    
    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.AQUA + "用於擦乾機械核心的潮濕");
        tooltip.add("");
        
        int remainingUses = MAX_USES - stack.getItemDamage();
        TextFormatting color;
        if (remainingUses > 3) {
            color = TextFormatting.GREEN;
        } else if (remainingUses > 1) {
            color = TextFormatting.YELLOW;
        } else {
            color = TextFormatting.RED;
        }
        
        tooltip.add(color + "剩餘使用次數: " + remainingUses + "/" + MAX_USES);
        tooltip.add(TextFormatting.GRAY + "每次使用減少 " + DRY_AMOUNT + "% 潮濕度");
        
        if (remainingUses <= 0) {
            tooltip.add(TextFormatting.DARK_RED + "已完全濕透，無法使用");
        }
        
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "不能在雨中使用");
        tooltip.add(TextFormatting.DARK_GRAY + "用羊毛製作");
    }
    
    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return stack.getItemDamage() > 0;
    }
}