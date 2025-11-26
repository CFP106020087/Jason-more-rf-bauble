package com.moremod.block;

import com.moremod.moremod;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.tile.TileEntityTransferStation;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * 转移台方块
 */
public class BlockTransferStation extends Block implements ITileEntityProvider {

    public BlockTransferStation() {
        super(Material.IRON);

        setTranslationKey("transfer_station");
        setRegistryName("transfer_station");
        setCreativeTab(moremodCreativeTab.moremod_TAB);

        setHardness(3.0F);
        setResistance(15.0F);
        setHarvestLevel("pickaxe", 2); // 需要铁镐

        setLightLevel(0.5F); // 发出微光
    }

    // ==========================================
    // TileEntity
    // ==========================================

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityTransferStation();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    // ==========================================
    // 交互
    // ==========================================
    public static final int GUI_ID = 28;
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                   EntityPlayer playerIn, EnumHand hand,
                                   EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityTransferStation) {
            // 打开GUI（需要在GuiHandler中注册）
            playerIn.openGui(
                moremod.instance,
                GUI_ID, // GUI ID
                worldIn,
                pos.getX(),
                pos.getY(),
                pos.getZ()
            );
        }
        
        return true;
    }
    
    // ==========================================
    // 破坏时掉落物品
    // ==========================================
    
    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        
        if (te instanceof TileEntityTransferStation) {
            TileEntityTransferStation tile = (TileEntityTransferStation) te;
            
            // 掉落所有物品
            for (int i = 0; i < tile.getInventory().getSlots(); i++) {
                ItemStack stack = tile.getInventory().getStackInSlot(i);
                if (!stack.isEmpty()) {
                    InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
        
        super.breakBlock(worldIn, pos, state);
    }
    
    // ==========================================
    // 渲染
    // ==========================================
    
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }
    
    @SideOnly(Side.CLIENT)
    
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
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