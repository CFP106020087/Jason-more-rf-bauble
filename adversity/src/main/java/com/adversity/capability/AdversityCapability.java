package com.adversity.capability;

import com.adversity.affix.AffixData;
import com.adversity.affix.AffixRegistry;
import com.adversity.affix.IAffix;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 逆境 Capability 实现类
 */
public class AdversityCapability implements IAdversityCapability {

    private final Map<ResourceLocation, AffixData> affixes = new LinkedHashMap<>();
    private float difficultyLevel = 0f;
    private int tier = 0;
    private float healthMultiplier = 1.0f;
    private float damageMultiplier = 1.0f;
    private boolean processed = false;

    // ==================== 词条管理 ====================

    @Override
    public boolean addAffix(IAffix affix) {
        if (affix == null || affixes.containsKey(affix.getId())) {
            return false;
        }

        AffixData data = new AffixData(affix);
        affixes.put(affix.getId(), data);
        return true;
    }

    @Override
    public boolean removeAffix(IAffix affix) {
        if (affix == null) {
            return false;
        }
        return affixes.remove(affix.getId()) != null;
    }

    @Override
    public boolean hasAffix(IAffix affix) {
        return affix != null && affixes.containsKey(affix.getId());
    }

    @Override
    @Nullable
    public AffixData getAffixData(IAffix affix) {
        return affix != null ? affixes.get(affix.getId()) : null;
    }

    @Override
    public Collection<AffixData> getAllAffixData() {
        return Collections.unmodifiableCollection(affixes.values());
    }

    @Override
    public int getAffixCount() {
        return affixes.size();
    }

    @Override
    public void clearAffixes() {
        affixes.clear();
    }

    // ==================== 难度数据 ====================

    @Override
    public float getDifficultyLevel() {
        return difficultyLevel;
    }

    @Override
    public void setDifficultyLevel(float level) {
        this.difficultyLevel = Math.max(0, level);
    }

    @Override
    public int getTier() {
        return tier;
    }

    @Override
    public void setTier(int tier) {
        this.tier = Math.max(0, tier);
    }

    // ==================== 属性修正 ====================

    @Override
    public float getHealthMultiplier() {
        return healthMultiplier;
    }

    @Override
    public void setHealthMultiplier(float multiplier) {
        this.healthMultiplier = Math.max(0.1f, multiplier);
    }

    @Override
    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    @Override
    public void setDamageMultiplier(float multiplier) {
        this.damageMultiplier = Math.max(0.1f, multiplier);
    }

    // ==================== 序列化 ====================

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();

        // 保存词条
        NBTTagList affixList = new NBTTagList();
        for (AffixData data : affixes.values()) {
            affixList.appendTag(data.serializeNBT());
        }
        nbt.setTag("affixes", affixList);

        // 保存其他数据
        nbt.setFloat("difficultyLevel", difficultyLevel);
        nbt.setInteger("tier", tier);
        nbt.setFloat("healthMultiplier", healthMultiplier);
        nbt.setFloat("damageMultiplier", damageMultiplier);
        nbt.setBoolean("processed", processed);

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        affixes.clear();

        // 读取词条
        if (nbt.hasKey("affixes")) {
            NBTTagList affixList = nbt.getTagList("affixes", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < affixList.tagCount(); i++) {
                NBTTagCompound affixNbt = affixList.getCompoundTagAt(i);
                String affixId = affixNbt.getString("affixId");
                IAffix affix = AffixRegistry.getAffix(affixId);
                if (affix != null) {
                    AffixData data = new AffixData(affix);
                    data.deserializeNBT(affixNbt);
                    affixes.put(affix.getId(), data);
                }
            }
        }

        // 读取其他数据
        this.difficultyLevel = nbt.getFloat("difficultyLevel");
        this.tier = nbt.getInteger("tier");
        this.healthMultiplier = nbt.hasKey("healthMultiplier") ? nbt.getFloat("healthMultiplier") : 1.0f;
        this.damageMultiplier = nbt.hasKey("damageMultiplier") ? nbt.getFloat("damageMultiplier") : 1.0f;
        this.processed = nbt.getBoolean("processed");
    }

    // ==================== 标记 ====================

    @Override
    public boolean isProcessed() {
        return processed;
    }

    @Override
    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
}
