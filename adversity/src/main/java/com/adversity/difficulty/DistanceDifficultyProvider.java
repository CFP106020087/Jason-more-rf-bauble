package com.adversity.difficulty;

import com.adversity.config.AdversityConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 距离难度提供者 - 基于与世界出生点的距离计算难度
 */
public class DistanceDifficultyProvider implements IDifficultyProvider {

    @Override
    public String getId() {
        return "distance";
    }

    @Override
    public float getWeight() {
        return 1.0f;
    }

    @Override
    public float calculateDifficulty(World world, BlockPos pos, @Nullable EntityPlayer player) {
        BlockPos spawnPoint = world.getSpawnPoint();

        // 计算水平距离
        double dx = pos.getX() - spawnPoint.getX();
        double dz = pos.getZ() - spawnPoint.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        // 从配置读取参数
        double blocksPerDifficulty = AdversityConfig.difficulty.blocksPerDifficulty;
        double maxDifficulty = AdversityConfig.difficulty.maxDistanceDifficulty;

        // 计算难度
        float difficulty = (float) (distance / blocksPerDifficulty);
        return (float) Math.min(difficulty, maxDifficulty);
    }

    @Override
    public boolean isApplicable(World world, BlockPos pos, @Nullable EntityPlayer player) {
        // 在主世界生效
        return world.provider.getDimension() == 0;
    }
}
