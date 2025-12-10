package com.moremod.entity.boss;

import com.moremod.compat.IceAndFireReflectionHelper;
import com.moremod.util.combat.TrueDamageHelper;
import com.moremod.entity.EntityCursedKnight;
import com.moremod.entity.fx.EntityLaserBeam;
import com.moremod.entity.fx.EntityLightningArc;
import com.moremod.entity.projectile.EntityVoidBullet;
import com.moremod.entity.fx.EntityRiftLightning;
import com.moremod.util.BossBlockTracker;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketAnimation;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.*;

public class EntityRiftwarden extends EntityMob implements IAnimatable {

    private static final String MODID = "moremod";
    private static final float  MAX_HP           = 4000F;

    // ========== 移动速度配置 ==========
    // 玩家走路: ~0.22 blocks/tick, 跑步: ~0.28 blocks/tick
    // 设置为比玩家跑步略快，玩家需要边打边跑
    private static final double MOVE_SPEED       = 0.2D;

    // ========== Gate限伤系统（仿CursedKnight） ==========
    private static final String CHUNK_ID = MODID + ".rift_chunk";
    public static final DamageSource RIFT_CHUNK = new DamageSource(CHUNK_ID)
            .setDamageIsAbsolute()
            .setDamageBypassesArmor();

    // Gate系统核心变量
    private int invulTicks = 0;
    private boolean pendingChunk = false;
    private boolean applyingChunk = false;
    private float frozenHealth = -1F;
    private float frozenAbsorb = -1F;
    private int gateOpenFxCooldown = 0;
    private static final int INVUL_TICKS_BASE = 25;  // 1.25秒基础无敌

    // 限伤参数
    private static final float MIN_DAMAGE_THRESHOLD = 8.0F;      // 低于此伤害直接取消
    private static final float MAX_SINGLE_HIT_FRACTION = 0.05F;  // 单次最多5%最大生命(200伤害)

    public static final DamageSource LASER_DAMAGE_SOURCE = new DamageSource(MODID + ".laser")
            .setDamageBypassesArmor()
            .setDamageIsAbsolute();

    // GeckoLib
    private AnimationFactory factory = new AnimationFactory(this);

    // DataManager参数
    private static final DataParameter<String> ANIMATION = EntityDataManager.createKey(EntityRiftwarden.class, DataSerializers.STRING);
    private static final DataParameter<Boolean> ATTACKING = EntityDataManager.createKey(EntityRiftwarden.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> BALL_ROTATION = EntityDataManager.createKey(EntityRiftwarden.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> IS_CHARGING = EntityDataManager.createKey(EntityRiftwarden.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> RANGE_ANIM_STATE = EntityDataManager.createKey(EntityRiftwarden.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> IS_EXHAUSTED = EntityDataManager.createKey(EntityRiftwarden.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> IS_KNEELING = EntityDataManager.createKey(EntityRiftwarden.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> IS_CHANNELING = EntityDataManager.createKey(EntityRiftwarden.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> USE_RIGHT_CHANNEL = EntityDataManager.createKey(EntityRiftwarden.class, DataSerializers.BOOLEAN);

    // 动画名称常量
    private static final String ANIM_IDLE = "animation.crack.idle";
    private static final String ANIM_IDLE_BALL = "animation.crack.idle_ball";
    private static final String ANIM_MOVE = "animation.crack.move";
    private static final String ANIM_RIGHT_ATTACK = "animation.crack.range_right_attack";
    private static final String ANIM_LEFT_ATTACK = "animation.crack.range_left_attack";
    private static final String ANIM_RANGE_RIGHT = "animation.crack.right_attack";
    private static final String ANIM_RANGE_LEFT = "animation.crack.left_attack";
    private static final String ANIM_KNEEL = "animation.crack.kneel";
    private static final String ANIM_CHANNEL_RIGHT = "animation.crack.channel_right";
    private static final String ANIM_CHANNEL_LEFT = "animation.crack.channel_left";

    // 远程攻击动画控制变量
    private int rangeAnimTicks = 0;
    private boolean useRightRange = false;
    private static final int RANGE_ANIM_DURATION = 100;

    // 球体旋转动画
    private float ballRotation = 0.0F;
    private float ballOrbitRadius = 3.0F;

    // Boss信息条
    private final BossInfoServer bossBar =
            new BossInfoServer(this.getDisplayName(), BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS);
    private int phase = 0;

    // 召唤系统
    private int   minionCap        = 3;
    private int   summonCooldown   = 200;
    private int   summonPeriodTicks= 400;
    private boolean waveP1Fired = false;
    private boolean waveP2Fired = false;
    private boolean waveP3Fired = false;

    // 能力冷却 - 大幅降低以提升攻击欲望
    private int blockBreakCooldown = 0;
    private int antiFlyingCooldown = 0;
    private int bulletBarrageCooldown = 0;
    private int lightningCooldown = 0;
    private int lightningArcCooldown = 0;
    private int laserCooldown = 0;
    private EntityPlayer laserTarget = null;
    private int laserWarningTime = 0;
    private int laserChargeTime  = 0;
    private int laserFiringTime = 0;

    // ========== 第一阶段新增攻击冷却 ==========
    private int chainLightningCooldown = 0;
    private int spiralBulletCooldown = 0;
    private int burstBulletCooldown = 0;
    private float spiralAngle = 0F;

    // 闪电弧系统
    private List<EntityLightningArc> activeArcs = new ArrayList<>();
    private static final int ARC_DURATION = 40;
    private static final float ARC_DAMAGE = 10F;

    // 蓄力发射子弹机制
    private int  chargeShootTimer    = 0;
    private int  chargeShootCooldown = 160;  // 降低冷却
    private boolean isChargingShooting = false;
    private int  chargeShootDuration = 60;

    // 攻击动画
    private static final byte MSG_ATTACK_START = 10;
    private int attackAnimationTimer = 0;
    private static final int ATTACK_ANIMATION_LENGTH = 32;
    private int comboCounter = 0;
    private int slamCooldown = 0;
    private boolean useLeftAttack = false;
    private int currentAttackTicks = 0;

    // 远程攻击动画时长
    private static final int RANGE_ATTACK_ANIM_LENGTH = 34;

    // 激光虚弱状态
    private int laserExhaustionTime = 0;
    private static final int LASER_EXHAUSTION_DURATION = 400;
    private boolean isKneeling = false;

    // ========== 传送追击系统（增强版） ==========
    private int teleportCooldown = 0;
    private static final int TELEPORT_COOLDOWN_TIME = 40;           // 降低冷却（原60）
    private static final double TELEPORT_TRIGGER_DISTANCE = 256.0D; // 降低触发距离（原400）
    private static final double TELEPORT_CHASE_DISTANCE = 144.0D;   // 追击传送距离（12格）
    private int chaseTeleportCooldown = 0;                          // 追击传送独立冷却

    // 追踪被禁飞的玩家
    private final Set<Integer> disabledFlyingPlayers = new HashSet<>();

    // 常量
    private static final int   LASER_WARNING_TIME = 100;
    private static final int   LASER_CHARGE_TIME  = 60;
    private static final int   LASER_FIRING_TIME  = 60;
    private static final float LASER_DAMAGE_PER_TICK = 1.5F;
    private static final float BULLET_DAMAGE      = 8F;
    private static final float LIGHTNING_BASE_DAMAGE = 15F;
    private static final float CHAIN_LIGHTNING_DAMAGE = 8F;

    public EntityRiftwarden(World worldIn) {
        super(worldIn);
        this.setSize(0.9F, 2.8F);
        this.experienceValue = 500;
        this.isImmuneToFire = true;
        this.hurtResistantTime = 10;
        this.maxHurtResistantTime = 10;

        // ========== 提升上阶梯能力 ==========
        this.stepHeight = 1.5F;  // 可以直接走上1.5格高的台阶

        bossBar.setCreateFog(false);
        bossBar.setDarkenSky(false);
        bossBar.setPlayEndBossMusic(false);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(ANIMATION, "idle");
        this.dataManager.register(ATTACKING, false);
        this.dataManager.register(BALL_ROTATION, 0.0F);
        this.dataManager.register(IS_CHARGING, false);
        this.dataManager.register(RANGE_ANIM_STATE, 0);
        this.dataManager.register(IS_EXHAUSTED, false);
        this.dataManager.register(IS_KNEELING, false);
        this.dataManager.register(IS_CHANNELING, false);
        this.dataManager.register(USE_RIGHT_CHANNEL, false);
    }

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation(MODID, "entities/rift_warden");
    }

    // ========== AI配置 - 使用标准AI系统 ==========
    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));

        // 近战AI - 适中速度倍率
        this.tasks.addTask(1, new EntityAIAttackMelee(this, 1.2D, true) {  // 速度倍率1.2
            @Override
            public boolean shouldExecute() {
                if (EntityRiftwarden.this.shouldStopMovement()) return false;
                return super.shouldExecute();
            }
            @Override
            public boolean shouldContinueExecuting() {
                if (EntityRiftwarden.this.shouldStopMovement()) return false;
                return super.shouldContinueExecuting();
            }
            @Override
            public void updateTask() {
                EntityLivingBase target = EntityRiftwarden.this.getAttackTarget();
                if (target != null && !EntityRiftwarden.this.shouldStopMovement()) {
                    EntityRiftwarden.this.faceEntity(target, 30.0F, 30.0F);
                }
                super.updateTask();
            }
            @Override
            protected double getAttackReachSqr(EntityLivingBase attackTarget) {
                // 增大攻击范围判定
                return (double)(this.attacker.width * 2.5F * this.attacker.width * 2.5F + attackTarget.width);
            }
        });

        this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 1.0D) {
            @Override
            public boolean shouldExecute() {
                return !EntityRiftwarden.this.shouldStopMovement() && super.shouldExecute();
            }
        });
        this.tasks.addTask(7, new EntityAIWanderAvoidWater(this, 0.8D) {
            @Override
            public boolean shouldExecute() {
                return !EntityRiftwarden.this.shouldStopMovement() && super.shouldExecute();
            }
        });
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 16.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));

