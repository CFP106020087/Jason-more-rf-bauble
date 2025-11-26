package com.moremod.system;

import com.moremod.system.FleshRejectionSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 排异系统同步处理器
 * 
 * 职责：
 * - 处理延迟同步队列
 * - 确保NBT数据适时同步到客户端
 * - 避免频繁同步导致的性能问题
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class RejectionSyncHandler {
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        // ✅ 类型安全检查
        if (!(event.player instanceof EntityPlayerMP)) return;
        
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        
        // 处理排异系统的延迟同步
        FleshRejectionSystem.tickSyncSystem(player);
    }
}