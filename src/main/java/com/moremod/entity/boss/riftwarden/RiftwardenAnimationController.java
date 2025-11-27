package com.moremod.entity.boss.riftwarden;

import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;

/**
 * 动画控制器 - 处理 GeckoLib 动画与物理状态的同步
 * 
 * 核心问题解决：
 * 1. 动画速度与实际移动速度同步
 * 2. 动画状态与逻辑状态一致
 * 3. 平滑的动画过渡
 */
public class RiftwardenAnimationController {
    
    private final EntityRiftwarden entity;
    
    // 动画名称常量 - 必须与动画文件中的名称完全匹配
    public static final String ANIM_IDLE = "animation.crack.idle";
    public static final String ANIM_IDLE_BALL = "animation.crack.idle_ball";
    public static final String ANIM_WALK = "animation.crack.move";
    public static final String ANIM_RUN = "animation.crack.move";  // 没有单独的 run，用 move
    
    public static final String ANIM_MELEE_RIGHT = "animation.crack.right_attack";
    public static final String ANIM_MELEE_LEFT = "animation.crack.left_attack";
    public static final String ANIM_MELEE_SLAM = "animation.crack.right_attack";  // 暂用 right_attack
    
    public static final String ANIM_CAST_PREPARE = "animation.crack.range_right_attack";
    public static final String ANIM_CAST_HOLD = "animation.crack.range_right_attack";
    public static final String ANIM_CAST_RELEASE = "animation.crack.range_left_attack";
    
    public static final String ANIM_LASER_CHARGE = "animation.crack.range_right_attack";
    public static final String ANIM_LASER_FIRE = "animation.crack.range_left_attack";
    
    public static final String ANIM_KNEEL = "animation.crack.kneel";
    public static final String ANIM_STAGGER = "animation.crack.kneel";  // 暂用 kneel
    public static final String ANIM_TELEPORT = "animation.crack.idle_ball";  // 暂用 idle_ball
    
    // 动画同步因子
    // 公式: AnimSpeed = MoveSpeed * SYNC_FACTOR
    // 需要根据实际动画调整，使脚步与地面匹配
    private static final float WALK_SYNC_FACTOR = 2.8F;
    private static final float RUN_SYNC_FACTOR = 2.2F;
    
    // 最小/最大动画速度限制
    private static final float MIN_ANIM_SPEED = 0.3F;
    private static final float MAX_ANIM_SPEED = 2.0F;
    
    // 动画过渡时间 (ticks)
    private static final int TRANSITION_TICKS = 5;
    
    // 缓存的动画状态
    private String currentAnimation = ANIM_IDLE;
    private float currentAnimSpeed = 1.0F;
    private float targetAnimSpeed = 1.0F;
    
    public RiftwardenAnimationController(EntityRiftwarden entity) {
        this.entity = entity;
    }
    
    /**
     * GeckoLib 动画谓词 - 核心动画选择逻辑
     */
    public PlayState mainAnimationPredicate(AnimationEvent<EntityRiftwarden> event) {
        AnimationController<EntityRiftwarden> controller = event.getController();
        RiftwardenState state = entity.getCurrentState();
        RiftwardenAttackType attack = entity.getStateManager().getCurrentAttack();
        
        // 调试日志：每秒打印一次状态
        if (entity.ticksExisted % 20 == 0) {
            System.out.println("[Riftwarden Animation] Side=" + (entity.world.isRemote ? "CLIENT" : "SERVER") 
                + " State=" + state + " Anim=" + currentAnimation);
        }
        
        // 根据状态选择动画
        String targetAnim;
        float animSpeed = 1.0F;
        boolean loop = true;
        
        switch (state) {
            case IDLE:
                targetAnim = ANIM_IDLE_BALL;
                break;
                
            case WALKING:
                targetAnim = ANIM_WALK;
                animSpeed = calculateMoveAnimSpeed(event, WALK_SYNC_FACTOR);
                break;
                
            case PURSUING:
                targetAnim = ANIM_RUN;
                animSpeed = calculateMoveAnimSpeed(event, RUN_SYNC_FACTOR);
                break;
                
            case CASTING_PREPARE:
                targetAnim = ANIM_CAST_PREPARE;
                loop = false;
                break;
                
            case CASTING_LOCKED:
                targetAnim = ANIM_CAST_HOLD;
                break;
                
            case CASTING_EXECUTE:
                targetAnim = selectCastAnimation(attack);
                loop = false;
                break;
                
            case CASTING_RECOVER:
                targetAnim = ANIM_IDLE;
                break;
                
            case MELEE_WINDUP:
                targetAnim = selectMeleeWindupAnimation(attack);
                loop = false;
                // 蓄力动画速度可以根据蓄力进度调整
                animSpeed = 0.8F;
                break;
                
            case MELEE_STRIKE:
                targetAnim = selectMeleeStrikeAnimation(attack);
                loop = false;
                animSpeed = 1.2F; // 挥击略快
                break;
                
            case MELEE_RECOVER:
                targetAnim = ANIM_IDLE;
                break;
                
            case EXHAUSTED:
                targetAnim = ANIM_KNEEL;
                break;
                
            case STAGGERED:
                targetAnim = ANIM_STAGGER;
                loop = false;
                break;
                
            case TELEPORTING:
                targetAnim = ANIM_TELEPORT;
                loop = false;
                break;
                
            case GATE_ACTIVE:
                // 锁血状态使用特殊待机
                targetAnim = ANIM_IDLE_BALL;
                // 可以添加粒子效果标记
                break;
                
            default:
                targetAnim = ANIM_IDLE;
        }
        
        // 平滑动画速度变化
        animSpeed = smoothAnimSpeed(animSpeed);
        controller.setAnimationSpeed(animSpeed);
        
        // 动画切换
        if (!targetAnim.equals(currentAnimation)) {
            currentAnimation = targetAnim;
            
            AnimationBuilder builder = new AnimationBuilder();
            if (loop) {
                builder.addAnimation(targetAnim, true);
            } else {
                builder.addAnimation(targetAnim, false);
            }
            controller.setAnimation(builder);
        }
        
        return PlayState.CONTINUE;
    }
    
