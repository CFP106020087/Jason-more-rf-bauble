package com.adversity.capability;

import com.adversity.Adversity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * Capability 注册和管理
 */
public class CapabilityHandler {

    public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(Adversity.MODID, "adversity_data");

    @CapabilityInject(IAdversityCapability.class)
    public static Capability<IAdversityCapability> ADVERSITY_CAPABILITY = null;

    /**
     * 注册 Capability
     */
    public static void register() {
        CapabilityManager.INSTANCE.register(
            IAdversityCapability.class,
            new AdversityCapabilityStorage(),
            AdversityCapability::new
        );
        Adversity.LOGGER.info("Adversity Capability registered");
    }

    /**
     * 附加 Capability 到实体
     */
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();

        // 只附加到 EntityLiving（怪物、动物等）
        if (entity instanceof EntityLiving) {
            event.addCapability(CAPABILITY_ID, new AdversityCapabilityProvider());
        }
    }

    /**
     * 获取实体的 Adversity Capability
     */
    @Nullable
    public static IAdversityCapability getCapability(Entity entity) {
        if (entity == null || ADVERSITY_CAPABILITY == null) {
            return null;
        }

        if (entity.hasCapability(ADVERSITY_CAPABILITY, null)) {
            return entity.getCapability(ADVERSITY_CAPABILITY, null);
        }

        return null;
    }

    /**
     * 检查实体是否有 Adversity Capability
     */
    public static boolean hasCapability(Entity entity) {
        return entity != null && ADVERSITY_CAPABILITY != null
            && entity.hasCapability(ADVERSITY_CAPABILITY, null);
    }
}
