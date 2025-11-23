package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import com.moremod.config.EnergyBalanceConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 能量容量模块
 *
 * 功能：
 *  - 每级增加能量上限
 *  - Lv.1~10: 每级 +10000 RF
 *
 * 能量消耗：
 *  - 无被动消耗（增加容量本身不消耗能量）
 */
public class EnergyCapacityModule extends AbstractMechCoreModule {

    public static final EnergyCapacityModule INSTANCE = new EnergyCapacityModule();

    private EnergyCapacityModule() {
        super(
            "ENERGY_CAPACITY",
            "能量容量",
            "增加机械核心的最大能量存储",
            10  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        System.out.println("[EnergyCapacityModule] onActivate 被调用: player=" + player.getName() + ", newLevel=" + newLevel);
        // 激活时更新最大能量
        updateMaxEnergy(player, data, newLevel);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        System.out.println("[EnergyCapacityModule] onDeactivate 被调用: player=" + player.getName());
        // 停用时恢复基础能量上限
        data.setMaxEnergy(EnergyBalanceConfig.BASE_ENERGY_CAPACITY);

        // 如果当前能量超过基础容量，裁剪
        if (data.getEnergy() > EnergyBalanceConfig.BASE_ENERGY_CAPACITY) {
            data.setEnergy(EnergyBalanceConfig.BASE_ENERGY_CAPACITY);
        }
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        // 能量容量模块不需要 tick 逻辑
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        System.out.println("[EnergyCapacityModule] onLevelChanged 被调用: player=" + player.getName() + ", oldLevel=" + oldLevel + ", newLevel=" + newLevel);
        // 等级变化时更新最大能量
        updateMaxEnergy(player, data, newLevel);
    }

    /**
     * 更新最大能量
     */
    private void updateMaxEnergy(EntityPlayer player, IMechCoreData data, int level) {
        int baseCapacity = EnergyBalanceConfig.BASE_ENERGY_CAPACITY;
        int bonusPerLevel = EnergyBalanceConfig.ENERGY_PER_CAPACITY_LEVEL;
        int newMaxEnergy = baseCapacity + (level * bonusPerLevel);

        System.out.println("[EnergyCapacityModule] updateMaxEnergy:");
        System.out.println("  当前能量: " + data.getEnergy());
        System.out.println("  当前最大能量: " + data.getMaxEnergy());
        System.out.println("  新最大能量: " + newMaxEnergy);
        System.out.println("  (基础: " + baseCapacity + " + 等级" + level + " * " + bonusPerLevel + ")");

        data.setMaxEnergy(newMaxEnergy);

        System.out.println("  设置后最大能量: " + data.getMaxEnergy());

        // 不自动填充能量，保持当前值
        // 如果当前能量超过新上限（降级情况），裁剪
        if (data.getEnergy() > newMaxEnergy) {
            data.setEnergy(newMaxEnergy);
        }
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 无被动消耗
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 能量容量模块总是可以执行（无能量需求）
        return true;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        // 无需额外元数据
        return new NBTTagCompound();
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        // 无需验证
        return true;
    }
}
