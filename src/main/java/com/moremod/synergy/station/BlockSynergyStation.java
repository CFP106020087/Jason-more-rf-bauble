package com.moremod.synergy.station;

import com.moremod.client.gui.GuiHandler;
import com.moremod.moremod;
import com.moremod.moremodCreativeTab;
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

/**
 * Synergy 链结站方块
 *
 * 玩家可以在这个方块上配置模块链结。
 */
public class BlockSynergyStation extends Block implements ITileEntityProvider {

    public BlockSynergyStation() {
        super(Material.IRON);
        setTranslationKey("synergy_station");
        setRegistryName("synergy_station");
        setHardness(3.5F);
        setResistance(10.0F);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntitySynergyStation();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            // GUI ID 29 - 在 GuiHandler 中注册
            playerIn.openGui(moremod.instance, GuiHandler.SYNERGY_STATION_GUI,
                    worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        // 方块被破坏时不需要掉落物品，因为链结站不存储实体物品
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }
}
