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
 * 移动速度模块
 *
 * 功能：
 *  - 增加玩家移动速度
 *  - Lv.1: 20% 移速提升
 *  - Lv.2: 40% 移速提升
 *  - Lv.3: 60% 移速提升
 *  - Lv.4: 80% 移速提升
 *  - Lv.5: 100% 移速提升
 *
 * 能量消耗：
 *  - 被动消耗：8 * level RF/tick
 */
public class MovementSpeedModule extends AbstractMechCoreModule {

    public static final MovementSpeedModule INSTANCE = new MovementSpeedModule();

    // 属性修改器 UUID（固定，防止重复添加）
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("d8499b04-0e66-4726-ab29-64469d734e0d");
    private static final String SPEED_MODIFIER_NAME = "MechCore Movement Speed";

    private MovementSpeedModule() {
        super(
            "MOVEMENT_SPEED",
            "移动加速",
            "提升移动速度",
            5  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 激活时应用速度加成
        applyMovementSpeed(player, newLevel);

        // 初始化元数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setBoolean("SPEED_APPLIED", true);

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.AQUA + "⚡ 移动加速已激活"
        ), true);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 停用时移除速度加成
        removeMovementSpeed(player);

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

        // 确保速度加成已应用（防止其他 mod 移除）
        IAttributeInstance movementSpeed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (movementSpeed != null && movementSpeed.getModifier(SPEED_MODIFIER_UUID) == null) {
            applyMovementSpeed(player, level);
            meta.setBoolean("SPEED_APPLIED", true);
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        if (newLevel > 0) {
            // 等级变化时更新速度加成
            applyMovementSpeed(player, newLevel);
        } else {
            // 降级到 0 时移除速度加成
            removeMovementSpeed(player);
        }
    }

    /**
     * 应用移动速度加成
     */
    private void applyMovementSpeed(EntityPlayer player, int level) {
        IAttributeInstance movementSpeed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (movementSpeed == null) return;

        // 移除旧的修改器
        AttributeModifier oldModifier = movementSpeed.getModifier(SPEED_MODIFIER_UUID);
        if (oldModifier != null) {
            movementSpeed.removeModifier(oldModifier);
        }

        // 添加新的修改器（20%/40%/60%/80%/100%）
        double speedBonus = 0.2 * level;
        AttributeModifier newModifier = new AttributeModifier(
            SPEED_MODIFIER_UUID,
            SPEED_MODIFIER_NAME,
            speedBonus,
            2  // 操作类型：2=MULTIPLY_TOTAL（百分比加成）
        );

        movementSpeed.applyModifier(newModifier);

        // 在玩家 NBT 中存储速度加成（用于其他系统检查）
        player.getEntityData().setBoolean("MechanicalCoreSpeedApplied", true);
        player.getEntityData().setInteger("MechanicalCoreSpeedLevel", level);
    }

    /**
     * 移除移动速度加成
     */
    private void removeMovementSpeed(EntityPlayer player) {
        IAttributeInstance movementSpeed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (movementSpeed == null) return;

        AttributeModifier modifier = movementSpeed.getModifier(SPEED_MODIFIER_UUID);
        if (modifier != null) {
            movementSpeed.removeModifier(modifier);
        }

        // 清除 NBT 数据
        player.getEntityData().removeTag("MechanicalCoreSpeedApplied");
        player.getEntityData().removeTag("MechanicalCoreSpeedLevel");
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 每级 8 RF/tick
        return level * 8;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        return data.getEnergy() >= getPassiveEnergyCost(data.getModuleLevel(getModuleId()));
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setBoolean("SPEED_APPLIED", false);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("SPEED_APPLIED")) {
            meta.setBoolean("SPEED_APPLIED", false);
        }
        return true;
    }
}
