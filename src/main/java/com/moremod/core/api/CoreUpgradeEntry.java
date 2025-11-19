package com.moremod.core.api;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 单个升级模块的完整数据模型
 *
 * 包含升级的所有状态信息：
 * - 当前等级
 * - 拥有的最大等级（可因惩罚降低）
 * - 原始最大等级（历史最高等级）
 * - 上次等级（用于暂停/恢复）
 * - 损坏计数
 * - 惩罚标记
 * - 暂停状态
 * - 禁用状态
 */
public class CoreUpgradeEntry {

    private int level;           // 当前等级
    private int ownedMax;        // 拥有的最大等级（可因惩罚降低）
    private int originalMax;     // 原始最大等级（历史最高）
    private int lastLevel;       // 上次等级（暂停前的等级）
    private int damageCount;     // 损坏计数
    private int totalDamageCount; // 累计总损坏次数
    private boolean wasPunished;  // 是否被惩罚过
    private boolean isPaused;     // 是否暂停
    private boolean isDisabled;   // 是否禁用

    /**
     * 创建一个空的升级条目（全部初始化为0/false）
     */
    public CoreUpgradeEntry() {
        this(0, 0, 0, 0, 0, 0, false, false, false);
    }

    /**
     * 创建一个指定等级的新升级
     */
    public CoreUpgradeEntry(int level, int maxLevel) {
        this(level, maxLevel, maxLevel, 0, 0, 0, false, false, false);
    }

    /**
     * 完整构造函数
     */
    public CoreUpgradeEntry(int level, int ownedMax, int originalMax, int lastLevel,
                           int damageCount, int totalDamageCount,
                           boolean wasPunished, boolean isPaused, boolean isDisabled) {
        this.level = level;
        this.ownedMax = ownedMax;
        this.originalMax = originalMax;
        this.lastLevel = lastLevel;
        this.damageCount = damageCount;
        this.totalDamageCount = totalDamageCount;
        this.wasPunished = wasPunished;
        this.isPaused = isPaused;
        this.isDisabled = isDisabled;
    }

