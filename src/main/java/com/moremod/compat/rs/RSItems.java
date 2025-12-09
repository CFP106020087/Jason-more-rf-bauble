package com.moremod.compat.rs;

import com.moremod.compat.rs.ItemDimensionCard;
import com.moremod.compat.rs.ItemInfinityCard;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = "moremod")
public class RSItems {
    
    @GameRegistry.ObjectHolder("moremod:infinity_card")
    public static final Item INFINITY_CARD = null;
    
    @GameRegistry.ObjectHolder("moremod:dimension_card")
    public static final Item DIMENSION_CARD = null;
    
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // 只有 RS 加载时才注册
        if (!Loader.isModLoaded("refinedstorage")) {
            System.out.println("[MoreMod] Refined Storage not loaded, skipping RS items");
            return;
        }
        
        if (RSConfig.enableInfinityCard) {
            event.getRegistry().register(new ItemInfinityCard());
            System.out.println("[MoreMod] Registered RS Infinity Card");
        }
        
        if (RSConfig.enableDimensionCard) {
            event.getRegistry().register(new ItemDimensionCard());
            System.out.println("[MoreMod] Registered RS Dimension Card");
        }
    }
}
