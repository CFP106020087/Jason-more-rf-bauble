package com.moremod.world;

import com.moremod.init.ModBlocks;
import com.moremod.init.ModItems;
import com.moremod.printer.ItemPrintTemplate;
import com.moremod.printer.PrinterRecipe;
import com.moremod.printer.PrinterRecipeRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * 科技废墟世界生成器
 *
 * 在主世界野外生成残破的科技感废墟建筑
 * 内含稀有机械方块、打印模版和故障装备
 */
public class RuinsWorldGenerator implements IWorldGenerator {

    // 生成配置
    private static final int MIN_Y = 63;           // 最低生成高度
    private static final int SPAWN_CHANCE = 150;   // 生成概率 (1/150 区块) - 约每150个区块生成一个
    private static final int MIN_DISTANCE_FROM_SPAWN = 200;  // 距离出生点最小距离

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {

        // 只在主世界生成
        if (world.provider.getDimension() != 0) {
            return;
        }

        // 随机概率检查
        if (random.nextInt(SPAWN_CHANCE) != 0) {
            return;
        }

        int worldX = chunkX * 16 + 8;
        int worldZ = chunkZ * 16 + 8;

        // 检查距离出生点的距离
        if (Math.abs(worldX) < MIN_DISTANCE_FROM_SPAWN && Math.abs(worldZ) < MIN_DISTANCE_FROM_SPAWN) {
            return;
        }

        // 找到合适的生成位置
        BlockPos basePos = findSuitablePosition(world, worldX, worldZ, random);
        if (basePos == null) {
            return;
        }

        // 随机选择废墟类型
        int ruinType = random.nextInt(6);
        switch (ruinType) {
            case 0:
                generateSmallLab(world, basePos, random);
                break;
            case 1:
                generateMediumFacility(world, basePos, random);
                break;
            case 2:
                generateLargeTower(world, basePos, random);
                break;
            case 3:
                generateUndergroundBunker(world, basePos, random);
                break;
            case 4:
                generateCrashedMachine(world, basePos, random);
                break;
            case 5:
                generateAncientWorkshop(world, basePos, random);
                break;
        }

        System.out.println("[Ruins] 在 " + basePos + " 生成了科技废墟 (类型: " + ruinType + ")");
    }

