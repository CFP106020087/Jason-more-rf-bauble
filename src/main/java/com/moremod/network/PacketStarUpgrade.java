package com.moremod.network;

import com.moremod.container.ContainerSwordUpgradeStation;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 星形升级封包 - 箭头按钮点击触发
 * 
 * 客户端 -> 服务器
 * 触发统一升级所有材料
 */
public class PacketStarUpgrade implements IMessage {

    private BlockPos pos;

    // 无参构造函数（网络需要）
    public PacketStarUpgrade() {}

    public PacketStarUpgrade(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.pos.toLong());
    }

    public static class Handler implements IMessageHandler<PacketStarUpgrade, IMessage> {
        @Override
        public IMessage onMessage(PacketStarUpgrade message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            
            // 在主线程中处理
            player.getServerWorld().addScheduledTask(() -> {
                // 检查玩家是否打开了正确的容器
                if (player.openContainer instanceof ContainerSwordUpgradeStation) {
                    ContainerSwordUpgradeStation container = (ContainerSwordUpgradeStation) player.openContainer;
                    
                    // 验证位置匹配（防止作弊）
                    if (container.getTile().getPos().equals(message.pos)) {
                        // 执行统一升级
                        container.performStarUpgrade();
                        
                        // 播放音效
                        player.world.playSound(
                            null,
                            message.pos,
                            net.minecraft.init.SoundEvents.BLOCK_ANVIL_USE,
                            net.minecraft.util.SoundCategory.BLOCKS,
                            1.0F,
                            1.0F
                        );
                    }
                }
            });
            
            return null;
        }
    }
}
