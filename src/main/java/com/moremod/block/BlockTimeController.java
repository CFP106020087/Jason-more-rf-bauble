package com.moremod.block;

import com.moremod.moremod;
import com.moremod.tile.TileEntityTimeController;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockTimeController extends Block {

    // 仅保留“是否激活”作为方块状态；速度、模式放在 TileEntity
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockTimeController() {
        super(Material.IRON);
        setRegistryName(new ResourceLocation(moremod.MODID, "time_controller"));
        setTranslationKey(moremod.MODID + ".time_controller");
        setCreativeTab(CreativeTabs.REDSTONE);
        setHardness(3.0F);
        setResistance(10.0F);
        setLightOpacity(0);

        setDefaultState(this.blockState.getBaseState().withProperty(ACTIVE, Boolean.FALSE));
    }

    // ===== TileEntity 接口（推荐做法）=====
    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityTimeController();
    }

    // 交互：潜行切换模式；普通点击切换开关
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityTimeController) {
            TileEntityTimeController controller = (TileEntityTimeController) te;

            if (player.isSneaking()) {
                controller.cycleMode();
                player.sendMessage(new TextComponentString("时间控制器模式: " + controller.getModeName()));
            } else {
                boolean newActive = !state.getValue(ACTIVE);
                world.setBlockState(pos, state.withProperty(ACTIVE, newActive), 3);
                controller.setActive(newActive);
                player.sendMessage(new TextComponentString("时间控制器: " + (newActive ? "激活" : "关闭")));
            }
            return true;
        }
        return false;
    }

    // 红石：强度=速度等级；有信号即激活，无信号即关闭
    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (world.isRemote) return;

        int power = world.getRedstonePowerFromNeighbors(pos);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityTimeController) {
            TileEntityTimeController controller = (TileEntityTimeController) te;
            controller.setSpeedLevel(power);

            boolean shouldActive = power > 0;
            if (shouldActive != state.getValue(ACTIVE)) {
                world.setBlockState(pos, state.withProperty(ACTIVE, shouldActive), 3);
                controller.setActive(shouldActive);
            }
        }
    }

    // 激活时发光
    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(ACTIVE) ? 15 : 0;
    }

    // 比较器输出当前速度等级
    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityTimeController) {
            return ((TileEntityTimeController) te).getSpeedLevel();
        }
        return 0;
    }

    // —— 方块状态/元数据 —— //
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
