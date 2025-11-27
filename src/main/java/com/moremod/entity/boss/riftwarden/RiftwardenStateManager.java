package com.moremod.entity.boss.riftwarden;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 状态管理器 - 管理 Boss 的主状态和子状态
 * 
 * 设计原则：
 * 1. 状态互斥 - 同一时间只有一个主状态
 * 2. 状态转换需要条件检查
 * 3. 状态有持续时间和过渡时间
 */
public class RiftwardenStateManager {
    
    private final EntityRiftwarden entity;
    
    // 当前状态
    private RiftwardenState currentState = RiftwardenState.IDLE;
    private RiftwardenState previousState = RiftwardenState.IDLE;
    private RiftwardenAttackType currentAttack = RiftwardenAttackType.NONE;
    
    // 状态计时
    private int stateTimer = 0;           // 当前状态持续时间
    private int stateDuration = 0;        // 状态预期持续时间
    private int transitionTimer = 0;      // 过渡计时器
    
    // 状态锁定
    private boolean stateLocked = false;  // 状态锁定（不可被中断）
    private int lockDuration = 0;
    
    // 回调
    private Runnable onStateComplete = null;
    
    public RiftwardenStateManager(EntityRiftwarden entity) {
        this.entity = entity;
    }
    
    /**
     * 每tick更新
     */
    public void tick() {
        stateTimer++;
        
        // 处理状态锁定（修复：即使 lockDuration 已经是 0 也要解锁）
        if (stateLocked) {
            if (lockDuration > 0) {
                lockDuration--;
            }
            if (lockDuration <= 0) {
                stateLocked = false;
            }
        }
        
        // 处理过渡
        if (transitionTimer > 0) {
            transitionTimer--;
        }
        
        // 检查状态完成
        if (stateDuration > 0 && stateTimer >= stateDuration) {
            completeCurrentState();
        }
    }
    
    /**
     * 尝试转换到新状态
     * @return 是否成功转换
     */
    public boolean tryTransitionTo(RiftwardenState newState) {
        return tryTransitionTo(newState, 0, null);
    }
    
    public boolean tryTransitionTo(RiftwardenState newState, int duration) {
        return tryTransitionTo(newState, duration, null);
    }
    
    public boolean tryTransitionTo(RiftwardenState newState, int duration, Runnable onComplete) {
        // 检查是否可以转换
        if (!canTransitionTo(newState)) {
            // 调试日志：打印切换失败原因
            System.out.println("[Riftwarden State] FAILED: " + currentState + " -> " + newState 
                + " (locked=" + stateLocked + ", lockDur=" + lockDuration + ")");
            return false;
        }
        
        // 调试日志：打印成功切换
        System.out.println("[Riftwarden State] SUCCESS: " + currentState + " -> " + newState);
        
        // 执行转换
        performTransition(newState, duration, onComplete);
        return true;
    }
    
    /**
     * 强制转换状态（忽略锁定）
     */
    public void forceTransitionTo(RiftwardenState newState, int duration) {
        stateLocked = false;
        performTransition(newState, duration, null);
    }
    
    /**
     * 检查是否可以转换到目标状态
     */
    public boolean canTransitionTo(RiftwardenState newState) {
        // 状态锁定时不能转换
        if (stateLocked) {
            return false;
        }
        
        // 相同状态不需要转换
        if (newState == currentState) {
            return false;
        }
        
        // 死亡状态不能转出
        if (currentState == RiftwardenState.DEAD) {
            return false;
        }
        
        // 检查当前状态是否可中断
        if (!currentState.isInterruptible()) {
            // 只有更高优先级的状态可以中断
            return isHigherPriority(newState, currentState);
        }
        
        return true;
    }
    
    /**
     * 执行状态转换
     */
    private void performTransition(RiftwardenState newState, int duration, Runnable onComplete) {
        System.out.println("[Riftwarden State] TRANSITION: " + currentState + " -> " + newState + " (duration=" + duration + ")");
        
        previousState = currentState;
        currentState = newState;
        stateTimer = 0;
        stateDuration = duration;
        onStateComplete = onComplete;
        
        // 清除攻击类型（除非是攻击相关状态）
        if (!newState.isCasting() && !newState.isMelee()) {
            currentAttack = RiftwardenAttackType.NONE;
        }
        
        // 通知实体状态变化
        entity.onStateChanged(previousState, newState);
        
        // 同步到客户端
        entity.syncState();
    }
    
