package com.moremod.block;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.moremod;
import com.moremod.tile.TileCreativeWirelessTransmitter;
import com.raoulvdberge.refinedstorage.block.BlockCable;
import com.raoulvdberge.refinedstorage.block.BlockNode;
import com.raoulvdberge.refinedstorage.block.info.BlockInfoBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class BlockCreativeWirelessTransmitter extends BlockNode {

    private static final AxisAlignedBB AABB = new AxisAlignedBB(
            0.4D, 0.0D, 0.4D,
            0.6D, 0.6D, 0.6D
    );

    public BlockCreativeWirelessTransmitter() {
        super(BlockInfoBuilder
                .forMod(moremod.INSTANCE, moremod.MODID, "creative_wireless_transmitter")
                .tileEntity(TileCreativeWirelessTransmitter::new)
                .create());
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setHardness(1.0F);
        setResistance(5.0F);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing side,
                                    float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileCreativeWirelessTransmitter tile = (TileCreativeWirelessTransmitter) world.getTileEntity(pos);
            if (tile != null && tile.getNode() != null) {
                boolean connected = tile.getNode().getNetwork() != null;
                boolean active = tile.getNode().canUpdate();
                String msg =
                        TextFormatting.GOLD + "[Creative Wireless Transmitter]\n" +
                                TextFormatting.GRAY + "Network: " +
                                (connected ? TextFormatting.GREEN + "✔ Connected" : TextFormatting.RED + "✖ Disconnected") + "\n" +
                                TextFormatting.GRAY + "Status: " +
                                (active ? TextFormatting.GREEN + "✔ Active" : TextFormatting.RED + "✖ Inactive") + "\n" +
                                TextFormatting.GRAY + "Range: " + TextFormatting.AQUA + "∞ " + TextFormatting.GRAY + "(Infinite)";
                player.sendMessage(new TextComponentString(msg));
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
                                Block blockIn, BlockPos fromPos) {
        // ✅ 先让 RS 的 BlockNode 处理图变更
        super.neighborChanged(state, world, pos, blockIn, fromPos);

        if (!canPlaceBlockAt(world, pos) && world.getBlockState(pos).getBlock() == this) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return AABB;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) { return false; }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) { return false; }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        BlockPos down = pos.down();
        return world.isBlockLoaded(down) && world.getBlockState(down).getBlock() instanceof BlockCable;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return side == EnumFacing.UP && canPlaceBlockAt(world, pos); // 只允许从上方放置
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }

    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) { return 0; }

    @Override
    public boolean hasConnectedState() { return true; }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        String cableName = I18n.hasKey("tile.refinedstorage:cable.name")
                ? I18n.format("tile.refinedstorage:cable.name")
                : "Cable";
        tooltip.add(TextFormatting.GRAY + "Place on top of " + TextFormatting.WHITE + cableName + TextFormatting.GRAY + ".");
        tooltip.add(TextFormatting.AQUA + "Infinite range; cross-dimensional enabled.");
    }
}
