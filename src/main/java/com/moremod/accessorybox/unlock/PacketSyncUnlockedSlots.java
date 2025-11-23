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

    private Set<Integer> permanentSlots;  // 永久解锁
    private Set<Integer> temporarySlots;  // 临时解锁

    public PacketSyncUnlockedSlots() {
        this.permanentSlots = new HashSet<>();
        this.temporarySlots = new HashSet<>();
    }

    public PacketSyncUnlockedSlots(Set<Integer> permanent, Set<Integer> temporary) {
        this.permanentSlots = new HashSet<>(permanent);
        this.temporarySlots = new HashSet<>(temporary);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 写入永久解锁
        buf.writeInt(permanentSlots.size());
        for (Integer slotId : permanentSlots) {
            buf.writeInt(slotId);
        }

        // 写入临时解锁
        buf.writeInt(temporarySlots.size());
        for (Integer slotId : temporarySlots) {
            buf.writeInt(slotId);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // 读取永久解锁
        int permCount = buf.readInt();
        permanentSlots = new HashSet<>();
        for (int i = 0; i < permCount; i++) {
            permanentSlots.add(buf.readInt());
        }

        // 读取临时解锁
        int tempCount = buf.readInt();
        temporarySlots = new HashSet<>();
        for (int i = 0; i < tempCount; i++) {
            temporarySlots.add(buf.readInt());
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncUnlockedSlots, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncUnlockedSlots message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player != null) {
                    // 分别同步永久和临时
                    SlotUnlockManager.getInstance().receiveSync(
                            player.getUniqueID(),
                            message.permanentSlots,
                            message.temporarySlots
                    );
                }
            });
            return null;
        }
    }
}