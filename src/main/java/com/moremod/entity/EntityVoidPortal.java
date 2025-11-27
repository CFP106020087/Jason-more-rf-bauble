package com.moremod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

public class EntityVoidPortal extends Entity {

    // --- DataManager keys ---
    private static final DataParameter<Integer> TICKS_REMAINING = EntityDataManager.createKey(
            EntityVoidPortal.class, DataSerializers.VARINT);
    private static final DataParameter<BlockPos> TARGET_POS = EntityDataManager.createKey(
            EntityVoidPortal.class, DataSerializers.BLOCK_POS);

    // --- NBT keys for per-player persistent cooldowns ---
    private static final String NBT_CD_UNTIL          = "jb_portal_cd_until";
    private static final String NBT_LAST_SRC_X        = "jb_last_src_x";
    private static final String NBT_LAST_SRC_Y        = "jb_last_src_y";
    private static final String NBT_LAST_SRC_Z        = "jb_last_src_z";
    private static final String NBT_LAST_DST_X        = "jb_last_dst_x";
    private static final String NBT_LAST_DST_Y        = "jb_last_dst_y";
    private static final String NBT_LAST_DST_Z        = "jb_last_dst_z";
    private static final String NBT_ARRIVAL_GRACE_UNTIL = "jb_arrival_grace_until";

    // --- Tunables ---
    /** 传送门存在时长（tick） */
    private static final int LIFETIME_TICKS = 600; // 30s
    /** 到达门的“到达保护期”，在此期间即便站在门里也不会再次触发传送（tick） */
    private static final int ARRIVAL_GRACE_TICKS = 60; // 3s
    /** 全局冷却：任意传送后，在该时间内玩家不再被任何门吸走（tick） */
    private static final int GLOBAL_CD_TICKS = 40; // 2s
    /** 检测频率：每 N tick 检测一次 */
    private static final int SCAN_INTERVAL = 5; // tick
    /** 门的检测范围（严格 2x1x2，高度 2 格） */
    private static final double HALF_XZ = 1.0; // 半径1 => 宽2
    private static final double HEIGHT = 2.0;

    private BlockPos sourceBlock;
    private final Random fastRand = new Random();

    public EntityVoidPortal(World world) {
        super(world);
        setSize(1.0F, 2.0F);      // 视觉与碰撞大小
        this.noClip = true;
        this.isImmuneToFire = true;
        this.preventEntitySpawning = false;
    }

    public EntityVoidPortal(World world, BlockPos pos, BlockPos target, BlockPos source) {
        this(world);
        setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        setTargetPos(target);
        this.sourceBlock = source;
        setTicksRemaining(LIFETIME_TICKS);
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(TICKS_REMAINING, LIFETIME_TICKS);
        this.dataManager.register(TARGET_POS, BlockPos.ORIGIN);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (!world.isRemote) {
            // 倒计时 & 死亡
            int ticks = getTicksRemaining();
            if (ticks <= 0) {
                setDead();
                return;
            }
            setTicksRemaining(ticks - 1);

            // 临终提示音（5 秒前开始每秒提示一次）
            if (ticks <= 100 && ticks % 20 == 0) {
                world.playSound(null, posX, posY, posZ,
                        SoundEvents.BLOCK_NOTE_PLING, SoundCategory.BLOCKS, 0.8F, 0.5F);
            }

            // 传送检测
            if (ticks % SCAN_INTERVAL == 0) {
                checkAndTeleportEntities();
            }
        }

        // 客户端粒子
        if (world.isRemote || (!world.isRemote && world.rand.nextInt(3) == 0)) {
            spawnPortalParticles();
        }
    }

