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
        if (amount < 0) return false;

        // 应用能量效率（ENERGY_EFFICIENCY 模块）
        int efficiencyLevel = getModuleLevel("ENERGY_EFFICIENCY");
        double multiplier = getEfficiencyMultiplier(efficiencyLevel);
        int actualCost = (int) (amount * multiplier);

        // 确保至少消耗1点能量（如果原本要消耗的话）
        if (amount > 0 && actualCost == 0) {
            actualCost = 1;
        }

        if (energy < actualCost) {
            return false;
        }

        energy -= actualCost;
        markDirty();
        return true;
    }

    @Override
    public int addEnergy(int amount) {
        if (amount <= 0) return 0;

        int toAdd = Math.min(maxEnergy - energy, amount);
        energy += toAdd;

        if (toAdd > 0) {
            markDirty();
        }

        return toAdd;
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

    /**
     * 获取能量效率倍率
     *
     * 基于 ENERGY_EFFICIENCY 模块等级
     * - Lv.0: 1.00 (无减免)
     * - Lv.1: 0.85 (15% 减免)
     * - Lv.2: 0.70 (30% 减免)
     * - Lv.3: 0.55 (45% 减免)
     * - Lv.4: 0.40 (60% 减免)
     * - Lv.5: 0.25 (75% 减免)
     * - Lv.6+: 继续递减，最低 0.10
     */
    private double getEfficiencyMultiplier(int level) {
        switch (level) {
            case 0: return 1.00;
            case 1: return 0.85;
            case 2: return 0.70;
            case 3: return 0.55;
            case 4: return 0.40;
            case 5: return 0.25;
            default:
                if (level > 5) {
                    return Math.max(0.10, 0.25 - (level - 5) * 0.05);
                }
                return 1.00;
        }
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
        System.out.println("[MechCoreDataImpl@" + System.identityHashCode(this) + "] getModuleLevel: " + moduleId);
        int level = moduleContainer.getLevel(moduleId);
        System.out.println("[MechCoreDataImpl@" + System.identityHashCode(this) + "] 返回等级: " + level);
        return level;
    }

    @Override
    public void setModuleLevel(String moduleId, int level) {
        System.out.println("[MechCoreDataImpl@" + System.identityHashCode(this) + "] setModuleLevel: moduleId=" + moduleId + ", level=" + level);
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

    @Override
    public int getOriginalMaxLevel(String moduleId) {
        return moduleContainer.getOriginalMaxLevel(moduleId);
    }

    @Override
    public void setOriginalMaxLevel(String moduleId, int maxLevel) {
        moduleContainer.setOriginalMaxLevel(moduleId, maxLevel);
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
