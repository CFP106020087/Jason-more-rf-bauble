package com.moremod.network;

import com.moremod.capability.IPlayerTimeData;
import com.moremod.capability.PlayerTimeDataCapability;
import com.moremod.capability.PlayerTimeDataImpl;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketSyncPlayerTime implements IMessage {

    private int totalDays;
    private long totalPlayTime;
    private boolean hasEquippedTemporalHeart;
    private long lastLoginTime;

    public PacketSyncPlayerTime() {}

    public PacketSyncPlayerTime(int totalDays, long totalPlayTime, boolean hasEquippedTemporalHeart, long lastLoginTime) {
        this.totalDays = totalDays;
        this.totalPlayTime = totalPlayTime;
        this.hasEquippedTemporalHeart = hasEquippedTemporalHeart;
        this.lastLoginTime = lastLoginTime;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.totalDays = buf.readInt();
        this.totalPlayTime = buf.readLong();
        this.hasEquippedTemporalHeart = buf.readBoolean();
        this.lastLoginTime = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.totalDays);
        buf.writeLong(this.totalPlayTime);
        buf.writeBoolean(this.hasEquippedTemporalHeart);
        buf.writeLong(this.lastLoginTime);
    }

    public static class Handler implements IMessageHandler<PacketSyncPlayerTime, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncPlayerTime message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player != null) {
                    IPlayerTimeData data = PlayerTimeDataCapability.get(player);
                    if (data != null) {
                        data.setTotalDaysPlayed(message.totalDays);
                        data.setHasEquippedTemporalHeart(message.hasEquippedTemporalHeart);
                        data.setLastLoginTime(message.lastLoginTime);

                        if (data instanceof PlayerTimeDataImpl) {
                            ((PlayerTimeDataImpl) data).totalPlayTime = message.totalPlayTime;
                        }
                    }
                }
            });
            return null;
        }
    }
}