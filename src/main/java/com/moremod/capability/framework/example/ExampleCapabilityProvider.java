package com.moremod.capability.framework.example;

import com.moremod.api.capability.ICapabilityProvider;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 示例能力提供者
 * 演示如何创建能力提供者
 */
public class ExampleCapabilityProvider implements ICapabilityProvider<EntityPlayer, IExampleCapability> {

    @Override
    public IExampleCapability createCapability(EntityPlayer host) {
        return new ExampleCapabilityImpl();
    }

    @Override
    public Class<IExampleCapability> getCapabilityType() {
        return IExampleCapability.class;
    }

    @Override
    public String getCapabilityId() {
        return ExampleCapabilityImpl.CAPABILITY_ID;
    }
}
