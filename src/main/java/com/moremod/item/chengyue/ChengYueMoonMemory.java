package com.moremod.item.chengyue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * 澄月 - 月相记忆系统
 * 
 * 机制：
 * 1. 记录前一天的月相
 * 2. 当天全天享受前一天月相的加成
 * 3. 每个游戏日（24000 ticks）更新一次记忆
 * 4. 提供稳定的战斗策略空间
 */
public class ChengYueMoonMemory {
    
    // NBT键名
    private static final String KEY_REMEMBERED_PHASE = "RememberedPhase";
    private static final String KEY_LAST_UPDATE_DAY = "LastUpdateDay";
    private static final String KEY_MEMORY_ACTIVE = "MemoryActive";
    
    // 一个游戏日的长度（ticks）
    private static final long DAY_LENGTH = 24000L;
    
    // ==================== 月相记忆更新 ====================
    
    /**
     * 检查并更新月相记忆
     * 应该在每tick调用（在EventHandler中）
     * 
     * @return true如果发生了更新
     */
    public static boolean checkAndUpdateMemory(ItemStack stack, World world) {
        if (stack.isEmpty() || world.isRemote) return false;
        
        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        
        // 获取当前游戏日
        long currentDay = getCurrentDay(world);
        long lastUpdateDay = nbt.getLong(KEY_LAST_UPDATE_DAY);
        
        // 如果已经是新的一天，更新记忆
        if (currentDay > lastUpdateDay) {
            updateMemory(stack, world, currentDay);
            return true;
        }
        
        return false;
    }
    
    /**
     * 更新月相记忆
     */
    private static void updateMemory(ItemStack stack, World world, long currentDay) {
        NBTTagCompound nbt = stack.getTagCompound();
        
        // 记录当前月相作为"昨天的月相"
        int currentPhase = ChengYueMoonPhase.getCurrentPhase(world);
        nbt.setInteger(KEY_REMEMBERED_PHASE, currentPhase);
        nbt.setLong(KEY_LAST_UPDATE_DAY, currentDay);
        nbt.setBoolean(KEY_MEMORY_ACTIVE, true);
    }
    
    /**
     * 获取当前游戏日（从世界创建开始计算）
     */
    private static long getCurrentDay(World world) {
        return world.getTotalWorldTime() / DAY_LENGTH;
    }
    
    // ==================== 月相记忆读取 ====================
    
    /**
     * 获取记忆中的月相
     * 
     * @return 记忆中的月相（0-7），如果没有记忆返回-1
     */
    public static int getRememberedPhase(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        
        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        
        if (!nbt.getBoolean(KEY_MEMORY_ACTIVE)) {
            return -1; // 还没有激活记忆
        }
        
        return nbt.getInteger(KEY_REMEMBERED_PHASE);
    }
    
    /**
     * 检查是否有激活的月相记忆
     */
    public static boolean hasActiveMemory(ItemStack stack) {
        return getRememberedPhase(stack) != -1;
    }
    
    // ==================== 月相加成应用 ====================
    
    /**
     * 获取攻击力加成（使用记忆月相）
     * 如果没有记忆，回退到当前月相
     */
    public static float getDamageMultiplierWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        
        if (phase == -1) {
            // 没有记忆，使用当前月相
            return ChengYueMoonPhase.getDamageMultiplier(world);
        }
        
