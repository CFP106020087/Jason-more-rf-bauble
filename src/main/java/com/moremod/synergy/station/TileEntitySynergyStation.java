package com.moremod.synergy.station;

import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.core.SynergyManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
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

    /** 激活此链结的玩家 UUID（用于追踪谁在使用此站点） */
    private UUID activatedByPlayerUUID = null;

    /** 匹配的 Synergy ID（缓存） */
    private String matchedSynergyId = null;

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

    /**
     * 设置激活状态（内部使用，不触发 Synergy 激活）
     */
    private void setActivatedInternal(boolean activated) {
        this.activated = activated;
        markDirty();
        syncToClient();
    }

    /**
     * 激活链结（由玩家触发）
     * @param player 激活链结的玩家
     * @return 是否成功激活
     */
    public boolean activateLink(EntityPlayer player) {
        if (world == null || world.isRemote) return false;
        if (!hasValidLink()) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "需要至少2个模块才能形成链结"));
            return false;
        }

        // 查找匹配的 Synergy
        String synergyId = findMatchingSynergy();
        if (synergyId == null) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "当前模块组合没有对应的 Synergy"));
            return false;
        }

        // 如果已有其他玩家激活，先停用
        if (activated && activatedByPlayerUUID != null) {
            deactivateLinkInternal();
        }

        // 激活 Synergy
        SynergyManager manager = SynergyManager.getInstance();
        if (manager.activateSynergyForPlayer(player, synergyId)) {
            this.activated = true;
            this.activatedByPlayerUUID = player.getUniqueID();
            this.matchedSynergyId = synergyId;
            markDirty();
            syncToClient();

            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ Synergy 链结已激活: " +
                    TextFormatting.GOLD + manager.get(synergyId).getDisplayName()));
            return true;
        }

        return false;
    }

    /**
     * 停用链结（由玩家触发）
     * @param player 触发停用的玩家
     */
    public void deactivateLink(EntityPlayer player) {
        if (world == null || world.isRemote) return;
        if (!activated) return;

        deactivateLinkInternal();

        player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "Synergy 链结已停用"));
    }

    /**
     * 内部停用方法
     */
    private void deactivateLinkInternal() {
        if (activatedByPlayerUUID != null && matchedSynergyId != null) {
            // 需要找到对应的玩家来停用
            if (world != null && !world.isRemote) {
                EntityPlayer player = world.getPlayerEntityByUUID(activatedByPlayerUUID);
                if (player != null) {
                    SynergyManager.getInstance().deactivateSynergyForPlayer(player, matchedSynergyId);
                }
            }
        }

        this.activated = false;
        this.activatedByPlayerUUID = null;
        this.matchedSynergyId = null;
        markDirty();
        syncToClient();
    }

    /**
     * 切换激活状态（由玩家触发）
     * @param player 触发切换的玩家
     */
    public void toggleActivated(EntityPlayer player) {
        if (activated) {
            deactivateLink(player);
        } else {
            activateLink(player);
        }
    }

    /**
     * 查找匹配当前链结模块的 Synergy
     * @return 匹配的 Synergy ID，如果没有匹配返回 null
     */
    @Nullable
    public String findMatchingSynergy() {
        List<String> linkedModules = getLinkedModules();
        if (linkedModules.size() < 2) return null;

        SynergyManager manager = SynergyManager.getInstance();
        Set<String> linkedSet = new HashSet<>(linkedModules);

        // 遍历所有已注册的 Synergy，查找完全匹配的
        for (SynergyDefinition def : manager.getAll()) {
            Set<String> requiredModules = new HashSet<>(def.getRequiredModules());
            if (requiredModules.equals(linkedSet)) {
                return def.getId();
            }
        }

        return null;
    }

    /**
     * 获取激活此链结的玩家 UUID
     */
    @Nullable
    public UUID getActivatedByPlayerUUID() {
        return activatedByPlayerUUID;
    }

    /**
     * 获取匹配的 Synergy ID
     */
    @Nullable
    public String getMatchedSynergyId() {
        return matchedSynergyId;
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

        // 保存激活玩家 UUID
        if (activatedByPlayerUUID != null) {
            compound.setString("ActivatedByPlayer", activatedByPlayerUUID.toString());
        }

        // 保存匹配的 Synergy ID
        if (matchedSynergyId != null) {
            compound.setString("MatchedSynergyId", matchedSynergyId);
        }

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

        // 读取激活玩家 UUID
        if (compound.hasKey("ActivatedByPlayer")) {
            try {
                activatedByPlayerUUID = UUID.fromString(compound.getString("ActivatedByPlayer"));
            } catch (IllegalArgumentException e) {
                activatedByPlayerUUID = null;
            }
        }

        // 读取匹配的 Synergy ID
        if (compound.hasKey("MatchedSynergyId")) {
            matchedSynergyId = compound.getString("MatchedSynergyId");
        }
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
