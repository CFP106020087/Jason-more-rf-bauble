package com.moremod.block;

import com.moremod.tile.TileEntityBioGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
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
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * 生物质发电机 - 使用有机物发电(RF)
 * 放入种子、树苗、小麦等生物质，转化为RF能量
 */
public class BlockBioGenerator extends Block implements ITileEntityProvider {

    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockBioGenerator() {
        super(Material.IRON);
        setRegistryName("moremod", "bio_generator");
        setUnlocalizedName("moremod.bio_generator");
        setHarvestLevel("pickaxe", 1);
        setHardness(3.0F);
        setResistance(10.0F);
        setSoundType(SoundType.METAL);
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.REDSTONE);
        setDefaultState(blockState.getBaseState().withProperty(ACTIVE, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ACTIVE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(ACTIVE, meta == 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(ACTIVE) ? 1 : 0;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityBioGenerator) {
            return state.withProperty(ACTIVE, ((TileEntityBioGenerator) te).isGenerating());
        }
        return state;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityBioGenerator();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                     EntityPlayer playerIn, EnumHand hand,
                                     EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityBioGenerator) {
                TileEntityBioGenerator generator = (TileEntityBioGenerator) te;
                ItemStack heldItem = playerIn.getHeldItem(hand);

                // 尝试放入燃料
                if (!heldItem.isEmpty()) {
                    ItemStack remaining = generator.addFuel(heldItem);
                    if (remaining.getCount() != heldItem.getCount()) {
                        if (!playerIn.isCreative()) {
                            playerIn.setHeldItem(hand, remaining);
                        }
                        return true;
                    }
                }

                // 显示状态
                playerIn.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "生物质发电机状态:"
                ));
                playerIn.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "能量: " + TextFormatting.RED + generator.getEnergyStored() +
                    "/" + generator.getMaxEnergyStored() + " RF"
                ));
                playerIn.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "燃料: " + TextFormatting.GREEN + generator.getFuelCount() +
                    TextFormatting.GRAY + " | 发电: " + (generator.isGenerating() ?
                        TextFormatting.GREEN + "运行中" : TextFormatting.RED + "停止")
                ));
            }
        }
        return true;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityBioGenerator) {
            ((TileEntityBioGenerator) te).dropInventory();
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityBioGenerator && ((TileEntityBioGenerator) te).isGenerating()) {
            return 8;
        }
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityBioGenerator && ((TileEntityBioGenerator) te).isGenerating()) {
            // 绿色粒子效果表示正在发电
            double x = pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 0.5;

            worldIn.spawnParticle(net.minecraft.util.EnumParticleTypes.VILLAGER_HAPPY, x, y, z, 0, 0.05, 0);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "使用生物质发电");
        tooltip.add(TextFormatting.RED + "产出: 40 RF/t");
        tooltip.add(TextFormatting.YELLOW + "容量: 50,000 RF");
        tooltip.add(TextFormatting.GREEN + "燃料: 种子、树苗、小麦等");
    }
}
