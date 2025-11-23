package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 自动灭火模块
 *
 * 功能：
 *  - 玩家着火时自动灭火
 *  - Lv.1: 60 tick (3秒) 冷却
 *  - Lv.2: 40 tick (2秒) 冷却
 *  - Lv.3: 20 tick (1秒) 冷却
 *
 * 能量消耗：
 *  - 每次灭火：50 RF
 */
public class FireExtinguishModule extends AbstractMechCoreModule {

    public static final FireExtinguishModule INSTANCE = new FireExtinguishModule();

    private FireExtinguishModule() {
        super(
            "FIRE_EXTINGUISH",
            "自动灭火",
            "着火时自动灭火",
            3  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 初始化计时器
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setLong("LAST_EXTINGUISH", 0);
        meta.setBoolean("SYSTEM_ACTIVE", false);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 清除状态
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setBoolean("SYSTEM_ACTIVE", false);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        if (player.isBurning()) {
            long lastExtinguish = meta.getLong("LAST_EXTINGUISH");
            long currentTime = player.world.getTotalWorldTime();

            // 冷却时间：60/40/20 tick
            int cooldown = 80 - level * 20;

            if (currentTime - lastExtinguish >= cooldown) {
                // 灭火消耗能量
                if (data.consumeEnergy(50)) {
                    player.extinguish();
                    meta.setLong("LAST_EXTINGUISH", currentTime);

                    // 标记系统激活
                    if (!meta.getBoolean("SYSTEM_ACTIVE")) {
                        meta.setBoolean("SYSTEM_ACTIVE", true);
                    }

                    // 粒子效果
                    for (int i = 0; i < 10; i++) {
                        player.world.spawnParticle(
                                net.minecraft.util.EnumParticleTypes.WATER_SPLASH,
                                player.posX + (player.getRNG().nextDouble() - 0.5) * player.width,
                                player.posY + player.getRNG().nextDouble() * player.height,
                                player.posZ + (player.getRNG().nextDouble() - 0.5) * player.width,
                                0, 0.1, 0
                        );
                    }

                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.BLUE + "💧 自动灭火系统启动！"
                    ), true);
                } else {
                    // 能量不足，无法灭火
                    if (player.world.getTotalWorldTime() % 40 == 0) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.DARK_RED + "⚡ 灭火系统能量不足！"
                        ), true);
                    }
                }
            }
        } else {
            // 不在燃烧时重置状态
            if (meta.getBoolean("SYSTEM_ACTIVE")) {
                meta.setBoolean("SYSTEM_ACTIVE", false);
            }
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时重置计时器
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setLong("LAST_EXTINGUISH", 0);
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 自动灭火没有被动消耗（仅在灭火时消耗）
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 只要玩家在燃烧且有能量，就可以执行
        return player.isBurning() && data.getEnergy() >= 50;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setLong("LAST_EXTINGUISH", 0);
        meta.setBoolean("SYSTEM_ACTIVE", false);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("LAST_EXTINGUISH")) {
            meta.setLong("LAST_EXTINGUISH", 0);
        }
        if (!meta.hasKey("SYSTEM_ACTIVE")) {
            meta.setBoolean("SYSTEM_ACTIVE", false);
        }
        return true;
    }
}
