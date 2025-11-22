package com.moremod.eventHandler;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.IMechCoreModule;
import com.moremod.capability.module.ModuleContainer;
import com.moremod.capability.module.ModuleContext;
import com.moremod.upgrades.ModuleRegistry;
import net.minecraft.entity.player.EntityPlayer;
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

        // 标记脏（如果有变化）
        if (data.isDirty()) {
            // TODO: 网络同步
        }
    }
}
