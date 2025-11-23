package com.moremod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

public class EntityRiftPortal extends Entity {

    // ===== 新增：跨门/跨维全局冷却（写在实体 NBT 上） =====
    private static final String RIFT_COOLDOWN_NBT = "moremodRiftCooldownUntil";
    private static final int GLOBAL_COOLDOWN_TICKS = 40; // 2s，必要时加大到 60~80

    private BlockPos destinationPos;
    private int destinationDim;
    public UUID ownerUUID;
    private int lifetime = 6000;

    // 保留：仅用于“本门内的二次判定消抖”（可选）
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private static final int COOLDOWN_TICKS = 40;

    private float rotationAngle = 0;

    public EntityRiftPortal(World world) {
        super(world);
        this.setSize(3.0F, 3.0F);
        this.noClip = true;
        this.isImmuneToFire = true;
    }

    public EntityRiftPortal(World world, BlockPos pos, BlockPos destPos, int destDim, UUID owner) {
        this(world);
        this.setPosition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        this.destinationPos = destPos;
        this.destinationDim = destDim;
        this.ownerUUID = owner;
    }

    @Override
    protected void entityInit() {}

    @Override
    public void onUpdate() {
        super.onUpdate();

        rotationAngle += 2.0F;
        if (rotationAngle >= 360.0F) rotationAngle = 0.0F;

        if (world.isRemote) {
            spawnClientParticles();
        } else {
            lifetime--;
            if (lifetime <= 0) { closePortal(); return; }

            if (ticksExisted % 20 == 0) spawnServerParticles();

            handleTeleportation();

            if (ticksExisted % 100 == 0) cleanupCooldowns();
        }
    }

    private void handleTeleportation() {
        List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(this,
                this.getEntityBoundingBox().grow(0.5D));

        long now = world.getTotalWorldTime();

        for (Entity entity : entities) {
            // 1) 全局 NBT 冷却：任何门都尊重
            if (getGlobalCooldown(entity) > now) continue;

            // 2) 本门局部冷却（同一门内抖动）
            UUID uuid = entity.getUniqueID();
            Long doorCD = teleportCooldowns.get(uuid);
            if (doorCD != null && now < doorCD) continue;

            if (teleportEntity(entity)) {
                // 门内冷却
                teleportCooldowns.put(uuid, now + COOLDOWN_TICKS);
                // 全局冷却（写到实体上）
                setGlobalCooldown(entity, now + GLOBAL_COOLDOWN_TICKS);

                // 声音
                world.playSound(null, entity.posX, entity.posY, entity.posZ,
                        SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            }
        }
    }

    // ===== 全局冷却读/写（实体 NBT） =====
    private static long getGlobalCooldown(Entity e) {
        NBTTagCompound tag = e.getEntityData();
        return tag.hasKey(RIFT_COOLDOWN_NBT) ? tag.getLong(RIFT_COOLDOWN_NBT) : 0L;
    }
    private static void setGlobalCooldown(Entity e, long until) {
        e.getEntityData().setLong(RIFT_COOLDOWN_NBT, until);
    }

    private boolean teleportEntity(Entity entity) {
        if (destinationPos == null) return false;

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return false;

        WorldServer destWorld = server.getWorld(destinationDim);
        if (destWorld == null) return false;

        final double destX = destinationPos.getX() + 0.5D;
        final double destY = destinationPos.getY() + 0.25D; // 上浮一点，避免落地重叠
        final double destZ = destinationPos.getZ() + 0.5D;

        // 跨维
        if (entity.dimension != destinationDim) {
            if (entity instanceof EntityPlayerMP) {
                EntityPlayerMP player = (EntityPlayerMP) entity;

                // 先给“出发前”的对象上全局冷却（以防万一）
                setGlobalCooldown(player, world.getTotalWorldTime() + GLOBAL_COOLDOWN_TICKS);

                // 传送
                player.changeDimension(destinationDim, new RiftTeleporter(destWorld, destinationPos));

                // 抵达后校正位置（有些传送器会略有偏差）
                player.connection.setPlayerLocation(destX, destY, destZ, player.rotationYaw, player.rotationPitch);

                // 再次写全局冷却（确保目标世界生效）
                setGlobalCooldown(player, destWorld.getTotalWorldTime() + GLOBAL_COOLDOWN_TICKS);

                player.sendStatusMessage(
                        new net.minecraft.util.text.TextComponentString(
                                net.minecraft.util.text.TextFormatting.LIGHT_PURPLE + "⟐ 穿越维度裂隙..."), true);

            } else if (entity instanceof EntityLivingBase) {
                // 非玩家的 changeDimension 会新建一个实体并返回
                Entity newEnt = entity.changeDimension(destinationDim);
                if (newEnt != null) {
                    newEnt.setPositionAndUpdate(destX, destY, destZ);
                    // 给“新实体”写上全局冷却
                    setGlobalCooldown(newEnt, destWorld.getTotalWorldTime() + GLOBAL_COOLDOWN_TICKS);
                }
            } else {
                return false; // 其他实体（物品等）暂不支持
            }
        } else {
            // 同维
            entity.setPositionAndUpdate(destX, destY, destZ);
            setGlobalCooldown(entity, world.getTotalWorldTime() + GLOBAL_COOLDOWN_TICKS);
        }

        // 目标位置粒子（明确 WorldServer 重载）
        destWorld.spawnParticle(
                EnumParticleTypes.PORTAL,
                false,
                destX, destY + 0.75D, destZ,
                50,
                0.5D, 0.5D, 0.5D,
                0.1D,
                new int[0]
        );

        return true;
    }

    @SideOnly(Side.CLIENT)
    private void spawnClientParticles() {
        for (int i = 0; i < 3; i++) {
            double angle = (rotationAngle + i * 120) * Math.PI / 180.0D;
            double radius = 1.5D;

            double px = posX + Math.cos(angle) * radius;
            double pz = posZ + Math.sin(angle) * radius;

            for (int y = 0; y < 3; y++) {
                world.spawnParticle(EnumParticleTypes.PORTAL, px, posY + y, pz, 0.0D, 0.1D, 0.0D);
                world.spawnParticle(EnumParticleTypes.END_ROD, px, posY + y, pz, 0.0D, 0.05D, 0.0D);
            }
        }

        if (ticksExisted % 2 == 0) {
            double spiralAngle = (ticksExisted * 10) * Math.PI / 180.0D;
            double spiralRadius = 0.5D;
            double spiralHeight = (ticksExisted % 60) / 20.0D;

            world.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                    posX + Math.cos(spiralAngle) * spiralRadius,
                    posY + spiralHeight,
                    posZ + Math.sin(spiralAngle) * spiralRadius,
                    0.0D, 0.0D, 0.0D);
        }
    }

