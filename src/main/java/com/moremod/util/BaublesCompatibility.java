package com.moremod.util;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

/**
 * Baubles兼容性工具类
 * 处理不同版本的Baubles API差异
 */
public class BaublesCompatibility {

    private static final boolean BAUBLES_LOADED = Loader.isModLoaded("baubles");

    /**
     * 检查Baubles是否已加载
     */
    public static boolean isBaublesLoaded() {
        return BAUBLES_LOADED;
    }

    /**
     * 安全获取Baubles处理器
     */
    public static IBaublesItemHandler getBaublesHandler(EntityPlayer player) {
        if (!BAUBLES_LOADED) {
            return null;
        }

        try {
            return BaublesApi.getBaublesHandler(player);
        } catch (Exception e) {
            System.err.println("[MechanicalCore] 获取Baubles处理器失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 安全获取指定槽位的物品
     */
    public static ItemStack getBaubleItem(EntityPlayer player, int slot) {
        IBaublesItemHandler handler = getBaublesHandler(player);
        if (handler == null || slot < 0 || slot >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }

        try {
            return handler.getStackInSlot(slot);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    /**
     * 安全设置指定槽位的物品
     */
    public static boolean setBaubleItem(EntityPlayer player, int slot, ItemStack stack) {
        IBaublesItemHandler handler = getBaublesHandler(player);
        if (handler == null || slot < 0 || slot >= handler.getSlots()) {
            return false;
        }

        try {
            handler.setStackInSlot(slot, stack);
            return true;
        } catch (Exception e) {
            System.err.println("[MechanicalCore] 设置Baubles物品失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 寻找指定类型的饰品
     */
    public static ItemStack findBaubleOfType(EntityPlayer player, Class<?> itemClass) {
        IBaublesItemHandler handler = getBaublesHandler(player);
        if (handler == null) {
            return ItemStack.EMPTY;
        }

        try {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) {
                    return stack;
                }
            }
        } catch (Exception e) {
            System.err.println("[MechanicalCore] 查找Baubles物品失败: " + e.getMessage());
        }

        return ItemStack.EMPTY;
    }

    /**
     * 寻找空的Baubles槽位
     */
    public static int findEmptyBaubleSlot(EntityPlayer player, ItemStack itemToEquip) {
        IBaublesItemHandler handler = getBaublesHandler(player);
        if (handler == null) {
            return -1;
        }

        try {
            for (int i = 0; i < handler.getSlots(); i++) {
                if (handler.getStackInSlot(i).isEmpty() &&
                        handler.isItemValidForSlot(i, itemToEquip, player)) {
                    return i;
                }
            }
        } catch (Exception e) {
            System.err.println("[MechanicalCore] 查找空槽位失败: " + e.getMessage());
        }

        return -1;
    }

    /**
     * 尝试自动装备饰品
     */
    public static boolean tryEquipBauble(EntityPlayer player, ItemStack stack) {
        if (!BAUBLES_LOADED || stack.isEmpty()) {
            return false;
        }

        int emptySlot = findEmptyBaubleSlot(player, stack);
        if (emptySlot >= 0) {
            return setBaubleItem(player, emptySlot, stack.copy());
        }

        return false;
    }

    /**
     * 获取所有装备的饰品
     */
    public static ItemStack[] getAllEquippedBaubles(EntityPlayer player) {
        IBaublesItemHandler handler = getBaublesHandler(player);
        if (handler == null) {
            return new ItemStack[0];
        }

        try {
            ItemStack[] baubles = new ItemStack[handler.getSlots()];
            for (int i = 0; i < handler.getSlots(); i++) {
                baubles[i] = handler.getStackInSlot(i);
                if (baubles[i] == null) {
                    baubles[i] = ItemStack.EMPTY;
                }
            }
            return baubles;
        } catch (Exception e) {
            System.err.println("[MechanicalCore] 获取所有饰品失败: " + e.getMessage());
            return new ItemStack[0];
        }
    }

    /**
     * 检查玩家是否装备了指定物品
     */
    public static boolean hasEquippedBauble(EntityPlayer player, Class<?> itemClass) {
        return !findBaubleOfType(player, itemClass).isEmpty();
    }

    /**
     * 安全地同步Baubles到客户端
     */
    public static void syncBaubles(EntityPlayer player) {
        if (!BAUBLES_LOADED || player.world.isRemote) {
            return;
        }

        try {
            IBaublesItemHandler handler = getBaublesHandler(player);
            if (handler != null) {
                // 1.12.2的Baubles通常会自动同步，这里可以添加手动同步逻辑
                // 具体的同步方法可能因Baubles版本而异
            }
        } catch (Exception e) {
            System.err.println("[MechanicalCore] 同步Baubles失败: " + e.getMessage());
        }
    }
}