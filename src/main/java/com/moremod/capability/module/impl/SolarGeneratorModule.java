package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import com.moremod.config.EnergyBalanceConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

/**
 * 太阳能发电模块
 *
 * 功能：
 *  - 白天且能见天时产生能量
 *  - 高度加成（Y > 100）
 *  - 天气惩罚（雨天/雷暴）
 *  - Lv.1-5: 每级提升产能
 *
 * 能量产出：
 *  - 基础：40 RF/s * 等级
 *  - 高度加成：最大 1.3x
 *  - 雨天：0.4x
 *  - 雷暴：0.2x
 */
public class SolarGeneratorModule extends AbstractMechCoreModule {

    public static final SolarGeneratorModule INSTANCE = new SolarGeneratorModule();

    private SolarGeneratorModule() {
        super(
            "SOLAR_GENERATOR",
            "太阳能发电",
            "白天能见天时产生能量",
            5  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 初始化计时器
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setLong("SOLAR_LAST_TICK", 0);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 清除计时器
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setLong("SOLAR_LAST_TICK", 0);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        // 能见天、白天、光照阈值
        BlockPos pos = player.getPosition();
        if (!player.world.canSeeSky(pos)) return;
        if (!player.world.isDaytime()) return;

        int minSky = EnergyBalanceConfig.SolarGenerator.MIN_SKY_LIGHT;
        int skyLight = player.world.getLightFor(EnumSkyBlock.SKY, pos);
        if (skyLight < minSky) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 周期：每 20 tick
        long lastTick = meta.getLong("SOLAR_LAST_TICK");
        long currentTime = player.world.getTotalWorldTime();
        if (currentTime - lastTick < 20) return;

        // 基础：每级产能
        int base = EnergyBalanceConfig.SolarGenerator.ENERGY_PER_LEVEL * level;

        // 高度加成（>100 线性增长），封顶
        double heightBonus = 1.0;
        if (player.posY > 100.0) {
            heightBonus = 1.0 + (player.posY - 100.0) / 100.0;
            heightBonus = Math.min(heightBonus, EnergyBalanceConfig.SolarGenerator.HEIGHT_BONUS_MAX);
        }

        // 天气惩罚（雷暴优先于雨）
        double weather = 1.0;
        if (player.world.isThundering()) {
            weather = EnergyBalanceConfig.SolarGenerator.STORM_PENALTY;
        } else if (player.world.isRaining()) {
            weather = EnergyBalanceConfig.SolarGenerator.RAIN_PENALTY;
        }

        int energy = (int) Math.floor(base * heightBonus * weather);

        if (energy > 0) {
            data.addEnergy(energy);
            meta.setLong("SOLAR_LAST_TICK", currentTime);

            // 视觉效果
            if (energy >= 10 && player.world.rand.nextInt(4) == 0) {
                for (int i = 0; i < 2; i++) {
                    player.world.spawnParticle(
                            net.minecraft.util.EnumParticleTypes.VILLAGER_HAPPY,
                            player.posX + (player.getRNG().nextDouble() - 0.5),
                            player.posY + 1.8,
                            player.posZ + (player.getRNG().nextDouble() - 0.5),
                            0, -0.05, 0
                    );
                }
            }
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时重置计时器
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setLong("SOLAR_LAST_TICK", 0);
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 太阳能发电是产能模块，无消耗
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 只要是白天且能见天，就可以执行
        return player.world.isDaytime() && player.world.canSeeSky(player.getPosition());
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setLong("SOLAR_LAST_TICK", 0);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("SOLAR_LAST_TICK")) {
            meta.setLong("SOLAR_LAST_TICK", 0);
        }
        return true;
    }
}
