package com.moremod.entity.fx;

import com.moremod.util.combat.TrueDamageHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.UUID;

/**
 * 奇点实体 - 维度崩塌技能核心
 * Singularity Entity - Core of Dimensional Collapse
 *
 * 行为阶段：
 * 1. 上升阶段 (0-60 tick / 3秒) - 从玩家位置缓缓上升
 * 2. 稳定阶段 (60-200 tick / 7秒) - 停住，吸引+持续伤害
 * 3. 引爆阶段 (200 tick) - 爆炸造成大量伤害
 */
public class EntitySingularity extends Entity {

    // 同步数据
    private static final DataParameter<Float> SCALE = EntityDataManager.createKey(EntitySingularity.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> PHASE = EntityDataManager.createKey(EntitySingularity.class, DataSerializers.VARINT);
    private static final DataParameter<Float> STORED_DAMAGE = EntityDataManager.createKey(EntitySingularity.class, DataSerializers.FLOAT);

    // 阶段常量
    public static final int PHASE_RISING = 0;      // 上升阶段
    public static final int PHASE_STABLE = 1;      // 稳定阶段
    public static final int PHASE_EXPLODING = 2;   // 引爆阶段

    // 时间常量
    private static final int RISE_DURATION = 60;       // 3秒上升
    private static final int STABLE_DURATION = 140;    // 7秒稳定 (总10秒)
    private static final int TOTAL_DURATION = 200;     // 10秒总时长

    // 效果参数
    private static final double RISE_HEIGHT = 4.0;     // 上升高度
    private static final double PULL_RANGE = 15.0;     // 吸引范围
    private static final double DAMAGE_RANGE = 12.0;   // 伤害范围
    private static final float BASE_PULL_STRENGTH = 0.15f;  // 基础吸引力

    // 施法者信息
    private UUID casterUUID;
    private EntityPlayer caster;
    private int spatialCount = 1;  // 空间织印件数

    // 起始位置
    private double startY;

    public EntitySingularity(World worldIn) {
        super(worldIn);
        this.setSize(0.5F, 0.5F);
        this.noClip = true;
        this.isImmuneToFire = true;
    }

    public EntitySingularity(World worldIn, EntityPlayer caster, float storedDamage, int spatialCount) {
        this(worldIn);
        this.caster = caster;
        this.casterUUID = caster.getUniqueID();
        this.spatialCount = spatialCount;
        this.setStoredDamage(storedDamage);

        // 设置初始位置（玩家中心）
        this.setPosition(caster.posX, caster.posY + 1.0, caster.posZ);
        this.startY = this.posY;
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(SCALE, 1.0F);
        this.dataManager.register(PHASE, PHASE_RISING);
        this.dataManager.register(STORED_DAMAGE, 0F);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        // 查找施法者
        if (caster == null && casterUUID != null && !world.isRemote) {
            caster = world.getPlayerEntityByUUID(casterUUID);
        }

        int phase = getPhase();

        // 阶段转换
        if (ticksExisted < RISE_DURATION) {
            if (phase != PHASE_RISING) setPhase(PHASE_RISING);
            handleRisingPhase();
        } else if (ticksExisted < TOTAL_DURATION) {
            if (phase != PHASE_STABLE) {
                setPhase(PHASE_STABLE);
                // 播放稳定音效
                world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_PORTAL_AMBIENT,
                        SoundCategory.PLAYERS, 2.0F, 0.5F);
            }
            handleStablePhase();
        } else {
            if (phase != PHASE_EXPLODING) setPhase(PHASE_EXPLODING);
            handleExplosion();
            return;
        }

        // 粒子效果
        spawnParticles();

        // 缩放动画
        updateScale();
    }

    /**
     * 上升阶段 - 缓缓向上移动
     */
    private void handleRisingPhase() {
        // 缓动曲线上升
        float progress = (float) ticksExisted / RISE_DURATION;
        float easedProgress = easeOutQuad(progress);

        double targetY = startY + RISE_HEIGHT * easedProgress;
        this.setPosition(posX, targetY, posZ);

        // 上升阶段轻微吸引
        if (!world.isRemote && ticksExisted % 5 == 0) {
            pullEntities(0.3f);
        }
    }