    /**
     * 找到合适的生成位置
     */
    private BlockPos findSuitablePosition(World world, int x, int z, Random random) {
        // 在范围内随机偏移
        x += random.nextInt(8) - 4;
        z += random.nextInt(8) - 4;

        // 从高处向下搜索地面 (提高到 128 以适应山区)
        for (int y = 128; y >= MIN_Y; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            IBlockState belowState = world.getBlockState(pos.down());

            // 检查是否是空气且下方是实心方块
            if (state.getBlock() == Blocks.AIR &&
                belowState.isFullCube() &&
                belowState.getMaterial().isSolid()) {

                // 检查周围是否足够平坦 (放宽条件)
                int solidCount = 0;
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (world.getBlockState(pos.add(dx, -1, dz)).isFullCube()) {
                            solidCount++;
                        }
                    }
                }

                // 25 个方块中至少 15 个是实心 (60%)
                if (solidCount >= 15) {
                    return pos;
                }
            }
        }
        return null;
    }

    /**
     * 生成小型实验室废墟 (5x5x4)
     */
    private void generateSmallLab(World world, BlockPos pos, Random random) {
        // 地基
        fillArea(world, pos.add(-2, -1, -2), pos.add(2, -1, 2), Blocks.STONEBRICK.getDefaultState());

        // 墙壁 (部分损坏)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 3; y++) {
                    boolean isWall = (x == -2 || x == 2 || z == -2 || z == 2);
                    if (isWall && random.nextFloat() > 0.3f) {  // 30%概率缺失
                        BlockPos blockPos = pos.add(x, y, z);
                        if (y < 2) {
                            setBlockSafe(world, blockPos, getRandomRuinBlock(random));
                        } else {
                            setBlockSafe(world, blockPos, Blocks.IRON_BARS.getDefaultState());
                        }
                    }
                }
            }
        }

        // 屋顶 (部分损坏)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (random.nextFloat() > 0.4f) {
                    setBlockSafe(world, pos.add(x, 4, z), Blocks.STONE_SLAB.getDefaultState());
                }
            }
        }

        // 放置特殊方块和战利品
        placeRuinContents(world, pos, random, 1);
    }

    /**
     * 生成中型设施废墟 (9x9x6)
     */
    private void generateMediumFacility(World world, BlockPos pos, Random random) {
        // 清理内部空间
        fillArea(world, pos.add(-4, 0, -4), pos.add(4, 5, 4), Blocks.AIR.getDefaultState());

        // 地基
        fillArea(world, pos.add(-4, -1, -4), pos.add(4, -1, 4), Blocks.STONEBRICK.getDefaultState());

        // 外墙框架
        for (int y = 0; y <= 5; y++) {
            // 四个角柱
            for (int[] corner : new int[][]{{-4, -4}, {-4, 4}, {4, -4}, {4, 4}}) {
                if (random.nextFloat() > 0.2f) {
                    setBlockSafe(world, pos.add(corner[0], y, corner[1]), Blocks.IRON_BLOCK.getDefaultState());
                }
            }

            // 横梁 (每隔一层)
            if (y % 2 == 0) {
                for (int x = -3; x <= 3; x++) {
                    if (random.nextFloat() > 0.3f) {
                        setBlockSafe(world, pos.add(x, y, -4), getRandomRuinBlock(random));
                        setBlockSafe(world, pos.add(x, y, 4), getRandomRuinBlock(random));
                    }
                }
                for (int z = -3; z <= 3; z++) {
                    if (random.nextFloat() > 0.3f) {
                        setBlockSafe(world, pos.add(-4, y, z), getRandomRuinBlock(random));
                        setBlockSafe(world, pos.add(4, y, z), getRandomRuinBlock(random));
                    }
                }
            }
        }

        // 内部机械装置
        for (int i = 0; i < 3; i++) {
            int rx = random.nextInt(5) - 2;
            int rz = random.nextInt(5) - 2;
            setBlockSafe(world, pos.add(rx, 0, rz), Blocks.REDSTONE_BLOCK.getDefaultState());
            if (random.nextBoolean()) {
                setBlockSafe(world, pos.add(rx, 1, rz), Blocks.PISTON.getDefaultState());
            }
        }

        // 放置特殊方块和战利品
        placeRuinContents(world, pos, random, 2);
    }

    /**
     * 生成大型塔楼废墟 (7x7x12)
     */
    private void generateLargeTower(World world, BlockPos pos, Random random) {
        // 塔基
        fillArea(world, pos.add(-3, -1, -3), pos.add(3, -1, 3), Blocks.STONEBRICK.getDefaultState());

        // 塔身
        for (int y = 0; y <= 11; y++) {
            int radius = y < 8 ? 3 : 2;  // 顶部收窄

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    boolean isEdge = (Math.abs(x) == radius || Math.abs(z) == radius);
                    if (isEdge) {
                        // 随着高度增加，损坏概率增加
                        float damageChance = 0.2f + (y * 0.05f);
                        if (random.nextFloat() > damageChance) {
                            setBlockSafe(world, pos.add(x, y, z), getRandomRuinBlock(random));
                        }
                    }
                }
            }

            // 每层的地板
            if (y % 4 == 0 && y > 0) {
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        if (random.nextFloat() > 0.4f) {
                            setBlockSafe(world, pos.add(x, y, z), Blocks.STONE_SLAB.getDefaultState());
                        }
                    }
                }
            }
        }

        // 塔顶装饰
        setBlockSafe(world, pos.add(0, 12, 0), Blocks.SEA_LANTERN.getDefaultState());

        // 放置特殊方块和战利品
        placeRuinContents(world, pos, random, 3);
    }

    /**
     * 生成地下掩体废墟 (7x7x-5)
     */
    private void generateUndergroundBunker(World world, BlockPos pos, Random random) {
        BlockPos bunkerPos = pos.down(6);

        // 挖掘地下空间
        fillArea(world, bunkerPos.add(-3, 0, -3), bunkerPos.add(3, 4, 3), Blocks.AIR.getDefaultState());

        // 加固墙壁
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y <= 4; y++) {
                    boolean isWall = (Math.abs(x) == 3 || Math.abs(z) == 3 || y == 0 || y == 4);
                    if (isWall) {
                        setBlockSafe(world, bunkerPos.add(x, y, z), Blocks.STONEBRICK.getDefaultState());
                    }
                }
            }
        }

        // 入口竖井
        for (int y = 1; y <= 5; y++) {
            setBlockSafe(world, pos.add(0, -y, 0), Blocks.AIR.getDefaultState());
            setBlockSafe(world, pos.add(0, -y, 1), Blocks.LADDER.getDefaultState());
        }

        // 内部装饰
        setBlockSafe(world, bunkerPos.add(0, 1, 0), Blocks.REDSTONE_LAMP.getDefaultState());
        setBlockSafe(world, bunkerPos.add(2, 1, 2), Blocks.IRON_BLOCK.getDefaultState());
        setBlockSafe(world, bunkerPos.add(-2, 1, -2), Blocks.IRON_BLOCK.getDefaultState());

        // 放置特殊方块和战利品 (在掩体内)
        placeRuinContents(world, bunkerPos.up(), random, 2);
    }

    /**
     * 生成坠毁机器废墟 (随机散落)
     */
    private void generateCrashedMachine(World world, BlockPos pos, Random random) {
        // 撞击坑
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                int depth = 2 - (int) Math.sqrt(x * x + z * z);
                if (depth > 0) {
                    for (int y = 0; y > -depth; y--) {
                        setBlockSafe(world, pos.add(x, y, z), Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        // 机器残骸散落
        for (int i = 0; i < 15; i++) {
            int rx = random.nextInt(9) - 4;
            int ry = random.nextInt(3);
            int rz = random.nextInt(9) - 4;

            Block debris = random.nextInt(3) == 0 ? Blocks.IRON_BLOCK :
                          random.nextInt(2) == 0 ? Blocks.REDSTONE_BLOCK : Blocks.PISTON;
            setBlockSafe(world, pos.add(rx, ry, rz), debris.getDefaultState());
        }

        // 核心残骸
        setBlockSafe(world, pos, Blocks.GLOWSTONE.getDefaultState());

        // 放置特殊方块和战利品
        placeRuinContents(world, pos.up(), random, 1);
    }

    /**
     * 生成远古工坊废墟 (11x11x5)
     */
    private void generateAncientWorkshop(World world, BlockPos pos, Random random) {
        // 地基
        fillArea(world, pos.add(-5, -1, -5), pos.add(5, -1, 5), Blocks.STONEBRICK.getDefaultState());

        // 外墙
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = 0; y <= 4; y++) {
                    boolean isWall = (Math.abs(x) == 5 || Math.abs(z) == 5);
                    if (isWall && random.nextFloat() > 0.35f) {
                        setBlockSafe(world, pos.add(x, y, z), getRandomRuinBlock(random));
                    }
                }
            }
        }

        // 内部工作台布局
        // 中央熔炉区
        setBlockSafe(world, pos.add(0, 0, 0), Blocks.FURNACE.getDefaultState());
        setBlockSafe(world, pos.add(1, 0, 0), Blocks.FURNACE.getDefaultState());
        setBlockSafe(world, pos.add(-1, 0, 0), Blocks.FURNACE.getDefaultState());

        // 工作台区
        setBlockSafe(world, pos.add(-3, 0, -3), Blocks.CRAFTING_TABLE.getDefaultState());
        setBlockSafe(world, pos.add(3, 0, -3), Blocks.CRAFTING_TABLE.getDefaultState());
        setBlockSafe(world, pos.add(-3, 0, 3), Blocks.ANVIL.getDefaultState());
        setBlockSafe(world, pos.add(3, 0, 3), Blocks.ENCHANTING_TABLE.getDefaultState());

        // 屋顶
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                if (random.nextFloat() > 0.5f) {
                    setBlockSafe(world, pos.add(x, 5, z), Blocks.STONE_SLAB.getDefaultState());
                }
            }
        }

        // 放置特殊方块和战利品
        placeRuinContents(world, pos, random, 3);
    }

    /**
     * 放置废墟内容物（特殊方块和战利品）
     */
    private void placeRuinContents(World world, BlockPos center, Random random, int lootTier) {
        // 放置战利品箱
        BlockPos chestPos = center.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
        setBlockSafe(world, chestPos, Blocks.CHEST.getDefaultState());

        // 填充战利品
        TileEntity te = world.getTileEntity(chestPos);
        if (te instanceof TileEntityChest) {
            fillChestWithLoot((TileEntityChest) te, random, lootTier);
        }

        // 随机放置特殊机械方块 (非常稀有)
        if (random.nextFloat() < 0.15f * lootTier) {  // 5-15%概率
            BlockPos specialPos = center.add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            placeSpecialBlock(world, specialPos, random);
        }
    }

    /**
     * 填充战利品箱
     */
    private void fillChestWithLoot(TileEntityChest chest, Random random, int tier) {
        // 基础材料
        int materialCount = 3 + random.nextInt(5);
        for (int i = 0; i < materialCount; i++) {
            int slot = random.nextInt(27);
            ItemStack material = getRandomMaterial(random, tier);
            chest.setInventorySlotContents(slot, material);
        }

        // 打印模版 (稀有)
        if (random.nextFloat() < 0.1f * tier) {
            int slot = random.nextInt(27);
            ItemStack template = createRandomTemplate(random);
            chest.setInventorySlotContents(slot, template);
        }

        // 故障装备 (非常稀有) - 暂时用钻石装备代替
        if (random.nextFloat() < 0.05f * tier) {
            int slot = random.nextInt(27);
            ItemStack gear = getRandomGear(random);
            chest.setInventorySlotContents(slot, gear);
        }
    }

    /**
     * 获取随机废墟方块
     */
    private IBlockState getRandomRuinBlock(Random random) {
        int type = random.nextInt(10);
        switch (type) {
            case 0: return Blocks.STONEBRICK.getStateFromMeta(1);  // 苔石砖
            case 1: return Blocks.STONEBRICK.getStateFromMeta(2);  // 裂石砖
            case 2: return Blocks.IRON_BLOCK.getDefaultState();
            case 3: return Blocks.QUARTZ_BLOCK.getDefaultState();
            case 4: return Blocks.CONCRETE.getStateFromMeta(8);    // 灰色混凝土
            default: return Blocks.STONEBRICK.getDefaultState();
        }
    }

    /**
     * 放置特殊方块
     */
    private void placeSpecialBlock(World world, BlockPos pos, Random random) {
        int type = random.nextInt(10);
        Block specialBlock;

        try {
            switch (type) {
                case 0:
                case 1:
                    // 时间加速器 (残破)
                    specialBlock = ModBlocks.TEMPORAL_ACCELERATOR;
                    break;
                case 2:
                    // 保护立场生成器
                    specialBlock = ModBlocks.PROTECTION_FIELD_GENERATOR;
                    break;
                case 3:
                    // 重生站核心
                    specialBlock = ModBlocks.RESPAWN_CHAMBER_CORE;
                    break;
                case 4:
                    // 维度织布机
                    specialBlock = ModBlocks.dimensionLoom;
                    break;
                case 5:
                    // 打印机
                    specialBlock = ModBlocks.PRINTER;
                    break;
                default:
                    // 默认放置铁块
                    specialBlock = Blocks.IRON_BLOCK;
            }

            if (specialBlock != null) {
                setBlockSafe(world, pos, specialBlock.getDefaultState());
                System.out.println("[Ruins] 在 " + pos + " 放置了特殊方块: " + specialBlock.getRegistryName());
            }
        } catch (Exception e) {
            // 如果方块不存在，放置默认方块
            setBlockSafe(world, pos, Blocks.IRON_BLOCK.getDefaultState());
        }
    }

    /**
     * 获取随机材料
     */
    private ItemStack getRandomMaterial(Random random, int tier) {
        int type = random.nextInt(10 + tier * 3);
        int count = 1 + random.nextInt(tier * 2);

        switch (type) {
            case 0:
            case 1:
            case 2:
                return new ItemStack(net.minecraft.init.Items.IRON_INGOT, count);
            case 3:
            case 4:
                return new ItemStack(net.minecraft.init.Items.GOLD_INGOT, count);
            case 5:
            case 6:
                return new ItemStack(net.minecraft.init.Items.REDSTONE, count * 2);
            case 7:
                return new ItemStack(net.minecraft.init.Items.DIAMOND, Math.max(1, count / 2));
            case 8:
                return new ItemStack(net.minecraft.init.Items.EMERALD, Math.max(1, count / 2));
            case 9:
                return new ItemStack(net.minecraft.init.Items.ENDER_PEARL, Math.max(1, count / 3));
            default:
                // 尝试返回模组材料
                try {
                    if (ModItems.ANCIENT_CORE_FRAGMENT != null && random.nextBoolean()) {
                        return new ItemStack(ModItems.ANCIENT_CORE_FRAGMENT, 1);
                    }
                    if (ModItems.RIFT_CRYSTAL != null) {
                        return new ItemStack(ModItems.RIFT_CRYSTAL, 1);
                    }
                } catch (Exception e) {
                    // 忽略
                }
                return new ItemStack(net.minecraft.init.Items.QUARTZ, count);
        }
    }

    /**
     * 创建随机打印模版
     */
    private ItemStack createRandomTemplate(Random random) {
        try {
            if (ModItems.PRINT_TEMPLATE != null) {
                // 从配方注册表获取所有已注册的配方
                java.util.Collection<PrinterRecipe> recipes = PrinterRecipeRegistry.getAllRecipes();
                if (!recipes.isEmpty()) {
                    // 转换为数组并随机选择
                    PrinterRecipe[] recipeArray = recipes.toArray(new PrinterRecipe[0]);
                    PrinterRecipe selectedRecipe = recipeArray[random.nextInt(recipeArray.length)];
                    return ItemPrintTemplate.createTemplate(ModItems.PRINT_TEMPLATE, selectedRecipe.getTemplateId());
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        // 备用返回
        return new ItemStack(net.minecraft.init.Items.PAPER, 1);
    }

    /**
     * 获取随机装备
     */
    private ItemStack getRandomGear(Random random) {
        // 暂时返回钻石装备，后续可替换为故障装备
        int type = random.nextInt(5);
        switch (type) {
            case 0: return new ItemStack(net.minecraft.init.Items.DIAMOND_HELMET);
            case 1: return new ItemStack(net.minecraft.init.Items.DIAMOND_CHESTPLATE);
            case 2: return new ItemStack(net.minecraft.init.Items.DIAMOND_LEGGINGS);
            case 3: return new ItemStack(net.minecraft.init.Items.DIAMOND_BOOTS);
            default: return new ItemStack(net.minecraft.init.Items.DIAMOND_SWORD);
        }
    }

    /**
     * 安全设置方块
     */
    private void setBlockSafe(World world, BlockPos pos, IBlockState state) {
        if (world.isBlockLoaded(pos)) {
            world.setBlockState(pos, state, 2);
        }
    }

    /**
     * 填充区域
     */
    private void fillArea(World world, BlockPos from, BlockPos to, IBlockState state) {
        for (int x = from.getX(); x <= to.getX(); x++) {
            for (int y = from.getY(); y <= to.getY(); y++) {
                for (int z = from.getZ(); z <= to.getZ(); z++) {
                    setBlockSafe(world, new BlockPos(x, y, z), state);
                }
            }
        }
    }
}
