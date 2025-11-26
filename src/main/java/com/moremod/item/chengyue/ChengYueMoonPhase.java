package com.moremod.item.chengyue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * 澄月 - 月相系统
 * 
 * 8种月相，从一开始就有效果：
 * 0 = 满月：攻击+50%, 经验×2, 回复增强
 * 1 = 亏凸月：攻击+30%, 吸血+20%
 * 2 = 下弦月：防御+30%, 减伤+15%
 * 3 = 残月：闪避+20%, 移速+20%
 * 4 = 新月：暴击率+30%, 暴击伤害+50%
 * 5 = 娥眉月：攻速+30%, 连击易触发
 * 6 = 上弦月：平衡，全属性+15%
 * 7 = 盈凸月：吸血+50%, 回复+30%
 * 
 * 特殊事件：
 * - 血月（稀有）：所有效果×2 + 狂暴状态
 */
public class ChengYueMoonPhase {
    
    // 月相名称
    private static final String[] PHASE_NAMES = {
        "满月", "亏凸月", "下弦月", "残月",
        "新月", "娥眉月", "上弦月", "盈凸月"
    };
    
    // 月相颜色
    private static final TextFormatting[] PHASE_COLORS = {
        TextFormatting.GOLD,          // 满月 - 金色
        TextFormatting.YELLOW,        // 亏凸月 - 黄色
        TextFormatting.WHITE,         // 下弦月 - 白色
        TextFormatting.GRAY,          // 残月 - 灰色
        TextFormatting.DARK_PURPLE,   // 新月 - 深紫色
        TextFormatting.AQUA,          // 娥眉月 - 青色
        TextFormatting.BLUE,          // 上弦月 - 蓝色
        TextFormatting.GREEN          // 盈凸月 - 绿色
    };
    
    // ==================== 月相获取 ====================
    
    /**
     * 获取当前月相 (0-7)
     */
    public static int getCurrentPhase(World world) {
        return world.provider.getMoonPhase(world.getWorldTime());
    }
    
    /**
     * 获取月相名称
     */
    public static String getPhaseName(int phase) {
        return PHASE_NAMES[phase % 8];
    }
    
    /**
     * 获取月相颜色
     */
    public static TextFormatting getPhaseColor(int phase) {
        return PHASE_COLORS[phase % 8];
    }
    
    /**
     * 检查是否血月（需要Bloodmoon mod）
     */
    public static boolean isBloodMoon(World world) {
        // 如果你有Bloodmoon mod，可以在这里检测
        // 目前简化为：满月 + 5%概率
        return getCurrentPhase(world) == 0 && world.rand.nextFloat() < 0.05f;
    }
    
    // ==================== 属性加成 ====================
    
    /**
     * 获取攻击力加成（乘法）
     */
    public static float getDamageMultiplier(World world) {
        int phase = getCurrentPhase(world);
        
        switch (phase) {
            case 0: // 满月
                return 1.5f; // +50%
            case 1: // 亏凸月
                return 1.3f; // +30%
            case 6: // 上弦月
                return 1.15f; // +15%
            default:
                return 1.0f;
        }
    }
    
    /**
     * 获取暴击率加成（加法）
     */
    public static float getCritChanceBonus(World world) {
        int phase = getCurrentPhase(world);
        
        switch (phase) {
            case 4: // 新月
                return 0.3f; // +30%
            case 5: // 娥眉月
                return 0.1f; // +10%
            default:
                return 0.0f;
        }
    }
    
    /**
     * 获取暴击伤害加成（乘法）
     */
    public static float getCritDamageMultiplier(World world) {
        int phase = getCurrentPhase(world);
        
        if (phase == 4) { // 新月
            return 1.5f; // +50%
        }
        return 1.0f;
    }
    
    /**
     * 获取攻速加成（加法）
     */
    public static float getAttackSpeedBonus(World world) {
        int phase = getCurrentPhase(world);
        
        switch (phase) {
            case 5: // 娥眉月
                return 0.3f; // +30%
            case 6: // 上弦月
                return 0.15f; // +15%
            default:
                return 0.0f;
        }
    }
    
    /**
     * 获取生命偷取加成（乘法）
     */
    public static float getLifeStealMultiplier(World world) {
        int phase = getCurrentPhase(world);
        
        switch (phase) {
            case 7: // 盈凸月
                return 1.5f; // +50%
            case 1: // 亏凸月
                return 1.2f; // +20%
            default:
                return 1.0f;
        }
    }
    
    /**
     * 获取减伤加成（加法）
     */
    public static float getDamageReductionBonus(World world) {
        int phase = getCurrentPhase(world);
        
        switch (phase) {
            case 2: // 下弦月
                return 0.15f; // +15%
            case 6: // 上弦月
                return 0.05f; // +5%
            default:
                return 0.0f;
        }
    }
    
    /**
     * 获取闪避率加成（加法）
     */
    public static float getDodgeChanceBonus(World world) {
        int phase = getCurrentPhase(world);
        
        switch (phase) {
            case 3: // 残月
                return 0.2f; // +20%
            case 4: // 新月
                return 0.1f; // +10%
            default:
                return 0.0f;
        }
    }
    
