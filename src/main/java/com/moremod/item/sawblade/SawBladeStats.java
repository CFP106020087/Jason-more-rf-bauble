package com.moremod.item.sawblade;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/**
 * 锯刃剑 - 成长型数值系统
 * 
 * 5大技能：
 * 1. 出血猎手（被动核心）
 * 2. 撕裂打击（多段伤害）
 * 3. 猎杀本能（低血量+背刺增伤）
 * 4. 鲜血欢愉（buff增伤）
 * 5. 处决收割（主动技能）
 */
public class SawBladeStats {

    // ==================== 基础属性成长 ====================
    
    /**
     * 获取基础攻击力
     * 随等级成长：8.0 → 16.0
     */
    public static float getBaseDamage(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        float base = 8.0f;  // 初始攻击力
        float growth = level * 0.08f;  // 每级+0.08
        
        return Math.min(16.0f, base + growth);
    }
    
    /**
     * 获取攻击速度
     * 随等级提升：-2.4 → -2.0
     */
    public static float getAttackSpeed(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        float base = -2.4f;
        float growth = level * 0.004f;  // 每级+0.004
        
        return Math.max(-2.0f, base + growth);
    }
    
    // ==================== 技能1：出血猎手 ====================
    
    /**
     * 基础出血累积值
     * 随等级提升：10.0 → 20.0
     */
    public static float getBaseBleedBuildUp(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        int bleedKills = SawBladeNBT.getBleedKills(stack);
        
        float base = 10.0f;
        float levelBonus = level * 0.1f;
        float killBonus = (bleedKills / 100.0f) * 0.5f;
        
        return Math.min(20.0f, base + levelBonus + killBonus);
    }
    
    /**
     * 暴击出血倍率
     * 随等级提升：2.0 → 3.0
     */
    public static float getCritBleedMultiplier(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        float base = 2.0f;
        float growth = level * 0.01f;
        
        return Math.min(3.0f, base + growth);
    }
    
    /**
     * 出血爆裂伤害百分比
     * 随等级提升：10% → 30%
     */
    public static float getBleedBurstDamagePercent(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        int bleedKills = SawBladeNBT.getBleedKills(stack);
        
        float base = 0.10f;
        float levelBonus = level * 0.002f;
        float killBonus = (bleedKills / 500.0f) * 0.01f;
        
        return Math.min(0.30f, base + levelBonus + killBonus);
    }
    
    /**
     * 出血衰减速率
     * 随等级降低：10.0/秒 → 5.0/秒
     */
    public static float getBleedDecayRate(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        float base = 10.0f;
        float reduction = level * 0.05f;
        
        return Math.max(5.0f, base - reduction);
    }
    
    /**
     * 出血持续时间加成
     * Boss额外加成
     */
    public static float getBleedDurationMultiplier(EntityLivingBase target) {
        if (isBoss(target)) {
            return 1.5f;  // Boss出血持续更久
        }
        return 1.0f;
    }
    
    // ==================== 技能2：撕裂打击 ====================
    
    /**
     * 撕裂段数
     * 随等级提升：2 → 5
     */
    public static int getLacerationHits(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        int base = 2;
        int growth = level / 20;  // 每20级+1段
        
        return Math.min(5, base + growth);
    }
    
    /**
     * 每段撕裂伤害（基于基础伤害的百分比）
     * 随等级提升：20% → 35%
     */
    public static float getLacerationDamagePercent(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        float base = 0.20f;
        float growth = level * 0.0015f;
        
        return Math.min(0.35f, base + growth);
    }
    
    /**
     * 撕裂间隔（tick）
     * 随等级缩短：5 → 2
     */
    public static int getLacerationInterval(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        int base = 5;
        int reduction = level / 30;
        
        return Math.max(2, base - reduction);
    }
    
    // ==================== 技能3：猎杀本能 ====================
    
    /**
     * 对低血量目标增伤
     * <50%: 1.2x → 1.8x
     * <30%: 1.5x → 2.5x
     * <10%: 2.0x → 3.5x
     */
    public static float getHunterInstinctMultiplier(ItemStack stack, EntityLivingBase target) {
        if (target == null) return 1.0f;
        
        int level = SawBladeNBT.getLevel(stack);
        float hpRatio = target.getHealth() / target.getMaxHealth();
        
        // 基础倍率（随等级提升）
        float levelFactor = 1.0f + (level * 0.006f);
        
        if (hpRatio < 0.10f) {
            // <10% 血量
            return (2.0f + level * 0.015f) * levelFactor;  // 2.0x → 3.5x
        } else if (hpRatio < 0.30f) {
            // <30% 血量
            return (1.5f + level * 0.01f) * levelFactor;   // 1.5x → 2.5x
        } else if (hpRatio < 0.50f) {
            // <50% 血量
            return (1.2f + level * 0.006f) * levelFactor;  // 1.2x → 1.8x
        }
        
        return 1.0f;
    }
    
    /**
     * 背刺增伤（基于角度）
     * 正面(0°): 1.0x
     * 侧面(90°): 1.3x → 1.8x
     * 背面(180°): 2.0x → 3.5x
     */
    public static float getBackstabMultiplier(ItemStack stack, EntityPlayer attacker, EntityLivingBase target) {
        if (attacker == null || target == null) return 1.0f;
        
        int level = SawBladeNBT.getLevel(stack);
        int backstabKills = SawBladeNBT.getBackstabKills(stack);
        
        // 计算攻击角度
        float angle = getAttackAngle(attacker, target);
        
        // 基础倍率（随等级和背刺击杀提升）
        float levelFactor = 1.0f + (level * 0.015f);
        float killFactor = 1.0f + (backstabKills / 200.0f * 0.1f);
        
        // 角度插值
        if (angle < 45.0f) {
            // 正面：无加成
            return 1.0f;
        } else if (angle < 135.0f) {
            // 侧面：线性插值 1.3x → 1.8x
            float sideBase = 1.3f + (level * 0.005f);
            float t = (angle - 45.0f) / 90.0f;
            return sideBase + (t * 0.5f) * levelFactor * killFactor;
        } else {
            // 背面：2.0x → 3.5x
            float backBase = 2.0f + (level * 0.015f);
            float t = (angle - 135.0f) / 45.0f;
            return (backBase + t * 0.5f) * levelFactor * killFactor;
        }
    }
    
