package com.moremod.block;

import com.moremod.tile.TileEntityCompostBin;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
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

/**
 * 堆肥桶 - 将有机物转化为骨粉
 * 放入树叶、种子、腐肉等有机物，自动转化为骨粉
 */
public class BlockCompostBin extends Block implements ITileEntityProvider {

    // 桶形碰撞箱
    private static final AxisAlignedBB AABB = new AxisAlignedBB(0.0625, 0, 0.0625, 0.9375, 0.875, 0.9375);

    public BlockCompostBin() {
        super(Material.WOOD);
        setRegistryName("moremod", "compost_bin");
        setTranslationKey("moremod.compost_bin");
        setHarvestLevel("axe", 0);
        setHardness(1.5F);
        setResistance(5.0F);
        setSoundType(SoundType.WOOD);
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.DECORATIONS);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityCompostBin();
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                     EntityPlayer playerIn, EnumHand hand,
                                     EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityCompostBin) {
                TileEntityCompostBin compost = (TileEntityCompostBin) te;
                ItemStack heldItem = playerIn.getHeldItem(hand);

                // 尝试放入有机物
                if (!heldItem.isEmpty() && compost.isValidInput(heldItem)) {
                    ItemStack remaining = compost.addCompostMaterial(heldItem);
                    if (!playerIn.isCreative()) {
                        playerIn.setHeldItem(hand, remaining);
                    }
                    return true;
                }

                // 尝试取出骨粉
                ItemStack output = compost.extractOutput();
                if (!output.isEmpty()) {
                    if (!playerIn.addItemStackToInventory(output)) {
                        playerIn.dropItem(output, false);
                    }
                    return true;
                }

                // 显示状态
                int progress = compost.getCompostProgress();
                int stored = compost.getStoredAmount();
                int outputCount = compost.getOutputCount();

                playerIn.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "堆肥桶状态:" +
                    TextFormatting.GRAY + " 有机物: " + TextFormatting.YELLOW + stored + "/64" +
                    TextFormatting.GRAY + " | 进度: " + TextFormatting.AQUA + progress + "%" +
                    TextFormatting.GRAY + " | 骨粉: " + TextFormatting.WHITE + outputCount
                ));
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityCompostBin) {
            ((TileEntityCompostBin) te).dropInventory();
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "将有机物转化为骨粉");
        tooltip.add(TextFormatting.DARK_GREEN + "接受: 树叶、种子、腐肉、蜘蛛眼等");
        tooltip.add(TextFormatting.YELLOW + "右键放入/取出物品");
    }
}
