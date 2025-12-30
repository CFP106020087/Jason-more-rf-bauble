package com.moremod.block;

import com.moremod.block.entity.BioGeneratorBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 生物质发电机方块 - 1.20 Forge版本
 *
 * 功能：
 * - 使用有机物（种子、树苗、小麦等）发电
 * - 产出40 RF/t
 * - 容量50,000 RF
 *
 * 1.12 -> 1.20 API变更:
 * - PropertyBool -> BooleanProperty
 * - createBlockState -> createBlockStateDefinition
 * - randomDisplayTick参数变化
 */
public class BioGeneratorBlock extends BaseMachineBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public BioGeneratorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BioGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.BIO_GENERATOR.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BioGeneratorBlockEntity generator) {
            ItemStack heldItem = player.getItemInHand(hand);

            // 尝试放入燃料
            if (!heldItem.isEmpty()) {
                ItemStack remaining = generator.addFuel(heldItem);
                if (remaining.getCount() != heldItem.getCount()) {
                    if (!player.isCreative()) {
                        player.setItemInHand(hand, remaining);
                    }
                    return InteractionResult.SUCCESS;
                }
            }

            // 显示状态
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.GOLD + "生物质发电机状态:"
            ));
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.GRAY + "能量: " + ChatFormatting.RED +
                            generator.getEnergyStored() + "/" + generator.getMaxEnergyStored() + " RF"
            ));
            player.sendSystemMessage(Component.literal(
                    ChatFormatting.GRAY + "燃料: " + ChatFormatting.GREEN + generator.getFuelCount() +
                            ChatFormatting.GRAY + " | 发电: " + (generator.isGenerating() ?
                            ChatFormatting.GREEN + "运行中" : ChatFormatting.RED + "停止")
            ));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BioGeneratorBlockEntity generator) {
                generator.dropInventory();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BioGeneratorBlockEntity generator && generator.isGenerating()) {
            return 8;
        }
        return 0;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BioGeneratorBlockEntity generator && generator.isGenerating()) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;

            level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0, 0.05, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GRAY + "使用生物质发电"));
        tooltip.add(Component.literal(ChatFormatting.RED + "产出: 40 RF/t"));
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "容量: 50,000 RF"));
        tooltip.add(Component.literal(ChatFormatting.GREEN + "燃料: 种子、树苗、小麦等"));
    }
}
