package com.moremod.tile;

import com.moremod.node.NetworkNodeCreativeWirelessTransmitter;
import com.raoulvdberge.refinedstorage.tile.TileNode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class TileCreativeWirelessTransmitter
        extends TileNode<NetworkNodeCreativeWirelessTransmitter> {

    @Override
    @Nonnull
    public NetworkNodeCreativeWirelessTransmitter createNode(World world, BlockPos pos) {
        return new NetworkNodeCreativeWirelessTransmitter(world, pos);
    }

    @Override
    public String getNodeId() {
        // ✅ 必须与 Node.ID 完全一致
        return NetworkNodeCreativeWirelessTransmitter.ID;
    }

    @Nonnull
    public NetworkNodeCreativeWirelessTransmitter getNode() {
        return super.getNode();
    }
}
