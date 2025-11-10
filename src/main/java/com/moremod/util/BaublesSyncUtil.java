package com.moremod.util;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;

/** 兼容不同 Baubles 版本的安全同步工具 */
public final class BaublesSyncUtil {
    private BaublesSyncUtil() {}

    /** 同步所有 baubles 槽位（尽量只在服务端调用） */
    public static void safeSyncAll(EntityPlayer player) {
        IBaublesItemHandler h = BaublesApi.getBaublesHandler(player);
        if (h == null) return;

        // 优先尝试：新接口的 setChanged(slot, boolean)
        try {
            Method setChanged = h.getClass().getMethod("setChanged", int.class, boolean.class);
            for (int i = 0; i < h.getSlots(); i++) {
                setChanged.invoke(h, i, true);
            }
            System.out.println("[BaublesSyncUtil] 使用 setChanged 方法同步");
            return;
        } catch (Throwable ignored) {
            System.out.println("[BaublesSyncUtil] setChanged 方法不可用，使用兜底方案");
        }

        // ✅ 修复：使用 copy() 创建新引用，强制触发变更检测
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack cur = h.getStackInSlot(i);
            if (!cur.isEmpty()) {
                // 关键修复：先清空，再设置副本
                h.setStackInSlot(i, ItemStack.EMPTY);
                h.setStackInSlot(i, cur.copy());  // ← 使用 copy()
                System.out.println("[BaublesSyncUtil] 同步槽位 " + i + ": " + cur.getDisplayName());
            }
        }
    }

    /** 同步单个槽位 */
    public static void safeSyncSlot(EntityPlayer player, int slot) {
        IBaublesItemHandler h = BaublesApi.getBaublesHandler(player);
        if (h == null) return;

        try {
            Method setChanged = h.getClass().getMethod("setChanged", int.class, boolean.class);
            setChanged.invoke(h, slot, true);
            System.out.println("[BaublesSyncUtil] 使用 setChanged 同步槽位 " + slot);
            return;
        } catch (Throwable ignored) {
            System.out.println("[BaublesSyncUtil] setChanged 方法不可用，使用兜底方案");
        }

        // ✅ 修复：同样使用 copy()
        ItemStack cur = h.getStackInSlot(slot);
        if (!cur.isEmpty()) {
            h.setStackInSlot(slot, ItemStack.EMPTY);
            h.setStackInSlot(slot, cur.copy());
            System.out.println("[BaublesSyncUtil] 同步槽位 " + slot + ": " + cur.getDisplayName());
        }
    }
}