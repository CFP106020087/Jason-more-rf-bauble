package com.moremod.ritual.special;

import com.moremod.ritual.special.impl.SoulboundRitual;
import com.moremod.ritual.special.impl.UnbreakableRitual;

/**
 * 特殊仪式初始化器
 * 在 mod 初始化时注册所有内置的特殊仪式
 *
 * 使用方法：
 * 在 mod 的 init 或 postInit 阶段调用 SpecialRitualInit.init()
 *
 * 添加新仪式：
 * 1. 创建新的仪式类（继承 AbstractSpecialRitual 或实现 ISpecialRitual）
 * 2. 在本类的 init() 方法中注册
 *
 * 示例：
 * SpecialRitualRegistry.register(new MyCustomRitual());
 */
public class SpecialRitualInit {

    private static boolean initialized = false;

    /**
     * 初始化并注册所有内置特殊仪式
     * 应在 mod 的 init 或 postInit 阶段调用
     */
    public static void init() {
        if (initialized) {
            System.out.println("[moremod] SpecialRitualInit already initialized, skipping");
            return;
        }

        System.out.println("[moremod] Initializing Special Ritual System...");

        // ========== 注册内置仪式 ==========

        // 灵魂束缚 - 死亡不掉落
        SpecialRitualRegistry.register(new SoulboundRitual());

        // 不可破坏 - 永不损坏
        SpecialRitualRegistry.register(new UnbreakableRitual());

        // TODO: 从 TileEntityRitualCore 迁移更多仪式
        // SpecialRitualRegistry.register(new EnchantTransferRitual());
        // SpecialRitualRegistry.register(new CursePurificationRitual());
        // SpecialRitualRegistry.register(new EnchantInfusionRitual());
        // SpecialRitualRegistry.register(new DuplicationRitual());
        // ...

        initialized = true;

        // 打印注册结果
        System.out.println("[moremod] Special Ritual System initialized.");
        SpecialRitualRegistry.printAllRituals();
    }

    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 重置初始化状态（仅用于测试）
     */
    public static void reset() {
        SpecialRitualRegistry.clearAll();
        initialized = false;
    }
}
