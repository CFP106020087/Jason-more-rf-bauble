package com.moremod.synergy.core;

import com.moremod.synergy.api.IInstalledModuleView;
import com.moremod.synergy.api.IModuleProvider;
import com.moremod.synergy.api.ISynergyCondition;
import com.moremod.synergy.api.ISynergyEffect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Synergy 管理器 - 核心逻辑调度器
 *
 * 说明：
 * - 负责在事件中检测和触发 Synergy
 * - 连接 ModuleProvider 和 SynergyRegistry
 * - 完全无状态，线程安全
 */
public final class SynergyManager {

    private static final SynergyManager INSTANCE = new SynergyManager();
    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("synergy.debug", "false"));

    private IModuleProvider moduleProvider;

    private SynergyManager() {
        // 私有构造器，单例模式
    }

    public static SynergyManager getInstance() {
        return INSTANCE;
    }

    /**
     * 设置模块提供者（必须在使用前调用）
     *
     * @param provider 模块提供者实现
     */
    public void setModuleProvider(IModuleProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Module provider cannot be null");
        }
        this.moduleProvider = provider;
        System.out.println("[SynergyManager] Module provider set: " + provider.getClass().getSimpleName());
    }

    /**
     * 在事件中处理 Synergy
     *
     * @param player 玩家实体
     * @param event 触发的事件
     */
    public void processSynergiesInEvent(EntityPlayer player, Event event) {
        if (player == null || event == null) return;
        if (moduleProvider == null) {
            if (DEBUG) System.err.println("[SynergyManager] Module provider not set!");
            return;
        }

        // 获取玩家已安装的模块
        List<IInstalledModuleView> modules = moduleProvider.getInstalledModules(player);
        if (modules.isEmpty()) return;

        // 获取可能激活的 Synergy
        Set<String> installedIds = modules.stream()
                .map(IInstalledModuleView::getModuleId)
                .collect(Collectors.toSet());

        List<SynergyDefinition> applicableSynergies =
                SynergyRegistry.getInstance().findApplicableSynergies(installedIds);

        // 检测并触发
        for (SynergyDefinition synergy : applicableSynergies) {
            try {
                if (checkConditions(synergy, player, modules, event)) {
                    applyEffects(synergy, player, modules, event);
                }
            } catch (Exception e) {
                System.err.println("[SynergyManager] Error processing synergy: " + synergy.getId());
                e.printStackTrace();
            }
        }
    }

    /**
     * 在 Tick 中处理 Synergy（无事件）
     *
     * @param player 玩家实体
     */
    public void processSynergiesInTick(EntityPlayer player) {
        if (player == null) return;
        if (moduleProvider == null) {
            if (DEBUG) System.err.println("[SynergyManager] Module provider not set!");
            return;
        }

        // 获取玩家已安装的模块
        List<IInstalledModuleView> modules = moduleProvider.getInstalledModules(player);
        if (modules.isEmpty()) return;

        // 获取可能激活的 Synergy
        Set<String> installedIds = modules.stream()
                .map(IInstalledModuleView::getModuleId)
                .collect(Collectors.toSet());

        List<SynergyDefinition> applicableSynergies =
                SynergyRegistry.getInstance().findApplicableSynergies(installedIds);

        // 检测并触发（无事件）
        for (SynergyDefinition synergy : applicableSynergies) {
            try {
                if (checkConditions(synergy, player, modules, null)) {
                    applyEffects(synergy, player, modules, null);
                }
            } catch (Exception e) {
                System.err.println("[SynergyManager] Error processing synergy: " + synergy.getId());
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查所有条件是否满足
     *
     * @param synergy Synergy 定义
     * @param player 玩家
     * @param modules 已安装模块列表
     * @param event 事件（可能为 null）
     * @return true 表示所有条件满足
     */
    private boolean checkConditions(SynergyDefinition synergy, EntityPlayer player,
                                     List<IInstalledModuleView> modules, Event event) {
        // 如果没有条件，默认通过
        if (synergy.getConditions().isEmpty()) {
            return true;
        }

        // 所有条件都必须满足（AND 逻辑）
        for (ISynergyCondition condition : synergy.getConditions()) {
            if (!condition.test(player, modules, event)) {
                if (DEBUG) {
                    System.out.println("[SynergyManager] Condition failed: " + condition.getDescription());
                }
                return false;
            }
        }

        return true;
    }

    /**
     * 应用所有效果
     *
     * @param synergy Synergy 定义
     * @param player 玩家
     * @param modules 已安装模块列表
     * @param event 事件（可能为 null）
     */
    private void applyEffects(SynergyDefinition synergy, EntityPlayer player,
                              List<IInstalledModuleView> modules, Event event) {
        if (synergy.getEffects().isEmpty()) return;

        // 按优先级排序后执行
        List<ISynergyEffect> sortedEffects = new ArrayList<>(synergy.getEffects());
        sortedEffects.sort(Comparator.comparingInt(ISynergyEffect::getPriority));

        for (ISynergyEffect effect : sortedEffects) {
            try {
                boolean success = effect.apply(player, modules, event);
                if (DEBUG && success) {
                    System.out.println("[SynergyManager] Applied effect: " + effect.getDescription());
                }
            } catch (Exception e) {
                System.err.println("[SynergyManager] Error applying effect: " + effect.getDescription());
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取玩家当前激活的 Synergy 列表（用于 GUI 显示）
     *
     * @param player 玩家
     * @return 激活的 Synergy 列表
     */
    public List<SynergyDefinition> getActiveSynergies(EntityPlayer player) {
        if (player == null || moduleProvider == null) {
            return Collections.emptyList();
        }

        List<IInstalledModuleView> modules = moduleProvider.getInstalledModules(player);
        if (modules.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> installedIds = modules.stream()
                .map(IInstalledModuleView::getModuleId)
                .collect(Collectors.toSet());

        List<SynergyDefinition> applicableSynergies =
                SynergyRegistry.getInstance().findApplicableSynergies(installedIds);

        // 进一步过滤：检查条件（不传事件）
        return applicableSynergies.stream()
                .filter(synergy -> checkConditions(synergy, player, modules, null))
                .collect(Collectors.toList());
    }

    /**
     * 检查玩家是否拥有指定 Synergy
     *
     * @param player 玩家
     * @param synergyId Synergy ID
     * @return true 表示玩家拥有该 Synergy
     */
    public boolean hasActiveSynergy(EntityPlayer player, String synergyId) {
        return getActiveSynergies(player).stream()
                .anyMatch(s -> s.getId().equals(synergyId));
    }

    /**
     * 获取模块提供者
     *
     * @return 模块提供者
     */
    public IModuleProvider getModuleProvider() {
        return moduleProvider;
    }
}
