package com.moremod.entity.boss.riftwarden;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 战斗 AI Task
 *
 * 重写版本 - 模仿旧版独立冷却触发机制
 *
 * 关键改动：
 * 1. 每个攻击类型有独立冷却
 * 2. 每tick检查所有可用攻击
 * 3. LIGHTNING_ARC 和 LASER_BEAM 正确加入选择
 */
public class AIRiftwardenCombat extends EntityAIBase {

    private final EntityRiftwarden boss;
    private EntityLivingBase target;

    // 攻击状态
    private RiftwardenAttackType pendingAttack = RiftwardenAttackType.NONE;
    private int attackPhase = 0;  // 0=准备, 1=锁定, 2=执行, 3=恢复
    private int phaseTimer = 0;

    // ========== 独立冷却系统（模仿旧版） ==========
    private int globalCooldown = 0;
    private int meleeCooldown = 0;
    private int bulletBarrageCooldown = 0;
    private int spiralBulletCooldown = 0;
    private int burstBulletCooldown = 0;
    private int waveBulletCooldown = 0;
    private int chargeShootCooldown = 0;
    private int chainLightningCooldown = 0;
    private int lightningStrikeCooldown = 0;
    private int lightningArcCooldown = 0;  // 闪电弧冷却
    private int laserCooldown = 0;          // 激光冷却

    public AIRiftwardenCombat(EntityRiftwarden boss) {
        this.boss = boss;
        this.setMutexBits(1 | 2 | 4);
    }

    @Override
    public boolean shouldExecute() {
        // 更新所有冷却
        tickCooldowns();

        if (globalCooldown > 0) {
            return false;
        }

        target = boss.getAttackTarget();
        if (target == null || !target.isEntityAlive()) {
            return false;
        }

        RiftwardenState state = boss.getCurrentState();
        if (!state.isInterruptible()) {
            return false;
        }

        // 选择攻击
        pendingAttack = selectAttack();
        return pendingAttack != RiftwardenAttackType.NONE;
    }

    private void tickCooldowns() {
        if (globalCooldown > 0) globalCooldown--;
        if (meleeCooldown > 0) meleeCooldown--;
        if (bulletBarrageCooldown > 0) bulletBarrageCooldown--;
        if (spiralBulletCooldown > 0) spiralBulletCooldown--;
        if (burstBulletCooldown > 0) burstBulletCooldown--;
        if (waveBulletCooldown > 0) waveBulletCooldown--;
        if (chargeShootCooldown > 0) chargeShootCooldown--;
        if (chainLightningCooldown > 0) chainLightningCooldown--;
        if (lightningStrikeCooldown > 0) lightningStrikeCooldown--;
        if (lightningArcCooldown > 0) lightningArcCooldown--;
        if (laserCooldown > 0) laserCooldown--;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return boss.getStateManager().isInCombatState();
    }

    @Override
    public void startExecuting() {
        attackPhase = 0;
        startPreparePhase();
    }

    @Override
    public void updateTask() {
        phaseTimer--;

        if (phaseTimer <= 0) {
            advancePhase();
        }

        if (attackPhase == 0 && target != null && target.isEntityAlive()) {
            boss.getMovementController().lookAt(target);
        }
    }

    @Override
    public void resetTask() {
        pendingAttack = RiftwardenAttackType.NONE;
        attackPhase = 0;
        phaseTimer = 0;
        globalCooldown = 15;
    }

