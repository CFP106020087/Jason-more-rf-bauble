package com.moremod.block;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.multiblock.MultiblockUpgradeChamber;
import com.moremod.tile.TileEntityUpgradeChamberCore;
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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * 升級艙核心方塊
 *
 * 功能：
 * - 多方塊結構的控制中心
 * - 存儲RF能量
 * - 接受升級模組
 * - 檢測玩家進入並執行升級
 */
public class BlockUpgradeChamberCore extends BlockContainer {

    public BlockUpgradeChamberCore() {
        super(Material.IRON);
        setHardness(5.0F);
        setResistance(15.0F);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("upgrade_chamber_core");
        setTranslationKey("upgrade_chamber_core");
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityUpgradeChamberCore)) return false;

        TileEntityUpgradeChamberCore core = (TileEntityUpgradeChamberCore) te;

        // 蹲下右鍵：顯示結構檢查和狀態
        if (player.isSneaking()) {
            showStatus(player, core, pos, world);
            return true;
        }

        // 處理物品交互
        IItemHandler handler = core.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
        if (handler == null) return false;

        ItemStack heldItem = player.getHeldItem(hand);
        ItemStack currentModule = handler.getStackInSlot(0);

        // 空手取出模組
        if (heldItem.isEmpty() && !currentModule.isEmpty()) {
            ItemStack extracted = handler.extractItem(0, 64, false);
            if (!player.inventory.addItemStackToInventory(extracted)) {
                player.dropItem(extracted, false);
            }
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "已取出升級模組: " + extracted.getDisplayName()
            ));
            core.markDirty();
            world.notifyBlockUpdate(pos, state, state, 3);
            return true;
        }

        // 放入模組
        if (!heldItem.isEmpty() && currentModule.isEmpty()) {
            ItemStack toInsert = heldItem.copy();
            toInsert.setCount(1);
            ItemStack remainder = handler.insertItem(0, toInsert, false);
            if (remainder.isEmpty()) {
                heldItem.shrink(1);
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "已放入升級模組: " + toInsert.getDisplayName()
                ));
                core.markDirty();
                world.notifyBlockUpdate(pos, state, state, 3);
                return true;
            } else {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "此物品不是有效的升級模組！"
                ));
            }
        }

        // 替換模組
        if (!heldItem.isEmpty() && !currentModule.isEmpty()) {
            ItemStack toInsert = heldItem.copy();
            toInsert.setCount(1);

            // 先取出舊的
            ItemStack extracted = handler.extractItem(0, 64, false);
            // 放入新的
            ItemStack remainder = handler.insertItem(0, toInsert, false);

            if (remainder.isEmpty()) {
                heldItem.shrink(1);
                if (!player.inventory.addItemStackToInventory(extracted)) {
                    player.dropItem(extracted, false);
                }
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "已替換升級模組: " + toInsert.getDisplayName()
                ));
                core.markDirty();
                world.notifyBlockUpdate(pos, state, state, 3);
                return true;
            } else {
                // 放回舊的
                handler.insertItem(0, extracted, false);
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "此物品不是有效的升級模組！"
                ));
            }
        }

        return false;
    }

    private void showStatus(EntityPlayer player, TileEntityUpgradeChamberCore core, BlockPos pos, World world) {
        boolean structureValid = MultiblockUpgradeChamber.checkStructure(world, pos);
        int tier = MultiblockUpgradeChamber.getFrameTier(world, pos);
        int energy = core.getEnergyStored();
        int maxEnergy = core.getMaxEnergyStored();
        int requiredEnergy = core.getRequiredEnergy();
        ItemStack module = core.getModuleStack();
        boolean isRunning = core.isUpgrading();

        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "=== 升級艙狀態 ==="
        ));

        // 結構狀態
        if (structureValid) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 結構完整 " + TextFormatting.GRAY + "(等級: " + getTierName(tier) + ")"
            ));
        } else {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "✗ 結構不完整！請檢查多方塊結構"
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "使用說明書查看建造指南"
            ));
        }

        // 能量狀態
        float percentage = maxEnergy > 0 ? (energy * 100.0f / maxEnergy) : 0;
        TextFormatting energyColor = percentage >= 100 ? TextFormatting.GREEN :
                                     percentage >= 50 ? TextFormatting.YELLOW : TextFormatting.RED;
        player.sendMessage(new TextComponentString(
                energyColor + "能量: " + energy + " / " + maxEnergy + " RF (" + String.format("%.1f", percentage) + "%)"
        ));

        // 模組狀態
        if (!module.isEmpty()) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "模組: " + module.getDisplayName()
            ));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "所需能量: " + requiredEnergy + " RF"
            ));
        } else {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "模組: 無 (放入升級模組開始升級)"
            ));
        }

        // 運行狀態
        if (isRunning) {
            int progress = core.getProgress();
            int maxProgress = core.getMaxProgress();
            player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "⚡ 升級中... " + progress + "/" + maxProgress
            ));
        }
    }

    private String getTierName(int tier) {
        switch (tier) {
            case 4: return TextFormatting.GREEN + "綠寶石框架";
            case 3: return TextFormatting.AQUA + "鑽石框架";
            case 2: return TextFormatting.GOLD + "金框架";
            default: return TextFormatting.WHITE + "鐵框架";
        }
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityUpgradeChamberCore();
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
        if (te instanceof TileEntityUpgradeChamberCore) {
            TileEntityUpgradeChamberCore core = (TileEntityUpgradeChamberCore) te;
            ItemStack module = core.getModuleStack();
            if (!module.isEmpty()) {
                spawnAsEntity(worldIn, pos, module);
            }
        }
        super.breakBlock(worldIn, pos, state);
    }
}
