package com.moremod.mixin;

import com.moremod.api.IPlaceHolder;
import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.network.IWirelessTransmitter;
import com.raoulvdberge.refinedstorage.api.network.item.INetworkItem;
import com.raoulvdberge.refinedstorage.api.network.item.INetworkItemHandler;
import com.raoulvdberge.refinedstorage.api.network.item.INetworkItemProvider;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import com.raoulvdberge.refinedstorage.apiimpl.network.item.NetworkItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;

@Mixin(value = NetworkItemHandler.class, remap = false, priority = 1001)
public abstract class MixinNetworkItemHandler implements INetworkItemHandler {

    @Shadow @Final private INetwork network;
    @Shadow @Final private Map<EntityPlayer, INetworkItem> items;

    @Shadow public abstract void close(EntityPlayer player);
    @Shadow public abstract INetworkItem getItem(EntityPlayer player);
    @Shadow public abstract void drainEnergy(EntityPlayer player, int energy);

    /**
     * 放宽范围判定：同维度按距离检查；异维度只要有实现 IPlaceHolder 的发射器节点即放行
     */
    @Overwrite
    public void open(EntityPlayer player, ItemStack stack, int slotId) {
        if (player.world.isRemote) return;

        if (!(stack.getItem() instanceof INetworkItemProvider)) {
            player.sendMessage(new TextComponentTranslation("misc.refinedstorage:network_item.out_of_range"));
            return;
        }

        boolean inRange = computeInRangeForOpen(player);
        if (!inRange) {
            player.sendMessage(new TextComponentTranslation("misc.refinedstorage:network_item.out_of_range"));
            return;
        }

        INetworkItem item = ((INetworkItemProvider) stack.getItem()).provide(this, player, stack, slotId);
        if (item.onOpen(network)) {
            items.put(player, item);
        }
    }

    @Unique
    private boolean computeInRangeForOpen(EntityPlayer player) {
        if (network == null || network.getNodeGraph() == null) return false;

        for (INetworkNode node : network.getNodeGraph().all()) {
            if (!(node instanceof IWirelessTransmitter)) continue;
            if (!node.canUpdate()) continue;

            IWirelessTransmitter tx = (IWirelessTransmitter) node;

            if (tx.getDimension() == player.dimension) {
                // 用平方距离比较，避免 sqrt
                double dx = (tx.getOrigin().getX() + 0.5D) - player.posX;
                double dy = (tx.getOrigin().getY() + 0.5D) - player.posY;
                double dz = (tx.getOrigin().getZ() + 0.5D) - player.posZ;
                double distSq = dx * dx + dy * dy + dz * dz;
                double r = tx.getRange();
                if (distSq <= r * r) return true;
            } else if (node instanceof IPlaceHolder) {
                // ✨ 跨维度：只要存在 IPlaceHolder 标记的发射器，就算在范围
                return true;
            }
        }
        return false;
    }
}
