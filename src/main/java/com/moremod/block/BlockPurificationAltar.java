package com.moremod.block;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.moremod;
import com.moremod.tile.TileEntityPurificationAltar;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 提纯祭坛 - 方块类
 */
public class BlockPurificationAltar extends Block implements ITileEntityProvider {
    
    public static final String NAME = "purification_altar";
    public static final int GUI_ID = 27;
    public BlockPurificationAltar() {
        super(Material.ROCK);
        setTranslationKey( "moremod" + NAME);
        setRegistryName(NAME);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setHardness(5.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 2); // 铁镐
    }
    
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, 
                                   EntityPlayer playerIn, EnumHand hand, 
                                   EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityPurificationAltar) {
                // 打开GUI
                playerIn.openGui(moremod.instance,
                  GUI_ID,  worldIn, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }
    
    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityPurificationAltar) {
            TileEntityPurificationAltar altar = (TileEntityPurificationAltar) te;
            
            // 掉落所有物品
            for (int i = 0; i < 6; i++) {
                net.minecraft.item.ItemStack stack = altar.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    net.minecraft.inventory.InventoryHelper.spawnItemStack(
                        worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
        
        super.breakBlock(worldIn, pos, state);
    }
    
    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }
    
    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityPurificationAltar();
    }
    
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }
    
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }
    
    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }
}