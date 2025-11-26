package com.moremod.synergy.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * 玩家 Synergy 配置
 *
 * 存储玩家启用的 Synergy 列表。
 * 配置保存在机械核心的 NBT 中，而不是单独的数据文件。
 *
 * NBT 结构:
 * - SynergyConfig (TagCompound)
 *   - EnabledSynergies (TagList of Strings)
 *   - MaxSlots (int) - 最大可启用数量
 */
public class PlayerSynergyConfig {

    private static final String NBT_KEY_SYNERGY_CONFIG = "SynergyConfig";
    private static final String NBT_KEY_ENABLED = "EnabledSynergies";
    private static final String NBT_KEY_MAX_SLOTS = "MaxSlots";

    /** 默认最大可启用 Synergy 数量 */
    public static final int DEFAULT_MAX_SLOTS = 3;

    /** 玩家配置缓存 (UUID -> 启用的 Synergy ID 集合) */
    private static final Map<UUID, Set<String>> PLAYER_CONFIGS = new HashMap<>();

    // ==================== 核心 API ====================

    /**
     * 获取玩家启用的 Synergy 列表
     */
    @Nonnull
    public static Set<String> getEnabledSynergies(@Nonnull EntityPlayer player) {
        // 先检查缓存
        UUID playerId = player.getUniqueID();
        if (PLAYER_CONFIGS.containsKey(playerId)) {
            return PLAYER_CONFIGS.get(playerId);
        }

        // 从 NBT 加载
        Set<String> enabled = loadFromNBT(player);
        PLAYER_CONFIGS.put(playerId, enabled);
        return enabled;
    }

    /**
     * 检查玩家是否启用了指定 Synergy
     */
    public static boolean isSynergyEnabled(@Nonnull EntityPlayer player, @Nonnull String synergyId) {
        return getEnabledSynergies(player).contains(synergyId);
    }

    /**
     * 启用一个 Synergy
     *
     * @return true 如果成功启用，false 如果槽位已满或已启用
     */
    public static boolean enableSynergy(@Nonnull EntityPlayer player, @Nonnull String synergyId) {
        Set<String> enabled = getEnabledSynergies(player);

        // 检查是否已启用
        if (enabled.contains(synergyId)) {
            return false;
        }

        // 检查槽位
        int maxSlots = getMaxSlots(player);
        if (enabled.size() >= maxSlots) {
            return false;
        }

        // 检查是否满足模块要求
        SynergyDefinition def = SynergyManager.getInstance().get(synergyId);
        if (def == null) {
            return false;
        }

        enabled.add(synergyId);
        saveToNBT(player, enabled);
        return true;
    }

    /**
     * 禁用一个 Synergy
     */
    public static boolean disableSynergy(@Nonnull EntityPlayer player, @Nonnull String synergyId) {
        Set<String> enabled = getEnabledSynergies(player);

        if (!enabled.contains(synergyId)) {
            return false;
        }

        enabled.remove(synergyId);
        saveToNBT(player, enabled);
        return true;
    }

    /**
     * 切换 Synergy 启用状态
     */
    public static boolean toggleSynergy(@Nonnull EntityPlayer player, @Nonnull String synergyId) {
        if (isSynergyEnabled(player, synergyId)) {
            return disableSynergy(player, synergyId);
        } else {
            return enableSynergy(player, synergyId);
        }
    }

    /**
     * 清除所有启用的 Synergy
     */
    public static void clearAll(@Nonnull EntityPlayer player) {
        Set<String> enabled = getEnabledSynergies(player);
        enabled.clear();
        saveToNBT(player, enabled);
    }

