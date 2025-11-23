package com.moremod.capability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.concurrent.Callable;

/**
 * Capability注册和事件处理
 */
public class ChengYueCapabilityHandler {
    
    public static void register() {
        CapabilityManager.INSTANCE.register(
            ChengYueCapability.class,
            new Capability.IStorage<ChengYueCapability>() {
                @Override
                public NBTBase writeNBT(Capability<ChengYueCapability> capability, 
                                       ChengYueCapability instance, EnumFacing side) {
                    return instance.serializeNBT();
                }
                
                @Override
                public void readNBT(Capability<ChengYueCapability> capability, 
                                   ChengYueCapability instance, EnumFacing side, NBTBase nbt) {
                    instance.deserializeNBT((NBTTagCompound) nbt);
                }
            },
            new Callable<ChengYueCapability>() {
                @Override
                public ChengYueCapability call() throws Exception {
                    return new ChengYueCapability();
                }
            }
        );
    }
    
    @SubscribeEvent
    public void onAttachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof EntityPlayer)) {
            return;
        }
        
        event.addCapability(
            new ResourceLocation("moremod", "chengyue_data"),
            new ICapabilitySerializable<NBTTagCompound>() {
                ChengYueCapability instance = new ChengYueCapability();
                
                @Override
                public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
                    return capability == ChengYueCapability.CAPABILITY;
                }
                
                @Override
                public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
                    return capability == ChengYueCapability.CAPABILITY ? (T) instance : null;
                }
                
                @Override
                public NBTTagCompound serializeNBT() {
                    return instance.serializeNBT();
                }
                
                @Override
                public void deserializeNBT(NBTTagCompound nbt) {
                    instance.deserializeNBT(nbt);
                }
            }
        );
    }
    
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            EntityPlayer oldPlayer = event.getOriginal();
            EntityPlayer newPlayer = event.getEntityPlayer();
            
            ChengYueCapability oldCap = oldPlayer.getCapability(ChengYueCapability.CAPABILITY, null);
            ChengYueCapability newCap = newPlayer.getCapability(ChengYueCapability.CAPABILITY, null);
            
            if (oldCap != null && newCap != null) {
                newCap.deserializeNBT(oldCap.serializeNBT());
            }
        }
    }
}