package com.moremod.block;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.tile.TileEntityRitualCore;
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
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class BlockRitualCore extends BlockContainer {

    public BlockRitualCore() {
        super(Material.ROCK);
        setHardness(3.0F);
        setResistance(10.0F);
        setCreativeTab(moremodCreativeTab.moremod_TAB);

    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityRitualCore)) return false;

        if (world.isRemote) return true;

        TileEntityRitualCore core = (TileEntityRitualCore) te;
        IItemHandler handler = core.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
        if (handler == null) return false;

        ItemStack heldItem = player.getHeldItem(hand);

        // 优先取出输出槽
        ItemStack output = handler.getStackInSlot(1);
        if (!output.isEmpty()) {
            ItemStack extracted = handler.extractItem(1, 64, false);
            if (!player.inventory.addItemStackToInventory(extracted)) {
                handler.insertItem(1, extracted, false);
            }
            core.markDirty();
            world.notifyBlockUpdate(pos, state, state, 3);
            return true;
        }

        // 放入输入槽
        if (!heldItem.isEmpty()) {
            ItemStack current = handler.getStackInSlot(0);
            if (current.isEmpty()) {
                ItemStack toInsert = heldItem.copy();
                toInsert.setCount(1);
                ItemStack remainder = handler.insertItem(0, toInsert, false);
                if (remainder.isEmpty()) {
                    heldItem.shrink(1);
                    core.markDirty();
                    world.notifyBlockUpdate(pos, state, state, 3);
                    return true;
                }
            }
        }

        // 空手取出输入槽
        if (heldItem.isEmpty()) {
            ItemStack input = handler.getStackInSlot(0);
            if (!input.isEmpty()) {
                ItemStack extracted = handler.extractItem(0, 64, false);
                player.setHeldItem(hand, extracted);
                core.markDirty();
                world.notifyBlockUpdate(pos, state, state, 3);
                return true;
            }
        }

        return false;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityRitualCore();
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