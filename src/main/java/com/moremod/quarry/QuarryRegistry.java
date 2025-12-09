package com.moremod.quarry;

import com.moremod.quarry.block.BlockQuantumQuarry;
import com.moremod.quarry.block.BlockQuarryActuator;
import com.moremod.quarry.gui.ContainerQuantumQuarry;
import com.moremod.quarry.gui.GuiQuantumQuarry;
import com.moremod.quarry.network.PacketHandler;
import com.moremod.quarry.simulation.BiomeOreRegistry;
import com.moremod.quarry.simulation.QuarryLootManager;
import com.moremod.quarry.simulation.VirtualMiningSimulator;
import com.moremod.quarry.tile.TileQuantumQuarry;
import com.moremod.quarry.tile.TileQuarryActuator;
import net.minecraft.block.Block;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * 采石场系统注册
 * 
 * 使用方法：
 * 1. 在你的主 Mod 类的 preInit 中调用 QuarryRegistry.preInit()
 * 2. 在 init 中调用 QuarryRegistry.init(modInstance)
 * 3. 在 postInit 中调用 QuarryRegistry.postInit()
 */
@Mod.EventBusSubscriber
public class QuarryRegistry {
    
    public static final String MODID = "moremod";  // 修改为你的 mod id
    
    // 方块实例
    public static BlockQuantumQuarry blockQuantumQuarry;
    public static BlockQuarryActuator blockQuarryActuator;
    
    // GUI ID
    public static final int GUI_QUANTUM_QUARRY = 100;  // 修改为不冲突的 ID
    
    /**
     * PreInit 阶段调用
     */
    public static void preInit() {
        // 注册网络包
        PacketHandler.init();
    }
    
    /**
     * Init 阶段调用
     */
    public static void init(Object modInstance) {
        // 注册 TileEntity
        GameRegistry.registerTileEntity(TileQuantumQuarry.class, 
            new ResourceLocation(MODID, "quantum_quarry"));
        GameRegistry.registerTileEntity(TileQuarryActuator.class, 
            new ResourceLocation(MODID, "quarry_actuator"));
        
        // 注册 GUI Handler
        // NetworkRegistry.INSTANCE.registerGuiHandler(modInstance, new QuarryGuiHandler());
    }
    
    /**
     * PostInit 阶段调用
     */
    public static void postInit() {
        // 初始化模拟系统
        VirtualMiningSimulator.getInstance().initialize();
        
        // 初始化战利品管理器（自动注册到事件总线）
        QuarryLootManager.getInstance();
    }
    
    // ==================== 事件处理 ====================
    
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        blockQuantumQuarry = new BlockQuantumQuarry();
        blockQuantumQuarry.setRegistryName(MODID, "quantum_quarry");
        blockQuantumQuarry.setTranslationKey(MODID + ".quantum_quarry");
        
        blockQuarryActuator = new BlockQuarryActuator();
        blockQuarryActuator.setRegistryName(MODID, "quarry_actuator");
        blockQuarryActuator.setTranslationKey(MODID + ".quarry_actuator");
        
        event.getRegistry().register(blockQuantumQuarry);
        event.getRegistry().register(blockQuarryActuator);
    }
    
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new ItemBlock(blockQuantumQuarry)
            .setRegistryName(blockQuantumQuarry.getRegistryName()));
        event.getRegistry().register(new ItemBlock(blockQuarryActuator)
            .setRegistryName(blockQuarryActuator.getRegistryName()));
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(
            Item.getItemFromBlock(blockQuantumQuarry), 0,
            new net.minecraft.client.renderer.block.model.ModelResourceLocation(
                blockQuantumQuarry.getRegistryName(), "inventory"));
        
        ModelLoader.setCustomModelResourceLocation(
            Item.getItemFromBlock(blockQuarryActuator), 0,
            new net.minecraft.client.renderer.block.model.ModelResourceLocation(
                blockQuarryActuator.getRegistryName(), "inventory"));
    }
    
    // ==================== GUI Handler ====================
    
    public static class QuarryGuiHandler implements IGuiHandler {
        
        @Nullable
        @Override
        public Container getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
            if (ID == GUI_QUANTUM_QUARRY) {
                TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
                if (te instanceof TileQuantumQuarry) {
                    return new ContainerQuantumQuarry(player.inventory, (TileQuantumQuarry) te);
                }
            }
            return null;
        }
        
        @Nullable
        @Override
        @SideOnly(Side.CLIENT)
        public GuiContainer getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
            if (ID == GUI_QUANTUM_QUARRY) {
                TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
                if (te instanceof TileQuantumQuarry) {
                    return new GuiQuantumQuarry(player.inventory, (TileQuantumQuarry) te);
                }
            }
            return null;
        }
    }
}
