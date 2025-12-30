package com.moremod.compat.crafttweaker;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 宝石掉落生成器 v3.1 - 世界等级限制版
 *
 * 核心功能：
 * 1. 友善生物过滤
 * 2. 规则系统匹配
 * 3. 宝石生成
 * 4. 维度等级上限
 */
public class GemLootGenerator {

    private static final Random RANDOM = new Random();

    // 基础配置
    public static ItemStack BASE_GEM_ITEM = ItemStack.EMPTY;
    public static int MAX_GEM_LEVEL = 100;
    private static boolean enabled = true;
    private static boolean debugMode = false;

    // ==========================================
    // 维度等级上限系统 (从 ModConfig 读取)
    // ==========================================

    /** 自定义维度等级上限 (用于CT脚本覆盖配置) */
    private static final Map<Integer, Integer> CUSTOM_DIMENSION_LEVEL_CAPS = new HashMap<>();

    /**
     * 设置维度等级上限 (CT脚本用，会覆盖配置文件设置)
     * @param dimensionId 维度ID
     * @param maxLevel 最高等级，-1表示不限制
     */
    public static void setDimensionLevelCap(int dimensionId, int maxLevel) {
        if (maxLevel <= 0) {
            CUSTOM_DIMENSION_LEVEL_CAPS.remove(dimensionId);
        } else {
            CUSTOM_DIMENSION_LEVEL_CAPS.put(dimensionId, maxLevel);
        }
    }

    /**
     * 获取维度的宝石等级上限
     * 优先级: CT自定义 > 配置文件维度列表 > 默认值
     * @return 等级上限，-1表示无限制
     */
    public static int getDimensionLevelCap(int dimensionId) {
        // 优先使用CT自定义
        if (CUSTOM_DIMENSION_LEVEL_CAPS.containsKey(dimensionId)) {
            return CUSTOM_DIMENSION_LEVEL_CAPS.get(dimensionId);
        }
        // 从配置文件读取 (解析维度:等级格式)
        com.moremod.config.ModConfig.GemDimensionLimits cfg =
            com.moremod.config.ModConfig.gemDimension;

        // 解析配置数组查找匹配的维度
        if (cfg.dimensionLevelCaps != null) {
            for (String entry : cfg.dimensionLevelCaps) {
                if (entry == null || entry.isEmpty()) continue;
                try {
                    String[] parts = entry.split(":");
                    if (parts.length == 2) {
                        int dimId = Integer.parseInt(parts[0].trim());
                        int maxLevel = Integer.parseInt(parts[1].trim());
                        if (dimId == dimensionId) {
                            return maxLevel;
                        }
                    }
                } catch (NumberFormatException e) {
                    // 忽略无效的配置项
                    if (debugMode) {
                        System.err.println("[GemLoot] 无效的维度配置: " + entry);
                    }
                }
            }
        }

        // 返回默认值
        return cfg.defaultMaxLevel;
    }

    /**
     * 是否启用维度等级限制
     */
    public static boolean isDimensionLevelCapEnabled() {
        return com.moremod.config.ModConfig.gemDimension.enabled;
    }

    /**
     * 启用/禁用维度等级限制 (兼容旧API，但建议使用配置文件)
     * @deprecated 请使用配置文件设置
     */
    @Deprecated
    public static void setDimensionLevelCapEnabled(boolean enable) {
        // 保留方法以兼容旧CT脚本，但实际设置由配置文件控制
        if (debugMode) {
            System.out.println("[GemLoot] 警告: setDimensionLevelCapEnabled() 已弃用，请使用配置文件");
        }
    }

    /**
     * 清除所有自定义维度等级限制
     */
    public static void clearDimensionLevelCaps() {
        CUSTOM_DIMENSION_LEVEL_CAPS.clear();
    }

