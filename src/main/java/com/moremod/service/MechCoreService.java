package com.moremod.service;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.IMechCoreModule;
import com.moremod.upgrades.ModuleRegistry;
import com.moremod.api.event.EventService;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mechanical Core 全局服务
 *
 * 职责：
 *  ✓ 提供高层业务逻辑 API
 *  ✓ 协调 Capability、Module、Event
 *  ✓ 统一错误处理
 *  ✓ 权限验证
 *  ✓ 事件发布
 */
public class MechCoreService {

    private static final Logger logger = LogManager.getLogger(MechCoreService.class);

    private static final MechCoreService INSTANCE = new MechCoreService();

    public static MechCoreService getInstance() {
        return INSTANCE;
    }

    private MechCoreService() {
        // Singleton
    }

    // ────────────────────────────────────────────────────────────
    // 模块升级业务逻辑
    // ────────────────────────────────────────────────────────────

    /**
     * 升级模块
     * @return 是否成功
     */
    public boolean upgradeModule(EntityPlayer player, String moduleId, int targetLevel) {
        IMechCoreData data = getCapability(player);
        if (data == null) {
            sendError(player, "未找到机械核心数据");
            return false;
        }

        // 1. 验证模块
        IMechCoreModule module = ModuleRegistry.getNew(moduleId);
        if (module == null) {
            sendError(player, "未知模块: " + moduleId);
            logger.warn("Unknown module: {}", moduleId);
            return false;
        }

        // 2. 验证等级
        int currentLevel = data.getModuleLevel(moduleId);
        if (targetLevel <= currentLevel) {
            sendError(player, "目标等级必须高于当前等级");
            return false;
        }

        if (targetLevel > module.getMaxLevel()) {
            sendError(player, "超过最大等级: " + module.getMaxLevel());
            return false;
        }

        // 3. 检查材料（TODO: 实现材料系统）
        if (!hasMaterials(player, moduleId, targetLevel)) {
            sendError(player, "材料不足");
            return false;
        }

        // 4. 消耗材料
        consumeMaterials(player, moduleId, targetLevel);

        // 5. 升级
        int oldLevel = currentLevel;
        data.setModuleLevel(moduleId, targetLevel);

        // 6. 触发模块回调
        module.onLevelChanged(player, data, oldLevel, targetLevel);

        // 7. 发布事件（TODO: 创建 MechCoreModuleUpgradeEvent）
        // EventService.post(new MechCoreModuleUpgradeEvent(player, moduleId, oldLevel, targetLevel, data));

        // 8. 反馈
        sendSuccess(player, String.format(
            "%s 升级至 Lv.%d",
            module.getDisplayName(),
            targetLevel
        ));

        logger.info("Player {} upgraded module {} from {} to {}",
                   player.getName(), moduleId, oldLevel, targetLevel);

        // 9. 标记同步
        data.markDirty();

        return true;
    }

    /**
     * 激活/停用模块
     */
    public boolean toggleModule(EntityPlayer player, String moduleId) {
        IMechCoreData data = getCapability(player);
        if (data == null) return false;

        IMechCoreModule module = ModuleRegistry.getNew(moduleId);
        if (module == null) {
            sendError(player, "未知模块: " + moduleId);
            return false;
        }

        boolean currentlyActive = data.isModuleActive(moduleId);
        boolean newState = !currentlyActive;

        data.setModuleActive(moduleId, newState);

        if (newState) {
            module.onActivate(player, data, data.getModuleLevel(moduleId));
            sendSuccess(player, module.getDisplayName() + " 已激活");
        } else {
            module.onDeactivate(player, data);
            sendSuccess(player, module.getDisplayName() + " 已停用");
        }

        data.markDirty();

        return true;
    }

    // ────────────────────────────────────────────────────────────
    // 能量管理
    // ────────────────────────────────────────────────────────────

    /**
     * 充能
     */
    public int chargeEnergy(EntityPlayer player, int amount) {
        IMechCoreData data = getCapability(player);
        if (data == null) return 0;

        int received = data.receiveEnergy(amount);
        if (received > 0) {
            data.markDirty();
            logger.debug("Player {} charged {} RF", player.getName(), received);
        }

        return received;
    }

    /**
     * 消耗能量（带权限检查）
     */
    public boolean consumeEnergy(EntityPlayer player, int amount, String reason) {
        IMechCoreData data = getCapability(player);
        if (data == null) return false;

        if (data.getEnergy() < amount) {
            sendError(player, "能量不足: 需要 " + amount + " RF");
            return false;
        }

        boolean success = data.consumeEnergy(amount);
        if (success) {
            data.markDirty();
            logger.debug("Player {} consumed {} RF for: {}", player.getName(), amount, reason);
        }

        return success;
    }

    /**
     * 获取能量
     */
    public int getEnergy(EntityPlayer player) {
        IMechCoreData data = getCapability(player);
        return data != null ? data.getEnergy() : 0;
    }

    /**
     * 获取最大能量
     */
    public int getMaxEnergy(EntityPlayer player) {
        IMechCoreData data = getCapability(player);
        return data != null ? data.getMaxEnergy() : 0;
    }

    /**
     * 设置最大能量
     */
    public void setMaxEnergy(EntityPlayer player, int max) {
        IMechCoreData data = getCapability(player);
        if (data != null) {
            data.setMaxEnergy(max);
            data.markDirty();
        }
    }

    // ────────────────────────────────────────────────────────────
    // 模块查询
    // ────────────────────────────────────────────────────────────

    /**
     * 获取模块等级
     */
    public int getModuleLevel(EntityPlayer player, String moduleId) {
        IMechCoreData data = getCapability(player);
        return data != null ? data.getModuleLevel(moduleId) : 0;
    }

    /**
     * 模块是否激活
     */
    public boolean isModuleActive(EntityPlayer player, String moduleId) {
        IMechCoreData data = getCapability(player);
        return data != null && data.isModuleActive(moduleId);
    }

    // ────────────────────────────────────────────────────────────
    // 工具方法
    // ────────────────────────────────────────────────────────────

    /**
     * 获取玩家的 Capability
     */
    private IMechCoreData getCapability(EntityPlayer player) {
        if (player == null) return null;
        return player.getCapability(IMechCoreData.CAPABILITY, null);
    }

    /**
     * 发送成功消息
     */
    private void sendSuccess(EntityPlayer player, String message) {
        if (player != null && !player.world.isRemote) {
            player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ " + message
            ));
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(EntityPlayer player, String message) {
        if (player != null && !player.world.isRemote) {
            player.sendMessage(new TextComponentString(
                TextFormatting.RED + "✗ " + message
            ));
        }
    }

    /**
     * 检查材料（TODO: 实现）
     */
    private boolean hasMaterials(EntityPlayer player, String moduleId, int level) {
        // TODO: 实现材料检查逻辑
        return true;
    }

    /**
     * 消耗材料（TODO: 实现）
     */
    private void consumeMaterials(EntityPlayer player, String moduleId, int level) {
        // TODO: 实现材料消耗逻辑
    }
}
