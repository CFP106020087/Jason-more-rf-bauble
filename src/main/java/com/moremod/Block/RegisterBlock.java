// RegisterBlock.java
package com.moremod.block;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class RegisterBlock {

    // 方块实例

    public static final Block INFINITE_ENERGY_BLOCK = new com.moremod.block.BlockInfiniteEnergy();
    public static final Block ENERGY_HELL_BOOKSHELF = new com.moremod.block.BlockEnergyHellBookshelf();

    // 对应的 ItemBlock
    public static final Item ITEM_INFINITE_ENERGY_BLOCK = new ItemBlock(INFINITE_ENERGY_BLOCK).setRegistryName(INFINITE_ENERGY_BLOCK.getRegistryName());
    public static final Item ITEM_ENERGY_HELL_BOOKSHELF = new ItemBlock(ENERGY_HELL_BOOKSHELF).setRegistryName(ENERGY_HELL_BOOKSHELF.getRegistryName());

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(
                INFINITE_ENERGY_BLOCK,
                ENERGY_HELL_BOOKSHELF
        );
    }

    @SubscribeEvent
    public static void registerItemBlocks(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(

                ITEM_INFINITE_ENERGY_BLOCK,
                ITEM_ENERGY_HELL_BOOKSHELF
        );
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(ITEM_INFINITE_ENERGY_BLOCK, 0,
                new ModelResourceLocation(INFINITE_ENERGY_BLOCK.getRegistryName(), "inventory"));
        ModelLoader.setCustomModelResourceLocation(ITEM_ENERGY_HELL_BOOKSHELF, 0,
                new ModelResourceLocation(ENERGY_HELL_BOOKSHELF.getRegistryName(), "inventory"));
    }
}