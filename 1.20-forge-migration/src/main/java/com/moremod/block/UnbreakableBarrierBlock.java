package com.moremod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 不可破坏屏障方块 - 1.20 Forge版本
 *
 * 多种类型的不可破坏屏障，每种有不同的视觉效果
 */
public class UnbreakableBarrierBlock extends Block {

    public enum BarrierType {
        VOID_CRYSTAL("虚空水晶", 0x4A0080, true),
        QUANTUM_FIELD("量子力场", 0x00FFFF, false),
        TEMPORAL_LOCK("时间锁定", 0xFFD700, false),
        DIMENSIONAL_ANCHOR("维度锚定", 0xFF00FF, false),
        ETHEREAL_WALL("以太墙壁", 0x87CEEB, false);

        public final String name;
        public final int color;
        public final boolean canTeleport;

        BarrierType(String name, int color, boolean canTeleport) {
            this.name = name;
            this.color = color;
            this.canTeleport = canTeleport;
        }
    }

    private final BarrierType type;

    public UnbreakableBarrierBlock(BarrierType type) {
        super(BlockBehaviour.Properties.of()
                .strength(-1.0F, Float.MAX_VALUE)
                .noLootTable()
                .lightLevel(state -> type == BarrierType.VOID_CRYSTAL ? 12 : 8)
                .noOcclusion());
        this.type = type;
    }

    public BarrierType getType() {
        return type;
    }

    // 不可破坏
    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        // 爆炸时产生保护粒子效果
        if (!level.isClientSide()) {
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.BLOCKS, 1.0F, 2.0F);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (type == BarrierType.VOID_CRYSTAL) {
                player.displayClientMessage(Component.literal(
                        "§d【虚空水晶】§b 需要维度钥匙来激活传送门"
                ), true);

                level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL,
                        SoundSource.BLOCKS, 0.5F, 1.5F);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()) {
            player.displayClientMessage(Component.literal(
                    "§c⚠ 无法破坏 " + type.name + "！"
            ), true);

            level.playSound(null, pos, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR,
                    SoundSource.BLOCKS, 1.0F, 0.5F);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (type == BarrierType.VOID_CRYSTAL) {
            if (random.nextInt(5) == 0) {
                double x = pos.getX() + 0.5;
                double y = pos.getY() + random.nextDouble();
                double z = pos.getZ() + 0.5;

                level.addParticle(ParticleTypes.PORTAL, x, y, z,
                        (random.nextDouble() - 0.5) * 0.1,
                        random.nextDouble() * 0.1,
                        (random.nextDouble() - 0.5) * 0.1);

                if (random.nextInt(10) == 0) {
                    level.addParticle(ParticleTypes.ENCHANT,
                            x, y + 1.5, z,
                            (random.nextDouble() - 0.5) * 2.0,
                            random.nextDouble(),
                            (random.nextDouble() - 0.5) * 2.0);
                }
            }
        } else if (random.nextInt(10) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.PORTAL, x, y, z, 0, 0, 0);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level,
                                       BlockPos pos, Player player) {
        return ItemStack.EMPTY;
    }
}
