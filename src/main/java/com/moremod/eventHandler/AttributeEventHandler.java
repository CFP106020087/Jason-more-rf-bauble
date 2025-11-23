package com.moremod.eventHandler;

import com.moremod.compat.crafttweaker.AttributeHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 属性事件处理器
 * 
 * 功能：
 * 1. 玩家切换物品时更新属性
 * 2. 玩家登录时应用属性
 * 3. 定期检查和更新属性
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class AttributeEventHandler {
    
    // 记录每个玩家上次持有的武器（用于检测切换）
    private static final Map<UUID, ItemStack> LAST_HELD_ITEMS = new HashMap<>();
    
    /**
     * 玩家登录时应用属性
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;
        
        // 延迟应用（确保玩家完全加载）
        player.world.getMinecraftServer().addScheduledTask(() -> {
            AttributeHelper.refreshPlayerAttributes(player);
        });
    }
    
    /**
     * 玩家重生时重新应用属性
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;
        
        // 延迟应用
        player.world.getMinecraftServer().addScheduledTask(() -> {
            AttributeHelper.refreshPlayerAttributes(player);
        });
    }
    
    /**
     * 定期检查武器切换
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;
        
        EntityPlayer player = event.player;
        
        // 每20 tick检查一次（1秒）
        if (player.ticksExisted % 20 != 0) return;
        
        ItemStack currentItem = player.getHeldItemMainhand();
        UUID playerUUID = player.getUniqueID();
        ItemStack lastItem = LAST_HELD_ITEMS.get(playerUUID);
        
        // 检查是否切换了武器
        if (hasItemChanged(currentItem, lastItem)) {
            // 更新属性
            AttributeHelper.refreshPlayerAttributes(player);
            
            // 记录当前武器
            LAST_HELD_ITEMS.put(playerUUID, currentItem.copy());
        }
    }
    
    /**
     * 玩家退出时清理记录
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_HELD_ITEMS.remove(event.player.getUniqueID());
    }
    
    /**
     * 检查物品是否改变
     */
    private static boolean hasItemChanged(ItemStack current, ItemStack last) {
        // 两者都为空 = 没变
        if (current.isEmpty() && (last == null || last.isEmpty())) {
            return false;
        }
        
        // 一个空一个不空 = 改变了
        if (current.isEmpty() != (last == null || last.isEmpty())) {
            return true;
        }
        
        // 物品类型不同 = 改变了
        if (current.getItem() != last.getItem()) {
            return true;
        }
        
        // NBT不同 = 改变了（宝石镶嵌/移除）
        if (!ItemStack.areItemStackTagsEqual(current, last)) {
            return true;
        }
        
        return false;
    }
}