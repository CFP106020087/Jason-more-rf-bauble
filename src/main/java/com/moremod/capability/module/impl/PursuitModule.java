package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 追击模块
 *
 * 功能：
 *  - 对同一目标连续攻击时造成额外伤害
 *  - 追击冲刺功能
 *  - Lv.1: 最大2层追击（+20%伤害）
 *  - Lv.2: 最大4层追击（+40%伤害）+ 冲刺
 *  - Lv.3: 最大6层追击（+60%伤害）+ 冲刺
 *
 * 能量消耗：
 *  - 标记目标：5 RF/次
 *  - 追击冲刺：50 RF/次
 */
public class PursuitModule extends AbstractMechCoreModule {

    public static final PursuitModule INSTANCE = new PursuitModule();

    private PursuitModule() {
        super(
            "PURSUIT",
            "追击系统",
            "对同一目标连续攻击造成额外伤害",
            3  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 初始化追击数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setString("PURSUIT_TARGET", "");
        meta.setInteger("PURSUIT_STACKS", 0);
        meta.setLong("LAST_PURSUIT", 0);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 清除追击标记
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setString("PURSUIT_TARGET", "");
        meta.setInteger("PURSUIT_STACKS", 0);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        long lastPursuit = meta.getLong("LAST_PURSUIT");
        long currentTime = player.world.getTotalWorldTime();

        // 清理过期的追击标记（1秒 = 20 ticks）
        if (currentTime - lastPursuit > 20) {
            if (!meta.getString("PURSUIT_TARGET").isEmpty()) {
                meta.setString("PURSUIT_TARGET", "");
                meta.setInteger("PURSUIT_STACKS", 0);
            }
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时重置追击
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setString("PURSUIT_TARGET", "");
        meta.setInteger("PURSUIT_STACKS", 0);
    }

    /**
     * 标记追击目标
     *
     * 此方法应该在 AttackEntityEvent 中调用
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @param target 目标实体
     */
    public void markTarget(EntityPlayer player, IMechCoreData data, Entity target) {
        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        // 标记目标消耗少量能量
        if (!data.consumeEnergy(5)) {
            return;
        }

        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        String currentTarget = meta.getString("PURSUIT_TARGET");
        String targetUUID = target.getUniqueID().toString();

        if (currentTarget.equals(targetUUID)) {
            // 增加追击层数
            int stacks = meta.getInteger("PURSUIT_STACKS");
            int maxStacks = level * 2; // 2/4/6
            stacks = Math.min(stacks + 1, maxStacks);
            meta.setInteger("PURSUIT_STACKS", stacks);

            if (stacks % 2 == 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "⚔ 追击层数: " + stacks
                ), true);
            }
        } else {
            // 新目标，重置层数
            meta.setString("PURSUIT_TARGET", targetUUID);
            meta.setInteger("PURSUIT_STACKS", 1);
        }

        meta.setLong("LAST_PURSUIT", player.world.getTotalWorldTime());
    }

    /**
     * 获取追击伤害加成
     *
     * 此方法应该在 LivingHurtEvent 中调用
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @param target 目标实体
     * @return 追击伤害加成比例（0.0 = 无加成，0.1 = +10%）
     */
    public float getPursuitDamage(EntityPlayer player, IMechCoreData data, Entity target) {
        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return 0;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        String targetUUID = target.getUniqueID().toString();
        String markedTarget = meta.getString("PURSUIT_TARGET");

        if (!targetUUID.equals(markedTarget)) return 0;

        // 检查追击是否过期（1秒）
        long lastPursuit = meta.getLong("LAST_PURSUIT");
        if (player.world.getTotalWorldTime() - lastPursuit > 20) {
            // 追击过期，清除标记
            meta.setString("PURSUIT_TARGET", "");
            meta.setInteger("PURSUIT_STACKS", 0);
            return 0;
        }

        int stacks = meta.getInteger("PURSUIT_STACKS");
        // 每层10%额外伤害
        return stacks * 0.1F;
    }

    /**
     * 追击冲刺
     *
     * 此方法应该在玩家主动触发时调用（如潜行攻击）
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @param target 目标实体
     */
    public void dashToTarget(EntityPlayer player, IMechCoreData data, Entity target) {
        int level = data.getModuleLevel(getModuleId());
        if (level < 2) return; // 2级以上才有冲刺

        // 冲刺需要额外能量
        if (!data.consumeEnergy(50)) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "⚡ 能量不足，无法追击冲刺"
            ), true);
            return;
        }

        double distance = player.getDistance(target);
        if (distance > 2 && distance < 8) {
            // 向目标冲刺
            Vec3d direction = target.getPositionVector().subtract(player.getPositionVector()).normalize();
            player.addVelocity(direction.x * 0.8, 0.2, direction.z * 0.8);

            // 粒子轨迹
            for (int i = 0; i < 10; i++) {
                player.world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.CRIT,
                        player.posX, player.posY + 1, player.posZ,
                        direction.x * 0.1, 0, direction.z * 0.1
                );
            }

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "⚡ 追击冲刺！"
            ), true);
        }
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 追击模块无被动消耗（仅在使用时消耗）
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 只要有能量，就可以执行
        return data.getEnergy() >= 5;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setString("PURSUIT_TARGET", "");
        meta.setInteger("PURSUIT_STACKS", 0);
        meta.setLong("LAST_PURSUIT", 0);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("PURSUIT_TARGET")) {
            meta.setString("PURSUIT_TARGET", "");
        }
        if (!meta.hasKey("PURSUIT_STACKS")) {
            meta.setInteger("PURSUIT_STACKS", 0);
        }
        if (!meta.hasKey("LAST_PURSUIT")) {
            meta.setLong("LAST_PURSUIT", 0);
        }
        return true;
    }
}
