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
    private static final DataParameter<Integer> HIT_COUNT = EntityDataManager.createKey(
            EntityCursedKnight.class, DataSerializers.VARINT);

    // 動畫狀態常量
    private static final int ANIM_IDLE = 0;
    private static final int ANIM_CALL = 1;
    private static final int ANIM_MOVE = 2;
    private static final int ANIM_ATTACK = 3;
    private static final int ANIM_SKILL = 4;

    // 戰鬥狀態
    private int attackCooldown = 0;
    private int skillCooldown = 0;
    private int callAnimationTimer = 0;
    private boolean hasTriggeredCall = false;
    private int currentPhase = 0; // 0=正常, 1=憤怒, 2=狂暴
    // 戰鬥狀態
    private int spawnAnimationTimer = 0;
    private boolean hasSpawned = false;
    // 限傷機制
    private static final float MAX_DAMAGE_PERCENT = 0.15F;
    private static final int INVULNERABILITY_TIME = 30;
    private int invulnerabilityTimer = 0;
    // AI相關
    private EntityPlayer targetPlayer = null;
    private BlockPos homePosition;
    private int wanderTimer = 0;
    @Override
    protected ResourceLocation getLootTable() {return new ResourceLocation(MODID, "entities/curse_knight");}

    public EntityCursedKnight(World worldIn) {
        super(worldIn);
        this.setSize(0.6F, 1.95F);
        this.experienceValue = 100;
        this.homePosition = this.getPosition();

        // 生成時觸發召喚動畫
        if (!worldIn.isRemote) {
            this.spawnAnimationTimer = 64; // 3.2秒動畫
            this.callAnimationTimer = 64;
            this.hasTriggeredCall = true;
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(ANIMATION_STATE, ANIM_IDLE);
        this.dataManager.register(HIT_COUNT, 0);
    }

    @Override
    protected void initEntityAI() {
        // 優先級系統
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
                // 如果不移動則播放idle

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
        if (invulnerabilityTimer > 0) invulnerabilityTimer--;

        // 生成動畫處理
        if (spawnAnimationTimer > 0) {
            spawnAnimationTimer--;

            // 出生動畫期間不索敵
            this.setAttackTarget(null);
            this.getNavigator().clearPath();

            if (spawnAnimationTimer == 63 && !world.isRemote) {
                // 開始播放召喚動畫
                this.dataManager.set(ANIMATION_STATE, ANIM_CALL);
                triggerCallAnimation();
            }

            if (spawnAnimationTimer == 0) {
                hasSpawned = true;
                this.dataManager.set(ANIMATION_STATE, ANIM_IDLE);
            }

            return; // 出生動畫期間不執行其他邏輯
        }

        // 第一次看到玩家時觸發召喚動畫（如果不是出生動畫）
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

    private void triggerCallAnimation() {
        hasTriggeredCall = true;
        callAnimationTimer = 64; // 3.2秒動畫
        this.dataManager.set(ANIMATION_STATE, ANIM_CALL);

        // 播放召喚音效
        this.playSound(SoundEvents.EVOCATION_ILLAGER_PREPARE_ATTACK, 1.0F, 0.5F);

        // 召喚特效
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
        // 提升速度
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.32D);

        // 視覺效果
        if (!world.isRemote) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.VILLAGER_ANGRY,
                    this.posX, this.posY + this.height, this.posZ,
                    20, 0.5, 0.5, 0.5, 0);
        }
    }

    private void enterRageMode() {
        // 大幅提升屬性
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(12.0D);

        // 狂暴特效
        if (!world.isRemote) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.FLAME,
                    this.posX, this.posY, this.posZ,
                    30, 0.5, 1.0, 0.5, 0.1);
        }
    }

    // ========== 自定義AI任務 ==========

    // 攻擊AI
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
                    knight.spawnAnimationTimer <= 0; // 出生動畫期間不攻擊
        }

        @Override
        public void startExecuting() {
            knight.dataManager.set(ANIMATION_STATE, ANIM_ATTACK);
            animationTimer = 14; // 0.58秒動畫
            knight.attackCooldown = 30;
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = knight.getAttackTarget();
            if (target != null) {
                knight.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

                // 在動畫中間造成傷害
                if (animationTimer == 7) {
                    if (knight.getDistanceSq(target) < 9.0D) {
                        target.attackEntityFrom(DamageSource.causeMobDamage(knight),
                                (float)knight.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
                        // 普通攻擊不詛咒玩家
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

    // 技能AI
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
                    knight.spawnAnimationTimer <= 0 && // 出生動畫期間不使用技能
                    (knight.currentPhase > 0 || knight.rand.nextFloat() < 0.1F);
        }

        @Override
        public void startExecuting() {
            knight.dataManager.set(ANIMATION_STATE, ANIM_SKILL);
            animationTimer = 30; // 1.25秒動畫
            knight.skillCooldown = 200;
        }

        @Override
        public void updateTask() {
            // 旋轉攻擊
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

                // 應用詛咒效果 - 扣除最大生命值的5%
                float maxHealth = player.getMaxHealth();
                float curseDamage = maxHealth * 0.05F;

                // 應用詛咒
                if (!player.world.isRemote) {
                    // 檢查玩家是否已被詛咒
                    NBTTagCompound playerData = player.getEntityData();
                    if (!playerData.hasKey("CursedKnight_Cursed")) {
                        playerData.setBoolean("CursedKnight_Cursed", true);
                        playerData.setFloat("CursedKnight_MaxHealthReduction", curseDamage);

                        // 減少最大生命值
                        player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH)
                                .setBaseValue(maxHealth - curseDamage);

                        // 如果當前生命值超過新的最大值，調整它
                        if (player.getHealth() > player.getMaxHealth()) {
                            player.setHealth(player.getMaxHealth());
                        }

                        player.sendMessage(new TextComponentString(
                                "§4§l你被詛咒了！§r§c最大生命值減少" +
                                        String.format("%.1f", curseDamage) + "點 (5%)"
                        ));
                        player.sendMessage(new TextComponentString(
                                "§e使用純淨聖水可以解除詛咒"
                        ));
                    }
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

                    // 黑色煙霧粒子
                    ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                            knight.posX + Math.cos(angle) * radius,
                            knight.posY + 1.0,
                            knight.posZ + Math.sin(angle) * radius,
                            1, 0.0D, 0.0D, 0.0D, 0);

                    // 詛咒粒子
                    ws.spawnParticle(EnumParticleTypes.SPELL_MOB,
                            knight.posX + Math.cos(angle) * radius,
                            knight.posY + 1.2,
                            knight.posZ + Math.sin(angle) * radius,
                            1, 0.1, 0, 0.1, 0);
                }

                // 中心爆發的黑色粒子
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

                // 斬擊效果
                ws.spawnParticle(EnumParticleTypes.SWEEP_ATTACK,
                        knight.posX, knight.posY + 1, knight.posZ,
                        5, 2.0D, 0.5D, 2.0D, 0);
            }

            // 音效
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

    // 追擊AI
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
                    knight.spawnAnimationTimer <= 0; // 出生動畫期間不追擊
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

    // 巡邏AI
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
                    knight.spawnAnimationTimer <= 0; // 出生動畫期間不巡邏
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

    // 尋找目標AI
    static class AICursedKnightFindTarget extends EntityAINearestAttackableTarget<EntityPlayer> {
        private final EntityCursedKnight knight;

        public AICursedKnightFindTarget(EntityCursedKnight knight) {
            super(knight, EntityPlayer.class, true);
            this.knight = knight;
        }

        @Override
        public boolean shouldExecute() {
            return knight.callAnimationTimer <= 0 &&
                    knight.spawnAnimationTimer <= 0 && // 出生動畫期間不尋找目標
                    super.shouldExecute();
        }
    }

    // ========== 輔助方法 ==========

    private void spawnAmbientParticles() {
        if (!world.isRemote) {
            WorldServer ws = (WorldServer) world;

            // 根據階段產生不同粒子
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

            // 常駐的詛咒粒子
            ws.spawnParticle(EnumParticleTypes.SPELL_MOB,
                    this.posX + (rand.nextDouble() - 0.5) * 2.0,
                    this.posY + rand.nextDouble() * height,
                    this.posZ + (rand.nextDouble() - 0.5) * 2.0,
                    1, 0, 0.1D, 0.0D, 0);
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (callAnimationTimer > 0 || spawnAnimationTimer > 0) {
            return false; // 召喚/生成動畫期間無敵
        }

        // 無敵時間檢查
        if (invulnerabilityTimer > 0) {
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        posX, posY + height * 0.5, posZ,
                        10, 0.5, 0.5, 0.5, 0.1);
            }
            playSound(SoundEvents.ITEM_SHIELD_BLOCK, 1.0F, 1.5F);
            return false;
        }

        if (source.getTrueSource() instanceof EntityPlayer) {
            return super.attackEntityFrom(source, amount);
        }

        return false;
    }

    @Override
    protected void damageEntity(DamageSource damageSrc, float damageAmount) {
        // 無敵時間內完全免疫
        if (invulnerabilityTimer > 0) {
            return;
        }

        // 限傷機制
        if (damageSrc.getTrueSource() instanceof EntityPlayer) {
            float maxDamage = this.getMaxHealth() * MAX_DAMAGE_PERCENT;
            if (damageAmount > maxDamage) {
                damageAmount = maxDamage;

                // 限傷觸發特效
                if (world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;
                    for (int i = 0; i < 20; i++) {
                        double angle = (Math.PI * 2) * i / 20;
                        double px = posX + Math.cos(angle) * 2;
                        double pz = posZ + Math.sin(angle) * 2;
                        ws.spawnParticle(EnumParticleTypes.PORTAL,
                                px, posY + height * 0.5, pz,
                                1, 0, 0.5, 0, 0.1);
                    }
                }
                playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1.5F, 0.5F);
            }

            // 更新擊中次數
            int currentHits = this.dataManager.get(HIT_COUNT);
            currentHits++;
            this.dataManager.set(HIT_COUNT, currentHits);

            // 固定傷害系統
            float actualDamage = 20.0F;

            if (currentHits >= 5) {
                this.setHealth(0);
                super.damageEntity(damageSrc, this.getMaxHealth());
            } else {
                super.damageEntity(damageSrc, actualDamage);
                invulnerabilityTimer = INVULNERABILITY_TIME;

                EntityPlayer player = (EntityPlayer) damageSrc.getTrueSource();
                player.sendStatusMessage(new TextComponentString(
                        "§c詛咒騎士受到攻擊！ (" + currentHits + "/5)"
                ), true);

                if (world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;
                    ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                            posX, posY + height * 0.5, posZ,
                            30, 1, 1, 1, 0.2);
                }
            }
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("HitCount", this.dataManager.get(HIT_COUNT));
        compound.setInteger("AnimationState", this.dataManager.get(ANIMATION_STATE));
        compound.setInteger("Phase", currentPhase);
        compound.setBoolean("HasSpawned", hasSpawned);
        compound.setInteger("AttackCooldown", attackCooldown);
        compound.setInteger("SkillCooldown", skillCooldown);
        compound.setInteger("SpawnAnimationTimer", spawnAnimationTimer);
        compound.setInteger("InvulnerabilityTimer", invulnerabilityTimer);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.dataManager.set(HIT_COUNT, compound.getInteger("HitCount"));
        this.dataManager.set(ANIMATION_STATE, compound.getInteger("AnimationState"));
        this.currentPhase = compound.getInteger("Phase");
        this.hasSpawned = compound.getBoolean("HasSpawned");
        this.attackCooldown = compound.getInteger("AttackCooldown");
        this.skillCooldown = compound.getInteger("SkillCooldown");
        this.spawnAnimationTimer = compound.getInteger("SpawnAnimationTimer");
        this.invulnerabilityTimer = compound.getInteger("InvulnerabilityTimer");
    }
}