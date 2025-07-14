// BlockInfiniteEnergy.java
package com.moremod.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import javax.annotation.Nullable;

public class BlockInfiniteEnergy extends Block implements ITileEntityProvider {

    public BlockInfiniteEnergy() {
        super(Material.IRON);
        setTranslationKey("infinite_energy_block");
        setRegistryName("infinite_energy_block");
        setHardness(2.0F);
        setResistance(20.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityInfiniteEnergy();
    }
}
