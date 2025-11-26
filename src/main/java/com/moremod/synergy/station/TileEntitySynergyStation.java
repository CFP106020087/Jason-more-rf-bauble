package com.moremod.synergy.station;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Synergy 链结站 TileEntity
 *
 * 存储玩家配置的模块链结。
 * 链结配置存储在 TileEntity 中，而不是玩家数据中。
 *
 * 数据结构：
 * - 最多支持 6 个链结槽位
 * - 每个槽位可以放置一个模块 ID
 * - 当两个或更多槽位有模块时，形成链结
 */
public class TileEntitySynergyStation extends TileEntity {

    // ==================== 常量 ====================

    /** 链结槽位数量 */
    public static final int LINK_SLOT_COUNT = 6;

    // ==================== 存储 ====================

    /** 链结槽位中的模块 ID */
    private final String[] linkSlots = new String[LINK_SLOT_COUNT];

    /** 当前链结配置的名称 */
    private String configName = "";

    /** 是否已激活链结 */
    private boolean activated = false;

    // ==================== 构造 ====================

    public TileEntitySynergyStation() {
        // 初始化槽位为空字符串
        Arrays.fill(linkSlots, "");
    }

    // ==================== 槽位操作 ====================

    /**
     * 获取槽位中的模块 ID
     */
    public String getModuleInSlot(int slot) {
        if (slot < 0 || slot >= LINK_SLOT_COUNT) return "";
        return linkSlots[slot] != null ? linkSlots[slot] : "";
    }

    /**
     * 设置槽位中的模块
     */
    public void setModuleInSlot(int slot, String moduleId) {
        if (slot < 0 || slot >= LINK_SLOT_COUNT) return;
        linkSlots[slot] = moduleId != null ? moduleId : "";
        markDirty();
        syncToClient();
    }

    /**
     * 清空指定槽位
     */
    public void clearSlot(int slot) {
        setModuleInSlot(slot, "");
    }

    /**
     * 清空所有槽位
     */
    public void clearAllSlots() {
        Arrays.fill(linkSlots, "");
        activated = false;
        markDirty();
        syncToClient();
    }

    /**
     * 获取所有已放置的模块 ID（非空）
     */
    public List<String> getLinkedModules() {
        List<String> modules = new ArrayList<>();
        for (String moduleId : linkSlots) {
            if (moduleId != null && !moduleId.isEmpty()) {
                modules.add(moduleId);
            }
        }
        return modules;
    }

    /**
     * 获取链结模块数量
     */
    public int getLinkedModuleCount() {
        int count = 0;
        for (String moduleId : linkSlots) {
            if (moduleId != null && !moduleId.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 检查是否可以形成有效链结（至少2个模块）
     */
    public boolean hasValidLink() {
        return getLinkedModuleCount() >= 2;
    }

    /**
     * 检查槽位是否为空
     */
    public boolean isSlotEmpty(int slot) {
        if (slot < 0 || slot >= LINK_SLOT_COUNT) return true;
        return linkSlots[slot] == null || linkSlots[slot].isEmpty();
    }

    /**
     * 查找第一个空槽位，如果没有返回 -1
     */
    public int findFirstEmptySlot() {
        for (int i = 0; i < LINK_SLOT_COUNT; i++) {
            if (isSlotEmpty(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 检查模块是否已在任何槽位中
     */
    public boolean containsModule(String moduleId) {
        if (moduleId == null || moduleId.isEmpty()) return false;
        for (String slot : linkSlots) {
            if (moduleId.equals(slot)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 配置管理 ====================

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String name) {
        this.configName = name != null ? name : "";
        markDirty();
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
        markDirty();
        syncToClient();
    }

    /**
     * 切换激活状态
     */
    public void toggleActivated() {
        setActivated(!activated);
    }

    // ==================== 玩家交互 ====================

    public boolean isUsableByPlayer(EntityPlayer player) {
        if (world == null) return false;
        if (world.getTileEntity(pos) != this) return false;
        return player.getDistanceSq(
                (double) pos.getX() + 0.5D,
                (double) pos.getY() + 0.5D,
                (double) pos.getZ() + 0.5D
        ) <= 64.0D;
    }

    // ==================== NBT ====================

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        // 保存槽位
        NBTTagList slotList = new NBTTagList();
        for (int i = 0; i < LINK_SLOT_COUNT; i++) {
            slotList.appendTag(new NBTTagString(linkSlots[i] != null ? linkSlots[i] : ""));
        }
        compound.setTag("LinkSlots", slotList);

        // 保存配置名称
        compound.setString("ConfigName", configName);

        // 保存激活状态
        compound.setBoolean("Activated", activated);

        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        // 读取槽位
        if (compound.hasKey("LinkSlots", Constants.NBT.TAG_LIST)) {
            NBTTagList slotList = compound.getTagList("LinkSlots", Constants.NBT.TAG_STRING);
            for (int i = 0; i < Math.min(slotList.tagCount(), LINK_SLOT_COUNT); i++) {
                linkSlots[i] = slotList.getStringTagAt(i);
            }
        }

        // 读取配置名称
        configName = compound.getString("ConfigName");

        // 读取激活状态
        activated = compound.getBoolean("Activated");
    }

    // ==================== 同步 ====================

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 1, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    private void syncToClient() {
        if (world == null || world.isRemote) return;
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
    }

    // ==================== 调试 ====================

    @Override
    public String toString() {
        return "TileEntitySynergyStation{" +
                "linkedModules=" + getLinkedModules() +
                ", activated=" + activated +
                ", configName='" + configName + '\'' +
                '}';
    }
}
