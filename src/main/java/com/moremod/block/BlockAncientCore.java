// BlockAncientCore.java
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

public class BlockAncientCore extends Block {
    public BlockAncientCore() {
        super(Material.IRON);
        setRegistryName("ancient_core_block");
        setTranslationKey("ancient_core_block");
        setHardness(5.0F);
        setResistance(10.0F);
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        setLightLevel(0.7F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        for (int i = 0; i < 3; i++) {
            double d0 = pos.getX() + 0.5D + (rand.nextDouble() - 0.5D) * 0.5D;
            double d1 = pos.getY() + 0.5D + (rand.nextDouble() - 0.5D) * 0.5D;
            double d2 = pos.getZ() + 0.5D + (rand.nextDouble() - 0.5D) * 0.5D;
            worldIn.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
        }
    }
}