    /**
     * 计算攻击角度（0-180度）
     * 0° = 正面，180° = 背面
     */
    private static float getAttackAngle(EntityPlayer attacker, EntityLivingBase target) {
        // 目标朝向
        float targetYaw = target.rotationYaw;
        
        // 攻击者相对目标的角度
        double dx = attacker.posX - target.posX;
        double dz = attacker.posZ - target.posZ;
        float attackerAngle = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
        
        // 计算角度差
        float angleDiff = MathHelper.wrapDegrees(attackerAngle - targetYaw);
        
        // 转换为0-180度（绝对值）
        return Math.abs(angleDiff);
    }
    
    /**
     * 判断是否为背刺（角度>135°）
     */
    public static boolean isBackstab(EntityPlayer attacker, EntityLivingBase target) {
        return getAttackAngle(attacker, target) > 135.0f;
    }
    
    // ==================== 技能4：鲜血欢愉 ====================
    
    /**
     * 鲜血欢愉增伤（每层）
     * 随等级提升：15%/层 → 25%/层
     */
    public static float getBloodEuphoriaPerStack(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        float base = 0.15f;
        float growth = level * 0.001f;
        
        return Math.min(0.25f, base + growth);
    }
    
    /**
     * 鲜血欢愉最大层数
     * 随等级提升：1 → 3
     */
    public static int getBloodEuphoriaMaxStacks(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        if (level >= 50) return 3;
        if (level >= 25) return 2;
        return 1;
    }
    
    /**
     * 鲜血欢愉持续时间（秒）
     * 随等级提升：5秒 → 10秒
     */
    public static int getBloodEuphoriaDuration(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        int base = 5;
        int growth = level / 20;  // 每20级+1秒
        
        return Math.min(10, base + growth);
    }
    
    /**
     * 鲜血欢愉攻速加成
     * 随等级提升：20% → 40%
     */
    public static float getBloodEuphoriaAttackSpeed(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        float base = 0.20f;
        float growth = level * 0.002f;
        
        return Math.min(0.40f, base + growth);
    }
    
    /**
     * 鲜血欢愉生命偷取
     * 随等级解锁：Lv30+
     */
    public static float getBloodEuphoriaLifeSteal(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        if (level < 30) return 0.0f;
        
        float base = 0.10f;  // 10%生命偷取
        float growth = (level - 30) * 0.002f;
        
        return Math.min(0.20f, base + growth);
    }
    
    // ==================== 技能5：处决收割 ====================
    
    /**
     * 处决阈值
     * 随等级提升：25% → 40%
     */
    public static float getExecuteThreshold(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        int executeCount = SawBladeNBT.getExecuteCount(stack);
        
        float base = 0.25f;
        float levelBonus = level * 0.0015f;
        float executeBonus = (executeCount / 100.0f) * 0.01f;
        
        return Math.min(0.40f, base + levelBonus + executeBonus);
    }
    
    /**
     * 处决范围
     * 随等级提升：6格 → 12格
     */
    public static float getExecuteRange(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        float base = 6.0f;
        float growth = level * 0.06f;
        
        return Math.min(12.0f, base + growth);
    }
    
    /**
     * 处决目标数量
     * 随等级提升：3 → 8
     */
    public static int getExecuteMaxTargets(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        int base = 3;
        int growth = level / 20;
        
        return Math.min(8, base + growth);
    }
    
    /**
     * 处决冷却（秒）
     * 随等级降低：30秒 → 15秒
     */
    public static long getExecuteCooldown(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        int base = 30;
        int reduction = level / 5;
        int seconds = Math.max(15, base - reduction);
        
        return seconds * 20L;
    }
    
    /**
     * 连击延长时间（每击杀）
     * 随等级提升：1秒 → 3秒
     */
    public static int getExecuteChainExtension(ItemStack stack) {
        int level = SawBladeNBT.getLevel(stack);
        
        if (level < 40) return 1;
        if (level < 70) return 2;
        return 3;
    }
    
    // ==================== 辅助判定 ====================
    
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
        if (entity.getMaxHealth() >= 100.0f) {
            return true;
        }
        
        // 名称检测
        String name = entity.getName().toLowerCase();
        return name.contains("boss") || name.contains("king") || 
               name.contains("queen") || name.contains("lord") ||
               name.contains("dragon") || name.contains("wither");
    }
    
    /**
     * 获取综合伤害倍率
     * 整合所有技能加成
     */
    public static float getTotalDamageMultiplier(ItemStack stack, EntityPlayer attacker, 
                                                 EntityLivingBase target, int euphoriaStacks) {
        float mult = 1.0f;
        
        // 猎杀本能：低血量加成
        mult *= getHunterInstinctMultiplier(stack, target);
        
        // 背刺加成
        mult *= getBackstabMultiplier(stack, attacker, target);
        
        // 鲜血欢愉加成
        if (euphoriaStacks > 0) {
            float perStack = getBloodEuphoriaPerStack(stack);
            mult *= (1.0f + perStack * euphoriaStacks);
        }
        
        // Boss额外加成（5%）
        if (isBoss(target)) {
            mult *= 1.05f;
        }
        
        return mult;
    }
}
