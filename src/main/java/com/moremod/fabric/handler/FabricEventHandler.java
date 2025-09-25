package com.moremod.fabric.handler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.moremod.fabric.data.UpdatedFabricPlayerData;
import com.moremod.fabric.system.FabricWeavingSystem;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 完整的四界之织事件处理器 - 维度崩塌领域版本
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class FabricEventHandler {

    private static final Random RANDOM = new Random();
    private static final Map<UUID, PlayerFabricData> PLAYER_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, FrozenEntityData> FROZEN_ENTITIES = new ConcurrentHashMap<>();
    private static final Map<Integer, List<TimeStopZone>> ACTIVE_TIME_ZONES = new ConcurrentHashMap<>();
    private static final List<CollapseField> ACTIVE_COLLAPSE_FIELDS = new ArrayList<>();
    private static int tickCount = 0;

    // 属性UUID
    private static final UUID ABYSS_ATTACK_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F295");
    private static final UUID ABYSS_SPEED_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F296");
    private static final UUID TEMPORAL_SPEED_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F297");
    private static final UUID TEMPORAL_ATTACK_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F298");
    private static final UUID SPATIAL_DODGE_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F299");

    // 时停系统常量
    private static final int TIME_STOP_COOLDOWN = 30000;
    private static final float TIME_STOP_CHANCE_PER_PIECE = 0.04f;
    private static final float TIME_RIFT_CHANCE_PER_PIECE = 0.02f;

    // 伤害黑名单 - 防止循环增伤
    private static final Set<String> DAMAGE_AMPLIFICATION_BLACKLIST = new HashSet<>(Arrays.asList(
            "spaceTimeBarrier",
            "dimensionalCollapse",
            "phaseDamage"
    ));

    // 自定义伤害源
    public static class DimensionalDamageSource extends DamageSource {
        public DimensionalDamageSource(String name) {
            super(name);
            this.setDamageBypassesArmor();
            this.setMagicDamage();
        }
    }

    /**
     * 维度崩塌领域类
     */
    public static class CollapseField {
        public final BlockPos center;
        public final World world;
        public float currentRadius;
        public final float initialRadius;
        public int ticksRemaining;
        public final float totalDamage;
        public final EntityPlayer caster;
        public final UUID casterId;
        public final Set<UUID> affectedEntities = new HashSet<>();
        public int damageTickCounter = 0;
        public final float damagePerTick;
        public final int spatialCount;

        public CollapseField(BlockPos center, World world, float initialRadius, int duration,
                             float totalDamage, EntityPlayer caster, int spatialCount) {
            this.center = center;
            this.world = world;
            this.currentRadius = initialRadius;
            this.initialRadius = initialRadius;
            this.ticksRemaining = duration;
            this.totalDamage = totalDamage;
            this.caster = caster;
            this.casterId = caster.getUniqueID();
            this.spatialCount = spatialCount;
            this.damagePerTick = totalDamage / (duration / 5);
        }

        public void tick() {
            if (ticksRemaining <= 0 || !caster.isEntityAlive()) {
                deactivate();
                return;
            }

            ticksRemaining--;
            damageTickCounter++;

            // 逐渐收缩领域
            float shrinkProgress = 1f - ((float)ticksRemaining / (float)(ticksRemaining + (100 - ticksRemaining)));
            currentRadius = initialRadius * (0.3f + 0.7f * shrinkProgress);

            // 每5tick造成伤害和拉扯效果
            if (damageTickCounter >= 5) {
                damageTickCounter = 0;
                applyFieldEffects();
            }

            // 视觉效果
            if (ticksRemaining % 2 == 0) {
                spawnFieldParticles();
            }

            // 领域即将崩塌的警告
            if (ticksRemaining == 20) {
                world.playSound(null, center, SoundEvents.ENTITY_ENDERDRAGON_GROWL,
                        SoundCategory.PLAYERS, 2.0F, 0.5F);
            }
        }

        private void applyFieldEffects() {
            List<EntityLivingBase> victims = world.getEntitiesWithinAABB(
                    EntityLivingBase.class,
                    new AxisAlignedBB(center).grow(currentRadius),
                    e -> e != caster && e.isEntityAlive()
            );

            for (EntityLivingBase victim : victims) {
                double distance = victim.getDistance(center.getX(), center.getY(), center.getZ());
                if (distance > currentRadius) continue;

                // 距离系数：越靠近中心伤害越高
                float distanceModifier = 1f - (float)(distance / currentRadius) * 0.5f;
                float damage = damagePerTick * distanceModifier;

                // 造成伤害
                victim.attackEntityFrom(
                        new DimensionalDamageSource("dimensionalCollapse"),
                        damage
                );

                // 向中心拉扯，力度随距离递减
                Vec3d victimPos = victim.getPositionVector();
                Vec3d centerVec = new Vec3d(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                Vec3d pull = centerVec.subtract(victimPos).normalize();

                double pullStrength = 0.3 * distanceModifier * (1 + spatialCount * 0.2);
                victim.motionX += pull.x * pullStrength;
                victim.motionZ += pull.z * pullStrength;

                // 接近中心时的上升力
                if (distance < currentRadius * 0.3) {
                    victim.motionY += 0.1;
                }

                // 添加虚弱效果
                if (!affectedEntities.contains(victim.getUniqueID())) {
                    affectedEntities.add(victim.getUniqueID());
                    victim.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS,
                            ticksRemaining, spatialCount - 1));
                    victim.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE,
                            ticksRemaining, 0));
                }
            }
        }

        private void spawnFieldParticles() {
            if (!(world instanceof WorldServer)) return;
            WorldServer ws = (WorldServer) world;

            // 外圈粒子
            int particleCount = (int)(36 * (currentRadius / initialRadius));
            for (int i = 0; i < particleCount; i++) {
                double angle = (Math.PI * 2 * i) / particleCount;
                double x = center.getX() + 0.5 + Math.cos(angle) * currentRadius;
                double z = center.getZ() + 0.5 + Math.sin(angle) * currentRadius;

                ws.spawnParticle(EnumParticleTypes.PORTAL,
                        x, center.getY() + 0.5, z,
                        1, 0, 0.5, 0, 0.05);

                // 垂直环
                if (i % 3 == 0) {
                    double y = center.getY() + Math.sin(angle) * currentRadius;
                    ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                            center.getX() + 0.5, y + 0.5, z,
                            1, 0, 0.1, 0, 0.02);
                }
            }

            // 内部扭曲粒子
            for (int i = 0; i < 10; i++) {
                double randomRadius = RANDOM.nextDouble() * currentRadius;
                double randomAngle = RANDOM.nextDouble() * Math.PI * 2;
                double x = center.getX() + 0.5 + Math.cos(randomAngle) * randomRadius;
                double z = center.getZ() + 0.5 + Math.sin(randomAngle) * randomRadius;
                double y = center.getY() + RANDOM.nextDouble() * 3;

                ws.spawnParticle(EnumParticleTypes.END_ROD,
                        x, y, z,
                        1, 0, -0.1, 0, 0.01);
            }

            // 崩塌中心的强烈粒子效果
            if (ticksRemaining < 40) {
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                        center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                        1, 0, 0, 0.0, 0);
            }
        }

        public void deactivate() {
            // 最终爆发
            if (ticksRemaining <= 0 && world instanceof WorldServer) {
                finalCollapse();
            }

            affectedEntities.clear();
        }

        private void finalCollapse() {
            // 最终崩塌爆发
            float finalDamage = totalDamage * 0.3f;

            List<EntityLivingBase> victims = world.getEntitiesWithinAABB(
                    EntityLivingBase.class,
                    new AxisAlignedBB(center).grow(initialRadius),
                    e -> e != caster && e.isEntityAlive()
            );

            for (EntityLivingBase victim : victims) {
                double distance = victim.getDistance(center.getX(), center.getY(), center.getZ());
                float distanceModifier = Math.max(0, 1f - (float)(distance / initialRadius));

                victim.attackEntityFrom(
                        new DimensionalDamageSource("dimensionalCollapse"),
                        finalDamage * distanceModifier
                );

                // 最终击飞
                Vec3d knockback = victim.getPositionVector()
                        .subtract(new Vec3d(center.getX() + 0.5, center.getY(), center.getZ() + 0.5))
                        .normalize();
                victim.motionX = knockback.x * 2 * distanceModifier;
                victim.motionY = 0.8 * distanceModifier;
                victim.motionZ = knockback.z * 2 * distanceModifier;
            }

            // 音效和粒子
            world.playSound(null, center, SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.PLAYERS, 3.0F, 0.5F);

            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                for (int i = 0; i < 100; i++) {
                    double angle = RANDOM.nextDouble() * Math.PI * 2;
                    double radius = RANDOM.nextDouble() * initialRadius;
                    double x = center.getX() + 0.5 + Math.cos(angle) * radius;
                    double z = center.getZ() + 0.5 + Math.sin(angle) * radius;

                    ws.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                            x, center.getY() + 1, z,
                            1, 0, 0.2, 0, 0.1);
                }
            }
        }
    }/**
     * 玩家数据类
     */
    public static class PlayerFabricData {
        // 深渊布料
        public int abyssCount = 0;
        public int lastAbyssCount = 0;
        public float abyssPower = 0;
        public int abyssKills = 0;
        public long lastKillTime = 0;
        public boolean berserkMode = false;
        public long berserkEndTime = 0;

        // 时序布料
        public int temporalCount = 0;
        public int lastTemporalCount = 0;
        public LinkedList<TemporalSnapshot> temporalSnapshots = new LinkedList<>();
        public int rewindCount = 0;
        public long lastTimeStopTime = 0;
        public boolean chronoAccelerated = false;
        public long chronoEndTime = 0;
        public float temporalEnergy = 100f;

        // 时空布料
        public int spatialCount = 0;
        public int lastSpatialCount = 0;
        public float dimensionalEnergy = 100;
        public float storedDamage = 0;
        public int phaseStrikeCount = 0;
        public boolean collapseFieldReady = false;
        public long lastCollapseTime = 0;
        public static final long COLLAPSE_COOLDOWN = 10000;

        // 异界纤维
        public int otherworldCount = 0;
        public int lastOtherworldCount = 0;
        public float insight = 0;
        public float sanity = 100;
        public int forbiddenKnowledge = 0;
        public int abyssGazeStacks = 0;

        public boolean hasAbyssFabric() { return abyssCount > 0; }
        public boolean hasTemporalFabric() { return temporalCount > 0; }
        public boolean hasSpatialFabric() { return spatialCount > 0; }
        public boolean hasOtherworldFabric() { return otherworldCount > 0; }

        public void updateEquippedFabrics(EntityPlayer player) {
            lastAbyssCount = abyssCount;
            lastTemporalCount = temporalCount;
            lastSpatialCount = spatialCount;
            lastOtherworldCount = otherworldCount;

            abyssCount = 0;
            temporalCount = 0;
            spatialCount = 0;
            otherworldCount = 0;

            for (ItemStack armor : player.getArmorInventoryList()) {
                UpdatedFabricPlayerData.FabricType type = FabricWeavingSystem.getFabricType(armor);
                if (type != null) {
                    switch (type) {
                        case ABYSS: abyssCount++; break;
                        case TEMPORAL: temporalCount++; break;
                        case SPATIAL: spatialCount++; break;
                        case OTHERWORLD: otherworldCount++; break;
                    }
                }
            }
        }

        public boolean hasEquipmentChanged() {
            return abyssCount != lastAbyssCount ||
                    temporalCount != lastTemporalCount ||
                    spatialCount != lastSpatialCount ||
                    otherworldCount != lastOtherworldCount;
        }

        public static class TemporalSnapshot {
            public final double x, y, z;
            public final float health;
            public final int foodLevel;
            public final int experience;

            public TemporalSnapshot(double x, double y, double z, float health, int foodLevel, int exp) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.health = health;
                this.foodLevel = foodLevel;
                this.experience = exp;
            }
        }
    }

    // ========== 时停区域类 ==========
    public static class TimeStopZone {
        public final World world;
        public final BlockPos center;
        public final double range;
        public final EntityPlayer caster;
        public final int maxDuration;
        public int currentAge = 0;
        public boolean active = true;
        public final Set<UUID> affectedEntities = new HashSet<>();

        public TimeStopZone(World world, BlockPos center, double range, EntityPlayer caster, int duration) {
            this.world = world;
            this.center = center;
            this.range = range;
            this.caster = caster;
            this.maxDuration = duration;
        }

        public void tick() {
            if (!active) return;

            currentAge++;
            if (currentAge >= maxDuration) {
                deactivate();
                return;
            }

            List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                    EntityLivingBase.class,
                    new AxisAlignedBB(center).grow(range),
                    e -> e != caster && e.getDistanceSq(center) <= range * range
            );

            for (EntityLivingBase entity : entities) {
                if (!affectedEntities.contains(entity.getUniqueID())) {
                    freezeEntity(entity);
                    affectedEntities.add(entity.getUniqueID());
                }
            }

            if (currentAge % 20 == 0 && world instanceof WorldServer) {
                spawnZoneParticles();
            }
        }

        private void freezeEntity(EntityLivingBase entity) {
            FrozenEntityData data = new FrozenEntityData(entity);
            FROZEN_ENTITIES.put(entity.getUniqueID(), data);
        }

        private void spawnZoneParticles() {
            WorldServer ws = (WorldServer) world;
            for (int i = 0; i < 36; i++) {
                double angle = (Math.PI * 2 * i) / 36;
                double x = center.getX() + Math.cos(angle) * range;
                double z = center.getZ() + Math.sin(angle) * range;

                ws.spawnParticle(EnumParticleTypes.END_ROD,
                        x, center.getY() + 1, z,
                        1, 0, 0.1, 0, 0.05);
            }
        }

        public void deactivate() {
            active = false;

            for (UUID id : affectedEntities) {
                FrozenEntityData data = FROZEN_ENTITIES.remove(id);
                Entity entity = world.getEntityByID(id.hashCode());
                if (data != null && entity instanceof EntityLivingBase) {
                    data.restore((EntityLivingBase) entity);
                }
            }

            affectedEntities.clear();
        }
    }

    // ========== 冻结实体数据 ==========
    private static class FrozenEntityData {
        final Vec3d motion;
        final float yaw, pitch, yawHead;
        final int hurtTime;
        final BlockPos position;

        FrozenEntityData(EntityLivingBase entity) {
            this.motion = new Vec3d(entity.motionX, entity.motionY, entity.motionZ);
            this.yaw = entity.rotationYaw;
            this.pitch = entity.rotationPitch;
            this.yawHead = entity.rotationYawHead;
            this.hurtTime = entity.hurtTime;
            this.position = entity.getPosition();
        }

        void restore(EntityLivingBase entity) {
            if (entity != null && entity.isEntityAlive()) {
                entity.motionX = motion.x;
                entity.motionY = motion.y;
                entity.motionZ = motion.z;
            }
        }
    }

    // ========== 核心方法 ==========
    public static PlayerFabricData getPlayerData(EntityPlayer player) {
        return PLAYER_DATA.computeIfAbsent(player.getUniqueID(), k -> new PlayerFabricData());
    }

    public static void syncAllFabricDataToArmor(EntityPlayer player, PlayerFabricData data) {
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (!FabricWeavingSystem.hasFabric(armor)) continue;

            UpdatedFabricPlayerData.FabricType type = FabricWeavingSystem.getFabricType(armor);
            if (type == null) continue;

            NBTTagCompound armorTag = armor.getTagCompound();
            if (armorTag == null) continue;

            NBTTagCompound fabricData = armorTag.getCompoundTag("WovenFabric");

            switch (type) {
                case ABYSS:
                    fabricData.setInteger("AbyssKills", data.abyssKills);
                    fabricData.setFloat("AbyssPower", data.abyssPower);
                    fabricData.setLong("LastKillTime", data.lastKillTime);
                    break;

                case TEMPORAL:
                    fabricData.setInteger("RewindCount", data.rewindCount);
                    fabricData.setFloat("TemporalEnergy", data.temporalEnergy);
                    fabricData.setLong("LastTimeStop", data.lastTimeStopTime);
                    break;

                case SPATIAL:
                    fabricData.setFloat("StoredDamage", data.storedDamage);
                    fabricData.setFloat("DimensionalEnergy", data.dimensionalEnergy);
                    fabricData.setInteger("PhaseStrikeCount", data.phaseStrikeCount);
                    fabricData.setBoolean("CollapseReady", data.collapseFieldReady);
                    fabricData.setLong("LastCollapseTime", data.lastCollapseTime);
                    break;

                case OTHERWORLD:
                    fabricData.setInteger("Insight", (int)data.insight);
                    fabricData.setInteger("Sanity", (int)data.sanity);
                    fabricData.setInteger("ForbiddenKnowledge", data.forbiddenKnowledge);
                    fabricData.setInteger("AbyssGazeStacks", data.abyssGazeStacks);
                    break;
            }

            int power = calculateFabricPower(type, data);
            fabricData.setInteger("FabricPower", power);

            armorTag.setTag("WovenFabric", fabricData);
        }
    }

    private static int calculateFabricPower(UpdatedFabricPlayerData.FabricType type, PlayerFabricData data) {
        switch (type) {
            case ABYSS:
                return Math.min(100, (int)(data.abyssPower + data.abyssKills * 2));
            case TEMPORAL:
                return Math.min(100, (int)data.temporalEnergy);
            case SPATIAL:
                return Math.min(100, (int)data.dimensionalEnergy);
            case OTHERWORLD:
                return Math.min(100, 100 - (int)(100 - data.sanity));
            default:
                return 100;
        }
    }

    private static void loadFabricDataFromArmor(EntityPlayer player, PlayerFabricData data) {
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (!FabricWeavingSystem.hasFabric(armor)) continue;

            UpdatedFabricPlayerData.FabricType type = FabricWeavingSystem.getFabricType(armor);
            if (type == null) continue;

            NBTTagCompound fabricData = FabricWeavingSystem.getFabricData(armor);

            switch (type) {
                case ABYSS:
                    data.abyssKills = Math.max(data.abyssKills, fabricData.getInteger("AbyssKills"));
                    data.abyssPower = Math.max(data.abyssPower, fabricData.getFloat("AbyssPower"));
                    data.lastKillTime = Math.max(data.lastKillTime, fabricData.getLong("LastKillTime"));
                    break;

                case TEMPORAL:
                    data.rewindCount = Math.max(data.rewindCount, fabricData.getInteger("RewindCount"));
                    data.temporalEnergy = Math.max(data.temporalEnergy, fabricData.getFloat("TemporalEnergy"));
                    data.lastTimeStopTime = Math.max(data.lastTimeStopTime, fabricData.getLong("LastTimeStop"));
                    break;

                case SPATIAL:
                    data.storedDamage = Math.max(data.storedDamage, fabricData.getFloat("StoredDamage"));
                    data.dimensionalEnergy = Math.max(data.dimensionalEnergy, fabricData.getFloat("DimensionalEnergy"));
                    data.phaseStrikeCount = Math.max(data.phaseStrikeCount, fabricData.getInteger("PhaseStrikeCount"));
                    data.collapseFieldReady = fabricData.getBoolean("CollapseReady");
                    data.lastCollapseTime = Math.max(data.lastCollapseTime, fabricData.getLong("LastCollapseTime"));
                    break;

                case OTHERWORLD:
                    data.insight = Math.max(data.insight, fabricData.getInteger("Insight"));
                    data.sanity = Math.min(data.sanity, fabricData.getInteger("Sanity"));
                    data.forbiddenKnowledge = Math.max(data.forbiddenKnowledge, fabricData.getInteger("ForbiddenKnowledge"));
                    data.abyssGazeStacks = Math.max(data.abyssGazeStacks, fabricData.getInteger("AbyssGazeStacks"));
                    break;
            }
        }
    }

    // ========== 主要事件处理器 ==========

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        PlayerFabricData data = getPlayerData(player);

        loadFabricDataFromArmor(player, data);

        player.sendStatusMessage(new TextComponentString("§7[织印系统] 数据已加载"), true);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;
        PlayerFabricData data = getPlayerData(player);

        if (data.hasAbyssFabric()) removeAbyssEffects(player, data);
        if (data.hasTemporalFabric()) removeTemporalEffects(player, data);
        if (data.hasSpatialFabric()) removeSpatialEffects(player, data);
        if (data.hasOtherworldFabric()) removeOtherworldEffects(player, data);

        PLAYER_DATA.remove(player.getUniqueID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        PlayerFabricData data = getPlayerData(player);

        data.updateEquippedFabrics(player);

        if (data.hasEquipmentChanged()) {
            handleEquipmentChange(player, data);
        }

        if (player.ticksExisted % 10 == 0) {
            syncAllFabricDataToArmor(player, data);
        }

        if (player.ticksExisted % 20 == 0) {
            if (data.hasAbyssFabric()) {
                handleAbyssHunger(player, data);
                if (!hasAbyssModifiers(player)) {
                    applyAbyssAttributes(player, data);
                }
            } else if (data.abyssPower > 0 || data.abyssKills > 0) {
                data.abyssPower = Math.max(0, data.abyssPower - 2);
                data.abyssKills = Math.max(0, data.abyssKills - 1);
                if (data.abyssPower == 0 && data.abyssKills == 0) {
                    removeAbyssEffects(player, data);
                }
            }

            if (data.hasTemporalFabric()) {
                recordTemporalSnapshot(player, data);
                data.temporalEnergy = Math.min(100, data.temporalEnergy + 0.5f);
            } else if (data.temporalEnergy < 100) {
                data.temporalEnergy = Math.min(100, data.temporalEnergy + 1);
            }

            if (data.hasSpatialFabric()) {
                data.dimensionalEnergy = Math.min(100, data.dimensionalEnergy + 5);

                if (data.storedDamage > 50 && player.ticksExisted % 100 == 0) {
                    player.sendStatusMessage(new TextComponentString(
                            String.format("§d维度口袋: %.1f伤害已存储 %s",
                                    data.storedDamage, getStorageLevel(data.storedDamage))), true);
                }
            } else if (data.storedDamage > 0) {
                data.storedDamage = Math.max(0, data.storedDamage - 2);
            }

            if (data.hasOtherworldFabric()) {
                handleInsightAndSanity(player, data);
            } else if (data.insight > 0 || data.sanity < 100) {
                data.insight = Math.max(0, data.insight - 2);
                data.sanity = Math.min(100, data.sanity + 1);
            }

            syncAllFabricDataToArmor(player, data);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 处理时停区域
        ACTIVE_TIME_ZONES.forEach((dimId, zones) -> {
            zones.removeIf(zone -> {
                zone.tick();
                return !zone.active;
            });
        });

        // 处理崩塌领域
        Iterator<CollapseField> fieldIterator = ACTIVE_COLLAPSE_FIELDS.iterator();
        while (fieldIterator.hasNext()) {
            CollapseField field = fieldIterator.next();

            field.tick();

            if (field.ticksRemaining <= 0) {
                field.deactivate();
                fieldIterator.remove();
            }
        }

        if (tickCount++ % 100 == 0) {
            cleanupOfflinePlayers();
        }
    }// ========== 时空布料主要事件 - Shift+右键释放维度崩塌领域 ==========

    @SubscribeEvent
    public static void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        if (!player.isSneaking()) return;

        PlayerFabricData data = getPlayerData(player);

        // 修改：需要3件时空布料才能释放维度崩塌
        if (data.spatialCount < 3) {
            if (data.spatialCount > 0) {
                player.sendStatusMessage(new TextComponentString(
                        String.format("§c需要至少3件时空织印才能释放崩塌领域 (当前:%d件)",
                                data.spatialCount)), true);
            }
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - data.lastCollapseTime < PlayerFabricData.COLLAPSE_COOLDOWN) {
            long remainingCooldown = (PlayerFabricData.COLLAPSE_COOLDOWN - (currentTime - data.lastCollapseTime)) / 1000;
            player.sendStatusMessage(new TextComponentString(
                    String.format("§c维度崩塌领域冷却中... 剩余%d秒", remainingCooldown)), true);
            return;
        }

        float minDamageRequired = 30.0f;
        if (data.storedDamage < minDamageRequired) {
            player.sendStatusMessage(new TextComponentString(
                    String.format("§c需要至少%.0f点存储伤害才能释放崩塌领域 (当前:%.1f)",
                            minDamageRequired, data.storedDamage)), true);
            return;
        }

        activateDimensionalCollapse(player, data);
    }

    private static void activateDimensionalCollapse(EntityPlayer player, PlayerFabricData data) {
        World world = player.world;
        BlockPos targetPos = player.getPosition();

        float baseRadius = 6.0f + data.spatialCount * 2.0f;
        float radiusBonus = Math.min(10.0f, data.storedDamage / 50.0f);
        float totalRadius = baseRadius + radiusBonus;

        int duration = 100 + data.spatialCount * 20;

        float baseDamage = data.storedDamage * 2.0f;
        float equipmentMultiplier = 1.0f + data.spatialCount * 0.5f;
        float totalDamage = baseDamage * equipmentMultiplier;

        CollapseField field = new CollapseField(
                targetPos, world, totalRadius, duration,
                totalDamage, player, data.spatialCount
        );

        ACTIVE_COLLAPSE_FIELDS.add(field);

        float consumed = data.storedDamage * 0.8f;
        data.storedDamage -= consumed;
        data.dimensionalEnergy = Math.max(0, data.dimensionalEnergy - 30);
        data.lastCollapseTime = System.currentTimeMillis();

        createInitialShockwave(player, targetPos, totalRadius);

        player.sendStatusMessage(new TextComponentString(
                String.format("§5§l⟐ 维度崩塌领域展开！⟐\n§d范围:%.1f格 持续:%.1f秒 总伤害:%.0f",
                        totalRadius, duration/20.0f, totalDamage)), false);

        world.playSound(null, targetPos, SoundEvents.ENTITY_WITHER_SPAWN,
                SoundCategory.PLAYERS, 2.0F, 0.5F);

        syncAllFabricDataToArmor(player, data);
    }

    private static void createInitialShockwave(EntityPlayer player, BlockPos center, float radius) {
        World world = player.world;

        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                new AxisAlignedBB(center).grow(radius),
                e -> e != player && e.isEntityAlive()
        );

        for (EntityLivingBase entity : entities) {
            double distance = entity.getDistance(center.getX(), center.getY(), center.getZ());
            if (distance > radius) continue;

            Vec3d knockback = entity.getPositionVector()
                    .subtract(new Vec3d(center.getX() + 0.5, center.getY(), center.getZ() + 0.5))
                    .normalize();
            double force = 2.5 * (1 - distance / radius);
            entity.motionX = knockback.x * force;
            entity.motionY = 0.5 + force * 0.2;
            entity.motionZ = knockback.z * force;
            entity.velocityChanged = true;
        }

        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;

            for (int ring = 1; ring <= 3; ring++) {
                float ringRadius = radius * ring / 3;
                int particleCount = (int)(36 * ring / 3);

                for (int i = 0; i < particleCount; i++) {
                    double angle = (Math.PI * 2 * i) / particleCount;
                    double x = center.getX() + 0.5 + Math.cos(angle) * ringRadius;
                    double z = center.getZ() + 0.5 + Math.sin(angle) * ringRadius;

                    ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                            x, center.getY() + 0.5, z,
                            1, 0, 0.1, 0.0, 0);
                }
            }

            for (int y = 0; y < 20; y++) {
                ws.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                        center.getX() + 0.5, center.getY() + y, center.getZ() + 0.5,
                        5, 0.2, 0, 0.2, 0.02);
            }
        }
    }

    @SubscribeEvent
    public static void onSpatialDefense(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        PlayerFabricData data = getPlayerData(player);

        if (!data.hasSpatialFabric()) return;

        if (DAMAGE_AMPLIFICATION_BLACKLIST.contains(event.getSource().getDamageType())) {
            return;
        }

        // 时空之壁 - 反伤系统
        if (data.spatialCount >= 4 &&RANDOM.nextFloat() < data.spatialCount * 0.2f) {
            float damage = event.getAmount();
            boolean transferred = activateSpaceTimeBarrier(player, event.getSource(), damage, data);

            if (transferred) {
                event.setCanceled(true);

                float collected = damage * 0.5f;
                data.storedDamage += collected;

                data.dimensionalEnergy = Math.max(0, data.dimensionalEnergy - 10);

                player.sendStatusMessage(new TextComponentString(
                        String.format("§d⊡ 时空之壁！转移%.1f伤害，收集%.1f到口袋", damage, collected)), true);

                syncAllFabricDataToArmor(player, data);
                return;
            }
        }

        // 维度口袋 - 存储所有伤害
        if (data.spatialCount >= 2 && data.dimensionalEnergy > 5) {
            float storageRatio = 0.3f + (data.spatialCount - 2) * 0.2f;
            float damageToStore = event.getAmount() * storageRatio;

            data.storedDamage += damageToStore;
            event.setAmount(event.getAmount() - damageToStore);

            float energyCost = 2 + damageToStore * 0.05f;
            data.dimensionalEnergy = Math.max(0, data.dimensionalEnergy - energyCost);

            String storageLevel = getStorageLevel(data.storedDamage);
            player.sendStatusMessage(new TextComponentString(
                    String.format("§d%s 维度存储: +%.1f (总计:%.1f)",
                            storageLevel, damageToStore, data.storedDamage)), true);

            spawnDimensionalAbsorption(player, damageToStore, data.storedDamage);

            syncAllFabricDataToArmor(player, data);
        }
    }

    @SubscribeEvent
    public static void onSpatialAttack(LivingAttackEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        PlayerFabricData data = getPlayerData(player);

        if (!data.hasSpatialFabric()) return;

        EntityLivingBase target = event.getEntityLiving();

        if (RANDOM.nextFloat() < data.spatialCount * 0.15f) {
            target.hurtResistantTime = 0;
            target.hurtTime = 0;

            float phaseDamage = event.getAmount() * (1.5f + data.spatialCount * 0.5f);
            target.attackEntityFrom(
                    new DimensionalDamageSource("phaseDamage").setDamageBypassesArmor().setDamageIsAbsolute(),
                    phaseDamage
            );

            createDimensionalTear(target);

            data.phaseStrikeCount++;
            data.dimensionalEnergy = Math.max(0, data.dimensionalEnergy - 5);

            player.sendStatusMessage(new TextComponentString(
                    String.format("§d相位打击！%.1f真实伤害", phaseDamage)), true);

            syncAllFabricDataToArmor(player, data);
        }
    }

    @SubscribeEvent
    public static void onSpatialCrit(CriticalHitEvent event) {
        if (!(event.getEntityPlayer() instanceof EntityPlayer)) return;

        EntityPlayer player = event.getEntityPlayer();
        PlayerFabricData data = getPlayerData(player);

        if (!data.hasSpatialFabric() || data.storedDamage <= 0) return;

        if (event.getTarget() instanceof EntityLivingBase) {
            // 根据存储伤害增加暴击倍率，最多增加50%
            // 存储200伤害时达到上限
            float bonusMultiplier = 1.0f + Math.min(data.storedDamage / 100f, 2f);

            // 应用倍率增幅
            event.setDamageModifier(event.getDamageModifier() * bonusMultiplier);

            // 消耗少量存储伤害（可选）
            data.storedDamage = Math.max(0, data.storedDamage - 5);

            event.setResult(Event.Result.ALLOW);
            syncAllFabricDataToArmor(player, data);
        }
    }

    // ========== 深渊布料事件 ==========
    @SubscribeEvent
    public static void onAbyssKill(LivingDeathEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        PlayerFabricData data = getPlayerData(player);

        if (!data.hasAbyssFabric()) return;

        data.abyssKills++;
        data.lastKillTime = System.currentTimeMillis();

        float powerGain = calculateKillPower(event.getEntityLiving());
        data.abyssPower = Math.min(data.abyssPower + powerGain, 100.0f);

        float lifeSteal = data.abyssCount * 2.0f + powerGain;
        player.heal(lifeSteal);

        applyAbyssAttributes(player, data);

        if (data.abyssKills % (15 - data.abyssCount * 3) == 0) {
            triggerBerserkMode(player, data);
        }

        spawnAbyssParticles(player);

        if (data.abyssKills % 5 == 0) {
            player.sendStatusMessage(new TextComponentString(
                    String.format("§4深渊之力 §c+%.1f (击杀:%d)", data.abyssPower, data.abyssKills)), true);
        }

        syncAllFabricDataToArmor(player, data);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onAbyssDamage(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        PlayerFabricData data = getPlayerData(player);

        if (!data.hasAbyssFabric() || data.abyssPower <= 0) return;

        float multiplier = 1.0f + (data.abyssPower / 100.0f) * (0.5f * data.abyssCount);

        EntityLivingBase target = event.getEntityLiving();
        if (target.getHealth() < target.getMaxHealth() * 0.3f) {
            multiplier *= 1.5f;
            spawnExecuteParticles(target);
        }

        event.setAmount(event.getAmount() * multiplier);

        if (data.abyssCount >= 3) {
            applySplashDamage(player, target, event.getAmount() * 0.3f);
        }
    }

    // ========== 时序布料事件 ==========
    @SubscribeEvent
    public static void onTemporalDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        PlayerFabricData data = getPlayerData(player);

        if (!data.hasTemporalFabric() || data.temporalSnapshots.isEmpty()) return;

        float baseChance = Math.min(data.temporalCount * 0.25f, 0.75f);
        float actualChance = baseChance * (float)Math.pow(0.7, data.rewindCount);

        if (RANDOM.nextFloat() < actualChance) {
            event.setCanceled(true);
            performEnhancedTemporalRewind(player, data);
            createProtectiveTimeStop(player, data);
            data.rewindCount++;

            data.temporalEnergy = Math.max(0, data.temporalEnergy - 30);

            syncAllFabricDataToArmor(player, data);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTemporalHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        PlayerFabricData data = getPlayerData(player);

        if (!data.hasTemporalFabric()) return;

        float reduction = data.temporalCount * 0.15f;
        event.setAmount(event.getAmount() * (1 - reduction));

        if (shouldTriggerTimeStop(player, data)) {
            TimeStopZone zone = activateImprovedTimeStop(player, data);
            if (zone != null) {
                event.setAmount(event.getAmount() * 0.3f);
                data.lastTimeStopTime = System.currentTimeMillis();

                data.temporalEnergy = Math.max(0, data.temporalEnergy - 20);

                player.sendStatusMessage(new TextComponentString(
                        String.format("§b⏱ 时停领域展开！范围:%.1f格 持续:%.1f秒",
                                zone.range, zone.maxDuration / 20.0f)), true);

                syncAllFabricDataToArmor(player, data);
            }
        }

        if (shouldTriggerTimeRift(data)) {
            createAdvancedTemporalRift(player, event.getSource());
            event.setCanceled(true);

            data.temporalEnergy = Math.max(0, data.temporalEnergy - 10);

            player.sendStatusMessage(new TextComponentString(
                    "§b✦ 时间断层激活！伤害无效化"), true);

            syncAllFabricDataToArmor(player, data);
        }
    }

    // ========== 异界纤维事件 ==========
    @SubscribeEvent
    public static void onOtherworldTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        if (event.player.ticksExisted % 20 != 0) return;

        EntityPlayer player = event.player;
        PlayerFabricData data = getPlayerData(player);

        if (!data.hasOtherworldFabric()) return;

        processInsightEffects(player, data);

        if (data.sanity < 30) {
            applyMadnessEffects(player, data);
        }

        syncAllFabricDataToArmor(player, data);
    }

    // ========== 实体更新拦截 ==========
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (FROZEN_ENTITIES.containsKey(event.getEntityLiving().getUniqueID())) {
            event.getEntityLiving().motionY = 0;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();

        if (FROZEN_ENTITIES.containsKey(entity.getUniqueID())) {
            if (entity.isDead || entity.getHealth() <= 0) {
                FROZEN_ENTITIES.remove(entity.getUniqueID());
                return;
            }

            if (entity instanceof EntityDragon) {
                EntityDragon dragon = (EntityDragon) entity;
                if (dragon.getHealth() <= 0) {
                    FROZEN_ENTITIES.remove(entity.getUniqueID());
                    return;
                }
            }

            event.setCanceled(true);
            handleFrozenEntityTicks(entity);
        }
    }

    // ========== 所有辅助方法 ==========

    private static String getStorageLevel(float stored) {
        if (stored >= 500) return "§5§l⟐⟐⟐ 超载";
        if (stored >= 300) return "§d§l⟐⟐ 高能";
        if (stored >= 150) return "§d⟐ 充盈";
        if (stored >= 50) return "§b◈ 储能";
        return "◇ 微弱";
    }

    private static void handleEquipmentChange(EntityPlayer player, PlayerFabricData data) {
        if (data.lastAbyssCount > 0 && data.abyssCount == 0) {
            removeAbyssEffects(player, data);
        } else if (data.lastAbyssCount != data.abyssCount && data.abyssCount > 0) {
            applyAbyssAttributes(player, data);
        }

        if (data.lastTemporalCount > 0 && data.temporalCount == 0) {
            removeTemporalEffects(player, data);
        }

        if (data.lastSpatialCount > 0 && data.spatialCount == 0) {
            removeSpatialEffects(player, data);
        }

        if (data.lastOtherworldCount > 0 && data.otherworldCount == 0) {
            removeOtherworldEffects(player, data);
        }
    }

    private static void removeAbyssEffects(EntityPlayer player, PlayerFabricData data) {
        IAttributeInstance attack = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(ABYSS_ATTACK_UUID);
        }

        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(ABYSS_SPEED_UUID);
        }

        data.berserkMode = false;
        data.berserkEndTime = 0;
        data.abyssPower = 0;
        data.abyssKills = 0;
        data.lastKillTime = 0;

        player.sendStatusMessage(new TextComponentString("§4深渊之力消散..."), true);
    }

    private static void removeTemporalEffects(EntityPlayer player, PlayerFabricData data) {
        if (data.chronoAccelerated) {
            removeChronoAttributes(player, data);
        }

        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(TEMPORAL_SPEED_UUID);
        }

        IAttributeInstance attackSpeed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(TEMPORAL_ATTACK_UUID);
        }

        data.temporalSnapshots.clear();
        data.rewindCount = 0;
        data.temporalEnergy = 100f;

        player.sendStatusMessage(new TextComponentString("§b时间流恢复正常..."), true);
    }

    private static void removeSpatialEffects(EntityPlayer player, PlayerFabricData data) {
        if (data.storedDamage > 0) {
            player.sendStatusMessage(new TextComponentString(
                    String.format("§d存储的%.1f点伤害消散了", data.storedDamage)), true);
            data.storedDamage = 0;
        }

        data.phaseStrikeCount = 0;
        data.dimensionalEnergy = 100;
        data.collapseFieldReady = false;
    }

    private static void removeOtherworldEffects(EntityPlayer player, PlayerFabricData data) {
        if (data.sanity < 100) {
            player.sendStatusMessage(new TextComponentString("§5理智逐渐恢复..."), true);
        }

        data.abyssGazeStacks = 0;
        data.forbiddenKnowledge = 0;
        data.insight = 0;
        data.sanity = 100;
    }

    private static boolean hasAbyssModifiers(EntityPlayer player) {
        IAttributeInstance attack = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        return attack != null && attack.getModifier(ABYSS_ATTACK_UUID) != null;
    }

    private static void handleAbyssHunger(EntityPlayer player, PlayerFabricData data) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - data.lastKillTime > 10000 && data.abyssPower > 0) {
            data.abyssPower = Math.max(0, data.abyssPower - 1);

            float damage = data.abyssCount + data.abyssKills * 0.1f;
            player.attackEntityFrom(DamageSource.WITHER.setDamageBypassesArmor(), damage);

            if (data.abyssKills > 0) {
                data.abyssKills--;
            }

            applyAbyssAttributes(player, data);

            syncAllFabricDataToArmor(player, data);
        }
    }

    private static void applyAbyssAttributes(EntityPlayer player, PlayerFabricData data) {
        IAttributeInstance attack = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(ABYSS_ATTACK_UUID);
            if (data.hasAbyssFabric() && (data.abyssKills > 0 || data.abyssPower > 0)) {
                double bonus = data.abyssKills * 0.5 + data.abyssPower * 0.2;
                attack.applyModifier(new AttributeModifier(
                        ABYSS_ATTACK_UUID, "Abyss Attack", bonus, 0));
            }
        }

        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(ABYSS_SPEED_UUID);
            if (data.hasAbyssFabric() && data.abyssKills > 0) {
                double bonus = Math.min(data.abyssKills * 0.01, 0.5);
                speed.applyModifier(new AttributeModifier(
                        ABYSS_SPEED_UUID, "Abyss Speed", bonus, 2));
            }
        }
    }

    private static void triggerBerserkMode(EntityPlayer player, PlayerFabricData data) {
        data.berserkMode = true;
        data.berserkEndTime = System.currentTimeMillis() + 10000;

        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, player.getEntityBoundingBox().grow(5),
                e -> e != player && e.isEntityAlive());

        for (EntityLivingBase entity : entities) {
            Vec3d knockback = entity.getPositionVector()
                    .subtract(player.getPositionVector()).normalize().scale(2.0);
            entity.motionX = knockback.x;
            entity.motionY = 0.5;
            entity.motionZ = knockback.z;
            entity.attackEntityFrom(DamageSource.causePlayerDamage(player), 5.0f);
        }

        player.world.playSound(null, player.getPosition(),
                SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    private static float calculateKillPower(EntityLivingBase entity) {
        float health = entity.getMaxHealth();
        if (entity instanceof EntityWither || entity instanceof EntityDragon) return 10.0f;
        if (health > 100) return 5.0f;
        if (health > 50) return 3.0f;
        if (health > 20) return 1.5f;
        return 0.5f;
    }

    private static void applySplashDamage(EntityPlayer player, EntityLivingBase target, float damage) {
        List<EntityLivingBase> nearbyTargets = target.world.getEntitiesWithinAABB(
                EntityLivingBase.class, target.getEntityBoundingBox().grow(3),
                e -> e != target && e != player && e.isEntityAlive());

        for (EntityLivingBase splash : nearbyTargets) {
            splash.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);
        }
    }

    private static void recordTemporalSnapshot(EntityPlayer player, PlayerFabricData data) {
        PlayerFabricData.TemporalSnapshot snapshot = new PlayerFabricData.TemporalSnapshot(
                player.posX, player.posY, player.posZ,
                player.getHealth(), player.getFoodStats().getFoodLevel(),
                player.experienceTotal
        );

        data.temporalSnapshots.add(snapshot);

        while (data.temporalSnapshots.size() > 5) {
            data.temporalSnapshots.removeFirst();
        }
    }

    private static void performEnhancedTemporalRewind(EntityPlayer player, PlayerFabricData data) {
        if (data.temporalSnapshots.isEmpty()) return;

        PlayerFabricData.TemporalSnapshot snapshot = data.temporalSnapshots.getFirst();

        player.setPositionAndUpdate(snapshot.x, snapshot.y, snapshot.z);

        // 修改：直接回滿血
        player.setHealth(player.getMaxHealth());
        player.getFoodStats().setFoodLevel(20);  // 滿飢餓值
        player.getFoodStats().setFoodSaturationLevel(20.0F);  // 滿飽食度

        player.hurtResistantTime = 60;


        // 新增：再生效果，提供第二次恢復
        player.addPotionEffect(new PotionEffect(MobEffects.INSTANT_HEALTH, 10, 10, false, false));
        // 立即額外恢復一次
        player.heal(player.getMaxHealth());

        spawnRewindEffects(player);

        player.sendStatusMessage(new TextComponentString(
                String.format("§b⏮ 時序回溯成功！完全恢復！(第%d次)", data.rewindCount + 1)), false);
    }

    private static void createProtectiveTimeStop(EntityPlayer player, PlayerFabricData data) {
        createTimeStopZone(player.world, player.getPosition(), 10.0, player, 60);

        player.setEntityInvulnerable(true);

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                player.setEntityInvulnerable(false);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void createAdvancedTemporalRift(EntityPlayer player, DamageSource source) {
        player.noClip = true;
        player.setEntityInvulnerable(true);

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                player.noClip = false;
                player.setEntityInvulnerable(false);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        if (source.getTrueSource() instanceof EntityLivingBase) {
            EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();
            freezeEntity(attacker, 60);
            attacker.attackEntityFrom(DamageSource.MAGIC, 10.0f);
        }

        spawnRiftEffects(player);
    }

    private static TimeStopZone activateImprovedTimeStop(EntityPlayer player, PlayerFabricData data) {
        World world = player.world;

        double range;
        int duration;

        switch (data.temporalCount) {
            case 1:
                range = 8.0;
                duration = 60;
                break;
            case 2:
                range = 12.0;
                duration = 100;
                break;
            case 3:
                range = 16.0;
                duration = 140;
                break;
            case 4:
                range = 20.0;
                duration = 200;
                break;
            default:
                return null;
        }

        TimeStopZone zone = createTimeStopZone(world, player.getPosition(), range, player, duration);

        if (data.temporalCount >= 4 && zone != null) {
            applyChronoAcceleration(player, data, duration);
        }

        spawnTimeStopEffects(player, range);

        return zone;
    }

    private static TimeStopZone createTimeStopZone(World world, BlockPos center, double range,
                                                   EntityPlayer caster, int duration) {
        if (world.isRemote) return null;

        TimeStopZone zone = new TimeStopZone(world, center, range, caster, duration);

        int dimId = world.provider.getDimension();
        List<TimeStopZone> zones = ACTIVE_TIME_ZONES.computeIfAbsent(dimId, k -> new ArrayList<>());
        zones.add(zone);

        world.playSound(null, center, SoundEvents.ENTITY_EVOCATION_ILLAGER_AMBIENT,
                SoundCategory.PLAYERS, 1.0F, 0.5F);

        return zone;
    }

    private static void applyChronoAcceleration(EntityPlayer player, PlayerFabricData data, int duration) {
        data.chronoAccelerated = true;
        data.chronoEndTime = System.currentTimeMillis() + (duration * 50);

        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(TEMPORAL_SPEED_UUID);
            speed.applyModifier(new AttributeModifier(
                    TEMPORAL_SPEED_UUID, "Chrono Acceleration", 1.0, 2));
        }

        IAttributeInstance attackSpeed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(TEMPORAL_ATTACK_UUID);
            attackSpeed.applyModifier(new AttributeModifier(
                    TEMPORAL_ATTACK_UUID, "Chrono Attack Speed", 1.0, 2));
        }

        player.addPotionEffect(new PotionEffect(MobEffects.HASTE, duration, 4, false, false));

        new Thread(() -> {
            try {
                Thread.sleep(duration * 50);
                removeChronoAttributes(player, data);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        player.sendStatusMessage(new TextComponentString(
                "§b⚡ 时间加速！你的速度超越了时间！"), false);
    }

    private static void removeChronoAttributes(EntityPlayer player, PlayerFabricData data) {
        data.chronoAccelerated = false;

        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(TEMPORAL_SPEED_UUID);
        }

        IAttributeInstance attackSpeed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            attackSpeed.removeModifier(TEMPORAL_ATTACK_UUID);
        }

        if (player instanceof EntityPlayerMP) {
            player.sendStatusMessage(new TextComponentString("§7时间加速结束"), true);
        }
    }

    private static void freezeEntity(EntityLivingBase entity, int duration) {
        FrozenEntityData data = new FrozenEntityData(entity);
        FROZEN_ENTITIES.put(entity.getUniqueID(), data);

        new Thread(() -> {
            try {
                Thread.sleep(duration * 50);
                FrozenEntityData stored = FROZEN_ENTITIES.remove(entity.getUniqueID());
                if (stored != null && entity.isEntityAlive()) {
                    stored.restore(entity);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void handleFrozenEntityTicks(EntityLivingBase entity) {
        FrozenEntityData data = FROZEN_ENTITIES.get(entity.getUniqueID());

        if (entity.hurtTime > 0) {
            entity.hurtTime--;
        }
        if (entity.hurtResistantTime > 0) {
            entity.hurtResistantTime--;
        }

        if (data != null) {
            entity.rotationYaw = data.yaw;
            entity.rotationPitch = data.pitch;
            entity.rotationYawHead = data.yawHead;
            entity.renderYawOffset = data.yaw;

            entity.prevRotationYaw = data.yaw;
            entity.prevRotationPitch = data.pitch;
            entity.prevRotationYawHead = data.yawHead;
            entity.prevRenderYawOffset = data.yaw;
        }

        entity.prevPosX = entity.posX;
        entity.prevPosY = entity.posY;
        entity.prevPosZ = entity.posZ;

        entity.limbSwing = 0;
        entity.limbSwingAmount = 0;
        entity.prevLimbSwingAmount = 0;
        entity.swingProgress = 0;
        entity.prevSwingProgress = 0;
        entity.swingProgressInt = 0;

        entity.distanceWalkedModified = 0;
        entity.distanceWalkedOnStepModified = 0;
        entity.prevDistanceWalkedModified = 0;

        entity.cameraPitch = 0;
        entity.prevCameraPitch = 0;
        entity.rotationYawHead = entity.rotationYaw;
        entity.prevRotationYawHead = entity.rotationYaw;

        entity.motionX = 0;
        entity.motionY = 0;
        entity.motionZ = 0;
        entity.velocityChanged = true;
        entity.isAirBorne = false;
        entity.onGround = true;

        entity.setAir(300);

        if (entity.world instanceof WorldServer && entity.ticksExisted % 5 == 0) {
            spawnFrozenParticles(entity);
        }
    }

    private static boolean shouldTriggerTimeStop(EntityPlayer player, PlayerFabricData data) {
        if (System.currentTimeMillis() - data.lastTimeStopTime < TIME_STOP_COOLDOWN) {
            return false;
        }

        float chance = data.temporalCount * TIME_STOP_CHANCE_PER_PIECE;

        if (player.getHealth() < player.getMaxHealth() * 0.3f) {
            chance *= 2;
        }

        if (data.temporalCount >= 4) {
            chance += 0.1f;
        }

        return RANDOM.nextFloat() < chance;
    }

    private static boolean shouldTriggerTimeRift(PlayerFabricData data) {
        float chance = data.temporalCount * TIME_RIFT_CHANCE_PER_PIECE;

        if (data.temporalCount >= 4) {
            chance += 0.05f;
        }

        return RANDOM.nextFloat() < chance;
    }

    private static boolean activateSpaceTimeBarrier(EntityPlayer player, DamageSource source,
                                                    float damage, PlayerFabricData data) {
        double range = 5.0 + data.spatialCount * 2.0;
        float multiplier = 1.5f + data.spatialCount * 0.5f;
        float transferDamage = damage * multiplier;

        List<EntityLivingBase> targets = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                player.getEntityBoundingBox().grow(range),
                entity -> entity != player &&
                        entity.isEntityAlive() &&
                        !(entity instanceof EntityPlayer) &&
                        entity.getDistanceSq(player) <= range * range
        );

        if (targets.isEmpty()) {
            createSpaceDistortion(player, damage, data);
            return true;
        }

        float damagePerTarget = transferDamage / targets.size();

        for (EntityLivingBase target : targets) {
            float distance = target.getDistance(player);
            float distanceFactor = 1.0f - (distance / (float)range) * 0.3f;
            float finalDamage = damagePerTarget * distanceFactor;

            target.attackEntityFrom(
                    new DimensionalDamageSource("spaceTimeBarrier"),
                    finalDamage
            );

            if (player.world instanceof WorldServer) {
                spawnBarrierEffect(player, target);
            }
        }

        spawnBarrierShield(player, range);

        player.world.playSound(null, player.getPosition(),
                SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 0.5F);

        return true;
    }

    private static void createDimensionalTear(EntityLivingBase target) {
        if (!(target.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) target.world;

        world.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                target.posX, target.posY + target.height / 2, target.posZ,
                20, 0.5, 0.5, 0.5, 0.05);

        List<EntityLivingBase> nearby = target.world.getEntitiesWithinAABB(
                EntityLivingBase.class, target.getEntityBoundingBox().grow(3),
                e -> e != target);

        for (EntityLivingBase entity : nearby) {
            Vec3d knockback = entity.getPositionVector()
                    .subtract(target.getPositionVector()).normalize().scale(1.5);
            entity.motionX = knockback.x;
            entity.motionY = knockback.y;
            entity.motionZ = knockback.z;
        }
    }

    private static void createSpaceDistortion(EntityPlayer player, float damage, PlayerFabricData data) {
        data.storedDamage += damage * 0.5f;

        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        for (int i = 0; i < 20; i++) {
            double angle = (Math.PI * 2 * i) / 20;
            for (double r = 1; r <= 3; r += 0.5) {
                double x = player.posX + Math.cos(angle) * r;
                double z = player.posZ + Math.sin(angle) * r;
                world.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                        x, player.posY + 1, z,
                        1, 0, 0.2, 0, 0.05);
            }
        }

        player.sendStatusMessage(new TextComponentString(
                String.format("§d⊡ 时空之壁！能量储存至虚空（+%.1f）", damage * 0.5f)), true);
    }

    private static void handleInsightAndSanity(EntityPlayer player, PlayerFabricData data) {
        data.insight = Math.min(data.insight + data.otherworldCount, 100);
        data.sanity = Math.max(data.sanity - data.otherworldCount * 0.5f, 0);

        if (player.world.getLight(player.getPosition()) > 10) {
            data.sanity = Math.min(data.sanity + 1, 100);
        }
    }

    private static void processInsightEffects(EntityPlayer player, PlayerFabricData data) {
        if (data.insight >= 10) {
            revealInvisibleEntities(player);
        }

        if (data.insight >= 30 && RANDOM.nextFloat() < 0.05f) {
            whisperForbiddenKnowledge(player, data);
        }

        if (data.insight >= 50 && RANDOM.nextFloat() < 0.02f) {
            grantForbiddenKnowledge(player, data);
        }

        if (data.insight >= 70 && RANDOM.nextFloat() < 0.01f) {
            gazeIntoTheAbyss(player, data);
        }
    }

    private static void revealInvisibleEntities(EntityPlayer player) {
        List<Entity> entities = player.world.getEntitiesWithinAABBExcludingEntity(
                player, player.getEntityBoundingBox().grow(20));

        for (Entity entity : entities) {
            if (entity.isInvisible() && entity instanceof EntityLivingBase) {
                if (player.world instanceof WorldServer) {
                    ((WorldServer)player.world).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                            entity.posX, entity.posY + entity.height, entity.posZ,
                            3, 0.1, 0.1, 0.1, 0);
                }
            }
        }
    }

    private static void whisperForbiddenKnowledge(EntityPlayer player, PlayerFabricData data) {
        String[] whispers = {
                "§5§o他们在看着你...",
                "§5§o不要相信影子...",
                "§5§o真相就在眼前...",
                "§5§o醒来...醒来...",
                "§5§o深渊在凝视..."
        };
        player.sendStatusMessage(new TextComponentString(
                whispers[RANDOM.nextInt(whispers.length)]), true);
        data.sanity -= 3;
        data.abyssGazeStacks++;
    }

    private static void grantForbiddenKnowledge(EntityPlayer player, PlayerFabricData data) {
        data.forbiddenKnowledge++;

        switch (RANDOM.nextInt(4)) {
            case 0:
                player.capabilities.allowFlying = true;
                player.sendPlayerAbilities();
                break;
            case 1:
                player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 200, 1, false, false));
                break;
            case 2:
                player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 200, 0, false, false));
                break;
            case 3:
                player.heal(10);
                break;
        }

        data.sanity -= 10;
    }

    private static void gazeIntoTheAbyss(EntityPlayer player, PlayerFabricData data) {
        player.sendStatusMessage(new TextComponentString(
                "§5§l当你凝视深渊时，深渊也在凝视着你..."), false);

        data.sanity = Math.max(0, data.sanity - 30);
        data.abyssGazeStacks += 5;

        IAttributeInstance attack = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.applyModifier(new AttributeModifier(
                    UUID.randomUUID(), "Abyss Gaze", 10, 0));
        }
    }

    private static void applyMadnessEffects(EntityPlayer player, PlayerFabricData data) {
        int madnessLevel = (30 - (int)data.sanity) / 10;

        switch (madnessLevel) {
            case 0:
                break;
            case 1:
                if (RANDOM.nextFloat() < 0.1f) {
                    double x = player.posX + (RANDOM.nextDouble() - 0.5) * 3;
                    double z = player.posZ + (RANDOM.nextDouble() - 0.5) * 3;
                    player.setPositionAndUpdate(x, player.posY, z);
                }
                break;
            case 2:
                player.attackEntityFrom(DamageSource.MAGIC, 1);
                break;
            default:
                if (RANDOM.nextFloat() < 0.01f) {
                    player.attackEntityFrom(DamageSource.OUT_OF_WORLD, 1000);
                }
                break;
        }
    }

    // ========== 粒子效果 ==========

    private static void spawnDimensionalAbsorption(EntityPlayer player, float absorbed, float total) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        int particleCount = Math.min(50, (int)(absorbed * 2) + (int)(total / 50));

        for (int i = 0; i < particleCount; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double distance = RANDOM.nextDouble() * 3 + 1;

            double x = player.posX + Math.cos(angle) * distance;
            double z = player.posZ + Math.sin(angle) * distance;
            double y = player.posY + RANDOM.nextDouble() * player.height;

            Vec3d motion = new Vec3d(player.posX - x, player.posY + player.height/2 - y, player.posZ - z);
            motion = motion.normalize().scale(0.2);

            EnumParticleTypes particle = total > 100 ? EnumParticleTypes.DRAGON_BREATH : EnumParticleTypes.PORTAL;

            world.spawnParticle(particle, x, y, z, 1, motion.x, motion.y, motion.z, 0.05);
        }
    }

    private static void spawnBarrierEffect(EntityPlayer player, EntityLivingBase target) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        Vec3d start = new Vec3d(player.posX, player.posY + player.height/2, player.posZ);
        Vec3d end = new Vec3d(target.posX, target.posY + target.height/2, target.posZ);
        Vec3d diff = end.subtract(start);

        for (int i = 0; i <= 10; i++) {
            Vec3d pos = start.add(diff.scale(i / 10.0));
            world.spawnParticle(EnumParticleTypes.PORTAL,
                    pos.x, pos.y, pos.z,
                    1, 0, 0, 0, 0.1);
        }

        world.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                target.posX, target.posY + target.height/2, target.posZ,
                20, 0.5, 0.5, 0.5, 0.1);
    }

    private static void spawnBarrierShield(EntityPlayer player, double range) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        for (int i = 0; i < 36; i++) {
            double angle = (Math.PI * 2 * i) / 36;

            double x = player.posX + Math.cos(angle) * range;
            double z = player.posZ + Math.sin(angle) * range;
            world.spawnParticle(EnumParticleTypes.END_ROD,
                    x, player.posY + 1, z,
                    1, 0, 0.1, 0, 0.02);

            double y = player.posY + Math.sin(angle) * range;
            double xz = Math.cos(angle) * range;
            world.spawnParticle(EnumParticleTypes.END_ROD,
                    player.posX + xz, y + 1, player.posZ,
                    1, 0, 0.1, 0, 0.02);
        }
    }

    private static void spawnAbyssParticles(EntityPlayer player) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        for (int i = 0; i < 10; i++) {
            double angle = (Math.PI * 2 * i) / 10;
            double x = player.posX + Math.cos(angle) * 1.5;
            double z = player.posZ + Math.sin(angle) * 1.5;
            world.spawnParticle(EnumParticleTypes.FLAME, x, player.posY + 0.5, z,
                    1, 0, 0.1, 0, 0.05);
        }
    }

    private static void spawnTimeStopEffects(EntityPlayer player, double range) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < 36; i++) {
                double angle = (Math.PI * 2 * i) / 36;
                double r = range * (ring + 1) / 3;
                double x = player.posX + Math.cos(angle) * r;
                double z = player.posZ + Math.sin(angle) * r;

                world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                        x, player.posY + 1, z,
                        1, 0, 0, 0.0, 0);
            }
        }

        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2 * i) / 12;
            for (double r = 0; r < range; r += 2) {
                double x = player.posX + Math.cos(angle) * r;
                double z = player.posZ + Math.sin(angle) * r;

                world.spawnParticle(EnumParticleTypes.END_ROD,
                        x, player.posY + 0.1, z,
                        1, 0, 0.1, 0.0, 0);
            }
        }
    }

    private static void spawnRewindEffects(EntityPlayer player) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        for (int i = 0; i < 100; i++) {
            double angle = (Math.PI * 2 * i) / 20;
            double radius = 4 - (i / 100.0) * 4;
            double height = i / 20.0;
            double x = player.posX + Math.cos(angle) * radius;
            double z = player.posZ + Math.sin(angle) * radius;

            world.spawnParticle(EnumParticleTypes.PORTAL,
                    x, player.posY + height, z,
                    2, 0, 0, 0.0, 0);
        }
    }

    private static void spawnRiftEffects(EntityPlayer player) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        for (int i = 0; i < 30; i++) {
            Vec3d random = new Vec3d(
                    (RANDOM.nextDouble() - 0.5) * 3,
                    (RANDOM.nextDouble() - 0.5) * 3,
                    (RANDOM.nextDouble() - 0.5) * 3
            );

            world.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                    player.posX + random.x,
                    player.posY + 1 + random.y,
                    player.posZ + random.z,
                    1, 0, 0, 0, 0.05);
        }
    }

    private static void spawnFrozenParticles(EntityLivingBase entity) {
        if (!(entity.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) entity.world;

        for (int i = 0; i < 3; i++) {
            double x = entity.posX + (RANDOM.nextDouble() - 0.5) * entity.width * 2;
            double y = entity.posY + RANDOM.nextDouble() * entity.height;
            double z = entity.posZ + (RANDOM.nextDouble() - 0.5) * entity.width * 2;

            world.spawnParticle(EnumParticleTypes.END_ROD,
                    x, y, z,
                    1, 0, 0.05, 0.0, 0);
        }
    }

    private static void spawnExecuteParticles(EntityLivingBase target) {
        if (!(target.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) target.world;

        world.spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR,
                target.posX, target.posY + target.height, target.posZ,
                20, 0.5, 0.5, 0.5, 0.1);
    }

    /**
     * 清理离线玩家数据
     */
    private static void cleanupOfflinePlayers() {
        Set<UUID> toRemove = new HashSet<>();

        for (UUID playerId : PLAYER_DATA.keySet()) {
            boolean isOnline = false;

            try {
                for (WorldServer world : net.minecraftforge.common.DimensionManager.getWorlds()) {
                    if (world != null && world.getPlayerEntityByUUID(playerId) != null) {
                        isOnline = true;
                        break;
                    }
                }
            } catch (Exception e) {
                // 忽略异常
            }

            if (!isOnline) {
                toRemove.add(playerId);
            }
        }

        for (UUID id : toRemove) {
            PlayerFabricData data = PLAYER_DATA.get(id);
            if (data != null) {
                ACTIVE_TIME_ZONES.forEach((dimId, zones) -> {
                    zones.removeIf(zone -> {
                        if (zone.caster != null && zone.caster.getUniqueID().equals(id)) {
                            zone.deactivate();
                            return true;
                        }
                        return false;
                    });
                });

                ACTIVE_COLLAPSE_FIELDS.removeIf(field -> field.casterId.equals(id));
            }

            PLAYER_DATA.remove(id);
        }
    }

}  // 类结束