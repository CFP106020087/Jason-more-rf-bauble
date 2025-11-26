package com.moremod.network;

import com.moremod.item.VillagerProfessionTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUpdateVillagerTransformer implements IMessage {

    private String profession;
    private boolean isMainHand;

    public PacketUpdateVillagerTransformer() {}

    public PacketUpdateVillagerTransformer(String profession, boolean isMainHand) {
        this.profession = profession;
        this.isMainHand = isMainHand;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.profession = ByteBufUtils.readUTF8String(buf);
        this.isMainHand = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, profession);
        buf.writeBoolean(isMainHand);
    }

    public static class Handler implements IMessageHandler<PacketUpdateVillagerTransformer, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateVillagerTransformer message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack stack = player.getHeldItem(message.isMainHand ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND);
                if (stack.getItem() instanceof VillagerProfessionTool) {
                    VillagerProfessionTool.setSelectedProfession(stack, message.profession);
                }
            });
            return null;
        }
    }
}