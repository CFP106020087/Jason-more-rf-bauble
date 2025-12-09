package com.moremod.quarry.block;

import com.moremod.quarry.tile.TileQuantumQuarry;
import com.moremod.quarry.tile.TileQuarryActuator;
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
    
    // GUI ID - 需要在你的 mod 主类中注册
    public static final int GUI_ID = 0;  // 修改为你的实际 GUI ID
    
    public BlockQuantumQuarry() {
        super(Material.IRON);
        setHardness(5.0F);
        setResistance(10.0F);
        setLightLevel(1.0F);
        // setRegistryName("quantum_quarry");
        // setUnlocalizedName("quantum_quarry");
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
                // 可以发送消息给玩家说明结构不完整
                return true;
            }
            
            // 打开 GUI
            // playerIn.openGui(moremod.INSTANCE, GUI_ID, worldIn, pos.getX(), pos.getY(), pos.getZ());
            openGui(playerIn, worldIn, pos);
        }
        
        return true;
    }
    
    /**
     * 打开 GUI（需要根据你的 mod 实现）
     */
    protected void openGui(EntityPlayer player, World world, BlockPos pos) {
        // 方式1: 使用 Forge GUI 系统
        // player.openGui(moremod.INSTANCE, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
        
        // 方式2: 如果你使用自定义 GUI 系统，在这里实现
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
        tooltip.add(TextFormatting.GRAY + "Mines blocks from a virtual dimension");
        tooltip.add(TextFormatting.GRAY + "Requires Actuators on all 6 sides");
        tooltip.add("");
        tooltip.add(TextFormatting.AQUA + "Modes:");
        tooltip.add(TextFormatting.WHITE + "  Mining - Generate ores based on biome");
        tooltip.add(TextFormatting.WHITE + "  Mob Drops - Simulate monster kills");
        tooltip.add(TextFormatting.WHITE + "  Loot Table - Generate treasure loot");
    }
    
    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }
}
