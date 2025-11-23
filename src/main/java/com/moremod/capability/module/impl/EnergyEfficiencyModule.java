package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import com.moremod.config.EnergyBalanceConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 能量效率模块
 *
 * 功能：
 *  - 减少所有升级的能量消耗
 *  - Lv.1~5: 每级减少 5% 能量消耗（在 IMechCoreData.consumeEnergy 中应用）
 *
 * 能量消耗：
 *  - 被动消耗：每级 5 RF/s
 *
 * 注意：
 *  - 效率加成在 MechCoreDataImpl.consumeEnergy() 中自动应用
 *  - 本模块只需要维护等级数据，实际减免逻辑在 Capability 层实现
 */
public class EnergyEfficiencyModule extends AbstractMechCoreModule {

    public static final EnergyEfficiencyModule INSTANCE = new EnergyEfficiencyModule();

    private EnergyEfficiencyModule() {
        super(
            "ENERGY_EFFICIENCY",
            "能量效率",
            "减少所有升级的能量消耗",
            5  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        System.out.println("[EnergyEfficiencyModule] onActivate 被调用: player=" + player.getName() + ", newLevel=" + newLevel);
        // 激活时无需特殊操作
        // 效率加成在 consumeEnergy 时自动应用
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        System.out.println("[EnergyEfficiencyModule] onDeactivate 被调用: player=" + player.getName());
        // 停用时无需特殊操作
        // consumeEnergy 会自动检测等级为 0
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        // 能量效率模块不需要 tick 逻辑
        // 效率加成在每次 consumeEnergy 时自动应用
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        System.out.println("[EnergyEfficiencyModule] onLevelChanged 被调用: player=" + player.getName() + ", oldLevel=" + oldLevel + ", newLevel=" + newLevel);
        // 等级变化时无需特殊操作
        // 新等级会在下次 consumeEnergy 时自动生效

        // 可选：向玩家显示效率变化
        if (newLevel > oldLevel && !player.world.isRemote) {
            double reduction = newLevel * 5.0;
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.GREEN +
                    "⚡ 能量效率提升至 Lv." + newLevel + " (减少 " + (int)reduction + "% 消耗)"
                ),
                true
            );
        }
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 每级消耗 5 RF/s
        return EnergyBalanceConfig.BasicUpgrades.ENERGY_EFFICIENCY * level;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 能量效率模块总是可以执行（只要有能量支付被动消耗）
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

    /**
     * 获取当前效率加成（仅用于显示，实际计算在 MechCoreDataImpl 中）
     * @param level 模块等级
     * @return 减少百分比（0.05 = 5%）
     */
    public static double getEfficiencyReduction(int level) {
        return level * 0.05;  // 每级减少 5%
    }
}
