package com.moremod.capability;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * 澄月数据 - 存在玩家身上的Capability
 */
public class ChengYueCapability implements INBTSerializable<NBTTagCompound> {
    
    @CapabilityInject(ChengYueCapability.class)
    public static Capability<ChengYueCapability> CAPABILITY = null;
    
    // 连击系统
    private int combo = 0;
    private int maxCombo = 0;
    private long lastHitTime = 0;
    
    // 月华系统
    private int lunarPower = 100;
    private int maxLunarPower = 100;
    
    // 形态系统
    private int currentForm = 0;
    private long formSwitchTime = 0;
    
    // ==================== Getter/Setter ====================
    
    public int getCombo() { return combo; }
    public void setCombo(int combo) { 
        this.combo = combo;
        if (combo > maxCombo) maxCombo = combo;
    }
    public void addCombo() { setCombo(combo + 1); }
    public void resetCombo() { combo = 0; }
    public int getMaxCombo() { return maxCombo; }
    
    public long getLastHitTime() { return lastHitTime; }
    public void setLastHitTime(long time) { this.lastHitTime = time; }
    
    public int getLunarPower() { return lunarPower; }
    public void setLunarPower(int value) {
        this.lunarPower = Math.max(0, Math.min(maxLunarPower, value));
    }
    public void addLunarPower(int amount) {
        setLunarPower(lunarPower + amount);
    }
    public boolean consumeLunarPower(int amount) {
        if (lunarPower >= amount) {
            lunarPower -= amount;
            return true;
        }
        return false;
    }
    public int getMaxLunarPower() { return maxLunarPower; }
    public void setMaxLunarPower(int max) { this.maxLunarPower = max; }
    
    public int getCurrentForm() { return currentForm; }
    public void setCurrentForm(int form) { 
        this.currentForm = form % 8;
        this.formSwitchTime = System.currentTimeMillis();
    }
    public long getFormSwitchTime() { return formSwitchTime; }
    
    // ==================== NBT序列化 ====================
    
    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("MaxCombo", maxCombo);
        nbt.setInteger("LunarPower", lunarPower);
        nbt.setInteger("MaxLunarPower", maxLunarPower);
        nbt.setInteger("CurrentForm", currentForm);
        return nbt;
    }
    
    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        this.maxCombo = nbt.getInteger("MaxCombo");
        this.lunarPower = nbt.getInteger("LunarPower");
        this.maxLunarPower = nbt.getInteger("MaxLunarPower");
        this.currentForm = nbt.getInteger("CurrentForm");
        this.combo = 0;
        this.lastHitTime = 0;
    }
}