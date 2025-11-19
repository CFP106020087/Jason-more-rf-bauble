package com.moremod.core.registry;

import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 升级注册中心 - 管理所有升级定义的唯一来源
 *
 * 功能：
 * - 注册所有升级定义
 * - 提供别名到规范ID的映射
 * - 查询升级定义
 * - 提供统计信息
 */
public class UpgradeRegistry {

    // 升级注册表（规范ID -> 定义）
    private static final Map<String, UpgradeDefinition> REGISTRY = new LinkedHashMap<>();

    // 别名映射表（别名 -> 规范ID）
    private static final Map<String, String> ALIAS_TO_CANON = new HashMap<>();

    // 初始化标记
    private static boolean initialized = false;

    /**
     * 初始化注册表（在preInit阶段调用）
     */
    public static void init() {
        if (initialized) {
            return;
        }

        // 清空注册表
        REGISTRY.clear();
        ALIAS_TO_CANON.clear();

        // ===== 基础升级（来自原ItemMechanicalCore.UpgradeType） =====
        register(UpgradeDefinition.builder("ENERGY_CAPACITY")
                .displayName("能量容量")
                .color(TextFormatting.BLUE)
                .maxLevel(5)
                .category(UpgradeDefinition.UpgradeCategory.BASIC)
                .build());

        register(UpgradeDefinition.builder("ENERGY_EFFICIENCY")
                .displayName("能量效率")
                .color(TextFormatting.GREEN)
                .maxLevel(5)
                .category(UpgradeDefinition.UpgradeCategory.BASIC)
                .build());

        register(UpgradeDefinition.builder("ARMOR_ENHANCEMENT")
                .displayName("护甲强化")
                .color(TextFormatting.YELLOW)
                .maxLevel(5)
                .category(UpgradeDefinition.UpgradeCategory.BASIC)
                .build());

        register(UpgradeDefinition.builder("SPEED_BOOST")
                .displayName("速度提升")
                .color(TextFormatting.AQUA)
                .maxLevel(5)
                .category(UpgradeDefinition.UpgradeCategory.BASIC)
                .build());

        register(UpgradeDefinition.builder("REGENERATION")
                .displayName("生命恢复")
                .color(TextFormatting.RED)
                .maxLevel(5)
                .category(UpgradeDefinition.UpgradeCategory.BASIC)
                .build());

        register(UpgradeDefinition.builder("FLIGHT_MODULE")
                .displayName("飞行模块")
                .color(TextFormatting.LIGHT_PURPLE)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.BASIC)
                .build());

