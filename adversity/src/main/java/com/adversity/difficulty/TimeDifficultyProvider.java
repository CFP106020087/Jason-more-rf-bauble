package com.adversity.difficulty;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 时间难度提供者 - 基于游戏时间计算难度
 */
public class TimeDifficultyProvider implements IDifficultyProvider {

    // 每经过多少天增加 1 点难度
    private static final float DAYS_PER_DIFFICULTY = 5f;
    // 最大难度贡献
    private static final float MAX_DIFFICULTY = 8f;
    // 一天的 tick 数
    private static final long TICKS_PER_DAY = 24000L;

    @Override
    public String getId() {
        return "time";
    }

    @Override
    public float getWeight() {
        return 0.8f;
    }

    @Override
    public float calculateDifficulty(World world, BlockPos pos, @Nullable EntityPlayer player) {
        long worldTime = world.getTotalWorldTime();
        float days = worldTime / (float) TICKS_PER_DAY;

        float difficulty = days / DAYS_PER_DIFFICULTY;
        return Math.min(difficulty, MAX_DIFFICULTY);
    }

    @Override
    public boolean isApplicable(World world, BlockPos pos, @Nullable EntityPlayer player) {
        // 在所有维度生效
        return true;
    }
}
