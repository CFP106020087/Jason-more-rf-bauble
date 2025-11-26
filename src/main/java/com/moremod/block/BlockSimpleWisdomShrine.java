package com.moremod.block;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.tile.TileEntitySimpleWisdomShrine;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class BlockSimpleWisdomShrine extends BlockContainer {

    public BlockSimpleWisdomShrine() {
        super(Material.ROCK);
        setTranslationKey("simple_wisdom_shrine");
        setRegistryName("simple_wisdom_shrine");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setHardness(3.0f);
        setResistance(10.0f);
        setLightLevel(0.5f);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntitySimpleWisdomShrine();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntitySimpleWisdomShrine) {
            TileEntitySimpleWisdomShrine shrine = (TileEntitySimpleWisdomShrine) te;

            if (shrine.isFormed()) {
                player.sendMessage(new TextComponentString("§a✓ 简易智慧之泉已激活"));
                player.sendMessage(new TextComponentString("§7范围: " + shrine.getRange() + "格"));
                player.sendMessage(new TextComponentString("§7效果: 解锁交易 + 加速成长"));
            } else {
                player.sendMessage(new TextComponentString("§c✗ 结构未完成"));
                player.sendMessage(new TextComponentString("§7需要搭建3x3x3结构"));
            }
        }

        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntitySimpleWisdomShrine) {
            ((TileEntitySimpleWisdomShrine) te).onBroken();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
                                net.minecraft.block.Block blockIn, BlockPos fromPos) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntitySimpleWisdomShrine) {
                ((TileEntitySimpleWisdomShrine) te).checkStructure();
            }
        }
    }
}
