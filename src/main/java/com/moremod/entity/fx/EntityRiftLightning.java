package com.moremod.entity.fx;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class EntityRiftLightning extends EntityThrowable {

    private EntityLivingBase shooter;
    private EntityLivingBase target;
    private float damage = 15.0F;
    private int particleFrequency = 2;

    public EntityRiftLightning(World worldIn) {
        super(worldIn);
        this.setSize(0.5F, 0.5F);
    }

    public EntityRiftLightning(World worldIn, EntityLivingBase shooter, EntityLivingBase target) {
        super(worldIn, shooter);
        this.shooter = shooter;
        this.target = target;

        // 设置初始位置
        this.setPosition(shooter.posX, shooter.posY + shooter.getEyeHeight(), shooter.posZ);

        // 计算到目标的方向
        if (target != null) {
            double dx = target.posX - this.posX;
            double dy = target.posY + target.height * 0.5 - this.posY;
            double dz = target.posZ - this.posZ;

            setThrowableHeading(dx, dy, dz, 1.5F, 2.0F);
        }
    }

    // 替代shoot方法的设置运动方法
    public void setThrowableHeading(double x, double y, double z, float velocity, float inaccuracy) {
        double d0 = Math.sqrt(x * x + y * y + z * z);
        x = x / d0;
        y = y / d0;
        z = z / d0;

        x = x + this.rand.nextGaussian() * 0.007499999832361937D * (double)inaccuracy;
        y = y + this.rand.nextGaussian() * 0.007499999832361937D * (double)inaccuracy;
        z = z + this.rand.nextGaussian() * 0.007499999832361937D * (double)inaccuracy;

        x = x * (double)velocity;
        y = y * (double)velocity;
        z = z * (double)velocity;

        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;
    }

    // 为了兼容Boss代码中的shoot调用
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        setThrowableHeading(x, y, z, velocity, inaccuracy);
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (!this.world.isRemote) {
            if (result.entityHit != null && result.entityHit != this.shooter) {
                // 造成伤害
                if (result.entityHit instanceof EntityLivingBase) {
                    EntityLivingBase victim = (EntityLivingBase) result.entityHit;

                    DamageSource damageSource = this.shooter != null ?
                            DamageSource.causeMobDamage(this.shooter).setDamageBypassesArmor() :
                            DamageSource.MAGIC.setDamageBypassesArmor();

                    victim.attackEntityFrom(damageSource, this.damage);

                    // 击中特效
                    if (world instanceof WorldServer) {
                        WorldServer ws = (WorldServer) world;
                        ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                                victim.posX, victim.posY + victim.height * 0.5, victim.posZ,
                                30, 0.3, 0.3, 0.3, 0.1);

                        // 闪电爆发效果
                        for (int i = 0; i < 8; i++) {
                            double angle = (Math.PI * 2 * i) / 8;
                            ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                                    victim.posX + Math.cos(angle) * 0.5,
                                    victim.posY + victim.height * 0.5,
                                    victim.posZ + Math.sin(angle) * 0.5,
                                    1, 0, 0.2, 0, 0.05);
                        }
                    }

                    // 短暂减速效果
                    victim.motionX *= 0.6;
                    victim.motionZ *= 0.6;
                }
                this.setDead();
            } else if (result.typeOfHit == RayTraceResult.Type.BLOCK) {
                // 撞墙特效
                if (world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;
                    Vec3d hitVec = result.hitVec;
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                            hitVec.x, hitVec.y, hitVec.z,
                            20, 0.2, 0.2, 0.2, 0.05);
                }
                this.setDead();
            }
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        // 追踪目标
        if (!this.world.isRemote && this.target != null && this.target.isEntityAlive() && this.ticksExisted < 100) {
            double speed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);

            if (speed > 0.1 && this.ticksExisted % 2 == 0) {
                double dx = target.posX - this.posX;
                double dy = target.posY + target.height * 0.5 - this.posY;
                double dz = target.posZ - this.posZ;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (distance > 0.5) {
                    // 微调方向追踪
                    double trackingStrength = 0.05;
                    this.motionX += (dx / distance) * trackingStrength;
                    this.motionY += (dy / distance) * trackingStrength;
                    this.motionZ += (dz / distance) * trackingStrength;

                    // 保持速度恒定
                    double newSpeed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
                    if (newSpeed > 0) {
                        this.motionX = (this.motionX / newSpeed) * speed;
                        this.motionY = (this.motionY / newSpeed) * speed;
                        this.motionZ = (this.motionZ / newSpeed) * speed;
                    }
                }
            }
        }

        // 粒子轨迹
        if (this.ticksExisted % particleFrequency == 0) {
            spawnTrailParticles();
        }

        // 超时消失
        if (this.ticksExisted > 200) {
            this.setDead();
        }
    }

    private void spawnTrailParticles() {
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;

            // 紫色烟雾轨迹
            ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                    this.posX, this.posY, this.posZ,
                    3, 0.1, 0.1, 0.1, 0.01);

            // 黑色烟雾
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    this.posX, this.posY, this.posZ,
                    2, 0.05, 0.05, 0.05, 0.01);

            // 电弧效果
            if (this.ticksExisted % 4 == 0) {
                double radius = 0.3;
                for (int i = 0; i < 4; i++) {
                    double angle = this.rand.nextDouble() * Math.PI * 2;
                    ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                            this.posX + Math.cos(angle) * radius,
                            this.posY,
                            this.posZ + Math.sin(angle) * radius,
                            1, 0, 0, 0, 0.02);
                }
            }
        }
    }

    @Override
    protected float getGravityVelocity() {
        return 0.001F; // 几乎不受重力影响
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return this.damage;
    }

    // 用于渲染器的方法
    public boolean isRotating() {
        return true;
    }

    public float getRotationSpeed() {
        return 20.0F;
    }
}