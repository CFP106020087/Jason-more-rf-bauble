package com.moremod.core.capability;

import com.moremod.core.api.IMechanicalCoreData;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;

/**
 * 机械核心Capability注册类
 *
 * 在preInit阶段调用register()方法进行注册
 */
public class MechanicalCoreCapability {

    /**
     * Capability实例（由Forge自动注入）
     */
    @CapabilityInject(IMechanicalCoreData.class)
    public static Capability<IMechanicalCoreData> MECHANICAL_CORE_DATA = null;

    /**
     * 注册Capability（必须在preInit阶段调用）
     */
    public static void register() {
        CapabilityManager.INSTANCE.register(
                IMechanicalCoreData.class,
                new Storage(),
                MechanicalCoreData::new
        );
    }

    /**
     * Capability Storage实现
     */
    public static class Storage implements Capability.IStorage<IMechanicalCoreData> {

        @Nullable
        @Override
        public NBTBase writeNBT(Capability<IMechanicalCoreData> capability,
                               IMechanicalCoreData instance,
                               EnumFacing side) {
            return instance.serializeNBT();
        }

        @Override
        public void readNBT(Capability<IMechanicalCoreData> capability,
                           IMechanicalCoreData instance,
                           EnumFacing side,
                           NBTBase nbt) {
            if (nbt instanceof NBTTagCompound) {
                instance.deserializeNBT((NBTTagCompound) nbt);
            }
        }
    }
}
