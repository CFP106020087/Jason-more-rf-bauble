package com.moremod.accessorybox.unlock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;

/**
 * 槽位解锁事件处理器
 * 处理玩家登录/登出/重生/换维度 同步；以及世界卸载时的全局清理
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class UnlockEventHandler {

    // NBT 键名（需要与 SlotUnlockManager 保持一致）
    private static final String NBT_UNLOCKED_SLOTS = "UnlockedBaubleSlots";

    /**
     * 玩家死亡后克隆数据 - 关键！将解锁数据从旧玩家复制到新玩家
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        EntityPlayer oldPlayer = event.getOriginal();
        EntityPlayer newPlayer = event.getEntityPlayer();

        // 复制解锁槽位数据
        NBTTagCompound oldData = oldPlayer.getEntityData();
        NBTTagCompound newData = newPlayer.getEntityData();

        if (oldData.hasKey(NBT_UNLOCKED_SLOTS)) {
            int[] unlockedSlots = oldData.getIntArray(NBT_UNLOCKED_SLOTS);
            newData.setIntArray(NBT_UNLOCKED_SLOTS, unlockedSlots);

            System.out.println("[UnlockEventHandler] 玩家死亡克隆，复制解锁槽位数据: " +
                    oldPlayer.getName() + " -> " + unlockedSlots.length + " 个槽位");
        }
    }

    /**
     * 玩家登录时加载和同步解锁数据
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
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
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
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
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
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
    public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        EntityPlayer player = event.player;

        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;

            // 重新同步到客户端
            SlotUnlockManager.getInstance().syncToClient(playerMP);

            System.out.println("[UnlockEventHandler] 玩家切换维度，重新同步解锁数据: " + player.getName());
        }
    }

    /**
     * 世界卸载时（仅服务器端 & 主维度 0）清空所有玩家的解锁缓存
     * 解决单机同一进程切换存档时的“跨世界继承”问题
     */
    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() == null || event.getWorld().isRemote) return;

        // 仅在主维度卸载时清空（避免多维度卸载触发多次）
        try {
            if (event.getWorld().provider != null && event.getWorld().provider.getDimension() != 0) {
                return;
            }
        } catch (Throwable ignored) {}

        SlotUnlockManager.getInstance().clearAll();
        System.out.println("[UnlockEventHandler] Overworld 卸载，已清空全部解锁缓存");
    }
}
