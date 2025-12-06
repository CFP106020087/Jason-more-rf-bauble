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
 * 虚无之眸 - 七咒之戒联动饰品
 * Eye of the Void
 *
 * 效果：
 * - 基础效果：无（仅为联动载体）
 * - 七咒联动：致命伤害时消耗经验抵御死亡
 *
 * 机制：
 * - 需要佩戴七咒之戒才能装备
 * - 致命伤害时消耗 10 级经验阻止死亡
 * - 触发后恢复 4 点血量
 * - 60 秒冷却时间
 *
 * 代价：
 * - 经验是稀缺资源，每次触发损失大量等级
 * - 冷却期间无法再次触发
 */
public class ItemVoidGaze extends Item implements IBauble {

    // 经验消耗量（级数）
    private static final int XP_LEVEL_COST = 10;
    // 冷却时间（秒）
    private static final int COOLDOWN_SECONDS = 60;

    public ItemVoidGaze() {
        this.setMaxStackSize(1);
        this.setTranslationKey("void_gaze");
        this.setRegistryName("void_gaze");
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.CHARM;
    }

    @Override
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        // 必须佩戴七咒之戒才能装备
        if (!(player instanceof EntityPlayer))
            return false;

        EntityPlayer p = (EntityPlayer) player;
        return hasCursedRing(p);
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote) return;
        if (!(player instanceof EntityPlayer)) return;

        EntityPlayer p = (EntityPlayer) player;
        // 装备时的提示
        p.sendMessage(new net.minecraft.util.text.TextComponentString(
                TextFormatting.DARK_PURPLE + "虚无之眸低语：" +
                TextFormatting.GRAY + "当你凝视深渊，深渊也在凝视你..."
        ));
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 无特殊效果
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        // 死亡保护逻辑由 CurseDeathHook (ASM) 处理
        // 这里不需要做任何事
    }

    // ========== 辅助方法 ==========

    /**
     * 检查玩家是否佩戴虚无之眸
     */
    public static boolean hasVoidGaze(EntityPlayer player) {
        return CurseDeathHook.hasVoidGaze(player);
    }

    /**
     * 检查玩家是否佩戴七咒之戒
     */
    private static boolean hasCursedRing(EntityPlayer player) {
        return CurseDeathHook.hasCursedRing(player);
    }

    // ========== 物品属性 ==========

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 发光效果
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;

        list.add(TextFormatting.DARK_PURPLE + "═══════════════════════════");
        list.add(TextFormatting.LIGHT_PURPLE + "" + TextFormatting.BOLD + "虚无之眸");
        list.add(TextFormatting.DARK_GRAY + "Eye of the Void");
        list.add("");
        list.add(TextFormatting.GRAY + "一颗漆黑的眼珠，仿佛能看透生死");

        // 如果玩家没有佩戴七咒之戒，显示装备条件
        if (player == null || !hasCursedRing(player)) {
            list.add("");
            list.add(TextFormatting.DARK_RED + "⚠ 需要佩戴七咒之戒才能装备");
            list.add("");
            list.add(TextFormatting.DARK_PURPLE + "═══════════════════════════");
            return;
        }

        list.add("");
        list.add(TextFormatting.DARK_PURPLE + "◆ 七咒联动 - 深渊凝视");

        if (player != null && hasCursedRing(player)) {
            list.add(TextFormatting.LIGHT_PURPLE + "  ✓ 联动已激活");
        } else {
            list.add(TextFormatting.DARK_RED + "  ✗ 需要七咒之戒");
        }

        list.add("");
        list.add(TextFormatting.AQUA + "【死亡保护】");
        list.add(TextFormatting.GRAY + "  致命伤害时自动触发");
        list.add(TextFormatting.RED + "  消耗 " + TextFormatting.GREEN + XP_LEVEL_COST + " 级经验");
        list.add(TextFormatting.GRAY + "  恢复 " + TextFormatting.GREEN + "4" + TextFormatting.GRAY + " 点血量");
        list.add(TextFormatting.GRAY + "  冷却时间：" + TextFormatting.YELLOW + COOLDOWN_SECONDS + " 秒");

        // 显示冷却状态
        if (player != null && hasVoidGaze(player)) {
            list.add("");
            if (CurseDeathHook.isOnCooldown(player)) {
                int remaining = CurseDeathHook.getRemainingCooldown(player);
                list.add(TextFormatting.RED + "⏳ 冷却中：" + remaining + " 秒");
            } else {
                list.add(TextFormatting.GREEN + "✓ 已就绪");
            }

            // 显示当前经验等级
            list.add(TextFormatting.GRAY + "当前经验等级：" +
                    (player.experienceLevel >= XP_LEVEL_COST ?
                            TextFormatting.GREEN : TextFormatting.RED) +
                    player.experienceLevel);
        }

        list.add("");
        list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"当你凝视深渊，深渊也在保护你\"");
        list.add(TextFormatting.DARK_PURPLE + "═══════════════════════════");
    }
}
