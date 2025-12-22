package com.moremod.world;

import com.moremod.init.ModBlocks;
import com.moremod.schematic.Schematic;
import com.moremod.util.ReflectionHelper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 主世界自定义结构生成器
 * 从 resources/assets/moremod/schematics/overworld/ 加载 schematic 文件
 * 添加战利品箱、刷怪笼和特殊工作方块
 */
public class OverworldSchematicGenerator implements IWorldGenerator {

    // 生成配置
    private static final int SPAWN_CHANCE = 3000;  // 1/3000 区块概率 (降低生成率)
    private static final int MIN_DISTANCE_FROM_SPAWN = 300;
    private static final int MIN_Y = 60;

    // 特殊工作方块奖励列表
    private static final List<Block> WORKSTATION_REWARDS = new ArrayList<>();

    // Schematic 缓存
    private static final List<Schematic> SCHEMATICS = new ArrayList<>();
    private static boolean initialized = false;

    static {
        // 添加特殊工作方块奖励
        WORKSTATION_REWARDS.add(Blocks.CRAFTING_TABLE);
        WORKSTATION_REWARDS.add(Blocks.FURNACE);
        WORKSTATION_REWARDS.add(Blocks.ANVIL);
        WORKSTATION_REWARDS.add(Blocks.ENCHANTING_TABLE);
        WORKSTATION_REWARDS.add(Blocks.BREWING_STAND);
    }

    /**
     * 初始化加载 schematic 文件
     */
    private static void initialize() {
        if (initialized) return;
        initialized = true;

        System.out.println("[OverworldSchematicGenerator] 正在加载主世界结构...");

        // 加载 1.schematic 和 2.schematic
        for (int i = 1; i <= 2; i++) {
            String path = "/assets/moremod/schematics/overworld/" + i + ".schematic";
            try (InputStream is = OverworldSchematicGenerator.class.getResourceAsStream(path)) {
                if (is == null) {
                    System.err.println("[OverworldSchematicGenerator] 未找到: " + path);
                    continue;
                }
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
                Schematic schematic = Schematic.loadFromNBT(nbt);
                SCHEMATICS.add(schematic);
                System.out.println("[OverworldSchematicGenerator] 加载成功: " + i + ".schematic (" +
                        schematic.width + "x" + schematic.height + "x" + schematic.length + ")");
            } catch (Exception e) {
                System.err.println("[OverworldSchematicGenerator] 加载失败: " + path + " - " + e.getMessage());
            }
        }

        // 尝试添加 mod 方块到奖励列表
        try {
            if (ModBlocks.PRINTER != null) WORKSTATION_REWARDS.add(ModBlocks.PRINTER);
        } catch (Exception ignored) {}

        System.out.println("[OverworldSchematicGenerator] 加载完成，共 " + SCHEMATICS.size() + " 个结构");
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
                         IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        // 只在主世界生成
        if (world.provider.getDimension() != 0) return;

        initialize();

        if (SCHEMATICS.isEmpty()) return;

        // 生成概率检查
        if (random.nextInt(SPAWN_CHANCE) != 0) return;

        int worldX = chunkX * 16 + 8;
        int worldZ = chunkZ * 16 + 8;

        // 距离出生点检查
        double distFromSpawn = Math.sqrt(worldX * worldX + worldZ * worldZ);
        if (distFromSpawn < MIN_DISTANCE_FROM_SPAWN) return;

        // 寻找合适的地面高度
        int groundY = findGroundLevel(world, worldX, worldZ);
        if (groundY < MIN_Y) return;

        // 随机选择一个 schematic
        Schematic schematic = SCHEMATICS.get(random.nextInt(SCHEMATICS.size()));

        // 放置结构
        BlockPos basePos = new BlockPos(worldX, groundY, worldZ);
        placeStructure(world, basePos, schematic, random);

        System.out.println("[OverworldSchematicGenerator] 生成结构于 " + basePos);
    }

    /**
     * 寻找地面高度
     */
    private int findGroundLevel(World world, int x, int z) {
        for (int y = 100; y > 40; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            IBlockState stateAbove = world.getBlockState(pos.up());

            if (isSolidGround(state) && isAirOrVegetation(stateAbove)) {
                return y + 1;
            }
        }
        return -1;
    }

    private boolean isSolidGround(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.GRASS || block == Blocks.DIRT ||
               block == Blocks.STONE || block == Blocks.SAND ||
               block == Blocks.GRAVEL || block == Blocks.SANDSTONE;
    }

    private boolean isAirOrVegetation(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.AIR || block == Blocks.TALLGRASS ||
               block == Blocks.YELLOW_FLOWER || block == Blocks.RED_FLOWER ||
               block == Blocks.DOUBLE_PLANT;
    }

