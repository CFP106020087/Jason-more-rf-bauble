package com.moremod.synergy.effect;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.bridge.ExistingModuleBridge;
import com.moremod.synergy.core.SynergyContext;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 能量效果
 *
 * 添加或消耗能量。
 */
public class EnergyEffect implements ISynergyEffect {

    public enum EnergyAction {
        ADD,        // 添加能量
        CONSUME,    // 消耗能量
        REFUND      // 返还能量（基于原始消耗）
    }

    private final EnergyAction action;
    private final int amount;
    private final float refundPercent;
    private final boolean showMessage;
    private final String messagePrefix;

    private EnergyEffect(EnergyAction action, int amount, float refundPercent,
                         boolean showMessage, String messagePrefix) {
        this.action = action;
        this.amount = amount;
        this.refundPercent = refundPercent;
        this.showMessage = showMessage;
        this.messagePrefix = messagePrefix;
    }

    @Override
    public void apply(SynergyContext context) {
        ExistingModuleBridge bridge = ExistingModuleBridge.getInstance();

        int actualAmount = amount;

        switch (action) {
            case ADD:
                bridge.addEnergy(context.getPlayer(), actualAmount);
                if (showMessage && actualAmount > 0) {
                    context.getPlayer().sendStatusMessage(new TextComponentString(
                            TextFormatting.YELLOW + messagePrefix + " +" + actualAmount + " RF"
                    ), true);
                }
                break;

            case CONSUME:
                boolean success = bridge.consumeEnergy(context.getPlayer(), actualAmount);
                if (showMessage) {
                    if (success) {
                        context.getPlayer().sendStatusMessage(new TextComponentString(
                                TextFormatting.RED + messagePrefix + " -" + actualAmount + " RF"
                        ), true);
                    } else {
                        context.getPlayer().sendStatusMessage(new TextComponentString(
                                TextFormatting.DARK_RED + messagePrefix + " 能量不足"
                        ), true);
                    }
                }
                break;

            case REFUND:
                // 返还效果通常基于某个数值计算
                // 这里使用 customData 中的 energyConsumed 值（如果有）
                Integer consumed = context.getCustomData("energyConsumed", Integer.class);
                if (consumed != null && consumed > 0) {
                    int refund = (int) (consumed * refundPercent);
                    if (refund > 0) {
                        bridge.addEnergy(context.getPlayer(), refund);
                        if (showMessage) {
                            context.getPlayer().sendStatusMessage(new TextComponentString(
                                    TextFormatting.GREEN + messagePrefix + " 返还 " + refund + " RF"
                            ), true);
                        }
                    }
                }
                break;
        }
    }

    @Override
    public String getDescription() {
        switch (action) {
            case ADD:
                return "Add " + amount + " energy";
            case CONSUME:
                return "Consume " + amount + " energy";
            case REFUND:
                return "Refund " + (int)(refundPercent * 100) + "% energy";
            default:
                return "Unknown energy effect";
        }
    }

    // ==================== 静态工厂方法 ====================

    public static EnergyEffect add(int amount) {
        return new EnergyEffect(EnergyAction.ADD, amount, 0, false, "⚡");
    }

    public static EnergyEffect add(int amount, String message) {
        return new EnergyEffect(EnergyAction.ADD, amount, 0, true, message);
    }

    public static EnergyEffect consume(int amount) {
        return new EnergyEffect(EnergyAction.CONSUME, amount, 0, false, "⚡");
    }

    public static EnergyEffect consume(int amount, String message) {
        return new EnergyEffect(EnergyAction.CONSUME, amount, 0, true, message);
    }

    public static EnergyEffect refund(float percent) {
        return new EnergyEffect(EnergyAction.REFUND, 0, percent, false, "⚡");
    }

    public static EnergyEffect refund(float percent, String message) {
        return new EnergyEffect(EnergyAction.REFUND, 0, percent, true, message);
    }

    // ==================== Builder ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EnergyAction action = EnergyAction.ADD;
        private int amount = 0;
        private float refundPercent = 0;
        private boolean showMessage = false;
        private String messagePrefix = "⚡";

        public Builder add(int amount) {
            this.action = EnergyAction.ADD;
            this.amount = amount;
            return this;
        }

        public Builder consume(int amount) {
            this.action = EnergyAction.CONSUME;
            this.amount = amount;
            return this;
        }

        public Builder refund(float percent) {
            this.action = EnergyAction.REFUND;
            this.refundPercent = percent;
            return this;
        }

        public Builder showMessage(boolean show) {
            this.showMessage = show;
            return this;
        }

        public Builder messagePrefix(String prefix) {
            this.messagePrefix = prefix;
            this.showMessage = true;
            return this;
        }

        public EnergyEffect build() {
            return new EnergyEffect(action, amount, refundPercent, showMessage, messagePrefix);
        }
    }
}
