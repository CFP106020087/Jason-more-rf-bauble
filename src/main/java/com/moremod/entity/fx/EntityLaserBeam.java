package com.moremod.entity.fx;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class EntityLaserBeam extends Entity {
    // 颜色类型常量
    public static final int COLOR_BLUE = 0;    // 亮蓝色 - VoidRipper使用
    public static final int COLOR_PURPLE = 1;  // 紫色 - 玩家使用
    public static final int COLOR_RED = 2;     // 红色
    public static final int COLOR_GREEN = 3;   // 绿色
    public static final int COLOR_BLACK = 4;   // 黑色 - Riftwarden使用

    private static final DataParameter<Integer> OWNER_ID  =
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> TARGET_ID =
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> TICKS_LEFT=
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.VARINT);
    private static final DataParameter<Float>  WIDTH      =
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Float>  DMG_PER_TICK=
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> REQUIRE_LOS=
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.BOOLEAN);

    // 颜色参数
    private static final DataParameter<Integer> COLOR_TYPE =
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.VARINT);

    // 目标位置参数
    private static final DataParameter<Float> TARGET_X =
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> TARGET_Y =
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> TARGET_Z =
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> USE_POS_TARGET =
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.BOOLEAN);

    // 实际终点位置（被障碍物阻挡后的位置）
    private static final DataParameter<Float> ACTUAL_END_X =
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> ACTUAL_END_Y =
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> ACTUAL_END_Z =
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> IS_BLOCKED =
            EntityDataManager.createKey(EntityLaserBeam.class, DataSerializers.BOOLEAN);

    public EntityLaserBeam(World worldIn) {
        super(worldIn);
        this.setSize(0.1F, 0.1F);
        this.noClip = true;
        this.setNoGravity(true);
    }

    // 设置起始位置
    public void setStartPosition(Vec3d pos) {
        this.setPosition(pos.x, pos.y, pos.z);
    }

    // 获取玩家右手位置的辅助方法
    private Vec3d getPlayerHandPosition(EntityPlayer player) {
        float yaw = player.rotationYaw * (float)Math.PI / 180.0F;
        float pitch = player.rotationPitch * (float)Math.PI / 180.0F;

        // 计算视线方向
        Vec3d lookVec = player.getLookVec();

        // 计算右向量
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = lookVec.crossProduct(up).normalize();

        // 右手偏移量
        double sideOffset = 0.36D;    // 向右偏移
        double vertOffset = -0.15D;   // 向下偏移
        double forwardOffset = 0.4D;  // 向前偏移

        // 如果玩家在潜行，调整偏移
        if (player.isSneaking()) {
            vertOffset -= 0.08D;
        }

        // 计算右手位置
        return new Vec3d(
                player.posX + right.x * sideOffset + lookVec.x * forwardOffset,
                player.posY + player.getEyeHeight() + vertOffset + lookVec.y * forwardOffset,
                player.posZ + right.z * sideOffset + lookVec.z * forwardOffset
        );
    }

    // 带起点参数的构造函数
    public EntityLaserBeam(World w, Vec3d startPos, Entity target,
                           int lifetimeTicks, float width, float damagePerTick, int colorType) {
        this(w);
        this.dataManager.set(TARGET_ID, target == null ? -1 : target.getEntityId());
        this.dataManager.set(TICKS_LEFT, lifetimeTicks);
        this.dataManager.set(WIDTH, width);
        this.dataManager.set(DMG_PER_TICK, damagePerTick);
        this.dataManager.set(REQUIRE_LOS, false);
        this.dataManager.set(USE_POS_TARGET, false);
        this.dataManager.set(COLOR_TYPE, colorType);

        // 使用提供的起点位置
        this.setPosition(startPos.x, startPos.y, startPos.z);
    }

    // 带颜色参数的构造函数
    public EntityLaserBeam(World w, EntityLivingBase owner, Entity target,
                           int lifetimeTicks, float width, float damagePerTick, int colorType) {
        this(w);
        setOwner(owner);
        setTarget(target);
        this.dataManager.set(TICKS_LEFT, lifetimeTicks);
        this.dataManager.set(WIDTH, width);
        this.dataManager.set(DMG_PER_TICK, damagePerTick);
        this.dataManager.set(REQUIRE_LOS, false);
        this.dataManager.set(USE_POS_TARGET, false);
        this.dataManager.set(COLOR_TYPE, colorType);

        // 根据owner类型调整发射位置
        if (owner instanceof EntityPlayer) {
            // 玩家从右手发射
            Vec3d handPos = getPlayerHandPosition((EntityPlayer) owner);
            this.setPosition(handPos.x, handPos.y, handPos.z);
        } else if (owner.getClass().getSimpleName().equals("EntityVoidRipper")) {
            // VoidRipper从胸部发射
            this.setPosition(owner.posX, owner.posY + owner.height * 0.75D, owner.posZ);
        } else {
            // 其他实体从眼睛高度发射
            this.setPosition(owner.posX, owner.posY + owner.getEyeHeight(), owner.posZ);
        }
    }

    // 默认构造函数（保持向后兼容）
    public EntityLaserBeam(World w, EntityLivingBase owner, Entity target,
                           int lifetimeTicks, float width, float damagePerTick) {
        this(w, owner, target, lifetimeTicks, width, damagePerTick, COLOR_PURPLE);
    }

    // 新构造函数：指向位置（带颜色）
    public EntityLaserBeam(World w, EntityLivingBase owner, Vec3d targetPos,
                           int lifetimeTicks, float width, int colorType) {
        this(w);
        setOwner(owner);
        this.dataManager.set(TARGET_X, (float)targetPos.x);
        this.dataManager.set(TARGET_Y, (float)targetPos.y);
        this.dataManager.set(TARGET_Z, (float)targetPos.z);
        this.dataManager.set(USE_POS_TARGET, true);
        this.dataManager.set(TICKS_LEFT, lifetimeTicks);
        this.dataManager.set(WIDTH, width);
        this.dataManager.set(DMG_PER_TICK, 0F);
        this.dataManager.set(REQUIRE_LOS, false);
        this.dataManager.set(COLOR_TYPE, colorType);

        // 根据owner类型调整发射位置
        if (owner instanceof EntityPlayer) {
            // 玩家从右手发射
            Vec3d handPos = getPlayerHandPosition((EntityPlayer) owner);
            this.setPosition(handPos.x, handPos.y, handPos.z);
        } else if (owner.getClass().getSimpleName().equals("EntityVoidRipper")) {
            this.setPosition(owner.posX, owner.posY + owner.height * 0.75D, owner.posZ);
        } else {
            this.setPosition(owner.posX, owner.posY + owner.getEyeHeight(), owner.posZ);
        }
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(OWNER_ID, -1);
        this.dataManager.register(TARGET_ID, -1);
        this.dataManager.register(TICKS_LEFT, 1);
        this.dataManager.register(WIDTH, 0.4F);
        this.dataManager.register(DMG_PER_TICK, 2.0F);
        this.dataManager.register(REQUIRE_LOS, false);
        this.dataManager.register(TARGET_X, 0F);
        this.dataManager.register(TARGET_Y, 0F);
        this.dataManager.register(TARGET_Z, 0F);
        this.dataManager.register(USE_POS_TARGET, false);
        this.dataManager.register(COLOR_TYPE, COLOR_PURPLE);
        this.dataManager.register(ACTUAL_END_X, 0F);
        this.dataManager.register(ACTUAL_END_Y, 0F);
        this.dataManager.register(ACTUAL_END_Z, 0F);
        this.dataManager.register(IS_BLOCKED, false);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        Entity owner = getOwnerEntity();
        if (owner == null) {
            setDead();
            return;
        }

        // 获取起点 - 根据owner类型调整
        Vec3d start;
        if (owner instanceof EntityPlayer) {
            // 玩家从右手发射
            start = getPlayerHandPosition((EntityPlayer) owner);
        } else if (owner.getClass().getSimpleName().equals("EntityVoidRipper")) {
            // VoidRipper从胸部发射，稍微向前
            float yaw = owner.rotationYaw * (float)Math.PI / 180.0F;
            double forwardOffset = 1.0D;
            start = new Vec3d(
                    owner.posX - Math.sin(yaw) * forwardOffset,
                    owner.posY + owner.height * 0.75D,
                    owner.posZ + Math.cos(yaw) * forwardOffset
            );
        } else {
            start = new Vec3d(owner.posX, owner.posY + owner.getEyeHeight() * 0.5, owner.posZ);
        }

        Vec3d targetEnd;
        if (this.dataManager.get(USE_POS_TARGET)) {
            targetEnd = getTargetPos();
        } else {
            Entity target = getTargetEntity();
            if (target != null) {
                targetEnd = new Vec3d(target.posX, target.posY + target.height * 0.5, target.posZ);
            } else {
                setDead();
                return;
            }
        }

        // 执行光线追踪检测障碍物
        RayTraceResult rayTrace = world.rayTraceBlocks(
                start,
                targetEnd,
                false,  // 不检测液体
                true,   // 忽略不可碰撞方块
                false   // 不返回最后一个方块
        );

        Vec3d actualEnd;
        if (rayTrace != null && rayTrace.typeOfHit == RayTraceResult.Type.BLOCK) {
            // 激光被方块阻挡
            actualEnd = rayTrace.hitVec;
            this.dataManager.set(ACTUAL_END_X, (float)actualEnd.x);
            this.dataManager.set(ACTUAL_END_Y, (float)actualEnd.y);
            this.dataManager.set(ACTUAL_END_Z, (float)actualEnd.z);
            this.dataManager.set(IS_BLOCKED, true);
        } else {
            // 激光没有被阻挡
            actualEnd = targetEnd;
            this.dataManager.set(IS_BLOCKED, false);
        }

        // 更新AABB - 为蓝色激光使用更小的范围
        float fat = getColorType() == COLOR_BLUE ? 0.5F : Math.max(0.3F, getBeamWidth());
        AxisAlignedBB bb = makeFatAABB(start, actualEnd, fat);
        this.setEntityBoundingBox(bb);
        this.setPosition((start.x + actualEnd.x) * 0.5, (start.y + actualEnd.y) * 0.5, (start.z + actualEnd.z) * 0.5);

        if (world.isRemote) return;

        int ticks = this.dataManager.get(TICKS_LEFT) - 1;
        if (ticks <= 0) {
            setDead();
            return;
        }
        this.dataManager.set(TICKS_LEFT, ticks);
    }

    // Getter 和 Setter 方法
    public int getColorType() {
        return this.dataManager.get(COLOR_TYPE);
    }

    public void setColorType(int colorType) {
        this.dataManager.set(COLOR_TYPE, colorType);
    }

    public void setOwner(Entity owner) {
        this.dataManager.set(OWNER_ID, owner == null ? -1 : owner.getEntityId());
    }

    public void setTarget(Entity target) {
        this.dataManager.set(TARGET_ID, target == null ? -1 : target.getEntityId());
    }

    public Entity getOwnerEntity() {
        int id = this.dataManager.get(OWNER_ID);
        return id >= 0 ? world.getEntityByID(id) : null;
    }

    public Entity getTargetEntity() {
        int id = this.dataManager.get(TARGET_ID);
        return id >= 0 ? world.getEntityByID(id) : null;
    }

    public Vec3d getTargetPos() {
        if (this.dataManager.get(USE_POS_TARGET)) {
            return new Vec3d(
                    this.dataManager.get(TARGET_X),
                    this.dataManager.get(TARGET_Y),
                    this.dataManager.get(TARGET_Z)
            );
        }
        return null;
    }

    public Vec3d getActualEndPos() {
        if (this.dataManager.get(IS_BLOCKED)) {
            return new Vec3d(
                    this.dataManager.get(ACTUAL_END_X),
                    this.dataManager.get(ACTUAL_END_Y),
                    this.dataManager.get(ACTUAL_END_Z)
            );
        }
        return null;
    }

    public boolean isBlocked() {
        return this.dataManager.get(IS_BLOCKED);
    }

    public float getBeamWidth() {
        return this.dataManager.get(WIDTH);
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }

    private static AxisAlignedBB makeFatAABB(Vec3d a, Vec3d b, double fat) {
        double minX = Math.min(a.x, b.x) - fat;
        double minY = Math.min(a.y, b.y) - fat;
        double minZ = Math.min(a.z, b.z) - fat;
        double maxX = Math.max(a.x, b.x) + fat;
        double maxY = Math.max(a.y, b.y) + fat;
        double maxZ = Math.max(a.z, b.z) + fat;
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        this.dataManager.set(TICKS_LEFT, nbt.getInteger("Ticks"));
        this.dataManager.set(WIDTH, nbt.getFloat("Width"));
        this.dataManager.set(USE_POS_TARGET, nbt.getBoolean("UsePosTarget"));
        this.dataManager.set(COLOR_TYPE, nbt.getInteger("ColorType"));
        if (nbt.getBoolean("UsePosTarget")) {
            this.dataManager.set(TARGET_X, nbt.getFloat("TargetX"));
            this.dataManager.set(TARGET_Y, nbt.getFloat("TargetY"));
            this.dataManager.set(TARGET_Z, nbt.getFloat("TargetZ"));
        }
        this.dataManager.set(IS_BLOCKED, nbt.getBoolean("IsBlocked"));
        if (nbt.getBoolean("IsBlocked")) {
            this.dataManager.set(ACTUAL_END_X, nbt.getFloat("ActualEndX"));
            this.dataManager.set(ACTUAL_END_Y, nbt.getFloat("ActualEndY"));
            this.dataManager.set(ACTUAL_END_Z, nbt.getFloat("ActualEndZ"));
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setInteger("Ticks", this.dataManager.get(TICKS_LEFT));
        nbt.setFloat("Width", this.dataManager.get(WIDTH));
        nbt.setBoolean("UsePosTarget", this.dataManager.get(USE_POS_TARGET));
        nbt.setInteger("ColorType", this.dataManager.get(COLOR_TYPE));
        if (this.dataManager.get(USE_POS_TARGET)) {
            nbt.setFloat("TargetX", this.dataManager.get(TARGET_X));
            nbt.setFloat("TargetY", this.dataManager.get(TARGET_Y));
            nbt.setFloat("TargetZ", this.dataManager.get(TARGET_Z));
        }
        nbt.setBoolean("IsBlocked", this.dataManager.get(IS_BLOCKED));
        if (this.dataManager.get(IS_BLOCKED)) {
            nbt.setFloat("ActualEndX", this.dataManager.get(ACTUAL_END_X));
            nbt.setFloat("ActualEndY", this.dataManager.get(ACTUAL_END_Y));
            nbt.setFloat("ActualEndZ", this.dataManager.get(ACTUAL_END_Z));
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }
}