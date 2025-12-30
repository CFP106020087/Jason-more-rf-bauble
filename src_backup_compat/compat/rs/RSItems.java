package com.moremod.compat.rs;

import com.moremod.compat.rs.ItemDimensionCard;
import com.moremod.compat.rs.ItemInfinityCard;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        if (!Loader.isModLoaded("refinedstorage")) {
            return;
        }

        if (INFINITY_CARD != null) {
            ModelLoader.setCustomModelResourceLocation(INFINITY_CARD, 0,
                    new ModelResourceLocation(INFINITY_CARD.getRegistryName(), "inventory"));
        }

        if (DIMENSION_CARD != null) {
            ModelLoader.setCustomModelResourceLocation(DIMENSION_CARD, 0,
                    new ModelResourceLocation(DIMENSION_CARD.getRegistryName(), "inventory"));
        }
    }
}
