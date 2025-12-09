package com.moremod.block;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.tile.TileEntityOilGenerator;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
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
 * 石油發電機
 *
 * 功能：
 * - 燃燒石油或植物油發電
 * - 輸出RF能量到相鄰機器
 */
public class BlockOilGenerator extends BlockContainer {

    public BlockOilGenerator() {
        super(Material.IRON);
        setHardness(4.0F);
        setResistance(10.0F);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("oil_generator");
        setTranslationKey("oil_generator");
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityOilGenerator)) return false;

        TileEntityOilGenerator generator = (TileEntityOilGenerator) te;
        ItemStack heldItem = player.getHeldItem(hand);

        // 蹲下右鍵：顯示狀態
        if (player.isSneaking()) {
            showStatus(player, generator);
            return true;
        }

        IItemHandler handler = generator.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
        if (handler == null) return false;

        // 空手：取出燃料
        if (heldItem.isEmpty()) {
            ItemStack fuel = handler.extractItem(0, 64, false);
            if (!fuel.isEmpty()) {
                if (!player.inventory.addItemStackToInventory(fuel)) {
                    player.dropItem(fuel, false);
                }
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "取出: " + fuel.getDisplayName() + " x" + fuel.getCount()
                ));
                return true;
            }
        } else {
            // 放入燃料
            if (TileEntityOilGenerator.isValidFuel(heldItem)) {
                ItemStack toInsert = heldItem.copy();
                ItemStack remainder = handler.insertItem(0, toInsert, false);
                int inserted = toInsert.getCount() - remainder.getCount();
                if (inserted > 0) {
                    heldItem.shrink(inserted);
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "放入燃料: " + toInsert.getDisplayName() + " x" + inserted
                    ));
                    return true;
                }
            }
            // 放入增速插件 (槽位1-4)
            else if (TileEntityOilGenerator.isValidUpgrade(heldItem)) {
                // 嘗試找一個空的增速槽
                for (int slot = 1; slot <= 4; slot++) {
                    if (handler.getStackInSlot(slot).isEmpty()) {
                        ItemStack toInsert = heldItem.copy();
                        toInsert.setCount(1);
                        ItemStack remainder = handler.insertItem(slot, toInsert, false);
                        if (remainder.isEmpty()) {
                            heldItem.shrink(1);
                            int upgradeCount = 0;
                            for (int i = 1; i <= 4; i++) {
                                if (!handler.getStackInSlot(i).isEmpty()) upgradeCount++;
                            }
                            player.sendMessage(new TextComponentString(
                                    TextFormatting.AQUA + "安裝增速插件! " + TextFormatting.YELLOW +
                                    "(" + upgradeCount + "/4) " + TextFormatting.GREEN +
                                    "+" + (upgradeCount * 50) + "% 發電速度"
                            ));
                            return true;
                        }
                    }
                }
                // 所有槽位已滿
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "增速插件槽已滿! (最多4個)"
                ));
            } else {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "此物品不是有效的燃料或增速插件！"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "有效燃料: 原油桶、植物油桶"
                ));
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "有效增速插件: 增速插件、紅石、螢石粉、烈焰粉、綠寶石"
                ));
            }
        }

        return false;
    }

    private void showStatus(EntityPlayer player, TileEntityOilGenerator generator) {
        int energy = generator.getEnergyStored();
        int maxEnergy = generator.getMaxEnergyStored();
        int burnTime = generator.getBurnTime();
        int maxBurnTime = generator.getMaxBurnTime();

        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "=== 石油發電機狀態 ==="
        ));

        // 能量
        float percentage = maxEnergy > 0 ? (energy * 100.0f / maxEnergy) : 0;
        TextFormatting energyColor = percentage >= 80 ? TextFormatting.GREEN :
                                     percentage >= 50 ? TextFormatting.YELLOW : TextFormatting.RED;
        player.sendMessage(new TextComponentString(
                energyColor + "儲能: " + formatAmount(energy) + " / " + formatAmount(maxEnergy) + " RF"
        ));

        // 發電速率
        player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "發電速率: " + generator.getRFPerTick() + " RF/t"
        ));

        // 燃燒狀態
        if (generator.isBurning()) {
            float burnPercent = maxBurnTime > 0 ? (burnTime * 100.0f / maxBurnTime) : 0;
            player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "⚡ 燃燒中... " + String.format("%.1f", burnPercent) + "% 剩餘"
            ));
        } else {
            if (energy >= maxEnergy) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 電量已滿"
                ));
            } else {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "待機中 (放入燃料開始發電)"
                ));
            }
        }

        // 輸出提示
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_GRAY + "自動向相鄰機器輸出能量"
        ));
    }

    private String formatAmount(int amount) {
        if (amount >= 1000000) {
            return String.format("%.1fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("%.1fk", amount / 1000.0);
        }
        return String.valueOf(amount);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityOilGenerator) {
            TileEntityOilGenerator generator = (TileEntityOilGenerator) te;
            IItemHandler handler = generator.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityOilGenerator();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.SOLID;
    }
}
