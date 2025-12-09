package com.moremod.block;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.item.energy.ItemOilBucket;
import com.moremod.item.energy.ItemOilProspector;
import com.moremod.multiblock.MultiblockOilExtractor;
import com.moremod.tile.TileEntityOilExtractorCore;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * 抽油機核心方塊
 *
 * 功能：
 * - 多方塊結構的控制中心
 * - 消耗RF能量從地下提取石油
 * - 輸出石油桶
 */
public class BlockOilExtractorCore extends BlockContainer {

    public BlockOilExtractorCore() {
        super(Material.IRON);
        setHardness(5.0F);
        setResistance(15.0F);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("oil_extractor_core");
        setTranslationKey("oil_extractor_core");
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityOilExtractorCore)) return false;

        TileEntityOilExtractorCore core = (TileEntityOilExtractorCore) te;
        ItemStack heldItem = player.getHeldItem(hand);

        // 蹲下右鍵：顯示狀態
        if (player.isSneaking()) {
            showStatus(player, core, pos, world);
            return true;
        }

        // 空手右鍵：提取石油桶
        if (heldItem.isEmpty()) {
            ItemStack extracted = core.extractOilBucket();
            if (!extracted.isEmpty()) {
                if (!player.inventory.addItemStackToInventory(extracted)) {
                    player.dropItem(extracted, false);
                }
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "取出石油桶 x1"
                ));
                return true;
            } else {
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "沒有可提取的石油"
                ));
            }
        }

        return false;
    }

    private void showStatus(EntityPlayer player, TileEntityOilExtractorCore core, BlockPos pos, World world) {
        boolean structureValid = MultiblockOilExtractor.checkStructure(world, pos);
        ChunkPos chunkPos = new ChunkPos(pos);
        ItemOilProspector.OilVeinData oilData = ItemOilProspector.getOilVeinData(world, chunkPos);

        int energy = core.getEnergyStored();
        int maxEnergy = core.getMaxEnergyStored();
        int storedOil = core.getStoredOil();
        int maxOil = core.getMaxOilStorage();
        int buckets = core.getAvailableBuckets();

        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "=== 抽油機狀態 ==="
        ));

        // 結構狀態
        if (structureValid) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 結構完整"
            ));
        } else {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 結構不完整！請檢查多方塊結構"
            ));
        }

        // 石油礦脈狀態
        if (oilData.hasOil) {
            int remaining = core.getRemainingOil();
            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "石油礦脈: " + formatAmount(remaining) + " / " + formatAmount(oilData.amount) + " mB"
            ));
        } else {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 此區塊沒有石油礦脈！"
            ));
        }

        // 能量狀態
        float percentage = maxEnergy > 0 ? (energy * 100.0f / maxEnergy) : 0;
        TextFormatting energyColor = percentage >= 50 ? TextFormatting.GREEN :
                                     percentage >= 20 ? TextFormatting.YELLOW : TextFormatting.RED;
        player.sendMessage(new TextComponentString(
                energyColor + "能量: " + formatAmount(energy) + " / " + formatAmount(maxEnergy) + " RF"
        ));

        // 儲油罐狀態
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_GRAY + "內部儲油: " + formatAmount(storedOil) + " / " + formatAmount(maxOil) + " mB"
        ));

        // 可提取桶數
        if (buckets > 0) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "可提取: " + buckets + " 桶石油"
            ));
        }

        // 運行狀態
        if (core.isRunning()) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "⚡ 正在抽取石油..."
            ));
        }
    }

    private String formatAmount(int amount) {
        if (amount >= 1000000) {
            return String.format("%.1fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("%.1fk", amount / 1000.0);
        }
        return String.valueOf(amount);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityOilExtractorCore();
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
}