    // --------------------------
    // 传送与冷却核心逻辑
    // --------------------------
    private void checkAndTeleportEntities() {
        BlockPos targetPos = getTargetPos();
        if (targetPos == null || targetPos.equals(BlockPos.ORIGIN)) return;

        // 严格 2x1x2 的门区域（中心在实体位置）
        AxisAlignedBB searchBox = new AxisAlignedBB(
                posX - HALF_XZ, posY, posZ - HALF_XZ,
                posX + HALF_XZ, posY + HEIGHT, posZ + HALF_XZ
        );

        List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(this, searchBox);
        long now = world.getTotalWorldTime();

        for (Entity e : entities) {
            if (!(e instanceof EntityPlayer)) continue;
            EntityPlayer player = (EntityPlayer) e;

            if (player.isSpectator() || !player.isEntityAlive()) continue;

            NBTTagCompound tag = getOrInitPersistTag(player);

            // 全局冷却：任意传送后的一段时间内，不再被任何门吸走
            long cdUntil = tag.getLong(NBT_CD_UNTIL);
            if (cdUntil > now) continue;

            // 到达保护：如果“上一次的目的地” == 当前门的坐标，并且仍处于到达保护期，则忽略
            long graceUntil = tag.getLong(NBT_ARRIVAL_GRACE_UNTIL);
            if (graceUntil > now) {
                BlockPos lastDst = readPos(tag, NBT_LAST_DST_X, NBT_LAST_DST_Y, NBT_LAST_DST_Z);
                if (lastDst != null && lastDst.equals(getThisBlockPos())) {
                    // 仍在到达保护期内，忽略本次检测
                    continue;
                }
            }

            // 到这里说明可被传送
            teleportEntity(player, targetPos);
        }
    }

