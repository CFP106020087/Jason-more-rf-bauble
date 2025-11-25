package com.moremod.block;

import com.moremod.moremod;
import com.moremod.client.gui.GuiHandler;
import com.moremod.tile.TileEntitySwordUpgradeStationMaterial;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class BlockSwordUpgradeStationMaterial extends Block implements ITileEntityProvider {

    public static final PropertyDirection FACING = net.minecraft.block.BlockHorizontal.FACING;

    public BlockSwordUpgradeStationMaterial() {
        super(Material.ANVIL);
        setRegistryName("sword_upgrade_station_material");
        setTranslationKey("sword_upgrade_station_material");
        setHardness(5.0F);
        setResistance(2000.0F);
        setHarvestLevel("pickaxe", 1);
        setSoundType(SoundType.ANVIL);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) { return new TileEntitySwordUpgradeStationMaterial(); }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) return true;
        playerIn.openGui(moremod.instance, GuiHandler.SWORD_UPGRADE_STATION_MATERIAL_GUI,
                worldIn, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);

        if (te instanceof TileEntitySwordUpgradeStationMaterial) {
            TileEntitySwordUpgradeStationMaterial tile = (TileEntitySwordUpgradeStationMaterial) te;
            IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

            if (handler != null) {
                // 只掉落输入槽（SLOT_BASE 和 SLOT_MAT），跳过输出预览槽（SLOT_OUT）
                for (int i = 0; i < 2; i++) { // 只遍历前两个槽位
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
        }

        super.breakBlock(worldIn, pos, state);
    }


    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        worldIn.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
    }

    @Override public IBlockState getStateFromMeta(int meta) { return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)
    ); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getHorizontalIndex(); }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
}