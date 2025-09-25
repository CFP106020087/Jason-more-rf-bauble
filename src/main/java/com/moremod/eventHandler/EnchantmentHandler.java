package com.moremod.eventHandler;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.upgrades.auxiliary.AuxiliaryUpgradeManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 附魔增强事件处理器
 * 处理经验增幅升级对附魔台的加成
 * 等级1: +5级
 * 等级2: +10级
 * 等级3: +15级
 */
public class EnchantmentHandler {

    @SubscribeEvent
    public static void onEnchantmentLevelSet(EnchantmentLevelSetEvent event) {
        // 找到近距离玩家
        EntityPlayer player = event.getWorld().getClosestPlayer(
                event.getPos().getX() + 0.5,
                event.getPos().getY() + 0.5,
                event.getPos().getZ() + 0.5,
                5.0,
                false
        );
        if (player == null) return;

        // 核心与升级等级
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;

        int expLevel = ItemMechanicalCore.getUpgradeLevel(core, "EXP_AMPLIFIER");
        if (expLevel <= 0) return;

        // 附魔加成
        int bonus = AuxiliaryUpgradeManager.ExpAmplifierSystem.getEnchantmentBonus(player);
        if (bonus <= 0) return;

        int originalLevel = event.getLevel();
        int newLevel = originalLevel + bonus;
        event.setLevel(newLevel);

        // 只在跨 30 级时提示
        if (originalLevel <= 30 && newLevel > 30) {
            String roman = expLevel == 1 ? "I" : expLevel == 2 ? "II" : "III";
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "⚡ 经验矩阵 " + roman + " 激活！" +
                            TextFormatting.GOLD + " 附魔等级: " + newLevel +
                            TextFormatting.YELLOW + " (+" + bonus + ")"
            ), true);
        }

        System.out.println("[moremod] 附魔等级提升: " + originalLevel + " -> " + newLevel + " (+" + bonus + ")");
    }
}
