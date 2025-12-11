package com.moremod.ritual.special;

import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 特殊仪式注册表
 * 管理所有特殊仪式的注册、查询和执行
 *
 * 使用方法：
 * 1. 在 mod 初始化时注册仪式：SpecialRitualRegistry.register(new SoulboundRitual());
 * 2. 在 TileEntityRitualCore 中通过注册表查找和执行仪式
 * 3. CraftTweaker 可以通过注册表修改仪式参数
 */
public class SpecialRitualRegistry {

    // 按ID存储的仪式映射
    private static final Map<String, ISpecialRitual> RITUALS = new LinkedHashMap<>();

    // 被禁用的仪式ID集合
    private static final Set<String> DISABLED_RITUALS = new HashSet<>();

    // CraftTweaker 覆盖配置
    private static final Map<String, RitualOverrides> OVERRIDES = new HashMap<>();

    // ==================== 注册方法 ====================

    /**
     * 注册一个特殊仪式
     * @param ritual 要注册的仪式
     */
    public static void register(ISpecialRitual ritual) {
        if (ritual == null) {
            throw new IllegalArgumentException("Cannot register null ritual");
        }
        String id = ritual.getId().toLowerCase(Locale.ROOT);
        if (RITUALS.containsKey(id)) {
            System.out.println("[moremod] Warning: Overwriting existing ritual: " + id);
        }
        RITUALS.put(id, ritual);
        System.out.println("[moremod] Registered special ritual: " + id + " (" + ritual.getDisplayName() + ")");
    }

    /**
     * 注销一个特殊仪式
     * @param id 仪式ID
     */
    public static void unregister(String id) {
        id = id.toLowerCase(Locale.ROOT);
        if (RITUALS.remove(id) != null) {
            System.out.println("[moremod] Unregistered special ritual: " + id);
        }
    }

    /**
     * 禁用一个仪式（不删除，只是标记为禁用）
     */
    public static void disable(String id) {
        DISABLED_RITUALS.add(id.toLowerCase(Locale.ROOT));
    }

    /**
     * 启用一个仪式
     */
    public static void enable(String id) {
        DISABLED_RITUALS.remove(id.toLowerCase(Locale.ROOT));
    }

    /**
     * 清除所有仪式（慎用，主要用于测试）
     */
    public static void clearAll() {
        RITUALS.clear();
        DISABLED_RITUALS.clear();
        OVERRIDES.clear();
    }

    // ==================== 查询方法 ====================

    /**
     * 获取指定ID的仪式
     * @param id 仪式ID
     * @return 仪式实例，如果不存在或被禁用则返回null
     */
    @Nullable
    public static ISpecialRitual get(String id) {
        id = id.toLowerCase(Locale.ROOT);
        if (DISABLED_RITUALS.contains(id)) {
            return null;
        }
        return RITUALS.get(id);
    }

    /**
     * 获取所有已注册的仪式
     * @return 不可修改的仪式集合
     */
    public static Collection<ISpecialRitual> getAll() {
        return Collections.unmodifiableCollection(RITUALS.values());
    }

    /**
     * 获取所有已启用的仪式
     */
    public static List<ISpecialRitual> getAllEnabled() {
        List<ISpecialRitual> enabled = new ArrayList<>();
        for (Map.Entry<String, ISpecialRitual> entry : RITUALS.entrySet()) {
            if (!DISABLED_RITUALS.contains(entry.getKey()) && entry.getValue().isEnabled()) {
                enabled.add(entry.getValue());
            }
        }
        return enabled;
    }

    /**
     * 获取指定阶层的所有仪式
     */
    public static List<ISpecialRitual> getByTier(int tier) {
        List<ISpecialRitual> result = new ArrayList<>();
        for (ISpecialRitual ritual : getAllEnabled()) {
            if (ritual.getRequiredTier() == tier) {
                result.add(ritual);
            }
        }
        return result;
    }

    /**
     * 检查仪式是否存在
     */
    public static boolean exists(String id) {
        return RITUALS.containsKey(id.toLowerCase(Locale.ROOT));
    }

    /**
     * 检查仪式是否启用
     */
    public static boolean isEnabled(String id) {
        id = id.toLowerCase(Locale.ROOT);
        return RITUALS.containsKey(id) && !DISABLED_RITUALS.contains(id);
    }

    // ==================== 匹配方法 ====================

