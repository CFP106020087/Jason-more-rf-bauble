package com.moremod.node;

import com.moremod.api.IPlaceHolder;
import com.raoulvdberge.refinedstorage.api.network.IWirelessTransmitter;
import com.raoulvdberge.refinedstorage.api.network.INetworkNodeVisitor;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNode;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class NetworkNodeCreativeWirelessTransmitter extends NetworkNode
        implements IWirelessTransmitter, IPlaceHolder {

    // ✅ 必须使用命名空间 ID，且与注册工厂和 Tile 的 getNodeId 完全一致
    public static final String ID = "moremod:creative_wireless_transmitter";

    public NetworkNodeCreativeWirelessTransmitter(World world, BlockPos pos) {
        super(world, pos);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getEnergyUsage() {
        return 0; // 创造级：无能耗
    }

    // ✅ 放宽导通：任意方向都允许，以免图构建在部分情况下失败
    @Override
    public boolean canConduct(@Nullable EnumFacing direction) {
        return direction != null;
    }

    // 仍然只向下访问 cable（与方块“必须放在电缆上方”的逻辑一致）
    @Override
    public void visit(INetworkNodeVisitor.Operator operator) {
        operator.apply(world, pos.down(), EnumFacing.UP);
    }

    @Override
    public boolean hasConnectivityState() {
        return true;
    }

    // 创造级：只要网络在&红石允许，就激活
    @Override
    public boolean canUpdate() {
        boolean ok = isEnabled() && getNetwork() != null;
        setActive(ok);
        return ok;
    }

    // ━━━━━ IWirelessTransmitter ━━━━━
    @Override
    public int getRange() {
        return Integer.MAX_VALUE; // 同维度实际等于无限
    }

    @Override
    public BlockPos getOrigin() {
        return getPos();
    }

    @Override
    public int getDimension() {
        return world.provider.getDimension();
    }

    // ━━━━━ NBT（占位，无额外字段）━━━━━
    @Override
    public void read(NBTTagCompound tag) {
        super.read(tag);
    }
    @Override
    public NBTTagCompound write(NBTTagCompound tag) {
        return super.write(tag);
    }
    @Override
    public NBTTagCompound writeConfiguration(NBTTagCompound tag) {
        return super.writeConfiguration(tag);
    }
    @Override
    public void readConfiguration(NBTTagCompound tag) {
        super.readConfiguration(tag);
    }
}
