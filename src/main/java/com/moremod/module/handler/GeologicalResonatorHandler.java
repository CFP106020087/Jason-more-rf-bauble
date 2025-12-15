package com.moremod.module.handler;

import com.moremod.module.effect.EventContext;
import com.moremod.module.effect.IModuleEventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * 地质共振仪 (GEOLOGICAL_RESONATOR) 处理器 - 远程矿物提取。
 */
public class GeologicalResonatorHandler implements IModuleEventHandler {

    // 每 Tick 的维持能耗 (50 RF/t = 1000 RF/s)
    private static final int PASSIVE_COST_PER_TICK = 50;
    private static final int CONVERSION_COST = 800;
    private final Random random = new Random();

    // 【优化】矿物词典缓存
    private static final Set<Integer> KNOWN_ORE_IDS = new HashSet<>();
    // 特殊可提取方块（非标准矿物词典）
    private static final Set<String> SPECIAL_VALUABLE_BLOCKS = new HashSet<>();
    private static boolean oreCacheInitialized = false;

    /**
     * 懒加载初始化矿物缓存
     */
    private static void initializeOreCache() {
        if (oreCacheInitialized) return;
        oreCacheInitialized = true;

        System.out.println("[GeologicalResonator] 正在初始化矿物缓存...");
        String[] oreNames = OreDictionary.getOreNames();
        for (String name : oreNames) {
            // 只缓存以 "ore" 开头的词典名称
            if (name.startsWith("ore")) {
                KNOWN_ORE_IDS.add(OreDictionary.getOreID(name));
            }
        }

        // 添加特殊可提取方块（非标准矿物词典）
        SPECIAL_VALUABLE_BLOCKS.add("minecraft:ancient_debris");
        SPECIAL_VALUABLE_BLOCKS.add("astralsorcery:blockcelestialcrystals");
        SPECIAL_VALUABLE_BLOCKS.add("nb:netherite_ore");

        System.out.println("[GeologicalResonator] 缓存了 " + KNOWN_ORE_IDS.size() + " 种矿物类型，" + SPECIAL_VALUABLE_BLOCKS.size() + " 种特殊方块。");
    }

    @Override
    public void onTick(EventContext ctx) {
        initializeOreCache();

        // 1. 处理维持能耗 (每 Tick)
        if (!ctx.consumeEnergy(PASSIVE_COST_PER_TICK)) {
            return;
        }

        // 2. 执行共振采样 (仅在服务端)
        if (ctx.player.world.isRemote) return;

        // 根据等级决定每 Tick 采样次数 (1, 2, 3)
        int samples = ctx.level;

        for (int i = 0; i < samples; i++) {
            // 检查是否有足够的能量进行下一次转化
            if (ctx.getEnergy() < CONVERSION_COST) return;

            // 执行采样
            if (performResonance(ctx)) {
                // 成功转化，消耗能量
                ctx.consumeEnergy(CONVERSION_COST);
                // 继续下一次采样 (允许多次提取/Tick)
            }
        }
    }

    /**
     * 执行一次随机采样和转化
     */
    private boolean performResonance(EventContext ctx) {
        World world = ctx.player.world;
        EntityPlayer player = ctx.player;

        // 计算半径 (12, 16, 20)
        int radius = 8 + ctx.level * 4;
        BlockPos center = player.getPosition();

        // 随机选择 X 和 Z
        int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
        int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;

        // 【优化】Y轴偏置采样: 优先采样玩家下方区域
        int y;
        if (center.getY() > 5) {
            // 从基岩层 (Y=3) 到玩家当前高度之间随机选择
            y = random.nextInt(center.getY() - 3) + 3;
        } else {
            y = center.getY();
        }

        BlockPos targetPos = new BlockPos(x, y, z);

        // 检查区块是否加载
        if (!world.isBlockLoaded(targetPos)) return false;

        IBlockState state = world.getBlockState(targetPos);

        // 判断是否是矿物
        if (isValuableOre(state, world, targetPos, player)) {
            // 执行提取
            extractOre(world, player, targetPos, state);
            return true; // 成功转化
        }
        return false;
    }