    /**
     * 完成当前状态
     */
    private void completeCurrentState() {
        if (onStateComplete != null) {
            Runnable callback = onStateComplete;
            onStateComplete = null;
            callback.run();
        }
        
        // 默认回到IDLE（使用 force 确保能转换）
        if (currentState != RiftwardenState.IDLE && !stateLocked) {
            // 检查是否应该回到移动状态
            if (entity.getAttackTarget() != null) {
                forceTransitionTo(RiftwardenState.PURSUING, 0);
            } else {
                forceTransitionTo(RiftwardenState.IDLE, 0);
            }
        }
    }
    
    /**
     * 判断状态优先级
     */
    private boolean isHigherPriority(RiftwardenState newState, RiftwardenState currentState) {
        // 死亡最高优先级
        if (newState == RiftwardenState.DEAD) return true;
        
        // 硬直可以打断大部分状态
        if (newState == RiftwardenState.STAGGERED) {
            return currentState != RiftwardenState.DEAD;
        }
        
        // 虚弱可以打断攻击状态
        if (newState == RiftwardenState.EXHAUSTED) {
            return currentState.isCasting() || currentState.isMelee();
        }
        
        return false;
    }
    
    // ========== 便捷方法 ==========
    
    /**
     * 开始攻击
     */
    public boolean startAttack(RiftwardenAttackType attackType, int prepareDuration) {
        RiftwardenState prepareState = attackType.getId() >= 20 ? 
            RiftwardenState.CASTING_PREPARE : RiftwardenState.MELEE_WINDUP;
        
        if (tryTransitionTo(prepareState, prepareDuration)) {
            this.currentAttack = attackType;
            return true;
        }
        return false;
    }
    
    /**
     * 锁定攻击（进入执行阶段）
     */
    public void lockAttack(int executeDuration) {
        RiftwardenState executeState = currentAttack.getId() >= 20 ?
            RiftwardenState.CASTING_EXECUTE : RiftwardenState.MELEE_STRIKE;
        
        forceTransitionTo(executeState, executeDuration);
        lockState(executeDuration);
    }
    
    /**
     * 锁定当前状态
     */
    public void lockState(int duration) {
        this.stateLocked = true;
        this.lockDuration = duration;
    }
    
    /**
     * 进入恢复阶段
     */
    public void enterRecovery(int recoveryDuration) {
        RiftwardenState recoverState = currentAttack.getId() >= 20 ?
            RiftwardenState.CASTING_RECOVER : RiftwardenState.MELEE_RECOVER;
        
        forceTransitionTo(recoverState, recoveryDuration);
    }
    
    // ========== Getters ==========
    
    public RiftwardenState getCurrentState() { return currentState; }
    public RiftwardenState getPreviousState() { return previousState; }
    public RiftwardenAttackType getCurrentAttack() { return currentAttack; }
    public int getStateTimer() { return stateTimer; }
    public int getStateDuration() { return stateDuration; }
    public boolean isStateLocked() { return stateLocked; }
    
    public float getStateProgress() {
        if (stateDuration <= 0) return 0F;
        return (float) stateTimer / stateDuration;
    }
    
    public boolean isInCombatState() {
        return currentState.isCasting() || currentState.isMelee() || 
               currentState == RiftwardenState.EXHAUSTED;
    }
    
    // ========== NBT ==========
    
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("State", currentState.getId());
        nbt.setInteger("Attack", currentAttack.getId());
        nbt.setInteger("StateTimer", stateTimer);
        nbt.setInteger("StateDuration", stateDuration);
        nbt.setBoolean("Locked", stateLocked);
        nbt.setInteger("LockDuration", lockDuration);
    }
    
    public void readFromNBT(NBTTagCompound nbt) {
        currentState = RiftwardenState.fromId(nbt.getInteger("State"));
        currentAttack = RiftwardenAttackType.fromId(nbt.getInteger("Attack"));
        stateTimer = nbt.getInteger("StateTimer");
        stateDuration = nbt.getInteger("StateDuration");
        stateLocked = nbt.getBoolean("Locked");
        lockDuration = nbt.getInteger("LockDuration");
    }
}