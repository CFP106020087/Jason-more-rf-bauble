package com.moremod.network;

import com.moremod.client.overlay.BrokenGodAscensionOverlay;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 升格动画触发包 (服务端 -> 客户端)
 * Ascension Animation Trigger Packet (Server -> Client)
 *
 * 当玩家升格为破碎之神时，服务端发送此包触发客户端的动画效果
 */
public class PacketAscensionAnimation implements IMessage {

    public PacketAscensionAnimation() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // 无额外数据
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 无额外数据
    }

    public static class Handler implements IMessageHandler<PacketAscensionAnimation, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketAscensionAnimation message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                // 触发升格动画
                BrokenGodAscensionOverlay.startAnimation();
            });
            return null;
        }
    }
}
