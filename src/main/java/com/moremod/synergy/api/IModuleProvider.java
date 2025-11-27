package com.moremod.synergy.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模块提供者接口
 *
 * 负责从玩家/机械核心中获取已安装模块的视图。
 * 这是 Synergy 系统与现有模块系统之间的桥梁。
 *
 * 使用方式：
 * - SynergyManager 通过这个接口获取玩家的模块快照
 * - 适配层（Bridge）实现这个接口，封装对旧系统的访问
 */
public interface IModuleProvider {

    /**
     * 获取玩家所有已安装的模块视图
     *
     * @param player 目标玩家
     * @return 模块视图列表（永不为 null，可能为空列表）
     */
    @Nonnull
    List<IInstalledModuleView> getInstalledModules(@Nonnull EntityPlayer player);

    /**
     * 获取指定机械核心中所有已安装的模块视图
     *
     * @param coreStack 机械核心物品堆
     * @return 模块视图列表（永不为 null，可能为空列表）
     */
    @Nonnull
    List<IInstalledModuleView> getInstalledModules(@Nonnull ItemStack coreStack);

    /**
     * 获取玩家的机械核心物品堆
     *
     * @param player 目标玩家
     * @return 机械核心 ItemStack，如果玩家没有装备则返回 null
     */
    @Nullable
    ItemStack getMechanicalCore(@Nonnull EntityPlayer player);

    /**
     * 检查玩家是否拥有指定的模块
     *
     * @param player 目标玩家
     * @param moduleId 模块ID
     * @return true 如果拥有该模块（不论是否激活）
     */
    default boolean hasModule(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        return getInstalledModules(player).stream()
                .anyMatch(m -> m.getModuleId().equalsIgnoreCase(moduleId));
    }

    /**
     * 检查玩家是否拥有指定的激活模块
     *
     * @param player 目标玩家
     * @param moduleId 模块ID
     * @return true 如果拥有该模块且处于激活状态
     */
    default boolean hasActiveModule(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        return getInstalledModules(player).stream()
                .anyMatch(m -> m.getModuleId().equalsIgnoreCase(moduleId) && m.isActive());
    }

    /**
     * 获取玩家所有激活模块的 ID 集合
     *
     * @param player 目标玩家
     * @return 激活模块 ID 的集合
     */
    default Set<String> getActiveModuleIds(@Nonnull EntityPlayer player) {
        return getInstalledModules(player).stream()
                .filter(IInstalledModuleView::isActive)
                .map(IInstalledModuleView::getModuleId)
                .collect(Collectors.toSet());
    }

    /**
     * 获取指定模块的视图
     *
     * @param player 目标玩家
     * @param moduleId 模块ID
     * @return 模块视图，如果未安装则返回 null
     */
    @Nullable
    default IInstalledModuleView getModule(@Nonnull EntityPlayer player, @Nonnull String moduleId) {
        return getInstalledModules(player).stream()
                .filter(m -> m.getModuleId().equalsIgnoreCase(moduleId))
                .findFirst()
                .orElse(null);
    }
}
