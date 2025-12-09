package com.moremod.block;

import com.moremod.tile.TileEntityFishingNet;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 渔网方块 - 放置在水面上自动钓鱼
 * 参考Actually Additions的设计
 */
public class BlockFishingNet extends Block implements ITileEntityProvider {

    // 薄的碰撞箱，像地毯一样
    private static final AxisAlignedBB AABB = new AxisAlignedBB(0, 0, 0, 1, 0.0625, 1);

    public BlockFishingNet() {
        super(Material.WOOD);
        setRegistryName("moremod", "fishing_net");
        setTranslationKey("moremod.fishing_net");
        setHarvestLevel("axe", 0);
        setHardness(0.5F);
        setResistance(3.0F);
        setSoundType(SoundType.WOOD);
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.DECORATIONS);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return AABB;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityFishingNet();
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                     EntityPlayer playerIn, EnumHand hand,
                                     EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityFishingNet) {
                TileEntityFishingNet fishingNet = (TileEntityFishingNet) te;

                // 显示状态信息
                boolean hasWater = fishingNet.hasWaterBelow();
                int itemCount = 0;

                IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                if (handler != null) {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            itemCount += stack.getCount();
                        }
                    }
                }

                if (hasWater) {
                    playerIn.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "渔网状态: " + TextFormatting.GREEN + "工作中" +
                        TextFormatting.GRAY + " | 存储物品: " + TextFormatting.YELLOW + itemCount
                    ));
                } else {
                    playerIn.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "渔网状态: " + TextFormatting.RED + "需要水源！" +
                        TextFormatting.GRAY + " (在渔网下方放置水)"
                    ));
                }
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityFishingNet) {
            TileEntityFishingNet fishingNet = (TileEntityFishingNet) te;
            fishingNet.dropInventory();
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "放置在水面上自动钓鱼");
        tooltip.add(TextFormatting.AQUA + "需要下方有水源方块");
        tooltip.add(TextFormatting.YELLOW + "可用漏斗抽取物品");
    }

    /**
     * 检查是否可以放置在此位置（最好下方是水）
     */
    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        // 可以放置在任何位置，但只有下方是水才会工作
        return super.canPlaceBlockAt(worldIn, pos);
    }
}