        register(UpgradeDefinition.builder("SHIELD_GENERATOR")
                .displayName("护盾发生器")
                .color(TextFormatting.GOLD)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.BASIC)
                .build());

        register(UpgradeDefinition.builder("TEMPERATURE_CONTROL")
                .displayName("温度调节")
                .color(TextFormatting.DARK_AQUA)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.BASIC)
                .build());

        // ===== 生存类升级 =====
        register(UpgradeDefinition.builder("YELLOW_SHIELD")
                .displayName("黄条护盾")
                .color(TextFormatting.YELLOW)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.SURVIVAL)
                .build());

        register(UpgradeDefinition.builder("HEALTH_REGEN")
                .displayName("纳米修复")
                .color(TextFormatting.RED)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.SURVIVAL)
                .build());

        register(UpgradeDefinition.builder("HUNGER_THIRST")
                .displayName("代谢调节")
                .color(TextFormatting.GREEN)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.SURVIVAL)
                .build());

        register(UpgradeDefinition.builder("THORNS")
                .displayName("反应装甲")
                .color(TextFormatting.DARK_RED)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.SURVIVAL)
                .build());

        register(UpgradeDefinition.builder("FIRE_EXTINGUISH")
                .displayName("自动灭火")
                .color(TextFormatting.BLUE)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.SURVIVAL)
                .build());

        // ===== 辅助类升级 =====
        register(UpgradeDefinition.builder("WATERPROOF_MODULE")
                .displayName("防水模块")
                .color(TextFormatting.AQUA)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.AUXILIARY)
                .aliases("WATERPROOF", "waterproof_module", "waterproof")
                .build());

        register(UpgradeDefinition.builder("ORE_VISION")
                .displayName("矿物透视")
                .color(TextFormatting.GOLD)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.AUXILIARY)
                .build());

        register(UpgradeDefinition.builder("MOVEMENT_SPEED")
                .displayName("伺服电机")
                .color(TextFormatting.AQUA)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.AUXILIARY)
                .build());

        register(UpgradeDefinition.builder("STEALTH")
                .displayName("光学迷彩")
                .color(TextFormatting.DARK_GRAY)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.AUXILIARY)
                .build());

        register(UpgradeDefinition.builder("EXP_AMPLIFIER")
                .displayName("经验矩阵")
                .color(TextFormatting.GREEN)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.AUXILIARY)
                .build());

        register(UpgradeDefinition.builder("POISON_IMMUNITY")
                .displayName("毒免疫")
                .color(TextFormatting.DARK_GREEN)
                .maxLevel(1)
                .category(UpgradeDefinition.UpgradeCategory.AUXILIARY)
                .build());

        register(UpgradeDefinition.builder("NIGHT_VISION")
                .displayName("夜视")
                .color(TextFormatting.YELLOW)
                .maxLevel(1)
                .category(UpgradeDefinition.UpgradeCategory.AUXILIARY)
                .build());

        register(UpgradeDefinition.builder("WATER_BREATHING")
                .displayName("水下呼吸")
                .color(TextFormatting.AQUA)
                .maxLevel(1)
                .category(UpgradeDefinition.UpgradeCategory.AUXILIARY)
                .build());

        register(UpgradeDefinition.builder("ITEM_MAGNET")
                .displayName("物品磁铁")
                .color(TextFormatting.LIGHT_PURPLE)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.AUXILIARY)
                .build());

        register(UpgradeDefinition.builder("NEURAL_SYNCHRONIZER")
                .displayName("神经同步器")
                .color(TextFormatting.AQUA)
                .maxLevel(1)
                .category(UpgradeDefinition.UpgradeCategory.AUXILIARY)
                .build());

        // ===== 战斗类升级 =====
        register(UpgradeDefinition.builder("DAMAGE_BOOST")
                .displayName("力量增幅")
                .color(TextFormatting.DARK_RED)
                .maxLevel(5)
                .category(UpgradeDefinition.UpgradeCategory.COMBAT)
                .build());

        register(UpgradeDefinition.builder("ATTACK_SPEED")
                .displayName("反应增强")
                .color(TextFormatting.YELLOW)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.COMBAT)
                .build());

        register(UpgradeDefinition.builder("RANGE_EXTENSION")
                .displayName("范围拓展")
                .color(TextFormatting.BLUE)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.COMBAT)
                .build());

        register(UpgradeDefinition.builder("PURSUIT")
                .displayName("追击系统")
                .color(TextFormatting.LIGHT_PURPLE)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.COMBAT)
                .build());

        register(UpgradeDefinition.builder("CRITICAL_STRIKE")
                .displayName("暴击")
                .color(TextFormatting.GOLD)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.COMBAT)
                .build());

        register(UpgradeDefinition.builder("MAGIC_ABSORB")
                .displayName("魔力吸收模块")
                .color(TextFormatting.DARK_PURPLE)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.COMBAT)
                .build());

        // ===== 能源类升级 =====
        register(UpgradeDefinition.builder("KINETIC_GENERATOR")
                .displayName("动能发电")
                .color(TextFormatting.GRAY)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.ENERGY)
                .build());

        register(UpgradeDefinition.builder("SOLAR_GENERATOR")
                .displayName("太阳能板")
                .color(TextFormatting.YELLOW)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.ENERGY)
                .build());

        register(UpgradeDefinition.builder("VOID_ENERGY")
                .displayName("虚空共振")
                .color(TextFormatting.DARK_PURPLE)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.ENERGY)
                .build());

        register(UpgradeDefinition.builder("COMBAT_CHARGER")
                .displayName("战斗充能")
                .color(TextFormatting.RED)
                .maxLevel(3)
                .category(UpgradeDefinition.UpgradeCategory.ENERGY)
                .build());

        initialized = true;
    }

    /**
     * 注册升级定义
     */
    public static void register(UpgradeDefinition definition) {
        String canonId = definition.getId();

        // 注册主ID
        REGISTRY.put(canonId, definition);

        // 注册所有别名
        for (String alias : definition.getAliases()) {
            String normalizedAlias = alias.trim().toUpperCase(Locale.ROOT);
            if (!normalizedAlias.equals(canonId)) {
                ALIAS_TO_CANON.put(normalizedAlias, canonId);
            }
        }
    }

    /**
     * 获取规范ID（处理别名）
     *
     * @param id 原始ID（可能是别名）
     * @return 规范ID
     */
    public static String canonicalIdOf(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }

        String normalized = id.trim().toUpperCase(Locale.ROOT);

        // 先查询别名映射
        String canon = ALIAS_TO_CANON.get(normalized);
        if (canon != null) {
            return canon;
        }

        // 如果在注册表中存在，则返回该ID
        if (REGISTRY.containsKey(normalized)) {
            return normalized;
        }

        // 否则返回规范化后的ID（兜底）
        return normalized;
    }

    /**
     * 获取升级定义
     */
    @Nullable
    public static UpgradeDefinition getDefinition(String upgradeId) {
        String canonId = canonicalIdOf(upgradeId);
        return REGISTRY.get(canonId);
    }

    /**
     * 获取所有升级定义
     */
    public static Collection<UpgradeDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /**
     * 获取所有规范ID
     */
    public static Set<String> getAllIds() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }

    /**
     * 获取指定类别的所有升级
     */
    public static List<UpgradeDefinition> getByCategory(UpgradeDefinition.UpgradeCategory category) {
        List<UpgradeDefinition> result = new ArrayList<>();
        for (UpgradeDefinition def : REGISTRY.values()) {
            if (def.getCategory() == category) {
                result.add(def);
            }
        }
        return result;
    }

    /**
     * 检查升级是否已注册
     */
    public static boolean isRegistered(String upgradeId) {
        String canonId = canonicalIdOf(upgradeId);
        return REGISTRY.containsKey(canonId);
    }

    /**
     * 获取升级的最大等级（如果未注册则返回默认值5）
     */
    public static int getMaxLevel(String upgradeId) {
        UpgradeDefinition def = getDefinition(upgradeId);
        return def != null ? def.getMaxLevel() : 5;
    }

    /**
     * 获取升级的显示名称
     */
    public static String getDisplayName(String upgradeId) {
        UpgradeDefinition def = getDefinition(upgradeId);
        if (def != null) {
            return def.getDisplayName();
        }

        // 兜底：格式化ID
        return formatId(upgradeId);
    }

    /**
     * 获取升级的显示颜色
     */
    public static TextFormatting getColor(String upgradeId) {
        UpgradeDefinition def = getDefinition(upgradeId);
        return def != null ? def.getColor() : TextFormatting.WHITE;
    }

    /**
     * 获取升级的类别
     */
    public static UpgradeDefinition.UpgradeCategory getCategory(String upgradeId) {
        UpgradeDefinition def = getDefinition(upgradeId);
        return def != null ? def.getCategory() : UpgradeDefinition.UpgradeCategory.BASIC;
    }

    /**
     * 获取升级的所有别名（包括规范ID本身）
     */
    public static Set<String> getAliases(String upgradeId) {
        String canonId = canonicalIdOf(upgradeId);
        UpgradeDefinition def = REGISTRY.get(canonId);

        if (def == null) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<>(def.getAliases());
        result.add(canonId);
        return result;
    }

    /**
     * 格式化ID为显示名称（兜底方法）
     */
    private static String formatId(String id) {
        if (id == null || id.isEmpty()) {
            return "Unknown";
        }

        // 将下划线替换为空格，首字母大写
        String[] parts = id.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (result.length() > 0) result.append(" ");
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                result.append(part.substring(1));
            }
        }

        return result.toString();
    }

    /**
     * 获取注册的升级数量
     */
    public static int getRegistrySize() {
        return REGISTRY.size();
    }

    /**
     * 打印注册表信息（调试用）
     */
    public static void printRegistry() {
        System.out.println("=== Upgrade Registry ===");
        System.out.println("Total upgrades: " + REGISTRY.size());
        System.out.println("Total aliases: " + ALIAS_TO_CANON.size());

        for (UpgradeDefinition.UpgradeCategory category : UpgradeDefinition.UpgradeCategory.values()) {
            List<UpgradeDefinition> upgrades = getByCategory(category);
            if (!upgrades.isEmpty()) {
                System.out.println("\n" + category.getDisplayName() + " (" + upgrades.size() + "):");
                for (UpgradeDefinition def : upgrades) {
                    System.out.println("  - " + def.getId() + " (" + def.getDisplayName() + ")");
                    if (!def.getAliases().isEmpty()) {
                        System.out.println("    Aliases: " + def.getAliases());
                    }
                }
            }
        }
    }
}
