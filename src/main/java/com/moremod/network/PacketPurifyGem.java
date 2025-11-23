package com.moremod.network;

import com.moremod.container.ContainerPurificationAltar;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 网络包：提纯宝石
 * 
 * 客户端发送到服务器，触发提纯操作
 */
public class PacketPurifyGem implements IMessage {
    
    public PacketPurifyGem() {
        // 无参构造函数（必须）
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        // 没有数据需要读取
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        // 没有数据需要写入
    }
    
    /**
     * 消息处理器
     */
    public static class Handler implements IMessageHandler<PacketPurifyGem, IMessage> {
        
        @Override
        public IMessage onMessage(PacketPurifyGem message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            
            // 在主线程执行
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerPurificationAltar) {
                    ContainerPurificationAltar container = 
                        (ContainerPurificationAltar) player.openContainer;
                    
                    // 开始提纯
                    boolean success = container.startPurifying();
                    
                    if (!success) {
                        // 提纯失败（经验不足、条件不满足等）
                        // 可以发送失败消息给客户端
                    }
                }
            });
            
            return null;
        }
    }
}