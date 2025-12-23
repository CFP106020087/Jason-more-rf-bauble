package com.moremod.init;

import com.moremod.enchantment.EnchantmentTrueDamage;
import com.moremod.sponsor.SponsorConfig;
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
        // 真伤附魔属于赞助者物品，跟随赞助者配置
        if (SponsorConfig.enabled) {
            TRUE_DAMAGE = new EnchantmentTrueDamage();
            event.getRegistry().register(TRUE_DAMAGE);
            System.out.println("[MoreMod] ⚔️ 真伤附魔已注册");
        } else {
            System.out.println("[MoreMod] 真伤附魔已禁用 (赞助者物品关闭)");
        }
    }
}
