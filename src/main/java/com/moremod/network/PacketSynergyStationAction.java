package com.moremod.network;

import com.moremod.synergy.station.TileEntitySynergyStation;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Synergy 链结站操作网络包 (Client -> Server)
 *
 * 用于同步客户端 GUI 的操作到服务器
 */
public class PacketSynergyStationAction implements IMessage {

    public enum ActionType {
        SET_SLOT,      // 设置槽位模块
        CLEAR_SLOT,    // 清空槽位
        CLEAR_ALL,     // 清空所有
        TOGGLE_ACTIVE  // 切换激活状态
    }

    private BlockPos pos;
    private ActionType action;
    private int slot;
    private String moduleId;

    public PacketSynergyStationAction() {
    }

    public PacketSynergyStationAction(BlockPos pos, ActionType action) {
        this.pos = pos;
        this.action = action;
        this.slot = -1;
        this.moduleId = "";
    }

    public PacketSynergyStationAction(BlockPos pos, ActionType action, int slot) {
        this.pos = pos;
        this.action = action;
        this.slot = slot;
        this.moduleId = "";
    }

    public PacketSynergyStationAction(BlockPos pos, ActionType action, int slot, String moduleId) {
        this.pos = pos;
        this.action = action;
        this.slot = slot;
        this.moduleId = moduleId != null ? moduleId : "";
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        action = ActionType.values()[buf.readByte()];
        slot = buf.readByte();
        moduleId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeByte(action.ordinal());
        buf.writeByte(slot);
        ByteBufUtils.writeUTF8String(buf, moduleId);
    }

    public static class Handler implements IMessageHandler<PacketSynergyStationAction, IMessage> {
        @Override
        public IMessage onMessage(PacketSynergyStationAction message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            player.getServerWorld().addScheduledTask(() -> {
                // 验证距离
                if (player.getDistanceSq(message.pos) > 64.0) {
                    return;
                }

                TileEntity te = player.world.getTileEntity(message.pos);
                if (!(te instanceof TileEntitySynergyStation)) {
                    return;
                }

                TileEntitySynergyStation station = (TileEntitySynergyStation) te;

                switch (message.action) {
                    case SET_SLOT:
                        if (message.slot >= 0 && message.slot < TileEntitySynergyStation.LINK_SLOT_COUNT) {
                            station.setModuleInSlot(message.slot, message.moduleId);
                        }
                        break;

                    case CLEAR_SLOT:
                        if (message.slot >= 0 && message.slot < TileEntitySynergyStation.LINK_SLOT_COUNT) {
                            station.clearSlot(message.slot);
                        }
                        break;

                    case CLEAR_ALL:
                        station.clearAllSlots();
                        break;

                    case TOGGLE_ACTIVE:
                        station.toggleActivated();
                        break;
                }
            });

            return null;
        }
    }
}
