package com.moremod.item;

import com.moremod.block.BlockUnbreakableBarrier;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.dimension.PersonalDimensionManager;
import com.moremod.dimension.PersonalDimensionManager.PersonalSpace;
import com.moremod.dungeon.portal.PortalManager;
import com.moremod.entity.EntityVoidPortal;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 维度钥匙 - 完整修复版
 *
 * 功能：
 * 1. ✅ 对虚空水晶使用 - 开启传送门
 * 2. ✅ 对维度墙使用 - 临时开门
 */
public class ItemDimensionalKey extends Item {

    // 门的尺寸
    private static final int DOOR_WIDTH = 3;
    private static final int DOOR_HEIGHT = 4;
    private static final int DOOR_DURATION_TICKS = 600; // 30秒

    public ItemDimensionalKey() {
        setRegistryName("dimensional_key");
        setTranslationKey("dimensional_key");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
        // 耐久度设为0表示无限耐久
        setMaxDamage(0);
    }

    @Override
    public boolean isDamageable() {
        return false; // 永不消耗耐久
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        // 不做任何事，防止耐久消耗
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
                                      EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        // 仅限私人维度使用
        if (world.provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "维度钥匙只能在私人维度使用"
            ), true);
            return EnumActionResult.FAIL;
        }

        final ItemStack stack = player.getHeldItem(hand);
        final IBlockState state = world.getBlockState(pos);
        final Block block = state.getBlock();

        // 获取玩家空间
        final UUID pid = player.getUniqueID();
        final PersonalSpace space = PersonalDimensionManager.getPlayerSpace(pid);

        if (space == null) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "你还没有私人空间"
            ), true);
            return EnumActionResult.FAIL;
        }

        // ===== 功能1: 虚空水晶传送 =====
        if (block instanceof BlockUnbreakableBarrier) {
            BlockUnbreakableBarrier barrier = (BlockUnbreakableBarrier) block;
            if (barrier.getType() == BlockUnbreakableBarrier.BarrierType.VOID_CRYSTAL) {
                return handleVoidCrystal(player, world, pos, facing, stack);
            }
        }

        // ===== 功能2: 维度墙开门 =====
        if (isAnchorWallBlock(state) && space.isWall(pos)) {
            return handleDimensionWall(player, world, pos, facing, stack, space);
        }

        // 其他情况
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.YELLOW + "请对准虚空水晶或维度墙使用"
        ), true);
        return EnumActionResult.FAIL;
    }

    /**
     * 处理虚空水晶 - 创建传送门
     */
    private EnumActionResult handleVoidCrystal(EntityPlayer player, World world,
                                               BlockPos pos, EnumFacing facing,
                                               ItemStack stack) {
        // 查询目标位置
        PortalManager.LocationData locationData = PortalManager.getDestination(world, pos);

        if (locationData == null) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "此虚空水晶尚未链接到其他位置"
            ), true);
            return EnumActionResult.FAIL;
        }

        final BlockPos destination = locationData.pos;

        // 跨维度提示
        if (locationData.dimension != world.provider.getDimension()) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GOLD + "目标位于维度 " + locationData.dimension
            ), true);
        }

        // 检查是否已有传送门
        AxisAlignedBB area = new AxisAlignedBB(
                pos.getX() - 3, pos.getY() - 1, pos.getZ() - 3,
                pos.getX() + 3, pos.getY() + 3, pos.getZ() + 3
        );

        if (!world.getEntitiesWithinAABB(EntityVoidPortal.class, area).isEmpty()) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "此处已有传送门开启"
            ), true);
            return EnumActionResult.SUCCESS;
        }

        // 创建传送门实体
        BlockPos portalPos = pos.offset(facing);
        EntityVoidPortal portal = new EntityVoidPortal(world, portalPos, destination, pos);
        world.spawnEntity(portal);

        // 消耗耐久
        stack.damageItem(1, player);

        // 音效和提示
        world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                SoundCategory.BLOCKS, 1.0F, 1.0F);

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.GREEN + "✨ 虚空传送门已开启（30秒）"
        ), true);

        return EnumActionResult.SUCCESS;
    }

    /**
     * 处理维度墙 - 打开临时通道
     */
    private EnumActionResult handleDimensionWall(EntityPlayer player, World world,
                                                 BlockPos pos, EnumFacing facing,
                                                 ItemStack stack, PersonalSpace space) {
        if (createTemporaryDoor(world, pos, facing, player, space)) {
            // 消耗耐久
            stack.damageItem(1, player);

            // 音效
            world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);

            // 安排恢复
            PersonalDimensionManager.scheduleWallRestore(player.getUniqueID(), DOOR_DURATION_TICKS);

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "✨ 维度门已开启（30秒后自动关闭）"
            ), true);

            return EnumActionResult.SUCCESS;
        }

        return EnumActionResult.FAIL;
    }

    /**
     * 创建临时门
     */
    private boolean createTemporaryDoor(World world, BlockPos clickedPos, EnumFacing facing,
                                        EntityPlayer player, PersonalSpace space) {
        final BlockPos doorCenter = findDoorPosition(world, clickedPos, facing, space);

        if (doorCenter == null) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "无法确定门的位置"
            ), true);
            return false;
        }

        boolean isYAxis = (facing == EnumFacing.UP || facing == EnumFacing.DOWN);
        List<BlockPos> doorHoles = new ArrayList<>();
        int removed = 0;

        if (isYAxis) {
            // 天花/地板：开 3×3 洞口
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos doorPos = doorCenter.add(x, 0, z);
                    if (space.isWall(doorPos)) {
                        IBlockState cur = world.getBlockState(doorPos);
                        if (isAnchorWallBlock(cur)) {
                            world.setBlockToAir(doorPos);
                            doorHoles.add(doorPos);
                            notifyNeighbors(world, doorPos, cur);
                            spawnDoorParticles((WorldServer) world, doorPos);
                            removed++;
                        }
                    }
                }
            }
        } else {
            // 墙面：开 3×4 洞口
            for (int w = -(DOOR_WIDTH / 2); w <= (DOOR_WIDTH / 2); w++) {
                for (int h = 0; h < DOOR_HEIGHT; h++) {
                    BlockPos doorPos = (facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH)
                            ? doorCenter.add(w, h, 0)
                            : doorCenter.add(0, h, w);

                    if (space.isWall(doorPos)) {
                        IBlockState cur = world.getBlockState(doorPos);
                        if (isAnchorWallBlock(cur)) {
                            world.setBlockToAir(doorPos);
                            doorHoles.add(doorPos);
                            notifyNeighbors(world, doorPos, cur);
                            spawnDoorParticles((WorldServer) world, doorPos);
                            removed++;
                        }
                    }
                }
            }
        }

        if (removed == 0) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "无法在此位置创建门"
            ), true);
            return false;
        }

        // 记录门洞位置
        PersonalDimensionManager.setDoorHoles(player.getUniqueID(), doorHoles);

        // 门框效果
        addDoorFrameEffects((WorldServer) world, doorCenter, facing);

        return true;
    }

    /**
     * 判断是否为维度锚定墙
     */
    private boolean isAnchorWallBlock(IBlockState state) {
        Block block = state.getBlock();

        // 自定义墙体方块
        if (block instanceof BlockUnbreakableBarrier) {
            return true;
        }

        // 通过注册名识别
        if (block.getRegistryName() != null) {
            String name = block.getRegistryName().toString();
            if (name.contains("unbreakable_barrier")) {
                return true;
            }
        }

        // 兜底：基岩
        return block == Blocks.BEDROCK;
    }

    /**
     * 确定门中心位置
     */
    private BlockPos findDoorPosition(World world, BlockPos clickedPos, EnumFacing facing,
                                      PersonalSpace space) {
        if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) {
            return clickedPos;
        }

        // 墙面门：放在地面上方
        int floorY = space.innerMinPos.getY() + 1;
        return new BlockPos(clickedPos.getX(), floorY, clickedPos.getZ());
    }

    /**
     * 通知邻区块更新
     */
    private void notifyNeighbors(World world, BlockPos pos, IBlockState oldState) {
        world.markBlockRangeForRenderUpdate(pos, pos);
        world.notifyBlockUpdate(pos, oldState, Blocks.AIR.getDefaultState(), 3);
    }

    /**
     * 门洞粒子效果
     */
    private void spawnDoorParticles(WorldServer world, BlockPos pos) {
        for (int i = 0; i < 5; i++) {
            double x = pos.getX() + 0.5 + (world.rand.nextDouble() - 0.5);
            double y = pos.getY() + 0.5 + (world.rand.nextDouble() - 0.5);
            double z = pos.getZ() + 0.5 + (world.rand.nextDouble() - 0.5);
            world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, 1, 0, 0, 0, 0.0D);
        }
    }

    /**
     * 门框粒子效果
     */
    private void spawnFrameParticles(WorldServer world, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        world.spawnParticle(EnumParticleTypes.END_ROD, x, y, z, 1, 0, 0.05, 0, 0.0D);
        world.spawnParticle(EnumParticleTypes.SPELL_INSTANT, x, y, z, 1, 0, 0, 0, 0.0D);
    }

    /**
     * 添加门框视觉效果
     */
    private void addDoorFrameEffects(WorldServer world, BlockPos doorCenter, EnumFacing facing) {
        boolean isYAxis = (facing == EnumFacing.UP || facing == EnumFacing.DOWN);

        if (isYAxis) {
            // 水平门框
            for (int i = -2; i <= 2; i++) {
                spawnFrameParticles(world, doorCenter.add(i, 0, -2));
                spawnFrameParticles(world, doorCenter.add(i, 0, 2));
                spawnFrameParticles(world, doorCenter.add(-2, 0, i));
                spawnFrameParticles(world, doorCenter.add(2, 0, i));
            }
        } else {
            // 垂直门框
            boolean nsWall = (facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH);

            for (int h = -1; h <= DOOR_HEIGHT; h++) {
                if (nsWall) {
                    spawnFrameParticles(world, doorCenter.add(-2, h, 0));
                    spawnFrameParticles(world, doorCenter.add(2, h, 0));
                } else {
                    spawnFrameParticles(world, doorCenter.add(0, h, -2));
                    spawnFrameParticles(world, doorCenter.add(0, h, 2));
                }
            }

            for (int w = -2; w <= 2; w++) {
                if (nsWall) {
                    spawnFrameParticles(world, doorCenter.add(w, -1, 0));
                    spawnFrameParticles(world, doorCenter.add(w, DOOR_HEIGHT, 0));
                } else {
                    spawnFrameParticles(world, doorCenter.add(0, -1, w));
                    spawnFrameParticles(world, doorCenter.add(0, DOOR_HEIGHT, w));
                }
            }
        }
    }

    // ==================== Tooltip / Rarity ====================

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.LIGHT_PURPLE + "✨ 维度钥匙");
        tooltip.add(TextFormatting.GRAY + "多功能维度工具");

        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "功能1 - 虚空传送：");
            tooltip.add(TextFormatting.WHITE + "▸ 对虚空水晶使用");
            tooltip.add(TextFormatting.WHITE + "▸ 开启临时传送门");
            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "功能2 - 墙壁开门：");
            tooltip.add(TextFormatting.WHITE + "▸ 对维度墙使用");
            tooltip.add(TextFormatting.WHITE + "▸ 创建3×4临时通道");
            tooltip.add("");
            tooltip.add(TextFormatting.GREEN + "▸ 效果持续30秒");
            tooltip.add(TextFormatting.RED + "▸ 仅限私人维度使用");
            tooltip.add("");
            tooltip.add(TextFormatting.AQUA + "⚡ 耐久度: 无限");
        } else {
            tooltip.add(TextFormatting.DARK_GRAY + "按住 " +
                    TextFormatting.YELLOW + "Shift" +
                    TextFormatting.DARK_GRAY + " 查看详情");
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }
}