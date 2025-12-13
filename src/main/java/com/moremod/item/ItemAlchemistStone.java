package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * 炼药师的术石
 *
 * 佩戴条件：需要佩戴七咒之戒
 *
 * 正面效果：
 * - 在炼药台上可以炼出超过等级上限的药水
 * - 红石可以一直放（延长时间无上限）
 * - 荧光石可以一直放（提升等级，最多超5级）
 *
 * 负面效果：
 * - 佩戴后无法摘除
 * - 最多只能获得5个正面药水效果
 */
public class ItemAlchemistStone extends Item implements IBauble {

    // 最大允许超过原版上限的等级数
    public static final int MAX_EXTRA_AMPLIFIER = 5;

    // 最大正面药水效果数量
    public static final int MAX_BENEFICIAL_EFFECTS = 5;

    public ItemAlchemistStone() {
        this.setMaxStackSize(1);
        this.setTranslationKey("alchemist_stone");
        this.setRegistryName("alchemist_stone");
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.CHARM;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 发光效果
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
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        // 无法摘除 - 负面效果
        return false;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote || !(player instanceof EntityPlayer))
            return;

        EntityPlayer p = (EntityPlayer) player;
        p.sendMessage(new net.minecraft.util.text.TextComponentString(
            TextFormatting.DARK_PURPLE + "[炼药师的术石] " +
            TextFormatting.RED + "术石已与你的灵魂绑定，无法移除..."
        ));
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase livingPlayer) {
        if (livingPlayer.world.isRemote || !(livingPlayer instanceof EntityPlayer))
            return;

        EntityPlayer player = (EntityPlayer) livingPlayer;

        // 每秒检查一次正面药水效果数量
        if (player.ticksExisted % 20 != 0)
            return;

        enforceBeneficialEffectLimit(player);
    }

    /**
     * 强制执行正面药水效果上限
     * 如果超过5个，移除最新添加的
     */
    private void enforceBeneficialEffectLimit(EntityPlayer player) {
        Collection<PotionEffect> effects = player.getActivePotionEffects();

        int beneficialCount = 0;
        PotionEffect weakestBeneficial = null;
        int shortestDuration = Integer.MAX_VALUE;

        for (PotionEffect effect : effects) {
            Potion potion = effect.getPotion();
            if (potion.isBeneficial()) {
                beneficialCount++;
                // 找到持续时间最短的正面效果（优先移除）
                if (effect.getDuration() < shortestDuration) {
                    shortestDuration = effect.getDuration();
                    weakestBeneficial = effect;
                }
            }
        }

        // 如果超过上限，移除持续时间最短的正面效果
        if (beneficialCount > MAX_BENEFICIAL_EFFECTS && weakestBeneficial != null) {
            player.removePotionEffect(weakestBeneficial.getPotion());

            // 通知玩家
            if (player.ticksExisted % 100 == 0) { // 每5秒最多提示一次
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    TextFormatting.DARK_PURPLE + "[炼药师的术石] " +
                    TextFormatting.GRAY + "你的身体无法承受更多正面效果..."
                ));
            }
        }
    }

    /**
     * 检查玩家是否佩戴炼药师的术石
     */
    public static boolean isWearing(EntityPlayer player) {
        if (player == null) return false;

        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
            if (!bauble.isEmpty() && bauble.getItem() instanceof ItemAlchemistStone) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查玩家是否佩戴七咒之戒
     */
    private static boolean hasCursedRing(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack bauble = BaublesApi.getBaubles(player).getStackInSlot(i);
            if (!bauble.isEmpty() &&
                    bauble.getItem().getRegistryName() != null &&
                    "cursed_ring".equals(bauble.getItem().getRegistryName().getPath())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> list, ITooltipFlag flagIn) {
        EntityPlayer clientPlayer = net.minecraft.client.Minecraft.getMinecraft().player;

        // 检查是否有七咒之戒
        if (clientPlayer == null || !hasCursedRing(clientPlayer)) {
            list.add("");
            list.add(TextFormatting.DARK_RED + "需要佩戴七咒之戒才能装备");
            return;
        }

        list.add("");
        list.add(TextFormatting.DARK_PURPLE + "═══════ 炼药术的禁忌 ═══════");
        list.add("");
        list.add(TextFormatting.GOLD + "◆ 正面效果：");
        list.add(TextFormatting.GREEN + "  ▪ 炼药台可突破药水等级上限");
        list.add(TextFormatting.GREEN + "  ▪ 荧光石可连续使用，最多+" + MAX_EXTRA_AMPLIFIER + "级");
        list.add(TextFormatting.GREEN + "  ▪ 红石可连续使用，无限延长时间");
        list.add("");
        list.add(TextFormatting.DARK_GRAY + "━━━━ " + TextFormatting.DARK_RED + "代价" + TextFormatting.DARK_GRAY + " ━━━━");
        list.add(TextFormatting.RED + "  ▪ 一旦佩戴，无法摘除");
        list.add(TextFormatting.RED + "  ▪ 最多只能拥有" + MAX_BENEFICIAL_EFFECTS + "个正面药水效果");
        list.add("");
        list.add(TextFormatting.DARK_GRAY + "「为了追求更高的炼药境界，");
        list.add(TextFormatting.DARK_GRAY + "  炼药师愿意付出一切代价...」");
    }
}