    /**
     * 提取矿物并替换为石头
     */
    private void extractOre(World world, EntityPlayer player, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();

        // 1. 获取掉落物 (为平衡性，不应用时运 fortune=0)
        NonNullList<ItemStack> drops = NonNullList.create();
        try {
            block.getDrops(drops, world, pos, state, 0);
        } catch (Exception e) {
            // 捕获某些mod方块可能出现的异常
            System.err.println("[GeologicalResonator] 获取掉落物失败: " + e.getMessage());
            return;
        }

        if (drops.isEmpty()) {
            System.out.println("[GeologicalResonator] 警告: 矿物无掉落物 " + block.getRegistryName());
            return;
        }

        // 2. 先替换方块为石头，再生成掉落物
        // 使用 flag 2 (SEND_TO_CLIENTS) 确保客户端同步
        boolean replaced = world.setBlockState(pos, Blocks.STONE.getDefaultState(), 2);

        if (!replaced) {
            // 尝试使用破坏再放置的方式
            world.destroyBlock(pos, false); // 不掉落
            world.setBlockState(pos, Blocks.STONE.getDefaultState(), 2);
            System.out.println("[GeologicalResonator] 使用备用方式替换方块: " + pos);
        }

        // 3. 生成掉落物在玩家位置
        for (ItemStack drop : drops) {
            InventoryHelper.spawnItemStack(world, player.posX, player.posY + 0.5, player.posZ, drop);
        }

        // 4. 播放粒子效果
        playResonanceEffect(world, pos);

        System.out.println("[GeologicalResonator] 成功提取: " + block.getRegistryName() + " @ " + pos);
    }

    /**
     * 【健壮实现】判断方块是否是矿物。
     */
    private boolean isValuableOre(IBlockState state, World world, BlockPos pos, EntityPlayer player) {
        Block block = state.getBlock();

        // 性能优化: 快速排除常见方块和无效方块
        if (block == Blocks.AIR || block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRASS ||
            block == Blocks.SAND || block == Blocks.GRAVEL || state.getMaterial().isLiquid() ||
            state.getBlockHardness(world, pos) < 0) {
            return false;
        }

        // 检查是否是特殊可提取方块（如远古残骸、天辉水晶）
        if (block.getRegistryName() != null && SPECIAL_VALUABLE_BLOCKS.contains(block.getRegistryName().toString())) {
            return true;
        }

        // 获取方块对应的 ItemStack，用于矿物词典查询
        ItemStack stack = ItemStack.EMPTY;

        // 方法一: 使用 getPickBlock (模拟鼠标中键)，最准确的方式
        try {
            // 构建一个指向方块中心的 RayTraceResult
            Vec3d centerVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            RayTraceResult raytrace = new RayTraceResult(centerVec, EnumFacing.UP, pos);
            stack = block.getPickBlock(state, raytrace, world, pos, player);
        } catch (Exception e) {
            // 忽略异常，尝试后备方案
        }

        // 方法二: 后备方案，使用 damageDropped
        if (stack.isEmpty()) {
            Item item = Item.getItemFromBlock(block);
            if (item != Items.AIR) {
                try {
                    // 使用 damageDropped 获取正确的元数据
                    stack = new ItemStack(item, 1, block.damageDropped(state));
                } catch (Exception ignored) {}
            }
        }

        if (stack.isEmpty()) return false;

        // 使用 Forge 矿物词典缓存检查
        int[] oreIds = OreDictionary.getOreIDs(stack);
        for (int id : oreIds) {
            if (KNOWN_ORE_IDS.contains(id)) {
                return true;
            }
        }

        return false;
    }

    private void playResonanceEffect(World world, BlockPos pos) {
        if (world instanceof WorldServer) {
            // 在被提取的方块位置播放传送门粒子效果
            ((WorldServer) world).spawnParticle(
                EnumParticleTypes.PORTAL,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                15, 0.3, 0.3, 0.3, 0.5
            );
        }
    }

    @Override
    public int getPassiveEnergyCost() {
        return 0; // 在 onTick 中手动处理
    }
}