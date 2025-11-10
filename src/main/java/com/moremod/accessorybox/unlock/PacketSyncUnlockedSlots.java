package com.moremod.accessorybox.unlock;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashSet;
import java.util.Set;

/**
 * 同步玩家解锁槽位数据包
 * 服务器 -> 客户端
 */
public class PacketSyncUnlockedSlots implements IMessage {
    
    private Set<Integer> unlockedSlots;
    
    public PacketSyncUnlockedSlots() {
        this.unlockedSlots = new HashSet<>();
    }
    
    public PacketSyncUnlockedSlots(Set<Integer> unlockedSlots) {
        this.unlockedSlots = new HashSet<>(unlockedSlots);
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        // 写入解锁槽位数量
        buf.writeInt(unlockedSlots.size());
        
        // 写入每个槽位ID
        for (Integer slotId : unlockedSlots) {
            buf.writeInt(slotId);
        }
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        // 读取数量
        int count = buf.readInt();
        
        unlockedSlots = new HashSet<>();
        
        // 读取每个槽位ID
        for (int i = 0; i < count; i++) {
            unlockedSlots.add(buf.readInt());
        }
    }
    
    public static class Handler implements IMessageHandler<PacketSyncUnlockedSlots, IMessage> {
        
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncUnlockedSlots message, MessageContext ctx) {
            // 在主线程处理
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player != null) {
                    SlotUnlockManager.getInstance().receiveSync(
                        player.getUniqueID(),
                        message.unlockedSlots
                    );
                }
            });
            return null;
        }
    }
}
