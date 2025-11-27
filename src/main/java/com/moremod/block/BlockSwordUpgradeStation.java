package com.moremod.block;

import com.moremod.tile.TileEntitySwordUpgradeStation;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.moremod.moremod;
import com.moremod.client.gui.GuiHandler;
import com.moremod.tile.TileEntitySwordUpgradeStationMaterial;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class BlockSwordUpgradeStation extends Block implements ITileEntityProvider {

    public BlockSwordUpgradeStation() {
        super(Material.IRON);
        setTranslationKey("sword_upgrade_station");
        setRegistryName("sword_upgrade_station");
        setHardness(3.5F);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntitySwordUpgradeStation();
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

    /**
     * ✅ 修复：方块破坏时只掉落输入槽物品，避免预览槽物品复制
     */
}