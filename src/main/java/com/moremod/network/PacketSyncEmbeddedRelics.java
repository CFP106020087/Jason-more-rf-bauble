package com.moremod.network;

import com.moremod.entity.curse.EmbeddedCurseManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashSet;
import java.util.Set;

/**
 * 七圣遗物嵌入数据同步包 (服务端 -> 客户端)
 * Sacred Relic Embedded Data Sync Packet (Server -> Client)
 *
 * 用于在维度切换、登录、嵌入/移除遗物时同步数据到客户端
 */
public class PacketSyncEmbeddedRelics implements IMessage {

    private Set<String> embeddedRelicIds;

    public PacketSyncEmbeddedRelics() {
        this.embeddedRelicIds = new HashSet<>();
    }

    public PacketSyncEmbeddedRelics(Set<EmbeddedCurseManager.EmbeddedRelicType> relics) {
        this.embeddedRelicIds = new HashSet<>();
        for (EmbeddedCurseManager.EmbeddedRelicType type : relics) {
            this.embeddedRelicIds.add(type.getId());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        embeddedRelicIds.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            embeddedRelicIds.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(embeddedRelicIds.size());
        for (String id : embeddedRelicIds) {
            ByteBufUtils.writeUTF8String(buf, id);
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncEmbeddedRelics, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncEmbeddedRelics message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player == null) return;

                // 更新客户端缓存
                EmbeddedCurseManager.updateClientCache(player, message.embeddedRelicIds);
            });
            return null;
        }
    }
}
