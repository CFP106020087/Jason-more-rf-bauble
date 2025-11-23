package com.moremod.network;

import com.moremod.tile.TileEntityItemTransporter;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 物品传输器配置同步包
 * 客户端 → 服务端
 */
public class PacketTransporterConfig implements IMessage {

    private BlockPos pos;
    private int pullSlotStart;
    private int pullSlotEnd;
    private int pushSlotStart;
    private int pushSlotEnd;
    private int pullSideIndex;
    private int pushSideIndex;
    private boolean isWhitelist;
    private boolean respectMeta;
    private boolean respectNBT;
    private boolean respectMod;
    private int respectOredict;
    private boolean redstoneControlled;

    // 必须有无参构造函数
    public PacketTransporterConfig() {}

    public PacketTransporterConfig(TileEntityItemTransporter tile) {
        this.pos = tile.getPos();
        this.pullSlotStart = tile.pullSlotStart;
        this.pullSlotEnd = tile.pullSlotEnd;
        this.pushSlotStart = tile.pushSlotStart;
        this.pushSlotEnd = tile.pushSlotEnd;
        this.pullSideIndex = tile.pullSide.getIndex();
        this.pushSideIndex = tile.pushSide.getIndex();
        this.isWhitelist = tile.isWhitelist;
        this.respectMeta = tile.respectMeta;
        this.respectNBT = tile.respectNBT;
        this.respectMod = tile.respectMod;
        this.respectOredict = tile.respectOredict;
        this.redstoneControlled = tile.redstoneControlled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // 读取位置
        this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());

        // 读取槽位配置
        this.pullSlotStart = buf.readInt();
        this.pullSlotEnd = buf.readInt();
        this.pushSlotStart = buf.readInt();
        this.pushSlotEnd = buf.readInt();

        // 读取方向
        this.pullSideIndex = buf.readInt();
        this.pushSideIndex = buf.readInt();

        // 读取过滤设置
        this.isWhitelist = buf.readBoolean();
        this.respectMeta = buf.readBoolean();
        this.respectNBT = buf.readBoolean();
        this.respectMod = buf.readBoolean();
        this.respectOredict = buf.readInt();
        this.redstoneControlled = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 写入位置
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());

        // 写入槽位配置
        buf.writeInt(pullSlotStart);
        buf.writeInt(pullSlotEnd);
        buf.writeInt(pushSlotStart);
        buf.writeInt(pushSlotEnd);

        // 写入方向
        buf.writeInt(pullSideIndex);
        buf.writeInt(pushSideIndex);

        // 写入过滤设置
        buf.writeBoolean(isWhitelist);
        buf.writeBoolean(respectMeta);
        buf.writeBoolean(respectNBT);
        buf.writeBoolean(respectMod);
        buf.writeInt(respectOredict);
        buf.writeBoolean(redstoneControlled);
    }

    /**
     * 处理器：服务端接收包并更新TileEntity
     */
    public static class Handler implements IMessageHandler<PacketTransporterConfig, IMessage> {
        @Override
        public IMessage onMessage(PacketTransporterConfig message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            // 必须在主线程执行
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity te = player.world.getTileEntity(message.pos);

                if (te instanceof TileEntityItemTransporter) {
                    TileEntityItemTransporter tile = (TileEntityItemTransporter) te;

                    // 更新槽位配置
                    tile.pullSlotStart = message.pullSlotStart;
                    tile.pullSlotEnd = message.pullSlotEnd;
                    tile.pushSlotStart = message.pushSlotStart;
                    tile.pushSlotEnd = message.pushSlotEnd;

                    // 更新方向
                    tile.pullSide = net.minecraft.util.EnumFacing.byIndex(message.pullSideIndex);
                    tile.pushSide = net.minecraft.util.EnumFacing.byIndex(message.pushSideIndex);

                    // 更新过滤设置
                    tile.isWhitelist = message.isWhitelist;
                    tile.respectMeta = message.respectMeta;
                    tile.respectNBT = message.respectNBT;
                    tile.respectMod = message.respectMod;
                    tile.respectOredict = message.respectOredict;
                    tile.redstoneControlled = message.redstoneControlled;

                    // 标记需要保存
                    tile.markDirty();

                    System.out.println("[MoreMod] 服务端已更新传输器配置: " + message.pos);
                }
            });

            return null; // 不需要回复
        }
    }
}