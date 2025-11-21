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
 * 吸血效果 - Lifesteal Effect
 *
 * 说明：
 * - 攻击敌人时恢复生命值
 * - 恢复量基于造成的伤害
 * - 只在 LivingHurtEvent 中有效，且玩家是攻击者
 */
public class LifestealEffect implements ISynergyEffect {

    private final float healPercentage;      // 吸血百分比（0.2 = 恢复伤害的 20%）
    private final float maxHealPerHit;       // 单次最大恢复量
    private final boolean showMessage;       // 是否显示消息

    /**
     * 创建吸血效果
     *
     * @param healPercentage 吸血百分比（0.2 = 恢复伤害的 20%）
     */
    public LifestealEffect(float healPercentage) {
        this(healPercentage, 5.0f, false);
    }

    /**
     * 完整构造器
     *
     * @param healPercentage 吸血百分比
     * @param maxHealPerHit 单次最大恢复量
     * @param showMessage 是否显示消息
     */
    public LifestealEffect(float healPercentage, float maxHealPerHit, boolean showMessage) {
        this.healPercentage = Math.max(0, healPercentage);
        this.maxHealPerHit = Math.max(0, maxHealPerHit);
        this.showMessage = showMessage;
    }

    @Override
    public boolean apply(EntityPlayer player, List<IInstalledModuleView> modules, Event event) {
        if (!(event instanceof LivingHurtEvent)) {
            return false;
        }

        LivingHurtEvent hurtEvent = (LivingHurtEvent) event;

        // 检查攻击者是否是玩家
        if (hurtEvent.getSource().getTrueSource() != player) {
            return false;
        }

        // 检查受伤者不是玩家自己
        if (hurtEvent.getEntityLiving() == player) {
            return false;
        }

        float damage = hurtEvent.getAmount();
        float healAmount = Math.min(damage * healPercentage, maxHealPerHit);

        // 确保不超过最大生命值
        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(healAmount);

            // 显示消息
            if (showMessage && !player.world.isRemote && player.world.rand.nextInt(3) == 0) {
                player.sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.DARK_RED + "♥ Lifesteal: +" +
                                        String.format("%.1f", healAmount) + " HP"
                        ),
                        true
                );
            }

            return true;
        }

        return false;
    }

    @Override
    public String getDescription() {
        return String.format("Lifesteal[%.0f%% up to %.1f HP]",
                healPercentage * 100,
                maxHealPerHit);
    }

    @Override
    public int getPriority() {
        // 吸血应该在伤害加成之后执行
        return 200;
    }
}
