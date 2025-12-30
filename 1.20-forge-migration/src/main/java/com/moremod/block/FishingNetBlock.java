package com.moremod.block;

import com.moremod.block.entity.FishingNetBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 渔网方块 - 1.20 Forge版本
 *
 * 功能：
 * - 放置在水面上自动钓鱼
 * - 需要下方有水源
 * - 可用漏斗抽取物品
 *
 * 1.12 -> 1.20 API变更:
 * - AxisAlignedBB -> VoxelShape
 * - getBoundingBox -> getShape
 */
public class FishingNetBlock extends BaseMachineBlock {

    // 薄的碰撞箱，像地毯一样
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public FishingNetBlock(Properties properties) {
        super(properties.noOcclusion());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FishingNetBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.FISHING_NET.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FishingNetBlockEntity fishingNet) {
            boolean hasWater = fishingNet.hasWaterBelow();
            AtomicInteger itemCount = new AtomicInteger(0);

            be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        itemCount.addAndGet(stack.getCount());
                    }
                }
            });

            if (hasWater) {
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.AQUA + "渔网状态: " + ChatFormatting.GREEN + "工作中" +
                                ChatFormatting.GRAY + " | 存储物品: " + ChatFormatting.YELLOW + itemCount.get()
                ));
            } else {
                player.sendSystemMessage(Component.literal(
                        ChatFormatting.AQUA + "渔网状态: " + ChatFormatting.RED + "需要水源！" +
                                ChatFormatting.GRAY + " (在渔网下方放置水)"
                ));
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FishingNetBlockEntity fishingNet) {
                fishingNet.dropInventory();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GRAY + "放置在水面上自动钓鱼"));
        tooltip.add(Component.literal(ChatFormatting.AQUA + "需要下方有水源方块"));
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "可用漏斗抽取物品"));
    }
}
