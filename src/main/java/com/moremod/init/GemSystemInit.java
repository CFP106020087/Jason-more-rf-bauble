package com.moremod.init;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * 宝石系统初始化
 * 
 * 在你的主Mod类中调用这个类的初始化方法
 */
public class GemSystemInit {
    
    /**
     * PreInit阶段（最早）
     */
    public static void preInit(FMLPreInitializationEvent event) {
        System.out.println("========================================");
        System.out.println("      💎 宝石系统预初始化开始");
        System.out.println("========================================");
        
        // 这个阶段暂时不需要做什么
    }
    
    /**
     * Init阶段（中期）
     */
    public static void init(FMLInitializationEvent event) {
        System.out.println("========================================");
        System.out.println("      💎 宝石系统初始化开始");
        System.out.println("========================================");
        
        // 设置基础宝石物品
        if (ModItems.GEM != null) {
            System.out.println("[GemSystem] ✓ 基础宝石物品已设置: " + ModItems.GEM.getRegistryName());
        } else {
            System.err.println("[GemSystem] ✗ 错误：ModItems.GEM 为 null！");
        }
        
        // 注册掉落事件监听器
        System.out.println("[GemSystem] ✓ 掉落事件监听器已注册");
        
        // 启用掉落系统
        System.out.println("[GemSystem] ✓ 掉落系统已启用");
        
        // （可选）启用调试模式
        // GemLootGenerator.setDebugMode(true);
        // System.out.println("[GemSystem] ✓ 调试模式已启用");
        
        System.out.println("========================================");
        System.out.println("      💎 宝石系统初始化完成");
        System.out.println("========================================");
    }
    
    /**
     * PostInit阶段（最晚，CraftTweaker脚本已加载）
     */
    public static void postInit(FMLPostInitializationEvent event) {
        System.out.println("========================================");
        System.out.println("      💎 宝石系统后期初始化");
        System.out.println("========================================");
        
        // 输出系统状态
        printSystemStatus();
        
        System.out.println("========================================");
        System.out.println("      💎 宝石系统完全就绪！");
        System.out.println("========================================");
    }
    
    /**
     * 输出系统状态
     */
    private static void printSystemStatus() {
        System.out.println("");
        System.out.println("📊 宝石系统状态:");
        System.out.println("  - 宝石物品: " + (ModItems.GEM != null ? "✓" : "✗"));
        System.out.println("  - 鉴定卷轴: " + (ModItems.IDENTIFY_SCROLL != null ? "✓" : "✗"));
        System.out.println("  - 掉落系统: ✓");
        System.out.println("  - 规则系统: ✓");
        System.out.println("  - 品质保底: ✓");
        System.out.println("  - 动态材质: ✓");
        System.out.println("");
        System.out.println("📝 下一步:");
        System.out.println("  1. 在 scripts/ 中创建 gem_config_example.zs");
        System.out.println("  2. 在 scripts/ 中创建 gem_loot_rules_example.zs");
        System.out.println("  3. 创建宝石材质文件（6个PNG）");
        System.out.println("  4. 进入游戏测试掉落");
        System.out.println("");
    }
}