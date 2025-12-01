package com.moremod.system.humanity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/**
 * 生物档案数据结构 - 猎人协议核心
 * Biological Profile - Hunter Protocol Core
 *
 * 通过收集样本和击杀分析目标生物，获得针对性的战斗加成
 */
public class BiologicalProfile {

    /**
     * 档案等级
     */
    public enum Tier {
        NONE(0, "未分析", "Unanalyzed"),
        BASIC(1, "初级", "Basic"),           // 1样本
        COMPLETE(2, "完整", "Complete"),     // 3样本
        MASTERED(3, "精通", "Mastered");     // 5样本 + 50击杀

        public final int level;
        public final String displayNameCN;
        public final String displayNameEN;

        Tier(int level, String nameCN, String nameEN) {
            this.level = level;
            this.displayNameCN = nameCN;
            this.displayNameEN = nameEN;
        }

        public static Tier fromLevel(int level) {
            for (Tier tier : values()) {
                if (tier.level == level) return tier;
            }
            return NONE;
        }
    }

    // NBT 键名
    private static final String NBT_ENTITY_ID = "EntityId";
    private static final String NBT_SAMPLE_COUNT = "SampleCount";
    private static final String NBT_KILL_COUNT = "KillCount";
    private static final String NBT_TIER = "Tier";
    private static final String NBT_IS_ACTIVE = "IsActive";
    private static final String NBT_ANALYSIS_TIME = "AnalysisTimeRequired";
    private static final String NBT_ENERGY_REQUIRED = "EnergyRequired";
    private static final String NBT_ANALYSIS_PROGRESS = "AnalysisProgress";

    // 样本需求
    public static final int SAMPLES_FOR_BASIC = 1;
    public static final int SAMPLES_FOR_COMPLETE = 3;
    public static final int SAMPLES_FOR_MASTERED = 5;
    public static final int KILLS_FOR_MASTERED = 50;

    // 实例字段
    private ResourceLocation entityId;
    private int sampleCount;
    private int killCount;
    private Tier currentTier;
    private boolean isActive;
    private int analysisTimeRequired;  // 所需分析时间(ticks)
    private int energyRequired;        // 所需能量(RF)
    private int analysisProgress;      // 当前分析进度(ticks)

    /**
     * 默认构造函数（用于反序列化）
     */
    public BiologicalProfile() {
        this.entityId = null;
        this.sampleCount = 0;
        this.killCount = 0;
        this.currentTier = Tier.NONE;
        this.isActive = false;
        this.analysisTimeRequired = 0;
        this.energyRequired = 0;
        this.analysisProgress = 0;
    }

    /**
     * 创建新档案
     */
    public BiologicalProfile(ResourceLocation entityId) {
        this.entityId = entityId;
        this.sampleCount = 0;
        this.killCount = 0;
        this.currentTier = Tier.NONE;
        this.isActive = false;
        this.analysisTimeRequired = calculateAnalysisTime(entityId);
        this.energyRequired = calculateEnergyRequired(entityId);
        this.analysisProgress = 0;
    }

    // ========== 效果计算 ==========

    /**
     * 获取伤害加成
     */
    public float getDamageBonus() {
        switch (currentTier) {
            case BASIC:
                return 0.15f;      // +15%
            case COMPLETE:
                return 0.30f;   // +30%
            case MASTERED:
                return 0.50f;   // +50%
            default:
                return 0f;
        }
    }

    /**
     * 获取暴击加成
     */
    public float getCritBonus() {
        return currentTier == Tier.MASTERED ? 0.20f : 0f;  // 精通+20%暴击
    }

    /**
     * 获取掉落加成
     */
    public float getDropBonus() {
        switch (currentTier) {
            case BASIC:
                return 0.20f;      // +20%
            case COMPLETE:
                return 0.50f;      // +50%
            case MASTERED:
                return 1.00f;      // +100% (双倍)
            default:
                return 0f;
        }
    }

