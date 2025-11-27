package com.moremod.entity.boss.riftwarden;

import com.moremod.util.BossBlockTracker;
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
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

/**
 * EntityRiftwarden - 虚空守望者Boss
 * 
 * 重构版本 - 模块化架构
 * 
 * 组件：
 * - RiftwardenStateManager: 状态管理
 * - RiftwardenMovementController: 物理化运动
 * - RiftwardenCombatController: 战斗逻辑
 * - RiftwardenAnimationController: 动画同步
 * - RiftwardenEnvironmentController: 环境控制
 */
public class EntityRiftwarden extends EntityMob implements IAnimatable {
    
    private static final String MODID = "moremod";
    
    // ========== 基础属性 ==========
    private static final float MAX_HP = 4000F;
    private static final double MOVE_SPEED = 0.30D;
    
    // ========== 数据同步参数 ==========
    private static final DataParameter<Integer> STATE_ID = EntityDataManager.createKey(
        EntityRiftwarden.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> ATTACK_ID = EntityDataManager.createKey(
        EntityRiftwarden.class, DataSerializers.VARINT);
    private static final DataParameter<Float> STATE_PROGRESS = EntityDataManager.createKey(
        EntityRiftwarden.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> PHASE = EntityDataManager.createKey(
        EntityRiftwarden.class, DataSerializers.VARINT);
    private static final DataParameter<Float> BALL_ROTATION = EntityDataManager.createKey(
        EntityRiftwarden.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> GATE_PROGRESS = EntityDataManager.createKey(
        EntityRiftwarden.class, DataSerializers.FLOAT);
    
    // ========== 组件 ==========
    private final AnimationFactory animationFactory = new AnimationFactory(this);
    private RiftwardenStateManager stateManager;
    private RiftwardenMovementController movementController;
    private RiftwardenCombatController combatController;
    private RiftwardenAnimationController animationController;
    private RiftwardenEnvironmentController environmentController;
    
    // ========== Boss系统 ==========
    private final BossInfoServer bossBar = new BossInfoServer(
        this.getDisplayName(), BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS);
    private int phase = 0;
    
    // ========== 限伤系统 ==========
    private int gateInvulTicks = 0;
    private float gateHealthSnapshot = -1F;
    private float accumulatedDamage = 0F;
    private static final int GATE_DURATION_BASE = 40;
    private static final int MAX_CHUNKS_PER_CYCLE = 2;
    
    // ========== 召唤系统 ==========
    private int summonCooldown = 200;
    private int minionCap = 3;
    private boolean[] wavesFired = new boolean[3];
    
    // ========== 杂项 ==========
    private float ballRotation = 0F;
    
    public EntityRiftwarden(World worldIn) {
        super(worldIn);
        this.setSize(0.9F, 2.8F);
        this.experienceValue = 500;
        this.isImmuneToFire = true;
        
        // 初始化组件
        this.stateManager = new RiftwardenStateManager(this);
        this.movementController = new RiftwardenMovementController(this);
        this.moveHelper = this.movementController;  // 关键：注册到EntityLiving的moveHelper
        this.combatController = new RiftwardenCombatController(this);
        this.animationController = new RiftwardenAnimationController(this);
        this.environmentController = new RiftwardenEnvironmentController(this);
        
        // Boss条设置
        bossBar.setCreateFog(false);
        bossBar.setDarkenSky(false);
        bossBar.setPlayEndBossMusic(false);
    }
    
    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(STATE_ID, RiftwardenState.IDLE.getId());
        this.dataManager.register(ATTACK_ID, RiftwardenAttackType.NONE.getId());
        this.dataManager.register(STATE_PROGRESS, 0F);
        this.dataManager.register(PHASE, 0);
        this.dataManager.register(BALL_ROTATION, 0F);
        this.dataManager.register(GATE_PROGRESS, 0F);
    }
    
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(MAX_HP);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(MOVE_SPEED);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(14.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(12.0D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0D);
    }
    
    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new AIRiftwardenCombat(this));
        this.tasks.addTask(2, new AIRiftwardenPursue(this));
        this.tasks.addTask(3, new AIRiftwardenWander(this));
        this.tasks.addTask(4, new AIRiftwardenLookAround(this));
        
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
    }
    
    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation(MODID, "entities/rift_warden");
    }
    
    // ========== 主循环 ==========
    
    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        
        // 清除药水效果
        if (!world.isRemote) {
            this.clearActivePotions();
        }
        
        // 更新组件
        stateManager.tick();

        if (!world.isRemote) {
            combatController.tick();
            environmentController.tick();
            tickGateSystem();
            tickSummons();
            
            // 关键修复：当有攻击目标且状态允许转向时，始终看向目标
            // 这解决了 Combat AI 的死锁问题（需要面向才能攻击，但不攻击就不转向）
            EntityLivingBase attackTarget = this.getAttackTarget();
            if (attackTarget != null && attackTarget.isEntityAlive() && getCurrentState().allowsRotation()) {
                movementController.lookAt(attackTarget);
            }
            
            // 瞬移检查
            EntityPlayer target = getHighestThreatOrNearest();
            if (target != null) {
                combatController.tryTeleport(target);
            }
        }
        
        // 更新球体旋转
        ballRotation += 2.0F + phase * 0.5F;
        if (ballRotation >= 360F) ballRotation -= 360F;
        this.dataManager.set(BALL_ROTATION, ballRotation);
        
        // 更新Boss条
        updateBossBar();
    }
    
    // ========== 状态回调 ==========
    
    public void onStateChanged(RiftwardenState from, RiftwardenState to) {
        // 进入攻击状态时停止移动
        if (to.isCasting() || to.isMelee()) {
            movementController.enterAttackState();
        }
        
        // 进入虚弱状态
        if (to == RiftwardenState.EXHAUSTED) {
            onEnterExhaustion();
        }
        
        // 离开虚弱状态
        if (from == RiftwardenState.EXHAUSTED && to != RiftwardenState.EXHAUSTED) {
            onExitExhaustion();
        }
        
        // 同步状态到客户端
        syncState();
    }
    
    // ========== 伤害系统 ==========
    
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        // 虚弱状态：双倍伤害，绕过限伤
        if (getCurrentState() == RiftwardenState.EXHAUSTED) {
            this.hurtResistantTime = 0;
            this.maxHurtResistantTime = 0;
            
            // 暴击反馈
            if (world instanceof WorldServer && source.getTrueSource() instanceof EntityPlayer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                    posX, posY + height * 0.5, posZ,
                    20, 0.5, 0.5, 0.5, 0.1);
                playGlobal(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.2F, 1.0F);
            }
            
            return super.attackEntityFrom(source, amount * 2.0F);
        }
        
        // 内部伤害直接通过
        if (isInternalDamage(source)) {
            return super.attackEntityFrom(source, amount);
        }
        
        // 虚空伤害通过
        if (source == DamageSource.OUT_OF_WORLD) {
            return super.attackEntityFrom(source, amount);
        }
        
        // 限伤逻辑
        float chunkSize = getChunkSize();
        float cappedDamage = Math.min(amount, chunkSize);
        
        EntityLivingBase attacker = source.getTrueSource() instanceof EntityLivingBase ?
            (EntityLivingBase) source.getTrueSource() : null;
        
        if (gateInvulTicks > 0) {
            // 锁血中：累积伤害
            accumulateDamage(cappedDamage, attacker);
            spawnBlockParticle();
            return false;
        } else {
            // 触发锁血
            openGate(cappedDamage, attacker);
            this.hurtTime = 10;
            this.maxHurtTime = 10;
            return false;
        }
    }
    
    private void openGate(float initialDamage, EntityLivingBase attacker) {
        int duration = Math.max(24, GATE_DURATION_BASE - phase * 4);
        
        if (gateInvulTicks <= 0) {
            gateHealthSnapshot = this.getHealth();
            accumulatedDamage = 0F;
        }
        
        gateInvulTicks = Math.max(gateInvulTicks, duration);
        accumulatedDamage += initialDamage;
        
        // 通知战斗控制器
        combatController.onDamageTaken(attacker, initialDamage);
        
        playGlobal(SoundEvents.ITEM_SHIELD_BLOCK, 0.9F, 0.9F);
        spawnRing(EnumParticleTypes.PORTAL, 48, 2.2);
        
        // 进入锁血状态
        stateManager.tryTransitionTo(RiftwardenState.GATE_ACTIVE);
    }
    
    private void accumulateDamage(float damage, EntityLivingBase attacker) {
        accumulatedDamage += damage;
        combatController.onDamageTaken(attacker, damage);
        playGlobal(SoundEvents.BLOCK_STONE_HIT, 0.4F, 1.2F + rand.nextFloat() * 0.2F);
    }
    
    private void tickGateSystem() {
        if (gateInvulTicks <= 0) return;
        
        gateInvulTicks--;
        
        // 粒子效果（降低频率）
        if (ticksExisted % 10 == 0) {
            spawnRing(EnumParticleTypes.REDSTONE, 12, 1.8);
        }
        
        // 同步进度
        float progress = (float) gateInvulTicks / GATE_DURATION_BASE;
        this.dataManager.set(GATE_PROGRESS, progress);
        
        // 锁血期间的反击
        combatController.tickGateCounterAttack();
        
        // 锁血结束
        if (gateInvulTicks == 0) {
            applyAccumulatedDamage();
        }
    }
    
    private void applyAccumulatedDamage() {
        if (accumulatedDamage <= 0F) {
            resetGateState();
            return;
        }
        
        float maxDamage = getChunkSize() * MAX_CHUNKS_PER_CYCLE;
        float actualDamage = Math.min(accumulatedDamage, maxDamage);
        float newHealth = Math.max(1F, gateHealthSnapshot - actualDamage);
        
        this.setHealth(newHealth);
        
        playGlobal(SoundEvents.BLOCK_ANVIL_LAND, 0.7F, 1.0F);
        spawnRing(EnumParticleTypes.CRIT_MAGIC, 32, 2.6);
        
        // 伤害数字粒子
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR,
                posX, posY + height + 0.5, posZ,
                (int)(actualDamage / 10), 0.3, 0.2, 0.3, 0.0);
        }
        
        resetGateState();
    }
    
    private void resetGateState() {
        gateHealthSnapshot = -1F;
        accumulatedDamage = 0F;
        this.dataManager.set(GATE_PROGRESS, 0F);
        
        if (getCurrentState() == RiftwardenState.GATE_ACTIVE) {
            stateManager.tryTransitionTo(RiftwardenState.IDLE);
        }
    }
    
    public float getChunkSize() {
        return this.getMaxHealth() / 20.0F;
    }
    
    private boolean isInternalDamage(DamageSource source) {
        return source != null && source.getDamageType() != null && 
               source.getDamageType().startsWith(MODID);
    }
    
    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        if (getCurrentState() == RiftwardenState.EXHAUSTED) {
            return false;
        }
        if (source == DamageSource.OUT_OF_WORLD) {
            return false;
        }
        return super.isEntityInvulnerable(source);
    }
    
    // ========== 虚弱状态 ==========
    
    private void onEnterExhaustion() {
        this.hurtResistantTime = 0;
        this.maxHurtResistantTime = 0;
        this.setNoAI(true);
        
        playGlobal(SoundEvents.ENTITY_VILLAGER_HURT, 1.0F, 0.5F);
        playGlobal(SoundEvents.ENTITY_PLAYER_BREATH, 1.5F, 0.8F);
        
        // 通知玩家
        notifyNearbyPlayers("§6§l虚空守望者陷入虚弱！抓住机会输出！");
        
        // 烟雾效果
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            for (int i = 0; i < 50; i++) {
                double angle = (Math.PI * 2 * i) / 50;
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                    posX + Math.cos(angle) * 2,
                    posY + 0.1,
                    posZ + Math.sin(angle) * 2,
                    1, 0, 0.1, 0, 0.02);
            }
        }
    }
    
    private void onExitExhaustion() {
        this.maxHurtResistantTime = 10;
        this.setNoAI(false);
        
        playGlobal(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 1.5F, 0.8F);
        playGlobal(SoundEvents.ENTITY_WITHER_SPAWN, 1.0F, 1.5F);
        
        // 恢复时的反击
        combatController.performRecoveryBurst();
        
        notifyNearbyPlayers("§4§l虚空守望者恢复了力量！");
        
        // 爆炸效果
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                posX, posY + height * 0.5, posZ, 5, 0, 0, 0.0, 0);
        }
    }
    
    // ========== Boss条和阶段 ==========
    
    private void updateBossBar() {
        bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        
        int newPhase = computePhase();
        if (newPhase != phase) {
            int oldPhase = phase;
            phase = newPhase;
            onPhaseChanged(oldPhase, newPhase);
        }
        
        this.dataManager.set(PHASE, phase);
    }
    
    private int computePhase() {
        float pct = this.getHealth() / this.getMaxHealth();
        if (pct > 0.75F) return 0;
        if (pct > 0.50F) return 1;
        if (pct > 0.25F) return 2;
        return 3;
    }
    
    private void onPhaseChanged(int oldPhase, int newPhase) {
        playGlobal(SoundEvents.ENTITY_ENDERDRAGON_GROWL, 1.0F, 0.95F + 0.05F * newPhase);
        spawnRing(EnumParticleTypes.SPELL_WITCH, 96, 3.0 + newPhase);
        
        // 阶段变化时召唤小怪
        if (newPhase >= 1 && newPhase <= 3 && !wavesFired[newPhase - 1]) {
            wavesFired[newPhase - 1] = true;
            combatController.summonMinions(2, 6, 12);
        }
        
        // 更新Boss条颜色
        switch (newPhase) {
            case 0: bossBar.setColor(BossInfo.Color.PURPLE); break;
            case 1: bossBar.setColor(BossInfo.Color.BLUE); break;
            case 2: bossBar.setColor(BossInfo.Color.YELLOW); break;
            case 3: bossBar.setColor(BossInfo.Color.RED); break;
        }
        
        // 更新小怪上限
        minionCap = 3 + newPhase;
    }
    
    // ========== 召唤系统 ==========
    
    private void tickSummons() {
        if (summonCooldown > 0) {
            summonCooldown--;
            return;
        }
        
        int alive = countNearbyMinions(48);
        if (alive < minionCap) {
            combatController.summonMinions(Math.min(2, minionCap - alive), 6, 12);
        }
        
        summonCooldown = 400 - phase * 30;
    }
    
    private int countNearbyMinions(double range) {
        return (int) world.getEntitiesWithinAABB(
            com.moremod.entity.EntityCursedKnight.class,
            new AxisAlignedBB(posX - range, posY - range, posZ - range,
                             posX + range, posY + range, posZ + range)
        ).stream().filter(Entity::isEntityAlive).count();
    }
    
    // ========== GeckoLib ==========
    
    @Override
    public void registerControllers(AnimationData data) {
        animationController.registerControllers(data);
    }
    
    @Override
    public AnimationFactory getFactory() {
        return animationFactory;
    }
    
    // ========== 数据同步 ==========
    
    public void syncState() {
        this.dataManager.set(STATE_ID, stateManager.getCurrentState().getId());
        this.dataManager.set(ATTACK_ID, stateManager.getCurrentAttack().getId());
        this.dataManager.set(STATE_PROGRESS, stateManager.getStateProgress());
    }
    
    // ========== Boss追踪 ==========
    
    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        bossBar.addPlayer(player);
        BossBlockTracker.startTracking(this);
    }
    
    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        bossBar.removePlayer(player);
    }
    
    @Override
    public void setCustomNameTag(String name) {
        super.setCustomNameTag(name);
        bossBar.setName(this.getDisplayName());
    }
    
    @Override
    public void setDead() {
        super.setDead();
        BossBlockTracker.stopTracking(this);
        environmentController.cleanup();
        combatController.cleanup();
    }
    
    // ========== NBT ==========
    
    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        
        stateManager.writeToNBT(nbt);
        combatController.writeToNBT(nbt);
        
        nbt.setInteger("Phase", phase);
        nbt.setInteger("GateInvulTicks", gateInvulTicks);
        nbt.setFloat("GateHealthSnapshot", gateHealthSnapshot);
        nbt.setFloat("AccumulatedDamage", accumulatedDamage);
        nbt.setInteger("SummonCooldown", summonCooldown);
        nbt.setInteger("MinionCap", minionCap);
        
        for (int i = 0; i < wavesFired.length; i++) {
            nbt.setBoolean("WaveFired" + i, wavesFired[i]);
        }
    }
    
    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        
        stateManager.readFromNBT(nbt);
        combatController.readFromNBT(nbt);
        
        phase = nbt.getInteger("Phase");
        gateInvulTicks = nbt.getInteger("GateInvulTicks");
        gateHealthSnapshot = nbt.getFloat("GateHealthSnapshot");
        accumulatedDamage = nbt.getFloat("AccumulatedDamage");
        summonCooldown = nbt.getInteger("SummonCooldown");
        minionCap = Math.max(3, nbt.getInteger("MinionCap"));
        
        for (int i = 0; i < wavesFired.length; i++) {
            wavesFired[i] = nbt.getBoolean("WaveFired" + i);
        }
        
        // 同步状态到 DataManager（客户端会自动接收）
        syncState();
    }
    
    // ========== 掉落 ==========
    
    @Override
    protected void dropFewItems(boolean wasRecentlyHit, int looting) {
        entityDropItem(new ItemStack(Items.NETHER_STAR), 0F);
        if (rand.nextFloat() < 0.15F + 0.05F * looting) {
            entityDropItem(new ItemStack(Items.ELYTRA), 0F);
        }
        entityDropItem(new ItemStack(Items.ENDER_PEARL, 6 + rand.nextInt(6 + looting * 2)), 0F);
        entityDropItem(new ItemStack(Items.DIAMOND, 2 + rand.nextInt(2 + looting)), 0F);
    }
    
    // ========== 工具方法 ==========
    
    public void playGlobal(SoundEvent sound, float volume, float pitch) {
        world.playSound(null, getPosition(), sound, SoundCategory.HOSTILE, volume, pitch);
    }
    
    private void notifyNearbyPlayers(String message) {
        world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(50))
            .forEach(p -> p.sendMessage(new net.minecraft.util.text.TextComponentString(message)));
    }
    
    private void spawnBlockParticle() {
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                posX + (rand.nextDouble() - 0.5) * width,
                posY + rand.nextDouble() * height,
                posZ + (rand.nextDouble() - 0.5) * width,
                5, 0, 0, 0, 0.05,
                net.minecraft.block.Block.getStateId(
                    net.minecraft.init.Blocks.OBSIDIAN.getDefaultState()));
        }
    }
    
    private void spawnRing(EnumParticleTypes type, int count, double radius) {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;
        
        double cx = posX;
        double cy = posY + height * 0.6;
        double cz = posZ;
        
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            ws.spawnParticle(type,
                cx + Math.cos(angle) * radius, cy, cz + Math.sin(angle) * radius,
                1, 0, 0, 0, 0.0);
        }
    }
    
    private EntityPlayer getHighestThreatOrNearest() {
        EntityPlayer highThreat = combatController.getHighestThreatPlayer();
        if (highThreat != null && highThreat.isEntityAlive()) {
            return highThreat;
        }
        return world.getNearestAttackablePlayer(this, 50, 50);
    }
    
    // ========== IBossBlockTrackable ==========
    
    @Override
    public World getEntityWorld() {
        return this.world;
    }
    
    // ========== Getters ==========
    
    public RiftwardenStateManager getStateManager() { return stateManager; }
    public RiftwardenMovementController getMovementController() { return movementController; }
    public RiftwardenCombatController getCombatController() { return combatController; }
    public RiftwardenAnimationController getAnimationController() { return animationController; }
    public RiftwardenEnvironmentController getEnvironmentController() { return environmentController; }
    
    public RiftwardenState getCurrentState() { 
        // 客户端从 DataManager 读取同步的状态（动画用）
        if (world.isRemote) {
            return RiftwardenState.fromId(this.dataManager.get(STATE_ID));
        }
        return stateManager.getCurrentState(); 
    }
    
    public int getPhase() { return phase; }
    
    public float getBallRotation() { 
        return dataManager.get(BALL_ROTATION); 
    }
    
    public float getGateProgress() { 
        return dataManager.get(GATE_PROGRESS); 
    }
    
    public boolean isGateActive() { 
        return gateInvulTicks > 0; 
    }
    
    public boolean isExhausted() { 
        return getCurrentState() == RiftwardenState.EXHAUSTED; 
    }
    
    public float getAccumulatedDamage() {
        return accumulatedDamage;
    }
}