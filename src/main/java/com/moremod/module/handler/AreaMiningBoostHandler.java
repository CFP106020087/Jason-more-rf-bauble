package com.moremod.module.handler;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.module.effect.EventContext;
import com.moremod.module.effect.IModuleEventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

/**
 * 范围挖掘模块处理器 (Vein Mining)
 *
 * 效果: 挖掘方块时自动连锁挖掘相邻的同类型方块
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

    // 静态初始化：注册Forge事件
    static {
        MinecraftForge.EVENT_BUS.register(new BlockBreakListener());
    }

    /**
     * Forge事件监听器 - 监听方块破坏事件
     */
    public static class BlockBreakListener {

        @SubscribeEvent
        public void onBlockBreak(BlockEvent.BreakEvent event) {
            EntityPlayer player = event.getPlayer();
            if (player == null || player.world.isRemote) return;

            // 防止递归
            if (currentlyMining.contains(player.getUniqueID())) return;

            // 检查玩家是否装备了机械核心且模块激活
            ItemStack coreStack = ItemMechanicalCoreExtended.getCoreFromPlayer(player);
            if (coreStack.isEmpty()) return;

            int level = ItemMechanicalCoreExtended.getEffectiveUpgradeLevel(coreStack, "AREA_MINING_BOOST");
            if (level <= 0) return;

            // 执行连锁挖掘
            performVeinMine(player, coreStack, event.getWorld(), event.getPos(), event.getState(), level);
        }
    }

    /**
     * 执行连锁挖掘
     */
    private static void performVeinMine(EntityPlayer player, ItemStack coreStack,
                                         World world, BlockPos startPos,
                                         IBlockState targetState, int level) {
        Block targetBlock = targetState.getBlock();
        int maxBlocks = level < MAX_BLOCKS_PER_LEVEL.length ? MAX_BLOCKS_PER_LEVEL[level] : 32;

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

        // 检查能量是否足够
        int totalEnergy = toBreak.size() * ENERGY_PER_BLOCK;
        int availableEnergy = ItemMechanicalCore.getEnergy(coreStack);

        // 根据能量限制实际挖掘数量
        int blocksToMine = Math.min(toBreak.size(), availableEnergy / ENERGY_PER_BLOCK);
        if (blocksToMine <= 0) return;

        // 标记正在挖掘，防止递归
        currentlyMining.add(player.getUniqueID());

        try {
            // 消耗能量
            ItemMechanicalCore.consumeEnergy(coreStack, blocksToMine * ENERGY_PER_BLOCK);

            // 挖掘方块
            for (int i = 0; i < blocksToMine; i++) {
                BlockPos pos = toBreak.get(i);
                IBlockState state = world.getBlockState(pos);

                // 掉落物品
                state.getBlock().harvestBlock(world, player, pos, state,
                        world.getTileEntity(pos), player.getHeldItemMainhand());

                // 破坏方块
                world.setBlockToAir(pos);
            }
        } finally {
            currentlyMining.remove(player.getUniqueID());
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
