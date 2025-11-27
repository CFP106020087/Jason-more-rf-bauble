package com.moremod.network;

import com.moremod.tile.TileEntityPurificationAltar;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 提纯祭坛 - Reroll请求网络包
 * 
 * 客户端 → 服务器
 */
public class PacketPurificationReroll implements IMessage {
    
    private BlockPos pos;
    
    public PacketPurificationReroll() {
        // 必须有无参构造函数
    }
    
    public PacketPurificationReroll(BlockPos pos) {
        this.pos = pos;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        this.pos = new BlockPos(x, y, z);
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
    }
    
    /**
     * 服务器端处理
     */
    public static class Handler implements IMessageHandler<PacketPurificationReroll, IMessage> {
        
        @Override
        public IMessage onMessage(PacketPurificationReroll message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            
            // 在主线程执行
            player.getServerWorld().addScheduledTask(() -> {
                // 检查距离
                if (player.getDistanceSq(message.pos) > 64.0) {
                    System.out.println("[PurificationAltar] 玩家距离太远");
                    return;
                }
                
                // 获取TileEntity
                TileEntity te = player.world.getTileEntity(message.pos);
                if (te instanceof TileEntityPurificationAltar) {
                    TileEntityPurificationAltar altar = (TileEntityPurificationAltar) te;
                    
                    System.out.println("[PurificationAltar] 收到Reroll请求");
                    System.out.println("[PurificationAltar] canPurify: " + altar.canPurify());
                    System.out.println("[PurificationAltar] isPurifying: " + altar.isPurifying());
                    System.out.println("[PurificationAltar] inputCount: " + altar.getInputGemCount());
                    
                    boolean result = altar.startPurifying(player);
                    System.out.println("[PurificationAltar] startPurifying result: " + result);
                } else {
                    System.out.println("[PurificationAltar] TileEntity not found at " + message.pos);
                }
            });
            
            return null;
        }
    }
}