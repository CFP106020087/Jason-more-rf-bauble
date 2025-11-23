package com.moremod.network;

import com.moremod.container.ContainerSwordUpgradeStation;
import com.moremod.tile.TileEntitySwordUpgradeStation;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 网络封包类 - 剑升级站操作
 *
 * 包含三个封包类：
 * 1. PacketStarUpgrade - 统一升级（箭头按钮，升级模式）
 * 2. PacketRemoveAllGems - 拆除所有宝石（箭头按钮，拆除模式）
 * 3. PacketRemoveSingleGem - 拆除单个宝石（右键材料槽）
 */

// ==================== 统一升级封包 ====================

/**
 * 封包：统一升级（箭头按钮，升级模式）
 */

// ==================== 拆除所有宝石封包 ====================

/**
 * 封包：拆除所有宝石（箭头按钮，拆除模式）
 */
public class PacketRemoveAllGems implements IMessage {
    private BlockPos pos;

    public PacketRemoveAllGems() {}

    public PacketRemoveAllGems(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
    }

    public static class Handler implements IMessageHandler<PacketRemoveAllGems, IMessage> {
        @Override
        public IMessage onMessage(PacketRemoveAllGems message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;

            FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerSwordUpgradeStation) {
                    ContainerSwordUpgradeStation container = (ContainerSwordUpgradeStation) player.openContainer;

                    // 验证模式
                    if (container.getCurrentMode() == TileEntitySwordUpgradeStation.Mode.REMOVAL
                            && container.canPerformRemoveAll()) {
                        container.performRemoveAll(player);

                        // 同步容器
                        player.inventoryContainer.detectAndSendChanges();
                    }
                }
            });

            return null;
        }
    }
}

// ==================== 拆除单个宝石封包 ====================

/**
 * 封包：拆除单个宝石（右键点击材料槽）
 */
