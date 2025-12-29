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
            player.sendMessage(new TextComponentString("§e[调试] 检测到量子祭坛，但未找到Boss房间结构"));
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
     *
     * Boss房间大小: 40x40
     * 祭坛位置: (20, 1, 20) = BOSS_ROOM_SIZE / 2
     * 柱子位置: (8,8), (8,27), (27,8), (27,27)
     */
    private static boolean isInBossRoom(World world, BlockPos altarPos) {
        int pillarsFound = 0;

        // Boss房间模板中，祭坛在中心(20,1,20)
        // 柱子位置: (8,8), (8,27), (27,8), (27,27)
        // 相对于中心祭坛(20,20)的偏移量:
        int[][] pillarOffsets = {
                {-12, -12},  // 8-20 = -12, 8-20 = -12
                {-12, 7},    // 8-20 = -12, 27-20 = 7
                {7, -12},    // 27-20 = 7, 8-20 = -12
                {7, 7}       // 27-20 = 7, 27-20 = 7
        };

        StringBuilder debugInfo = new StringBuilder();

        // 检查每个可能的柱子位置
        for (int[] offset : pillarOffsets) {
            BlockPos pillarBase = altarPos.add(offset[0], 0, offset[1]);

            // ★ 扩大Y轴搜索范围：从 -2 到 +20（覆盖整个柱子高度）
            boolean hasPillar = false;
            for (int y = -2; y <= 20; y++) {
                BlockPos checkPos = pillarBase.up(y);
                net.minecraft.block.Block block = world.getBlockState(checkPos).getBlock();

                // 检查是否是不可破坏方块（柱子材料）- 支持所有屏障类型
                if (block == com.moremod.init.ModBlocks.UNBREAKABLE_BARRIER_VOID ||
                        block == com.moremod.init.ModBlocks.UNBREAKABLE_BARRIER_ANCHOR ||
                        block == com.moremod.init.ModBlocks.UNBREAKABLE_BARRIER_TEMPORAL ||
                        block instanceof com.moremod.block.BlockUnbreakableBarrier) {
                    hasPillar = true;
                    debugInfo.append("  柱子").append(pillarsFound + 1).append(": ").append(checkPos).append(" [").append(block.getLocalizedName()).append("]\n");
                    break;
                }
            }

            if (hasPillar) {
                pillarsFound++;
            } else {
                // ★ 调试：显示该位置实际有什么方块
                BlockPos samplePos = pillarBase.up(1);
                net.minecraft.block.Block sampleBlock = world.getBlockState(samplePos).getBlock();
                debugInfo.append("  未找到柱子@").append(pillarBase).append(" 实际方块: ").append(sampleBlock.getLocalizedName()).append("\n");
            }
        }

        // 至少找到2个柱子才确认是Boss房间（容错处理）
        boolean isBossRoom = pillarsFound >= 2;

        // 调试日志
        System.out.println("[Boss祭坛] 祭坛位置: " + altarPos + ", 找到柱子: " + pillarsFound + "/4, 判定: " + (isBossRoom ? "是Boss房间" : "不是Boss房间"));
        if (!isBossRoom) {
            System.out.println("[Boss祭坛] 详细信息:\n" + debugInfo.toString());
        }

        return isBossRoom;
    }
}