    /**
     * 计算移动动画速度
     * 
     * 数学原理：
     * - limbSwingAmount 反映实体的实际移动幅度
     * - 将其乘以同步因子，使动画脚步与地面移动匹配
     * - 限制在合理范围内避免过快或过慢
     */
    private float calculateMoveAnimSpeed(AnimationEvent<?> event, float syncFactor) {
        // limbSwingAmount 是 Minecraft 内部计算的移动幅度
        float limbSwingAmount = event.getLimbSwingAmount();
        
        // 也可以用实际速度计算
        double actualSpeed = entity.getMovementController().getCurrentSpeed();
        
        // 混合两种方式，取更平滑的结果
        float speedFromLimb = Math.abs(limbSwingAmount) * syncFactor;
        float speedFromActual = (float)(actualSpeed * 20.0 * syncFactor); // 转换为 blocks/second
        
        // 使用较大值，但平滑过渡
        float targetSpeed = Math.max(speedFromLimb, speedFromActual * 0.8F);
        
        // 限制范围
        return Math.max(MIN_ANIM_SPEED, Math.min(MAX_ANIM_SPEED, targetSpeed));
    }
    
    /**
     * 平滑动画速度变化
     */
    private float smoothAnimSpeed(float target) {
        targetAnimSpeed = target;
        
        // 使用指数平滑
        float smoothFactor = 0.15F;
        currentAnimSpeed += (targetAnimSpeed - currentAnimSpeed) * smoothFactor;
        
        return currentAnimSpeed;
    }
    
    /**
     * 根据攻击类型选择施法动画
     */
    private String selectCastAnimation(RiftwardenAttackType attack) {
        switch (attack) {
            case LASER_BEAM:
                return ANIM_LASER_FIRE;
            case LIGHTNING_STRIKE:
            case LIGHTNING_ARC:
            case CHAIN_LIGHTNING:
                return ANIM_CAST_RELEASE;
            default:
                return ANIM_CAST_RELEASE;
        }
    }
    
    /**
     * 根据攻击类型选择近战蓄力动画
     */
    private String selectMeleeWindupAnimation(RiftwardenAttackType attack) {
        switch (attack) {
            case MELEE_SLAM:
                return ANIM_MELEE_SLAM; // 可能需要单独的蓄力版本
            case MELEE_LEFT:
                return ANIM_MELEE_LEFT;
            case MELEE_RIGHT:
            default:
                return ANIM_MELEE_RIGHT;
        }
    }
    
    /**
     * 根据攻击类型选择近战挥击动画
     */
    private String selectMeleeStrikeAnimation(RiftwardenAttackType attack) {
        switch (attack) {
            case MELEE_SLAM:
                return ANIM_MELEE_SLAM;
            case MELEE_LEFT:
                return ANIM_MELEE_LEFT;
            case MELEE_RIGHT:
            default:
                return ANIM_MELEE_RIGHT;
        }
    }
    
    /**
     * 注册动画控制器
     */
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(
            entity, 
            "main_controller", 
            TRANSITION_TICKS,  // 过渡时间
            this::mainAnimationPredicate
        ));
        
        // 可以添加额外的控制器用于叠加动画（如球体旋转）
        data.addAnimationController(new AnimationController<>(
            entity,
            "ball_controller",
            0,
            this::ballAnimationPredicate
        ));
    }
    
    /**
     * 球体动画控制器（叠加动画）
     * 注意：动画文件中球体旋转已内嵌在主动画中，这里不需要单独处理
     */
    private PlayState ballAnimationPredicate(AnimationEvent<EntityRiftwarden> event) {
        // 球体旋转已在主动画中处理，不需要额外的叠加动画
        return PlayState.STOP;
    }
    
    // ========== 工具方法 ==========
    
    public String getCurrentAnimation() {
        return currentAnimation;
    }
    
    public float getCurrentAnimSpeed() {
        return currentAnimSpeed;
    }
}