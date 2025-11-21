package com.moremod.synergy.condition;

import com.moremod.synergy.api.IInstalledModuleView;
import com.moremod.synergy.api.ISynergyCondition;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 模块组合条件 - 检查玩家是否拥有指定的模块组合
 *
 * 说明：
 * - 最基础的条件类型
 * - 检查玩家是否同时拥有指定的所有模块
 * - 可选择是否要求模块激活
 */
public class ModuleCombinationCondition implements ISynergyCondition {

    private final Set<String> requiredModules;
    private final boolean requireActive;
    private final int minTotalLevel; // 所有模块的总等级需达到的最小值（可选）

    public ModuleCombinationCondition(String... moduleIds) {
        this(true, 0, moduleIds);
    }

    public ModuleCombinationCondition(boolean requireActive, String... moduleIds) {
        this(requireActive, 0, moduleIds);
    }

    public ModuleCombinationCondition(boolean requireActive, int minTotalLevel, String... moduleIds) {
        this.requiredModules = new HashSet<>(Arrays.asList(moduleIds));
        this.requireActive = requireActive;
        this.minTotalLevel = minTotalLevel;
    }

    @Override
    public boolean test(EntityPlayer player, List<IInstalledModuleView> modules, Event event) {
        if (player == null || modules == null || modules.isEmpty()) {
            return false;
        }

        // 检查所有必需的模块是否都存在
        Set<String> foundModules = new HashSet<>();
        int totalLevel = 0;

        for (IInstalledModuleView module : modules) {
            if (requiredModules.contains(module.getModuleId())) {
                // 如果要求激活，检查激活状态
                if (requireActive && !module.isActive()) {
                    continue; // 模块未激活，跳过
                }

                foundModules.add(module.getModuleId());
                totalLevel += module.getLevel();
            }
        }

        // 所有必需模块都找到
        boolean hasAllModules = foundModules.containsAll(requiredModules);

        // 检查总等级
        boolean meetsLevelRequirement = totalLevel >= minTotalLevel;

        return hasAllModules && meetsLevelRequirement;
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("HasModules[");
        sb.append(String.join(", ", requiredModules));
        if (minTotalLevel > 0) {
            sb.append(" TotalLv>=").append(minTotalLevel);
        }
        if (requireActive) {
            sb.append(" Active");
        }
        sb.append("]");
        return sb.toString();
    }
}
