package com.moremod.item.chengyue;

import com.moremod.capability.ChengYueCapability;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 月华系统 - 使用Capability
 */
public class ChengYueLunarPower {
    
    public static final int DEFAULT_MAX = 100;
    
    public static boolean isUnlocked(ItemStack stack) {
        return ChengYueNBT.getLevel(stack) >= 35;
    }
    
    public static void onHit(ChengYueCapability cap) {
        cap.addLunarPower(2);
    }
    
    public static void onKill(ChengYueCapability cap, boolean isBoss) {
        cap.addLunarPower(isBoss ? 20 : 10);
    }
    
    public static void tickRegen(ItemStack stack, ChengYueCapability cap, EntityPlayer player) {
        World world = player.world;
        int phase = ChengYueMoonPhase.getCurrentPhase(world);
        
        if (phase == 0 && !world.isDaytime()) {
            cap.addLunarPower(5);
        } else {
            cap.addLunarPower(1);
        }
    }
}