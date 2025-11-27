package com.moremod.synergy.effect;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.core.SynergyContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 治疗效果
 *
 * 恢复玩家生命值或吸收心（护盾）。
 */
public class HealEffect implements ISynergyEffect {

    public enum HealType {
        HEALTH,         // 恢复生命值
        ABSORPTION,     // 添加吸收心（护盾）
        PERCENT_HEALTH, // 按百分比恢复生命值
        PERCENT_MAX     // 按最大生命值百分比恢复
    }

    private final HealType type;
    private final float value;
    private final float maxAbsorption;
    private final boolean showMessage;
    private final boolean showParticles;

    private HealEffect(HealType type, float value, float maxAbsorption,
                       boolean showMessage, boolean showParticles) {
        this.type = type;
        this.value = value;
        this.maxAbsorption = maxAbsorption;
        this.showMessage = showMessage;
        this.showParticles = showParticles;
    }

    @Override
    public void apply(SynergyContext context) {
        EntityPlayer player = context.getPlayer();

        switch (type) {
            case HEALTH:
                healHealth(player, value);
                break;

            case ABSORPTION:
                addAbsorption(player, value);
                break;

            case PERCENT_HEALTH:
                // 按当前缺失生命值的百分比恢复
                float missing = player.getMaxHealth() - player.getHealth();
                healHealth(player, missing * value);
                break;

            case PERCENT_MAX:
                // 按最大生命值的百分比恢复
                healHealth(player, player.getMaxHealth() * value);
                break;
        }
    }

    private void healHealth(EntityPlayer player, float amount) {
        if (amount <= 0) return;

        float before = player.getHealth();
        player.heal(amount);
        float after = player.getHealth();
        float actualHeal = after - before;

        if (actualHeal > 0) {
            if (showMessage) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GREEN + "❤ Synergy治疗 +" + String.format("%.1f", actualHeal)
                ), true);
            }

            if (showParticles) {
                spawnHealParticles(player);
            }
        }
    }

    private void addAbsorption(EntityPlayer player, float amount) {
        if (amount <= 0) return;

        float current = player.getAbsorptionAmount();
        float newAmount = current + amount;

        // 应用上限
        if (maxAbsorption > 0) {
            newAmount = Math.min(newAmount, maxAbsorption);
        }

        if (newAmount > current) {
            player.setAbsorptionAmount(newAmount);

            if (showMessage) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.YELLOW + "💛 Synergy护盾 +" + String.format("%.1f", newAmount - current)
                ), true);
            }

            if (showParticles) {
                spawnShieldParticles(player);
            }
        }
    }

    private void spawnHealParticles(EntityPlayer player) {
        for (int i = 0; i < 5; i++) {
            player.world.spawnParticle(
                    EnumParticleTypes.HEART,
                    player.posX + (player.getRNG().nextDouble() - 0.5) * 0.5,
                    player.posY + player.getRNG().nextDouble() * 2,
                    player.posZ + (player.getRNG().nextDouble() - 0.5) * 0.5,
                    0, 0.05, 0
            );
        }
    }

    private void spawnShieldParticles(EntityPlayer player) {
        for (int i = 0; i < 8; i++) {
            player.world.spawnParticle(
                    EnumParticleTypes.CRIT,
                    player.posX + (player.getRNG().nextDouble() - 0.5) * player.width,
                    player.posY + player.getRNG().nextDouble() * player.height,
                    player.posZ + (player.getRNG().nextDouble() - 0.5) * player.width,
                    0, 0.1, 0
            );
        }
    }

    @Override
    public String getDescription() {
        switch (type) {
            case HEALTH:
                return "Heal " + value + " health";
            case ABSORPTION:
                return "Add " + value + " absorption";
            case PERCENT_HEALTH:
                return "Heal " + (int)(value * 100) + "% of missing health";
            case PERCENT_MAX:
                return "Heal " + (int)(value * 100) + "% of max health";
            default:
                return "Unknown heal effect";
        }
    }

    // ==================== 静态工厂方法 ====================

    public static HealEffect health(float amount) {
        return new HealEffect(HealType.HEALTH, amount, 0, false, false);
    }

    public static HealEffect health(float amount, boolean showMessage) {
        return new HealEffect(HealType.HEALTH, amount, 0, showMessage, true);
    }

    public static HealEffect absorption(float amount) {
        return new HealEffect(HealType.ABSORPTION, amount, 20, false, false);
    }

    public static HealEffect absorption(float amount, float maxAbsorption) {
        return new HealEffect(HealType.ABSORPTION, amount, maxAbsorption, false, false);
    }

    public static HealEffect absorption(float amount, float maxAbsorption, boolean showMessage) {
        return new HealEffect(HealType.ABSORPTION, amount, maxAbsorption, showMessage, true);
    }

    public static HealEffect percentMissing(float percent) {
        return new HealEffect(HealType.PERCENT_HEALTH, percent, 0, false, false);
    }

    public static HealEffect percentMax(float percent) {
        return new HealEffect(HealType.PERCENT_MAX, percent, 0, false, false);
    }

    public static HealEffect percentMax(float percent, boolean showMessage) {
        return new HealEffect(HealType.PERCENT_MAX, percent, 0, showMessage, true);
    }
}
