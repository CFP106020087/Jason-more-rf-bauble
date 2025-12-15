package com.moremod.network;

import com.moremod.capability.ChengYueCapability;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 澄月技能同步封包 - 从服务端发送到客户端
 */
public class PacketChengYueSkill implements IMessage {

    private int skillType;

    public PacketChengYueSkill() {}

    public PacketChengYueSkill(int skillType) {
        this.skillType = skillType;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        skillType = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(skillType);
    }

    public static class Handler implements IMessageHandler<PacketChengYueSkill, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketChengYueSkill message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player != null) {
                    ChengYueCapability cap = player.getCapability(ChengYueCapability.CAPABILITY, null);
                    if (cap != null) {
                        cap.activateSkill(message.skillType);
                        System.out.println("[ChengYue] 客户端收到技能包: skillType=" + message.skillType);
                    } else {
                        System.out.println("[ChengYue] 客户端警告: cap 为 null!");
                    }
                }
            });
            return null;
        }
    }
}
