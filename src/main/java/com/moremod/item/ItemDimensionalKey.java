package com.moremod.item;

import com.moremod.block.BlockUnbreakableBarrier;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.dimension.PersonalDimensionManager;
import com.moremod.dungeon.portal.PortalManager;  // 改用新的 PortalManager
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
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ItemDimensionalKey extends Item {

    // 门的大小
    private static final int DOOR_WIDTH = 3;
    private static final int DOOR_HEIGHT = 4;
    private static final int DOOR_DURATION_TICKS = 600; // 30秒

    public ItemDimensionalKey() {
        setRegistryName("dimensional_key");
        setTranslationKey("dimensional_key");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
        setMaxDamage(100);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
                                      EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {

        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        ItemStack stack = player.getHeldItem(hand);
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // 只在私人维度使用
        if (world.provider.getDimension() != PersonalDimensionManager.PERSONAL_DIM_ID) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "维度钥匙只能在私人维度使用"
            ), true);
            return EnumActionResult.FAIL;
        }

        // 检查是否是虚空水晶
        if (block instanceof BlockUnbreakableBarrier) {
            BlockUnbreakableBarrier barrier = (BlockUnbreakableBarrier) block;

            if (barrier.getType() == BlockUnbreakableBarrier.BarrierType.VOID_CRYSTAL) {
                // 处理虚空水晶 - 创建传送门
                return handleVoidCrystal(player, world, pos, facing, stack);
            } else {
                // 处理其他屏障 - 打开临时门
                return handleDimensionWall(player, world, pos, facing, stack);
            }
        }

        // 不是屏障方块
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.YELLOW + "请对准虚空水晶或维度墙使用"
        ), true);

        return EnumActionResult.FAIL;
    }

    /**
     * 处理虚空水晶 - 创建传送门
     */
    private EnumActionResult handleVoidCrystal(EntityPlayer player, World world, BlockPos pos,
                                               EnumFacing facing, ItemStack stack) {
        // 使用新的 PortalManager 获取传送目标
        PortalManager.LocationData locationData = PortalManager.getDestination(world, pos);

        if (locationData == null) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "此虚空水晶尚未链接到其他位置"
            ), true);
            return EnumActionResult.FAIL;
        }

        // 从 LocationData 中获取目标位置
        BlockPos destination = locationData.pos;

        // 如果需要跨维度传送，可以使用 locationData.dimension
        if (locationData.dimension != world.provider.getDimension()) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GOLD + "目标位于维度 " + locationData.dimension
            ), true);
        }

        // 检查是否已有传送门
        AxisAlignedBB searchArea = new AxisAlignedBB(
                pos.getX() - 3, pos.getY() - 1, pos.getZ() - 3,
                pos.getX() + 3, pos.getY() + 3, pos.getZ() + 3
        );

        List<EntityVoidPortal> existingPortals = world.getEntitiesWithinAABB(
                EntityVoidPortal.class, searchArea
        );

        if (!existingPortals.isEmpty()) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "此处已有传送门开启"
            ), true);
            return EnumActionResult.SUCCESS;
        }

        // 创建传送门实体（2x1大小）
        BlockPos portalPos = pos.offset(facing);
        EntityVoidPortal portal = new EntityVoidPortal(world, portalPos, destination, pos);
        world.spawnEntity(portal);

        // 消耗耐久
        stack.damageItem(1, player);

        // 音效
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
    private EnumActionResult handleDimensionWall(EntityPlayer player, World world, BlockPos pos,
                                                 EnumFacing facing, ItemStack stack) {
        // 获取玩家的私人空间
        UUID playerId = player.getUniqueID();
        PersonalDimensionManager.PersonalSpace space = PersonalDimensionManager.getPlayerSpace(playerId);

        if (space == null) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "你还没有私人空间"
            ), true);
            return EnumActionResult.FAIL;
        }

        // 检查是否在墙壁位置
        if (!space.isWall(pos)) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "只能在维度墙壁位置使用"
            ), true);
            return EnumActionResult.FAIL;
        }

        // 创建临时门
        if (createTemporaryDoor(world, pos, facing, player, space)) {
            // 消耗耐久
            stack.damageItem(1, player);

            // 播放音效
            world.playSound(null, pos, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);

            // 安排墙壁恢复任务
            PersonalDimensionManager.scheduleWallRestore(playerId, DOOR_DURATION_TICKS);

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
                                        EntityPlayer player, PersonalDimensionManager.PersonalSpace space) {
        BlockPos doorCenter = findDoorPosition(world, clickedPos, facing, space);
        if (doorCenter == null) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "无法确定门的位置"
            ), true);
            return false;
        }

        boolean isYAxis = (facing == EnumFacing.UP || facing == EnumFacing.DOWN);
        int doorCount = 0;

        if (isYAxis) {
            // 水平门（天花板或地板）
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos doorPos = doorCenter.add(x, 0, z);

                    if (space.isWall(doorPos)) {
                        IBlockState currentState = world.getBlockState(doorPos);
                        if (isWallBlock(currentState)) {
                            world.setBlockToAir(doorPos);
                            world.markBlockRangeForRenderUpdate(doorPos, doorPos);
                            world.notifyBlockUpdate(doorPos, currentState, Blocks.AIR.getDefaultState(), 3);

                            spawnDoorParticles(world, doorPos);
                            doorCount++;
                        }
                    }
                }
            }
        } else {
            // 垂直门（墙壁）
            for (int w = -1; w <= 1; w++) {
                for (int h = 0; h < DOOR_HEIGHT; h++) {
                    BlockPos doorPos;

                    if (facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH) {
                        doorPos = doorCenter.add(w, h, 0);
                    } else {
                        doorPos = doorCenter.add(0, h, w);
                    }

                    if (space.isWall(doorPos)) {
                        IBlockState currentState = world.getBlockState(doorPos);
                        if (isWallBlock(currentState)) {
                            world.setBlockToAir(doorPos);
                            world.markBlockRangeForRenderUpdate(doorPos, doorPos);
                            world.notifyBlockUpdate(doorPos, currentState, Blocks.AIR.getDefaultState(), 3);

                            spawnDoorParticles(world, doorPos);
                            doorCount++;
                        }
                    }
                }
            }
        }

        if (doorCount == 0) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "无法在此位置创建门"
            ), true);
            return false;
        }

        addDoorFrameEffects(world, doorCenter, facing);
        return true;
    }

    /**
     * 检查是否是墙壁方块
     */
    private boolean isWallBlock(IBlockState state) {
        Block block = state.getBlock();

        if (block instanceof BlockUnbreakableBarrier) {
            return true;
        }

        if (block.getRegistryName() != null) {
            String blockName = block.getRegistryName().toString();
            if (blockName.contains("unbreakable_barrier")) {
                return true;
            }
        }

        if (block == Blocks.BEDROCK) {
            return true;
        }

        return false;
    }

    /**
     * 找到合适的门位置
     */
    private BlockPos findDoorPosition(World world, BlockPos clickedPos, EnumFacing facing,
                                      PersonalDimensionManager.PersonalSpace space) {
        if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) {
            return clickedPos;
        }

        int floorY = space.innerMinPos.getY() + 1;
        BlockPos doorPos = new BlockPos(clickedPos.getX(), floorY, clickedPos.getZ());

        return doorPos;
    }

    /**
     * 添加门框效果
     */
    private void addDoorFrameEffects(World world, BlockPos doorCenter, EnumFacing facing) {
        boolean isYAxis = (facing == EnumFacing.UP || facing == EnumFacing.DOWN);

        if (isYAxis) {
            for (int i = -2; i <= 2; i++) {
                spawnFrameParticles(world, doorCenter.add(i, 0, -2));
                spawnFrameParticles(world, doorCenter.add(i, 0, 2));
                spawnFrameParticles(world, doorCenter.add(-2, 0, i));
                spawnFrameParticles(world, doorCenter.add(2, 0, i));
            }
        } else {
            boolean isNSWall = (facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH);

            for (int h = -1; h <= DOOR_HEIGHT; h++) {
                if (isNSWall) {
                    spawnFrameParticles(world, doorCenter.add(-2, h, 0));
                    spawnFrameParticles(world, doorCenter.add(2, h, 0));
                } else {
                    spawnFrameParticles(world, doorCenter.add(0, h, -2));
                    spawnFrameParticles(world, doorCenter.add(0, h, 2));
                }
            }

            for (int w = -2; w <= 2; w++) {
                if (isNSWall) {
                    spawnFrameParticles(world, doorCenter.add(w, -1, 0));
                    spawnFrameParticles(world, doorCenter.add(w, DOOR_HEIGHT, 0));
                } else {
                    spawnFrameParticles(world, doorCenter.add(0, -1, w));
                    spawnFrameParticles(world, doorCenter.add(0, DOOR_HEIGHT, w));
                }
            }
        }
    }

    private void spawnDoorParticles(World world, BlockPos pos) {
        for (int i = 0; i < 5; i++) {
            double x = pos.getX() + 0.5 + (world.rand.nextDouble() - 0.5);
            double y = pos.getY() + 0.5 + (world.rand.nextDouble() - 0.5);
            double z = pos.getZ() + 0.5 + (world.rand.nextDouble() - 0.5);

            world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, 0, 0, 0);
        }
    }

    private void spawnFrameParticles(World world, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        world.spawnParticle(EnumParticleTypes.END_ROD, x, y, z, 0, 0.05, 0);
        world.spawnParticle(EnumParticleTypes.SPELL_INSTANT, x, y, z, 0, 0, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
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
            tooltip.add(TextFormatting.AQUA + "⚡ 耐久度: " +
                    (stack.getMaxDamage() - stack.getItemDamage()) + "/" + stack.getMaxDamage());
        } else {
            tooltip.add(TextFormatting.DARK_GRAY + "按住 " + TextFormatting.YELLOW + "Shift" +
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