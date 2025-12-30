package com.moremod.system.humanity.intel;

import com.moremod.system.humanity.BiologicalProfile;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 生物样本物品
 * Biological Sample Item
 *
 * 高人性玩家击杀生物时概率掉落
 * 用于合成情报书，获得对该生物的永久伤害加成
 */
public class ItemBiologicalSample extends Item {

    private static final String NBT_ENTITY_ID = "EntityId";
    private static final String NBT_ENTITY_NAME = "EntityName";

    public ItemBiologicalSample() {
        setTranslationKey("biological_sample");
        setRegistryName("biological_sample");
        setMaxStackSize(64);
        setCreativeTab(CreativeTabs.MISC);
    }

    /**
     * 创建包含生物信息的样本
     */
    public static ItemStack createSample(EntityLivingBase entity) {
        ResourceLocation entityId = EntityList.getKey(entity);
        if (entityId == null) return ItemStack.EMPTY;
        return createSampleFromId(entity, entityId);
    }

    /**
     * 创建包含生物信息的样本（优化版本）
     * 接受已获取的entityId避免重复查询EntityList
     */
    public static ItemStack createSampleFromId(EntityLivingBase entity, ResourceLocation entityId) {
        if (entityId == null) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(IntelSystemItems.BIOLOGICAL_SAMPLE);

        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(NBT_ENTITY_ID, entityId.toString());

        // 获取实体名称
        String entityName = entity.getName();
        tag.setString(NBT_ENTITY_NAME, entityName);

        stack.setTagCompound(tag);
        return stack;
    }

    /**
     * 从样本中获取生物ID
     */
    @Nullable
    public static ResourceLocation getEntityId(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return null;

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(NBT_ENTITY_ID)) return null;

        return new ResourceLocation(tag.getString(NBT_ENTITY_ID));
    }

    /**
     * 从样本中获取生物名称
     */
    public static String getEntityName(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return "未知生物";

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(NBT_ENTITY_NAME)) return "未知生物";

        return tag.getString(NBT_ENTITY_NAME);
    }

    /**
     * 检查两个样本是否属于同一种生物
     */
    public static boolean isSameEntity(ItemStack sample1, ItemStack sample2) {
        ResourceLocation id1 = getEntityId(sample1);
        ResourceLocation id2 = getEntityId(sample2);

        if (id1 == null || id2 == null) return false;
        return id1.equals(id2);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        ResourceLocation entityId = getEntityId(stack);
        String entityName = getEntityName(stack);

        if (entityId != null) {
            tooltip.add(TextFormatting.AQUA + "样本来源: " + TextFormatting.WHITE + entityName);
            tooltip.add(TextFormatting.GRAY + "(" + entityId.toString() + ")");

            // 显示玩家已有的猎人协议数据
            EntityPlayer player = Minecraft.getMinecraft().player;
            if (player != null) {
                IHumanityData data = HumanityCapabilityHandler.getData(player);
                if (data != null && data.isSystemActive()) {
                    BiologicalProfile profile = data.getProfile(entityId);
                    if (profile != null && profile.getCurrentTier() != BiologicalProfile.Tier.NONE) {
                        tooltip.add("");
                        tooltip.add(TextFormatting.GOLD + "【猎人协议】");
                        tooltip.add(TextFormatting.WHITE + "  档案等级: " +
                                getTierColor(profile.getCurrentTier()) + profile.getCurrentTier().displayNameCN);
                        tooltip.add(TextFormatting.WHITE + "  已收集样本: " +
                                TextFormatting.GREEN + profile.getSampleCount());
                        tooltip.add(TextFormatting.WHITE + "  击杀数: " +
                                TextFormatting.RED + profile.getKillCount());

                        float damageBonus = profile.getDamageBonus() * 100;
                        if (damageBonus > 0) {
                            tooltip.add(TextFormatting.WHITE + "  伤害加成: " +
                                    TextFormatting.YELLOW + "+" + String.format("%.0f%%", damageBonus));
                        }

                        float critBonus = profile.getCritBonus() * 100;
                        if (critBonus > 0) {
                            tooltip.add(TextFormatting.WHITE + "  暴击率: " +
                                    TextFormatting.LIGHT_PURPLE + "+" + String.format("%.0f%%", critBonus));
                        }

                        // 显示激活状态
                        if (profile.isActive()) {
                            tooltip.add(TextFormatting.GREEN + "  ✓ 已激活");
                        } else {
                            tooltip.add(TextFormatting.GRAY + "  ○ 未激活");
                        }
                    }

                    // 显示情报系统数据
                    int intelLevel = IntelDataHelper.getIntelLevel(data, entityId);
                    if (intelLevel > 0) {
                        tooltip.add("");
                        tooltip.add(TextFormatting.LIGHT_PURPLE + "【情报系统】");
                        tooltip.add(TextFormatting.WHITE + "  情报等级: " +
                                TextFormatting.AQUA + intelLevel + "/" + IntelDataHelper.MAX_INTEL_LEVEL);
                        float intelDamage = (IntelDataHelper.calculateDamageMultiplier(data, entityId) - 1.0f) * 100;
                        tooltip.add(TextFormatting.WHITE + "  额外伤害: " +
                                TextFormatting.YELLOW + "+" + String.format("%.0f%%", intelDamage));
                    }
                }
            }

            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "可用于合成情报书");
            tooltip.add(TextFormatting.GRAY + "配方: 样本 + 书 + 经验瓶");
        } else {
            tooltip.add(TextFormatting.RED + "无效样本");
        }
    }

    /**
     * 根据档案等级返回对应的颜色
     */
    @SideOnly(Side.CLIENT)
    private static TextFormatting getTierColor(BiologicalProfile.Tier tier) {
        switch (tier) {
            case MASTERED:
                return TextFormatting.GOLD;
            case COMPLETE:
                return TextFormatting.GREEN;
            case BASIC:
                return TextFormatting.YELLOW;
            default:
                return TextFormatting.GRAY;
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String entityName = getEntityName(stack);
        if (entityName != null && !entityName.equals("未知生物")) {
            return entityName + "的生物样本";
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getEntityId(stack) != null;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        // 只在创造模式标签页中添加空样本
        if (this.isInCreativeTab(tab)) {
            items.add(new ItemStack(this));
        }
    }
}
