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

// Lycanites Mobs
import com.lycanitesmobs.core.entity.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.TameableCreatureEntity;

import java.util.Random;

/**
 * 宝石掉落生成器 v3.0 - 精简版
 * 
 * 核心功能：
 * 1. 友善生物过滤
 * 2. 规则系统匹配
 * 3. 宝石生成
 */
public class GemLootGenerator {

    private static final Random RANDOM = new Random();
    
    // 基础配置
    public static ItemStack BASE_GEM_ITEM = ItemStack.EMPTY;
    public static int MAX_GEM_LEVEL = 100;
    private static boolean enabled = true;
    private static boolean debugMode = false;

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

        // 生成宝石
        int dropCount = rule.getDropCount(RANDOM);
        for (int i = 0; i < dropCount; i++) {
            int gemLevel = rule.minLevel + RANDOM.nextInt(
                Math.max(1, rule.maxLevel - rule.minLevel + 1)
            );
            
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
                        "[GemLoot] %s 掉落: Lv%d, %d词条",
                        entity.getName(), gemLevel, affixCount
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
        
        // Lycanites驯服生物
        if (entity instanceof TameableCreatureEntity) {
            TameableCreatureEntity tameable = (TameableCreatureEntity) entity;
            if (tameable.isTamed() || !tameable.isAggressive()) {
                return true;
            }
        }
        
        // Lycanites非攻击性生物
        if (entity instanceof BaseCreatureEntity) {
            BaseCreatureEntity creature = (BaseCreatureEntity) entity;
            if (!creature.isAggressive()) {
                return true;
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