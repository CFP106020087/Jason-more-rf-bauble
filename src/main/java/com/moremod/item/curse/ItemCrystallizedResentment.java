package com.moremod.item.curse;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import com.moremod.core.CurseDeathHook;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 怨念结晶 - 七咒之戒联动饰品
 * Crystallized Resentment
 *
 * 效果：
 * - 正面：受伤时对攻击者施加凋零 II (3秒)
 * - 负面：无法获得再生效果
 *
 * 机制：
 * - 需要佩戴七咒之戒才能装备
 * - 凋零反击由事件处理器实现
 * - 再生禁止由 onWornTick 实现
 */
public class ItemCrystallizedResentment extends Item implements IBauble {

    // 凋零等级 (0-indexed, 1 = Wither II)
    public static final int WITHER_LEVEL = 1;
    // 凋零持续时间 (tick)
    public static final int WITHER_DURATION = 60; // 3秒

    public ItemCrystallizedResentment() {
        this.setMaxStackSize(1);
        this.setTranslationKey("crystallized_resentment");
        this.setRegistryName("crystallized_resentment");
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.CHARM;
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        if (!(player instanceof EntityPlayer)) return false;
        return CurseDeathHook.hasCursedRing((EntityPlayer) player);
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote) return;
        if (!(player instanceof EntityPlayer)) return;

        EntityPlayer p = (EntityPlayer) player;
        p.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.DARK_PURPLE + "怨念结晶低语：" +
                TextFormatting.GRAY + "让仇恨成为你的武器..."
        ));
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 无特殊处理
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (entity.world.isRemote) return;
        if (!(entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) entity;

        // 移除再生效果
        if (player.isPotionActive(MobEffects.REGENERATION)) {
            player.removePotionEffect(MobEffects.REGENERATION);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 检查玩家是否佩戴怨念结晶
     */
    public static boolean isWearing(EntityPlayer player) {
        try {
            for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
                ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
                if (!bauble.isEmpty() && bauble.getItem() instanceof ItemCrystallizedResentment) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * 对攻击者施加凋零效果
     */
    public static void applyWitherToAttacker(EntityLivingBase attacker) {
        if (attacker == null) return;
        attacker.addPotionEffect(new PotionEffect(MobEffects.WITHER, WITHER_DURATION, WITHER_LEVEL, false, true));
    }

    // ========== 物品属性 ==========

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;

        list.add(TextFormatting.DARK_PURPLE + "===========================");
        list.add(TextFormatting.DARK_PURPLE + "" + TextFormatting.BOLD + "怨念结晶");
        list.add(TextFormatting.DARK_GRAY + "Crystallized Resentment");
        list.add("");
        list.add(TextFormatting.GRAY + "凝聚了无尽怨恨的黑色结晶");

        if (player == null || !CurseDeathHook.hasCursedRing(player)) {
            list.add("");
            list.add(TextFormatting.DARK_RED + "! 需要佩戴七咒之戒才能装备");
            list.add("");
            list.add(TextFormatting.DARK_PURPLE + "===========================");
            return;
        }

        list.add("");
        list.add(TextFormatting.GREEN + ". 正面效果");
        list.add(TextFormatting.GRAY + "  受伤时对攻击者施加 " + TextFormatting.DARK_PURPLE + "凋零 II" + TextFormatting.GRAY + " (3秒)");

        list.add("");
        list.add(TextFormatting.RED + ". 负面效果");
        list.add(TextFormatting.DARK_RED + "  无法获得再生效果");

        list.add("");
        list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"以怨报怨，永无止境\"");
        list.add(TextFormatting.DARK_PURPLE + "===========================");
    }
}
