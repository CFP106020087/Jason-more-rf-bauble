package com.moremod.entity;

import com.moremod.util.DamageSourceSwordBeam;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.nbt.NBTTagCompound;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EntitySwordBeam extends EntityThrowable {

    private float damage = 10.0F;
    private int maxLifeTicks = 100;
    private int lifeTicks = 0;
    private BeamType beamType = BeamType.NORMAL;
    private int penetrateCount = 0;
    private int maxPenetrate = 0;

    // 防止重复伤害同一目标
    private Set<UUID> hitEntities = new HashSet<>();

    // 视觉属性
    private float red = 1.0F;
    private float green = 1.0F;
    private float blue = 1.0F;
    private float scale = 1.0F;

    public enum BeamType {
        NORMAL,
        SPIRAL,
        CRESCENT,
        CROSS,
        DRAGON,
        PHOENIX
    }

    public EntitySwordBeam(World worldIn) {
        super(worldIn);
        this.setSize(0.5F, 0.5F);
    }

    public EntitySwordBeam(World worldIn, EntityLivingBase thrower) {
        super(worldIn, thrower);
        this.setSize(0.5F, 0.5F);
    }

    public EntitySwordBeam(World worldIn, double x, double y, double z) {
        super(worldIn, x, y, z);
        this.setSize(0.5F, 0.5F);
    }

    // ========================================
    // 设置器
    // ========================================

    public EntitySwordBeam setDamage(float damage) {
        this.damage = damage;
        return this;
    }

    public EntitySwordBeam setMaxLifetime(int ticks) {
        this.maxLifeTicks = ticks;
        return this;
    }

    public EntitySwordBeam setBeamType(BeamType type) {
        this.beamType = type;
        return this;
    }

    public EntitySwordBeam setColor(float r, float g, float b) {
        this.red = r;
        this.green = g;
        this.blue = b;
        return this;
    }

    public EntitySwordBeam setScale(float scale) {
        this.scale = scale;
        return this;
    }

    public EntitySwordBeam setPenetrate(int count) {
        this.maxPenetrate = count;
        return this;
    }

    // Getters
    public BeamType getBeamType() { return beamType; }
    public float getRed() { return red; }
    public float getGreen() { return green; }
    public float getBlue() { return blue; }
    public float getScale() { return scale; }
    public float getLifeProgress() {
        return (float) lifeTicks / maxLifeTicks;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        lifeTicks++;
        if (lifeTicks > maxLifeTicks) {
            this.setDead();
            return;
        }

        // 超出世界边界检查
        if (Math.abs(posX) > 30000000 || Math.abs(posZ) > 30000000 || posY < -64 || posY > 256) {
            this.setDead();
            return;
        }

        if (world.isRemote) {
            spawnEnhancedTrailParticles();
        }

        switch (beamType) {
            case SPIRAL:
                applySpiralMotion();
                break;
            case DRAGON:
                applyDragonMotion();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (world.isRemote || isDead) return;

        if (result.typeOfHit == RayTraceResult.Type.ENTITY) {
            if (result.entityHit instanceof EntityLivingBase) {
                EntityLivingBase target = (EntityLivingBase) result.entityHit;

                // 忽略发射者
                if (target == this.getThrower()) {
                    return;
                }

                // 防止重复伤害
                if (hitEntities.contains(target.getUniqueID())) {
                    return;
                }

                // ⭐ 使用自定义伤害源，标记为剑气伤害
                DamageSource beamDamage = new DamageSourceSwordBeam(this, getThrower());

                // 尝试造成伤害
                boolean damaged = target.attackEntityFrom(beamDamage, damage);

                if (damaged) {
                    // 记录已击中的目标
                    hitEntities.add(target.getUniqueID());

                    // 击中特效
                    spawnHitEffects(result.hitVec.x, result.hitVec.y, result.hitVec.z);
                }

                // 穿透检查
                penetrateCount++;
                if (penetrateCount > maxPenetrate) {
                    spawnImpactEffects(result);
                    this.setDead();
                }
            }
        }
        else if (result.typeOfHit == RayTraceResult.Type.BLOCK) {
            spawnImpactEffects(result);
            this.setDead();
        }
    }

    // 特殊运动模式
    private void applySpiralMotion() {
        double angle = lifeTicks * 0.3;
        double radius = 0.3;
        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        motionX += offsetX * 0.01;
        motionZ += offsetZ * 0.01;
    }

    private void applyDragonMotion() {
        double wave = Math.sin(lifeTicks * 0.2) * 0.05;
        motionY += wave;
    }

    // 粒子效果方法保持不变...
    private void spawnEnhancedTrailParticles() {
        for (int i = 0; i < 4; i++) {
            double offsetX = (rand.nextDouble() - 0.5) * 0.4;
            double offsetY = (rand.nextDouble() - 0.5) * 0.4;
            double offsetZ = (rand.nextDouble() - 0.5) * 0.4;

            world.spawnParticle(EnumParticleTypes.REDSTONE,
                    posX + offsetX, posY + offsetY, posZ + offsetZ,
                    red, green, blue);
        }

        if (rand.nextFloat() < 0.3F) {
            world.spawnParticle(EnumParticleTypes.FLAME,
                    posX, posY, posZ, 0, 0, 0);
        }

        spawnTypeSpecificParticles();
    }

    private void spawnTypeSpecificParticles() {
        switch (beamType) {
            case SPIRAL:
                double angle = lifeTicks * 0.3;
                double radius = 0.5;
                double px = posX + Math.cos(angle) * radius;
                double pz = posZ + Math.sin(angle) * radius;
                world.spawnParticle(EnumParticleTypes.REDSTONE,
                        px, posY, pz, red, green, blue);
                break;
            case DRAGON:
                if (rand.nextFloat() < 0.5F) {
                    world.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                            posX, posY, posZ,
                            (rand.nextDouble() - 0.5) * 0.1,
                            (rand.nextDouble() - 0.5) * 0.1,
                            (rand.nextDouble() - 0.5) * 0.1);
                }
                break;
            case PHOENIX:
                if (rand.nextFloat() < 0.7F) {
                    world.spawnParticle(EnumParticleTypes.FLAME,
                            posX, posY, posZ,
                            (rand.nextDouble() - 0.5) * 0.05,
                            -0.1,
                            (rand.nextDouble() - 0.5) * 0.05);
                }
                break;
            default:
                break;
        }
    }

    private void spawnHitEffects(double x, double y, double z) {
        if (world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer serverWorld = (net.minecraft.world.WorldServer) world;
            for (int i = 0; i < 20; i++) {
                serverWorld.spawnParticle(EnumParticleTypes.REDSTONE,
                        x, y, z, 1,
                        (rand.nextDouble() - 0.5) * 0.5,
                        (rand.nextDouble() - 0.5) * 0.5,
                        (rand.nextDouble() - 0.5) * 0.5, 0);
            }
            serverWorld.spawnParticle(EnumParticleTypes.SWEEP_ATTACK,
                    x, y, z, 3, 0.1, 0.1, 0.1, 0);
        }
    }

    private void spawnImpactEffects(RayTraceResult result) {
        if (world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer serverWorld = (net.minecraft.world.WorldServer) world;
            double x = result.hitVec.x;
            double y = result.hitVec.y;
            double z = result.hitVec.z;

            serverWorld.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                    x, y, z, 1, 0, 0, 0.0, 0);

            for (int i = 0; i < 40; i++) {
                double vx = (rand.nextDouble() - 0.5) * 0.5;
                double vy = (rand.nextDouble() - 0.5) * 0.5;
                double vz = (rand.nextDouble() - 0.5) * 0.5;
                serverWorld.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                        x, y, z, 1, vx, vy, vz, 0.1);
            }
        }

        world.playSound(null, posX, posY, posZ,
                net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE,
                net.minecraft.util.SoundCategory.PLAYERS,
                0.7F, 1.2F + rand.nextFloat() * 0.4F);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setFloat("Damage", damage);
        compound.setInteger("MaxLifeTicks", maxLifeTicks);
        compound.setInteger("LifeTicks", lifeTicks);
        compound.setInteger("BeamType", beamType.ordinal());
        compound.setInteger("MaxPenetrate", maxPenetrate);
        compound.setInteger("PenetrateCount", penetrateCount);
        compound.setFloat("Red", red);
        compound.setFloat("Green", green);
        compound.setFloat("Blue", blue);
        compound.setFloat("Scale", scale);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        damage = compound.getFloat("Damage");
        maxLifeTicks = compound.getInteger("MaxLifeTicks");
        lifeTicks = compound.getInteger("LifeTicks");
        beamType = BeamType.values()[compound.getInteger("BeamType")];
        maxPenetrate = compound.getInteger("MaxPenetrate");
        penetrateCount = compound.getInteger("PenetrateCount");
        red = compound.getFloat("Red");
        green = compound.getFloat("Green");
        blue = compound.getFloat("Blue");
        scale = compound.getFloat("Scale");
    }

    @Override
    protected float getGravityVelocity() {
        return 0.0F;
    }
}