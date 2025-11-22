package com.moremod.upgrades.platform;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * 模块数据存储包装器
 *
 * 功能：
 * - 封装所有 NBT 读写操作
 * - 兼容旧的 NBT 格式
 * - 提供统一的数据访问接口
 *
 * 设计：
 * - 新数据存储在 "ModulePlatform" 标签下
 * - 兼容读取旧的 "upgrade_*" 字段
 * - 写入时同时更新新旧两种格式（过渡期）
 */
public class ModuleDataStorage {

    private static final String NEW_TAG = "ModulePlatform";
    private static final String MODULES_TAG = "modules";
    private static final String OLD_UPGRADE_PREFIX = "upgrade_";
    private static final String OLD_HAS_PREFIX = "HasUpgrade_";
    private static final String OLD_OWNED_PREFIX = "OwnedMax_";
    private static final String OLD_PAUSED_PREFIX = "IsPaused_";
    private static final String OLD_DISABLED_PREFIX = "Disabled_";

    // ===== 加载模块状态（从 ItemStack NBT） =====

    /**
     * 从核心物品加载所有模块状态
     */
    public static Map<String, ModuleState> loadAllStates(ItemStack coreStack) {
        Map<String, ModuleState> states = new HashMap<>();

        if (coreStack.isEmpty() || !coreStack.hasTagCompound()) {
            return states;
        }

        NBTTagCompound rootNBT = coreStack.getTagCompound();

        // 优先读取新格式
        if (rootNBT.hasKey(NEW_TAG)) {
            NBTTagCompound platformNBT = rootNBT.getCompoundTag(NEW_TAG);
            if (platformNBT.hasKey(MODULES_TAG)) {
                NBTTagList modulesList = platformNBT.getTagList(MODULES_TAG, Constants.NBT.TAG_COMPOUND);
                for (int i = 0; i < modulesList.tagCount(); i++) {
                    NBTTagCompound moduleNBT = modulesList.getCompoundTagAt(i);
                    String moduleId = moduleNBT.getString("moduleId");
                    if (!moduleId.isEmpty()) {
                        ModuleState state = new ModuleState(moduleId);
                        state.deserializeNBT(moduleNBT);
                        states.put(normalizeId(moduleId), state);
                    }
                }
            }
        }

        // 兼容读取旧格式（如果新格式不存在）
        if (states.isEmpty()) {
            loadLegacyFormat(rootNBT, states);
        }

        return states;
    }

    /**
     * 加载单个模块状态
     */
    public static ModuleState loadState(ItemStack coreStack, String moduleId) {
        String normalizedId = normalizeId(moduleId);
        Map<String, ModuleState> allStates = loadAllStates(coreStack);
        return allStates.getOrDefault(normalizedId, new ModuleState(normalizedId));
    }

    // ===== 保存模块状态（到 ItemStack NBT） =====

    /**
     * 保存所有模块状态
     */
    public static void saveAllStates(ItemStack coreStack, Map<String, ModuleState> states) {
        if (coreStack.isEmpty()) {
            return;
        }

        if (!coreStack.hasTagCompound()) {
            coreStack.setTagCompound(new NBTTagCompound());
        }

        NBTTagCompound rootNBT = coreStack.getTagCompound();

        // 保存新格式
        NBTTagCompound platformNBT = new NBTTagCompound();
        NBTTagList modulesList = new NBTTagList();

        for (ModuleState state : states.values()) {
            modulesList.appendTag(state.serializeNBT());
        }

        platformNBT.setTag(MODULES_TAG, modulesList);
        rootNBT.setTag(NEW_TAG, platformNBT);

        // 同时更新旧格式（兼容性）
        saveLegacyFormat(rootNBT, states);
    }

    /**
     * 保存单个模块状态
     */
    public static void saveState(ItemStack coreStack, ModuleState state) {
        Map<String, ModuleState> allStates = loadAllStates(coreStack);
        allStates.put(normalizeId(state.getModuleId()), state);
        saveAllStates(coreStack, allStates);
    }

    // ===== 兼容旧格式 =====

