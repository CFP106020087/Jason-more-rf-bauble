package com.moremod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 仪式座椅实体
 * 用于让玩家"坐"在祭坛核心上进行嵌入仪式
 */
public class EntityRitualSeat extends Entity {

    private BlockPos corePos;
    private int lifetime = 0;
    private static final int MAX_LIFETIME = 6000; // 5分钟自动消失

    public EntityRitualSeat(World world) {
        super(world);
        this.setSize(0.0F, 0.0F);
        this.noClip = true;
        this.setInvisible(true);
    }

    public EntityRitualSeat(World world, BlockPos corePos) {
        this(world);
        this.corePos = corePos;
        this.setPosition(corePos.getX() + 0.5, corePos.getY() + 0.3, corePos.getZ() + 0.5);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (world.isRemote) return;

        lifetime++;

        // 检查是否应该移除
        if (shouldRemove()) {
            setDead();
            return;
        }

        // 保持位置
        if (corePos != null) {
            this.setPosition(corePos.getX() + 0.5, corePos.getY() + 0.3, corePos.getZ() + 0.5);
        }
    }

    private boolean shouldRemove() {
        // 超时
        if (lifetime > MAX_LIFETIME) return true;

        // 没有乘客
        if (getPassengers().isEmpty()) return true;

        // 祭坛核心被移除
        if (corePos != null && world.isAirBlock(corePos)) return true;

        return false;
    }

    @Override
    public double getMountedYOffset() {
        return 0.0;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    protected void entityInit() {
        // 无需数据同步
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("CorePos")) {
            int[] pos = compound.getIntArray("CorePos");
            if (pos.length == 3) {
                corePos = new BlockPos(pos[0], pos[1], pos[2]);
            }
        }
        lifetime = compound.getInteger("Lifetime");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        if (corePos != null) {
            compound.setIntArray("CorePos", new int[]{corePos.getX(), corePos.getY(), corePos.getZ()});
        }
        compound.setInteger("Lifetime", lifetime);
    }

    /**
     * 获取正在坐着的玩家
     */
    public EntityPlayer getSeatedPlayer() {
        for (Entity passenger : getPassengers()) {
            if (passenger instanceof EntityPlayer) {
                return (EntityPlayer) passenger;
            }
        }
        return null;
    }

    /**
     * 让玩家坐下
     */
    public boolean seatPlayer(EntityPlayer player) {
        if (player.isRiding()) return false;
        if (!getPassengers().isEmpty()) return false;

        player.startRiding(this, true);
        return true;
    }

    public BlockPos getCorePos() {
        return corePos;
    }
}