        // 使用记忆月相计算
        return getDamageMultiplierByPhase(phase);
    }
    
    /**
     * 获取暴击率加成（使用记忆月相）
     */
    public static float getCritChanceBonusWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        
        if (phase == -1) {
            return ChengYueMoonPhase.getCritChanceBonus(world);
        }
        
        return getCritChanceBonusByPhase(phase);
    }
    
    /**
     * 获取暴击伤害加成（使用记忆月相）
     */
    public static float getCritDamageMultiplierWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        
        if (phase == -1) {
            return ChengYueMoonPhase.getCritDamageMultiplier(world);
        }
        
        return getCritDamageMultiplierByPhase(phase);
    }
    
    /**
     * 获取攻速加成（使用记忆月相）
     */
    public static float getAttackSpeedBonusWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        
        if (phase == -1) {
            return ChengYueMoonPhase.getAttackSpeedBonus(world);
        }
        
        return getAttackSpeedBonusByPhase(phase);
    }
    
    /**
     * 获取生命偷取加成（使用记忆月相）
     */
    public static float getLifeStealMultiplierWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        
        if (phase == -1) {
            return ChengYueMoonPhase.getLifeStealMultiplier(world);
        }
        
        return getLifeStealMultiplierByPhase(phase);
    }
    
    /**
     * 获取减伤加成（使用记忆月相）
     */
    public static float getDamageReductionBonusWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        
        if (phase == -1) {
            return ChengYueMoonPhase.getDamageReductionBonus(world);
        }
        
        return getDamageReductionBonusByPhase(phase);
    }
    
    /**
     * 获取闪避率加成（使用记忆月相）
     */
    public static float getDodgeChanceBonusWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        
        if (phase == -1) {
            return ChengYueMoonPhase.getDodgeChanceBonus(world);
        }
        
        return getDodgeChanceBonusByPhase(phase);
    }
    
    /**
     * 获取经验倍率（使用记忆月相）
     */
    public static float getExpMultiplierWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        
        if (phase == -1) {
            return ChengYueMoonPhase.getExpMultiplier(world);
        }
        
        return getExpMultiplierByPhase(phase);
    }
    
    // ==================== 按月相计算加成（内部方法）====================
    
    private static float getDamageMultiplierByPhase(int phase) {
        switch (phase) {
            case 0: return 1.5f;  // 满月
            case 1: return 1.3f;  // 亏凸月
            case 6: return 1.15f; // 上弦月
            default: return 1.0f;
        }
    }
    
    private static float getCritChanceBonusByPhase(int phase) {
        switch (phase) {
            case 4: return 0.3f; // 新月
            case 5: return 0.1f; // 娥眉月
            default: return 0.0f;
        }
    }
    
    private static float getCritDamageMultiplierByPhase(int phase) {
        if (phase == 4) return 1.5f; // 新月
        return 1.0f;
    }
    
    private static float getAttackSpeedBonusByPhase(int phase) {
        switch (phase) {
            case 5: return 0.3f;  // 娥眉月
            case 6: return 0.15f; // 上弦月
            default: return 0.0f;
        }
    }
    
    private static float getLifeStealMultiplierByPhase(int phase) {
        switch (phase) {
            case 7: return 1.5f; // 盈凸月
            case 1: return 1.2f; // 亏凸月
            default: return 1.0f;
        }
    }
    
    private static float getDamageReductionBonusByPhase(int phase) {
        switch (phase) {
            case 2: return 0.15f; // 下弦月
            case 6: return 0.05f; // 上弦月
            default: return 0.0f;
        }
    }
    
    private static float getDodgeChanceBonusByPhase(int phase) {
        switch (phase) {
            case 3: return 0.2f; // 残月
            case 4: return 0.1f; // 新月
            default: return 0.0f;
        }
    }
    
    private static float getExpMultiplierByPhase(int phase) {
        if (phase == 0) return 2.0f; // 满月
        return 1.0f;
    }
    
    // ==================== 通知系统 ====================
    
    /**
     * 通知玩家月相记忆更新
     */
    public static void notifyMemoryUpdate(EntityPlayer player, ItemStack stack, World world) {
        if (world.isRemote) return;
        
        int rememberedPhase = getRememberedPhase(stack);
        if (rememberedPhase == -1) return;
        
        String phaseName = ChengYueMoonPhase.getPhaseName(rememberedPhase);
        TextFormatting color = ChengYueMoonPhase.getPhaseColor(rememberedPhase);
        String icon = ChengYueMoonPhase.getPhaseIcon(rememberedPhase);
        
        player.sendMessage(new TextComponentString(
            TextFormatting.LIGHT_PURPLE + "【月相记忆】" +
            TextFormatting.GRAY + " 今日继承 " +
            color + icon + " " + phaseName +
            TextFormatting.GRAY + " 的力量！"
        ));
    }
    
    // ==================== 显示信息 ====================
    
    /**
     * 获取月相记忆状态
     */
    public static String getMemoryStatus(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        
        if (phase == -1) {
            return "§7月相记忆: §c未激活";
        }
        
        String phaseName = ChengYueMoonPhase.getPhaseName(phase);
        TextFormatting color = ChengYueMoonPhase.getPhaseColor(phase);
        String icon = ChengYueMoonPhase.getPhaseIcon(phase);
        
        // 获取当前游戏日和下次更新时间
        long currentDay = getCurrentDay(world);
        long currentTime = world.getTotalWorldTime();
        long nextDayStart = (currentDay + 1) * DAY_LENGTH;
        long ticksRemaining = nextDayStart - currentTime;
        int minutesRemaining = (int)(ticksRemaining / 1200); // 1分钟 = 1200 ticks
        
        StringBuilder sb = new StringBuilder();
        sb.append("§d【月相记忆】\n");
        sb.append(color).append(icon).append(" ").append(phaseName).append("\n");
        sb.append("§7持续今日全天\n");
        sb.append("§7更新倒计时: §f").append(minutesRemaining).append(" 分钟");
        
        return sb.toString();
    }
    
    /**
     * 获取简短状态（用于HUD或Tooltip）
     */
    public static String getMemoryTooltip(ItemStack stack) {
        int phase = getRememberedPhase(stack);
        
        if (phase == -1) {
            return "§7记忆: §c未激活";
        }
        
        String phaseName = ChengYueMoonPhase.getPhaseName(phase);
        TextFormatting color = ChengYueMoonPhase.getPhaseColor(phase);
        String icon = ChengYueMoonPhase.getPhaseIcon(phase);
        
        return "§d记忆: " + color + icon + " " + phaseName;
    }
    
    /**
     * 获取详细效果列表
     */
    public static String getMemoryEffects(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        
        if (phase == -1) {
            return "§7暂无记忆效果";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("§d【记忆效果】\n");
        
        // 根据记忆的月相显示效果
        float dmgMult = getDamageMultiplierByPhase(phase);
        if (dmgMult > 1.0f) {
            sb.append("§c攻击: ×").append(String.format("%.2f", dmgMult)).append("\n");
        }
        
        float critBonus = getCritChanceBonusByPhase(phase);
        if (critBonus > 0) {
            sb.append("§6暴击率: +").append((int)(critBonus * 100)).append("%\n");
        }
        
        float critDmgMult = getCritDamageMultiplierByPhase(phase);
        if (critDmgMult > 1.0f) {
            sb.append("§6暴击伤害: ×").append(String.format("%.2f", critDmgMult)).append("\n");
        }
        
        float lsMult = getLifeStealMultiplierByPhase(phase);
        if (lsMult > 1.0f) {
            sb.append("§2吸血: ×").append(String.format("%.2f", lsMult)).append("\n");
        }
        
        float drBonus = getDamageReductionBonusByPhase(phase);
        if (drBonus > 0) {
            sb.append("§9减伤: +").append((int)(drBonus * 100)).append("%\n");
        }
        
        float dodgeBonus = getDodgeChanceBonusByPhase(phase);
        if (dodgeBonus > 0) {
            sb.append("§b闪避: +").append((int)(dodgeBonus * 100)).append("%\n");
        }
        
        float expMult = getExpMultiplierByPhase(phase);
        if (expMult > 1.0f) {
            sb.append("§d经验: ×").append(String.format("%.1f", expMult)).append("\n");
        }
        
        return sb.toString();
    }
    
    // ==================== 调试功能 ====================
    
    /**
     * 手动设置月相记忆（用于测试）
     */
    public static void setMemoryPhase(ItemStack stack, int phase, World world) {
        if (stack.isEmpty() || phase < 0 || phase > 7) return;
        
        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        
        nbt.setInteger(KEY_REMEMBERED_PHASE, phase);
        nbt.setLong(KEY_LAST_UPDATE_DAY, getCurrentDay(world));
        nbt.setBoolean(KEY_MEMORY_ACTIVE, true);
    }
    
    /**
     * 重置月相记忆
     */
    public static void resetMemory(ItemStack stack) {
        if (stack.isEmpty()) return;
        
        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();
        
        nbt.removeTag(KEY_REMEMBERED_PHASE);
        nbt.removeTag(KEY_LAST_UPDATE_DAY);
        nbt.setBoolean(KEY_MEMORY_ACTIVE, false);
    }
}
