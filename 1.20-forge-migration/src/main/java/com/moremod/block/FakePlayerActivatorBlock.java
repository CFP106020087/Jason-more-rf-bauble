package com.moremod.block;

import com.moremod.block.entity.FakePlayerActivatorBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * 假玩家激活器方块 - 1.20 Forge版本
 *
 * 功能：
 * - 使用假玩家核心模拟玩家操作
 * - 自动右键点击前方方块
 * - 自动使用物品（骨粉、种子等）
 * - 可朝向任意方向
 */
public class FakePlayerActivatorBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public FakePlayerActivatorBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.5F, 10.0F)
                .sound(SoundType.METAL)
                .lightLevel(state -> state.getValue(ACTIVE) ? 7 : 0)
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        Direction facing;

        if (player != null) {
            // 根据玩家视角确定朝向
            float pitch = player.getXRot();
            if (pitch > 45) {
                facing = Direction.UP;
            } else if (pitch < -45) {
                facing = Direction.DOWN;
            } else {
                facing = context.getHorizontalDirection().getOpposite();
            }
        } else {
            facing = Direction.NORTH;
        }

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ACTIVE, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FakePlayerActivatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == ModBlockEntities.FAKE_PLAYER_ACTIVATOR.get()
                ? (lvl, pos, st, be) -> ((FakePlayerActivatorBlockEntity) be).serverTick()
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FakePlayerActivatorBlockEntity activator)) {
            return InteractionResult.PASS;
        }

        // TODO: 打开GUI
        // 目前显示状态信息
        if (player.isShiftKeyDown()) {
            boolean active = state.getValue(ACTIVE);
            Direction facing = state.getValue(FACING);
            int energy = activator.getEnergyStored();

            player.displayClientMessage(Component.literal(
                    "§6=== 假玩家激活器 ==="
            ), false);
            player.displayClientMessage(Component.literal(
                    "§e朝向: " + facing.getName()
            ), false);
            player.displayClientMessage(Component.literal(
                    (active ? "§a运行中" : "§c已停止") + " §7| 能量: " + energy + " RF"
            ), false);
        } else {
            player.displayClientMessage(Component.literal(
                    "§6【假玩家激活器】§r 蹲下右键查看状态"
            ), true);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FakePlayerActivatorBlockEntity activator) {
                IItemHandler handler = activator.getItemHandler();
                if (handler != null) {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                        }
                    }
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    /**
     * 设置激活状态
     */
    public static void setActiveState(Level level, BlockPos pos, boolean active) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FakePlayerActivatorBlock) {
            if (state.getValue(ACTIVE) != active) {
                level.setBlock(pos, state.setValue(ACTIVE, active), 3);
            }
        }
    }
}
