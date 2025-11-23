// ===== EntityVoidBullet.java - 大型虚空子弹 =====
package com.moremod.entity.projectile;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class EntityVoidBullet extends EntityThrowable {

    private float damage = 8.0F;
    private EntityLivingBase shooter;
    private int particleColor = 0; // 0=紫色, 1=蓝色, 2=红色

    public EntityVoidBullet(World worldIn) {
        super(worldIn);
        // 设置更大的碰撞箱
        this.setSize(1.0F, 1.0F); // 从0.25改为1.0
        this.noClip = false;
    }

    public EntityVoidBullet(World worldIn, EntityLivingBase shooter) {
        super(worldIn, shooter);
        this.shooter = shooter;
        this.setSize(1.0F, 1.0F);
        this.noClip = false;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setParticleColor(int color) {
        this.particleColor = color;
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (world.isRemote) return;

        if (result.entityHit != null && result.entityHit != this.shooter) {
            // 无视护甲的伤害
            DamageSource source = new DamageSource("moremod.void_bullet")
                    .setDamageBypassesArmor()
                    .setDamageIsAbsolute()
                    .setProjectile();

            result.entityHit.attackEntityFrom(source, this.damage);

            // 爆炸粒子效果
            if (world instanceof WorldServer) {
                ((WorldServer)world).spawnParticle(
                        EnumParticleTypes.SPELL_WITCH,
                        this.posX, this.posY, this.posZ,
                        30, 0.5, 0.5, 0.5, 0.1
                );

                // 额外的爆炸效果
                ((WorldServer)world).spawnParticle(
                        EnumParticleTypes.CRIT_MAGIC,
                        this.posX, this.posY, this.posZ,
                        20, 0.3, 0.3, 0.3, 0.05
                );
            }
        }

        // 击中方块也产生效果
        if (result.typeOfHit == RayTraceResult.Type.BLOCK && world instanceof WorldServer) {
            ((WorldServer)world).spawnParticle(
                    EnumParticleTypes.SPELL_MOB,
                    this.posX, this.posY, this.posZ,
                    15, 0.2, 0.2, 0.2, 0.0
            );
        }

        this.setDead();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        // 大型粒子轨迹 - 服务端和客户端都生成
        if (this.ticksExisted % 2 == 0) {
            // 核心大粒子团
            for (int i = 0; i < 5; i++) {
                double offsetX = (rand.nextDouble() - 0.5) * 0.3;
                double offsetY = (rand.nextDouble() - 0.5) * 0.3;
                double offsetZ = (rand.nextDouble() - 0.5) * 0.3;

                if (world.isRemote) {
                    // 客户端粒子
                    world.spawnParticle(
                            EnumParticleTypes.PORTAL,
                            this.posX + offsetX,
                            this.posY + offsetY,
                            this.posZ + offsetZ,
                            0, 0, 0
                    );

                    // 外层光环
                    world.spawnParticle(
                            EnumParticleTypes.SPELL_WITCH,
                            this.posX + offsetX * 2,
                            this.posY + offsetY * 2,
                            this.posZ + offsetZ * 2,
                            0, 0, 0
                    );
                } else if (world instanceof WorldServer) {
                    // 服务端粒子（确保所有玩家都能看到）

                }
            }

            // 拖尾效果
            Vec3d motion = new Vec3d(this.motionX, this.motionY, this.motionZ).normalize();
            for (int i = 1; i <= 3; i++) {
                double trailX = this.posX - motion.x * i * 0.5;
                double trailY = this.posY - motion.y * i * 0.5;
                double trailZ = this.posZ - motion.z * i * 0.5;

                if (world.isRemote) {
                    world.spawnParticle(
                            EnumParticleTypes.SPELL_MOB,
                            trailX, trailY, trailZ,
                            0, 0, 0
                    );
                }
            }
        }

        // 旋转粒子环
        if (!world.isRemote && world instanceof WorldServer) {
            double angle = this.ticksExisted * 0.5;
            for (int i = 0; i < 4; i++) {
                double ringAngle = angle + (Math.PI * 2 * i / 4);
                double ringX = this.posX + Math.cos(ringAngle) * 0.5;
                double ringY = this.posY;
                double ringZ = this.posZ + Math.sin(ringAngle) * 0.5;


            }
        }

        // 超时销毁
        if (this.ticksExisted > 100) { // 5秒
            this.setDead();
        }
    }

    @Override
    protected float getGravityVelocity() {
        return 0.001F; // 几乎无重力
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setFloat("damage", this.damage);
        compound.setInteger("particleColor", this.particleColor);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.damage = compound.getFloat("damage");
        this.particleColor = compound.getInteger("particleColor");
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public float getCollisionBorderSize() {
        return 1.0F;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        // 增加渲染距离
        return distance < 4096.0D; // 64格内都渲染
    }
}