package com.moremod.system.humanity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 人性值数据实现
 * Humanity Data Implementation
 */
public class HumanityDataImpl implements IHumanityData {

    // NBT 键名
    public static final String NBT_HUMANITY_VALUE = "humanity_value";
    public static final String NBT_SYSTEM_ACTIVE = "humanity_system_active";
    public static final String NBT_DISSOLUTION_ACTIVE = "dissolution_active";
    public static final String NBT_DISSOLUTION_TICKS = "dissolution_ticks";
    public static final String NBT_EXISTENCE_ANCHOR_UNTIL = "existence_anchor_until";
    public static final String NBT_PROFILES = "biological_profiles";
    public static final String NBT_ACTIVE_PROFILES = "active_profiles";
    public static final String NBT_ANALYZING_ENTITY = "analyzing_entity";
    public static final String NBT_ANALYSIS_PROGRESS = "analysis_progress";
    public static final String NBT_LAST_COMBAT_TIME = "last_combat_time";
    public static final String NBT_LAST_SLEEP_TIME = "last_sleep_time";
    public static final String NBT_TICKS_SINCE_SLEEP = "ticks_since_sleep";
    public static final String NBT_ASCENSION_ROUTE = "ascension_route";
    public static final String NBT_DISSOLUTION_SURVIVALS = "dissolution_survivals";
    public static final String NBT_LOW_HUMANITY_TICKS = "low_humanity_ticks";
    public static final String NBT_HIGH_HUMANITY_TICKS = "high_humanity_ticks";
    public static final String NBT_OPERATION_VALUE = "operation_value";
    public static final String NBT_SHUTDOWN_TIMER = "shutdown_timer";
    public static final String NBT_LEARNED_INTEL = "learned_intel";

    // 默认值
    public static final float DEFAULT_HUMANITY = 75.0f;
    public static final float MIN_HUMANITY = 0.0f;
    public static final float MAX_HUMANITY = 100.0f;
    public static final int DEFAULT_DISSOLUTION_DURATION = 20 * 60; // 60秒

    // 实例字段
    private float humanity = DEFAULT_HUMANITY;
    private boolean systemActive = false;
    private boolean dissolutionActive = false;
    private int dissolutionTicks = 0;
    private long existenceAnchorUntil = 0;

    // 猎人协议数据
    private Map<ResourceLocation, BiologicalProfile> profiles = new HashMap<>();
    private Set<ResourceLocation> activeProfiles = new HashSet<>();
    private ResourceLocation analyzingEntity = null;
    private int analysisProgress = 0;

    // 状态追踪
    private long lastCombatTime = 0;
    private long lastSleepTime = 0;
    private long ticksSinceSleep = 0;

    // 升格系统
    private AscensionRoute ascensionRoute = AscensionRoute.NONE;
    private int dissolutionSurvivals = 0;
    private long lowHumanityTicks = 0; // 低人性值累计时间（破碎之神升格条件）
    private long highHumanityTicks = 0; // 高人性值累计时间（香巴拉升格条件）
    private int operationValue = 100; // 破碎之神专用
    private int shutdownTimer = 0; // 停机模式剩余时间

    // 高人性情报系统
    private Map<ResourceLocation, Integer> learnedIntel = new HashMap<>();

    // ========== 核心数值 ==========

    @Override
    public float getHumanity() {
        return humanity;
    }

    @Override
    public void setHumanity(float value) {
        this.humanity = MathHelper.clamp(value, MIN_HUMANITY, MAX_HUMANITY);
    }

    @Override
    public void modifyHumanity(float delta) {
        setHumanity(this.humanity + delta);
    }

    // ========== 系统状态 ==========

    @Override
    public boolean isSystemActive() {
        return systemActive;
    }

    @Override
    public void activateSystem() {
        this.systemActive = true;
    }

    @Override
    public void deactivateSystem() {
        // 1. 设置系统为非激活状态
        this.systemActive = false;

        // 2. 终止崩解状态（如果还在进行中）
        if (this.dissolutionActive) {
            this.dissolutionActive = false;
            this.dissolutionTicks = 0;
        }

        // 3. 取消正在进行的分析
        if (this.analyzingEntity != null) {
            BiologicalProfile profile = profiles.get(analyzingEntity);
            if (profile != null) {
                profile.setAnalysisProgress(0);
            }
            this.analyzingEntity = null;
        }

        // 4. 清除存在锚定（因为系统被停用，锚定也失去意义）
        this.existenceAnchorUntil = 0;

        // 5. 重置战斗状态
        this.lastCombatTime = 0;

        // 注意：保留人性值和生物档案数据，以便系统重新激活时可以恢复
        // 但将人性值冻结在当前值（不重置）
    }

