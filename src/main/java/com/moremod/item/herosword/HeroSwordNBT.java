package com.moremod.item.herosword;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 勇者之剑 - NBT存储（成长系统版）
 * 
 * 支持完整的成长体系
 */
public class HeroSwordNBT {

    // NBT键值
    private static final String KEY_LEVEL       = "HS_Level";
    private static final String KEY_EXP         = "HS_Exp";
    private static final String KEY_TOTAL_KILLS = "HS_TotalKills";
    private static final String KEY_BOSS_KILLS  = "HS_BossKills";
    private static final String KEY_HITS        = "HS_Hits";
    private static final String KEY_HIT_TIME    = "HS_HitTime";
    private static final String KEY_ATTACK_TIME = "HS_AttackTime";
    private static final String KEY_SKILL_CD    = "HS_SkillCD";
    private static final String KEY_TRUE_PROCS  = "HS_TrueProcs";  // 真伤触发次数统计

    /**
     * 初始化NBT
     */
    public static void init(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = stack.getTagCompound();
        if (!nbt.hasKey(KEY_LEVEL)) {
            nbt.setInteger(KEY_LEVEL, 1);
            nbt.setInteger(KEY_EXP, 0);
            nbt.setInteger(KEY_TOTAL_KILLS, 0);
            nbt.setInteger(KEY_BOSS_KILLS, 0);
            nbt.setInteger(KEY_HITS, 0);
            nbt.setLong(KEY_HIT_TIME, 0L);
            nbt.setLong(KEY_ATTACK_TIME, 0L);
            nbt.setLong(KEY_SKILL_CD, 0L);
            nbt.setInteger(KEY_TRUE_PROCS, 0);
        }
    }

    // ==================== 等级系统 ====================

