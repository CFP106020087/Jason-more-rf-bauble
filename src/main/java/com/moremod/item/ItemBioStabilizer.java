package com.moremod.item;

import com.moremod.config.FleshRejectionConfig;
import com.moremod.system.FleshRejectionSystem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class ItemBioStabilizer extends Item {
    
    public ItemBioStabilizer() {
        setRegistryName("bio_stabilizer");
        setTranslationKey("bio_stabilizer");
        setMaxStackSize(16);
    }
    
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "✖ 需要裝備機械核心"
            ));
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        
        if (FleshRejectionSystem.hasTranscended(player)) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ 血肉已完全適應機械化"
            ));
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        
        // 冷却检查
        long lastUse = FleshRejectionSystem.getLastStabilizerUse(player);
        long currentTime = world.getTotalWorldTime();
        
        if (currentTime - lastUse < FleshRejectionConfig.stabilizerCooldown) {
            int remaining = (int)((FleshRejectionConfig.stabilizerCooldown - (currentTime - lastUse)) / 20);
            player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "⏱ 冷卻中... " + remaining + "秒"
            ));
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        
        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        if (rejection < FleshRejectionConfig.stabilizerMinRejection) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ 血肉狀態穩定，無需注射"
            ));
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        
        // 注射
        FleshRejectionSystem.reduceRejection(player, (float)FleshRejectionConfig.stabilizerReduction);
        FleshRejectionSystem.setLastStabilizerUse(player, currentTime);
        
        stack.shrink(1);
        
        world.playSound(null, player.getPosition(),
            SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 0.5f, 1.5f);
        
        player.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 60, 0));
        
        player.sendStatusMessage(new TextComponentString(
            TextFormatting.GREEN + "✓ 生物穩定劑注射完成 (-" + (int)FleshRejectionConfig.stabilizerReduction + ")"
        ), true);
        
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
    
    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 40;
    }
    
    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.BOW;
    }
    
    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + "抑制血肉對機械的排異反應");
        tooltip.add(TextFormatting.RED + "含有納米機器與免疫抑制劑");
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "使用：-" + (int)FleshRejectionConfig.stabilizerReduction + " 排異值");
        tooltip.add(TextFormatting.DARK_GRAY + "冷卻：" + (FleshRejectionConfig.stabilizerCooldown / 20) + "秒");
    }
}