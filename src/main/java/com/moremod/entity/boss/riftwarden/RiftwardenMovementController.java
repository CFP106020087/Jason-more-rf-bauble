package com.moremod.entity.boss.riftwarden;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.util.math.MathHelper;

/**
 * 物理化运动控制器
 * 
 * 注意：不要手动应用摩擦力，Minecraft 原版已经处理了
 * 我们只负责：平滑转向、攻击后滑行、状态检查
 */
public class RiftwardenMovementController extends EntityMoveHelper {

    private final EntityRiftwarden boss;

    // ========== 转向常量 ==========
    private static final float BASE_MAX_TURN_SPEED = 6.0F;      // 提高基础转速
    private static final float CASTING_MAX_TURN_SPEED = 2.0F;
    private static final float TURN_ACCELERATION = 1.2F;        // 提高加速度
    private static final float TURN_DECELERATION = 1.8F;

    // ========== 运动状态 ==========
    private float currentAngularVelocity = 0F;
    private boolean hasTargetYaw = false;
    private float targetYaw = 0F;

    // ========== 滑行状态（仅用于攻击后的惯性）==========
    private int slideTimer = 0;
    private double slideStartVelX = 0;
    private double slideStartVelZ = 0;

    public RiftwardenMovementController(EntityRiftwarden boss) {
        super(boss);
        this.boss = boss;
    }

    @Override
    public void onUpdateMoveHelper() {
        // 检查状态
        RiftwardenState state = boss.getCurrentState();
        
        // 更新转向（始终执行，除非状态禁止）
        if (state.allowsRotation()) {
            updateRotation();
        }

        // 滑行状态：不执行正常移动逻辑
        if (slideTimer > 0) {
            updateSlide();
            return;
        }

        // 状态不允许移动时，清除移动
        if (!state.allowsMovement()) {
            this.action = Action.WAIT;
            boss.setMoveForward(0F);
            return;
        }

        // 正常移动逻辑
        updateMovement();
    }

    /**
     * 更新转向 - 使用角速度限制实现平滑转向
     */
    private void updateRotation() {
        // 从 AI 的移动目标计算朝向
        if (this.action == Action.MOVE_TO) {
            double dx = this.posX - boss.posX;
            double dz = this.posZ - boss.posZ;
            if (dx * dx + dz * dz > 0.01) {
                float moveTargetYaw = (float)(MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90F;
                setTargetYaw(moveTargetYaw);
            }
        }

        if (!hasTargetYaw) {
            // 无目标时角速度自然衰减
            currentAngularVelocity *= 0.85F;
            if (Math.abs(currentAngularVelocity) < 0.1F) {
                currentAngularVelocity = 0F;
            }
            return;
        }

        float maxTurnSpeed = getMaxTurnSpeed();
        float angleDiff = MathHelper.wrapDegrees(targetYaw - boss.rotationYaw);

        // 计算目标角速度
        float desiredAngularVelocity;
        if (Math.abs(angleDiff) < 1.0F) {
            // 接近目标，直接对齐
            boss.rotationYaw = targetYaw;
            currentAngularVelocity = 0F;
            hasTargetYaw = false;
            return;
        } else if (Math.abs(angleDiff) < 20.0F) {
            // 接近时减速
            desiredAngularVelocity = angleDiff * 0.4F;
        } else {
            // 全速转向
            desiredAngularVelocity = Math.signum(angleDiff) * maxTurnSpeed;
        }

        // 角速度平滑过渡
        float velocityDiff = desiredAngularVelocity - currentAngularVelocity;
        float accel = (velocityDiff * currentAngularVelocity >= 0) ? TURN_ACCELERATION : TURN_DECELERATION;
        currentAngularVelocity += Math.signum(velocityDiff) * Math.min(Math.abs(velocityDiff), accel);

        // 限制最大角速度
        currentAngularVelocity = MathHelper.clamp(currentAngularVelocity, -maxTurnSpeed, maxTurnSpeed);

        // 应用旋转
        boss.rotationYaw += currentAngularVelocity;
        boss.rotationYawHead = boss.rotationYaw;
    }

    /**
     * 更新移动 - 委托给 Minecraft 原版系统
     */
    private void updateMovement() {
        if (this.action != Action.MOVE_TO) {
            boss.setMoveForward(0F);
            return;
        }

        double dx = this.posX - boss.posX;
        double dy = this.posY - boss.posY;
        double dz = this.posZ - boss.posZ;
        double distSq = dx * dx + dz * dz;

        // 到达目标
        if (distSq < 0.25D) {
            boss.setMoveForward(0F);
            this.action = Action.WAIT;
            return;
        }

        // 计算朝向目标的角度差
        float targetYaw = (float)(MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90F;
        float angleDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - boss.rotationYaw));

        // 根据朝向调整速度
        // 朝向正确时全速，偏离时减速（防止螃蟹走路）
        float speedMultiplier;
        if (angleDiff < 30F) {
            speedMultiplier = 1.0F;
        } else if (angleDiff < 60F) {
            speedMultiplier = 0.7F;
        } else if (angleDiff < 90F) {
            speedMultiplier = 0.4F;
        } else {
            // 背对目标时几乎不移动，优先转向
            speedMultiplier = 0.1F;
        }

        // 应用移动速度
        float moveSpeed = (float)(this.speed * speedMultiplier);
        boss.setAIMoveSpeed(moveSpeed);
        boss.setMoveForward(moveSpeed);
    }

