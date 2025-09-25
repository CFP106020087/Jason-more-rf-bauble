package com.moremod.entity.fx;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityLightningArc extends Entity {

    private static final DataParameter<Integer> FROM_ID =
            EntityDataManager.createKey(EntityLightningArc.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> TO_ID =
            EntityDataManager.createKey(EntityLightningArc.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> TICKS_LEFT =
            EntityDataManager.createKey(EntityLightningArc.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> SEED =
            EntityDataManager.createKey(EntityLightningArc.class, DataSerializers.VARINT);

    public EntityLightningArc(World worldIn) {
        super(worldIn);
        this.setSize(0.1F,0.1F);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public EntityLightningArc(World w, EntityLivingBase from, EntityLivingBase to, int lifeTicks) {
        this(w);
        setFrom(from);
        setTo(to);
        this.dataManager.set(TICKS_LEFT, lifeTicks);
        this.dataManager.set(SEED, w.rand.nextInt());
        this.setPosition(from.posX, from.posY + from.height*0.5, from.posZ);
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(FROM_ID, -1);
        this.dataManager.register(TO_ID, -1);
        this.dataManager.register(TICKS_LEFT, 6);
        this.dataManager.register(SEED, 0);
    }

    public void setFrom(Entity e){ this.dataManager.set(FROM_ID, e==null?-1:e.getEntityId()); }
    public void setTo(Entity e){ this.dataManager.set(TO_ID, e==null?-1:e.getEntityId()); }
    public Entity getFrom(){ int id = this.dataManager.get(FROM_ID); return id<0?null:world.getEntityByID(id); }
    public Entity getTo(){ int id = this.dataManager.get(TO_ID); return id<0?null:world.getEntityByID(id); }
    public int getSeed(){ return this.dataManager.get(SEED); }
    public int getTicksLeft(){ return this.dataManager.get(TICKS_LEFT); }

    @Override
    public boolean isInRangeToRenderDist(double distance) { return true; }

    @Override
    public void onUpdate() {
        super.onUpdate();

        // 更新 AABB 覆盖整条雷弧
        Entity f = getFrom(), t = getTo();
        if (f != null && t != null) {
            Vec3d s = new Vec3d(f.posX, f.posY + f.height*0.5, f.posZ);
            Vec3d e = new Vec3d(t.posX, t.posY + t.height*0.5, t.posZ);
            AxisAlignedBB bb = new AxisAlignedBB(
                    Math.min(s.x, e.x) - 0.6, Math.min(s.y, e.y) - 0.6, Math.min(s.z, e.z) - 0.6,
                    Math.max(s.x, e.x) + 0.6, Math.max(s.y, e.y) + 0.6, Math.max(s.z, e.z) + 0.6
            );
            this.setEntityBoundingBox(bb);
            this.setPosition((s.x+e.x)*0.5, (s.y+e.y)*0.5, (s.z+e.z)*0.5);
        }

        if (world.isRemote) return;
        int tLeft = this.dataManager.get(TICKS_LEFT) - 1;
        if (tLeft <= 0) { setDead(); return; }
        this.dataManager.set(TICKS_LEFT, tLeft);
    }

    @Override protected void readEntityFromNBT(NBTTagCompound nbt) {
        this.dataManager.set(TICKS_LEFT, nbt.getInteger("Ticks"));
        this.dataManager.set(SEED, nbt.getInteger("Seed"));
    }
    @Override protected void writeEntityToNBT(NBTTagCompound nbt) {
        nbt.setInteger("Ticks", this.dataManager.get(TICKS_LEFT));
        nbt.setInteger("Seed", this.dataManager.get(SEED));
    }
    @Override public boolean canBeCollidedWith() { return false; }
}
