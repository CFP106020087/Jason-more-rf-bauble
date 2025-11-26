package com.moremod.item.chengyue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * 澄月 - 等级系统
 * 
 * 等级范围：0-30级（第一阶段）
 * 经验来源：
 * - 攻击：10 EXP
 * - 击杀普通怪：50 EXP
 * - 击杀精英怪：100-500 EXP（根据词缀）
 * - 击杀Boss：500-2000 EXP
 * 
 * 月相加成：
 * - 满月：经验×2
 */
public class ChengYueLevel {
    
    // ==================== 经验获取 ====================
    
    /**
     * 增加经验（会自动应用月相记忆加成）
     */
    public static void addExp(ItemStack stack, EntityPlayer player, long baseExp) {
        if (stack.isEmpty() || player == null) return;
        
        ChengYueNBT.init(stack);
        World world = player.world;
        
        // 应用月相记忆加成
        float moonMultiplier = ChengYueMoonMemory.getExpMultiplierWithMemory(stack, world);
        long actualExp = (long)(baseExp * moonMultiplier);
        
        // 获取当前数据
        int level = ChengYueNBT.getLevel(stack);
        long currentExp = ChengYueNBT.getExp(stack);
        long expToNext = ChengYueNBT.getExpToNext(stack);
        
        currentExp += actualExp;
        
        // 检查升级
        boolean leveled = false;
        while (currentExp >= expToNext && level < 30) { // 第一阶段上限30级
            currentExp -= expToNext;
            level++;
            leveled = true;
            
            // 计算下一级所需经验
            expToNext = calculateExpToNext(level);
            
            // 保存数据
            ChengYueNBT.setLevel(stack, level);
            ChengYueNBT.setExpToNext(stack, expToNext);
            
            // 升级回调
            onLevelUp(stack, player, level);
        }
        
        // 保存经验
        ChengYueNBT.setExp(stack, currentExp);
        
        // 显示经验获得提示（可选）
        if (moonMultiplier > 1.0f) {
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "+" + actualExp + " EXP " +
                TextFormatting.GOLD + "(月相记忆×" + String.format("%.1f", moonMultiplier) + ")"
            ), true);
        }
    }
    
    /**
     * 增加经验（无月相加成，用于特殊情况）
     */
    public static void addExpRaw(ItemStack stack, long exp) {
        ChengYueNBT.init(stack);
        
        int level = ChengYueNBT.getLevel(stack);
        if (level >= 30) return; // 已达上限
        
        long currentExp = ChengYueNBT.getExp(stack);
        long expToNext = ChengYueNBT.getExpToNext(stack);
        
        currentExp += exp;
        
        while (currentExp >= expToNext && level < 30) {
            currentExp -= expToNext;
            level++;
            expToNext = calculateExpToNext(level);
            ChengYueNBT.setLevel(stack, level);
            ChengYueNBT.setExpToNext(stack, expToNext);
        }
        
        ChengYueNBT.setExp(stack, currentExp);
    }
    
    // ==================== 经验计算 ====================
    
    /**
     * 计算升级所需经验
     * 公式：1000 × (1.08 ^ level)
     * 
     * Level 1  -> 2:    1,080 EXP
     * Level 10 -> 11:   2,159 EXP
     * Level 20 -> 21:   4,661 EXP
     * Level 30 -> 31:  10,063 EXP (但第一阶段到30级封顶)
     */
    public static long calculateExpToNext(int level) {
        return (long)(1000 * Math.pow(1.08, level));
    }
    
    /**
     * 计算从0级到指定等级的总经验
     */
    public static long calculateTotalExp(int level) {
        long total = 0;
        for (int i = 0; i < level; i++) {
            total += calculateExpToNext(i);
        }
        return total;
    }
    
    /**
     * 获取升级进度百分比
     */
    public static float getExpProgress(ItemStack stack) {
        ChengYueNBT.init(stack);
        long current = ChengYueNBT.getExp(stack);
        long toNext = ChengYueNBT.getExpToNext(stack);
        
        if (toNext <= 0) return 1.0f;
        return Math.min(1.0f, (float)current / toNext);
    }
    
    // ==================== 升级回调 ====================
    
    /**
     * 升级时触发
     */
    private static void onLevelUp(ItemStack stack, EntityPlayer player, int newLevel) {
        // 通知玩家
        String message = TextFormatting.AQUA + "【澄月】" + 
                        TextFormatting.WHITE + " 等级提升至 " +
                        TextFormatting.GOLD + "Lv." + newLevel + "！";
        
        player.sendMessage(new TextComponentString(message));
        
        // 播放升级音效
        playLevelUpSound(player);
        
        // 特殊等级奖励
        checkMilestone(stack, player, newLevel);
        
        // 检查阶位晋升
        checkEvolution(stack, player, newLevel);
    }
    
    /**
     * 检查里程碑奖励
     */
    private static void checkMilestone(ItemStack stack, EntityPlayer player, int level) {
        switch (level) {
            case 5:
                player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "【里程碑】生命偷取效果提升！"));
                break;
            case 10:
                player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "【里程碑】暴击率提升！"));
                break;
            case 15:
                player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "【里程碑】攻击力大幅提升！"));
                break;
            case 20:
                player.sendMessage(new TextComponentString(
                    TextFormatting.BLUE + "【里程碑】月之庇护冷却缩短！"));
                break;
            case 25:
                player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "【里程碑】接近阶位晋升！"));
                break;
            case 30:
                player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "★ 【满级】第一阶段完成！★"));
                break;
        }
    }
    
    /**
     * 检查阶位晋升
     */
    private static void checkEvolution(ItemStack stack, EntityPlayer player, int level) {
        int currentStage = ChengYueNBT.getStage(stack);
        
        // 阶位1 -> 阶位2：11级 + 100击杀
        if (currentStage == 1 && level >= 11) {
            long kills = ChengYueNBT.getKillCount(stack);
            if (kills >= 100) {
                evolve(stack, player, 2);
            } else {
                player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "【提示】达到11级，击杀100只怪物可晋升阶位！(" + kills + "/100)"
                ));
            }
        }
        
        // 阶位2 -> 阶位3：26级 + 500击杀 + 5Boss
        if (currentStage == 2 && level >= 26) {
            long kills = ChengYueNBT.getKillCount(stack);
            int bossKills = ChengYueNBT.getBossKills(stack);
            
            if (kills >= 500 && bossKills >= 5) {
                evolve(stack, player, 3);
            } else {
                player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "【提示】达到26级，完成以下条件可晋升：\n" +
                    "  击杀: " + kills + "/500\n" +
                    "  Boss: " + bossKills + "/5"
                ));
            }
        }
    }
    
    /**
     * 执行阶位晋升
     */
    private static void evolve(ItemStack stack, EntityPlayer player, int newStage) {
        ChengYueNBT.setStage(stack, newStage);
        
        String stageName = getStageName(newStage);
        
        player.sendMessage(new TextComponentString(
            TextFormatting.LIGHT_PURPLE + "━━━━━━━━━━━━━━━━\n" +
            TextFormatting.GOLD + "【阶位晋升】\n" +
            TextFormatting.AQUA + stageName + "\n" +
            TextFormatting.GRAY + "全属性大幅提升！\n" +
            TextFormatting.LIGHT_PURPLE + "━━━━━━━━━━━━━━━━"
        ));
        
        // 播放特殊音效
        playEvolutionSound(player);
        
        // 粒子特效（可选，需要网络同步）
        // spawnEvolutionParticles(player);
    }
    
    // ==================== 阶位相关 ====================
    
    /**
     * 获取阶位名称（简洁优雅）
     */
    public static String getStageName(int stage) {
        switch (stage) {
            case 1: return "初月";  // First Quarter
            case 2: return "明月";  // Bright Moon
            case 3: return "满月";  // Full Moon
            default: return "?";
        }
    }
    
    /**
     * 获取阶位罗马数字
     */
    public static String getStageRoman(int stage) {
        switch (stage) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            default: return "?";
        }
    }
    
    /**
     * 获取阶位颜色
     */
    public static TextFormatting getStageColor(int stage) {
        switch (stage) {
            case 1: return TextFormatting.GRAY;
            case 2: return TextFormatting.AQUA;
            case 3: return TextFormatting.LIGHT_PURPLE;
            default: return TextFormatting.WHITE;
        }
    }
    
    // ==================== 音效 ====================
    
    /**
     * 播放升级音效
     */
    private static void playLevelUpSound(EntityPlayer player) {
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            getSoundEvent("entity.player.levelup"),
            SoundCategory.PLAYERS,
            0.5f, 1.0f
        );
    }
    
    /**
     * 播放阶位晋升音效
     */
    private static void playEvolutionSound(EntityPlayer player) {
        player.world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            getSoundEvent("ui.toast.challenge_complete"),
            SoundCategory.PLAYERS,
            1.0f, 1.0f
        );
    }
    
    /**
     * 安全获取音效
     */
    private static SoundEvent getSoundEvent(String name) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(
            new net.minecraft.util.ResourceLocation(name)
        );
        return sound != null ? sound : ForgeRegistries.SOUND_EVENTS.getValue(
            new net.minecraft.util.ResourceLocation("entity.experience_orb.pickup")
        );
    }
    
    // ==================== 调试 ====================
    
    /**
     * 获取等级信息文本
     */
    public static String getLevelInfo(ItemStack stack) {
        ChengYueNBT.init(stack);
        
        int level = ChengYueNBT.getLevel(stack);
        long exp = ChengYueNBT.getExp(stack);
        long toNext = ChengYueNBT.getExpToNext(stack);
        int stage = ChengYueNBT.getStage(stack);
        
        float progress = getExpProgress(stack);
        
        return String.format(
            "等级: %d | 阶位: %d (%s)\n经验: %d / %d (%.1f%%)",
            level, stage, getStageName(stage),
            exp, toNext, progress * 100
        );
    }
}
