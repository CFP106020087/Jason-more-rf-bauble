package com.moremod.block;

import com.moremod.client.gui.GuiHandler;
import com.moremod.moremod;
import com.moremod.tile.TileTradingStation;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** 🏪 村民交易機方塊（1.12.2）- 無方向性簡化版 */
public class BlockTradingStation extends Block implements ITileEntityProvider {

    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockTradingStation() {
        super(Material.IRON);
        setTranslationKey("trading_station");
        setRegistryName("trading_station");
        setHardness(3.5F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 1);

        // 默認狀態：只有 active
        this.setDefaultState(
                this.blockState.getBaseState()
                        .withProperty(ACTIVE, Boolean.FALSE)
        );
    }

    /* ---------------- 狀態容器 ---------------- */

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ACTIVE);  // 只有 ACTIVE
    }

    /* ---------------- meta ↔ state 映射 ---------------- */

    @Override
    public IBlockState getStateFromMeta(int meta) {
        boolean active = (meta & 1) != 0;  // bit0: active
        return this.getDefaultState().withProperty(ACTIVE, active);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(ACTIVE) ? 1 : 0;
    }

    /* ---------------- 互動 / GUI ---------------- */

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileTradingStation) {
                System.out.println("[BlockTradingStation] 玩家 " + player.getName() + " 右鍵點擊交易機");
                player.openGui(
                        moremod.instance,
                        GuiHandler.TRADING_STATION_GUI,
                        world, pos.getX(), pos.getY(), pos.getZ()
                );
            }
        }
        return true;
    }

    /* ---------------- TileEntity 相關 ---------------- */

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileTradingStation();
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        System.out.println("[BlockTradingStation] 創建 TileTradingStation");
        return new TileTradingStation();
    }

    @Override
    public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int id, int param) {
        super.eventReceived(state, worldIn, pos, id, param);
        TileEntity tileentity = worldIn.getTileEntity(pos);
        return tileentity != null && tileentity.receiveClientEvent(id, param);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileTradingStation) {
            // TODO: 掉落物品
        }
        super.breakBlock(world, pos, state);
    }
}