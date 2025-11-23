package com.moremod.item.herosword;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 勇者之剑 - 成长型数值系统
 * 
 * 四大技能全部与武器成长挂钩
 */
public class HeroSwordStats {

    // ==================== 巨像杀手（血量差距增伤）====================

    /**
     * 计算巨像杀手倍率
     * 数值与等级、总击杀数挂钩
     */
    public static float getGiantSlayerMultiplier(EntityPlayer player, EntityLivingBase target, ItemStack stack) {
        if (player == null || target == null) return 1.0F;

        float playerMaxHP = player.getMaxHealth();
        float targetMaxHP = target.getMaxHealth();
        
        if (playerMaxHP <= 0.0F || targetMaxHP <= playerMaxHP) return 1.0F;

        int level = HeroSwordNBT.getLevel(stack);
        int kills = HeroSwordNBT.getTotalKills(stack);
        
        // 血量比例
        float hpRatio = targetMaxHP / playerMaxHP;
        
        // 基础加成：20% + 等级成长 + 击杀成长
        float baseBonus = 0.20F + (level * 0.008F) + (kills / 1000.0F * 0.05F);
        
        // 使用对数缩放
        float mult = 1.0F + (float)(Math.log(hpRatio) * baseBonus * 2.5F);
        
        // Boss额外加成（随等级提升）
        if (isBoss(target)) {
            float bossBonus = 1.25F + (level * 0.005F);
            mult *= bossBonus;
        }
        
        // 巨型生物加成
        if (targetMaxHP >= 200.0F) {
            mult *= 1.35F;
        }
        
        // 上限：基础2.5倍，每10级+0.2倍，最高5倍
        float maxMult = Math.min(5.0F, 2.5F + (level / 10.0F * 0.2F));
        
        if (mult > maxMult) {
            mult = maxMult;
        }

        return mult;
    }

    // ==================== 宿命重担（受击+Debuff+压血增伤）====================

    /**
     * 计算宿命重担倍率
     * 考虑：受击层数、Debuff数量、当前血量
     */
    public static float getFateBurdenMultiplier(ItemStack stack, EntityPlayer player) {
        int level = HeroSwordNBT.getLevel(stack);
        int hits = HeroSwordNBT.getHitsTaken(stack);
        
        // 基础倍率：受击层数
        float hitMult = 1.0F;
        if (hits > 0) {
            // 每层基础3%，随等级提升
            float perHit = 0.03F + (level * 0.0005F);
            hitMult = 1.0F + (Math.min(hits, 50) * perHit);
        }
        
        // Debuff加成
        float debuffMult = 1.0F;
        int debuffCount = 0;
        for (Potion potion : player.getActivePotionMap().keySet()) {
            if (potion.isBadEffect()) {
                debuffCount++;
            }
        }
        if (debuffCount > 0) {
            // 每个Debuff 5%加成，随等级提升
            float perDebuff = 0.05F + (level * 0.001F);
            debuffMult = 1.0F + (Math.min(debuffCount, 5) * perDebuff);
        }
        
        // 压血加成（血量越低伤害越高）
        float hpMult = 1.0F;
        float hpRatio = player.getHealth() / player.getMaxHealth();
        if (hpRatio < 0.5F) {
            // 50%血以下开始生效，越低越强
            float lowHpBonus = (0.5F - hpRatio) * (1.0F + level * 0.01F);
            hpMult = 1.0F + lowHpBonus;
        }
        
        // 综合计算
        float totalMult = hitMult * debuffMult * hpMult;
        
        // 上限：基础2倍，每10级+0.1倍，最高3倍
        float maxMult = Math.min(3.0F, 2.0F + (level / 10.0F * 0.1F));
        
        if (totalMult > maxMult) {
            totalMult = maxMult;
        }

        return totalMult;
    }

    // ==================== 宿命裁决（真实伤害转换）====================

