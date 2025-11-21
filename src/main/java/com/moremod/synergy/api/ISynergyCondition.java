package com.moremod.synergy.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;

/**
 * Synergy 触发条件接口
 *
 * 说明：
 * - 定义何时一个 Synergy 应该被激活
 * - 可以是模块组合条件、事件类型条件、玩家状态条件等
 * - 支持组合逻辑（AND/OR）
 */
public interface ISynergyCondition {

    /**
     * 检查条件是否满足
     *
     * @param player 玩家实体
     * @param modules 玩家当前安装的模块列表
     * @param event 触发的事件（可能为 null，如在 tick 中检查）
     * @return true 表示条件满足，应触发 Synergy
     */
    boolean test(EntityPlayer player, List<IInstalledModuleView> modules, Event event);

    /**
     * 获取条件描述（用于调试/GUI）
     *
     * @return 条件描述字符串
     */
    default String getDescription() {
        return this.getClass().getSimpleName();
    }
}
