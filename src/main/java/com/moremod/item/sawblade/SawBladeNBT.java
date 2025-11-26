package com.moremod.item.sawblade;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 锯刃剑 - 成长系统数据管理
 * 
 * 对标勇者之剑的复杂度
 * 存储：等级、经验、击杀统计、技能数据
 */
public class SawBladeNBT {

    // ==================== NBT键名 ====================
    
    // 成长系统
    private static final String KEY_LEVEL = "SB_Level";
    private static final String KEY_EXP = "SB_Exp";
    private static final String KEY_TOTAL_KILLS = "SB_TotalKills";
    private static final String KEY_BOSS_KILLS = "SB_BossKills";
    private static final String KEY_BLEED_KILLS = "SB_BleedKills";        // 出血击杀数
    private static final String KEY_BACKSTAB_KILLS = "SB_BackstabKills";  // 背刺击杀数
    
    // 战斗统计
    private static final String KEY_TOTAL_DAMAGE = "SB_TotalDamage";      // 总伤害
    private static final String KEY_BLEED_PROCS = "SB_BleedProcs";        // 出血触发次数
    private static final String KEY_EXECUTE_COUNT = "SB_ExecuteCount";    // 处决次数
    
    // 技能冷却
    private static final String KEY_SKILL_CD = "SB_SkillCD";              // 主动技能冷却
    
    // 战斗状态
    private static final String KEY_LAST_ATTACK = "SB_LastAttack";        // 上次攻击时间
    private static final String KEY_COMBO_COUNT = "SB_ComboCount";        // 连击数
    private static final String KEY_COMBO_TIME = "SB_ComboTime";          // 连击时间
    
    // ==================== 初始化 ====================
    
    /**
     * 初始化NBT（首次获得时）
     */
    public static void init(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = stack.getTagCompound();
        if (!nbt.hasKey(KEY_LEVEL)) {
            // 初始化所有数据
            nbt.setInteger(KEY_LEVEL, 1);
            nbt.setInteger(KEY_EXP, 0);
            nbt.setInteger(KEY_TOTAL_KILLS, 0);
            nbt.setInteger(KEY_BOSS_KILLS, 0);
            nbt.setInteger(KEY_BLEED_KILLS, 0);
            nbt.setInteger(KEY_BACKSTAB_KILLS, 0);
            nbt.setFloat(KEY_TOTAL_DAMAGE, 0.0f);
            nbt.setInteger(KEY_BLEED_PROCS, 0);
            nbt.setInteger(KEY_EXECUTE_COUNT, 0);
            nbt.setLong(KEY_SKILL_CD, 0L);
            nbt.setLong(KEY_LAST_ATTACK, 0L);
            nbt.setInteger(KEY_COMBO_COUNT, 0);
            nbt.setLong(KEY_COMBO_TIME, 0L);
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
     * 参考勇者之剑但稍微快一些（因为锯刃剑需要累积出血）
     */
    public static int getExpToNext(ItemStack stack) {
        int lv = getLevel(stack);
        
        if (lv >= 100) return Integer.MAX_VALUE;
        
        // 分段成长曲线（比勇者之剑略快）
        if (lv <= 10) {
            return 40 + lv * 8;         // 1-10级：48-120
        } else if (lv <= 30) {
            return 120 + (lv-10) * 20;  // 11-30级：140-520
        } else if (lv <= 50) {
            return 520 + (lv-30) * 40;  // 31-50级：560-1320
        } else if (lv <= 70) {
            return 1320 + (lv-50) * 80; // 51-70级：1400-2920
        } else if (lv <= 90) {
            return 2920 + (lv-70) * 150; // 71-90级：3070-5920
        } else {
            return 5920 + (lv-90) * 400; // 91-100级：6320-10320
        }
    }
    
    /**
     * 添加经验并检查升级
     */
    public static boolean addExp(ItemStack stack, int amount, boolean isBoss, boolean isBleed, boolean isBackstab) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        
        int lv = nbt.getInteger(KEY_LEVEL);
        if (lv >= 100) return false;
        
        int exp = nbt.getInteger(KEY_EXP);
        
        // 经验加成
        float multiplier = 1.0f;
        if (isBoss) multiplier *= 3.0f;      // Boss 3倍
        if (isBleed) multiplier *= 1.5f;     // 出血击杀 1.5倍
        if (isBackstab) multiplier *= 1.3f;  // 背刺击杀 1.3倍
        
        int gain = (int)(amount * multiplier);
        exp += gain;
        
        // 检查升级
        boolean leveledUp = false;
        int need = getExpToNext(stack);
        while (exp >= need && lv < 100) {
            exp -= need;
            lv++;
            nbt.setInteger(KEY_LEVEL, lv);
            leveledUp = true;
            
            // 升级里程碑奖励
            if (lv % 10 == 0) {
                // 每10级重置技能冷却
                nbt.setLong(KEY_SKILL_CD, 0L);
            }
            
            need = getExpToNext(stack);
        }
        
        nbt.setInteger(KEY_EXP, exp);
        return leveledUp;
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
    
    public static int getBleedKills(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_BLEED_KILLS);
    }
    
    public static int getBackstabKills(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_BACKSTAB_KILLS);
    }
    
