package com.moremod.synergy.network;

import com.moremod.synergy.data.PlayerSynergyData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 网络包：切换 Synergy 激活状态
 *
 * 说明：
 * - 客户端发送到服务端
 * - 切换指定 Synergy 的激活状态
 * - 服务端验证后更新玩家数据
 *
 * 注册方式：
 * 在你的网络通道注册代码中添加：
 * <pre>
 * INSTANCE.registerMessage(
 *     PacketToggleSynergy.Handler.class,
 *     PacketToggleSynergy.class,
 *     nextId++,
 *     Side.SERVER
 * );
 * </pre>
 */
public class PacketToggleSynergy implements IMessage {

    private String synergyId;

    // 无参构造器（必须）
    public PacketToggleSynergy() {
    }

    public PacketToggleSynergy(String synergyId) {
        this.synergyId = synergyId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.synergyId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, synergyId);
    }

    public static class Handler implements IMessageHandler<PacketToggleSynergy, IMessage> {
        @Override
        public IMessage onMessage(PacketToggleSynergy message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // 在服务端线程执行
            player.getServerWorld().addScheduledTask(() -> {
                PlayerSynergyData data = PlayerSynergyData.get(player);

                // 切换激活状态
                if (data.isSynergyActivated(message.synergyId)) {
                    data.deactivateSynergy(message.synergyId);
                } else {
                    data.activateSynergy(message.synergyId);
                }

                // 保存数据
                data.saveToPlayer(player);
            });

            return null;
        }
    }
}
