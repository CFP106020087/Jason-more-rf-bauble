package com.moremod.core.api;

import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * 机械核心数据的Capability接口
 *
 * 提供统一的API访问所有升级数据，替代原有的NBT直接读写
 *
 * 核心功能：
 * - 升级等级管理（get/set Level）
 * - 暂停/恢复系统
 * - 禁用/启用管理
 * - 惩罚/修复系统
 * - 损坏计数追踪
 * - NBT 序列化/反序列化
 */
public interface IMechanicalCoreData {

    // ===== 基础升级访问 =====

    /**
     * 获取升级条目（如果不存在则返回null）
     *
     * @param upgradeId 升级ID（会自动规范化）
     * @return 升级条目，如果不存在则返回null
     */
    @Nullable
    CoreUpgradeEntry get(String upgradeId);

    /**
     * 获取或创建升级条目（如果不存在则创建新的）
     *
     * @param upgradeId 升级ID（会自动规范化）
     * @return 升级条目，永不为null
     */
    CoreUpgradeEntry getOrCreate(String upgradeId);

    /**
     * 获取所有已安装的升级条目（ownedMax > 0）
     *
     * @return 升级ID -> 升级条目的映射
     */
    Map<String, CoreUpgradeEntry> getAllEntries();

    /**
     * 获取所有已安装的升级ID列表
     *
     * @return 规范化的升级ID列表
     */
    List<String> getInstalledUpgrades();

    // ===== 等级管理 =====

    /**
     * 获取升级的当前等级
     *
     * @param upgradeId 升级ID
     * @return 当前等级（未安装则返回0）
     */
    int getLevel(String upgradeId);

    /**
     * 设置升级等级
     *
     * 注意：会自动限制在 [0, ownedMax] 范围内
     *
     * @param upgradeId 升级ID
     * @param level 要设置的等级
     */
    void setLevel(String upgradeId, int level);

    /**
     * 获取拥有的最大等级
     *
     * @param upgradeId 升级ID
     * @return 拥有的最大等级
     */
    int getOwnedMax(String upgradeId);

    /**
     * 设置拥有的最大等级
     *
     * @param upgradeId 升级ID
     * @param maxLevel 最大等级
     */
    void setOwnedMax(String upgradeId, int maxLevel);

    /**
     * 获取原始最大等级（历史最高等级）
     *
     * @param upgradeId 升级ID
     * @return 原始最大等级
     */
    int getOriginalMax(String upgradeId);

    /**
     * 获取有效等级（考虑暂停和禁用状态）
     *
     * @param upgradeId 升级ID
     * @return 有效等级（暂停或禁用时返回0）
     */
    int getEffectiveLevel(String upgradeId);

    // ===== 暂停/恢复系统 =====

    /**
     * 暂停升级（保存当前等级并设置为0）
     *
     * @param upgradeId 升级ID
     */
    void pause(String upgradeId);

    /**
     * 恢复升级（从暂停前的等级恢复）
     *
     * @param upgradeId 升级ID
     */
    void resume(String upgradeId);

    /**
     * 检查升级是否暂停
     *
     * @param upgradeId 升级ID
     * @return 是否暂停
     */
    boolean isPaused(String upgradeId);

    /**
     * 获取上次等级（暂停前的等级）
     *
     * @param upgradeId 升级ID
     * @return 上次等级
     */
    int getLastLevel(String upgradeId);

    // ===== 禁用/启用系统 =====

    /**
     * 禁用升级
     *
     * @param upgradeId 升级ID
     * @param disabled 是否禁用
     */
    void setDisabled(String upgradeId, boolean disabled);

    /**
     * 检查升级是否禁用
     *
     * @param upgradeId 升级ID
     * @return 是否禁用
     */
    boolean isDisabled(String upgradeId);

    // ===== 激活状态 =====

    /**
     * 检查升级是否激活（等级 > 0 且未暂停且未禁用）
     *
     * @param upgradeId 升级ID
     * @return 是否激活
     */
    boolean isActive(String upgradeId);

    /**
     * 检查升级是否已安装（ownedMax > 0）
     *
     * @param upgradeId 升级ID
     * @return 是否已安装
     */
    boolean isInstalled(String upgradeId);

    // ===== 惩罚/修复系统 =====

    /**
     * 降级升级（减少拥有的最大等级）
     *
     * @param upgradeId 升级ID
     * @param amount 降级数量
     */
    void degrade(String upgradeId, int amount);

    /**
     * 修复升级（恢复拥有的最大等级）
     *
     * @param upgradeId 升级ID
     * @param targetLevel 目标等级
     * @return 是否修复成功
     */
    boolean repair(String upgradeId, int targetLevel);

    /**
     * 完全修复升级到原始等级
     *
     * @param upgradeId 升级ID
     */
    void fullRepair(String upgradeId);

    /**
     * 检查升级是否损坏
     *
     * @param upgradeId 升级ID
     * @return 是否损坏（ownedMax < originalMax）
     */
    boolean isDamaged(String upgradeId);

    /**
     * 获取损坏计数
     *
     * @param upgradeId 升级ID
     * @return 损坏计数
     */
    int getDamageCount(String upgradeId);

    /**
     * 获取累计总损坏次数
     *
     * @param upgradeId 升级ID
     * @return 累计总损坏次数
     */
    int getTotalDamageCount(String upgradeId);

    /**
     * 检查是否被惩罚过
     *
     * @param upgradeId 升级ID
     * @return 是否被惩罚过
     */
    boolean wasPunished(String upgradeId);

    /**
     * 设置惩罚标记
     *
     * @param upgradeId 升级ID
     * @param punished 是否被惩罚
     */
    void setWasPunished(String upgradeId, boolean punished);

    // ===== 批量操作 =====

    /**
     * 移除升级（完全删除升级数据）
     *
     * @param upgradeId 升级ID
     */
    void remove(String upgradeId);

    /**
     * 重置升级（设置等级为0，但保留安装信息）
     *
     * @param upgradeId 升级ID
     */
    void reset(String upgradeId);

    /**
     * 清空所有升级数据
     */
    void clear();

    // ===== 统计信息 =====

    /**
     * 获取已安装的升级数量
     *
     * @return 已安装的升级数量
     */
    int getInstalledCount();

    /**
     * 获取激活的升级数量
     *
     * @return 激活的升级数量
     */
    int getActiveCount();

    /**
     * 获取总等级（所有升级的等级之和）
     *
     * @return 总等级
     */
    int getTotalLevel();

    /**
     * 获取总激活等级（所有激活升级的等级之和）
     *
     * @return 总激活等级
     */
    int getTotalActiveLevel();

    // ===== NBT 序列化 =====

    /**
     * 写入NBT
     *
     * @param nbt NBT标签
     * @return 写入后的NBT标签
     */
    NBTTagCompound writeToNBT(NBTTagCompound nbt);

    /**
     * 从NBT读取
     *
     * @param nbt NBT标签
     */
    void readFromNBT(NBTTagCompound nbt);

    /**
     * 序列化为NBT
     *
     * @return NBT标签
     */
    NBTTagCompound serializeNBT();

    /**
     * 从NBT反序列化
     *
     * @param nbt NBT标签
     */
    void deserializeNBT(NBTTagCompound nbt);
}
