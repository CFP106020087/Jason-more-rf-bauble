package com.moremod.entity.boss.riftwarden;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.Vec3d;

/**
 * 游荡 AI - 无目标时随机移动
 */
public class AIRiftwardenWander extends EntityAIBase {

    private final EntityRiftwarden boss;
    private Vec3d targetPos;

    private static final double WANDER_SPEED = 0.28D;
    private static final int WANDER_COOLDOWN = 60;  // 缩短到3秒

    private int cooldownTimer = 0;  // 从0开始，不再有启动延迟

    public AIRiftwardenWander(EntityRiftwarden boss) {
        this.boss = boss;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        // 有目标时不游荡
        if (boss.getAttackTarget() != null) {
            return false;
        }

        // 状态检查
        RiftwardenState state = boss.getCurrentState();
        if (!state.allowsMovement()) {
            return false;
        }

        // 冷却检查
        if (cooldownTimer > 0) {
            cooldownTimer--;
            return false;
        }

        // 寻找随机位置
        Vec3d vec = RandomPositionGenerator.findRandomTarget(boss, 10, 7);
        if (vec == null) {
            // 找不到位置时设置短冷却
            cooldownTimer = 20;
            return false;
        }

        targetPos = vec;
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (boss.getAttackTarget() != null) {
            return false;
        }

        if (!boss.getCurrentState().allowsMovement()) {
            return false;
        }

        // 到达目标或路径失效
        if (boss.getNavigator().noPath()) {
            return false;
        }

        double distSq = boss.getDistanceSq(targetPos.x, targetPos.y, targetPos.z);
        return distSq > 2.0;
    }

    @Override
    public void startExecuting() {
        boss.getNavigator().tryMoveToXYZ(targetPos.x, targetPos.y, targetPos.z, WANDER_SPEED);
        boss.getStateManager().tryTransitionTo(RiftwardenState.WALKING);
    }

    @Override
    public void updateTask() {
        // Navigator 会自动处理移动
    }

    @Override
    public void resetTask() {
        targetPos = null;
        cooldownTimer = WANDER_COOLDOWN;  // 完成后设置冷却

        if (boss.getCurrentState() == RiftwardenState.WALKING) {
            boss.getStateManager().tryTransitionTo(RiftwardenState.IDLE);
        }
    }
}