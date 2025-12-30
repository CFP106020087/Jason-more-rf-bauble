package com.moremod;

import com.moremod.init.ModBlocks;
import com.moremod.init.ModItems;
import com.moremod.init.ModBlockEntities;
import com.moremod.init.ModMenuTypes;
import com.moremod.init.ModCreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Jason's More RF Bauble - 1.20.1 Forge 移植版
 *
 * 移植自1.12.2版本，专注于机械核心系统
 *
 * Phase 1 解耦完成的依赖:
 * - Baubles -> Curios API
 * - GeckoLib 3.x -> GeckoLib 4.x
 * - CraftTweaker 1.12 -> CraftTweaker 1.20
 *
 * 已解耦（不再依赖）:
 * - Champions, Infernal Mobs, Lycanites Mobs
 * - SR Parasites, Enhanced Visuals, PotionCore
 * - JEI, Ice and Fire, Refined Storage
 */
@Mod(MoreMod.MOD_ID)
public class MoreMod {
    public static final String MOD_ID = "moremod";
    public static final Logger LOGGER = LogManager.getLogger();

    public MoreMod() {
        LOGGER.info("Jason's More RF Bauble initializing...");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册Deferred Registers
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        // 注册生命周期事件
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        // 注册Forge事件
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Jason's More RF Bauble registration complete");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Common setup...");
        event.enqueueWork(() -> {
            // 网络包注册
            // ModNetwork.register();

            // 配方注册等
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Client setup...");
        event.enqueueWork(() -> {
            // 渲染器注册
            // Screen注册等
        });
    }
}
