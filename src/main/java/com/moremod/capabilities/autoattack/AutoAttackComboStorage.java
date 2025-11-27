package com.moremod.capabilities.autoattack;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

/**
 * 自动攻击数据存储
 */
public class AutoAttackComboStorage implements IStorage<IAutoAttackCombo> {
    
    @Override
    public NBTBase writeNBT(Capability<IAutoAttackCombo> capability, IAutoAttackCombo instance, EnumFacing side) {
        NBTTagCompound nbt = new NBTTagCompound();
        
        nbt.setBoolean("autoAttacking", instance.isAutoAttacking());
        nbt.setInteger("comboCount", instance.getComboCount());
        nbt.setFloat("comboPower", instance.getComboPower());
        nbt.setLong("lastAttackTime", instance.getLastAttackTime());
        nbt.setInteger("comboTime", instance.getComboTime());
        nbt.setFloat("attackSpeedMultiplier", instance.getAttackSpeedMultiplier());
        
        return nbt;
    }
    
    @Override
    public void readNBT(Capability<IAutoAttackCombo> capability, IAutoAttackCombo instance, EnumFacing side, NBTBase nbt) {
        if (nbt instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) nbt;
            
            instance.setAutoAttacking(compound.getBoolean("autoAttacking"));
            instance.setComboCount(compound.getInteger("comboCount"));
            instance.setComboPower(compound.getFloat("comboPower"));
            instance.setLastAttackTime(compound.getLong("lastAttackTime"));
            instance.setComboTime(compound.getInteger("comboTime"));
            instance.setAttackSpeedMultiplier(compound.getFloat("attackSpeedMultiplier"));
        }
    }
}