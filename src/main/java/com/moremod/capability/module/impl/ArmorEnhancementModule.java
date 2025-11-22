package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

/**
 * 护甲强化模块
 *
 * 功能：
 *  - 增加玩家护甲值
 *  - Lv.1~5: 每级 +2 护甲点数
 *
 * 能量消耗：
 *  - 每级 5 RF/tick
 */
public class ArmorEnhancementModule extends AbstractMechCoreModule {

    public static final ArmorEnhancementModule INSTANCE = new ArmorEnhancementModule();

    // 属性修改器 UUID（固定，防止重复添加）
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("a8f9e5b2-1c3d-4e6f-8a9b-0c1d2e3f4a5b");
    private static final String ARMOR_MODIFIER_NAME = "MechCore Armor Enhancement";

    private ArmorEnhancementModule() {
        super(
            "ARMOR_ENHANCEMENT",
            "护甲强化",
            "增加玩家的护甲防御值",
            5  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 激活时应用护甲加成
        applyArmorBonus(player, newLevel);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 停用时移除护甲加成
        removeArmorBonus(player);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        // 确保护甲加成已应用（防止其他 mod 移除）
        IAttributeInstance armor = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armor != null && armor.getModifier(ARMOR_MODIFIER_UUID) == null) {
            applyArmorBonus(player, level);
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        if (newLevel > 0) {
            // 等级变化时更新护甲加成
            applyArmorBonus(player, newLevel);
        } else {
            // 降级到 0 时移除护甲加成
            removeArmorBonus(player);
        }
    }

    /**
     * 应用护甲加成
     */
    private void applyArmorBonus(EntityPlayer player, int level) {
        IAttributeInstance armor = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armor == null) return;

        // 移除旧的修改器
        AttributeModifier oldModifier = armor.getModifier(ARMOR_MODIFIER_UUID);
        if (oldModifier != null) {
            armor.removeModifier(oldModifier);
        }

        // 添加新的修改器（每级 +2 护甲）
        double armorBonus = level * 2.0;
        AttributeModifier newModifier = new AttributeModifier(
            ARMOR_MODIFIER_UUID,
            ARMOR_MODIFIER_NAME,
            armorBonus,
            0  // 操作类型：0=ADD（直接加值）
        );

        armor.applyModifier(newModifier);
    }

    /**
     * 移除护甲加成
     */
    private void removeArmorBonus(EntityPlayer player) {
        IAttributeInstance armor = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armor == null) return;

        AttributeModifier modifier = armor.getModifier(ARMOR_MODIFIER_UUID);
        if (modifier != null) {
            armor.removeModifier(modifier);
        }
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
        return new NBTTagCompound();
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        return true;
    }
}