    /**
     * 获取经验倍率
     */
    public static float getExpMultiplier(World world) {
        int phase = getCurrentPhase(world);
        
        if (phase == 0) { // 满月
            return 2.0f; // 双倍经验
        }
        return 1.0f;
    }
    
    /**
     * 获取连击加成（降低超时时间）
     */
    public static int getComboTimeoutBonus(World world) {
        int phase = getCurrentPhase(world);
        
        if (phase == 5) { // 娥眉月
            return 40; // 额外2秒容错
        }
        return 0;
    }
    
    // ==================== 特殊效果检测 ====================
    
    /**
     * 是否触发"月之祝福"（满月特效）
     */
    public static boolean hasFullMoonBlessing(World world) {
        return getCurrentPhase(world) == 0;
    }
    
    /**
     * 是否触发"暗影之力"（新月特效）
     */
    public static boolean hasNewMoonPower(World world) {
        return getCurrentPhase(world) == 4;
    }
    
    /**
     * 获取全属性加成（上弦月平衡效果）
     */
    public static float getBalancedBonus(World world) {
        int phase = getCurrentPhase(world);
        
        if (phase == 6) { // 上弦月
            return 1.15f; // 全属性+15%
        }
        return 1.0f;
    }
    
    // ==================== 显示信息 ====================
    
    /**
     * 获取当前月相的描述
     */
    public static String getPhaseDescription(World world) {
        int phase = getCurrentPhase(world);
        
        switch (phase) {
            case 0:
                return "攻击+50%, 经验×2";
            case 1:
                return "攻击+30%, 吸血+20%";
            case 2:
                return "减伤+15%";
            case 3:
                return "闪避+20%, 移速+20%";
            case 4:
                return "暴击率+30%, 暴击伤害+50%";
            case 5:
                return "攻速+30%, 连击易触发";
            case 6:
                return "全属性+15%";
            case 7:
                return "吸血+50%, 回复+30%";
            default:
                return "未知月相";
        }
    }
    
    /**
     * 发送月相切换提示
     */
    public static void notifyPhaseChange(EntityPlayer player, int oldPhase, int newPhase) {
        TextFormatting color = getPhaseColor(newPhase);
        String name = getPhaseName(newPhase);
        String desc = getPhaseDescription(player.world);
        
        player.sendMessage(new TextComponentString(
            TextFormatting.LIGHT_PURPLE + "【月相转换】" +
            color + name + " " +
            TextFormatting.GRAY + "(" + desc + ")"
        ));
    }
    
    /**
     * 获取月相图标（用于HUD，可选）
     */
    public static String getPhaseIcon(int phase) {
        switch (phase) {
            case 0: return "●"; // 满月
            case 1: return "◕"; // 亏凸月
            case 2: return "◑"; // 下弦月
            case 3: return "◔"; // 残月
            case 4: return "○"; // 新月
            case 5: return "◔"; // 娥眉月
            case 6: return "◐"; // 上弦月
            case 7: return "◕"; // 盈凸月
            default: return "?";
        }
    }
    
    // ==================== 调试信息 ====================
    
    /**
     * 获取详细的月相状态（用于调试或属性面板）
     */
    public static String getDetailedStatus(World world) {
        int phase = getCurrentPhase(world);
        StringBuilder sb = new StringBuilder();
        
        sb.append(TextFormatting.AQUA).append("当前月相: ");
        sb.append(getPhaseColor(phase)).append(getPhaseName(phase)).append(" ");
        sb.append(getPhaseIcon(phase)).append("\n");
        
        sb.append(TextFormatting.GRAY).append("效果: ").append(getPhaseDescription(world)).append("\n");
        
        // 详细数值
        float dmgMult = getDamageMultiplier(world);
        if (dmgMult > 1.0f) {
            sb.append(TextFormatting.RED).append("  攻击: ×").append(String.format("%.2f", dmgMult)).append("\n");
        }
        
        float critBonus = getCritChanceBonus(world);
        if (critBonus > 0) {
            sb.append(TextFormatting.YELLOW).append("  暴击率: +").append((int)(critBonus * 100)).append("%\n");
        }
        
        float lsMult = getLifeStealMultiplier(world);
        if (lsMult > 1.0f) {
            sb.append(TextFormatting.GREEN).append("  吸血: ×").append(String.format("%.2f", lsMult)).append("\n");
        }
        
        float drBonus = getDamageReductionBonus(world);
        if (drBonus > 0) {
            sb.append(TextFormatting.BLUE).append("  减伤: +").append((int)(drBonus * 100)).append("%\n");
        }
        
        float dodgeBonus = getDodgeChanceBonus(world);
        if (dodgeBonus > 0) {
            sb.append(TextFormatting.AQUA).append("  闪避: +").append((int)(dodgeBonus * 100)).append("%\n");
        }
        
        float expMult = getExpMultiplier(world);
        if (expMult > 1.0f) {
            sb.append(TextFormatting.LIGHT_PURPLE).append("  经验: ×").append(String.format("%.1f", expMult)).append("\n");
        }
        
        return sb.toString();
    }
}
