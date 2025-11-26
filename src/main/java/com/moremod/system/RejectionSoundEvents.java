package com.moremod.system;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(modid = "moremod")
public class RejectionSoundEvents {

    public static SoundEvent HEARTBEAT;

    @SubscribeEvent
    public static void onRegisterSounds(RegistryEvent.Register<SoundEvent> event) {
        IForgeRegistry<SoundEvent> registry = event.getRegistry();

        HEARTBEAT = register(registry, "heartbeat");
    }

    private static SoundEvent register(IForgeRegistry<SoundEvent> registry, String name) {
        ResourceLocation id = new ResourceLocation("moremod", name);
        SoundEvent event = new SoundEvent(id).setRegistryName(id);
        registry.register(event);
        return event;
    }
}
