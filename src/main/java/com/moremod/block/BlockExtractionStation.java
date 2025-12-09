package com.moremod.block;

import com.moremod.moremod;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.moremod;
import com.moremod.tile.TileEntityExtractionStation;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockExtractionStation extends Block implements ITileEntityProvider {
    
    public static final int GUI_ID = 26;
    
    public BlockExtractionStation() {
        super(Material.IRON);
        setTranslationKey("extraction_station");
        setRegistryName("extraction_station");
        setHardness(3.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 1);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }
    
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                   EntityPlayer player, EnumHand hand,
                                   EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(moremod.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
    
    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityExtractionStation();
    }
    
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityExtractionStation) {
            TileEntityExtractionStation tile = (TileEntityExtractionStation) te;
            
            if (!tile.getInputStack().isEmpty()) {
                spawnAsEntity(world, pos, tile.getInputStack());
            }
            if (!tile.getOutputStack(0).isEmpty()) {
                spawnAsEntity(world, pos, tile.getOutputStack(0));
            }
        }
        super.breakBlock(world, pos, state);
    }
    
    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }
}