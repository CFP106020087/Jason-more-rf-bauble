package com.moremod.block;

import com.moremod.moremod;
import com.moremod.tile.TileEntityBottlingMachine;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Random;

public class BlockBottlingMachine extends BlockContainer {

    // 仅保留“是否工作中”的状态
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockBottlingMachine() {
        super(Material.IRON);
        setHardness(3.5F);
        setResistance(17.5F);
        // 默认不激活
        setDefaultState(this.blockState.getBaseState().withProperty(ACTIVE, Boolean.FALSE));
    }

    // ---------- 交互 ----------
    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) return true;

        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityBottlingMachine)) return false;

        // 先尝试与流体容器交互（桶/瓶等）
        ItemStack held = playerIn.getHeldItem(hand);
        if (!held.isEmpty()) {
            if (FluidUtil.interactWithFluidHandler(playerIn, hand, worldIn, pos, facing)) {
                return true;
            }
        }

        // 打开GUI
        playerIn.openGui(moremod.instance, 21, worldIn, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state,
                                EntityLivingBase placer, ItemStack stack) {
        // 这里不再设置朝向；可选处理自定义名称
        if (stack.hasDisplayName()) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityBottlingMachine) {
                // 可在此把自定义名写入TE
            }
        }
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityBottlingMachine) {
            ItemStackHandler inv = ((TileEntityBottlingMachine) te).getItemHandler();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (!s.isEmpty()) {
                    InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), s);
                }
            }
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityBottlingMachine();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    // ---------- 方块状态 ----------
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ACTIVE);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(ACTIVE) ? 1 : 0; // 仅用第0位表示是否激活
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(ACTIVE, (meta & 1) != 0);
    }

    // ---------- 粒子/音效（激活时） ----------
    @Override
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        if (stateIn.getValue(ACTIVE)) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            if (rand.nextDouble() < 0.1) {
                worldIn.playSound(x, y, z,
                        net.minecraft.init.SoundEvents.BLOCK_WATER_AMBIENT,
                        SoundCategory.BLOCKS, 0.1F, 1.0F, false);
            }
            double ox = rand.nextDouble() * 0.6 - 0.3;
            double oz = rand.nextDouble() * 0.6 - 0.3;
            worldIn.spawnParticle(EnumParticleTypes.WATER_SPLASH, x + ox, y + 0.2, z + oz, 0, 0, 0);
        }
    }

    // 外部调用：切换工作状态（会触发模型切换）
    public static void setState(boolean active, World worldIn, BlockPos pos) {
        IBlockState s = worldIn.getBlockState(pos);
        TileEntity te = worldIn.getTileEntity(pos);
        if (s.getBlock() instanceof BlockBottlingMachine && s.getValue(ACTIVE) != active) {
            worldIn.setBlockState(pos, s.withProperty(ACTIVE, active), 3);
            if (te != null) {
                te.validate();
                worldIn.setTileEntity(pos, te);
            }
        }
    }
}
