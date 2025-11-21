package com.moremod.synergy.effect;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.synergy.api.IInstalledModuleView;
import com.moremod.synergy.api.ISynergyEffect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;

/**
 * 能量流失效果 - Drawback 负面效果
 *
 * 说明：
 * - 持续消耗玩家机械核心的能量
 * - 可设置每秒消耗量和触发间隔
 * - 能量不足时停止工作（不会造成额外伤害）
 */
public class EnergyDrainEffect implements ISynergyEffect {

    private final int energyPerSecond;       // 每秒能量消耗
    private final int tickInterval;          // 触发间隔（tick）
    private final boolean showMessage;       // 是否显示消息
    private final boolean disableOnDepleted; // 能量耗尽时是否禁用

    /**
     * 创建能量流失效果
     *
     * @param energyPerSecond 每秒能量消耗（RF）
     */
    public EnergyDrainEffect(int energyPerSecond) {
        this(energyPerSecond, 20, false, false);
    }

    /**
     * 完整构造器
     *
     * @param energyPerSecond 每秒能量消耗
     * @param tickInterval 触发间隔（tick）
     * @param showMessage 是否显示消息
     * @param disableOnDepleted 能量耗尽时是否禁用
     */
    public EnergyDrainEffect(int energyPerSecond, int tickInterval, boolean showMessage, boolean disableOnDepleted) {
        this.energyPerSecond = energyPerSecond;
        this.tickInterval = Math.max(1, tickInterval);
        this.showMessage = showMessage;
        this.disableOnDepleted = disableOnDepleted;
    }

    @Override
    public boolean apply(EntityPlayer player, List<IInstalledModuleView> modules, Event event) {
        if (player == null || player.world.isRemote) {
            return false;
        }

        // 检查触发间隔
        if (player.world.getTotalWorldTime() % tickInterval != 0) {
            return false;
        }

        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) {
            return false;
        }

        // 计算实际消耗（根据间隔调整）
        int actualDrain = (int) (energyPerSecond * (tickInterval / 20.0f));

        // 尝试消耗能量
        boolean consumed = ItemMechanicalCore.consumeEnergy(coreStack, actualDrain, false);

        if (!consumed && disableOnDepleted) {
            // 能量不足，显示警告
            if (showMessage && player.world.getTotalWorldTime() % 60 == 0) {
                player.sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.YELLOW + "⚡ Synergy 能量不足，负面效果暂停"
                        ),
                        true
                );
            }
            return false;
        }

        // 显示消息
        if (showMessage && consumed && player.world.getTotalWorldTime() % 100 == 0) {
            player.sendStatusMessage(
                    new TextComponentString(
                            TextFormatting.GOLD + "⚡ Synergy 维持成本: -" + actualDrain + " RF/s"
                    ),
                    true
            );
        }

        return consumed;
    }

    @Override
    public String getDescription() {
        return String.format("EnergyDrain[%d RF/s]", energyPerSecond);
    }

    @Override
    public int getPriority() {
        // Drawback 应该在最后执行
        return 500;
    }
}
