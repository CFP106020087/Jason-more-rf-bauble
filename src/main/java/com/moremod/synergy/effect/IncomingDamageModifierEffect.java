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
 * 受伤伤害修改效果 - Drawback 负面效果
 *
 * 说明：
 * - 修改玩家受到的伤害（增加或减少）
 * - 只在 LivingHurtEvent 中有效，且玩家是受害者
 * - 用于实现"玻璃大炮"类型的 Synergy（高输出高脆皮）
 */
public class IncomingDamageModifierEffect implements ISynergyEffect {

    private final float multiplier;      // 伤害倍率（1.0 = 不变，1.3 = +30%）
    private final float flatBonus;       // 固定伤害加成
    private final boolean showMessage;   // 是否显示消息

    /**
     * 创建受伤伤害修改效果
     *
     * @param multiplier 伤害倍率（1.3 = 受到的伤害 +30%）
     */
    public IncomingDamageModifierEffect(float multiplier) {
        this(multiplier, 0f, false);
    }

    /**
     * 完整构造器
     *
     * @param multiplier 伤害倍率
     * @param flatBonus 固定伤害加成
     * @param showMessage 是否显示消息
     */
    public IncomingDamageModifierEffect(float multiplier, float flatBonus, boolean showMessage) {
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

        // 检查受伤者是否是玩家
        if (hurtEvent.getEntityLiving() != player) {
            return false;
        }

        float originalDamage = hurtEvent.getAmount();
        float newDamage = originalDamage * multiplier + flatBonus;

        // 确保伤害不为负
        newDamage = Math.max(0, newDamage);

        hurtEvent.setAmount(newDamage);

        // 显示消息
        if (showMessage && !player.world.isRemote && multiplier > 1.0f) {
            if (player.world.rand.nextInt(5) == 0) {
                float increase = newDamage - originalDamage;
                player.sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.RED + "⚠ Synergy Drawback: 受伤 +" +
                                        String.format("%.1f", increase)
                        ),
                        true
                );
            }
        }

        return true;
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("IncomingDamage[");
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
        // Drawback 应该在最后执行
        return 500;
    }
}
