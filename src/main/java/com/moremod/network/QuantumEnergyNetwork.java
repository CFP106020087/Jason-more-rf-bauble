package com.moremod.network;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 极简量子网络：
 * - 用 networkId 记录一个 FE 方块的 (pos, dim)
 * - requestEnergy / sendEnergy 直接对远端 TE 做能力调用
 * - 不做任何缓冲；区块/世界未加载就返回 0
 * - 提供 save/load 以便 WorldSavedData 持久化（可选）
 * - 兼容：提供 3参 与 4参 的 requestEnergy/sendEnergy 重载
 */
public class QuantumEnergyNetwork {

    private static final boolean DEBUG = false;

    private static class NetworkNode {
        final UUID id;
        int dimension;
        BlockPos anchorPos;
        long lastUpdate;

        NetworkNode(UUID id) { this.id = id; }
    }

    private static final Map<UUID, NetworkNode> networks = new ConcurrentHashMap<>();

    // ========= 注册 / 查询 =========

    public static void registerDirectLink(UUID networkId, BlockPos pos, int dimension) {
        NetworkNode node = networks.computeIfAbsent(networkId, NetworkNode::new);
        node.anchorPos = pos;
        node.dimension = dimension;
        node.lastUpdate = System.currentTimeMillis();
        if (DEBUG) System.out.println("[QEN] link " + networkId + " -> " + pos + " dim=" + dimension);
    }

    /** 心跳：兼容旧调用，刷新 lastUpdate */
    public static void updateNetwork(UUID networkId) {
        NetworkNode node = networks.get(networkId);
        if (node != null) {
            node.lastUpdate = System.currentTimeMillis();
            if (DEBUG) System.out.println("[QEN] heartbeat " + networkId + " at " + node.anchorPos + " dim=" + node.dimension);
        }
    }

    // ========= 拉取能量（4参 + 3参重载） =========

    /**
     * 从网络锚点（远端方块）“拉取”能量到电池
     * @param networkId 绑定ID
     * @param amount    期望拉取量
     * @param requestDim 电池所在维度
     * @param crossDimEff 跨维效率（0~1）
     */
    public static int requestEnergy(UUID networkId, int amount, int requestDim, float crossDimEff) {
        NetworkNode node = networks.get(networkId);
        if (node == null || node.anchorPos == null) return 0;

        WorldServer w = DimensionManager.getWorld(node.dimension);
        if (w == null || !w.isBlockLoaded(node.anchorPos)) return 0;

        TileEntity te = w.getTileEntity(node.anchorPos);
        if (te == null) return 0;

        IEnergyStorage src = getLinkedStorage(te);
        if (src == null || !src.canExtract()) return 0;

        int sim = src.extractEnergy(amount, true);
        if (sim <= 0) return 0;

        int extracted = src.extractEnergy(sim, false);
        if (extracted <= 0) return 0;

        if (requestDim != node.dimension) {
            extracted = Math.round(extracted * crossDimEff);
        }

        node.lastUpdate = System.currentTimeMillis();

        // 刷新方块以便灯/渲染变更
        te.markDirty();
        IBlockState st = w.getBlockState(node.anchorPos);
        w.notifyBlockUpdate(node.anchorPos, st, st, 3);

        if (DEBUG) System.out.println("[QEN] pull " + extracted + "RF from " + node.anchorPos + " dim=" + node.dimension);
        return extracted;
    }

    /** 兼容旧签名（默认跨维效率 0.9f） */
    public static int requestEnergy(UUID networkId, int amount, int requestDim) {
        return requestEnergy(networkId, amount, requestDim, 0.90f);
    }

    // ========= 推送能量（4参 + 3参重载） =========

