package com.moremod.capabilities.autoattack;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

/**
 * 自动攻击Capability提供者
 */
public class AutoAttackComboProvider implements ICapabilitySerializable<NBTBase> {
    
    @CapabilityInject(IAutoAttackCombo.class)
    public static final Capability<IAutoAttackCombo> AUTO_ATTACK_CAP = null;
    
    private IAutoAttackCombo instance = AUTO_ATTACK_CAP.getDefaultInstance();
    
    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == AUTO_ATTACK_CAP;
    }
    
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return capability == AUTO_ATTACK_CAP ? AUTO_ATTACK_CAP.<T>cast(this.instance) : null;
    }
    
    @Override
    public NBTBase serializeNBT() {
        return AUTO_ATTACK_CAP.getStorage().writeNBT(AUTO_ATTACK_CAP, this.instance, null);
    }
    
    @Override
    public void deserializeNBT(NBTBase nbt) {
        AUTO_ATTACK_CAP.getStorage().readNBT(AUTO_ATTACK_CAP, this.instance, null, nbt);
    }
}