package com.moremod.ritual;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * 旧仪式系统配置
 * 存储 TileEntityRitualCore 中硬编码仪式的可配置参数
 *
 * 支持通过 CraftTweaker 修改：
 * - 持续时间
 * - 失败率
 * - 能量消耗
 * - 基座材料
 * - 禁用/启用
 */
public class LegacyRitualConfig {

    // 仪式ID常量
    public static final String CURSE_PURIFICATION = "curse_purification";
    public static final String ENCHANT_TRANSFER = "enchant_transfer";
    public static final String ENCHANT_INFUSION = "enchant_infusion";
    public static final String CURSE_CREATION = "curse_creation";
    public static final String WEAPON_EXP_BOOST = "weapon_exp_boost";
    public static final String MURAMASA_BOOST = "muramasa_boost";
    public static final String FABRIC_ENHANCE = "fabric_enhance";
    public static final String SOUL_BINDING = "soul_binding";
    public static final String DUPLICATION = "duplication";
    public static final String EMBEDDING = "embedding";
    public static final String UNBREAKABLE = "unbreakable";
    public static final String SOULBOUND = "soulbound";

    // 配置存储
    private static final Map<String, RitualParams> OVERRIDES = new HashMap<>();
    private static final Set<String> DISABLED = new HashSet<>();
    private static final Set<String> CLEARED_MATERIALS = new HashSet<>(); // 标记已清除默认材料的仪式

    // ==================== 默认值 ====================

    private static final Map<String, RitualParams> DEFAULTS = new HashMap<>();

    static {
        // 初始化默认值（与 TileEntityRitualCore 中的硬编码值一致）
        DEFAULTS.put(CURSE_PURIFICATION, new RitualParams(200, 0.0f, 150000, 2));
        DEFAULTS.put(ENCHANT_TRANSFER, new RitualParams(300, 0.10f, 200000, 3));
        DEFAULTS.put(ENCHANT_INFUSION, new RitualParams(200, 0.90f, 100000, 3));
        DEFAULTS.put(CURSE_CREATION, new RitualParams(200, 0.0f, 100000, 2));
        DEFAULTS.put(WEAPON_EXP_BOOST, new RitualParams(150, 0.0f, 100000, 2));
        DEFAULTS.put(MURAMASA_BOOST, new RitualParams(150, 0.0f, 100000, 2));
        DEFAULTS.put(FABRIC_ENHANCE, new RitualParams(200, 0.0f, 120000, 2));
        DEFAULTS.put(SOUL_BINDING, new RitualParams(400, 0.50f, 200000, 3));
        DEFAULTS.put(DUPLICATION, new RitualParams(300, 0.99f, 250000, 3));
        DEFAULTS.put(EMBEDDING, new RitualParams(100, 0.0f, 0, 3));
        DEFAULTS.put(UNBREAKABLE, new RitualParams(400, 0.20f, 300000, 3));
        DEFAULTS.put(SOULBOUND, new RitualParams(300, 0.10f, 200000, 3));
    }

    // ==================== 查询方法 ====================

    /**
     * 获取仪式持续时间
     */
    public static int getDuration(String ritualId) {
        RitualParams override = OVERRIDES.get(ritualId.toLowerCase(Locale.ROOT));
        if (override != null && override.duration != null) {
            return override.duration;
        }
        RitualParams def = DEFAULTS.get(ritualId.toLowerCase(Locale.ROOT));
        return def != null ? def.duration : 200;
    }

    /**
     * 获取仪式失败率
     */
    public static float getFailChance(String ritualId) {
        RitualParams override = OVERRIDES.get(ritualId.toLowerCase(Locale.ROOT));
        if (override != null && override.failChance != null) {
            return override.failChance;
        }
        RitualParams def = DEFAULTS.get(ritualId.toLowerCase(Locale.ROOT));
        return def != null ? def.failChance : 0.0f;
    }

    /**
     * 获取仪式能量消耗（每基座）
     */
    public static int getEnergyPerPedestal(String ritualId) {
        RitualParams override = OVERRIDES.get(ritualId.toLowerCase(Locale.ROOT));
        if (override != null && override.energyPerPedestal != null) {
            return override.energyPerPedestal;
        }
        RitualParams def = DEFAULTS.get(ritualId.toLowerCase(Locale.ROOT));
        return def != null ? def.energyPerPedestal : 100000;
    }

