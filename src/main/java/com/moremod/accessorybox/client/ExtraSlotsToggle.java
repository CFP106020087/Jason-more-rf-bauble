package com.moremod.accessorybox.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 客户端会话内的 EX 可见性状态 */
@SideOnly(Side.CLIENT)
public final class ExtraSlotsToggle {
    private static boolean visible = true; // 默认开启；需要默认关闭就改为 false

    private ExtraSlotsToggle() {}

    public static boolean isVisible() {
        return visible;
    }

    public static void setVisible(boolean v) {
        visible = v;
    }

    public static void toggle() {
        visible = !visible;
    }
}
