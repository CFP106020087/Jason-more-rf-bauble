package com.moremod.entity.boss.riftwarden;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 环顾 AI - 注视附近玩家或随机看向
 */
public class AIRiftwardenLookAround extends EntityAIBase {
    
    private final EntityRiftwarden boss;
    private EntityPlayer watchTarget;
    
    private int lookTimer = 0;
    private float randomYaw = 0;
    
    public AIRiftwardenLookAround(EntityRiftwarden boss) {
        this.boss = boss;
        this.setMutexBits(2);  // 注视任务
    }
    
    @Override
    public boolean shouldExecute() {
        // 状态检查
        if (!boss.getCurrentState().allowsRotation()) {
            return false;
        }
        
        // 有攻击目标时由其他 AI 处理
        if (boss.getAttackTarget() != null) {
            return false;
        }
        
        // 寻找附近玩家
        watchTarget = boss.world.getClosestPlayerToEntity(boss, 16.0);
        
        return true;  // 即使没有玩家也可以随机看
    }
    
    @Override
    public boolean shouldContinueExecuting() {
        if (!boss.getCurrentState().allowsRotation()) {
            return false;
        }
        
        if (boss.getAttackTarget() != null) {
            return false;
        }
        
        lookTimer--;
        return lookTimer > 0;
    }
    
    @Override
    public void startExecuting() {
        lookTimer = 40 + boss.getRNG().nextInt(40);
        
        if (watchTarget == null) {
            // 随机朝向
            randomYaw = boss.rotationYaw + (boss.getRNG().nextFloat() - 0.5F) * 90F;
        }
    }
    
    @Override
    public void updateTask() {
        if (watchTarget != null && watchTarget.isEntityAlive()) {
            // 看向玩家
            boss.getMovementController().lookAt(watchTarget);
        } else {
            // 看向随机方向
            boss.getMovementController().setTargetYaw(randomYaw);
        }
    }
    
    @Override
    public void resetTask() {
        watchTarget = null;
    }
}