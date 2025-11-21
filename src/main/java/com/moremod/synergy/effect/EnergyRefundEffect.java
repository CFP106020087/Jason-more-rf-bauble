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
 * 能量退还效果 - 在满足条件时退还能量
 *
 * 说明：
 * - 用于实现"能量循环"类 Synergy
 * - 可以按百分比或固定值退还能量
 * - 支持概率触发
 */
public class EnergyRefundEffect implements ISynergyEffect {

    private final int fixedAmount;       // 固定退还量（RF）
    private final float percentage;      // 按最大容量的百分比退还（0.0 ~ 1.0）
    private final float chance;          // 触发概率（0.0 ~ 1.0）
    private final boolean showMessage;   // 是否显示消息

    /**
     * 创建固定量退还效果
     *
     * @param amount 退还的能量量（RF）
     */
    public EnergyRefundEffect(int amount) {
        this(amount, 0f, 1.0f, false);
    }

    /**
     * 创建百分比退还效果
     *
     * @param percentage 退还百分比（0.0 ~ 1.0）
     */
    public EnergyRefundEffect(float percentage) {
        this(0, percentage, 1.0f, false);
    }

    /**
     * 创建带概率的退还效果
     *
     * @param amount 退还的能量量（RF）
     * @param chance 触发概率（0.0 ~ 1.0）
     * @param showMessage 是否显示消息
     */
    public EnergyRefundEffect(int amount, float chance, boolean showMessage) {
        this(amount, 0f, chance, showMessage);
    }

    /**
     * 完整构造器
     *
     * @param fixedAmount 固定退还量
     * @param percentage 百分比退还
     * @param chance 触发概率
     * @param showMessage 是否显示消息
     */
    public EnergyRefundEffect(int fixedAmount, float percentage, float chance, boolean showMessage) {
        this.fixedAmount = fixedAmount;
        this.percentage = percentage;
        this.chance = Math.max(0f, Math.min(1f, chance));
        this.showMessage = showMessage;
    }

    @Override
    public boolean apply(EntityPlayer player, List<IInstalledModuleView> modules, Event event) {
        if (player == null || player.world.isRemote) {
            return false;
        }

        // 概率检查
        if (chance < 1.0f && player.getRNG().nextFloat() > chance) {
            return false;
        }

        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) {
            return false;
        }

        // 计算退还量
        int refundAmount = fixedAmount;

        if (percentage > 0) {
            int maxEnergy = ItemMechanicalCore.getMaxEnergy(coreStack);
            refundAmount += (int) (maxEnergy * percentage);
        }

        if (refundAmount <= 0) {
            return false;
        }

        // 退还能量
        int actualRefund = ItemMechanicalCore.addEnergy(coreStack, refundAmount);

        if (actualRefund > 0) {
            // 显示消息
            if (showMessage) {
                player.sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.AQUA + "⚡ Synergy 能量回馈: +" + actualRefund + " RF"
                        ),
                        true
                );
            }

            // 粒子效果
            if (player.world.rand.nextInt(3) == 0) {
                for (int i = 0; i < 5; i++) {
                    player.world.spawnParticle(
                            net.minecraft.util.EnumParticleTypes.VILLAGER_HAPPY,
                            player.posX + (player.getRNG().nextDouble() - 0.5),
                            player.posY + 1.5,
                            player.posZ + (player.getRNG().nextDouble() - 0.5),
                            0, 0.1, 0
                    );
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("EnergyRefund[");
        if (fixedAmount > 0) {
            sb.append(fixedAmount).append("RF");
        }
        if (percentage > 0) {
            if (fixedAmount > 0) sb.append(" + ");
            sb.append((int)(percentage * 100)).append("%");
        }
        if (chance < 1.0f) {
            sb.append(" ").append((int)(chance * 100)).append("%chance");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int getPriority() {
        // 能量操作应该在较高优先级执行
        return 50;
    }
}
