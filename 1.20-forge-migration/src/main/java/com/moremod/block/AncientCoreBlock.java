package com.moremod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 古代核心方块 - 1.20 Forge版本
 *
 * 装饰性方块，带有附魔粒子效果
 * 用于高级合成配方
 */
public class AncientCoreBlock extends Block {

    public AncientCoreBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(5.0F, 10.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> 11)); // 0.7F * 15 ≈ 11
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // 生成附魔粒子效果
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
