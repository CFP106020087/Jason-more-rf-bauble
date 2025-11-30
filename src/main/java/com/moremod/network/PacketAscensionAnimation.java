package com.moremod.network;

import com.moremod.client.overlay.BrokenGodAscensionOverlay;
import com.moremod.client.overlay.ShambhalaAscensionOverlay;
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
 * 当玩家升格时，服务端发送此包触发客户端的动画效果
 * 支持两种升格路线：破碎之神、香巴拉
 */
public class PacketAscensionAnimation implements IMessage {

    /** 升格类型: 0 = 破碎之神, 1 = 香巴拉 */
    private int ascensionType;

    public PacketAscensionAnimation() {
        this.ascensionType = 0;
    }

    public PacketAscensionAnimation(int type) {
        this.ascensionType = type;
    }

    /** 创建破碎之神升格动画包 */
    public static PacketAscensionAnimation brokenGod() {
        return new PacketAscensionAnimation(0);
    }

    /** 创建香巴拉升格动画包 */
    public static PacketAscensionAnimation shambhala() {
        return new PacketAscensionAnimation(1);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.ascensionType = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.ascensionType);
    }

    public static class Handler implements IMessageHandler<PacketAscensionAnimation, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketAscensionAnimation message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                // 根据升格类型触发对应动画
                if (message.ascensionType == 1) {
                    ShambhalaAscensionOverlay.startAnimation();
                } else {
                    BrokenGodAscensionOverlay.startAnimation();
                }
            });
            return null;
        }
    }
}
