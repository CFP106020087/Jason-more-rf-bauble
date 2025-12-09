package com.moremod.block;

import com.moremod.client.gui.GuiHandler;
import com.moremod.moremod;
import com.moremod.tile.TileEntityFakePlayerActivator;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * 假玩家激活器 - 使用假玩家核心模拟玩家操作
 * 功能：
 * - 自动右键点击前方方块
 * - 自动使用物品（骨粉、种子等）
 * - 模拟玩家交互
 */
public class BlockFakePlayerActivator extends Block implements ITileEntityProvider {

    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockFakePlayerActivator() {
        super(Material.IRON);
        setTranslationKey("moremod.fake_player_activator");
        setRegistryName("fake_player_activator");
        setCreativeTab(CreativeTabs.REDSTONE);
        setHardness(3.5F);
        setResistance(10.0F);
        setDefaultState(blockState.getBaseState()
            .withProperty(FACING, EnumFacing.NORTH)
            .withProperty(ACTIVE, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ACTIVE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byIndex(meta & 7);
        boolean active = (meta & 8) != 0;
        return getDefaultState().withProperty(FACING, facing).withProperty(ACTIVE, active);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getIndex();
        if (state.getValue(ACTIVE)) {
            meta |= 8;
        }
        return meta;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        // 面向玩家看的方向
        EnumFacing playerFacing = placer.getHorizontalFacing().getOpposite();
        // 如果玩家看上/下，则使用上下方向
        if (placer.rotationPitch > 45) {
            playerFacing = EnumFacing.UP;
        } else if (placer.rotationPitch < -45) {
            playerFacing = EnumFacing.DOWN;
        }
        return getDefaultState().withProperty(FACING, playerFacing).withProperty(ACTIVE, false);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }

        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityFakePlayerActivator)) {
            return false;
        }

        // 打开 GUI
        playerIn.openGui(moremod.INSTANCE, GuiHandler.FAKE_PLAYER_ACTIVATOR_GUI,
                worldIn, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityFakePlayerActivator) {
            TileEntityFakePlayerActivator activator = (TileEntityFakePlayerActivator) te;
            // 掉落所有物品
            IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityFakePlayerActivator();
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(ACTIVE) ? 7 : 0;
    }

    /**
     * 设置激活状态
     */
    public static void setActiveState(World world, BlockPos pos, boolean active) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockFakePlayerActivator) {
            if (state.getValue(ACTIVE) != active) {
                world.setBlockState(pos, state.withProperty(ACTIVE, active), 3);
            }
        }
    }
}
