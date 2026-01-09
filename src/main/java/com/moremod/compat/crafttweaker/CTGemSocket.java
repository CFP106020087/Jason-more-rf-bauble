package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker 宝石镶嵌配置接口
 *
 * 用法:
 * mods.moremod.GemSocket.setMaxSockets(4);  // 设置最大镶嵌数 (1-6)
 * mods.moremod.GemSocket.getMaxSockets();   // 获取当前最大镶嵌数
 */
@ZenRegister
@ZenClass("mods.moremod.GemSocket")
public class CTGemSocket {

    /**
     * 设置最大镶嵌数量
     * @param max 最大数量 (1-6)
     */
    @ZenMethod
    public static void setMaxSockets(int max) {
        GemSocketHelper.setMaxSockets(max);
    }

    /**
     * 获取当前最大镶嵌数量
     */
    @ZenMethod
    public static int getMaxSockets() {
        return GemSocketHelper.getMaxSockets();
    }
}
