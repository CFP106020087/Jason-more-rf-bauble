package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

/**
 * 物品磁铁模块
 *
 * 功能：
 *  - 自动吸引周围物品和经验球
 *  - Lv.1: 8 格范围
 *  - Lv.2: 12 格范围
 *  - Lv.3: 16 格范围
 *
 * 能量消耗：
 *  - 基础消耗：5 * level RF/tick
 *  - 吸引物品额外消耗：1 RF/item
 *
 * 特性：
 *  - 自动吸引掉落物
 *  - 自动吸引经验球
 *  - 可穿墙吸引（非实体碰撞）
 *  - 统计吸引数量
 */
public class ItemMagnetModule extends AbstractMechCoreModule {

    public static final ItemMagnetModule INSTANCE = new ItemMagnetModule();

    // 吸引间隔（tick）
    private static final int ATTRACT_INTERVAL = 5;

    private ItemMagnetModule() {
        super(
            "ITEM_MAGNET",
            "物品磁铁",
            "自动吸引周围物品和经验球",
            3  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 初始化元数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setLong("ITEMS_ATTRACTED", 0);
        meta.setLong("XP_ATTRACTED", 0);

        int range = getAttractionRange(newLevel);
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "🧲 物品磁铁已激活 (范围: " + range + " 格)"
        ), true);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 显示统计信息
        long itemsAttracted = meta.getLong("ITEMS_ATTRACTED");
        long xpAttracted = meta.getLong("XP_ATTRACTED");

        if (itemsAttracted > 0 || xpAttracted > 0) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "物品磁铁统计: 吸引物品 " + itemsAttracted + " 个，经验球 " + xpAttracted + " 个"
            ), false);
        }
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        // 只在服务端执行
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        // 每 N tick 执行一次（减少性能开销）
        if (player.world.getTotalWorldTime() % ATTRACT_INTERVAL != 0) {
            return;
        }

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 获取吸引范围
        int range = getAttractionRange(level);
        double rangeSq = range * range;

        AxisAlignedBB searchBox = player.getEntityBoundingBox().grow(range, range, range);

        // 吸引掉落物
        List<EntityItem> items = player.world.getEntitiesWithinAABB(EntityItem.class, searchBox);
        int itemsAttracted = 0;

        for (EntityItem item : items) {
            // 检查距离（精确距离检查）
            double distSq = player.getDistanceSq(item);
            if (distSq > rangeSq) continue;

            // 检查是否可以拾取
            if (item.cannotPickup()) continue;
            if (item.getAge() < 10) continue;  // 刚掉落的物品有短暂的拾取延迟

            // 消耗能量
            if (!data.consumeEnergy(1)) {
                break;  // 能量不足，停止吸引
            }

            // 吸引物品
            attractEntity(player, item);
            itemsAttracted++;
        }

        if (itemsAttracted > 0) {
            meta.setLong("ITEMS_ATTRACTED", meta.getLong("ITEMS_ATTRACTED") + itemsAttracted);
        }

        // 吸引经验球
        List<EntityXPOrb> orbs = player.world.getEntitiesWithinAABB(EntityXPOrb.class, searchBox);
        int orbsAttracted = 0;

        for (EntityXPOrb orb : orbs) {
            // 检查距离（精确距离检查）
            double distSq = player.getDistanceSq(orb);
            if (distSq > rangeSq) continue;

            // 消耗能量
            if (!data.consumeEnergy(1)) {
                break;  // 能量不足，停止吸引
            }

            // 吸引经验球
            attractEntity(player, orb);
            orbsAttracted++;
        }

        if (orbsAttracted > 0) {
            meta.setLong("XP_ATTRACTED", meta.getLong("XP_ATTRACTED") + orbsAttracted);
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时提示新范围
        if (newLevel > 0) {
            int range = getAttractionRange(newLevel);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "🧲 物品磁铁范围已更新: " + range + " 格"
            ), true);
        }
    }

    /**
     * 吸引实体到玩家
     */
    private void attractEntity(EntityPlayer player, net.minecraft.entity.Entity entity) {
        // 计算方向向量
        double dx = player.posX - entity.posX;
        double dy = player.posY + player.getEyeHeight() / 2.0 - entity.posY;
        double dz = player.posZ - entity.posZ;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist < 0.1) return;  // 已经很近了

        // 归一化并缩放速度
        double speed = Math.min(0.3, dist * 0.1);  // 距离越远，速度越快（但有上限）
        double vx = (dx / dist) * speed;
        double vy = (dy / dist) * speed;
        double vz = (dz / dist) * speed;

        // 设置运动向量
        entity.motionX = vx;
        entity.motionY = vy;
        entity.motionZ = vz;

        // 重置下落距离（防止掉落伤害）
        entity.fallDistance = 0;
    }

    /**
     * 获取吸引范围
     */
    public int getAttractionRange(int level) {
        switch (level) {
            case 1:
                return 8;   // 8 格
            case 2:
                return 12;  // 12 格
            case 3:
                return 16;  // 16 格
            default:
                return 0;
        }
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 基础消耗：5 * level RF/tick
        return 5 * level;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 总是可以执行
        return true;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setLong("ITEMS_ATTRACTED", 0);
        meta.setLong("XP_ATTRACTED", 0);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("ITEMS_ATTRACTED")) {
            meta.setLong("ITEMS_ATTRACTED", 0);
        }
        if (!meta.hasKey("XP_ATTRACTED")) {
            meta.setLong("XP_ATTRACTED", 0);
        }
        return true;
    }
}
