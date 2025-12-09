package com.moremod.block.energy;

import com.moremod.Moremod;
import com.moremod.client.gui.GuiHandler;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.tile.TileEntityChargingStation;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 充能站方塊
 *
 * 功能：
 * - 可放入物品充電
 * - 玩家站在上面也可充電
 * - 極大容量、無限快充電速度
 */
public class BlockChargingStation extends Block implements ITileEntityProvider {

    private static final AxisAlignedBB BOUNDING_BOX = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.875, 1.0);

    public BlockChargingStation() {
        super(Material.IRON);
        setHardness(5.0F);
        setResistance(10.0F);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("charging_station");
        setTranslationKey("charging_station");
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityChargingStation) {
                player.openGui(Moremod.instance, GuiHandler.CHARGING_STATION_GUI, world, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BOUNDING_BOX;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityChargingStation();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityChargingStation) {
            ((TileEntityChargingStation) te).dropItems();
        }
        super.breakBlock(world, pos, state);
    }
}
