package com.moremod.compat.crafttweaker;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityAmbientCreature;  // 蝙蝠等
import net.minecraft.entity.passive.EntityWaterMob;         // 鱿鱼等
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

// Lycanites Mobs 接口 (1.12.2)
import com.lycanitesmobs.core.entity.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.TameableCreatureEntity;

import java.util.Random;

/**
 * 宝石掉落生成器 v2.4 - 完整版
 * 
 * 核心特性：
 * 1. ✅ 使用HIGH优先级，在Infernal之前执行
 * 2. ✅ Ice and Fire优先检查，确保龙正常掉落
 * 3. ✅ 过滤友善生物（动物、环境生物、驯服宠物）
 * 4. ✅ 使用GemNBTHelper创建带属性的宝石
 * 5. ✅ 集成GemLootRuleManager规则系统
 * 6. ✅ Lycanites接口判断（1.12.2）
 * 
 * 过滤规则（按优先级）：
 * 0. Ice and Fire野生生物 → ✅ 掉落
 * 1. 原版动物（猪牛羊鸡） → ❌ 不掉
 * 2. 原版驯服宠物（狼猫马） → ❌ 不掉
 * 3. 环境生物（蝙蝠、鱿鱼） → ❌ 不掉
 * 4. Lycanites友善/驯服生物 → ❌ 不掉
 * 5. 村民 → ❌ 不掉
 * 6. 其他敌对生物 → ✅ 掉落
 */
public class GemLootGenerator {

    private static final Random RANDOM = new Random();

    // ==========================================
    // 公共配置
    // ==========================================
    
    public static boolean FILTER_PEACEFUL = true;      // 友善生物过滤
    public static int MAX_GEM_LEVEL = 80;              // 宝石等级上限
    public static boolean HEALTH_BALANCE = true;       // 血量平衡
    public static ItemStack BASE_GEM_ITEM = ItemStack.EMPTY;  // 基础宝石物品
    private static boolean debugMode = false;          // 调试模式
    private static boolean enabled = true;             // 系统开关

    // ==========================================
    // 战利品事件处理（HIGH优先级）
    // ==========================================