    /**
     * 添加击杀（带分类统计）
     */
    public static void addKill(ItemStack stack, boolean isBoss, boolean isBleed, boolean isBackstab) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        
        int total = nbt.getInteger(KEY_TOTAL_KILLS) + 1;
        nbt.setInteger(KEY_TOTAL_KILLS, total);
        
        if (isBoss) {
            int bossKills = nbt.getInteger(KEY_BOSS_KILLS) + 1;
            nbt.setInteger(KEY_BOSS_KILLS, bossKills);
        }
        
        if (isBleed) {
            int bleedKills = nbt.getInteger(KEY_BLEED_KILLS) + 1;
            nbt.setInteger(KEY_BLEED_KILLS, bleedKills);
        }
        
        if (isBackstab) {
            int backstabKills = nbt.getInteger(KEY_BACKSTAB_KILLS) + 1;
            nbt.setInteger(KEY_BACKSTAB_KILLS, backstabKills);
        }
        
        // 基础经验：10点
        addExp(stack, 10, isBoss, isBleed, isBackstab);
    }
    
    // ==================== 战斗统计 ====================
    
    public static float getTotalDamage(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getFloat(KEY_TOTAL_DAMAGE);
    }
    
    public static void addDamage(ItemStack stack, float amount) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        float total = nbt.getFloat(KEY_TOTAL_DAMAGE) + amount;
        nbt.setFloat(KEY_TOTAL_DAMAGE, total);
    }
    
    public static int getBleedProcs(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_BLEED_PROCS);
    }
    
    public static void addBleedProc(ItemStack stack) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        int procs = nbt.getInteger(KEY_BLEED_PROCS) + 1;
        nbt.setInteger(KEY_BLEED_PROCS, procs);
    }
    
    public static int getExecuteCount(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_EXECUTE_COUNT);
    }
    
    public static void addExecute(ItemStack stack) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        int count = nbt.getInteger(KEY_EXECUTE_COUNT) + 1;
        nbt.setInteger(KEY_EXECUTE_COUNT, count);
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
    
    // ==================== 连击系统 ====================
    
    public static int getComboCount(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_COMBO_COUNT);
    }
    
    public static long getComboTime(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getLong(KEY_COMBO_TIME);
    }
    
    /**
     * 添加连击（3秒内连续攻击）
     */
    public static int addCombo(ItemStack stack, long currentTick) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        
        long lastCombo = nbt.getLong(KEY_COMBO_TIME);
        int combo = nbt.getInteger(KEY_COMBO_COUNT);
        
        // 3秒内算连击
        if (currentTick - lastCombo < 60L) {
            combo++;
        } else {
            combo = 1;
        }
        
        nbt.setInteger(KEY_COMBO_COUNT, combo);
        nbt.setLong(KEY_COMBO_TIME, currentTick);
        
        return combo;
    }
    
    /**
     * 重置连击
     */
    public static void resetCombo(ItemStack stack) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        nbt.setInteger(KEY_COMBO_COUNT, 0);
        nbt.setLong(KEY_COMBO_TIME, 0L);
    }
    
    // ==================== 战斗状态 ====================
    
    public static void markAttack(ItemStack stack, long tick) {
        init(stack);
        stack.getTagCompound().setLong(KEY_LAST_ATTACK, tick);
    }
    
    public static boolean isInCombat(ItemStack stack, long currentTick) {
        init(stack);
        long lastAttack = stack.getTagCompound().getLong(KEY_LAST_ATTACK);
        return (currentTick - lastAttack) < 200L;  // 10秒
    }
    
    // ==================== 成长阶段判定 ====================
    
    /**
     * 获取当前成长阶段称号
     */
    public static String getGrowthTitle(ItemStack stack) {
        int level = getLevel(stack);
        int bleedKills = getBleedKills(stack);
        int backstabKills = getBackstabKills(stack);
        
        // 特殊称号
        if (level >= 100) {
            if (bleedKills >= 1000) return "血神";
            if (backstabKills >= 500) return "暗影主宰";
            return "终极猎杀者";
        }
        
        // 常规称号
        if (level >= 80) return "传说猎手";
        if (level >= 60) return "大师猎手";
        if (level >= 40) return "精英猎手";
        if (level >= 20) return "老练猎手";
        if (level >= 10) return "熟练猎手";
        return "新手猎手";
    }
    
    /**
     * 获取武器描述（Lore）
     */
    public static String getWeaponLore(ItemStack stack) {
        int level = getLevel(stack);
        int kills = getTotalKills(stack);
        
        if (level >= 100 && kills >= 10000) {
            return "染血千里，寸草不生";
        } else if (level >= 80) {
            return "猎杀的艺术在于精准与耐心";
        } else if (level >= 50) {
            return "每一次撕裂都是一首死亡之歌";
        } else if (level >= 30) {
            return "锯齿间流淌着猎物的恐惧";
        } else {
            return "一把饥渴的刀刃";
        }
    }
}
