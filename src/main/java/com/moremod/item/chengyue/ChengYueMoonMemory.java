package com.moremod.item.chengyue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * 澄月 - 月相记忆系统（平衡版）
 *
 * 重要改动：
 * - 记忆只代表“昨日加成”，不再和当前月相自动相乘。
 * - 没有记忆时，所有 WithMemory 方法返回 1.0（倍率）或 0（加法）。
 */
public class ChengYueMoonMemory {

    private static final String KEY_REMEMBERED_PHASE = "RememberedPhase";
    private static final String KEY_LAST_UPDATE_DAY = "LastUpdateDay";
    private static final String KEY_MEMORY_ACTIVE = "MemoryActive";

    private static final long DAY_LENGTH = 24000L;

    // ==================== 月相记忆更新 ====================

    public static boolean checkAndUpdateMemory(ItemStack stack, World world) {
        if (stack.isEmpty() || world.isRemote) return false;

        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();

        long currentDay = getCurrentDay(world);
        long lastUpdateDay = nbt.getLong(KEY_LAST_UPDATE_DAY);

        if (currentDay > lastUpdateDay) {
            updateMemory(stack, world, currentDay);
            return true;
        }

        return false;
    }

    private static void updateMemory(ItemStack stack, World world, long currentDay) {
        NBTTagCompound nbt = stack.getTagCompound();
        int currentPhase = ChengYueMoonPhase.getCurrentPhase(world);

        nbt.setInteger(KEY_REMEMBERED_PHASE, currentPhase);
        nbt.setLong(KEY_LAST_UPDATE_DAY, currentDay);
        nbt.setBoolean(KEY_MEMORY_ACTIVE, true);
    }

    private static long getCurrentDay(World world) {
        return world.getTotalWorldTime() / DAY_LENGTH;
    }

    // ==================== 月相记忆读取 ====================

    public static int getRememberedPhase(ItemStack stack) {
        if (stack.isEmpty()) return -1;

        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();

        if (!nbt.getBoolean(KEY_MEMORY_ACTIVE)) {
            return -1;
        }

        return nbt.getInteger(KEY_REMEMBERED_PHASE);
    }

    public static boolean hasActiveMemory(ItemStack stack) {
        return getRememberedPhase(stack) != -1;
    }

    // ==================== WithMemory 加成（不再回退当前月相）====================

