package com.moremod.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 剑气实体 - 完整实现，包含数据同步
 */
public class EntitySwordBeam extends EntityThrowable implements IAnimatable {

    private final AnimationFactory factory = new AnimationFactory(this);

    // ========================================
    // 数据同步参数 - 关键！
    // ========================================
    private static final DataParameter<Integer> BEAM_TYPE = EntityDataManager.createKey(EntitySwordBeam.class, DataSerializers.VARINT);
    private static final DataParameter<Float> DAMAGE = EntityDataManager.createKey(EntitySwordBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> COLOR_R = EntityDataManager.createKey(EntitySwordBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> COLOR_G = EntityDataManager.createKey(EntitySwordBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> COLOR_B = EntityDataManager.createKey(EntitySwordBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> SCALE = EntityDataManager.createKey(EntitySwordBeam.class, DataSerializers.FLOAT);

    private int maxLifeTicks = 300;
    private int lifeTicks = 0;
    private int penetrateCount = 0;
    private int maxPenetrate = 0;

    private Set<UUID> hitEntities = new HashSet<>();

    public enum BeamType {
        NORMAL(0),
        SPIRAL(1),
        CRESCENT(2),
        CROSS(3),
        DRAGON(4),
        PHOENIX(5),
        BALL(6);

        private final int id;

        BeamType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static BeamType fromId(int id) {
            for (BeamType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return NORMAL;
        }
    }

    // ========================================
    // 构造函数
    // ========================================
    
    public EntitySwordBeam(World worldIn) {
        super(worldIn);
        this.setSize(0.5F, 0.5F);
    }

    public EntitySwordBeam(World worldIn, EntityLivingBase thrower) {
        super(worldIn, thrower);
        this.setSize(0.5F, 0.5F);

        // -----------------------------------------
        // 1. 使用真实视线方向（最关键！）
        // -----------------------------------------
        Vec3d look = thrower.getLookVec();

        // -----------------------------------------
        // 2. 胸口高度（比眼睛低 0.5）
        // -----------------------------------------
        double chestY = thrower.posY + thrower.getEyeHeight() - 0.5D;

        // -----------------------------------------
        // 3. 前方 1.6 格（可调整）
        // -----------------------------------------
        double distance = 1.6D;

        double sx = thrower.posX + look.x * distance;
        double sy = chestY;
        double sz = thrower.posZ + look.z * distance;

        // 设置生成点
        this.setPosition(sx, sy, sz);

        // -----------------------------------------
        // 4. 飞行速度（完全沿视线方向）
        // -----------------------------------------
        double velocity = 0.8F;
        this.motionX = look.x * velocity;
        this.motionY = look.y * velocity;
        this.motionZ = look.z * velocity;

        // 保证立即更新
        this.velocityChanged = true;

        // -----------------------------------------
        // 5. 让剑气的 yaw/pitch 与玩家一致（渲染角度用得到）
        // -----------------------------------------
        this.rotationYaw = thrower.rotationYaw;
        this.rotationPitch = thrower.rotationPitch;
    }

    public EntitySwordBeam(World worldIn, double x, double y, double z) {
        super(worldIn, x, y, z);
        this.setSize(0.5F, 0.5F);
    }

    // ========================================
    // 数据管理器初始化 - 重要！
    // ========================================
    
    @Override
    protected void entityInit() {
        super.entityInit();
        // 注册数据参数
        this.dataManager.register(BEAM_TYPE, 0); // 默认NORMAL
        this.dataManager.register(DAMAGE, 10.0F);
        this.dataManager.register(COLOR_R, 1.0F);
        this.dataManager.register(COLOR_G, 1.0F);
        this.dataManager.register(COLOR_B, 1.0F);
        this.dataManager.register(SCALE, 1.0F);
    }

    // ========================================
    // GeckoLib动画
    // ========================================

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        BeamType type = getBeamType();
        switch (type) {
            case DRAGON:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.dragon.fly", true));
                break;
            case BALL:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.ball.pulse", true));
                break;
            default:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("lxs_light.animation", true));
                break;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    // ========================================
    // 设置器 - 使用DataManager
    // ========================================

    public EntitySwordBeam setDamage(float damage) {
        this.dataManager.set(DAMAGE, damage);
        return this;
    }

    public EntitySwordBeam setMaxLifetime(int ticks) {
        this.maxLifeTicks = ticks;
        return this;
    }

    public EntitySwordBeam setBeamType(BeamType type) {
        this.dataManager.set(BEAM_TYPE, type.getId());
        return this;
    }

    public EntitySwordBeam setColor(float r, float g, float b) {
        this.dataManager.set(COLOR_R, r);
        this.dataManager.set(COLOR_G, g);
        this.dataManager.set(COLOR_B, b);
        return this;
    }

    public EntitySwordBeam setScale(float scale) {
        this.dataManager.set(SCALE, scale);
        return this;
    }

    public EntitySwordBeam setPenetrate(int count) {
        this.maxPenetrate = count;
        return this;
    }

    // ========================================
    // 获取器 - 从DataManager读取
    // ========================================
    
    public BeamType getBeamType() {
        return BeamType.fromId(this.dataManager.get(BEAM_TYPE));
    }
    
    public float getRed() {
        return this.dataManager.get(COLOR_R);
    }
    
    public float getGreen() {
        return this.dataManager.get(COLOR_G);
    }
    
    public float getBlue() {
        return this.dataManager.get(COLOR_B);
    }
    
    public float getScale() {
        return this.dataManager.get(SCALE);
    }
    
    public float getDamage() {
        return this.dataManager.get(DAMAGE);
    }
    
    public float getLifeProgress() {
        return (float) lifeTicks / maxLifeTicks;
    }

    // ========================================
    // 更新逻辑
    // ========================================
    @Override
    public void onUpdate() {
        // 不要调用 super.onUpdate() ！！！
        // super.onUpdate();  // ❌ 删掉这行

        // 自己手动处理位置更新
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        // 手动移动
        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;

        // 手动设置位置
        this.setPosition(this.posX, this.posY, this.posZ);

        // 你的其他逻辑
        lifeTicks++;
        if (lifeTicks > maxLifeTicks) {
            this.setDead();
            return;
        }

        // 碰撞检测
        RayTraceResult raytraceresult = ProjectileHelper.forwardsRaycast(this, true, false, this.getThrower());
        if (raytraceresult != null) {
            this.onImpact(raytraceresult);
        }

        // 粒子等
        if (world.isRemote) {
            spawnTrailParticles();
        }
    }

    @Override
    protected void onImpact(RayTraceResult result) {
        if (world.isRemote || isDead) return;

        // 不击中发射者
        if (result.entityHit != null && result.entityHit == this.getThrower()) {
            return;
        }

        if (result.typeOfHit == RayTraceResult.Type.ENTITY) {
            if (result.entityHit instanceof EntityLivingBase) {
                EntityLivingBase target = (EntityLivingBase) result.entityHit;

                if (hitEntities.contains(target.getUniqueID())) return;

                float damage = getDamage();
                boolean damaged = target.attackEntityFrom(
                    DamageSource.causeThrownDamage(this, getThrower()), damage);

                if (damaged) {
                    hitEntities.add(target.getUniqueID());
                    spawnHitEffects(result.hitVec.x, result.hitVec.y, result.hitVec.z);
                }

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

    // ========================================
    // 特殊运动
    // ========================================

    private void applySpiralMotion() {
        double angle = lifeTicks * 0.3;
        double radius = 0.3;
        motionX += Math.cos(angle) * radius * 0.01;
        motionZ += Math.sin(angle) * radius * 0.01;
    }

    private void applyDragonMotion() {
        motionY += Math.sin(lifeTicks * 0.2) * 0.05;
    }

    private void applyBallMotion() {
        if (lifeTicks < 20) motionY += 0.01;
    }

    // ========================================
    // 粒子效果 - 简化版，避免崩溃
    // ========================================

    @SideOnly(Side.CLIENT)
    private void spawnTrailParticles() {
        // 使用原版粒子，避免自定义粒子崩溃
        for (int i = 0; i < 2; i++) {
            world.spawnParticle(EnumParticleTypes.REDSTONE,
                posX + (rand.nextDouble() - 0.5) * 0.4,
                posY + (rand.nextDouble() - 0.5) * 0.4,
                posZ + (rand.nextDouble() - 0.5) * 0.4,
                getRed(), getGreen(), getBlue());
        }

        // 类型特定粒子
        BeamType type = getBeamType();
        switch (type) {
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
                if (rand.nextFloat() < 0.3F) {
                    world.spawnParticle(EnumParticleTypes.FLAME,
                        posX, posY, posZ, 0, 0.05, 0);
                }
                break;
            case BALL:
                if (rand.nextFloat() < 0.8F) {
                    double angle = rand.nextDouble() * Math.PI * 2;
                    double radius = 0.3;
                    world.spawnParticle(EnumParticleTypes.END_ROD,
                        posX + Math.cos(angle) * radius,
                        posY,
                        posZ + Math.sin(angle) * radius,
                        0, 0, 0);
                }
                break;
        }
    }

    private void spawnHitEffects(double x, double y, double z) {
        if (world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer serverWorld = (net.minecraft.world.WorldServer) world;
            for (int i = 0; i < 20; i++) {
                serverWorld.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                    x, y, z, 1,
                    (rand.nextDouble() - 0.5) * 0.5,
                    (rand.nextDouble() - 0.5) * 0.5,
                    (rand.nextDouble() - 0.5) * 0.5, 0);
            }
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
        }

        world.playSound(null, posX, posY, posZ,
            net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE,
            net.minecraft.util.SoundCategory.PLAYERS,
            0.7F, 1.2F + rand.nextFloat() * 0.4F);
    }

    // ========================================
    // NBT
    // ========================================

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("BeamType", getBeamType().getId());
        compound.setFloat("Damage", getDamage());
        compound.setInteger("MaxLifeTicks", maxLifeTicks);
        compound.setInteger("LifeTicks", lifeTicks);
        compound.setInteger("MaxPenetrate", maxPenetrate);
        compound.setInteger("PenetrateCount", penetrateCount);
        compound.setFloat("Red", getRed());
        compound.setFloat("Green", getGreen());
        compound.setFloat("Blue", getBlue());
        compound.setFloat("Scale", getScale());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        setBeamType(BeamType.fromId(compound.getInteger("BeamType")));
        setDamage(compound.getFloat("Damage"));
        maxLifeTicks = compound.getInteger("MaxLifeTicks");
        lifeTicks = compound.getInteger("LifeTicks");
        maxPenetrate = compound.getInteger("MaxPenetrate");
        penetrateCount = compound.getInteger("PenetrateCount");
        setColor(
            compound.getFloat("Red"),
            compound.getFloat("Green"),
            compound.getFloat("Blue")
        );
        setScale(compound.getFloat("Scale"));
    }

    @Override
    protected float getGravityVelocity() {
        return 0.0F;
    }
}