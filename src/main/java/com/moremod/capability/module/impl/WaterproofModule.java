package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import com.moremod.upgrades.WetnessSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 防水模块
 *
 * 功能：
 *  - 保护机械核心免受水损害
 *  - 集成 WetnessSystem（潮湿值管理系统）
 *  - Lv.1: 减少 50% 潮湿值增加
 *  - Lv.2: 完全免疫潮湿
 *  - Lv.3: 完全免疫 + 快速干燥
 *
 * 潮湿值系统：
 *  - 潮湿值范围：0-100
 *  - 雨天增加：4/秒（雷雨 5/秒）
 *  - 自然干燥：2/秒（离开雨 3 秒后）
 *  - 高温加速：4/秒（SimpleDifficulty 温度 >15）
 *  - 故障阈值：80（超过时触发故障效果）
 *
 * 故障效果：
 *  - MALFUNCTION 药水效果（等级随时间和潮湿值升级）
 *  - 能量消耗：50/100/150 RF/秒（Lv.0/1/2）
 *  - 随机短路：损失部分能量（15%/11%/7% 几率）
 *
 * 能量消耗：
 *  - Lv.1: 5 RF/tick
 *  - Lv.2: 10 RF/tick
 *  - Lv.3: 15 RF/tick
 *
 * 注意：
 *  - 此模块作为 WetnessSystem 的包装器
 *  - 保留原有的全局状态管理（确保与其他系统兼容）
 *  - 防水等级通过 WaterproofUpgrade 系统获取
 */
public class WaterproofModule extends AbstractMechCoreModule {

    public static final WaterproofModule INSTANCE = new WaterproofModule();

    private WaterproofModule() {
        super(
            "WATERPROOF_MODULE",
            "防水系统",
            "保护核心免受水损害",
            3  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        String protection;
        switch (newLevel) {
            case 1:
                protection = "部分防护（50% 减免）";
                break;
            case 2:
                protection = "完全防护（免疫潮湿）";
                break;
            case 3:
                protection = "完全防护 + 快速干燥";
                break;
            default:
                protection = "未知等级";
        }

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.AQUA + "✓ 防水系统已激活 - " +
                        TextFormatting.WHITE + protection
        ), true);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.GRAY + "防水系统已关闭"
        ), true);

        // 警告：失去防护后可能很快故障
        if (player.world.isRaining() && player.world.canSeeSky(player.getPosition())) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 警告：失去防水保护，正在淋雨！"
            ), false);
        }
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        // WetnessSystem 会在其自己的 tick 中处理潮湿值更新
        // 这里只需要确保模块激活即可
        // （实际的潮湿值管理由 WetnessSystem 的静态方法处理）

        // Lv.3 的快速干燥效果已经在 WetnessSystem 中通过等级检查实现
        // 不需要在这里额外处理
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        if (newLevel > 0) {
            String message;
            switch (newLevel) {
                case 1:
                    message = "防水系统升级：部分防护（50% 减免）";
                    break;
                case 2:
                    message = "防水系统升级：完全防护（免疫潮湿）";
                    break;
                case 3:
                    message = "防水系统升级：完全防护 + 快速干燥";
                    break;
                default:
                    message = "防水系统已升级";
            }

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.AQUA + message
            ), true);
        }

        // 从高等级降低到低等级时的警告
        if (oldLevel == 2 && newLevel == 1 && player.world.isRaining()) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚠ 防水等级降低，现在只有部分防护"
            ), true);
        } else if (oldLevel >= 2 && newLevel == 0 && player.world.isRaining()) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 失去防水保护！"
            ), true);
        }
    }

    /**
     * 获取当前防水等级
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @return 防水等级（0-3）
     */
    public int getWaterproofLevel(EntityPlayer player, IMechCoreData data) {
        return data.getModuleLevel(getModuleId());
    }

    /**
     * 检查是否免疫潮湿
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @return 是否免疫（Lv.2+）
     */
    public boolean isImmuneToWetness(EntityPlayer player, IMechCoreData data) {
        return data.getModuleLevel(getModuleId()) >= 2;
    }

    /**
     * 获取潮湿值增加倍率
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @return 倍率（Lv.0: 1.0, Lv.1: 0.5, Lv.2+: 0.0）
     */
    public float getWetnessMultiplier(EntityPlayer player, IMechCoreData data) {
        int level = data.getModuleLevel(getModuleId());
        if (level >= 2) {
            return 0.0F;  // 完全免疫
        } else if (level == 1) {
            return 0.5F;  // 50% 减免
        } else {
            return 1.0F;  // 无防护
        }
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 能量消耗：5/10/15 RF/tick
        return level * 5;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        return data.getEnergy() >= getPassiveEnergyCost(data.getModuleLevel(getModuleId()));
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        // 防水模块的状态由 WetnessSystem 全局管理
        // 不需要额外的元数据
        return new NBTTagCompound();
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        // 无需验证
        return true;
    }
}