        // 目标选择
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, 0, true, false, null));
    }

    /**
     * 是否需要停止移动
     */
    public boolean shouldStopMovement() {
        return isChargingShooting && chargeShootTimer < chargeShootDuration
                || laserWarningTime > 0
                || laserChargeTime > 0
                || laserFiringTime > 0
                || laserExhaustionTime > 0
                || this.dataManager.get(IS_KNEELING);
    }

    /**
     * 统一的"忙碌"状态检查
     */
    public boolean isBusy() {
        return isChargingShooting
                || laserWarningTime > 0
                || laserChargeTime > 0
                || laserFiringTime > 0
                || currentAttackTicks > 0
                || laserExhaustionTime > 0
                || this.dataManager.get(IS_KNEELING);
    }

    private boolean isPerformingRangeAttack() {
        return laserFiringTime > 0 || rangeAnimTicks > 0 || this.dataManager.get(RANGE_ANIM_STATE) > 0;
    }

    private boolean isPerformingRangeSkill() {
        return isChargingShooting ||
                laserWarningTime > 0 ||
                laserChargeTime > 0 ||
                laserFiringTime > 0 ||
                rangeAnimTicks > 0 ||
                this.dataManager.get(RANGE_ANIM_STATE) > 0;
    }

    // ========== 增强传送系统 ==========
    private void handleTeleportation(EntityPlayer target) {
        if (target == null || world.isRemote || laserExhaustionTime > 0) return;

        if (teleportCooldown > 0) teleportCooldown--;
        if (chaseTeleportCooldown > 0) chaseTeleportCooldown--;

        double distanceSq = this.getDistanceSq(target);

        // 1. 远距离传送（玩家太远）
        if (!isPerformingRangeSkill() && teleportCooldown <= 0 &&
                distanceSq > TELEPORT_TRIGGER_DISTANCE && distanceSq < 2500) {
            performTeleport(target, false);
            return;
        }

        // 2. 追击传送（玩家逃跑中，且无法追上）
        if (chaseTeleportCooldown <= 0 && distanceSq > TELEPORT_CHASE_DISTANCE) {
            // 检查是否长时间追不上
            if (!this.getNavigator().noPath() && this.ticksExisted % 60 == 0) {
                // 每3秒检查一次，如果玩家持续远离则传送
                performTeleport(target, true);
            }
        }
    }

    private void performTeleport(EntityPlayer target, boolean isChase) {
        if (world.isRemote) return;

        spawnTeleportEffect(this.posX, this.posY, this.posZ, true);
        playGlobal(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1.5F, 0.5F);

        double angle = this.rand.nextDouble() * Math.PI * 2;
        double distance = isChase ? (2 + this.rand.nextDouble() * 2) : (3 + this.rand.nextDouble() * 2);
        double newX = target.posX + Math.cos(angle) * distance;
        double newZ = target.posZ + Math.sin(angle) * distance;
        double newY = target.posY;

        BlockPos targetPos = new BlockPos(newX, newY, newZ);
        while (world.isAirBlock(targetPos) && targetPos.getY() > 0) {
            targetPos = targetPos.down();
        }
        targetPos = targetPos.up();

        if (!world.isAirBlock(targetPos) || !world.isAirBlock(targetPos.up())) {
            newX = target.posX;
            newY = target.posY + 2;
            newZ = target.posZ;
        } else {
            newY = targetPos.getY();
        }

        this.setPositionAndUpdate(newX, newY, newZ);

        spawnTeleportEffect(newX, newY, newZ, false);
        playGlobal(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1.5F, 1.5F);

        this.faceEntity(target, 360.0F, 360.0F);
        this.hurtResistantTime = 20;

        if (isChase) {
            chaseTeleportCooldown = 80 - phase * 10;  // 追击传送独立冷却
        } else {
            teleportCooldown = TELEPORT_COOLDOWN_TIME - phase * 8;
        }

        // 传送后立即发射子弹
        if (phase >= 1) {
            int bulletCount = 4 + phase * 2;
            for (int i = 0; i < bulletCount; i++) {
                double shootAngle = (Math.PI * 2 * i) / bulletCount;
                shootRadialBullet(shootAngle);
            }
        }

        if (target instanceof EntityPlayerMP) {
            String msg = isChase ? "§5虚空守望者追踪你的气息！" : "§5虚空守望者撕裂空间出现在你身边！";
            target.sendMessage(new net.minecraft.util.text.TextComponentString(msg));
        }
    }

    private void spawnTeleportEffect(double x, double y, double z, boolean isStart) {
        if (!PARTICLES_ENABLED) return;
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 精简：只生成少量标志性粒子
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 * i) / 8;
            double px = x + Math.cos(angle) * 1.5;
            double pz = z + Math.sin(angle) * 1.5;
            ws.spawnParticle(EnumParticleTypes.PORTAL, px, y + this.height * 0.5, pz, 2, 0.1, 0.3, 0.1, 0.0);
        }
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(MAX_HP);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(MOVE_SPEED);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(14.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(12.0D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0D);  // 大追踪范围
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        this.setHealth(this.getMaxHealth());

        // 确保寻路器设置正确
        if (this.getNavigator() instanceof PathNavigateGround) {
            PathNavigateGround nav = (PathNavigateGround) this.getNavigator();
            nav.setCanSwim(true);
            nav.setBreakDoors(true);
        }
    }

    // GeckoLib 动画相关
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationEvent event) {
        // 优先级0：虚弱状态动画
        if (this.dataManager.get(IS_KNEELING) || this.dataManager.get(IS_EXHAUSTED)) {
            event.getController().setAnimationSpeed(1.0);
            event.getController().setAnimation(new AnimationBuilder().addAnimation(ANIM_KNEEL, true));
            return PlayState.CONTINUE;
        }

        // 优先级1：引导动画
        if (this.dataManager.get(IS_CHANNELING)) {
            event.getController().setAnimationSpeed(1.0);
            if (this.dataManager.get(USE_RIGHT_CHANNEL)) {
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation(ANIM_RANGE_RIGHT, false)
                        .addAnimation(ANIM_CHANNEL_RIGHT, true));
            } else {
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation(ANIM_RANGE_LEFT, false)
                        .addAnimation(ANIM_CHANNEL_LEFT, true));
            }
            return PlayState.CONTINUE;
        }

        // 优先级2：蓄力射击动画
        boolean isCharging = this.dataManager.get(IS_CHARGING);
        if (isCharging) {
            event.getController().setAnimationSpeed(1.0);
            if (this.dataManager.get(USE_RIGHT_CHANNEL)) {
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation(ANIM_RANGE_RIGHT, false)
                        .addAnimation(ANIM_CHANNEL_RIGHT, true));
            } else {
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation(ANIM_RANGE_LEFT, false)
                        .addAnimation(ANIM_CHANNEL_LEFT, true));
            }
            return PlayState.CONTINUE;
        }

        int rangeAnimState = this.dataManager.get(RANGE_ANIM_STATE);

        // 优先级3：远程攻击动画
        if (rangeAnimState > 0 || rangeAnimTicks > 0) {
            event.getController().setAnimationSpeed(1.0);
            if (rangeAnimState == 1) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation(ANIM_RANGE_RIGHT, false));
            } else if (rangeAnimState == 2) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation(ANIM_RANGE_LEFT, false));
            } else if (useRightRange) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation(ANIM_RANGE_RIGHT, false));
            } else {
                event.getController().setAnimation(new AnimationBuilder().addAnimation(ANIM_RANGE_LEFT, false));
            }
            return PlayState.CONTINUE;
        }

        // 优先级4：近战攻击动画
        if (this.dataManager.get(ATTACKING) || currentAttackTicks > 0) {
            event.getController().setAnimationSpeed(1.0);
            if (useLeftAttack) {
                event.getController().setAnimation(new AnimationBuilder().addAnimation(ANIM_LEFT_ATTACK, false));
            } else {
                event.getController().setAnimation(new AnimationBuilder().addAnimation(ANIM_RIGHT_ATTACK, false));
            }
            return PlayState.CONTINUE;
        }

        // 优先级5：移动或待机
        // 使用 limbSwingAmount 检测移动
        float limbSwing = this.limbSwingAmount;
        boolean isActuallyMoving = limbSwing > 0.01F || event.isMoving();

        if (isActuallyMoving) {
            // ========== 固定动画速度 ==========
            // 动画周期 1.0833秒（约22tick），设计移速约 0.048 blocks/tick
            // MOVE_SPEED=0.32, AI倍率1.2 → 实际约 0.32×1.2 ≈ 0.38 blocks/tick
            // 速度比: 0.38 / 0.048 ≈ 8x
            //
            // 但8x太快会看不清动画，使用固定值让动画看起来自然
            // 实测：2.5-3.0x 看起来比较舒服
            event.getController().setAnimationSpeed(2.5);
            event.getController().setAnimation(new AnimationBuilder().addAnimation(ANIM_MOVE, true));
        } else {
            event.getController().setAnimationSpeed(1.0);
            event.getController().setAnimation(new AnimationBuilder().addAnimation(ANIM_IDLE_BALL, true));
        }

        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    private boolean isPlayerOnRightSide(EntityPlayer player) {
        double facingX = -Math.sin(Math.toRadians(this.rotationYaw));
        double facingZ = Math.cos(Math.toRadians(this.rotationYaw));

        double toPlayerX = player.posX - this.posX;
        double toPlayerZ = player.posZ - this.posZ;

        double crossProduct = facingX * toPlayerZ - facingZ * toPlayerX;
        return crossProduct > 0;
    }

    private Vec3d getHandPosition(boolean rightHand) {
        double shoulderHeight = this.height * 0.8;
        double armLength = 1.5;
        double armAngle = Math.toRadians(130);

        double handY = this.posY + shoulderHeight + Math.sin(armAngle) * armLength;
        double sideOffset = rightHand ? 0.5 : -0.5;

        double yawRad = Math.toRadians(this.rotationYaw);
        double handX = this.posX - Math.sin(yawRad + Math.PI/2) * sideOffset;
        double handZ = this.posZ + Math.cos(yawRad + Math.PI/2) * sideOffset;

        return new Vec3d(handX, handY, handZ);
    }

    public void setAnimation(String animation) {
        this.dataManager.set(ANIMATION, animation);
    }

    public String getAnimation() {
        return this.dataManager.get(ANIMATION);
    }

    @Override
    public boolean attackEntityAsMob(Entity entityIn) {
        if (shouldStopMovement()) return false;

        this.attackAnimationTimer = ATTACK_ANIMATION_LENGTH;
        this.currentAttackTicks = ATTACK_ANIMATION_LENGTH + 5;
        this.useLeftAttack = this.rand.nextBoolean();
        this.dataManager.set(ATTACKING, true);

        if (!this.world.isRemote) {
            ((WorldServer)this.world).getEntityTracker().sendToTracking(this,
                    new SPacketAnimation(this, 4));
            this.world.setEntityState(this, MSG_ATTACK_START);
        }

        boolean flag = super.attackEntityAsMob(entityIn);
        if (flag && entityIn instanceof EntityPlayer) {
            double knockbackX = (entityIn.posX - this.posX) * 0.5D;
            double knockbackZ = (entityIn.posZ - this.posZ) * 0.5D;
            double knockbackY = 0.4D;

            entityIn.addVelocity(knockbackX, knockbackY, knockbackZ);

            if (PARTICLES_ENABLED && world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                        entityIn.posX, entityIn.posY + entityIn.height * 0.5, entityIn.posZ,
                        5, 0.2, 0.2, 0.2, 0.05);
            }

            this.playSound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 1.0F, 0.8F);
            this.playSound(SoundEvents.ENTITY_PLAYER_HURT_ON_FIRE, 0.5F, 0.5F);

            if (phase >= 3) {
                performMeleeCombo(entityIn);
            }
        }

        if (phase >= 2 && slamCooldown <= 0) {
            performGroundSlam();
        }

        return flag;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void handleStatusUpdate(byte id) {
        if (id == MSG_ATTACK_START) {
            this.attackAnimationTimer = ATTACK_ANIMATION_LENGTH;
            this.currentAttackTicks = ATTACK_ANIMATION_LENGTH + 5;
            return;
        }
        super.handleStatusUpdate(id);
    }

    private void performMeleeCombo(Entity target) {
        if (this.rand.nextFloat() < 0.3F) {
            comboCounter = 3;

            for (int i = 0; i < comboCounter; i++) {
                final int strike = i;
                if (this.world instanceof WorldServer) {
                    ((WorldServer)world).addScheduledTask(() -> {
                        if (target.isEntityAlive() && this.getDistanceSq(target) < 9.0) {
                            this.attackAnimationTimer = ATTACK_ANIMATION_LENGTH / 2;
                            if (!this.world.isRemote) {
                                this.world.setEntityState(this, MSG_ATTACK_START);
                            }

                            target.attackEntityFrom(DamageSource.causeMobDamage(this),
                                    (float)this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue() * 0.7F);

                            double kbX = (target.posX - this.posX) * (0.3 + strike * 0.1);
                            double kbZ = (target.posZ - this.posZ) * (0.3 + strike * 0.1);
                            target.addVelocity(kbX, 0.2, kbZ);

                            this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.8F + strike * 0.1F);

                            if (PARTICLES_ENABLED && world instanceof WorldServer) {
                                WorldServer ws = (WorldServer) world;
                                ws.spawnParticle(EnumParticleTypes.SWEEP_ATTACK,
                                        target.posX, target.posY + target.height * 0.5, target.posZ,
                                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                            }
                        }
                    });
                }
            }
        }
    }

    private void performGroundSlam() {
        if (this.onGround && this.rand.nextFloat() < 0.2F) {
            List<EntityPlayer> nearbyPlayers = this.world.getEntitiesWithinAABB(
                    EntityPlayer.class,
                    new AxisAlignedBB(this.posX - 5, this.posY - 2, this.posZ - 5,
                            this.posX + 5, this.posY + 2, this.posZ + 5)
            );

            if (!nearbyPlayers.isEmpty()) {
                this.motionY = 0.6;
                this.attackAnimationTimer = ATTACK_ANIMATION_LENGTH * 2;
                if (!this.world.isRemote) {
                    this.world.setEntityState(this, MSG_ATTACK_START);
                }
                slamCooldown = 100;

                if (this.world instanceof WorldServer) {
                    ((WorldServer)world).addScheduledTask(() -> {
                        if (this.onGround) {
                            for (EntityPlayer player : nearbyPlayers) {
                                if (player.getDistanceSq(this) < 25) {
                                    player.attackEntityFrom(DamageSource.causeMobDamage(this), 15.0F);

                                    double dx = player.posX - this.posX;
                                    double dz = player.posZ - this.posZ;
                                    double dist = Math.sqrt(dx * dx + dz * dz);

                                    if (dist > 0) {
                                        player.addVelocity(
                                                (dx / dist) * 1.5,
                                                0.8,
                                                (dz / dist) * 1.5
                                        );
                                    }
                                }
                            }

                            this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5F, 0.5F);

                            // 精简：只生成一圈爆炸粒子
                            if (PARTICLES_ENABLED && world instanceof WorldServer) {
                                WorldServer ws = (WorldServer) world;
                                for (int i = 0; i < 8; i++) {
                                    double angle = (Math.PI * 2 * i) / 8;
                                    double px = this.posX + Math.cos(angle) * 2.5;
                                    double pz = this.posZ + Math.sin(angle) * 2.5;
                                    ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                                            px, this.posY, pz,
                                            1, 0, 0.2, 0, 0.05);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        this.bossBar.addPlayer(player);
        BossBlockTracker.startTracking(this);
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void setCustomNameTag(String name) {
        super.setCustomNameTag(name);
        this.bossBar.setName(this.getDisplayName());
    }

    @Override
    public void setDead() {
        super.setDead();
        BossBlockTracker.stopTracking(this);
        restoreAllPlayersFlight();

        for (EntityLightningArc arc : activeArcs) {
            if (arc != null && arc.isEntityAlive()) {
                arc.setDead();
            }
        }
        activeArcs.clear();
    }

    private void restoreAllPlayersFlight() {
        if (!world.isRemote) {
            List<EntityPlayer> players = this.world.getEntitiesWithinAABB(
                    EntityPlayer.class,
                    new AxisAlignedBB(this.posX - 100, this.posY - 100, this.posZ - 100,
                            this.posX + 100, this.posY + 100, this.posZ + 100)
            );
            for (EntityPlayer player : players) {
                if (disabledFlyingPlayers.contains(player.getEntityId())) {
                    if (player instanceof EntityPlayerMP) {
                        player.sendMessage(new net.minecraft.util.text.TextComponentString("§a虚空守望者已被击败，飞行能力已恢复"));
                        ((EntityPlayerMP) player).sendPlayerAbilities();
                    }
                }
            }
            disabledFlyingPlayers.clear();
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();

        // ========== 调试日志（测试后删除）==========
        if (!this.world.isRemote && this.ticksExisted % 20 == 0) {
            EntityPlayer p = this.world.getNearestAttackablePlayer(this, 50, 50);
            EntityLivingBase target = this.getAttackTarget();
            String stopReason = "";
            if (shouldStopMovement()) {
                if (isChargingShooting) stopReason += "charging,";
                if (laserWarningTime > 0) stopReason += "laserWarn,";
                if (laserChargeTime > 0) stopReason += "laserCharge,";
                if (laserFiringTime > 0) stopReason += "laserFire,";
                if (laserExhaustionTime > 0) stopReason += "exhausted,";
                if (this.dataManager.get(IS_KNEELING)) stopReason += "kneeling,";
            }
            System.out.println("[Riftwarden] " +
                    "target=" + (target != null ? "YES" : "NO") +
                    ", player=" + (p != null ? String.format("%.1f格", Math.sqrt(getDistanceSq(p))) : "NULL") +
                    ", shouldStop=" + shouldStopMovement() + (stopReason.isEmpty() ? "" : "(" + stopReason + ")") +
                    ", limbSwing=" + String.format("%.2f", limbSwingAmount) +
                    ", speed=" + String.format("%.3f", Math.sqrt(motionX*motionX + motionZ*motionZ)));
        }

        if (!this.world.isRemote) {
            // 药水免疫已通过 isPotionApplicable() 实现，无需每tick清除

            // ========== Gate系统处理 ==========
            handleGateInvulnerability();

            // 处理虚弱状态
            updateExhaustionState();

            // 虚弱期间禁止其他行为
            if (laserExhaustionTime > 0) {
                this.motionX = 0;
                this.motionZ = 0;
                return;
            }
        }

        // ========== 简化的移动控制 - 只在特定状态停止移动 ==========
        if (!this.world.isRemote) {
            if (shouldStopMovement()) {
                // 清除路径并停止移动
                this.getNavigator().clearPath();
                this.motionX = 0;
                this.motionZ = 0;

                // 面向玩家
                EntityPlayer nearestPlayer = this.world.getNearestAttackablePlayer(this, 50, 50);
                if (nearestPlayer != null) {
                    float turnSpeed = (laserFiringTime > 0) ? 5.0F : 10.0F;
                    this.faceEntity(nearestPlayer, turnSpeed, turnSpeed);
                }
            } else {
                // 让AI系统处理移动，只做辅助
                EntityPlayer nearestPlayer = this.world.getNearestAttackablePlayer(this, 50, 50);
                if (nearestPlayer != null) {
                    // 确保有攻击目标
                    if (this.getAttackTarget() == null) {
                        this.setAttackTarget(nearestPlayer);
                    }
                    // 处理传送
                    handleTeleportation(nearestPlayer);
                }
            }
        }

        // 球体旋转动画
        ballRotation += 2.0F;
        if (ballRotation >= 360.0F) {
            ballRotation -= 360.0F;
        }
        this.dataManager.set(BALL_ROTATION, ballRotation);

        if (phase >= 2) {
            ballOrbitRadius = 3.5F + phase * 0.5F;
        }

        // 攻击动画计时器
        if (currentAttackTicks > 0) {
            currentAttackTicks--;
            if (currentAttackTicks == 0) {
                this.dataManager.set(ATTACKING, false);
            }
        }

        // 远程动画计时器
        if (rangeAnimTicks > 0) {
            rangeAnimTicks--;
            if (rangeAnimTicks <= 5) {
                this.dataManager.set(RANGE_ANIM_STATE, 0);
            }
        }

        if (gateOpenFxCooldown > 0) gateOpenFxCooldown--;
        if (slamCooldown > 0) slamCooldown--;

        if (this.attackAnimationTimer > 0) {
            this.attackAnimationTimer--;

            if (!world.isRemote && this.attackAnimationTimer > ATTACK_ANIMATION_LENGTH / 2) {
                if (PARTICLES_ENABLED && world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;
                    double armOffset = this.attackAnimationTimer % 2 == 0 ? 1.0 : -1.0;
                    ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                            this.posX + armOffset,
                            this.posY + this.height * 0.7,
                            this.posZ,
                            3, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }

        if (!this.world.isRemote) {
            updateCooldowns();
            handleSummons();
            continuousBlockBreak();
            disablePlayerFlight();
            handleChargeShooting();
            updateLightningArcs();

            EntityPlayer target = this.world.getNearestAttackablePlayer(this, 50, 50);
            if (target != null && !this.isGateInvulnerable()) {
                executeAbilities(target);
            }

            updateLaserCharge();
        }

        if (isChargingShooting && this.ticksExisted % 2 == 0) {
            spawnChargeParticles();
        }

        updateBossBar();
    }

    // ========== Gate限伤系统核心方法（仿CursedKnight） ==========

    private void handleGateInvulnerability() {
        if (this.invulTicks > 0) {
            this.invulTicks--;

            // 冻结血量 - 但真伤上下文中跳过（允许破碎之神等真伤生效）
            if (!this.applyingChunk && !TrueDamageHelper.isInTrueDamageContext()) {
                if (this.frozenHealth >= 0F && this.getHealth() != this.frozenHealth) {
                    this.setHealth(this.frozenHealth);
                }
                if (this.frozenAbsorb >= 0F && this.getAbsorptionAmount() != this.frozenAbsorb) {
                    this.setAbsorptionAmount(this.frozenAbsorb);
                }
            }

            // 无敌粒子效果 - 精简
            if (this.ticksExisted % 10 == 0 && world instanceof WorldServer) {
                spawnRing(EnumParticleTypes.PORTAL, 8, 1.5D);
            }

            // 无敌结束
            if (this.invulTicks == 0) {
                if (this.pendingChunk && this.getHealth() > 0F) {
                    float dmg = getChunkSize();
                    this.applyingChunk = true;
                    try {
                        super.attackEntityFrom(RIFT_CHUNK, dmg);
                        playSound(SoundEvents.BLOCK_ANVIL_LAND, 0.8F, 0.8F);
                        spawnRing(EnumParticleTypes.CRIT_MAGIC, 48, 2.5D);
                    } finally {
                        this.applyingChunk = false;
                    }
                }
                this.pendingChunk = false;
                this.frozenHealth = -1F;
                this.frozenAbsorb = -1F;
            }
        }
    }

    /**
     * 获取单次伤害上限（最大生命值的1/20）
     */
    public float getChunkSize() {
        return this.getMaxHealth() / 20.0F;  // 4000/20 = 200
    }

    /**
     * 是否处于Gate无敌状态
     */
    public boolean isGateInvulnerable() {
        return invulTicks > 0;
    }

    /**
     * 开启Gate并冻结血量
     */
    public void openGateAndFreeze(boolean scheduleChunk) {
        int base = INVUL_TICKS_BASE - phase * 4;  // 阶段越高无敌时间越短
        this.invulTicks = Math.max(this.invulTicks, Math.max(15, base));

        this.frozenHealth = this.getHealth();
        this.frozenAbsorb = this.getAbsorptionAmount();
        this.pendingChunk = scheduleChunk;

        if (gateOpenFxCooldown <= 0) {
            playSound(SoundEvents.ITEM_SHIELD_BLOCK, 0.8F, 1.0F);
            spawnRing(EnumParticleTypes.SPELL_WITCH, 32, 2.0D);
            gateOpenFxCooldown = 5;
        }
    }

    /**
     * 是否是可信的Chunk伤害源
     */
    public boolean isTrustedChunkSource(DamageSource src) {
        return src != null && CHUNK_ID.equals(src.getDamageType());
    }

    // 虚弱状态处理
    private void enterExhaustionState() {
        this.laserExhaustionTime = LASER_EXHAUSTION_DURATION;
        this.isKneeling = true;
        this.dataManager.set(IS_KNEELING, true);
        this.dataManager.set(IS_EXHAUSTED, true);

        this.motionX = 0;
        this.motionZ = 0;
        this.setNoAI(true);

        // 虚弱期间关闭Gate保护
        this.invulTicks = 0;
        this.frozenHealth = -1F;
        this.frozenAbsorb = -1F;
        this.hurtResistantTime = 0;
        this.maxHurtResistantTime = 0;

        playGlobal(SoundEvents.ENTITY_VILLAGER_HURT, 1.0F, 0.5F);
        playGlobal(SoundEvents.ENTITY_PLAYER_BREATH, 1.5F, 0.8F);

        List<EntityPlayer> nearbyPlayers = this.world.getEntitiesWithinAABB(
                EntityPlayer.class,
                new AxisAlignedBB(this.posX - 50, this.posY - 50, this.posZ - 50,
                        this.posX + 50, this.posY + 50, this.posZ + 50)
        );

        for (EntityPlayer player : nearbyPlayers) {
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).sendStatusMessage(
                        new net.minecraft.util.text.TextComponentString("§6§l虚空守望者陷入虚弱！"), false);
                ((EntityPlayerMP) player).sendStatusMessage(
                        new net.minecraft.util.text.TextComponentString("§e抓住机会进行输出！"), true);

                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "§6§l[系统] §e虚空守望者因过度使用激光而陷入虚弱状态！持续" +
                                (LASER_EXHAUSTION_DURATION/20) + "秒！"));
            }
        }

        if (PARTICLES_ENABLED && world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            // 只生成少量烟雾表示虚弱
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    this.posX, this.posY + this.height * 0.5, this.posZ,
                    8, 0.5, 0.3, 0.5, 0.02);
        }
    }

    private void updateExhaustionState() {
        if (laserExhaustionTime > 0) {
            laserExhaustionTime--;

            // 虚弱期间确保无Gate保护
            this.invulTicks = 0;
            this.hurtResistantTime = 0;
            this.maxHurtResistantTime = 0;

            // 精简粒子：每40tick一次，只生成少量
            if (PARTICLES_ENABLED && laserExhaustionTime % 40 == 0 && world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.CLOUD,
                        this.posX, this.posY + this.height * 0.3, this.posZ,
                        2, 0.2, 0.1, 0.2, 0.01);
            }

            if (laserExhaustionTime % 20 == 0) {
                playGlobal(SoundEvents.ENTITY_PLAYER_BREATH, 0.8F, 0.7F);
            }

            if (laserExhaustionTime <= 60 && laserExhaustionTime % 20 == 0) {
                int secondsLeft = laserExhaustionTime / 20;
                if (secondsLeft > 0) {
                    List<EntityPlayer> players = this.world.getEntitiesWithinAABB(
                            EntityPlayer.class,
                            new AxisAlignedBB(this.posX - 30, this.posY - 30, this.posZ - 30,
                                    this.posX + 30, this.posY + 30, this.posZ + 30)
                    );
                    for (EntityPlayer player : players) {
                        if (player instanceof EntityPlayerMP) {
                            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                    "§c虚弱状态还剩 " + secondsLeft + " 秒！"));
                        }
                    }
                }
            }

            if (laserExhaustionTime == 0) {
                exitExhaustionState();
            }
        }
    }

    private void exitExhaustionState() {
        this.isKneeling = false;
        this.dataManager.set(IS_KNEELING, false);
        this.dataManager.set(IS_EXHAUSTED, false);
        this.setNoAI(false);

        // 恢复正常无敌帧
        this.hurtResistantTime = 10;
        this.maxHurtResistantTime = 10;

        playGlobal(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 1.5F, 0.8F);
        playGlobal(SoundEvents.ENTITY_WITHER_SPAWN, 1.0F, 1.5F);

        List<EntityPlayer> nearbyPlayers = this.world.getEntitiesWithinAABB(
                EntityPlayer.class,
                new AxisAlignedBB(this.posX - 50, this.posY - 50, this.posZ - 50,
                        this.posX + 50, this.posY + 50, this.posZ + 50)
        );

        for (EntityPlayer player : nearbyPlayers) {
            if (player instanceof EntityPlayerMP) {
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "§4§l虚空守望者恢复了力量！"));
            }
        }

        // 精简：只生成一个爆炸效果
        if (PARTICLES_ENABLED && world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                    this.posX, this.posY + this.height * 0.5, this.posZ,
                    2, 0, 0, 0.0, 0);
        }

        performRecoveryAttack();
    }

    private void performRecoveryAttack() {
        int bulletCount = 24 + phase * 4;
        for (int i = 0; i < bulletCount; i++) {
            double angle = (Math.PI * 2 * i) / bulletCount;
            shootRadialBullet(angle);
        }

        if (phase >= 2) {
            List<EntityPlayer> targets = this.world.getEntitiesWithinAABB(
                    EntityPlayer.class,
                    new AxisAlignedBB(this.posX - 20, this.posY - 20, this.posZ - 20,
                            this.posX + 20, this.posY + 20, this.posZ + 20)
            );

            for (EntityPlayer target : targets) {
                if (target.isEntityAlive()) {
                    EntityRiftLightning bolt = new EntityRiftLightning(world, this, target);
                    bolt.setDamage(LIGHTNING_BASE_DAMAGE * 0.5F);
                    world.spawnEntity(bolt);
                }
            }
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        // 虚弱状态允许所有攻击，无Gate保护
        if (laserExhaustionTime > 0) {
            this.hurtResistantTime = 0;

            if (world instanceof WorldServer && source.getTrueSource() instanceof EntityPlayer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                        this.posX, this.posY + this.height * 0.5, this.posZ,
                        20, 0.5, 0.5, 0.5, 0.1);
                playGlobal(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.5F, 1.0F);
            }
            return super.attackEntityFrom(source, amount);
        }

        // Gate无敌期间阻挡非信任伤害
        if (this.isGateInvulnerable() && !isTrustedChunkSource(source)) {
            if (world instanceof WorldServer && gateOpenFxCooldown <= 0) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        posX, posY + height * 0.5, posZ,
                        10, 0.5, 0.5, 0.5, 0.1);
                playSound(SoundEvents.ITEM_SHIELD_BLOCK, 1.0F, 1.5F);
                gateOpenFxCooldown = 3;
            }
            return false;
        }

        return super.attackEntityFrom(source, amount);
    }

    /**
     * 核心伤害处理 - Gate限伤系统
     */
    @Override
    protected void damageEntity(DamageSource damageSrc, float damageAmount) {
        // 信任伤害源直接通过
        if (isTrustedChunkSource(damageSrc)) {
            super.damageEntity(damageSrc, damageAmount);
            return;
        }

        // 虚弱期间无Gate保护，双倍伤害
        if (laserExhaustionTime > 0) {
            super.damageEntity(damageSrc, damageAmount * 2.0F);
            return;
        }

        // Gate无敌期间不处理
        if (this.isGateInvulnerable()) {
            return;
        }

        // ========== 限伤核心逻辑 ==========

        // 1. 低于门槛的伤害直接取消
        if (damageAmount < MIN_DAMAGE_THRESHOLD) {
            if (world instanceof WorldServer && gateOpenFxCooldown <= 0) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.BARRIER,
                        posX, posY + height * 0.5, posZ,
                        3, 0.3, 0.3, 0.3, 0.0);
                playSound(SoundEvents.ITEM_SHIELD_BLOCK, 0.5F, 1.8F);
                gateOpenFxCooldown = 3;
            }

            // 给玩家反馈
            if (damageSrc.getTrueSource() instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) damageSrc.getTrueSource();
                if (player instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) player).sendStatusMessage(
                            new net.minecraft.util.text.TextComponentString(
                                    "§7伤害不足 §c" + String.format("%.1f", damageAmount) +
                                            " §7< §e" + String.format("%.1f", MIN_DAMAGE_THRESHOLD) + " §7(已取消)"),
                            true);
                }
            }
            return;  // 伤害被取消
        }

        // 2. 计算实际伤害（上限为最大生命值的1/20）
        float maxDamage = getChunkSize();  // 200
        float actualDamage = Math.min(damageAmount, maxDamage);

        // 3. 判断是否需要累积大额伤害
        boolean scheduleChunk = damageAmount >= maxDamage;

        // 4. 开启Gate并冻结血量
        openGateAndFreeze(scheduleChunk);

        // 5. 立即应用限制后的伤害
        if (!this.applyingChunk) {
            this.applyingChunk = true;
            try {
                super.damageEntity(damageSrc, actualDamage);
            } finally {
                this.applyingChunk = false;
            }
        }

        // 6. 给玩家反馈
        if (damageSrc.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) damageSrc.getTrueSource();
            float healthPercent = (this.getHealth() / this.getMaxHealth()) * 100;

            if (player instanceof EntityPlayerMP) {
                String msg;
                if (damageAmount > maxDamage) {
                    msg = String.format("§c虚空守望者 §7| §e%.0f%% §7(伤害被限制: %.0f→%.0f)",
                            healthPercent, damageAmount, actualDamage);
                } else {
                    msg = String.format("§c虚空守望者 §7| §e%.0f%% §7(伤害: %.0f)",
                            healthPercent, actualDamage);
                }
                ((EntityPlayerMP) player).sendStatusMessage(new net.minecraft.util.text.TextComponentString(msg), true);

                if (scheduleChunk) {
                    player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            "§6§l强力一击！将在无敌结束后造成额外伤害！"));
                }
            }
        }

        // 7. 特效 - 精简
        if (PARTICLES_ENABLED && world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                    posX, posY + height * 0.5, posZ,
                    4, 0.3, 0.3, 0.3, 0.02);
        }
    }

    private void updateLightningArcs() {
        Iterator<EntityLightningArc> iterator = activeArcs.iterator();
        while (iterator.hasNext()) {
            EntityLightningArc arc = iterator.next();
            if (arc == null || !arc.isEntityAlive() || arc.getTicksLeft() <= 0) {
                if (arc != null && arc.isEntityAlive()) {
                    arc.setDead();
                }
                iterator.remove();
            } else {
                Entity target = arc.getTo();
                if (target instanceof EntityLivingBase && target.isEntityAlive()) {
                    if (arc.ticksExisted % 5 == 0) {
                        target.attackEntityFrom(DamageSource.causeMobDamage(this).setDamageBypassesArmor(),
                                ARC_DAMAGE / 4);
                        // 移除粒子效果，闪电实体本身已有视觉效果
                    }
                }
            }
        }
    }

    private void updateBossBar() {
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

        int newPhase = computePhase();
        if (newPhase != phase) {
            phase = newPhase;
            onPhaseChanged(phase);
        }
    }

    private int computePhase() {
        float pct = this.getHealth() / this.getMaxHealth();
        if (pct > 0.75F) {
            this.bossBar.setColor(BossInfo.Color.PURPLE);
            return 0;
        }
        if (pct > 0.50F) {
            this.bossBar.setColor(BossInfo.Color.BLUE);
            return 1;
        }
        if (pct > 0.25F) {
            this.bossBar.setColor(BossInfo.Color.YELLOW);
            return 2;
        }
        this.bossBar.setColor(BossInfo.Color.RED);
        return 3;
    }

    private void onPhaseChanged(int phase) {
        playGlobal(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 1.0F, 0.95F + 0.05F * phase);
        spawnRing(EnumParticleTypes.SPELL_WITCH, 96, 3.0 + phase);

        // 阶段变化时提升攻击能力
        switch (phase) {
            case 0:
                this.summonPeriodTicks = 400;
                this.minionCap = 3;
                break;
            case 1:
                this.summonPeriodTicks = 320;  // 更频繁
                this.minionCap = 4;
                this.stepHeight = 1.8F;  // 提升爬楼能力
                break;
            case 2:
                this.summonPeriodTicks = 260;
                this.minionCap = 5;
                this.stepHeight = 2.0F;
                break;
            case 3:
                this.summonPeriodTicks = 200;
                this.minionCap = 6;
                this.stepHeight = 2.5F;  // 狂暴阶段可以跳上更高台阶
                break;
        }

        if (phase == 1 && !waveP1Fired) { waveP1Fired = true; trySummonWave(1, 6, 10); }
        if (phase == 2 && !waveP2Fired) { waveP2Fired = true; trySummonWave(2, 6, 12); }
        if (phase == 3 && !waveP3Fired) { waveP3Fired = true; trySummonWave(2, 6, 14); }
    }

    private void handleChargeShooting() {
        chargeShootCooldown--;

        if (chargeShootCooldown <= 0 && !isChargingShooting && !isPerformingRangeAttack() && laserExhaustionTime <= 0) {
            EntityPlayer target = this.world.getNearestAttackablePlayer(this, 40, 40);
            if (target != null) {
                boolean playerOnRight = isPlayerOnRightSide(target);
                this.useRightRange = playerOnRight;

                this.rangeAnimTicks = RANGE_ATTACK_ANIM_LENGTH + chargeShootDuration + 20;

                isChargingShooting = true;
                chargeShootTimer = chargeShootDuration + 20;

                this.dataManager.set(IS_CHARGING, true);
                this.dataManager.set(USE_RIGHT_CHANNEL, useRightRange);
                this.dataManager.set(RANGE_ANIM_STATE, 0);

                this.faceEntity(target, 30.0F, 30.0F);

                playGlobal(SoundEvents.ENTITY_WITHER_AMBIENT, 1.5F, 0.5F);
            }
            chargeShootCooldown = 160 - phase * 25;  // 降低冷却
        }

        if (isChargingShooting) {
            chargeShootTimer--;

            if (chargeShootTimer <= chargeShootDuration && chargeShootTimer % 10 == 0) {
                spawnRing(EnumParticleTypes.SMOKE_LARGE, 32, 2.0D + (chargeShootDuration - chargeShootTimer) * 0.05);
            }

            if (chargeShootTimer <= 0) {
                performChargedBarrage();
                isChargingShooting = false;
                this.dataManager.set(IS_CHARGING, false);
            }
        }
    }

    private void performChargedBarrage() {
        List<EntityPlayer> targets = this.world.getEntitiesWithinAABB(
                EntityPlayer.class,
                new AxisAlignedBB(this.posX - 40, this.posY - 40, this.posZ - 40,
                        this.posX + 40, this.posY + 40, this.posZ + 40)
        );
        if (targets.isEmpty()) return;

        int bulletCount = 16 + phase * 4;
        for (int i = 0; i < bulletCount; i++) {
            double angle = (Math.PI * 2 * i) / bulletCount;
            shootRadialBullet(angle);
        }

        for (EntityPlayer target : targets) {
            for (int i = 0; i < 2 + phase; i++) {
                shootVoidBullet(target, (i - 1) * 0.1);
            }
        }

        playGlobal(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 0.7F);
        spawnRing(EnumParticleTypes.EXPLOSION_LARGE, 8, 3.0D);
    }

    private void shootRadialBullet(double angle) {
        EntityVoidBullet bullet = new EntityVoidBullet(this.world, this);

        double dx = Math.cos(angle);
        double dz = Math.sin(angle);

        bullet.setDamage(BULLET_DAMAGE * 0.75F);
        bullet.setPosition(this.posX + dx * 2,
                this.posY + this.height * 0.5,
                this.posZ + dz * 2);
        bullet.shoot(dx, 0.1, dz, 1.2F, 0);

        this.world.spawnEntity(bullet);
    }

    private void spawnChargeParticles() {
        if (PARTICLES_ENABLED && world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            Vec3d handPos = getHandPosition(this.useRightRange);
            double radius = 3.0 + (chargeShootDuration - chargeShootTimer) * 0.05;
            for (int i = 0; i < 3; i++) {
                double angle = this.rand.nextDouble() * Math.PI * 2;
                double x = handPos.x + Math.cos(angle) * radius;
                double z = handPos.z + Math.sin(angle) * radius;
                double y = handPos.y + this.rand.nextDouble();
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z, 1, 0, 0.1, 0, 0.0);
            }
        }
    }

    private void updateCooldowns() {
        if (blockBreakCooldown > 0) blockBreakCooldown--;
        if (antiFlyingCooldown > 0) antiFlyingCooldown--;
        if (bulletBarrageCooldown > 0) bulletBarrageCooldown--;
        if (lightningCooldown > 0) lightningCooldown--;
        if (lightningArcCooldown > 0) lightningArcCooldown--;
        if (laserCooldown > 0) laserCooldown--;
        if (teleportCooldown > 0) teleportCooldown--;
        if (chaseTeleportCooldown > 0) chaseTeleportCooldown--;
        if (chainLightningCooldown > 0) chainLightningCooldown--;
        if (spiralBulletCooldown > 0) spiralBulletCooldown--;
        if (burstBulletCooldown > 0) burstBulletCooldown--;
    }

    private void handleSummons() {
        summonCooldown--;
        if (summonCooldown <= 0) {
            int alive = countKnightsNearby(48);
            if (alive < minionCap) {
                int need = Math.min(2, minionCap - alive);
                trySummonWave(need, 6, 12);
            }
            summonCooldown = summonPeriodTicks;
        }
    }

    // ========== 执行能力（增强攻击欲望） ==========
    private void executeAbilities(EntityPlayer target) {
        if (laserExhaustionTime > 0) return;

        double distSq = this.getDistanceSq(target);

        // 所有阶段都有的基础攻击 - 更激进
        tryShootBullets(target);

        // 第一阶段攻击
        if (phase == 0) {
            tryChainLightning(target);
            trySpiralBullets(target);
            tryBurstBullets(target);
        }

        // 第二阶段及以后 - 远程技能更激进
        if (phase >= 1) {
            tryLightningStrike(target);
            if (phase >= 2) {
                tryLightningArc(target);
            }
        }

        // 激光 - 稍微降低触发门槛
        if (phase >= 2 && distSq > 36 && laserWarningTime <= 0 && laserChargeTime <= 0 && laserFiringTime <= 0) {
            tryLaserBeam(target);
        }
    }

    // ========== 第一阶段：链式闪电 ==========
    private void tryChainLightning(EntityPlayer target) {
        if (chainLightningCooldown > 0 || laserExhaustionTime > 0) return;
        if (this.getDistanceSq(target) > 400) return;

        if (this.rand.nextFloat() > 0.35F) return;  // 提高触发概率

        try {
            float[] damagePerHop = new float[]{
                    CHAIN_LIGHTNING_DAMAGE,
                    CHAIN_LIGHTNING_DAMAGE * 0.75F,
                    CHAIN_LIGHTNING_DAMAGE * 0.5F,
                    CHAIN_LIGHTNING_DAMAGE * 0.25F
            };

            boolean success = IceAndFireReflectionHelper.createChainLightningFromTarget(
                    this.world,
                    target,
                    this,
                    damagePerHop,
                    8,
                    false
            );
            if (!success) {
                fallbackChainLightning(target);
                chainLightningCooldown = 60;
                return;
            }

            playGlobal(SoundEvents.ENTITY_LIGHTNING_THUNDER, 0.6F, 1.8F);
            // 移除手部粒子，闪电实体本身已有效果
        } catch (Exception e) {
            fallbackChainLightning(target);
        }

        chainLightningCooldown = 60;  // 降低冷却
    }

    private void fallbackChainLightning(EntityPlayer target) {
        target.attackEntityFrom(DamageSource.causeMobDamage(this).setDamageBypassesArmor(),
                CHAIN_LIGHTNING_DAMAGE);

        List<EntityPlayer> nearbyPlayers = world.getEntitiesWithinAABB(
                EntityPlayer.class,
                target.getEntityBoundingBox().grow(8.0),
                p -> p != target && p.isEntityAlive()
        );

        int chainCount = 0;
        EntityLivingBase lastTarget = target;

        for (EntityPlayer chainTarget : nearbyPlayers) {
            if (chainCount >= 3) break;

            chainTarget.attackEntityFrom(
                    DamageSource.causeMobDamage(this).setDamageBypassesArmor(),
                    CHAIN_LIGHTNING_DAMAGE * (1.0F - chainCount * 0.2F)
            );

            spawnLightningLine(lastTarget, chainTarget);
            lastTarget = chainTarget;
            chainCount++;
        }

        playGlobal(SoundEvents.ENTITY_LIGHTNING_THUNDER, 0.6F, 1.8F);
    }

    private void spawnLightningLine(EntityLivingBase from, EntityLivingBase to) {
        if (!PARTICLES_ENABLED || !(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        double dx = to.posX - from.posX;
        double dy = (to.posY + to.height * 0.5) - (from.posY + from.height * 0.5);
        double dz = to.posZ - from.posZ;

        // 减少粒子数量：10->4
        int particles = 4;
        for (int i = 0; i <= particles; i++) {
            double p = (double) i / particles;
            double px = from.posX + dx * p;
            double py = from.posY + from.height * 0.5 + dy * p;
            double pz = from.posZ + dz * p;

            ws.spawnParticle(EnumParticleTypes.SPELL_INSTANT, px, py, pz, 1, 0, 0, 0.0, 0);
        }
    }

    // ========== 螺旋子弹 ==========
    private void trySpiralBullets(EntityPlayer target) {
        if (spiralBulletCooldown > 0 || laserExhaustionTime > 0) return;
        if (this.getDistanceSq(target) > 625) return;

        int bulletCount = 6;
        for (int i = 0; i < bulletCount; i++) {
            double angle = spiralAngle + (Math.PI * 2 * i) / bulletCount;

            EntityVoidBullet bullet = new EntityVoidBullet(this.world, this);
            bullet.setDamage(BULLET_DAMAGE * 0.6F);

            double dx = Math.cos(angle);
            double dz = Math.sin(angle);

            bullet.setPosition(
                    this.posX + dx * 1.5,
                    this.posY + this.height * 0.6,
                    this.posZ + dz * 1.5
            );
            bullet.shoot(dx, 0.05, dz, 0.8F, 0);

            this.world.spawnEntity(bullet);
        }

        spiralAngle += Math.PI / 6;
        if (spiralAngle >= Math.PI * 2) {
            spiralAngle -= Math.PI * 2;
        }

        playGlobal(SoundEvents.ENTITY_BLAZE_SHOOT, 0.8F, 1.2F);
        spiralBulletCooldown = 20;  // 降低冷却
    }

    // ========== 爆发子弹 ==========
    private void tryBurstBullets(EntityPlayer target) {
        if (burstBulletCooldown > 0 || laserExhaustionTime > 0) return;
        if (this.getDistanceSq(target) > 400) return;

        if (this.rand.nextFloat() > 0.30F) return;  // 提高概率

        int bulletCount = 5;
        double baseAngle = Math.atan2(target.posZ - this.posZ, target.posX - this.posX);
        double spreadAngle = Math.PI / 8;

        for (int i = 0; i < bulletCount; i++) {
            double angle = baseAngle - spreadAngle / 2 + (spreadAngle * i) / (bulletCount - 1);

            EntityVoidBullet bullet = new EntityVoidBullet(this.world, this);
            bullet.setDamage(BULLET_DAMAGE * 0.5F);

            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            double dy = (target.posY + target.getEyeHeight() - this.posY - this.height * 0.6) /
                    this.getDistance(target) * 0.5;

            bullet.setPosition(
                    this.posX + dx * 2,
                    this.posY + this.height * 0.6,
                    this.posZ + dz * 2
            );
            bullet.shoot(dx, dy, dz, 1.8F, 1.0F);

            this.world.spawnEntity(bullet);
        }

        playGlobal(SoundEvents.ENTITY_BLAZE_SHOOT, 1.0F, 0.7F);

        if (PARTICLES_ENABLED && world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    this.posX, this.posY + this.height * 0.6, this.posZ,
                    15, 0.3, 0.3, 0.3, 0.05);
        }

        burstBulletCooldown = 45;  // 降低冷却
    }

    private void continuousBlockBreak() {
        if (this.ticksExisted % 10 == 0) {
            BlockPos center = this.getPosition();

            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 2; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos pos = center.add(x, y, z);
                        if (BossBlockTracker.isBossPlayerBlock(this, pos)) {
                            IBlockState state = world.getBlockState(pos);
                            world.destroyBlock(pos, false);
                            BossBlockTracker.removeBlock(this, pos);

                            if (PARTICLES_ENABLED && world instanceof WorldServer) {
                                WorldServer ws = (WorldServer) world;
                                ws.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                        10, 0.25, 0.25, 0.25, 0.05,
                                        Block.getStateId(state)
                                );
                            }
                        }
                    }
                }
            }

            if (blockBreakCooldown <= 0) {
                areaPlayerBlockDestruction();
                blockBreakCooldown = 30;
            }
        }
    }

    private void areaPlayerBlockDestruction() {
        List<BlockPos> playerBlocks = BossBlockTracker.getBossPlayerBlocks(this, 3);
        if (playerBlocks.isEmpty()) return;

        int destroyed = 0;
        for (BlockPos pos : playerBlocks) {
            if (this.rand.nextFloat() < 0.5F) {
                IBlockState state = world.getBlockState(pos);
                if (!world.isAirBlock(pos)) {
                    world.destroyBlock(pos, true);
                    BossBlockTracker.removeBlock(this, pos);
                    destroyed++;

                    if (PARTICLES_ENABLED && world instanceof WorldServer) {
                        WorldServer ws = (WorldServer) world;
                        ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                5, 0.25, 0.25, 0.25, 0.05
                        );
                    }
                }
            }
        }

        if (destroyed > 0) {
            playGlobal(SoundEvents.ENTITY_GENERIC_EXPLODE, 0.5F, 1.2F);
        }
    }

    private void disablePlayerFlight() {
        if (antiFlyingCooldown > 0) {
            antiFlyingCooldown--;
            return;
        }

        List<EntityPlayer> players = this.world.getEntitiesWithinAABB(
                EntityPlayer.class,
                new AxisAlignedBB(this.posX - 50, this.posY - 50, this.posZ - 50,
                        this.posX + 50, this.posY + 50, this.posZ + 50)
        );

        for (EntityPlayer player : players) {
            if (player.capabilities.isFlying) {
                player.capabilities.isFlying = false;
                disabledFlyingPlayers.add(player.getEntityId());

                if (player instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) player).sendPlayerAbilities();
                    player.sendMessage(new net.minecraft.util.text.TextComponentString("§c虚空守望者的力量禁锢了你的飞行能力！"));
                }

                if (PARTICLES_ENABLED && world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;
                    ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                            player.posX, player.posY, player.posZ,
                            30, 0.5, 1.0, 0.5, 0.1
                    );
                }
                player.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 1.0F, 1.0F);
            }
        }
        antiFlyingCooldown = 10;
    }

    private void tryShootBullets(EntityPlayer target) {
        if (bulletBarrageCooldown <= 0 && this.getDistanceSq(target) < 400) {
            int bulletCount = 3 + phase;
            double spread = Math.PI / 6;

            for (int i = 0; i < bulletCount; i++) {
                double angle = -spread / 2 + (spread * i / Math.max(1, bulletCount - 1));
                shootVoidBullet(target, angle);
            }
            playGlobal(SoundEvents.ENTITY_BLAZE_SHOOT, 1.0F, 0.5F);
            bulletBarrageCooldown = 45 - phase * 8;  // 降低冷却
        }
    }

    private void shootVoidBullet(EntityPlayer target, double angleOffset) {
        EntityVoidBullet bullet = new EntityVoidBullet(this.world, this);

        double dx = target.posX - this.posX;
        double dy = target.posY + target.getEyeHeight() - this.posY - this.height * 0.5;
        double dz = target.posZ - this.posZ;

        double cos = Math.cos(angleOffset);
        double sin = Math.sin(angleOffset);
        double newDx = dx * cos - dz * sin;
        double newDz = dx * sin + dz * cos;

        bullet.setDamage(BULLET_DAMAGE);

        double spawnDistance = 2.0;
        double length = Math.sqrt(newDx * newDx + dy * dy + newDz * newDz);
        bullet.setPosition(
                this.posX + (newDx / length) * spawnDistance,
                this.posY + this.height * 0.7,
                this.posZ + (newDz / length) * spawnDistance
        );
        bullet.shoot(newDx, dy, newDz, 1.5F, 2.0F);

        // 精简射击粒子
        if (PARTICLES_ENABLED && world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    bullet.posX, bullet.posY, bullet.posZ,
                    3, 0.2, 0.2, 0.2, 0.05);
        }
        this.world.spawnEntity(bullet);
    }

    private void tryLightningStrike(EntityPlayer target) {
        if (lightningCooldown <= 0 && target != null && laserExhaustionTime <= 0
                && !isChargingShooting
                && laserWarningTime <= 0 && laserChargeTime <= 0 && laserFiringTime <= 0) {
            this.faceEntity(target, 30.0F, 30.0F);

            this.useRightRange = isPlayerOnRightSide(target);
            this.rangeAnimTicks = RANGE_ATTACK_ANIM_LENGTH + 40;

            this.dataManager.set(RANGE_ANIM_STATE, useRightRange ? 1 : 2);

            playGlobal(SoundEvents.ENTITY_WITHER_AMBIENT, 1.0F, 1.5F);

            // 延迟发射闪电
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int i = 0; i < 30; i++) {
                    final int delay = i;
                    ws.addScheduledTask(() -> {
                        if (delay == 29 && target.isEntityAlive()) {
                            launchLightningProjectiles(target);
                        }
                    });
                }
            }

            lightningCooldown = 80 - phase * 15;  // 降低冷却
        }
    }

    private void launchLightningProjectiles(EntityLivingBase target) {
        if (world.isRemote || target == null) return;

        int projectileCount = 3 + phase;
        Vec3d handPos = getHandPosition(this.useRightRange);

        playGlobal(SoundEvents.ENTITY_WITHER_AMBIENT, 1.5F, 2.0F);
        createLightningChargingEffect();

        for (int i = 0; i < projectileCount; i++) {
            EntityRiftLightning bolt = new EntityRiftLightning(world, this, target);

            if (i > 0) {
                double angleOffset = (i - projectileCount/2.0) * 0.2;
                bolt.motionX += Math.cos(angleOffset) * 0.3;
                bolt.motionZ += Math.sin(angleOffset) * 0.3;
            }

            bolt.setPosition(
                    handPos.x,
                    handPos.y + i * 0.2,
                    handPos.z
            );

            bolt.setDamage(LIGHTNING_BASE_DAMAGE + phase * 3);
            world.spawnEntity(bolt);
        }

        playGlobal(SoundEvents.ENTITY_LIGHTNING_THUNDER, 0.7F, 1.5F);
    }

    private void tryLightningArc(EntityPlayer target) {
        if (lightningArcCooldown <= 0 && target != null && this.getDistanceSq(target) < 900
                && laserExhaustionTime <= 0
                && !isChargingShooting
                && laserWarningTime <= 0 && laserChargeTime <= 0 && laserFiringTime <= 0) {
            this.faceEntity(target, 30.0F, 30.0F);

            boolean playerOnRight = isPlayerOnRightSide(target);
            this.useRightRange = !playerOnRight;

            this.rangeAnimTicks = RANGE_ATTACK_ANIM_LENGTH + ARC_DURATION + 30;

            this.dataManager.set(RANGE_ANIM_STATE, useRightRange ? 1 : 2);

            createLightningChargingEffect();
            playGlobal(SoundEvents.ENTITY_WITHER_SPAWN, 1.5F, 2.0F);

            // 延迟创建闪电弧
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int i = 0; i < 35; i++) {
                    final int delay = i;
                    ws.addScheduledTask(() -> {
                        if (delay == 34 && target.isEntityAlive()) {
                            createLightningArcs(target);
                        }
                    });
                }
            }

            lightningArcCooldown = 120 - phase * 20;  // 降低冷却
        }
    }

    private void createLightningArcs(EntityPlayer target) {
        if (world.isRemote || target == null) return;

        playGlobal(SoundEvents.ENTITY_WITHER_SPAWN, 1.5F, 2.0F);

        Vec3d handPos = getHandPosition(this.useRightRange);

        EntityLightningArc mainArc = new EntityLightningArc(world, this, target, ARC_DURATION);
        world.spawnEntity(mainArc);
        activeArcs.add(mainArc);

        if (phase >= 3) {
            List<EntityPlayer> nearbyPlayers = world.getEntitiesWithinAABB(
                    EntityPlayer.class,
                    new AxisAlignedBB(target.posX - 8, target.posY - 8, target.posZ - 8,
                            target.posX + 8, target.posY + 8, target.posZ + 8)
            );

            for (EntityPlayer nearbyPlayer : nearbyPlayers) {
                if (nearbyPlayer != target && nearbyPlayer.isEntityAlive() && activeArcs.size() < 3 + phase) {
                    EntityLightningArc chainArc = new EntityLightningArc(world, (EntityLivingBase)target,
                            (EntityLivingBase)nearbyPlayer, ARC_DURATION - 10);
                    world.spawnEntity(chainArc);
                    activeArcs.add(chainArc);
                }
            }
        }

        createLightningArcEffect(target, handPos);
        playGlobal(SoundEvents.ENTITY_LIGHTNING_IMPACT, 1.0F, 1.2F);
    }

    private void createLightningArcEffect(EntityPlayer target, Vec3d startPos) {
        if (!PARTICLES_ENABLED || !(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 精简：只在起点和终点生成少量粒子
        ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, startPos.x, startPos.y, startPos.z, 2, 0.1, 0.1, 0.1, 0.0);
        ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, target.posX, target.posY + target.height * 0.5, target.posZ, 2, 0.2, 0.2, 0.2, 0.02);
    }

    private void createLightningChargingEffect() {
        if (!PARTICLES_ENABLED || !(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        Vec3d handPos = getHandPosition(this.useRightRange);
        // 精简：只在手部位置生成少量粒子
        ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                handPos.x, handPos.y, handPos.z,
                3, 0.2, 0.2, 0.2, 0.02);
    }

    private void tryLaserBeam(EntityPlayer target) {
        if (laserCooldown <= 0 && this.getDistanceSq(target) < 625
                && !isChargingShooting
                && laserExhaustionTime <= 0) {
            this.faceEntity(target, 30.0F, 30.0F);

            boolean playerOnRight = isPlayerOnRightSide(target);
            this.useRightRange = playerOnRight;

            this.rangeAnimTicks = LASER_WARNING_TIME + LASER_CHARGE_TIME + LASER_FIRING_TIME + 20;

            this.dataManager.set(IS_CHANNELING, true);
            this.dataManager.set(USE_RIGHT_CHANNEL, useRightRange);
            this.dataManager.set(RANGE_ANIM_STATE, 0);

            this.laserTarget = target;
            this.laserWarningTime = LASER_WARNING_TIME;
            this.laserChargeTime  = 0;
            this.laserFiringTime = 0;

            if (target instanceof EntityPlayerMP) {
                ((EntityPlayerMP) target).sendStatusMessage(
                        new net.minecraft.util.text.TextComponentString("§4§l⚠ 警告 ⚠"), false);
                ((EntityPlayerMP) target).sendStatusMessage(
                        new net.minecraft.util.text.TextComponentString("§c致命镭射蓄力中"), true);
            }

            playGlobal(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 1.0F, 0.5F);
            this.laserCooldown = 200;  // 降低冷却
        }
    }

    private void fireLaser() {
        if (laserTarget == null || !laserTarget.isEntityAlive()) {
            laserTarget = null;
            return;
        }

        this.laserFiringTime = LASER_FIRING_TIME;

        Vec3d handPos = getHandPosition(this.useRightRange);

        if (!world.isRemote) {
            EntityLaserBeam beam = new EntityLaserBeam(
                    world, this, laserTarget,
                    LASER_FIRING_TIME,
                    0.8F,
                    0
            );
            beam.setStartPosition(handPos);
            world.spawnEntity(beam);
        }

        playGlobal(SoundEvents.ENTITY_LIGHTNING_THUNDER, 1.0F, 2.0F);
        playGlobal(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 0.5F, 0.5F);

        List<EntityPlayer> nearbyPlayers = this.world.getEntitiesWithinAABB(
                EntityPlayer.class,
                new AxisAlignedBB(this.posX - 30, this.posY - 30, this.posZ - 30,
                        this.posX + 30, this.posY + 30, this.posZ + 30)
        );
        for (EntityPlayer player : nearbyPlayers) {
            if (player instanceof EntityPlayerMP && player != laserTarget) {
                player.sendMessage(new net.minecraft.util.text.TextComponentString("§6§l虚空守望者正在释放激光，现在是攻击的好机会！"));
            }
        }
    }

    private void updateLaserCharge() {
        if (laserWarningTime > 0 && laserTarget != null) {
            laserWarningTime--;

            if (laserWarningTime % 3 == 0) {
                if (laserWarningTime % 10 == 0) {
                    playGlobal(SoundEvents.BLOCK_NOTE_PLING,
                            2.0F,
                            0.5F + (LASER_WARNING_TIME - laserWarningTime) * 0.03F);
                    playGlobal(SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.5F, 2.0F);
                }

                if (laserWarningTime % 20 == 0 && laserTarget instanceof EntityPlayerMP) {
                    int secondsLeft = laserWarningTime / 20;
                    laserTarget.sendMessage(new net.minecraft.util.text.TextComponentString(
                            "§c§l激光将在 " + secondsLeft + " 秒后发射！"));
                }
            }

            if (laserWarningTime == 0) {
                if (laserTarget == null || !laserTarget.isEntityAlive()) {
                    laserTarget = null;
                    return;
                }
                laserChargeTime = LASER_CHARGE_TIME;

                if (laserTarget instanceof EntityPlayerMP) {
                    laserTarget.sendMessage(new net.minecraft.util.text.TextComponentString("§4§l激光充能中！！！"));
                }

                playGlobal(SoundEvents.BLOCK_PORTAL_AMBIENT, 1.5F, 2.0F);
                playGlobal(SoundEvents.ENTITY_WITHER_SPAWN, 1.0F, 2.0F);

                this.faceEntity(laserTarget, 30.0F, 30.0F);
            }
            return;
        }

        if (laserChargeTime > 0 && laserTarget != null) {
            laserChargeTime--;

            if (laserChargeTime % 10 == 0) {
                float pitch = 2.0F + (LASER_CHARGE_TIME - laserChargeTime) * 0.02F;
                playGlobal(SoundEvents.ENTITY_WITHER_AMBIENT, 0.5F, pitch);
            }

            if (laserChargeTime == 0) {
                if (laserTarget != null && laserTarget.isEntityAlive() && this.canEntityBeSeen(laserTarget)) {
                    fireLaser();
                } else {
                    if (laserTarget instanceof EntityPlayerMP) {
                        ((EntityPlayerMP) laserTarget).sendMessage(
                                new net.minecraft.util.text.TextComponentString("§a成功躲避了激光攻击！"));
                    }
                    playGlobal(SoundEvents.BLOCK_FIRE_EXTINGUISH, 1.0F, 0.5F);
                    laserTarget = null;
                    this.dataManager.set(RANGE_ANIM_STATE, 0);
                    this.dataManager.set(IS_CHANNELING, false);
                }
            }
            return;
        }

        if (laserFiringTime > 0) {
            laserFiringTime--;

            if (laserTarget != null && laserTarget.isEntityAlive()) {
                if (this.canEntityBeSeen(laserTarget) && this.getDistanceSq(laserTarget) < 625) {
                    int originalHurtTime = laserTarget.hurtResistantTime;
                    laserTarget.hurtResistantTime = 0;

                    laserTarget.attackEntityFrom(LASER_DAMAGE_SOURCE, LASER_DAMAGE_PER_TICK);

                    laserTarget.hurtResistantTime = Math.min(originalHurtTime, 5);

                    if (PARTICLES_ENABLED && world instanceof WorldServer && laserFiringTime % 4 == 0) {
                        WorldServer ws = (WorldServer) world;

                        Vec3d handPos = getHandPosition(this.useRightRange);

                        double dx = laserTarget.posX - handPos.x;
                        double dy = laserTarget.posY + laserTarget.height * 0.5 - handPos.y;
                        double dz = laserTarget.posZ - handPos.z;
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                        // 精简：沿线只生成少量粒子
                        int particleCount = Math.max(3, (int) (distance * 0.5));
                        for (int i = 0; i < particleCount; i++) {
                            double p = (double) i / particleCount;
                            double px = handPos.x + dx * p;
                            double py = handPos.y + dy * p;
                            double pz = handPos.z + dz * p;

                            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                                    px, py, pz,
                                    1, 0.05, 0.0, 0.05, 0.0);
                        }
                    }

                    if (laserFiringTime % 5 == 0) {
                        laserTarget.playSound(SoundEvents.ENTITY_PLAYER_HURT_ON_FIRE, 0.5F, 1.5F);
                    }
                }
            } else {
                laserFiringTime = 0;
            }

            if (laserFiringTime % 10 == 0) {
                playGlobal(SoundEvents.ENTITY_BLAZE_BURN, 1.0F, 0.5F);
            }

            if (laserFiringTime == 0) {
                this.laserTarget = null;
                this.dataManager.set(RANGE_ANIM_STATE, 0);
                this.dataManager.set(IS_CHANNELING, false);

                enterExhaustionState();
            }
        }
    }

    // 粒子效果总开关 - 设为false完全关闭
    private static final boolean PARTICLES_ENABLED = false;  // 关闭所有粒子
    private static final float PARTICLE_REDUCTION = 0.25F;

    private void spawnRing(EnumParticleTypes type, int count, double radius) {
        if (!PARTICLES_ENABLED) return;
        if (this.world.isRemote || !(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) this.world;
        // 大幅减少粒子数量
        int actualCount = Math.max(4, (int)(count * PARTICLE_REDUCTION));
        double cx = this.posX;
        double cy = this.posY + this.height * 0.6D;
        double cz = this.posZ;
        for (int i = 0; i < actualCount; i++) {
            double ang = (Math.PI * 2 * i) / actualCount;
            double dx = cx + Math.cos(ang) * radius;
            double dz = cz + Math.sin(ang) * radius;
            ws.spawnParticle(type, dx, cy, dz, 1, 0, 0, 0, 0.0D);
        }
    }

    private void playGlobal(SoundEvent s, float vol, float pitch) {
        this.world.playSound(null, this.getPosition(), s, SoundCategory.HOSTILE, vol, pitch);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setInteger("Phase", phase);
        nbt.setInteger("SummonCD", summonCooldown);
        nbt.setInteger("SummonPeriod", summonPeriodTicks);
        nbt.setInteger("MinionCap", minionCap);
        nbt.setBoolean("WaveP1", waveP1Fired);
        nbt.setBoolean("WaveP2", waveP2Fired);
        nbt.setBoolean("WaveP3", waveP3Fired);
        nbt.setInteger("LaserCD", laserCooldown);
        nbt.setInteger("LaserCharge", laserChargeTime);
        nbt.setInteger("LaserWarning", laserWarningTime);
        nbt.setInteger("LaserFiring", laserFiringTime);
        nbt.setInteger("LightningArcCD", lightningArcCooldown);
        nbt.setBoolean("IsChargingShooting", isChargingShooting);
        nbt.setInteger("ChargeShootTimer", chargeShootTimer);
        nbt.setInteger("ChargeShootCooldown", chargeShootCooldown);
        nbt.setInteger("AttackAnimTimer", attackAnimationTimer);
        nbt.setInteger("SlamCD", slamCooldown);
        nbt.setInteger("RangeAnimTicks", rangeAnimTicks);
        nbt.setBoolean("UseRightRange", useRightRange);
        nbt.setInteger("TeleportCD", teleportCooldown);
        nbt.setInteger("ChaseTeleportCD", chaseTeleportCooldown);
        nbt.setInteger("LaserExhaustion", laserExhaustionTime);
        nbt.setBoolean("IsKneeling", isKneeling);
        nbt.setInteger("ChainLightningCD", chainLightningCooldown);
        nbt.setInteger("SpiralBulletCD", spiralBulletCooldown);
        nbt.setInteger("BurstBulletCD", burstBulletCooldown);
        nbt.setFloat("SpiralAngle", spiralAngle);
        // Gate系统
        nbt.setInteger("GateInvul", invulTicks);
        nbt.setBoolean("GatePendingChunk", pendingChunk);
        nbt.setFloat("GateFrozenHealth", frozenHealth);
        nbt.setFloat("GateFrozenAbsorb", frozenAbsorb);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.phase = nbt.getInteger("Phase");
        this.summonCooldown = nbt.getInteger("SummonCD");
        this.summonPeriodTicks = Math.max(120, nbt.getInteger("SummonPeriod"));
        this.minionCap = Math.max(2, nbt.getInteger("MinionCap"));
        this.waveP1Fired = nbt.getBoolean("WaveP1");
        this.waveP2Fired = nbt.getBoolean("WaveP2");
        this.waveP3Fired = nbt.getBoolean("WaveP3");
        this.laserCooldown = nbt.getInteger("LaserCD");
        this.laserChargeTime = nbt.getInteger("LaserCharge");
        this.laserWarningTime = nbt.getInteger("LaserWarning");
        this.laserFiringTime = nbt.getInteger("LaserFiring");
        this.lightningArcCooldown = nbt.getInteger("LightningArcCD");
        this.isChargingShooting = nbt.getBoolean("IsChargingShooting");
        this.chargeShootTimer = nbt.getInteger("ChargeShootTimer");
        this.chargeShootCooldown = nbt.getInteger("ChargeShootCooldown");
        this.attackAnimationTimer = nbt.getInteger("AttackAnimTimer");
        this.slamCooldown = nbt.getInteger("SlamCD");
        this.rangeAnimTicks = nbt.getInteger("RangeAnimTicks");
        this.useRightRange = nbt.getBoolean("UseRightRange");
        this.teleportCooldown = nbt.getInteger("TeleportCD");
        this.chaseTeleportCooldown = nbt.getInteger("ChaseTeleportCD");
        this.laserExhaustionTime = nbt.getInteger("LaserExhaustion");
        this.isKneeling = nbt.getBoolean("IsKneeling");
        this.dataManager.set(IS_KNEELING, this.isKneeling);
        this.chainLightningCooldown = nbt.getInteger("ChainLightningCD");
        this.spiralBulletCooldown = nbt.getInteger("SpiralBulletCD");
        this.burstBulletCooldown = nbt.getInteger("BurstBulletCD");
        this.spiralAngle = nbt.getFloat("SpiralAngle");
        // Gate系统
        this.invulTicks = nbt.getInteger("GateInvul");
        this.pendingChunk = nbt.getBoolean("GatePendingChunk");
        this.frozenHealth = nbt.getFloat("GateFrozenHealth");
        this.frozenAbsorb = nbt.getFloat("GateFrozenAbsorb");
    }

    @Override
    protected void dropFewItems(boolean wasRecentlyHit, int looting) {
        this.entityDropItem(new ItemStack(Items.NETHER_STAR), 0.0F);
        if (this.rand.nextFloat() < 0.15F + 0.05F * looting) {
            this.entityDropItem(new ItemStack(Items.ELYTRA), 0.0F);
        }
        int cores = 2 + this.rand.nextInt(2 + looting);
        this.entityDropItem(new ItemStack(Items.ENDER_PEARL, 6 + this.rand.nextInt(6 + looting * 2)), 0.0F);
        this.entityDropItem(new ItemStack(Items.DIAMOND, cores), 0.0F);
    }

    // Getter方法
    public int getLaserChargeTime() { return this.laserChargeTime; }
    public int getLaserFiringTime() { return this.laserFiringTime; }
    public EntityPlayer getLaserTarget() { return this.laserTarget; }
    public boolean isChargingLaser() { return this.laserChargeTime > 0 && this.laserTarget != null; }
    public boolean isFiringLaser() { return this.laserFiringTime > 0 && this.laserTarget != null; }
    public boolean isChargingShooting() { return this.isChargingShooting || this.dataManager.get(IS_CHARGING); }
    public int getChargeShootTimer() { return this.chargeShootTimer; }
    public int getPhase() { return this.phase; }
    public float getBallRotation() { return this.dataManager.get(BALL_ROTATION); }
    public float getBallOrbitRadius() { return this.ballOrbitRadius; }
    public boolean isMoving() {
        return Math.abs(this.motionX) > 0.05 || Math.abs(this.motionZ) > 0.05;
    }
    public int getRangeAnimTicks() { return this.rangeAnimTicks; }
    public boolean isUsingRightRange() { return this.useRightRange; }
    public boolean isExhausted() { return this.laserExhaustionTime > 0; }
    public int getExhaustionTimeLeft() { return this.laserExhaustionTime; }

    public float getAttackAnimationScale(float partialTicks) {
        if (this.attackAnimationTimer <= 0) return 0.0F;
        float animationProgress = (float)(this.attackAnimationTimer - partialTicks) / (float)ATTACK_ANIMATION_LENGTH;
        if (animationProgress > 0.5F) {
            return (animationProgress - 0.5F) * 2.0F;
        } else {
            return 1.0F - (animationProgress * 2.0F);
        }
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        return super.isEntityInvulnerable(source);
    }

    /**
     * 完全免疫药水效果 - 药水根本无法被附加
     */
    @Override
    public boolean isPotionApplicable(PotionEffect potioneffectIn) {
        return false;
    }

    private int countKnightsNearby(double r) {
        AxisAlignedBB box = new AxisAlignedBB(this.posX - r, this.posY - r, this.posZ - r,
                this.posX + r, this.posY + r, this.posZ + r);
        List<EntityCursedKnight> list = this.world.getEntitiesWithinAABB(EntityCursedKnight.class, box);
        int c = 0;
        for (EntityCursedKnight k : list) {
            if (k.isEntityAlive()) c++;
        }
        return c;
    }

    private void trySummonWave(int count, int minRadius, int maxRadius) {
        if (world.isRemote) return;
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            if (spawnOneKnight(minRadius, maxRadius)) spawned++;
        }
        if (spawned > 0) {
            playGlobal(SoundEvents.ENTITY_WITHER_SPAWN, 0.7F, 1.2F);
        }
    }

    private boolean spawnOneKnight(int minRadius, int maxRadius) {
        if (!(this.world instanceof WorldServer)) return false;
        WorldServer ws = (WorldServer) this.world;

        for (int tries = 0; tries < 10; tries++) {
            double ang = this.rand.nextDouble() * Math.PI * 2.0;
            int rad = minRadius + this.rand.nextInt(Math.max(1, maxRadius - minRadius + 1));
            int dx = (int) Math.round(Math.cos(ang) * rad);
            int dz = (int) Math.round(Math.sin(ang) * rad);
            int dy = 1 + this.rand.nextInt(2);

            BlockPos pos = new BlockPos(this.posX + dx, this.posY + dy, this.posZ + dz);

            if (!world.isAirBlock(pos) || !world.isAirBlock(pos.up())) continue;

            EntityCursedKnight k = new EntityCursedKnight(ws);
            k.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            EntityPlayer target = ws.getNearestAttackablePlayer(this, 32, 32);
            if (target != null) k.setAttackTarget(target);

            spawnBurstAt(ws, pos, EnumParticleTypes.SPELL_MOB, 8);  // 减少粒子

            ws.spawnEntity(k);
            return true;
        }
        return false;
    }

    private void spawnBurstAt(WorldServer ws, BlockPos p, EnumParticleTypes type, int count) {
        if (!PARTICLES_ENABLED) return;
        // 减少到原来的1/3
        int actualCount = Math.max(3, count / 3);
        for (int i = 0; i < actualCount; i++) {
            double ox = (this.rand.nextDouble() - 0.5) * 0.6;
            double oy = this.rand.nextDouble() * 0.8;
            double oz = (this.rand.nextDouble() - 0.5) * 0.6;
            ws.spawnParticle(type, p.getX() + 0.5 + ox, p.getY() + 0.2 + oy, p.getZ() + 0.5 + oz,
                    1, 0, 0, 0, 0.0);
        }
    }

    // ========== 事件处理器（Gate系统核心） ==========
    @Mod.EventBusSubscriber(modid = MODID)
    public static class EventHooks {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onAttackPre(LivingAttackEvent e) {
            if (!(e.getEntityLiving() instanceof EntityRiftwarden)) return;
            EntityRiftwarden boss = (EntityRiftwarden) e.getEntityLiving();

            // 虚弱状态允许所有攻击
            if (boss.laserExhaustionTime > 0) {
                return;
            }

            // Gate无敌期间阻挡非信任伤害
            if (boss.isGateInvulnerable() && !boss.isTrustedChunkSource(e.getSource())) {
                e.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onHeal(LivingHealEvent e) {
            if (!(e.getEntityLiving() instanceof EntityRiftwarden)) return;
            EntityRiftwarden boss = (EntityRiftwarden) e.getEntityLiving();

            // Gate无敌期间禁止治疗
            if (boss.isGateInvulnerable()) {
                e.setCanceled(true);
                e.setAmount(0F);
                return;
            }

            // 战斗中限制治疗效果
            if (boss.getAttackTarget() != null) {
                e.setAmount(e.getAmount() * 0.25F);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public static void onFinalDamage(LivingDamageEvent e) {
            if (!(e.getEntityLiving() instanceof EntityRiftwarden)) return;
            EntityRiftwarden boss = (EntityRiftwarden) e.getEntityLiving();

            // 信任伤害源通过
            if (boss.isTrustedChunkSource(e.getSource())) {
                return;
            }

            // Gate无敌期间取消伤害
            if (boss.isGateInvulnerable()) {
                e.setCanceled(true);
                e.setAmount(0F);
            }
        }
    }
}