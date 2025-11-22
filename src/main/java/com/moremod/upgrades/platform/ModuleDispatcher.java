package com.moremod.upgrades.platform;

import com.moremod.upgrades.api.IUpgradeModule;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * 模块分发器
 *
 * 功能：
 * - 分发 tick 到所有激活的模块
 * - 分发事件到所有激活的模块
 * - 自动处理能量消耗
 * - 处理模块生命周期
 *
 * 设计：
 * - 无状态类（所有方法都是静态的）
 * - 异常安全（单个模块失败不影响其他模块）
 */
public class ModuleDispatcher {

    private static final boolean DEBUG = false;

    // ===== Tick 分发 =====

    /**
     * 分发 tick 到所有激活的模块
     */
    public static void tickAllModules(@Nonnull EntityPlayer player,
                                     @Nonnull ItemStack coreStack,
                                     @Nonnull Map<String, ModuleState> allStates) {
        if (player.world.isRemote) {
            return; // 只在服务端执行
        }

        ModuleRegistry registry = ModuleRegistry.getInstance();

        for (Map.Entry<String, ModuleState> entry : allStates.entrySet()) {
            String moduleId = entry.getKey();
            ModuleState state = entry.getValue();

            // 跳过未激活的模块
            if (!state.isActive()) {
                continue;
            }

            // 获取模块实例
            IUpgradeModule module = registry.getModule(moduleId);
            if (module == null) {
                continue;
            }

            try {
                // 创建上下文
                ModuleContext context = new ModuleContext(player, coreStack, state);

                // 检查能量状态
                EnergyDepletionManager.EnergyStatus energyStatus = context.getEnergyStatus();
                if (!module.canRunWithEnergyStatus(energyStatus)) {
                    if (DEBUG) {
                        System.out.println("[ModuleDispatcher] 模块 " + moduleId + " 因能量状态跳过: " + energyStatus);
                    }
                    continue;
                }

                // 消耗被动能量
                int passiveCost = module.getPassiveEnergyCost(state.getEffectiveLevel());
                if (passiveCost > 0) {
                    if (!context.consumeEnergyBalanced(moduleId, passiveCost)) {
                        if (DEBUG) {
                            System.out.println("[ModuleDispatcher] 模块 " + moduleId + " 能量不足: " + passiveCost);
                        }
                        continue;
                    }
                }

                // 调用模块的 onTick
                module.onTick(player, coreStack, state.getEffectiveLevel());

            } catch (Throwable t) {
                System.err.println("[ModuleDispatcher] 模块 tick 失败: " + moduleId);
                t.printStackTrace();
            }
        }
    }

    // ===== 事件分发 =====

    /**
     * 分发事件到所有激活的模块
     */
    public static void dispatchEvent(@Nonnull Event event,
                                    @Nonnull EntityPlayer player,
                                    @Nonnull ItemStack coreStack,
                                    @Nonnull Map<String, ModuleState> allStates) {
        ModuleRegistry registry = ModuleRegistry.getInstance();

        for (Map.Entry<String, ModuleState> entry : allStates.entrySet()) {
            String moduleId = entry.getKey();
            ModuleState state = entry.getValue();

            // 跳过未激活的模块
            if (!state.isActive()) {
                continue;
            }

            // 获取模块实例
            IUpgradeModule module = registry.getModule(moduleId);
            if (module == null) {
                continue;
            }

            try {
                // 检查能量状态
                ModuleContext context = new ModuleContext(player, coreStack, state);
                EnergyDepletionManager.EnergyStatus energyStatus = context.getEnergyStatus();
                if (!module.canRunWithEnergyStatus(energyStatus)) {
                    continue;
                }

                // 调用模块的事件处理
                module.handleEvent(event, player, coreStack, state.getEffectiveLevel());

            } catch (Throwable t) {
                System.err.println("[ModuleDispatcher] 模块事件处理失败: " + moduleId);
                t.printStackTrace();
            }
        }
    }

    // ===== 生命周期分发 =====

    /**
     * 分发装备事件
     */
    public static void dispatchEquip(@Nonnull EntityPlayer player,
                                    @Nonnull ItemStack coreStack,
                                    @Nonnull Map<String, ModuleState> allStates) {
        ModuleRegistry registry = ModuleRegistry.getInstance();

        for (Map.Entry<String, ModuleState> entry : allStates.entrySet()) {
            String moduleId = entry.getKey();
            ModuleState state = entry.getValue();

            // 跳过等级为0的模块
            if (state.getLevel() <= 0) {
                continue;
            }

            IUpgradeModule module = registry.getModule(moduleId);
            if (module == null) {
                continue;
            }

            try {
                module.onEquip(player, coreStack, state.getLevel());
                if (DEBUG) {
                    System.out.println("[ModuleDispatcher] 模块 " + moduleId + " 已装备，等级 " + state.getLevel());
                }
            } catch (Throwable t) {
                System.err.println("[ModuleDispatcher] 模块装备失败: " + moduleId);
                t.printStackTrace();
            }
        }
    }

    /**
     * 分发卸载事件
     */
    public static void dispatchUnequip(@Nonnull EntityPlayer player,
                                      @Nonnull ItemStack coreStack,
                                      @Nonnull Map<String, ModuleState> allStates) {
        ModuleRegistry registry = ModuleRegistry.getInstance();

        for (Map.Entry<String, ModuleState> entry : allStates.entrySet()) {
            String moduleId = entry.getKey();
            ModuleState state = entry.getValue();

            // 跳过等级为0的模块
            if (state.getLevel() <= 0) {
                continue;
            }

            IUpgradeModule module = registry.getModule(moduleId);
            if (module == null) {
                continue;
            }

            try {
                module.onUnequip(player, coreStack, state.getLevel());
                if (DEBUG) {
                    System.out.println("[ModuleDispatcher] 模块 " + moduleId + " 已卸载");
                }
            } catch (Throwable t) {
                System.err.println("[ModuleDispatcher] 模块卸载失败: " + moduleId);
                t.printStackTrace();
            }
        }
    }

    // ===== 单个模块操作 =====

    /**
     * Tick 单个模块
     */
    public static void tickModule(@Nonnull String moduleId,
                                  @Nonnull EntityPlayer player,
                                  @Nonnull ItemStack coreStack,
                                  @Nonnull ModuleState state) {
        if (!state.isActive()) {
            return;
        }

        IUpgradeModule module = ModuleRegistry.getInstance().getModule(moduleId);
        if (module == null) {
            return;
        }

        try {
            ModuleContext context = new ModuleContext(player, coreStack, state);

            // 检查能量
            int passiveCost = module.getPassiveEnergyCost(state.getEffectiveLevel());
            if (passiveCost > 0 && !context.consumeEnergyBalanced(moduleId, passiveCost)) {
                return;
            }

            module.onTick(player, coreStack, state.getEffectiveLevel());

        } catch (Throwable t) {
            System.err.println("[ModuleDispatcher] 模块 tick 失败: " + moduleId);
            t.printStackTrace();
        }
    }
}
