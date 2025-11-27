package com.moremod.entity.boss.riftwarden;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

/**
 * 战斗 AI Task
 * 
 * 负责选择和执行攻击
 * 
 * 关键设计：
 * 1. Mutex 设置禁用移动和注视
 * 2. 攻击帧同步 - 伤害判定在动画特定帧
 * 3. 三阶段攻击流程：准备 -> 锁定 -> 执行
 */
public class AIRiftwardenCombat extends EntityAIBase {
    
    private final EntityRiftwarden boss;
    private EntityLivingBase target;
    
    // 攻击状态
    private RiftwardenAttackType pendingAttack = RiftwardenAttackType.NONE;
    private int attackPhase = 0;  // 0=准备, 1=锁定, 2=执行, 3=恢复
    private int phaseTimer = 0;
    
    // 攻击冷却
    private int globalCooldown = 0;
    private int meleeCooldown = 0;
    private int rangedCooldown = 0;
    
    public AIRiftwardenCombat(EntityRiftwarden boss) {
        this.boss = boss;
        // 禁用移动(1)和注视(2)
        this.setMutexBits(1 | 2 | 4);
    }
    
    @Override
    public boolean shouldExecute() {
        if (globalCooldown > 0) {
            globalCooldown--;
            return false;
        }
        
        target = boss.getAttackTarget();
        if (target == null || !target.isEntityAlive()) {
            return false;
        }
        
        // 检查是否可以开始攻击
        RiftwardenState state = boss.getCurrentState();
        if (!state.isInterruptible()) {
            return false;
        }
        
        // 选择攻击
        pendingAttack = selectAttack();
        return pendingAttack != RiftwardenAttackType.NONE;
    }
    
    @Override
    public boolean shouldContinueExecuting() {
        // 攻击进行中
        return boss.getStateManager().isInCombatState();
    }
    
    @Override
    public void startExecuting() {
        attackPhase = 0;
        startPreparePhase();
    }
    
    @Override
    public void updateTask() {
        if (meleeCooldown > 0) meleeCooldown--;
        if (rangedCooldown > 0) rangedCooldown--;
        
        phaseTimer--;
        
        if (phaseTimer <= 0) {
            advancePhase();
        }
        
        // 准备阶段可以追踪目标
        if (attackPhase == 0 && target != null && target.isEntityAlive()) {
            boss.getMovementController().lookAt(target);
        }
    }
    
    @Override
    public void resetTask() {
        pendingAttack = RiftwardenAttackType.NONE;
        attackPhase = 0;
        phaseTimer = 0;
        globalCooldown = 20;  // 攻击后短暂冷却
    }
    
    /**
     * 选择攻击类型
     */
    private RiftwardenAttackType selectAttack() {
        if (target == null) return RiftwardenAttackType.NONE;
        
        double distSq = boss.getDistanceSq(target);
        int phase = boss.getPhase();
        
        // 近战范围
        if (distSq < 9.0D && meleeCooldown <= 0) {
            // 检查是否面向目标
            if (boss.getMovementController().isFacing(target, 30F)) {
                meleeCooldown = 40;
                return selectMeleeAttack();
            }
        }
        
        // 远程范围
        if (distSq < 400.0D && rangedCooldown <= 0) {
            rangedCooldown = 60 - phase * 10;
            return selectRangedAttack(distSq, phase);
        }
        
        return RiftwardenAttackType.NONE;
    }
    
    private RiftwardenAttackType selectMeleeAttack() {
        float roll = boss.getRNG().nextFloat();
        
        if (boss.getPhase() >= 2 && roll < 0.2F) {
            return RiftwardenAttackType.MELEE_SLAM;
        }
        
        return boss.getRNG().nextBoolean() ? 
            RiftwardenAttackType.MELEE_RIGHT : 
            RiftwardenAttackType.MELEE_LEFT;
    }
    
    private RiftwardenAttackType selectRangedAttack(double distSq, int phase) {
        float roll = boss.getRNG().nextFloat();
        
        // 第一阶段攻击
        if (phase == 0) {
            if (roll < 0.3F) return RiftwardenAttackType.SPIRAL_BULLETS;
            if (roll < 0.5F) return RiftwardenAttackType.BURST_BULLETS;
            if (roll < 0.7F) return RiftwardenAttackType.WAVE_BULLETS;
            return RiftwardenAttackType.BULLET_BARRAGE;
        }
        
        // 高阶段攻击
        if (phase >= 2 && roll < 0.15F) {
            return RiftwardenAttackType.LASER_BEAM;
        }
        
        if (phase >= 1 && roll < 0.4F) {
            return RiftwardenAttackType.LIGHTNING_STRIKE;
        }
        
        return RiftwardenAttackType.CHARGE_SHOOT;
    }
    
    /**
     * 开始准备阶段
     */
    private void startPreparePhase() {
        attackPhase = 0;
        
        int prepareDuration = getPrepareDuration(pendingAttack);
        
        if (boss.getStateManager().startAttack(pendingAttack, prepareDuration)) {
            phaseTimer = prepareDuration;
            boss.getMovementController().enterAttackState();
        } else {
            pendingAttack = RiftwardenAttackType.NONE;
        }
    }
    
    /**
     * 推进攻击阶段
     */
    private void advancePhase() {
        attackPhase++;
        
        switch (attackPhase) {
            case 1: // 锁定阶段
                startLockPhase();
                break;
            case 2: // 执行阶段
                startExecutePhase();
                break;
            case 3: // 恢复阶段
                startRecoverPhase();
                break;
            default:
                // 完成
                break;
        }
    }
    
    private void startLockPhase() {
        int lockDuration = getLockDuration(pendingAttack);
        boss.getStateManager().lockAttack(lockDuration);
        phaseTimer = lockDuration;
    }
    
    private void startExecutePhase() {
        // 这里执行实际的攻击效果
        boss.getCombatController().executeAttack(pendingAttack, target);
        
        int executeDuration = getExecuteDuration(pendingAttack);
        phaseTimer = executeDuration;
    }
    
    private void startRecoverPhase() {
        int recoverDuration = getRecoverDuration(pendingAttack);
        boss.getStateManager().enterRecovery(recoverDuration);
        phaseTimer = recoverDuration;
    }
    
    // ========== 时间配置 ==========
    
    private int getPrepareDuration(RiftwardenAttackType attack) {
        switch (attack) {
            case MELEE_RIGHT:
            case MELEE_LEFT:
                return 15;  // 近战蓄力
            case MELEE_SLAM:
                return 25;
            case LASER_BEAM:
                return 100; // 激光预警
            case LIGHTNING_STRIKE:
                return 30;
            default:
                return 20;
        }
    }
    
    private int getLockDuration(RiftwardenAttackType attack) {
        switch (attack) {
            case LASER_BEAM:
                return 60;  // 激光充能
            case LIGHTNING_ARC:
                return 35;
            default:
                return 10;
        }
    }
    
    private int getExecuteDuration(RiftwardenAttackType attack) {
        switch (attack) {
            case MELEE_RIGHT:
            case MELEE_LEFT:
                return 12;  // 挥击帧
            case MELEE_SLAM:
                return 20;
            case LASER_BEAM:
                return 60;  // 激光持续
            default:
                return 15;
        }
    }
    
    private int getRecoverDuration(RiftwardenAttackType attack) {
        switch (attack) {
            case MELEE_SLAM:
                return 30;
            case LASER_BEAM:
                return 0;  // 激光后进入虚弱，不需要恢复
            default:
                return 15;
        }
    }
}