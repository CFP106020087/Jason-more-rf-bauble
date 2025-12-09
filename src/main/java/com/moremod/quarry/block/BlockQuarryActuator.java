package com.moremod.quarry.block;

import com.moremod.quarry.tile.TileQuantumQuarry;
import com.moremod.quarry.tile.TileQuarryActuator;
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
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 采石场代理方块（Actuator）
 * 放置在量子采石场六面，用于能量输入和红石控制
 */
public class BlockQuarryActuator extends Block implements ITileEntityProvider {
    
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    
    public BlockQuarryActuator() {
        super(Material.IRON);
        setHardness(5.0F);
        setResistance(10.0F);
        setLightLevel(0.5F);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.DOWN));
        // setRegistryName("quarry_actuator");
        // setUnlocalizedName("quarry_actuator");
    }
    
    // ==================== 方块状态 ====================
    
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }
    
    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }
    
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta));
    }
    
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileQuarryActuator) {
            return state.withProperty(FACING, ((TileQuarryActuator) te).getFacing());
        }
        return state;
    }
    
    // ==================== TileEntity ====================
    
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileQuarryActuator();
    }
    
    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }
    
    // ==================== 放置逻辑 ====================
    
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, 
                                            float hitX, float hitY, float hitZ, 
                                            int meta, EntityLivingBase placer, EnumHand hand) {
        // 放置时朝向与点击面相反（指向核心）
        return getDefaultState().withProperty(FACING, facing.getOpposite());
    }
    
    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, 
                                EntityLivingBase placer, ItemStack stack) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileQuarryActuator) {
            ((TileQuarryActuator) te).setFacing(state.getValue(FACING));
        }
    }
    
    @Override
    public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side) {
        // 只能放置在量子采石场旁边
        BlockPos corePos = pos.offset(side.getOpposite());
        TileEntity te = worldIn.getTileEntity(corePos);
        return te instanceof TileQuantumQuarry;
    }
    
    // ==================== 交互 ====================
    
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, 
                                    EntityPlayer playerIn, EnumHand hand, 
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) return true;
        
        // 转发到核心方块
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileQuarryActuator) {
            TileQuantumQuarry core = ((TileQuarryActuator) te).getCore();
            if (core != null) {
                BlockPos corePos = core.getPos();
                IBlockState coreState = worldIn.getBlockState(corePos);
                return coreState.getBlock().onBlockActivated(
                    worldIn, corePos, coreState, playerIn, hand, facing, hitX, hitY, hitZ
                );
            }
        }
        
        return false;
    }
    
    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, 
                                Block blockIn, BlockPos fromPos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileQuarryActuator) {
            ((TileQuarryActuator) te).onNeighborChanged();
        }
        
        // 检查核心是否还存在
        if (!worldIn.isRemote) {
            EnumFacing facing = state.getValue(FACING);
            BlockPos corePos = pos.offset(facing);
            TileEntity coreTe = worldIn.getTileEntity(corePos);
            if (!(coreTe instanceof TileQuantumQuarry)) {
                // 核心不存在，破坏自己
                worldIn.destroyBlock(pos, true);
            }
        }
    }
    
    // ==================== 提示信息 ====================
    
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, 
                               List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "Place on all 6 sides of a Quantum Quarry");
        tooltip.add(TextFormatting.GRAY + "Transfers energy and redstone signal");
    }
}
