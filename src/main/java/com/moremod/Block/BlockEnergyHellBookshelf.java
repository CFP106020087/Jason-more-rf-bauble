// BlockEnergyHellBookshelf.java
package com.moremod.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class BlockEnergyHellBookshelf extends Block {

    public static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, 15);

    public BlockEnergyHellBookshelf() {
        super(Material.ROCK, MapColor.BLACK);
        setHardness(2.0F);
        setResistance(10.0F);
        setSoundType(SoundType.STONE);
        setTranslationKey("energy_hell_bookshelf");
        setRegistryName("energy_hell_bookshelf");
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        setDefaultState(getDefaultState().withProperty(LEVEL, 0));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityEnergyHellBookshelf();
    }

    @Override
    public float getEnchantPowerBonus(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityEnergyHellBookshelf) {
            return ((TileEntityEnergyHellBookshelf) tile).getCurrentPower();
        }
        return 0;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("info.moremod.energy_bookshelf.tooltip"));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LEVEL);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(LEVEL, meta);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LEVEL);
    }
}
