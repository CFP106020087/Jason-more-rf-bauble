package com.moremod.block;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.tile.TileEntityTemporalAccelerator;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public class BlockTemporalAccelerator extends Block implements ITileEntityProvider {

    public BlockTemporalAccelerator() {
        super(Material.IRON);
        setTranslationKey("temporal_accelerator");
        setRegistryName("temporal_accelerator");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setHardness(3.0F);
        setResistance(10.0F);
        setLightLevel(0.5F);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityTemporalAccelerator();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity tileEntity = worldIn.getTileEntity(pos);
            if (tileEntity instanceof TileEntityTemporalAccelerator) {
                TileEntityTemporalAccelerator accelerator = (TileEntityTemporalAccelerator) tileEntity;

                // 显示状态信息
                accelerator.toggleActive();
                String status = accelerator.isActive() ?
                        TextFormatting.GREEN + "已激活" : TextFormatting.RED + "已关闭";

                int energy = accelerator.getEnergyStored();
                int maxEnergy = accelerator.getMaxEnergyStored();
                int percentage = (int)((energy * 100.0) / maxEnergy);

                playerIn.sendMessage(new TextComponentString(
                        TextFormatting.GOLD + "时间加速器 " + status));
                playerIn.sendMessage(new TextComponentString(
                        TextFormatting.AQUA + "能量: " + TextFormatting.WHITE +
                                energy + "/" + maxEnergy + " FE" +
                                TextFormatting.GRAY + " (" + percentage + "%)"));
                playerIn.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "消耗: " + TextFormatting.WHITE +
                                accelerator.getEnergyConsumption() + " FE/tick"));
            }
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state,
                                EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        if (!worldIn.isRemote && placer instanceof EntityPlayer) {
            ((EntityPlayer) placer).sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "时间加速器已放置"));
            ((EntityPlayer) placer).sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "• 加速范围: 16x16x9"));
            ((EntityPlayer) placer).sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "• 能量消耗: 20 FE/tick (激活时)"));
            ((EntityPlayer) placer).sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "• 从任意面输入FE能量"));
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        worldIn.removeTileEntity(pos);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityTemporalAccelerator) {
            TileEntityTemporalAccelerator accelerator = (TileEntityTemporalAccelerator) te;
            if (accelerator.isActive() && accelerator.getEnergyStored() > 0) {
                // 时间粒子效果
                for (int i = 0; i < 3; i++) {
                    double x = pos.getX() + 0.5D + (rand.nextDouble() - 0.5D) * 0.6D;
                    double y = pos.getY() + 1.1D;
                    double z = pos.getZ() + 0.5D + (rand.nextDouble() - 0.5D) * 0.6D;
                    worldIn.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, 0.0D, 0.1D, 0.0D);
                }

                // 能量环形粒子效果
                if (rand.nextInt(5) == 0) {
                    double angle = rand.nextDouble() * Math.PI * 2;
                    double radius = 0.6D;
                    double x = pos.getX() + 0.5D + Math.cos(angle) * radius;
                    double y = pos.getY() + 0.5D;
                    double z = pos.getZ() + 0.5D + Math.sin(angle) * radius;
                    worldIn.spawnParticle(EnumParticleTypes.END_ROD, x, y, z, 0.0D, 0.02D, 0.0D);
                }

                // 能量不足警告粒子
                if (accelerator.getEnergyStored() < 1000) {
                    if (rand.nextInt(10) == 0) {
                        double x = pos.getX() + 0.5D;
                        double y = pos.getY() + 1.5D;
                        double z = pos.getZ() + 0.5D;
                        worldIn.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, 0.0D, 0.0D, 0.0D);
                    }
                }
            }
        }
    }
}