    /**
     * 选择攻击类型 - 模仿旧版独立触发机制
     */
    private RiftwardenAttackType selectAttack() {
        if (target == null) return RiftwardenAttackType.NONE;

        double distSq = boss.getDistanceSq(target);
        int phase = boss.getPhase();

        // ========== 近战检查 (距离 < 3格) ==========
        if (distSq < 9.0D && meleeCooldown <= 0) {
            if (boss.getMovementController().isFacing(target, 30F)) {
                return selectMeleeAttack();
            }
        }

        // ========== 远程攻击 - 独立检查每种攻击 ==========
        // 按优先级顺序检查，高优先级攻击先检查

        // 激光 (phase >= 2, 距离 < 25格, 独立冷却)
        if (phase >= 2 && distSq < 625.0D && laserCooldown <= 0) {
            if (boss.getRNG().nextFloat() < 0.08F) {  // 8%概率尝试
                laserCooldown = 250 - phase * 20;
                return RiftwardenAttackType.LASER_BEAM;
            }
        }

        // 闪电弧 (phase >= 2, 距离 < 30格, 独立冷却)
        if (phase >= 2 && distSq < 900.0D && lightningArcCooldown <= 0) {
            if (boss.getRNG().nextFloat() < 0.15F) {  // 15%概率尝试
                lightningArcCooldown = 150 - phase * 20;
                return RiftwardenAttackType.LIGHTNING_ARC;
            }
        }

        // 闪电打击 (phase >= 1, 距离 < 25格, 独立冷却)
        if (phase >= 1 && distSq < 625.0D && lightningStrikeCooldown <= 0) {
            if (boss.getRNG().nextFloat() < 0.20F) {  // 20%概率尝试
                lightningStrikeCooldown = 100 - phase * 20;
                return RiftwardenAttackType.LIGHTNING_STRIKE;
            }
        }

        // 链式闪电 (phase == 0, 距离 < 20格)
        if (phase == 0 && distSq < 400.0D && chainLightningCooldown <= 0) {
            if (boss.getRNG().nextFloat() < 0.30F) {  // 30%概率
                chainLightningCooldown = 80;
                return RiftwardenAttackType.CHAIN_LIGHTNING;
            }
        }

        // 蓄力弹幕 (所有阶段, 距离 < 40格, 独立冷却)
        if (distSq < 1600.0D && chargeShootCooldown <= 0) {
            if (boss.getRNG().nextFloat() < 0.05F) {  // 5%概率
                chargeShootCooldown = 200 - phase * 20;
                return RiftwardenAttackType.CHARGE_SHOOT;
            }
        }

        // ========== 基础子弹攻击 (距离 < 20格) ==========
        if (distSq < 400.0D) {
            // 爆发子弹 (25%概率)
            if (burstBulletCooldown <= 0 && boss.getRNG().nextFloat() < 0.25F) {
                burstBulletCooldown = 60;
                return RiftwardenAttackType.BURST_BULLETS;
            }

            // 螺旋子弹
            if (spiralBulletCooldown <= 0) {
                spiralBulletCooldown = 30;
                return RiftwardenAttackType.SPIRAL_BULLETS;
            }

            // 波浪子弹
            if (waveBulletCooldown <= 0) {
                waveBulletCooldown = 40;
                return RiftwardenAttackType.WAVE_BULLETS;
            }

            // 基础弹幕
            if (bulletBarrageCooldown <= 0) {
                bulletBarrageCooldown = 60 - phase * 10;
                return RiftwardenAttackType.BULLET_BARRAGE;
            }
        }

        return RiftwardenAttackType.NONE;
    }

    private RiftwardenAttackType selectMeleeAttack() {
        meleeCooldown = 40;

        float roll = boss.getRNG().nextFloat();
        if (boss.getPhase() >= 2 && roll < 0.2F) {
            return RiftwardenAttackType.MELEE_SLAM;
        }

        return boss.getRNG().nextBoolean() ?
            RiftwardenAttackType.MELEE_RIGHT :
            RiftwardenAttackType.MELEE_LEFT;
    }

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

    private void advancePhase() {
        attackPhase++;

        switch (attackPhase) {
            case 1:
                startLockPhase();
                break;
            case 2:
                startExecutePhase();
                break;
            case 3:
                startRecoverPhase();
                break;
            default:
                break;
        }
    }

    private void startLockPhase() {
        int lockDuration = getLockDuration(pendingAttack);
        boss.getStateManager().lockAttack(lockDuration);
        phaseTimer = lockDuration;
    }

    private void startExecutePhase() {
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
                return 15;
            case MELEE_SLAM:
                return 25;
            case LASER_BEAM:
                return 100;  // 激光预警
            case LIGHTNING_ARC:
                return 35;   // 电弧蓄力
            case LIGHTNING_STRIKE:
                return 30;
            case CHAIN_LIGHTNING:
                return 20;
            case CHARGE_SHOOT:
                return 60;
            default:
                return 20;
        }
    }

    private int getLockDuration(RiftwardenAttackType attack) {
        switch (attack) {
            case LASER_BEAM:
                return 60;   // 激光充能
            case LIGHTNING_ARC:
                return 35;   // 电弧锁定
            default:
                return 10;
        }
    }

    private int getExecuteDuration(RiftwardenAttackType attack) {
        switch (attack) {
            case MELEE_RIGHT:
            case MELEE_LEFT:
                return 12;
            case MELEE_SLAM:
                return 20;
            case LASER_BEAM:
                return 60;   // 激光持续
            case LIGHTNING_ARC:
                return 40;   // 电弧持续
            default:
                return 15;
        }
    }

    private int getRecoverDuration(RiftwardenAttackType attack) {
        switch (attack) {
            case MELEE_SLAM:
                return 30;
            case LASER_BEAM:
                return 0;    // 激光后进入虚弱
            case LIGHTNING_ARC:
                return 20;
            default:
                return 15;
        }
    }
}
