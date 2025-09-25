package com.moremod.entity.fx;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

public class EntityPlayerLaserBeam extends Entity implements IEntityAdditionalSpawnData {

    private EntityPlayer owner;
    private int ownerId = -1;
    private Vec3d targetPos;
    private int lifetime;
    private EnumHand hand;
    private boolean isBlocked = false;
    private Vec3d blockedPos = null;

    public EntityPlayerLaserBeam(World worldIn) {
        super(worldIn);
        this.setSize(0.1F, 0.1F);
        this.noClip = true;
        this.setNoGravity(true);
        this.ignoreFrustumCheck = true;  // 禁用视锥裁剪
        this.preventEntitySpawning = false;
    }

    public EntityPlayerLaserBeam(World world, EntityPlayer player, Vec3d target, int lifetime, EnumHand hand) {
        this(world);
        this.owner = player;
        this.ownerId = player.getEntityId();
        this.targetPos = target;
        this.lifetime = lifetime;
        this.hand = hand;

        Vec3d handPos = getHandPosition();
        this.setPosition(handPos.x, handPos.y, handPos.z);
    }

    @Override
    protected void entityInit() {
        // 短期实体，不需要DataManager
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        // 客户端第一帧解析owner
        if (world.isRemote && owner == null && ownerId != -1) {
            Entity e = world.getEntityByID(ownerId);
            if (e instanceof EntityPlayer) owner = (EntityPlayer) e;
        }

        if (owner == null || !owner.isEntityAlive() || lifetime <= 0) {
            setDead();
            return;
        }
        lifetime--;

        Vec3d startPos = getHandPosition();

        // 检测障碍物
        RayTraceResult rayTrace = world.rayTraceBlocks(startPos, targetPos, false, true, false);
        if (rayTrace != null && rayTrace.typeOfHit == RayTraceResult.Type.BLOCK) {
            isBlocked = true;
            blockedPos = rayTrace.hitVec;
        } else {
            isBlocked = false;
            blockedPos = null;
        }

        Vec3d actualEnd = isBlocked ? blockedPos : targetPos;

        // 更新碰撞箱
        AxisAlignedBB bb = new AxisAlignedBB(
                Math.min(startPos.x, actualEnd.x) - 0.5,
                Math.min(startPos.y, actualEnd.y) - 0.5,
                Math.min(startPos.z, actualEnd.z) - 0.5,
                Math.max(startPos.x, actualEnd.x) + 0.5,
                Math.max(startPos.y, actualEnd.y) + 0.5,
                Math.max(startPos.z, actualEnd.z) + 0.5
        );
        this.setEntityBoundingBox(bb);

        // 实体位置设为光束中点
        this.setPosition(
                (startPos.x + actualEnd.x) * 0.5,
                (startPos.y + actualEnd.y) * 0.5,
                (startPos.z + actualEnd.z) * 0.5
        );
    }
    public Vec3d getHandPosition() {
        if (owner == null) return new Vec3d(posX, posY, posZ);

        Vec3d lookVec = owner.getLookVec();
        Vec3d eyePos = new Vec3d(owner.posX, owner.posY + owner.getEyeHeight(), owner.posZ);

        // 修正：反转左右手的逻辑
        double handSign;
        if (hand == EnumHand.MAIN_HAND) {
            // 主手：右手惯用者向右(-1)，左手惯用者向左(+1)
            handSign = (owner.getPrimaryHand() == EnumHandSide.RIGHT) ? -1.0D : 1.0D;
        } else {
            // 副手：与主手相反
            handSign = (owner.getPrimaryHand() == EnumHandSide.RIGHT) ? 1.0D : -1.0D;
        }

        Vec3d right = new Vec3d(0, 1, 0).crossProduct(lookVec).normalize();

        double forward = 0.55D;
        double lateral = 0.36D * handSign;  // 横向偏移
        double down = 0.38D + (owner.isSneaking() ? 0.10D : 0.0D);

        return new Vec3d(
                eyePos.x + lookVec.x * forward + right.x * lateral,
                eyePos.y - down + lookVec.y * forward + right.y * lateral,
                eyePos.z + lookVec.z * forward + right.z * lateral
        );
    }

    // Getters
    public EntityPlayer getOwner() { return owner; }
    public Vec3d getTargetPos() { return targetPos; }
    public Vec3d getStartPos() { return getHandPosition(); }
    public Vec3d getActualEndPos() { return isBlocked ? blockedPos : targetPos; }
    public boolean isBlocked() { return isBlocked; }
    public EnumHand getHand() { return hand; }

    // ========== 无限渲染距离设置 ==========
    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return true;  // 无论多远都渲染
    }

    public double getMaxRenderDistanceSquared() {
        return Double.MAX_VALUE;  // 无限渲染距离
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;  // 在透明pass渲染
    }




    // NBT存储
    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        this.ownerId = nbt.getInteger("OwnerId");
        this.lifetime = nbt.getInteger("Lifetime");

        double tx = nbt.getDouble("Tx");
        double ty = nbt.getDouble("Ty");
        double tz = nbt.getDouble("Tz");
        this.targetPos = new Vec3d(tx, ty, tz);

        int h = nbt.getInteger("Hand");
        this.hand = (h == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setInteger("OwnerId", owner != null ? owner.getEntityId() : ownerId);
        nbt.setInteger("Lifetime", lifetime);

        if (targetPos != null) {
            nbt.setDouble("Tx", targetPos.x);
            nbt.setDouble("Ty", targetPos.y);
            nbt.setDouble("Tz", targetPos.z);
        }

        nbt.setInteger("Hand", hand == EnumHand.MAIN_HAND ? 0 : 1);
    }

    // 生成包数据同步
    @Override
    public void writeSpawnData(ByteBuf buf) {
        buf.writeInt(owner != null ? owner.getEntityId() : ownerId);

        if (targetPos != null) {
            buf.writeDouble(targetPos.x);
            buf.writeDouble(targetPos.y);
            buf.writeDouble(targetPos.z);
        } else {
            buf.writeDouble(posX);
            buf.writeDouble(posY);
            buf.writeDouble(posZ);
        }

        buf.writeInt(lifetime);
        buf.writeByte(hand == null ? 0 : (hand == EnumHand.MAIN_HAND ? 0 : 1));
    }

    @Override
    public void readSpawnData(ByteBuf buf) {
        this.ownerId = buf.readInt();
        double tx = buf.readDouble();
        double ty = buf.readDouble();
        double tz = buf.readDouble();
        this.targetPos = new Vec3d(tx, ty, tz);

        this.lifetime = buf.readInt();
        int h = buf.readByte();
        this.hand = (h == 0 ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND);
    }
}