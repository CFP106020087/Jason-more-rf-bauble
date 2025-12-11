package com.moremod.ritual;

import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

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
        log("Set " + ritualId + " pedestal items (" + items.size() + " items)");
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
        OVERRIDES.remove(ritualId.toLowerCase(Locale.ROOT));
        DISABLED.remove(ritualId.toLowerCase(Locale.ROOT));
        log("Reset ritual to default: " + ritualId);
    }

    /**
     * 重置所有仪式
     */
    public static void resetAll() {
        OVERRIDES.clear();
        DISABLED.clear();
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
}
