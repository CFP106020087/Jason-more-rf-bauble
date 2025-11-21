package com.moremod.synergy.effect;

import com.moremod.synergy.api.IInstalledModuleView;
import com.moremod.synergy.api.ISynergyEffect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;

/**
 * 伤害修改效果 - 修改伤害值
 *
 * 说明：
 * - 用于增加或减少伤害
 * - 只在 LivingHurtEvent 中有效
 * - 支持固定值和百分比增伤
 */
public class DamageModifierEffect implements ISynergyEffect {

    private final float multiplier;      // 伤害倍率（1.0 = 不变，1.5 = +50%）
    private final float flatBonus;       // 固定伤害加成
    private final boolean showMessage;    // 是否显示消息

    public DamageModifierEffect(float multiplier) {
        this(multiplier, 0f, false);
    }

    public DamageModifierEffect(float multiplier, float flatBonus) {
        this(multiplier, flatBonus, false);
    }

    public DamageModifierEffect(float multiplier, float flatBonus, boolean showMessage) {
        this.multiplier = multiplier;
        this.flatBonus = flatBonus;
        this.showMessage = showMessage;
    }

    @Override
    public boolean apply(EntityPlayer player, List<IInstalledModuleView> modules, Event event) {
        if (!(event instanceof LivingHurtEvent)) {
            return false;
        }

        LivingHurtEvent hurtEvent = (LivingHurtEvent) event;

        // 检查伤害源是否来自玩家
        if (hurtEvent.getSource().getTrueSource() != player) {
            return false;
        }

        float originalDamage = hurtEvent.getAmount();
        float newDamage = originalDamage * multiplier + flatBonus;

        // 确保伤害不为负
        newDamage = Math.max(0, newDamage);

        hurtEvent.setAmount(newDamage);

        // 显示消息
        if (showMessage && !player.world.isRemote) {
            float bonus = newDamage - originalDamage;
            if (bonus > 0 && player.world.rand.nextInt(5) == 0) {
                player.sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.LIGHT_PURPLE + "⚔ Synergy 伤害加成: +" +
                                        String.format("%.1f", bonus)
                        ),
                        true
                );
            }
        }

        return true;
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("DamageModifier[");
        if (multiplier != 1.0f) {
            sb.append("x").append(String.format("%.2f", multiplier));
        }
        if (flatBonus != 0) {
            if (multiplier != 1.0f) sb.append(" ");
            sb.append(flatBonus > 0 ? "+" : "").append(String.format("%.1f", flatBonus));
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int getPriority() {
        // 伤害修改应该在较低优先级执行（让其他效果先计算）
        return 200;
    }
}
