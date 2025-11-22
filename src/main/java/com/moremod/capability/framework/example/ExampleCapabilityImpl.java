package com.moremod.capability.framework.example;

import com.moremod.api.capability.ICapability;
import com.moremod.capability.framework.BaseCapability;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 示例能力实现
 * 演示如何实现自定义能力
 */
public class ExampleCapabilityImpl extends BaseCapability<EntityPlayer> implements IExampleCapability {

    public static final String CAPABILITY_ID = "moremod:example_energy";
    private static final String NBT_ENERGY = "Energy";

    private int energy = 0;
    private int maxEnergy = 1000;

    public ExampleCapabilityImpl() {
        super(CAPABILITY_ID);
    }

    @Override
    public int getEnergy() {
        return energy;
    }

    @Override
    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(energy, maxEnergy));
        markDirty();
    }

    @Override
    public int addEnergy(int amount) {
        int oldEnergy = energy;
        setEnergy(energy + amount);
        return energy - oldEnergy;
    }

    @Override
    public boolean consumeEnergy(int amount) {
        if (energy >= amount) {
            setEnergy(energy - amount);
            return true;
        }
        return false;
    }

    @Override
    public int getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public void serializeNBT(NBTTagCompound nbt) {
        nbt.setInteger(NBT_ENERGY, energy);
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        energy = nbt.getInteger(NBT_ENERGY);
    }

    @Override
    public boolean shouldSync() {
        return true; // 需要同步到客户端
    }

    @Override
    public ICapability<EntityPlayer> copyTo(EntityPlayer host) {
        ExampleCapabilityImpl copy = new ExampleCapabilityImpl();
        copy.energy = this.energy;
        copy.maxEnergy = this.maxEnergy;
        return copy;
    }

    @Override
    public void tick(EntityPlayer host) {
        // 示例：每 tick 恢复 1 点能量
        if (energy < maxEnergy) {
            addEnergy(1);
        }
    }
}
