package com.moremod.network;

import baubles.api.BaublesApi;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageSyncJetpackTagToClient implements IMessage {

    private int slot;
    private ItemStack stack;

    public MessageSyncJetpackTagToClient() {}

    public MessageSyncJetpackTagToClient(int slot, ItemStack stack) {
        this.slot = slot;
        this.stack = stack.copy(); // 防止引用修改
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.slot = buf.readInt();
        this.stack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.slot);
        ByteBufUtils.writeItemStack(buf, this.stack);
    }

    public static class Handler implements IMessageHandler<MessageSyncJetpackTagToClient, IMessage> {
        @SideOnly(Side.CLIENT)
        @Override
        public IMessage onMessage(MessageSyncJetpackTagToClient message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player == null) return;

                if (message.slot >= 0 && message.slot < BaublesApi.getBaublesHandler(player).getSlots()) {
                    BaublesApi.getBaublesHandler(player).setStackInSlot(message.slot, message.stack);
                }
            });
            return null;
        }
    }
}
