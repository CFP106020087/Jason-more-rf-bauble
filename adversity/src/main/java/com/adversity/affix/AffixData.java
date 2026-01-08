package com.adversity.affix;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 词条数据实现类
 */
public class AffixData implements IAffixData {

    private final IAffix affix;
    private NBTTagCompound customData;
    private int tickCount;
    private boolean active;
    private int cooldown;

    public AffixData(IAffix affix) {
        this.affix = affix;
        this.customData = new NBTTagCompound();
        this.tickCount = 0;
        this.active = true;
        this.cooldown = 0;
    }

    @Override
    public IAffix getAffix() {
        return affix;
    }

    @Override
    public NBTTagCompound getCustomData() {
        return customData;
    }

    @Override
    public void setCustomData(NBTTagCompound data) {
        this.customData = data != null ? data : new NBTTagCompound();
    }

    @Override
    public int getTickCount() {
        return tickCount;
    }

    @Override
    public void incrementTick() {
        this.tickCount++;
    }

    @Override
    public void resetTick() {
        this.tickCount = 0;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }

    @Override
    public void setCooldown(int ticks) {
        this.cooldown = Math.max(0, ticks);
    }

    @Override
    public void decrementCooldown() {
        if (cooldown > 0) {
            cooldown--;
        }
    }

    /**
     * 序列化到 NBT
     */
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("affixId", affix.getId().toString());
        nbt.setTag("customData", customData);
        nbt.setInteger("tickCount", tickCount);
        nbt.setBoolean("active", active);
        nbt.setInteger("cooldown", cooldown);

        // 让词条写入自己的数据
        NBTTagCompound affixNbt = affix.writeToNBT(this);
        if (affixNbt != null && !affixNbt.isEmpty()) {
            nbt.setTag("affixSpecific", affixNbt);
        }

        return nbt;
    }

    /**
     * 从 NBT 反序列化
     */
    public void deserializeNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("customData")) {
            this.customData = nbt.getCompoundTag("customData");
        }
        this.tickCount = nbt.getInteger("tickCount");
        this.active = nbt.getBoolean("active");
        this.cooldown = nbt.getInteger("cooldown");

        // 让词条读取自己的数据
        if (nbt.hasKey("affixSpecific")) {
            affix.readFromNBT(nbt.getCompoundTag("affixSpecific"), this);
        }
    }
}
