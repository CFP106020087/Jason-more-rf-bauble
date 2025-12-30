package com.moremod.block;

import com.moremod.block.entity.RitualCoreBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * 祭坛核心方块 - 1.20 Forge版本
 *
 * 功能：
 * - 嵌入仪式的核心
 * - 物品处理
 * - 玩家坐入功能
 */
public class RitualCoreBlock extends BaseMachineBlock {

    public RitualCoreBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RitualCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.RITUAL_CORE.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RitualCoreBlockEntity core)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 蹲下右键 = 尝试坐上祭坛进行嵌入仪式
        if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
            return core.seatPlayer(player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        IItemHandler handler = level.getCapability(ForgeCapabilities.ITEM_HANDLER, pos, hit.getDirection());
        if (handler == null) {
            return InteractionResult.FAIL;
        }

        ItemStack heldItem = player.getItemInHand(hand);

        // 优先取出输出槽
        ItemStack output = handler.getStackInSlot(1);
        if (!output.isEmpty()) {
            ItemStack extracted = handler.extractItem(1, 64, false);
            if (!player.getInventory().add(extracted)) {
                handler.insertItem(1, extracted, false);
            }
            core.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            return InteractionResult.SUCCESS;
        }

        // 放入输入槽
        if (!heldItem.isEmpty()) {
            ItemStack current = handler.getStackInSlot(0);
            if (current.isEmpty()) {
                ItemStack toInsert = heldItem.copy();
                toInsert.setCount(1);
                ItemStack remainder = handler.insertItem(0, toInsert, false);
                if (remainder.isEmpty()) {
                    heldItem.shrink(1);
                    core.setChanged();
                    level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                    return InteractionResult.SUCCESS;
                }
            }
        }

        // 空手取出输入槽
        if (heldItem.isEmpty()) {
            ItemStack input = handler.getStackInSlot(0);
            if (!input.isEmpty()) {
                ItemStack extracted = handler.extractItem(0, 64, false);
                player.setItemInHand(hand, extracted);
                core.setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }
}
