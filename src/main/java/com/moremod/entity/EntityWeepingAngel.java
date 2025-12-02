package com.moremod.entity;

import com.moremod.entity.curse.CurseManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.block.Block;
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

/**
 * 哭泣天使实体 - 镐子特攻版本
 * 使用镐子攻击才能造成有效伤害
 */
public class EntityWeepingAngel extends EntityMob implements IAnimatable {

    private AnimationFactory factory = new AnimationFactory(this);

    // 数据参数
    private static final DataParameter<Boolean> PLAYER_WATCHING = EntityDataManager.createKey(EntityWeepingAngel.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> TIME_LOCKED = EntityDataManager.createKey(EntityWeepingAngel.class, DataSerializers.BOOLEAN);

    // 属性
    private int teleportCooldown = 0;
    private BlockPos lastPos = null;
    private int stuckTimer = 0;
    private float lastDamageMultiplier = 1.0F;  // 记录上次受到的伤害倍率

    // 限伤机制 - 时间锁定系统（调整后）
    private static final String TIME_LOCK_ID = MODID + ".time_lock";
    public static final DamageSource TIME_LOCK_DAMAGE = new DamageSource(TIME_LOCK_ID)
            .setDamageIsAbsolute()
            .setDamageBypassesArmor();

    private int timeLockTicks = 0;
    private boolean pendingDamage = false;
    private boolean applyingDamage = false;
    private float frozenHealth = -1F;
    private float frozenAbsorption = -1F;
    private int timeLockCooldown = 0;
    private static final int TIME_LOCK_BASE_DURATION = 40; // 2秒基础无敌时间（降低）
    private static final float DAMAGE_THRESHOLD = 25.0F; // 触发限伤的伤害阈值（提高）

    // 配置
    private static final double DETECTION_RANGE = 64.0D;
    private static final double ATTACK_RANGE = 2.0D;
    private static final int TELEPORT_COOLDOWN_TICKS = 20;

    @Override
    protected ResourceLocation getLootTable() {
        return new ResourceLocation(MODID, "entities/weeping_angel");
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
        this.dataManager.register(TIME_LOCKED, Boolean.valueOf(false));
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
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(150.0D);  // 提高血量
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.45D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(10.0D);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(15.0D);  // 提高护甲
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
    }

    @Override
    public void onLivingUpdate() {
        // 清除大部分药水效果（保留少数）
        if (!this.world.isRemote) {
            clearMostPotions();
        }

        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        }

        if (this.timeLockCooldown > 0) {
            this.timeLockCooldown--;
        }

        this.updateWatchedState();
        this.handleTimeLock();

        // 被玩家看着时或时间锁定时石化
        if (this.isPlayerWatching() || this.isTimeLocked()) {
            this.motionX = 0;
            this.motionY = 0;
            this.motionZ = 0;
            this.rotationYaw = this.prevRotationYaw;
            this.rotationPitch = this.prevRotationPitch;

            if (lastPos != null && lastPos.equals(this.getPosition())) {
                stuckTimer++;
                if (stuckTimer > 200 && !this.isTimeLocked()) {
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

        // 时间锁定时的视觉效果
        if (this.isTimeLocked() && !world.isRemote && this.ticksExisted % 3 == 0) {
            spawnTimeLockParticles();
        }
    }

    /**
     * 清除大部分药水效果，但保留少数特殊效果
     */
    private void clearMostPotions() {
        if (this.getActivePotionEffects().isEmpty()) return;

        // 保留的药水效果
        Potion wither = Potion.getPotionFromResourceLocation("minecraft:wither");
        Potion instantDamage = Potion.getPotionFromResourceLocation("minecraft:instant_damage");

        for (PotionEffect effect : this.getActivePotionEffects()) {
            Potion potion = effect.getPotion();
            if (potion != wither && potion != instantDamage) {
                this.removePotionEffect(potion);
            }
        }
    }

    /**
     * 计算基于武器类型的伤害倍率
     */
    private float calculateDamageMultiplier(DamageSource source) {
        if (source.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) source.getTrueSource();
            ItemStack heldItem = player.getHeldItemMainhand();

            if (!heldItem.isEmpty()) {
                Item item = heldItem.getItem();

                // 镐子类武器造成额外伤害
                if (item instanceof ItemPickaxe) {
                    // 根据镐子材质给予不同倍率
                    ItemPickaxe pickaxe = (ItemPickaxe) item;
                    String toolMaterial = pickaxe.getToolMaterialName();

                    float multiplier = 2.0F;  // 默认倍率
                    switch (toolMaterial) {
                        case "WOOD":
                        case "GOLD":
                            multiplier = 1.5F;
                            break;
                        case "STONE":
                            multiplier = 2.0F;
                            break;
                        case "IRON":
                            multiplier = 2.5F;
                            break;
                        case "DIAMOND":
                            multiplier = 3.0F;
                            break;
                    }

                    // 镐子攻击时播放特殊音效和粒子
                    if (!world.isRemote) {
                        playSound(SoundEvents.BLOCK_STONE_HIT, 1.5F, 0.8F);
                        playSound(SoundEvents.BLOCK_ANVIL_PLACE, 0.5F, 1.5F);

                        // 石屑飞溅效果
                        if (world instanceof WorldServer) {
                            WorldServer ws = (WorldServer) world;
                            ws.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                                    this.posX, this.posY + this.height * 0.5, this.posZ,
                                    20, 0.3, 0.3, 0.3, 0.1,
                                    Block.getStateId(Blocks.STONE.getDefaultState()));
                        }

                        // 给玩家反馈信息
                        if (multiplier >= 2.5F && rand.nextFloat() < 0.3) {
                            player.sendMessage(new TextComponentString("§7石质外壳出现裂痕！"));
                        }
                    }

                    return multiplier;
                }

                // 剑类武器减少伤害
                else if (item instanceof ItemSword) {
                    // 剑攻击效果不佳
                    if (!world.isRemote && rand.nextFloat() < 0.2) {
                        player.sendMessage(new TextComponentString("§c剑刃难以穿透石质外壳！"));
                        playSound(SoundEvents.ENTITY_ITEM_BREAK, 0.5F, 1.5F);
                    }
                    return 0.3F;
                }

                // 斧头适中伤害
                else if (item instanceof ItemAxe) {
                    return 0.8F;
                }

                // 锤子类（如果有模组添加）
                else if (item.getTranslationKey().toLowerCase().contains("hammer")) {
                    return 2.5F;
                }

                // 其他武器效果很差
                else {
                    return 0.2F;
                }
            }

            // 空手伤害极低
            if (!world.isRemote && rand.nextFloat() < 0.1) {
                player.sendMessage(new TextComponentString("§c空手无法撼动石像！"));
            }
            return 0.05F;
        }

        // 非玩家伤害正常计算
        return 1.0F;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        // 时间锁定期间减伤（不是完全免疫）
        if (this.isTimeLocked() && !isTrustedDamageSource(source)) {
            amount *= 0.2F;  // 80%减伤
        }

        // 检查攻击者使用的武器
        float damageMultiplier = calculateDamageMultiplier(source);
        this.lastDamageMultiplier = damageMultiplier;
        amount *= damageMultiplier;

        // 被玩家看着时的减伤（调整为更合理的值）
        if (this.isPlayerWatching()) {
            amount *= 0.5F;  // 50%减伤而非90%
        }

        // 检查是否触发时间锁定
        if (!this.isTimeLocked() && amount >= DAMAGE_THRESHOLD && timeLockCooldown <= 0 && !isTrustedDamageSource(source)) {
            // 只有镐子造成的高伤害才能触发时间锁定
            if (this.lastDamageMultiplier >= 2.0F) {
                boolean scheduleDamage = amount >= getTimeLockDamage();
                activateTimeLock(scheduleDamage);
                return false;
            }
        }

        boolean result = super.attackEntityFrom(source, amount);

        if (result && !this.isPlayerWatching() && !this.isTimeLocked() && this.rand.nextFloat() < 0.3F) {
            this.attemptTeleportBehindTarget();
        }

        return result;
    }

    private void handleTimeLock() {
        if (this.timeLockTicks > 0) {
            this.timeLockTicks--;

            // 在时间锁定期间冻结血量
            if (!this.applyingDamage) {
                if (this.frozenHealth >= 0F && this.getHealth() != this.frozenHealth) {
                    this.setHealth(this.frozenHealth);
                }
                if (this.frozenAbsorption >= 0F && this.getAbsorptionAmount() != this.frozenAbsorption) {
                    this.setAbsorptionAmount(this.frozenAbsorption);
                }
            }

            // 时间锁定结束时应用累积的伤害
            if (this.timeLockTicks == 0) {
                this.dataManager.set(TIME_LOCKED, false);

                if (this.pendingDamage && this.getHealth() > 0F) {
                    float damage = getTimeLockDamage();
                    this.applyingDamage = true;
                    try {
                        super.attackEntityFrom(TIME_LOCK_DAMAGE, damage);
                        playSound(SoundEvents.BLOCK_GLASS_BREAK, 1.0F, 0.5F);
                        spawnDamageParticles();
                    } finally {
                        this.applyingDamage = false;
                    }
                }
                this.pendingDamage = false;
                this.frozenHealth = -1F;
                this.frozenAbsorption = -1F;
            }
        }
    }

    private void activateTimeLock(boolean scheduleDamage) {
        int duration = TIME_LOCK_BASE_DURATION;

        // 如果已经在时间锁定中，延长持续时间
        this.timeLockTicks = Math.max(this.timeLockTicks, duration);
        this.dataManager.set(TIME_LOCKED, true);

        // 冻结当前血量
        this.frozenHealth = this.getHealth();
        this.frozenAbsorption = this.getAbsorptionAmount();
        this.pendingDamage = scheduleDamage;

        // 播放效果
        if (!world.isRemote) {
            playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 0.8F, 1.5F);
            playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 0.5F);

            // 通知附近玩家
            List<EntityPlayer> nearbyPlayers = this.world.getEntitiesWithinAABB(
                    EntityPlayer.class,
                    this.getEntityBoundingBox().grow(20)
            );

            for (EntityPlayer player : nearbyPlayers) {
                player.sendMessage(new TextComponentString("§5哭泣天使启动了时间锁定！"));
            }

            spawnTimeLockActivationEffect();
        }

