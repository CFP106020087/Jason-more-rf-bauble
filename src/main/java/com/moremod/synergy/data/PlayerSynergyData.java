package com.moremod.synergy.data;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import java.util.*;

/**
 * 玩家 Synergy 激活状态数据
 *
 * 说明：
 * - 存储每个玩家激活了哪些 Synergy
 * - 使用玩家 UUID 作为 key
 * - 数据存储在玩家的持久化 NBT 中
 *
 * 注意：
 * - Synergy 必须通过 Synergy Linker 方块激活
 * - 只拥有模块组合不会自动激活 Synergy
 */
public class PlayerSynergyData {

    private static final String NBT_KEY = "MoreModSynergies";
    private static final String NBT_ACTIVATED = "Activated";

    private final UUID playerUUID;
    private final Set<String> activatedSynergies;

    public PlayerSynergyData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.activatedSynergies = new HashSet<>();
    }

    /**
     * 从玩家获取 Synergy 数据
     *
     * @param player 玩家
     * @return PlayerSynergyData 对象
     */
    public static PlayerSynergyData get(EntityPlayer player) {
        PlayerSynergyData data = new PlayerSynergyData(player.getUniqueID());
        data.loadFromPlayer(player);
        return data;
    }

    /**
     * 从玩家 NBT 加载数据
     *
     * @param player 玩家
     */
    public void loadFromPlayer(EntityPlayer player) {
        NBTTagCompound playerData = player.getEntityData();
        if (!playerData.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            playerData.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }

        NBTTagCompound persistedData = playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (persistedData.hasKey(NBT_KEY)) {
            deserializeNBT(persistedData.getCompoundTag(NBT_KEY));
        }
    }

    /**
     * 保存数据到玩家 NBT
     *
     * @param player 玩家
     */
    public void saveToPlayer(EntityPlayer player) {
        NBTTagCompound playerData = player.getEntityData();
        if (!playerData.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            playerData.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }

        NBTTagCompound persistedData = playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        persistedData.setTag(NBT_KEY, serializeNBT());
    }

    /**
     * 激活一个 Synergy
     *
     * @param synergyId Synergy ID
     * @return true 表示激活成功（之前未激活）
     */
    public boolean activateSynergy(String synergyId) {
        return activatedSynergies.add(synergyId);
    }

    /**
     * 停用一个 Synergy
     *
     * @param synergyId Synergy ID
     * @return true 表示停用成功（之前已激活）
     */
    public boolean deactivateSynergy(String synergyId) {
        return activatedSynergies.remove(synergyId);
    }

    /**
     * 检查 Synergy 是否已激活
     *
     * @param synergyId Synergy ID
     * @return true 表示已激活
     */
    public boolean isSynergyActivated(String synergyId) {
        return activatedSynergies.contains(synergyId);
    }

    /**
     * 获取所有已激活的 Synergy ID
     *
     * @return 不可修改的 Set
     */
    public Set<String> getActivatedSynergies() {
        return Collections.unmodifiableSet(activatedSynergies);
    }

    /**
     * 清空所有激活状态
     */
    public void clearAll() {
        activatedSynergies.clear();
    }

    /**
     * 序列化为 NBT
     *
     * @return NBT 数据
     */
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList activatedList = new NBTTagList();

        for (String synergyId : activatedSynergies) {
            activatedList.appendTag(new NBTTagString(synergyId));
        }

        nbt.setTag(NBT_ACTIVATED, activatedList);
        return nbt;
    }

    /**
     * 从 NBT 反序列化
     *
     * @param nbt NBT 数据
     */
    public void deserializeNBT(NBTTagCompound nbt) {
        activatedSynergies.clear();

        if (nbt.hasKey(NBT_ACTIVATED)) {
            NBTTagList activatedList = nbt.getTagList(NBT_ACTIVATED, Constants.NBT.TAG_STRING);
            for (int i = 0; i < activatedList.tagCount(); i++) {
                activatedSynergies.add(activatedList.getStringTagAt(i));
            }
        }
    }

    /**
     * 获取玩家 UUID
     *
     * @return UUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    @Override
    public String toString() {
        return "PlayerSynergyData{" +
                "playerUUID=" + playerUUID +
                ", activatedSynergies=" + activatedSynergies +
                '}';
    }
}
