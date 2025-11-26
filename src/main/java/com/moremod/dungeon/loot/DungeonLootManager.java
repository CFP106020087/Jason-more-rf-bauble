package com.moremod.dungeon.loot;

import com.moremod.dungeon.DungeonTypes.RoomType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityLockableLoot;
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
 * 地牢戰利品管理器 (Minecraft 1.12.2)
 * 使用 JSON 戰利品表，路徑：assets/moremod/loot_tables/dungeon/*.json
 */
@Mod.EventBusSubscriber(modid = MODID)
public class DungeonLootManager {

    private static final Random RANDOM = new Random();
    private static DungeonLootManager INSTANCE;

    // ---- 戰利品表資源位置 ----
    public static final ResourceLocation LOOT_TABLE_NORMAL   = new ResourceLocation(MODID, "dungeon/dungeon_normal");
    public static final ResourceLocation LOOT_TABLE_TREASURE = new ResourceLocation(MODID, "dungeon/dungeon_treasure");
    public static final ResourceLocation LOOT_TABLE_BOSS     = new ResourceLocation(MODID, "dungeon/dungeon_boss");
    public static final ResourceLocation LOOT_TABLE_TRAP     = new ResourceLocation(MODID, "dungeon/dungeon_trap");
    public static final ResourceLocation LOOT_TABLE_HUB      = new ResourceLocation(MODID, "dungeon/dungeon_hub");
    public static final ResourceLocation LOOT_TABLE_ENTRANCE = new ResourceLocation(MODID, "dungeon/dungeon_entrance");
    public static final ResourceLocation LOOT_TABLE_EXIT     = new ResourceLocation(MODID, "dungeon/dungeon_exit");

    private DungeonLootManager() {}

    public static DungeonLootManager getInstance() {
        if (INSTANCE == null) INSTANCE = new DungeonLootManager();
        return INSTANCE;
    }

    // 根據房間類型挑選表
    private ResourceLocation getLootTableForRoom(RoomType roomType) {
        switch (roomType) {
            case NORMAL:    return LOOT_TABLE_NORMAL;
            case TREASURE:  return LOOT_TABLE_TREASURE;
            case BOSS:      return LOOT_TABLE_BOSS;
            case TRAP:      return LOOT_TABLE_TRAP;
            case HUB:       return LOOT_TABLE_HUB;
            case ENTRANCE:  return LOOT_TABLE_ENTRANCE;
            case EXIT:      return LOOT_TABLE_EXIT;
            default:        return LOOT_TABLE_NORMAL;
        }
    }

    /** 設置 loot table；實際戰利品在玩家「打開」時才生成 */
    public void fillChest(World world, BlockPos pos, RoomType roomType) {
        if (world.isRemote) return;

        // 放置箱子（若不是普通/陷阱箱，強制換成普通箱）
        Block block = world.getBlockState(pos).getBlock();
        if (!(block instanceof net.minecraft.block.BlockChest)) {
            world.setBlockState(pos, Blocks.CHEST.getDefaultState(), 2);
        }

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) te;
            ResourceLocation rl = getLootTableForRoom(roomType);
            chest.setLootTable(rl, RANDOM.nextLong());
            System.out.println("[MoreMod] set lootTable for " + roomType.name + " => " + rl);
        }
    }

    /** 立即生成（不等打開）；帶同步與調試打印 */
    public void fillChestImmediately(World world, BlockPos pos, RoomType roomType) {
        if (world.isRemote || !(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        Block block = world.getBlockState(pos).getBlock();
        if (!(block instanceof net.minecraft.block.BlockChest)) {
            world.setBlockState(pos, Blocks.CHEST.getDefaultState(), 2);
        }

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityChest)) return;
        TileEntityChest chest = (TileEntityChest) te;
        chest.clear();

        ResourceLocation rl = getLootTableForRoom(roomType);
        LootTable table = ws.getLootTableManager().getLootTableFromLocation(rl);
        System.out.println("[MoreMod] Lookup " + rl + " => " +
                (table == LootTable.EMPTY_LOOT_TABLE ? "EMPTY (NOT FOUND)" : "OK"));

        if (table != null && table != LootTable.EMPTY_LOOT_TABLE) {
            LootContext ctx = new LootContext.Builder(ws).build();
            table.fillInventory(chest, ws.rand, ctx);
            // 立即生成模式下清掉表引用，避免重複
            chest.setLootTable(null, 0);
        }

        // 同步到客戶端
        chest.markDirty();
        IBlockState s = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, s, s, 3);
    }

    /** 填充多個箱子 */
    public void fillChests(World world, BlockPos[] positions, RoomType roomType) {
        for (BlockPos p : positions) fillChest(world, p, roomType);
    }

    /** 在範圍內尋找所有（普通/陷阱）箱子 */
    public void fillChestsInArea(World world, BlockPos center, int radius, RoomType roomType) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = center.add(x, y, z);
                    if (world.getBlockState(p).getBlock() instanceof net.minecraft.block.BlockChest) {
                        fillChest(world, p, roomType);
                    }
                }
            }
        }
    }

    /** 創建（可選陷阱）箱並綁定 loot table */
    public void createSpecialChest(World world, BlockPos pos, RoomType roomType, boolean trapped) {
        if (trapped && (roomType == RoomType.TRAP || roomType == RoomType.TREASURE)) {
            world.setBlockState(pos, Blocks.TRAPPED_CHEST.getDefaultState(), 2);
        } else {
            world.setBlockState(pos, Blocks.CHEST.getDefaultState(), 2);
        }

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityLockableLoot) {
            ((TileEntityLockableLoot) te).setLootTable(getLootTableForRoom(roomType), RANDOM.nextLong());
        }

        if (roomType == RoomType.TREASURE || roomType == RoomType.BOSS) {
            world.playEvent(2003, pos.up(), 0);
        }
    }

    /** 監聽：只打印我們的表是否被載入（方便排錯） */
    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (MODID.equals(name.getNamespace()) && name.getPath().startsWith("dungeon/")) {
            System.out.println("[MoreMod] loot table loaded: " + name);
        }
    }

    /** 簡易描述（調試用） */
    public String getRoomDescription(RoomType type) {
        switch (type) {
            case ENTRANCE: return "入口房間 - 基礎補給";
            case NORMAL:   return "普通房間 - 標準戰利品";
            case HUB:      return "樞紐房間 - 探索輔助";
            case TRAP:     return "陷阱房間 - 少量補償";
            case TREASURE: return "寶藏房間 - 豐富獎勵";
            case BOSS:     return "BOSS房間 - 稀有戰利品";
            case EXIT:     return "出口房間 - 完成獎勵";
            default:       return "未知房間類型";
        }
    }

    public void debugListLootTables() {
        System.out.println("[MoreMod] loot tables:");
        System.out.println(" - " + LOOT_TABLE_NORMAL);
        System.out.println(" - " + LOOT_TABLE_TREASURE);
        System.out.println(" - " + LOOT_TABLE_BOSS);
        System.out.println(" - " + LOOT_TABLE_TRAP);
        System.out.println(" - " + LOOT_TABLE_HUB);
        System.out.println(" - " + LOOT_TABLE_ENTRANCE);
        System.out.println(" - " + LOOT_TABLE_EXIT);
    }
}
