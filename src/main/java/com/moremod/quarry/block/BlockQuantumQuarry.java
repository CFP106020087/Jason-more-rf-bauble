package com.moremod.quarry.block;

import com.moremod.moremod;
import com.moremod.quarry.QuarryRegistry;
import com.moremod.quarry.tile.TileQuantumQuarry;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 量子采石场主方块
 */
public class BlockQuantumQuarry extends Block implements ITileEntityProvider {

    public BlockQuantumQuarry() {
        super(Material.IRON);
        setHardness(5.0F);
        setResistance(10.0F);
        setLightLevel(0.5F);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileQuantumQuarry();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) return true;

        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileQuantumQuarry) {
            TileQuantumQuarry quarry = (TileQuantumQuarry) te;

            // 检查结构是否完整
            if (!quarry.isStructureValid()) {
                playerIn.sendMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 結構不完整！需要在六面放置量子驅動器"
                ));
                return true;
            }

            // 打开 GUI
            playerIn.openGui(moremod.INSTANCE, QuarryRegistry.GUI_QUANTUM_QUARRY,
                    worldIn, pos.getX(), pos.getY(), pos.getZ());
        }

        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileQuantumQuarry) {
            // 掉落缓冲区中的物品
            TileQuantumQuarry quarry = (TileQuantumQuarry) te;
            // 可以选择掉落物品或保留
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos,
                                Block blockIn, BlockPos fromPos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileQuantumQuarry) {
            ((TileQuantumQuarry) te).updateRedstoneState();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn,
                               List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "從虛擬維度中採集方塊");
        tooltip.add(TextFormatting.GRAY + "需要在六面放置量子驅動器");
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "模式:");
        tooltip.add(TextFormatting.WHITE + "  採礦 - 根據生態系生成礦物");
        tooltip.add(TextFormatting.WHITE + "  怪物掉落 - 模擬擊殺怪物");
        tooltip.add(TextFormatting.WHITE + "  戰利品 - 生成寶藏物品");
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }
}
