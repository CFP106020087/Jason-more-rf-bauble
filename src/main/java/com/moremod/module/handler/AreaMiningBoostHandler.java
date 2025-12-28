package com.moremod.module.handler;

import com.moremod.module.effect.EventContext;
import com.moremod.module.effect.IModuleEventHandler;
import com.moremod.network.PacketVeinMiningKey;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;

import java.util.*;

/**
 * 范围挖掘模块处理器 (Vein Mining)
 *
 * 效果: 挖掘方块时自动连锁挖掘相邻的同类型方块
 * 触发条件: 玩家必须按住 ~ 键才会触发范围挖掘
 *
 * Lv1: 最多连锁 8 个方块
 * Lv2: 最多连锁 16 个方块
 * Lv3: 最多连锁 32 个方块
 *
 * 能耗: 每个额外方块消耗 50 RF
 */
public class AreaMiningBoostHandler implements IModuleEventHandler {

    // 每级最大连锁数量
    private static final int[] MAX_BLOCKS_PER_LEVEL = {0, 8, 16, 32};

    // 每个额外方块的能耗
    private static final int ENERGY_PER_BLOCK = 50;

    // 防止递归挖掘的标记
    private static final Set<UUID> currentlyMining = new HashSet<>();

    @Override
    public void onBlockBreak(EventContext ctx, BlockEvent.BreakEvent event) {
        // 如果事件已被取消，不处理
        if (event.isCanceled()) return;

        EntityPlayer player = ctx.player;

        // 防止递归
        if (currentlyMining.contains(player.getUniqueID())) return;

        // 只在服务端执行
        if (event.getWorld().isRemote) return;

        // 检查玩家是否按住范围挖掘触发键（~键）
        if (!PacketVeinMiningKey.isPlayerHoldingKey(player.getUniqueID())) {
            return; // 没有按住按键，不触发范围挖掘
        }

        // 执行连锁挖掘
        performVeinMine(ctx, event.getWorld(), event.getPos(), event.getState());
    }

    /**
     * 执行连锁挖掘
     */
    private void performVeinMine(EventContext ctx, World world, BlockPos startPos, IBlockState targetState) {
        Block targetBlock = targetState.getBlock();
        int maxBlocks = ctx.level < MAX_BLOCKS_PER_LEVEL.length ? MAX_BLOCKS_PER_LEVEL[ctx.level] : 32;

        // BFS查找相邻同类型方块
        Queue<BlockPos> toCheck = new LinkedList<>();
        Set<BlockPos> checked = new HashSet<>();
        List<BlockPos> toBreak = new ArrayList<>();

        toCheck.add(startPos);
        checked.add(startPos);

        while (!toCheck.isEmpty() && toBreak.size() < maxBlocks) {
            BlockPos current = toCheck.poll();

            // 检查所有相邻方块（6个面 + 12个边 + 8个角 = 26个方向）
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos neighbor = current.add(dx, dy, dz);
                        if (checked.contains(neighbor)) continue;
                        checked.add(neighbor);

                        IBlockState neighborState = world.getBlockState(neighbor);
                        // 检查是否是同类型方块
                        if (neighborState.getBlock() == targetBlock) {
                            toCheck.add(neighbor);
                            toBreak.add(neighbor);

                            if (toBreak.size() >= maxBlocks) break;
                        }
                    }
                    if (toBreak.size() >= maxBlocks) break;
                }
                if (toBreak.size() >= maxBlocks) break;
            }
        }

        if (toBreak.isEmpty()) return;

        // 根据能量限制实际挖掘数量
        int availableEnergy = ctx.getEnergy();
        int blocksToMine = Math.min(toBreak.size(), availableEnergy / ENERGY_PER_BLOCK);
        if (blocksToMine <= 0) return;

        // 标记正在挖掘，防止递归
        currentlyMining.add(ctx.player.getUniqueID());

        try {
            // 消耗能量
            ctx.consumeEnergy(blocksToMine * ENERGY_PER_BLOCK);

            // 挖掘方块
            for (int i = 0; i < blocksToMine; i++) {
                BlockPos pos = toBreak.get(i);
                IBlockState state = world.getBlockState(pos);
                if (state.getBlock().isAir(state, world, pos)) continue;

                // 使用 destroyBlock 正确处理掉落和移除
                // 参数 true = 掉落物品
                world.destroyBlock(pos, true);
            }
        } finally {
            currentlyMining.remove(ctx.player.getUniqueID());
        }
    }

    @Override
    public int getPassiveEnergyCost() {
        return 0; // 主动消耗，无被动消耗
    }

    @Override
    public String getDescription() {
        return "范围挖掘 - 连锁挖掘相邻同类型方块";
    }
}
