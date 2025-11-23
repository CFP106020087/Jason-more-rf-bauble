package com.moremod.upgrades;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.eventHandler.EventHandlerJetpack;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.UUID;

/**
 * 升级效果应用系统
 * 处理所有升级组件的具体效果
 */
public class UpgradeEffectManager {

    /**
     * 应用所有升级效果到玩家
     */
    public static void applyAllEffects(EntityPlayer player, ItemStack coreStack) {
        if (!ItemMechanicalCore.isMechanicalCore(coreStack)) {
            return;
        }

        // ✅ Set player context for all upgrade reads
        ItemMechanicalCore.setPlayerContext(player);
        try {
            // 应用各种升级效果
            // 能量容量效果已经在其他地方处理（如核心本身的容量计算）
            // 不需要额外的tick效果

            // 能量效率使用新的系统
            applyEnergyEfficiencyEffect(player, coreStack);

            // 护甲强化现在通过事件处理，这里只需要显示状态
            // 实际减伤在 ArmorEnhancementEventHandler 中处理

            applySpeedBoostEffect(player, coreStack);
            applyRegenerationEffect(player, coreStack);

            // 飞行模块使用新的处理器
            applyFlightModuleEffect(player, coreStack);

            applyShieldGeneratorEffect(player, coreStack);

            // 温度控制效果
            com.moremod.upgrades.TemperatureControlEffect.applyTemperatureControl(player, coreStack);
            WaterproofUpgrade.applyWaterproofEffect(player, coreStack);
        } finally {
            // ✅ Clear player context
            ItemMechanicalCore.clearPlayerContext();
        }
    }

    /**
     * 能量效率效果 - 集成新的能量效率管理器
     */
    private static void applyEnergyEfficiencyEffect(EntityPlayer player, ItemStack coreStack) {
        // 使用新的能量效率管理器来处理效果
        EnergyEfficiencyManager.applyPassiveEffects(player, coreStack);
    }

    /**
     * 速度提升效果
     */
    private static void applySpeedBoostEffect(EntityPlayer player, ItemStack coreStack) {
        int level = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.SPEED_BOOST);

