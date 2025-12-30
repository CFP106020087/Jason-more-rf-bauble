package com.moremod.block;

import com.moremod.block.entity.TimeControllerBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * 时间控制器方块 - 1.20 Forge版本
 *
 * 功能：
 * - 控制世界时间（加速、减速、暂停、倒流、永昼、永夜）
 * - 消耗RF能量
 */
public class TimeControllerBlock extends Block implements EntityBlock {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public TimeControllerBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.0F, 10.0F)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> state.getValue(ACTIVE) ? 12 : 0));
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TimeControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == ModBlockEntities.TIME_CONTROLLER.get()
                ? (lvl, pos, st, be) -> ((TimeControllerBlockEntity) be).serverTick()
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TimeControllerBlockEntity controller) {
            if (player.isShiftKeyDown()) {
                // Shift+右键：调整速度
                int newLevel = (controller.getSpeedLevel() + 1) % 16;
                controller.setSpeedLevel(newLevel);
            } else {
                // 右键：切换模式
                controller.cycleMode();
            }

            player.displayClientMessage(Component.literal(controller.getStatusText()), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TimeControllerBlockEntity controller) {
            float ratio = (float) controller.getEnergyStored() / controller.getMaxEnergyStored();
            return (int) (ratio * 15);
        }
        return 0;
    }

    /**
     * 设置激活状态
     */
    public static void setActiveState(Level level, BlockPos pos, boolean active) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TimeControllerBlock) {
            if (state.getValue(ACTIVE) != active) {
                level.setBlock(pos, state.setValue(ACTIVE, active), 3);
            }
        }
    }
}