    /**
     * 稳定阶段 - 吸引 + 持续伤害
     */
    private void handleStablePhase() {
        if (world.isRemote) return;

        // 每 tick 吸引
        pullEntities(1.0f);

        // 每 10 tick 造成伤害
        if (ticksExisted % 10 == 0) {
            dealDamageToNearby();
        }

        // 脉冲音效
        if (ticksExisted % 20 == 0) {
            world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_PORTAL_AMBIENT,
                    SoundCategory.PLAYERS, 1.0F, 0.3F);
        }
    }

    /**
     * 吸引周围实体
     */
    private void pullEntities(float strengthMultiplier) {
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                new AxisAlignedBB(posX - PULL_RANGE, posY - PULL_RANGE, posZ - PULL_RANGE,
                        posX + PULL_RANGE, posY + PULL_RANGE, posZ + PULL_RANGE),
                e -> e != caster && e.isEntityAlive() && !(e instanceof EntityPlayer && ((EntityPlayer)e).isCreative())
        );

        for (EntityLivingBase entity : entities) {
            double dx = posX - entity.posX;
            double dy = posY - entity.posY;
            double dz = posZ - entity.posZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance < 0.5) distance = 0.5;
            if (distance > PULL_RANGE) continue;

            // 距离越近吸引力越强
            double pullStrength = BASE_PULL_STRENGTH * strengthMultiplier * (1.0 - distance / PULL_RANGE);
            pullStrength *= (1.0 + spatialCount * 0.2);  // 件数加成

            double factor = pullStrength / distance;
            entity.motionX += dx * factor;
            entity.motionY += dy * factor;
            entity.motionZ += dz * factor;
            entity.velocityChanged = true;
        }
    }

    /**
     * 对附近实体造成真实伤害
     */
    private void dealDamageToNearby() {
        float storedDamage = getStoredDamage();
        if (storedDamage <= 0) return;

        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                new AxisAlignedBB(posX - DAMAGE_RANGE, posY - DAMAGE_RANGE, posZ - DAMAGE_RANGE,
                        posX + DAMAGE_RANGE, posY + DAMAGE_RANGE, posZ + DAMAGE_RANGE),
                e -> e != caster && e.isEntityAlive()
        );

        for (EntityLivingBase entity : entities) {
            double distance = entity.getDistance(posX, posY, posZ);
            if (distance > DAMAGE_RANGE) continue;

            // 距离倍率：距离越近伤害越高
            // 0格 = 2.0x, 6格 = 1.0x, 12格 = 0.5x
            float distanceMultiplier = (float) (2.0 - (distance / DAMAGE_RANGE) * 1.5);
            distanceMultiplier = Math.max(0.5f, distanceMultiplier);

            // 件数倍率
            float pieceMultiplier = 1.0f + (spatialCount - 3) * 0.25f;  // 3件=1.0x, 4件=1.25x

            // 每次伤害 = 存储伤害 * 0.1 * 距离倍率 * 件数倍率
            float damage = storedDamage * 0.1f * distanceMultiplier * pieceMultiplier;

            TrueDamageHelper.applyWrappedTrueDamage(entity, caster, damage, TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);

            // 伤害特效
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR,
                        entity.posX, entity.posY + entity.height / 2, entity.posZ,
                        (int) (damage / 2), 0.2, 0.2, 0.2, 0.1);
            }
        }
    }

    /**
     * 引爆阶段
     */
    private void handleExplosion() {
        if (world.isRemote) {
            // 客户端爆炸特效
            spawnExplosionParticles();
        } else {
            // 服务端爆炸伤害
            float storedDamage = getStoredDamage();
            float explosionMultiplier = 2.0f + spatialCount * 0.5f;  // 3件=3.5x, 4件=4.0x

            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                    EntityLivingBase.class,
                    new AxisAlignedBB(posX - DAMAGE_RANGE, posY - DAMAGE_RANGE, posZ - DAMAGE_RANGE,
                            posX + DAMAGE_RANGE, posY + DAMAGE_RANGE, posZ + DAMAGE_RANGE),
                    e -> e != caster && e.isEntityAlive()
            );

            for (EntityLivingBase entity : entities) {
                double distance = entity.getDistance(posX, posY, posZ);
                if (distance > DAMAGE_RANGE) continue;

                // 爆炸伤害随距离衰减
                float distanceFalloff = (float) (1.0 - (distance / DAMAGE_RANGE) * 0.5);
                float explosionDamage = storedDamage * explosionMultiplier * distanceFalloff;

                TrueDamageHelper.applyWrappedTrueDamage(entity, caster, explosionDamage, TrueDamageHelper.TrueDamageFlag.PHANTOM_STRIKE);
            }

            // 爆炸音效
            world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.PLAYERS, 2.0F, 0.5F);
        }

        this.setDead();
    }

    /**
     * 生成粒子效果
     */
    private void spawnParticles() {
        if (!world.isRemote) return;

        int phase = getPhase();
        float scale = getScale();

        // 核心粒子
        for (int i = 0; i < 5; i++) {
            double offsetX = (rand.nextDouble() - 0.5) * scale;
            double offsetY = (rand.nextDouble() - 0.5) * scale;
            double offsetZ = (rand.nextDouble() - 0.5) * scale;

            world.spawnParticle(EnumParticleTypes.PORTAL,
                    posX + offsetX, posY + offsetY, posZ + offsetZ,
                    0, 0, 0);
        }

        // 吸引轨迹粒子
        if (phase == PHASE_STABLE && ticksExisted % 2 == 0) {
            for (int i = 0; i < 8; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double dist = PULL_RANGE * rand.nextDouble();
                double px = posX + Math.cos(angle) * dist;
                double pz = posZ + Math.sin(angle) * dist;
                double py = posY + (rand.nextDouble() - 0.5) * 4;

                // 粒子朝向奇点移动
                double dx = (posX - px) * 0.1;
                double dy = (posY - py) * 0.1;
                double dz = (posZ - pz) * 0.1;

                world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        px, py, pz, dx, dy, dz);
            }
        }

        // 旋转环
        double ringAngle = ticksExisted * 0.2;
        for (int i = 0; i < 4; i++) {
            double a = ringAngle + i * Math.PI / 2;
            double rx = posX + Math.cos(a) * scale * 1.5;
            double rz = posZ + Math.sin(a) * scale * 1.5;
            world.spawnParticle(EnumParticleTypes.SPELL_MOB,
                    rx, posY, rz, 0.1, 0, 0.1);
        }
    }

    /**
     * 爆炸粒子效果
     */
    private void spawnExplosionParticles() {
        for (int i = 0; i < 100; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double pitch = (rand.nextDouble() - 0.5) * Math.PI;
            double speed = 0.5 + rand.nextDouble() * 1.5;

            double vx = Math.cos(angle) * Math.cos(pitch) * speed;
            double vy = Math.sin(pitch) * speed;
            double vz = Math.sin(angle) * Math.cos(pitch) * speed;

            world.spawnParticle(EnumParticleTypes.PORTAL,
                    posX, posY, posZ, vx, vy, vz);
            world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                    posX, posY, posZ, vx * 0.5, vy * 0.5, vz * 0.5);
        }
    }

    /**
     * 更新缩放
     */
    private void updateScale() {
        float targetScale;
        int phase = getPhase();

        if (phase == PHASE_RISING) {
            // 上升时逐渐变大
            targetScale = 0.5f + (float) ticksExisted / RISE_DURATION * 1.0f;
        } else if (phase == PHASE_STABLE) {
            // 稳定时脉动
            float pulse = (float) Math.sin(ticksExisted * 0.2) * 0.2f;
            targetScale = 1.5f + pulse;
        } else {
            // 爆炸时急剧膨胀
            targetScale = 3.0f;
        }

        setScale(targetScale);
    }

    /**
     * 缓动函数
     */
    private float easeOutQuad(float t) {
        return 1 - (1 - t) * (1 - t);
    }

    // ========== Getters / Setters ==========

    public float getScale() {
        return this.dataManager.get(SCALE);
    }

    public void setScale(float scale) {
        this.dataManager.set(SCALE, scale);
    }

    public int getPhase() {
        return this.dataManager.get(PHASE);
    }

    public void setPhase(int phase) {
        this.dataManager.set(PHASE, phase);
    }

    public float getStoredDamage() {
        return this.dataManager.get(STORED_DAMAGE);
    }

    public void setStoredDamage(float damage) {
        this.dataManager.set(STORED_DAMAGE, damage);
    }

    public int getSpatialCount() {
        return spatialCount;
    }

    // ========== NBT ==========

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasUniqueId("CasterUUID")) {
            this.casterUUID = compound.getUniqueId("CasterUUID");
        }
        this.spatialCount = compound.getInteger("SpatialCount");
        this.startY = compound.getDouble("StartY");
        setStoredDamage(compound.getFloat("StoredDamage"));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        if (casterUUID != null) {
            compound.setUniqueId("CasterUUID", casterUUID);
        }
        compound.setInteger("SpatialCount", spatialCount);
        compound.setDouble("StartY", startY);
        compound.setFloat("StoredDamage", getStoredDamage());
    }

    // ========== 其他 ==========

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 4096.0D;
    }
}
