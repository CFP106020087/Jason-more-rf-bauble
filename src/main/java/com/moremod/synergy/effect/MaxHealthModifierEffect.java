package com.moremod.synergy.effect;

import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.synergy.core.SynergyContext;
import com.moremod.synergy.core.SynergyPlayerState;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;

import java.util.UUID;

/**
 * 最大生命值修改效果
 *
 * 修改玩家的最大 HP 上限（百分比）。
 */
public class MaxHealthModifierEffect implements ISynergyEffect {

    private static final UUID SYNERGY_HEALTH_MODIFIER_UUID =
            UUID.fromString("8b7a9e3f-1234-4567-89ab-cdef01234567");

    private final float percentChange;
    private final boolean permanent;  // 是否为永久修改（本次游戏内）

    public MaxHealthModifierEffect(float percentChange, boolean permanent) {
        this.percentChange = percentChange;
        this.permanent = permanent;
    }

    @Override
    public void apply(SynergyContext context) {
        EntityPlayer player = context.getPlayer();
        SynergyPlayerState state = SynergyPlayerState.get(player);

        if (permanent) {
            state.addMaxHealthModifier(percentChange);
        }

        // 应用属性修改
        applyHealthModifier(player, state.getMaxHealthModifier());
    }

    private void applyHealthModifier(EntityPlayer player, float totalModifier) {
        IAttributeInstance healthAttribute = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);

        // 移除旧的修改器
        AttributeModifier existing = healthAttribute.getModifier(SYNERGY_HEALTH_MODIFIER_UUID);
        if (existing != null) {
            healthAttribute.removeModifier(existing);
        }

        // 添加新的修改器
        if (totalModifier != 0) {
            AttributeModifier modifier = new AttributeModifier(
                    SYNERGY_HEALTH_MODIFIER_UUID,
                    "Synergy Health Modifier",
                    totalModifier / 100.0,  // 转换为乘数
                    2  // Operation 2 = 乘法
            );
            healthAttribute.applyModifier(modifier);

            // 如果当前 HP 超过新的最大值，调整
            float newMax = player.getMaxHealth();
            if (player.getHealth() > newMax) {
                player.setHealth(newMax);
            }
        }
    }

    @Override
    public String getDescription() {
        String prefix = percentChange >= 0 ? "+" : "";
        return prefix + percentChange + "% max HP" + (permanent ? " (permanent)" : "");
    }

    public static MaxHealthModifierEffect reduce(float percent) {
        return new MaxHealthModifierEffect(-percent, true);
    }

    public static MaxHealthModifierEffect increase(float percent) {
        return new MaxHealthModifierEffect(percent, true);
    }

    public static MaxHealthModifierEffect temporary(float percent) {
        return new MaxHealthModifierEffect(percent, false);
    }
}
