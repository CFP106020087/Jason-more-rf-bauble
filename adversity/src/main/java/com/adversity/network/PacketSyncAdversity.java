package com.adversity.network;

import com.adversity.client.ClientAdversityCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步怪物难度数据包
 * 从服务端发送到客户端，用于渲染词条信息
 */
public class PacketSyncAdversity implements IMessage {

    private int entityId;
    private int tier;
    private float difficultyLevel;
    private float healthMultiplier;
    private float damageMultiplier;
    private List<String> affixIds;

    public PacketSyncAdversity() {
        this.affixIds = new ArrayList<>();
    }

    public PacketSyncAdversity(int entityId, int tier, float difficultyLevel,
                               float healthMultiplier, float damageMultiplier,
                               List<ResourceLocation> affixes) {
        this.entityId = entityId;
        this.tier = tier;
        this.difficultyLevel = difficultyLevel;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.affixIds = new ArrayList<>();
        for (ResourceLocation loc : affixes) {
            this.affixIds.add(loc.toString());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        tier = buf.readInt();
        difficultyLevel = buf.readFloat();
        healthMultiplier = buf.readFloat();
        damageMultiplier = buf.readFloat();

        int count = buf.readInt();
        affixIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            affixIds.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(tier);
        buf.writeFloat(difficultyLevel);
        buf.writeFloat(healthMultiplier);
        buf.writeFloat(damageMultiplier);

        buf.writeInt(affixIds.size());
        for (String id : affixIds) {
            ByteBufUtils.writeUTF8String(buf, id);
        }
    }

    /**
     * 客户端消息处理器
     */
    public static class Handler implements IMessageHandler<PacketSyncAdversity, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncAdversity message, MessageContext ctx) {
            // 在主线程执行
            Minecraft.getMinecraft().addScheduledTask(() -> handleMessage(message));
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleMessage(PacketSyncAdversity message) {
            // 将数据存储到客户端缓存
            List<ResourceLocation> affixes = new ArrayList<>();
            for (String id : message.affixIds) {
                affixes.add(new ResourceLocation(id));
            }

            ClientAdversityCache.updateEntity(
                message.entityId,
                message.tier,
                message.difficultyLevel,
                message.healthMultiplier,
                message.damageMultiplier,
                affixes
            );
        }
    }
}
