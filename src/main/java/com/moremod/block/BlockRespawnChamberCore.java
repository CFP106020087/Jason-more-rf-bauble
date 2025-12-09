package com.moremod.block;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.multiblock.MultiblockRespawnChamber;
import com.moremod.tile.TileEntityRespawnChamberCore;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * 重生倉核心方塊
 *
 * 功能：
 * - 多方塊結構的控制中心
 * - 玩家右鍵綁定重生點
 * - 破碎之神停機重啟後傳送至此
 */
public class BlockRespawnChamberCore extends BlockContainer {

    public BlockRespawnChamberCore() {
        super(Material.IRON);
        setHardness(5.0F);
        setResistance(15.0F);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("respawn_chamber_core");
        setTranslationKey("respawn_chamber_core");
        setLightLevel(0.5F); // 微弱光源
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityRespawnChamberCore)) return false;

        TileEntityRespawnChamberCore core = (TileEntityRespawnChamberCore) te;

        // 蹲下右鍵：顯示狀態
        if (player.isSneaking()) {
            showStatus(player, core, pos, world);
            return true;
        }

        // 普通右鍵：綁定重生點
        core.bindPlayer(player);
        return true;
    }

    /**
     * 顯示重生倉狀態
     */
    private void showStatus(EntityPlayer player, TileEntityRespawnChamberCore core, BlockPos pos, World world) {
        boolean structureValid = MultiblockRespawnChamber.checkStructure(world, pos);
        int tier = structureValid ? MultiblockRespawnChamber.getFrameTier(world, pos) : 0;

        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "=== 重生倉狀態 ==="
        ));

        // 結構狀態
        if (structureValid) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 結構完整 " + TextFormatting.GRAY + "(等級: " + getTierName(tier) + ")"
            ));
        } else {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 結構不完整！"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "需要 3x3x3 結構："
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "  - 地板: 8個框架方塊環繞核心"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "  - 中層: 4角框架，中間空氣"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "  - 天花板: 8個框架 + 中心光源"
            ));
        }

        // 綁定狀態
        if (core.hasBoundPlayer()) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "綁定玩家: " + TextFormatting.WHITE + core.getBoundPlayerName()
            ));
        } else {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "綁定玩家: 無"
            ));
        }

        // 位置信息
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "位置: " + TextFormatting.WHITE +
                        String.format("X:%d Y:%d Z:%d (維度:%d)",
                                pos.getX(), pos.getY(), pos.getZ(),
                                world.provider.getDimension())
        ));

        // 使用提示
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_GRAY + "右鍵核心綁定重生點"
        ));
    }

    private String getTierName(int tier) {
        switch (tier) {
            case 4: return TextFormatting.GREEN + "綠寶石框架";
            case 3: return TextFormatting.AQUA + "鑽石/黑曜石框架";
            case 2: return TextFormatting.GOLD + "金框架";
            default: return TextFormatting.WHITE + "鐵框架";
        }
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityRespawnChamberCore();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return true;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }

    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.SOLID;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityRespawnChamberCore) {
            // TileEntity的invalidate()會自動清除全局追踪
        }
        super.breakBlock(worldIn, pos, state);
    }
}
