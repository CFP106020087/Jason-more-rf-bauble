package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 魔法吸收模块
 *
 * 功能：
 *  - 吸收魔法伤害并转化为能量
 *  - Lv.1: 吸收 30% 魔法伤害，每点伤害转化 20 RF
 *  - Lv.2: 吸收 50% 魔法伤害，每点伤害转化 30 RF
 *  - Lv.3: 吸收 70% 魔法伤害，每点伤害转化 40 RF
 *
 * 能量消耗：
 *  - 无被动消耗（仅在吸收时产能）
 *
 * 注意：
 *  - 魔法伤害包括：药水效果、魔法弹、间接魔法攻击等
 *  - 实际的伤害吸收逻辑需要在 LivingHurtEvent 中实现
 */
public class MagicAbsorbModule extends AbstractMechCoreModule {

    public static final MagicAbsorbModule INSTANCE = new MagicAbsorbModule();

    private MagicAbsorbModule() {
        super(
            "MAGIC_ABSORB",
            "魔法吸收",
            "吸收魔法伤害转化为能量",
            3  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 初始化元数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setLong("TOTAL_ABSORBED", 0);
        meta.setLong("TOTAL_ENERGY_GAINED", 0);

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.AQUA + "⚡ 魔法吸收模块已安装 (等级 " + newLevel + ")"
        ), true);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 显示统计信息
        long totalAbsorbed = meta.getLong("TOTAL_ABSORBED");
        long totalEnergyGained = meta.getLong("TOTAL_ENERGY_GAINED");

        if (totalAbsorbed > 0) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "魔法吸收统计: 吸收 " + totalAbsorbed + " 点伤害，获得 " + totalEnergyGained + " RF"
            ), false);
        }
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        // 魔法吸收无被动 tick 效果
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时提示
        if (newLevel > 0) {
            float absorption = getAbsorptionRate(newLevel);
            int energyRate = getEnergyPerDamage(newLevel);

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.AQUA + "魔法吸收已升级: " +
                            TextFormatting.YELLOW + (int)(absorption * 100) + "% 吸收, " +
                            TextFormatting.GREEN + energyRate + " RF/点伤害"
            ), true);
        }
    }

    /**
     * 吸收魔法伤害
     *
     * 此方法应该在 LivingHurtEvent 中调用
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @param magicDamage 魔法伤害值
     * @return 吸收后的剩余伤害
     */
    public float absorbMagicDamage(EntityPlayer player, IMechCoreData data, float magicDamage) {
        int level = data.getModuleLevel(getModuleId());
        if (level <= 0 || magicDamage <= 0) {
            return magicDamage;
        }

        // 计算吸收率和能量转化率
        float absorptionRate = getAbsorptionRate(level);
        int energyPerDamage = getEnergyPerDamage(level);

        // 计算吸收的伤害
        float absorbedDamage = magicDamage * absorptionRate;
        float remainingDamage = magicDamage - absorbedDamage;

        // 转化为能量
        int energyGained = (int) (absorbedDamage * energyPerDamage);
        if (energyGained > 0) {
            data.addEnergy(energyGained);

            // 更新统计
            NBTTagCompound meta = data.getModuleMeta(getModuleId());
            meta.setLong("TOTAL_ABSORBED", meta.getLong("TOTAL_ABSORBED") + (long) absorbedDamage);
            meta.setLong("TOTAL_ENERGY_GAINED", meta.getLong("TOTAL_ENERGY_GAINED") + energyGained);

            // 显示吸收效果（降低频率）
            if (player.world.rand.nextInt(3) == 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "✨ 吸收魔法 +" + energyGained + " RF"
                ), true);
            }
        }

        return remainingDamage;
    }

    /**
     * 获取魔法伤害吸收率
     */
    public float getAbsorptionRate(int level) {
        switch (level) {
            case 1:
                return 0.3F;  // 30%
            case 2:
                return 0.5F;  // 50%
            case 3:
                return 0.7F;  // 70%
            default:
                return 0.0F;
        }
    }

    /**
     * 获取每点伤害转化的能量
     */
    public int getEnergyPerDamage(int level) {
        switch (level) {
            case 1:
                return 20;  // 20 RF/伤害
            case 2:
                return 30;  // 30 RF/伤害
            case 3:
                return 40;  // 40 RF/伤害
            default:
                return 0;
        }
    }

    /**
     * 检查伤害源是否为魔法伤害
     */
    public static boolean isMagicDamage(net.minecraft.util.DamageSource source) {
        // 魔法伤害通常标记为 isMagicDamage() 或特定类型
        if (source.isMagicDamage()) {
            return true;
        }

        // 检查特定的魔法伤害源
        String damageType = source.getDamageType();
        return damageType.contains("magic") ||
                damageType.contains("potion") ||
                damageType.contains("thorns") ||
                damageType.contains("indirectMagic") ||
                damageType.contains("witchcraft");
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 魔法吸收无被动消耗（吸收时产能）
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
        meta.setLong("TOTAL_ABSORBED", 0);
        meta.setLong("TOTAL_ENERGY_GAINED", 0);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("TOTAL_ABSORBED")) {
            meta.setLong("TOTAL_ABSORBED", 0);
        }
        if (!meta.hasKey("TOTAL_ENERGY_GAINED")) {
            meta.setLong("TOTAL_ENERGY_GAINED", 0);
        }
        return true;
    }
}