    public static int getLevel(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_LEVEL);
    }

    public static int getExp(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_EXP);
    }

    /**
     * 获取升级所需经验（成长曲线）
     * 前期快速成长，后期缓慢
     */
    public static int getExpToNext(ItemStack stack) {
        int lv = getLevel(stack);
        
        if (lv >= 100) return Integer.MAX_VALUE; // 满级
        
        // 分段成长曲线
        if (lv <= 10) {
            return 50 + lv * 10;        // 1-10级：60-150
        } else if (lv <= 30) {
            return 150 + (lv-10) * 25;  // 11-30级：175-650
        } else if (lv <= 50) {
            return 650 + (lv-30) * 50;  // 31-50级：700-1650
        } else if (lv <= 70) {
            return 1650 + (lv-50) * 100; // 51-70级：1750-3650
        } else if (lv <= 90) {
            return 3650 + (lv-70) * 200; // 71-90级：3850-7650
        } else {
            return 7650 + (lv-90) * 500; // 91-100级：8150-12650
        }
    }

    /**
     * 添加经验并检查升级
     */
    public static void addKillExp(ItemStack stack, boolean isBoss) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();

        int lv = nbt.getInteger(KEY_LEVEL);
        if (lv >= 100) return; // 满级
        
        int exp = nbt.getInteger(KEY_EXP);
        int totalKills = nbt.getInteger(KEY_TOTAL_KILLS);
        int bossKills = nbt.getInteger(KEY_BOSS_KILLS);

        // 经验计算（随等级衰减，Boss不衰减）
        int gain;
        if (isBoss) {
            gain = 100 + lv * 5;  // Boss经验随等级增长
            bossKills++;
            nbt.setInteger(KEY_BOSS_KILLS, bossKills);
        } else {
            // 普通怪经验衰减
            int baseExp = 10;
            float decay = Math.max(0.1F, 1.0F - (lv / 100.0F));
            gain = (int)(baseExp * decay);
        }
        
        exp += gain;
        totalKills++;
        nbt.setInteger(KEY_TOTAL_KILLS, totalKills);

        // 检查升级
        int need = getExpToNext(stack);
        while (exp >= need && lv < 100) {
            exp -= need;
            lv++;
            nbt.setInteger(KEY_LEVEL, lv);
            
            // 升级里程碑奖励
            if (lv % 10 == 0) {
                // 每10级清空受击层数作为奖励
                nbt.setInteger(KEY_HITS, 0);
            }
            
            need = getExpToNext(stack);
        }

        nbt.setInteger(KEY_EXP, exp);
    }

    // ==================== 击杀统计 ====================

    public static int getTotalKills(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_TOTAL_KILLS);
    }

    public static int getBossKills(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_BOSS_KILLS);
    }

    // ==================== 宿命重担系统 ====================

    /**
     * 添加受击层数（最高100层）
     */
    public static void addHitTaken(ItemStack stack, int amount, long tick) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();

        int hits = nbt.getInteger(KEY_HITS);
        hits = Math.min(100, hits + amount);  // 上限100层
        
        nbt.setInteger(KEY_HITS, hits);
        nbt.setLong(KEY_HIT_TIME, tick);
    }

    public static int getHitsTaken(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_HITS);
    }

    /**
     * 渐进式衰减（改进版）
     * - 5秒保护期
     * - 5-15秒：缓慢衰减
     * - 15-30秒：快速衰减
     * - 30秒后：清零
     */
    public static void decayHitsTaken(ItemStack stack, long currentTick) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();

        int hits = nbt.getInteger(KEY_HITS);
        if (hits <= 0) return;

        long lastHit = nbt.getLong(KEY_HIT_TIME);
        if (lastHit <= 0L) return;

        long secondsSince = (currentTick - lastHit) / 20L;

        if (secondsSince < 5) {
            return; // 保护期
        } else if (secondsSince < 15) {
            // 缓慢衰减：每2秒-1层
            int decay = (int)((secondsSince - 5) / 2);
            hits = Math.max(0, hits - decay);
        } else if (secondsSince < 30) {
            // 快速衰减：每秒-2层
            int slowDecay = 5;  // 前10秒衰减了5层
            int fastDecay = (int)((secondsSince - 15) * 2);
            hits = Math.max(0, hits - slowDecay - fastDecay);
        } else {
            hits = 0; // 30秒后清零
        }

        if (hits != nbt.getInteger(KEY_HITS)) {
            nbt.setInteger(KEY_HITS, hits);
        }
    }

    // ==================== 战斗状态 ====================

    public static void markAttack(ItemStack stack, long tick) {
        init(stack);
        stack.getTagCompound().setLong(KEY_ATTACK_TIME, tick);
    }

    public static boolean isInCombat(ItemStack stack, long currentTick) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        
        long lastAttack = nbt.getLong(KEY_ATTACK_TIME);
        long lastHit = nbt.getLong(KEY_HIT_TIME);
        
        long lastCombat = Math.max(lastAttack, lastHit);
        return (currentTick - lastCombat) < 200L;  // 10秒
    }

    // ==================== 技能冷却 ====================

    public static long getSkillCooldown(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getLong(KEY_SKILL_CD);
    }

    public static void setSkillCooldown(ItemStack stack, long tick) {
        init(stack);
        stack.getTagCompound().setLong(KEY_SKILL_CD, tick);
    }

    // ==================== 真伤统计 ====================

    public static void addTrueDamageProc(ItemStack stack) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        int procs = nbt.getInteger(KEY_TRUE_PROCS);
        nbt.setInteger(KEY_TRUE_PROCS, procs + 1);
    }

    public static int getTrueDamageProcs(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_TRUE_PROCS);
    }

    // ==================== 成长阶段判定 ====================

    /**
     * 获取当前成长阶段称号
     */
    public static String getGrowthTitle(ItemStack stack) {
        int level = getLevel(stack);
        int kills = getTotalKills(stack);
        int bossKills = getBossKills(stack);
        
        // 特殊称号
        if (level >= 100) {
            if (bossKills >= 100) return "终焉之主";
            if (kills >= 10000) return "万灵审判者";
            return "永恒勇者";
        }
        
        // 常规称号
        if (level >= 80) return "传说勇者";
        if (level >= 60) return "史诗勇者";
        if (level >= 40) return "精锐勇者";
        if (level >= 20) return "老练勇者";
        if (level >= 10) return "新锐勇者";
        return "见习勇者";
    }
}