    // ========== 崩解状态 ==========

    @Override
    public boolean isDissolutionActive() {
        return dissolutionActive;
    }

    @Override
    public int getDissolutionTicks() {
        return dissolutionTicks;
    }

    @Override
    public void setDissolutionTicks(int ticks) {
        this.dissolutionTicks = Math.max(0, ticks);
    }

    @Override
    public void startDissolution(int durationTicks) {
        this.dissolutionActive = true;
        this.dissolutionTicks = durationTicks > 0 ? durationTicks : DEFAULT_DISSOLUTION_DURATION;
    }

    @Override
    public void endDissolution(boolean survived) {
        this.dissolutionActive = false;
        this.dissolutionTicks = 0;
        // 具体的人性值重置逻辑在外部处理
    }

    // ========== 存在锚定 ==========

    @Override
    public long getExistenceAnchorUntil() {
        return existenceAnchorUntil;
    }

    @Override
    public void setExistenceAnchorUntil(long worldTime) {
        this.existenceAnchorUntil = worldTime;
    }

    @Override
    public boolean isExistenceAnchored(long currentWorldTime) {
        return currentWorldTime < existenceAnchorUntil;
    }

    // ========== 猎人协议 ==========

    @Override
    public Map<ResourceLocation, BiologicalProfile> getProfiles() {
        return Collections.unmodifiableMap(profiles);
    }

    @Override
    public Set<ResourceLocation> getActiveProfiles() {
        return Collections.unmodifiableSet(activeProfiles);
    }

    @Override
    @Nullable
    public BiologicalProfile getProfile(ResourceLocation entityId) {
        return profiles.get(entityId);
    }

    @Override
    public void setProfile(ResourceLocation entityId, BiologicalProfile profile) {
        profiles.put(entityId, profile);
    }

    @Override
    public void addSample(ResourceLocation entityId) {
        BiologicalProfile profile = profiles.get(entityId);
        if (profile == null) {
            profile = new BiologicalProfile(entityId);
            profiles.put(entityId, profile);
        }
        profile.addSample();
    }

    @Override
    public void incrementKillCount(ResourceLocation entityId) {
        BiologicalProfile profile = profiles.get(entityId);
        if (profile == null) {
            profile = new BiologicalProfile(entityId);
            profiles.put(entityId, profile);
        }
        profile.incrementKillCount();
    }

    @Override
    public int getKillCount(ResourceLocation entityId) {
        BiologicalProfile profile = profiles.get(entityId);
        return profile != null ? profile.getKillCount() : 0;
    }

    @Override
    public boolean activateProfile(ResourceLocation entityId) {
        BiologicalProfile profile = profiles.get(entityId);
        if (profile == null || profile.getCurrentTier() == BiologicalProfile.Tier.NONE) {
            return false;
        }

        // 检查槽位限制
        int maxActive = getMaxActiveProfiles();
        if (activeProfiles.size() >= maxActive && !activeProfiles.contains(entityId)) {
            return false;
        }

        profile.setActive(true);
        activeProfiles.add(entityId);
        return true;
    }

    @Override
    public void deactivateProfile(ResourceLocation entityId) {
        BiologicalProfile profile = profiles.get(entityId);
        if (profile != null) {
            profile.setActive(false);
        }
        activeProfiles.remove(entityId);
    }

    @Override
    public int getMaxActiveProfiles() {
        // 人性值/10 + 5，向下取整
        // 100% = 15槽，50% = 10槽，40% = 9槽，低于40% = 无法使用
        if (humanity < 40f) return 0;
        return (int) (humanity / 10f) + 5;
    }

    // ========== 分析系统 ==========

    @Override
    @Nullable
    public ResourceLocation getAnalyzingEntity() {
        return analyzingEntity;
    }

    @Override
    public int getAnalysisProgress() {
        if (analyzingEntity == null) return 0;
        BiologicalProfile profile = profiles.get(analyzingEntity);
        if (profile == null) return 0;
        return profile.getAnalysisPercent();
    }

