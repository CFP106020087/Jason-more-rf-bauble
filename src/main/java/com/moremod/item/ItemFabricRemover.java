package com.moremod.item;

import com.moremod.creativetab.MoremodMaterialsTab;
import com.moremod.fabric.data.UpdatedFabricPlayerData;
import com.moremod.fabric.system.FabricWeavingSystem;
import com.moremod.init.ModItems;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 织布拆解器 - 从盔甲上移除织入的布料并返还材料
 * 使用方式：潜行+右键移除头盔织布，右键移除胸甲织布
 *          潜行+主手换到副手后右键移除护腿/靴子
 * 简化版：右键循环检测并移除第一个有织布的盔甲
 */
@SuppressWarnings("deprecation")
public class ItemFabricRemover extends Item {

    public ItemFabricRemover() {
        setRegistryName(new ResourceLocation("moremod", "fabric_remover"));
        setTranslationKey("moremod.fabric_remover");
        setMaxStackSize(1);
        setMaxDamage(16); // 可用16次
        setCreativeTab(MoremodMaterialsTab.MATERIALS_TAB);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack heldStack = player.getHeldItem(hand);

        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.PASS, heldStack);
        }

        // 检查玩家穿戴的所有盔甲
        boolean removed = false;
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (FabricWeavingSystem.hasFabric(armor)) {
                // 获取织布类型
                UpdatedFabricPlayerData.FabricType fabricType = FabricWeavingSystem.getFabricType(armor);
                if (fabricType == null) continue;

                // 移除织布
                if (FabricWeavingSystem.removeFabric(armor)) {
                    // 返还织布材料
                    ItemStack fabricItem = getFabricItem(fabricType);
                    if (!fabricItem.isEmpty()) {
                        if (!player.inventory.addItemStackToInventory(fabricItem)) {
                            // 背包满了，掉落到地上
                            player.dropItem(fabricItem, false);
                        }
                    }

                    // 扣除耐久
                    heldStack.damageItem(1, player);

                    // 发送消息
                    String fabricName = fabricType.getDisplayName();
                    player.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "成功拆解 " + TextFormatting.GOLD + fabricName +
                                    TextFormatting.GREEN + "，已返还材料！"));

                    removed = true;
                    break; // 每次只移除一件
                }
            }
        }

        if (!removed) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "未找到织有布料的盔甲！"));
            return new ActionResult<>(EnumActionResult.FAIL, heldStack);
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, heldStack);
    }

    /**
     * 根据织布类型获取对应的物品
     */
    private ItemStack getFabricItem(UpdatedFabricPlayerData.FabricType type) {
        switch (type) {
            // 高级织布
            case ABYSS:
                return new ItemStack(ModItems.ABYSSAL_FABRIC);
            case TEMPORAL:
                return new ItemStack(ModItems.CHRONO_FABRIC);
            case SPATIAL:
                return new ItemStack(ModItems.SPACETIME_FABRIC);
            case OTHERWORLD:
                return new ItemStack(ModItems.OTHERWORLDLY_FIBER);

            // 基础织布
            case RESILIENT:
                return new ItemStack(ModItems.RESILIENT_FIBER);
            case VITAL:
                return new ItemStack(ModItems.VITAL_THREAD);
            case LIGHT:
                return new ItemStack(ModItems.LIGHT_WEAVE);
            case PREDATOR:
                return new ItemStack(ModItems.PREDATOR_CLOTH);
            case SIPHON:
                return new ItemStack(ModItems.SIPHON_WRAP);

            default:
                return ItemStack.EMPTY;
        }
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.UNCOMMON;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + I18n.translateToLocal("item.moremod.fabric_remover.desc"));
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + I18n.translateToLocal("item.moremod.fabric_remover.usage"));

        // 显示剩余使用次数
        int remaining = getMaxDamage() - stack.getItemDamage();
        tooltip.add(TextFormatting.YELLOW + I18n.translateToLocal("item.moremod.fabric_remover.uses") + ": " + remaining);
    }
}
