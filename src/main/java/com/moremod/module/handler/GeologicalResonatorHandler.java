package com.moremod.module.handler;

import com.moremod.module.effect.EventContext;
import com.moremod.module.effect.IModuleEventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;

/**
 * 地质共振仪 (GEOLOGICAL_RESONATOR) 处理器
 *
 * 功能：扫描玩家周围的矿物方块，远程提取并替换为石头
 *
 * 参数（与 tooltip 同步）：
 * - 共振半径: 12/16/20 格 (Lv1/2/3)
 * - 转化速度: 每 Tick 采样 1/2/3 次
 * - 维持能耗: 50 RF/t
 * - 转化能耗: 800 RF/矿物
 */
public class GeologicalResonatorHandler implements IModuleEventHandler {

    // 能耗配置 (与 tooltip 同步)
    private static final int PASSIVE_COST_PER_TICK = 50;   // 维持能耗 50 RF/t
    private static final int CONVERSION_COST = 800;         // 每次转化消耗 800 RF

    // 矿物词典前缀
    private static final String ORE_PREFIX = "ore";

    // 扫描状态 - 使用玩家UUID隔离
    private final Map<UUID, ScanState> playerScanStates = new WeakHashMap<>();

    /**
     * 玩家扫描状态
     */
    private static class ScanState {
        int currentIndex = 0;           // 当前扫描索引
        List<BlockPos> scanQueue;       // 扫描队列（优先级排序）
        BlockPos lastPlayerPos;         // 上次玩家位置
        int lastRadius;                 // 上次半径
        long lastRebuildTick;           // 上次重建队列的tick

        ScanState() {
            this.scanQueue = new ArrayList<>();
        }
    }

    @Override
    public void onTick(EventContext ctx) {
        // 1. 处理维持能耗
        if (!ctx.consumeEnergy(PASSIVE_COST_PER_TICK)) {
            return;
        }

        // 2. 仅服务端执行
        if (ctx.player.world.isRemote) return;
        if (!(ctx.player.world instanceof WorldServer)) return;

        EntityPlayer player = ctx.player;
        WorldServer world = (WorldServer) player.world;

        // 3. 获取等级相关参数
        int level = ctx.level;
        int radius = 8 + level * 4;          // 12, 16, 20
        int samplesPerTick = level;          // 1, 2, 3

        // 4. 获取或创建扫描状态
        ScanState state = playerScanStates.computeIfAbsent(player.getUniqueID(), k -> new ScanState());

        // 5. 检查是否需要重建扫描队列
        BlockPos playerPos = player.getPosition();
        long currentTick = world.getTotalWorldTime();

        boolean needRebuild = state.scanQueue.isEmpty()
                || state.lastRadius != radius
                || state.lastPlayerPos == null
                || playerPos.distanceSq(state.lastPlayerPos) > 16  // 移动超过4格
                || (currentTick - state.lastRebuildTick) > 200;    // 或每10秒重建

        if (needRebuild) {
            rebuildScanQueue(state, player, radius);
            state.lastPlayerPos = playerPos;
            state.lastRadius = radius;
            state.lastRebuildTick = currentTick;
        }

        // 6. 执行采样
        int successCount = 0;
        for (int i = 0; i < samplesPerTick && !state.scanQueue.isEmpty(); i++) {
            // 检查能量
            if (ctx.getEnergy() < CONVERSION_COST) break;

            // 获取下一个扫描位置
            if (state.currentIndex >= state.scanQueue.size()) {
                state.currentIndex = 0;
            }

            BlockPos scanPos = state.scanQueue.get(state.currentIndex);
            state.currentIndex++;

            // 检查距离（玩家可能已移动）
            if (scanPos.distanceSq(player.posX, player.posY, player.posZ) > radius * radius) {
                continue;
            }

            // 尝试提取矿物
            if (tryExtractOre(world, player, scanPos, ctx)) {
                successCount++;
                // 从队列移除已提取的位置
                state.scanQueue.remove(--state.currentIndex);
                if (state.currentIndex < 0) state.currentIndex = 0;
            }
        }

        // 7. 成功提取时播放效果
        if (successCount > 0) {
            playExtractionEffect(world, player, successCount);
        }
    }

