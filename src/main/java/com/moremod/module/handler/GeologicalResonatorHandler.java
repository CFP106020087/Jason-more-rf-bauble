package com.moremod.module.handler;

import com.moremod.module.effect.EventContext;
import com.moremod.module.effect.IModuleEventHandler;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 地质共振仪 (GEOLOGICAL_RESONATOR) 处理器 - 远程矿物提取。
 * 使用 FakePlayer + harvestBlock 走完整挖矿流程，兼容 Astral Sorcery 等模组。
 */
public class GeologicalResonatorHandler implements IModuleEventHandler {

    // 每 Tick 的维持能耗 (50 RF/t = 1000 RF/s)
    private static final int PASSIVE_COST_PER_TICK = 50;
    private static final int CONVERSION_COST = 800;
    private final Random random = new Random();

    // 采样频率控制（每 X tick 采样一次）
    private static final int SAMPLE_INTERVAL = 10; // 每0.5秒采样一次
    private int tickCounter = 0;

    // FakePlayer 配置
    private static final GameProfile RESONATOR_PROFILE = new GameProfile(
            UUID.fromString("41C82C87-7AFB-4024-BA57-13D2C99CAE77"),
            "[GeologicalResonator]"
    );

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
                // 排除下界合金/远古残骸相关（避免模组兼容性问题）
                String lowerName = name.toLowerCase();
                if (lowerName.contains("netherite") || lowerName.contains("ancientdebris") || lowerName.contains("ancient_debris")) {
                    continue;
                }
                KNOWN_ORE_IDS.add(OreDictionary.getOreID(name));
            }
        }

        // 特殊可提取方块已清空（移除了Astral Sorcery天辉水晶等问题方块）

        System.out.println("[GeologicalResonator] 缓存了 " + KNOWN_ORE_IDS.size() + " 种矿物类型。");
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
        if (!(ctx.player.world instanceof WorldServer)) return;

        // ★ 性能优化：降低采样频率，每 SAMPLE_INTERVAL tick 采样一次
        tickCounter++;
        if (tickCounter < SAMPLE_INTERVAL) {
            return;
        }
        tickCounter = 0;

        // 根据等级决定每次采样数量 (1, 2, 3)
        int samples = ctx.level;

        for (int i = 0; i < samples; i++) {
            // 检查是否有足够的能量进行下一次转化
            if (ctx.getEnergy() < CONVERSION_COST) return;

            // 执行采样
            if (performResonance(ctx)) {
                // 成功转化，消耗能量
                ctx.consumeEnergy(CONVERSION_COST);
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

        // Y轴偏置采样: 优先采样玩家下方区域
        int y;
        if (center.getY() > 5) {
            y = random.nextInt(center.getY() - 3) + 3;
        } else {
            y = center.getY();
        }

        BlockPos targetPos = new BlockPos(x, y, z);

        // 检查区块是否加载
        if (!world.isBlockLoaded(targetPos)) return false;

        IBlockState state = world.getBlockState(targetPos);
        Block block = state.getBlock();

        // 判断是否是矿物并执行提取
        if (isValuableOre(state, world, targetPos, player)) {
            return extractOreWithFakePlayer((WorldServer) world, player, targetPos, state);
        }
        return false;
    }

    /**
     * 使用 FakePlayer 完整挖矿流程提取矿物
     */
    private boolean extractOreWithFakePlayer(WorldServer world, EntityPlayer realPlayer, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        String registryName = block.getRegistryName() != null ? block.getRegistryName().toString() : "";

        // 1. 创建 FakePlayer 并定位到目标方块附近（≤10格，通过安全检查）
        FakePlayer fakePlayer = FakePlayerFactory.get(world, RESONATOR_PROFILE);

        // 将 FakePlayer 定位到目标方块旁边（确保距离 < 10）
        fakePlayer.setPosition(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        // 给 FakePlayer 一把钻石镐（确保能挖掘）
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        fakePlayer.setHeldItem(EnumHand.MAIN_HAND, pickaxe);

        boolean success = false;

        // 2. 记录提取前的附近物品实体
        AxisAlignedBB collectBox = new AxisAlignedBB(pos).grow(2.0);
        List<EntityItem> itemsBefore = world.getEntitiesWithinAABB(EntityItem.class, collectBox);

        // 3. 执行完整挖矿流程
        boolean canHarvest = block.canHarvestBlock(world, pos, fakePlayer);

        if (canHarvest) {
            // 调用 removedByPlayer（触发方块的 breakBlock 逻辑）
            boolean removed = block.removedByPlayer(state, world, pos, fakePlayer, true);

            if (removed) {
                // 调用 harvestBlock（生成掉落物）
                block.harvestBlock(world, fakePlayer, pos, state, world.getTileEntity(pos), pickaxe);

                // 确保方块被移除
                if (world.getBlockState(pos).getBlock() != Blocks.AIR) {
                    world.setBlockToAir(pos);
                }

                success = true;
            }
        } else {
            // 无法正常采集，使用备用方案
            NonNullList<ItemStack> drops = NonNullList.create();
            block.getDrops(drops, world, pos, state, 0);

            if (!drops.isEmpty()) {
                world.setBlockToAir(pos);
                for (ItemStack drop : drops) {
                    if (!drop.isEmpty()) {
                        Block.spawnAsEntity(world, pos, drop);
                    }
                }
                success = true;
            }
        }

        // 4. 收集掉落物并传送到玩家位置
        if (success) {
            List<EntityItem> itemsAfter = world.getEntitiesWithinAABB(EntityItem.class, collectBox);

            for (EntityItem entityItem : itemsAfter) {
                // 只传送新生成的物品
                if (!itemsBefore.contains(entityItem) && !entityItem.isDead) {
                    // 传送物品到玩家位置
                    entityItem.setPosition(realPlayer.posX, realPlayer.posY + 0.5, realPlayer.posZ);
                    entityItem.motionX = 0;
                    entityItem.motionY = 0.1;
                    entityItem.motionZ = 0;
                }
            }

            // 播放效果
            playResonanceEffect(world, pos);
        }

        return success;
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

        // 检查是否是特殊可提取方块
        if (block.getRegistryName() != null) {
            String regName = block.getRegistryName().toString().toLowerCase();

            // ★ 早期排除下界合金/远古残骸（避免getPickBlock反射导致卡死）
            if (regName.contains("netherite") || regName.contains("debris") || regName.contains("ancient_debris")) {
                return false;
            }

            if (SPECIAL_VALUABLE_BLOCKS.contains(block.getRegistryName().toString())) {
                return true;
            }
        }

        // 获取方块对应的 ItemStack，用于矿物词典查询
        ItemStack stack = ItemStack.EMPTY;

        // 方法一: 使用 getPickBlock
        try {
            Vec3d centerVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            RayTraceResult raytrace = new RayTraceResult(centerVec, EnumFacing.UP, pos);
            stack = block.getPickBlock(state, raytrace, world, pos, player);
        } catch (Exception ignored) {}

        // 方法二: 后备方案
        if (stack.isEmpty()) {
            Item item = Item.getItemFromBlock(block);
            if (item != Items.AIR) {
                try {
                    stack = new ItemStack(item, 1, block.damageDropped(state));
                } catch (Exception ignored) {}
            }
        }

        if (stack.isEmpty()) return false;

        // 使用矿物词典检查
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
