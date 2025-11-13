package com.moremod.accessorybox.unlock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * 槽位解锁事件处理器
 * 处理玩家登录/登出/重生/换维度 同步；以及世界卸载时的全局清理
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class UnlockEventHandler {

    /**
     * 玩家登录时加载和同步解锁数据
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;

        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;

            // 从玩家NBT加载解锁数据
            SlotUnlockManager.getInstance().loadFromPlayer(player);

            // 同步到客户端
            SlotUnlockManager.getInstance().syncToClient(playerMP);

            System.out.println("[UnlockEventHandler] 玩家登录，同步解锁数据: " + player.getName());
        }
    }

    /**
     * 玩家登出时保存数据
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;

        // 保存解锁数据到NBT
        SlotUnlockManager.getInstance().saveToPlayer(player);

        // 清理该玩家的内存缓存（避免同进程切换到另一世界时“继承”）
        SlotUnlockManager.getInstance().onPlayerLogout(player.getUniqueID());

        System.out.println("[UnlockEventHandler] 玩家登出，保存并清理内存数据: " + player.getName());
    }

    /**
     * 玩家重生时重新加载数据
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        EntityPlayer player = event.player;

        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;

            // 重新加载解锁数据
            SlotUnlockManager.getInstance().loadFromPlayer(player);

            // 同步到客户端
            SlotUnlockManager.getInstance().syncToClient(playerMP);

            System.out.println("[UnlockEventHandler] 玩家重生，重新同步解锁数据: " + player.getName());
        }
    }

    /**
     * 玩家切换维度时重新同步
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        EntityPlayer player = event.player;

        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;

            // 重新同步到客户端
            SlotUnlockManager.getInstance().syncToClient(playerMP);

            System.out.println("[UnlockEventHandler] 玩家切换维度，重新同步解锁数据: " + player.getName());
        }
    }

    /**
     * 世界卸载时清空所有玩家的解锁缓存
     * 解决单机同一进程切换存档时的"跨世界继承"问题
     */
    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() == null) return;

        // 客户端：直接清空（避免跨世界污染）
        if (event.getWorld().isRemote) {
            SlotUnlockManager.getInstance().clearAll();
            System.out.println("[UnlockEventHandler] [客户端] 世界卸载，已清空全部解锁缓存");
            return;
        }

        // 服务器端：仅在主维度卸载时清空（避免多维度卸载触发多次）
        try {
            if (event.getWorld().provider != null && event.getWorld().provider.getDimension() != 0) {
                return;
            }
        } catch (Throwable ignored) {}

        SlotUnlockManager.getInstance().clearAll();
        System.out.println("[UnlockEventHandler] [服务器] Overworld 卸载，已清空全部解锁缓存");
    }
}
