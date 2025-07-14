package com.moremod.item;

import com.moremod.client.ClientTickEvent;
import com.moremod.client.JetpackKeyHandler;
import com.moremod.eventHandler.BatteryChargeHandler;
import com.moremod.eventHandler.EventHandlerJetpack;
import com.moremod.eventHandler.MechanicalHeartEventHandler; // 时间之心事件处理器
import com.moremod.eventHandler.CreativeBatteryChargeHandler; // 新增：创造电池充电处理器
import com.moremod.network.PacketHandler;
import com.moremod.sounds.ModSounds; // 音效系统

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = moremod.MODID, name = moremod.NAME, version = moremod.VERSION)
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class moremod {

    public static final String MODID = "moremod";
    public static final String NAME = "More Mod";
    public static final String VERSION = "1.0";

    @Instance(MODID)
    public static moremod instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        // ✅ 注册音效 - 时间之心音效系统
        ModSounds.registerSounds(MODID);

        // ✅ 网络通信
        PacketHandler.registerMessages();

        // ✅ 注册 TileEntities

        // ✅ 客户端事件注册
        if (event.getSide().isClient()) {
            MinecraftForge.EVENT_BUS.register(new ClientTickEvent());       // 客户端 Tick
            JetpackKeyHandler.registerKeys();                               // 键位绑定
            MinecraftForge.EVENT_BUS.register(new JetpackKeyHandler());    // 键位监听
            MinecraftForge.EVENT_BUS.register(EventHandlerJetpack.class);  // ✔ Jetpack 客户端事件（静态）
            MinecraftForge.EVENT_BUS.register(new BatteryChargeHandler());  // 普通电池充电处理器
        }

        // ✅ 服务端 / 通用事件注册
        // 时间之心事件处理器
        MinecraftForge.EVENT_BUS.register(new MechanicalHeartEventHandler());

        // 🌟 新增：创造电池充电处理器
        MinecraftForge.EVENT_BUS.register(new CreativeBatteryChargeHandler());
    }

    // 注册物品
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        RegisterItem.registerItems(event);
    }

    // 注册模型（客户端）
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        RegisterItem.registerModels(event);
    }
}