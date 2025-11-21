package com.moremod.synergy.effect;

import com.moremod.synergy.api.IInstalledModuleView;
import com.moremod.synergy.api.ISynergyEffect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;

/**
 * 护盾授予效果 - 给予玩家吸收心（黄心）
 *
 * 说明：
 * - 使用原版的 Absorption Hearts 机制
 * - 可以立即授予或逐渐恢复
 * - 支持条件触发（如受到伤害时）
 */
public class ShieldGrantEffect implements ISynergyEffect {

    private final float amount;          // 护盾量（2.0 = 1颗黄心）
    private final boolean instant;       // 是否立即授予（否则叠加）
    private final float maxAmount;       // 最大护盾量限制（0 = 无限制）
    private final boolean showMessage;   // 是否显示消息

    /**
     * 创建立即授予护盾效果
     *
     * @param amount 护盾量（2.0 = 1颗黄心）
     */
    public ShieldGrantEffect(float amount) {
        this(amount, true, 0f, false);
    }

    /**
     * 创建护盾效果
     *
     * @param amount 护盾量
     * @param instant 是否立即授予
     * @param maxAmount 最大护盾量限制
     * @param showMessage 是否显示消息
     */
    public ShieldGrantEffect(float amount, boolean instant, float maxAmount, boolean showMessage) {
        this.amount = amount;
        this.instant = instant;
        this.maxAmount = maxAmount;
        this.showMessage = showMessage;
    }

    @Override
    public boolean apply(EntityPlayer player, List<IInstalledModuleView> modules, Event event) {
        if (player == null || player.world.isRemote) {
            return false;
        }

        float currentShield = player.getAbsorptionAmount();

        // 检查是否达到上限
        if (maxAmount > 0 && currentShield >= maxAmount) {
            return false;
        }

        float newShield;
        if (instant) {
            // 立即设置为指定值
            newShield = amount;
        } else {
            // 叠加
            newShield = currentShield + amount;
        }

        // 应用上限
        if (maxAmount > 0) {
            newShield = Math.min(newShield, maxAmount);
        }

        // 确保不为负
        newShield = Math.max(0, newShield);

        player.setAbsorptionAmount(newShield);

        // 显示消息
        if (showMessage && newShield > currentShield) {
            float gained = newShield - currentShield;
            player.sendStatusMessage(
                    new TextComponentString(
                            TextFormatting.YELLOW + "💛 Synergy 护盾: +" +
                                    String.format("%.1f", gained)
                    ),
                    true
            );
        }

        // 粒子效果
        if (newShield > currentShield) {
            for (int i = 0; i < 8; i++) {
                player.world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.CRIT,
                        player.posX + (player.getRNG().nextDouble() - 0.5) * player.width * 2,
                        player.posY + player.getRNG().nextDouble() * player.height,
                        player.posZ + (player.getRNG().nextDouble() - 0.5) * player.width * 2,
                        (player.getRNG().nextDouble() - 0.5) * 0.3,
                        player.getRNG().nextDouble() * 0.3,
                        (player.getRNG().nextDouble() - 0.5) * 0.3
                );
            }
        }

        return newShield > currentShield;
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("ShieldGrant[");
        sb.append(instant ? "Set " : "Add ").append(amount);
        if (maxAmount > 0) {
            sb.append(" Max:").append(maxAmount);
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int getPriority() {
        // 护盾授予应该在中等优先级执行
        return 100;
    }
}