    /**
     * 向网络锚点（远端方块）“推送”能量（反向回充）
     */
    public static int sendEnergy(UUID networkId, int amount, int senderDim, float crossDimEff) {
        NetworkNode node = networks.get(networkId);
        if (node == null || node.anchorPos == null || amount <= 0) return 0;

        WorldServer w = DimensionManager.getWorld(node.dimension);
        if (w == null || !w.isBlockLoaded(node.anchorPos)) return 0;

        TileEntity te = w.getTileEntity(node.anchorPos);
        if (te == null) return 0;

        IEnergyStorage dst = getLinkedStorage(te);
        if (dst == null || !dst.canReceive()) return 0;

        if (senderDim != node.dimension) {
            amount = Math.round(amount * crossDimEff);
        }

        int accepted = dst.receiveEnergy(amount, false);
        if (accepted <= 0) return 0;

        node.lastUpdate = System.currentTimeMillis();

        te.markDirty();
        IBlockState st = w.getBlockState(node.anchorPos);
        w.notifyBlockUpdate(node.anchorPos, st, st, 3);

        if (DEBUG) System.out.println("[QEN] push " + accepted + "RF to " + node.anchorPos + " dim=" + node.dimension);
        return accepted;
    }

    /** 兼容旧签名（默认跨维效率 0.9f） */
    public static int sendEnergy(UUID networkId, int amount, int senderDim) {
        return sendEnergy(networkId, amount, senderDim, 0.90f);
    }

    // ========= 查询远端能量 =========

    /**
     * 估算远端方块当前能量（仅用于 Tooltip 展示；世界/区块未加载返回 -1）
     */
    public static int getNetworkPower(UUID networkId) {
        NetworkNode node = networks.get(networkId);
        if (node == null || node.anchorPos == null) return -1;

        WorldServer w = DimensionManager.getWorld(node.dimension);
        if (w == null || !w.isBlockLoaded(node.anchorPos)) return -1;

        TileEntity te = w.getTileEntity(node.anchorPos);
        if (te == null) return -1;

        IEnergyStorage es = getLinkedStorage(te);
        return (es == null) ? -1 : es.getEnergyStored();
    }

    public static void cleanupNetworks(long maxIdleMillis) {
        long now = System.currentTimeMillis();
        networks.entrySet().removeIf(e -> (now - e.getValue().lastUpdate) > maxIdleMillis);
    }

    // ========= 能力获取（先 null 面，后各面） =========
    private static IEnergyStorage getLinkedStorage(TileEntity te) {
        IEnergyStorage es = te.getCapability(CapabilityEnergy.ENERGY, null);
        if (es != null) return es;
        for (EnumFacing f : EnumFacing.VALUES) {
            es = te.getCapability(CapabilityEnergy.ENERGY, f);
            if (es != null) return es;
        }
        return null;
    }

    // ========= 持久化（可选：配合 WorldSavedData 调用） =========

    public static NBTTagCompound saveNetworkData() {
        NBTTagCompound out = new NBTTagCompound();
        NBTTagCompound map = new NBTTagCompound();

        for (Map.Entry<UUID, NetworkNode> e : networks.entrySet()) {
            NetworkNode n = e.getValue();
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Dim", n.dimension);
            tag.setLong("Pos", n.anchorPos == null ? 0L : n.anchorPos.toLong());
            tag.setLong("LU", n.lastUpdate);
            map.setTag(e.getKey().toString(), tag);
        }
        out.setTag("Nets", map);
        return out;
    }

    public static void loadNetworkData(NBTTagCompound nbt) {
        networks.clear();
        if (nbt == null || !nbt.hasKey("Nets")) return;

        NBTTagCompound map = nbt.getCompoundTag("Nets");
        for (String k : map.getKeySet()) {
            try {
                UUID id = UUID.fromString(k);
                NBTTagCompound tag = map.getCompoundTag(k);
                NetworkNode node = new NetworkNode(id);
                node.dimension = tag.getInteger("Dim");
                long posLong = tag.getLong("Pos");
                node.anchorPos = posLong == 0L ? null : BlockPos.fromLong(posLong);
                node.lastUpdate = tag.getLong("LU");
                if (node.anchorPos != null) networks.put(id, node);
            } catch (Throwable ignored) {}
        }
    }
}
