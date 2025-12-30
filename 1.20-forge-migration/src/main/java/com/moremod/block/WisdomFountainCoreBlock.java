package com.moremod.block;

import com.moremod.block.entity.WisdomFountainBlockEntity;
import com.moremod.init.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * 智慧之泉核心方块 - 1.20 Forge版本
 *
 * 功能：
 * - 多方块结构检测
 * - 村民转化功能
 * - 附魔书交互
 */
public class WisdomFountainCoreBlock extends BaseMachineBlock {

    public WisdomFountainCoreBlock(Properties properties) {
        super(properties.lightLevel(state -> 8)); // 0.5F * 15 ≈ 8
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WisdomFountainBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.WISDOM_FOUNTAIN.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof WisdomFountainBlockEntity fountain) {
                if (fountain.isFormed()) {
                    player.sendSystemMessage(Component.literal(
                            ChatFormatting.GREEN + "神碑智慧之泉已激活！"
                    ));
                    player.sendSystemMessage(Component.literal(
                            ChatFormatting.YELLOW + "手持附魔书对附近村民Shift+右键进行转化"
                    ));
                } else {
                    player.sendSystemMessage(Component.literal(
                            ChatFormatting.RED + "结构不完整，请检查多方块结构"
                    ));
                    player.sendSystemMessage(Component.literal(
                            ChatFormatting.GRAY + "提示：使用守护者方块和符文虚空石方块构建"
                    ));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
