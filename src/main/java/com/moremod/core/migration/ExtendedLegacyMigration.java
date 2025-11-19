package com.moremod.core.migration;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 扩展的旧数据迁移 - 处理惩罚系统和其他特殊NBT键
 *
 * 这些NBT键不属于Capability数据模型，保留在ItemStack的NBT中
 */
public class ExtendedLegacyMigration {

    /**
     * 迁移惩罚系统数据
     *
     * 这些键保留在NBT中，不迁移到Capability：
     * - PenaltyCap_ID
     * - PenaltyExpire_ID
     * - PenaltyTier_ID
     * - PenaltyDebtFE_ID
     * - PenaltyDebtXP_ID
     *
     * （这些键已经在NBT中，无需迁移，只需确保不被删除）
     */
    public static void migratePenaltySystem(ItemStack stack) {
        // 惩罚系统数据已经在NBT中，无需特殊处理
        // 这个方法是预留的，以防未来需要规范化惩罚键
    }

    /**
     * 迁移能量效率统计
     *
     * 保留在NBT中：
     * - TotalEnergySaved
     * - SessionEnergySaved
     */
    public static void migrateEnergyStats(ItemStack stack) {
        // 能量统计已经在NBT中，无需特殊处理
    }

    /**
     * 迁移速度模式
     *
     * 保留在NBT中：
     * - CoreSpeedMode
     */
    public static void migrateSpeedMode(ItemStack stack) {
        // 速度模式已经在NBT中，无需特殊处理
    }

    /**
     * 迁移紧急模式标记
     *
     * 保留在NBT中：
     * - EmergencyMode
     */
    public static void migrateEmergencyMode(ItemStack stack) {
        // 紧急模式已经在NBT中，无需特殊处理
    }

    /**
     * 完整迁移（包括所有系统）
     */
    public static void migrateAll(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            return;
        }

        // 检查是否已迁移
        if (nbt.getBoolean("Core3_ExtendedMigrated")) {
            return;
        }

        // 执行迁移（当前这些系统不需要特殊处理，只是确保键存在）
        migratePenaltySystem(stack);
        migrateEnergyStats(stack);
        migrateSpeedMode(stack);
        migrateEmergencyMode(stack);

        // 标记为已迁移
        nbt.setBoolean("Core3_ExtendedMigrated", true);
    }

    /**
     * 清理旧的、不再使用的NBT键
     *
     * 注意：谨慎使用！只清理确认不再需要的键
     */
    public static void cleanupObsoleteKeys(NBTTagCompound nbt) {
        // 目前不删除任何键，保持完全向后兼容
        // 未来如果确认某些键不再需要，可以在这里添加
    }
}
