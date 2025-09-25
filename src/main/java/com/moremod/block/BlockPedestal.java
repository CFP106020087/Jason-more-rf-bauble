package com.moremod.block;

import com.moremod.tile.TileEntityPedestal;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class BlockPedestal extends BlockContainer {

    public BlockPedestal() {
        super(Material.ROCK);
        setHardness(2.0F);
        setResistance(6.0F);

    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityPedestal)) return false;

        if (world.isRemote) return true;

        TileEntityPedestal pedestal = (TileEntityPedestal) te;

        // 创造模式下，shift+右键充满能量
        if (player.isCreative() && player.isSneaking()) {
            pedestal.getEnergy().receiveEnergy(50000, false);
            player.sendMessage(new TextComponentString("Pedestal charged to " +
                    pedestal.getEnergy().getEnergyStored() + " FE"));
            return true;
        }

        IItemHandler handler = pedestal.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
        if (handler == null) return false;

        ItemStack heldItem = player.getHeldItem(hand);
        ItemStack pedestalItem = handler.getStackInSlot(0);

        if (!pedestalItem.isEmpty()) {
            ItemStack extracted = handler.extractItem(0, pedestalItem.getCount(), false);
            if (!player.inventory.addItemStackToInventory(extracted)) {
                handler.insertItem(0, extracted, false);
            }
            pedestal.markDirty();
            world.notifyBlockUpdate(pos, state, state, 3);
            return true;
        }

        if (!heldItem.isEmpty()) {
            ItemStack toInsert = heldItem.copy();
            toInsert.setCount(1);
            ItemStack remainder = handler.insertItem(0, toInsert, false);
            if (remainder.isEmpty()) {
                heldItem.shrink(1);
                pedestal.markDirty();
                world.notifyBlockUpdate(pos, state, state, 3);
                return true;
            }
        }

        return false;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityPedestal();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
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

    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}