        this.timeLockCooldown = 200; // 10秒冷却
    }

    private void spawnTimeLockParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 螺旋上升的粒子效果
        double angle = (this.ticksExisted * 0.2) % (Math.PI * 2);
        double radius = 1.5;

        for (int i = 0; i < 3; i++) {
            double offsetAngle = angle + (i * Math.PI * 2 / 3);
            double px = this.posX + Math.cos(offsetAngle) * radius;
            double pz = this.posZ + Math.sin(offsetAngle) * radius;
            double py = this.posY + (this.ticksExisted % 40) * 0.05;

            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    px, py, pz,
                    1, 0, 0.1, 0, 0.02);

            ws.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                    px, py, pz,
                    1, 0, 0.5, 0, 0.1);
        }
    }

    private void spawnTimeLockActivationEffect() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 扩散的环形效果
        for (int ring = 0; ring < 3; ring++) {
            double radius = 1.0 + ring * 0.5;
            for (int i = 0; i < 32; i++) {
                double angle = (Math.PI * 2 * i) / 32;
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        this.posX + Math.cos(angle) * radius,
                        this.posY + this.height * 0.5,
                        this.posZ + Math.sin(angle) * radius,
                        1, 0, 0.1, 0, 0.05);
            }
        }
    }

    private void spawnDamageParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        ws.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                this.posX, this.posY + this.height * 0.5, this.posZ,
                30, 0.5, 0.5, 0.5, 0.1);
    }

    private float getTimeLockDamage() {
        return this.getMaxHealth() / 10.0F; // 最大生命值的10%
    }

    public boolean isTimeLocked() {
        return this.timeLockTicks > 0 || this.dataManager.get(TIME_LOCKED);
    }

    private boolean isTrustedDamageSource(DamageSource source) {
        return source != null && TIME_LOCK_ID.equals(source.getDamageType());
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        // 时间锁定期间不再完全免疫
        return super.isEntityInvulnerable(source);
    }

    @Override
    public boolean isPotionApplicable(PotionEffect potioneffectIn) {
        // 允许某些特殊药水效果
        Potion potion = potioneffectIn.getPotion();
        if (potion == Potion.getPotionFromResourceLocation("minecraft:wither") ||
                potion == Potion.getPotionFromResourceLocation("minecraft:instant_damage")) {
            return true;
        }
        return false;
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
            if (!isWatched && wasWatched && !this.isTimeLocked()) {
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
        if (this.teleportCooldown > 0 || this.isTimeLocked()) {
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

    // ========== 工具提示（供物品描述调用） ==========

    public static void addTooltip(List<String> tooltip) {
        tooltip.add("§7提示：哭泣天使的石质身躯");
        tooltip.add("§b使用镐子可造成额外伤害：");
        tooltip.add("§7  木/金镐: §e1.5x §7伤害");
        tooltip.add("§7  石镐: §e2.0x §7伤害");
        tooltip.add("§7  铁镐: §e2.5x §7伤害");
        tooltip.add("§7  钻石镐: §e3.0x §7伤害");
        tooltip.add("§c剑类武器效果极差（0.3x）");
    }

    // ========== GeckoLib动画 ==========

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        AnimationBuilder builder = new AnimationBuilder();

        if (!this.isPlayerWatching() && !this.isTimeLocked()) {
            // 没被看着且没有时间锁定时：播放放下手的动画
            builder.addAnimation("animation.angel.arms_down", true);
            event.getController().setAnimation(builder);
            return PlayState.CONTINUE;
        } else {
            // 被看着或时间锁定时：不播放动画，保持默认的遮脸模型
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
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setBoolean("PlayerWatching", this.isPlayerWatching());
        compound.setInteger("TeleportCooldown", this.teleportCooldown);
        compound.setInteger("TimeLockTicks", this.timeLockTicks);
        compound.setBoolean("TimeLocked", this.dataManager.get(TIME_LOCKED));
        compound.setBoolean("PendingDamage", this.pendingDamage);
        compound.setFloat("FrozenHealth", this.frozenHealth);
        compound.setFloat("FrozenAbsorption", this.frozenAbsorption);
        compound.setInteger("TimeLockCooldown", this.timeLockCooldown);
        compound.setFloat("LastDamageMultiplier", this.lastDamageMultiplier);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setPlayerWatching(compound.getBoolean("PlayerWatching"));
        this.teleportCooldown = compound.getInteger("TeleportCooldown");
        this.timeLockTicks = compound.getInteger("TimeLockTicks");
        this.dataManager.set(TIME_LOCKED, compound.getBoolean("TimeLocked"));
        this.pendingDamage = compound.getBoolean("PendingDamage");
        this.frozenHealth = compound.getFloat("FrozenHealth");
        this.frozenAbsorption = compound.getFloat("FrozenAbsorption");
        this.timeLockCooldown = compound.getInteger("TimeLockCooldown");
        this.lastDamageMultiplier = compound.getFloat("LastDamageMultiplier");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isPlayerWatching() || this.isTimeLocked() ? null : SoundEvents.BLOCK_STONE_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        // 根据武器类型播放不同音效
        if (this.lastDamageMultiplier >= 2.0F) {
            return SoundEvents.BLOCK_ANVIL_HIT;  // 镐子命中
        }
        return SoundEvents.BLOCK_STONE_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_STONE_BREAK;
    }

    // ========== 事件处理器 ==========

    @Mod.EventBusSubscriber(modid = MODID)
    public static class EventHooks {

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onAttackPre(LivingAttackEvent e) {
            if (!(e.getEntityLiving() instanceof EntityWeepingAngel)) return;
            EntityWeepingAngel angel = (EntityWeepingAngel) e.getEntityLiving();

            // 时间锁定不再完全取消伤害
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onHeal(LivingHealEvent e) {
            if (!(e.getEntityLiving() instanceof EntityWeepingAngel)) return;
            EntityWeepingAngel angel = (EntityWeepingAngel) e.getEntityLiving();

            if (angel.isTimeLocked()) {
                e.setCanceled(true);
                e.setAmount(0F);
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public static void onFinalDamage(LivingDamageEvent e) {
            if (!(e.getEntityLiving() instanceof EntityWeepingAngel)) return;
            EntityWeepingAngel angel = (EntityWeepingAngel) e.getEntityLiving();

            if (angel.isTrustedDamageSource(e.getSource())) {
                return; // 允许信任来源的伤害通过
            }

            // 时间锁定期间伤害已在 attackEntityFrom 中处理
        }
    }

    // ========== AI类 ==========

    static class AIWeepingAngelAttack extends EntityAIBase {
        private final EntityWeepingAngel angel;
        private int attackTick;
        private int curseCooldown = 0;

        public AIWeepingAngelAttack(EntityWeepingAngel angel) {
            this.angel = angel;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = this.angel.getAttackTarget();
            return target != null && target.isEntityAlive() &&
                    !this.angel.isPlayerWatching() && !this.angel.isTimeLocked();
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = this.angel.getAttackTarget();
            if (target == null) return;

            double distance = this.angel.getDistanceSq(target);

            if (this.curseCooldown > 0) {
                this.curseCooldown--;
            }

            if (distance < ATTACK_RANGE * ATTACK_RANGE) {
                if (this.attackTick <= 0) {
                    this.angel.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
                    this.angel.attackEntityAsMob(target);

                    if (target instanceof EntityPlayer) {
                        EntityPlayer player = (EntityPlayer) target;

                        if (this.curseCooldown <= 0 && this.angel.rand.nextFloat() < 0.3F) {
                            CurseManager.applyCurse(player);
                            player.sendMessage(new TextComponentString("§4哭泣天使的触碰让你感到生命力在流失..."));
                            this.angel.world.playSound(null, player.posX, player.posY, player.posZ,
                                    SoundEvents.ENTITY_WITHER_HURT,
                                    net.minecraft.util.SoundCategory.HOSTILE,
                                    0.5F, 0.5F);
                            this.curseCooldown = 1200;
                        }

                        player.addPotionEffect(new PotionEffect(
                                Potion.getPotionFromResourceLocation("minecraft:slowness"),
                                100, 2));
                        player.addPotionEffect(new PotionEffect(
                                Potion.getPotionFromResourceLocation("minecraft:mining_fatigue"),
                                200, 1));

                        if (this.angel.rand.nextFloat() < 0.2F) {
                            player.addPotionEffect(new PotionEffect(
                                    Potion.getPotionFromResourceLocation("minecraft:blindness"),
                                    60, 0));
                        }
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

        @Override
        public void resetTask() {
            super.resetTask();
            this.attackTick = 0;
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
            if (target == null || this.angel.isPlayerWatching() || this.angel.isTimeLocked()) {
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