    @Override
    public boolean startAnalysis(ResourceLocation entityId) {
        // 检查是否已在分析其他目标
        if (analyzingEntity != null) {
            return false;
        }

        BiologicalProfile profile = profiles.get(entityId);
        if (profile == null) {
            return false;
        }

        // 检查是否有样本但未分析
        if (profile.getSampleCount() <= 0) {
            return false;
        }

        // 已经达到最高等级
        if (profile.getCurrentTier() == BiologicalProfile.Tier.MASTERED) {
            return false;
        }

        this.analyzingEntity = entityId;
        profile.setAnalysisProgress(0);
        return true;
    }

    @Override
    public void cancelAnalysis() {
        if (analyzingEntity != null) {
            BiologicalProfile profile = profiles.get(analyzingEntity);
            if (profile != null) {
                profile.setAnalysisProgress(0);
            }
        }
        this.analyzingEntity = null;
    }

    @Override
    public void completeAnalysis() {
        if (analyzingEntity == null) return;

        BiologicalProfile profile = profiles.get(analyzingEntity);
        if (profile != null) {
            profile.tryUpgrade();
        }

        this.analyzingEntity = null;
    }

    @Override
    public int tickAnalysis(int energyAvailable) {
        if (analyzingEntity == null) return 0;

        BiologicalProfile profile = profiles.get(analyzingEntity);
        if (profile == null) {
            cancelAnalysis();
            return 0;
        }

        // 计算每tick需要的能量
        int totalEnergy = profile.getEnergyRequired();
        int adjustedTime = profile.getAdjustedAnalysisTime(humanity);
        int energyPerTick = Math.max(1, totalEnergy / adjustedTime);

        // 检查能量是否足够
        if (energyAvailable < energyPerTick) {
            return 0; // 能量不足，分析暂停
        }

        // 推进进度
        int currentProgress = profile.getAnalysisProgress();
        profile.setAnalysisProgress(currentProgress + 1);

        // 检查是否完成
        if (profile.getAnalysisProgress() >= adjustedTime) {
            completeAnalysis();
        }

        return energyPerTick;
    }

    // ========== 战斗状态追踪 ==========

    @Override
    public long getLastCombatTime() {
        return lastCombatTime;
    }

    @Override
    public void setLastCombatTime(long time) {
        this.lastCombatTime = time;
    }

    @Override
    public boolean isInCombat(long currentTime, int combatTimeout) {
        return (currentTime - lastCombatTime) < combatTimeout;
    }

    // ========== 睡眠追踪 ==========

    @Override
    public long getTicksSinceLastSleep() {
        return ticksSinceSleep;
    }

    @Override
    public void setLastSleepTime(long time) {
        this.lastSleepTime = time;
    }

    @Override
    public void resetSleepDeprivation() {
        this.ticksSinceSleep = 0;
    }

    /**
     * 每tick更新睡眠计时
     */
    public void tickSleepDeprivation() {
        this.ticksSinceSleep++;
    }

    // ========== 升格路线 ==========

    @Override
    public AscensionRoute getAscensionRoute() {
        return ascensionRoute;
    }

    @Override
    public void setAscensionRoute(AscensionRoute route) {
        this.ascensionRoute = route != null ? route : AscensionRoute.NONE;
    }

    @Override
    public int getDissolutionSurvivals() {
        return dissolutionSurvivals;
    }

    @Override
    public void incrementDissolutionSurvivals() {
        this.dissolutionSurvivals++;
    }

    // ========== 低人性累计时间 ==========

    @Override
    public long getLowHumanityTicks() {
        return lowHumanityTicks;
    }

    @Override
    public void setLowHumanityTicks(long ticks) {
        this.lowHumanityTicks = Math.max(0, ticks);
    }

    @Override
    public void addLowHumanityTicks(long ticks) {
        this.lowHumanityTicks = Math.max(0, this.lowHumanityTicks + ticks);
    }

    // ========== 高人性累计时间（香巴拉升格条件） ==========

    @Override
    public long getHighHumanityTicks() {
        return highHumanityTicks;
    }

    @Override
    public void setHighHumanityTicks(long ticks) {
        this.highHumanityTicks = Math.max(0, ticks);
    }

    @Override
    public void addHighHumanityTicks(long ticks) {
        this.highHumanityTicks = Math.max(0, this.highHumanityTicks + ticks);
    }

    // ========== 破碎之神专用 ==========

    @Override
    public int getOperationValue() {
        return operationValue;
    }

    @Override
    public void setOperationValue(int value) {
        this.operationValue = MathHelper.clamp(value, 0, 100);
    }

    @Override
    public void modifyOperationValue(int delta) {
        setOperationValue(this.operationValue + delta);
    }