    /**
     * 根据中心物品和基座物品查找匹配的仪式
     * @param centerItem 中心物品
     * @param pedestalItems 基座物品
     * @param altarTier 当前祭坛阶层
     * @return 匹配的仪式，如果没有则返回null
     */
    @Nullable
    public static ISpecialRitual findMatchingRitual(ItemStack centerItem, List<ItemStack> pedestalItems, int altarTier) {
        for (ISpecialRitual ritual : getAllEnabled()) {
            // 检查阶层要求
            if (altarTier < ritual.getRequiredTier()) {
                continue;
            }

            // 检查中心物品
            if (!ritual.isValidCenterItem(centerItem)) {
                continue;
            }

            // 检查基座材料
            if (ritual.checkPedestalMaterials(pedestalItems)) {
                return ritual;
            }
        }
        return null;
    }

    // ==================== CraftTweaker 覆盖支持 ====================

    /**
     * 设置仪式参数覆盖（CraftTweaker用）
     */
    public static void setOverride(String id, RitualOverrides overrides) {
        OVERRIDES.put(id.toLowerCase(Locale.ROOT), overrides);
    }

    /**
     * 获取仪式参数覆盖
     */
    @Nullable
    public static RitualOverrides getOverride(String id) {
        return OVERRIDES.get(id.toLowerCase(Locale.ROOT));
    }

    /**
     * 清除仪式参数覆盖
     */
    public static void clearOverride(String id) {
        OVERRIDES.remove(id.toLowerCase(Locale.ROOT));
    }

    /**
     * 获取仪式的有效持续时间（考虑覆盖）
     */
    public static int getEffectiveDuration(ISpecialRitual ritual) {
        RitualOverrides override = OVERRIDES.get(ritual.getId().toLowerCase(Locale.ROOT));
        if (override != null && override.duration != null) {
            return override.duration;
        }
        // 使用默认值方法避免无限递归
        if (ritual instanceof AbstractSpecialRitual) {
            return ((AbstractSpecialRitual) ritual).getDefaultDuration();
        }
        return 200; // 默认10秒
    }

    /**
     * 获取仪式的有效失败率（考虑覆盖）
     */
    public static float getEffectiveFailChance(ISpecialRitual ritual) {
        RitualOverrides override = OVERRIDES.get(ritual.getId().toLowerCase(Locale.ROOT));
        if (override != null && override.failChance != null) {
            return override.failChance;
        }
        // 使用默认值方法避免无限递归
        if (ritual instanceof AbstractSpecialRitual) {
            return ((AbstractSpecialRitual) ritual).getDefaultFailChance();
        }
        return 0.0f; // 默认不失败
    }

    /**
     * 获取仪式的有效能量消耗（考虑覆盖）
     */
    public static int getEffectiveEnergyPerPedestal(ISpecialRitual ritual) {
        RitualOverrides override = OVERRIDES.get(ritual.getId().toLowerCase(Locale.ROOT));
        if (override != null && override.energyPerPedestal != null) {
            return override.energyPerPedestal;
        }
        // 使用默认值方法避免无限递归
        if (ritual instanceof AbstractSpecialRitual) {
            return ((AbstractSpecialRitual) ritual).getDefaultEnergyPerPedestal();
        }
        return 100000; // 默认100k RF
    }

    /**
     * 仪式参数覆盖类
     */
    public static class RitualOverrides {
        public Integer duration;
        public Float failChance;
        public Integer energyPerPedestal;
        public List<ItemStack> pedestalItems;

        public RitualOverrides() {}

        public RitualOverrides setDuration(int duration) {
            this.duration = duration;
            return this;
        }

        public RitualOverrides setFailChance(float failChance) {
            this.failChance = failChance;
            return this;
        }

        public RitualOverrides setEnergyPerPedestal(int energy) {
            this.energyPerPedestal = energy;
            return this;
        }

        public RitualOverrides setPedestalItems(List<ItemStack> items) {
            this.pedestalItems = items;
            return this;
        }
    }

    // ==================== 调试方法 ====================

    /**
     * 打印所有已注册的仪式（调试用）
     */
    public static void printAllRituals() {
        System.out.println("=== Registered Special Rituals ===");
        for (Map.Entry<String, ISpecialRitual> entry : RITUALS.entrySet()) {
            ISpecialRitual r = entry.getValue();
            String status = DISABLED_RITUALS.contains(entry.getKey()) ? "[DISABLED]" : "[ENABLED]";
            System.out.println(String.format("  %s %s: %s (Tier %d, %d ticks, %.0f%% fail)",
                    status, entry.getKey(), r.getDisplayName(),
                    r.getRequiredTier(), r.getDuration(), r.getFailChance() * 100));
        }
        System.out.println("=================================");
    }
}
