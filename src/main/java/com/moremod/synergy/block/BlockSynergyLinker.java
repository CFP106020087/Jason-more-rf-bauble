package com.moremod.synergy.block;

import com.moremod.synergy.tile.TileEntitySynergyLinker;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Synergy Linker 方块
 *
 * 说明：
 * - 用于激活/停用玩家的 Synergy
 * - 右键打开 GUI 界面
 * - 不存储任何数据（数据在玩家 NBT 中）
 *
 * 注册方式：
 * - 在你的方块注册代码中添加此方块
 * - 在 GuiHandler 中注册对应的 GUI
 */
public class BlockSynergyLinker extends Block implements ITileEntityProvider {

    public BlockSynergyLinker() {
        super(Material.IRON);
        setTranslationKey("synergy_linker");
        setRegistryName("synergy_linker");
        setHardness(3.0F);
        setResistance(10.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntitySynergyLinker();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            // 注意：你需要在 GuiHandler 中定义 GUI_ID
            // 例如：public static final int SYNERGY_LINKER_GUI = 29;
            // 然后这里调用：
            // playerIn.openGui(YourMod.instance, GuiHandler.SYNERGY_LINKER_GUI,
            //         worldIn, pos.getX(), pos.getY(), pos.getZ());

            // 临时示例代码（你需要替换为实际的 GUI ID）
            // playerIn.openGui(moremod.instance, 29, worldIn, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }
}