    // ===== Getters & Setters =====

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(0, level);
    }

    public int getOwnedMax() {
        return ownedMax;
    }

    public void setOwnedMax(int ownedMax) {
        this.ownedMax = Math.max(0, ownedMax);
    }

    public int getOriginalMax() {
        return originalMax;
    }

    public void setOriginalMax(int originalMax) {
        this.originalMax = Math.max(0, originalMax);
    }

    public int getLastLevel() {
        return lastLevel;
    }

    public void setLastLevel(int lastLevel) {
        this.lastLevel = Math.max(0, lastLevel);
    }

    public int getDamageCount() {
        return damageCount;
    }

    public void setDamageCount(int damageCount) {
        this.damageCount = Math.max(0, damageCount);
    }

    public int getTotalDamageCount() {
        return totalDamageCount;
    }

    public void setTotalDamageCount(int totalDamageCount) {
        this.totalDamageCount = Math.max(0, totalDamageCount);
    }

    public boolean wasPunished() {
        return wasPunished;
    }

    public void setWasPunished(boolean wasPunished) {
        this.wasPunished = wasPunished;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public void setDisabled(boolean disabled) {
        this.isDisabled = disabled;
    }

    // ===== 逻辑方法 =====

    /**
     * 是否已安装（拥有最大等级 > 0）
     */
    public boolean isInstalled() {
        return ownedMax > 0;
    }

    /**
     * 是否激活（等级 > 0 且未暂停且未禁用）
     */
    public boolean isActive() {
        return level > 0 && !isPaused && !isDisabled;
    }

    /**
     * 是否损坏（拥有的最大等级 < 原始最大等级）
     */
    public boolean isDamaged() {
        return ownedMax < originalMax && originalMax > 0;
    }

    /**
     * 暂停升级（记住当前等级，然后设置为0）
     */
    public void pause() {
        if (!isPaused && level > 0) {
            lastLevel = level;
            level = 0;
            isPaused = true;
        }
    }

    /**
     * 恢复升级（从上次等级恢复）
     */
    public void resume() {
        if (isPaused) {
            level = Math.min(lastLevel, ownedMax);
            isPaused = false;
        }
    }

    /**
     * 降级（减少拥有的最大等级，并调整当前等级）
     */
    public void degrade(int amount) {
        // 首次降级时记录原始最大等级
        if (originalMax == 0 && ownedMax > 0) {
            originalMax = ownedMax;
        }

        ownedMax = Math.max(0, ownedMax - amount);

        // 调整当前等级不超过新的最大值
        if (level > ownedMax) {
            level = ownedMax;
        }

        // 标记为惩罚过并增加损坏计数
        wasPunished = true;
        damageCount += amount;
        totalDamageCount += amount;
    }

    /**
     * 修复（恢复拥有的最大等级）
     */
    public void repair(int targetLevel) {
        if (targetLevel > ownedMax && targetLevel <= originalMax) {
            int repaired = targetLevel - ownedMax;
            ownedMax = targetLevel;

            // 减少损坏计数
            damageCount = Math.max(0, damageCount - repaired);

            // 如果完全修复，清除惩罚标记
            if (ownedMax >= originalMax) {
                wasPunished = false;
            }
        }
    }

    /**
     * 完全修复到原始等级
     */
    public void fullRepair() {
        if (originalMax > 0) {
            ownedMax = originalMax;
            level = originalMax;
            damageCount = 0;
            wasPunished = false;
        }
    }

    // ===== NBT 序列化 =====

    /**
     * 写入NBT
     */
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("Level", level);
        nbt.setInteger("OwnedMax", ownedMax);
        nbt.setInteger("OriginalMax", originalMax);
        nbt.setInteger("LastLevel", lastLevel);
        nbt.setInteger("DamageCount", damageCount);
        nbt.setInteger("TotalDamageCount", totalDamageCount);
        nbt.setBoolean("WasPunished", wasPunished);
        nbt.setBoolean("IsPaused", isPaused);
        nbt.setBoolean("IsDisabled", isDisabled);
        return nbt;
    }

    /**
     * 从NBT读取
     */
    public void readFromNBT(NBTTagCompound nbt) {
        level = nbt.getInteger("Level");
        ownedMax = nbt.getInteger("OwnedMax");
        originalMax = nbt.getInteger("OriginalMax");
        lastLevel = nbt.getInteger("LastLevel");
        damageCount = nbt.getInteger("DamageCount");
        totalDamageCount = nbt.getInteger("TotalDamageCount");
        wasPunished = nbt.getBoolean("WasPunished");
        isPaused = nbt.getBoolean("IsPaused");
        isDisabled = nbt.getBoolean("IsDisabled");
    }

    /**
     * 创建NBT标签
     */
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        return writeToNBT(nbt);
    }

    /**
     * 从NBT标签创建
     */
    public static CoreUpgradeEntry deserializeNBT(NBTTagCompound nbt) {
        CoreUpgradeEntry entry = new CoreUpgradeEntry();
        entry.readFromNBT(nbt);
        return entry;
    }

    /**
     * 复制
     */
    public CoreUpgradeEntry copy() {
        return new CoreUpgradeEntry(level, ownedMax, originalMax, lastLevel,
                                   damageCount, totalDamageCount,
                                   wasPunished, isPaused, isDisabled);
    }

    @Override
    public String toString() {
        return String.format("CoreUpgradeEntry[level=%d, ownedMax=%d, originalMax=%d, " +
                           "lastLevel=%d, damageCount=%d, totalDamageCount=%d, " +
                           "wasPunished=%b, isPaused=%b, isDisabled=%b]",
                level, ownedMax, originalMax, lastLevel, damageCount, totalDamageCount,
                wasPunished, isPaused, isDisabled);
    }
}
