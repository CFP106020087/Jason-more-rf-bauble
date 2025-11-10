// BlockGuardianStone.java
package com.moremod.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

public class BlockGuardianStone extends Block {
    public BlockGuardianStone() {
        super(Material.ROCK);
        setRegistryName("guardian_stone_block");
        setTranslationKey("guardian_stone_block");
        setHardness(3.0F);
        setResistance(10.0F);
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }
}