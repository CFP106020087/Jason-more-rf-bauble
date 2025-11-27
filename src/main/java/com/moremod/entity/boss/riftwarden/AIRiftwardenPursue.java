package com.moremod.entity.boss.riftwarden;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

/**
 * 追击 AI
 */
public class AIRiftwardenPursue extends EntityAIBase {

    private final EntityRiftwarden boss;
    private EntityLivingBase target;
    private int pathRecalcTimer = 0;

    private static final double PURSUE_SPEED = 0.4D;  // 稍微提高速度
    private static final double ATTACK_RANGE_SQ = 9.0D;
    private static final double GIVE_UP_RANGE_SQ = 2500.0D;
    private static final int PATH_RECALC_INTERVAL = 10;

    public AIRiftwardenPursue(EntityRiftwarden boss) {
        this.boss = boss;
        this.setMutexBits(1 | 2);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase attackTarget = boss.getAttackTarget();
        if (attackTarget == null || !attackTarget.isEntityAlive()) {
            return false;
        }

        RiftwardenState state = boss.getCurrentState();
        if (!state.allowsMovement()) {
            return false;
        }

        double distSq = boss.getDistanceSq(attackTarget);
        
        // 太近或太远都不追
        if (distSq < ATTACK_RANGE_SQ || distSq > GIVE_UP_RANGE_SQ) {
            return false;
        }

        this.target = attackTarget;
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (target == null || !target.isEntityAlive()) {
            return false;
        }

        if (boss.getAttackTarget() != target) {
            return false;
        }

        if (!boss.getCurrentState().allowsMovement()) {
            return false;
        }

        double distSq = boss.getDistanceSq(target);
        return distSq >= ATTACK_RANGE_SQ && distSq <= GIVE_UP_RANGE_SQ;
    }

    @Override
    public void startExecuting() {
        pathRecalcTimer = 0;
        boss.getNavigator().tryMoveToEntityLiving(target, PURSUE_SPEED);
        boss.getStateManager().tryTransitionTo(RiftwardenState.PURSUING);
    }

    @Override
    public void updateTask() {
        if (target == null) return;

        // 朝向目标
        boss.getLookHelper().setLookPositionWithEntity(target, 30F, 30F);
        boss.getMovementController().lookAt(target);

        // 定期重新计算路径
        pathRecalcTimer++;
        if (pathRecalcTimer >= PATH_RECALC_INTERVAL) {
            pathRecalcTimer = 0;
            boss.getNavigator().tryMoveToEntityLiving(target, PURSUE_SPEED);
        }
    }

    @Override
    public void resetTask() {
        target = null;
        boss.getNavigator().clearPath();

        if (boss.getCurrentState() == RiftwardenState.PURSUING) {
            boss.getStateManager().tryTransitionTo(RiftwardenState.IDLE);
        }
    }
}