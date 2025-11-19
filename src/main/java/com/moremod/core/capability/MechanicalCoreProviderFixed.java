package com.moremod.core.capability;

import com.moremod.core.api.IMechanicalCoreData;
import com.moremod.core.migration.MechanicalCoreLegacyMigration;
import com.moremod.config.EnergyBalanceConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 修正的Provider - 同时提供能量和数据能力
 *
 * ⚠️ 重要：原系统使用独立的能量Provider，这里合并为一个
 */
public class MechanicalCoreProviderFixed implements ICapabilityProvider, INBTSerializable<NBTTagCompound> {

    private final IMechanicalCoreData data = new MechanicalCoreData();
    private final MechanicalCoreEnergyStorage energyStorage;
    private final ItemStack stack;
    private boolean migrated = false;

    // 递归保护
    private static final ThreadLocal<Boolean> isCalculatingEnergy = ThreadLocal.withInitial(() -> false);

    public MechanicalCoreProviderFixed(ItemStack stack) {
        this.stack = stack;
        this.energyStorage = new MechanicalCoreEnergyStorage(stack);
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == MechanicalCoreCapability.MECHANICAL_CORE_DATA ||
               capability == CapabilityEnergy.ENERGY;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        // 数据能力
        if (capability == MechanicalCoreCapability.MECHANICAL_CORE_DATA) {
            if (!migrated) {
                migrateFromLegacyNBT();
                migrated = true;
            }
            return MechanicalCoreCapability.MECHANICAL_CORE_DATA.cast(data);
        }

        // 能量能力
        if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(energyStorage);
        }

        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();

        // 保存Capability数据
        NBTTagCompound capData = data.serializeNBT();
        nbt.setTag("CoreData", capData);

        // 保存能量（能量存储在NBT的"Energy"键中）
        nbt.setInteger("Energy", energyStorage.getEnergyStored());

        // 标记为已迁移
        nbt.setBoolean("Migrated", migrated);

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        // 读取Capability数据
        if (nbt.hasKey("CoreData")) {
            NBTTagCompound capData = nbt.getCompoundTag("CoreData");
            data.deserializeNBT(capData);
        }

        // 读取迁移标记
        migrated = nbt.getBoolean("Migrated");

        // 如果未迁移，且ItemStack的NBT包含旧数据，则执行迁移
        if (!migrated && stack != null && stack.hasTagCompound()) {
            NBTTagCompound stackNBT = stack.getTagCompound();
            if (stackNBT != null && !stackNBT.getBoolean("Core3_Migrated")) {
                migrateFromLegacyNBT();
                migrated = true;
            }
        }
    }

    private void migrateFromLegacyNBT() {
        if (stack == null || !stack.hasTagCompound()) {
            return;
        }

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            return;
        }

        // 检查是否已经迁移过
        if (nbt.getBoolean("Core3_Migrated")) {
            // 已迁移，直接加载Capability数据
            if (nbt.hasKey("CoreData")) {
                data.deserializeNBT(nbt.getCompoundTag("CoreData"));
            }
            migrated = true;
            return;
        }

        // 执行旧数据迁移
        MechanicalCoreLegacyMigration.migrate(stack, data);

        // 标记为已迁移
        nbt.setBoolean("Core3_Migrated", true);

        // 保存迁移后的数据
        NBTTagCompound capData = data.serializeNBT();
        nbt.setTag("CoreData", capData);

        migrated = true;
    }

    public IMechanicalCoreData getData() {
        if (!migrated) {
            migrateFromLegacyNBT();
            migrated = true;
        }
        return data;
    }

    /**
     * 能量存储实现（与原系统兼容）
     */
    private class MechanicalCoreEnergyStorage implements IEnergyStorage {
        private static final String NBT_ENERGY = "Energy";
        private final ItemStack container;

        MechanicalCoreEnergyStorage(ItemStack stack) {
            this.container = stack;
            initNBT();
        }

        private void initNBT() {
            if (!container.hasTagCompound()) {
                container.setTagCompound(new NBTTagCompound());
            }
            if (!container.getTagCompound().hasKey(NBT_ENERGY)) {
                container.getTagCompound().setInteger(NBT_ENERGY, 0);
            }
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energy = getEnergyStored();
            int maxEnergy = getMaxEnergyStored();
            int received = Math.min(maxEnergy - energy,
                                  Math.min(maxReceive, EnergyBalanceConfig.BASE_ENERGY_TRANSFER));
            if (!simulate && received > 0) {
                setEnergy(energy + received);
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int energy = getEnergyStored();
            int extracted = Math.min(energy,
                                   Math.min(maxExtract, EnergyBalanceConfig.BASE_ENERGY_TRANSFER));
            if (!simulate && extracted > 0) {
                setEnergy(energy - extracted);
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return container.hasTagCompound() ?
                   container.getTagCompound().getInteger(NBT_ENERGY) : 0;
        }

        @Override
        public int getMaxEnergyStored() {
            // 递归保护
            if (isCalculatingEnergy.get()) {
                return EnergyBalanceConfig.BASE_ENERGY_CAPACITY;
            }

            try {
                isCalculatingEnergy.set(true);

                int capacityLevel = 0;
                if (container.hasTagCompound()) {
                    NBTTagCompound nbt = container.getTagCompound();

                    // 检查energy_capacity升级是否禁用/暂停
                    if (!nbt.getBoolean("Disabled_energy_capacity") &&
                        !nbt.getBoolean("IsPaused_energy_capacity")) {
                        capacityLevel = nbt.getInteger("upgrade_energy_capacity");
                        if (capacityLevel < 0) capacityLevel = 0;
                    }
                }

                return EnergyBalanceConfig.BASE_ENERGY_CAPACITY +
                       capacityLevel * EnergyBalanceConfig.ENERGY_PER_CAPACITY_LEVEL;
            } finally {
                isCalculatingEnergy.set(false);
            }
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }

        private void setEnergy(int energy) {
            if (!container.hasTagCompound()) {
                container.setTagCompound(new NBTTagCompound());
            }
            container.getTagCompound().setInteger(NBT_ENERGY,
                Math.max(0, Math.min(getMaxEnergyStored(), energy)));
        }
    }
}
