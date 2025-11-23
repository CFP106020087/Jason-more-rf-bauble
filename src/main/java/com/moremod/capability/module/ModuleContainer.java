package com.moremod.capability.module;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模块容器 - 模块运行时管线
 *
 * 职责：
 *  ✓ 管理所有已激活的模块实例
 *  ✓ 维护模块等级状态
 *  ✓ 执行模块生命周期
 *  ✓ 序列化模块状态
 */
public class ModuleContainer {

    /** 模块等级存储 (moduleId → level) */
    private final Map<String, Integer> moduleLevels = new HashMap<>();

    /** 模块激活状态 (moduleId → active) */
    private final Map<String, Boolean> moduleActive = new HashMap<>();

    /** 模块元数据存储 (moduleId → NBT) */
    private final Map<String, NBTTagCompound> moduleMeta = new HashMap<>();

    // ────────────────────────────────────────────────────────────
    // 模块注册 & 查询
    // ────────────────────────────────────────────────────────────

    /** 获取模块等级 */
    public int getLevel(String moduleId) {
        int level = moduleLevels.getOrDefault(moduleId, 0);
        System.out.println("[ModuleContainer] getLevel: moduleId=" + moduleId + ", level=" + level + ", 容器内容=" + moduleLevels);
        return level;
    }

    /** 设置模块等级 */
    public void setLevel(String moduleId, int level) {
        System.out.println("[ModuleContainer] setLevel: moduleId=" + moduleId + ", level=" + level + ", 设置前容器=" + moduleLevels);
        if (level <= 0) {
            moduleLevels.remove(moduleId);
            moduleActive.remove(moduleId);
            System.out.println("[ModuleContainer] 等级≤0，已移除模块");
        } else {
            moduleLevels.put(moduleId, level);
            moduleActive.putIfAbsent(moduleId, true);
            System.out.println("[ModuleContainer] 已写入，设置后容器=" + moduleLevels);
        }
    }

    /** 模块是否激活 */
    public boolean isActive(String moduleId) {
        return moduleActive.getOrDefault(moduleId, false);
    }

    /** 设置激活状态 */
    public void setActive(String moduleId, boolean active) {
        if (moduleLevels.containsKey(moduleId)) {
            moduleActive.put(moduleId, active);
        }
    }

    /** 获取所有已激活的模块 ID */
    public Collection<String> getActiveModules() {
        return moduleActive.entrySet().stream()
            .filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /** 获取所有模块 ID（包括未激活） */
    public Collection<String> getAllModules() {
        return moduleLevels.keySet();
    }

    // ────────────────────────────────────────────────────────────
    // 元数据管理
    // ────────────────────────────────────────────────────────────

    /** 获取模块元数据 */
    public NBTTagCompound getMeta(String moduleId) {
        return moduleMeta.computeIfAbsent(moduleId, k -> new NBTTagCompound());
    }

    /** 设置模块元数据 */
    public void setMeta(String moduleId, NBTTagCompound meta) {
        moduleMeta.put(moduleId, meta);
    }

    // ────────────────────────────────────────────────────────────
    // 序列化
    // ────────────────────────────────────────────────────────────

    /** 序列化到 NBT */
    public NBTTagCompound serializeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();

        // 存储等级
        NBTTagCompound levels = new NBTTagCompound();
        moduleLevels.forEach((id, level) -> levels.setInteger(id, level));
        nbt.setTag("LEVELS", levels);

        // 存储激活状态
        NBTTagCompound active = new NBTTagCompound();
        moduleActive.forEach((id, isActive) -> active.setBoolean(id, isActive));
        nbt.setTag("ACTIVE", active);

        // 存储元数据
        NBTTagCompound meta = new NBTTagCompound();
        moduleMeta.forEach((id, data) -> meta.setTag(id, data));
        nbt.setTag("META", meta);

        return nbt;
    }

    /** 从 NBT 反序列化 */
    public void deserializeNBT(NBTTagCompound nbt) {
        moduleLevels.clear();
        moduleActive.clear();
        moduleMeta.clear();

        // 读取等级
        if (nbt.hasKey("LEVELS")) {
            NBTTagCompound levels = nbt.getCompoundTag("LEVELS");
            for (String key : levels.getKeySet()) {
                moduleLevels.put(key, levels.getInteger(key));
            }
        }

        // 读取激活状态
        if (nbt.hasKey("ACTIVE")) {
            NBTTagCompound active = nbt.getCompoundTag("ACTIVE");
            for (String key : active.getKeySet()) {
                moduleActive.put(key, active.getBoolean(key));
            }
        }

        // 读取元数据
        if (nbt.hasKey("META")) {
            NBTTagCompound meta = nbt.getCompoundTag("META");
            for (String key : meta.getKeySet()) {
                moduleMeta.put(key, meta.getCompoundTag(key));
            }
        }
    }
}