        if (level > 0) {
            // 基础速度提升
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 100, level - 1, true, false));

            // 高等级提供跳跃提升
            if (level >= 2) {
                player.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 100, level - 2, true, false));
            }

            // 最高等级提供急迫效果
            if (level >= 3) {
                player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 100, 0, true, false));
            }
        }
    }

    /**
     * 生命恢复效果
     */
    private static void applyRegenerationEffect(EntityPlayer player, ItemStack coreStack) {
        int level = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.REGENERATION);

        if (level > 0) {
            // 生命恢复效果
            int interval = Math.max(60 - level * 15, 20);
            if (player.world.getTotalWorldTime() % interval == 0) {
                if (player.getHealth() < player.getMaxHealth()) {
                    player.heal(0.5F * level);
                }
            }

            // 高等级提供再生效果
            if (level >= 2) {
                player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 100, level - 2, true, false));
            }

            // 最高等级提供生命提升
            if (level >= 3) {
                player.addPotionEffect(new PotionEffect(MobEffects.HEALTH_BOOST, 100, (level - 3) * 2, true, false));
            }
        }
    }

    /**
     * 飞行模块效果 - 已在 EventHandlerJetpack 中处理
     */
    private static void applyFlightModuleEffect(EntityPlayer player, ItemStack coreStack) {
        // 飞行效果现在完全在 EventHandlerJetpack.onPlayerTick 中处理
        // 这里不需要做任何事情
    }

    /**
     * 护盾发生器效果
     */
    private static void applyShieldGeneratorEffect(EntityPlayer player, ItemStack coreStack) {
        int level = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.SHIELD_GENERATOR);

        if (level > 0) {
            // 定期刷新护盾
            int interval = Math.max(1200 - level * 300, 300);
            if (player.world.getTotalWorldTime() % interval == 0) {
                int absorptionLevel = level - 1;
                int duration = 600 + level * 300;

                player.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, duration, absorptionLevel, true, false));

                player.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "⚡ 能量护盾已充能完毕"
                ));
            }

            // 高等级在受伤时触发紧急护盾
            if (level >= 2 && player.getHealth() <= player.getMaxHealth() * 0.3F) {
                if (player.world.getTotalWorldTime() % 1200 == 0) {
                    player.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 600, level, true, false));
                    // 不再给抗性效果，因为护甲强化已经提供减伤

                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "⚠ " + TextFormatting.GOLD + "紧急护盾启动！"
                    ));
                }
            }
        }
    }

    /**
     * 升级成功时的特效和消息
     */
    public static void playUpgradeEffect(EntityPlayer player, ItemMechanicalCore.UpgradeType type, int newLevel) {
        // 播放音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F + newLevel * 0.1F);

        // 发送升级消息
        TextFormatting color = getLevelColor(newLevel, getMaxLevel(type));
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ " + type.getColor() + type.getDisplayName() +
                        TextFormatting.WHITE + " 升级至 " + color + "Lv." + newLevel
        ));

        // 特殊升级的额外消息
        if (type == ItemMechanicalCore.UpgradeType.ENERGY_CAPACITY && newLevel > 0) {
            int capacity = 100000 + (newLevel * 50000);
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "⚡ 能量容量提升至 " + capacity + " RF！"
            ));
        }

        if (type == ItemMechanicalCore.UpgradeType.ENERGY_EFFICIENCY && newLevel > 0) {
            int percentage = (int)((1.0 - EnergyEfficiencyManager.getEfficiencyMultiplier(player)) * 100);
            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "⚡ 能量消耗减少 " + percentage + "%！"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "所有RF/FE设备的能量消耗都将获得此加成"
            ));
        }

        if (type == ItemMechanicalCore.UpgradeType.ARMOR_ENHANCEMENT && newLevel > 0) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.BLUE + "🛡 护甲强化等级 " + newLevel + " - 基础减伤: " + (newLevel * 10) + "%"
            ));
            if (newLevel >= 2) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "✦ 解锁特殊防护：火焰/爆炸抗性"
                ));
            }
            if (newLevel >= 3) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "✦ 解锁反击能力"
                ));
            }
            if (newLevel >= 5) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + "✦✦ 解锁致命伤害保护！"
                ));
            }
        }

        if (type == ItemMechanicalCore.UpgradeType.FLIGHT_MODULE && newLevel == 1) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "✦ 恭喜！你已解锁飞行能力！"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "使用 V 键开关飞行，H 键切换悬停模式"
            ));
        }

        if (newLevel == getMaxLevel(type)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "⭐ " + type.getDisplayName() + " 已达到最大等级！"
            ));
        }
    }

    /**
     * 获取升级等级的颜色显示
     */
    public static TextFormatting getLevelColor(int level, int maxLevel) {
        if (level == 0) return TextFormatting.GRAY;
        if (level < maxLevel * 0.3) return TextFormatting.WHITE;
        if (level < maxLevel * 0.6) return TextFormatting.GREEN;
        if (level < maxLevel * 0.9) return TextFormatting.BLUE;
        return TextFormatting.GOLD;
    }

    public static int getMaxLevel(ItemMechanicalCore.UpgradeType type) {
        switch (type) {
            case ENERGY_CAPACITY: return 10;
            case ENERGY_EFFICIENCY: return 5;
            case ARMOR_ENHANCEMENT: return 5;
            case SPEED_BOOST: return 3;
            case REGENERATION: return 3;
            case FLIGHT_MODULE: return 3;  // 改为3级（基础/高级/终极）
            case SHIELD_GENERATOR: return 3;
            case TEMPERATURE_CONTROL: return 5;
            default: return 5;
        }
    }
}