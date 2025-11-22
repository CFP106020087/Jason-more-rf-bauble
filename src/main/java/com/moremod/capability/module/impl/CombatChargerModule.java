package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import com.moremod.config.EnergyBalanceConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 战斗充能模块
 *
 * 功能：
 *  - 击杀敌人时产生能量
 *  - 连杀系统
 *  - Boss 倍率
 *  - Lv.1-5: 每级提升产能
 *
 * 能量产出：
 *  - 基础：maxHP * 20 RF/HP * 等级
 *  - Boss 倍率：3.0x
 *  - Mini-Boss 倍率：2.0x
 *  - 连杀倍率：1.0 + 0.1 * streak (最大 2.0x)
 *  - 连杀超时：6000 ticks (5分钟)
 */
public class CombatChargerModule extends AbstractMechCoreModule {

    public static final CombatChargerModule INSTANCE = new CombatChargerModule();

    private CombatChargerModule() {
        super(
            "COMBAT_CHARGER",
            "战斗充能",
            "击杀敌人时产生能量",
            5  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 初始化连杀数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setInteger("COMBAT_STREAK", 0);
        meta.setLong("LAST_KILL_TIME", 0);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 清除连杀数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setInteger("COMBAT_STREAK", 0);
        meta.setLong("LAST_KILL_TIME", 0);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 检查连杀是否超时
        long lastKillTime = meta.getLong("LAST_KILL_TIME");
        long currentTime = player.world.getTotalWorldTime();

        if (currentTime - lastKillTime > EnergyBalanceConfig.CombatCharger.STREAK_TIMEOUT) {
            // 连杀超时，重置
            int streak = meta.getInteger("COMBAT_STREAK");
            if (streak > 0) {
                meta.setInteger("COMBAT_STREAK", 0);
            }
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时重置连杀
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setInteger("COMBAT_STREAK", 0);
    }

    /**
     * 击杀敌人时产生能量
     *
     * 此方法应该在 LivingDeathEvent 中调用
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @param killed 被击杀的实体
     */
    public void onEntityKill(EntityPlayer player, IMechCoreData data, EntityLivingBase killed) {
        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        float maxHP = killed.getMaxHealth();
        double base = maxHP * EnergyBalanceConfig.CombatCharger.ENERGY_PER_HP * level;

        // Boss / 小Boss 倍率
        double bossMul = 1.0;
        if (killed instanceof net.minecraft.entity.boss.EntityDragon
                || killed instanceof net.minecraft.entity.boss.EntityWither) {
            bossMul = EnergyBalanceConfig.CombatCharger.BOSS_MULTIPLIER;
        } else if (!killed.isNonBoss()) {
            bossMul = EnergyBalanceConfig.CombatCharger.MINIBOSS_MULTIPLIER;
        }

        // 连杀系统
        long currentTime = player.world.getTotalWorldTime();
        long lastKillTime = meta.getLong("LAST_KILL_TIME");

        // 检查连杀是否超时
        if (currentTime - lastKillTime > EnergyBalanceConfig.CombatCharger.STREAK_TIMEOUT) {
            meta.setInteger("COMBAT_STREAK", 0);
        }

        int streak = meta.getInteger("COMBAT_STREAK") + 1;
        meta.setInteger("COMBAT_STREAK", streak);
        meta.setLong("LAST_KILL_TIME", currentTime);

        double streakMul = Math.min(1.0 + 0.1 * streak, EnergyBalanceConfig.CombatCharger.MAX_STREAK_BONUS);

        int energy = (int) Math.floor(base * bossMul * streakMul);

        if (energy > 0) {
            data.addEnergy(energy);

            // 视觉效果
            for (int i = 0; i < 12; i++) {
                player.world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.SPELL_MOB,
                        killed.posX + (player.getRNG().nextDouble() - 0.5) * 2,
                        killed.posY + player.getRNG().nextDouble() * 2,
                        killed.posZ + (player.getRNG().nextDouble() - 0.5) * 2,
                        1.0, 0.0, 0.0
                );
            }

            // Boss/小Boss 掉落能量精华（红石）
            if (bossMul > 1.0) {
                net.minecraft.item.ItemStack orb = new net.minecraft.item.ItemStack(
                        net.minecraft.init.Items.REDSTONE, Math.max(1, level));
                orb.setStackDisplayName("§c能量精华");
                net.minecraft.entity.item.EntityItem drop = new net.minecraft.entity.item.EntityItem(
                        killed.world,
                        killed.posX,
                        killed.posY,
                        killed.posZ,
                        orb);
                drop.setDefaultPickupDelay();
                killed.world.spawnEntity(drop);
            }
        }
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 战斗充能是产能模块，无消耗
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 总是可以执行
        return true;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setInteger("COMBAT_STREAK", 0);
        meta.setLong("LAST_KILL_TIME", 0);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("COMBAT_STREAK")) {
            meta.setInteger("COMBAT_STREAK", 0);
        }
        if (!meta.hasKey("LAST_KILL_TIME")) {
            meta.setLong("LAST_KILL_TIME", 0);
        }
        return true;
    }
}
