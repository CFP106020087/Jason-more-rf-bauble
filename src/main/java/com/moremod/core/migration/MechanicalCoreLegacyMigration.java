package com.moremod.core.migration;

import com.moremod.core.api.CoreUpgradeEntry;
import com.moremod.core.api.IMechanicalCoreData;
import com.moremod.core.registry.UpgradeRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.*;

/**
 * 旧存档迁移工具
 *
 * 负责将旧的NBT键值迁移到新的Capability系统
 *
 * 支持的旧NBT键格式：
 * - upgrade_ID
 * - HasUpgrade_ID
 * - OwnedMax_ID
 * - OriginalMax_ID
 * - DamageCount_ID
 * - TotalDamageCount_ID
 * - WasPunished_ID
 * - IsPaused_ID
 * - LastLevel_ID
 * - Disabled_ID
 *
 * 所有键都支持大小写变体和别名
 */
public class MechanicalCoreLegacyMigration {

    // NBT键前缀
    private static final String PREFIX_UPGRADE = "upgrade_";
    private static final String PREFIX_HAS_UPGRADE = "HasUpgrade_";
    private static final String PREFIX_OWNED_MAX = "OwnedMax_";
    private static final String PREFIX_ORIGINAL_MAX = "OriginalMax_";
    private static final String PREFIX_DAMAGE_COUNT = "DamageCount_";
    private static final String PREFIX_TOTAL_DAMAGE = "TotalDamageCount_";
    private static final String PREFIX_WAS_PUNISHED = "WasPunished_";
    private static final String PREFIX_IS_PAUSED = "IsPaused_";
    private static final String PREFIX_LAST_LEVEL = "LastLevel_";
    private static final String PREFIX_DISABLED = "Disabled_";

    // Waterproof特殊别名
    private static final Set<String> WATERPROOF_ALIASES = new HashSet<>(Arrays.asList(
            "WATERPROOF_MODULE", "WATERPROOF", "waterproof_module", "waterproof"
    ));

