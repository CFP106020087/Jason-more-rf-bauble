package com.moremod.item.sawblade;

import com.moremod.item.sawblade.potion.PotionBloodEuphoria;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 锯刃剑 - 精美Tooltip显示（勇者之剑风格）
 */
@SideOnly(Side.CLIENT)
public class SawBladeTooltip {
    
    public static void addTooltip(ItemStack stack, EntityPlayer player, 
                                  List<String> tooltip, ITooltipFlag flag) {
        SawBladeNBT.init(stack);
        
        // 获取数据
        int level = SawBladeNBT.getLevel(stack);
        int exp = SawBladeNBT.getExp(stack);
        int expNext = SawBladeNBT.getExpToNext(stack);
        int totalKills = SawBladeNBT.getTotalKills(stack);
        int bossKills = SawBladeNBT.getBossKills(stack);
        int bleedKills = SawBladeNBT.getBleedKills(stack);
        String title = SawBladeNBT.getGrowthTitle(stack);
        
        // ========== 标题 ==========
        tooltip.add(TextFormatting.RED + "══════ " +
                TextFormatting.DARK_RED + "锯刃剑" +
                TextFormatting.RED + " ══════");
        
        // ========== 成长信息 ==========
        tooltip.add(TextFormatting.GOLD + title +
                TextFormatting.WHITE + " Lv." + level);
        
        // 经验条
        if (level < 100) {
            float progress = (float)exp / expNext;
            tooltip.add(TextFormatting.GRAY + "经验: " +
                    makeProgressBar(progress, 15) +
                    TextFormatting.WHITE + " " + exp + "/" + expNext);
        } else {
            tooltip.add(TextFormatting.LIGHT_PURPLE + "★ 满级 ★");
        }
        
        // 击杀统计
        String killInfo = TextFormatting.DARK_GRAY + "击杀: " + totalKills;
        if (bossKills > 0) {
            killInfo += TextFormatting.GOLD + " (Boss: " + bossKills + ")";
        }
        if (bleedKills > 0) {
            killInfo += TextFormatting.RED + " (出血: " + bleedKills + ")";
        }
        tooltip.add(killInfo);
        
        // ========== 武器基础 ==========
        tooltip.add("");
        tooltip.add(TextFormatting.WHITE + "【基础属性】");
        
        float damage = SawBladeStats.getBaseDamage(stack);
        float speed = SawBladeStats.getAttackSpeed(stack);
        
        tooltip.add(TextFormatting.GRAY + "攻击力: " +
                TextFormatting.RED + String.format("%.1f", damage));
        tooltip.add(TextFormatting.GRAY + "攻击速度: " +
                TextFormatting.AQUA + String.format("%.2f", speed));
        
        // ========== 技能总览/详细 ==========
        if (player != null && player.isSneaking()) {
            addDetailedSkills(stack, player, tooltip);
        } else {
            addSimpleSkills(stack, player, tooltip);
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_GRAY + "潜行查看详细技能");
        }
        
        // ========== 统计数据（F3+H）==========
        if (flag.isAdvanced()) {
            addStatistics(stack, tooltip);
        }
        
