package com.moremod.potion;

import com.moremod.item.sawblade.potion.PotionBloodEuphoria;  // ✨ 新增导入
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
    public static final Potion MOON_AFFLICTION = new PotionMoonAffliction();

    // ✨ 新增：鲜血欢愉
    public static final Potion BLOOD_EUPHORIA = new PotionBloodEuphoria();

    /**
     * 注册药水效果
     */
    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        IForgeRegistry<Potion> registry = event.getRegistry();

        // 注册故障效果
        registry.register(MALFUNCTION);
        registry.register(MINOR_MALFUNCTION);
        registry.register(MOON_AFFLICTION);

        // ✨ 注册鲜血欢愉
        registry.register(BLOOD_EUPHORIA);

        System.out.println("[MoreMod] Blood Euphoria Potion registered!");
    }
}