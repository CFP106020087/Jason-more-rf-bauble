package com.moremod.synergy.effect;

import com.moremod.synergy.api.IInstalledModuleView;
import com.moremod.synergy.api.ISynergyEffect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;

/**
 * 生命值流失效果 - Drawback 负面效果
 *
 * 说明：
 * - 持续扣除玩家生命值
 * - 可设置每秒流失量和触发间隔
 * - 用于平衡强力 Synergy 的负面效果
 */
public class HealthDrainEffect implements ISynergyEffect {

    private final float damagePerSecond;     // 每秒伤害量
    private final int tickInterval;          // 触发间隔（tick）
    private final boolean showMessage;       // 是否显示消息
    private final String damageName;         // 伤害来源名称

    /**
     * 创建生命流失效果
     *
     * @param damagePerSecond 每秒伤害量（0.5 = 1/4 颗心）
     */
    public HealthDrainEffect(float damagePerSecond) {
        this(damagePerSecond, 20, false, "Synergy Drawback");
    }

    /**
     * 完整构造器
     *
     * @param damagePerSecond 每秒伤害量
     * @param tickInterval 触发间隔（tick）
     * @param showMessage 是否显示消息
     * @param damageName 伤害来源名称
     */
    public HealthDrainEffect(float damagePerSecond, int tickInterval, boolean showMessage, String damageName) {
        this.damagePerSecond = damagePerSecond;
        this.tickInterval = Math.max(1, tickInterval);
        this.showMessage = showMessage;
        this.damageName = damageName;
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

        // 计算实际伤害（根据间隔调整）
        float actualDamage = damagePerSecond * (tickInterval / 20.0f);

        // 确保玩家至少保留 0.5 生命值（不会直接杀死）
        if (player.getHealth() <= 1.0f) {
            return false;
        }

        // 应用伤害
        DamageSource source = new DamageSource(damageName).setDamageBypassesArmor().setDamageIsAbsolute();
        player.attackEntityFrom(source, actualDamage);

        // 显示消息
        if (showMessage && player.world.getTotalWorldTime() % 100 == 0) {
            player.sendStatusMessage(
                    new TextComponentString(
                            TextFormatting.RED + "⚠ Synergy Drawback: 生命流失"
                    ),
                    true
            );
        }

        // 粒子效果
        if (player.world.rand.nextInt(3) == 0) {
            for (int i = 0; i < 3; i++) {
                player.world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.DAMAGE_INDICATOR,
                        player.posX + (player.getRNG().nextDouble() - 0.5) * 0.5,
                        player.posY + 1.0 + player.getRNG().nextDouble() * 0.5,
                        player.posZ + (player.getRNG().nextDouble() - 0.5) * 0.5,
                        0, 0, 0
                );
            }
        }

        return true;
    }

    @Override
    public String getDescription() {
        return String.format("HealthDrain[%.1f HP/s]", damagePerSecond);
    }

    @Override
    public int getPriority() {
        // Drawback 应该在最后执行
        return 500;
    }
}
