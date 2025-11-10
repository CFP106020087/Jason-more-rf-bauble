package com.moremod.block;

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
import com.moremod.moremod;
import com.moremod.client.gui.GuiHandler;

public class BlockSwordUpgradeStation extends Block implements ITileEntityProvider {

    public BlockSwordUpgradeStation() {
        super(Material.IRON);
        setTranslationKey("sword_upgrade_station");
        setRegistryName("sword_upgrade_station");
        setHardness(3.5F);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new com.moremod.tile.TileEntitySwordUpgradeStation();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            playerIn.openGui(moremod.instance, GuiHandler.SWORD_UPGRADE_STATION_GUI,
                    worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
