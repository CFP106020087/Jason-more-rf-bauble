package com.moremod.system.humanity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * 人性值光谱系统 - Capability 接口
 * Humanity Spectrum System - Capability Interface
 *
 * 人性值不是道德判断，而是「你在世界物理规则中属于哪个分类」
 * 高人性 = 被世界纳入自然系统的合法生命
 * 低人性 = 世界无法分类的异常现象
 */
public interface IHumanityData {

    // ========== 核心数值 ==========

    /**
     * 获取人性值 (0.0 - 100.0)
     */
    float getHumanity();

    /**
     * 设置人性值
     * @param value 目标值，会被限制在 0.0 - 100.0 范围内
     */
    void setHumanity(float value);

    /**
     * 修改人性值（增减）
     * @param delta 变化量，正数增加，负数减少
     */
    void modifyHumanity(float delta);

    // ========== 系统状态 ==========

    /**
     * 检查人性值系统是否已激活
     * 必须完成排异系统的「突破」后才会激活
     */
    boolean isSystemActive();

    /**
     * 激活人性值系统（突破时调用）
     */
    void activateSystem();

    /**
     * 停用人性值系统
     */
    void deactivateSystem();

    // ========== 崩解状态 (Dissolution) ==========

    /**
     * 检查是否处于崩解状态
     * 人性值降到0时触发
     */
    boolean isDissolutionActive();

    /**
     * 获取崩解剩余时间 (ticks)
     */
    int getDissolutionTicks();

    /**
     * 设置崩解剩余时间
     */
    void setDissolutionTicks(int ticks);

    /**
     * 开始崩解状态
     * @param durationTicks 崩解持续时间（tick）
     */
    void startDissolution(int durationTicks);

    /**
     * 结束崩解状态
     * @param survived 是否存活（存活=强制回弹，死亡=重置到50%）
     */
    void endDissolution(boolean survived);

    // ========== 存在锚定 (Existence Anchor) ==========

    /**
     * 获取存在锚定结束时间（世界时间戳）
     * 在锚定期间内，人性不能低于10%
     */
    long getExistenceAnchorUntil();

    /**
     * 设置存在锚定结束时间
     */
    void setExistenceAnchorUntil(long worldTime);

    /**
     * 检查是否处于存在锚定状态
     * @param currentWorldTime 当前世界时间
     */
    boolean isExistenceAnchored(long currentWorldTime);

    // ========== 猎人协议 (Hunter Protocol) ==========

    /**
     * 获取所有生物档案
     */
    Map<ResourceLocation, BiologicalProfile> getProfiles();

    /**
     * 获取已激活的档案集合
     */
    Set<ResourceLocation> getActiveProfiles();

    /**
     * 获取指定生物的档案
     */
    @Nullable
    BiologicalProfile getProfile(ResourceLocation entityId);

    /**
     * 添加或更新生物档案
     */
    void setProfile(ResourceLocation entityId, BiologicalProfile profile);

    /**
     * 添加样本
     * @param entityId 生物ID
     */
    void addSample(ResourceLocation entityId);

    /**
     * 增加击杀计数
     */
    void incrementKillCount(ResourceLocation entityId);

    /**
     * 获取指定生物的击杀计数
     */
    int getKillCount(ResourceLocation entityId);

    /**
     * 激活档案
     * @return 是否成功激活
     */
    boolean activateProfile(ResourceLocation entityId);

    /**
     * 停用档案
     */
    void deactivateProfile(ResourceLocation entityId);

    /**
     * 获取最大可激活档案数量
     * 基于当前人性值计算
     */
    int getMaxActiveProfiles();

    // ========== 分析系统 ==========

    /**
     * 获取当前正在分析的生物ID
     */
    @Nullable
    ResourceLocation getAnalyzingEntity();

    /**
     * 获取分析进度 (0-100)
     */
    int getAnalysisProgress();