    private void spawnServerParticles() {
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(
                    EnumParticleTypes.PORTAL,
                    false,
                    posX, posY + 1.5D, posZ,
                    20,
                    1.0D, 1.0D, 1.0D,
                    0.05D,
                    new int[0]
            );
        }
    }

    private void closePortal() {
        if (!world.isRemote) {
            world.playSound(null, posX, posY, posZ,
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.0F, 0.8F);

            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(
                        EnumParticleTypes.EXPLOSION_LARGE,
                        false,
                        posX, posY + 1.5D, posZ,
                        1,
                        0.0D, 0.0D, 0.0D,
                        0.0D,
                        new int[0]
                );
            }

            if (ownerUUID != null) {
                EntityPlayer owner = world.getPlayerEntityByUUID(ownerUUID);
                if (owner != null) {
                    owner.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                            net.minecraft.util.text.TextFormatting.GRAY + "⟐ 维度裂隙已关闭"), true);
                }
            }
        }
        this.setDead();
    }

    private void cleanupCooldowns() {
        long now = world.getTotalWorldTime();
        teleportCooldowns.entrySet().removeIf(e -> now >= e.getValue());
    }

    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean canBePushed() { return false; }
    @Override public boolean attackEntityFrom(DamageSource source, float amount) { return false; }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return this.getEntityBoundingBox().grow(2.0D);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 256.0D;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        if (destinationPos != null) {
            tag.setLong("DestPos", destinationPos.toLong());
            tag.setInteger("DestDim", destinationDim);
        }
        if (ownerUUID != null) tag.setString("Owner", ownerUUID.toString());
        tag.setInteger("Lifetime", lifetime);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        if (tag.hasKey("DestPos")) {
            destinationPos = BlockPos.fromLong(tag.getLong("DestPos"));
            destinationDim = tag.getInteger("DestDim");
        }
        if (tag.hasKey("Owner")) ownerUUID = UUID.fromString(tag.getString("Owner"));
        lifetime = tag.getInteger("Lifetime");
    }

    public int getRemainingSeconds() { return lifetime / 20; }

    private static class RiftTeleporter extends net.minecraft.world.Teleporter {
        private final BlockPos targetPos;
        public RiftTeleporter(WorldServer world, BlockPos targetPos) {
            super(world);
            this.targetPos = targetPos;
        }
        @Override
        public void placeInPortal(Entity entity, float rotationYaw) {
            entity.setPosition(targetPos.getX() + 0.5D, targetPos.getY() + 0.25D, targetPos.getZ() + 0.5D);
            entity.motionX = entity.motionY = entity.motionZ = 0.0D;
        }
        @Override public boolean placeInExistingPortal(Entity entity, float rotationYaw) { placeInPortal(entity, rotationYaw); return true; }
        @Override public boolean makePortal(Entity entity) { return true; }
    }
}
