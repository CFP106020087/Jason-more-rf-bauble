package com.moremod.core.capability;

import com.moremod.core.api.CoreUpgradeEntry;
import com.moremod.core.api.IMechanicalCoreData;
import com.moremod.core.registry.UpgradeRegistry;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 机械核心数据的默认实现
 *
 * 使用Map存储所有升级数据，键为规范化的升级ID
 */
public class MechanicalCoreData implements IMechanicalCoreData {

    // 存储所有升级数据（升级ID -> 升级条目）
    private final Map<String, CoreUpgradeEntry> upgrades = new LinkedHashMap<>();

    // ===== 基础升级访问 =====

    @Nullable
    @Override
    public CoreUpgradeEntry get(String upgradeId) {
        String canonId = canonical(upgradeId);
        return upgrades.get(canonId);
    }

    @Override
    public CoreUpgradeEntry getOrCreate(String upgradeId) {
        String canonId = canonical(upgradeId);
        return upgrades.computeIfAbsent(canonId, k -> new CoreUpgradeEntry());
    }

    @Override
    public Map<String, CoreUpgradeEntry> getAllEntries() {
        return upgrades.entrySet().stream()
                .filter(e -> e.getValue().isInstalled())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public List<String> getInstalledUpgrades() {
        return upgrades.entrySet().stream()
                .filter(e -> e.getValue().isInstalled())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // ===== 等级管理 =====

    @Override
    public int getLevel(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        return entry != null ? entry.getLevel() : 0;
    }

    @Override
    public void setLevel(String upgradeId, int level) {
        CoreUpgradeEntry entry = getOrCreate(upgradeId);

        // 限制在有效范围内
        int clampedLevel = Math.max(0, Math.min(level, entry.getOwnedMax()));
        entry.setLevel(clampedLevel);

        // 如果设置为0以上，确保ownedMax至少为该等级
        if (level > 0 && entry.getOwnedMax() == 0) {
            entry.setOwnedMax(level);
            if (entry.getOriginalMax() == 0) {
                entry.setOriginalMax(level);
            }
        }
    }

    @Override
    public int getOwnedMax(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        return entry != null ? entry.getOwnedMax() : 0;
    }

    @Override
    public void setOwnedMax(String upgradeId, int maxLevel) {
        CoreUpgradeEntry entry = getOrCreate(upgradeId);
        entry.setOwnedMax(maxLevel);

        // 首次设置时，同时设置原始最大等级
        if (entry.getOriginalMax() == 0 && maxLevel > 0) {
            entry.setOriginalMax(maxLevel);
        }

        // 如果当前等级超过新的最大值，调整当前等级
        if (entry.getLevel() > maxLevel) {
            entry.setLevel(maxLevel);
        }
    }

    @Override
    public int getOriginalMax(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        return entry != null ? entry.getOriginalMax() : 0;
    }

    @Override
    public int getEffectiveLevel(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        if (entry == null || !entry.isActive()) {
            return 0;
        }
        return entry.getLevel();
    }

    // ===== 暂停/恢复系统 =====

    @Override
    public void pause(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        if (entry != null) {
            entry.pause();
        }
    }

    @Override
    public void resume(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        if (entry != null) {
            entry.resume();
        }
    }

    @Override
    public boolean isPaused(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        return entry != null && entry.isPaused();
    }

    @Override
    public int getLastLevel(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        return entry != null ? entry.getLastLevel() : 0;
    }

    // ===== 禁用/启用系统 =====

    @Override
    public void setDisabled(String upgradeId, boolean disabled) {
        CoreUpgradeEntry entry = getOrCreate(upgradeId);
        entry.setDisabled(disabled);
    }

    @Override
    public boolean isDisabled(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        return entry != null && entry.isDisabled();
    }

    // ===== 激活状态 =====

    @Override
    public boolean isActive(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        return entry != null && entry.isActive();
    }

    @Override
    public boolean isInstalled(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        return entry != null && entry.isInstalled();
    }

    // ===== 惩罚/修复系统 =====

    @Override
    public void degrade(String upgradeId, int amount) {
        CoreUpgradeEntry entry = get(upgradeId);
        if (entry != null && entry.isInstalled()) {
            entry.degrade(amount);
        }
    }

    @Override
    public boolean repair(String upgradeId, int targetLevel) {
        CoreUpgradeEntry entry = get(upgradeId);
        if (entry == null || !entry.isInstalled()) {
            return false;
        }

        int currentMax = entry.getOwnedMax();
        int originalMax = entry.getOriginalMax();

        // 检查是否可以修复到目标等级
        if (targetLevel > originalMax || targetLevel <= currentMax) {
            return false;
        }

        entry.repair(targetLevel);
        return true;
    }

    @Override
    public void fullRepair(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        if (entry != null && entry.isInstalled()) {
            entry.fullRepair();
        }
    }

    @Override
    public boolean isDamaged(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        return entry != null && entry.isDamaged();
    }

    @Override
    public int getDamageCount(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        return entry != null ? entry.getDamageCount() : 0;
    }

    @Override
    public int getTotalDamageCount(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        return entry != null ? entry.getTotalDamageCount() : 0;
    }

    @Override
    public boolean wasPunished(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        return entry != null && entry.wasPunished();
    }

    @Override
    public void setWasPunished(String upgradeId, boolean punished) {
        CoreUpgradeEntry entry = getOrCreate(upgradeId);
        entry.setWasPunished(punished);
    }

    // ===== 批量操作 =====

    @Override
    public void remove(String upgradeId) {
        String canonId = canonical(upgradeId);
        upgrades.remove(canonId);
    }

    @Override
    public void reset(String upgradeId) {
        CoreUpgradeEntry entry = get(upgradeId);
        if (entry != null) {
            entry.setLevel(0);
            entry.setPaused(false);
            entry.setDisabled(false);
        }
    }

    @Override
    public void clear() {
        upgrades.clear();
    }

    // ===== 统计信息 =====

    @Override
    public int getInstalledCount() {
        return (int) upgrades.values().stream()
                .filter(CoreUpgradeEntry::isInstalled)
                .count();
    }

    @Override
    public int getActiveCount() {
        return (int) upgrades.values().stream()
                .filter(CoreUpgradeEntry::isActive)
                .count();
    }

    @Override
    public int getTotalLevel() {
        return upgrades.values().stream()
                .filter(CoreUpgradeEntry::isInstalled)
                .mapToInt(CoreUpgradeEntry::getLevel)
                .sum();
    }

    @Override
    public int getTotalActiveLevel() {
        return upgrades.values().stream()
                .filter(CoreUpgradeEntry::isActive)
                .mapToInt(CoreUpgradeEntry::getLevel)
                .sum();
    }

    // ===== NBT 序列化 =====

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagCompound upgradesNBT = new NBTTagCompound();

        for (Map.Entry<String, CoreUpgradeEntry> entry : upgrades.entrySet()) {
            String upgradeId = entry.getKey();
            CoreUpgradeEntry upgradeEntry = entry.getValue();

            // 只保存已安装的升级
            if (upgradeEntry.isInstalled() || upgradeEntry.getOriginalMax() > 0) {
                NBTTagCompound entryNBT = new NBTTagCompound();
                upgradeEntry.writeToNBT(entryNBT);
                upgradesNBT.setTag(upgradeId, entryNBT);
            }
        }

        nbt.setTag("Upgrades", upgradesNBT);
        nbt.setString("Version", "3.0"); // 标记为新版本数据
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        upgrades.clear();

        if (nbt.hasKey("Upgrades")) {
            NBTTagCompound upgradesNBT = nbt.getCompoundTag("Upgrades");

            for (String upgradeId : upgradesNBT.getKeySet()) {
                NBTTagCompound entryNBT = upgradesNBT.getCompoundTag(upgradeId);
                CoreUpgradeEntry entry = new CoreUpgradeEntry();
                entry.readFromNBT(entryNBT);

                String canonId = canonical(upgradeId);
                upgrades.put(canonId, entry);
            }
        }
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        return writeToNBT(nbt);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        readFromNBT(nbt);
    }

    // ===== 辅助方法 =====

    /**
     * 规范化升级ID（去空白 + 全大写，并查询别名映射）
     */
    private String canonical(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }

        // 去除空白并转大写
        String normalized = id.trim().toUpperCase(Locale.ROOT);

        // 查询别名映射表，获取规范ID
        return UpgradeRegistry.canonicalIdOf(normalized);
    }

    @Override
    public String toString() {
        int installed = getInstalledCount();
        int active = getActiveCount();
        int totalLevel = getTotalLevel();
        return String.format("MechanicalCoreData[installed=%d, active=%d, totalLevel=%d]",
                installed, active, totalLevel);
    }
}
