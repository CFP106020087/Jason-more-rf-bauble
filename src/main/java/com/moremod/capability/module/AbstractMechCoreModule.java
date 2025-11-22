package com.moremod.capability.module;

import com.moremod.capability.IMechCoreData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 模块抽象基类
 *
 * 提供默认实现，减少样板代码
 */
public abstract class AbstractMechCoreModule implements IMechCoreModule {

    private final String moduleId;
    private final String displayName;
    private final String description;
    private final int maxLevel;

    protected AbstractMechCoreModule(
        String moduleId,
        String displayName,
        String description,
        int maxLevel
    ) {
        this.moduleId = moduleId;
        this.displayName = displayName;
        this.description = description;
        this.maxLevel = maxLevel;
    }

    @Override
    public String getModuleId() {
        return moduleId;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    // ────────────────────────────────────────────────────────────
    // 默认空实现（子类按需覆盖）
    // ────────────────────────────────────────────────────────────

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 默认不做任何事
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 默认不做任何事
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        // 默认不做任何事
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 默认不做任何事
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 默认无被动消耗
        return 0;
    }

    @Override
    public int getActiveEnergyCost(int level, ModuleContext context) {
        // 默认无主动消耗
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 默认总是可以执行
        return true;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        // 默认空元数据
        return new NBTTagCompound();
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        // 默认总是有效
        return true;
    }
}