    // ========== 停机模式 ==========

    @Override
    public boolean isInShutdown() {
        return shutdownTimer > 0;
    }

    @Override
    public int getShutdownTimer() {
        return shutdownTimer;
    }

    @Override
    public void setShutdownTimer(int ticks) {
        this.shutdownTimer = Math.max(0, ticks);
    }

    // ========== 高人性情报系统 ==========

    @Override
    public Map<ResourceLocation, Integer> getLearnedIntel() {
        return Collections.unmodifiableMap(learnedIntel);
    }

    @Override
    public int getIntelLevel(ResourceLocation entityId) {
        if (entityId == null) return 0;
        return learnedIntel.getOrDefault(entityId, 0);
    }

    @Override
    public void setIntelLevel(ResourceLocation entityId, int level) {
        if (entityId == null) return;
        if (level <= 0) {
            learnedIntel.remove(entityId);
        } else {
            learnedIntel.put(entityId, level);
        }
    }

    // ========== NBT序列化 ==========

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();

        // 核心数值
        nbt.setFloat(NBT_HUMANITY_VALUE, humanity);
        nbt.setBoolean(NBT_SYSTEM_ACTIVE, systemActive);

        // 崩解状态
        nbt.setBoolean(NBT_DISSOLUTION_ACTIVE, dissolutionActive);
        nbt.setInteger(NBT_DISSOLUTION_TICKS, dissolutionTicks);
        nbt.setLong(NBT_EXISTENCE_ANCHOR_UNTIL, existenceAnchorUntil);

        // 生物档案
        NBTTagList profileList = new NBTTagList();
        for (Map.Entry<ResourceLocation, BiologicalProfile> entry : profiles.entrySet()) {
            NBTTagCompound profileNbt = entry.getValue().serializeNBT();
            profileList.appendTag(profileNbt);
        }
        nbt.setTag(NBT_PROFILES, profileList);

        // 激活的档案
        NBTTagList activeList = new NBTTagList();
        for (ResourceLocation entityId : activeProfiles) {
            NBTTagCompound activeNbt = new NBTTagCompound();
            activeNbt.setString("id", entityId.toString());
            activeList.appendTag(activeNbt);
        }
        nbt.setTag(NBT_ACTIVE_PROFILES, activeList);

        // 分析状态
        if (analyzingEntity != null) {
            nbt.setString(NBT_ANALYZING_ENTITY, analyzingEntity.toString());
        }

        // 状态追踪
        nbt.setLong(NBT_LAST_COMBAT_TIME, lastCombatTime);
        nbt.setLong(NBT_LAST_SLEEP_TIME, lastSleepTime);
        nbt.setLong(NBT_TICKS_SINCE_SLEEP, ticksSinceSleep);

        // 升格系统
        nbt.setString(NBT_ASCENSION_ROUTE, ascensionRoute.getId());
        nbt.setInteger(NBT_DISSOLUTION_SURVIVALS, dissolutionSurvivals);
        nbt.setLong(NBT_LOW_HUMANITY_TICKS, lowHumanityTicks);
        nbt.setLong(NBT_HIGH_HUMANITY_TICKS, highHumanityTicks);
        nbt.setInteger(NBT_OPERATION_VALUE, operationValue);
        nbt.setInteger(NBT_SHUTDOWN_TIMER, shutdownTimer);

        // 高人性情报系统
        NBTTagList intelList = new NBTTagList();
        for (Map.Entry<ResourceLocation, Integer> entry : learnedIntel.entrySet()) {
            NBTTagCompound intelNbt = new NBTTagCompound();
            intelNbt.setString("entity_id", entry.getKey().toString());
            intelNbt.setInteger("level", entry.getValue());
            intelList.appendTag(intelNbt);
        }
        nbt.setTag(NBT_LEARNED_INTEL, intelList);

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        // 核心数值
        this.humanity = nbt.getFloat(NBT_HUMANITY_VALUE);
        if (this.humanity <= 0 && !nbt.hasKey(NBT_HUMANITY_VALUE)) {
            this.humanity = DEFAULT_HUMANITY; // 默认值
        }
        this.systemActive = nbt.getBoolean(NBT_SYSTEM_ACTIVE);

        // 崩解状态
        this.dissolutionActive = nbt.getBoolean(NBT_DISSOLUTION_ACTIVE);
        this.dissolutionTicks = nbt.getInteger(NBT_DISSOLUTION_TICKS);
        this.existenceAnchorUntil = nbt.getLong(NBT_EXISTENCE_ANCHOR_UNTIL);

