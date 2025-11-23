package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import com.moremod.config.EnergyBalanceConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 虚空能量模块
 *
 * 功能：
 *  - 末地/深层环境产生能量
 *  - 充能系统（积累后转换）
 *  - Lv.1-5: 每级提升产能
 *
 * 能量产出：
 *  - 充能速率：2/tick * 等级 * 区域倍率
 *  - 转换率：100 充能 → 25 RF
 *  - 末地倍率：1.5x
 *  - 末地额外奖励：80 RF/5s * 等级
 *  - 深层：Y < 20 (3x), Y < 0 (更高)
 */
public class VoidEnergyModule extends AbstractMechCoreModule {

    public static final VoidEnergyModule INSTANCE = new VoidEnergyModule();

    private VoidEnergyModule() {
        super(
            "VOID_ENERGY",
            "虚空能量",
            "末地或深层环境产生能量",
            5  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 初始化充能数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setInteger("VOID_CHARGE", 0);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 清除充能数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setInteger("VOID_CHARGE", 0);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        boolean inVoidZone = false;
        float zoneMult = 1;

        // 末地
        if (player.dimension == 1) {
            inVoidZone = true;
            zoneMult = EnergyBalanceConfig.VoidEnergy.END_MULTIPLIER;
        }

        // 深层/虚空
        if (player.posY < EnergyBalanceConfig.VoidEnergy.DEEP_Y_LEVEL) {
            inVoidZone = true;
            if (player.posY < EnergyBalanceConfig.VoidEnergy.VOID_Y_LEVEL) {
                // 更深处额外加成
                zoneMult = Math.max(zoneMult, 3);
            }
        }

        if (!inVoidZone) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 每 tick 充能
        int perTick = EnergyBalanceConfig.VoidEnergy.CHARGE_PER_TICK;
        int charge = meta.getInteger("VOID_CHARGE");
        charge += (int)(perTick * level * zoneMult);

        // 转换：每满 100 充能 → CHARGE_CONVERSION RF
        int batches = charge / 100;
        if (batches > 0) {
            int rf = batches * EnergyBalanceConfig.VoidEnergy.CHARGE_CONVERSION;

            if (rf > 0) {
                data.addEnergy(rf);

                // 视觉效果
                for (int i = 0; i < Math.min(6, batches * 2); i++) {
                    player.world.spawnParticle(
                            net.minecraft.util.EnumParticleTypes.PORTAL,
                            player.posX + (player.getRNG().nextDouble() - 0.5) * 2,
                            player.posY + player.getRNG().nextDouble() * 2,
                            player.posZ + (player.getRNG().nextDouble() - 0.5) * 2,
                            0, 0, 0
                    );
                }
            }
            charge = charge % 100;
        }

        meta.setInteger("VOID_CHARGE", charge);

        // 末地额外奖励：每 100 tick
        if (player.dimension == 1 && player.world.getTotalWorldTime() % 100 == 0) {
            int bonus = EnergyBalanceConfig.VoidEnergy.END_BONUS * level;

            if (bonus > 0) {
                data.addEnergy(bonus);
            }
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时不清除充能（保留进度）
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 虚空能量是产能模块，无消耗
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 只要在虚空区域，就可以执行
        return player.dimension == 1 || player.posY < EnergyBalanceConfig.VoidEnergy.DEEP_Y_LEVEL;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setInteger("VOID_CHARGE", 0);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("VOID_CHARGE")) {
            meta.setInteger("VOID_CHARGE", 0);
        }
        return true;
    }
}
