// BlockWisdomFountainCore.java
package com.moremod.block;

import com.moremod.tile.TileEntityWisdomFountain;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockWisdomFountainCore extends Block implements ITileEntityProvider {

    public BlockWisdomFountainCore() {
        super(Material.ROCK);
        setRegistryName("wisdom_fountain_core");
        setTranslationKey("wisdom_fountain_core");
        setHardness(3.0F);
        setResistance(10.0F);
        setCreativeTab(CreativeTabs.DECORATIONS);
        setLightLevel(0.5F);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityWisdomFountain) {
                TileEntityWisdomFountain fountain = (TileEntityWisdomFountain) te;
                if (fountain.isFormed()) {
                    playerIn.sendMessage(new TextComponentString("§a神碑智慧之泉已激活！"));
                    playerIn.sendMessage(new TextComponentString("§e手持附魔书对附近村民Shift+右键进行转化"));
                } else {
                    playerIn.sendMessage(new TextComponentString("§c结构不完整，请检查多方块结构"));
                    playerIn.sendMessage(new TextComponentString("§7提示：使用守护者方块和符文虚空石方块构建"));
                }
            }
        }
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityWisdomFountain();
    }
}