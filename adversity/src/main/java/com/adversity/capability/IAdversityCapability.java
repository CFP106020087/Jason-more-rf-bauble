package com.adversity.capability;

import com.adversity.affix.AffixData;
import com.adversity.affix.IAffix;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * 逆境 Capability 接口 - 存储实体的词条和难度数据
 */
public interface IAdversityCapability {

    // ==================== 词条管理 ====================

    /**
     * 添加词条
     *
     * @return 如果添加成功返回 true
     */
    boolean addAffix(IAffix affix);

    /**
     * 移除词条
     *
     * @return 如果移除成功返回 true
     */
    boolean removeAffix(IAffix affix);

    /**
     * 检查是否拥有指定词条
     */
    boolean hasAffix(IAffix affix);

    /**
     * 获取指定词条的数据
     */
    @Nullable
    AffixData getAffixData(IAffix affix);

    /**
     * 获取所有词条数据
     */
    Collection<AffixData> getAllAffixData();

    /**
     * 获取词条数量
     */
    int getAffixCount();

    /**
     * 清除所有词条
     */
    void clearAffixes();

    // ==================== 难度数据 ====================

    /**
     * 获取该实体的难度等级
     */
    float getDifficultyLevel();

    /**
     * 设置难度等级
     */
    void setDifficultyLevel(float level);

    /**
     * 获取该实体的等级（用于显示）
     */
    int getTier();

    /**
     * 设置等级
     */
    void setTier(int tier);

    // ==================== 属性修正 ====================

    /**
     * 获取生命值倍率
     */
    float getHealthMultiplier();

    /**
     * 设置生命值倍率
     */
    void setHealthMultiplier(float multiplier);

    /**
     * 获取伤害倍率
     */
    float getDamageMultiplier();

    /**
     * 设置伤害倍率
     */
    void setDamageMultiplier(float multiplier);

    // ==================== 序列化 ====================

    /**
     * 序列化到 NBT
     */
    NBTTagCompound serializeNBT();

    /**
     * 从 NBT 反序列化
     */
    void deserializeNBT(NBTTagCompound nbt);

    // ==================== 标记 ====================

    /**
     * 检查该实体是否已被逆境系统处理
     */
    boolean isProcessed();

    /**
     * 标记该实体已被处理
     */
    void setProcessed(boolean processed);
}
