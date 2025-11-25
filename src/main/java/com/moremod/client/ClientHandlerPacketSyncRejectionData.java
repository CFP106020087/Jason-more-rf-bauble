package com.moremod.client;

import com.moremod.network.PacketSyncRejectionData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientHandlerPacketSyncRejectionData implements IMessageHandler<PacketSyncRejectionData, IMessage> {

    @Override
    public IMessage onMessage(PacketSyncRejectionData msg, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            NBTTagCompound nbt = msg.getData();
            if (nbt != null) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.player != null) {
                    mc.player.getEntityData()
                        .getCompoundTag("MoreMod_RejectionData")
                        .merge(nbt);
                }
            }
        });
        return null;
    }
}