    /**
     * 从旧格式 NBT 加载
     */
    private static void loadLegacyFormat(NBTTagCompound rootNBT, Map<String, ModuleState> states) {
        // 扫描所有 "upgrade_*" 字段
        for (String key : rootNBT.getKeySet()) {
            if (key.startsWith(OLD_UPGRADE_PREFIX)) {
                String moduleId = key.substring(OLD_UPGRADE_PREFIX.length());
                String normalizedId = normalizeId(moduleId);

                if (!states.containsKey(normalizedId)) {
                    ModuleState state = new ModuleState(normalizedId);

                    // 读取等级
                    int level = rootNBT.getInteger(key);
                    state.setLevel(level);

                    // 读取拥有上限
                    String ownedKey = OLD_OWNED_PREFIX + moduleId;
                    if (rootNBT.hasKey(ownedKey)) {
                        state.setOwnedMaxLevel(rootNBT.getInteger(ownedKey));
                    } else {
                        state.setOwnedMaxLevel(level);
                    }

                    // 读取暂停状态
                    String pausedKey = OLD_PAUSED_PREFIX + moduleId;
                    if (rootNBT.hasKey(pausedKey)) {
                        state.setPaused(rootNBT.getBoolean(pausedKey));
                    }

                    // 读取禁用状态
                    String disabledKey = OLD_DISABLED_PREFIX + moduleId;
                    if (rootNBT.hasKey(disabledKey)) {
                        state.setDisabled(rootNBT.getBoolean(disabledKey));
                    }

                    states.put(normalizedId, state);
                }
            }
        }
    }

    /**
     * 保存到旧格式 NBT（兼容性）
     */
    private static void saveLegacyFormat(NBTTagCompound rootNBT, Map<String, ModuleState> states) {
        for (ModuleState state : states.values()) {
            String moduleId = state.getModuleId();

            // 写入等级
            rootNBT.setInteger(OLD_UPGRADE_PREFIX + moduleId, state.getLevel());

            // 写入安装标记
            if (state.getLevel() > 0 || state.getOwnedMaxLevel() > 0) {
                rootNBT.setBoolean(OLD_HAS_PREFIX + moduleId, true);
            }

            // 写入拥有上限
            if (state.getOwnedMaxLevel() > 0) {
                rootNBT.setInteger(OLD_OWNED_PREFIX + moduleId, state.getOwnedMaxLevel());
            }

            // 写入暂停状态
            if (state.isPaused()) {
                rootNBT.setBoolean(OLD_PAUSED_PREFIX + moduleId, true);
            } else {
                rootNBT.removeTag(OLD_PAUSED_PREFIX + moduleId);
            }

            // 写入禁用状态
            if (state.isDisabled()) {
                rootNBT.setBoolean(OLD_DISABLED_PREFIX + moduleId, true);
            } else {
                rootNBT.removeTag(OLD_DISABLED_PREFIX + moduleId);
            }
        }
    }

    // ===== 工具方法 =====

    /**
     * 规范化模块ID（转大写）
     */
    public static String normalizeId(String moduleId) {
        return moduleId == null ? "" : moduleId.trim().toUpperCase();
    }

    /**
     * 检查核心物品是否有指定模块
     */
    public static boolean hasModule(ItemStack coreStack, String moduleId) {
        ModuleState state = loadState(coreStack, moduleId);
        return state.getLevel() > 0 || state.getOwnedMaxLevel() > 0;
    }

    /**
     * 获取模块等级
     */
    public static int getModuleLevel(ItemStack coreStack, String moduleId) {
        return loadState(coreStack, moduleId).getLevel();
    }

    /**
     * 设置模块等级
     */
    public static void setModuleLevel(ItemStack coreStack, String moduleId, int level) {
        ModuleState state = loadState(coreStack, moduleId);
        state.setLevel(level);
        saveState(coreStack, state);
    }

    /**
     * 检查模块是否激活
     */
    public static boolean isModuleActive(ItemStack coreStack, String moduleId) {
        return loadState(coreStack, moduleId).isActive();
    }

    /**
     * 触摸模块（确保模块状态被初始化）
     *
     * 用于在升级物品时确保模块平台状态与旧系统同步。
     * 如果模块状态不存在，会自动创建并从旧格式迁移数据。
     *
     * @param coreStack 核心物品
     * @param moduleId 模块ID
     */
    public static void touchModule(ItemStack coreStack, String moduleId) {
        ModuleState state = loadState(coreStack, moduleId);
        // 保存状态以确保新格式被写入
        saveState(coreStack, state);
    }
}
