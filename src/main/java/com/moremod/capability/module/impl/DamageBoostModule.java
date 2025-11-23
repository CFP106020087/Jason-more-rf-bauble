package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 伤害提升模块
 *
 * 功能：
 *  - 增加玩家造成的伤害
 *  - 暴击系统
 *  - Lv.1: 25% 伤害提升, 10% 暴击率
 *  - Lv.2: 50% 伤害提升, 20% 暴击率
 *  - Lv.3: 75% 伤害提升, 30% 暴击率
 *  - Lv.4: 100% 伤害提升, 40% 暴击率
 *  - Lv.5: 125% 伤害提升, 50% 暴击率
 *
 * 能量消耗：
 *  - 每次攻击：20 * level RF
 *  - 暴击额外消耗：10 RF
 */
public class DamageBoostModule extends AbstractMechCoreModule {

    public static final DamageBoostModule INSTANCE = new DamageBoostModule();

    private DamageBoostModule() {
        super(
            "DAMAGE_BOOST",
            "伤害强化",
            "提升攻击伤害并触发暴击",
            5  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 伤害提升模块无需激活逻辑
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 伤害提升模块无需停用逻辑
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        // 伤害提升模块不需要 tick 逻辑（纯事件驱动）
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时无需特殊处理
    }

    /**
     * 获取伤害倍率
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @return 伤害倍率（1.0 = 无加成）
     */
    public float getDamageMultiplier(EntityPlayer player, IMechCoreData data) {
        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return 1.0F;

        // 检查能量
        int energyCost = 20 * level;
        if (!data.consumeEnergy(energyCost)) {
            // 能量不足
            if (player.world.getTotalWorldTime() % 60 == 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚡ 伤害增幅能量不足"
                ), true);
            }
            return 1.0F;
        }

        // 伤害加成：25%/50%/75%/100%/125%
        return 1.0F + (0.25F * level);
    }

    /**
     * 应用暴击效果
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @param baseDamage 基础伤害
     * @return 应用暴击后的伤害
     */
    public float applyCritical(EntityPlayer player, IMechCoreData data, float baseDamage) {
        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return baseDamage;

        // 暴击几率：10%/20%/30%/40%/50%
        float critChance = 0.1F * level;

        if (player.getRNG().nextFloat() < critChance) {
            // 暴击消耗额外能量
            if (!data.consumeEnergy(10)) {
                return baseDamage; // 能量不足无法暴击
            }

            // 暴击伤害：2x
            float critDamage = baseDamage * 2.0F;

            // 暴击特效
            player.world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.CRIT_MAGIC,
                    player.posX, player.posY + player.getEyeHeight(), player.posZ,
                    0, 0, 0
            );

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GOLD + "⚔ 暴击！"
            ), true);

            return critDamage;
        }

        return baseDamage;
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 伤害提升无被动消耗（仅在攻击时消耗）
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        int level = data.getModuleLevel(getModuleId());
        return data.getEnergy() >= 20 * level;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        // 伤害提升模块无需元数据
        return new NBTTagCompound();
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        // 无需验证
        return true;
    }
}
