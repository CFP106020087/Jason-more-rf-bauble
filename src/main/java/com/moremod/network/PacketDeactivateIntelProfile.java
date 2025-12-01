package com.moremod.network;

import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 卸除情报档案网络包
 * Deactivate Intel Profile Packet
 *
 * 客户端 -> 服务端
 * 请求卸除指定的情报档案，释放槽位
 */
public class PacketDeactivateIntelProfile implements IMessage {

    private String entityIdString;

    public PacketDeactivateIntelProfile() {}

    public PacketDeactivateIntelProfile(ResourceLocation entityId) {
        this.entityIdString = entityId.toString();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityIdString = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, entityIdString);
    }

    public static class Handler implements IMessageHandler<PacketDeactivateIntelProfile, IMessage> {

        @Override
        public IMessage onMessage(PacketDeactivateIntelProfile message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // 在主线程执行
            player.getServerWorld().addScheduledTask(() -> {
                IHumanityData data = HumanityCapabilityHandler.getData(player);
                if (data == null) return;

                ResourceLocation entityId = new ResourceLocation(message.entityIdString);

                // 检查是否已激活
                if (data.getActiveProfiles().contains(entityId)) {
                    // 卸除档案
                    data.deactivateProfile(entityId);

                    // 同步数据到客户端
                    HumanityCapabilityHandler.syncToClient(player);

                    System.out.println("[IntelSystem] " + player.getName() + " 卸除了情报档案: " + entityId);
                }
            });

            return null;
        }
    }
}