    /**
     * 获取真伤转换概率
     * 与等级、击杀数挂钩
     */
    public static float getTrueDamageChance(ItemStack stack) {
        int level = HeroSwordNBT.getLevel(stack);
        int kills = HeroSwordNBT.getTotalKills(stack);
        
        // 基础15% + 等级成长 + 击杀成长
        float base = 0.15F;
        float levelBonus = level * 0.006F;
        float killBonus = (kills / 500.0F) * 0.02F;
        
        float chance = base + levelBonus + killBonus;
        
        // 上限50%
        return Math.min(0.50F, chance);
    }

    /**
     * 获取真伤转换效率
     * 返回转换比例（将多少普通伤害转为真伤）
     */
    public static float getTrueDamageConversion(ItemStack stack) {
        int level = HeroSwordNBT.getLevel(stack);
        
        // 基础50%转换 + 等级成长
        float base = 0.50F;
        float levelBonus = level * 0.005F;
        
        // 上限90%转换
        return Math.min(0.90F, base + levelBonus);
    }

    // ==================== 终局审判（主动处决）====================

    /**
     * 获取处决阈值
     * 与等级、击杀数挂钩
     */
    public static float getExecuteThreshold(ItemStack stack) {
        int level = HeroSwordNBT.getLevel(stack);
        int kills = HeroSwordNBT.getTotalKills(stack);
        
        // 基础20% + 等级成长 + 击杀成长
        float base = 0.20F;
        float levelBonus = (level / 100.0F) * 0.25F;  // 100级时+25%
        float killBonus = (kills / 2000.0F) * 0.05F;  // 2000击杀+5%
        
        // 上限50%
        return Math.min(0.50F, base + levelBonus + killBonus);
    }

    /**
     * 获取处决范围
     */
    public static float getExecuteRange(ItemStack stack) {
        int level = HeroSwordNBT.getLevel(stack);
        
        // 基础5格 + 每10级+1格
        return Math.min(12.0F, 5.0F + (level / 10.0F));
    }

    /**
     * 获取处决数量上限
     */
    public static int getMaxExecuteTargets(ItemStack stack) {
        int level = HeroSwordNBT.getLevel(stack);
        
        // 基础2个 + 每20级+1个
        return Math.min(10, 2 + (level / 20));
    }

    /**
     * 获取技能冷却
     */
    public static long getExecuteCooldown(ItemStack stack) {
        int level = HeroSwordNBT.getLevel(stack);
        
        // 基础20秒，每10级-1秒，最低5秒
        int seconds = Math.max(5, 20 - (level / 10));
        return seconds * 20L;
    }

    // ==================== 辅助方法 ====================

    /**
     * Boss判定（增强版）
     */
    public static boolean isBoss(EntityLivingBase entity) {
        if (entity == null) return false;
        
        // 原版Boss
        if (entity instanceof EntityDragon || entity instanceof EntityWither) {
            return true;
        }
        
        // 高血量判定
        if (entity.getMaxHealth() >= 100.0F) {
            return true;
        }
        
        // 名称检测
        String name = entity.getName().toLowerCase();
        return name.contains("boss") || name.contains("king") || 
               name.contains("queen") || name.contains("lord");
    }

    /**
     * 获取完整状态描述（用于调试）
     */
    public static void debugStats(EntityPlayer player, ItemStack stack) {
        if (player.world.isRemote) return;
        
        int level = HeroSwordNBT.getLevel(stack);
        int kills = HeroSwordNBT.getTotalKills(stack);
        int hits = HeroSwordNBT.getHitsTaken(stack);
        
        player.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "=== 勇者之剑状态 ===" + "\n" +
            TextFormatting.WHITE + "等级: " + level + " | 击杀: " + kills + "\n" +
            TextFormatting.RED + "巨像杀手上限: ×" + String.format("%.1f", 2.5F + (level/10.0F * 0.2F)) + "\n" +
            TextFormatting.YELLOW + "宿命重担: " + hits + "层" + "\n" +
            TextFormatting.LIGHT_PURPLE + "真伤概率: " + String.format("%.0f%%", getTrueDamageChance(stack) * 100) + "\n" +
            TextFormatting.DARK_PURPLE + "处决阈值: " + String.format("%.0f%%", getExecuteThreshold(stack) * 100)
        ));
    }
}