    /**
     * 获取最大可启用槽位数
     */
    public static int getMaxSlots(@Nonnull EntityPlayer player) {
        ItemStack core = getMechanicalCore(player);
        if (core.isEmpty() || !core.hasTagCompound()) {
            return DEFAULT_MAX_SLOTS;
        }

        NBTTagCompound nbt = core.getTagCompound();
        if (nbt.hasKey(NBT_KEY_SYNERGY_CONFIG, Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound config = nbt.getCompoundTag(NBT_KEY_SYNERGY_CONFIG);
            if (config.hasKey(NBT_KEY_MAX_SLOTS)) {
                return config.getInteger(NBT_KEY_MAX_SLOTS);
            }
        }

        return DEFAULT_MAX_SLOTS;
    }

    /**
     * 设置最大可启用槽位数
     */
    public static void setMaxSlots(@Nonnull EntityPlayer player, int maxSlots) {
        ItemStack core = getMechanicalCore(player);
        if (core.isEmpty()) return;

        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            core.setTagCompound(nbt);
        }

        NBTTagCompound config = nbt.getCompoundTag(NBT_KEY_SYNERGY_CONFIG);
        config.setInteger(NBT_KEY_MAX_SLOTS, Math.max(1, maxSlots));
        nbt.setTag(NBT_KEY_SYNERGY_CONFIG, config);
    }

    /**
     * 获取剩余可用槽位数
     */
    public static int getRemainingSlots(@Nonnull EntityPlayer player) {
        return getMaxSlots(player) - getEnabledSynergies(player).size();
    }

    // ==================== NBT 操作 ====================

    private static Set<String> loadFromNBT(@Nonnull EntityPlayer player) {
        Set<String> enabled = new LinkedHashSet<>();

        ItemStack core = getMechanicalCore(player);
        if (core.isEmpty() || !core.hasTagCompound()) {
            return enabled;
        }

        NBTTagCompound nbt = core.getTagCompound();
        if (!nbt.hasKey(NBT_KEY_SYNERGY_CONFIG, Constants.NBT.TAG_COMPOUND)) {
            return enabled;
        }

        NBTTagCompound config = nbt.getCompoundTag(NBT_KEY_SYNERGY_CONFIG);
        if (!config.hasKey(NBT_KEY_ENABLED, Constants.NBT.TAG_LIST)) {
            return enabled;
        }

        NBTTagList list = config.getTagList(NBT_KEY_ENABLED, Constants.NBT.TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) {
            String id = list.getStringTagAt(i);
            if (id != null && !id.isEmpty()) {
                enabled.add(id);
            }
        }

        return enabled;
    }

    private static void saveToNBT(@Nonnull EntityPlayer player, @Nonnull Set<String> enabled) {
        ItemStack core = getMechanicalCore(player);
        if (core.isEmpty()) return;

        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            core.setTagCompound(nbt);
        }

        NBTTagCompound config = nbt.getCompoundTag(NBT_KEY_SYNERGY_CONFIG);

        // 保存启用列表
        NBTTagList list = new NBTTagList();
        for (String id : enabled) {
            list.appendTag(new NBTTagString(id));
        }
        config.setTag(NBT_KEY_ENABLED, list);

        // 保留 maxSlots
        if (!config.hasKey(NBT_KEY_MAX_SLOTS)) {
            config.setInteger(NBT_KEY_MAX_SLOTS, DEFAULT_MAX_SLOTS);
        }

        nbt.setTag(NBT_KEY_SYNERGY_CONFIG, config);

        // 更新缓存
        PLAYER_CONFIGS.put(player.getUniqueID(), enabled);
    }

    // ==================== 工具方法 ====================

    private static ItemStack getMechanicalCore(@Nonnull EntityPlayer player) {
        // 使用现有的桥接方法
        return com.moremod.item.ItemMechanicalCoreExtended.getCoreFromPlayer(player);
    }

    /**
     * 清除玩家缓存（玩家离线时调用）
     */
    public static void clearCache(@Nonnull EntityPlayer player) {
        PLAYER_CONFIGS.remove(player.getUniqueID());
    }

    /**
     * 清除所有缓存
     */
    public static void clearAllCache() {
        PLAYER_CONFIGS.clear();
    }

    /**
     * 重新从 NBT 加载玩家配置
     */
    public static void reloadConfig(@Nonnull EntityPlayer player) {
        PLAYER_CONFIGS.remove(player.getUniqueID());
        getEnabledSynergies(player); // 触发重新加载
    }
}
