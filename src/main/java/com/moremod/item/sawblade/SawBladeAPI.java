package com.moremod.item.sawblade;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * 锯刃剑系统 - 对外API
 * 
 * 提供简单的接口供外部调用
 */
public class SawBladeAPI {
    
    // ==================== 出血系统 ====================
    
    public static float getBleed(EntityLivingBase entity) {
        if (entity == null) return 0.0f;
        return entity.getEntityData().getFloat("moremod_bleed_buildup");
    }
    
    public static void setBleed(EntityLivingBase entity, float value) {
        if (entity == null) return;
        entity.getEntityData().setFloat("moremod_bleed_buildup", Math.max(0, Math.min(100, value)));
    }
    
    public static void addBleed(EntityLivingBase entity, float amount) {
        setBleed(entity, getBleed(entity) + amount);
    }
    
    public static void clearBleed(EntityLivingBase entity) {
        if (entity == null) return;
        entity.getEntityData().removeTag("moremod_bleed_buildup");
        entity.getEntityData().removeTag("moremod_bleed_decay");
    }
    
    public static boolean hasBleed(EntityLivingBase entity) {
        return getBleed(entity) > 0;
    }
    
    // ==================== 成长系统 ====================
    
    public static int getLevel(ItemStack stack) {
        return SawBladeNBT.getLevel(stack);
    }
    
    public static int getExp(ItemStack stack) {
        return SawBladeNBT.getExp(stack);
    }
    
    public static int getExpToNext(ItemStack stack) {
        return SawBladeNBT.getExpToNext(stack);
    }
    
    public static boolean addExp(ItemStack stack, int amount) {
        return SawBladeNBT.addExp(stack, amount, false, false, false);
    }
    
    // ==================== 统计数据 ====================
    
    public static int getTotalKills(ItemStack stack) {
        return SawBladeNBT.getTotalKills(stack);
    }
    
    public static int getBossKills(ItemStack stack) {
        return SawBladeNBT.getBossKills(stack);
    }
    
    public static int getBleedKills(ItemStack stack) {
        return SawBladeNBT.getBleedKills(stack);
    }
    
    public static int getBackstabKills(ItemStack stack) {
        return SawBladeNBT.getBackstabKills(stack);
    }
    
    // ==================== 技能数值 ====================
    
    public static float getBaseDamage(ItemStack stack) {
        return SawBladeStats.getBaseDamage(stack);
    }
    
    public static float getAttackSpeed(ItemStack stack) {
        return SawBladeStats.getAttackSpeed(stack);
    }
    
    public static float getExecuteThreshold(ItemStack stack) {
        return SawBladeStats.getExecuteThreshold(stack);
    }
    
    // ==================== 主动技能 ====================
    
    public static boolean castExecute(EntityPlayer player, ItemStack stack) {
        return SawBladeActiveSkill.tryCast(player.world, player, stack);
    }
    
    // ==================== 调试 ====================
    
    public static String getDebugInfo(ItemStack stack) {
        int lv = SawBladeNBT.getLevel(stack);
        int kills = SawBladeNBT.getTotalKills(stack);
        int exp = SawBladeNBT.getExp(stack);
        int expNext = SawBladeNBT.getExpToNext(stack);
        
        return String.format(
            "Lv.%d [%d/%d] | Kills:%d | Damage:%.1f | Speed:%.2f",
            lv, exp, expNext, kills,
            SawBladeStats.getBaseDamage(stack),
            SawBladeStats.getAttackSpeed(stack)
        );
    }
}
