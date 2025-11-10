package com.moremod.potion;

import net.minecraft.potion.Potion;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * 模组药水效果注册类 - Minecraft 1.12.2
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ModPotions {

    // 药水效果实例
    public static final Potion MALFUNCTION = new PotionMalfunction();
    public static final Potion MINOR_MALFUNCTION = new PotionMinorMalfunction();

    // ✨ 新增：月殇药水
    public static final Potion MOON_AFFLICTION = new PotionMoonAffliction();

    /**
     * 注册药水效果
     */
    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        IForgeRegistry<Potion> registry = event.getRegistry();

        // 注册故障效果
        registry.register(MALFUNCTION);
        registry.register(MINOR_MALFUNCTION);

        // ✨ 注册月殇效果
        registry.register(MOON_AFFLICTION);
    }
}