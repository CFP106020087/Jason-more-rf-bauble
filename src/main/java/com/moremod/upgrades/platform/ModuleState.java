package com.moremod.upgrades.platform;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 模块状态封装类
 *
 * 功能：
 * - 保存模块的运行时状态（等级、暂停、冷却等）
 * - 支持自定义数据存储
 * - 自动序列化到 NBT
 */
public class ModuleState {

    private final String moduleId;
    private int level;
    private int ownedMaxLevel;
    private boolean paused;
    private boolean disabled;
    private long cooldownExpireTime;
    private NBTTagCompound customData;

    public ModuleState(String moduleId) {
        this.moduleId = moduleId;
        this.level = 0;
        this.ownedMaxLevel = 0;
        this.paused = false;
        this.disabled = false;
        this.cooldownExpireTime = 0;
        this.customData = new NBTTagCompound();
    }

    // ===== 基础状态访问 =====

    public String getModuleId() {
        return moduleId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(0, level);
        if (level > ownedMaxLevel) {
            ownedMaxLevel = level;
        }
    }

    public int getOwnedMaxLevel() {
        return ownedMaxLevel;
    }

    public void setOwnedMaxLevel(int max) {
        this.ownedMaxLevel = Math.max(0, max);
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isActive() {
        return level > 0 && !paused && !disabled;
    }

    public int getEffectiveLevel() {
        return isActive() ? level : 0;
    }

    // ===== 冷却系统 =====

    public boolean isOnCooldown(long currentTime) {
        return currentTime < cooldownExpireTime;
    }

    public void setCooldown(long currentTime, long duration) {
        this.cooldownExpireTime = currentTime + duration;
    }

    public void clearCooldown() {
        this.cooldownExpireTime = 0;
    }

    public long getRemainingCooldown(long currentTime) {
        if (!isOnCooldown(currentTime)) {
            return 0;
        }
        return cooldownExpireTime - currentTime;
    }

    // ===== 自定义数据存储 =====

    public NBTTagCompound getCustomData() {
        return customData;
    }

    public void setCustomInt(String key, int value) {
        customData.setInteger(key, value);
    }

    public int getCustomInt(String key, int defaultValue) {
        return customData.hasKey(key) ? customData.getInteger(key) : defaultValue;
    }

    public void setCustomLong(String key, long value) {
        customData.setLong(key, value);
    }

    public long getCustomLong(String key, long defaultValue) {
        return customData.hasKey(key) ? customData.getLong(key) : defaultValue;
    }

    public void setCustomBoolean(String key, boolean value) {
        customData.setBoolean(key, value);
    }

    public boolean getCustomBoolean(String key, boolean defaultValue) {
        return customData.hasKey(key) ? customData.getBoolean(key) : defaultValue;
    }

    public void setCustomString(String key, String value) {
        customData.setString(key, value);
    }

    public String getCustomString(String key, String defaultValue) {
        return customData.hasKey(key) ? customData.getString(key) : defaultValue;
    }

    // ===== NBT 序列化 =====

    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("moduleId", moduleId);
        nbt.setInteger("level", level);
        nbt.setInteger("ownedMax", ownedMaxLevel);
        nbt.setBoolean("paused", paused);
        nbt.setBoolean("disabled", disabled);
        nbt.setLong("cooldown", cooldownExpireTime);
        if (!customData.isEmpty()) {
            nbt.setTag("custom", customData.copy());
        }
        return nbt;
    }

    public void deserializeNBT(NBTTagCompound nbt) {
        this.level = nbt.getInteger("level");
        this.ownedMaxLevel = nbt.getInteger("ownedMax");
        this.paused = nbt.getBoolean("paused");
        this.disabled = nbt.getBoolean("disabled");
        this.cooldownExpireTime = nbt.getLong("cooldown");
        if (nbt.hasKey("custom")) {
            this.customData = nbt.getCompoundTag("custom");
        }
    }

    // ===== 工具方法 =====

    public void reset() {
        this.level = 0;
        this.paused = false;
        this.disabled = false;
        this.cooldownExpireTime = 0;
        this.customData = new NBTTagCompound();
    }

    @Override
    public String toString() {
        return String.format("ModuleState{id=%s, level=%d/%d, active=%s}",
                moduleId, level, ownedMaxLevel, isActive());
    }
}
