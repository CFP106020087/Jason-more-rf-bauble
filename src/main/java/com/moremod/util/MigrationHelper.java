package com.moremod.util;

import com.moremod.capability.IMechCoreData;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * NBT 数据迁移工具
 *
 * 用于将旧的三重存储 NBT 格式迁移到新的紧凑格式
 *
 * 旧格式问题：
 *  - 同一数据存储 3 次（原始、大写、小写）
 *  - Waterproof 模块有 4 个别名
 *  - 每个核心约 1600+ NBT keys
 *
 * 新格式：
 *  - 统一大写下划线命名
 *  - 层级化存储
 *  - 每个核心约 30-50 keys（减少 97%）
 */
public class MigrationHelper {

    private static final Logger logger = LogManager.getLogger(MigrationHelper.class);

    /** Waterproof 模块的所有别名 */
    private static final Set<String> WATERPROOF_ALIASES = new HashSet<>(Arrays.asList(
        "WATERPROOF_MODULE",
        "WATERPROOF",
        "waterproof_module",
        "waterproof"
    ));

    /** 所有已知的模块 ID（标准化大写格式） */
    private static final String[] KNOWN_MODULES = {
        "ENERGY_CAPACITY",
        "ENERGY_EFFICIENCY",
        "ARMOR_ENHANCEMENT",
        "SPEED_BOOST",
        "REGENERATION",
        "FLIGHT_MODULE",
        "SHIELD_GENERATOR",
        "TEMPERATURE_CONTROL",
        "WATERPROOF_MODULE",
        "MAGIC_ABSORB",
        "NEURAL_SYNCHRONIZER"
    };

    /**
     * 从旧格式 ItemStack NBT 迁移到 Capability
     *
     * @param data Capability 数据实例
     * @param oldNBT 旧的 ItemStack NBT
     */
    public static void migrateFromItemStack(IMechCoreData data, NBTTagCompound oldNBT) {
        if (oldNBT == null || oldNBT.getKeySet().isEmpty()) {
            return;
        }

        logger.info("Starting migration from old ItemStack NBT format...");

        int migratedModules = 0;

        // 迁移能量数据
        int energy = getMaxValue(oldNBT, "Energy", "energy", "ENERGY");
        if (energy > 0) {
            data.setEnergy(energy);
            logger.debug("Migrated energy: {}", energy);
        }

        // 迁移最大能量
        int maxEnergy = getMaxValue(oldNBT, "MaxEnergy", "max_energy", "MAX_ENERGY");
        if (maxEnergy > 0) {
            data.setMaxEnergy(maxEnergy);
            logger.debug("Migrated max energy: {}", maxEnergy);
        }

        // 迁移所有模块等级
        for (String moduleId : KNOWN_MODULES) {
            int level = getModuleLevel(oldNBT, moduleId);
            if (level > 0) {
                data.setModuleLevel(moduleId, level);
                migratedModules++;
                logger.debug("Migrated module: {} → level {}", moduleId, level);

                // 迁移元数据（如果存在）
                NBTTagCompound meta = getModuleMeta(oldNBT, moduleId);
                if (meta != null && !meta.getKeySet().isEmpty()) {
                    data.setModuleMeta(moduleId, meta);
                    logger.debug("Migrated metadata for: {}", moduleId);
                }
            }
        }

        logger.info("Migration complete: {} modules migrated", migratedModules);
    }

    /**
     * 从旧格式 NBT 获取模块等级（处理三重存储）
     *
     * @param nbt 旧 NBT
     * @param moduleId 标准化模块 ID（大写）
     * @return 模块等级（取最大值）
     */
    private static int getModuleLevel(NBTTagCompound nbt, String moduleId) {
        // 特殊处理 Waterproof 模块（4 个别名）
        if ("WATERPROOF_MODULE".equals(moduleId)) {
            int maxLevel = 0;
            for (String alias : WATERPROOF_ALIASES) {
                int level = getUpgradeLevel(nbt, alias);
                maxLevel = Math.max(maxLevel, level);
            }
            return maxLevel;
        }

        // 标准模块：检查三种命名格式
        return getUpgradeLevel(nbt, moduleId);
    }

    /**
     * 获取升级等级（检查所有可能的命名变体）
     *
     * @param nbt 旧 NBT
     * @param id 模块 ID
     * @return 等级（取最大值）
     */
    private static int getUpgradeLevel(NBTTagCompound nbt, String id) {
        int level = 0;

        // 检查 3 种命名格式
        level = Math.max(level, nbt.getInteger("upgrade_" + id));
        level = Math.max(level, nbt.getInteger("upgrade_" + id.toUpperCase()));
        level = Math.max(level, nbt.getInteger("upgrade_" + id.toLowerCase()));

        return level;
    }

    /**
     * 获取模块元数据
     *
     * @param nbt 旧 NBT
     * @param moduleId 模块 ID
     * @return 元数据 NBT
     */
    private static NBTTagCompound getModuleMeta(NBTTagCompound nbt, String moduleId) {
        NBTTagCompound meta = new NBTTagCompound();

        // 迁移 OriginalMax
        int originalMax = getMaxValue(
            nbt,
            "OriginalMax_" + moduleId,
            "OriginalMax_" + moduleId.toUpperCase(),
            "OriginalMax_" + moduleId.toLowerCase()
        );
        if (originalMax > 0) {
            meta.setInteger("ORIGINAL_MAX", originalMax);
        }

        // 迁移 DamageCount
        int damageCount = getMaxValue(
            nbt,
            "DamageCount_" + moduleId,
            "DamageCount_" + moduleId.toUpperCase(),
            "DamageCount_" + moduleId.toLowerCase()
        );
        if (damageCount > 0) {
            meta.setInteger("DAMAGE_COUNT", damageCount);
        }

        // 迁移 FirstUpgradeTime
        long firstUpgradeTime = getMaxLongValue(
            nbt,
            "FirstUpgradeTime_" + moduleId,
            "FirstUpgradeTime_" + moduleId.toUpperCase()
        );
        if (firstUpgradeTime > 0) {
            meta.setLong("FIRST_UPGRADE_TIME", firstUpgradeTime);
        }

        return meta;
    }

    /**
     * 获取多个可能 key 的最大值（int）
     */
    private static int getMaxValue(NBTTagCompound nbt, String... keys) {
        int max = 0;
        for (String key : keys) {
            if (nbt.hasKey(key)) {
                max = Math.max(max, nbt.getInteger(key));
            }
        }
        return max;
    }

    /**
     * 获取多个可能 key 的最大值（long）
     */
    private static long getMaxLongValue(NBTTagCompound nbt, String... keys) {
        long max = 0;
        for (String key : keys) {
            if (nbt.hasKey(key)) {
                max = Math.max(max, nbt.getLong(key));
            }
        }
        return max;
    }

    /**
     * 检查 NBT 是否是旧格式
     *
     * @param nbt NBT 数据
     * @return 是否是旧格式
     */
    public static boolean isOldFormat(NBTTagCompound nbt) {
        // 新格式有 CORE 顶层容器和 VERSION 字段
        if (nbt.hasKey("CORE")) {
            NBTTagCompound core = nbt.getCompoundTag("CORE");
            return !core.hasKey("VERSION") || core.getInteger("VERSION") < 2;
        }
        return true; // 没有 CORE 容器说明是旧格式
    }
}