    public static float getDamageMultiplierWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        if (phase == -1) return 1.0f;
        return getDamageMultiplierByPhase(phase);
    }

    public static float getCritChanceBonusWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        if (phase == -1) return 0.0f;
        return getCritChanceBonusByPhase(phase);
    }

    public static float getCritDamageMultiplierWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        if (phase == -1) return 1.0f;
        return getCritDamageMultiplierByPhase(phase);
    }

    public static float getAttackSpeedBonusWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        if (phase == -1) return 0.0f;
        return getAttackSpeedBonusByPhase(phase);
    }

    public static float getLifeStealMultiplierWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        if (phase == -1) return 1.0f;
        return getLifeStealMultiplierByPhase(phase);
    }

    public static float getDamageReductionBonusWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        if (phase == -1) return 0.0f;
        return getDamageReductionBonusByPhase(phase);
    }

    public static float getDodgeChanceBonusWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        if (phase == -1) return 0.0f;
        return getDodgeChanceBonusByPhase(phase);
    }

    public static float getExpMultiplierWithMemory(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);
        if (phase == -1) return 1.0f;
        return getExpMultiplierByPhase(phase);
    }

    // ==================== 按月相计算加成（内部方法）====================

    private static float getDamageMultiplierByPhase(int phase) {
        switch (phase) {
            case 0: return 1.5f;
            case 1: return 1.3f;
            case 6: return 1.15f;
            default: return 1.0f;
        }
    }

    private static float getCritChanceBonusByPhase(int phase) {
        switch (phase) {
            case 4: return 0.3f;
            case 5: return 0.1f;
            default: return 0.0f;
        }
    }

    private static float getCritDamageMultiplierByPhase(int phase) {
        if (phase == 4) return 1.5f;
        return 1.0f;
    }

    private static float getAttackSpeedBonusByPhase(int phase) {
        switch (phase) {
            case 5: return 0.3f;
            case 6: return 0.15f;
            default: return 0.0f;
        }
    }

    private static float getLifeStealMultiplierByPhase(int phase) {
        switch (phase) {
            case 7: return 1.5f;
            case 1: return 1.2f;
            default: return 1.0f;
        }
    }

    private static float getDamageReductionBonusByPhase(int phase) {
        switch (phase) {
            case 2: return 0.15f;
            case 6: return 0.05f;
            default: return 0.0f;
        }
    }

    private static float getDodgeChanceBonusByPhase(int phase) {
        switch (phase) {
            case 3: return 0.2f;
            case 4: return 0.1f;
            default: return 0.0f;
        }
    }

    private static float getExpMultiplierByPhase(int phase) {
        if (phase == 0) return 2.0f;
        return 1.0f;
    }

    // ==================== 通知与显示 ====================

    public static void notifyMemoryUpdate(EntityPlayer player, ItemStack stack, World world) {
        if (world.isRemote) return;

        int phase = getRememberedPhase(stack);
        if (phase == -1) return;

        String phaseName = ChengYueMoonPhase.getPhaseName(phase);
        TextFormatting color = ChengYueMoonPhase.getPhaseColor(phase);
        String icon = ChengYueMoonPhase.getPhaseIcon(phase);

        player.sendMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "【月相记忆】" +
                        TextFormatting.GRAY + " 今日继承 " +
                        color + icon + " " + phaseName +
                        TextFormatting.GRAY + " 的力量！"
        ));
    }

    public static String getMemoryStatus(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);

        if (phase == -1) {
            return "§7月相记忆: §c未激活";
        }

        String phaseName = ChengYueMoonPhase.getPhaseName(phase);
        TextFormatting color = ChengYueMoonPhase.getPhaseColor(phase);
        String icon = ChengYueMoonPhase.getPhaseIcon(phase);

        long currentDay = getCurrentDay(world);
        long currentTime = world.getTotalWorldTime();
        long nextDayStart = (currentDay + 1) * DAY_LENGTH;
        long ticksRemaining = nextDayStart - currentTime;
        int minutesRemaining = (int) (ticksRemaining / 1200);

        StringBuilder sb = new StringBuilder();
        sb.append("§d【月相记忆】\n");
        sb.append(color).append(icon).append(" ").append(phaseName).append("\n");
        sb.append("§7持续今日全天\n");
        sb.append("§7更新倒计时: §f").append(minutesRemaining).append(" 分钟");

        return sb.toString();
    }

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

    public static String getMemoryEffects(ItemStack stack, World world) {
        int phase = getRememberedPhase(stack);

        if (phase == -1) {
            return "§7暂无记忆效果";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§d【记忆效果】\n");

        float dmgMult = getDamageMultiplierByPhase(phase);
        if (dmgMult > 1.0f) {
            sb.append("§c攻击: ×").append(String.format("%.2f", dmgMult)).append("\n");
        }

        float critBonus = getCritChanceBonusByPhase(phase);
        if (critBonus > 0) {
            sb.append("§6暴击率: +").append((int) (critBonus * 100)).append("%\n");
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
            sb.append("§9减伤: +").append((int) (drBonus * 100)).append("%\n");
        }

        float dodgeBonus = getDodgeChanceBonusByPhase(phase);
        if (dodgeBonus > 0) {
            sb.append("§b闪避: +").append((int) (dodgeBonus * 100)).append("%\n");
        }

        float expMult = getExpMultiplierByPhase(phase);
        if (expMult > 1.0f) {
            sb.append("§d经验: ×").append(String.format("%.1f", expMult)).append("\n");
        }

        return sb.toString();
    }

    // ==================== 调试 ====================

    public static void setMemoryPhase(ItemStack stack, int phase, World world) {
        if (stack.isEmpty() || phase < 0 || phase > 7) return;

        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();

        nbt.setInteger(KEY_REMEMBERED_PHASE, phase);
        nbt.setLong(KEY_LAST_UPDATE_DAY, getCurrentDay(world));
        nbt.setBoolean(KEY_MEMORY_ACTIVE, true);
    }

    public static void resetMemory(ItemStack stack) {
        if (stack.isEmpty()) return;

        ChengYueNBT.init(stack);
        NBTTagCompound nbt = stack.getTagCompound();

        nbt.removeTag(KEY_REMEMBERED_PHASE);
        nbt.removeTag(KEY_LAST_UPDATE_DAY);
        nbt.setBoolean(KEY_MEMORY_ACTIVE, false);
    }
}
