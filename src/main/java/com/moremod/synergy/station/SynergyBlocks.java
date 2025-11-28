package com.moremod.synergy.station;

import com.moremod.moremod;
import com.moremod.synergy.item.ItemSynergyGuide;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Synergy 系统方块和物品注册
 *
 * 独立的注册类，通过 EventBusSubscriber 自动加载。
 * 如果要完全移除 Synergy 系统，删除整个 synergy 包即可。
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class SynergyBlocks {

    /** Synergy 链结站方块 */
    public static Block SYNERGY_STATION;

    /** Synergy 协同手册 */
    public static Item SYNERGY_GUIDE;

    /**
     * 注册方块
     */
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        try {
            SYNERGY_STATION = new BlockSynergyStation();
            event.getRegistry().register(SYNERGY_STATION);
            System.out.println("[Synergy] Synergy Station block registered");

            // 注册 TileEntity
            GameRegistry.registerTileEntity(TileEntitySynergyStation.class,
                    new ResourceLocation(moremod.MODID, "synergy_station"));
            System.out.println("[Synergy] Synergy Station TileEntity registered");

        } catch (Exception e) {
            System.err.println("[Synergy] Failed to register Synergy blocks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 注册物品（ItemBlock 和独立物品）
     */
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        try {
            if (SYNERGY_STATION != null) {
                event.getRegistry().register(new ItemBlock(SYNERGY_STATION)
                        .setRegistryName(SYNERGY_STATION.getRegistryName()));
                System.out.println("[Synergy] Synergy Station ItemBlock registered");
            }

            // 注册协同手册
            SYNERGY_GUIDE = new ItemSynergyGuide();
            event.getRegistry().register(SYNERGY_GUIDE);
            System.out.println("[Synergy] Synergy Guide item registered");

        } catch (Exception e) {
            System.err.println("[Synergy] Failed to register Synergy items: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 注册模型（客户端）
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        try {
            if (SYNERGY_STATION != null) {
                ModelLoader.setCustomModelResourceLocation(
                        Item.getItemFromBlock(SYNERGY_STATION), 0,
                        new ModelResourceLocation(SYNERGY_STATION.getRegistryName(), "inventory")
                );
                System.out.println("[Synergy] Synergy Station model registered");
            }

            // 注册协同手册模型
            if (SYNERGY_GUIDE != null) {
                ModelLoader.setCustomModelResourceLocation(
                        SYNERGY_GUIDE, 0,
                        new ModelResourceLocation(SYNERGY_GUIDE.getRegistryName(), "inventory")
                );
                System.out.println("[Synergy] Synergy Guide model registered");
            }

        } catch (Exception e) {
            System.err.println("[Synergy] Failed to register Synergy models: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
