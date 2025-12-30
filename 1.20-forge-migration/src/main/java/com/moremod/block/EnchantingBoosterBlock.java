package com.moremod.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 附魔增强方块 - 1.20 Forge版本
 *
 * 功能：
 * - 放置在附魔台附近增强附魔等级上限
 * - 通过祭坛仪式创建
 * - 不同类型提供不同附魔加成
 */
public class EnchantingBoosterBlock extends Block {

    public static final EnumProperty<BoosterType> TYPE = EnumProperty.create("type", BoosterType.class);

    public EnchantingBoosterBlock(BoosterType type) {
        super(BlockBehaviour.Properties.of()
                .strength(3.0F, 15.0F)
                .sound(SoundType.STONE)
                .lightLevel(state -> state.getValue(TYPE).getLightLevel()));
        this.registerDefaultState(this.stateDefinition.any().setValue(TYPE, type));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    /**
     * 提供附魔增益 - 类似书架
     */
    @Override
    public float getEnchantPowerBonus(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return state.getValue(TYPE).getEnchantBonus();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BoosterType type = state.getValue(TYPE);
            player.displayClientMessage(Component.literal(
                    ChatFormatting.AQUA + "附魔增益: +" + ChatFormatting.GOLD + type.getEnchantBonus() +
                    ChatFormatting.GRAY + " (等效于 " + type.getEnchantBonus() + " 个书架)"
            ), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        BoosterType type = this.defaultBlockState().getValue(TYPE);
        tooltip.add(Component.literal(ChatFormatting.AQUA + "附魔增益: " + ChatFormatting.GOLD + "+" + type.getEnchantBonus()));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "等效于 " + type.getEnchantBonus() + " 个书架"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal(ChatFormatting.DARK_PURPLE + type.getDescription()));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BoosterType type = state.getValue(TYPE);

        // 高级方块显示粒子效果
        if (type.getEnchantBonus() >= 2.0f && random.nextInt(5) == 0) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;

            level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.2, 0);
        }

        // 最高级方块显示更多粒子
        if (type == BoosterType.SOUL_LIBRARY && random.nextInt(3) == 0) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();

            level.addParticle(ParticleTypes.PORTAL, x, y, z, 0, 0, 0);
        }
    }

    /**
     * 附魔增强方块类型
     */
    public enum BoosterType implements StringRepresentable {
        ARCANE_STONE("arcane_stone", 1.0f, 0, "蕴含微弱魔力的石头"),
        ENCHANTED_BOOKSHELF("enchanted_bookshelf", 2.0f, 3, "经过仪式强化的书架"),
        KNOWLEDGE_CRYSTAL("knowledge_crystal", 3.0f, 7, "凝聚了无数知识的水晶"),
        SOUL_LIBRARY("soul_library", 5.0f, 11, "封印了学者灵魂的图书馆碎片");

        private final String name;
        private final float enchantBonus;
        private final int lightLevel;
        private final String description;

        BoosterType(String name, float enchantBonus, int lightLevel, String description) {
            this.name = name;
            this.enchantBonus = enchantBonus;
            this.lightLevel = lightLevel;
            this.description = description;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public float getEnchantBonus() {
            return enchantBonus;
        }

        public int getLightLevel() {
            return lightLevel;
        }

        public String getDescription() {
            return description;
        }
    }
}
