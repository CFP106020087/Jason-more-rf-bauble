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
            return;
        } catch (Throwable ignored) {}

        // 兜底：把同一个栈再 set 回去，触发变更监听 -> 同步
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack cur = h.getStackInSlot(i);
            h.setStackInSlot(i, cur);
        }
    }

    /** 同步单个槽位 */
    public static void safeSyncSlot(EntityPlayer player, int slot) {
        IBaublesItemHandler h = BaublesApi.getBaublesHandler(player);
        if (h == null) return;

        try {
            Method setChanged = h.getClass().getMethod("setChanged", int.class, boolean.class);
            setChanged.invoke(h, slot, true);
            return;
        } catch (Throwable ignored) {}

        ItemStack cur = h.getStackInSlot(slot);
        h.setStackInSlot(slot, cur);
    }
}
