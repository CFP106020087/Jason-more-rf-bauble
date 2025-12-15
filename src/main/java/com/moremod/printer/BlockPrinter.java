package com.moremod.printer;

import com.moremod.client.gui.GuiHandler;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.moremod;
import com.moremod.multiblock.MultiblockPrinter;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * 打印机核心方块
 *
 * 多方块结构的核心，负责打印物品
 */
public class BlockPrinter extends Block implements ITileEntityProvider {

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockPrinter() {
        super(Material.IRON);
        setHardness(5.0F);
        setResistance(10.0F);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("printer");
        setTranslationKey("printer");
        setDefaultState(blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(ACTIVE, false));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityPrinter) {
                TileEntityPrinter printer = (TileEntityPrinter) te;

                // 检查多方块结构
                if (!printer.isMultiblockFormed()) {
                    // 发送结构未形成的消息
                    player.sendMessage(new TextComponentString("\u00a7c" + "多方块结构未形成！"));
                    player.sendMessage(new TextComponentString("\u00a77" + MultiblockPrinter.getBuildGuide()));
                    return true;
                }

                player.openGui(moremod.instance, GuiHandler.PRINTER_GUI, world, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, net.minecraft.item.ItemStack stack) {
        world.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ACTIVE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byHorizontalIndex(meta & 3);
        boolean active = (meta & 4) != 0;
        return getDefaultState().withProperty(FACING, facing).withProperty(ACTIVE, active);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(ACTIVE)) {
            meta |= 4;
        }
        return meta;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityPrinter();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityPrinter) {
            ((TileEntityPrinter) te).dropItems();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    /**
     * 使用标准方块模型渲染 (OBJ)
     */
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    /**
     * 渲染层 - 使用 CUTOUT 以支持透明
     */
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    /**
     * 碰撞箱 - 完整方块
     */
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    /**
     * 设置激活状态（用于动画）
     */
    public static void setActive(World world, BlockPos pos, boolean active) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockPrinter) {
            world.setBlockState(pos, state.withProperty(ACTIVE, active), 3);
        }
    }
}