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
 * 负面药水效果 - Drawback 负面效果
 *
 * 说明：
 * - 给予玩家负面药水效果（如虚弱、缓慢、饥饿等）
 * - 可设置效果类型、等级和持续时间
 * - 用于平衡强力 Synergy
 */
public class DebuffEffect implements ISynergyEffect {

    private final Potion potion;             // 药水效果类型
    private final int amplifier;             // 效果等级（0 = I, 1 = II, ...）
    private final int duration;              // 持续时间（tick）
    private final int tickInterval;          // 触发间隔（tick）
    private final boolean showMessage;       // 是否显示消息

    /**
     * 创建负面药水效果
     *
     * @param potion 药水效果类型
     * @param amplifier 效果等级（0 = I, 1 = II, ...）
     * @param duration 持续时间（tick）
     */
    public DebuffEffect(Potion potion, int amplifier, int duration) {
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
    public DebuffEffect(Potion potion, int amplifier, int duration, int tickInterval, boolean showMessage) {
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
        PotionEffect effect = new PotionEffect(potion, duration, amplifier, false, false);
        player.addPotionEffect(effect);

        // 显示消息
        if (showMessage && player.world.getTotalWorldTime() % 100 == 0) {
            player.sendStatusMessage(
                    new TextComponentString(
                            TextFormatting.DARK_PURPLE + "⚠ Synergy Drawback: " +
                                    potion.getName() + " " + (amplifier + 1)
                    ),
                    true
            );
        }

        return true;
    }

    @Override
    public String getDescription() {
        return String.format("Debuff[%s %d for %ds]",
                potion != null ? potion.getName() : "Unknown",
                amplifier + 1,
                duration / 20);
    }

    @Override
    public int getPriority() {
        // Drawback 应该在最后执行
        return 500;
    }

    // 便捷工厂方法

    /**
     * 虚弱效果
     *
     * @param level 等级（0 = I, 1 = II, ...）
     * @param durationSeconds 持续时间（秒）
     * @return DebuffEffect 对象
     */
    public static DebuffEffect weakness(int level, int durationSeconds) {
        return new DebuffEffect(
                Potion.getPotionFromResourceLocation("minecraft:weakness"),
                level,
                durationSeconds * 20
        );
    }

    /**
     * 缓慢效果
     *
     * @param level 等级
     * @param durationSeconds 持续时间（秒）
     * @return DebuffEffect 对象
     */
    public static DebuffEffect slowness(int level, int durationSeconds) {
        return new DebuffEffect(
                Potion.getPotionFromResourceLocation("minecraft:slowness"),
                level,
                durationSeconds * 20
        );
    }

    /**
     * 饥饿效果
     *
     * @param level 等级
     * @param durationSeconds 持续时间（秒）
     * @return DebuffEffect 对象
     */
    public static DebuffEffect hunger(int level, int durationSeconds) {
        return new DebuffEffect(
                Potion.getPotionFromResourceLocation("minecraft:hunger"),
                level,
                durationSeconds * 20
        );
    }

    /**
     * 挖掘疲劳效果
     *
     * @param level 等级
     * @param durationSeconds 持续时间（秒）
     * @return DebuffEffect 对象
     */
    public static DebuffEffect miningFatigue(int level, int durationSeconds) {
        return new DebuffEffect(
                Potion.getPotionFromResourceLocation("minecraft:mining_fatigue"),
                level,
                durationSeconds * 20
        );
    }
}
