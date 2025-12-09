package com.moremod.block;

import com.moremod.tile.TileEntityAnimalFeeder;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
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
 * 动物喂食器 - 自动喂养周围的动物使其繁殖
 * 放入小麦、胡萝卜、种子等，自动寻找周围可繁殖的动物
 */
public class BlockAnimalFeeder extends Block implements ITileEntityProvider {

    public BlockAnimalFeeder() {
        super(Material.WOOD);
        setRegistryName("moremod", "animal_feeder");
        setTranslationKey("moremod.animal_feeder");
        setHarvestLevel("axe", 0);
        setHardness(2.0F);
        setResistance(5.0F);
        setSoundType(SoundType.WOOD);
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.DECORATIONS);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityAnimalFeeder();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                     EntityPlayer playerIn, EnumHand hand,
                                     EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityAnimalFeeder) {
                TileEntityAnimalFeeder feeder = (TileEntityAnimalFeeder) te;
                ItemStack heldItem = playerIn.getHeldItem(hand);

                // 尝试放入食物
                if (!heldItem.isEmpty()) {
                    ItemStack remaining = feeder.addFood(heldItem);
                    if (remaining.getCount() != heldItem.getCount()) {
                        if (!playerIn.isCreative()) {
                            playerIn.setHeldItem(hand, remaining);
                        }
                        return true;
                    }
                }

                // 显示状态
                playerIn.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "喂食器状态:" +
                    TextFormatting.GRAY + " 食物: " + TextFormatting.YELLOW + feeder.getFoodCount() +
                    TextFormatting.GRAY + " | 范围: " + TextFormatting.AQUA + "8格" +
                    TextFormatting.GRAY + " | 冷却: " + TextFormatting.GREEN + feeder.getCooldownSeconds() + "秒"
                ));
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityAnimalFeeder) {
            ((TileEntityAnimalFeeder) te).dropInventory();
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "自动喂养周围的动物使其繁殖");
        tooltip.add(TextFormatting.GOLD + "范围: 8格");
        tooltip.add(TextFormatting.YELLOW + "接受: 小麦、胡萝卜、种子等");
    }
}
