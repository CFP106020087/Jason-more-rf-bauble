// ============================================
// 文件: ServerTickHandler.java
// 位置: src/main/java/com/moremod/eventHandler/ServerTickHandler.java
// ============================================

package com.moremod.event.eventHandler;

import com.moremod.capability.IPlayerTimeData;
import com.moremod.capability.PlayerTimeDataCapability;
import com.moremod.network.PacketSyncPlayerTime;
import com.moremod.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ServerTickHandler {

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;

            // 每20tick (1秒) 更新一次
            if (tickCounter % 20 == 0) {
                MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                if (server != null) {
                    for (EntityPlayer player : server.getPlayerList().getPlayers()) {
                        IPlayerTimeData data = PlayerTimeDataCapability.get(player);
                        if (data != null) {
                            int oldDays = data.getTotalDaysPlayed();
                            data.addPlayTime(20); // 添加20tick的游戏时间
                            int newDays = data.getTotalDaysPlayed();

                            // 智能同步策略
                            boolean shouldSync = false;

                            // 1. 天数发生变化时立即同步
                            if (oldDays != newDays) {
                                shouldSync = true;
                            }
                            // 2. 每10秒定期同步（确保数据一致性）
                            else if (tickCounter % 200 == 0) {
                                shouldSync = true;
                            }
                            // 3. 玩家装备了时光之心且每5秒同步一次（提升响应性）
                            else if (data.hasEquippedTemporalHeart() && tickCounter % 100 == 0) {
                                shouldSync = true;
                            }

                            // 发送同步包
                            if (shouldSync) {
                                PacketSyncPlayerTime packet = new PacketSyncPlayerTime(
                                        data.getTotalDaysPlayed(),
                                        data.getTotalPlayTime(),
                                        data.hasEquippedTemporalHeart(),
                                        data.getLastLoginTime()
                                );
                                PacketHandler.INSTANCE.sendTo(packet, (EntityPlayerMP) player);
                            }
                        }
                    }
                }
            }
        }
    }
}