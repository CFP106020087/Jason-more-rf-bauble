package com.moremod.sounds;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class ModSounds {

    public static SoundEvent MECHANICAL_HEART_TICK;

    public static void registerSounds(String modid) {
        MECHANICAL_HEART_TICK = registerSound(modid, "mechanical_heart_tick");
    }

    private static SoundEvent registerSound(String modid, String soundName) {
        ResourceLocation location = new ResourceLocation(modid, soundName);
        SoundEvent event = new SoundEvent(location);
        event.setRegistryName(location);
        ForgeRegistries.SOUND_EVENTS.register(event);
        return event;
    }
}