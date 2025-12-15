package com.moremod.sponsor.item;

import com.google.common.collect.Multimap;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.sponsor.SponsorConfig;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 赞助者武器基类
 *
 * 继承此类来创建自定义赞助者武器
 */
public class SponsorSword extends ItemSword {

    // 赞助者专属材料（可自定义属性）
    public static final ToolMaterial SPONSOR_MATERIAL = EnumHelper.addToolMaterial(
        "SPONSOR",
        3,              // 采集等级
        2000,           // 耐久
        8.0F,           // 效率
        3.0F,           // 基础攻击伤害
        18              // 附魔能力
    );

    protected final String displayName;
    protected final String sponsorName;

    /**
     * 创建赞助者武器
     *
     * @param registryName 注册名
     * @param displayName 显示名称
     */
    public SponsorSword(String registryName, String displayName) {
        this(registryName, displayName, null);
    }

    /**
     * 创建赞助者武器（带赞助者名称）
     *
     * @param registryName 注册名
     * @param displayName 显示名称
     * @param sponsorName 赞助者名称（会显示在tooltip中）
     */
    public SponsorSword(String registryName, String displayName, @Nullable String sponsorName) {
        super(SPONSOR_MATERIAL);
        this.displayName = displayName;
        this.sponsorName = sponsorName;
        this.setRegistryName("moremod", registryName);
        this.setTranslationKey(registryName);
        this.setMaxStackSize(1);

        // 根据配置决定是否显示在创造标签页
        if (SponsorConfig.items.showInCreativeTab) {
            this.setCreativeTab(moremodCreativeTab.moremod_TAB);
        }
    }

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
        tooltip.add("\u00a77感谢您对模组的支持!");

        // 添加自定义tooltip
        addCustomTooltip(stack, worldIn, tooltip, flagIn);
    }

    /**
     * 子类可以覆写此方法添加自定义tooltip
     */
    protected void addCustomTooltip(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        // 默认不添加额外信息
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
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> modifiers = super.getAttributeModifiers(slot, stack);

        if (slot == EntityEquipmentSlot.MAINHAND) {
            // 应用配置中的额外伤害加成
            double bonusDamage = SponsorConfig.weapons.baseDamageBonus;
            if (bonusDamage > 0) {
                modifiers.put(
                    SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Sponsor Bonus", bonusDamage, 0)
                );
            }
        }

        return modifiers;
    }

    @Override
    public int getItemEnchantability() {
        return SponsorConfig.items.allowEnchanting ? super.getItemEnchantability() : 0;
    }
}
