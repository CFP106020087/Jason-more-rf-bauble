package com.moremod.item;

import com.moremod.creativetab.MoremodMaterialsTab;
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
 * 使用方式：丢在地上与织有布料的盔甲接触即可自动拆除
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

        // 提示玩家使用丢出机制
        player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "将拆解器丢在地上，与织有布料的盔甲接触即可拆除！"));

        // 始终返回 SUCCESS 以阻止装备盔甲
        return new ActionResult<>(EnumActionResult.SUCCESS, heldStack);
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
