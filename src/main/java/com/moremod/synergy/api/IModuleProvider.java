package com.moremod.synergy.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * 模块信息提供者接口
 *
 * 说明：
 * - 负责从现有系统（Mechanical Core / 玩家）中提取模块列表
 * - 转换为 Synergy 系统可识别的 IInstalledModuleView
 * - 这是 Synergy 与现有系统之间唯一的桥接点
 */
public interface IModuleProvider {

    /**
     * 获取玩家已安装的模块列表
     *
     * @param player 玩家实体
     * @return 已安装模块的只读视图列表（空列表表示无模块）
     */
    List<IInstalledModuleView> getInstalledModules(EntityPlayer player);

    /**
     * 获取指定核心物品上的模块列表
     *
     * @param coreStack Mechanical Core 物品
     * @return 已安装模块的只读视图列表
     */
    List<IInstalledModuleView> getInstalledModules(ItemStack coreStack);

    /**
     * 检查玩家是否拥有指定模块（且激活）
     *
     * @param player 玩家实体
     * @param moduleId 模块 ID
     * @return true 表示玩家拥有该模块且模块激活
     */
    boolean hasActiveModule(EntityPlayer player, String moduleId);

    /**
     * 获取玩家指定模块的等级
     *
     * @param player 玩家实体
     * @param moduleId 模块 ID
     * @return 模块等级（0 表示未安装或未激活）
     */
    int getModuleLevel(EntityPlayer player, String moduleId);
}
