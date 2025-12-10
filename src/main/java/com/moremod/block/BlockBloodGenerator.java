package com.moremod.block;

import com.moremod.client.gui.GuiHandler;
import com.moremod.moremod;
import com.moremod.tile.TileEntityBloodGenerator;
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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 血液发电机方块
 * 将沾血的武器放入，提取血液能量转换为RF
 */
public class BlockBloodGenerator extends Block implements ITileEntityProvider {

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockBloodGenerator() {
        super(Material.IRON);
        setTranslationKey("moremod.blood_generator");
        setRegistryName("blood_generator");
        setCreativeTab(CreativeTabs.REDSTONE);
        setHardness(5.0F);
        setResistance(15.0F);
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
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        return getDefaultState()
            .withProperty(FACING, placer.getHorizontalFacing().getOpposite())
            .withProperty(ACTIVE, false);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityBloodGenerator) {
            playerIn.openGui(moremod.instance, GuiHandler.BLOOD_GENERATOR_GUI,
                    worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityBloodGenerator) {
            IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    InventoryHelper.spawnItemStack(worldIn,
                        pos.getX(), pos.getY(), pos.getZ(),
                        handler.getStackInSlot(i));
                }
            }
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityBloodGenerator();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(ACTIVE) ? 9 : 0;
    }

    /**
     * 设置激活状态
     */
    public static void setActiveState(World world, BlockPos pos, boolean active) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockBloodGenerator) {
            if (state.getValue(ACTIVE) != active) {
                world.setBlockState(pos, state.withProperty(ACTIVE, active), 3);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        if (stateIn.getValue(ACTIVE)) {
            // 活跃时产生红色粒子效果
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5;

            // 血液滴落效果
            if (rand.nextInt(3) == 0) {
                worldIn.spawnParticle(EnumParticleTypes.REDSTONE,
                    x + (rand.nextDouble() - 0.5) * 0.5,
                    y,
                    z + (rand.nextDouble() - 0.5) * 0.5,
                    0.8, 0.0, 0.0); // 红色粒子
            }

            // 烟雾效果
            if (rand.nextInt(5) == 0) {
                worldIn.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    x, y + 0.2, z,
                    0, 0.05, 0);
            }
        }
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }
}
