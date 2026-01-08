package com.adversity.difficulty;

import com.adversity.config.AdversityConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 时间难度提供者 - 基于游戏时间计算难度
 */
public class TimeDifficultyProvider implements IDifficultyProvider {

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

        // 从配置读取参数
        double daysPerDifficulty = AdversityConfig.difficulty.daysPerDifficulty;
        double maxDifficulty = AdversityConfig.difficulty.maxTimeDifficulty;

        float difficulty = (float) (days / daysPerDifficulty);
        return (float) Math.min(difficulty, maxDifficulty);
    }

    @Override
    public boolean isApplicable(World world, BlockPos pos, @Nullable EntityPlayer player) {
        // 在所有维度生效
        return true;
    }
}