    /**
     * 开始分析
     * @param entityId 要分析的生物ID
     * @return 是否成功开始
     */
    boolean startAnalysis(ResourceLocation entityId);

    /**
     * 取消当前分析
     */
    void cancelAnalysis();

    /**
     * 分析完成处理
     */
    void completeAnalysis();

    /**
     * 每tick调用，推进分析进度
     * @param energyAvailable 可用能量
     * @return 实际消耗的能量
     */
    int tickAnalysis(int energyAvailable);

    // ========== 战斗状态追踪 ==========

    /**
     * 获取上次战斗时间
     */
    long getLastCombatTime();

    /**
     * 设置上次战斗时间
     */
    void setLastCombatTime(long time);

    /**
     * 检查是否处于战斗状态
     * @param currentTime 当前时间
     * @param combatTimeout 战斗超时时间(ticks)
     */
    boolean isInCombat(long currentTime, int combatTimeout);

    // ========== 睡眠追踪 ==========

    /**
     * 获取自上次睡眠以来经过的ticks
     */
    long getTicksSinceLastSleep();

    /**
     * 设置上次睡眠时间
     */
    void setLastSleepTime(long time);

    /**
     * 重置睡眠计数器（睡觉时调用）
     */
    void resetSleepDeprivation();

    // ========== 升格路线 (Ascension Route) ==========

    /**
     * 获取当前升格路线
     */
    AscensionRoute getAscensionRoute();

    /**
     * 设置升格路线
     */
    void setAscensionRoute(AscensionRoute route);

    /**
     * 获取崩解存活次数（用于破碎之神升格条件）
     */
    int getDissolutionSurvivals();

    /**
     * 增加崩解存活次数
     */
    void incrementDissolutionSurvivals();

    /**
     * 获取低人性值累计时间（tick）
     * 用于破碎之神升格条件
     */
    long getLowHumanityTicks();

    /**
     * 设置低人性值累计时间
     */
    void setLowHumanityTicks(long ticks);

    /**
     * 增加低人性值累计时间
     */
    void addLowHumanityTicks(long ticks);

    // ========== 香巴拉升格条件 ==========

    /**
     * 获取高人性值累计时间（tick）
     * 用于香巴拉升格条件
     */
    long getHighHumanityTicks();

    /**
     * 设置高人性值累计时间
     */
    void setHighHumanityTicks(long ticks);

    /**
     * 增加高人性值累计时间
     */
    void addHighHumanityTicks(long ticks);

    // ========== 破碎之神专用 ==========

    /**
     * 获取运转值（破碎之神需要维护）
     * 0-100，低于20会受到持续伤害
     */
    int getOperationValue();

    /**
     * 设置运转值
     */
    void setOperationValue(int value);

    /**
     * 修改运转值
     */
    void modifyOperationValue(int delta);

    // ========== 停机模式 (Shutdown Mode) ==========

    /**
     * 检查是否处于停机状态
     */
    boolean isInShutdown();

    /**
     * 获取停机剩余时间（ticks）
     */
    int getShutdownTimer();

    /**
     * 设置停机剩余时间
     */
    void setShutdownTimer(int ticks);

    // ========== NBT序列化 ==========

    /**
     * 序列化为NBT
     */
    NBTTagCompound serializeNBT();

    /**
     * 从NBT反序列化
     */
    void deserializeNBT(NBTTagCompound nbt);

    /**
     * 复制数据（用于死亡重生）
     */
    void copyFrom(IHumanityData other);

    // ========== 高人性情报系统 (High Humanity Intel System) ==========

    /**
     * 获取已学习的情报（生物ID -> 学习次数）
     * 这是高人性玩家的特攻系统，每学习一次情报书提供 +10% 伤害
     */
    Map<ResourceLocation, Integer> getLearnedIntel();

    /**
     * 获取对指定生物的情报等级
     */
    int getIntelLevel(ResourceLocation entityId);

    /**
     * 设置对指定生物的情报等级
     */
    void setIntelLevel(ResourceLocation entityId, int level);
}
