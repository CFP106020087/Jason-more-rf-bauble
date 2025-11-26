package com.moremod.block;

import com.google.common.base.Predicates;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.tile.TileEntityItemTransporter;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
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
 * 高级物品传输器（支持六向放置）
 * - 修复：facing 属性允许 UP/DOWN；映射用 EnumFacing.byIndex(..)
 * - 渲染：使用标准 MODEL（默认）
 */
public class ItemTransporter extends Block implements ITileEntityProvider {

    /** 六向可用（包含 up/down） */
    public static final PropertyDirection FACING =
            PropertyDirection.create("facing", Predicates.alwaysTrue());

    public static final ItemTransporter INSTANCE = new ItemTransporter();

    public ItemTransporter() {
        super(Material.IRON);
        this.setHardness(3.0F);
        this.setResistance(5.0F);
        this.setTranslationKey("item_transporter");
        this.setRegistryName("moremod", "item_transporter");
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);

        // ★ 默认状态要带上 FACING，否则 blockstates 无法匹配
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH));
    }

    // ---------- TileEntity ----------
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityItemTransporter();
    }

    // ---------- 放置与状态（兼容 1.12.2 常用签名） ----------
    /** 允许把点击的面直接作为朝向（含 UP/DOWN） */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        // 你的映射里 getFront 改名为 byIndex
        return this.getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    // ---------- 交互 ----------
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityItemTransporter) {
                // 打开你的 GUI
                playerIn.openGui(
                        com.moremod.moremod.instance,
                        com.moremod.client.gui.GuiHandler.ITEM_TRANSPORTER_GUI,
                        worldIn, pos.getX(), pos.getY(), pos.getZ()
                );
                System.out.println("[MoreMod] 玩家打开物品传输器GUI");
            }
        }
        return true;
    }

    // ---------- tooltip ----------
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn,
                               List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "高级物品传输系统");
        tooltip.add("");
        tooltip.add(TextFormatting.GREEN + "功能:");
        tooltip.add(TextFormatting.GRAY + "  • 方向控制（6个方向）");
        tooltip.add(TextFormatting.GRAY + "  • 槽位范围控制");
        tooltip.add(TextFormatting.GRAY + "  • 物品过滤（12槽位）");
        tooltip.add(TextFormatting.GRAY + "  • 多种匹配模式");
        tooltip.add(TextFormatting.GRAY + "  • 红石控制");
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "操作:");
        tooltip.add(TextFormatting.GRAY + "  • 空手右键: 查看状态/打开GUI");
        tooltip.add(TextFormatting.GRAY + "  • Shift+手持物品: 添加过滤");
    }

    // ---------- 比较器输出（可选） ----------
    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityItemTransporter) {
            TileEntityItemTransporter trans = (TileEntityItemTransporter) te;
            return trans.inventory.getStackInSlot(0).isEmpty() ? 0 : 15;
        }
        return 0;
    }
}