    /**
     * 获取所有已配置的维度及其等级上限
     * @return Map<维度ID, 等级上限>
     */
    public static Map<Integer, Integer> getAllDimensionLevelCaps() {
        Map<Integer, Integer> result = new HashMap<>();

        // 添加配置文件中的设置
        com.moremod.config.ModConfig.GemDimensionLimits cfg =
            com.moremod.config.ModConfig.gemDimension;
        if (cfg.dimensionLevelCaps != null) {
            for (String entry : cfg.dimensionLevelCaps) {
                if (entry == null || entry.isEmpty()) continue;
                try {
                    String[] parts = entry.split(":");
                    if (parts.length == 2) {
                        int dimId = Integer.parseInt(parts[0].trim());
                        int maxLevel = Integer.parseInt(parts[1].trim());
                        result.put(dimId, maxLevel);
                    }
                } catch (NumberFormatException e) {
                    // 忽略无效配置
                }
            }
        }

        // CT自定义覆盖配置文件
        result.putAll(CUSTOM_DIMENSION_LEVEL_CAPS);

        return result;
    }

    /**
     * 打印所有维度等级配置 (调试用)
     */
    public static void printDimensionLevelCaps() {
        System.out.println("========== 宝石维度等级限制配置 ==========");
        System.out.println("启用状态: " + isDimensionLevelCapEnabled());
        System.out.println("默认等级上限: " + com.moremod.config.ModConfig.gemDimension.defaultMaxLevel);
        System.out.println("维度配置列表:");
        Map<Integer, Integer> caps = getAllDimensionLevelCaps();
        for (Map.Entry<Integer, Integer> entry : caps.entrySet()) {
            String dimName = getDimensionName(entry.getKey());
            System.out.println("  维度 " + entry.getKey() + " (" + dimName + "): " +
                (entry.getValue() < 0 ? "不限制" : "最高" + entry.getValue() + "级"));
        }
        System.out.println("==========================================");
    }

    /**
     * 获取维度名称 (仅供显示用)
     */
    private static String getDimensionName(int dimensionId) {
        switch (dimensionId) {
            case 0: return "主世界";
            case -1: return "地狱";
            case 1: return "末地";
            case 7: return "暮色森林";
            case 111: return "暮色森林(旧版)";
            case -9999: return "裂缝维度";
            default: return "未知维度";
        }
    }

    // ==========================================
    // 主事件处理
    // ==========================================
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onLivingDrops(LivingDropsEvent event) {
        if (!enabled || BASE_GEM_ITEM.isEmpty()) {
            return;
        }

        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.world;

        // 跳过玩家和客户端
        if (world.isRemote || entity instanceof EntityPlayer) {
            return;
        }

        // 友善生物过滤
        if (isPeacefulCreature(entity)) {
            if (debugMode) {
                System.out.println("[GemLoot] 跳过友善生物: " + entity.getName());
            }
            return;
        }

        // 获取掉落规则
        GemLootRuleManager.LootRule rule = GemLootRuleManager.findRule(entity);

        // 掉落判定
        if (RANDOM.nextFloat() >= rule.dropChance) {
            return;
        }

        // 获取维度等级上限
        int dimensionId = world.provider.getDimension();
        int dimensionCap = isDimensionLevelCapEnabled() ? getDimensionLevelCap(dimensionId) : -1;

        // 生成宝石
        int dropCount = rule.getDropCount(RANDOM);
        for (int i = 0; i < dropCount; i++) {
            int gemLevel = rule.minLevel + RANDOM.nextInt(
                Math.max(1, rule.maxLevel - rule.minLevel + 1)
            );

            // 应用维度等级上限
            if (dimensionCap > 0 && gemLevel > dimensionCap) {
                gemLevel = dimensionCap;
                if (debugMode) {
                    System.out.println(String.format(
                        "[GemLoot] 维度%d等级上限%d，宝石降级",
                        dimensionId, dimensionCap
                    ));
                }
            }

            int affixCount = rule.minAffixes + RANDOM.nextInt(
                Math.max(1, rule.maxAffixes - rule.minAffixes + 1)
            );

            ItemStack gem = GemNBTHelper.createUnidentifiedGemWithQuality(
                BASE_GEM_ITEM,
                gemLevel,
                affixCount,
                rule.minQuality,
                rule.rerollCount
            );

            if (!gem.isEmpty()) {
                event.getDrops().add(new net.minecraft.entity.item.EntityItem(
                    world,
                    entity.posX,
                    entity.posY,
                    entity.posZ,
                    gem
                ));

                if (debugMode) {
                    System.out.println(String.format(
                        "[GemLoot] %s 掉落: Lv%d, %d词条 (维度%d)",
                        entity.getName(), gemLevel, affixCount, dimensionId
                    ));
                }
            }
        }
    }

