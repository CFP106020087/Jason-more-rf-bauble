package com.moremod.network;

import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 人性值数据同步包 (服务端 -> 客户端)
 * Humanity Data Sync Packet (Server -> Client)
 */
public class PacketSyncHumanityData implements IMessage {

    private NBTTagCompound data;

    public PacketSyncHumanityData() {
        this.data = new NBTTagCompound();
    }

    public PacketSyncHumanityData(IHumanityData humanityData) {
        this.data = humanityData.serializeNBT();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.data = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, this.data);
    }

    public static class Handler implements IMessageHandler<PacketSyncHumanityData, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncHumanityData message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player == null) return;

                IHumanityData data = HumanityCapabilityHandler.getData(player);
                if (data != null) {
                    data.deserializeNBT(message.data);
                }
            });
            return null;
        }
    }
}
