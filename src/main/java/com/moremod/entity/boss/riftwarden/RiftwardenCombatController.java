package com.moremod.entity.boss.riftwarden;

import com.github.alexthe666.iceandfire.api.ChainLightningUtils;
import com.moremod.entity.EntityCursedKnight;
import com.moremod.entity.fx.EntityLaserBeam;
import com.moremod.entity.fx.EntityLightningArc;
import com.moremod.entity.fx.EntityRiftLightning;
import com.moremod.entity.projectile.EntityVoidBullet;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import java.util.*;

/**
 * 战斗控制器 - 处理所有攻击逻辑和伤害计算
 * 
 * 职责：
 * 1. 执行具体攻击效果
 * 2. 管理攻击冷却
 * 3. 威胁度系统
 * 4. 锁血期间的反击
 * 5. 召唤小怪
 */
public class RiftwardenCombatController {
    
    private final EntityRiftwarden boss;
    
    // ========== 伤害常量 ==========
    private static final float MELEE_DAMAGE_BASE = 12.0F;
    private static final float BULLET_DAMAGE = 8.0F;
    private static final float LIGHTNING_DAMAGE = 15.0F;
    private static final float CHAIN_LIGHTNING_DAMAGE = 8.0F;
    private static final float LASER_DAMAGE_PER_TICK = 1.5F;
    private static final float ARC_DAMAGE_PER_TICK = 2.5F;
    
    // ========== 攻击冷却 ==========
    private Map<RiftwardenAttackType, Integer> attackCooldowns = new EnumMap<>(RiftwardenAttackType.class);
    
    // ========== 威胁度系统 ==========
    private Map<Integer, Float> threatMap = new HashMap<>();
    private int threatDecayTimer = 0;
    private static final int THREAT_DECAY_INTERVAL = 20;
    private static final float THREAT_DECAY_RATE = 0.95F;
    
    // ========== 攻击追踪 ==========
    private EntityLivingBase lastAttacker = null;
    private int lastAttackerTimeout = 0;
    private int counterAttackCooldown = 0;
    
    // ========== 激光系统 ==========
    private EntityPlayer laserTarget = null;
    private int laserPhase = 0;  // 0=无, 1=警告, 2=充能, 3=发射
    private int laserTimer = 0;
    private static final int LASER_WARNING_TIME = 100;
    private static final int LASER_CHARGE_TIME = 60;
    private static final int LASER_FIRE_TIME = 60;
    
    // ========== 闪电弧 ==========
    private List<EntityLightningArc> activeArcs = new ArrayList<>();
    
    // ========== 弹幕参数 ==========
    private float spiralAngle = 0F;
    private float waveAngle = 0F;
    
    // ========== 伤害源 ==========
    public static final DamageSource LASER_DAMAGE = new DamageSource("moremod.laser")
            .setDamageBypassesArmor().setDamageIsAbsolute();
    public static final DamageSource COUNTER_DAMAGE = new DamageSource("moremod.counter")
            .setDamageBypassesArmor();
    
    public RiftwardenCombatController(EntityRiftwarden boss) {
        this.boss = boss;
        initCooldowns();
    }
    
    private void initCooldowns() {
        for (RiftwardenAttackType type : RiftwardenAttackType.values()) {
            attackCooldowns.put(type, 0);
        }
    }
    
    /**
     * 每tick更新
     */
    public void tick() {
        // 更新冷却
        tickCooldowns();
        
        // 更新威胁度
        tickThreatSystem();
        
        // 更新攻击者追踪
        tickAttackerTracking();
        
        // 更新激光
        tickLaser();
        
        // 更新闪电弧
        tickLightningArcs();
    }
    
    private void tickCooldowns() {
        for (RiftwardenAttackType type : attackCooldowns.keySet()) {
            int cd = attackCooldowns.get(type);
            if (cd > 0) {
                attackCooldowns.put(type, cd - 1);
            }
        }
        
        if (counterAttackCooldown > 0) {
            counterAttackCooldown--;
        }
    }
    
    private void tickAttackerTracking() {
        if (lastAttackerTimeout > 0) {
            lastAttackerTimeout--;
            if (lastAttackerTimeout == 0) {
                lastAttacker = null;
            }
        }
    }
    
    // ========== 威胁度系统 ==========
    
