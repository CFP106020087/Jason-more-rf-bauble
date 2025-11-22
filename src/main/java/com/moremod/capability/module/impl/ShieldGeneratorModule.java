package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import com.moremod.config.EnergyBalanceConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 护盾发生器模块（黄条护盾）
 *
 * 功能：
 *  - Lv.1: 基础护盾（1 点黄条）
 *  - Lv.2: 增强护盾（2 点黄条）
 *  - Lv.3: 高级护盾（3 点黄条）
 *  - Lv.4: 终极护盾（4 点黄条）
 *  - Lv.5: 完美护盾（5 点黄条）
 *
 * 能量消耗：
 *  - 维持：每点护盾 20 RF/tick
 *  - 恢复：每点护盾 100 RF
 */
public class ShieldGeneratorModule extends AbstractMechCoreModule {

    public static final ShieldGeneratorModule INSTANCE = new ShieldGeneratorModule();

    private ShieldGeneratorModule() {
        super(
            "YELLOW_SHIELD",
            "黄条护盾",
            "提供额外护盾保护层",
            5  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 激活时初始化护盾值
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        if (!meta.hasKey("CURRENT_SHIELD")) {
            meta.setFloat("CURRENT_SHIELD", 0.0F);
        }
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 停用时清除护盾
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setFloat("CURRENT_SHIELD", 0.0F);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        float currentShield = meta.getFloat("CURRENT_SHIELD");
        float maxShield = (float) level;

        // 护盾恢复逻辑
        if (currentShield < maxShield) {
            int restoreCost = EnergyBalanceConfig.SurvivalActive.SHIELD_RESTORE_PER_POINT;

            if (data.getEnergy() >= restoreCost) {
                data.consumeEnergy(restoreCost);
                currentShield = Math.min(currentShield + 0.1F, maxShield);
                meta.setFloat("CURRENT_SHIELD", currentShield);
            }
        }

        // 更新玩家吸收伤害属性（黄条显示）
        if (currentShield > 0) {
            player.setAbsorptionAmount(currentShield);
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时调整护盾上限
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        float currentShield = meta.getFloat("CURRENT_SHIELD");

        // 如果当前护盾超过新上限，裁剪到新上限
        if (currentShield > newLevel) {
            meta.setFloat("CURRENT_SHIELD", (float) newLevel);
            player.setAbsorptionAmount((float) newLevel);
        }
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 每点护盾维持消耗
        return level * EnergyBalanceConfig.SurvivalActive.SHIELD_MAINTAIN_PER_LEVEL;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        return data.getEnergy() >= getPassiveEnergyCost(data.getModuleLevel(getModuleId()));
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setFloat("CURRENT_SHIELD", 0.0F);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("CURRENT_SHIELD")) {
            meta.setFloat("CURRENT_SHIELD", 0.0F);
        }
        return true;
    }
}
