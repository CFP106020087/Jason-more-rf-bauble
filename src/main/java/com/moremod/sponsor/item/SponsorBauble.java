package com.moremod.sponsor.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.sponsor.SponsorConfig;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 赞助者饰品基类
 *
 * 继承此类来创建自定义赞助者饰品
 */
public class SponsorBauble extends Item implements IBauble {

    protected final String displayName;
    protected final String sponsorName;
    protected final BaubleType baubleType;

    /**
     * 创建赞助者饰品
     *
     * @param registryName 注册名
     * @param displayName 显示名称
     * @param baubleType 饰品类型 (AMULET, RING, BELT, TRINKET, HEAD, BODY, CHARM)
     */
    public SponsorBauble(String registryName, String displayName, BaubleType baubleType) {
        this(registryName, displayName, baubleType, null);
    }

    /**
     * 创建赞助者饰品（带赞助者名称）
     *
     * @param registryName 注册名
     * @param displayName 显示名称
     * @param baubleType 饰品类型
     * @param sponsorName 赞助者名称
     */
    public SponsorBauble(String registryName, String displayName, BaubleType baubleType, @Nullable String sponsorName) {
        super();
        this.displayName = displayName;
        this.sponsorName = sponsorName;
        this.baubleType = baubleType;
        this.setRegistryName("moremod", registryName);
        this.setTranslationKey(registryName);
        this.setMaxStackSize(1);

        if (SponsorConfig.items.showInCreativeTab) {
            this.setCreativeTab(moremodCreativeTab.moremod_TAB);
        }
    }

    // ==================== IBauble 实现 ====================

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return baubleType;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        if (player.world.isRemote) return;

        // 根据配置的间隔执行被动效果
        if (player.ticksExisted % SponsorConfig.baubles.effectInterval == 0) {
            onSponsorBaubleTick(itemstack, player);
        }
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (!player.world.isRemote && player instanceof EntityPlayer) {
            ((EntityPlayer) player).sendMessage(new TextComponentString(
                TextFormatting.GOLD + "" + TextFormatting.BOLD + "[赞助者] " +
                TextFormatting.RESET + TextFormatting.YELLOW + displayName + " 的力量觉醒了"
            ));
        }
        onSponsorBaubleEquipped(itemstack, player);
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (!player.world.isRemote && player instanceof EntityPlayer) {
            ((EntityPlayer) player).sendMessage(new TextComponentString(
                TextFormatting.GRAY + displayName + " 的力量沉睡了"
            ));
        }
        onSponsorBaubleUnequipped(itemstack, player);
    }

    // ==================== 子类可覆写的方法 ====================

    /**
     * 子类覆写此方法实现被动效果
     * 每隔 SponsorConfig.baubles.effectInterval tick 调用一次
     */
    protected void onSponsorBaubleTick(ItemStack itemstack, EntityLivingBase player) {
        // 默认不添加被动效果
    }

    /**
     * 子类覆写此方法在装备时执行操作
     */
    protected void onSponsorBaubleEquipped(ItemStack itemstack, EntityLivingBase player) {
        // 默认不添加装备效果
    }

    /**
     * 子类覆写此方法在卸下时执行操作
     */
    protected void onSponsorBaubleUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 默认不添加卸下效果
    }

    /**
     * 子类可以覆写此方法添加自定义tooltip
     */
    protected void addCustomTooltip(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        // 默认不添加额外信息
    }

    /**
     * 子类可以覆写此方法添加效果描述
     */
    protected void addEffectTooltip(ItemStack stack, List<String> tooltip) {
        // 默认不添加效果描述
    }

    // ==================== Item 覆写 ====================

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return displayName;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        // 赞助者标识
        tooltip.add("\u00a76\u00a7l[赞助者专属]");

        if (sponsorName != null && !sponsorName.isEmpty()) {
            tooltip.add("\u00a7e赞助者: \u00a7f" + sponsorName);
        }

        tooltip.add("");

        // 饰品类型
        tooltip.add("\u00a77饰品类型: \u00a7b" + getBaubleTypeName());

        // 添加效果描述
        addEffectTooltip(stack, tooltip);

        tooltip.add("");
        tooltip.add("\u00a77感谢您对模组的支持!");

        // 添加自定义tooltip
        addCustomTooltip(stack, worldIn, tooltip, flagIn);
    }

    /**
     * 获取饰品类型的中文名
     */
    private String getBaubleTypeName() {
        switch (baubleType) {
            case AMULET: return "护符";
            case RING: return "戒指";
            case BELT: return "腰带";
            case TRINKET: return "饰品";
            case HEAD: return "头饰";
            case BODY: return "身体";
            case CHARM: return "符咒";
            default: return baubleType.name();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return SponsorConfig.items.hasGlowEffect || super.hasEffect(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return SponsorConfig.items.allowEnchanting;
    }

    @Override
    public int getItemEnchantability() {
        return SponsorConfig.items.allowEnchanting ? 15 : 0;
    }
}
