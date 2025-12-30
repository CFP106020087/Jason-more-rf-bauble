package com.moremod.block.energy;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.tile.TileEntityEnergyLink;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 能量链接器方块
 *
 * 功能：
 * - 右键末影珍珠：绑定模式（先点击源，再点击目标）
 * - 空手右键：切换输入/输出模式
 * - Shift+右键：显示状态信息
 * - 可跨维度无线传输能量
 */
public class BlockEnergyLink extends Block implements ITileEntityProvider {

    public BlockEnergyLink() {
        super(Material.IRON);
        setHardness(3.0F);
        setResistance(10.0F);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("energy_link");
        setTranslationKey("energy_link");
        setLightLevel(0.3F);  // 轻微发光
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityEnergyLink)) return false;

        TileEntityEnergyLink link = (TileEntityEnergyLink) te;
        ItemStack heldItem = player.getHeldItem(hand);

        // 使用末影珍珠绑定
        if (heldItem.getItem() == Items.ENDER_PEARL) {
            return handleEnderPearlBinding(player, link, heldItem, world, pos);
        }

        // 空手操作
        if (heldItem.isEmpty()) {
            if (player.isSneaking()) {
                // Shift+右键：显示状态
                showStatus(player, link, world);
            } else {
                // 右键：切换模式
                link.toggleMode();
                String modeName = link.getMode() == TileEntityEnergyLink.LinkMode.INPUT ? "输入" : "输出";
                player.sendStatusMessage(new TextComponentString(
                    TextFormatting.AQUA + "模式切换为: " + TextFormatting.YELLOW + modeName
                ), true);
            }
            return true;
        }

        return false;
    }

    /**
     * 处理末影珍珠绑定
     */
    private boolean handleEnderPearlBinding(EntityPlayer player, TileEntityEnergyLink link,
                                            ItemStack pearl, World world, BlockPos pos) {
        NBTTagCompound pearlNbt = pearl.getTagCompound();

        // 检查珍珠是否已经记录了源位置
        if (pearlNbt != null && pearlNbt.hasKey("EnergyLinkSource")) {
            // 第二次点击：完成绑定
            BlockPos sourcePos = new BlockPos(
                pearlNbt.getInteger("SourceX"),
                pearlNbt.getInteger("SourceY"),
                pearlNbt.getInteger("SourceZ")
            );
            int sourceDim = pearlNbt.getInteger("SourceDim");

            // 不能绑定到自己
            if (sourcePos.equals(pos) && sourceDim == world.provider.getDimension()) {
                player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "不能绑定到自身！"
                ), true);
                return true;
            }

            // 获取源链接器
            TileEntityEnergyLink sourceLink = getEnergyLinkAt(world, sourcePos, sourceDim);
            if (sourceLink == null) {
                player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "源链接器不存在或已被破坏！"
                ), true);
                // 清除珍珠NBT
                pearl.setTagCompound(null);
                return true;
            }

            // 双向绑定
            sourceLink.setLinkedPos(pos, world.provider.getDimension());
            link.setLinkedPos(sourcePos, sourceDim);

            // 自动设置模式：源为输入，目标为输出
            sourceLink.toggleMode(); // 设为INPUT
            if (sourceLink.getMode() != TileEntityEnergyLink.LinkMode.INPUT) {
                sourceLink.toggleMode();
            }
            if (link.getMode() != TileEntityEnergyLink.LinkMode.OUTPUT) {
                link.toggleMode();
            }

            // 清除珍珠NBT
            pearl.setTagCompound(null);

            // 消耗一个珍珠
            if (!player.isCreative()) {
                pearl.shrink(1);
            }

            player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "绑定成功！" +
                TextFormatting.GRAY + " 源链接器设为输入模式，目标设为输出模式"
            ));

            // 显示绑定信息
            String dimInfo = (sourceDim == world.provider.getDimension()) ? "" :
                TextFormatting.LIGHT_PURPLE + " (跨维度)";
            player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "链接: " +
                TextFormatting.WHITE + String.format("(%d, %d, %d)", sourcePos.getX(), sourcePos.getY(), sourcePos.getZ()) +
                TextFormatting.GRAY + " <-> " +
                TextFormatting.WHITE + String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ()) +
                dimInfo
            ));

            return true;
        } else {
            // 第一次点击：记录源位置
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setBoolean("EnergyLinkSource", true);
            nbt.setInteger("SourceX", pos.getX());
            nbt.setInteger("SourceY", pos.getY());
            nbt.setInteger("SourceZ", pos.getZ());
            nbt.setInteger("SourceDim", world.provider.getDimension());
            pearl.setTagCompound(nbt);

            player.sendStatusMessage(new TextComponentString(
                TextFormatting.YELLOW + "已记录源位置！右键另一个能量链接器完成绑定"
            ), true);

            return true;
        }
    }

    /**
     * 获取指定位置的能量链接器
     */
    @Nullable
    private TileEntityEnergyLink getEnergyLinkAt(World world, BlockPos pos, int dimension) {
        if (dimension == world.provider.getDimension()) {
            TileEntity te = world.getTileEntity(pos);
            return (te instanceof TileEntityEnergyLink) ? (TileEntityEnergyLink) te : null;
        } else {
            net.minecraft.world.WorldServer targetWorld = world.getMinecraftServer().getWorld(dimension);
            if (targetWorld != null) {
                TileEntity te = targetWorld.getTileEntity(pos);
                return (te instanceof TileEntityEnergyLink) ? (TileEntityEnergyLink) te : null;
            }
        }
        return null;
    }

    /**
     * 显示链接器状态
     */
    private void showStatus(EntityPlayer player, TileEntityEnergyLink link, World world) {
        player.sendMessage(new TextComponentString(
            TextFormatting.GOLD + "=== 能量链接器状态 ==="
        ));

        String modeName = link.getMode() == TileEntityEnergyLink.LinkMode.INPUT ?
            TextFormatting.GREEN + "输入 (抽取能量)" :
            TextFormatting.YELLOW + "输出 (发送能量)";
        player.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "模式: " + modeName
        ));

        player.sendMessage(new TextComponentString(
            TextFormatting.GRAY + "能量: " + TextFormatting.AQUA +
            formatEnergy(link.getEnergyStored()) + " / " + formatEnergy(link.getMaxEnergyStored()) + " RF"
        ));

        if (link.isLinked()) {
            BlockPos linkedPos = link.getLinkedPos();
            int linkedDim = link.getLinkedDimension();
            String dimInfo = (linkedDim == world.provider.getDimension()) ?
                "" : TextFormatting.LIGHT_PURPLE + " (维度 " + linkedDim + ")";

            player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "已绑定: " + TextFormatting.WHITE +
                String.format("(%d, %d, %d)", linkedPos.getX(), linkedPos.getY(), linkedPos.getZ()) +
                dimInfo
            ));

            // 检查配对是否有效
            TileEntityEnergyLink linkedTile = getEnergyLinkAt(world, linkedPos, linkedDim);
            if (linkedTile == null) {
                player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "警告: 配对的链接器不存在！"
                ));
            }
        } else {
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "未绑定 - 使用末影珍珠绑定"
            ));
        }
    }

    private String formatEnergy(int energy) {
        if (energy >= 1000000) {
            return String.format("%.1fM", energy / 1000000.0);
        } else if (energy >= 1000) {
            return String.format("%.1fk", energy / 1000.0);
        }
        return String.valueOf(energy);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityEnergyLink();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityEnergyLink) {
            TileEntityEnergyLink link = (TileEntityEnergyLink) te;
            // 清除配对链接器的绑定
            if (link.isLinked()) {
                TileEntityEnergyLink linkedTile = getEnergyLinkAt(world, link.getLinkedPos(), link.getLinkedDimension());
                if (linkedTile != null) {
                    linkedTile.clearLink();
                }
            }
        }
        super.breakBlock(world, pos, state);
    }
}
