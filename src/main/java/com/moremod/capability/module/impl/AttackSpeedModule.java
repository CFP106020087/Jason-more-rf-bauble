package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.UUID;

/**
 * 攻击速度模块
 *
 * 功能：
 *  - 增加玩家攻击速度
 *  - 连击系统
 *  - Lv.1: 20% 攻速提升
 *  - Lv.2: 40% 攻速提升
 *  - Lv.3: 60% 攻速提升
 *
 * 能量消耗：
 *  - 被动消耗：5 * level RF/tick
 *  - 连击消耗：5 RF/次
 */
public class AttackSpeedModule extends AbstractMechCoreModule {

    public static final AttackSpeedModule INSTANCE = new AttackSpeedModule();

    // 属性修改器 UUID（固定，防止重复添加）
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("d8499b04-2222-4726-ab29-64469d734e0d");
    private static final String SPEED_MODIFIER_NAME = "MechCore Attack Speed";

    private AttackSpeedModule() {
        super(
            "ATTACK_SPEED",
            "攻击加速",
            "提升攻击速度和连击能力",
            3  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 激活时应用攻速加成
        applyAttackSpeed(player, newLevel);

        // 初始化元数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setBoolean("SPEED_APPLIED", true);
        meta.setLong("LAST_ATTACK", 0);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 停用时移除攻速加成
        removeAttackSpeed(player);

        // 清除元数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setBoolean("SPEED_APPLIED", false);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 确保攻速加成已应用（防止其他 mod 移除）
        IAttributeInstance attackSpeed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (attackSpeed != null && attackSpeed.getModifier(SPEED_MODIFIER_UUID) == null) {
            applyAttackSpeed(player, level);
            meta.setBoolean("SPEED_APPLIED", true);
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        if (newLevel > 0) {
            // 等级变化时更新攻速加成
            applyAttackSpeed(player, newLevel);
        } else {
            // 降级到 0 时移除攻速加成
            removeAttackSpeed(player);
        }
    }

    /**
     * 应用攻击速度加成
     */
    private void applyAttackSpeed(EntityPlayer player, int level) {
        IAttributeInstance attackSpeed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (attackSpeed == null) return;

        // 移除旧的修改器
        AttributeModifier oldModifier = attackSpeed.getModifier(SPEED_MODIFIER_UUID);
        if (oldModifier != null) {
            attackSpeed.removeModifier(oldModifier);
        }

        // 添加新的修改器（20%/40%/60%）
        double speedBonus = 0.2 * level;
        AttributeModifier newModifier = new AttributeModifier(
            SPEED_MODIFIER_UUID,
            SPEED_MODIFIER_NAME,
            speedBonus,
            2  // 操作类型：2=MULTIPLY_TOTAL（百分比加成）
        );

        attackSpeed.applyModifier(newModifier);
    }

    /**
     * 移除攻击速度加成
     */
    private void removeAttackSpeed(EntityPlayer player) {
        IAttributeInstance attackSpeed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (attackSpeed == null) return;

        AttributeModifier modifier = attackSpeed.getModifier(SPEED_MODIFIER_UUID);
        if (modifier != null) {
            attackSpeed.removeModifier(modifier);
        }
    }

    /**
     * 检查连击
     *
     * 此方法应该在 AttackEntityEvent 中调用
     *
     * @param player 玩家
     * @param data 机械核心数据
     */
    public void checkCombo(EntityPlayer player, IMechCoreData data) {
        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        long lastAttack = meta.getLong("LAST_ATTACK");
        long currentTime = player.world.getTotalWorldTime();

        // 连击窗口：40 tick (2秒)
        if (currentTime - lastAttack < 40) {
            // 连击消耗少量能量
            if (data.consumeEnergy(5)) {
                // 连击加成：减少攻击消耗
                player.addExhaustion(-0.5F);

                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.YELLOW + "⚔ 连击！"
                ), true);
            }
        }

        meta.setLong("LAST_ATTACK", currentTime);
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 每级 5 RF/tick
        return level * 5;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        return data.getEnergy() >= getPassiveEnergyCost(data.getModuleLevel(getModuleId()));
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setBoolean("SPEED_APPLIED", false);
        meta.setLong("LAST_ATTACK", 0);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("SPEED_APPLIED")) {
            meta.setBoolean("SPEED_APPLIED", false);
        }
        if (!meta.hasKey("LAST_ATTACK")) {
            meta.setLong("LAST_ATTACK", 0);
        }
        return true;
    }
}
