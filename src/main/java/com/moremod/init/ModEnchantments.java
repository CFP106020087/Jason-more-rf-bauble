package com.moremod.init;

import com.moremod.enchantment.EnchantmentTrueDamage;
import net.minecraft.enchantment.Enchantment;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 附魔注册
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ModEnchantments {

    public static Enchantment TRUE_DAMAGE;

    @SubscribeEvent
    public static void onRegisterEnchantments(RegistryEvent.Register<Enchantment> event) {
        TRUE_DAMAGE = new EnchantmentTrueDamage();
        event.getRegistry().register(TRUE_DAMAGE);
        System.out.println("[MoreMod] ⚔️ 真伤附魔已注册");
    }
}
