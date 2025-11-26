package com.moremod.synergy.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Synergy 玩家状态管理器
 *
 * 存储每个玩家的 Synergy 相关状态数据：
 * - 冷却时间
 * - 排异值 (Rejection)
 * - 激活的 Synergy 状态
 * - 临时属性修改
 * - 位置历史（用于时间回溯等）
 */
public class SynergyPlayerState {

    // ==================== 静态实例管理 ====================

    private static final Map<UUID, SynergyPlayerState> PLAYER_STATES = new HashMap<>();

    public static SynergyPlayerState get(EntityPlayer player) {
        return PLAYER_STATES.computeIfAbsent(player.getUniqueID(), uuid -> new SynergyPlayerState(player));
    }

    public static void remove(EntityPlayer player) {
        PLAYER_STATES.remove(player.getUniqueID());
    }

    public static void clear() {
        PLAYER_STATES.clear();
    }

    // ==================== 实例字段 ====================

    private final UUID playerUUID;

    // 冷却系统
    private final Map<String, Long> cooldowns = new HashMap<>();

    // 排异值系统 (0-100)
    private float rejection = 0f;
    private static final float MAX_REJECTION = 100f;
    private static final float REJECTION_DECAY_PER_SECOND = 0.5f;

    // 激活状态
    private final Map<String, ActiveSynergyState> activeStates = new HashMap<>();

    // 临时属性修改器
    private final Map<String, Float> tempModifiers = new HashMap<>();

    // HP 上限修改（百分比）
    private float maxHealthModifier = 0f;

    // 位置历史（用于时间回溯）
    private final LinkedList<PositionSnapshot> positionHistory = new LinkedList<>();
    private static final int MAX_HISTORY_SIZE = 100; // 5秒 @ 20 ticks

    // 最后站立位置（用于检测站桩）
    private Vec3d lastPosition = null;
    private int standingTicks = 0;

    // 连击计数器
    private int comboCount = 0;
    private long lastHitTime = 0;
    private static final long COMBO_TIMEOUT_MS = 3000;

    // Causality Loop 触发次数
    private int causalityLoopCount = 0;
    private long lastCausalityLoopTime = 0;

    // 特殊状态标记
    private boolean inPhaseState = false;
    private boolean inBorrowedTime = false;
    private boolean inTimeDept = false;
    private boolean inGlassCannon = false;
    private boolean inMeltdown = false;

    // ==================== 构造器 ====================

    private SynergyPlayerState(EntityPlayer player) {
        this.playerUUID = player.getUniqueID();
    }

    // ==================== Tick 更新 ====================

