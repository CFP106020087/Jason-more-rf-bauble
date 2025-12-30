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

// Phase 1 解耦: Lycanites Mobs 改用反射检测
// 已移除: import com.lycanitesmobs.core.entity.BaseCreatureEntity;
// 已移除: import com.lycanitesmobs.core.entity.TameableCreatureEntity;

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
    // 维度等级上限系统
    // ==========================================

    /** 维度ID -> 宝石等级上限 */
    private static final Map<Integer, Integer> DIMENSION_LEVEL_CAPS = new HashMap<>();

    /** 是否启用维度等级限制 */
    private static boolean dimensionLevelCapEnabled = true;

    static {
        // 默认维度等级上限
        DIMENSION_LEVEL_CAPS.put(0, 30);   // 主世界: 最高30级
        DIMENSION_LEVEL_CAPS.put(-1, 40);  // 地狱: 最高40级
        // 其他维度（末地等）不限制
    }

    /**
     * 设置维度等级上限
     * @param dimensionId 维度ID
     * @param maxLevel 最高等级，-1表示不限制
     */
    public static void setDimensionLevelCap(int dimensionId, int maxLevel) {
        if (maxLevel <= 0) {
            DIMENSION_LEVEL_CAPS.remove(dimensionId);
        } else {
            DIMENSION_LEVEL_CAPS.put(dimensionId, maxLevel);
        }
    }

    /**
     * 获取维度的宝石等级上限
     * @return 等级上限，-1表示无限制
     */
    public static int getDimensionLevelCap(int dimensionId) {
        return DIMENSION_LEVEL_CAPS.getOrDefault(dimensionId, -1);
    }

    /**
     * 启用/禁用维度等级限制
     */
    public static void setDimensionLevelCapEnabled(boolean enable) {
        dimensionLevelCapEnabled = enable;
    }

    /**
     * 清除所有维度等级限制
     */
    public static void clearDimensionLevelCaps() {
        DIMENSION_LEVEL_CAPS.clear();
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
        int dimensionCap = dimensionLevelCapEnabled ? getDimensionLevelCap(dimensionId) : -1;

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
    // 友善生物判断（精简版）
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
        
        // Phase 1 解耦: Lycanites生物检测改用反射
        if (isLycanitesFriendlyEntity(entity)) {
            return true;
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

    // ==========================================
    // Phase 1 解耦: Lycanites反射辅助方法
    // ==========================================

    private static Class<?> tameableCreatureClass = null;
    private static Class<?> baseCreatureClass = null;
    private static java.lang.reflect.Method isTamedMethod = null;
    private static java.lang.reflect.Method isAggressiveMethod = null;
    private static boolean lycanitesChecked = false;

    /**
     * 使用反射检测Lycanites友善生物
     * 当Lycanites Mobs不存在时安全返回false
     */
    private static boolean isLycanitesFriendlyEntity(EntityLivingBase entity) {
        if (!lycanitesChecked) {
            lycanitesChecked = true;
            try {
                tameableCreatureClass = Class.forName("com.lycanitesmobs.core.entity.TameableCreatureEntity");
                baseCreatureClass = Class.forName("com.lycanitesmobs.core.entity.BaseCreatureEntity");
                isTamedMethod = tameableCreatureClass.getMethod("isTamed");
                isAggressiveMethod = baseCreatureClass.getMethod("isAggressive");
            } catch (Exception e) {
                // Lycanites Mobs不存在，静默失败
                tameableCreatureClass = null;
                baseCreatureClass = null;
            }
        }

        if (tameableCreatureClass == null) return false;

        try {
            // 检查驯服生物
            if (tameableCreatureClass.isInstance(entity)) {
                boolean isTamed = (Boolean) isTamedMethod.invoke(entity);
                boolean isAggressive = (Boolean) isAggressiveMethod.invoke(entity);
                if (isTamed || !isAggressive) return true;
            }
            // 检查非攻击性生物
            else if (baseCreatureClass.isInstance(entity)) {
                boolean isAggressive = (Boolean) isAggressiveMethod.invoke(entity);
                if (!isAggressive) return true;
            }
        } catch (Exception e) {
            // 反射调用失败，静默失败
        }
        return false;
    }
}