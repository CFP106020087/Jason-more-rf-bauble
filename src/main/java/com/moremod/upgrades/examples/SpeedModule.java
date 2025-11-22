package com.moremod.upgrades.examples;

import com.moremod.upgrades.platform.BaseUpgradeModule;
import com.moremod.upgrades.platform.ModuleContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;

/**
 * 速度提升模块示例
 *
 * 功能：
 * - 给玩家添加速度药水效果
 * - 等级越高速度越快
 * - 自动消耗能量
 *
 * 这是一个完整的模块示例，展示了如何使用新的包装层系统。
 */
public class SpeedModule extends BaseUpgradeModule {

    // 模块ID（必须唯一）
    public static final String MODULE_ID = "SPEED_BOOST";

    // 单例实例（用于注册）
    public static final SpeedModule INSTANCE = new SpeedModule();

    /**
     * 构造函数
     * - 模块ID: SPEED_BOOST
     * - 显示名称: 速度提升
     * - 最大等级: 5
     */
    private SpeedModule() {
        super(MODULE_ID, "速度提升", 5);
    }

    // ===== 核心逻辑 =====

    @Override
    protected void onModuleTick(@Nonnull ModuleContext context) {
        // 只在服务端执行
        if (context.isClientSide()) {
            return;
        }

        EntityPlayer player = context.getPlayer();
        int level = context.getEffectiveLevel();

        // 每 20 tick（1秒）应用一次速度效果
        if (context.getWorldTime() % 20 == 0) {
            applySpeedEffect(player, level);
        }

        // 示例：使用自定义数据存储
        // 记录总 tick 次数
        int totalTicks = context.getCustomInt("totalTicks", 0);
        context.setCustomInt("totalTicks", totalTicks + 1);

        // 每 100 tick 显示一次状态
        if (totalTicks > 0 && totalTicks % 100 == 0) {
            sendStatusMessage(player,
                    TextFormatting.AQUA + "速度模块运行中 (Lv." + level + ")");
        }
    }

    @Override
    protected void onModuleEquip(@Nonnull ModuleContext context) {
        // 装备时立即应用效果
        EntityPlayer player = context.getPlayer();
        int level = context.getLevel();

        applySpeedEffect(player, level);

        // 重置计数器
        context.setCustomInt("totalTicks", 0);

        sendMessage(player,
                TextFormatting.GREEN + "⚡ 速度提升模块已激活！" +
                TextFormatting.GRAY + " (等级 " + level + ")");
    }

    @Override
    protected void onModuleUnequip(@Nonnull ModuleContext context) {
        // 卸载时移除速度效果
        EntityPlayer player = context.getPlayer();
        player.removePotionEffect(MobEffects.SPEED);

        sendMessage(player,
                TextFormatting.GRAY + "速度提升模块已停用");
    }

    // ===== 配置 =====

    @Override
    protected int getBaseEnergyCost() {
        // 每级每 tick 消耗 5 RF
        return 5;
    }

    @Override
    protected boolean shouldSendEquipMessage() {
        // 不使用默认装备消息（我们自己发送了）
        return false;
    }

    @Override
    protected boolean isDebugMode() {
        // 开启调试模式（生产环境应关闭）
        return false;
    }

    // ===== 辅助方法 =====

    /**
     * 应用速度效果
     * - 等级1: 速度 I  (20% 加速)
     * - 等级2: 速度 I  (持续时间更长)
     * - 等级3: 速度 II (40% 加速)
     * - 等级4: 速度 II (持续时间更长)
     * - 等级5: 速度 III (60% 加速)
     */
    private void applySpeedEffect(EntityPlayer player, int level) {
        int amplifier = (level - 1) / 2;  // 0, 0, 1, 1, 2
        int duration = 40 + (level * 10);  // 2秒 + 等级*0.5秒

        PotionEffect speedEffect = new PotionEffect(
                MobEffects.SPEED,     // 效果类型
                duration,             // 持续时间（tick）
                amplifier,            // 等级（0=I, 1=II, 2=III）
                true,                 // 环境效果
                false                 // 显示粒子
        );

        player.addPotionEffect(speedEffect);
    }

    @Override
    public String toString() {
        return "SpeedModule{maxLevel=5, energyCost=5/tick/level}";
    }
}
