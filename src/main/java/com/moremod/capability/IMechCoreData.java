package com.moremod.capability;

import com.moremod.capability.module.ModuleContainer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

/**
 * Mechanical Core 玩家数据能力接口
 *
 * 职责：
 *  ✓ 存储玩家的核心数据（能量、模块等级）
 *  ✓ 提供模块容器访问
 *  ✓ NBT 序列化/反序列化
 *  ✓ 网络同步标记
 *  ✓ 完全脱离 ItemStack NBT
 */
public interface IMechCoreData {

    @CapabilityInject(IMechCoreData.class)
    Capability<IMechCoreData> CAPABILITY = null;

    // ────────────────────────────────────────────────────────────
    // 核心状态数据
    // ────────────────────────────────────────────────────────────

    /** 获取当前能量 (RF) */
    int getEnergy();

    /** 设置能量 */
    void setEnergy(int amount);

    /** 消耗能量（返回是否成功） */
    boolean consumeEnergy(int amount);

    /** 增加能量（用于发电模块，返回实际增加量） */
    int addEnergy(int amount);

    /** 接收能量（返回实际接收量） */
    int receiveEnergy(int maxReceive);

    /** 获取最大能量容量 */
    int getMaxEnergy();

    /** 设置最大能量容量 */
    void setMaxEnergy(int max);

    // ────────────────────────────────────────────────────────────
    // 模块系统
    // ────────────────────────────────────────────────────────────

    /** 获取模块容器 */
    ModuleContainer getModuleContainer();

    /** 获取模块等级（快捷方法） */
    int getModuleLevel(String moduleId);


    /** 设置模块等级 */
    void setModuleLevel(String moduleId, int level);

    /** 模块是否激活 */
    boolean isModuleActive(String moduleId);

    /** 激活/停用模块 */
    void setModuleActive(String moduleId, boolean active);

    /** 获取模块原始最高等级（用于修复系统） */
    int getOriginalMaxLevel(String moduleId);

    /** 设置模块原始最高等级（永不降低） */
    void setOriginalMaxLevel(String moduleId, int maxLevel);

    // ────────────────────────────────────────────────────────────
    // 元数据（紧凑存储）
    // ────────────────────────────────────────────────────────────

    /** 获取模块元数据 */
    NBTTagCompound getModuleMeta(String moduleId);

    /** 设置模块元数据 */
    void setModuleMeta(String moduleId, NBTTagCompound meta);

    // ────────────────────────────────────────────────────────────
    // 生命周期 & 同步
    // ────────────────────────────────────────────────────────────

    /** 标记为脏（需要同步） */
    void markDirty();

    /** 是否需要同步 */
    boolean isDirty();

    /** 清除脏标记 */
    void clearDirty();

    /** 序列化到 NBT */
    NBTTagCompound serializeNBT();

    /** 从 NBT 反序列化 */
    void deserializeNBT(NBTTagCompound nbt);

    /** 复制数据（用于死亡保留） */
    void copyFrom(IMechCoreData source);
}