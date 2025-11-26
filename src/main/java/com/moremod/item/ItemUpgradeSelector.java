package com.moremod.item;

import com.moremod.client.gui.GuiUpgradeSelection;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.util.SoulbindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemUpgradeSelector extends Item {

    public static final String REG_NAME = "upgrade_selector";

    public ItemUpgradeSelector() {
        setRegistryName(REG_NAME);
        setTranslationKey(REG_NAME);
        setMaxStackSize(1);
        setNoRepair();
        try { setCreativeTab(moremodCreativeTab.moremod_TAB); } catch (Throwable ignored) {}
    }

    // 右键：服务端先绑定；非主人禁止；客户端开GUI
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            if (!SoulbindUtil.isSoulbound(stack)) {
                SoulbindUtil.bindToOwner(stack, player); // 服务端写入，才能同步保存
            }
            if (!SoulbindUtil.isOwner(stack, player) && !player.isCreative()) {
                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.RED + "这不是你的升级选择器"), true);
                return new ActionResult<>(EnumActionResult.FAIL, stack);
            }
        } else {
            openSelectionGui(stack);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    // 物品在玩家身上时，服务端补绑定（避免某些发放路径漏写）
    @Override
    public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        if (!world.isRemote && entity instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) entity;
            if (!SoulbindUtil.isSoulbound(stack)) {
                SoulbindUtil.bindToOwner(stack, p);
            }
        }
        super.onUpdate(stack, world, entity, slot, selected);
    }

    // 主人不可丢弃（Q/背包丢）——创造例外
    @Override
    public boolean onDroppedByPlayer(ItemStack stack, EntityPlayer player) {
        if (SoulbindUtil.isSoulbound(stack) && SoulbindUtil.isOwner(stack, player) && !player.isCreative()) {
            if (player.world.isRemote) {
                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.YELLOW + "灵魂绑定物品不可丢弃"), true);
            }
            return false;
        }
        return super.onDroppedByPlayer(stack, player);
    }

    // 显示名附加 [灵魂绑定]
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String base = super.getItemStackDisplayName(stack);
        if (SoulbindUtil.isSoulbound(stack)) {
            return net.minecraft.util.text.TextFormatting.LIGHT_PURPLE + base +
                    net.minecraft.util.text.TextFormatting.RESET + " " +
                    net.minecraft.util.text.TextFormatting.DARK_AQUA + "[灵魂绑定]";
        }
        return base;
    }

    @SideOnly(Side.CLIENT)
    private void openSelectionGui(ItemStack stack) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiUpgradeSelection(stack));
    }

    // 供事件/外部调用
    public static boolean isSoulbound(ItemStack s) { return SoulbindUtil.isSoulbound(s); }
    public static boolean isOwner(ItemStack s, EntityPlayer p) { return SoulbindUtil.isOwner(s, p); }
}
