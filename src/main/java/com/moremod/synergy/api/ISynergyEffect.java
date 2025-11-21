package com.moremod.synergy.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;

/**
 * Synergy 效果接口
 *
 * 说明：
 * - 定义当 Synergy 触发时应该执行什么效果
 * - 可以是伤害修改、能量操作、属性加成、护盾生成等
 * - 效果应该是"外挂式"的，不修改原有模块逻辑
 */
public interface ISynergyEffect {

    /**
     * 应用效果
     *
     * @param player 玩家实体
     * @param modules 玩家当前安装的模块列表
     * @param event 触发的事件（可能为 null）
     * @return true 表示效果成功应用
     */
    boolean apply(EntityPlayer player, List<IInstalledModuleView> modules, Event event);

    /**
     * 获取效果描述（用于调试/GUI）
     *
     * @return 效果描述字符串
     */
    default String getDescription() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取效果优先级（数值越小越先执行）
     *
     * @return 优先级值
     */
    default int getPriority() {
        return 100;
    }
}