    /**
     * 重建扫描队列 - 优先扫描玩家下方
     */
    private void rebuildScanQueue(ScanState state, EntityPlayer player, int radius) {
        state.scanQueue.clear();
        state.currentIndex = 0;

        BlockPos center = player.getPosition();
        List<BlockPos> positions = new ArrayList<>();

        // 收集范围内所有位置
        for (int y = -radius; y <= radius / 2; y++) {  // 偏向下方
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // 球形范围检查
                    if (x * x + y * y + z * z <= radius * radius) {
                        positions.add(center.add(x, y, z));
                    }
                }
            }
        }

        // 按距离排序（优先下方和近处）
        positions.sort((a, b) -> {
            // 下方优先
            int yBiasA = a.getY() < center.getY() ? -10 : 0;
            int yBiasB = b.getY() < center.getY() ? -10 : 0;

            double distA = a.distanceSq(center) + yBiasA;
            double distB = b.distanceSq(center) + yBiasB;
            return Double.compare(distA, distB);
        });

        state.scanQueue = positions;
    }

    /**
     * 尝试提取指定位置的矿物
     */
    private boolean tryExtractOre(WorldServer world, EntityPlayer player, BlockPos pos, EventContext ctx) {
        IBlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();

        // 跳过空气和基岩
        if (block == Blocks.AIR || block == Blocks.BEDROCK) {
            return false;
        }

        // 检查是否为矿物
        if (!isOreBlock(blockState)) {
            return false;
        }

        // 获取掉落物
        List<ItemStack> drops = block.getDrops(world, pos, blockState, 0);
        if (drops.isEmpty()) {
            // 如果没有掉落物，尝试使用方块本身
            ItemStack blockStack = new ItemStack(block, 1, block.getMetaFromState(blockState));
            if (!blockStack.isEmpty()) {
                drops = Collections.singletonList(blockStack);
            }
        }

        if (drops.isEmpty()) {
            return false;
        }

        // 消耗能量
        if (!ctx.consumeEnergy(CONVERSION_COST)) {
            return false;
        }

        // 替换为石头
        world.setBlockState(pos, Blocks.STONE.getDefaultState(), 3);

        // 给予玩家掉落物
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                ItemStack toGive = drop.copy();
                if (!player.inventory.addItemStackToInventory(toGive)) {
                    // 背包满了，掉落在玩家脚下
                    EntityItem entityItem = new EntityItem(world, player.posX, player.posY + 0.5, player.posZ, toGive);
                    entityItem.setNoPickupDelay();
                    world.spawnEntity(entityItem);
                }
            }
        }

        // 在矿物位置播放粒子
        world.spawnParticle(
            EnumParticleTypes.PORTAL,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            8, 0.3, 0.3, 0.3, 0.05
        );

        return true;
    }

    /**
     * 检查方块是否为矿物（基于矿物词典）
     */
    private boolean isOreBlock(IBlockState state) {
        Block block = state.getBlock();
        int meta = block.getMetaFromState(state);
        ItemStack stack = new ItemStack(block, 1, meta);

        if (stack.isEmpty()) {
            return false;
        }

        // 检查矿物词典
        int[] oreIds = OreDictionary.getOreIDs(stack);
        for (int id : oreIds) {
            String oreName = OreDictionary.getOreName(id);
            if (oreName.startsWith(ORE_PREFIX)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 播放提取效果
     */
    private void playExtractionEffect(WorldServer world, EntityPlayer player, int count) {
        world.spawnParticle(
            EnumParticleTypes.VILLAGER_HAPPY,
            player.posX, player.posY + 1.0, player.posZ,
            count * 3, 0.4, 0.4, 0.4, 0.02
        );
    }

    @Override
    public int getPassiveEnergyCost() {
        return 0; // 在 onTick 中手动处理
    }
}