        // 生物档案
        this.profiles.clear();
        NBTTagList profileList = nbt.getTagList(NBT_PROFILES, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < profileList.tagCount(); i++) {
            NBTTagCompound profileNbt = profileList.getCompoundTagAt(i);
            BiologicalProfile profile = BiologicalProfile.fromNBT(profileNbt);
            if (profile.getEntityId() != null) {
                profiles.put(profile.getEntityId(), profile);
            }
        }

        // 激活的档案
        this.activeProfiles.clear();
        NBTTagList activeList = nbt.getTagList(NBT_ACTIVE_PROFILES, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < activeList.tagCount(); i++) {
            NBTTagCompound activeNbt = activeList.getCompoundTagAt(i);
            if (activeNbt.hasKey("id")) {
                ResourceLocation entityId = new ResourceLocation(activeNbt.getString("id"));
                activeProfiles.add(entityId);
            }
        }

        // 分析状态
        if (nbt.hasKey(NBT_ANALYZING_ENTITY)) {
            this.analyzingEntity = new ResourceLocation(nbt.getString(NBT_ANALYZING_ENTITY));
        } else {
            this.analyzingEntity = null;
        }

        // 状态追踪
        this.lastCombatTime = nbt.getLong(NBT_LAST_COMBAT_TIME);
        this.lastSleepTime = nbt.getLong(NBT_LAST_SLEEP_TIME);
        this.ticksSinceSleep = nbt.getLong(NBT_TICKS_SINCE_SLEEP);

        // 升格系统
        this.ascensionRoute = AscensionRoute.fromId(nbt.getString(NBT_ASCENSION_ROUTE));
        this.dissolutionSurvivals = nbt.getInteger(NBT_DISSOLUTION_SURVIVALS);
        this.lowHumanityTicks = nbt.getLong(NBT_LOW_HUMANITY_TICKS);
        this.highHumanityTicks = nbt.getLong(NBT_HIGH_HUMANITY_TICKS);
        this.operationValue = nbt.hasKey(NBT_OPERATION_VALUE) ? nbt.getInteger(NBT_OPERATION_VALUE) : 100;
        this.shutdownTimer = nbt.getInteger(NBT_SHUTDOWN_TIMER);

        // 高人性情报系统
        this.learnedIntel.clear();
        if (nbt.hasKey(NBT_LEARNED_INTEL)) {
            NBTTagList intelList = nbt.getTagList(NBT_LEARNED_INTEL, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < intelList.tagCount(); i++) {
                NBTTagCompound intelNbt = intelList.getCompoundTagAt(i);
                if (intelNbt.hasKey("entity_id") && intelNbt.hasKey("level")) {
                    ResourceLocation entityId = new ResourceLocation(intelNbt.getString("entity_id"));
                    int level = intelNbt.getInteger("level");
                    if (level > 0) {
                        learnedIntel.put(entityId, level);
                    }
                }
            }
        }
    }

    @Override
    public void copyFrom(IHumanityData other) {
        this.humanity = other.getHumanity();
        this.systemActive = other.isSystemActive();
        this.dissolutionActive = other.isDissolutionActive();
        this.dissolutionTicks = other.getDissolutionTicks();
        this.existenceAnchorUntil = other.getExistenceAnchorUntil();
        this.lastCombatTime = other.getLastCombatTime();
        this.ticksSinceSleep = other.getTicksSinceLastSleep();

        // 复制档案
        this.profiles.clear();
        for (Map.Entry<ResourceLocation, BiologicalProfile> entry : other.getProfiles().entrySet()) {
            this.profiles.put(entry.getKey(), entry.getValue().copy());
        }

        this.activeProfiles.clear();
        this.activeProfiles.addAll(other.getActiveProfiles());

        ResourceLocation analyzing = other.getAnalyzingEntity();
        this.analyzingEntity = analyzing;

        // 升格系统
        this.ascensionRoute = other.getAscensionRoute();
        this.dissolutionSurvivals = other.getDissolutionSurvivals();
        this.lowHumanityTicks = other.getLowHumanityTicks();
        this.highHumanityTicks = other.getHighHumanityTicks();
        this.operationValue = other.getOperationValue();
        this.shutdownTimer = other.getShutdownTimer();

        // 高人性情报系统
        this.learnedIntel.clear();
        this.learnedIntel.putAll(other.getLearnedIntel());
    }
}