    /**
     * 执行迁移（从ItemStack的NBT迁移到Capability）
     *
     * @param stack 机械核心物品栈
     * @param data Capability数据实例
     */
    public static void migrate(ItemStack stack, IMechanicalCoreData data) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return;
        }

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            return;
        }

        // 检查是否已迁移
        if (nbt.getBoolean("Core3_Migrated")) {
            return;
        }

        // 收集所有升级ID（从各种来源）
        Set<String> allUpgradeIds = collectAllUpgradeIds(nbt);

        // 为每个升级ID迁移数据
        for (String rawId : allUpgradeIds) {
            migrateUpgrade(nbt, data, rawId);
        }

        // 标记迁移完成（在Provider中会设置）
        // nbt.setBoolean("Core3_Migrated", true);
    }

    /**
     * 收集NBT中所有出现过的升级ID
     */
    private static Set<String> collectAllUpgradeIds(NBTTagCompound nbt) {
        Set<String> ids = new LinkedHashSet<>();

        // 遍历所有NBT键
        for (String key : nbt.getKeySet()) {
            String id = null;

            // 尝试从各种前缀中提取升级ID
            if (key.startsWith(PREFIX_UPGRADE)) {
                id = key.substring(PREFIX_UPGRADE.length());
            } else if (key.startsWith(PREFIX_HAS_UPGRADE)) {
                id = key.substring(PREFIX_HAS_UPGRADE.length());
            } else if (key.startsWith(PREFIX_OWNED_MAX)) {
                id = key.substring(PREFIX_OWNED_MAX.length());
            } else if (key.startsWith(PREFIX_ORIGINAL_MAX)) {
                id = key.substring(PREFIX_ORIGINAL_MAX.length());
            } else if (key.startsWith(PREFIX_DAMAGE_COUNT)) {
                id = key.substring(PREFIX_DAMAGE_COUNT.length());
            } else if (key.startsWith(PREFIX_TOTAL_DAMAGE)) {
                id = key.substring(PREFIX_TOTAL_DAMAGE.length());
            } else if (key.startsWith(PREFIX_WAS_PUNISHED)) {
                id = key.substring(PREFIX_WAS_PUNISHED.length());
            } else if (key.startsWith(PREFIX_IS_PAUSED)) {
                id = key.substring(PREFIX_IS_PAUSED.length());
            } else if (key.startsWith(PREFIX_LAST_LEVEL)) {
                id = key.substring(PREFIX_LAST_LEVEL.length());
            } else if (key.startsWith(PREFIX_DISABLED)) {
                id = key.substring(PREFIX_DISABLED.length());
            }

            if (id != null && !id.isEmpty()) {
                ids.add(id);
            }
        }

        // 添加所有已注册的升级ID（确保不遗漏）
        ids.addAll(UpgradeRegistry.getAllIds());

        return ids;
    }

    /**
     * 迁移单个升级的数据
     */
    private static void migrateUpgrade(NBTTagCompound nbt, IMechanicalCoreData data, String rawId) {
        // 获取规范ID
        String canonId = UpgradeRegistry.canonicalIdOf(rawId);

        // 生成所有可能的变体（大小写 + 别名）
        Set<String> variants = generateVariants(rawId, canonId);

        // 读取所有数据（从所有变体中取最大值/最新值）
        int level = readMaxInt(nbt, PREFIX_UPGRADE, variants);
        int ownedMax = readMaxInt(nbt, PREFIX_OWNED_MAX, variants);
        int originalMax = readMaxInt(nbt, PREFIX_ORIGINAL_MAX, variants);
        int lastLevel = readMaxInt(nbt, PREFIX_LAST_LEVEL, variants);
        int damageCount = readMaxInt(nbt, PREFIX_DAMAGE_COUNT, variants);
        int totalDamageCount = readMaxInt(nbt, PREFIX_TOTAL_DAMAGE, variants);

        boolean hasUpgrade = readAnyBoolean(nbt, PREFIX_HAS_UPGRADE, variants);
        boolean wasPunished = readAnyBoolean(nbt, PREFIX_WAS_PUNISHED, variants);
        boolean isPaused = readAnyBoolean(nbt, PREFIX_IS_PAUSED, variants);
        boolean isDisabled = readAnyBoolean(nbt, PREFIX_DISABLED, variants);

        // 如果没有任何数据，跳过
        if (level == 0 && ownedMax == 0 && originalMax == 0 && !hasUpgrade) {
            return;
        }

        // 创建或获取升级条目
        CoreUpgradeEntry entry = data.getOrCreate(canonId);

        // 应用数据
        entry.setLevel(level);

        // OwnedMax处理：如果未设置但有等级，使用等级作为ownedMax
        if (ownedMax == 0 && level > 0) {
            ownedMax = level;
        }
        entry.setOwnedMax(ownedMax);

        // OriginalMax处理：如果未设置但有ownedMax，使用ownedMax
        if (originalMax == 0 && ownedMax > 0) {
            originalMax = ownedMax;
        }
        entry.setOriginalMax(originalMax);

        entry.setLastLevel(lastLevel);
        entry.setDamageCount(damageCount);
        entry.setTotalDamageCount(totalDamageCount);
        entry.setWasPunished(wasPunished);
        entry.setPaused(isPaused);
        entry.setDisabled(isDisabled);

        // 特殊处理：如果暂停但lastLevel为0，使用当前level
        if (isPaused && lastLevel == 0 && level > 0) {
            entry.setLastLevel(level);
        }
    }

    /**
     * 生成所有可能的ID变体（包括大小写和别名）
     */
    private static Set<String> generateVariants(String rawId, String canonId) {
        Set<String> variants = new LinkedHashSet<>();

        // 添加原始ID的各种形式
        variants.add(rawId);
        variants.add(rawId.toUpperCase(Locale.ROOT));
        variants.add(rawId.toLowerCase(Locale.ROOT));

        // 添加规范ID的各种形式
        variants.add(canonId);
        variants.add(canonId.toUpperCase(Locale.ROOT));
        variants.add(canonId.toLowerCase(Locale.ROOT));

        // 添加所有注册的别名
        Set<String> aliases = UpgradeRegistry.getAliases(canonId);
        for (String alias : aliases) {
            variants.add(alias);
            variants.add(alias.toUpperCase(Locale.ROOT));
            variants.add(alias.toLowerCase(Locale.ROOT));
        }

        // 特殊处理：Waterproof的所有别名
        if (isWaterproofRelated(rawId) || isWaterproofRelated(canonId)) {
            for (String wpAlias : WATERPROOF_ALIASES) {
                variants.add(wpAlias);
                variants.add(wpAlias.toUpperCase(Locale.ROOT));
                variants.add(wpAlias.toLowerCase(Locale.ROOT));
            }
        }

        return variants;
    }

    /**
     * 检查ID是否与Waterproof相关
     */
    private static boolean isWaterproofRelated(String id) {
        if (id == null) return false;
        String upper = id.toUpperCase(Locale.ROOT);
        return WATERPROOF_ALIASES.contains(id) ||
               WATERPROOF_ALIASES.contains(upper) ||
               upper.contains("WATERPROOF");
    }

    /**
     * 从NBT读取整数（从所有变体中取最大值）
     */
    private static int readMaxInt(NBTTagCompound nbt, String prefix, Set<String> variants) {
        int max = 0;

        for (String variant : variants) {
            String key = prefix + variant;
            if (nbt.hasKey(key)) {
                int value = nbt.getInteger(key);
                max = Math.max(max, value);
            }
        }

        return max;
    }

    /**
     * 从NBT读取布尔值（任意一个为true则返回true）
     */
    private static boolean readAnyBoolean(NBTTagCompound nbt, String prefix, Set<String> variants) {
        for (String variant : variants) {
            String key = prefix + variant;
            if (nbt.hasKey(key) && nbt.getBoolean(key)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 清理旧的NBT键（可选，建议在迁移后调用以减少NBT大小）
     *
     * 注意：默认不清理，以保持向后兼容
     */
    public static void cleanupLegacyKeys(NBTTagCompound nbt) {
        Set<String> keysToRemove = new HashSet<>();

        for (String key : nbt.getKeySet()) {
            if (key.startsWith(PREFIX_UPGRADE) ||
                key.startsWith(PREFIX_HAS_UPGRADE) ||
                key.startsWith(PREFIX_OWNED_MAX) ||
                key.startsWith(PREFIX_ORIGINAL_MAX) ||
                key.startsWith(PREFIX_DAMAGE_COUNT) ||
                key.startsWith(PREFIX_TOTAL_DAMAGE) ||
                key.startsWith(PREFIX_WAS_PUNISHED) ||
                key.startsWith(PREFIX_IS_PAUSED) ||
                key.startsWith(PREFIX_LAST_LEVEL) ||
                key.startsWith(PREFIX_DISABLED)) {
                keysToRemove.add(key);
            }
        }

        // 删除旧键
        for (String key : keysToRemove) {
            nbt.removeTag(key);
        }
    }
}
