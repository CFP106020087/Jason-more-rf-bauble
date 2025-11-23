package com.moremod.entity;

import com.moremod.curse.CurseManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
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

public class EntityCursedKnight extends EntityMob implements IAnimatable {

    private AnimationFactory factory = new AnimationFactory(this);

    // 動畫狀態
    private static final DataParameter<Integer> ANIMATION_STATE = EntityDataManager.createKey(
            EntityCursedKnight.class, DataSerializers.VARINT);

    // 動畫狀態常量
    private static final int ANIM_IDLE = 0;
    private static final int ANIM_CALL = 1;
    private static final int ANIM_MOVE = 2;
    private static final int ANIM_ATTACK = 3;
    private static final int ANIM_SKILL = 4;

    // ========== Gate 限伤系统（参考 Riftwarden）==========
    private static final String CHUNK_ID = MODID + ".cursed_knight_chunk";
    public static final DamageSource CURSE_CHUNK = new DamageSource(CHUNK_ID)
            .setDamageIsAbsolute()
            .setDamageBypassesArmor();

    private int invulTicks = 0;
    private boolean pendingChunk = false;
    private boolean applyingChunk = false;
    private float frozenHealth = -1F;
    private float frozenAbsorb = -1F;
    private int gateOpenFxCooldown = 0;
    private static final int INVUL_TICKS_BASE = 30; // 1.5秒基础无敌

    // 戰鬥狀態
    private int attackCooldown = 0;
    private int skillCooldown = 0;
    private int callAnimationTimer = 0;
    private boolean hasTriggeredCall = false;
    private int currentPhase = 0; // 0=正常, 1=憤怒, 2=狂暴
    private int spawnAnimationTimer = 0;
    private boolean hasSpawned = false;

