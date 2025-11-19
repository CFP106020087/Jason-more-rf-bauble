package com.moremod.core.capability;

import com.moremod.core.api.IMechanicalCoreData;
import com.moremod.core.migration.MechanicalCoreLegacyMigration;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 机械核心Capability Provider
 *
 * 负责：
 * 1. 提供IMechanicalCoreData能力
 * 2. 处理NBT序列化/反序列化
 * 3. 自动触发旧存档迁移
 */
public class MechanicalCoreProvider implements ICapabilityProvider, INBTSerializable<NBTTagCompound> {

    private final IMechanicalCoreData data = new MechanicalCoreData();
    private final ItemStack stack;
    private boolean migrated = false;

    public MechanicalCoreProvider(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == MechanicalCoreCapability.MECHANICAL_CORE_DATA;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == MechanicalCoreCapability.MECHANICAL_CORE_DATA) {
            // 第一次访问时执行旧数据迁移
            if (!migrated) {
                migrateFromLegacyNBT();
                migrated = true;
            }
            return MechanicalCoreCapability.MECHANICAL_CORE_DATA.cast(data);
        }
        return null;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();

        // 保存Capability数据
        NBTTagCompound capData = data.serializeNBT();
        nbt.setTag("CoreData", capData);

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

    /**
     * 从旧的NBT数据迁移到Capability
     */
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

    /**
     * 获取数据实例（用于直接访问）
     */
    public IMechanicalCoreData getData() {
        if (!migrated) {
            migrateFromLegacyNBT();
            migrated = true;
        }
        return data;
    }
}
