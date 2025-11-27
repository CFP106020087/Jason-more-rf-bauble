package com.moremod.compat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

/**
 * 特殊Mob检测器（Champions & Infernal Mobs兼容）
 * 
 * 基于Champions mod实际源码实现：
 * - IChampionship接口（不是IChampion）
 * - CapabilityChampionship.getChampionship()静态方法
 * - ChampionHelper验证方法
 * 
 * Infernal Mobs检测基于NBT标签系统
 */
public class SpecialMobDetector {

    // ==========================================
    // Champions Mod 兼容（软依赖）
    // ==========================================
    
    /**
     * Champions的IChampionship能力注入
     * 如果Champions未安装，此字段为null，不会导致崩溃
     */
    @CapabilityInject(IChampionship.class)
    private static Capability<IChampionship> CHAMPIONSHIP_CAP = null;
    
    /**
     * IChampionship接口占位符
     * 实际接口来自Champions mod: c4.champions.common.capability.IChampionship
     * 
     * 使用@CapabilityInject时不需要真正导入类，编译时会被替换
     */
    private interface IChampionship {
        // 占位符接口，实际使用Champions的IChampionship
        IRank getRank();
    }
    
    private interface IRank {
        int getTier();
    }

    /**
     * 检测实体是否为Champions精英怪
     * 
     * @param entity 要检测的实体
     * @return 如果是Champions实体返回true
     */
    public static boolean isChampion(EntityLivingBase entity) {
        if (CHAMPIONSHIP_CAP == null) {
            // Champions未安装
            return false;
        }
        
        try {
            // 检查实体是否有Championship能力
            if (entity.hasCapability(CHAMPIONSHIP_CAP, null)) {
                IChampionship championship = entity.getCapability(CHAMPIONSHIP_CAP, null);
                
                // 验证能力数据有效且是精英等级
                if (championship != null && championship.getRank() != null) {
                    // Champions源码中：getRank()返回null表示普通怪物
                    // 非null表示是Champion
                    return true;
                }
            }
        } catch (Exception e) {
            // 静默处理异常，避免模组冲突导致崩溃
            System.err.println("[GemLoot] Champions检测异常: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * 获取Champion等级（0-4）
     * 
     * @param entity Champion实体
     * @return 等级值，0表示非Champion
     */
    public static int getChampionTier(EntityLivingBase entity) {
        if (CHAMPIONSHIP_CAP == null) {
            return 0;
        }
        
        try {
            if (entity.hasCapability(CHAMPIONSHIP_CAP, null)) {
                IChampionship championship = entity.getCapability(CHAMPIONSHIP_CAP, null);
                
                if (championship != null && championship.getRank() != null) {
                    return championship.getRank().getTier();
                }
            }
        } catch (Exception e) {
            System.err.println("[GemLoot] Champion等级获取异常: " + e.getMessage());
        }
        
        return 0;
    }

    // ==========================================
    // Infernal Mobs 兼容（基于NBT检测）
    // ==========================================

    /**
     * 检测实体是否为Infernal Mob
     * 
     * Infernal Mobs通过NBT标签系统标记精英怪：
     * - "InfernalMobsMod" = "true" 主要标记
     * - "InfernalMobsName" = "修饰词 修饰词 实体名" 名称标记
     * 
     * @param entity 要检测的实体
     * @return 如果是Infernal Mob返回true
     */
    public static boolean isInfernalMob(EntityLivingBase entity) {
        try {
            NBTTagCompound nbt = entity.getEntityData();
            
            // 方法1：直接NBT标签检测（最可靠）
            if (nbt.hasKey("InfernalMobsMod") || nbt.hasKey("InfernalMobsName")) {
                return true;
            }
            
            // 方法2：自定义名称模式检测（备用）
            if (entity.hasCustomName()) {
                String customName = entity.getCustomNameTag();
                
                // Infernal名称包含颜色代码和多个修饰词
                // 例如: "§cPoisonous §6Webber §fZombie"
                if (customName.contains("§")) {
                    // 移除颜色代码后检查单词数
                    String cleanName = customName.replaceAll("§.", "");
                    String[] parts = cleanName.trim().split("\\s+");
                    
                    // 至少2个修饰词 + 实体名 = 3个单词以上
                    if (parts.length >= 3) {
                        return true;
                    }
                }
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("[GemLoot] Infernal检测异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取Infernal Mob等级（1=精英, 2=超级, 3=地狱）
     * 
     * @param entity Infernal实体
     * @return 等级值，0表示非Infernal
     */
    public static int getInfernalTier(EntityLivingBase entity) {
        if (!isInfernalMob(entity)) {
            return 0;
        }
        
        try {
            NBTTagCompound nbt = entity.getEntityData();
            String infernalName = nbt.getString("InfernalMobsName");
            
            // 从名称中计算修饰词数量
            int modifierCount = countInfernalModifiers(infernalName);
            
            // Infernal Mobs等级规则：
            // 2-4 修饰词 = 精英 (Elite)
            // 5-9 修饰词 = 超级 (Ultra)
            // 10+ 修饰词 = 地狱 (Infernal)
            if (modifierCount >= 10) return 3;
            if (modifierCount >= 5) return 2;
            if (modifierCount >= 2) return 1;
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("[GemLoot] Infernal等级获取异常: " + e.getMessage());
            return 1; // 默认返回精英等级
        }
    }

    /**
     * 从Infernal名称中计算修饰词数量
     * 
     * @param infernalName Infernal Mob的完整名称
     * @return 修饰词数量
     */
    private static int countInfernalModifiers(String infernalName) {
        if (infernalName == null || infernalName.isEmpty()) {
            return 0;
        }
        
        // 移除颜色代码
        String cleanName = infernalName.replaceAll("§.", "");
        
        // 分割单词（修饰词 + 实体名）
        String[] parts = cleanName.trim().split("\\s+");
        
        // 修饰词数 = 总单词数 - 1（减去实体名）
        return Math.max(0, parts.length - 1);
    }

    // ==========================================
    // 统一接口
    // ==========================================

    /**
     * 检测实体是否为任何特殊Mob（Champion或Infernal）
     * 
     * @param entity 要检测的实体
     * @return 如果是特殊mob返回true
     */
    public static boolean isSpecialMob(EntityLivingBase entity) {
        return isChampion(entity) || isInfernalMob(entity);
    }

    /**
     * 获取特殊Mob的等级（用于战利品调整）
     * 
     * @param entity 特殊mob实体
     * @return 等级值（0=普通, 1-4=各种精英等级）
     */
    public static int getSpecialTier(EntityLivingBase entity) {
        // 优先检查Champion（通常等级更高）
        int championTier = getChampionTier(entity);
        if (championTier > 0) {
            return championTier;
        }
        
        // 检查Infernal
        int infernalTier = getInfernalTier(entity);
        if (infernalTier > 0) {
            return infernalTier;
        }
        
        return 0;
    }

    /**
     * 获取特殊Mob的类型名称（调试用）
     * 
     * @param entity 实体
     * @return 类型名称字符串
     */
    public static String getSpecialMobType(EntityLivingBase entity) {
        if (isChampion(entity)) {
            int tier = getChampionTier(entity);
            return "Champion(T" + tier + ")";
        }
        
        if (isInfernalMob(entity)) {
            int tier = getInfernalTier(entity);
            String[] names = {"", "Elite", "Ultra", "Infernal"};
            return "Infernal(" + (tier < names.length ? names[tier] : "T" + tier) + ")";
        }
        
        return "Normal";
    }

    /**
     * 检查Champions mod是否已加载
     * 
     * @return true如果Champions已安装
     */
    public static boolean isChampionsLoaded() {
        return CHAMPIONSHIP_CAP != null;
    }
}