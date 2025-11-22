package com.moremod.module.integration;

import com.moremod.module.api.IModuleContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Capability 系统软集成
 *
 * 特性:
 * - 反射检测 Forge Capability 系统
 * - 可选依赖（Capability 不存在时不会崩溃）
 * - 模块可作为 Capability 挂载
 */
public class CapabilityIntegration {

    private static boolean capabilitySystemAvailable = false;

    static {
        tryInitialize();
    }

    /**
     * 尝试初始化 Capability 集成
     */
    private static void tryInitialize() {
        try {
            // 尝试加载 Forge Capability 类
            Class.forName("net.minecraftforge.common.capabilities.Capability");
            Class.forName("net.minecraftforge.common.capabilities.ICapabilityProvider");

            capabilitySystemAvailable = true;
            System.out.println("[CapabilityIntegration] Forge Capability system detected and ready");
        } catch (Throwable t) {
            System.out.println("[CapabilityIntegration] Forge Capability system not available: " + t.getMessage());
            capabilitySystemAvailable = false;
        }
    }

    /**
     * 检查 Capability 系统是否可用
     */
    public static boolean isAvailable() {
        return capabilitySystemAvailable;
    }

    /**
     * 获取对象的 Capability（通过反射）
     *
     * @param object 目标对象
     * @param capabilityName Capability 名称
     * @param context 模块上下文
     * @return Capability 实例，不存在则返回null
     */
    @Nullable
    public static Object getCapability(@Nonnull Object object, @Nonnull String capabilityName,
                                       @Nonnull IModuleContext context) {
        if (!isAvailable()) {
            context.debug("Capability system not available");
            return null;
        }

        try {
            // 这里可以扩展为通过反射调用 getCapability
            // 目前返回null作为占位符
            context.debug("Capability lookup not yet implemented: " + capabilityName);
            return null;
        } catch (Throwable t) {
            context.error("Failed to get capability " + capabilityName + ": " + t.getMessage());
            return null;
        }
    }

    /**
     * 检查对象是否有指定 Capability
     *
     * @param object 目标对象
     * @param capabilityName Capability 名称
     * @param context 模块上下文
     * @return true 有, false 没有
     */
    public static boolean hasCapability(@Nonnull Object object, @Nonnull String capabilityName,
                                        @Nonnull IModuleContext context) {
        return getCapability(object, capabilityName, context) != null;
    }
}
