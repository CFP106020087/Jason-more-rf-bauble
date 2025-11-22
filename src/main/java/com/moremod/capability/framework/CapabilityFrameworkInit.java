package com.moremod.capability.framework;

import com.moremod.api.capability.CapabilityService;
import com.moremod.api.capability.ICapabilityContainer;
import com.moremod.api.capability.ICapabilityDescriptor;
import com.moremod.api.capability.ICapabilityRegistry;
import com.moremod.capability.framework.example.ExampleCapabilityProvider;
import com.moremod.capability.framework.example.IExampleCapability;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 能力框架初始化类
 * 演示如何初始化和使用能力系统
 */
public class CapabilityFrameworkInit {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation CAPABILITY_KEY = new ResourceLocation("moremod", "capabilities");

    /**
     * 初始化能力系统
     * 应在 Mod 的 PreInit 阶段调用
     */
    public static void preInit() {
        LOGGER.info("Initializing Capability Framework...");

        // 初始化服务
        CapabilityService.initialize();

        // 注册能力
        registerCapabilities();

        // 注册事件处理器
        MinecraftForge.EVENT_BUS.register(new CapabilityEventHandler());

        LOGGER.info("Capability Framework initialized (using fallback: {})",
            CapabilityService.isUsingFallback());
    }

    /**
     * 注册所有能力
     */
    private static void registerCapabilities() {
        ICapabilityRegistry registry = CapabilityService.getRegistry();

        // 注册示例能力
        ICapabilityDescriptor<EntityPlayer> exampleDescriptor =
            CapabilityDescriptorImpl.builder(
                "moremod:example_energy",
                new ExampleCapabilityProvider(),
                EntityPlayer.class
            )
            .priority(100)
            .autoSerialize(true)
            .autoSync(true)
            .description("Example energy capability for demonstration")
            .build();

        registry.registerCapability(exampleDescriptor);

        LOGGER.info("Registered {} capabilities", registry.getAllDescriptors().size());
    }

    /**
     * 完成初始化
     * 应在 Mod 的 Init 阶段调用
     */
    public static void init() {
        // 冻结注册表
        CapabilityService.getRegistry().freeze();
        LOGGER.info("Capability registry frozen");
    }

    /**
     * 能力事件处理器
     */
    public static class CapabilityEventHandler {

        /**
         * 附加能力到实体
         */
        @SubscribeEvent
        public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            Entity entity = event.getObject();

            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                ICapabilityContainer<EntityPlayer> container = CapabilityService.createContainer(player);

                // 自动附加所有注册的能力
                ICapabilityRegistry registry = CapabilityService.getRegistry();
                for (ICapabilityDescriptor<EntityPlayer> descriptor :
                        registry.getDescriptorsForHost(EntityPlayer.class)) {

                    if (descriptor.shouldAttachTo(player)) {
                        container.attachCapability(descriptor.getProvider().createCapability(player));
                    }
                }

                // 附加容器到实体
                event.addCapability(CAPABILITY_KEY, new CapabilityContainerProvider<>(container));
            }
        }

        /**
         * 玩家死亡时复制能力
         */
        @SubscribeEvent
        public void onPlayerClone(PlayerEvent.Clone event) {
            EntityPlayer oldPlayer = event.getOriginal();
            EntityPlayer newPlayer = event.getEntityPlayer();

            ICapabilityContainer<EntityPlayer> oldContainer = getContainer(oldPlayer);
            ICapabilityContainer<EntityPlayer> newContainer = getContainer(newPlayer);

            if (oldContainer != null && newContainer != null) {
                // 复制能力数据
                NBTTagCompound nbt = new NBTTagCompound();
                oldContainer.serializeNBT(nbt);
                newContainer.deserializeNBT(nbt);
            }
        }

        /**
         * 玩家 Tick 更新能力
         */
        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.side.isServer()) {
                ICapabilityContainer<EntityPlayer> container = getContainer(event.player);
                if (container != null) {
                    container.tick();
                }
            }
        }

        /**
         * 获取玩家的能力容器
         */
        private ICapabilityContainer<EntityPlayer> getContainer(EntityPlayer player) {
            if (player.hasCapability(CapabilityContainerProvider.CAPABILITY, null)) {
                return player.getCapability(CapabilityContainerProvider.CAPABILITY, null);
            }
            return null;
        }
    }
}