    /**
     * 检查是否可以升级
     */
    public boolean canUpgrade() {
        switch (currentTier) {
            case NONE:
                return sampleCount >= SAMPLES_FOR_BASIC;
            case BASIC:
                return sampleCount >= SAMPLES_FOR_COMPLETE;
            case COMPLETE:
                return sampleCount >= SAMPLES_FOR_MASTERED && killCount >= KILLS_FOR_MASTERED;
            default:
                return false;
        }
    }

    /**
     * 尝试升级等级
     * @return 是否成功升级
     */
    public boolean tryUpgrade() {
        if (!canUpgrade()) return false;

        switch (currentTier) {
            case NONE:
                currentTier = Tier.BASIC;
                return true;
            case BASIC:
                currentTier = Tier.COMPLETE;
                return true;
            case COMPLETE:
                currentTier = Tier.MASTERED;
                return true;
            default:
                return false;
        }
    }

    /**
     * 添加样本
     */
    public void addSample() {
        this.sampleCount++;
        // 自动尝试升级
        while (canUpgrade()) {
            if (!tryUpgrade()) break;
        }
    }

    /**
     * 增加击杀计数
     */
    public void incrementKillCount() {
        this.killCount++;
        // 检查精通升级
        if (currentTier == Tier.COMPLETE && canUpgrade()) {
            tryUpgrade();
        }
    }

    // ========== 分析时间计算 ==========

    /**
     * 计算基础分析时间
     */
    private static int calculateAnalysisTime(ResourceLocation entityId) {
        if (entityId == null) return 20 * 60 * 5; // 默认5分钟

        String path = entityId.toString().toLowerCase();

        // Boss检测
        if (isBossEntity(path)) {
            return 20 * 60 * 120;  // 120分钟
        }

        // 精英怪检测
        if (isEliteEntity(path)) {
            return 20 * 60 * 30;   // 30分钟
        }

        // 普通怪物
        return 20 * 60 * 5;    // 5分钟
    }

    /**
     * 计算所需能量
     */
    private static int calculateEnergyRequired(ResourceLocation entityId) {
        if (entityId == null) return 5000;

        String path = entityId.toString().toLowerCase();

        if (isBossEntity(path)) {
            return 500000;
        }
        if (isEliteEntity(path)) {
            return 50000;
        }
        return 5000;
    }

    /**
     * Boss判断
     */
    public static boolean isBossEntity(String entityPath) {
        // Minecraft原版Boss
        if (entityPath.contains("ender_dragon") ||
            entityPath.contains("wither") ||
            entityPath.contains("elder_guardian")) {
            return true;
        }

        // SRP Parasites Boss
        if (entityPath.contains("srparasites") &&
            (entityPath.contains("mother") ||
             entityPath.contains("boss") ||
             entityPath.contains("apex"))) {
            return true;
        }

        // Lycanites Boss
        if (entityPath.contains("lycanites") &&
            (entityPath.contains("boss") ||
             entityPath.contains("asmodeus") ||
             entityPath.contains("rahovart") ||
             entityPath.contains("amalgalich"))) {
            return true;
        }

        return false;
    }

    /**
     * 精英怪判断
     */
    public static boolean isEliteEntity(String entityPath) {
        // 末影人、凋灵骷髅等
        if (entityPath.contains("enderman") ||
            entityPath.contains("wither_skeleton") ||
            entityPath.contains("evoker") ||
            entityPath.contains("vindicator")) {
            return true;
        }

        // SRP精英
        if (entityPath.contains("srparasites") &&
            (entityPath.contains("stalker") ||
             entityPath.contains("hunter") ||
             entityPath.contains("evolved"))) {
            return true;
        }

        // Lycanites精英
        if (entityPath.contains("lycanites") &&
            (entityPath.contains("elite") ||
             entityPath.contains("alpha"))) {
            return true;
        }

        return false;
    }

    /**
     * 获取基于人性值调整后的分析时间
     */
    public int getAdjustedAnalysisTime(float humanity) {
        // 高人性加速分析
        // 人性100%时 = 0.5倍时间
        float speedMult = 1.0f - (humanity - 50f) / 100f;
        speedMult = Math.max(0.5f, Math.min(1.0f, speedMult));
        return (int)(analysisTimeRequired * speedMult);
    }

