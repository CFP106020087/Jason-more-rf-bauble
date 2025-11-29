package com.moremod.system.humanity.intel;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
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
        ItemStack stack = new ItemStack(IntelSystemItems.BIOLOGICAL_SAMPLE);

        ResourceLocation entityId = EntityList.getKey(entity);
        if (entityId == null) return ItemStack.EMPTY;

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
            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "可用于合成情报书");
            tooltip.add(TextFormatting.GRAY + "配方: 样本 + 书 + 经验瓶");
        } else {
            tooltip.add(TextFormatting.RED + "无效样本");
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