    /**
     * 更新滑行（攻击后的惯性）
     */
    private void updateSlide() {
        slideTimer--;
        
        // 滑行期间不控制移动，让惯性自然衰减
        // Minecraft 原版摩擦力会处理速度衰减
        boss.setMoveForward(0F);
        this.action = Action.WAIT;
    }

    /**
     * 获取当前最大转速
     */
    private float getMaxTurnSpeed() {
        RiftwardenState state = boss.getCurrentState();
        float baseSpeed = BASE_MAX_TURN_SPEED + boss.getPhase() * 0.5F;

        if (state.isCasting()) {
            return CASTING_MAX_TURN_SPEED;
        }
        if (!state.allowsRotation()) {
            return 0F;
        }

        return baseSpeed;
    }

    // ========== 公共 API ==========

    /**
     * 设置目标朝向
     */
    public void setTargetYaw(float yaw) {
        this.targetYaw = MathHelper.wrapDegrees(yaw);
        this.hasTargetYaw = true;
    }

    /**
     * 朝向目标实体
     */
    public void lookAt(EntityLivingBase target) {
        if (target == null) return;

        double dx = target.posX - boss.posX;
        double dz = target.posZ - boss.posZ;
        float yaw = (float)(MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90F;
        setTargetYaw(yaw);
    }

    /**
     * 进入攻击状态 - 开始滑行
     */
    public void enterAttackState() {
        slideStartVelX = boss.motionX;
        slideStartVelZ = boss.motionZ;
        slideTimer = 10;  // 缩短滑行时间

        boss.getNavigator().clearPath();
        this.action = Action.WAIT;
    }

    /**
     * 停止移动
     */
    public void stopMovement() {
        this.action = Action.WAIT;
        boss.setMoveForward(0F);
        boss.getNavigator().clearPath();
    }

    /**
     * 检查是否朝向目标
     */
    public boolean isFacing(EntityLivingBase target, float tolerance) {
        if (target == null) return false;

        double dx = target.posX - boss.posX;
        double dz = target.posZ - boss.posZ;
        float targetYaw = (float)(MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90F;
        float diff = Math.abs(MathHelper.wrapDegrees(targetYaw - boss.rotationYaw));

        return diff <= tolerance;
    }

    /**
     * 获取当前速度
     */
    public double getCurrentSpeed() {
        return Math.sqrt(boss.motionX * boss.motionX + boss.motionZ * boss.motionZ);
    }

    /**
     * 获取角速度
     */
    public float getAngularVelocity() {
        return currentAngularVelocity;
    }

    /**
     * 是否正在滑行
     */
    public boolean isSliding() {
        return slideTimer > 0;
    }

    /**
     * 是否静止
     */
    public boolean isStationary() {
        return getCurrentSpeed() < 0.01 && !isSliding();
    }

    /**
     * 是否正在转向
     */
    public boolean isTurning() {
        return Math.abs(currentAngularVelocity) > 1.0F;
    }
}