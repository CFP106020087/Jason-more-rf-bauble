package com.moremod.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

import static com.moremod.moremod.MODID;

/**
 * 科技废墟战利品管理器 (Minecraft 1.12.2)
 * 使用 JSON 战利品表，路径：assets/moremod/loot_tables/ruins/*.json
 *
 * 支持5个等级的战利品表：
 * - tier_1: 基础奖励（研究前哨站、信号塔等）
 * - tier_2: 普通奖励（坠毁空降舱等）
 * - tier_3: 中级奖励（机械综合体、数据中心等）
 * - tier_4: 高级奖励（地下金库、废弃工厂、能量中继站）
 * - tier_5: 顶级奖励（量子采矿场、时间实验室、聚变反应环、虚空工厂）
 */
@Mod.EventBusSubscriber(modid = MODID)
public class RuinsLootManager {

    private static final Random RANDOM = new Random();
    private static RuinsLootManager INSTANCE;

    // ---- 战利品表资源位置 ----
    public static final ResourceLocation LOOT_TABLE_TIER_1 = new ResourceLocation(MODID, "ruins/tier_1");
    public static final ResourceLocation LOOT_TABLE_TIER_2 = new ResourceLocation(MODID, "ruins/tier_2");
    public static final ResourceLocation LOOT_TABLE_TIER_3 = new ResourceLocation(MODID, "ruins/tier_3");
    public static final ResourceLocation LOOT_TABLE_TIER_4 = new ResourceLocation(MODID, "ruins/tier_4");
    public static final ResourceLocation LOOT_TABLE_TIER_5 = new ResourceLocation(MODID, "ruins/tier_5");

    private RuinsLootManager() {}

    public static RuinsLootManager getInstance() {
        if (INSTANCE == null) INSTANCE = new RuinsLootManager();
        return INSTANCE;
    }

    /**
     * 根据等级获取对应的战利品表
     * @param tier 1-5
     */
    public ResourceLocation getLootTableForTier(int tier) {
        switch (tier) {
            case 1: return LOOT_TABLE_TIER_1;
            case 2: return LOOT_TABLE_TIER_2;
            case 3: return LOOT_TABLE_TIER_3;
            case 4: return LOOT_TABLE_TIER_4;
            case 5: return LOOT_TABLE_TIER_5;
            default:
                // tier超出范围时，使用最接近的等级
                if (tier <= 0) return LOOT_TABLE_TIER_1;
                return LOOT_TABLE_TIER_5;
        }
    }