    private void tickThreatSystem() {
        threatDecayTimer++;
        if (threatDecayTimer >= THREAT_DECAY_INTERVAL) {
            threatDecayTimer = 0;
            
            Iterator<Map.Entry<Integer, Float>> it = threatMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Float> entry = it.next();
                float newThreat = entry.getValue() * THREAT_DECAY_RATE;
                if (newThreat < 1.0F) {
                    it.remove();
                } else {
                    entry.setValue(newThreat);
                }
            }
        }
    }
    
    public void addThreat(EntityLivingBase entity, float amount) {
        if (entity == null) return;
        int id = entity.getEntityId();
        float current = threatMap.getOrDefault(id, 0F);
        threatMap.put(id, current + amount);
    }
    
    public EntityPlayer getHighestThreatPlayer() {
        EntityPlayer highest = null;
        float highestValue = 0F;
        
        for (Map.Entry<Integer, Float> entry : threatMap.entrySet()) {
            net.minecraft.entity.Entity entity = boss.world.getEntityByID(entry.getKey());
            if (entity instanceof EntityPlayer && entity.isEntityAlive()) {
                if (entry.getValue() > highestValue) {
                    highestValue = entry.getValue();
                    highest = (EntityPlayer) entity;
                }
            }
        }
        
        return highest;
    }
    
    // ========== 伤害回调 ==========
    
    public void onDamageTaken(EntityLivingBase attacker, float damage) {
        if (attacker != null) {
            lastAttacker = attacker;
            lastAttackerTimeout = 60;
            addThreat(attacker, damage);
        }
    }
    
    // ========== 锁血期间反击 ==========
    
    public void tickGateCounterAttack() {
        if (counterAttackCooldown > 0) return;
        if (lastAttacker == null || !lastAttacker.isEntityAlive()) return;
        if (boss.world.isRemote) return;
        
        int phase = boss.getPhase();
        int counterType = boss.getRNG().nextInt(3 + phase);
        
        switch (counterType) {
            case 0:
                shootCounterBullet(lastAttacker, 0);
                break;
            case 1:
                performCounterPulse();
                break;
            case 2:
                if (phase >= 1 && boss.getDistanceSq(lastAttacker) < 100) {
                    performCounterDash();
                } else {
                    shootCounterBullet(lastAttacker, 0);
                }
                break;
            default:
                // 多重反击
                for (int i = -1; i <= 1; i++) {
                    shootCounterBullet(lastAttacker, i * 0.15);
                }
                break;
        }
        
        counterAttackCooldown = 15;
    }
    
    private void shootCounterBullet(EntityLivingBase target, double angleOffset) {
        EntityVoidBullet bullet = new EntityVoidBullet(boss.world, boss);
        
        double dx = target.posX - boss.posX;
        double dy = target.posY + target.getEyeHeight() * 0.5 - boss.posY - boss.height * 0.5;
        double dz = target.posZ - boss.posZ;
        
        if (angleOffset != 0) {
            double cos = Math.cos(angleOffset);
            double sin = Math.sin(angleOffset);
            double newDx = dx * cos - dz * sin;
            double newDz = dx * sin + dz * cos;
            dx = newDx;
            dz = newDz;
        }
        
        bullet.setDamage(BULLET_DAMAGE * 0.6F);
        bullet.setPosition(boss.posX, boss.posY + boss.height * 0.6, boss.posZ);
        bullet.shoot(dx, dy, dz, 1.8F, 1.0F);
        bullet.setGlowing(true);
        
        boss.world.spawnEntity(bullet);
        boss.playGlobal(SoundEvents.ENTITY_BLAZE_SHOOT, 0.6F, 1.5F);
    }
    
    private void performCounterPulse() {
        List<EntityPlayer> nearby = boss.world.getEntitiesWithinAABB(
            EntityPlayer.class, boss.getEntityBoundingBox().grow(4.0));
        
        for (EntityPlayer player : nearby) {
            double dx = player.posX - boss.posX;
            double dz = player.posZ - boss.posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0) {
                player.addVelocity((dx / dist) * 0.8, 0.3, (dz / dist) * 0.8);
                player.attackEntityFrom(COUNTER_DAMAGE, 4.0F);
            }
        }
        
        if (!nearby.isEmpty()) {
            boss.playGlobal(SoundEvents.ENTITY_GENERIC_EXPLODE, 0.5F, 1.5F);
            spawnRing(EnumParticleTypes.EXPLOSION_NORMAL, 24, 2.0);
        }
    }
    
    private void performCounterDash() {
        if (lastAttacker == null) return;
        
        double dx = lastAttacker.posX - boss.posX;
        double dz = lastAttacker.posZ - boss.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        
        if (dist > 0 && dist < 10) {
            boss.motionX = (dx / dist) * 1.2;
            boss.motionY = 0.2;
            boss.motionZ = (dz / dist) * 1.2;
            
            boss.playGlobal(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 0.8F, 1.5F);
            spawnBurst(EnumParticleTypes.SMOKE_LARGE, 20);
        }
    }
    
    // ========== 攻击执行 ==========
    
    /**
     * 执行攻击 - 由 AI Task 在执行阶段调用
     * 
     * @param attack 攻击类型
     * @param target 目标
     */
    public void executeAttack(RiftwardenAttackType attack, EntityLivingBase target) {
        if (boss.world.isRemote) return;
        
        switch (attack) {
            // 近战
            case MELEE_RIGHT:
            case MELEE_LEFT:
                executeMeleeSwing(target, attack == RiftwardenAttackType.MELEE_LEFT);
                break;
            case MELEE_SLAM:
                executeMeleeSlam();
                break;
                
            // 子弹类
            case BULLET_BARRAGE:
                executeBulletBarrage(target);
                break;
            case SPIRAL_BULLETS:
                executeSpiralBullets();
                break;
            case BURST_BULLETS:
                executeBurstBullets(target);
                break;
            case WAVE_BULLETS:
                executeWaveBullets();
                break;
            case PREDICTIVE_SHOT:
                executePredictiveShot(target);
                break;
            case CHARGE_SHOOT:
                executeChargeShoot(target);
                break;
                
            // 闪电类
            case CHAIN_LIGHTNING:
                executeChainLightning(target);
                break;
            case LIGHTNING_STRIKE:
                executeLightningStrike(target);
                break;
            case LIGHTNING_ARC:
                executeLightningArc(target);
                break;
                
            // 激光
            case LASER_BEAM:
                startLaser(target instanceof EntityPlayer ? (EntityPlayer) target : null);
                break;
                
            // 特殊
            case COUNTER_ATTACK:
                if (lastAttacker != null) {
                    shootCounterBullet(lastAttacker, 0);
                }
                break;
            case RECOVERY_BURST:
                performRecoveryBurst();
                break;
                
            default:
                break;
        }
        
        // 设置冷却
        int cooldown = getAttackCooldown(attack);
        attackCooldowns.put(attack, cooldown);
    }
    
    private int getAttackCooldown(RiftwardenAttackType attack) {
        int phase = boss.getPhase();
        int baseCooldown;
        
        switch (attack) {
            case MELEE_RIGHT:
            case MELEE_LEFT:
                baseCooldown = 40;
                break;
            case MELEE_SLAM:
                baseCooldown = 100;
                break;
            case LASER_BEAM:
                baseCooldown = 250;
                break;
            case LIGHTNING_STRIKE:
            case LIGHTNING_ARC:
                baseCooldown = 100;
                break;
            case CHAIN_LIGHTNING:
                baseCooldown = 80;
                break;
            default:
                baseCooldown = 60;
        }
        
        // 高阶段冷却更短
        return Math.max(20, baseCooldown - phase * 10);
    }
    
    public boolean isOnCooldown(RiftwardenAttackType attack) {
        return attackCooldowns.getOrDefault(attack, 0) > 0;
    }
    
    // ========== 近战攻击 ==========
    
    private void executeMeleeSwing(EntityLivingBase target, boolean leftHand) {
        if (target == null) return;
        
        double distSq = boss.getDistanceSq(target);
        if (distSq > 12.0) return;  // 检查距离
        
        // 检查角度（前方扇形）
        if (!boss.getMovementController().isFacing(target, 60F)) return;
        
        float damage = MELEE_DAMAGE_BASE + boss.getPhase() * 2;
        
        // 造成伤害
        target.attackEntityFrom(DamageSource.causeMobDamage(boss), damage);
        
        // 击退
        double knockbackX = (target.posX - boss.posX) * 0.5;
        double knockbackZ = (target.posZ - boss.posZ) * 0.5;
        target.addVelocity(knockbackX, 0.4, knockbackZ);
        
        // 效果
        boss.playGlobal(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 1.0F, 0.8F);
        spawnHitParticles(target);
        
        // 威胁度
        if (target instanceof EntityPlayer) {
            addThreat(target, damage * 0.5F);
        }
    }
    
    private void executeMeleeSlam() {
        List<EntityPlayer> nearby = boss.world.getEntitiesWithinAABB(
            EntityPlayer.class,
            new AxisAlignedBB(
                boss.posX - 5, boss.posY - 2, boss.posZ - 5,
                boss.posX + 5, boss.posY + 2, boss.posZ + 5
            )
        );
        
        for (EntityPlayer player : nearby) {
            double dx = player.posX - boss.posX;
            double dz = player.posZ - boss.posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            
            // 距离衰减伤害
            float damage = 15.0F * (1.0F - (float)(dist / 5.0));
            if (damage > 0) {
                player.attackEntityFrom(DamageSource.causeMobDamage(boss), damage);
                
                if (dist > 0) {
                    player.addVelocity((dx / dist) * 1.5, 0.8, (dz / dist) * 1.5);
                }
            }
        }
        
        boss.playGlobal(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5F, 0.5F);
        
        // 地面波纹效果
        if (boss.world instanceof WorldServer) {
            WorldServer ws = (WorldServer) boss.world;
            for (int ring = 1; ring <= 5; ring++) {
                double radius = ring;
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2 * i) / 16;
                    ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                        boss.posX + Math.cos(angle) * radius,
                        boss.posY + 0.1,
                        boss.posZ + Math.sin(angle) * radius,
                        1, 0, 0.1, 0, 0.05);
                }
            }
        }
    }
    
    // ========== 子弹攻击 ==========
    
    private void executeBulletBarrage(EntityLivingBase target) {
        if (target == null) return;
        
        int bulletCount = 3 + boss.getPhase();
        double spread = Math.PI / 6;
        
        for (int i = 0; i < bulletCount; i++) {
            double angle = -spread / 2 + (spread * i / Math.max(1, bulletCount - 1));
            shootTargetedBullet(target, angle, BULLET_DAMAGE);
        }
        
        boss.playGlobal(SoundEvents.ENTITY_BLAZE_SHOOT, 1.0F, 0.5F);
    }
    
    private void executeSpiralBullets() {
        int bulletCount = 6;
        
        for (int i = 0; i < bulletCount; i++) {
            double angle = spiralAngle + (Math.PI * 2 * i) / bulletCount;
            shootRadialBullet(angle, BULLET_DAMAGE * 0.6F, 0.8F);
        }
        
        spiralAngle += Math.PI / 6;
        if (spiralAngle >= Math.PI * 2) {
            spiralAngle -= Math.PI * 2;
        }
        
        boss.playGlobal(SoundEvents.ENTITY_BLAZE_SHOOT, 0.8F, 1.2F);
    }
    
    private void executeBurstBullets(EntityLivingBase target) {
        if (target == null) return;
        
        int bulletCount = 5;
        double baseAngle = Math.atan2(target.posZ - boss.posZ, target.posX - boss.posX);
        double spreadAngle = Math.PI / 8;
        
        for (int i = 0; i < bulletCount; i++) {
            double angle = baseAngle - spreadAngle / 2 + (spreadAngle * i) / (bulletCount - 1);
            
            EntityVoidBullet bullet = new EntityVoidBullet(boss.world, boss);
            bullet.setDamage(BULLET_DAMAGE * 0.5F);
            
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            double dy = (target.posY + target.getEyeHeight() - boss.posY - boss.height * 0.6) /
                    boss.getDistance(target) * 0.5;
            
            bullet.setPosition(boss.posX + dx * 2, boss.posY + boss.height * 0.6, boss.posZ + dz * 2);
            bullet.shoot(dx, dy, dz, 1.8F, 1.0F);
            
            boss.world.spawnEntity(bullet);
        }
        
        boss.playGlobal(SoundEvents.ENTITY_BLAZE_SHOOT, 1.0F, 0.7F);
        spawnBurst(EnumParticleTypes.SMOKE_LARGE, 15);
    }
    
    private void executeWaveBullets() {
        int bulletCount = 12;
        
        for (int i = 0; i < bulletCount; i++) {
            double angle = waveAngle + (Math.PI * 2 * i) / bulletCount;
            double heightOffset = Math.sin(angle * 3) * 0.3;
            
            EntityVoidBullet bullet = new EntityVoidBullet(boss.world, boss);
            bullet.setDamage(BULLET_DAMAGE * 0.5F);
            
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            
            bullet.setPosition(
                boss.posX + dx * 2,
                boss.posY + boss.height * 0.5 + heightOffset,
                boss.posZ + dz * 2
            );
            bullet.shoot(dx, heightOffset * 0.5, dz, 1.0F, 0);
            
            boss.world.spawnEntity(bullet);
        }
        
        waveAngle += Math.PI / 4;
        if (waveAngle >= Math.PI * 2) {
            waveAngle -= Math.PI * 2;
        }
        
        boss.playGlobal(SoundEvents.ENTITY_BLAZE_SHOOT, 0.7F, 1.3F);
        spawnRing(EnumParticleTypes.SPELL_WITCH, 24, 2.0);
    }
    
    private void executePredictiveShot(EntityLivingBase target) {
        if (target == null) return;
        
        // 计算预测位置
        double dist = boss.getDistance(target);
        double bulletSpeed = 1.5;
        double flightTime = dist / (bulletSpeed * 20);
        
        double predictX = target.posX + target.motionX * 20 * flightTime;
        double predictY = target.posY + target.getEyeHeight() * 0.5 + target.motionY * 20 * flightTime;
        double predictZ = target.posZ + target.motionZ * 20 * flightTime;
        
        // 重力补偿
        if (!target.onGround) {
            predictY -= 0.5 * 0.08 * 20 * 20 * flightTime * flightTime;
        }
        
        EntityVoidBullet bullet = new EntityVoidBullet(boss.world, boss);
        bullet.setDamage(BULLET_DAMAGE * 0.8F);
        bullet.setGlowing(true);
        
        double dx = predictX - boss.posX;
        double dy = predictY - boss.posY - boss.height * 0.6;
        double dz = predictZ - boss.posZ;
        
        bullet.setPosition(boss.posX, boss.posY + boss.height * 0.6, boss.posZ);
        bullet.shoot(dx, dy, dz, (float) bulletSpeed, 0.5F);
        
        boss.world.spawnEntity(bullet);
        boss.playGlobal(SoundEvents.ENTITY_BLAZE_SHOOT, 0.9F, 1.0F);
        
        // 预测位置标记
        if (boss.world instanceof WorldServer) {
            ((WorldServer) boss.world).spawnParticle(EnumParticleTypes.SPELL_WITCH,
                predictX, predictY, predictZ, 5, 0.2, 0.2, 0.2, 0.0);
        }
    }
    
    private void executeChargeShoot(EntityLivingBase target) {
        // 环形弹幕
        int bulletCount = 16 + boss.getPhase() * 4;
        for (int i = 0; i < bulletCount; i++) {
            double angle = (Math.PI * 2 * i) / bulletCount;
            shootRadialBullet(angle, BULLET_DAMAGE * 0.75F, 1.2F);
        }
        
        // 追踪弹
        if (target != null) {
            List<EntityPlayer> targets = boss.world.getEntitiesWithinAABB(
                EntityPlayer.class,
                new AxisAlignedBB(boss.posX - 40, boss.posY - 40, boss.posZ - 40,
                                 boss.posX + 40, boss.posY + 40, boss.posZ + 40)
            );
            
            for (EntityPlayer p : targets) {
                for (int i = 0; i < 2 + boss.getPhase(); i++) {
                    shootTargetedBullet(p, (i - 1) * 0.1, BULLET_DAMAGE);
                }
            }
        }
        
        boss.playGlobal(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 0.7F);
        spawnRing(EnumParticleTypes.EXPLOSION_LARGE, 8, 3.0);
    }
    
    // ========== 子弹工具方法 ==========
    
    private void shootTargetedBullet(EntityLivingBase target, double angleOffset, float damage) {
        EntityVoidBullet bullet = new EntityVoidBullet(boss.world, boss);
        
        double dx = target.posX - boss.posX;
        double dy = target.posY + target.getEyeHeight() - boss.posY - boss.height * 0.5;
        double dz = target.posZ - boss.posZ;
        
        if (angleOffset != 0) {
            double cos = Math.cos(angleOffset);
            double sin = Math.sin(angleOffset);
            double newDx = dx * cos - dz * sin;
            double newDz = dx * sin + dz * cos;
            dx = newDx;
            dz = newDz;
        }
        
        bullet.setDamage(damage);
        
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double spawnDist = 2.0;
        bullet.setPosition(
            boss.posX + (dx / length) * spawnDist,
            boss.posY + boss.height * 0.7,
            boss.posZ + (dz / length) * spawnDist
        );
        bullet.shoot(dx, dy, dz, 1.5F, 2.0F);
        
        boss.world.spawnEntity(bullet);
    }
    
    private void shootRadialBullet(double angle, float damage, float speed) {
        EntityVoidBullet bullet = new EntityVoidBullet(boss.world, boss);
        
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        
        bullet.setDamage(damage);
        bullet.setPosition(boss.posX + dx * 2, boss.posY + boss.height * 0.5, boss.posZ + dz * 2);
        bullet.shoot(dx, 0.1, dz, speed, 0);
        
        boss.world.spawnEntity(bullet);
    }
    
    // ========== 闪电攻击 ==========
    
    private void executeChainLightning(EntityLivingBase target) {
        if (target == null) return;
        
        try {
            float[] damagePerHop = new float[] {
                CHAIN_LIGHTNING_DAMAGE,
                CHAIN_LIGHTNING_DAMAGE * 0.75F,
                CHAIN_LIGHTNING_DAMAGE * 0.5F,
                CHAIN_LIGHTNING_DAMAGE * 0.25F
            };
            
            ChainLightningUtils.createChainLightningFromTarget(
                boss.world, target, boss, damagePerHop, 8, false
            );
            
            boss.playGlobal(SoundEvents.ENTITY_LIGHTNING_THUNDER, 0.6F, 1.8F);
            
            Vec3d handPos = getHandPosition(boss.getRNG().nextBoolean());
            if (boss.world instanceof WorldServer) {
                ((WorldServer) boss.world).spawnParticle(EnumParticleTypes.SPELL_INSTANT,
                    handPos.x, handPos.y, handPos.z, 15, 0.2, 0.2, 0.2, 0.1);
            }
        } catch (Exception e) {
            // Ice and Fire 不可用，使用备用方案
            fallbackChainLightning(target);
        }
    }
    
    private void fallbackChainLightning(EntityLivingBase target) {
        target.attackEntityFrom(DamageSource.causeMobDamage(boss).setDamageBypassesArmor(),
            CHAIN_LIGHTNING_DAMAGE);
        
        List<EntityPlayer> nearby = boss.world.getEntitiesWithinAABB(
            EntityPlayer.class,
            target.getEntityBoundingBox().grow(8.0),
            p -> p != target && p.isEntityAlive()
        );
        
        int chainCount = 0;
        EntityLivingBase lastTarget = target;
        
        for (EntityPlayer chainTarget : nearby) {
            if (chainCount >= 3) break;
            
            chainTarget.attackEntityFrom(
                DamageSource.causeMobDamage(boss).setDamageBypassesArmor(),
                CHAIN_LIGHTNING_DAMAGE * (1.0F - chainCount * 0.2F)
            );
            
            spawnLightningLine(lastTarget, chainTarget);
            lastTarget = chainTarget;
            chainCount++;
        }
        
        boss.playGlobal(SoundEvents.ENTITY_LIGHTNING_THUNDER, 0.6F, 1.8F);
    }
    
    private void executeLightningStrike(EntityLivingBase target) {
        if (target == null) return;
        
        int projectileCount = 3 + boss.getPhase();
        Vec3d handPos = getHandPosition(true);
        
        for (int i = 0; i < projectileCount; i++) {
            EntityRiftLightning bolt = new EntityRiftLightning(boss.world, boss, target);
            
            if (i > 0) {
                double angleOffset = (i - projectileCount / 2.0) * 0.2;
                bolt.motionX += Math.cos(angleOffset) * 0.3;
                bolt.motionZ += Math.sin(angleOffset) * 0.3;
            }
            
            bolt.setPosition(handPos.x, handPos.y + i * 0.2, handPos.z);
            bolt.setDamage(LIGHTNING_DAMAGE + boss.getPhase() * 3);
            
            boss.world.spawnEntity(bolt);
        }
        
        boss.playGlobal(SoundEvents.ENTITY_LIGHTNING_THUNDER, 0.7F, 1.5F);
        spawnLightningChargeEffect(handPos);
    }
    
    private void executeLightningArc(EntityLivingBase target) {
        if (!(target instanceof EntityPlayer)) return;
        EntityPlayer playerTarget = (EntityPlayer) target;
        
        Vec3d handPos = getHandPosition(false);
        
        // 主弧
        EntityLightningArc mainArc = new EntityLightningArc(boss.world, boss, playerTarget, 40);
        boss.world.spawnEntity(mainArc);
        activeArcs.add(mainArc);
        
        // 高阶段连锁
        if (boss.getPhase() >= 3) {
            List<EntityPlayer> nearby = boss.world.getEntitiesWithinAABB(
                EntityPlayer.class,
                new AxisAlignedBB(target.posX - 8, target.posY - 8, target.posZ - 8,
                                 target.posX + 8, target.posY + 8, target.posZ + 8)
            );
            
            for (EntityPlayer nearbyPlayer : nearby) {
                if (nearbyPlayer != target && nearbyPlayer.isEntityAlive() && activeArcs.size() < 3 + boss.getPhase()) {
                    EntityLightningArc chainArc = new EntityLightningArc(
                        boss.world, target, nearbyPlayer, 30);
                    boss.world.spawnEntity(chainArc);
                    activeArcs.add(chainArc);
                }
            }
        }
        
        boss.playGlobal(SoundEvents.ENTITY_WITHER_SPAWN, 1.5F, 2.0F);
        spawnArcEffect(playerTarget, handPos);
    }
    
    private void tickLightningArcs() {
        Iterator<EntityLightningArc> it = activeArcs.iterator();
        while (it.hasNext()) {
            EntityLightningArc arc = it.next();
            if (arc == null || !arc.isEntityAlive() || arc.getTicksLeft() <= 0) {
                if (arc != null && arc.isEntityAlive()) {
                    arc.setDead();
                }
                it.remove();
            } else {
                net.minecraft.entity.Entity target = arc.getTo();
                if (target instanceof EntityLivingBase && target.isEntityAlive()) {
                    if (arc.ticksExisted % 5 == 0) {
                        target.attackEntityFrom(
                            DamageSource.causeMobDamage(boss).setDamageBypassesArmor(),
                            ARC_DAMAGE_PER_TICK
                        );
                        
                        if (boss.world instanceof WorldServer) {
                            ((WorldServer) boss.world).spawnParticle(EnumParticleTypes.SPELL_INSTANT,
                                target.posX, target.posY + target.height * 0.5, target.posZ,
                                10, 0.3, 0.3, 0.3, 0.1);
                        }
                    }
                }
            }
        }
    }
    
    // ========== 激光系统 ==========
    
    private void startLaser(EntityPlayer target) {
        if (target == null) return;
        
        laserTarget = target;
        laserPhase = 1;  // 警告阶段
        laserTimer = LASER_WARNING_TIME;
        
        if (target instanceof EntityPlayerMP) {
            ((EntityPlayerMP) target).sendStatusMessage(
                new net.minecraft.util.text.TextComponentString("§4§l⚠ 警告 ⚠"), false);
            ((EntityPlayerMP) target).sendStatusMessage(
                new net.minecraft.util.text.TextComponentString("§c致命镭射蓄力中"), true);
        }
        
        boss.playGlobal(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 1.0F, 0.5F);
    }
    
    private void tickLaser() {
        if (laserPhase == 0 || laserTarget == null) return;
        
        laserTimer--;
        
        switch (laserPhase) {
            case 1: // 警告
                tickLaserWarning();
                break;
            case 2: // 充能
                tickLaserCharge();
                break;
            case 3: // 发射
                tickLaserFire();
                break;
        }
    }
    
    private void tickLaserWarning() {
        if (laserTarget == null || !laserTarget.isEntityAlive()) {
            cancelLaser();
            return;
        }
        
        // 追踪目标
        boss.getMovementController().lookAt(laserTarget);
        
        // 警告音效
        if (laserTimer % 10 == 0) {
            boss.playGlobal(SoundEvents.BLOCK_NOTE_PLING, 2.0F,
                0.5F + (LASER_WARNING_TIME - laserTimer) * 0.03F);
        }
        
        // 倒计时提示
        if (laserTimer % 20 == 0 && laserTarget instanceof EntityPlayerMP) {
            int secondsLeft = laserTimer / 20;
            laserTarget.sendMessage(new net.minecraft.util.text.TextComponentString(
                "§c§l激光将在 " + secondsLeft + " 秒后发射！"));
        }
        
        // 进入充能阶段
        if (laserTimer <= 0) {
            laserPhase = 2;
            laserTimer = LASER_CHARGE_TIME;
            
            if (laserTarget instanceof EntityPlayerMP) {
                laserTarget.sendMessage(new net.minecraft.util.text.TextComponentString(
                    "§4§l激光充能中！！！"));
            }
            
            boss.playGlobal(SoundEvents.BLOCK_PORTAL_AMBIENT, 1.5F, 2.0F);
        }
    }
    
    private void tickLaserCharge() {
        if (laserTarget == null || !laserTarget.isEntityAlive()) {
            cancelLaser();
            return;
        }
        
        // 缓慢追踪
        boss.getMovementController().lookAt(laserTarget);
        
        // 充能音效
        if (laserTimer % 10 == 0) {
            float pitch = 2.0F + (LASER_CHARGE_TIME - laserTimer) * 0.02F;
            boss.playGlobal(SoundEvents.ENTITY_WITHER_AMBIENT, 0.5F, pitch);
        }
        
        // 开始发射
        if (laserTimer <= 0) {
            if (boss.canEntityBeSeen(laserTarget)) {
                fireLaser();
            } else {
                // 目标躲开了
                if (laserTarget instanceof EntityPlayerMP) {
                    laserTarget.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "§a成功躲避了激光攻击！"));
                }
                boss.playGlobal(SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.0F, 0.5F);
                cancelLaser();
            }
        }
    }
    
    private void fireLaser() {
        laserPhase = 3;
        laserTimer = LASER_FIRE_TIME;
        
        Vec3d handPos = getHandPosition(true);
        
        // 生成激光束实体
        EntityLaserBeam beam = new EntityLaserBeam(
            boss.world, boss, laserTarget, LASER_FIRE_TIME, 0.8F, 0
        );
        beam.setStartPosition(handPos);
        boss.world.spawnEntity(beam);
        
        boss.playGlobal(SoundEvents.ENTITY_LIGHTNING_THUNDER, 1.0F, 2.0F);
        boss.playGlobal(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 0.5F, 0.5F);
        
        // 通知其他玩家
        List<EntityPlayer> nearby = boss.world.getEntitiesWithinAABB(
            EntityPlayer.class, boss.getEntityBoundingBox().grow(30));
        for (EntityPlayer player : nearby) {
            if (player != laserTarget && player instanceof EntityPlayerMP) {
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    "§6§l虚空守望者正在释放激光，现在是攻击的好机会！"));
            }
        }
    }
    
    private void tickLaserFire() {
        if (laserTarget == null || !laserTarget.isEntityAlive()) {
            endLaser();
            return;
        }
        
        // 缓慢追踪
        boss.getMovementController().lookAt(laserTarget);
        
        // 造成伤害
        if (boss.canEntityBeSeen(laserTarget) && boss.getDistanceSq(laserTarget) < 625) {
            int originalHurtTime = laserTarget.hurtResistantTime;
            laserTarget.hurtResistantTime = 0;
            laserTarget.attackEntityFrom(LASER_DAMAGE, LASER_DAMAGE_PER_TICK);
            laserTarget.hurtResistantTime = Math.min(originalHurtTime, 5);
            
            // 伤害粒子
            if (boss.world instanceof WorldServer && boss.ticksExisted % 2 == 0) {
                ((WorldServer) boss.world).spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                    laserTarget.posX, laserTarget.posY + laserTarget.height * 0.5, laserTarget.posZ,
                    10, 0.3, 0.3, 0.3, 0.1);
            }
        }
        
        // 结束
        if (laserTimer <= 0) {
            endLaser();
        }
    }
    
    private void endLaser() {
        laserPhase = 0;
        laserTarget = null;
        
        // 进入虚弱状态
        boss.getStateManager().forceTransitionTo(RiftwardenState.EXHAUSTED, 400);
    }
    
    private void cancelLaser() {
        laserPhase = 0;
        laserTarget = null;
    }
    
    public boolean isFiringLaser() {
        return laserPhase == 3;
    }
    
    public EntityPlayer getLaserTarget() {
        return laserTarget;
    }
    
    // ========== 恢复爆发 ==========
    
    public void performRecoveryBurst() {
        // 环形弹幕
        int bulletCount = 24 + boss.getPhase() * 4;
        for (int i = 0; i < bulletCount; i++) {
            double angle = (Math.PI * 2 * i) / bulletCount;
            shootRadialBullet(angle, BULLET_DAMAGE * 0.75F, 1.2F);
        }
        
        // 高阶段闪电
        if (boss.getPhase() >= 2) {
            List<EntityPlayer> targets = boss.world.getEntitiesWithinAABB(
                EntityPlayer.class, boss.getEntityBoundingBox().grow(20));
            
            for (EntityPlayer target : targets) {
                if (target.isEntityAlive()) {
                    EntityRiftLightning bolt = new EntityRiftLightning(boss.world, boss, target);
                    bolt.setDamage(LIGHTNING_DAMAGE * 0.5F);
                    boss.world.spawnEntity(bolt);
                }
            }
        }
        
        boss.playGlobal(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 0.7F);
        spawnRing(EnumParticleTypes.EXPLOSION_LARGE, 16, 4.0);
    }
    
    // ========== 召唤 ==========
    
    public void summonMinions(int count, int minRadius, int maxRadius) {
        if (boss.world.isRemote) return;
        
        int spawned = 0;
        for (int i = 0; i < count && spawned < count; i++) {
            if (spawnOneMinion(minRadius, maxRadius)) {
                spawned++;
            }
        }
        
        if (spawned > 0) {
            boss.playGlobal(SoundEvents.ENTITY_WITHER_SPAWN, 0.7F, 1.2F);
        }
    }
    
    private boolean spawnOneMinion(int minRadius, int maxRadius) {
        for (int tries = 0; tries < 10; tries++) {
            double angle = boss.getRNG().nextDouble() * Math.PI * 2;
            int radius = minRadius + boss.getRNG().nextInt(Math.max(1, maxRadius - minRadius + 1));
            int dx = (int) Math.round(Math.cos(angle) * radius);
            int dz = (int) Math.round(Math.sin(angle) * radius);
            int dy = 1 + boss.getRNG().nextInt(2);
            
            BlockPos pos = new BlockPos(boss.posX + dx, boss.posY + dy, boss.posZ + dz);
            
            if (!boss.world.isAirBlock(pos) || !boss.world.isAirBlock(pos.up())) {
                continue;
            }
            
            EntityCursedKnight knight = new EntityCursedKnight(boss.world);
            knight.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            
            EntityPlayer target = boss.world.getNearestAttackablePlayer(boss, 32, 32);
            if (target != null) {
                knight.setAttackTarget(target);
            }
            
            // 生成效果
            if (boss.world instanceof WorldServer) {
                WorldServer ws = (WorldServer) boss.world;
                for (int i = 0; i < 30; i++) {
                    double ox = (boss.getRNG().nextDouble() - 0.5) * 0.8;
                    double oy = boss.getRNG().nextDouble();
                    double oz = (boss.getRNG().nextDouble() - 0.5) * 0.8;
                    ws.spawnParticle(EnumParticleTypes.SPELL_MOB,
                        pos.getX() + 0.5 + ox, pos.getY() + 0.2 + oy, pos.getZ() + 0.5 + oz,
                        1, 0, 0, 0, 0.0);
                }
            }
            
            boss.world.spawnEntity(knight);
            return true;
        }
        
        return false;
    }
    
    // ========== 清理 ==========
    
    public void cleanup() {
        for (EntityLightningArc arc : activeArcs) {
            if (arc != null && arc.isEntityAlive()) {
                arc.setDead();
            }
        }
        activeArcs.clear();
        
        cancelLaser();
    }
    
    // ========== 工具方法 ==========
    
    private Vec3d getHandPosition(boolean rightHand) {
        double shoulderHeight = boss.height * 0.8;
        double armLength = 1.5;
        double sideOffset = rightHand ? 0.5 : -0.5;
        
        double yawRad = Math.toRadians(boss.rotationYaw);
        double handX = boss.posX - Math.sin(yawRad + Math.PI / 2) * sideOffset;
        double handY = boss.posY + shoulderHeight;
        double handZ = boss.posZ + Math.cos(yawRad + Math.PI / 2) * sideOffset;
        
        return new Vec3d(handX, handY, handZ);
    }
    
    private void spawnRing(EnumParticleTypes type, int count, double radius) {
        if (!(boss.world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) boss.world;
        
        double cx = boss.posX;
        double cy = boss.posY + boss.height * 0.6;
        double cz = boss.posZ;
        
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            ws.spawnParticle(type,
                cx + Math.cos(angle) * radius, cy, cz + Math.sin(angle) * radius,
                1, 0, 0, 0, 0.0);
        }
    }
    
    private void spawnBurst(EnumParticleTypes type, int count) {
        if (!(boss.world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) boss.world;
        
        ws.spawnParticle(type,
            boss.posX, boss.posY + boss.height * 0.5, boss.posZ,
            count, 0.5, 0.5, 0.5, 0.1);
    }
    
    private void spawnHitParticles(EntityLivingBase target) {
        if (!(boss.world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) boss.world;
        
        ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
            target.posX, target.posY + target.height * 0.5, target.posZ,
            20, 0.3, 0.3, 0.3, 0.1);
    }
    
    private void spawnLightningLine(EntityLivingBase from, EntityLivingBase to) {
        if (!(boss.world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) boss.world;
        
        double dx = to.posX - from.posX;
        double dy = (to.posY + to.height * 0.5) - (from.posY + from.height * 0.5);
        double dz = to.posZ - from.posZ;
        
        int particles = 10;
        for (int i = 0; i <= particles; i++) {
            double p = (double) i / particles;
            double px = from.posX + dx * p + (boss.getRNG().nextDouble() - 0.5) * 0.3;
            double py = from.posY + from.height * 0.5 + dy * p + (boss.getRNG().nextDouble() - 0.5) * 0.3;
            double pz = from.posZ + dz * p + (boss.getRNG().nextDouble() - 0.5) * 0.3;
            
            ws.spawnParticle(EnumParticleTypes.SPELL_INSTANT, px, py, pz, 1, 0, 0, 0.0, 0);
        }
    }
    
    private void spawnLightningChargeEffect(Vec3d pos) {
        if (!(boss.world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) boss.world;
        
        for (int ring = 0; ring < 3; ring++) {
            double radius = 1.0 + ring * 0.3;
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20;
                ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    pos.x + Math.cos(angle) * radius,
                    pos.y + ring * 0.2,
                    pos.z + Math.sin(angle) * radius,
                    1, 0, 0.1, 0.0, 0);
            }
        }
    }
    private int teleportCooldown = 0;
    private static final int TELEPORT_COOLDOWN_BASE = 60;
    private static final double TELEPORT_TRIGGER_DIST_SQ = 400.0;  // 20格
    private static final double TELEPORT_MAX_DIST_SQ = 2500.0;     // 50格

    public void tryTeleport(EntityPlayer target) {
        if (teleportCooldown > 0) {
            teleportCooldown--;
            return;
        }

        if (target == null || boss.world.isRemote) return;

        // 状态检查
        RiftwardenState state = boss.getCurrentState();
        if (state.isCasting() || state.isMelee() || state == RiftwardenState.EXHAUSTED) {
            return;
        }

        double distSq = boss.getDistanceSq(target);
        if (distSq < TELEPORT_TRIGGER_DIST_SQ || distSq > TELEPORT_MAX_DIST_SQ) {
            return;
        }

        // 执行瞬移
        performTeleport(target);
    }

    private void performTeleport(EntityPlayer target) {
        // 进入瞬移状态
        boss.getStateManager().forceTransitionTo(RiftwardenState.TELEPORTING, 10);

        // 起始效果
        spawnTeleportEffect(boss.posX, boss.posY, boss.posZ, true);
        boss.playGlobal(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1.5F, 0.5F);

        // 计算目标位置
        double angle = boss.getRNG().nextDouble() * Math.PI * 2;
        double distance = 3 + boss.getRNG().nextDouble() * 2;
        double newX = target.posX + Math.cos(angle) * distance;
        double newZ = target.posZ + Math.sin(angle) * distance;
        double newY = target.posY;

        // 寻找有效位置
        BlockPos targetPos = new BlockPos(newX, newY, newZ);
        while (boss.world.isAirBlock(targetPos) && targetPos.getY() > 0) {
            targetPos = targetPos.down();
        }
        targetPos = targetPos.up();

        if (!boss.world.isAirBlock(targetPos) || !boss.world.isAirBlock(targetPos.up())) {
            newX = target.posX;
            newY = target.posY + 2;
            newZ = target.posZ;
        } else {
            newY = targetPos.getY();
        }

        // 移动
        boss.setPositionAndUpdate(newX, newY, newZ);

        // 结束效果
        spawnTeleportEffect(newX, newY, newZ, false);
        boss.playGlobal(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1.5F, 1.5F);

        // 设置朝向目标
        boss.getMovementController().lookAt(target);

        // 临时无敌帧
        boss.hurtResistantTime = 20;

        // 高阶段瞬移后攻击
        if (boss.getPhase() >= 2) {
            for (int i = 0; i < 3 + boss.getPhase(); i++) {
                double bulletAngle = (Math.PI * 2 * i) / (3 + boss.getPhase());
                shootRadialBullet(bulletAngle, BULLET_DAMAGE * 0.6F, 1.0F);
            }
        }

        // 提示
        if (target instanceof EntityPlayerMP) {
            target.sendMessage(new net.minecraft.util.text.TextComponentString(
                    "§5虚空守望者撕裂空间出现在你身边！"));
        }

        teleportCooldown = TELEPORT_COOLDOWN_BASE - boss.getPhase() * 10;
    }

    private void spawnTeleportEffect(double x, double y, double z, boolean isStart) {
        if (!(boss.world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) boss.world;

        for (int i = 0; i < 32; i++) {
            double angle = (Math.PI * 2 * i) / 32;
            for (double h = 0; h < boss.height; h += 0.5) {
                double radius = isStart ? 1.5 : 2.0;
                double px = x + Math.cos(angle) * radius;
                double pz = z + Math.sin(angle) * radius;

                ws.spawnParticle(EnumParticleTypes.PORTAL, px, y + h, pz, 2, 0.1, 0.1, 0.1, 0.0);
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH, px, y + h, pz, 1, 0, 0, 0, 0.0);
            }
        }

        if (!isStart) {
            ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                    x, y + boss.height * 0.5, z, 3, 0, 0.0, 0.0, 0);
        }
    }
    private void spawnArcEffect(EntityPlayer target, Vec3d startPos) {
        if (!(boss.world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) boss.world;
        
        double dx = target.posX - startPos.x;
        double dy = target.posY + target.height * 0.5 - startPos.y;
        double dz = target.posZ - startPos.z;
        
        int particles = 20;
        for (int i = 0; i <= particles; i++) {
            double p = (double) i / particles;
            double px = startPos.x + dx * p + (boss.getRNG().nextDouble() - 0.5) * 0.5;
            double py = startPos.y + dy * p + (boss.getRNG().nextDouble() - 0.5) * 0.5;
            double pz = startPos.z + dz * p + (boss.getRNG().nextDouble() - 0.5) * 0.5;
            
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, px, py, pz, 1, 0, 0, 0.0, 0);
            ws.spawnParticle(EnumParticleTypes.SPELL_MOB, px, py, pz, 1, 0.1, 0, 0.1, 0);
        }
        
        // 目标脚下光环
        for (int i = 0; i < 16; i++) {
            double angle = (Math.PI * 2 * i) / 16;
            ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                target.posX + Math.cos(angle) * 1.5, target.posY,
                target.posZ + Math.sin(angle) * 1.5,
                1, 0, 0.2, 0, 0.05);
        }
    }
    
    // ========== NBT ==========
    
    public void writeToNBT(NBTTagCompound nbt) {
        // 保存冷却
        NBTTagCompound cooldownsTag = new NBTTagCompound();
        for (Map.Entry<RiftwardenAttackType, Integer> entry : attackCooldowns.entrySet()) {
            cooldownsTag.setInteger(entry.getKey().name(), entry.getValue());
        }
        nbt.setTag("AttackCooldowns", cooldownsTag);
        
        // 保存激光状态
        nbt.setInteger("LaserPhase", laserPhase);
        nbt.setInteger("LaserTimer", laserTimer);
        
        // 保存弹幕角度
        nbt.setFloat("SpiralAngle", spiralAngle);
        nbt.setFloat("WaveAngle", waveAngle);
    }
    
    public void readFromNBT(NBTTagCompound nbt) {
        // 读取冷却
        if (nbt.hasKey("AttackCooldowns")) {
            NBTTagCompound cooldownsTag = nbt.getCompoundTag("AttackCooldowns");
            for (RiftwardenAttackType type : RiftwardenAttackType.values()) {
                if (cooldownsTag.hasKey(type.name())) {
                    attackCooldowns.put(type, cooldownsTag.getInteger(type.name()));
                }
            }
        }
        
        // 读取激光状态
        laserPhase = nbt.getInteger("LaserPhase");
        laserTimer = nbt.getInteger("LaserTimer");
        
        // 读取弹幕角度
        spiralAngle = nbt.getFloat("SpiralAngle");
        waveAngle = nbt.getFloat("WaveAngle");
    }
}