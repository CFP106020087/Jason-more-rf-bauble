package com.moremod.causal;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.*;

public final class EnergyHelper {
    public static boolean drainCoreEnergy(EntityPlayer p, int amount){
        if (amount<=0) return true;
        IBaublesItemHandler b = BaublesApi.getBaublesHandler(p);
        if (b!=null){
            for (int i=0;i<b.getSlots();i++){
                ItemStack s=b.getStackInSlot(i);
                IEnergyStorage es = s.hasCapability(CapabilityEnergy.ENERGY,null)? s.getCapability(CapabilityEnergy.ENERGY,null):null;
                if (es!=null && es.extractEnergy(amount,true)>=amount){ es.extractEnergy(amount,false); return true; }
            }
        }
        for (ItemStack s: p.inventory.mainInventory){
            IEnergyStorage es = s.hasCapability(CapabilityEnergy.ENERGY,null)? s.getCapability(CapabilityEnergy.ENERGY,null):null;
            if (es!=null && es.extractEnergy(amount,true)>=amount){ es.extractEnergy(amount,false); return true; }
        }
        return false;
    }
}
