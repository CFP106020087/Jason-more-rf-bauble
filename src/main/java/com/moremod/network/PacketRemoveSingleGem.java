package com.moremod.network;

import com.moremod.tile.TileEntitySwordUpgradeStation;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.entity.player.EntityPlayerMP;
import com.moremod.container.ContainerSwordUpgradeStation;

/**
 * 寶石拆除網路封包
 * 客戶端發送給伺服器，請求拆除指定槽位的寶石
 */
public class PacketRemoveSingleGem implements IMessage {
    private BlockPos pos;
    private int slotIndex;

    public PacketRemoveSingleGem() {}

    public PacketRemoveSingleGem(BlockPos pos, int slotIndex) {
        this.pos = pos;
        this.slotIndex = slotIndex;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        slotIndex = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeInt(slotIndex);
    }

    public static class Handler implements IMessageHandler<PacketRemoveSingleGem, IMessage> {
        @Override
        public IMessage onMessage(PacketRemoveSingleGem message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerSwordUpgradeStation) {
                    ContainerSwordUpgradeStation container = (ContainerSwordUpgradeStation) player.openContainer;

                    // 验证模式
                    if (container.getCurrentMode() == TileEntitySwordUpgradeStation.Mode.REMOVAL) {
                        // ⚠️ 修复：performRemoveSingleGem 现在是 void 方法
                        // TileEntity 的 removeSingleGem 已经处理了给玩家物品的逻辑
                        container.performRemoveSingleGem(message.slotIndex, player);

                        // 同步容器
                        container.detectAndSendChanges();
                    }
                }
            });

            return null;
        }
    }
}