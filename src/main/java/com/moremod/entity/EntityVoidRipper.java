package com.moremod.entity;

import com.moremod.combat.TrueDamageHelper;
import com.moremod.entity.fx.EntityLaserBeam;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.List;

import static com.moremod.moremod.MODID;

public class EntityVoidRipper extends EntityMob implements IAnimatable {

    private AnimationFactory factory = new AnimationFactory(this);

    // ===== Animation name constants =====
    private static final String ANIM_IDLE                  = "animation.void_ripper.idle";
    private static final String ANIM_WALK                  = "animation.void_ripper.walk";
    private static final String ANIM_RUN                   = "animation.void_ripper.run";
    private static final String ANIM_BERSERK_IDLE          = "animation.void_ripper.berserk_idle";
    private static final String ANIM_BERSERK_RUN           = "animation.void_ripper.berserk_run";
    private static final String ANIM_SLASH                 = "animation.void_ripper.attack_slash";
    private static final String ANIM_VOID_BURST            = "animation.void_ripper.attack_void_burst_enhanced";
    private static final String ANIM_GRAB_EXT              = "animation.void_ripper.attack_grab_extended";
    private static final String ANIM_LASER_WARNING         = "animation.void_ripper.laser_warning";
    private static final String ANIM_LASER_CHARGING        = "animation.void_ripper.laser_charging";
    private static final String ANIM_LASER_FIRE            = "animation.void_ripper.laser_fire";
    private static final String ANIM_BERSERK_COMBO         = "animation.void_ripper.berserk_combo";
    private static final String ANIM_BERSERK_RUNNING_COMBO = "animation.void_ripper.berserk_running_combo";
    private static final String ANIM_DUAL_SLAM             = "animation.void_ripper.dual_slam";
    private static final String ANIM_LEFT_HAND             = "animation.void_ripper.attack_left_hand";

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation(MODID, "entities/void_ripper");
    }

    // 数据同步参数
    private static final DataParameter<Integer> ATTACK_STATE = EntityDataManager.createKey(EntityVoidRipper.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> IS_CHARGING = EntityDataManager.createKey(EntityVoidRipper.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> IS_RUNNING = EntityDataManager.createKey(EntityVoidRipper.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> IS_BERSERK = EntityDataManager.createKey(EntityVoidRipper.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> GRABBED_ENTITY = EntityDataManager.createKey(EntityVoidRipper.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LASER_TARGET = EntityDataManager.createKey(EntityVoidRipper.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> LASER_STATE = EntityDataManager.createKey(EntityVoidRipper.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> ATTACK_HAND = EntityDataManager.createKey(EntityVoidRipper.class, DataSerializers.VARINT);

    // 攻击状态
    private static final int STATE_IDLE = 0;
    private static final int STATE_SLASH = 1;
    private static final int STATE_VOID_BURST = 2;
    private static final int STATE_GRAB = 3;
    private static final int STATE_LASER = 4;
    private static final int STATE_BERSERK_COMBO = 5;
    private static final int STATE_DUAL_SLAM = 6;
    private static final int STATE_LEFT_HAND = 7;
    private static final int STATE_BERSERK_RUNNING_COMBO = 8;

    // 攻击手部
    private static final int HAND_LEFT = 0;
    private static final int HAND_RIGHT = 1;
    private static final int HAND_BOTH = 2;

    // 激光状态
    private static final int LASER_IDLE = 0;
    private static final int LASER_WARNING = 1;
    private static final int LASER_CHARGING = 2;
    private static final int LASER_FIRING = 3;

    // 激光参数
    private static final int LASER_WARNING_TIME = 40;
    private static final int LASER_CHARGE_TIME = 60;
    private static final int LASER_FIRE_TIME = 80;
    private static final float LASER_DAMAGE_PER_TICK = 2.0F;
    private static final float LASER_RANGE = 120.0F;
    private static final int LASER_DAMAGE_START_DELAY = 15; // 激光伤害延迟

    // ========== Gate 限伤系统 ==========
    private static final String CHUNK_ID = MODID + ".void_ripper_chunk";
    public static final DamageSource RIPPER_CHUNK = new DamageSource(CHUNK_ID)
            .setDamageIsAbsolute()
            .setDamageBypassesArmor();

    private int invulTicks = 0;
    private boolean pendingChunk = false;
    private boolean applyingChunk = false;
    private float frozenHealth = -1F;
    private float frozenAbsorb = -1F;
    private int gateOpenFxCooldown = 0;
    private static final int INVUL_TICKS_BASE = 40;

    // 虚空爆发增强参数
    private static final float VOID_BURST_RADIUS = 15.0F;
    private static final int VOID_BURST_CHARGE_TIME = 150;
    private static final float VOID_BURST_DAMAGE_PER_TICK = 3.0F;
    private static final int VOID_BURST_DAMAGE_DURATION = 60;
    private int voidBurstDamageTicks = 0;
    private int voidBurstParticleTimer = 0;

    // 抓取增强参数
    private static final float GRAB_REACH = 10.0F;
    private static final int GRAB_EXTEND_TIME = 20;
    private int grabExtendTimer = 0;
    private boolean isArmExtended = false;
    private int grabAnimationTime = 80;

    // 狂暴奔跑连击参数
    private boolean isRunningCombo = false;
    private int runningComboTimer = 0;
    private static final int RUNNING_COMBO_MAX_TIME = 200;
    private float runningComboSpeed = 0.6F;

    // 左手攻击参数
    private int leftHandCooldown = 0;
    private static final int LEFT_HAND_COOLDOWN_TIME = 10;
    private static final int LEFT_HAND_ANIMATION_TIME = 24;
    private int leftHandCombo = 0;

    // 冷却和计时器
    private int attackCooldown = 0;
    private int specialAttackCooldown = 0;
    private int voidBurstCharge = 0;
    private float coreRotation = 0;
    private int animationTick = 0;
    private int laserTimer = 0;
    private int laserCooldown = 0;
    private int dualSlamCooldown = 0;
    private int currentAttackHand = HAND_RIGHT;

    // AI卡住检测
    private int stuckTimer = 0;
    private BlockPos lastPos = BlockPos.ORIGIN;
    private int noTargetTimer = 0;

    // 抓取系统
    private Entity grabbedEntity = null;
    private int grabDuration = 0;
    private static final int MAX_GRAB_TIME = 100;
    private double grabOffsetX, grabOffsetY, grabOffsetZ;

    // 狂暴系统
    private boolean isBerserk = false;
    private int berserkTimer = 0;
    private static final int BERSERK_DURATION = 400;
    private static final float BERSERK_THRESHOLD = 0.3F;
    private int comboAttackCounter = 0;

    // 额外特效
    private int voidAuraTimer = 0;
    private float shadowTrailAlpha = 0;

    public EntityVoidRipper(World worldIn) {
        super(worldIn);
        this.setSize(1.2F, 3.5F);
        this.experienceValue = 100;
        this.isImmuneToFire = true;
        this.stepHeight = 1.5F;
        this.moveHelper = new EntityMoveHelper(this);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(ATTACK_STATE, STATE_IDLE);
        this.dataManager.register(IS_CHARGING, false);
        this.dataManager.register(IS_RUNNING, false);
        this.dataManager.register(IS_BERSERK, false);
        this.dataManager.register(GRABBED_ENTITY, -1);
        this.dataManager.register(LASER_TARGET, -1);
        this.dataManager.register(LASER_STATE, LASER_IDLE);
        this.dataManager.register(ATTACK_HAND, HAND_RIGHT);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new AIAntiStuck());
        this.tasks.addTask(2, new AILaserAttack());
        this.tasks.addTask(3, new AIVoidBurstEnhanced());
        this.tasks.addTask(4, new AIGrabExtended());
        this.tasks.addTask(5, new AIBerserkRunningCombo());
        this.tasks.addTask(6, new AILeftHandRapidAttack());
        this.tasks.addTask(7, new AIBerserkCombo());
        this.tasks.addTask(8, new AISlashAttack());
        this.tasks.addTask(9, new AIDualSlamAttack());
        this.tasks.addTask(10, new EntityAIAttackMelee(this, 1.2D, false));
        this.tasks.addTask(11, new EntityAIMoveTowardsRestriction(this, 1.0D));
        this.tasks.addTask(12, new EntityAIWanderAvoidWater(this, 0.8D));
        this.tasks.addTask(13, new EntityAIWatchClosest(this, EntityPlayer.class, 16.0F));
        this.tasks.addTask(14, new EntityAILookIdle(this));

        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(200.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(15.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(10.0D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.8D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0D);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();

        // Gate 系统处理（最优先）
        handleInvulnerability();

        // 检测是否卡住
        checkIfStuck();

        // 更新各种状态
        updateVoidBurstEnhanced();
        updateGrabEnhanced();
        updateRunningCombo();
        updateLeftHandAttack();
        updateBerserkState();
        updateLaserState();
        updateMovementState();

        // 动画计时器
        if (animationTick > 0) {
            animationTick--;
            if (animationTick == 0) {
                setAttackState(STATE_IDLE);
            }
        }

        // 冷却计时器
        if (attackCooldown > 0) attackCooldown--;
        if (specialAttackCooldown > 0) specialAttackCooldown--;
        if (leftHandCooldown > 0) leftHandCooldown--;
        if (laserCooldown > 0) laserCooldown--;
        if (dualSlamCooldown > 0) dualSlamCooldown--;

        // 核心旋转
        coreRotation += getIsBerserk() ? 15.0F : (getIsRunning() ? 10.0F : 5.0F);
        if (coreRotation >= 360.0F) coreRotation -= 360.0F;

        // 虚空光环效果
        voidAuraTimer++;
        if (voidAuraTimer > 100) voidAuraTimer = 0;

        // 客户端粒子效果
        if (world.isRemote) {
            spawnVoidParticles();

            if (getIsRunning()) {
                shadowTrailAlpha = Math.min(1.0F, shadowTrailAlpha + 0.1F);
                spawnRunningEffects();
            } else {
                shadowTrailAlpha = Math.max(0.0F, shadowTrailAlpha - 0.05F);
            }

            if (getIsCharging()) {
                spawnChargingEffects();
            }

            if (getIsBerserk()) {
                spawnBerserkEffects();
            }
        }
    }

    // ========== Gate 系统核心方法 ==========

    private void handleInvulnerability() {
        if (this.invulTicks > 0) {
            this.invulTicks--;

            // 死亡检查：允许死亡
            if (this.getHealth() <= 0F) {
                this.invulTicks = 0;
                this.pendingChunk = false;
                this.frozenHealth = -1F;
                this.frozenAbsorb = -1F;
                return;
            }

            // 冻结血量
            if (!this.applyingChunk) {
                if (this.frozenHealth >= 0F && this.getHealth() != this.frozenHealth) {
                    this.setHealth(this.frozenHealth);
                }
                if (this.frozenAbsorb >= 0F && this.getAbsorptionAmount() != this.frozenAbsorb) {
                    this.setAbsorptionAmount(this.frozenAbsorb);
                }
            }

            // 无敌粒子效果
            if (this.ticksExisted % 5 == 0 && world instanceof WorldServer) {
                spawnRing(EnumParticleTypes.REDSTONE, 16, 1.5D);
            }

            // 无敌结束
            if (this.invulTicks == 0) {
                if (this.pendingChunk && this.getHealth() > 0F) {
                    float dmg = getChunkSize();
                    this.applyingChunk = true;
                    try {
                        super.attackEntityFrom(RIPPER_CHUNK, dmg);
                        playSound(SoundEvents.BLOCK_ANVIL_LAND, 0.7F, 1.0F);
                        spawnRing(EnumParticleTypes.CRIT_MAGIC, 32, 2.0D);
                    } finally {
                        this.applyingChunk = false;
                    }
                }
                this.pendingChunk = false;
                this.frozenHealth = -1F;
                this.frozenAbsorb = -1F;
            }
        }

        if (gateOpenFxCooldown > 0) gateOpenFxCooldown--;
    }

    public float getChunkSize() {
        return 30.0F;
    }

    public boolean isGateInvulnerable() {
        return invulTicks > 0;
    }

    public void openGateAndFreeze(boolean scheduleChunk) {
        int base = isBerserk ? INVUL_TICKS_BASE - 10 : INVUL_TICKS_BASE;
        this.invulTicks = Math.max(this.invulTicks, Math.max(20, base));

        this.frozenHealth = this.getHealth();
        this.frozenAbsorb = this.getAbsorptionAmount();
        this.pendingChunk = scheduleChunk;

        if (gateOpenFxCooldown <= 0) {
            playSound(SoundEvents.ITEM_SHIELD_BLOCK, 0.9F, 0.9F);
            spawnRing(EnumParticleTypes.PORTAL, 32, 2.0D);
            gateOpenFxCooldown = 5;
        }
    }

    public boolean isTrustedDamageSource(DamageSource src) {
        if (src == null) return false;
        return CHUNK_ID.equals(src.getDamageType());
    }

    private void spawnRing(EnumParticleTypes type, int count, double radius) {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;
        double cx = this.posX;
        double cy = this.posY + this.height * 0.5D;
        double cz = this.posZ;
        for (int i = 0; i < count; i++) {
            double ang = (Math.PI * 2 * i) / count;
            double dx = cx + Math.cos(ang) * radius;
            double dz = cz + Math.sin(ang) * radius;
            ws.spawnParticle(type, dx, cy, dz, 1, 0, 0, 0, 0.0D);
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        // 信任伤害源直接通过
        if (isTrustedDamageSource(source)) {
            return super.attackEntityFrom(source, amount);
        }

        // Gate 无敌检查
        if (this.isGateInvulnerable()) {
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

    @Override
    protected void damageEntity(DamageSource damageSrc, float damageAmount) {
        // 信任伤害源直接通过
        if (isTrustedDamageSource(damageSrc)) {
            super.damageEntity(damageSrc, damageAmount);
            return;
        }

        // Gate 无敌期间不处理
        if (this.isGateInvulnerable()) {
            return;
        }

        // 固定伤害：每次 20 血
        float actualDamage = 20.0F;

        // 判断是否需要累积大额伤害
        boolean scheduleChunk = damageAmount >= getChunkSize();

        // 开启 Gate 并冻结血量
        openGateAndFreeze(scheduleChunk);

        // 立即应用固定伤害
        if (!this.applyingChunk) {
            this.applyingChunk = true;
            try {
                super.damageEntity(damageSrc, actualDamage);
            } finally {
                this.applyingChunk = false;
            }
        }

        // 给玩家反馈
        if (damageSrc.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) damageSrc.getTrueSource();
            float healthPercent = (this.getHealth() / this.getMaxHealth()) * 100;
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    String.format("§5虚空撕裂者 §7| §e%.0f%%", healthPercent)
            ), true);
        }

        // 特效
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                    posX, posY + height * 0.5, posZ,
                    20, 0.5, 0.5, 0.5, 0.1);

            if (scheduleChunk) {
                ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                        posX, posY + height * 0.5, posZ,
                        30, 1, 1, 1, 0.2);

                if (damageSrc.getTrueSource() instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) damageSrc.getTrueSource();
                    player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            "§6§l强力一击！将在无敌结束后造成额外伤害！"));
                }
            }
        }
    }

    // 新增防卡住检测
    private void checkIfStuck() {
        BlockPos currentPos = new BlockPos(this);

        if (currentPos.equals(lastPos)) {
            stuckTimer++;
            if (stuckTimer > 60) {
                if (onGround) {
                    motionY = 0.0D;
                }

                if (stuckTimer > 100) {
                    double angle = rand.nextDouble() * Math.PI * 2;
                    motionX = Math.cos(angle) * 0.3;
                    motionZ = Math.sin(angle) * 0.3;

                    if (stuckTimer > 120) {
                        stuckTimer = 0;

                        EntityLivingBase target = getAttackTarget();
                        if (target != null && rand.nextInt(3) == 0) {
                            attemptTeleportNearTarget(target);
                        }
                    }
                }
            }
        } else {
            stuckTimer = 0;
            lastPos = currentPos;
        }

        if (getAttackTarget() == null) {
            noTargetTimer++;
            if (noTargetTimer > 200) {
                noTargetTimer = 0;
                List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class,
                        getEntityBoundingBox().grow(40.0D));
                if (!players.isEmpty()) {
                    setAttackTarget(players.get(rand.nextInt(players.size())));
                }
            }
        } else {
            noTargetTimer = 0;
        }
    }

    private void attemptTeleportNearTarget(EntityLivingBase target) {
        double angle = rand.nextDouble() * Math.PI * 2;
        double distance = 3.0D + rand.nextDouble() * 3.0D;
        double tpX = target.posX + Math.cos(angle) * distance;
        double tpY = target.posY;
        double tpZ = target.posZ + Math.sin(angle) * distance;

        BlockPos tpPos = new BlockPos(tpX, tpY, tpZ);
        if (world.isAirBlock(tpPos) && world.isAirBlock(tpPos.up())) {
            this.setPositionAndUpdate(tpX, tpY, tpZ);
            playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1.0F, 1.0F);

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int i = 0; i < 50; i++) {
                    ws.spawnParticle(EnumParticleTypes.PORTAL,
                            tpX + (rand.nextDouble() - 0.5) * 2,
                            tpY + rand.nextDouble() * 2,
                            tpZ + (rand.nextDouble() - 0.5) * 2,
                            1, 0, 0, 0, 0.1);
                }
            }
        }
    }

    // 增强版虚空爆发
    private void updateVoidBurstEnhanced() {
        if (getAttackState() == STATE_VOID_BURST) {
            voidBurstCharge++;

            if (voidBurstCharge < VOID_BURST_CHARGE_TIME) {
                this.motionX = 0;
                this.motionY = 0;
                this.motionZ = 0;

                if (world instanceof WorldServer && voidBurstCharge % 2 == 0) {
                    WorldServer ws = (WorldServer) world;
                    int particleCount = Math.min(50, voidBurstCharge / 2);

                    for (int i = 0; i < particleCount; i++) {
                        double angle = rand.nextDouble() * Math.PI * 2;
                        double distance = VOID_BURST_RADIUS * (1.0 - (double)voidBurstCharge / VOID_BURST_CHARGE_TIME);
                        double px = posX + Math.cos(angle) * distance;
                        double py = posY + rand.nextDouble() * 3;
                        double pz = posZ + Math.sin(angle) * distance;

                        double vx = (posX - px) * 0.1;
                        double vy = (posY + 1.5 - py) * 0.1;
                        double vz = (posZ - pz) * 0.1;

                        ws.spawnParticle(EnumParticleTypes.PORTAL,
                                px, py, pz, 1, vx, vy, vz, 0);
                    }

                    if (voidBurstCharge > VOID_BURST_CHARGE_TIME / 2) {
                        for (int i = 0; i < 10; i++) {
                            double angle = rand.nextDouble() * Math.PI * 2;
                            double dist = rand.nextDouble() * VOID_BURST_RADIUS;
                            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                                    posX + Math.cos(angle) * dist,
                                    posY + 0.1,
                                    posZ + Math.sin(angle) * dist,
                                    1, 0.0D, 0.1D, 0.0D, 0);
                        }
                    }
                }

                if (voidBurstCharge % 20 == 0) {
                    float pitch = 0.5F + (float)voidBurstCharge / VOID_BURST_CHARGE_TIME;
                    playSound(SoundEvents.ENTITY_ENDERMEN_AMBIENT, 2.0F, pitch);
                }

            } else if (voidBurstCharge == VOID_BURST_CHARGE_TIME) {
                performVoidBurstEnhanced();
                voidBurstDamageTicks = VOID_BURST_DAMAGE_DURATION;
            }

            if (voidBurstDamageTicks > 0) {
                voidBurstDamageTicks--;

                if (!world.isRemote && voidBurstDamageTicks % 5 == 0) {
                    List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class,
                            new AxisAlignedBB(posX - VOID_BURST_RADIUS, posY - 5, posZ - VOID_BURST_RADIUS,
                                    posX + VOID_BURST_RADIUS, posY + 8, posZ + VOID_BURST_RADIUS));

                    for (EntityLivingBase entity : entities) {
                        if (entity != this) {
                            float distance = entity.getDistance(this);
                            if (distance <= VOID_BURST_RADIUS) {
                                float damage = VOID_BURST_DAMAGE_PER_TICK * (1.0F - distance / VOID_BURST_RADIUS);
                                entity.attackEntityFrom(DamageSource.causeMobDamage(this).setMagicDamage(), damage);
                                entity.addPotionEffect(new PotionEffect(MobEffects.WITHER, 40, 2));
                            }
                        }
                    }

                    if (world instanceof WorldServer) {
                        WorldServer ws = (WorldServer) world;
                        for (int i = 0; i < 30; i++) {
                            double angle = rand.nextDouble() * Math.PI * 2;
                            double dist = rand.nextDouble() * VOID_BURST_RADIUS;
                            double height = rand.nextDouble() * 5;

                            ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                                    posX + Math.cos(angle) * dist,
                                    posY + height,
                                    posZ + Math.sin(angle) * dist,
                                    1, 0, -0.2, 0, 0.05);
                        }
                    }
                }

                if (voidBurstDamageTicks == 0) {
                    voidBurstCharge = 0;
                    setAttackState(STATE_IDLE);
                    setIsCharging(false);
                    specialAttackCooldown = 300;
                }
            }
        }
    }

    private void performVoidBurstEnhanced() {
        if (world.isRemote) return;

        playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 3.0F, 0.5F);
        playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 0.5F);
        playSound(SoundEvents.ENTITY_WITHER_SPAWN, 2.0F, 1.0F);

        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class,
                new AxisAlignedBB(posX - VOID_BURST_RADIUS, posY - 5, posZ - VOID_BURST_RADIUS,
                        posX + VOID_BURST_RADIUS, posY + 8, posZ + VOID_BURST_RADIUS));

        for (EntityLivingBase entity : entities) {
            if (entity != this) {
                float distance = entity.getDistance(this);
                if (distance <= VOID_BURST_RADIUS) {
                    float damage = 40.0F * (1.0F - distance / VOID_BURST_RADIUS);
                    entity.attackEntityFrom(DamageSource.causeMobDamage(this).setMagicDamage(), damage);

                    double dx = entity.posX - this.posX;
                    double dz = entity.posZ - this.posZ;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0) {
                        double force = 3.0 * (1.0F - distance / VOID_BURST_RADIUS);
                        entity.motionX = (dx / dist) * force;
                        entity.motionY = 1.5;
                        entity.motionZ = (dz / dist) * force;
                    }

                    entity.addPotionEffect(new PotionEffect(MobEffects.WITHER, 200, 2));
                    entity.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 100, 0));
                    entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 150, 2));
                    entity.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 80, 1));
                }
            }
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;

            for (int ring = 0; ring < 5; ring++) {
                double radius = (ring + 1) * 3;
                for (int i = 0; i < 60; i++) {
                    double angle = (Math.PI * 2) * i / 60;
                    double px = posX + Math.cos(angle) * radius;
                    double pz = posZ + Math.sin(angle) * radius;

                    ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                            px, posY + 1, pz,
                            1, 0.0D, 0.5D, 0.0D, 0);
                }
            }

            for (int i = 0; i < 200; i++) {
                ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                        posX, posY + i * 0.1, posZ,
                        5, 0.5, 0, 0.5, 0.2);
            }
        }
    }

    // 增强版抓取
    private void updateGrabEnhanced() {
        if (getAttackState() == STATE_GRAB) {
            if (!isArmExtended) {
                grabExtendTimer++;

                if (grabExtendTimer >= GRAB_EXTEND_TIME) {
                    isArmExtended = true;

                    Vec3d lookVec = this.getLookVec();
                    Vec3d startPos = new Vec3d(posX, posY + getEyeHeight(), posZ);
                    Vec3d endPos = startPos.add(lookVec.x * GRAB_REACH, lookVec.y * GRAB_REACH, lookVec.z * GRAB_REACH);

                    List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class,
                            new AxisAlignedBB(
                                    Math.min(startPos.x, endPos.x) - 1, Math.min(startPos.y, endPos.y) - 1, Math.min(startPos.z, endPos.z) - 1,
                                    Math.max(startPos.x, endPos.x) + 1, Math.max(startPos.y, endPos.y) + 1, Math.max(startPos.z, endPos.z) + 1
                            ));

                    EntityLivingBase closestTarget = null;
                    double closestDistance = Double.MAX_VALUE;

                    for (EntityLivingBase target : targets) {
                        if (target != this) {
                            double distance = target.getDistanceSq(this);
                            if (distance < closestDistance && distance <= GRAB_REACH * GRAB_REACH) {
                                closestDistance = distance;
                                closestTarget = target;
                            }
                        }
                    }

                    if (closestTarget != null) {
                        grabEntity(closestTarget);
                    }
                }

                if (world instanceof WorldServer && grabExtendTimer % 2 == 0) {
                    WorldServer ws = (WorldServer) world;
                    Vec3d lookVec = this.getLookVec();
                    double progress = (double)grabExtendTimer / GRAB_EXTEND_TIME;

                    for (int i = 0; i < 5; i++) {
                        double dist = progress * GRAB_REACH * ((double)i / 5);
                        double px = posX + lookVec.x * dist;
                        double py = posY + getEyeHeight() + lookVec.y * dist;
                        double pz = posZ + lookVec.z * dist;

                        ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                                px, py, pz,
                                3, 0.1, 0.1, 0.1, 0);
                    }
                }
            }

            if (grabbedEntity != null) {
                updateGrabbedEntity();
            }
        } else {
            grabExtendTimer = 0;
            isArmExtended = false;
        }
    }

    private void grabEntity(Entity entity) {
        if (entity == null || !entity.isEntityAlive()) return;

        grabbedEntity = entity;
        grabDuration = 0;
        setGrabbedEntityId(entity.getEntityId());

        double angle = Math.atan2(entity.posZ - this.posZ, entity.posX - this.posX);
        grabOffsetX = Math.cos(angle) * 2.0D;
        grabOffsetY = 0.5D;
        grabOffsetZ = Math.sin(angle) * 2.0D;

        playSound(SoundEvents.ENTITY_ENDERMEN_SCREAM, 1.5F, 0.8F);

        if (entity instanceof EntityLivingBase) {
            ((EntityLivingBase) entity).addPotionEffect(
                    new PotionEffect(MobEffects.SLOWNESS, MAX_GRAB_TIME, 10));
        }
    }

    private void updateGrabbedEntity() {
        if (!grabbedEntity.isEntityAlive() || grabDuration >= MAX_GRAB_TIME ||
                grabbedEntity.getDistance(this) > 12.0F) {
            releaseGrabbedEntity();
            return;
        }

        grabDuration++;

        double targetX = this.posX + grabOffsetX;
        double targetY = this.posY + this.height * 0.7 + grabOffsetY;
        double targetZ = this.posZ + grabOffsetZ;

        double dx = targetX - grabbedEntity.posX;
        double dy = targetY - grabbedEntity.posY;
        double dz = targetZ - grabbedEntity.posZ;

        grabbedEntity.motionX = dx * 0.5D;
        grabbedEntity.motionY = dy * 0.5D;
        grabbedEntity.motionZ = dz * 0.5D;

        if (grabbedEntity instanceof EntityLivingBase) {
            ((EntityLivingBase) grabbedEntity).setPositionAndUpdate(targetX, targetY, targetZ);
        }

        if (grabDuration % 20 == 0 && !world.isRemote) {
            grabbedEntity.attackEntityFrom(DamageSource.causeMobDamage(this), 5.0F);

            if (grabbedEntity instanceof EntityPlayer) {
                ((EntityPlayer) grabbedEntity).addPotionEffect(
                        new PotionEffect(MobEffects.WEAKNESS, 40, 1));
            }
        }

        if (world.isRemote && grabDuration % 5 == 0) {
            for (int i = 0; i < 5; i++) {
                world.spawnParticle(EnumParticleTypes.PORTAL,
                        grabbedEntity.posX + (rand.nextDouble() - 0.5D),
                        grabbedEntity.posY + grabbedEntity.height * 0.5,
                        grabbedEntity.posZ + (rand.nextDouble() - 0.5D),
                        0, 0, 0);
            }
        }

        if (grabDuration >= 60 && rand.nextFloat() < 0.3F) {
            throwGrabbedEntity();
        }
    }

    private void releaseGrabbedEntity() {
        if (grabbedEntity != null) {
            setGrabbedEntityId(-1);
            grabbedEntity = null;
            grabDuration = 0;
        }
    }

    private void throwGrabbedEntity() {
        if (grabbedEntity == null) return;

        EntityLivingBase target = this.getAttackTarget();
        double throwX, throwY = 0.8D, throwZ;

        if (target != null) {
            double dx = target.posX - this.posX;
            double dz = target.posZ - this.posZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            throwX = (dx / dist) * 2.5D;
            throwZ = (dz / dist) * 2.5D;
        } else {
            double angle = this.rotationYaw * Math.PI / 180.0D;
            throwX = -Math.sin(angle) * 2.5D;
            throwZ = Math.cos(angle) * 2.5D;
        }

        grabbedEntity.motionX = throwX;
        grabbedEntity.motionY = throwY;
        grabbedEntity.motionZ = throwZ;

        if (!world.isRemote) {
            grabbedEntity.attackEntityFrom(DamageSource.causeMobDamage(this), 10.0F);
        }

        playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5F, 0.5F);
        releaseGrabbedEntity();
    }

    // 狂暴奔跑连击
    private void updateRunningCombo() {
        if (isRunningCombo) {
            runningComboTimer++;

            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D + runningComboSpeed);

            EntityLivingBase target = this.getAttackTarget();
            if (target != null) {
                double dx = target.posX - this.posX;
                double dz = target.posZ - this.posZ;
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist > 0) {
                    this.motionX = (dx / dist) * runningComboSpeed;
                    this.motionZ = (dz / dist) * runningComboSpeed;

                    if (runningComboTimer % 5 == 0 && dist < 4.0) {
                        boolean useLeftHand = (runningComboTimer / 5) % 2 == 0;

                        float damage = (float)getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
                        damage *= 1.2F;

                        target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);

                        target.motionX = (dx / dist) * 0.5;
                        target.motionY = 0.3;
                        target.motionZ = (dz / dist) * 0.5;

                        if (world instanceof WorldServer) {
                            WorldServer ws = (WorldServer) world;
                            ws.spawnParticle(useLeftHand ? EnumParticleTypes.SPELL_WITCH : EnumParticleTypes.FLAME,
                                    target.posX, target.posY + target.height * 0.5, target.posZ,
                                    20, 0.3, 0.3, 0.3, 0.1);
                        }

                        playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5F, 0.8F + rand.nextFloat() * 0.4F);
                    }
                }

                if (world instanceof WorldServer && runningComboTimer % 2 == 0) {
                    WorldServer ws = (WorldServer) world;
                    for (int i = 0; i < 5; i++) {
                        ws.spawnParticle(EnumParticleTypes.FLAME,
                                posX - motionX * i * 0.5,
                                posY + 0.5,
                                posZ - motionZ * i * 0.5,
                                1, 0, 0, 0, 0.05);

                        ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                                posX - motionX * i * 0.5,
                                posY,
                                posZ - motionZ * i * 0.5,
                                1, 0.0D, 0.0D, 0.0D, 0);
                    }
                }
            }

            if (runningComboTimer >= RUNNING_COMBO_MAX_TIME || target == null || target.getDistance(this) > 8.0) {
                isRunningCombo = false;
                runningComboTimer = 0;
                setAttackState(STATE_IDLE);
                this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            }
        }
    }

    // 优化的左手攻击
    private void updateLeftHandAttack() {
        if (getAttackState() == STATE_LEFT_HAND) {
            EntityLivingBase target = this.getAttackTarget();

            int attackFrame = LEFT_HAND_ANIMATION_TIME - 9;
            boolean shouldHit = animationTick == attackFrame ||
                    animationTick == attackFrame - 2 ||
                    animationTick == attackFrame + 2;

            if (target != null && shouldHit) {
                if (this.getDistanceSq(target) < 12.0D) {
                    float damage = (float)getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
                    damage *= (1.1F + leftHandCombo * 0.1F);

                    target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);

                    target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 80, 1));
                    target.addPotionEffect(new PotionEffect(MobEffects.WITHER, 60, 0));

                    double angle = Math.atan2(target.posZ - this.posZ, target.posX - this.posX);
                    angle += Math.PI / 4;
                    target.motionX = Math.cos(angle) * (1.2 + leftHandCombo * 0.2);
                    target.motionY = 0.4;
                    target.motionZ = Math.sin(angle) * (1.2 + leftHandCombo * 0.2);

                    if (world instanceof WorldServer) {
                        WorldServer ws = (WorldServer) world;
                        ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                                target.posX, target.posY + target.height * 0.5, target.posZ,
                                30 + leftHandCombo * 10, 0.5, 0.5, 0.5, 0.2);

                        ws.spawnParticle(EnumParticleTypes.PORTAL,
                                target.posX, target.posY + target.height * 0.5, target.posZ,
                                20, 0.3, 0.3, 0.3, 0);
                    }

                    playSound(SoundEvents.ENTITY_ENDERMEN_SCREAM, 1.5F, 1.2F + leftHandCombo * 0.1F);

                    leftHandCombo++;
                    if (leftHandCombo > 5) leftHandCombo = 0;
                }
            }
        } else {
            if (leftHandCombo > 0 && animationTick % 20 == 0) {
                leftHandCombo--;
            }
        }
    }

    private void updateBerserkState() {
        if (!isBerserk && getHealth() < getMaxHealth() * BERSERK_THRESHOLD) {
            enterBerserkMode();
        }

        if (isBerserk) {
            berserkTimer--;
            if (berserkTimer <= 0) {
                exitBerserkMode();
            }

            if (!world.isRemote && this.ticksExisted % 20 == 0) {
                this.addPotionEffect(new PotionEffect(MobEffects.SPEED, 40, 1, false, false));
                this.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 40, 1, false, false));
                this.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 40, 0, false, false));
            }
        }
    }

    private void enterBerserkMode() {
        isBerserk = true;
        berserkTimer = BERSERK_DURATION;
        setIsBerserk(true);

        if (!world.isRemote) {
            playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 2.0F, 0.5F);

            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class,
                    getEntityBoundingBox().grow(5.0D));
            for (EntityLivingBase entity : entities) {
                if (entity != this) {
                    double dx = entity.posX - this.posX;
                    double dz = entity.posZ - this.posZ;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0) {
                        entity.motionX = (dx / dist) * 2.0D;
                        entity.motionY = 1.0D;
                        entity.motionZ = (dz / dist) * 2.0D;
                    }
                }
            }

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int i = 0; i < 100; i++) {
                    double angle = (Math.PI * 2) * i / 100;
                    double radius = i * 0.1D;
                    double px = posX + Math.cos(angle) * radius;
                    double pz = posZ + Math.sin(angle) * radius;
                    ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                            px, posY + 2, pz, 1, 0, 0.1D, 0, 0.05D);
                }
            }
        }
    }

    private void exitBerserkMode() {
        isBerserk = false;
        setIsBerserk(false);
        playSound(SoundEvents.ENTITY_ENDERDRAGON_AMBIENT, 1.5F, 1.0F);
    }

    private void updateLaserState() {
        int laserState = getLaserState();

        switch (laserState) {
            case LASER_WARNING:
                updateLaserWarning();
                break;
            case LASER_CHARGING:
                updateLaserCharging();
                break;
            case LASER_FIRING:
                updateLaserFiring();
                break;
        }
    }

    private void startLaserAttack(EntityLivingBase target) {
        setLaserTargetId(target.getEntityId());
        setLaserState(LASER_WARNING);
        laserTimer = LASER_WARNING_TIME;
        setAttackState(STATE_LASER);
        animationTick = 240;

        setIsCharging(true);
        playSound(SoundEvents.BLOCK_NOTE_PLING, 1.5F, 0.5F);
    }

    private void updateLaserWarning() {
        laserTimer--;

        EntityLivingBase target = getLaserTargetEntity();
        if (target == null || !target.isEntityAlive()) {
            cancelLaser();
            return;
        }

        this.motionX = 0;
        this.motionY = 0;
        this.motionZ = 0;
        this.isAirBorne = false;
        this.fallDistance = 0;
        this.getNavigator().clearPath();
        this.getMoveHelper().setMoveTo(this.posX, this.posY, this.posZ, 0.0D);

        this.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

        // 强化警告提示
        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;

            if (laserTimer % 20 == 0) {
                int secondsLeft = laserTimer / 20;
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "§c§l⚠ 激光锁定！" + secondsLeft + " 秒后发射！§r §e使用盾牌格挡！"));
            }

            if (laserTimer == 20 || laserTimer == 10) {
                playSound(SoundEvents.BLOCK_NOTE_PLING, 3.0F, 2.0F);
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "§4§l§n危险！激光即将发射！举起盾牌！"));
            }
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;

            if (laserTimer % 5 == 0) {
                double radius = 3.0D;
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2) * i / 16;
                    double px = target.posX + Math.cos(angle) * radius;
                    double pz = target.posZ + Math.sin(angle) * radius;

                    ws.spawnParticle(EnumParticleTypes.REDSTONE,
                            px, target.posY + 0.1D, pz,
                            1, 1.0D, 0.0D, 0.0D, 0.0D);
                }
            }

            Vec3d handPos = getLaserEmissionPoint();
            for (int i = 0; i < 5; i++) {
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        handPos.x + (rand.nextDouble() - 0.5D) * 0.5D,
                        handPos.y + (rand.nextDouble() - 0.5D) * 0.3D,
                        handPos.z + (rand.nextDouble() - 0.5D) * 0.5D,
                        1, 0.0D, 0.0D, 0.0D, 0);
            }
        }

        if (laserTimer <= 0) {
            setLaserState(LASER_CHARGING);
            laserTimer = LASER_CHARGE_TIME;
            playSound(SoundEvents.ENTITY_WITHER_AMBIENT, 2.0F, 0.5F);
        }
    }

    private void updateLaserCharging() {
        laserTimer--;

        EntityLivingBase target = getLaserTargetEntity();
        if (target == null || !target.isEntityAlive()) {
            cancelLaser();
            return;
        }

        this.motionX = 0;
        this.motionY = 0;
        this.motionZ = 0;
        this.isAirBorne = false;
        this.fallDistance = 0;
        this.getNavigator().clearPath();
        this.getMoveHelper().setMoveTo(this.posX, this.posY, this.posZ, 0.0D);

        this.getLookHelper().setLookPositionWithEntity(target, 10.0F, 10.0F);

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            Vec3d handPos = getLaserEmissionPoint();

            double chargeProgress = 1.0D - (double)laserTimer / LASER_CHARGE_TIME;
            int particleCount = (int)(10 + chargeProgress * 20);

            for (int i = 0; i < particleCount; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double radius = 3.0D * (1.0D - chargeProgress);
                double px = handPos.x + Math.cos(angle) * radius;
                double py = handPos.y + (rand.nextDouble() - 0.5D);
                double pz = handPos.z + Math.sin(angle) * radius;

                double vx = (handPos.x - px) * 0.1D;
                double vz = (handPos.z - pz) * 0.1D;

                ws.spawnParticle(EnumParticleTypes.SPELL_MOB,
                        px, py, pz, 1,
                        0.2D, 0.6D, 1.0D, 0.0D);
            }

            if (laserTimer % 5 == 0) {
                for (int i = 0; i < 3; i++) {
                    ws.spawnParticle(EnumParticleTypes.SPELL_INSTANT,
                            handPos.x + (rand.nextDouble() - 0.5D),
                            handPos.y + (rand.nextDouble() - 0.5D),
                            handPos.z + (rand.nextDouble() - 0.5D),
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }

        if (laserTimer % 10 == 0) {
            float pitch = 0.5F + (1.0F - (float)laserTimer / LASER_CHARGE_TIME) * 1.5F;
            playSound(SoundEvents.BLOCK_PORTAL_AMBIENT, 1.5F, pitch);
        }

        if (laserTimer <= 0) {
            setLaserState(LASER_FIRING);
            laserTimer = LASER_FIRE_TIME;
            playSound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 2.0F, 2.0F);
        }
    }

    // ========== 修复后的激光发射逻辑 ==========
    private void updateLaserFiring() {
        laserTimer--;

        EntityLivingBase target = getLaserTargetEntity();
        if (target == null || !target.isEntityAlive()) {
            cancelLaser();
            return;
        }

        this.motionX = 0;
        this.motionY = 0;
        this.motionZ = 0;

        this.getLookHelper().setLookPositionWithEntity(target, 5.0F, 5.0F);

        Vec3d laserPos = getLaserEmissionPoint();
        Vec3d targetPos = new Vec3d(target.posX, target.posY + target.height * 0.5D, target.posZ);

        // 生成激光实体 - 伤害设为0，让下面的代码完全控制伤害
        if (laserTimer == LASER_FIRE_TIME - 1 && !world.isRemote) {
            EntityLaserBeam beam = new EntityLaserBeam(
                    world,
                    this,
                    target,
                    LASER_FIRE_TIME,
                    0.7F,
                    0.0F,  // ★★★ 关键：伤害设为0 ★★★
                    0x0099FF
            );
            Vec3d emissionPoint = getLaserEmissionPoint();
            beam.setPosition(emissionPoint.x, emissionPoint.y, emissionPoint.z);
            world.spawnEntity(beam);

            playSound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 2.0F, 2.0F);
            playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 1.5F, 0.5F);
        }

        Vec3d direction = targetPos.subtract(laserPos).normalize();
        double laserLength = Math.min(LASER_RANGE, laserPos.distanceTo(targetPos));
        Vec3d endPos = laserPos.add(direction.x * laserLength, direction.y * laserLength, direction.z * laserLength);

        // 粒子效果和方块破坏
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            int segments = (int)(laserLength * 4);

            for (int i = 0; i <= segments; i++) {
                double progress = (double)i / segments;
                double px = laserPos.x + (endPos.x - laserPos.x) * progress;
                double py = laserPos.y + (endPos.y - laserPos.y) * progress;
                double pz = laserPos.z + (endPos.z - laserPos.z) * progress;

                if (i % 10 == 0 && !world.isRemote && laserTimer % 10 == 0) {
                    BlockPos blockPos = new BlockPos(px, py, pz);
                    IBlockState blockState = world.getBlockState(blockPos);

                    if (!blockState.getBlock().isAir(blockState, world, blockPos)) {
                        float hardness = blockState.getBlockHardness(world, blockPos);

                        if (hardness >= 0 && hardness < 5.0F && rand.nextFloat() < 0.3F) {
                            world.destroyBlock(blockPos, true);
                            playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 0.5F, 1.5F);

                            ws.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                                    px, py, pz,
                                    20, 0.2D, 0.2D, 0.2D, 0.1D,
                                    Block.getStateId(blockState));
                        }
                    }
                }
            }

            ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                    endPos.x, endPos.y, endPos.z,
                    5, 0.5D, 0.5D, 0.5D, 0.1D);
        }

        // ========== 激光伤害判定（修复版）==========
        int ticksSinceFiring = LASER_FIRE_TIME - laserTimer;

        // 前15tick不造成伤害，给玩家反应时间
        if (!world.isRemote && laserTimer % 2 == 0 && ticksSinceFiring >= LASER_DAMAGE_START_DELAY) {
            if (target.getDistance(this) <= LASER_RANGE && canEntityBeSeen(target)) {

                if (target instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) target;

                    // 使用更可靠的格挡检测
                    boolean isBlocking = isPlayerBlocking(player);

                    if (isBlocking) {
                        // ========== 成功格挡 ==========
                        handleLaserBlocked(player);

                    } else {
                        // ========== 没有格挡 = 即死 ==========
                        handleLaserHit(player);
                        return; // 玩家死了，结束方法
                    }

                } else {
                    // 非玩家目标：正常伤害
                    target.attackEntityFrom(
                            DamageSource.causeMobDamage(this).setDamageIsAbsolute().setDamageBypassesArmor(),
                            LASER_DAMAGE_PER_TICK * 3.0F);
                    target.setFire(3);
                }
            }

            // 范围伤害（激光终点附近）
            List<EntityLivingBase> nearbyEntities = world.getEntitiesWithinAABB(EntityLivingBase.class,
                    new AxisAlignedBB(endPos.x - 2, endPos.y - 2, endPos.z - 2,
                            endPos.x + 2, endPos.y + 2, endPos.z + 2));

            for (EntityLivingBase entity : nearbyEntities) {
                if (entity != this && entity != target) {
                    // 范围伤害也检测格挡
                    if (entity instanceof EntityPlayer) {
                        EntityPlayer nearbyPlayer = (EntityPlayer) entity;
                        if (!isPlayerBlocking(nearbyPlayer)) {
                            entity.attackEntityFrom(DamageSource.causeMobDamage(this), LASER_DAMAGE_PER_TICK * 2.0F);
                            entity.setFire(2);
                        }
                    } else {
                        entity.attackEntityFrom(DamageSource.causeMobDamage(this), LASER_DAMAGE_PER_TICK * 2.0F);
                        entity.setFire(2);
                    }
                }
            }
        }

        // 前15tick的警告提示
        if (!world.isRemote && ticksSinceFiring < LASER_DAMAGE_START_DELAY && ticksSinceFiring % 5 == 0) {
            if (target instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) target;
                int ticksLeft = LASER_DAMAGE_START_DELAY - ticksSinceFiring;
                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        "§c§l⚠ 激光即将命中！举起盾牌！ ⚠"), true);
            }
        }

        if (laserTimer % 20 == 0) {
            playSound(SoundEvents.ENTITY_BLAZE_BURN, 2.0F, 0.5F);
        }

        if (laserTimer <= 0) {
            setIsCharging(false);
            cancelLaser();
            laserCooldown = 200;
        }
    }

    // ★★★ 更可靠的格挡检测方法 ★★★
    private boolean isPlayerBlocking(EntityPlayer player) {
        // 方法1：直接检查isActiveItemStackBlocking
        if (player.isActiveItemStackBlocking()) {
            return true;
        }

        // 方法2：检查玩家是否正在使用物品且手中有盾牌
        if (player.isHandActive()) {
            ItemStack activeStack = player.getActiveItemStack();
            if (!activeStack.isEmpty() && activeStack.getItem() instanceof ItemShield) {
                return true;
            }
        }

        // 方法3：检查副手盾牌 + 使用状态
        ItemStack offHand = player.getHeldItemOffhand();
        if (!offHand.isEmpty() && offHand.getItem() instanceof ItemShield) {
            if (player.isHandActive() && player.getActiveHand() == EnumHand.OFF_HAND) {
                return true;
            }
        }

        // 方法4：检查主手盾牌
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof ItemShield) {
            if (player.isHandActive() && player.getActiveHand() == EnumHand.MAIN_HAND) {
                return true;
            }
        }

        return false;
    }

    // ★★★ 处理格挡成功 ★★★
    private void handleLaserBlocked(EntityPlayer player) {
        // 1. 消耗盾牌耐久
        ItemStack shield = player.getActiveItemStack();
        if (!shield.isEmpty()) {
            shield.damageItem(5, player);

            if (shield.getItemDamage() >= shield.getMaxDamage()) {
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "§c§l✦ 你的盾牌被激光击碎了！ ✦"));
                playSound(SoundEvents.ITEM_SHIELD_BREAK, 1.5F, 1.0F);

                // 盾牌碎了给20tick无敌，避免立即死亡
                player.hurtResistantTime = 20;
            }
        }

        // 2. 轻微击退效果
        Vec3d laserDir = player.getPositionVector().subtract(this.getPositionVector()).normalize();
        double knockbackStrength = 0.3;
        player.motionX += laserDir.x * knockbackStrength;
        player.motionY += 0.2;
        player.motionZ += laserDir.z * knockbackStrength;
        player.velocityChanged = true;

        // 3. 造成少量穿透伤害
        player.attackEntityFrom(DamageSource.causeMobDamage(this), 3.0F);

        // 4. 格挡音效和粒子
        playSound(SoundEvents.ITEM_SHIELD_BLOCK, 2.0F, 0.8F);

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;

            // 格挡火花效果
            for (int i = 0; i < 20; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double speed = 0.2 + rand.nextDouble() * 0.2;
                ws.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                        player.posX,
                        player.posY + player.height * 0.5,
                        player.posZ,
                        1,
                        Math.cos(angle) * speed,
                        rand.nextDouble() * 0.2,
                        Math.sin(angle) * speed,
                        0.05);
            }

            // 护盾光环
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12;
                double px = player.posX + Math.cos(angle) * 1.2;
                double pz = player.posZ + Math.sin(angle) * 1.2;
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        px, player.posY + 1, pz,
                        1, 0, 0.1, 0.0, 0);
            }
        }

        // 5. 格挡成功提示（每20tick显示一次）
        if ((LASER_FIRE_TIME - laserTimer) % 20 == 0) {
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    "§a§l✓ 格挡成功！保持举盾！"), true);
        }
    }

    // ★★★ 处理激光命中（没有格挡）★★★
    private void handleLaserHit(EntityPlayer player) {
        // 即死
        TrueDamageHelper.triggerVanillaDeathChain(player);

        // 死亡特效
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;

            // 死亡爆炸粒子
            for (int i = 0; i < 100; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double pitch = rand.nextDouble() * Math.PI;
                double speed = 0.5 + rand.nextDouble() * 0.5;

                ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                        player.posX,
                        player.posY + player.height * 0.5,
                        player.posZ,
                        1,
                        Math.cos(angle) * Math.sin(pitch) * speed,
                        Math.cos(pitch) * speed,
                        Math.sin(angle) * Math.sin(pitch) * speed,
                        0.2);
            }

            // 黑色爆炸环
            for (int ring = 0; ring < 3; ring++) {
                double radius = (ring + 1) * 1.5;
                for (int i = 0; i < 32; i++) {
                    double angle = (Math.PI * 2 * i) / 32;
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                            player.posX + Math.cos(angle) * radius,
                            player.posY + 0.1,
                            player.posZ + Math.sin(angle) * radius,
                            1, 0, 0.5, 0, 0.1);
                }
            }
        }

        // 即死音效
        playSound(SoundEvents.ENTITY_LIGHTNING_THUNDER, 2.0F, 0.5F);
        playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 0.5F);

        // 死亡消息
        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                "§4§l✦ 你被虚空激光彻底撕裂了！ ✦"));

        // 取消激光
        cancelLaser();
        laserCooldown = 200;
    }

    private Vec3d getLaserEmissionPoint() {
        double baseX = this.posX;
        double baseY = this.posY + this.height * 0.65D;
        double baseZ = this.posZ;

        float yaw = this.rotationYaw * (float)Math.PI / 180.0F;

        if (getLaserState() == LASER_FIRING || getLaserState() == LASER_CHARGING) {
            double forwardReach = 1.5D;
            double upwardLift = 0.3D;

            double handsX = baseX - Math.sin(yaw) * forwardReach;
            double handsY = baseY + this.height * 0.2D + upwardLift;
            double handsZ = baseZ + Math.cos(yaw) * forwardReach;

            return new Vec3d(handsX, handsY, handsZ);
        } else {
            double forwardOffset = 0.5D;
            double coreX = baseX - Math.sin(yaw) * forwardOffset;
            double coreZ = baseZ + Math.cos(yaw) * forwardOffset;

            return new Vec3d(coreX, baseY, coreZ);
        }
    }

    private void cancelLaser() {
        setLaserState(LASER_IDLE);
        setLaserTargetId(-1);
        laserTimer = 0;
        setIsCharging(false);
        if (getAttackState() == STATE_LASER) {
            setAttackState(STATE_IDLE);
        }
    }

    private void updateMovementState() {
        EntityLivingBase target = this.getAttackTarget();
        boolean isMoving = Math.abs(this.motionX) > 0.05D || Math.abs(this.motionZ) > 0.05D;

        if (target != null) {
            double distanceSq = this.getDistanceSq(target);
            setIsRunning(isMoving && distanceSq > 64.0D);
        } else {
            setIsRunning(false);
        }

        if (getIsRunning() && !isRunningCombo) {
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.45D);
        } else if (!isRunningCombo) {
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        }
    }

    // 粒子效果方法
    private void spawnVoidParticles() {
        if (this.rand.nextInt(3) == 0) {
            double d0 = this.posX + (this.rand.nextDouble() - 0.5D) * 1.5D;
            double d1 = this.posY + this.rand.nextDouble() * 3.5D;
            double d2 = this.posZ + (this.rand.nextDouble() - 0.5D) * 1.5D;
            this.world.spawnParticle(EnumParticleTypes.PORTAL, d0, d1, d2, 0.0D, -0.1D, 0.0D);
        }
    }

    private void spawnRunningEffects() {
        for (int i = 0; i < 2; i++) {
            double d0 = this.posX - this.motionX * i * 0.5D;
            double d1 = this.posY + 0.5D;
            double d2 = this.posZ - this.motionZ * i * 0.5D;
            this.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, d0, d1, d2, 0, 0, 0);
        }
    }

    private void spawnChargingEffects() {
        for (int i = 0; i < 5; i++) {
            double d0 = this.posX + (this.rand.nextDouble() - 0.5D) * 4.0D;
            double d1 = this.posY + this.rand.nextDouble() * 3.5D;
            double d2 = this.posZ + (this.rand.nextDouble() - 0.5D) * 4.0D;
            double dx = (this.posX - d0) * 0.15D;
            double dy = (this.posY + 2.0D - d1) * 0.15D;
            double dz = (this.posZ - d2) * 0.15D;
            this.world.spawnParticle(EnumParticleTypes.PORTAL, d0, d1, d2, dx, dy, dz);
        }
    }

    private void spawnBerserkEffects() {
        if (this.rand.nextInt(2) == 0) {
            double d0 = this.posX + (this.rand.nextDouble() - 0.5D) * 2.0D;
            double d1 = this.posY + this.rand.nextDouble() * 3.5D;
            double d2 = this.posZ + (this.rand.nextDouble() - 0.5D) * 2.0D;
            this.world.spawnParticle(EnumParticleTypes.FLAME, d0, d1, d2, 0.0D, 0.1D, 0.0D);
            this.world.spawnParticle(EnumParticleTypes.SPELL_MOB, d0, d1, d2, 1.0D, 0.0D, 0.0D);
        }
    }

    // AI任务类
    class AIAntiStuck extends EntityAIBase {
        private int checkTimer = 0;

        @Override
        public boolean shouldExecute() {
            checkTimer++;
            if (checkTimer < 40) return false;
            checkTimer = 0;

            return stuckTimer > 60 || (!EntityVoidRipper.this.getNavigator().noPath() &&
                    EntityVoidRipper.this.getNavigator().getPath() != null &&
                    !EntityVoidRipper.this.getNavigator().getPath().isFinished());
        }

        @Override
        public void startExecuting() {
            EntityVoidRipper.this.getNavigator().clearPath();

            EntityLivingBase target = EntityVoidRipper.this.getAttackTarget();
            if (target != null) {
                EntityVoidRipper.this.getNavigator().tryMoveToEntityLiving(target, 1.2D);
            }
        }
    }

    class AIVoidBurstEnhanced extends EntityAIBase {
        public AIVoidBurstEnhanced() {
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = EntityVoidRipper.this.getAttackTarget();
            return target != null && target.isEntityAlive()
                    && EntityVoidRipper.this.getHealth() < EntityVoidRipper.this.getMaxHealth() * 0.5F
                    && specialAttackCooldown == 0
                    && getAttackState() == STATE_IDLE;
        }

        @Override
        public void startExecuting() {
            setAttackState(STATE_VOID_BURST);
            setIsCharging(true);
            specialAttackCooldown = 300;
            voidBurstCharge = 0;
            animationTick = 360;
            EntityVoidRipper.this.motionX = 0;
            EntityVoidRipper.this.motionY = 0;
            EntityVoidRipper.this.motionZ = 0;

            playSound(SoundEvents.EVOCATION_ILLAGER_PREPARE_SUMMON, 2.0F, 0.5F);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return voidBurstCharge < VOID_BURST_CHARGE_TIME + VOID_BURST_DAMAGE_DURATION;
        }

        @Override
        public void resetTask() {
            setIsCharging(false);
            voidBurstCharge = 0;
            voidBurstDamageTicks = 0;
        }
    }

    class AIGrabExtended extends EntityAIBase {
        private EntityLivingBase targetEntity;

        public AIGrabExtended() {
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (grabbedEntity != null || attackCooldown > 0 || getAttackState() != STATE_IDLE) {
                return false;
            }

            EntityLivingBase target = EntityVoidRipper.this.getAttackTarget();
            if (target == null || !target.isEntityAlive()) return false;

            double distanceSq = EntityVoidRipper.this.getDistanceSq(target);
            if (distanceSq < 16.0D || distanceSq > 100.0D) return false;

            targetEntity = target;
            return rand.nextInt(30) == 0;
        }

        @Override
        public void startExecuting() {
            setAttackState(STATE_GRAB);
            animationTick = grabAnimationTime;
            attackCooldown = 60;
            grabExtendTimer = 0;
            isArmExtended = false;
        }

        @Override
        public void updateTask() {
            if (targetEntity != null && !isArmExtended) {
                EntityVoidRipper.this.getLookHelper().setLookPositionWithEntity(targetEntity, 30.0F, 30.0F);
            }
        }

        @Override
        public boolean shouldContinueExecuting() {
            return animationTick > 0 || grabbedEntity != null;
        }

        @Override
        public void resetTask() {
            grabExtendTimer = 0;
            isArmExtended = false;
            releaseGrabbedEntity();
        }
    }

    class AIBerserkRunningCombo extends EntityAIBase {
        private EntityLivingBase target;
        private int chargeTimer = 0;
        private boolean hasHit = false;

        @Override
        public boolean shouldExecute() {
            if (!isBerserk || getAttackState() != STATE_IDLE) return false;

            target = EntityVoidRipper.this.getAttackTarget();
            return target != null && EntityVoidRipper.this.getDistanceSq(target) > 16.0D
                    && EntityVoidRipper.this.getDistanceSq(target) < 64.0D;
        }

        @Override
        public void startExecuting() {
            setAttackState(STATE_BERSERK_RUNNING_COMBO);
            isRunningCombo = true;
            runningComboTimer = 0;
            chargeTimer = 0;
            hasHit = false;
            animationTick = RUNNING_COMBO_MAX_TIME;

            EntityVoidRipper.this.faceEntity(target, 30.0F, 30.0F);

            playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 1.5F, 1.5F);
        }

        @Override
        public void updateTask() {
            if (target == null || !target.isEntityAlive()) {
                isRunningCombo = false;
                return;
            }

            runningComboTimer++;
            chargeTimer++;

            double dx = target.posX - EntityVoidRipper.this.posX;
            double dz = target.posZ - EntityVoidRipper.this.posZ;
            float targetYaw = (float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;

            float yawDiff = MathHelper.wrapDegrees(targetYaw - EntityVoidRipper.this.rotationYaw);
            EntityVoidRipper.this.rotationYaw += yawDiff * 0.3F;
            EntityVoidRipper.this.rotationYawHead = EntityVoidRipper.this.rotationYaw;
            EntityVoidRipper.this.renderYawOffset = EntityVoidRipper.this.rotationYaw;

            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0) {
                float speed = Math.min(0.6F, 0.3F + chargeTimer * 0.01F);
                EntityVoidRipper.this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D + speed);

                EntityVoidRipper.this.motionX = (dx / dist) * speed;
                EntityVoidRipper.this.motionZ = (dz / dist) * speed;

                if (dist < 3.0 && runningComboTimer % 8 == 0) {
                    boolean useLeftHand = (runningComboTimer / 8) % 2 == 0;

                    float damage = (float)getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
                    damage *= 1.3F;

                    target.attackEntityFrom(DamageSource.causeMobDamage(EntityVoidRipper.this), damage);

                    target.motionX = (dx / dist) * 0.4;
                    target.motionY = 0.2;
                    target.motionZ = (dz / dist) * 0.4;

                    if (world instanceof WorldServer) {
                        WorldServer ws = (WorldServer) world;
                        ws.spawnParticle(useLeftHand ? EnumParticleTypes.SPELL_WITCH : EnumParticleTypes.FLAME,
                                target.posX, target.posY + target.height * 0.5, target.posZ,
                                15, 0.2, 0.2, 0.2, 0.05);
                    }

                    playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5F, 0.8F + rand.nextFloat() * 0.4F);
                    hasHit = true;
                }
            }

            if (world instanceof WorldServer && runningComboTimer % 3 == 0) {
                WorldServer ws = (WorldServer) world;
                for (int i = 0; i < 3; i++) {
                    ws.spawnParticle(EnumParticleTypes.FLAME,
                            posX - motionX * i * 0.5,
                            posY + 0.5,
                            posZ - motionZ * i * 0.5,
                            1, 0, 0, 0, 0.02);
                }
            }

            if (runningComboTimer >= RUNNING_COMBO_MAX_TIME || target.getDistance(EntityVoidRipper.this) > 10.0
                    || (hasHit && target.getDistance(EntityVoidRipper.this) < 2.0)) {
                isRunningCombo = false;
            }
        }

        @Override
        public boolean shouldContinueExecuting() {
            return isRunningCombo && target != null && target.isEntityAlive();
        }

        @Override
        public void resetTask() {
            isRunningCombo = false;
            runningComboTimer = 0;
            chargeTimer = 0;
            hasHit = false;
            setAttackState(STATE_IDLE);
            EntityVoidRipper.this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        }
    }

    class AIBerserkCombo extends EntityAIBase {
        private int comboStage = 0;
        private EntityLivingBase target;
        private int attackTimer = 0;

        @Override
        public boolean shouldExecute() {
            if (!isBerserk || getAttackState() != STATE_IDLE) return false;

            target = EntityVoidRipper.this.getAttackTarget();
            return target != null && EntityVoidRipper.this.getDistanceSq(target) < 16.0D;
        }

        @Override
        public void startExecuting() {
            setAttackState(STATE_BERSERK_COMBO);
            comboStage = 0;
            animationTick = 60;
            attackTimer = 0;

            EntityVoidRipper.this.faceEntity(target, 30.0F, 30.0F);
        }

        @Override
        public void updateTask() {
            if (target == null || !target.isEntityAlive()) return;

            EntityVoidRipper.this.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

            double dx = target.posX - EntityVoidRipper.this.posX;
            double dz = target.posZ - EntityVoidRipper.this.posZ;
            float targetYaw = (float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
            float yawDiff = Math.abs(MathHelper.wrapDegrees(EntityVoidRipper.this.rotationYaw - targetYaw));

            if (yawDiff > 45.0F) {
                EntityVoidRipper.this.rotationYaw = targetYaw;
                EntityVoidRipper.this.rotationYawHead = targetYaw;
                EntityVoidRipper.this.renderYawOffset = targetYaw;
                return;
            }

            attackTimer++;

            if (attackTimer % 10 == 0 && comboStage < 5) {
                if (EntityVoidRipper.this.getDistanceSq(target) < 9.0D) {
                    float damage = (float)getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
                    damage *= (1.0F + comboStage * 0.15F);

                    target.attackEntityFrom(DamageSource.causeMobDamage(EntityVoidRipper.this), damage);

                    double knockback = 0.2D + comboStage * 0.1D;
                    target.motionX = (dx / Math.sqrt(dx * dx + dz * dz)) * knockback;
                    target.motionY = 0.1D + comboStage * 0.05D;
                    target.motionZ = (dz / Math.sqrt(dx * dx + dz * dz)) * knockback;

                    playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                            1.0F + comboStage * 0.1F, 0.8F - comboStage * 0.05F);

                    if (!world.isRemote && world instanceof WorldServer) {
                        WorldServer ws = (WorldServer) world;
                        ws.spawnParticle(EnumParticleTypes.CRIT,
                                target.posX, target.posY + target.height * 0.5, target.posZ,
                                10 + comboStage * 3, 0.3, 0.3, 0.3, 0.1);
                    }
                }
                comboStage++;
            }
        }

        @Override
        public boolean shouldContinueExecuting() {
            return animationTick > 0 && target != null && target.isEntityAlive()
                    && EntityVoidRipper.this.getDistanceSq(target) < 25.0D;
        }

        @Override
        public void resetTask() {
            comboStage = 0;
            attackTimer = 0;
        }
    }

    class AILeftHandRapidAttack extends EntityAIBase {
        private EntityLivingBase target;
        private int attackTimer = 0;

        @Override
        public boolean shouldExecute() {
            if (leftHandCooldown > 0 || getAttackState() != STATE_IDLE) return false;

            target = EntityVoidRipper.this.getAttackTarget();
            return target != null && EntityVoidRipper.this.getDistanceSq(target) < 12.0D
                    && rand.nextInt(5) == 0;
        }

        @Override
        public void startExecuting() {
            setAttackState(STATE_LEFT_HAND);
            setAttackHand(HAND_LEFT);
            animationTick = LEFT_HAND_ANIMATION_TIME;
            leftHandCooldown = LEFT_HAND_COOLDOWN_TIME;
            attackTimer = 0;

            playSound(SoundEvents.EVOCATION_ILLAGER_CAST_SPELL, 1.5F, 0.8F);
        }

        @Override
        public void updateTask() {
            attackTimer++;

            if (attackTimer % 4 == 0 && target != null) {
                EntityVoidRipper.this.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

                if (EntityVoidRipper.this.getDistanceSq(target) > 9.0D) {
                    EntityVoidRipper.this.getMoveHelper().setMoveTo(
                            target.posX, target.posY, target.posZ, 1.5D);
                }
            }
        }

        @Override
        public boolean shouldContinueExecuting() {
            return animationTick > 0;
        }

        @Override
        public void resetTask() {
            attackTimer = 0;
        }
    }

    class AISlashAttack extends EntityAIBase {
        private int attackTime = 0;

        public AISlashAttack() {
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = EntityVoidRipper.this.getAttackTarget();
            return target != null && target.isEntityAlive()
                    && EntityVoidRipper.this.getDistanceSq(target) < 9.0D
                    && attackCooldown == 0
                    && getAttackState() == STATE_IDLE;
        }

        @Override
        public void startExecuting() {
            setAttackState(STATE_SLASH);
            attackCooldown = 25;
            animationTick = 30;
            attackTime = 10;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return animationTick > 0 && EntityVoidRipper.this.getAttackTarget() != null;
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = EntityVoidRipper.this.getAttackTarget();
            if (target != null) {
                EntityVoidRipper.this.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

                attackTime--;
                if (attackTime == 0) {
                    if (EntityVoidRipper.this.getDistanceSq(target) < 9.0D) {
                        float damage = (float)getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
                        target.attackEntityFrom(DamageSource.causeMobDamage(EntityVoidRipper.this), damage);
                        playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.5F);
                    }
                }
            }
        }
    }

    class AIDualSlamAttack extends EntityAIBase {
        private EntityLivingBase target;

        public AIDualSlamAttack() {
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (dualSlamCooldown > 0 || getAttackState() != STATE_IDLE) {
                return false;
            }

            target = EntityVoidRipper.this.getAttackTarget();
            if (target == null || !target.isEntityAlive()) {
                return false;
            }

            double distanceSq = EntityVoidRipper.this.getDistanceSq(target);
            return distanceSq < 16.0D && rand.nextInt(50) == 0;
        }

        @Override
        public void startExecuting() {
            performDualSlam();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return animationTick > 0;
        }
    }

    class AILaserAttack extends EntityAIBase {
        public AILaserAttack() {
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (laserCooldown > 0 || getAttackState() != STATE_IDLE) return false;

            EntityLivingBase target = EntityVoidRipper.this.getAttackTarget();
            if (target == null || !target.isEntityAlive()) return false;

            double distance = EntityVoidRipper.this.getDistanceSq(target);
            return distance > 64.0D && distance < LASER_RANGE * LASER_RANGE &&
                    EntityVoidRipper.this.canEntityBeSeen(target);
        }

        @Override
        public void startExecuting() {
            EntityLivingBase target = EntityVoidRipper.this.getAttackTarget();
            startLaserAttack(target);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return getLaserState() != LASER_IDLE;
        }

        @Override
        public void resetTask() {
            cancelLaser();
        }
    }

    private void performDualSlam() {
        setAttackState(STATE_DUAL_SLAM);
        setAttackHand(HAND_BOTH);
        animationTick = 40;
        dualSlamCooldown = 100;

        if (world instanceof WorldServer) {
            ((WorldServer)world).addScheduledTask(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                List<EntityLivingBase> targets = world.getEntitiesWithinAABB(
                        EntityLivingBase.class,
                        new AxisAlignedBB(posX - 4, posY - 2, posZ - 4,
                                posX + 4, posY + 3, posZ + 4));

                for (EntityLivingBase target : targets) {
                    if (target != this) {
                        float distance = target.getDistance(this);
                        float damage = 25.0F - (distance * 5.0F);

                        if (damage > 0) {
                            target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);

                            double dx = target.posX - this.posX;
                            double dz = target.posZ - this.posZ;
                            double dist = Math.sqrt(dx * dx + dz * dz);

                            if (dist > 0) {
                                target.motionX = (dx / dist) * 2.0D;
                                target.motionY = 1.2D;
                                target.motionZ = (dz / dist) * 2.0D;
                            }

                            target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 3));
                            target.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 60, 0));
                        }
                    }
                }

                playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 0.5F);

                if (world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;

                    for (int ring = 0; ring < 3; ring++) {
                        double radius = (ring + 1) * 1.5;
                        for (int i = 0; i < 32; i++) {
                            double angle = (Math.PI * 2) * i / 32;
                            double px = posX + Math.cos(angle) * radius;
                            double pz = posZ + Math.sin(angle) * radius;

                            ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                                    px, posY + 0.1, pz,
                                    1, 0, 0.5, 0, 0.1);
                        }
                    }

                    for (int i = 0; i < 50; i++) {
                        ws.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                                posX + (rand.nextDouble() - 0.5) * 4,
                                posY + 0.1,
                                posZ + (rand.nextDouble() - 0.5) * 4,
                                1, 0.5, 0.5, 0.5, 0.2,
                                7);
                    }
                }
            });
        }
    }

    // Getters and Setters
    public void setAttackState(int state) {
        this.dataManager.set(ATTACK_STATE, state);
    }

    public int getAttackState() {
        return this.dataManager.get(ATTACK_STATE);
    }

    public void setIsCharging(boolean charging) {
        this.dataManager.set(IS_CHARGING, charging);
    }

    public boolean getIsCharging() {
        return this.dataManager.get(IS_CHARGING);
    }

    public void setIsRunning(boolean running) {
        this.dataManager.set(IS_RUNNING, running);
    }

    public boolean getIsRunning() {
        return this.dataManager.get(IS_RUNNING);
    }

    public void setIsBerserk(boolean berserk) {
        this.dataManager.set(IS_BERSERK, berserk);
    }

    public boolean getIsBerserk() {
        return this.dataManager.get(IS_BERSERK);
    }

    public void setGrabbedEntityId(int id) {
        this.dataManager.set(GRABBED_ENTITY, id);
    }

    public int getGrabbedEntityId() {
        return this.dataManager.get(GRABBED_ENTITY);
    }

    public Entity getGrabbedEntity() {
        int id = getGrabbedEntityId();
        return id == -1 ? null : world.getEntityByID(id);
    }

    public void setLaserTargetId(int id) {
        this.dataManager.set(LASER_TARGET, id);
    }

    public int getLaserTargetId() {
        return this.dataManager.get(LASER_TARGET);
    }

    public EntityLivingBase getLaserTargetEntity() {
        int id = getLaserTargetId();
        if (id == -1) return null;
        Entity entity = world.getEntityByID(id);
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    public void setLaserState(int state) {
        this.dataManager.set(LASER_STATE, state);
    }

    public int getLaserState() {
        return this.dataManager.get(LASER_STATE);
    }

    public void setAttackHand(int hand) {
        this.dataManager.set(ATTACK_HAND, hand);
    }

    public int getAttackHand() {
        return this.dataManager.get(ATTACK_HAND);
    }

    // GeckoLib动画控制
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    @SuppressWarnings("unchecked")
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        final int state = getAttackState();

        switch (state) {
            case STATE_SLASH: {
                event.getController().setAnimation(
                        new AnimationBuilder().addAnimation(ANIM_SLASH, false)
                );
                return PlayState.CONTINUE;
            }

            case STATE_VOID_BURST: {
                event.getController().setAnimation(
                        new AnimationBuilder().addAnimation(ANIM_VOID_BURST, false)
                );
                return PlayState.CONTINUE;
            }

            case STATE_GRAB: {
                event.getController().setAnimation(
                        new AnimationBuilder().addAnimation(ANIM_GRAB_EXT, false)
                );
                return PlayState.CONTINUE;
            }

            case STATE_LASER: {
                final int ls = getLaserState();
                if (ls == LASER_WARNING) {
                    event.getController().setAnimation(
                            new AnimationBuilder().addAnimation(ANIM_LASER_WARNING, false)
                    );
                } else if (ls == LASER_CHARGING) {
                    event.getController().setAnimation(
                            new AnimationBuilder().addAnimation(ANIM_LASER_CHARGING, false)
                    );
                } else {
                    event.getController().setAnimation(
                            new AnimationBuilder().addAnimation(ANIM_LASER_FIRE, false)
                    );
                }
                return PlayState.CONTINUE;
            }

            case STATE_BERSERK_COMBO: {
                event.getController().setAnimation(
                        new AnimationBuilder().addAnimation(ANIM_BERSERK_COMBO, false)
                );
                return PlayState.CONTINUE;
            }

            case STATE_BERSERK_RUNNING_COMBO: {
                event.getController().setAnimation(
                        new AnimationBuilder().addAnimation(ANIM_BERSERK_RUNNING_COMBO, true)
                );
                return PlayState.CONTINUE;
            }

            case STATE_DUAL_SLAM: {
                event.getController().setAnimation(
                        new AnimationBuilder().addAnimation(ANIM_DUAL_SLAM, false)
                );
                return PlayState.CONTINUE;
            }

            case STATE_LEFT_HAND: {
                event.getController().setAnimation(
                        new AnimationBuilder().addAnimation(ANIM_LEFT_HAND, false)
                );
                return PlayState.CONTINUE;
            }

            default: {
                if (getIsBerserk()) {
                    if (event.isMoving()) {
                        event.getController().setAnimation(
                                new AnimationBuilder().addAnimation(ANIM_BERSERK_RUN, true)
                        );
                    } else {
                        event.getController().setAnimation(
                                new AnimationBuilder().addAnimation(ANIM_BERSERK_IDLE, true)
                        );
                    }
                } else if (event.isMoving()) {
                    if (getIsRunning()) {
                        event.getController().setAnimation(
                                new AnimationBuilder().addAnimation(ANIM_RUN, true)
                        );
                    } else {
                        event.getController().setAnimation(
                                new AnimationBuilder().addAnimation(ANIM_WALK, true)
                        );
                    }
                } else {
                    event.getController().setAnimation(
                            new AnimationBuilder().addAnimation(ANIM_IDLE, true)
                    );
                }
                return PlayState.CONTINUE;
            }
        }
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getIsBerserk() ? SoundEvents.ENTITY_ENDERDRAGON_AMBIENT :
                (this.rand.nextInt(2) == 0 ? SoundEvents.ENTITY_ENDERMEN_AMBIENT : SoundEvents.ENTITY_VEX_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENTITY_ENDERMEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ENDERDRAGON_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 1.5F;
    }

    @Override
    protected float getSoundPitch() {
        return super.getSoundPitch() * 0.8F;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("AttackState", getAttackState());
        compound.setBoolean("IsCharging", getIsCharging());
        compound.setBoolean("IsRunning", getIsRunning());
        compound.setBoolean("IsBerserk", isBerserk);
        compound.setInteger("BerserkTimer", berserkTimer);
        compound.setInteger("GrabbedEntity", getGrabbedEntityId());
        compound.setInteger("LaserTarget", getLaserTargetId());
        compound.setInteger("LaserState", getLaserState());
        compound.setInteger("LaserTimer", laserTimer);
        compound.setInteger("LaserCooldown", laserCooldown);
        compound.setInteger("AttackHand", getAttackHand());
        compound.setInteger("DualSlamCooldown", dualSlamCooldown);
        compound.setInteger("AttackCooldown", attackCooldown);
        compound.setInteger("SpecialAttackCooldown", specialAttackCooldown);
        compound.setInteger("VoidBurstCharge", voidBurstCharge);
        compound.setFloat("CoreRotation", coreRotation);
        compound.setInteger("AnimationTick", animationTick);
        compound.setInteger("CurrentAttackHand", currentAttackHand);
        compound.setInteger("GrabDuration", grabDuration);
        compound.setInteger("ComboAttackCounter", comboAttackCounter);
        compound.setInteger("VoidAuraTimer", voidAuraTimer);
        compound.setFloat("ShadowTrailAlpha", shadowTrailAlpha);
        compound.setInteger("LeftHandCooldown", leftHandCooldown);
        compound.setBoolean("IsRunningCombo", isRunningCombo);
        compound.setInteger("RunningComboTimer", runningComboTimer);
        compound.setInteger("LeftHandCombo", leftHandCombo);
        compound.setInteger("StuckTimer", stuckTimer);
        compound.setInteger("NoTargetTimer", noTargetTimer);

        // Gate 系统
        compound.setInteger("GateInvul", invulTicks);
        compound.setBoolean("GatePendingChunk", pendingChunk);
        compound.setFloat("GateFrozenHealth", frozenHealth);
        compound.setFloat("GateFrozenAbsorb", frozenAbsorb);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        setAttackState(compound.getInteger("AttackState"));
        setIsCharging(compound.getBoolean("IsCharging"));
        setIsRunning(compound.getBoolean("IsRunning"));
        isBerserk = compound.getBoolean("IsBerserk");
        berserkTimer = compound.getInteger("BerserkTimer");
        setGrabbedEntityId(compound.getInteger("GrabbedEntity"));
        setLaserTargetId(compound.getInteger("LaserTarget"));
        setLaserState(compound.getInteger("LaserState"));
        laserTimer = compound.getInteger("LaserTimer");
        laserCooldown = compound.getInteger("LaserCooldown");
        setAttackHand(compound.getInteger("AttackHand"));
        dualSlamCooldown = compound.getInteger("DualSlamCooldown");
        attackCooldown = compound.getInteger("AttackCooldown");
        specialAttackCooldown = compound.getInteger("SpecialAttackCooldown");
        voidBurstCharge = compound.getInteger("VoidBurstCharge");
        coreRotation = compound.getFloat("CoreRotation");
        animationTick = compound.getInteger("AnimationTick");
        currentAttackHand = compound.getInteger("CurrentAttackHand");
        grabDuration = compound.getInteger("GrabDuration");
        comboAttackCounter = compound.getInteger("ComboAttackCounter");
        voidAuraTimer = compound.getInteger("VoidAuraTimer");
        shadowTrailAlpha = compound.getFloat("ShadowTrailAlpha");
        leftHandCooldown = compound.getInteger("LeftHandCooldown");
        isRunningCombo = compound.getBoolean("IsRunningCombo");
        runningComboTimer = compound.getInteger("RunningComboTimer");
        leftHandCombo = compound.getInteger("LeftHandCombo");
        stuckTimer = compound.getInteger("StuckTimer");
        noTargetTimer = compound.getInteger("NoTargetTimer");

        // Gate 系统
        this.invulTicks = compound.getInteger("GateInvul");
        this.pendingChunk = compound.getBoolean("GatePendingChunk");
        this.frozenHealth = compound.getFloat("GateFrozenHealth");
        this.frozenAbsorb = compound.getFloat("GateFrozenAbsorb");

        if (isBerserk) {
            setIsBerserk(true);
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);

        if (!world.isRemote) {
            world.createExplosion(this, posX, posY, posZ, 0, false);

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;

                for (int i = 0; i < 200; i++) {
                    double angle = rand.nextDouble() * Math.PI * 2;
                    double radius = rand.nextDouble() * 5;
                    double px = posX + Math.cos(angle) * radius;
                    double py = posY + rand.nextDouble() * 3;
                    double pz = posZ + Math.sin(angle) * radius;

                    ws.spawnParticle(EnumParticleTypes.PORTAL,
                            px, py, pz,
                            1, 0, -0.5, 0, 0.1);
                }

                for (int i = 0; i < 100; i++) {
                    ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                            posX, posY + height * 0.5, posZ,
                            1,
                            (rand.nextDouble() - 0.5) * 3,
                            rand.nextDouble() * 2,
                            (rand.nextDouble() - 0.5) * 3,
                            0.2);
                }
            }

            world.playSound(null, posX, posY, posZ,
                    SoundEvents.ENTITY_ENDERDRAGON_DEATH,
                    getSoundCategory(), 2.0F, 0.8F);
            world.playSound(null, posX, posY, posZ,
                    SoundEvents.ENTITY_GENERIC_EXPLODE,
                    getSoundCategory(), 1.5F, 0.5F);
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void collideWithEntity(Entity entityIn) {
        if (!this.isRidingSameEntity(entityIn)) {
            if (!entityIn.noClip && !this.noClip) {
                double d0 = entityIn.posX - this.posX;
                double d1 = entityIn.posZ - this.posZ;
                double d2 = MathHelper.absMax(d0, d1);

                if (d2 >= 0.01D) {
                    d2 = (double)MathHelper.sqrt(d2);
                    d0 = d0 / d2;
                    d1 = d1 / d2;
                    double d3 = 1.0D / d2;

                    if (d3 > 1.0D) {
                        d3 = 1.0D;
                    }

                    d0 = d0 * d3;
                    d1 = d1 * d3;
                    d0 = d0 * 0.5D;
                    d1 = d1 * 0.5D;

                    if (entityIn instanceof EntityLivingBase) {
                        entityIn.addVelocity(d0 * 2, 0.2D, d1 * 2);
                    } else {
                        entityIn.addVelocity(d0, 0.0D, d1);
                    }
                }
            }
        }
    }

    @Override
    public boolean isNonBoss() {
        return false;
    }

    @Override
    public void addVelocity(double x, double y, double z) {
        super.addVelocity(x * 0.2, y * 0.5, z * 0.2);
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    @Override
    protected void dropLoot(boolean wasRecentlyHit, int lootingModifier, DamageSource source) {
        super.dropLoot(wasRecentlyHit, lootingModifier, source);
    }

    public float getCoreRotation() {
        return coreRotation;
    }

    public int getInvulnerabilityTimer() {
        return invulTicks;
    }

    public boolean isInvulnerable() {
        return invulTicks > 0;
    }

    // ========== 事件处理器 ==========

    @Mod.EventBusSubscriber(modid = MODID)
    public static class EventHooks {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onAttackPre(LivingAttackEvent e) {
            if (!(e.getEntityLiving() instanceof EntityVoidRipper)) return;
            EntityVoidRipper ripper = (EntityVoidRipper) e.getEntityLiving();

            if (ripper.isTrustedDamageSource(e.getSource())) {
                return;
            }

            if (ripper.isGateInvulnerable()) {
                e.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onHeal(LivingHealEvent e) {
            if (!(e.getEntityLiving() instanceof EntityVoidRipper)) return;
            EntityVoidRipper ripper = (EntityVoidRipper) e.getEntityLiving();

            if (ripper.isGateInvulnerable()) {
                e.setCanceled(true);
                e.setAmount(0F);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public static void onFinalDamage(LivingDamageEvent e) {
            if (!(e.getEntityLiving() instanceof EntityVoidRipper)) return;
            EntityVoidRipper ripper = (EntityVoidRipper) e.getEntityLiving();

            if (ripper.isTrustedDamageSource(e.getSource())) {
                return;
            }

            if (ripper.isGateInvulnerable()) {
                e.setCanceled(true);
                e.setAmount(0F);
            }
        }
    }
}