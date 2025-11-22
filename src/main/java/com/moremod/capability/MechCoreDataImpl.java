package com.moremod.capability;

import com.moremod.capability.module.ModuleContainer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Mechanical Core 数据实现
 *
 * 存储玩家的所有机械核心相关数据
 */
public class MechCoreDataImpl implements IMechCoreData {

    private int energy;
    private int maxEnergy;
    private final ModuleContainer moduleContainer;
    private boolean dirty;

    public MechCoreDataImpl() {
        this.energy = 0;
        this.maxEnergy = 100000; // 默认容量
        this.moduleContainer = new ModuleContainer();
        this.dirty = false;
    }

    // ────────────────────────────────────────────────────────────
    // 能量管理
    // ────────────────────────────────────────────────────────────

    @Override
    public int getEnergy() {
        return energy;
    }

    @Override
    public void setEnergy(int amount) {
        this.energy = Math.max(0, Math.min(amount, maxEnergy));
        markDirty();
    }

    @Override
    public boolean consumeEnergy(int amount) {
        if (amount < 0 || energy < amount) {
            return false;
        }
        energy -= amount;
        markDirty();
        return true;
    }

    @Override
    public int receiveEnergy(int maxReceive) {
        if (maxReceive < 0) return 0;

        int received = Math.min(maxEnergy - energy, maxReceive);
        energy += received;

        if (received > 0) {
            markDirty();
        }

        return received;
    }

    @Override
    public int getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public void setMaxEnergy(int max) {
        this.maxEnergy = Math.max(0, max);
        // 确保当前能量不超过新的最大值
        if (energy > maxEnergy) {
            energy = maxEnergy;
        }
        markDirty();
    }

    // ────────────────────────────────────────────────────────────
    // 模块系统
    // ────────────────────────────────────────────────────────────

    @Override
    public ModuleContainer getModuleContainer() {
        return moduleContainer;
    }

    @Override
    public int getModuleLevel(String moduleId) {
        return moduleContainer.getLevel(moduleId);
    }

    @Override
    public void setModuleLevel(String moduleId, int level) {
        moduleContainer.setLevel(moduleId, level);
        markDirty();
    }

    @Override
    public boolean isModuleActive(String moduleId) {
        return moduleContainer.isActive(moduleId);
    }

    @Override
    public void setModuleActive(String moduleId, boolean active) {
        moduleContainer.setActive(moduleId, active);
        markDirty();
    }

    // ────────────────────────────────────────────────────────────
    // 元数据
    // ────────────────────────────────────────────────────────────

    @Override
    public NBTTagCompound getModuleMeta(String moduleId) {
        return moduleContainer.getMeta(moduleId);
    }

    @Override
    public void setModuleMeta(String moduleId, NBTTagCompound meta) {
        moduleContainer.setMeta(moduleId, meta);
        markDirty();
    }

    // ────────────────────────────────────────────────────────────
    // 生命周期 & 同步
    // ────────────────────────────────────────────────────────────

    @Override
    public void markDirty() {
        this.dirty = true;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void clearDirty() {
        this.dirty = false;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();

        // 顶层容器
        NBTTagCompound core = new NBTTagCompound();

        // 版本号
        core.setInteger("VERSION", 2);

        // 能量数据
        core.setInteger("ENERGY", energy);
        core.setInteger("MAX_ENERGY", maxEnergy);

        // 模块数据
        core.setTag("MODULES", moduleContainer.serializeNBT());

        nbt.setTag("CORE", core);

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        if (!nbt.hasKey("CORE")) {
            // 尝试旧格式迁移（如果需要）
            return;
        }

        NBTTagCompound core = nbt.getCompoundTag("CORE");

        // 读取能量
        this.energy = core.getInteger("ENERGY");
        this.maxEnergy = core.getInteger("MAX_ENERGY");
        if (this.maxEnergy <= 0) {
            this.maxEnergy = 100000; // 防止数据损坏
        }

        // 读取模块数据
        if (core.hasKey("MODULES")) {
            moduleContainer.deserializeNBT(core.getCompoundTag("MODULES"));
        }

        clearDirty();
    }

    @Override
    public void copyFrom(IMechCoreData source) {
        if (source == null) return;

        // 复制能量
        this.energy = source.getEnergy();
        this.maxEnergy = source.getMaxEnergy();

        // 复制模块数据（通过 NBT）
        NBTTagCompound sourceNBT = source.serializeNBT();
        if (sourceNBT.hasKey("CORE")) {
            NBTTagCompound coreNBT = sourceNBT.getCompoundTag("CORE");
            if (coreNBT.hasKey("MODULES")) {
                moduleContainer.deserializeNBT(coreNBT.getCompoundTag("MODULES"));
            }
        }

        markDirty();
    }
}
