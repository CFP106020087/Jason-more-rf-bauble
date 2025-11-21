package com.moremod.network;

import com.moremod.system.FleshRejectionSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSyncRejectionData implements IMessage {

    private NBTTagCompound data;

    public PacketSyncRejectionData() {}

    public PacketSyncRejectionData(NBTTagCompound data) {
        this.data = data;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, data);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        data = ByteBufUtils.readTag(buf);
    }

    // 客户端接收
    public static class Handler implements IMessageHandler<PacketSyncRejectionData, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncRejectionData msg, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;

                // 覆盖客户端 EntityData
                if (player != null && msg.data != null) {
                    NBTTagCompound root = player.getEntityData()
                            .getCompoundTag("MoreMod_RejectionData");

                    root.merge(msg.data);
                }
            });
            return null;
        }
    }

    /** 用于服务器发送封包 */
    public static void send(EntityPlayerMP player, NBTTagCompound data) {
        NetworkHandler.INSTANCE.sendTo(new PacketSyncRejectionData(data), player);
    }
}
