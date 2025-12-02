package com.moremod.dungeon.boss;

import com.moremod.init.ModBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class BossAltarInteractionHandler {

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        if (world.isRemote) return;

        BlockPos pos = event.getPos();
        EntityPlayer player = event.getEntityPlayer();

        // 检查是否是量子锁定方块（Boss祭坛）
        if (world.getBlockState(pos).getBlock() != ModBlocks.UNBREAKABLE_BARRIER_QUANTUM) {
            return;
        }

        // 检查是否在Boss房间内
        if (!isInBossRoom(world, pos)) {
            // 不在Boss房间的信标不做处理
            return;
        }

        ItemStack heldItem = player.getHeldItem(event.getHand());

        // 检查玩家是否持有下界之星
        if (heldItem.isEmpty() || heldItem.getItem() != Items.NETHER_STAR) {
            player.sendMessage(new TextComponentString("§c需要下界之星才能激活量子祭坛！"));
            event.setCanceled(true);
            return;
        }

        // 尝试生成Boss
        if (DungeonBossSpawner.trySpawnBoss(world, pos, player)) {
            // 消耗下界之星（创造模式除外）
            if (!player.capabilities.isCreativeMode) {
                heldItem.shrink(1);
            }

            player.sendMessage(new TextComponentString("§5量子祭坛开始共鸣..."));
        }

        event.setCanceled(true);
    }

    /**
     * 检查祭坛是否在Boss房间内
     * 根据EnhancedRoomTemplates中的实际柱子位置调整
     */
    private static boolean isInBossRoom(World world, BlockPos altarPos) {
        int pillarsFound = 0;

        // Boss房间模板中，祭坛在中心(18,1,18)
        // 柱子位置相对于基点是: (8,8), (8,27), (27,8), (27,27)
        // 相对于中心祭坛的偏移量:
        int[][] pillarOffsets = {
                {-10, -10},  // 8-18 = -10
                {-10, 9},    // 8-18 = -10, 27-18 = 9
                {9, -10},    // 27-18 = 9
                {9, 9},      // 27-18 = 9
                // 额外的柱子
                {-1, -10},   // 17-18 = -1, 8-18 = -10
                {-1, 9},     // 17-18 = -1, 27-18 = 9
                {-10, -1},   // 8-18 = -10, 17-18 = -1
                {9, -1}      // 27-18 = 9, 17-18 = -1
        };

        // 检查每个可能的柱子位置
        for (int[] offset : pillarOffsets) {
            BlockPos pillarBase = altarPos.add(offset[0], 0, offset[1]);

            // 检查柱子是否存在（检查柱子的几个高度）
            boolean hasPillar = false;
            for (int y = -1; y <= 5; y++) {
                BlockPos checkPos = pillarBase.up(y);

                // 检查是否是不可破坏方块（柱子材料）
                if (world.getBlockState(checkPos).getBlock() ==
                        com.moremod.init.ModBlocks.UNBREAKABLE_BARRIER_VOID ||
                        world.getBlockState(checkPos).getBlock() ==
                                com.moremod.init.ModBlocks.UNBREAKABLE_BARRIER_ANCHOR) {
                    hasPillar = true;
                    break;
                }
            }

            if (hasPillar) {
                pillarsFound++;
            }
        }

        // 至少找到4个柱子才确认是Boss房间
        boolean isBossRoom = pillarsFound >= 4;

        if (isBossRoom) {
            System.out.println("[Boss祭坛] 检测到Boss房间，找到 " + pillarsFound + " 个柱子");
        }

        return isBossRoom;
    }
}