package com.moremod.system.humanity;

import com.moremod.moremod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 人性值能力处理器
 * Humanity Capability Handler
 *
 * 处理能力注册、附加和玩家死亡/重生时的数据保留
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class HumanityCapabilityHandler {

    public static final ResourceLocation HUMANITY_CAP_KEY =
            new ResourceLocation(moremod.MODID, "humanity_data");

    /**
     * 注册能力
     * 在 preInit 中调用
     */
    public static void register() {
        CapabilityManager.INSTANCE.register(
                IHumanityData.class,
                new HumanityDataStorage(),
                HumanityDataImpl::new
        );
        System.out.println("[moremod] ✅ 人性值Capability注册完成");
    }

    /**
     * 附加能力到玩家实体
     */
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(HUMANITY_CAP_KEY, new HumanityDataProvider());
        }
    }

    /**
     * 玩家死亡后克隆数据
     * 保留人性值系统数据（按设计文档处理）
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        EntityPlayer original = event.getOriginal();
        EntityPlayer newPlayer = event.getEntityPlayer();

        IHumanityData oldData = original.getCapability(HumanityDataProvider.HUMANITY_CAP, null);
        IHumanityData newData = newPlayer.getCapability(HumanityDataProvider.HUMANITY_CAP, null);

        if (oldData != null && newData != null) {
            // 复制所有数据
            newData.copyFrom(oldData);

            // 处理死亡时的特殊逻辑
            if (event.isWasDeath()) {
                if (oldData.isDissolutionActive()) {
                    // 崩解中死亡：人性重置为50%
                    newData.endDissolution(false);
                    newData.setHumanity(50f);
                }
                // 其他死亡不改变人性值
            }
        }
    }

    /**
     * 获取玩家的人性值数据
     * @param player 玩家实体
     * @return 人性值数据，可能为null
     */
    public static IHumanityData getData(EntityPlayer player) {
        return player.getCapability(HumanityDataProvider.HUMANITY_CAP, null);
    }

    /**
     * 检查玩家是否有人性值能力
     */
    public static boolean hasCapability(EntityPlayer player) {
        return player.hasCapability(HumanityDataProvider.HUMANITY_CAP, null);
    }
}