    // ========== Getter/Setter ==========

    public ResourceLocation getEntityId() {
        return entityId;
    }

    public void setEntityId(ResourceLocation entityId) {
        this.entityId = entityId;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public int getKillCount() {
        return killCount;
    }

    public void setKillCount(int killCount) {
        this.killCount = killCount;
    }

    public Tier getCurrentTier() {
        return currentTier;
    }

    public void setCurrentTier(Tier tier) {
        this.currentTier = tier;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public int getAnalysisTimeRequired() {
        return analysisTimeRequired;
    }

    public void setAnalysisTimeRequired(int time) {
        this.analysisTimeRequired = time;
    }

    public int getEnergyRequired() {
        return energyRequired;
    }

    public void setEnergyRequired(int energy) {
        this.energyRequired = energy;
    }

    public int getAnalysisProgress() {
        return analysisProgress;
    }

    public void setAnalysisProgress(int progress) {
        this.analysisProgress = progress;
    }

    /**
     * 获取分析完成百分比
     */
    public int getAnalysisPercent() {
        if (analysisTimeRequired <= 0) return 100;
        return (int) ((float) analysisProgress / analysisTimeRequired * 100);
    }

    // ========== NBT 序列化 ==========

    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();

        if (entityId != null) {
            nbt.setString(NBT_ENTITY_ID, entityId.toString());
        }
        nbt.setInteger(NBT_SAMPLE_COUNT, sampleCount);
        nbt.setInteger(NBT_KILL_COUNT, killCount);
        nbt.setInteger(NBT_TIER, currentTier.level);
        nbt.setBoolean(NBT_IS_ACTIVE, isActive);
        nbt.setInteger(NBT_ANALYSIS_TIME, analysisTimeRequired);
        nbt.setInteger(NBT_ENERGY_REQUIRED, energyRequired);
        nbt.setInteger(NBT_ANALYSIS_PROGRESS, analysisProgress);

        return nbt;
    }

    public void deserializeNBT(NBTTagCompound nbt) {
        if (nbt.hasKey(NBT_ENTITY_ID)) {
            this.entityId = new ResourceLocation(nbt.getString(NBT_ENTITY_ID));
        }
        this.sampleCount = nbt.getInteger(NBT_SAMPLE_COUNT);
        this.killCount = nbt.getInteger(NBT_KILL_COUNT);
        this.currentTier = Tier.fromLevel(nbt.getInteger(NBT_TIER));
        this.isActive = nbt.getBoolean(NBT_IS_ACTIVE);
        this.analysisTimeRequired = nbt.getInteger(NBT_ANALYSIS_TIME);
        this.energyRequired = nbt.getInteger(NBT_ENERGY_REQUIRED);
        this.analysisProgress = nbt.getInteger(NBT_ANALYSIS_PROGRESS);
    }

    public static BiologicalProfile fromNBT(NBTTagCompound nbt) {
        BiologicalProfile profile = new BiologicalProfile();
        profile.deserializeNBT(nbt);
        return profile;
    }

    /**
     * 复制档案
     */
    public BiologicalProfile copy() {
        BiologicalProfile copy = new BiologicalProfile();
        copy.entityId = this.entityId;
        copy.sampleCount = this.sampleCount;
        copy.killCount = this.killCount;
        copy.currentTier = this.currentTier;
        copy.isActive = this.isActive;
        copy.analysisTimeRequired = this.analysisTimeRequired;
        copy.energyRequired = this.energyRequired;
        copy.analysisProgress = this.analysisProgress;
        return copy;
    }

    @Override
    public String toString() {
        return String.format("BiologicalProfile[%s | %s | 样本:%d | 击杀:%d | 激活:%s]",
                entityId != null ? entityId.toString() : "null",
                currentTier.displayNameCN,
                sampleCount,
                killCount,
                isActive ? "是" : "否");
    }
}
