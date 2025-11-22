package com.moremod.synergy.effect;

import com.moremod.synergy.api.ISynergyEffect;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.UUID;

/**
 * 血液燃料税效果 - 降低最大生命值
 *
 * 机制：
 * - 激活时：永久降低2颗心(4点)最大生命值
 * - 停用时：恢复生命值，但扣除10级经验作为"灵魂修复代价"
 * - 死亡重生后效果重置，但XP惩罚依然存在
 */
public class BloodFuelTaxEffect implements ISynergyEffect {

    private static final UUID HEALTH_MODIFIER_UUID =
        UUID.fromString("a1b2c3d4-5678-90ab-cdef-fedcba098765");

    private static final double HEALTH_REDUCTION = -4.0; // -2颗心
    private static final int XP_COST_ON_DEACTIVATE = 10; // 停用时扣10级

    @Override
    public String getEffectId() {
        return "blood_fuel_tax";
    }

    /**
     * Synergy激活时调用 - 降低最大生命值
     */
    public void onSynergyActivate(EntityPlayer player, ItemStack core) {
        IAttributeInstance health = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (health == null) return;

        // 移除旧的修改器（如果存在）
        health.removeModifier(HEALTH_MODIFIER_UUID);

        // 添加新的生命值惩罚
        AttributeModifier modifier = new AttributeModifier(
            HEALTH_MODIFIER_UUID,
            "Tidal Overload Blood Tax",
            HEALTH_REDUCTION,
            0 // ADDITION操作
        );
        health.applyModifier(modifier);

        // 通知玩家
        player.sendMessage(new TextComponentString(
            TextFormatting.DARK_RED + "💀 雷云心脏：你的血液现在是燃料"
        ));
        player.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "最大生命值 -2❤ (停用时需支付 " + XP_COST_ON_DEACTIVATE + " 级经验恢复)"
        ));

        // 如果当前生命值超过新的最大值，调整
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * Synergy停用时调用 - 恢复生命值但扣除经验
     */
    public void onSynergyDeactivate(EntityPlayer player, ItemStack core) {
        IAttributeInstance health = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (health == null) return;

        // 移除生命值惩罚
        health.removeModifier(HEALTH_MODIFIER_UUID);

        // 扣除经验作为"灵魂修复代价"
        int currentLevel = player.experienceLevel;
        int newLevel = Math.max(0, currentLevel - XP_COST_ON_DEACTIVATE);
        int levelLost = currentLevel - newLevel;

        player.experienceLevel = newLevel;

        // 如果经验不足10级，额外扣除部分经验条
        if (levelLost < XP_COST_ON_DEACTIVATE) {
            player.experience = Math.max(0, player.experience - 0.5F);
        }

        // 通知玩家
        if (levelLost > 0) {
            player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "⚠ 灵魂修复完成 | 代价: -" + levelLost + " 级经验"
            ));
        } else {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "⚠ 经验不足！灵魂修复扣除了经验条"
            ));
        }

        player.sendMessage(new TextComponentString(
            TextFormatting.GREEN + "✓ 最大生命值已恢复"
        ));
    }

    /**
     * 玩家死亡时调用 - 清除修改器
     */
    public void onPlayerDeath(EntityPlayer player) {
        IAttributeInstance health = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(HEALTH_MODIFIER_UUID);
        }
    }

    /**
     * 玩家重生时调用 - 效果需要重新激活
     */
    public void onPlayerRespawn(EntityPlayer player) {
        // 死亡后效果自动清除，需要玩家重新激活Synergy
        player.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "雷云心脏效果已在死亡后重置"
        ));
    }
}
