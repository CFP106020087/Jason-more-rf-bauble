package com.moremod.capability.module;

import com.moremod.capability.IMechCoreData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Mechanical Core 模块接口
 *
 * 所有升级模块的统一契约
 *
 * 设计原则：
 *  ✓ 单一职责：一个模块只做一件事
 *  ✓ 无状态：所有状态存在 IMechCoreData 中
 *  ✓ 可测试：纯逻辑，无 static 依赖
 *  ✓ 可配置：通过 NBT 元数据配置
 */
public interface IMechCoreModule {

    // ────────────────────────────────────────────────────────────
    // 模块身份
    // ────────────────────────────────────────────────────────────

    /** 模块唯一 ID（大写下划线格式，例如 FLIGHT_MODULE） */
    String getModuleId();

    /** 模块显示名称 */
    String getDisplayName();

    /** 模块描述 */
    String getDescription();

    /** 最大等级 */
    int getMaxLevel();

    // ────────────────────────────────────────────────────────────
    // 生命周期回调
    // ────────────────────────────────────────────────────────────

    /**
     * 模块激活时调用（首次启用或等级提升）
     * @param player 玩家
     * @param data 核心数据
     * @param newLevel 新等级
     */
    void onActivate(EntityPlayer player, IMechCoreData data, int newLevel);

    /**
     * 模块停用时调用
     * @param player 玩家
     * @param data 核心数据
     */
    void onDeactivate(EntityPlayer player, IMechCoreData data);

    /**
     * 每 Tick 调用（仅在激活状态）
     * @param player 玩家
     * @param data 核心数据
     * @param context 执行上下文（世界、时间等）
     */
    void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context);

    /**
     * 模块等级改变时调用
     * @param player 玩家
     * @param data 核心数据
     * @param oldLevel 旧等级
     * @param newLevel 新等级
     */
    void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel);

    // ────────────────────────────────────────────────────────────
    // 能量 & 条件
    // ────────────────────────────────────────────────────────────

    /**
     * 计算被动能量消耗（RF/tick）
     * @param level 当前等级
     * @return 每 tick 消耗的能量
     */
    int getPassiveEnergyCost(int level);

    /**
     * 计算主动能量消耗（使用技能时）
     * @param level 当前等级
     * @param context 使用上下文
     * @return 能量消耗
     */
    int getActiveEnergyCost(int level, ModuleContext context);

    /**
     * 是否可以执行（检查前置条件）
     * @param player 玩家
     * @param data 核心数据
     * @return 是否满足执行条件
     */
    boolean canExecute(EntityPlayer player, IMechCoreData data);

    // ────────────────────────────────────────────────────────────
    // 配置 & 元数据
    // ────────────────────────────────────────────────────────────

    /**
     * 获取默认元数据（首次激活时使用）
     * @return 默认配置
     */
    NBTTagCompound getDefaultMeta();

    /**
     * 验证元数据（防止数据损坏）
     * @param meta 待验证的元数据
     * @return 是否有效
     */
    boolean validateMeta(NBTTagCompound meta);
}
