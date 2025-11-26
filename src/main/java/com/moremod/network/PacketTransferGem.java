package com.moremod.network;

import com.moremod.container.ContainerTransferStation;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 客户端 → 服务端：请求执行转移操作
 */
public class PacketTransferGem implements IMessage {
    
    public PacketTransferGem() {
        // 无参构造函数（必须）
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        // 这个包不需要传输数据
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        // 这个包不需要传输数据
    }
    
    public static class Handler implements IMessageHandler<PacketTransferGem, IMessage> {
        
        @Override
        public IMessage onMessage(PacketTransferGem message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            
            // 在主线程执行
            player.getServerWorld().addScheduledTask(() -> {
                // 检查玩家当前打开的容器
                if (player.openContainer instanceof ContainerTransferStation) {
                    ContainerTransferStation container = 
                        (ContainerTransferStation) player.openContainer;
                    
                    // 执行转移
                    boolean success = container.performTransfer(player);
                    
                    if (success) {
                        // 可以发送成功消息给客户端（可选）
                        player.sendMessage(new net.minecraft.util.text.TextComponentString(
                            "§a词条转移成功！"
                        ));
                    } else {
                        // 发送失败消息
                        String error = container.getErrorMessage();
                        if (!error.isEmpty()) {
                            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                                "§c转移失败: " + error
                            ));
                        }
                    }
                }
            });
            
            return null;
        }
    }
}