package com.moremod.block;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.moremod;
import com.moremod.tile.TileEntityTimeController;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

public class BlockTimeController extends Block {

    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockTimeController() {
        super(Material.IRON);
        setRegistryName(new ResourceLocation(moremod.MODID, "time_controller"));
        setTranslationKey(moremod.MODID + ".time_controller");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setHardness(3.0F);
        setResistance(10.0F);

        setDefaultState(this.blockState.getBaseState().withProperty(ACTIVE, false));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityTimeController();
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            world.scheduleUpdate(pos, this, 10); // 检查状态
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityTimeController) {
                TileEntityTimeController controller = (TileEntityTimeController) te;
                boolean shouldBeActive = controller.isActive();

                if (shouldBeActive != state.getValue(ACTIVE)) {
                    world.setBlockState(pos, state.withProperty(ACTIVE, shouldBeActive), 3);
                }
            }
            world.scheduleUpdate(pos, this, 10); // 持续检查
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityTimeController) {
                TileEntityTimeController controller = (TileEntityTimeController) te;

                if (player.isSneaking()) {
                    // Shift+右键：调整速度
                    int newLevel = (controller.getSpeedLevel() + 1) % 16;
                    controller.setSpeedLevel(newLevel);
                } else {
                    // 右键：切换模式
                    controller.cycleMode();
                }

                player.sendMessage(new TextComponentString(controller.getStatusText()));
            }
        }
        return true;
    }

    // 激活时发光
    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(ACTIVE) ? 12 : 0;
    }

    // 比较器输出能量百分比
    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityTimeController) {
            TileEntityTimeController controller = (TileEntityTimeController) te;
            float ratio = (float) controller.getEnergyStored() / controller.getMaxEnergyStored();
            return (int) (ratio * 15);
        }
        return 0;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ACTIVE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(ACTIVE, meta == 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(ACTIVE) ? 1 : 0;
    }
}