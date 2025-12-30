package com.moremod.item.causal;

import net.minecraft.entity.*;
import net.minecraft.nbt.NBTTagCompound;

/**
 * 简化版压制器 - 已解耦外部MOD依赖
 * 原版本支持 InfernalMobs 和 Champions，已移至 src_backup_compat/causal/
 *
 * Phase 1 解耦: 移除了以下依赖
 * - atomicstryker.infernalmobs (InfernalMobs)
 * - com.moremod.compat.ChampionReflectionHelper (Champions)
 */
public final class CombinedSuppressor {

    private static final String NBT_SUPP = "moremod:suppressed";

    /**
     * 每tick检查实体是否在因果场内
     * 外部MOD的压制逻辑已移除，仅保留核心标记功能
     */
    public static void tick(EntityLivingBase e) {
        if (e.world.isRemote) return;

        boolean inField = CausalFieldManager.isInField(e.world, e.posX, e.posY, e.posZ);
        NBTTagCompound tag = e.getEntityData();
        boolean wasSuppressed = tag.getBoolean(NBT_SUPP);

        if (inField && !wasSuppressed) {
            // 进入因果场
            tag.setBoolean(NBT_SUPP, true);
            onEnterField(e);
        } else if (!inField && wasSuppressed) {
            // 离开因果场
            tag.setBoolean(NBT_SUPP, false);
            onLeaveField(e);
        }
    }

    /**
     * 实体进入因果场时的处理
     * 可在此添加核心效果（不依赖外部MOD）
     */
    private static void onEnterField(EntityLivingBase e) {
        // 核心压制效果（无外部MOD依赖）
        // 例如：清除药水效果、禁用AI等
    }

    /**
     * 实体离开因果场时的处理
     */
    private static void onLeaveField(EntityLivingBase e) {
        // 恢复效果
    }

    /**
     * 检查实体是否被压制
     */
    public static boolean isSuppressed(EntityLivingBase e) {
        return e.getEntityData().getBoolean(NBT_SUPP);
    }
}
