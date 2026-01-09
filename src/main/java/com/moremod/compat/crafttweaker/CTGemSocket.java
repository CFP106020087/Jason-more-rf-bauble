package com.moremod.compat.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker 宝石配置接口
 *
 * 用法:
 * mods.moremod.GemSocket.setMaxSockets(4);  // 设置最大镶嵌数 (1-6)
 * mods.moremod.GemSocket.getMaxSockets();   // 获取当前最大镶嵌数
 * mods.moremod.GemSocket.setMaxAffixes(4);  // 设置单个宝石最大词条数 (1-6)
 * mods.moremod.GemSocket.getMaxAffixes();   // 获取当前最大词条数
 */
@ZenRegister
@ZenClass("mods.moremod.GemSocket")
public class CTGemSocket {

    // ==================== 镶嵌数量配置 ====================

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

    // ==================== 词条数量配置 ====================

    /**
     * 设置单个宝石最大词条数量
     * @param max 最大数量 (1-6)
     */
    @ZenMethod
    public static void setMaxAffixes(int max) {
        GemLootRuleManager.setMaxAffixes(max);
    }

    /**
     * 获取当前单个宝石最大词条数量
     */
    @ZenMethod
    public static int getMaxAffixes() {
        return GemLootRuleManager.getMaxAffixes();
    }
}
