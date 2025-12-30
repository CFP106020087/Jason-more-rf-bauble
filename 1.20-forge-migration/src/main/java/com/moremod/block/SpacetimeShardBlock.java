package com.moremod.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 时空碎片矿石方块 - 1.20 Forge版本
 *
 * 功能：
 * - 在私人维度虚空中生成的特殊矿石
 * - 发光效果
 * - 特殊粒子效果
 */
public class SpacetimeShardBlock extends Block {

    public SpacetimeShardBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(5.0F, 10.0F)
                .sound(SoundType.GLASS)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> 11) // 0.7F * 15 ≈ 11
                .noOcclusion());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5);
            double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5);
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5);

            // 时空粒子效果 - 传送门效果
            level.addParticle(ParticleTypes.PORTAL, x, y, z,
                    (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.5,
                    (random.nextDouble() - 0.5) * 0.5);

            // 偶尔显示末地烛粒子
            if (random.nextInt(5) == 0) {
                level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.LIGHT_PURPLE + "蕴含时空之力的神秘矿石"));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "只存在于维度虚空中"));
        tooltip.add(Component.literal(ChatFormatting.AQUA + "需要铁镐或更好的工具"));
    }

    @Override
    public int getExpDrop(BlockState state, net.minecraft.world.level.LevelAccessor level,
                          RandomSource random, BlockPos pos, int fortune, int silkTouch) {
        if (silkTouch > 0) {
            return 0;
        }
        return 3 + random.nextInt(5); // 3-7 经验
    }
}
