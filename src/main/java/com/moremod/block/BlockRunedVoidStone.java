// BlockRunedVoidStone.java
package com.moremod.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public class BlockRunedVoidStone extends Block {
    public BlockRunedVoidStone() {
        super(Material.ROCK);
        setRegistryName("runed_void_stone_block");
        setTranslationKey("runed_void_stone_block");
        setHardness(50.0F);
        setResistance(2000.0F);
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        setLightLevel(0.3F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        if (rand.nextInt(5) == 0) {
            double d0 = pos.getX() + rand.nextDouble();
            double d1 = pos.getY() + 1.1D;
            double d2 = pos.getZ() + rand.nextDouble();
            worldIn.spawnParticle(EnumParticleTypes.PORTAL, d0, d1, d2, 0.0D, 0.0D, 0.0D);
        }
    }
}