    // AI相關
    private EntityPlayer targetPlayer = null;
    private BlockPos homePosition;
    private int wanderTimer = 0;

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation(MODID, "entities/curse_knight");
    }

    public EntityCursedKnight(World worldIn) {
        super(worldIn);
        this.setSize(0.6F, 1.95F);
        this.experienceValue = 100;
        this.homePosition = this.getPosition();

        // 生成時觸發召喚動畫
        if (!worldIn.isRemote) {
            this.spawnAnimationTimer = 64;
            this.callAnimationTimer = 64;
            this.hasTriggeredCall = true;
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(ANIMATION_STATE, ANIM_IDLE);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new AICursedKnightAttack(this));
        this.tasks.addTask(2, new AICursedKnightSkill(this));
        this.tasks.addTask(3, new AICursedKnightChase(this));
        this.tasks.addTask(4, new AICursedKnightPatrol(this));
        this.tasks.addTask(5, new EntityAIWatchClosest(this, EntityPlayer.class, 16.0F));
        this.tasks.addTask(6, new EntityAILookIdle(this));

        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        this.targetTasks.addTask(2, new AICursedKnightFindTarget(this));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.28D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(8.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(10.0D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0D);
    }

    // ========== GeckoLib 動畫控制 ==========
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationEvent<EntityCursedKnight> event) {
        int state = this.dataManager.get(ANIMATION_STATE);

        switch(state) {
            case ANIM_CALL:
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation("animation.skeleton.call", false));
                return PlayState.CONTINUE;

            case ANIM_ATTACK:
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation("animation.skeleton.attack", false));
                return PlayState.CONTINUE;

            case ANIM_SKILL:
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation("animation.skeleton.skill", false));
                return PlayState.CONTINUE;

            case ANIM_MOVE:
                if (event.isMoving()) {
                    event.getController().setAnimation(new AnimationBuilder()
                            .addAnimation("animation.skeleton.move", true));
                    return PlayState.CONTINUE;
                }

            case ANIM_IDLE:
            default:
                event.getController().setAnimation(new AnimationBuilder()
                        .addAnimation("animation.skeleton.idle", true));
                return PlayState.CONTINUE;
        }
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    // ========== 戰鬥邏輯 ==========
    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();

        // 更新冷卻
        if (attackCooldown > 0) attackCooldown--;
        if (skillCooldown > 0) skillCooldown--;
        if (callAnimationTimer > 0) callAnimationTimer--;
        if (gateOpenFxCooldown > 0) gateOpenFxCooldown--;

        // Gate 系统处理
        handleInvulnerability();

        // 生成動畫處理
        if (spawnAnimationTimer > 0) {
            spawnAnimationTimer--;
            this.setAttackTarget(null);
            this.getNavigator().clearPath();

            if (spawnAnimationTimer == 63 && !world.isRemote) {
                this.dataManager.set(ANIMATION_STATE, ANIM_CALL);
                triggerCallAnimation();
            }

            if (spawnAnimationTimer == 0) {
                hasSpawned = true;
                this.dataManager.set(ANIMATION_STATE, ANIM_IDLE);
            }
            return;
        }

        // 第一次看到玩家時觸發召喚動畫
        if (!hasTriggeredCall && this.getAttackTarget() != null && !world.isRemote) {
            triggerCallAnimation();
        }

        // 更新階段
        updatePhase();

        // 根據移動狀態更新動畫
        if (!world.isRemote) {
            if (this.motionX * this.motionX + this.motionZ * this.motionZ > 0.0001D) {
                if (this.dataManager.get(ANIMATION_STATE) == ANIM_IDLE) {
                    this.dataManager.set(ANIMATION_STATE, ANIM_MOVE);
                }
            } else if (this.dataManager.get(ANIMATION_STATE) == ANIM_MOVE) {
                this.dataManager.set(ANIMATION_STATE, ANIM_IDLE);
            }
        }

        // 環境粒子效果
        if (!world.isRemote && this.ticksExisted % 10 == 0) {
            spawnAmbientParticles();
        }
    }

    // ========== Gate 系统核心方法 ==========

    private void handleInvulnerability() {
        if (this.invulTicks > 0) {
            this.invulTicks--;

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
                        super.attackEntityFrom(CURSE_CHUNK, dmg);
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
    }

    public float getChunkSize() {
        return 20.0F; // 累积伤害阈值
    }

    public boolean isGateInvulnerable() {
        return invulTicks > 0;
    }

    public void openGateAndFreeze(boolean scheduleChunk) {
        int base = INVUL_TICKS_BASE - currentPhase * 5;
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

    public boolean isTrustedChunkSource(DamageSource src) {
        return src != null && CHUNK_ID.equals(src.getDamageType());
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

    // ========== 伤害处理（完全重写）==========

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (callAnimationTimer > 0 || spawnAnimationTimer > 0) {
            return false; // 召喚/生成動畫期間無敵
        }

        // Gate 无敌检查
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

        // 只接受玩家伤害
        if (!(source.getTrueSource() instanceof EntityPlayer)) {
            return false;
        }

        return super.attackEntityFrom(source, amount);
    }

    @Override
    protected void damageEntity(DamageSource damageSrc, float damageAmount) {
        // 信任伤害源直接通过
        if (isTrustedChunkSource(damageSrc)) {
            super.damageEntity(damageSrc, damageAmount);
            return;
        }

        // Gate 无敌期间不处理
        if (this.isGateInvulnerable()) {
            return;
        }

        // 只接受玩家伤害
        if (!(damageSrc.getTrueSource() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) damageSrc.getTrueSource();

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
        float healthPercent = (this.getHealth() / this.getMaxHealth()) * 100;
        player.sendStatusMessage(new TextComponentString(
                String.format("§c诅咒骑士 §7| §e%.0f%%", healthPercent)
        ), true);

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
                player.sendMessage(new TextComponentString("§6§l强力一击！将在无敌结束后造成额外伤害！"));
            }
        }
    }

    // ========== 原有方法保持不变 ==========

    private void triggerCallAnimation() {
        hasTriggeredCall = true;
        callAnimationTimer = 64;
        this.dataManager.set(ANIMATION_STATE, ANIM_CALL);
        this.playSound(SoundEvents.EVOCATION_ILLAGER_PREPARE_ATTACK, 1.0F, 0.5F);

        if (!world.isRemote) {
            WorldServer ws = (WorldServer) world;
            for (int i = 0; i < 50; i++) {
                double angle = (Math.PI * 2 * i) / 50;
                double radius = 3.0;
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        this.posX + Math.cos(angle) * radius,
                        this.posY + 0.1,
                        this.posZ + Math.sin(angle) * radius,
                        1, 0, 0.5D, 0.0D, 0);
            }
        }
    }

    private void updatePhase() {
        float healthPercent = this.getHealth() / this.getMaxHealth();

        if (healthPercent <= 0.3F && currentPhase < 2) {
            currentPhase = 2;
            enterRageMode();
        } else if (healthPercent <= 0.6F && currentPhase < 1) {
            currentPhase = 1;
            enterAngryMode();
        }
    }

    private void enterAngryMode() {
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.32D);
        if (!world.isRemote) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.VILLAGER_ANGRY,
                    this.posX, this.posY + this.height, this.posZ,
                    20, 0.5, 0.5, 0.5, 0);
        }
    }

    private void enterRageMode() {
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(12.0D);
        if (!world.isRemote) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.FLAME,
                    this.posX, this.posY, this.posZ,
                    30, 0.5, 1.0, 0.5, 0.1);
        }
    }

    // ========== AI 類（保持不变）==========

    static class AICursedKnightAttack extends EntityAIBase {
        private final EntityCursedKnight knight;
        private int animationTimer = 0;

        public AICursedKnightAttack(EntityCursedKnight knight) {
            this.knight = knight;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = knight.getAttackTarget();
            return target != null &&
                    target.isEntityAlive() &&
                    knight.getDistanceSq(target) < 9.0D &&
                    knight.attackCooldown <= 0 &&
                    knight.callAnimationTimer <= 0 &&
                    knight.spawnAnimationTimer <= 0;
        }

        @Override
        public void startExecuting() {
            knight.dataManager.set(ANIMATION_STATE, ANIM_ATTACK);
            animationTimer = 14;
            knight.attackCooldown = 30;
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = knight.getAttackTarget();
            if (target != null) {
                knight.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

                if (animationTimer == 7) {
                    if (knight.getDistanceSq(target) < 9.0D) {
                        target.attackEntityFrom(DamageSource.causeMobDamage(knight),
                                (float)knight.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
                    }
                }
            }
            animationTimer--;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return animationTimer > 0;
        }

        @Override
        public void resetTask() {
            knight.dataManager.set(ANIMATION_STATE, ANIM_IDLE);
        }
    }

    // ========== 技能 AI（使用统一诅咒系统）==========

    static class AICursedKnightSkill extends EntityAIBase {
        private final EntityCursedKnight knight;
        private int animationTimer = 0;

        public AICursedKnightSkill(EntityCursedKnight knight) {
            this.knight = knight;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = knight.getAttackTarget();
            return target != null &&
                    target.isEntityAlive() &&
                    knight.skillCooldown <= 0 &&
                    knight.callAnimationTimer <= 0 &&
                    knight.spawnAnimationTimer <= 0 &&
                    (knight.currentPhase > 0 || knight.rand.nextFloat() < 0.1F);
        }

        @Override
        public void startExecuting() {
            knight.dataManager.set(ANIMATION_STATE, ANIM_SKILL);
            animationTimer = 30;
            knight.skillCooldown = 200;
        }

        @Override
        public void updateTask() {
            if (animationTimer == 20) {
                performSpinAttack();
            }
            animationTimer--;
        }

        private void performSpinAttack() {
            List<EntityPlayer> nearbyPlayers = knight.world.getEntitiesWithinAABB(
                    EntityPlayer.class,
                    knight.getEntityBoundingBox().grow(5.0D));

            for (EntityPlayer player : nearbyPlayers) {
                // 基礎傷害
                player.attackEntityFrom(DamageSource.causeMobDamage(knight), 10.0F);

                // ✅ 使用统一的诅咒系统
                if (!player.world.isRemote) {
                    CurseManager.applyCurse(player);
                }

                // 擊退
                double dx = player.posX - knight.posX;
                double dz = player.posZ - knight.posZ;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0) {
                    player.addVelocity(dx / dist * 0.8, 0.3, dz / dist * 0.8);
                }
            }

            // 黑色粒子特效
            if (!knight.world.isRemote) {
                WorldServer ws = (WorldServer) knight.world;

                // 旋轉斬擊軌跡
                for (int i = 0; i < 360; i += 10) {
                    double angle = Math.toRadians(i);
                    double radius = 3.0;

                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                            knight.posX + Math.cos(angle) * radius,
                            knight.posY + 1.0,
                            knight.posZ + Math.sin(angle) * radius,
                            1, 0.0D, 0.0D, 0.0D, 0);

                    ws.spawnParticle(EnumParticleTypes.SPELL_MOB,
                            knight.posX + Math.cos(angle) * radius,
                            knight.posY + 1.2,
                            knight.posZ + Math.sin(angle) * radius,
                            1, 0.1, 0, 0.1, 0);
                }

                // 中心爆發
                for (int i = 0; i < 30; i++) {
                    double offsetX = (knight.rand.nextDouble() - 0.5) * 3;
                    double offsetY = knight.rand.nextDouble() * 2;
                    double offsetZ = (knight.rand.nextDouble() - 0.5) * 3;

                    ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                            knight.posX + offsetX,
                            knight.posY + offsetY,
                            knight.posZ + offsetZ,
                            1, offsetX * 0.1, 0.1, offsetZ * 0.1, 0);
                }

                // 地面衝擊波
                for (int ring = 1; ring <= 3; ring++) {
                    for (int i = 0; i < 20 * ring; i++) {
                        double angle = (Math.PI * 2 * i) / (20 * ring);
                        double radius = ring * 1.5;

                        ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                                knight.posX + Math.cos(angle) * radius,
                                knight.posY + 0.1,
                                knight.posZ + Math.sin(angle) * radius,
                                1, 0, 0, 0.0D, 0);
                    }
                }

                ws.spawnParticle(EnumParticleTypes.SWEEP_ATTACK,
                        knight.posX, knight.posY + 1, knight.posZ,
                        5, 2.0D, 0.5D, 2.0D, 0);
            }

            knight.playSound(SoundEvents.ENTITY_WITHER_SHOOT, 1.0F, 0.5F);
            knight.playSound(SoundEvents.ENTITY_ENDERDRAGON_FLAP, 1.0F, 0.8F);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return animationTimer > 0;
        }

        @Override
        public void resetTask() {
            knight.dataManager.set(ANIMATION_STATE, ANIM_IDLE);
        }
    }

    // 追擊 AI
    static class AICursedKnightChase extends EntityAIBase {
        private final EntityCursedKnight knight;

        public AICursedKnightChase(EntityCursedKnight knight) {
            this.knight = knight;
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = knight.getAttackTarget();
            return target != null &&
                    target.isEntityAlive() &&
                    knight.getDistanceSq(target) > 9.0D &&
                    knight.callAnimationTimer <= 0 &&
                    knight.spawnAnimationTimer <= 0;
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = knight.getAttackTarget();
            if (target != null) {
                knight.getNavigator().tryMoveToEntityLiving(target,
                        knight.currentPhase == 2 ? 1.0D : 0.8D);
                knight.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            }
        }
    }

    // 巡邏 AI
    static class AICursedKnightPatrol extends EntityAIBase {
        private final EntityCursedKnight knight;
        private double targetX, targetY, targetZ;

        public AICursedKnightPatrol(EntityCursedKnight knight) {
            this.knight = knight;
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return knight.getAttackTarget() == null &&
                    knight.rand.nextFloat() < 0.02F &&
                    knight.spawnAnimationTimer <= 0;
        }

        @Override
        public void startExecuting() {
            BlockPos pos = knight.homePosition.add(
                    knight.rand.nextInt(16) - 8,
                    0,
                    knight.rand.nextInt(16) - 8);

            targetX = pos.getX();
            targetY = knight.world.getHeight(pos).getY();
            targetZ = pos.getZ();

            knight.getNavigator().tryMoveToXYZ(targetX, targetY, targetZ, 0.6D);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !knight.getNavigator().noPath() && knight.getAttackTarget() == null;
        }
    }

    // 尋找目標 AI
    static class AICursedKnightFindTarget extends EntityAINearestAttackableTarget<EntityPlayer> {
        private final EntityCursedKnight knight;

        public AICursedKnightFindTarget(EntityCursedKnight knight) {
            super(knight, EntityPlayer.class, true);
            this.knight = knight;
        }

        @Override
        public boolean shouldExecute() {
            return knight.callAnimationTimer <= 0 &&
                    knight.spawnAnimationTimer <= 0 &&
                    super.shouldExecute();
        }
    }

    // ========== 輔助方法 ==========

    private void spawnAmbientParticles() {
        if (!world.isRemote) {
            WorldServer ws = (WorldServer) world;

            if (currentPhase == 2) {
                ws.spawnParticle(EnumParticleTypes.FLAME,
                        this.posX + (rand.nextDouble() - 0.5) * 1.5,
                        this.posY + rand.nextDouble() * height,
                        this.posZ + (rand.nextDouble() - 0.5) * 1.5,
                        1, 0.0D, 0.05D, 0.0D, 0);
            } else if (currentPhase == 1) {
                ws.spawnParticle(EnumParticleTypes.VILLAGER_ANGRY,
                        this.posX, this.posY + height, this.posZ,
                        1, 0.0D, 0.0D, 0.0D, 0);
            }

            ws.spawnParticle(EnumParticleTypes.SPELL_MOB,
                    this.posX + (rand.nextDouble() - 0.5) * 2.0,
                    this.posY + rand.nextDouble() * height,
                    this.posZ + (rand.nextDouble() - 0.5) * 2.0,
                    1, 0, 0.1D, 0.0D, 0);
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("AnimationState", this.dataManager.get(ANIMATION_STATE));
        compound.setInteger("Phase", currentPhase);
        compound.setBoolean("HasSpawned", hasSpawned);
        compound.setInteger("AttackCooldown", attackCooldown);
        compound.setInteger("SkillCooldown", skillCooldown);
        compound.setInteger("SpawnAnimationTimer", spawnAnimationTimer);

        // Gate 系统
        compound.setInteger("GateInvul", invulTicks);
        compound.setBoolean("GatePendingChunk", pendingChunk);
        compound.setFloat("GateFrozenHealth", frozenHealth);
        compound.setFloat("GateFrozenAbsorb", frozenAbsorb);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.dataManager.set(ANIMATION_STATE, compound.getInteger("AnimationState"));
        this.currentPhase = compound.getInteger("Phase");
        this.hasSpawned = compound.getBoolean("HasSpawned");
        this.attackCooldown = compound.getInteger("AttackCooldown");
        this.skillCooldown = compound.getInteger("SkillCooldown");
        this.spawnAnimationTimer = compound.getInteger("SpawnAnimationTimer");

        // Gate 系统
        this.invulTicks = compound.getInteger("GateInvul");
        this.pendingChunk = compound.getBoolean("GatePendingChunk");
        this.frozenHealth = compound.getFloat("GateFrozenHealth");
        this.frozenAbsorb = compound.getFloat("GateFrozenAbsorb");
    }

    // ========== 事件处理器（参考 Riftwarden）==========

    @Mod.EventBusSubscriber(modid = MODID)
    public static class EventHooks {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onAttackPre(LivingAttackEvent e) {
            if (!(e.getEntityLiving() instanceof EntityCursedKnight)) return;
            EntityCursedKnight knight = (EntityCursedKnight) e.getEntityLiving();
            if (knight.isGateInvulnerable() && !knight.isTrustedChunkSource(e.getSource())) {
                e.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onHeal(LivingHealEvent e) {
            if (!(e.getEntityLiving() instanceof EntityCursedKnight)) return;
            EntityCursedKnight knight = (EntityCursedKnight) e.getEntityLiving();
            if (knight.isGateInvulnerable()) {
                e.setCanceled(true);
                e.setAmount(0F);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public static void onFinalDamage(LivingDamageEvent e) {
            if (!(e.getEntityLiving() instanceof EntityCursedKnight)) return;
            EntityCursedKnight knight = (EntityCursedKnight) e.getEntityLiving();

            if (knight.isTrustedChunkSource(e.getSource())) {
                return; // 信任伤害源通过
            }

            if (knight.isGateInvulnerable()) {
                e.setCanceled(true);
                e.setAmount(0F);
            }
        }
    }
}