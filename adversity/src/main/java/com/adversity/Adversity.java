package com.adversity;

import com.adversity.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = Adversity.MODID,
    name = Adversity.NAME,
    version = Adversity.VERSION,
    acceptedMinecraftVersions = "[1.12.2]"
)
public class Adversity {

    public static final String MODID = "adversity";
    public static final String NAME = "Adversity";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static Adversity instance;

    @SidedProxy(
        clientSide = "com.adversity.proxy.ClientProxy",
        serverSide = "com.adversity.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Adversity Pre-Initialization");
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Adversity Initialization");
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("Adversity Post-Initialization");
        proxy.postInit(event);
    }
}
