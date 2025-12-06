package com.moremod.item.curse;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import com.moremod.core.CurseDeathHook;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 饕餮指骨 - 七咒之戒联动饰品
 * Gluttonous Phalanx
 *
 * 效果：
 * - 正面：掠夺等级 +2
 * - 负面：饥饿消耗速度 ×2
 *
 * 机制：
 * - 需要佩戴七咒之戒才能装备
 * - 掠夺加成由事件处理器实现
 * - 饥饿消耗由 onWornTick 实现
 */
public class ItemGluttonousPhalanx extends Item implements IBauble {

    // 掠夺等级加成
    public static final int LOOTING_BONUS = 2;
    // 额外消耗倍率 (实际消耗 = 正常消耗 × HUNGER_MULTIPLIER)
    public static final float HUNGER_MULTIPLIER = 2.0f;

    public ItemGluttonousPhalanx() {
        this.setMaxStackSize(1);
        this.setTranslationKey("gluttonous_phalanx");
        this.setRegistryName("gluttonous_phalanx");
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.BODY; // 身体槽位，避免与其他七咒饰品冲突
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
                TextFormatting.DARK_GREEN + "饕餮指骨低语：" +
                TextFormatting.GRAY + "贪婪永不满足..."
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

        // 每4秒增加额外的饥饿消耗
        if (player.ticksExisted % 80 == 0) {
            if (!player.capabilities.isCreativeMode) {
                // 增加额外的exhaustion，模拟×2饥饿消耗
                player.getFoodStats().addExhaustion(1.0f);
            }
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 检查玩家是否佩戴饕餮指骨
     */
    public static boolean isWearing(EntityPlayer player) {
        try {
            for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
                ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
                if (!bauble.isEmpty() && bauble.getItem() instanceof ItemGluttonousPhalanx) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * 获取掠夺加成
     */
    public static int getLootingBonus() {
        return LOOTING_BONUS;
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
        list.add(TextFormatting.DARK_GREEN + "" + TextFormatting.BOLD + "饕餮指骨");
        list.add(TextFormatting.DARK_GRAY + "Gluttonous Phalanx");
        list.add("");
        list.add(TextFormatting.GRAY + "饕餮的指骨，永远渴望更多");

        if (player == null || !CurseDeathHook.hasCursedRing(player)) {
            list.add("");
            list.add(TextFormatting.DARK_RED + "! 需要佩戴七咒之戒才能装备");
            list.add("");
            list.add(TextFormatting.DARK_PURPLE + "===========================");
            return;
        }

        list.add("");
        list.add(TextFormatting.GREEN + ". 正面效果");
        list.add(TextFormatting.GRAY + "  掠夺等级 " + TextFormatting.GREEN + "+" + LOOTING_BONUS);

        list.add("");
        list.add(TextFormatting.RED + ". 负面效果");
        list.add(TextFormatting.DARK_RED + "  饥饿消耗速度 x" + (int)HUNGER_MULTIPLIER);

        list.add("");
        list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"贪婪永无止境\"");
        list.add(TextFormatting.DARK_PURPLE + "===========================");
    }
}