    /**
     * 放置结构并添加战利品
     */
    private void placeStructure(World world, BlockPos basePos, Schematic schematic, Random random) {
        // 1. 放置 schematic
        schematic.place(world, basePos.getX(), basePos.getY(), basePos.getZ());

        int sizeX = schematic.width;
        int sizeY = schematic.height;
        int sizeZ = schematic.length;

        // 2. 添加战利品箱 (2-4个)
        int chestCount = 2 + random.nextInt(3);
        for (int i = 0; i < chestCount; i++) {
            BlockPos chestPos = findRandomAirPos(world, basePos, sizeX, sizeY, sizeZ, random);
            if (chestPos != null) {
                placeRewardChest(world, chestPos, random);
            }
        }

        // 3. 添加刷怪笼 (1-2个，Weeping Angel)
        int spawnerCount = 1 + random.nextInt(2);
        for (int i = 0; i < spawnerCount; i++) {
            BlockPos spawnerPos = findRandomAirPos(world, basePos, sizeX, sizeY, sizeZ, random);
            if (spawnerPos != null) {
                placeWeepingAngelSpawner(world, spawnerPos);
            }
        }

        // 4. 添加特殊工作方块 (1-2个)
        int workstationCount = 1 + random.nextInt(2);
        for (int i = 0; i < workstationCount; i++) {
            BlockPos workPos = findRandomAirPos(world, basePos, sizeX, sizeY, sizeZ, random);
            if (workPos != null) {
                placeWorkstation(world, workPos, random);
            }
        }
    }

    /**
     * 在结构内找一个空气位置
     */
    private BlockPos findRandomAirPos(World world, BlockPos base, int sizeX, int sizeY, int sizeZ, Random random) {
        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = 2 + random.nextInt(Math.max(1, sizeX - 4));
            int dy = 1 + random.nextInt(Math.max(1, sizeY - 2));
            int dz = 2 + random.nextInt(Math.max(1, sizeZ - 4));

            BlockPos pos = base.add(dx, dy, dz);
            if (world.isAirBlock(pos) && !world.isAirBlock(pos.down())) {
                return pos;
            }
        }
        return null;
    }

    /**
     * 放置战利品箱
     */
    private void placeRewardChest(World world, BlockPos pos, Random random) {
        world.setBlockState(pos, Blocks.CHEST.getDefaultState(), 2);

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) te;

            // 添加随机战利品
            int itemCount = 3 + random.nextInt(5);
            for (int i = 0; i < itemCount; i++) {
                ItemStack loot = getRandomLoot(random);
                int slot = random.nextInt(27);
                if (chest.getStackInSlot(slot).isEmpty()) {
                    chest.setInventorySlotContents(slot, loot);
                }
            }
        }
    }

    /**
     * 获取随机战利品
     */
    private ItemStack getRandomLoot(Random random) {
        int type = random.nextInt(10);
        switch (type) {
            case 0: return new ItemStack(Items.DIAMOND, 1 + random.nextInt(3));
            case 1: return new ItemStack(Items.EMERALD, 1 + random.nextInt(4));
            case 2: return new ItemStack(Items.GOLD_INGOT, 2 + random.nextInt(5));
            case 3: return new ItemStack(Items.IRON_INGOT, 3 + random.nextInt(6));
            case 4: return new ItemStack(Items.ENDER_PEARL, 1 + random.nextInt(3));
            case 5: return new ItemStack(Items.BLAZE_ROD, 1 + random.nextInt(2));
            case 6: return new ItemStack(Items.GOLDEN_APPLE, 1);
            case 7: return new ItemStack(Items.EXPERIENCE_BOTTLE, 3 + random.nextInt(5));
            case 8: return new ItemStack(Items.ENCHANTED_BOOK);
            default: return new ItemStack(Items.REDSTONE, 5 + random.nextInt(10));
        }
    }

    /**
     * 放置 Weeping Angel 刷怪笼
     */
    private void placeWeepingAngelSpawner(World world, BlockPos pos) {
        world.setBlockState(pos, Blocks.MOB_SPAWNER.getDefaultState(), 2);

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityMobSpawner) {
            TileEntityMobSpawner spawner = (TileEntityMobSpawner) te;
            MobSpawnerBaseLogic logic = spawner.getSpawnerBaseLogic();
            logic.setEntityId(new ResourceLocation("moremod", "weeping_angel"));

            // 配置刷怪笼参数
            try {
                ReflectionHelper.SpawnerConfig config = ReflectionHelper.SpawnerConfig.normal();
                ReflectionHelper.setSpawnerByNBT(logic, config);
            } catch (Exception e) {
                System.err.println("[OverworldSchematicGenerator] 设置刷怪笼失败: " + e.getMessage());
            }
        }
    }

    /**
     * 放置特殊工作方块
     */
    private void placeWorkstation(World world, BlockPos pos, Random random) {
        Block workstation = WORKSTATION_REWARDS.get(random.nextInt(WORKSTATION_REWARDS.size()));
        world.setBlockState(pos, workstation.getDefaultState(), 2);
    }
}
