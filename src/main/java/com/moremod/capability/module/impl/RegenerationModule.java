package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import com.moremod.config.EnergyBalanceConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 生命恢复模块
 *
 * 功能：
 *  - 自动恢复玩家生命值
 *  - Lv.1: 每 100 tick 恢复 0.5 心
 *  - Lv.2: 每 80 tick 恢复 0.5 心
 *  - Lv.3: 每 60 tick 恢复 0.5 心
 *  - Lv.4: 每 40 tick 恢复 0.5 心
 *  - Lv.5: 每 20 tick 恢复 0.5 心
 *
 * 能量消耗：
 *  - 每次恢复消耗 100 RF
 */
public class RegenerationModule extends AbstractMechCoreModule {

    public static final RegenerationModule INSTANCE = new RegenerationModule();

    private RegenerationModule() {
        super(
            "HEALTH_REGEN",
            "生命恢复",
            "自动恢复玩家生命值",
            5  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 初始化恢复计时器
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setInteger("REGEN_TIMER", 0);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 清除恢复计时器
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.removeTag("REGEN_TIMER");
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        // 如果玩家生命已满，不执行恢复
        if (player.getHealth() >= player.getMaxHealth()) {
            return;
        }

        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        int timer = meta.getInteger("REGEN_TIMER");

        // 计算恢复间隔（等级越高，间隔越短）
        int regenInterval = getRegenInterval(level);

        timer++;
        if (timer >= regenInterval) {
            // 检查能量
            int energyCost = EnergyBalanceConfig.SurvivalActive.HEALTH_REGEN_COST_PER_HALF_HEART;

            if (data.getEnergy() >= energyCost) {
                // 消耗能量并恢复生命
                data.consumeEnergy(energyCost);
                player.heal(1.0F);  // 0.5 心 = 1.0 HP
                timer = 0;
            } else {
                // 能量不足，暂停恢复（但保持计时器）
            }
        }

        meta.setInteger("REGEN_TIMER", timer);
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时重置计时器
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setInteger("REGEN_TIMER", 0);
    }

    /**
     * 获取恢复间隔（tick）
     */
    private int getRegenInterval(int level) {
        switch (level) {
            case 1: return 100;
            case 2: return 80;
            case 3: return 60;
            case 4: return 40;
            case 5: return 20;
            default: return 100;
        }
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 生命恢复模块没有固定的被动消耗
        // 实际消耗取决于恢复频率
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 只要玩家生命未满，就可以执行
        return player.getHealth() < player.getMaxHealth();
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setInteger("REGEN_TIMER", 0);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("REGEN_TIMER")) {
            meta.setInteger("REGEN_TIMER", 0);
        }
        return true;
    }
}
