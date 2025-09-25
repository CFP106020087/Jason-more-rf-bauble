package com.moremod.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.List;

import static com.moremod.moremod.MODID;

/**
 * 哭泣天使实体 - 反转方案
 * 默认模型：遮脸状态
 * 动画控制：放下手的动作
 */
public class EntityWeepingAngel extends EntityMob implements IAnimatable {

    private AnimationFactory factory = new AnimationFactory(this);

    // 数据参数
    private static final DataParameter<Boolean> PLAYER_WATCHING = EntityDataManager.createKey(EntityWeepingAngel.class, DataSerializers.BOOLEAN);

    // 属性
    private int teleportCooldown = 0;
    private BlockPos lastPos = null;
    private int stuckTimer = 0;

    // 配置
    private static final double DETECTION_RANGE = 64.0D;
    private static final double ATTACK_RANGE = 2.0D;
    private static final int TELEPORT_COOLDOWN_TICKS = 20;
    @Override
    protected ResourceLocation getLootTable() {return new ResourceLocation(MODID, "entities/weeping_angel");
    }
    public EntityWeepingAngel(World worldIn) {
        super(worldIn);
        this.setSize(0.6F, 2.0F);
        this.experienceValue = 50;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(PLAYER_WATCHING, Boolean.valueOf(false));
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new AIWeepingAngelAttack(this));
        this.tasks.addTask(2, new AIWeepingAngelTeleport(this));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));

        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.45D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(10.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(8.0D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
    }

    @Override
    public void onLivingUpdate() {
        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        }

        this.updateWatchedState();

        // 被玩家看着时石化
        if (this.isPlayerWatching()) {
            this.motionX = 0;
            this.motionY = 0;
            this.motionZ = 0;
            this.rotationYaw = this.prevRotationYaw;
            this.rotationPitch = this.prevRotationPitch;

            if (lastPos != null && lastPos.equals(this.getPosition())) {
                stuckTimer++;
                if (stuckTimer > 200) {
                    this.attemptTeleportBehindTarget();
                    stuckTimer = 0;
                }
            } else {
                lastPos = this.getPosition();
                stuckTimer = 0;
            }
        } else {
            stuckTimer = 0;
            super.onLivingUpdate();
        }
    }

    private void updateWatchedState() {
        boolean wasWatched = this.isPlayerWatching();
        boolean isWatched = false;

        List<EntityPlayer> players = this.world.getEntitiesWithinAABB(EntityPlayer.class,
                this.getEntityBoundingBox().grow(DETECTION_RANGE));

        for (EntityPlayer player : players) {
            if (this.canPlayerSee(player)) {
                isWatched = true;
                break;
            }
        }

        if (isWatched != wasWatched) {
            this.setPlayerWatching(isWatched);

            // 从被观察到不被观察，可能传送
            if (!isWatched && wasWatched) {
                if (this.rand.nextFloat() < 0.5F) {
                    this.attemptTeleportBehindTarget();
                }
            }
        }
    }

    private boolean canPlayerSee(EntityPlayer player) {
        double distance = player.getDistance(this);
        if (distance > DETECTION_RANGE) {
            return false;
        }

        if (player.isPlayerSleeping()) {
            return false;
        }

        if (player.isPotionActive(Potion.getPotionFromResourceLocation("minecraft:blindness"))) {
            return false;
        }

        Vec3d lookVec = player.getLook(1.0F);
        Vec3d playerPos = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3d angelPos = new Vec3d(this.posX, this.posY + this.height / 2.0F, this.posZ);
        Vec3d toAngel = angelPos.subtract(playerPos).normalize();

        double dotProduct = lookVec.dotProduct(toAngel);
        double viewThreshold = 0.98D - (distance / DETECTION_RANGE) * 0.2D;

        if (dotProduct > viewThreshold) {
            return this.world.rayTraceBlocks(playerPos, angelPos, false, true, false) == null;
        }

        return false;
    }

    private boolean attemptTeleportBehindTarget() {
        if (this.teleportCooldown > 0) {
            return false;
        }

        EntityLivingBase target = this.getAttackTarget();
        if (target == null) {
            return false;
        }

        double angle = Math.toRadians(target.rotationYaw + 180);
        double tx = target.posX - Math.sin(angle) * 3.0D;
        double ty = target.posY;
        double tz = target.posZ + Math.cos(angle) * 3.0D;

        if (this.attemptTeleport(tx, ty, tz)) {
            this.teleportCooldown = TELEPORT_COOLDOWN_TICKS;
            this.playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1.0F, 1.0F);
            return true;
        }

        return false;
    }

    // ========== GeckoLib动画 ==========

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        AnimationBuilder builder = new AnimationBuilder();

        if (!this.isPlayerWatching()) {
            // 没被看着时：播放放下手的动画
            builder.addAnimation("animation.angel.arms_down", true);
            event.getController().setAnimation(builder);
            return PlayState.CONTINUE;
        } else {
            // 被看着时：不播放动画，保持默认的遮脸模型
            return PlayState.STOP;
        }
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 3, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    // ========== Getter/Setter ==========

    public boolean isPlayerWatching() {
        return this.dataManager.get(PLAYER_WATCHING);
    }

    public void setPlayerWatching(boolean watching) {
        this.dataManager.set(PLAYER_WATCHING, watching);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.isPlayerWatching()) {
            amount *= 0.1F;
        }

        boolean result = super.attackEntityFrom(source, amount);

        if (result && !this.isPlayerWatching() && this.rand.nextFloat() < 0.3F) {
            this.attemptTeleportBehindTarget();
        }

        return result;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setBoolean("PlayerWatching", this.isPlayerWatching());
        compound.setInteger("TeleportCooldown", this.teleportCooldown);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setPlayerWatching(compound.getBoolean("PlayerWatching"));
        this.teleportCooldown = compound.getInteger("TeleportCooldown");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isPlayerWatching() ? null : SoundEvents.BLOCK_STONE_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.BLOCK_STONE_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_STONE_BREAK;
    }

    // ========== AI类 ==========

    static class AIWeepingAngelAttack extends EntityAIBase {
        private final EntityWeepingAngel angel;
        private int attackTick;

        public AIWeepingAngelAttack(EntityWeepingAngel angel) {
            this.angel = angel;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = this.angel.getAttackTarget();
            return target != null && target.isEntityAlive() && !this.angel.isPlayerWatching();
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = this.angel.getAttackTarget();
            if (target == null) return;

            double distance = this.angel.getDistanceSq(target);

            if (distance < ATTACK_RANGE * ATTACK_RANGE) {
                if (this.attackTick <= 0) {
                    this.angel.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
                    this.angel.attackEntityAsMob(target);

                    if (target instanceof EntityPlayer) {
                        EntityPlayer player = (EntityPlayer) target;
                        player.addPotionEffect(new PotionEffect(
                                Potion.getPotionFromResourceLocation("minecraft:slowness"),
                                100, 2));
                        player.addPotionEffect(new PotionEffect(
                                Potion.getPotionFromResourceLocation("minecraft:mining_fatigue"),
                                200, 1));
                    }

                    this.attackTick = 20;
                }
            } else {
                this.angel.getNavigator().tryMoveToEntityLiving(target, 1.0D);
            }

            this.angel.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

            if (this.attackTick > 0) {
                this.attackTick--;
            }
        }
    }

    static class AIWeepingAngelTeleport extends EntityAIBase {
        private final EntityWeepingAngel angel;
        private int teleportDelay;

        public AIWeepingAngelTeleport(EntityWeepingAngel angel) {
            this.angel = angel;
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = this.angel.getAttackTarget();
            if (target == null || this.angel.isPlayerWatching()) {
                return false;
            }

            double distance = this.angel.getDistanceSq(target);
            return distance > 256.0D || !this.angel.getEntitySenses().canSee(target);
        }

        @Override
        public void startExecuting() {
            this.teleportDelay = 0;
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = this.angel.getAttackTarget();
            if (target == null) return;

            if (this.teleportDelay++ >= 40) {
                this.angel.attemptTeleportBehindTarget();
                this.teleportDelay = 0;
            }
        }
    }
}