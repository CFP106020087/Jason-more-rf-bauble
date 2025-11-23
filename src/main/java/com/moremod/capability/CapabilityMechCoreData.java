package com.moremod.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;

/**
 * Capability 注册类
 *
 * 在 Mod 初始化时调用，注册 MechCoreData Capability
 */
public class CapabilityMechCoreData {

    /**
     * 注册 Capability
     * 在 CommonProxy.preInit() 中调用
     */
    public static void register() {
        CapabilityManager.INSTANCE.register(
            IMechCoreData.class,
            new MechCoreDataStorage(),
            MechCoreDataImpl::new
        );
    }
}
