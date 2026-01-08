package com.adversity.difficulty;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 难度提供者接口 - 定义难度计算的数据来源
 */
public interface IDifficultyProvider {

    /**
     * 获取提供者的唯一标识符
     */
    String getId();

    /**
     * 获取提供者的权重（用于混合计算）
     */
    float getWeight();

    /**
     * 计算在指定位置的难度值
     *
     * @param world  世界
     * @param pos    位置
     * @param player 最近的玩家（可能为 null）
     * @return 难度贡献值（通常 0-10）
     */
    float calculateDifficulty(World world, BlockPos pos, @Nullable EntityPlayer player);

    /**
     * 检查此提供者是否应该在当前环境下生效
     */
    boolean isApplicable(World world, BlockPos pos, @Nullable EntityPlayer player);
}
