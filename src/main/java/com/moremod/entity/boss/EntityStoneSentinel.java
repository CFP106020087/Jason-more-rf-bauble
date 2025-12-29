package com.moremod.entity.boss;

import com.moremod.entity.projectile.EntityVoidBullet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.EntityList;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.*;

import static com.moremod.moremod.MODID;

public class EntityStoneSentinel extends EntityMob implements IAnimatable {

    // GeckoLib - 延迟初始化避免服务端崩溃
    private AnimationFactory factory = null;

    private AnimationFactory getOrCreateFactory() {
        if (factory == null) {
            factory = new AnimationFactory(this);
        }
        return factory;
    }

    private static final DataParameter<Integer> EYE_STATE = EntityDataManager.createKey(EntityStoneSentinel.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> IS_ANGRY  = EntityDataManager.createKey(EntityStoneSentinel.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> ATTACK_TYPE = EntityDataManager.createKey(EntityStoneSentinel.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> IS_DYING = EntityDataManager.createKey(EntityStoneSentinel.class, DataSerializers.BOOLEAN);

    private static final int EYE_CLOSED = 0;
    private static final int EYE_OPENING = 1;
    private static final int EYE_OPEN = 2;
    private static final int EYE_ANGRY = 3;
    private static final int EYE_CLOSING = 4;

    private static final int ATTACK_NONE = 0;
    private static final int ATTACK_SPIRAL_FIRE = 1;
    private static final int ATTACK_POISON = 2;
    private static final int ATTACK_EARTHQUAKE = 3;
    private static final int ATTACK_GRAVITY = 4;
    private static final int ATTACK_SUMMON = 5;

    // —— 难度参数 —— //
    private static final float MAX_DAMAGE_PERCENT = 0.1F; // 单次伤害上限 3%
    private static final int   INVULNERABILITY_TIME = 60;  // 受击无敌 60t

    private static final int    NO_LOS_PUNISH_TICKS = 60;  // 无视线惩罚阈值
    private static final double TRUE_DOT_PERCENT = 0.06;   // 无视线真伤占比

    private int eyeOpenTimer = 0;
    private int attackCooldown = 0;
    private int currentAttackTimer = 0;
    private float eyeYaw = 0;
    private float eyePitch = 0;
    private EntityPlayer trackedPlayer = null;
    private int deathAnimationTimer = 0;
    private float targetYaw = 0;
    private float currentYaw = 0;

    private List<AttackCircle> activeCircles = new ArrayList<>();
    private int spiralFireTimer = 0;
    private int poisonCloudTimer = 0;
    private int earthquakeTimer = 0;
    private List<EntityVoidBullet> gravityBullets = new ArrayList<>();

    private int invulnerabilityTimer = 0;

    // 阶段状态
    private boolean phase75 = false;
    private boolean phase50 = false;
    private boolean phase25 = false;
    private int     noLoSTimer = 0;
    private int     periodicShieldTimer = 0; // 25%后周期性全免疫

    // —— 召唤配置 —— //
    private static final ResourceLocation WEepingAngelRL = new ResourceLocation(MODID, "weeping_angel"); // 按你给的 id
    private static final int SUMMON_CAP_NEARBY = 4;     // 附近最大天使数
    private static final int SUMMON_TRY_SPAWN  = 2;     // 每次最多召 2 个
    private static final double SUMMON_RADIUS  = 6.0;   // 召唤半径

    private final BossInfoServer bossBar = new BossInfoServer(
            new net.minecraft.util.text.TextComponentString("Stone Sentinel"),
            BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS
    );
    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation(MODID, "entities/stone_sentinel");
    }
    public EntityStoneSentinel(World worldIn) {
        super(worldIn);
        this.setSize(4.0F, 6.0F);
        this.experienceValue = 1200;
        this.isImmuneToFire = true;

        this.setCustomNameTag("§5Stone Sentinel");
        this.setAlwaysRenderNameTag(false);

        this.bossBar.setDarkenSky(true);
        this.bossBar.setPlayEndBossMusic(false);
        this.bossBar.setCreateFog(true);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(EYE_STATE, EYE_CLOSED);
        this.dataManager.register(IS_ANGRY, false);
        this.dataManager.register(ATTACK_TYPE, ATTACK_NONE);
        this.dataManager.register(IS_DYING, false);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(1, new AIEyeTracking());
        this.tasks.addTask(2, new AISpiralFireAttack());
        this.tasks.addTask(3, new AIPoisonCloud());
        this.tasks.addTask(4, new AIEarthquake());
        this.tasks.addTask(5, new AIGravityWell());
        this.tasks.addTask(6, new AISummonMinions());
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(1200.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(40.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(30.0D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();

        // 不移动
        this.motionX = this.motionY = this.motionZ = 0;

        // 负面效果免疫（清除已上的坏效果，双保险）
        if (!this.world.isRemote && !this.getActivePotionEffects().isEmpty()) {
            // 先收集需要移除的药水，避免ConcurrentModificationException
            List<Potion> toRemove = new ArrayList<>();
            for (PotionEffect pe : this.getActivePotionEffects()) {
                if (pe.getPotion().isBadEffect()) {
                    toRemove.add(pe.getPotion());
                }
            }
            for (Potion p : toRemove) {
                this.removePotionEffect(p);
            }
        }

        // 朝向最近玩家
        EntityPlayer nearestPlayer = world.getClosestPlayerToEntity(this, 32.0D);
        if (nearestPlayer != null) {
            double dx = nearestPlayer.posX - this.posX;
            double dz = nearestPlayer.posZ - this.posZ;
            targetYaw = (float)(MathHelper.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
            float yawDiff = targetYaw - currentYaw;
            while (yawDiff > 180.0F) yawDiff -= 360.0F;
            while (yawDiff < -180.0F) yawDiff += 360.0F;
            currentYaw += yawDiff * 0.15F;
            this.rotationYaw = this.rotationYawHead = this.renderYawOffset = currentYaw;
            this.prevRotationYaw = this.prevRotationYawHead = this.prevRenderYawOffset = currentYaw;
        }

        // Boss 血条
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        if (getIsAngry()) this.bossBar.setColor(BossInfo.Color.RED);
        else if (getEyeState() == EYE_CLOSED) this.bossBar.setColor(BossInfo.Color.YELLOW);
        else this.bossBar.setColor(BossInfo.Color.PURPLE);

        // 死亡动画
        if (getIsDying()) { handleDeathAnimation(); return; }

        // 受击无敌
        if (invulnerabilityTimer > 0) invulnerabilityTimer--;

        // 阶段推进
        handlePhases();

        // 眼睛/攻击
        updateEyeState();
        updateAttackCircles();
        updateCurrentAttack();

        if (!world.isRemote && this.ticksExisted % 20 == 0) spawnAmbientParticles();

        if (attackCooldown > 0) attackCooldown--;

        // —— 已移除禁飞机制 —— //

        // 无视线惩罚
        checkAndPunishNoLOS(nearestPlayer);
    }

    private void handlePhases() {
        double hp = this.getHealth() / this.getMaxHealth();
        if (!phase75 && hp <= 0.75) {
            phase75 = true;
            setIsAngry(true);
            openEye();
            playSound(SoundEvents.ENTITY_WITHER_SPAWN, 2.5F, 0.7F);
        }
        if (!phase50 && hp <= 0.50) {
            phase50 = true;
            playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 2.5F, 0.7F);
        }
        if (!phase25 && hp <= 0.25) {
            phase25 = true;
            setIsAngry(true);
            setEyeState(EYE_ANGRY);
            periodicShieldTimer = 0;
            playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 2.8F, 0.6F);
        }

        // 25% 后周期性全免疫（40t开/60t关）
        if (phase25) {
            periodicShieldTimer++;
            if (periodicShieldTimer % 100 < 40) {
                invulnerabilityTimer = 2; // 持续刷新受击无敌帧
                if (world instanceof WorldServer && ticksExisted % 5 == 0) {
                    ((WorldServer)world).spawnParticle(EnumParticleTypes.SPELL_MOB_AMBIENT,
                            posX, posY + height*0.6, posZ, 8, 1, 0.4, 1, 0.1);
                }
            }
        }
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        this.bossBar.addPlayer(player);
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

    private void updateEyeState() {
        EntityPlayer nearestPlayer = world.getClosestPlayerToEntity(this, 32.0D);

        if (nearestPlayer != null && canEntityBeSeen(nearestPlayer)) {
            trackedPlayer = nearestPlayer;

            double dx = nearestPlayer.posX - this.posX;
            double dy = (nearestPlayer.posY + nearestPlayer.getEyeHeight()) - (this.posY + 4.5);
            double dz = nearestPlayer.posZ - this.posZ;
            double distance = MathHelper.sqrt(dx * dx + dz * dz);
            eyeYaw = (float)(MathHelper.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
            eyePitch = (float)(-(MathHelper.atan2(dy, distance) * 180.0D / Math.PI));

            if (this.getHealth() < this.getMaxHealth() * 0.5F && !getIsAngry()) setIsAngry(true);

            if ((getEyeState() == EYE_CLOSED || getEyeState() == EYE_CLOSING) && eyeOpenTimer == 0 && attackCooldown <= 0 && rand.nextInt(100) < 45) {
                openEye();
            } else if (!phase25 && (getEyeState() == EYE_OPEN || getEyeState() == EYE_ANGRY)) {
                if (eyeOpenTimer > 70 + rand.nextInt(30)) closeEye();
            }
        } else {
            if (!phase25 && getEyeState() != EYE_CLOSED && getEyeState() != EYE_CLOSING) closeEye();
            trackedPlayer = null;
        }

        if (getEyeState() == EYE_OPENING) {
            eyeOpenTimer++;
            if (eyeOpenTimer >= 25) setEyeState(getIsAngry() ? EYE_ANGRY : EYE_OPEN);
        } else if (getEyeState() == EYE_CLOSING) {
            eyeOpenTimer--;
            if (eyeOpenTimer <= 0) { setEyeState(EYE_CLOSED); eyeOpenTimer = 0; }
        } else if (getEyeState() == EYE_OPEN || getEyeState() == EYE_ANGRY) {
            eyeOpenTimer++;
        } else if (getEyeState() == EYE_CLOSED) {
            if (eyeOpenTimer > 0) eyeOpenTimer--;
        }
    }

    // —— 恢复：开眼环 50 个 —— //
    private void openEye() {
        setEyeState(EYE_OPENING);
        playSound(SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, 2.0F, 0.5F);
        if (!world.isRemote) {
            WorldServer ws = (WorldServer) world;
            for (int i = 0; i < 50; i++) { // 36 -> 50
                double ang = (Math.PI*2) * i / 50;
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        posX + Math.cos(ang)*3, posY + 4.5, posZ + Math.sin(ang)*3,
                        1, 0, 0.1, 0.0, 0);
            }
        }
    }
    private void closeEye() {
        setEyeState(EYE_CLOSING);
        playSound(SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, 2.0F, 0.5F);
    }

    private void updateAttackCircles() {
        activeCircles.removeIf(circle -> {
            circle.timer--;

            if (!world.isRemote) {
                if (circle.target != null && circle.target.isEntityAlive() && circle.timer > 20) {
                    double speed = 0.18 * (phase50 ? 1.25 : 1.0);
                    double dx = circle.target.posX - circle.x;
                    double dz = circle.target.posZ - circle.z;
                    double dist = Math.sqrt(dx*dx + dz*dz);
                    if (dist > 0.5) { circle.x += (dx/dist)*speed; circle.z += (dz/dist)*speed; }
                }
                if (circle.timer % 2 == 0) spawnCircleParticles(circle);
                if (circle.timer == 0) executeCircleAttack(circle);
            }
            return circle.timer <= 0;
        });
    }

    // —— 恢复：垂直光柱（每4点一柱）—— //
    private void spawnCircleParticles(AttackCircle circle) {
        if (!(world instanceof WorldServer)) return;
        if (circle.target == null || !circle.target.isEntityAlive()) return; // 空检查
        WorldServer ws = (WorldServer) world;
        int particles = 48; // 稍高密度
        double targetY = circle.target.posY; // 缓存避免重复访问
        for (int i = 0; i < particles; i++) {
            double angle = (Math.PI * 2) * i / particles;
            double px = circle.x + Math.cos(angle) * circle.radius;
            double pz = circle.z + Math.sin(angle) * circle.radius;

            // 地面轮廓
            ws.spawnParticle(EnumParticleTypes.REDSTONE,
                    px, targetY + 0.1, pz,
                    1, 1.0, 0, 0.0, 0);

            // 垂直光柱（5层）
            if (i % 4 == 0) {
                for (int h = 0; h < 5; h++) {
                    ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                            px, targetY + h * 0.5, pz,
                            1, 0, 0.1, 0.0, 0);
                }
            }
        }
    }

    private void executeCircleAttack(AttackCircle circle) {
        // 空检查：目标可能已死亡或为null
        double baseY = (circle.target != null && circle.target.isEntityAlive()) ? circle.target.posY : this.posY;

        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class,
                new AxisAlignedBB(circle.x - circle.radius, baseY - 1, circle.z - circle.radius,
                        circle.x + circle.radius, baseY + 3, circle.z + circle.radius));

        for (EntityLivingBase target : targets) {
            if (target != this) {
                double dx = target.posX - circle.x;
                double dz = target.posZ - circle.z;
                if (dx*dx + dz*dz <= circle.radius * circle.radius) applyCircleEffect(circle, target);
            }
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            for (int i = 0; i < 50; i++) {
                double ang = rand.nextDouble() * Math.PI * 2;
                double r = rand.nextDouble() * circle.radius;
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                        circle.x + Math.cos(ang) * r,
                        baseY + rand.nextDouble() * 2,
                        circle.z + Math.sin(ang) * r,
                        1, 0, 0.1, 0.0, 0);
            }
        }
        playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 1.0F);
    }

    private void applyCircleEffect(AttackCircle circle, EntityLivingBase target) {
        switch (circle.type) {
            case ATTACK_SPIRAL_FIRE:
                target.attackEntityFrom(DamageSource.causeMobDamage(this).setFireDamage(), 40.0F);
                target.setFire(6);
                break;
            case ATTACK_POISON:
                target.attackEntityFrom(DamageSource.causeMobDamage(this), 14.0F);
                target.addPotionEffect(new PotionEffect(MobEffects.POISON, 240, 2));
                target.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 120, 1));
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 1));
                break;
            case ATTACK_EARTHQUAKE:
                target.attackEntityFrom(DamageSource.causeMobDamage(this).setDamageIsAbsolute().setDamageBypassesArmor(), 18.0F);
                target.motionY = 0.9;
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 80, 2));
                break;
            case ATTACK_GRAVITY:
                double dx = circle.x - target.posX;
                double dz = circle.z - target.posZ;
                double dist = Math.sqrt(dx*dx + dz*dz);
                if (dist > 0) {
                    double mul = 0.55 * (phase50 ? 1.2 : 1.0);
                    target.motionX = (dx/dist) * mul;
                    target.motionZ = (dz/dist) * mul;
                }
                target.attackEntityFrom(DamageSource.causeMobDamage(this), 18.0F);
                break;
        }
    }

    private void updateCurrentAttack() {
        if (currentAttackTimer > 0) {
            currentAttackTimer--;
            switch (getAttackType()) {
                case ATTACK_SPIRAL_FIRE: updateSpiralFireAttack(); break;
                case ATTACK_POISON:      updatePoisonAttack(); break;
                case ATTACK_EARTHQUAKE:  updateEarthquakeAttack(); break;
                case ATTACK_GRAVITY:     updateGravityAttack(); break;
                case ATTACK_SUMMON:      updateSummonAttack(); break;
            }
            if (currentAttackTimer == 0) setAttackType(ATTACK_NONE);
        }
    }

    private void updateSpiralFireAttack() {
        if (trackedPlayer != null && spiralFireTimer < 90) {
            spiralFireTimer++;
            if (spiralFireTimer <= 18) {
                if (world instanceof WorldServer && spiralFireTimer % 3 == 0) {
                    WorldServer ws = (WorldServer) world;
                    int warningParticles = 36;
                    for (int i = 0; i < warningParticles; i++) {
                        double angle = (Math.PI * 2) * i / warningParticles;
                        double r = 3.0;
                        ws.spawnParticle(EnumParticleTypes.REDSTONE,
                                trackedPlayer.posX + Math.cos(angle)*r,
                                trackedPlayer.posY + 0.1,
                                trackedPlayer.posZ + Math.sin(angle)*r, 1, 1.0, 0.5, 0.0, 0);
                    }
                }
                if (spiralFireTimer == 10) playSound(SoundEvents.BLOCK_NOTE_BELL, 1.8F, 0.7F);
                return;
            }
            if (world instanceof WorldServer && spiralFireTimer % (phase50 ? 2 : 3) == 0) {
                WorldServer ws = (WorldServer) world;
                for (int layer = 0; layer < 2; layer++) {
                    double baseR = 1.8 + layer * 0.9;
                    int spirals = 2;
                    for (int s = 0; s < spirals; s++) {
                        double angle = (spiralFireTimer * 0.18) + (s * Math.PI*2/spirals) + (layer * Math.PI/5);
                        double h = (spiralFireTimer - 18) * 0.12;
                        double px = trackedPlayer.posX + Math.cos(angle) * baseR;
                        double py = trackedPlayer.posY + h;
                        double pz = trackedPlayer.posZ + Math.sin(angle) * baseR;
                        ws.spawnParticle(EnumParticleTypes.FLAME, px, py, pz, 2, 0.1,0.1,0.1,0.01);
                        if (spiralFireTimer % 6 == 0) ws.spawnParticle(EnumParticleTypes.LAVA, px, py, pz, 1, 0.1,0.1,0.1,0);
                    }
                }
                int ground = 24; // 地面火圈略增
                for (int i = 0; i < ground; i++) {
                    double angle = (Math.PI * 2) * i / ground;
                    double r = 2.2;
                    ws.spawnParticle(EnumParticleTypes.FLAME,
                            trackedPlayer.posX + Math.cos(angle)*r,
                            trackedPlayer.posY + 0.1,
                            trackedPlayer.posZ + Math.sin(angle)*r, 1, 0,0.02,0.0,0);
                }
            }
            if (spiralFireTimer > 18 && spiralFireTimer % 12 == 0) {
                List<EntityLivingBase> t = world.getEntitiesWithinAABB(EntityLivingBase.class,
                        new AxisAlignedBB(trackedPlayer.posX - 2.2, trackedPlayer.posY, trackedPlayer.posZ - 2.2,
                                trackedPlayer.posX + 2.2, trackedPlayer.posY + 8, trackedPlayer.posZ + 2.2));
                for (EntityLivingBase e : t) {
                    if (e != this && e instanceof EntityPlayer) {
                        e.attackEntityFrom(DamageSource.causeMobDamage(this).setFireDamage(), 5.0F);
                        e.setFire(3);
                    }
                }
            }
            if (spiralFireTimer >= 90) spiralFireTimer = 0;
        }
    }

    private void updatePoisonAttack() {
        if (world instanceof WorldServer && poisonCloudTimer % 8 == 0) {
            WorldServer ws = (WorldServer) world;
            for (int i = 0; i < 24; i++) {
                double angle = rand.nextDouble()*Math.PI*2;
                double r = rand.nextDouble()*8;
                ws.spawnParticle(EnumParticleTypes.SPELL_MOB,
                        posX + Math.cos(angle)*r, posY + rand.nextDouble()*3, posZ + Math.sin(angle)*r,
                        1, 0,0.1,0.0,0);
            }
            List<EntityLivingBase> t = world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(8.5));
            for (EntityLivingBase e : t) {
                if (e != this) {
                    e.addPotionEffect(new PotionEffect(MobEffects.POISON, 60, 2));
                    e.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 80, 1));
                }
            }
        }
        poisonCloudTimer++;
    }

    // —— 恢复：地震多层 + CRIT闪 + 随机喷裂 —— //
    private void updateEarthquakeAttack() {
        if (earthquakeTimer % (phase50 ? 8 : 10) == 0) {
            int wave = earthquakeTimer / (phase50 ? 8 : 10);
            double radius = wave * 2.7;

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int layer = 0; layer < 2; layer++) {
                    int particles = 80; // 恢复到原始级别
                    for (int i = 0; i < particles; i++) {
                        double angle = (Math.PI * 2) * i / particles;
                        double r = radius + layer * 0.3;

                        ws.spawnParticle(EnumParticleTypes.CLOUD,
                                posX + Math.cos(angle) * r,
                                posY + 0.1,
                                posZ + Math.sin(angle) * r,
                                2, 0, 0.2, 0, 0.05);

                        ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                                posX + Math.cos(angle) * r,
                                posY + 0.5 + layer * 0.2,
                                posZ + Math.sin(angle) * r,
                                1, 0.1, 0.3, 0.1, 0);

                        if (i % 4 == 0) {
                            ws.spawnParticle(EnumParticleTypes.CRIT,
                                    posX + Math.cos(angle) * r,
                                    posY + 1.0,
                                    posZ + Math.sin(angle) * r,
                                    1, 0, 0.5, 0.0, 0);
                        }
                    }
                }

                // 随机喷裂
                for (int i = 0; i < 20; i++) {
                    double randAngle = rand.nextDouble() * Math.PI * 2;
                    double randRadius = radius + (rand.nextDouble() - 0.5) * 2;
                    ws.spawnParticle(EnumParticleTypes.CLOUD,
                            posX + Math.cos(randAngle) * randRadius,
                            posY + rand.nextDouble() * 2,
                            posZ + Math.sin(randAngle) * randRadius,
                            1, 0, 0.8, 0, 0.1);
                }
            }

            List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class,
                    new AxisAlignedBB(posX - radius - 1, posY - 2, posZ - radius - 1,
                            posX + radius + 1, posY + 5, posZ + radius + 1));

            for (EntityLivingBase target : targets) {
                if (target != this) {
                    double dist = target.getDistance(this);
                    if (Math.abs(dist - radius) < 2.0) {
                        target.attackEntityFrom(DamageSource.causeMobDamage(this).setDamageIsAbsolute().setDamageBypassesArmor(), 17.0F);
                        target.motionY = 0.9;
                    }
                }
            }

            playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 0.5F);
        }
        earthquakeTimer++;
    }

    private void updateGravityAttack() {
        if (currentAttackTimer == 90 && !world.isRemote) {
            EntityVoidBullet core = new EntityVoidBullet(world, this);
            core.setPosition(posX, posY + 3, posZ);
            core.setDamage(24.0F);
            core.setParticleColor(2);
            core.motionX = core.motionY = core.motionZ = 0;
            core.noClip = true;
            world.spawnEntity(core);
            gravityBullets.add(core);

            for (int i = 0; i < 3; i++) {
                EntityVoidBullet orb = new EntityVoidBullet(world, this);
                double angle = (Math.PI * 2) * i / 3;
                double R = 4;
                orb.setPosition(posX + Math.cos(angle)*R, posY + 3, posZ + Math.sin(angle)*R);
                orb.setDamage(12.0F);
                orb.setParticleColor(2);
                orb.motionX = -Math.sin(angle) * 0.24;
                orb.motionY = 0;
                orb.motionZ =  Math.cos(angle) * 0.24;
                world.spawnEntity(orb);
                gravityBullets.add(orb);
            }
        }

        Vec3d center = new Vec3d(posX, posY + 2, posZ);
        List<EntityLivingBase> tg = world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(12.5));
        for (EntityLivingBase e : tg) {
            if (e != this) {
                Vec3d v = center.subtract(e.posX, e.posY, e.posZ);
                double d = v.length();
                if (d > 3 && d < 12.5) {
                    double scale = 0.34 * (1 - d / 12.5) * (phase50 ? 1.25 : 1.0);
                    Vec3d pull = v.normalize().scale(scale);
                    e.motionX += pull.x; e.motionY += pull.y; e.motionZ += pull.z;
                }
            }
        }

        if (!gravityBullets.isEmpty() && currentAttackTimer > 10) {
            for (EntityVoidBullet b : gravityBullets) {
                if (!b.isDead && b.getDistance(this) < 5) {
                    b.motionX *= 0.88; b.motionY = 0; b.motionZ *= 0.88;
                }
            }
        }

        if (world instanceof WorldServer && currentAttackTimer % 2 == 0) {
            WorldServer ws = (WorldServer) world;
            for (int ring = 0; ring < 3; ring++) {
                double rr = 4 + ring * 2.6;
                int p = 60;
                for (int i = 0; i < p; i++) {
                    double ang = (Math.PI*2)*i/p;
                    ws.spawnParticle(EnumParticleTypes.PORTAL,
                            posX + Math.cos(ang)*rr, posY + 0.1, posZ + Math.sin(ang)*rr, 1, 0,0,0.0,0);
                }
            }

            // 中央随机上升/下坠的传送粒子
            for (int i = 0; i < 15; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double radius = rand.nextDouble() * 3;
                double height = rand.nextDouble() * 5;
                ws.spawnParticle(EnumParticleTypes.PORTAL,
                        posX + Math.cos(angle) * radius,
                        posY + height,
                        posZ + Math.sin(angle) * radius,
                        1, -Math.cos(angle) * 0.2, -0.1, -Math.sin(angle) * 0.2, 0);
            }
        }
        gravityBullets.removeIf(b -> b.isDead);
    }

    private void updateSummonAttack() {
        if (currentAttackTimer == 50 && !world.isRemote) {
            // 召唤 moremod:weeping_amgel（限制数量）
            int existing = countEntitiesNearby(WEepingAngelRL, 16.0);
            int canSpawn = Math.max(0, SUMMON_CAP_NEARBY - existing);
            int toSpawn = Math.min(SUMMON_TRY_SPAWN, canSpawn);

            for (int i = 0; i < toSpawn; i++) {
                double angle = (Math.PI * 2) * (i + rand.nextDouble()) / Math.max(1, toSpawn);
                double dist = SUMMON_RADIUS + rand.nextDouble() * 2.0;
                double x = posX + Math.cos(angle) * dist;
                double z = posZ + Math.sin(angle) * dist;
                double y = posY + 1;

                // 找地面安全落点
                BlockPos spawnPos = new BlockPos(x, y, z);
                spawnPos = findGround(spawnPos, 6);

                if (spawnPos != null && world.isAirBlock(spawnPos) && world.isAirBlock(spawnPos.up())) {
                    Entity entity = EntityList.createEntityByIDFromName(WEepingAngelRL, world);
                    if (entity != null) {
                        entity.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                        world.spawnEntity(entity);

                        if (world instanceof WorldServer) {
                            WorldServer ws = (WorldServer) world;
                            for (int j = 0; j < 30; j++) {
                                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                                        entity.posX + (rand.nextDouble()-0.5),
                                        entity.posY + rand.nextDouble() * 2,
                                        entity.posZ + (rand.nextDouble()-0.5),
                                        1, 0, 0.1, 0.0, 0);
                            }
                        }
                    }
                }
            }
            playSound(SoundEvents.EVOCATION_ILLAGER_PREPARE_SUMMON, 2.0F, 1.0F);
        }
    }

    private BlockPos findGround(BlockPos start, int maxDown) {
        BlockPos pos = start;
        for (int i = 0; i < maxDown; i++) {
            if (!world.isAirBlock(pos.down())) return pos;
            pos = pos.down();
        }
        return null;
    }

    private int countEntitiesNearby(ResourceLocation rl, double radius) {
        AxisAlignedBB box = new AxisAlignedBB(posX - radius, posY - 8, posZ - radius, posX + radius, posY + 8, posZ + radius);
        int cnt = 0;
        for (Entity e : world.getEntitiesWithinAABB(Entity.class, box)) {
            ResourceLocation key = EntityList.getKey(e.getClass());
            if (key != null && key.equals(rl)) cnt++;
        }
        return cnt;
    }

    private void handleDeathAnimation() {
        deathAnimationTimer++;
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            int particles = Math.min(deathAnimationTimer * 2, 120);
            for (int i = 0; i < particles; i++) {
                double a = rand.nextDouble()*Math.PI*2;
                double r = rand.nextDouble()*3;
                double h = rand.nextDouble()*this.height;
                ws.spawnParticle(EnumParticleTypes.CLOUD,
                        posX + (rand.nextDouble()-0.5)*width,
                        posY + h,
                        posZ + (rand.nextDouble()-0.5)*width,
                        1, Math.cos(a)*0.2, 0.3, Math.sin(a)*0.2, 0);
            }
            if (deathAnimationTimer >= 40) {
                for (int i = 0; i < 240; i++) {
                    double a = rand.nextDouble()*Math.PI*2;
                    double p = rand.nextDouble()*Math.PI - Math.PI/2;
                    double vx = Math.cos(a)*Math.cos(p)*0.5;
                    double vy = Math.sin(p)*0.5 + 0.3;
                    double vz = Math.sin(a)*Math.cos(p)*0.5;
                    ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, posX, posY + this.height/2, posZ, 1, vx, vy, vz, 0);
                }
                playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 4.0F, 0.5F);
                this.setDead();
            }
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        setIsDying(true);
        setEyeState(EYE_CLOSING);
    }

    private void spawnAmbientParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;
        for (int i = 0; i < 3; i++) {
            ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    posX + (rand.nextDouble()-0.5)*width,
                    posY + rand.nextDouble()*this.height,
                    posZ + (rand.nextDouble()-0.5)*width,
                    1, 0, -0.1, 0, 0.0);
        }
        if (getIsAngry()) {
            ws.spawnParticle(EnumParticleTypes.FLAME,
                    posX + (rand.nextDouble()-0.5)*width,
                    posY + this.height,
                    posZ + (rand.nextDouble()-0.5)*width,
                    1, 0, 0.1, 0.0, 0);
        }
    }

    // —— 反爆发/反投射/反伤 —— //
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (invulnerabilityTimer > 0) return false;

        // 非睁眼期对投射物完全免疫
        if ((getEyeState() == EYE_CLOSED || getEyeState() == EYE_CLOSING || (!phase25 && getEyeState() != EYE_OPEN && getEyeState() != EYE_ANGRY))
                && source.isProjectile()) return false;

        // 闭眼：90%减伤 + 50%真·魔法反伤
        if (!phase25 && getEyeState() == EYE_CLOSED) {
            float reduced = amount * 0.1F;
            boolean res = super.attackEntityFrom(source, reduced);
            reflectDamageToAttacker(source, amount * 0.5F);
            return res;
        }
        return super.attackEntityFrom(source, amount);
    }

    private void reflectDamageToAttacker(DamageSource src, float trueMagic) {
        Entity attacker = src.getTrueSource();
        if (attacker instanceof EntityLivingBase) {
            ((EntityLivingBase)attacker).attackEntityFrom(DamageSource.MAGIC.setDamageBypassesArmor().setDamageIsAbsolute(), trueMagic);
        }
    }

    @Override
    protected void damageEntity(DamageSource damageSrc, float damageAmount) {
        if (invulnerabilityTimer > 0) return;
        float maxDamage = (float)(this.getMaxHealth() * MAX_DAMAGE_PERCENT);
        if (damageAmount > maxDamage) damageAmount = maxDamage;
        super.damageEntity(damageSrc, damageAmount);
        invulnerabilityTimer = INVULNERABILITY_TIME;
    }

    @Override public boolean canBePushed() { return false; }
    @Override public void move(net.minecraft.entity.MoverType type, double x, double y, double z) {}
    @Override public boolean isNonBoss() { return false; }

    // —— 负面效果免疫：仅允许正面Buff —— //
    @Override
    public boolean isPotionApplicable(PotionEffect effect) {
        Potion p = effect.getPotion();
        return !p.isBadEffect();
    }

    // —— AI —— //
    class AIEyeTracking extends EntityAIBase {
        @Override public boolean shouldExecute() { return trackedPlayer != null && getEyeState() != EYE_CLOSED; }
        @Override public void updateTask() {}
    }
    class AISpiralFireAttack extends EntityAIBase {
        @Override public boolean shouldExecute() {
            return trackedPlayer != null && attackCooldown <= 0 && getEyeState() == EYE_OPEN && rand.nextInt(100) < (phase50 ? 32 : 22);
        }
        @Override public void startExecuting() {
            setAttackType(ATTACK_SPIRAL_FIRE);
            currentAttackTimer = 110;
            attackCooldown = phase50 ? 170 : 200;
            spiralFireTimer = 0;

            AttackCircle c = new AttackCircle();
            c.x = trackedPlayer.posX; c.z = trackedPlayer.posZ; c.target = trackedPlayer;
            c.radius = 3.0; c.timer = 40; c.type = ATTACK_SPIRAL_FIRE;
            activeCircles.add(c);
            playSound(SoundEvents.ENTITY_BLAZE_AMBIENT, 2.0F, 0.5F);
        }
    }
    class AIPoisonCloud extends EntityAIBase {
        @Override public boolean shouldExecute() {
            return trackedPlayer != null && attackCooldown <= 0 && getEyeState() != EYE_CLOSED && rand.nextInt(100) < (phase50 ? 28 : 18);
        }
        @Override public void startExecuting() {
            setAttackType(ATTACK_POISON);
            currentAttackTimer = 110;
            attackCooldown = phase50 ? 150 : 170;
            poisonCloudTimer = 0;

            for (int i = 0; i < 3; i++) {
                AttackCircle c = new AttackCircle();
                c.x = trackedPlayer.posX + (rand.nextDouble()-0.5)*6;
                c.z = trackedPlayer.posZ + (rand.nextDouble()-0.5)*6;
                c.target = trackedPlayer; c.radius = 3.0;
                c.timer = 40 + i*18; c.type = ATTACK_POISON;
                activeCircles.add(c);
            }
        }
    }
    class AIEarthquake extends EntityAIBase {
        @Override public boolean shouldExecute() {
            return trackedPlayer != null && attackCooldown <= 0 && getIsAngry() && rand.nextInt(100) < (phase50 ? 32 : 24);
        }
        @Override public void startExecuting() {
            setAttackType(ATTACK_EARTHQUAKE);
            currentAttackTimer = 90;
            attackCooldown = phase50 ? 160 : 180;
            earthquakeTimer = 0;
            playSound(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 2.0F, 0.5F);
        }
    }
    class AIGravityWell extends EntityAIBase {
        @Override public boolean shouldExecute() {
            return trackedPlayer != null && attackCooldown <= 0 && getIsAngry() && rand.nextInt(100) < (phase50 ? 28 : 20);
        }
        @Override public void startExecuting() {
            setAttackType(ATTACK_GRAVITY);
            currentAttackTimer = 110;
            attackCooldown = phase50 ? 180 : 200;
            AttackCircle c = new AttackCircle();
            c.x = posX; c.z = posZ; c.target = trackedPlayer; c.radius = 8.2; c.timer = 90; c.type = ATTACK_GRAVITY;
            activeCircles.add(c);
            playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 2.0F, 0.5F);
        }
    }
    class AISummonMinions extends EntityAIBase {
        @Override public boolean shouldExecute() {
            return trackedPlayer != null && attackCooldown <= 0 && getHealth() < getMaxHealth()*0.35F && rand.nextInt(100) < 22;
        }
        @Override public void startExecuting() {
            setAttackType(ATTACK_SUMMON);
            currentAttackTimer = 70;
            attackCooldown = 260;
            playSound(SoundEvents.EVOCATION_ILLAGER_PREPARE_SUMMON, 2.0F, 1.0F);
        }
    }

    private class AttackCircle {
        double x, z;
        double radius;
        EntityPlayer target;
        int timer;
        int type;
    }

    public int  getEyeState() { return this.dataManager.get(EYE_STATE); }
    public void setEyeState(int s) { this.dataManager.set(EYE_STATE, s); }
    public boolean getIsAngry() { return this.dataManager.get(IS_ANGRY); }
    public void setIsAngry(boolean a) { this.dataManager.set(IS_ANGRY, a); }
    public int  getAttackType() { return this.dataManager.get(ATTACK_TYPE); }
    public void setAttackType(int t) { this.dataManager.set(ATTACK_TYPE, t); }
    public boolean getIsDying() { return this.dataManager.get(IS_DYING); }
    public void setIsDying(boolean d) { this.dataManager.set(IS_DYING, d); }

    // —— 无视线惩罚：碎石风暴（真伤%）+ 可选清理软方块 —— //
    private void checkAndPunishNoLOS(EntityPlayer nearest) {
        boolean hasLOS = nearest != null && canEntityBeSeen(nearest);
        if (!hasLOS) noLoSTimer++; else noLoSTimer = 0;

        if (noLoSTimer >= NO_LOS_PUNISH_TICKS && !world.isRemote) {
            noLoSTimer = 0;
            List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(24));
            for (EntityPlayer p : players) {
                float maxHp = p.getMaxHealth();
                p.attackEntityFrom(DamageSource.MAGIC.setDamageBypassesArmor().setDamageIsAbsolute(), Math.max(2.0F, (float)(maxHp * TRUE_DOT_PERCENT)));
                p.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 60, 2));
                p.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 1));
            }
            if (world instanceof WorldServer) {
                ((WorldServer)world).spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, posX, posY+1, posZ, 40, 2,1,2.0, 0);
            }
            playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.2F, 0.7F);

            // 可选：清理近身低硬度方块，避免永久卡位（谨慎启用）
            // cleanSoftBlocksAround(3);
        }
    }

    private void cleanSoftBlocksAround(int radius) {
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++)
            for (int y = 0; y <= 3; y++)
                for (int z = -radius; z <= radius; z++) {
                    mp.setPos((int)posX + x, (int)posY + y, (int)posZ + z);
                    if (world.isAirBlock(mp)) continue;
                    if (world.getBlockState(mp).getBlock() == Blocks.DIRT
                            || world.getBlockState(mp).getBlock() == Blocks.SAND
                            || world.getBlockState(mp).getBlock() == Blocks.GRAVEL) {
                        world.destroyBlock(mp, false);
                    }
                }
    }

    // —— GeckoLib —— //
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    private PlayState predicate(AnimationEvent<EntityStoneSentinel> event) {
        if (getIsDying()) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.stone.death", false));
        } else if (getEyeState() == EYE_OPENING) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.stone.open_eye", false));
        } else if (getEyeState() == EYE_CLOSING) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.stone.close_eye", false));
        } else if (getEyeState() == EYE_OPEN) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.stone.open_idle", true));
        } else if (getEyeState() == EYE_ANGRY) {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.stone.angry_eye", true));
        } else {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.stone.closed_idle", true));
        }
        return PlayState.CONTINUE;
    }
    @Override public AnimationFactory getFactory() { return getOrCreateFactory(); }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setInteger("EyeState", getEyeState());
        nbt.setBoolean("IsAngry", getIsAngry());
        nbt.setInteger("AttackType", getAttackType());
        nbt.setInteger("InvulnerabilityTimer", invulnerabilityTimer);
        nbt.setBoolean("IsDying", getIsDying());
        nbt.setBoolean("Phase75", phase75);
        nbt.setBoolean("Phase50", phase50);
        nbt.setBoolean("Phase25", phase25);
        nbt.setInteger("PeriodicShieldTimer", periodicShieldTimer);
        nbt.setInteger("NoLoSTimer", noLoSTimer);
    }
    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        setEyeState(nbt.getInteger("EyeState"));
        setIsAngry(nbt.getBoolean("IsAngry"));
        setAttackType(nbt.getInteger("AttackType"));
        invulnerabilityTimer = nbt.getInteger("InvulnerabilityTimer");
        setIsDying(nbt.getBoolean("IsDying"));
        phase75 = nbt.getBoolean("Phase75");
        phase50 = nbt.getBoolean("Phase50");
        phase25 = nbt.getBoolean("Phase25");
        periodicShieldTimer = nbt.getInteger("PeriodicShieldTimer");
        noLoSTimer = nbt.getInteger("NoLoSTimer");
    }
}
