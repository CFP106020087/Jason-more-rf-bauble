package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

/**
 * 攻击范围扩展模块
 *
 * 功能：
 *  - 增加玩家攻击触及距离
 *  - Lv.1: +3 格
 *  - Lv.2: +6 格
 *  - Lv.3: +9 格
 *
 * 能量消耗：
 *  - 被动消耗：10 * level RF/tick
 */
public class RangeExtensionModule extends AbstractMechCoreModule {

    public static final RangeExtensionModule INSTANCE = new RangeExtensionModule();

    // 属性修改器 UUID（固定，防止重复添加）
    private static final UUID REACH_MODIFIER_UUID = UUID.fromString("d8499b04-3333-4726-ab29-64469d734e0d");
    private static final String REACH_MODIFIER_NAME = "MechCore Reach Extension";

    private RangeExtensionModule() {
        super(
            "RANGE_EXTENSION",
            "范围扩展",
            "增加攻击和交互距离",
            3  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 激活时应用触及距离加成
        applyReachExtension(player, newLevel);

        // 初始化元数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setBoolean("REACH_APPLIED", true);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 停用时移除触及距离加成
        removeReachExtension(player);

        // 清除元数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setBoolean("REACH_APPLIED", false);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 确保触及距离加成已应用（防止其他 mod 移除）
        IAttributeInstance reachDistance = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE);
        if (reachDistance != null && reachDistance.getModifier(REACH_MODIFIER_UUID) == null) {
            applyReachExtension(player, level);
            meta.setBoolean("REACH_APPLIED", true);
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        if (newLevel > 0) {
            // 等级变化时更新触及距离加成
            applyReachExtension(player, newLevel);
        } else {
            // 降级到 0 时移除触及距离加成
            removeReachExtension(player);
        }
    }

    /**
     * 应用触及距离加成
     */
    private void applyReachExtension(EntityPlayer player, int level) {
        IAttributeInstance reachDistance = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE);
        if (reachDistance == null) return;

        // 移除旧的修改器
        AttributeModifier oldModifier = reachDistance.getModifier(REACH_MODIFIER_UUID);
        if (oldModifier != null) {
            reachDistance.removeModifier(oldModifier);
        }

        // 添加新的修改器（+3/+6/+9 格）
        double reachBonus = 3.0 * level;
        AttributeModifier newModifier = new AttributeModifier(
            REACH_MODIFIER_UUID,
            REACH_MODIFIER_NAME,
            reachBonus,
            0  // 操作类型：0=ADD（直接加值）
        );

        reachDistance.applyModifier(newModifier);

        // 在玩家 NBT 中存储触及距离加成（用于其他系统检查）
        player.getEntityData().setDouble("MechanicalCoreExtendedReach", reachBonus);
    }

    /**
     * 移除触及距离加成
     */
    private void removeReachExtension(EntityPlayer player) {
        IAttributeInstance reachDistance = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE);
        if (reachDistance == null) return;

        AttributeModifier modifier = reachDistance.getModifier(REACH_MODIFIER_UUID);
        if (modifier != null) {
            reachDistance.removeModifier(modifier);
        }

        // 清除 NBT 数据
        player.getEntityData().removeTag("MechanicalCoreExtendedReach");
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 每级 10 RF/tick
        return level * 10;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        return data.getEnergy() >= getPassiveEnergyCost(data.getModuleLevel(getModuleId()));
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setBoolean("REACH_APPLIED", false);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("REACH_APPLIED")) {
            meta.setBoolean("REACH_APPLIED", false);
        }
        return true;
    }
}
