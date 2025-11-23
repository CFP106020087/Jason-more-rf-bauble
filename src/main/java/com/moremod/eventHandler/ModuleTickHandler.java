package com.moremod.eventHandler;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.IMechCoreModule;
import com.moremod.capability.module.ModuleContainer;
import com.moremod.capability.module.ModuleContext;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.network.NetworkHandler;
import com.moremod.network.PacketSyncMechCoreData;
import com.moremod.upgrades.ModuleRegistry;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 模块 Tick 处理器
 *
 * 职责：
 *  ✓ 每个玩家 tick 调用所有激活模块的 onTick
 *  ✓ 自动能量消耗
 *  ✓ 能量不足时自动停用模块
 */
public class ModuleTickHandler {

    private static final Logger logger = LogManager.getLogger(ModuleTickHandler.class);

    /** Tick 间隔（每 N tick 执行一次，减少性能开销） */
    private static final int TICK_INTERVAL = 5;

    private int tickCounter = 0;

    /**
     * 玩家 Tick 事件
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务端 + End 阶段执行
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }

        // 降频执行
        if (++tickCounter < TICK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        EntityPlayer player = event.player;
        IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);

        if (data == null) {
            return;
        }

        // 创建执行上下文
        ModuleContext context = new ModuleContext(
            player.world,
            player.world.getTotalWorldTime(),
            event.side
        );

        // 获取所有激活的模块
        ModuleContainer container = data.getModuleContainer();

        for (String moduleId : container.getActiveModules()) {
            IMechCoreModule module = ModuleRegistry.getNew(moduleId);

            if (module == null) {
                logger.warn("Active module not found in registry: {}", moduleId);
                continue;
            }

            // 检查执行条件
            if (!module.canExecute(player, data)) {
                continue;
            }

            int level = container.getLevel(moduleId);

            // 计算被动能量消耗
            int energyCost = module.getPassiveEnergyCost(level);

            // 检查能量
            if (energyCost > 0) {
                if (data.getEnergy() < energyCost) {
                    // 能量不足，停用模块
                    container.setActive(moduleId, false);
                    module.onDeactivate(player, data);

                    logger.debug("Module {} deactivated due to low energy for player: {}",
                               moduleId, player.getName());
                    continue;
                }

                // 消耗能量
                data.consumeEnergy(energyCost);
            }

            // 执行模块 tick
            try {
                module.onTick(player, data, context);
            } catch (Exception e) {
                logger.error("Error executing module {} tick for player: {}",
                           moduleId, player.getName(), e);
            }
        }

        // 能量惩罚系统（仅每秒检查一次）
        if (player.world.getTotalWorldTime() % 20 == 0) {
            handleEnergyPunishment(player, data);
        }

        // 网络同步（如果有变化）
        if (data.isDirty()) {
            syncToClient(player, data);
            data.clearDirty();
        }
    }

    /**
     * 同步 Capability 数据到客户端
     */
    private void syncToClient(EntityPlayer player, IMechCoreData data) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }

        try {
            PacketSyncMechCoreData packet = new PacketSyncMechCoreData(data);
            NetworkHandler.CHANNEL.sendTo(packet, (EntityPlayerMP) player);
        } catch (Exception e) {
            logger.error("Failed to sync MechCoreData to client for player: {}",
                       player.getName(), e);
        }
    }

    /**
     * 处理能量惩罚系统
     *
     * 基于 Capability 能量状态调用惩罚系统
     */
    private void handleEnergyPunishment(EntityPlayer player, IMechCoreData data) {
        // 计算能量状态
        EnergyDepletionManager.EnergyStatus status = calculateEnergyStatus(data);

        // 仅在低能量状态下执行惩罚
        if (status == EnergyDepletionManager.EnergyStatus.EMERGENCY ||
            status == EnergyDepletionManager.EnergyStatus.CRITICAL) {

            // 获取核心物品（用于 EnergyPunishmentSystem 的 ItemStack 操作）
            ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);

            if (!coreStack.isEmpty() && ItemMechanicalCore.isMechanicalCore(coreStack)) {
                // 调用惩罚系统（保持与旧系统兼容）
                EnergyPunishmentSystem.tick(coreStack, player, status);
            }
        }
    }

    /**
     * 根据 Capability 数据计算能量状态
     */
    private EnergyDepletionManager.EnergyStatus calculateEnergyStatus(IMechCoreData data) {
        int current = data.getEnergy();
        int max = data.getMaxEnergy();

        if (max == 0) {
            return EnergyDepletionManager.EnergyStatus.CRITICAL;
        }

        float percentage = (float) current / max;

        if (percentage >= EnergyDepletionManager.EnergyStatus.NORMAL.threshold) {
            return EnergyDepletionManager.EnergyStatus.NORMAL;
        } else if (percentage >= EnergyDepletionManager.EnergyStatus.POWER_SAVING.threshold) {
            return EnergyDepletionManager.EnergyStatus.POWER_SAVING;
        } else if (percentage >= EnergyDepletionManager.EnergyStatus.EMERGENCY.threshold) {
            return EnergyDepletionManager.EnergyStatus.EMERGENCY;
        } else {
            return EnergyDepletionManager.EnergyStatus.CRITICAL;
        }
    }
}
