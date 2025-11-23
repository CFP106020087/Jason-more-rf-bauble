package com.moremod.item.chengyue;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import java.util.UUID;

/**
 * 简化版NBT - 只存储武器本身的数据
 */
public class ChengYueNBT {
    
    // 武器属性
    public static final String KEY_LEVEL = "SwordLevel";
    public static final String KEY_EXP = "SwordExp";
    public static final String KEY_EXP_TO_NEXT = "ExpToNext";
    public static final String KEY_STAGE = "EvolutionStage";
    public static final String KEY_KILL_COUNT = "KillCount";
    public static final String KEY_BOSS_KILLS = "BossKillCount";
    public static final String KEY_ELITE_KILLS = "EliteKillCount";
    
    // ✨ 保留：灵魂绑定
    public static final String KEY_SOUL_BOUND = "SoulBound";
    
    public static void init(ItemStack stack) {
        if (stack.isEmpty()) return;
        
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        
        NBTTagCompound nbt = stack.getTagCompound();
        
        if (!nbt.hasKey(KEY_LEVEL)) nbt.setInteger(KEY_LEVEL, 0);
        if (!nbt.hasKey(KEY_EXP)) nbt.setLong(KEY_EXP, 0L);
        if (!nbt.hasKey(KEY_EXP_TO_NEXT)) nbt.setLong(KEY_EXP_TO_NEXT, 1000L);
        if (!nbt.hasKey(KEY_STAGE)) nbt.setInteger(KEY_STAGE, 1);
        if (!nbt.hasKey(KEY_KILL_COUNT)) nbt.setLong(KEY_KILL_COUNT, 0L);
        if (!nbt.hasKey(KEY_BOSS_KILLS)) nbt.setInteger(KEY_BOSS_KILLS, 0);
        if (!nbt.hasKey(KEY_ELITE_KILLS)) nbt.setInteger(KEY_ELITE_KILLS, 0);
    }
    
    // ==================== 等级和经验 ====================
    
    public static int getLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        init(stack);
        return stack.getTagCompound().getInteger(KEY_LEVEL);
    }
    
    public static void setLevel(ItemStack stack, int level) {
        init(stack);
        stack.getTagCompound().setInteger(KEY_LEVEL, level);
    }
    
    public static long getExp(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getLong(KEY_EXP);
    }
    
    public static void setExp(ItemStack stack, long exp) {
        init(stack);
        stack.getTagCompound().setLong(KEY_EXP, exp);
    }
    
    public static long getExpToNext(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getLong(KEY_EXP_TO_NEXT);
    }
    
    public static void setExpToNext(ItemStack stack, long exp) {
        init(stack);
        stack.getTagCompound().setLong(KEY_EXP_TO_NEXT, exp);
    }
    
    // ==================== 进化阶段 ====================
    
    public static int getStage(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_STAGE);
    }
    
    public static void setStage(ItemStack stack, int stage) {
        init(stack);
        stack.getTagCompound().setInteger(KEY_STAGE, stage);
    }
    
    // ==================== 击杀统计 ====================
    
    public static long getKillCount(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getLong(KEY_KILL_COUNT);
    }
    
    public static void setKillCount(ItemStack stack, long count) {
        init(stack);
        stack.getTagCompound().setLong(KEY_KILL_COUNT, count);
    }
    
    public static int getBossKills(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_BOSS_KILLS);
    }
    
    public static void setBossKills(ItemStack stack, int count) {
        init(stack);
        stack.getTagCompound().setInteger(KEY_BOSS_KILLS, count);
    }
    
    public static int getEliteKills(ItemStack stack) {
        init(stack);
        return stack.getTagCompound().getInteger(KEY_ELITE_KILLS);
    }
    
    public static void setEliteKills(ItemStack stack, int count) {
        init(stack);
        stack.getTagCompound().setInteger(KEY_ELITE_KILLS, count);
    }
    
    // ==================== ✨ 灵魂绑定（保留）====================
    
    /**
     * 获取灵魂绑定的玩家UUID
     */
    public static UUID getSoulBound(ItemStack stack) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        
        if (!nbt.hasKey(KEY_SOUL_BOUND)) {
            return null;
        }
        
        // 1.12.2 使用 hasUniqueId 和 getUniqueId
        if (nbt.hasUniqueId(KEY_SOUL_BOUND)) {
            return nbt.getUniqueId(KEY_SOUL_BOUND);
        }
        
        return null;
    }
    
    /**
     * 设置灵魂绑定
     */
    public static void setSoulBound(ItemStack stack, UUID playerUUID) {
        init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        
        if (playerUUID != null) {
            nbt.setUniqueId(KEY_SOUL_BOUND, playerUUID);
        } else {
            nbt.removeTag(KEY_SOUL_BOUND);
        }
    }
    
    /**
     * 检查是否已绑定
     */
    public static boolean isSoulBound(ItemStack stack) {
        return getSoulBound(stack) != null;
    }
    
    /**
     * 检查是否绑定到指定玩家
     */
    public static boolean isSoulBoundTo(ItemStack stack, UUID playerUUID) {
        UUID bound = getSoulBound(stack);
        return bound != null && bound.equals(playerUUID);
    }
    
    /**
     * 清除灵魂绑定
     */
    public static void clearSoulBound(ItemStack stack) {
        setSoulBound(stack, null);
    }
}