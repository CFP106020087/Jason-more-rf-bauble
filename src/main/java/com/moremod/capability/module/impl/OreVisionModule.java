package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 矿物透视模块
 *
 * 功能：
 *  - 高亮显示周围矿石
 *  - Lv.1: 8格范围
 *  - Lv.2: 16格范围
 *  - Lv.3: 24格范围
 *  - Lv.4: 32格范围
 *  - Lv.5: 40格范围
 *
 * 能量消耗：
 *  - 激活消耗：100 RF
 *  - 维持消耗：50 + level*10 RF/s
 *
 * 特性：
 *  - 支持原版矿物和 Mod 矿物（OreDictionary）
 *  - 矿物分类过滤
 *  - 区块扫描缓存
 *  - 客户端渲染（需要渲染系统支持）
 *
 * 注意：
 *  - 实际的渲染逻辑需要在客户端渲染系统中实现
 *  - 此模块提供扫描和能量管理功能
 *  - 通过 onToggle 方法控制开关
 */
public class OreVisionModule extends AbstractMechCoreModule {

    public static final OreVisionModule INSTANCE = new OreVisionModule();

    private OreVisionModule() {
        super(
            "ORE_VISION",
            "矿物透视",
            "高亮显示周围矿石",
            5  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 初始化元数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setBoolean("VISION_ACTIVE", false);
        meta.setInteger("SELECTED_ORE_INDEX", -1);  // -1 表示显示全部
        meta.setLong("LAST_ENERGY_CHECK", 0);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 停用时关闭透视
        disableOreVision(player, data);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        // 矿物透视主要在客户端运行，这里只处理能量消耗
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 如果透视未激活，跳过
        if (!meta.getBoolean("VISION_ACTIVE")) return;

        // 每秒检查一次能量
        if (player.world.getTotalWorldTime() % 20 == 0) {
            int energyCost = 50 + (level * 10);

            if (!data.consumeEnergy(energyCost)) {
                // 能量不足，关闭透视
                disableOreVision(player, data);
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚡ 能量不足，矿物透视已关闭"
                ), true);
            }
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时如果透视激活，更新扫描范围
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        if (meta.getBoolean("VISION_ACTIVE") && newLevel > 0) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.AQUA + "⛏ 矿物透视范围已更新: " + (8 * newLevel) + " 格"
            ), true);
        }
    }

    /**
     * 启用矿物透视
     */
    public void enableOreVision(EntityPlayer player, IMechCoreData data) {
        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "未安装矿物透视模块"
            ), true);
            return;
        }

        // 检查能量
        if (data.getEnergy() < 100) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "⚡ 能量不足，矿物透视无法开启"
            ), true);
            return;
        }

        // 消耗激活能量
        if (!data.consumeEnergy(100)) return;

        // 启用透视
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setBoolean("VISION_ACTIVE", true);

        // 设置玩家 NBT（用于客户端检测）
        player.getEntityData().setBoolean("MechanicalCoreOreVisionActive", true);
        player.getEntityData().setInteger("MechanicalCoreOreVisionLevel", level);

        int range = 8 * level;
        int selectedIndex = meta.getInteger("SELECTED_ORE_INDEX");
        String filter = selectedIndex == -1 ? "全部" : "已过滤";

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.GOLD + "⛏ 矿物透视已启动 - " +
                        TextFormatting.AQUA + filter +
                        TextFormatting.GRAY + " (" + range + "格)"
        ), true);
    }

    /**
     * 禁用矿物透视
     */
    public void disableOreVision(EntityPlayer player, IMechCoreData data) {
        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        if (!meta.getBoolean("VISION_ACTIVE")) return;

        meta.setBoolean("VISION_ACTIVE", false);

        // 清除玩家 NBT
        player.getEntityData().removeTag("MechanicalCoreOreVisionActive");
        player.getEntityData().removeTag("MechanicalCoreOreVisionLevel");

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.GRAY + "矿物透视已关闭"
        ), true);
    }

    /**
     * 切换矿物透视状态
     */
    public void toggleOreVision(EntityPlayer player, IMechCoreData data) {
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        if (meta.getBoolean("VISION_ACTIVE")) {
            disableOreVision(player, data);
        } else {
            enableOreVision(player, data);
        }
    }

    /**
     * 切换矿物分类
     *
     * 用于过滤显示特定类型的矿物
     */
    public void cycleOreCategory(EntityPlayer player, IMechCoreData data) {
        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        if (!meta.getBoolean("VISION_ACTIVE")) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "请先开启矿物透视"
            ), true);
            return;
        }

        int selectedIndex = meta.getInteger("SELECTED_ORE_INDEX");
        selectedIndex++;

        // 假设最多有 10 种矿物分类，-1 表示全部
        // 实际分类数量应该由客户端渲染系统提供
        if (selectedIndex >= 10) {
            selectedIndex = -1;
        }

        meta.setInteger("SELECTED_ORE_INDEX", selectedIndex);

        String filterText;
        if (selectedIndex == -1) {
            filterText = "全部";
        } else {
            filterText = "分类 " + (selectedIndex + 1);
        }

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.GOLD + "⛏ 矿物过滤: " + TextFormatting.AQUA + filterText
        ), true);
    }

    /**
     * 获取扫描范围
     */
    public int getScanRange(int level) {
        return 8 * level;  // 8/16/24/32/40 格
    }

    /**
     * 检查矿物透视是否激活
     */
    public boolean isOreVisionActive(IMechCoreData data) {
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        return meta.getBoolean("VISION_ACTIVE");
    }

    /**
     * 获取当前选中的矿物分类
     */
    public int getSelectedOreIndex(IMechCoreData data) {
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        return meta.getInteger("SELECTED_ORE_INDEX");
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 矿物透视的能量消耗是动态的，在 onTick 中处理
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 总是可以执行（能量检查在 enableOreVision 中）
        return true;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setBoolean("VISION_ACTIVE", false);
        meta.setInteger("SELECTED_ORE_INDEX", -1);
        meta.setLong("LAST_ENERGY_CHECK", 0);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("VISION_ACTIVE")) {
            meta.setBoolean("VISION_ACTIVE", false);
        }
        if (!meta.hasKey("SELECTED_ORE_INDEX")) {
            meta.setInteger("SELECTED_ORE_INDEX", -1);
        }
        if (!meta.hasKey("LAST_ENERGY_CHECK")) {
            meta.setLong("LAST_ENERGY_CHECK", 0);
        }
        return true;
    }
}
