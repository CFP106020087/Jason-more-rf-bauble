package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 飞行模块
 *
 * 功能：
 *  - Lv.1: 基础飞行（创造模式风格）
 *  - Lv.2: 高级飞行（速度提升）
 *  - Lv.3: 终极飞行（惯性消除 + 悬停模式）
 *
 * 能量消耗：
 *  - Lv.1: 10 RF/tick
 *  - Lv.2: 15 RF/tick
 *  - Lv.3: 20 RF/tick
 */
public class FlightModule extends AbstractMechCoreModule {

    public static final FlightModule INSTANCE = new FlightModule();

    private FlightModule() {
        super(
            "FLIGHT_MODULE",
            "飞行模块",
            "提供创造模式飞行能力",
            3
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 激活飞行
        if (!player.world.isRemote && !player.isCreative() && !player.isSpectator()) {
            player.capabilities.allowFlying = true;
            player.sendPlayerAbilities();
        }
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 停用飞行
        if (!player.world.isRemote && !player.isCreative() && !player.isSpectator()) {
            player.capabilities.allowFlying = false;
            player.capabilities.isFlying = false;
            player.sendPlayerAbilities();
        }
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        // 确保飞行能力开启
        if (!player.isCreative() && !player.isSpectator()) {
            if (!player.capabilities.allowFlying) {
                player.capabilities.allowFlying = true;
                player.sendPlayerAbilities();
            }

            // Lv.2+: 飞行速度提升
            if (level >= 2 && player.capabilities.isFlying) {
                float speedBoost = 0.05F * (level - 1);
                player.capabilities.setFlySpeed(0.05F + speedBoost);
            }

            // Lv.3: 惯性消除（悬停模式）
            if (level >= 3) {
                NBTTagCompound meta = data.getModuleMeta(getModuleId());
                boolean hoverMode = meta.getBoolean("HOVER_MODE");

                if (hoverMode && player.capabilities.isFlying) {
                    // 减少惯性
                    player.motionX *= 0.5;
                    player.motionZ *= 0.5;

                    // 悬停高度锁定
                    if (meta.hasKey("ALTITUDE_LOCK")) {
                        double lockedY = meta.getDouble("ALTITUDE_LOCK");
                        if (Math.abs(player.posY - lockedY) < 0.5) {
                            player.motionY = 0;
                            player.setPosition(player.posX, lockedY, player.posZ);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        if (newLevel > 0 && oldLevel == 0) {
            // 首次解锁
            onActivate(player, data, newLevel);
        } else if (newLevel == 0 && oldLevel > 0) {
            // 完全移除
            onDeactivate(player, data);
        }
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        return level * 10; // Lv.1=10RF, Lv.2=20RF, Lv.3=30RF
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 能量充足且不在创造/旁观模式
        return data.getEnergy() >= getPassiveEnergyCost(data.getModuleLevel(getModuleId()))
            && !player.isCreative()
            && !player.isSpectator();
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setBoolean("HOVER_MODE", false);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        // 确保必需字段存在
        if (!meta.hasKey("HOVER_MODE")) {
            meta.setBoolean("HOVER_MODE", false);
        }
        return true;
    }
}
