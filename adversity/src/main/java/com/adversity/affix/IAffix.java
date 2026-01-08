package com.adversity.affix;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;

/**
 * 词条接口 - 定义怪物词条的核心行为
 */
public interface IAffix {

    /**
     * 获取词条的唯一标识符
     */
    ResourceLocation getId();

    /**
     * 获取词条显示名称的翻译键
     */
    String getTranslationKey();

    /**
     * 获取词条的显示名称（已翻译）
     */
    String getDisplayName();

    /**
     * 获取词条类型
     */
    AffixType getType();

    /**
     * 获取词条的权重（用于随机选择）
     * 权重越高，越容易被选中
     */
    int getWeight();

    /**
     * 获取词条的最低难度要求
     * 只有当难度达到此值时，词条才可能出现
     */
    float getMinDifficulty();

    /**
     * 检查词条是否可以应用到指定实体
     */
    boolean canApplyTo(EntityLiving entity);

    /**
     * 检查词条是否与另一个词条兼容
     */
    boolean isCompatibleWith(IAffix other);

    // ==================== 生命周期 ====================

    /**
     * 当词条被应用到实体时调用
     */
    void onApply(EntityLiving entity, IAffixData data);

    /**
     * 当词条从实体移除时调用
     */
    void onRemove(EntityLiving entity, IAffixData data);

    /**
     * 每 tick 调用（仅服务端）
     */
    void onTick(EntityLiving entity, IAffixData data);

    // ==================== 战斗事件 ====================

    /**
     * 当拥有此词条的实体攻击其他实体时调用
     *
     * @param attacker 攻击者（拥有词条的实体）
     * @param target   被攻击者
     * @param damage   原始伤害值
     * @param data     词条数据
     * @return 修改后的伤害值
     */
    float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data);

    /**
     * 当拥有此词条的实体受到伤害时调用
     *
     * @param entity 受伤实体（拥有词条的实体）
     * @param source 伤害来源
     * @param damage 原始伤害值
     * @param data   词条数据
     * @return 修改后的伤害值
     */
    float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data);

    /**
     * 当拥有此词条的实体死亡时调用
     */
    void onDeath(EntityLiving entity, DamageSource source, IAffixData data);

    // ==================== 数据序列化 ====================

    /**
     * 将词条特有数据写入 NBT
     */
    NBTTagCompound writeToNBT(IAffixData data);

    /**
     * 从 NBT 读取词条特有数据
     */
    void readFromNBT(NBTTagCompound nbt, IAffixData data);
}