    /**
     * 计算能量超载加成
     * @param ritualId 仪式ID
     * @param pedestalCount 基座数量
     * @param totalEnergy 总能量
     * @return 超载加成 (0.0 - 0.5)，最多50%额外成功率
     */
    public static float getOverloadBonus(String ritualId, int pedestalCount, int totalEnergy) {
        int energyPerPedestal = getEnergyPerPedestal(ritualId);
        int requiredEnergy = energyPerPedestal * pedestalCount;
        if (requiredEnergy <= 0) return 0;

        float energyRatio = (float) totalEnergy / requiredEnergy;
        if (energyRatio <= 1.0f) return 0;

        // 每超过100%能量，给予10%成功率加成，最多50%
        float overloadPercent = (energyRatio - 1.0f);
        return Math.min(0.5f, overloadPercent * 0.1f);
    }

    /**
     * 获取能量超载调整后的失败率
     * @param ritualId 仪式ID
     * @param pedestalCount 基座数量
     * @param totalEnergy 总能量
     * @return 调整后的失败率
     */
    public static float getOverloadAdjustedFailChance(String ritualId, int pedestalCount, int totalEnergy) {
        float baseFailChance = getFailChance(ritualId);
        float overloadBonus = getOverloadBonus(ritualId, pedestalCount, totalEnergy);
        return Math.max(0, baseFailChance - overloadBonus);
    }

    /**
     * 获取仪式所需阶层
     */
    public static int getRequiredTier(String ritualId) {
        RitualParams override = OVERRIDES.get(ritualId.toLowerCase(Locale.ROOT));
        if (override != null && override.requiredTier != null) {
            return override.requiredTier;
        }
        RitualParams def = DEFAULTS.get(ritualId.toLowerCase(Locale.ROOT));
        return def != null ? def.requiredTier : 1;
    }

    /**
     * 获取自定义基座材料（如果有）
     */
    @Nullable
    public static List<ItemStack> getCustomPedestalItems(String ritualId) {
        RitualParams override = OVERRIDES.get(ritualId.toLowerCase(Locale.ROOT));
        if (override != null && override.pedestalItems != null) {
            return new ArrayList<>(override.pedestalItems);
        }
        return null;
    }

    /**
     * 检查仪式是否启用
     */
    public static boolean isEnabled(String ritualId) {
        return !DISABLED.contains(ritualId.toLowerCase(Locale.ROOT));
    }

    // ==================== 设置方法（CraftTweaker 调用）====================

    /**
     * 设置仪式持续时间
     */
    public static void setDuration(String ritualId, int duration) {
        getOrCreateOverride(ritualId).duration = duration;
        log("Set " + ritualId + " duration to " + duration + " ticks");
    }

    /**
     * 设置仪式失败率
     */
    public static void setFailChance(String ritualId, float chance) {
        getOrCreateOverride(ritualId).failChance = Math.max(0, Math.min(1, chance));
        log("Set " + ritualId + " fail chance to " + (chance * 100) + "%");
    }

    /**
     * 设置仪式能量消耗
     */
    public static void setEnergyPerPedestal(String ritualId, int energy) {
        getOrCreateOverride(ritualId).energyPerPedestal = Math.max(0, energy);
        log("Set " + ritualId + " energy to " + energy + " RF/pedestal");
    }

    /**
     * 设置仪式所需阶层
     */
    public static void setRequiredTier(String ritualId, int tier) {
        getOrCreateOverride(ritualId).requiredTier = Math.max(1, Math.min(3, tier));
        log("Set " + ritualId + " required tier to " + tier);
    }

    /**
     * 设置自定义基座材料
     */
    public static void setPedestalItems(String ritualId, List<ItemStack> items) {
        getOrCreateOverride(ritualId).pedestalItems = new ArrayList<>(items);
        // 设置材料后也标记为已修改（即使是空列表）
        CLEARED_MATERIALS.add(ritualId.toLowerCase(Locale.ROOT));
        log("Set " + ritualId + " pedestal items (" + items.size() + " items)");
    }

