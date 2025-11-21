package com.moremod.synergy.effect;

import com.moremod.synergy.api.IInstalledModuleView;
import com.moremod.synergy.api.ISynergyEffect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;

/**
 * 正面药水效果 - Buff 正面效果
 *
 * 说明：
 * - 给予玩家正面药水效果（如速度、力量、抗性提升等）
 * - 可设置效果类型、等级和持续时间
 * - 用于增强 Synergy 的正面效果
 */
public class PotionBuffEffect implements ISynergyEffect {

    private final Potion potion;             // 药水效果类型
    private final int amplifier;             // 效果等级（0 = I, 1 = II, ...）
    private final int duration;              // 持续时间（tick）
    private final int tickInterval;          // 触发间隔（tick）
    private final boolean showMessage;       // 是否显示消息

    /**
     * 创建正面药水效果
     *
     * @param potion 药水效果类型
     * @param amplifier 效果等级（0 = I, 1 = II, ...）
     * @param duration 持续时间（tick）
     */
    public PotionBuffEffect(Potion potion, int amplifier, int duration) {
        this(potion, amplifier, duration, 20, false);
    }

    /**
     * 完整构造器
     *
     * @param potion 药水效果类型
     * @param amplifier 效果等级
     * @param duration 持续时间（tick）
     * @param tickInterval 触发间隔（tick）
     * @param showMessage 是否显示消息
     */
    public PotionBuffEffect(Potion potion, int amplifier, int duration, int tickInterval, boolean showMessage) {
        this.potion = potion;
        this.amplifier = Math.max(0, amplifier);
        this.duration = Math.max(1, duration);
        this.tickInterval = Math.max(1, tickInterval);
        this.showMessage = showMessage;
    }

    @Override
    public boolean apply(EntityPlayer player, List<IInstalledModuleView> modules, Event event) {
        if (player == null || player.world.isRemote || potion == null) {
            return false;
        }

        // 检查触发间隔
        if (player.world.getTotalWorldTime() % tickInterval != 0) {
            return false;
        }

        // 应用药水效果
        PotionEffect effect = new PotionEffect(potion, duration, amplifier, false, true);
        player.addPotionEffect(effect);

        // 显示消息
        if (showMessage && player.world.getTotalWorldTime() % 100 == 0) {
            player.sendStatusMessage(
                    new TextComponentString(
                            TextFormatting.GREEN + "✓ Synergy Buff: " +
                                    potion.getName() + " " + (amplifier + 1)
                    ),
                    true
            );
        }

        return true;
    }

    @Override
    public String getDescription() {
        return String.format("Buff[%s %d for %ds]",
                potion != null ? potion.getName() : "Unknown",
                amplifier + 1,
                duration / 20);
    }

    @Override
    public int getPriority() {
        // Buff 应该早于 Drawback 执行
        return 100;
    }
}
