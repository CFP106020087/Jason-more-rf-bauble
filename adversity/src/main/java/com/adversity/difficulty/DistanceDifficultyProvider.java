package com.adversity.difficulty;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 距离难度提供者 - 基于与世界出生点的距离计算难度
 */
public class DistanceDifficultyProvider implements IDifficultyProvider {

    // 每隔多少方块增加 1 点难度
    private static final float BLOCKS_PER_DIFFICULTY = 500f;
    // 最大难度贡献
    private static final float MAX_DIFFICULTY = 10f;

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

        // 计算难度
        float difficulty = (float) (distance / BLOCKS_PER_DIFFICULTY);
        return Math.min(difficulty, MAX_DIFFICULTY);
    }

    @Override
    public boolean isApplicable(World world, BlockPos pos, @Nullable EntityPlayer player) {
        // 在主世界生效
        return world.provider.getDimension() == 0;
    }
}