    /**
     * HIGH优先级确保在Infernal Mobs（NORMAL）之前执行
     */
    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = false)
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

        // ==========================================
        // ⭐ v2.3宽松策略：友善生物过滤
        // ==========================================
        if (FILTER_PEACEFUL && isPeacefulCreature(entity)) {
            if (debugMode) {
                System.out.println(String.format(
                        "[GemLoot-v2.3] ❌ 友善生物不掉落: %s (%.1f血)",
                        entity.getName(),
                        entity.getMaxHealth()
                ));
            }
            return; // 友善生物不掉落
        }

        // ==========================================
        // 使用规则系统匹配掉落
        // ==========================================
        GemLootRuleManager.LootRule rule = GemLootRuleManager.findRule(entity);

        if (debugMode) {
            NBTTagCompound nbt = entity.getEntityData();
            String infernalTag = nbt.getString("InfernalMobsMod");
            
            System.out.println(String.format(
                    "[GemLoot-Debug] 实体: %s | 类: %s | 血量: %.1f | Infernal: %s",
                    entity.getName(),
                    entity.getClass().getSimpleName(),
                    entity.getMaxHealth(),
                    infernalTag.isEmpty() ? "无" : infernalTag
            ));
            System.out.println(String.format(
                    "[GemLoot-Debug] 匹配规则: %s | 掉落率: %.2f%%",
                    rule.id,
                    rule.dropChance * 100
            ));
        }

        // ==========================================
        // 掉落判定
        // ==========================================
        float roll = RANDOM.nextFloat();
        if (roll >= rule.dropChance) {
            if (debugMode) {
                System.out.println(String.format(
                        "[GemLoot] ❌ 掉落判定失败: %.2f%% < %.2f%%",
                        roll * 100,
                        rule.dropChance * 100
                ));
            }
            return;
        }

        // ==========================================
        // 生成宝石（使用GemNBTHelper）
        // ==========================================
        int dropCount = rule.getDropCount(RANDOM);
        
        for (int i = 0; i < dropCount; i++) {
            // 随机等级
            int gemLevel = rule.minLevel + RANDOM.nextInt(
                    Math.max(1, rule.maxLevel - rule.minLevel + 1)
            );

            // 随机词条数
            int affixCount = rule.minAffixes + RANDOM.nextInt(
                    Math.max(1, rule.maxAffixes - rule.minAffixes + 1)
            );

            // ⭐ 使用GemNBTHelper创建带属性的宝石
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
                            "[GemLoot] ✅ %s 掉落宝石: Lv%d, %d词条, 品质≥%.0f%%, roll×%d",
                            entity.getName(),
                            gemLevel,
                            affixCount,
                            rule.minQuality * 100,
                            rule.rerollCount
                    ));
                }
            }
        }
    }

    // ==========================================
    // ⭐ v2.3宽松策略：友善生物判断
    // ==========================================

    /**
     * 判断是否为友善生物（需要排除）
     * 
     * 检查顺序（重要！）：
     * 0. ⭐ Ice and Fire - 优先检查，带血量门槛
     * 1. EntityAnimal - 原版动物
     * 2. EntityTameable已驯服 - 原版驯服宠物
     * 3. 环境生物 - 蝙蝠、鱿鱼等
     * 4. TameableCreatureEntity - Lycanites可驯服生物
     * 5. BaseCreatureEntity - Lycanites基础生物
     * 6. 村民
     */
    private static boolean isPeacefulCreature(EntityLivingBase entity) {
        String className = entity.getClass().getName();
        String classNameLower = className.toLowerCase();
        
        // ==========================================
        // ⭐ 步骤0：Ice and Fire - 最优先检查
        // ==========================================
        if (classNameLower.contains("iceandfire")) {
            // 驯服的不掉
            if (entity instanceof EntityTameable) {
                EntityTameable tameable = (EntityTameable) entity;
                if (tameable.isTamed()) {
                    if (debugMode) {
                        System.out.println("[GemLoot-Exclude] Ice and Fire驯服生物: " + entity.getName());
                    }
                    return true;
                }
            }
            
            // 野生Ice and Fire生物 → 进入规则匹配
            // ⭐ 血量门槛由GemLootRuleManager处理（200血）
            if (debugMode) {
                System.out.println(String.format(
                        "[GemLoot-Include] 野生Ice and Fire: %s (%.1f血)",
                        entity.getName(),
                        entity.getMaxHealth()
                ));
            }
            return false;
        }
        
        // ==========================================
        // 步骤1：EntityAnimal - 原版动物
        // ==========================================
        if (entity instanceof EntityAnimal) {
            if (debugMode) {
                System.out.println("[GemLoot-Exclude] EntityAnimal: " + entity.getName());
            }
            return true;
        }
        
        // ==========================================
        // 步骤2：EntityTameable已驯服 - 原版驯服生物
        // ==========================================
        if (entity instanceof EntityTameable) {
            EntityTameable tameable = (EntityTameable) entity;
            if (tameable.isTamed()) {
                if (debugMode) {
                    System.out.println("[GemLoot-Exclude] 已驯服: " + entity.getName());
                }
                return true;
            }
        }
        
        // ==========================================
        // 步骤3：环境生物 - 蝙蝠、鱿鱼等
        // ==========================================
        
        // 3.1 EntityAmbientCreature - 环境生物（蝙蝠）
        if (entity instanceof EntityAmbientCreature) {
            if (debugMode) {
                System.out.println("[GemLoot-Exclude] 环境生物: " + entity.getName());
            }
            return true;
        }
        
        // 3.2 EntityWaterMob - 水生生物（鱿鱼）
        if (entity instanceof EntityWaterMob) {
            if (debugMode) {
                System.out.println("[GemLoot-Exclude] 水生生物: " + entity.getName());
            }
            return true;
        }
        
        // 3.3 通过类名判断（兜底）
        if (classNameLower.contains("squid") || 
            classNameLower.contains("bat") ||
            classNameLower.contains("parrot")) {
            if (debugMode) {
                System.out.println("[GemLoot-Exclude] 环境生物（类名匹配）: " + entity.getName());
            }
            return true;
        }
        
        // ==========================================
        // 步骤4：Lycanites - TameableCreatureEntity
        // ==========================================
        if (entity instanceof TameableCreatureEntity) {
            TameableCreatureEntity tameable = (TameableCreatureEntity) entity;
            
            // 已驯服
            if (tameable.isTamed()) {
                if (debugMode) {
                    System.out.println("[GemLoot-Exclude] Lycanites宠物: " + entity.getName());
                }
                return true;
            }
            
            // 非攻击性
            if (!tameable.isAggressive()) {
                if (debugMode) {
                    System.out.println("[GemLoot-Exclude] Lycanites非攻击性: " + entity.getName());
                }
                return true;
            }
            
            // 野生攻击性 → 掉落
            if (debugMode) {
                System.out.println("[GemLoot-Include] Lycanites攻击性: " + entity.getName());
            }
            return false;
        }
        
        // ==========================================
        // 步骤5：Lycanites - BaseCreatureEntity
        // ==========================================
        if (entity instanceof BaseCreatureEntity) {
            BaseCreatureEntity creature = (BaseCreatureEntity) entity;
            
            // 非攻击性
            if (!creature.isAggressive()) {
                if (debugMode) {
                    System.out.println("[GemLoot-Exclude] Lycanites非攻击性: " + entity.getName());
                }
                return true;
            }
            
            // 攻击性 → 掉落
            if (debugMode) {
                System.out.println("[GemLoot-Include] Lycanites攻击性: " + entity.getName());
            }
            return false;
        }
        
        // ==========================================
        // 步骤6：村民
        // ==========================================
        if (classNameLower.contains("villager")) {
            if (debugMode) {
                System.out.println("[GemLoot-Exclude] 村民: " + entity.getName());
            }
            return true;
        }
        
        // ==========================================
        // 默认：掉落
        // ==========================================
        if (debugMode) {
            System.out.println("[GemLoot-Include] 默认掉落: " + entity.getName());
        }
        return false;
    }

    // ==========================================
    // 配置API
    // ==========================================

    public static void setBaseGemItem(ItemStack gemItem) {
        BASE_GEM_ITEM = gemItem.copy();
        System.out.println("[GemLoot-v2.3] 基础宝石物品已设置: " + gemItem.getDisplayName());
    }

    public static void setFilterPeaceful(boolean filter) {
        FILTER_PEACEFUL = filter;
        System.out.println("[GemLoot-v2.3] 友善生物过滤: " + (filter ? "开启" : "关闭"));
    }

    public static void setMaxGemLevel(int level) {
        MAX_GEM_LEVEL = Math.max(1, Math.min(level, 100));
        System.out.println("[GemLoot-v2.3] 宝石等级上限: " + MAX_GEM_LEVEL);
    }

    public static void setHealthBalance(boolean balance) {
        HEALTH_BALANCE = balance;
        System.out.println("[GemLoot-v2.3] 血量平衡: " + (balance ? "开启" : "关闭"));
    }

    public static void setDebugMode(boolean debug) {
        debugMode = debug;
        System.out.println("[GemLoot-v2.3] 调试模式: " + (debug ? "开启" : "关闭"));
    }

    public static void setEnabled(boolean enable) {
        enabled = enable;
        System.out.println("[GemLoot-v2.3] 宝石掉落系统: " + (enable ? "开启" : "关闭"));
    }

    public static void applyRecommendedSettings() {
        setFilterPeaceful(true);
        setMaxGemLevel(80);
        setHealthBalance(true);
        setDebugMode(false);
        System.out.println("[GemLoot-v2.3] ✅ 已应用推荐配置");
    }

    public static void applyLenientSettings() {
        setFilterPeaceful(true);
        setMaxGemLevel(100);
        setHealthBalance(false);
        setDebugMode(false);
        System.out.println("[GemLoot-v2.3] ✅ 已应用宽松配置");
    }

    public static void applyStrictSettings() {
        setFilterPeaceful(true);
        setMaxGemLevel(60);
        setHealthBalance(true);
        setDebugMode(false);
        System.out.println("[GemLoot-v2.3] ✅ 已应用严格配置");
    }

    // ==========================================
    // 手动生成方法（保持兼容）
    // ==========================================

    public static ItemStack generateGemWithRule(GemLootRuleManager.LootRule rule) {
        if (BASE_GEM_ITEM.isEmpty()) return ItemStack.EMPTY;

        int gemLevel = rule.minLevel + RANDOM.nextInt(
                Math.max(1, rule.maxLevel - rule.minLevel + 1)
        );
        int affixCount = rule.minAffixes + RANDOM.nextInt(
                Math.max(1, rule.maxAffixes - rule.minAffixes + 1)
        );

        return GemNBTHelper.createUnidentifiedGemWithQuality(
                BASE_GEM_ITEM,
                gemLevel,
                affixCount,
                rule.minQuality,
                rule.rerollCount
        );
    }

    public static ItemStack generateChestGem(int chestTier) {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "chest_tier_" + chestTier,
                chestTier * 10,
                chestTier * 10 + 10,
                2, 4,
                1.0f,
                0.3f,
                2
        );
        return generateGemWithRule(rule);
    }

    public static ItemStack generateQuestReward(int level, int minAffixes, int maxAffixes) {
        GemLootRuleManager.LootRule rule = new GemLootRuleManager.LootRule(
                "quest_reward",
                level,
                level,
                minAffixes, maxAffixes,
                1.0f,
                0.5f,
                3
        );
        return generateGemWithRule(rule);
    }
}