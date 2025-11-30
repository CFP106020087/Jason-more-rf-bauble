package com.moremod.network;

import com.moremod.item.shambhala.ItemShambhalaVeil;
import com.moremod.system.ascension.ShambhalaHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 香巴拉宁静光环技能的数据包
 * 当玩家按下技能键时从客户端发送到服务器
 */
public class PacketShambhalaPeaceAura implements IMessage {

    public PacketShambhalaPeaceAura() {}

    @Override
    public void fromBytes(ByteBuf buf) {
        // 不需要参数
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 不需要参数
    }

    public static class Handler implements IMessageHandler<PacketShambhalaPeaceAura, IMessage> {

        @Override
        public IMessage onMessage(PacketShambhalaPeaceAura message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            player.getServerWorld().addScheduledTask(() -> {
                try {
                    // 检查玩家是否是香巴拉
                    if (!ShambhalaHandler.isShambhala(player)) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.RED + "只有香巴拉升格者才能使用宁静光环！"
                        ), true);
                        return;
                    }

                    // 执行宁静光环技能
                    ItemShambhalaVeil.activatePeaceAura(player);

                } catch (Exception e) {
                    System.err.println("[moremod] 宁静光环技能出错: " + e.getMessage());
                    e.printStackTrace();
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.RED + "技能执行失败：内部错误"
                    ), true);
                }
            });

            return null;
        }
    }
}