    /**
     * 清除仪式的默认材料配置
     * 清除后，该仪式将不检查材料（可以无材料执行）
     * 或者之后使用 setPedestalItems 设置新材料
     */
    public static void clearMaterials(String ritualId) {
        String id = ritualId.toLowerCase(Locale.ROOT);
        getOrCreateOverride(id).pedestalItems = new ArrayList<>(); // 空列表
        CLEARED_MATERIALS.add(id);
        log("Cleared " + ritualId + " materials (default recipe removed)");
    }

    /**
     * 禁用仪式
     */
    public static void disable(String ritualId) {
        DISABLED.add(ritualId.toLowerCase(Locale.ROOT));
        log("Disabled ritual: " + ritualId);
    }

    /**
     * 启用仪式
     */
    public static void enable(String ritualId) {
        DISABLED.remove(ritualId.toLowerCase(Locale.ROOT));
        log("Enabled ritual: " + ritualId);
    }

    /**
     * 重置仪式到默认值
     */
    public static void reset(String ritualId) {
        String id = ritualId.toLowerCase(Locale.ROOT);
        OVERRIDES.remove(id);
        DISABLED.remove(id);
        CLEARED_MATERIALS.remove(id);
        log("Reset ritual to default: " + ritualId);
    }

    /**
     * 重置所有仪式
     */
    public static void resetAll() {
        OVERRIDES.clear();
        DISABLED.clear();
        CLEARED_MATERIALS.clear();
        log("Reset all rituals to default");
    }

    // ==================== 工具方法 ====================

    private static RitualParams getOrCreateOverride(String ritualId) {
        return OVERRIDES.computeIfAbsent(ritualId.toLowerCase(Locale.ROOT), k -> new RitualParams());
    }

    private static void log(String msg) {
        System.out.println("[moremod] LegacyRitualConfig: " + msg);
    }

    /**
     * 打印所有仪式配置
     */
    public static void printAll() {
        System.out.println("=== Legacy Ritual Configuration ===");
        for (String id : DEFAULTS.keySet()) {
            String status = isEnabled(id) ? "ENABLED" : "DISABLED";
            boolean hasOverride = OVERRIDES.containsKey(id);
            System.out.println(String.format("  [%s] %s: %d ticks, %.0f%% fail, %d RF, Tier %d %s",
                    status, id,
                    getDuration(id),
                    getFailChance(id) * 100,
                    getEnergyPerPedestal(id),
                    getRequiredTier(id),
                    hasOverride ? "(MODIFIED)" : ""));
        }
        System.out.println("===================================");
    }

    /**
     * 获取所有仪式ID
     */
    public static Set<String> getAllRitualIds() {
        return Collections.unmodifiableSet(DEFAULTS.keySet());
    }

    // ==================== 参数类 ====================

    public static class RitualParams {
        public Integer duration;
        public Float failChance;
        public Integer energyPerPedestal;
        public Integer requiredTier;
        public List<ItemStack> pedestalItems;

        public RitualParams() {}

        public RitualParams(int duration, float failChance, int energyPerPedestal, int requiredTier) {
            this.duration = duration;
            this.failChance = failChance;
            this.energyPerPedestal = energyPerPedestal;
            this.requiredTier = requiredTier;
        }
    }

    // ==================== 材料需求系统 ====================

    /**
     * 材料需求定义
     * 支持精确匹配（物品+数量）或谓词匹配（任意满足条件的物品）
     */
    public static class MaterialRequirement {
        private final ItemStack exactItem;      // 精确匹配的物品
        private final int count;                // 所需数量
        private final Predicate<ItemStack> matcher; // 自定义匹配器
        private final String description;       // 描述（用于显示）

        // 精确匹配构造器
        public MaterialRequirement(Item item, int count) {
            this.exactItem = new ItemStack(item);
            this.count = count;
            this.matcher = null;
            this.description = item.getRegistryName() + " x" + count;
        }

        // 带meta的精确匹配构造器
        public MaterialRequirement(Item item, int meta, int count) {
            this.exactItem = new ItemStack(item, 1, meta);
            this.count = count;
            this.matcher = null;
            this.description = item.getRegistryName() + ":" + meta + " x" + count;
        }

