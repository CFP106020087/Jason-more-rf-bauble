package com.moremod.accessorybox.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

/**
 * CraftTweaker 兼容性辅助类
 * 用于检查 CraftTweaker 是否已加载并安全地调用 CrT 事件
 */
public class AccessoryBoxCrTCompat {

    private static Boolean craftTweakerLoaded = null;

    /**
     * 检查 CraftTweaker 是否已加载（带缓存）
     */
    public static boolean isCraftTweakerLoaded() {
        if (craftTweakerLoaded == null) {
            craftTweakerLoaded = Loader.isModLoaded("crafttweaker");
        }
        return craftTweakerLoaded;
    }

    /**
     * 触发装备前事件
     * @param player 玩家
     * @param slot 槽位索引
     * @param stack 物品
     * @return true 如果事件被取消
     */
    public static boolean fireEquipPre(EntityPlayer player, int slot, ItemStack stack) {
        if (!isCraftTweakerLoaded()) {
            return false;
        }
        try {
            return AccessoryBoxCrTEventHandler.handleEquipPre(player, slot, stack);
        } catch (Throwable e) {
            // 静默处理，避免崩溃
            return false;
        }
    }

    /**
     * 触发装备后事件
     * @param player 玩家
     * @param slot 槽位索引
     * @param stack 物品
     */
    public static void fireEquipPost(EntityPlayer player, int slot, ItemStack stack) {
        if (!isCraftTweakerLoaded()) {
            return;
        }
        try {
            AccessoryBoxCrTEventHandler.handleEquipPost(player, slot, stack);
        } catch (Throwable e) {
            // 静默处理
        }
    }

    /**
     * 触发卸下前事件
     * @param player 玩家
     * @param slot 槽位索引
     * @param stack 物品
     * @return true 如果事件被取消
     */
    public static boolean fireUnequipPre(EntityPlayer player, int slot, ItemStack stack) {
        if (!isCraftTweakerLoaded()) {
            return false;
        }
        try {
            return AccessoryBoxCrTEventHandler.handleUnequipPre(player, slot, stack);
        } catch (Throwable e) {
            // 静默处理
            return false;
        }
    }

    /**
     * 触发卸下后事件
     * @param player 玩家
     * @param slot 槽位索引
     * @param stack 物品
     */
    public static void fireUnequipPost(EntityPlayer player, int slot, ItemStack stack) {
        if (!isCraftTweakerLoaded()) {
            return;
        }
        try {
            AccessoryBoxCrTEventHandler.handleUnequipPost(player, slot, stack);
        } catch (Throwable e) {
            // 静默处理
        }
    }

    /**
     * 触发佩戴 Tick 事件
     * @param player 玩家
     * @param slot 槽位索引
     * @param stack 物品
     */
    public static void fireWearingTick(EntityPlayer player, int slot, ItemStack stack) {
        if (!isCraftTweakerLoaded()) {
            return;
        }
        try {
            AccessoryBoxCrTEventHandler.handleWearingTick(player, slot, stack);
        } catch (Throwable e) {
            // 静默处理
        }
    }
}