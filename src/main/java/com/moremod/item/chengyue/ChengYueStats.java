package com.moremod.item.chengyue;

import com.moremod.capability.ChengYueCapability;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 澄月 - 属性计算（平衡白值版）
 */
public class ChengYueStats {
    
    public static double getDamage(ItemStack stack, EntityPlayer player) {
        ChengYueNBT.init(stack);
        
        int level = ChengYueNBT.getLevel(stack);
        int stage = ChengYueNBT.getStage(stack);
        
        // ✨ 修改：合理的白值成长
        double base = 6.0;  // 基础攻击（钻石剑级别）
        
        // 等级加成：对数成长，降低速度
        double levelBonus = 3.0 * Math.log(level + 1) / Math.log(2);
        
        // 阶段加成：每阶段+4攻击
        double stageBonus = (stage - 1) * 4.0;  // stage从1开始，所以-1
        
        double total = base + levelBonus + stageBonus;
        
        // 月相加成（保持不变）
        if (player != null && player.world != null) {
            float moonMult = ChengYueMoonMemory.getDamageMultiplierWithMemory(stack, player.world);
            moonMult = 1.0f + (moonMult - 1.0f) * 0.6f;
            total *= moonMult;
        }
        
        // 形态加成（保持不变）
        if (player != null && ChengYueFormManager.isUnlocked(stack)) {
            ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);
            if (cap != null) {
                int formIndex = cap.getCurrentForm();
                ChengYueMoonForm form = ChengYueMoonForm.values()[formIndex];
                total *= form.getDamageMultiplier();
            }
        }
        
        return total;
    }
    
    // ==================== 其他属性保持不变 ====================
    
    public static double getAttackSpeed(ItemStack stack, World world) {
        ChengYueNBT.init(stack);
        
        int level = ChengYueNBT.getLevel(stack);
        double speed = 1.6 + level * 0.01;
        
        if (world != null) {
            float moonBonus = ChengYueMoonPhase.getAttackSpeedBonus(world);
            speed += moonBonus;
        }
        
        return Math.min(4.0, speed);
    }
    
    public static float getCritChance(ItemStack stack, World world) {
        ChengYueNBT.init(stack);
        
        int level = ChengYueNBT.getLevel(stack);
        float critChance = 0.1f + level * 0.005f;
        
        if (world != null) {
            critChance += ChengYueMoonPhase.getCritChanceBonus(world);
        }
        
        return Math.min(0.8f, critChance);
    }
    
    public static float getCritDamage(ItemStack stack, World world) {
        ChengYueNBT.init(stack);
        
        int stage = ChengYueNBT.getStage(stack);
        float critDamage = 1.5f + stage * 0.25f;
        
        if (world != null) {
            float moonMult = ChengYueMoonPhase.getCritDamageMultiplier(world);
            critDamage *= moonMult;
        }
        
        return critDamage;
    }
    
    public static float getLifeSteal(ItemStack stack, World world) {
        ChengYueNBT.init(stack);
        
        int level = ChengYueNBT.getLevel(stack);
        float lifeSteal = 0.05f + level * 0.002f;
        
        if (world != null) {
            float moonMult = ChengYueMoonMemory.getLifeStealMultiplierWithMemory(stack, world);
            lifeSteal *= moonMult;
        }
        
        return Math.min(0.3f, lifeSteal);
    }
    
    public static float getDamageReduction(ItemStack stack, World world) {
        ChengYueNBT.init(stack);
        
        int stage = ChengYueNBT.getStage(stack);
        float reduction = stage * 0.05f;
        
        if (world != null) {
            reduction += ChengYueMoonMemory.getDamageReductionBonusWithMemory(stack, world);
        }
        
        return Math.min(0.5f, reduction);
    }
    
    public static float getDodgeChance(ItemStack stack, World world) {
        ChengYueNBT.init(stack);
        
        int level = ChengYueNBT.getLevel(stack);
        float dodge = level < 10 ? 0 : (level - 10) * 0.002f;
        
        if (world != null) {
            dodge += ChengYueMoonMemory.getDodgeChanceBonusWithMemory(stack, world);
        }
        
        return Math.min(0.3f, dodge);
    }
}