    /**
     * 设置箱子的loot table（延迟生成，玩家打开时才生成物品）
     */
    public void fillChest(World world, BlockPos pos, int tier) {
        if (world.isRemote) return;

        // 确保是箱子方块
        Block block = world.getBlockState(pos).getBlock();
        if (!(block instanceof net.minecraft.block.BlockChest)) {
            world.setBlockState(pos, Blocks.CHEST.getDefaultState(), 2);
        }

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) te;
            ResourceLocation rl = getLootTableForTier(tier);
            chest.setLootTable(rl, RANDOM.nextLong());
            System.out.println("[Ruins] 设置战利品表 Tier " + tier + " => " + rl);
        }
    }

    /**
     * 立即填充箱子（不等待玩家打开）
     * 用于世界生成时确保箱子有物品
     */
    public void fillChestImmediately(World world, BlockPos pos, int tier) {
        if (world.isRemote || !(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 确保是箱子方块
        Block block = world.getBlockState(pos).getBlock();
        if (!(block instanceof net.minecraft.block.BlockChest)) {
            world.setBlockState(pos, Blocks.CHEST.getDefaultState(), 2);
        }

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityChest)) return;
        TileEntityChest chest = (TileEntityChest) te;
        chest.clear();

        ResourceLocation rl = getLootTableForTier(tier);
        LootTable table = ws.getLootTableManager().getLootTableFromLocation(rl);

        System.out.println("[Ruins] 查找战利品表 " + rl + " => " +
                (table == LootTable.EMPTY_LOOT_TABLE ? "空（未找到）" : "成功"));

        if (table != null && table != LootTable.EMPTY_LOOT_TABLE) {
            LootContext ctx = new LootContext.Builder(ws).build();
            table.fillInventory(chest, ws.rand, ctx);
            // 立即生成模式下清掉表引用，避免重复生成
            chest.setLootTable(null, 0);
        } else {
            // 备用：如果战利品表未找到，使用硬编码的基础物品
            System.out.println("[Ruins] 警告：战利品表未找到，使用备用物品");
            fillChestFallback(chest, tier, ws.rand);
        }

        // 同步到客户端
        chest.markDirty();
        IBlockState s = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, s, s, 3);
    }

    /**
     * 备用填充方法（当JSON战利品表加载失败时使用）
     */
    private void fillChestFallback(TileEntityChest chest, int tier, Random random) {
        // 基础材料
        int materialCount = 3 + tier;
        for (int i = 0; i < materialCount; i++) {
            int slot = random.nextInt(27);
            int type = random.nextInt(5);
            switch (type) {
                case 0:
                    chest.setInventorySlotContents(slot,
                        new net.minecraft.item.ItemStack(net.minecraft.init.Items.IRON_INGOT, 1 + random.nextInt(3)));
                    break;
                case 1:
                    chest.setInventorySlotContents(slot,
                        new net.minecraft.item.ItemStack(net.minecraft.init.Items.GOLD_INGOT, 1 + random.nextInt(2)));
                    break;
                case 2:
                    chest.setInventorySlotContents(slot,
                        new net.minecraft.item.ItemStack(net.minecraft.init.Items.REDSTONE, 2 + random.nextInt(4)));
                    break;
                case 3:
                    chest.setInventorySlotContents(slot,
                        new net.minecraft.item.ItemStack(net.minecraft.init.Items.COAL, 3 + random.nextInt(5)));
                    break;
                default:
                    chest.setInventorySlotContents(slot,
                        new net.minecraft.item.ItemStack(net.minecraft.init.Items.QUARTZ, 2 + random.nextInt(3)));
            }
        }

        // 高等级有几率给钻石
        if (tier >= 3 && random.nextFloat() < 0.3f * tier) {
            chest.setInventorySlotContents(random.nextInt(27),
                new net.minecraft.item.ItemStack(net.minecraft.init.Items.DIAMOND, 1));
        }
    }

    /**
     * 在指定位置创建箱子并填充战利品
     */
    public void createAndFillChest(World world, BlockPos pos, int tier, boolean immediate) {
        // 放置箱子
        world.setBlockState(pos, Blocks.CHEST.getDefaultState(), 2);

        if (immediate) {
            fillChestImmediately(world, pos, tier);
        } else {
            fillChest(world, pos, tier);
        }
    }

    /**
     * 监听战利品表加载事件（用于调试）
     */
    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (MODID.equals(name.getNamespace()) && name.getPath().startsWith("ruins/")) {
            System.out.println("[Ruins] 战利品表已加载: " + name);
        }
    }

    /**
     * 调试：列出所有废墟战利品表
     */
    public void debugListLootTables() {
        System.out.println("[Ruins] 战利品表列表:");
        System.out.println(" - " + LOOT_TABLE_TIER_1 + " (Tier 1: 基础)");
        System.out.println(" - " + LOOT_TABLE_TIER_2 + " (Tier 2: 普通)");
        System.out.println(" - " + LOOT_TABLE_TIER_3 + " (Tier 3: 中级)");
        System.out.println(" - " + LOOT_TABLE_TIER_4 + " (Tier 4: 高级)");
        System.out.println(" - " + LOOT_TABLE_TIER_5 + " (Tier 5: 顶级)");
    }
}