    // ==========================================
    // Lycanites反射缓存
    // ==========================================

    private static boolean lycanitesChecked = false;
    private static boolean lycanitesAvailable = false;
    private static Class<?> baseCreatureEntityClass;
    private static Class<?> tameableCreatureEntityClass;
    private static Method isTamedMethod;
    private static Method isAggressiveMethod;

    private static void initLycanites() {
        if (lycanitesChecked) return;
        lycanitesChecked = true;

        try {
            baseCreatureEntityClass = Class.forName("com.lycanitesmobs.core.entity.BaseCreatureEntity");
            tameableCreatureEntityClass = Class.forName("com.lycanitesmobs.core.entity.TameableCreatureEntity");
            isTamedMethod = tameableCreatureEntityClass.getMethod("isTamed");
            isAggressiveMethod = baseCreatureEntityClass.getMethod("isAggressive");
            lycanitesAvailable = true;
        } catch (Exception e) {
            lycanitesAvailable = false;
        }
    }

    // ==========================================
    // 友善生物判断（反射版）
    // ==========================================

    private static boolean isPeacefulCreature(EntityLivingBase entity) {
        // 原版动物
        if (entity instanceof EntityAnimal) {
            return true;
        }

        // 驯服生物
        if (entity instanceof EntityTameable && ((EntityTameable) entity).isTamed()) {
            return true;
        }

        // 环境生物
        if (entity instanceof EntityAmbientCreature || entity instanceof EntityWaterMob) {
            return true;
        }

        // Lycanites检测（反射）
        initLycanites();
        if (lycanitesAvailable) {
            try {
                // Lycanites驯服生物
                if (tameableCreatureEntityClass.isInstance(entity)) {
                    boolean isTamed = (Boolean) isTamedMethod.invoke(entity);
                    boolean isAggressive = (Boolean) isAggressiveMethod.invoke(entity);
                    if (isTamed || !isAggressive) {
                        return true;
                    }
                }
                // Lycanites非攻击性生物
                else if (baseCreatureEntityClass.isInstance(entity)) {
                    boolean isAggressive = (Boolean) isAggressiveMethod.invoke(entity);
                    if (!isAggressive) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // 忽略反射异常
            }
        }

        // 村民
        if (entity.getClass().getName().toLowerCase().contains("villager")) {
            return true;
        }

        return false;
    }

    // ==========================================
    // 必要的配置方法
    // ==========================================
    
    public static void setBaseGemItem(ItemStack gemItem) {
        BASE_GEM_ITEM = gemItem.copy();
    }

    public static void setMaxGemLevel(int level) {
        MAX_GEM_LEVEL = Math.max(1, Math.min(level, 100));
    }

    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    public static void setDebugMode(boolean debug) {
        debugMode = debug;
    }
    
    // 保持兼容性（友善生物过滤始终开启）
    public static void setFilterPeaceful(boolean filter) {
        // 友善生物过滤现在是硬编码开启的，这个方法只是为了兼容性
        if (debugMode && !filter) {
            System.out.println("[GemLoot] 警告：友善生物过滤始终开启");
        }
    }
}