        // 谓词匹配构造器
        public MaterialRequirement(Predicate<ItemStack> matcher, int count, String description) {
            this.exactItem = null;
            this.count = count;
            this.matcher = matcher;
            this.description = description + " x" + count;
        }

        public int getCount() {
            return count;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 检查物品是否匹配此需求
         */
        public boolean matches(ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (matcher != null) {
                return matcher.test(stack);
            }
            if (exactItem != null) {
                if (exactItem.getMetadata() == 32767) {
                    // 忽略meta
                    return stack.getItem() == exactItem.getItem();
                }
                return stack.getItem() == exactItem.getItem() &&
                        stack.getMetadata() == exactItem.getMetadata();
            }
            return false;
        }

        /**
         * 从ItemStack创建（用于CraftTweaker）
         */
        public static MaterialRequirement fromItemStack(ItemStack stack) {
            return new MaterialRequirement(stack.getItem(), stack.getMetadata(), stack.getCount());
        }
    }

    // ==================== 默认材料配置 ====================

    private static final Map<String, List<MaterialRequirement>> DEFAULT_MATERIALS = new HashMap<>();

    static {
        // 注魔仪式 - 附魔书 ×3 (最少)
        DEFAULT_MATERIALS.put(ENCHANT_INFUSION, Arrays.asList(
                new MaterialRequirement(Items.ENCHANTED_BOOK, 3)
        ));

        // 复制仪式 - 虚空精华 ×8 (需要从ModItems获取，这里用占位符)
        // 注意：实际使用时需要在游戏初始化后设置
        DEFAULT_MATERIALS.put(DUPLICATION, new ArrayList<>()); // 将在PostInit中设置

        // 灵魂绑定仪式 - 灵魂材料 ×4 (最少)
        // 注意：实际使用时需要在游戏初始化后设置
        DEFAULT_MATERIALS.put(SOUL_BINDING, new ArrayList<>()); // 将在PostInit中设置

        // 诅咒净化仪式 - 圣水/金苹果 ×1
        DEFAULT_MATERIALS.put(CURSE_PURIFICATION, Arrays.asList(
                new MaterialRequirement(
                        stack -> stack.getItem() == Items.GOLDEN_APPLE ||
                                (stack.getItem() == Items.POTIONITEM), // 简化检查
                        1, "Holy Item"
                )
        ));

        // 附魔转移仪式 - 青金石/龙息 ×1 + 目标物品 ×1
        DEFAULT_MATERIALS.put(ENCHANT_TRANSFER, Arrays.asList(
                new MaterialRequirement(
                        stack -> (stack.getItem() == Items.DYE && stack.getMetadata() == 4) ||
                                stack.getItem() == Items.DRAGON_BREATH,
                        1, "Lapis/Dragon Breath"
                )
        ));

        // 诅咒创造仪式 - 墨囊 ×1 + 腐肉/蜘蛛眼 ×1
        DEFAULT_MATERIALS.put(CURSE_CREATION, Arrays.asList(
                new MaterialRequirement(Items.DYE, 0, 1), // 墨囊
                new MaterialRequirement(
                        stack -> stack.getItem() == Items.ROTTEN_FLESH ||
                                stack.getItem() == Items.SPIDER_EYE ||
                                stack.getItem() == Items.FERMENTED_SPIDER_EYE,
                        1, "Curse Material"
                )
        ));

        // 武器经验加速仪式 - 经验瓶/附魔书/绿宝石 ×1
        DEFAULT_MATERIALS.put(WEAPON_EXP_BOOST, Arrays.asList(
                new MaterialRequirement(
                        stack -> stack.getItem() == Items.EXPERIENCE_BOTTLE ||
                                stack.getItem() == Items.ENCHANTED_BOOK ||
                                stack.getItem() == Items.EMERALD,
                        1, "Exp Material"
                )
        ));

        // 村正攻击提升仪式 - 烈焰粉/恶魂之泪/金粒 ×1
        DEFAULT_MATERIALS.put(MURAMASA_BOOST, Arrays.asList(
                new MaterialRequirement(
                        stack -> stack.getItem() == Items.BLAZE_POWDER ||
                                stack.getItem() == Items.GHAST_TEAR ||
                                stack.getItem() == Items.GOLD_NUGGET,
                        1, "Muramasa Material"
                )
        ));

        // 织印强化仪式 - 龙息/末影之眼/下界之星/海晶碎片/烈焰粉 ×1
        DEFAULT_MATERIALS.put(FABRIC_ENHANCE, Arrays.asList(
                new MaterialRequirement(
                        stack -> stack.getItem() == Items.DRAGON_BREATH ||
                                stack.getItem() == Items.ENDER_EYE ||
                                stack.getItem() == Items.NETHER_STAR ||
                                stack.getItem() == Items.PRISMARINE_SHARD ||
                                stack.getItem() == Items.BLAZE_POWDER,
                        1, "Fabric Material"
                )
        ));

        // 不可破坏仪式 - 下界之星×2 + 黑曜石×2 + 钻石×4
        DEFAULT_MATERIALS.put(UNBREAKABLE, Arrays.asList(
                new MaterialRequirement(Items.NETHER_STAR, 2),
                new MaterialRequirement(Item.getItemFromBlock(Blocks.OBSIDIAN), 2),
                new MaterialRequirement(Items.DIAMOND, 4)
        ));

        // 灵魂束缚仪式 - 末影珍珠×4 + 恶魂之泪×2 + 金块×2
        DEFAULT_MATERIALS.put(SOULBOUND, Arrays.asList(
                new MaterialRequirement(Items.ENDER_PEARL, 4),
                new MaterialRequirement(Items.GHAST_TEAR, 2),
                new MaterialRequirement(Item.getItemFromBlock(Blocks.GOLD_BLOCK), 2)
        ));

        // 嵌入仪式 - 无材料需求（由七圣遗物自动触发）
        DEFAULT_MATERIALS.put(EMBEDDING, new ArrayList<>());
    }

