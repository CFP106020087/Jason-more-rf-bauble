package com.moremod.upgrades.platform;

import com.moremod.upgrades.api.IUpgradeModule;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nonnull;

/**
 * 升级模块基类
 *
 * 功能：
 * - 提供 IUpgradeModule 的默认实现
 * - 简化新模块开发
 * - 提供便捷的工具方法
 *
 * 使用示例：
 * <pre>
 * public class SpeedModule extends BaseUpgradeModule {
 *     public SpeedModule() {
 *         super("SPEED_BOOST", "速度提升", 5);
 *     }
 *
 *     {@literal @}Override
 *     protected void onModuleTick(ModuleContext ctx) {
 *         // 你的逻辑
 *     }
 * }
 * </pre>
 */
public abstract class BaseUpgradeModule implements IUpgradeModule {

    private final String moduleId;
    private final String displayName;
    private final int maxLevel;

    protected BaseUpgradeModule(@Nonnull String moduleId, @Nonnull String displayName, int maxLevel) {
        this.moduleId = moduleId.toUpperCase();
        this.displayName = displayName;
        this.maxLevel = maxLevel;
    }

    // ===== IUpgradeModule 实现 =====

    @Override
    @Nonnull
    public String getModuleId() {
        return moduleId;
    }

    @Override
    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public void onTick(@Nonnull EntityPlayer player, @Nonnull ItemStack core, int level) {
        if (level <= 0) {
            return;
        }

        try {
            ModuleState state = ModuleDataStorage.loadState(core, moduleId);
            ModuleContext context = new ModuleContext(player, core, state);

            // 调用子类的实现
            onModuleTick(context);

            // 保存状态（如果有修改）
            ModuleDataStorage.saveState(core, state);

        } catch (Throwable t) {
            System.err.println("[" + moduleId + "] Tick 失败: " + t.getMessage());
            if (isDebugMode()) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public void onEquip(@Nonnull EntityPlayer player, @Nonnull ItemStack core, int level) {
        if (player.world.isRemote) {
            return;
        }

        try {
            ModuleState state = ModuleDataStorage.loadState(core, moduleId);
            ModuleContext context = new ModuleContext(player, core, state);

            // 调用子类的实现
            onModuleEquip(context);

            // 发送装备消息
            if (shouldSendEquipMessage()) {
                sendMessage(player, TextFormatting.GREEN + "✓ " + displayName + " 已激活 (Lv." + level + ")");
            }

            // 保存状态
            ModuleDataStorage.saveState(core, state);

        } catch (Throwable t) {
            System.err.println("[" + moduleId + "] 装备失败: " + t.getMessage());
            if (isDebugMode()) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public void onUnequip(@Nonnull EntityPlayer player, @Nonnull ItemStack core, int level) {
        if (player.world.isRemote) {
            return;
        }

        try {
            ModuleState state = ModuleDataStorage.loadState(core, moduleId);
            ModuleContext context = new ModuleContext(player, core, state);

            // 调用子类的实现
            onModuleUnequip(context);

            // 发送卸载消息
            if (shouldSendUnequipMessage()) {
                sendMessage(player, TextFormatting.GRAY + "✗ " + displayName + " 已停用");
            }

            // 保存状态
            ModuleDataStorage.saveState(core, state);

        } catch (Throwable t) {
            System.err.println("[" + moduleId + "] 卸载失败: " + t.getMessage());
            if (isDebugMode()) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 默认：每级消耗 10 RF/tick
        return getBaseEnergyCost() * level;
    }

    @Override
    public boolean canRunWithEnergyStatus(@Nonnull EnergyDepletionManager.EnergyStatus status) {
        // 默认：只在正常状态下运行
        return status == EnergyDepletionManager.EnergyStatus.NORMAL;
    }

    @Override
    public void handleEvent(@Nonnull Event event, @Nonnull EntityPlayer player,
                          @Nonnull ItemStack core, int level) {
        if (level <= 0) {
            return;
        }

        try {
            ModuleState state = ModuleDataStorage.loadState(core, moduleId);
            ModuleContext context = new ModuleContext(player, core, state);

            // 调用子类的实现
            onModuleEvent(event, context);

            // 保存状态
            ModuleDataStorage.saveState(core, state);

        } catch (Throwable t) {
            System.err.println("[" + moduleId + "] 事件处理失败: " + t.getMessage());
            if (isDebugMode()) {
                t.printStackTrace();
            }
        }
    }

    // ===== 子类需要实现的方法 =====

    /**
     * 模块 tick 逻辑（子类实现）
     */
    protected void onModuleTick(@Nonnull ModuleContext context) {
        // 默认不执行任何操作
    }

    /**
     * 模块装备逻辑（子类可选实现）
     */
    protected void onModuleEquip(@Nonnull ModuleContext context) {
        // 默认不执行任何操作
    }

    /**
     * 模块卸载逻辑（子类可选实现）
     */
    protected void onModuleUnequip(@Nonnull ModuleContext context) {
        // 默认不执行任何操作
    }

    /**
     * 模块事件处理（子类可选实现）
     */
    protected void onModuleEvent(@Nonnull Event event, @Nonnull ModuleContext context) {
        // 默认不处理任何事件
    }

    // ===== 配置方法（子类可覆盖） =====

    /**
     * 获取基础能量消耗（每级）
     */
    protected int getBaseEnergyCost() {
        return 10;
    }

    /**
     * 是否发送装备消息
     */
    protected boolean shouldSendEquipMessage() {
        return true;
    }

    /**
     * 是否发送卸载消息
     */
    protected boolean shouldSendUnequipMessage() {
        return false;
    }

    /**
     * 是否开启调试模式
     */
    protected boolean isDebugMode() {
        return false;
    }

    // ===== 工具方法 =====

    /**
     * 发送消息给玩家
     */
    protected void sendMessage(@Nonnull EntityPlayer player, @Nonnull String message) {
        if (!player.world.isRemote) {
            player.sendMessage(new TextComponentString(message));
        }
    }

    /**
     * 发送状态栏消息
     */
    protected void sendStatusMessage(@Nonnull EntityPlayer player, @Nonnull String message) {
        if (!player.world.isRemote) {
            player.sendStatusMessage(new TextComponentString(message), true);
        }
    }

    /**
     * 检查冷却（便捷方法）
     */
    protected boolean checkCooldown(@Nonnull ModuleContext context, long cooldownTicks) {
        if (context.isOnCooldown()) {
            return false;
        }
        context.setCooldown(cooldownTicks);
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s{id=%s, maxLevel=%d}", getClass().getSimpleName(), moduleId, maxLevel);
    }
}
