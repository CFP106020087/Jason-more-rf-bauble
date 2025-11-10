package com.moremod.item;

import com.moremod.dimension.PersonalDimensionManager;
import com.moremod.dimension.PersonalDimensionManager.PersonalSpace;
import com.moremod.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * 维度之钥（Dimensional Key）
 * - 在私人维度的锚定墙上开一个 2x3 的临时门洞
 * - 所有被移除的墙方块通过 PersonalDimensionManager.recordDoorHole(...) 记录
 * - 使用 PersonalDimensionManager.scheduleWallRestore(...) 定时回填
 *
 * 依赖：
 *   - PersonalDimensionManager#recordDoorHole(UUID, BlockPos)  （逐块记录）
 *   - PersonalDimensionManager#scheduleWallRestore(UUID, int)   （定时恢复，单位：ticks）
 */
public class ItemDimensionalKey extends Item {

    // 门洞宽/高（方块数）
    private static final int DOOR_WIDTH  = 2;
    private static final int DOOR_HEIGHT = 3;

    // 门洞维持时间（ticks）：这里默认 30 秒
    private static final int RESTORE_TICKS = 20 * 30;

    // 保护：距离上下边界至少预留 1 格
    private static final int Y_MARGIN = 1;

    public ItemDimensionalKey() {
        setRegistryName("dimensional_key");
        setTranslationKey("dimensional_key");
        setMaxStackSize(1);
        setMaxDamage(256); // 可自行调整耐久
        setCreativeTab(CreativeTabs.TOOLS);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
                                      EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        // 只允许在私人维度使用
        if (world.provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "只能在私人维度使用维度之钥！"), true);
            return EnumActionResult.FAIL;
        }

        // 必须有玩家空间
        PersonalSpace space = PersonalDimensionManager.getPlayerSpace(player.getUniqueID());
        if (space == null) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "未找到你的私人空间！"), true);
            return EnumActionResult.FAIL;
        }

        // 必须是房主本人
        if (!space.playerId.equals(player.getUniqueID())) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "你只能在自己的空间墙体上开门！"), true);
            return EnumActionResult.FAIL;
        }

        // 点击位置必须在该空间“墙体”上
        if (!space.isWall(pos)) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "请对准锚定墙体使用。"), true);
            return EnumActionResult.FAIL;
        }

        // 若存在锚定墙方块，额外校验（容错：没有该方块时也允许）
        IBlockState st = world.getBlockState(pos);
        if (!isAnchorWallBlock(st)) {
            // 不是我们认得的锚定墙，但仍处于墙体边界内 -> 放行（兼容老存档/替换方块）
            // 如果你想强制必须是锚定墙，将下面这行改为 return FAIL;
        }

        // 计算门洞朝向（位于 X 侧墙 or Z 侧墙）
        boolean onZWall = pos.getZ() == space.outerMinPos.getZ() || pos.getZ() == space.outerMaxPos.getZ();
        boolean onXWall = pos.getX() == space.outerMinPos.getX() || pos.getX() == space.outerMaxPos.getX();

        if (!onZWall && !onXWall) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "无法判定墙体朝向，请在墙面上使用。"), true);
            return EnumActionResult.FAIL;
        }

        // 限定门洞底部 Y（避免越界顶/底）
        int baseY = MathHelper.clamp(pos.getY(),
                space.innerMinPos.getY() + Y_MARGIN,
                space.innerMaxPos.getY() - (DOOR_HEIGHT - 1) - Y_MARGIN);

        int filled = carveDoorAndRecord((WorldServer) world, space, pos, onZWall, baseY);

        if (filled <= 0) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "该位置无法开门。"), true);
            return EnumActionResult.FAIL;
        }

        // 安排恢复（注意：Manager 内部若用 Map<UUID,Task> 将覆盖旧任务；
        // 但我们逐块 recordDoorHole，最终一次恢复会把所有门洞都回填）
        PersonalDimensionManager.scheduleWallRestore(player.getUniqueID(), RESTORE_TICKS);

        // 消耗耐久
        ItemStack stack = player.getHeldItem(hand);
        stack.damageItem(1, player);

        // 提示 & 粒子
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.GREEN + "已开启临时门洞，" + (RESTORE_TICKS / 20) + " 秒后自动恢复。"), true);
        spawnOpenParticles((WorldServer) world, pos);

        return EnumActionResult.SUCCESS;
    }

    /**
     * 在墙体上开 2x3 门洞，并逐块记录到 Manager。
     *
     * @param onZWall true=Z方向外墙（门宽沿X）；false=X方向外墙（门宽沿Z）
     * @return 实际清空/记录的墙方块数量
     */
    private int carveDoorAndRecord(WorldServer world, PersonalSpace space, BlockPos hitPos,
                                   boolean onZWall, int baseY) {
        int changed = 0;

        // 墙厚处理：外墙面向“内侧”的方向
        if (onZWall) {
            final int wallZ = (hitPos.getZ() == space.outerMinPos.getZ()) ? space.outerMinPos.getZ() : space.outerMaxPos.getZ();
            final int stepZ  = (wallZ == space.outerMinPos.getZ()) ? +1 : -1; // 往内是 +1 / -1

            // 门宽沿 X 轴
            int half = DOOR_WIDTH / 2;                // 2 -> 1
            int startX = hitPos.getX() - (half - 1);  // 使门洞相对居中：得到 [x, x+1]
            for (int dx = 0; dx < DOOR_WIDTH; dx++) {
                int x = MathHelper.clamp(startX + dx, space.outerMinPos.getX() + 1, space.outerMaxPos.getX() - 1);
                for (int dy = 0; dy < DOOR_HEIGHT; dy++) {
                    int y = baseY + dy;
                    // 仅清墙厚范围内的方块（避免掏进房间内部）
                    int z = wallZ; // 厚度通常=1，这里只处理墙所在那一列
                    BlockPos p = new BlockPos(x, y, z);
                    if (space.isWall(p)) {
                        IBlockState prev = world.getBlockState(p);
                        if (prev.getBlock() != Blocks.AIR) {
                            world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                            PersonalDimensionManager.recordDoorHole(space.playerId, p);
                            changed++;
                        }
                    }
                    // 若将来把墙厚设为 >1，可按 stepZ 继续推进内侧层：
                    // for (int t=1; t<wallThickness; t++) { z = wallZ + stepZ * t; ... }
                }
            }
        } else { // X 侧墙
            final int wallX = (hitPos.getX() == space.outerMinPos.getX()) ? space.outerMinPos.getX() : space.outerMaxPos.getX();
            final int stepX  = (wallX == space.outerMinPos.getX()) ? +1 : -1;

            int half = DOOR_WIDTH / 2;
            int startZ = hitPos.getZ() - (half - 1);
            for (int dz = 0; dz < DOOR_WIDTH; dz++) {
                int z = MathHelper.clamp(startZ + dz, space.outerMinPos.getZ() + 1, space.outerMaxPos.getZ() - 1);
                for (int dy = 0; dy < DOOR_HEIGHT; dy++) {
                    int y = baseY + dy;
                    int x = wallX;
                    BlockPos p = new BlockPos(x, y, z);
                    if (space.isWall(p)) {
                        IBlockState prev = world.getBlockState(p);
                        if (prev.getBlock() != Blocks.AIR) {
                            world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                            PersonalDimensionManager.recordDoorHole(space.playerId, p);
                            changed++;
                        }
                    }
                    // 同理：墙厚>1时，继续 x = wallX + stepX * t
                }
            }
        }

        return changed;
    }

    private boolean isAnchorWallBlock(IBlockState state) {
        try {
            Block anchor = ModBlocks.UNBREAKABLE_BARRIER_ANCHOR;
            if (anchor != null && state.getBlock() == anchor) return true;
        } catch (Throwable ignore) {}
        ResourceLocation rn = state.getBlock().getRegistryName();
        return rn != null && rn.toString().toLowerCase().contains("unbreakable_barrier");
    }

    private void spawnOpenParticles(WorldServer world, BlockPos pos) {
        for (int i = 0; i < 20; i++) {
            double ox = world.rand.nextGaussian() * 0.2;
            double oy = world.rand.nextGaussian() * 0.2;
            double oz = world.rand.nextGaussian() * 0.2;
            world.spawnParticle(EnumParticleTypes.PORTAL,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    1, ox, oy, oz, 0.01);
        }
        world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 0.7f, 1.2f);
    }
}