    // ==================== 材料查询方法 ====================

    /**
     * 获取仪式的材料需求列表
     * 优先返回自定义配置，否则返回默认配置
     */
    public static List<MaterialRequirement> getMaterialRequirements(String ritualId) {
        String id = ritualId.toLowerCase(Locale.ROOT);

        // 检查是否有自定义材料（通过 setPedestalItems 设置）
        RitualParams override = OVERRIDES.get(id);
        if (override != null && override.pedestalItems != null && !override.pedestalItems.isEmpty()) {
            // 将自定义 ItemStack 列表转换为 MaterialRequirement 列表
            List<MaterialRequirement> result = new ArrayList<>();
            Map<String, Integer> countMap = new HashMap<>();

            // 统计每种物品的数量
            for (ItemStack stack : override.pedestalItems) {
                String key = stack.getItem().getRegistryName() + ":" + stack.getMetadata();
                countMap.merge(key, stack.getCount(), Integer::sum);
            }

            System.out.println("[LegacyRitual DEBUG] getMaterialRequirements for " + ritualId + ":");
            System.out.println("[LegacyRitual DEBUG]   pedestalItems count: " + override.pedestalItems.size());
            System.out.println("[LegacyRitual DEBUG]   countMap: " + countMap);

            // 创建 MaterialRequirement
            for (ItemStack stack : override.pedestalItems) {
                String key = stack.getItem().getRegistryName() + ":" + stack.getMetadata();
                Integer count = countMap.remove(key);
                if (count != null) {
                    result.add(new MaterialRequirement(stack.getItem(), stack.getMetadata(), count));
                    System.out.println("[LegacyRitual DEBUG]   Created requirement: " + key + " x" + count);
                }
            }

            return result;
        }

        // 返回默认配置
        List<MaterialRequirement> defaults = DEFAULT_MATERIALS.get(id);
        return defaults != null ? new ArrayList<>(defaults) : new ArrayList<>();
    }