        // ========== Lore ==========
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + 
                   SawBladeNBT.getWeaponLore(stack));
    }
    
    /**
     * 简化技能信息
     */
    private static void addSimpleSkills(ItemStack stack, EntityPlayer player, List<String> tooltip) {
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "◆ 出血猎手 " +
                   TextFormatting.GRAY + "- 累积出血并爆发");
        tooltip.add(TextFormatting.DARK_RED + "◆ 撕裂打击 " +
                   TextFormatting.GRAY + "- 多段持续伤害");
        tooltip.add(TextFormatting.YELLOW + "◆ 猎杀本能 " +
                   TextFormatting.GRAY + "- 低血量/背刺增伤");
        
        // 鲜血欢愉 - 显示当前层数
        int euphoriaStacks = player != null ? PotionBloodEuphoria.getStacks(player) : 0;
        String euphoriaText = TextFormatting.LIGHT_PURPLE + "◆ 鲜血欢愉";
        if (euphoriaStacks > 0) {
            euphoriaText += TextFormatting.GREEN + " [" + euphoriaStacks + "/" + 
                          SawBladeStats.getBloodEuphoriaMaxStacks(stack) + "]";
        }
        euphoriaText += TextFormatting.GRAY + " - 出血爆裂获得buff";
        tooltip.add(euphoriaText);
        
        tooltip.add(TextFormatting.DARK_PURPLE + "◆ 处决收割 " +
                   TextFormatting.DARK_GRAY + "[右键] " +
                   TextFormatting.GRAY + "- AOE处决");
    }
    
    /**
     * 详细技能信息
     */
    private static void addDetailedSkills(ItemStack stack, EntityPlayer player, List<String> tooltip) {
        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "━━━ 技能详情 ━━━");
        
        // 技能1：出血猎手
        float bleedBuildUp = SawBladeStats.getBaseBleedBuildUp(stack);
        float bleedDamage = SawBladeStats.getBleedBurstDamagePercent(stack) * 100;
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "◆ 出血猎手");
        tooltip.add(TextFormatting.GRAY + "  累积: " + 
                   TextFormatting.WHITE + String.format("%.1f", bleedBuildUp) + "/hit");
        tooltip.add(TextFormatting.GRAY + "  爆裂: " + 
                   TextFormatting.RED + String.format("%.0f%%", bleedDamage) + 
                   TextFormatting.GRAY + " 最大生命");
        
        // 技能2：撕裂打击
        int lacHits = SawBladeStats.getLacerationHits(stack);
        float lacDamage = SawBladeStats.getLacerationDamagePercent(stack) * 100;
        int lacInterval = SawBladeStats.getLacerationInterval(stack);
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_RED + "◆ 撕裂打击");
        tooltip.add(TextFormatting.GRAY + "  " + 
                   TextFormatting.WHITE + lacHits + "段" +
                   TextFormatting.GRAY + " × " + 
                   TextFormatting.RED + String.format("%.0f%%", lacDamage) + 
                   TextFormatting.GRAY + " 伤害");
        tooltip.add(TextFormatting.GRAY + "  间隔: " + 
                   TextFormatting.WHITE + lacInterval + " tick");
        
        // 技能3：猎杀本能
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "◆ 猎杀本能");
        tooltip.add(TextFormatting.GRAY + "  <50%HP: " + 
                   TextFormatting.YELLOW + "1.2-1.8×");
        tooltip.add(TextFormatting.GRAY + "  <30%HP: " + 
                   TextFormatting.GOLD + "1.5-2.5×");
        tooltip.add(TextFormatting.GRAY + "  <10%HP: " + 
                   TextFormatting.RED + "2.0-3.5×");
        tooltip.add(TextFormatting.GRAY + "  背刺: " + 
                   TextFormatting.DARK_RED + "最高3.5×");
        
        // 技能4：鲜血欢愉
        int euphoriaStacks = player != null ? PotionBloodEuphoria.getStacks(player) : 0;
        int maxStacks = SawBladeStats.getBloodEuphoriaMaxStacks(stack);
        float perStack = SawBladeStats.getBloodEuphoriaPerStack(stack) * 100;
        float attackSpeed = SawBladeStats.getBloodEuphoriaAttackSpeed(stack) * 100;
        float lifeSteal = SawBladeStats.getBloodEuphoriaLifeSteal(stack) * 100;
        
        tooltip.add("");
        String euphoriaTitle = TextFormatting.LIGHT_PURPLE + "◆ 鲜血欢愉";
        if (euphoriaStacks > 0) {
            euphoriaTitle += TextFormatting.GREEN + " [活跃 " + euphoriaStacks + "/" + maxStacks + "]";
        }
        tooltip.add(euphoriaTitle);
        
        tooltip.add(TextFormatting.GRAY + "  增伤: " + 
                   TextFormatting.LIGHT_PURPLE + String.format("+%.0f%%", perStack) + 
                   TextFormatting.GRAY + "/层 (最多" + maxStacks + "层)");
        tooltip.add(TextFormatting.GRAY + "  攻速: " + 
                   TextFormatting.AQUA + String.format("+%.0f%%", attackSpeed) + 
                   TextFormatting.GRAY + "/层");
        
        if (lifeSteal > 0) {
            tooltip.add(TextFormatting.GRAY + "  偷取: " + 
                       TextFormatting.GREEN + String.format("%.0f%%", lifeSteal));
        }
        
        // 技能5：处决收割
        float execThreshold = SawBladeStats.getExecuteThreshold(stack) * 100;
        float execRange = SawBladeStats.getExecuteRange(stack);
        int execTargets = SawBladeStats.getExecuteMaxTargets(stack);
        
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_PURPLE + "◆ 处决收割 " +
                   TextFormatting.DARK_GRAY + "[右键]");
        tooltip.add(TextFormatting.GRAY + "  阈值: " +
                   TextFormatting.RED + String.format("%.0f%%", execThreshold) + 
                   TextFormatting.GRAY + " HP");
        tooltip.add(TextFormatting.GRAY + "  范围: " +
                   TextFormatting.WHITE + String.format("%.0f", execRange) + "格" +
                   TextFormatting.GRAY + " | 数量: " +
                   TextFormatting.WHITE + execTargets);
        
        // 冷却显示
        if (player != null && player.world != null) {
            long now = player.world.getTotalWorldTime();
            long lastCast = SawBladeNBT.getSkillCooldown(stack);
            long cooldown = SawBladeStats.getExecuteCooldown(stack);
            long remaining = Math.max(0, cooldown - (now - lastCast));
            
            if (remaining > 0) {
                tooltip.add(TextFormatting.RED + "  ▸ 冷却: " + (remaining/20) + "秒");
            } else {
                tooltip.add(TextFormatting.GREEN + "  ▸ 技能就绪");
            }
        }
    }
    
    /**
     * 统计数据（F3+H高级信息）
     */
    private static void addStatistics(ItemStack stack, List<String> tooltip) {
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "=== Debug Info ===");
        
        int kills = SawBladeNBT.getTotalKills(stack);
        int bossKills = SawBladeNBT.getBossKills(stack);
        int bleedKills = SawBladeNBT.getBleedKills(stack);
        int backstabKills = SawBladeNBT.getBackstabKills(stack);
        int bleedProcs = SawBladeNBT.getBleedProcs(stack);
        int execCount = SawBladeNBT.getExecuteCount(stack);
        float totalDamage = SawBladeNBT.getTotalDamage(stack);
        
        tooltip.add(TextFormatting.DARK_GRAY + "Level: " + SawBladeNBT.getLevel(stack) + "/100");
        tooltip.add(TextFormatting.DARK_GRAY + "Kills: " + kills + 
                   " (Boss:" + bossKills + " Bleed:" + bleedKills + " Backstab:" + backstabKills + ")");
        tooltip.add(TextFormatting.DARK_GRAY + "Bleed Procs: " + bleedProcs);
        tooltip.add(TextFormatting.DARK_GRAY + "Executes: " + execCount);
        tooltip.add(TextFormatting.DARK_GRAY + "Total Damage: " + String.format("%.0f", totalDamage));
    }
    
    /**
     * 创建渐变进度条
     */
    private static String makeProgressBar(float progress, int length) {
        int filled = (int)(progress * length);
        StringBuilder sb = new StringBuilder();
        
        // 根据进度选择颜色
        String color = progress < 0.3F ? TextFormatting.RED.toString() :
                progress < 0.7F ? TextFormatting.YELLOW.toString() :
                        TextFormatting.GREEN.toString();
        
        sb.append(color);
        for (int i = 0; i < length; i++) {
            if (i < filled) {
                sb.append("▮");
            } else {
                sb.append(TextFormatting.DARK_GRAY).append("▯").append(color);
            }
        }
        
        return sb.toString();
    }
}