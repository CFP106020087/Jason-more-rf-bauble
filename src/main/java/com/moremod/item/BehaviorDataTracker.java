package com.moremod.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 行為數據追蹤器 - 管理所有玩家的行為數據
 */
public class BehaviorDataTracker {
    
    // ===== 內存緩存 =====
    private static final Map<UUID, BehaviorData> dataCache = new ConcurrentHashMap<>();
    
    /**
     * 獲取玩家的行為數據
     */
    public static BehaviorData getData(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        return dataCache.computeIfAbsent(uuid, k -> {
            BehaviorData data = new BehaviorData();
            // 嘗試從玩家 NBT 加載
            if (player.getEntityData().hasKey("BehaviorAnalysisData")) {
                data.readFromNBT(player.getEntityData().getCompoundTag("BehaviorAnalysisData"));
            }
            return data;
        });
    }
    
    /**
     * 保存玩家數據到 NBT
     */
    public static void saveData(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        BehaviorData data = dataCache.get(uuid);
        if (data != null) {
            NBTTagCompound nbt = new NBTTagCompound();
            data.writeToNBT(nbt);
            player.getEntityData().setTag("BehaviorAnalysisData", nbt);
        }
    }
    
    /**
     * 清除玩家數據（登出時）
     */
    public static void clearData(UUID uuid) {
        dataCache.remove(uuid);
    }
    
    /**
     * 衰減所有數據（可選功能，避免數據無限增長）
     * @param factor 保留比例（0.9 = 保留 90%）
     */
    public static void decayAllData(double factor) {
        dataCache.values().forEach(data -> data.decay(factor));
    }
    
    // ===== Capability 實現（可選，更專業的做法） =====
    
    @CapabilityInject(BehaviorData.class)
    public static final Capability<BehaviorData> CAPABILITY = null;
    
    public static class Provider implements ICapabilitySerializable<NBTTagCompound> {
        private final BehaviorData data = new BehaviorData();
        
        @Override
        public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CAPABILITY;
        }
        
        @Nullable
        @Override
        public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
            if (capability == CAPABILITY) {
                return CAPABILITY.cast(data);
            }
            return null;
        }
        
        @Override
        public NBTTagCompound serializeNBT() {
            NBTTagCompound nbt = new NBTTagCompound();
            data.writeToNBT(nbt);
            return nbt;
        }
        
        @Override
        public void deserializeNBT(NBTTagCompound nbt) {
            data.readFromNBT(nbt);
        }
    }
    
    public static class Storage implements Capability.IStorage<BehaviorData> {
        @Nullable
        @Override
        public NBTBase writeNBT(Capability<BehaviorData> capability, BehaviorData instance, EnumFacing side) {
            NBTTagCompound nbt = new NBTTagCompound();
            instance.writeToNBT(nbt);
            return nbt;
        }
        
        @Override
        public void readNBT(Capability<BehaviorData> capability, BehaviorData instance, EnumFacing side, NBTBase nbt) {
            if (nbt instanceof NBTTagCompound) {
                instance.readFromNBT((NBTTagCompound) nbt);
            }
        }
    }
    
    // ===== 快捷方法 =====
    
    public static void recordMobKill(EntityPlayer player) {
        getData(player).addMobKill();
    }
    
    public static void recordPlayerKill(EntityPlayer player) {
        getData(player).addPlayerKill();
    }
    
    public static void recordDamage(EntityPlayer player, double amount, boolean taken) {
        if (taken) {
            getData(player).addDamageTaken(amount);
        } else {
            getData(player).addDamageDealt(amount);
        }
    }
    
    public static void recordBlockPlaced(EntityPlayer player) {
        getData(player).addBlockPlaced();
    }
    
    public static void recordCrafting(EntityPlayer player) {
        getData(player).addCrafting();
    }
    
    public static void recordBlockMined(EntityPlayer player, boolean isOre) {
        getData(player).addBlockMined();
        if (isOre) {
            getData(player).addOreMined();
        }
    }
    
    public static void recordMovement(EntityPlayer player, double distance) {
        getData(player).addDistance(distance);
    }
    
    public static void recordDimensionChange(EntityPlayer player) {
        getData(player).addDimensionChange();
    }
    
    public static void recordCropHarvest(EntityPlayer player) {
        getData(player).addCropHarvest();
    }
    
    public static void recordAnimalBreed(EntityPlayer player) {
        getData(player).addAnimalBreed();
    }
    
    public static void recordDeath(EntityPlayer player) {
        getData(player).addDeath();
    }
    
    public static void recordPlayTime(EntityPlayer player, long ticks) {
        getData(player).addPlayTime(ticks);
    }
}
