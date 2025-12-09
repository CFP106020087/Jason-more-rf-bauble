package com.moremod.block;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.tile.TileEntityPlantOilPress;
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
 * 植物油壓榨機
 *
 * 功能：
 * - 消耗RF能量
 * - 將農作物壓榨成植物油
 */
public class BlockPlantOilPress extends BlockContainer {

    public BlockPlantOilPress() {
        super(Material.IRON);
        setHardness(4.0F);
        setResistance(10.0F);
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setRegistryName("plant_oil_press");
        setTranslationKey("plant_oil_press");
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityPlantOilPress)) return false;

        TileEntityPlantOilPress press = (TileEntityPlantOilPress) te;
        ItemStack heldItem = player.getHeldItem(hand);

        // 蹲下右鍵：顯示狀態
        if (player.isSneaking()) {
            showStatus(player, press);
            return true;
        }

        IItemHandler handler = press.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
        if (handler == null) return false;

        // 空手：取出植物油桶
        if (heldItem.isEmpty()) {
            ItemStack output = handler.extractItem(1, 64, false);
            if (!output.isEmpty()) {
                if (!player.inventory.addItemStackToInventory(output)) {
                    player.dropItem(output, false);
                }
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "取出: " + output.getDisplayName() + " x" + output.getCount()
                ));
                return true;
            }

            // 沒有輸出，嘗試取出輸入
            ItemStack input = handler.extractItem(0, 64, false);
            if (!input.isEmpty()) {
                if (!player.inventory.addItemStackToInventory(input)) {
                    player.dropItem(input, false);
                }
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "取出: " + input.getDisplayName() + " x" + input.getCount()
                ));
                return true;
            }
        } else {
            // 放入農作物
            if (TileEntityPlantOilPress.isValidInput(heldItem)) {
                ItemStack toInsert = heldItem.copy();
                ItemStack remainder = handler.insertItem(0, toInsert, false);
                int inserted = toInsert.getCount() - remainder.getCount();
                if (inserted > 0) {
                    heldItem.shrink(inserted);
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "放入: " + toInsert.getDisplayName() + " x" + inserted
                    ));
                    return true;
                }
            } else {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "此物品無法壓榨成植物油！"
                ));
            }
        }

        return false;
    }

    private void showStatus(EntityPlayer player, TileEntityPlantOilPress press) {
        int energy = press.getEnergyStored();
        int maxEnergy = press.getMaxEnergyStored();
        int progress = press.getProgress();
        int maxProgress = press.getMaxProgress();

        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "=== 壓榨機狀態 ==="
        ));

        // 能量
        float percentage = maxEnergy > 0 ? (energy * 100.0f / maxEnergy) : 0;
        TextFormatting energyColor = percentage >= 50 ? TextFormatting.GREEN :
                                     percentage >= 20 ? TextFormatting.YELLOW : TextFormatting.RED;
        player.sendMessage(new TextComponentString(
                energyColor + "能量: " + formatAmount(energy) + " / " + formatAmount(maxEnergy) + " RF"
        ));

        // 進度
        if (press.isProcessing()) {
            float progressPercent = maxProgress > 0 ? (progress * 100.0f / maxProgress) : 0;
            player.sendMessage(new TextComponentString(
                    TextFormatting.LIGHT_PURPLE + "⚡ 壓榨中... " + String.format("%.1f", progressPercent) + "%"
            ));
        } else {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "待機中 (放入農作物開始壓榨)"
            ));
        }

        // 可壓榨材料提示
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_GRAY + "可壓榨: 小麥、馬鈴薯、胡蘿蔔、甜菜根、南瓜、西瓜等"
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
        if (te instanceof TileEntityPlantOilPress) {
            TileEntityPlantOilPress press = (TileEntityPlantOilPress) te;
            IItemHandler handler = press.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
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
        return new TileEntityPlantOilPress();
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
