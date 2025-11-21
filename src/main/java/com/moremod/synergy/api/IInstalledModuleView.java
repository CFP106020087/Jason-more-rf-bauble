package com.moremod.synergy.api;

/**
 * 已安装模块的只读视图接口
 *
 * 说明：
 * - Synergy 系统通过此接口读取模块信息，完全解耦于具体实现
 * - 不依赖 IUpgradeModule、ItemMechanicalCore 等现有类的内部细节
 * - 使模块安装情况可以"影子化"地查询，无需修改现有系统
 */
public interface IInstalledModuleView {

    /**
     * 获取模块唯一标识符
     * @return 模块 ID（如 "MAGIC_ABSORB", "NEURAL_SYNCHRONIZER"）
     */
    String getModuleId();

    /**
     * 获取模块当前等级
     * @return 等级（0 表示未安装或已禁用）
     */
    int getLevel();

    /**
     * 模块是否激活（考虑能量状态、GUI 暂停等）
     * @return true 表示模块当前可以工作
     */
    boolean isActive();

    /**
     * 获取模块显示名称（可选，用于调试/GUI）
     * @return 显示名称
     */
    default String getDisplayName() {
        return getModuleId();
    }
}
