package com.moremod.item.chengyue;

import com.moremod.capability.ChengYueCapability;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * 澄月 - 形态管理器
 */
public class ChengYueFormManager {
    
    public static final int UNLOCK_AUTO_LEVEL = 35;
    public static final int UNLOCK_MANUAL_LEVEL = 40;
    
    public static final int SWITCH_COST = 20;
    public static final long SWITCH_COOLDOWN = 600;
    public static final long MANUAL_DURATION = 1200;
    
    public static boolean isUnlocked(ItemStack stack) {
        return ChengYueNBT.getLevel(stack) >= UNLOCK_AUTO_LEVEL;
    }
    
    public static boolean canManualSwitch(ItemStack stack) {
        return ChengYueNBT.getLevel(stack) >= UNLOCK_MANUAL_LEVEL;
    }
    
    public static ChengYueMoonForm getCurrentForm(EntityPlayer player) {
        ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);
        if (cap == null) {
            return ChengYueMoonForm.FULL_MOON;
        }
        
        int formIndex = cap.getCurrentForm();
        return ChengYueMoonForm.values()[formIndex];
    }
    
    public static void updateAutoForm(ItemStack stack, EntityPlayer player, World world) {
        if (!isUnlocked(stack) || player.world.isRemote) return;
        
        ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);
        if (cap == null) return;
        
        long currentTime = System.currentTimeMillis();
        long lastSwitch = cap.getFormSwitchTime();
        if (currentTime - lastSwitch < MANUAL_DURATION * 50) {
            return;
        }
        
        int rememberedPhase = ChengYueMoonMemory.getRememberedPhase(stack);
        if (rememberedPhase == -1) {
            rememberedPhase = ChengYueMoonPhase.getCurrentPhase(world);
        }
        
        ChengYueMoonForm targetForm = ChengYueMoonForm.getFormByMoonPhase(rememberedPhase);
        ChengYueMoonForm currentForm = getCurrentForm(player);
        
        if (targetForm != currentForm) {
            cap.setCurrentForm(targetForm.ordinal());
        }
    }
    
    public static boolean switchFormManual(ItemStack stack, EntityPlayer player, ChengYueMoonForm targetForm) {
        if (!canManualSwitch(stack) || player.world.isRemote) {
            return false;
        }
        
        ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);
        if (cap == null) return false;
        
        ChengYueMoonForm currentForm = getCurrentForm(player);
        if (currentForm == targetForm) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "已是该形态！"
            ));
            return false;
        }
        
        if (!cap.consumeLunarPower(SWITCH_COST)) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "月华不足！需要 " + SWITCH_COST + " 点"
            ));
            return false;
        }
        
        cap.setCurrentForm(targetForm.ordinal());
        
        player.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "【形态切换】" +
            targetForm.getFullDisplayName() +
            TextFormatting.GRAY + " (60秒)"
        ));
        
        return true;
    }
}