package com.moremod.init;

import com.moremod.block.BlockDimensionLoom;
import com.moremod.block.BlockSpacetimeShard;
import com.moremod.block.BlockTemporalAccelerator;
import com.moremod.block.BlockTimeController;
import com.moremod.block.BlockUnbreakableBarrier;
import com.moremod.moremod;
import com.moremod.tile.TileEntityDimensionLoom;
import com.moremod.tile.TileEntityTemporalAccelerator;
import com.moremod.tile.TileEntityTimeController;
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

@Mod.EventBusSubscriber(modid = moremod.MODID)
public class ModBlocks {

    public static final Block SPACETIME_SHARD_ORE = new BlockSpacetimeShard();

    public static Block TIME_CONTROLLER;

    public static final Block UNBREAKABLE_BARRIER_VOID     = new BlockUnbreakableBarrier(BlockUnbreakableBarrier.BarrierType.VOID_CRYSTAL);
    public static final Block UNBREAKABLE_BARRIER_QUANTUM  = new BlockUnbreakableBarrier(BlockUnbreakableBarrier.BarrierType.QUANTUM_FIELD);
    public static final Block UNBREAKABLE_BARRIER_TEMPORAL = new BlockUnbreakableBarrier(BlockUnbreakableBarrier.BarrierType.TEMPORAL_LOCK);
    public static final Block UNBREAKABLE_BARRIER_ANCHOR   = new BlockUnbreakableBarrier(BlockUnbreakableBarrier.BarrierType.DIMENSIONAL_ANCHOR);
    public static final Block UNBREAKABLE_BARRIER_ETHEREAL = new BlockUnbreakableBarrier(BlockUnbreakableBarrier.BarrierType.ETHEREAL_WALL);

    public static Block TEMPORAL_ACCELERATOR;
    public static BlockDimensionLoom dimensionLoom;

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(SPACETIME_SHARD_ORE);

        TIME_CONTROLLER = new BlockTimeController();
        event.getRegistry().register(TIME_CONTROLLER);

        dimensionLoom = new BlockDimensionLoom();
        event.getRegistry().register(dimensionLoom);

        event.getRegistry().registerAll(
                UNBREAKABLE_BARRIER_VOID,
                UNBREAKABLE_BARRIER_QUANTUM,
                UNBREAKABLE_BARRIER_TEMPORAL,
                UNBREAKABLE_BARRIER_ANCHOR,
                UNBREAKABLE_BARRIER_ETHEREAL
        );

        TEMPORAL_ACCELERATOR = new BlockTemporalAccelerator();
        event.getRegistry().register(TEMPORAL_ACCELERATOR);

        // TileEntity 注册（1.12 用 RL）
        GameRegistry.registerTileEntity(TileEntityDimensionLoom.class,
                new ResourceLocation(moremod.MODID, "dimension_loom"));

        GameRegistry.registerTileEntity(TileEntityTimeController.class,
                new ResourceLocation(moremod.MODID, "time_controller"));

        GameRegistry.registerTileEntity(TileEntityTemporalAccelerator.class,
                new ResourceLocation(moremod.MODID, "temporal_accelerator"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemBlock(SPACETIME_SHARD_ORE).setRegistryName(SPACETIME_SHARD_ORE.getRegistryName()));
        event.getRegistry().register(new ItemBlock(TIME_CONTROLLER).setRegistryName(TIME_CONTROLLER.getRegistryName()));
        event.getRegistry().register(new ItemBlock(UNBREAKABLE_BARRIER_VOID).setRegistryName(UNBREAKABLE_BARRIER_VOID.getRegistryName()));
        event.getRegistry().register(new ItemBlock(UNBREAKABLE_BARRIER_QUANTUM).setRegistryName(UNBREAKABLE_BARRIER_QUANTUM.getRegistryName()));
        event.getRegistry().register(new ItemBlock(UNBREAKABLE_BARRIER_TEMPORAL).setRegistryName(UNBREAKABLE_BARRIER_TEMPORAL.getRegistryName()));
        event.getRegistry().register(new ItemBlock(UNBREAKABLE_BARRIER_ANCHOR).setRegistryName(UNBREAKABLE_BARRIER_ANCHOR.getRegistryName()));
        event.getRegistry().register(new ItemBlock(UNBREAKABLE_BARRIER_ETHEREAL).setRegistryName(UNBREAKABLE_BARRIER_ETHEREAL.getRegistryName()));
        event.getRegistry().register(new ItemBlock(dimensionLoom).setRegistryName(dimensionLoom.getRegistryName()));

        if (TEMPORAL_ACCELERATOR != null) {
            event.getRegistry().register(new ItemBlock(TEMPORAL_ACCELERATOR).setRegistryName(TEMPORAL_ACCELERATOR.getRegistryName()));
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerBlockModel(SPACETIME_SHARD_ORE);
        registerBlockModel(TIME_CONTROLLER);
        registerBlockModel(UNBREAKABLE_BARRIER_VOID);
        registerBlockModel(UNBREAKABLE_BARRIER_QUANTUM);
        registerBlockModel(UNBREAKABLE_BARRIER_TEMPORAL);
        registerBlockModel(UNBREAKABLE_BARRIER_ANCHOR);
        registerBlockModel(UNBREAKABLE_BARRIER_ETHEREAL);
        registerBlockModel(dimensionLoom);
        if (TEMPORAL_ACCELERATOR != null) registerBlockModel(TEMPORAL_ACCELERATOR);
    }

    @SideOnly(Side.CLIENT)
    private static void registerBlockModel(Block block) {
        if (block != null) {
            ModelLoader.setCustomModelResourceLocation(
                    Item.getItemFromBlock(block), 0,
                    new ModelResourceLocation(block.getRegistryName(), "inventory")
            );
        }
    }
}