    /**
     * 检查基座物品是否满足仪式材料需求
     * 注意：每个基座只能放一个物品，所以这里统计的是"有多少个基座放了匹配的物品"
     *
     * @param ritualId 仪式ID
     * @param pedestalItems 基座上的物品列表
     * @return true 如果满足所有材料需求
     */
    public static boolean checkMaterialRequirements(String ritualId, List<ItemStack> pedestalItems) {
        List<MaterialRequirement> requirements = getMaterialRequirements(ritualId);

        System.out.println("[LegacyRitual DEBUG] checkMaterialRequirements for " + ritualId);
        System.out.println("[LegacyRitual DEBUG]   requirements count: " + requirements.size());

        if (requirements.isEmpty()) {
            System.out.println("[LegacyRitual DEBUG]   No requirements - returning true");
            return true; // 无材料需求
        }

        // 创建可用物品的副本（每个基座只能放一个物品，所以每个物品只能匹配一次）
        List<ItemStack> available = new ArrayList<>();
        for (ItemStack stack : pedestalItems) {
            if (!stack.isEmpty()) {
                available.add(stack.copy());
            }
        }

        System.out.println("[LegacyRitual DEBUG]   available pedestals: " + available.size());
        for (ItemStack stack : available) {
            System.out.println("[LegacyRitual DEBUG]     - " + stack.getItem().getRegistryName() + ":" + stack.getMetadata() + " x" + stack.getCount());
        }

        // 检查每个需求是否满足
        // 每个需求可能需要多个物品（例如钻石×4）
        // 需要从可用物品中找到足够数量的匹配物品
        for (MaterialRequirement req : requirements) {
            int needed = req.getCount();
            int found = 0;

            // 遍历可用物品，统计匹配的基座数量（不是堆叠数量）
            Iterator<ItemStack> iter = available.iterator();
            while (iter.hasNext() && found < needed) {
                ItemStack stack = iter.next();
                if (req.matches(stack)) {
                    found++;
                    iter.remove(); // 这个物品已被此需求使用，从可用列表中移除
                }
            }

            System.out.println("[LegacyRitual DEBUG]   Requirement " + req.getDescription() + ": found " + found + " / needed " + needed);

            if (found < needed) {
                System.out.println("[LegacyRitual DEBUG]   FAILED - not enough materials");
                return false;
            }
        }

        System.out.println("[LegacyRitual DEBUG]   SUCCESS - all requirements met");
        return true;
    }

    /**
     * 统计满足特定条件的基座物品数量
     *
     * @param pedestalItems 基座上的物品列表
     * @param matcher 匹配条件
     * @return 满足条件的物品总数
     */
    public static int countMatchingItems(List<ItemStack> pedestalItems, Predicate<ItemStack> matcher) {
        int count = 0;
        for (ItemStack stack : pedestalItems) {
            if (!stack.isEmpty() && matcher.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * 设置默认材料配置（用于在 PostInit 阶段设置 ModItems）
     */
    public static void setDefaultMaterials(String ritualId, List<MaterialRequirement> materials) {
        DEFAULT_MATERIALS.put(ritualId.toLowerCase(Locale.ROOT), new ArrayList<>(materials));
        log("Set default materials for " + ritualId + " (" + materials.size() + " requirements)");
    }

    /**
     * 检查是否有自定义材料配置
     * 返回true如果：
     * 1. 已通过 setPedestalItems 设置了自定义材料（非空）
     * 2. 已通过 clearMaterials 清除了默认材料（即使列表为空也返回true）
     */
    public static boolean hasCustomMaterials(String ritualId) {
        String id = ritualId.toLowerCase(Locale.ROOT);

        // 如果材料已被清除，返回true（即使pedestalItems为空）
        if (CLEARED_MATERIALS.contains(id)) {
            System.out.println("[LegacyRitual DEBUG] hasCustomMaterials(" + ritualId + "): true (cleared)");
            return true;
        }

        // 否则检查是否有非空的自定义材料
        RitualParams override = OVERRIDES.get(id);
        boolean result = override != null && override.pedestalItems != null && !override.pedestalItems.isEmpty();
        System.out.println("[LegacyRitual DEBUG] hasCustomMaterials(" + ritualId + "): " + result +
            " (override=" + (override != null) +
            ", pedestalItems=" + (override != null && override.pedestalItems != null ? override.pedestalItems.size() : "null") + ")");
        return result;
    }
}
