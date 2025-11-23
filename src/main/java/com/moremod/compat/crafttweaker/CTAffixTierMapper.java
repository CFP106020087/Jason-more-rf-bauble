package com.moremod.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * CraftTweaker接口 - Tier系统配置
 * 
 * 使用方法：
 * mods.moremod.AffixTier.enable(true);
 * mods.moremod.AffixTier.setModeExponential();
 * mods.moremod.AffixTier.printTable();
 */
@ZenRegister
@ZenClass("mods.moremod.AffixTier")
public class CTAffixTierMapper {
    
    // ==========================================
    // 启用/禁用
    // ==========================================
    
    @ZenMethod
    public static void enable(boolean enabled) {
        AffixTierMapper.setEnabled(enabled);
        CraftTweakerAPI.logInfo("[AffixTier] Tier系统: " + (enabled ? "已启用" : "已禁用"));
    }
    
    // ==========================================
    // 模式切换
    // ==========================================
    
    @ZenMethod
    public static void setModeLinear() {
        AffixTierMapper.setMode(AffixTierMapper.TierMode.LINEAR);
        CraftTweakerAPI.logInfo("[AffixTier] 模式: LINEAR (线性增长)");
    }
    
    @ZenMethod
    public static void setModeSqrt() {
        AffixTierMapper.setMode(AffixTierMapper.TierMode.SQRT);
        CraftTweakerAPI.logInfo("[AffixTier] 模式: SQRT (平方根，前期快后期慢)");
    }
    
    @ZenMethod
    public static void setModeExponential() {
        AffixTierMapper.setMode(AffixTierMapper.TierMode.EXPONENTIAL);
        CraftTweakerAPI.logInfo("[AffixTier] 模式: EXPONENTIAL (指数增长，推荐)");
    }
    
    @ZenMethod
    public static void setModeBreakpoint() {
        AffixTierMapper.setMode(AffixTierMapper.TierMode.BREAKPOINT);
        CraftTweakerAPI.logInfo("[AffixTier] 模式: BREAKPOINT (分段门槛，POE风格)");
    }
    
    // ==========================================
    // 调试工具
    // ==========================================
    
    /**
     * 打印当前模式的品质范围表
     */
    @ZenMethod
    public static void printTable() {
        AffixTierMapper.debugPrintCurrentMode();
    }
    
    /**
     * 打印所有模式对比
     */
    @ZenMethod
    public static void printAllModes() {
        AffixTierMapper.debugPrintAllModes();
    }
    
    /**
     * 模拟roll分布
     * 
     * @param gemLevel 宝石等级
     * @param rollCount roll次数（建议1000+）
     */
    @ZenMethod
    public static void simulateRolls(int gemLevel, int rollCount) {
        AffixTierMapper.debugSimulateRolls(gemLevel, rollCount);
    }
    
    /**
     * 获取当前状态
     */
    @ZenMethod
    public static void printStatus() {
        boolean enabled = AffixTierMapper.isEnabled();
        String mode = AffixTierMapper.getMode().name();
        
        CraftTweakerAPI.logInfo("========================================");
        CraftTweakerAPI.logInfo("  AffixTier 系统状态");
        CraftTweakerAPI.logInfo("========================================");
        CraftTweakerAPI.logInfo("  状态: " + (enabled ? "启用" : "禁用"));
        CraftTweakerAPI.logInfo("  模式: " + mode);
        CraftTweakerAPI.logInfo("========================================");
    }
}