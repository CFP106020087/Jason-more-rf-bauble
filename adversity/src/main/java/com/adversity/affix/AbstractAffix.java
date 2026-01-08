package com.adversity.affix;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;

/**
 * 词条抽象基类 - 提供默认实现，方便创建具体词条
 */
public abstract class AbstractAffix implements IAffix {

    protected final ResourceLocation id;
    protected final AffixType type;
    protected final int weight;
    protected final float minDifficulty;

    public AbstractAffix(ResourceLocation id, AffixType type, int weight, float minDifficulty) {
        this.id = id;
        this.type = type;
        this.weight = weight;
        this.minDifficulty = minDifficulty;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public String getTranslationKey() {
        return "adversity.affix." + id.getPath();
    }

    @Override
    public AffixType getType() {
        return type;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public float getMinDifficulty() {
        return minDifficulty;
    }

    @Override
    public boolean canApplyTo(EntityLiving entity) {
        // 默认可以应用到所有 EntityLiving
        return true;
    }

    @Override
    public boolean isCompatibleWith(IAffix other) {
        // 默认与所有词条兼容
        return true;
    }

    // ==================== 默认空实现 ====================

    @Override
    public void onApply(EntityLiving entity, IAffixData data) {
        // 默认无操作
    }

    @Override
    public void onRemove(EntityLiving entity, IAffixData data) {
        // 默认无操作
    }

    @Override
    public void onTick(EntityLiving entity, IAffixData data) {
        // 默认无操作
    }

    @Override
    public float onAttack(EntityLiving attacker, EntityLivingBase target, float damage, IAffixData data) {
        return damage; // 默认不修改伤害
    }

    @Override
    public float onHurt(EntityLiving entity, DamageSource source, float damage, IAffixData data) {
        return damage; // 默认不修改伤害
    }

    @Override
    public void onDeath(EntityLiving entity, DamageSource source, IAffixData data) {
        // 默认无操作
    }

    @Override
    public NBTTagCompound writeToNBT(IAffixData data) {
        return new NBTTagCompound();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, IAffixData data) {
        // 默认无操作
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || !(obj instanceof IAffix)) return false;
        return id.equals(((IAffix) obj).getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Affix[" + id + "]";
    }
}