    public void tick(EntityPlayer player) {
        long currentTime = System.currentTimeMillis();

        // 更新排异值衰减
        if (rejection > 0) {
            rejection = Math.max(0, rejection - REJECTION_DECAY_PER_SECOND / 20f);
        }

        // 记录位置历史
        recordPosition(player);

        // 检测站桩
        updateStandingState(player);

        // 更新连击超时
        if (comboCount > 0 && currentTime - lastHitTime > COMBO_TIMEOUT_MS) {
            comboCount = 0;
        }

        // 更新激活状态
        Iterator<Map.Entry<String, ActiveSynergyState>> it = activeStates.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ActiveSynergyState> entry = it.next();
            ActiveSynergyState state = entry.getValue();
            state.remainingTicks--;
            if (state.remainingTicks <= 0) {
                if (state.onExpire != null) {
                    state.onExpire.run();
                }
                it.remove();
            }
        }
    }

    private void recordPosition(EntityPlayer player) {
        PositionSnapshot snapshot = new PositionSnapshot(
                player.posX, player.posY, player.posZ,
                player.getHealth(),
                player.world.getTotalWorldTime()
        );
        positionHistory.addFirst(snapshot);
        while (positionHistory.size() > MAX_HISTORY_SIZE) {
            positionHistory.removeLast();
        }
    }

    private void updateStandingState(EntityPlayer player) {
        Vec3d currentPos = new Vec3d(player.posX, player.posY, player.posZ);
        if (lastPosition != null && lastPosition.squareDistanceTo(currentPos) < 0.01) {
            standingTicks++;
        } else {
            standingTicks = 0;
        }
        lastPosition = currentPos;
    }

    // ==================== 冷却系统 ====================

    public boolean isOnCooldown(String synergyId) {
        Long endTime = cooldowns.get(synergyId);
        return endTime != null && System.currentTimeMillis() < endTime;
    }

    public long getRemainingCooldown(String synergyId) {
        Long endTime = cooldowns.get(synergyId);
        if (endTime == null) return 0;
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    public void setCooldown(String synergyId, long durationMs) {
        cooldowns.put(synergyId, System.currentTimeMillis() + durationMs);
    }

    public void reduceCooldown(String synergyId, long reductionMs) {
        Long endTime = cooldowns.get(synergyId);
        if (endTime != null) {
            cooldowns.put(synergyId, endTime - reductionMs);
        }
    }

    // ==================== 排异值系统 ====================

    public float getRejection() {
        return rejection;
    }

    public void addRejection(float amount) {
        rejection = Math.min(MAX_REJECTION, rejection + amount);
    }

    public void setRejection(float value) {
        rejection = Math.max(0, Math.min(MAX_REJECTION, value));
    }

    public boolean isRejectionCritical() {
        return rejection >= 80f;
    }

    public float getRejectionPenalty() {
        // 返回一个 0-1 的惩罚系数
        return rejection / MAX_REJECTION;
    }

    // ==================== 激活状态系统 ====================

    public void activateState(String stateId, int durationTicks, Runnable onExpire) {
        activeStates.put(stateId, new ActiveSynergyState(stateId, durationTicks, onExpire));
    }

    public void activateState(String stateId, int durationTicks) {
        activateState(stateId, durationTicks, null);
    }

    public boolean hasActiveState(String stateId) {
        return activeStates.containsKey(stateId);
    }

    public int getStateRemainingTicks(String stateId) {
        ActiveSynergyState state = activeStates.get(stateId);
        return state != null ? state.remainingTicks : 0;
    }

    public void extendState(String stateId, int additionalTicks, int maxTicks) {
        ActiveSynergyState state = activeStates.get(stateId);
        if (state != null) {
            state.remainingTicks = Math.min(state.remainingTicks + additionalTicks, maxTicks);
        }
    }

    public void deactivateState(String stateId) {
        activeStates.remove(stateId);
    }

    // ==================== 临时修改器 ====================

    public void setTempModifier(String key, float value) {
        tempModifiers.put(key, value);
    }

    public float getTempModifier(String key, float defaultValue) {
        return tempModifiers.getOrDefault(key, defaultValue);
    }

    public void removeTempModifier(String key) {
        tempModifiers.remove(key);
    }

    // ==================== HP 上限修改 ====================

    public float getMaxHealthModifier() {
        return maxHealthModifier;
    }

    public void addMaxHealthModifier(float percent) {
        maxHealthModifier += percent;
    }

    public void resetMaxHealthModifier() {
        maxHealthModifier = 0f;
    }

    // ==================== 位置历史 ====================

    public PositionSnapshot getPositionAt(int ticksAgo) {
        if (ticksAgo < 0 || ticksAgo >= positionHistory.size()) {
            return null;
        }
        return positionHistory.get(ticksAgo);
    }

    public List<PositionSnapshot> getPositionHistory(int maxTicks) {
        int count = Math.min(maxTicks, positionHistory.size());
        return new ArrayList<>(positionHistory.subList(0, count));
    }

    // ==================== 站桩检测 ====================

    public int getStandingTicks() {
        return standingTicks;
    }

    public boolean isStandingStill(int requiredTicks) {
        return standingTicks >= requiredTicks;
    }

    // ==================== 连击系统 ====================

    public int getComboCount() {
        return comboCount;
    }

    public void incrementCombo() {
        comboCount++;
        lastHitTime = System.currentTimeMillis();
    }

    public void resetCombo() {
        comboCount = 0;
    }

    // ==================== Causality Loop ====================

    public int getCausalityLoopCount() {
        return causalityLoopCount;
    }

    public void incrementCausalityLoop() {
        causalityLoopCount++;
        lastCausalityLoopTime = System.currentTimeMillis();
    }

    public long getCausalityLoopCooldown() {
        // 每次触发增加 30 秒冷却
        return causalityLoopCount * 30000L;
    }

    public void resetCausalityLoop() {
        causalityLoopCount = 0;
    }

    // ==================== 特殊状态标记 ====================

    public boolean isInPhaseState() { return inPhaseState; }
    public void setInPhaseState(boolean value) { inPhaseState = value; }

    public boolean isInBorrowedTime() { return inBorrowedTime; }
    public void setInBorrowedTime(boolean value) { inBorrowedTime = value; }

    public boolean isInTimeDebt() { return inTimeDept; }
    public void setInTimeDebt(boolean value) { inTimeDept = value; }

    public boolean isInGlassCannon() { return inGlassCannon; }
    public void setInGlassCannon(boolean value) { inGlassCannon = value; }

    public boolean isInMeltdown() { return inMeltdown; }
    public void setInMeltdown(boolean value) { inMeltdown = value; }

    // ==================== 序列化 ====================

    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setFloat("rejection", rejection);
        nbt.setFloat("maxHealthModifier", maxHealthModifier);
        nbt.setInteger("causalityLoopCount", causalityLoopCount);
        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        rejection = nbt.getFloat("rejection");
        maxHealthModifier = nbt.getFloat("maxHealthModifier");
        causalityLoopCount = nbt.getInteger("causalityLoopCount");
    }

    // ==================== 内部类 ====================

    public static class PositionSnapshot {
        public final double x, y, z;
        public final float health;
        public final long worldTime;

        public PositionSnapshot(double x, double y, double z, float health, long worldTime) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.health = health;
            this.worldTime = worldTime;
        }

        public Vec3d toVec3d() {
            return new Vec3d(x, y, z);
        }

        public BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
    }

    public static class ActiveSynergyState {
        public final String stateId;
        public int remainingTicks;
        public final Runnable onExpire;

        public ActiveSynergyState(String stateId, int remainingTicks, Runnable onExpire) {
            this.stateId = stateId;
            this.remainingTicks = remainingTicks;
            this.onExpire = onExpire;
        }
    }
}