    private void teleportEntity(Entity entity, BlockPos target) {
        if (world.isRemote) return; // 只在服务端做

        // 传送前特效
        spawnTeleportEffect(entity.posX, entity.posY, entity.posZ);

        // 执行传送
        entity.dismountRidingEntity();

        // 轻微随机偏移，避免落点正中心再次进入扫描盒
        double lateral = 0.25 + fastRand.nextDouble() * 0.2; // 0.25~0.45
        double yawRad = fastRand.nextDouble() * Math.PI * 2.0;
        double offX = Math.cos(yawRad) * lateral;
        double offZ = Math.sin(yawRad) * lateral;

        entity.setPositionAndUpdate(
                target.getX() + 0.5 + offX,
                target.getY() + 0.6, // 稍微抬高，防止卡方块或再次贴地进入门
                target.getZ() + 0.5 + offZ
        );

        // 轻推一下动量，帮助玩家脱离门框
        entity.motionY = Math.max(entity.motionY, 0.15);
        entity.motionX += offX * 0.1;
        entity.motionZ += offZ * 0.1;
        entity.velocityChanged = true;

        // 传送后特效与音效（两端）
        spawnTeleportEffect(target.getX() + 0.5, target.getY() + 0.1, target.getZ() + 0.5);
        world.playSound(null, target, SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);

        // 标记冷却（持久化在玩家 NBT，避免重连丢失）
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            markPlayerCooldown(player, getThisBlockPos(), target, GLOBAL_CD_TICKS, ARRIVAL_GRACE_TICKS);

            // 可选：给玩家一个道具冷却（UI 反馈）
            player.getCooldownTracker().setCooldown(net.minecraft.init.Items.ENDER_PEARL, 20);
        }
    }

    private void markPlayerCooldown(EntityPlayer player, BlockPos src, BlockPos dst, int globalTicks, int arrivalGraceTicks) {
        long now = world.getTotalWorldTime();
        NBTTagCompound tag = getOrInitPersistTag(player);

        tag.setLong(NBT_CD_UNTIL, now + globalTicks);
        writePos(tag, NBT_LAST_SRC_X, NBT_LAST_SRC_Y, NBT_LAST_SRC_Z, src);
        writePos(tag, NBT_LAST_DST_X, NBT_LAST_DST_Y, NBT_LAST_DST_Z, dst);
        tag.setLong(NBT_ARRIVAL_GRACE_UNTIL, now + arrivalGraceTicks);
    }

    private static NBTTagCompound getOrInitPersistTag(EntityPlayer player) {
        // 注意：getEntityData() 在 1.12.2 是持久的（随玩家保存）
        NBTTagCompound tag = player.getEntityData();
        if (tag == null) {
            tag = new NBTTagCompound();
            // 在极少数环境，EntityData 不会为 null，这里是稳妥写法；若为 null 可设置，但通常不需要。
        }
        return tag;
    }

    private static void writePos(NBTTagCompound tag, String kx, String ky, String kz, BlockPos pos) {
        tag.setInteger(kx, pos.getX());
        tag.setInteger(ky, pos.getY());
        tag.setInteger(kz, pos.getZ());
    }

    private static BlockPos readPos(NBTTagCompound tag, String kx, String ky, String kz) {
        if (!tag.hasKey(kx)) return null;
        return new BlockPos(tag.getInteger(kx), tag.getInteger(ky), tag.getInteger(kz));
    }

    private BlockPos getThisBlockPos() {
        return new BlockPos((int) Math.floor(posX), (int) Math.floor(posY), (int) Math.floor(posZ));
    }

    // --------------------------
    // 视觉与粒子
    // --------------------------
    private void spawnPortalParticles() {
        // 主体粒子
        for (int i = 0; i < 3; i++) {
            double offsetX = (rand.nextDouble() - 0.5) * 2.0;
            double offsetY = rand.nextDouble() * HEIGHT;
            double offsetZ = (rand.nextDouble() - 0.5) * 2.0;
            double motionY = -rand.nextDouble() * 0.5;

            world.spawnParticle(EnumParticleTypes.PORTAL,
                    posX + offsetX, posY + offsetY, posZ + offsetZ,
                    offsetX * 0.1, motionY, offsetZ * 0.1);
        }

        // 边框粒子
        if (rand.nextInt(5) == 0) {
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2) * i / 8;
                double x = posX + Math.cos(angle) * (HALF_XZ + 0.2);
                double z = posZ + Math.sin(angle) * (HALF_XZ + 0.2);

                world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                        x, posY + 1, z,
                        (posX - x) * 0.1, 0.1, (posZ - z) * 0.1);
            }
        }

        // 即将关闭时的警告粒子（最后 5s 闪烁）
        int ticks = getTicksRemaining();
        if (ticks < 100 && ticks % 10 < 5) {
            world.spawnParticle(EnumParticleTypes.REDSTONE,
                    posX, posY + HEIGHT + 0.5, posZ,
                    0.5, 0.5, 0.5);
        }
    }

    private void spawnTeleportEffect(double x, double y, double z) {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;
        for (int i = 0; i < 32; i++) {
            ws.spawnParticle(EnumParticleTypes.PORTAL,
                    x, y + rand.nextDouble() * HEIGHT, z,
                    10, 0.5, 1.0, 0.5, 0.0);
        }
    }

    // --------------------------
    // NBT 同步
    // --------------------------
    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        setTicksRemaining(compound.getInteger("TicksRemaining"));

        if (compound.hasKey("TargetX")) {
            setTargetPos(new BlockPos(
                    compound.getInteger("TargetX"),
                    compound.getInteger("TargetY"),
                    compound.getInteger("TargetZ")
            ));
        }

        if (compound.hasKey("SourceX")) {
            sourceBlock = new BlockPos(
                    compound.getInteger("SourceX"),
                    compound.getInteger("SourceY"),
                    compound.getInteger("SourceZ")
            );
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setInteger("TicksRemaining", getTicksRemaining());

        BlockPos target = getTargetPos();
        if (target != null && !target.equals(BlockPos.ORIGIN)) {
            compound.setInteger("TargetX", target.getX());
            compound.setInteger("TargetY", target.getY());
            compound.setInteger("TargetZ", target.getZ());
        }

        if (sourceBlock != null) {
            compound.setInteger("SourceX", sourceBlock.getX());
            compound.setInteger("SourceY", sourceBlock.getY());
            compound.setInteger("SourceZ", sourceBlock.getZ());
        }
    }

    // --------------------------
    // DataManager helpers
    // --------------------------
    private void setTicksRemaining(int ticks) {
        this.dataManager.set(TICKS_REMAINING, ticks);
    }

    private int getTicksRemaining() {
        return this.dataManager.get(TICKS_REMAINING);
    }

    private void setTargetPos(BlockPos pos) {
        this.dataManager.set(TARGET_POS, pos);
    }

    private BlockPos getTargetPos() {
        return this.dataManager.get(TARGET_POS);
    }

    // --------------------------
    // Misc overrides
    // --------------------------
    @Override
    public boolean canBeCollidedWith() { return false; }

    @Override
    public boolean canBePushed() { return false; }

    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        // 碰撞检测统一在 checkAndTeleportEntities 中处理
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance) {
        return distance < 64.0; // 64 格内渲染
    }

    @Override
    public void setDead() {
        if (!world.isRemote) {
            world.playSound(null, posX, posY, posZ,
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.0F, 0.8F);
            // 结束时的小爆粒
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        posX, posY + 0.5, posZ,
                        40, 0.6, 0.6, 0.6, 0.0);
            }
        }
        super.setDead();
    }
}
