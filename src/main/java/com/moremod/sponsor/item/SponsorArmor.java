package com.moremod.sponsor.item;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.moremod;
import com.moremod.sponsor.SponsorConfig;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 赞助者盔甲基类
 *
 * 继承此类来创建自定义赞助者盔甲
 */
public class SponsorArmor extends ItemArmor {

    // 赞助者专属盔甲材料
    public static final ArmorMaterial SPONSOR_ARMOR_MATERIAL = EnumHelper.addArmorMaterial(
        "SPONSOR_ARMOR",
        "moremod:sponsor_armor",
        35,                                             // 耐久系数
        new int[]{3, 6, 8, 3},                          // 护甲值: 靴子, 护腿, 胸甲, 头盔
        18,                                             // 附魔能力
        SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,           // 穿戴音效
        2.5F                                            // 韧性
    );

    protected final String displayName;
    protected final String sponsorName;

    /**
     * 创建赞助者盔甲
     *
     * @param slot 装备槽位
     * @param registryName 注册名
     * @param displayName 显示名称
     */
    public SponsorArmor(EntityEquipmentSlot slot, String registryName, String displayName) {
        this(slot, registryName, displayName, null);
    }

    /**
     * 创建赞助者盔甲（带赞助者名称）
     *
     * @param slot 装备槽位
     * @param registryName 注册名
     * @param displayName 显示名称
     * @param sponsorName 赞助者名称
     */
    public SponsorArmor(EntityEquipmentSlot slot, String registryName, String displayName, @Nullable String sponsorName) {
        super(SPONSOR_ARMOR_MATERIAL, 0, slot);
        this.displayName = displayName;
        this.sponsorName = sponsorName;
        this.setRegistryName("moremod", registryName);
        this.setTranslationKey(registryName);
        this.setMaxStackSize(1);

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

        // 显示套装效果（如果有）
        addSetBonusTooltip(stack, tooltip);

        tooltip.add("\u00a77感谢您对模组的支持!");

        // 添加自定义tooltip
        addCustomTooltip(stack, worldIn, tooltip, flagIn);
    }

    /**
     * 子类可以覆写此方法添加套装效果描述
     */
    protected void addSetBonusTooltip(ItemStack stack, List<String> tooltip) {
        // 默认不添加套装效果
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
    public int getItemEnchantability() {
        return SponsorConfig.items.allowEnchanting ? super.getItemEnchantability() : 0;
    }

    /**
     * 自定义盔甲纹理路径
     */
    @Override
    @Nullable
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        // 默认使用 moremod:textures/models/armor/sponsor_armor_layer_1.png 和 _layer_2.png
        if (slot == EntityEquipmentSlot.LEGS) {
            return "moremod:textures/models/armor/sponsor_armor_layer_2.png";
        }
        return "moremod:textures/models/armor/sponsor_armor_layer_1.png";
    }

    /**
     * 穿戴时每tick调用
     */
    @Override
    public void onArmorTick(World world, EntityPlayer player, ItemStack itemStack) {
        super.onArmorTick(world, player, itemStack);

        // 子类可以覆写此方法添加被动效果
        onSponsorArmorTick(world, player, itemStack);
    }

    /**
     * 子类可以覆写此方法添加被动效果
     */
    protected void onSponsorArmorTick(World world, EntityPlayer player, ItemStack itemStack) {
        // 默认不添加被动效果
    }

    /**
     * 检查玩家是否穿戴了完整的赞助者套装
     */
    public static boolean hasFullSet(EntityPlayer player) {
        for (ItemStack stack : player.getArmorInventoryList()) {
            if (stack.isEmpty() || !(stack.getItem() instanceof SponsorArmor)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取玩家穿戴的赞助者盔甲数量
     */
    public static int getWornPieceCount(EntityPlayer player) {
        int count = 0;
        for (ItemStack stack : player.getArmorInventoryList()) {
            if (!stack.isEmpty() && stack.getItem() instanceof SponsorArmor) {
                count++;
            }
        }
        return count;
    }
}
