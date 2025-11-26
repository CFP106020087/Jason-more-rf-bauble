package com.moremod.synergy.effect;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.core.SynergyContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 伤害效果
 *
 * 对目标造成额外伤害，或修改伤害事件的伤害值。
 */
public class DamageEffect implements ISynergyEffect {

    public enum DamageType {
        FLAT,           // 固定额外伤害
        PERCENT,        // 百分比额外伤害（基于原始伤害）
        MULTIPLY,       // 伤害倍率
        TRUE_DAMAGE     // 真实伤害（无视护甲）
    }

    private final DamageType type;
    private final float value;
    private final boolean showMessage;
    private final String damageSourceName;

    private DamageEffect(DamageType type, float value, boolean showMessage, String damageSourceName) {
        this.type = type;
        this.value = value;
        this.showMessage = showMessage;
        this.damageSourceName = damageSourceName;
    }

    @Override
    public void apply(SynergyContext context) {
        EntityLivingBase target = context.getTarget();
        if (target == null) return;

        float originalDamage = context.getOriginalDamage();
        float extraDamage = 0;

        switch (type) {
            case FLAT:
                extraDamage = value;
                break;

            case PERCENT:
                extraDamage = originalDamage * value;
                break;

            case MULTIPLY:
                // 修改伤害事件的伤害值
                if (context.getTriggerEvent() instanceof LivingHurtEvent) {
                    LivingHurtEvent event = (LivingHurtEvent) context.getTriggerEvent();
                    float newDamage = event.getAmount() * value;
                    event.setAmount(newDamage);
                    if (showMessage) {
                        context.getPlayer().sendStatusMessage(new TextComponentString(
                                TextFormatting.RED + "⚔ Synergy伤害 x" + String.format("%.1f", value)
                        ), true);
                    }
                }
                return;

            case TRUE_DAMAGE:
                // 真实伤害，直接对目标造成
                DamageSource source = DamageSource.causePlayerDamage(context.getPlayer());
                source.setDamageBypassesArmor();
                target.attackEntityFrom(source, value);
                if (showMessage) {
                    context.getPlayer().sendStatusMessage(new TextComponentString(
                            TextFormatting.DARK_RED + "⚔ Synergy真实伤害 " + String.format("%.1f", value)
                    ), true);
                }
                return;
        }

        // 造成额外伤害
        if (extraDamage > 0) {
            DamageSource source;
            if (damageSourceName != null && !damageSourceName.isEmpty()) {
                source = new DamageSource(damageSourceName);
            } else {
                source = DamageSource.causePlayerDamage(context.getPlayer());
            }

            target.attackEntityFrom(source, extraDamage);

            if (showMessage) {
                context.getPlayer().sendStatusMessage(new TextComponentString(
                        TextFormatting.GOLD + "⚔ Synergy额外伤害 +" + String.format("%.1f", extraDamage)
                ), true);
            }

            // 伤害粒子效果
            target.world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.CRIT_MAGIC,
                    target.posX,
                    target.posY + target.height / 2,
                    target.posZ,
                    0.2, 0.2, 0.2
            );
        }
    }

    @Override
    public String getDescription() {
        switch (type) {
            case FLAT:
                return "Deal +" + value + " extra damage";
            case PERCENT:
                return "Deal +" + (int)(value * 100) + "% extra damage";
            case MULTIPLY:
                return "Multiply damage by " + value;
            case TRUE_DAMAGE:
                return "Deal " + value + " true damage";
            default:
                return "Unknown damage effect";
        }
    }

    @Override
    public int getPriority() {
        // 伤害倍率效果应该最先执行
        if (type == DamageType.MULTIPLY) {
            return 10;
        }
        return 100;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 固定额外伤害
     */
    public static DamageEffect flat(float damage) {
        return new DamageEffect(DamageType.FLAT, damage, false, null);
    }

    public static DamageEffect flat(float damage, boolean showMessage) {
        return new DamageEffect(DamageType.FLAT, damage, showMessage, null);
    }

    /**
     * 百分比额外伤害
     *
     * @param percent 百分比 (0.0 ~ 1.0)
     */
    public static DamageEffect percent(float percent) {
        return new DamageEffect(DamageType.PERCENT, percent, false, null);
    }

    public static DamageEffect percent(float percent, boolean showMessage) {
        return new DamageEffect(DamageType.PERCENT, percent, showMessage, null);
    }

    /**
     * 伤害倍率
     */
    public static DamageEffect multiply(float multiplier) {
        return new DamageEffect(DamageType.MULTIPLY, multiplier, false, null);
    }

    public static DamageEffect multiply(float multiplier, boolean showMessage) {
        return new DamageEffect(DamageType.MULTIPLY, multiplier, showMessage, null);
    }

    /**
     * 真实伤害
     */
    public static DamageEffect trueDamage(float damage) {
        return new DamageEffect(DamageType.TRUE_DAMAGE, damage, false, null);
    }

    public static DamageEffect trueDamage(float damage, boolean showMessage) {
        return new DamageEffect(DamageType.TRUE_DAMAGE, damage, showMessage, null);
    }
}
