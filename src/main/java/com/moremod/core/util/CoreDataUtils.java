package com.moremod.core.util;

import com.moremod.core.api.CoreUpgradeEntry;
import com.moremod.core.api.IMechanicalCoreData;
import com.moremod.core.capability.MechanicalCoreCapability;
import com.moremod.core.registry.UpgradeRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

/**
 * 核心数据工具类
 *
 * 提供与原系统兼容的辅助方法
 */
public class CoreDataUtils {

    /**
     * 获取核心数据Capability
     */
    @Nullable
    public static IMechanicalCoreData getData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.getCapability(MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);
    }

    /**
     * 安全的等级设置（兼容原系统的setUpgradeLevelSafe）
     *
     * @param stack ItemStack
     * @param upgradeId 升级ID
     * @param newLevel 新等级
     * @param isManualOperation 是否手动操作（GUI调整）
     */
    public static void setUpgradeLevelSafe(ItemStack stack, String upgradeId, int newLevel, boolean isManualOperation) {
        IMechanicalCoreData data = getData(stack);
        if (data == null) {
            return;
        }

        String canonId = UpgradeRegistry.canonicalIdOf(upgradeId);

        if (isManualOperation) {
            // 手动操作时的特殊逻辑

            // 获取当前拥有的最大等级
            int currentOwnedMax = data.getOwnedMax(canonId);
            int currentLevel = data.getLevel(canonId);

            // 如果没有OwnedMax，初始化
            if (currentOwnedMax <= 0) {
                int initialMax = Math.max(Math.max(currentLevel, newLevel), 1);
                data.setOwnedMax(canonId, initialMax);
            }

            // 如果新等级超过当前拥有的最大值，提升拥有最大值
            if (newLevel > currentOwnedMax) {
                data.setOwnedMax(canonId, newLevel);
            }

            // 如果设置为0，标记为暂停
            if (newLevel == 0) {
                data.pause(canonId);
            } else {
                // 恢复（但不改变等级，由下面的setLevel设置）
                CoreUpgradeEntry entry = data.get(canonId);
                if (entry != null && entry.isPaused()) {
                    entry.setPaused(false);
                }
            }
        }

        // 设置等级
        data.setLevel(canonId, newLevel);

        // 同步到NBT
        saveData(stack, data);
    }

    /**
     * 安全获取拥有的最大等级
     */
    public static int getSafeOwnedMax(ItemStack stack, String upgradeId) {
        IMechanicalCoreData data = getData(stack);
        if (data == null) {
            return 0;
        }

        String canonId = UpgradeRegistry.canonicalIdOf(upgradeId);
        int ownedMax = data.getOwnedMax(canonId);

        // 如果没有OwnedMax但有等级，自动设置
        if (ownedMax <= 0) {
            int level = data.getLevel(canonId);
            if (level > 0) {
                data.setOwnedMax(canonId, level);
                saveData(stack, data);
                return level;
            }
        }

        return ownedMax;
    }

    /**
     * 检查升级是否暂停
     */
    public static boolean isUpgradePaused(ItemStack stack, String upgradeId) {
        IMechanicalCoreData data = getData(stack);
        if (data == null) {
            return false;
        }

        String canonId = UpgradeRegistry.canonicalIdOf(upgradeId);
        return data.isPaused(canonId);
    }

    /**
     * 暂停升级
     */
    public static void pauseUpgrade(ItemStack stack, String upgradeId) {
        IMechanicalCoreData data = getData(stack);
        if (data != null) {
            String canonId = UpgradeRegistry.canonicalIdOf(upgradeId);
            data.pause(canonId);
            saveData(stack, data);
        }
    }

    /**
     * 恢复升级
     */
    public static void resumeUpgrade(ItemStack stack, String upgradeId) {
        IMechanicalCoreData data = getData(stack);
        if (data != null) {
            String canonId = UpgradeRegistry.canonicalIdOf(upgradeId);
            data.resume(canonId);
            saveData(stack, data);
        }
    }

    /**
     * 保存数据到NBT
     */
    public static void saveData(ItemStack stack, IMechanicalCoreData data) {
        if (stack != null && !stack.isEmpty() && data != null) {
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt != null) {
                nbt.setTag("CoreData", data.serializeNBT());
            }
        }
    }

    /**
     * 惩罚系统辅助方法（直接操作NBT，不在Capability中）
     */
    public static class PenaltyUtils {

        public static boolean isPenalized(ItemStack core, String id) {
            if (core == null || core.isEmpty() || !core.hasTagCompound()) {
                return false;
            }
            long exp = core.getTagCompound().getLong("PenaltyExpire_" + id);
            return exp > System.currentTimeMillis();
        }

        public static int getPenaltyCap(ItemStack core, String id) {
            if (core == null || core.isEmpty() || !core.hasTagCompound()) {
                return 0;
            }
            return core.getTagCompound().getInteger("PenaltyCap_" + id);
        }

        public static int getPenaltySecondsLeft(ItemStack core, String id) {
            if (core == null || core.isEmpty() || !core.hasTagCompound()) {
                return 0;
            }
            long exp = core.getTagCompound().getLong("PenaltyExpire_" + id);
            long left = exp - System.currentTimeMillis();
            return left > 0 ? (int)(left / 1000) : 0;
        }

        public static int getPenaltyTier(ItemStack core, String id) {
            if (core == null || core.isEmpty() || !core.hasTagCompound()) {
                return 0;
            }
            return core.getTagCompound().getInteger("PenaltyTier_" + id);
        }

        public static void applyPenalty(ItemStack core, String id, int cap, int seconds,
                                       int tierInc, int debtFE, int debtXP) {
            if (core == null || core.isEmpty()) {
                return;
            }

            NBTTagCompound nbt = getOrCreateNBT(core);
            long expire = System.currentTimeMillis() + Math.max(1000, seconds * 1000L);
            int newTier = Math.max(0, nbt.getInteger("PenaltyTier_" + id) + Math.max(0, tierInc));

            nbt.setInteger("PenaltyCap_" + id, Math.max(1, cap));
            nbt.setLong("PenaltyExpire_" + id, expire);
            nbt.setInteger("PenaltyTier_" + id, newTier);

            if (debtFE > 0) {
                nbt.setInteger("PenaltyDebtFE_" + id, debtFE);
            }
            if (debtXP > 0) {
                nbt.setInteger("PenaltyDebtXP_" + id, debtXP);
            }
        }

        public static void clearPenalty(ItemStack core, String id) {
            if (core == null || core.isEmpty() || !core.hasTagCompound()) {
                return;
            }

            NBTTagCompound nbt = core.getTagCompound();
            nbt.removeTag("PenaltyCap_" + id);
            nbt.removeTag("PenaltyExpire_" + id);
            nbt.removeTag("PenaltyDebtFE_" + id);
            nbt.removeTag("PenaltyDebtXP_" + id);
            nbt.removeTag("PenaltyTier_" + id);
        }

        private static NBTTagCompound getOrCreateNBT(ItemStack stack) {
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            return stack.getTagCompound();
        }
    }

    /**
     * 能量统计工具（直接操作NBT）
     */
    public static class EnergyStatsUtils {

        public static void recordEnergySaved(ItemStack stack, int saved) {
            if (stack == null || stack.isEmpty()) {
                return;
            }

            NBTTagCompound nbt = getOrCreateNBT(stack);
            nbt.setLong("TotalEnergySaved", nbt.getLong("TotalEnergySaved") + saved);
            nbt.setInteger("SessionEnergySaved", nbt.getInteger("SessionEnergySaved") + saved);
        }

        public static long getTotalEnergySaved(ItemStack stack) {
            if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
                return 0;
            }
            return stack.getTagCompound().getLong("TotalEnergySaved");
        }

        public static int getSessionEnergySaved(ItemStack stack) {
            if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
                return 0;
            }
            return stack.getTagCompound().getInteger("SessionEnergySaved");
        }

        public static void resetSessionStats(ItemStack stack) {
            if (stack != null && !stack.isEmpty() && stack.hasTagCompound()) {
                stack.getTagCompound().setInteger("SessionEnergySaved", 0);
            }
        }

        private static NBTTagCompound getOrCreateNBT(ItemStack stack) {
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            return stack.getTagCompound();
        }
    }

    /**
     * 速度模式工具（直接操作NBT）
     */
    public static class SpeedModeUtils {

        public enum SpeedMode {
            NORMAL("标准", 1.0),
            FAST("快速", 1.5),
            ULTRA("极速", 2.0);

            private final String name;
            private final double multiplier;

            SpeedMode(String name, double multiplier) {
                this.name = name;
                this.multiplier = multiplier;
            }

            public String getName() {
                return name;
            }

            public double getMultiplier() {
                return multiplier;
            }
        }

        public static SpeedMode getSpeedMode(ItemStack stack) {
            if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
                return SpeedMode.NORMAL;
            }

            int mode = stack.getTagCompound().getInteger("CoreSpeedMode");
            SpeedMode[] values = SpeedMode.values();
            return values[Math.min(Math.max(0, mode), values.length - 1)];
        }

        public static void setSpeedMode(ItemStack stack, SpeedMode mode) {
            if (stack == null || stack.isEmpty()) {
                return;
            }

            NBTTagCompound nbt = getOrCreateNBT(stack);
            nbt.setInteger("CoreSpeedMode", mode.ordinal());
        }

        public static void cycleSpeedMode(ItemStack stack) {
            SpeedMode current = getSpeedMode(stack);
            SpeedMode[] values = SpeedMode.values();
            SpeedMode next = values[(current.ordinal() + 1) % values.length];
            setSpeedMode(stack, next);
        }

        private static NBTTagCompound getOrCreateNBT(ItemStack stack) {
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            return stack.getTagCompound();
        }
    }
}
