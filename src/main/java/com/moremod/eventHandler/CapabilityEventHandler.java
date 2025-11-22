package com.moremod.eventHandler;

import com.moremod.MoreMod;
import com.moremod.capability.IMechCoreData;
import com.moremod.capability.MechCoreDataProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Capability 生命周期事件处理器
 *
 * 职责：
 *  ✓ Attach - 附加 Capability 到玩家
 *  ✓ Clone - 死亡复制数据
 *  ✓ Save/Load - 存档序列化（由 Provider 自动处理）
 */
public class CapabilityEventHandler {

    private static final ResourceLocation MECH_CORE_CAP_ID = new ResourceLocation(
        MoreMod.MODID,
        "mech_core_data"
    );

    /**
     * 附加 Capability 到玩家实体
     */
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getObject();

            // 附加 MechCoreData Capability
            event.addCapability(
                MECH_CORE_CAP_ID,
                new MechCoreDataProvider()
            );

            if (!player.world.isRemote) {
                MoreMod.logger.debug(
                    "Attached MechCoreData capability to player: {}",
                    player.getName()
                );
            }
        }
    }

    /**
     * 玩家死亡/重生时复制 Capability 数据
     *
     * 机械核心永不丢失
     */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        EntityPlayer oldPlayer = event.getOriginal();
        EntityPlayer newPlayer = event.getEntityPlayer();

        IMechCoreData oldData = oldPlayer.getCapability(IMechCoreData.CAPABILITY, null);
        IMechCoreData newData = newPlayer.getCapability(IMechCoreData.CAPABILITY, null);

        if (oldData != null && newData != null) {
            // 复制所有数据
            newData.copyFrom(oldData);

            // 标记为需要同步
            newData.markDirty();

            if (!newPlayer.world.isRemote) {
                MoreMod.logger.debug(
                    "Cloned MechCoreData for player: {}",
                    newPlayer.getName()
                );
            }
        }
    }

    /**
     * 玩家登出时的清理（可选）
     *
     * Capability 会自动随实体卸载而清理
     */
    @SubscribeEvent
    public void onPlayerLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;

        if (!player.world.isRemote) {
            MoreMod.logger.debug(
                "Player logout: {} (Capability will be auto-cleaned)",
                player.getName()
            );
        }
    }
}
