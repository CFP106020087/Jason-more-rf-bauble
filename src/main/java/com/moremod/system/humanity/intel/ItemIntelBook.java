package com.moremod.system.humanity.intel;

import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 情报书物品
 * Intel Book Item
 *
 * 高人性玩家通过学习情报书，获得对特定生物的永久伤害加成
 * 每本书提供 +10% (1.1x) 伤害，可无限叠加
 */
public class ItemIntelBook extends Item {

    private static final String NBT_ENTITY_ID = "EntityId";
    private static final String NBT_ENTITY_NAME = "EntityName";

    // 每本情报书提供的伤害加成 (10%)
    public static final float DAMAGE_BONUS_PER_BOOK = 0.10f;

    public ItemIntelBook() {
        setTranslationKey("intel_book");
        setRegistryName("intel_book");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
    }

    /**
     * 创建包含生物信息的情报书
     */
    public static ItemStack createBook(ResourceLocation entityId, String entityName) {
        ItemStack stack = new ItemStack(IntelSystemItems.INTEL_BOOK);

        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(NBT_ENTITY_ID, entityId.toString());
        tag.setString(NBT_ENTITY_NAME, entityName);
        stack.setTagCompound(tag);

        return stack;
    }

    /**
     * 从样本创建情报书
     */
    public static ItemStack createFromSample(ItemStack sample) {
        ResourceLocation entityId = ItemBiologicalSample.getEntityId(sample);
        String entityName = ItemBiologicalSample.getEntityName(sample);

        if (entityId == null) return ItemStack.EMPTY;

        return createBook(entityId, entityName);
    }

    /**
     * 获取情报书对应的生物ID
     */
    @Nullable
    public static ResourceLocation getEntityId(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return null;

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(NBT_ENTITY_ID)) return null;

        return new ResourceLocation(tag.getString(NBT_ENTITY_ID));
    }

    /**
     * 获取情报书对应的生物名称
     */
    public static String getEntityName(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return "未知生物";

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(NBT_ENTITY_NAME)) return "未知生物";

        return tag.getString(NBT_ENTITY_NAME);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);

        if (worldIn.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        ResourceLocation entityId = getEntityId(stack);
        if (entityId == null) {
            playerIn.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "这本情报书已损坏"), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        String entityName = getEntityName(stack);

        // 获取玩家的人性数据并学习情报
        IHumanityData data = HumanityCapabilityHandler.getData(playerIn);
        if (data == null) {
            playerIn.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "无法访问人性数据"), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        // 学习情报
        int currentLevel = IntelDataHelper.getIntelLevel(data, entityId);
        IntelDataHelper.incrementIntelLevel(data, entityId);
        int newLevel = currentLevel + 1;

        // 计算伤害加成
        float damageBonus = newLevel * DAMAGE_BONUS_PER_BOOK * 100f;

        // 消耗物品
        stack.shrink(1);

        // 播放学习音效
        worldIn.playSound(null, playerIn.posX, playerIn.posY, playerIn.posZ,
                SoundEvent.REGISTRY.getObject(new ResourceLocation("entity.player.levelup")),
                SoundCategory.PLAYERS, 1.0f, 1.2f);

        // 发送消息
        playerIn.sendStatusMessage(new TextComponentString(
                TextFormatting.GREEN + "学习成功! " +
                TextFormatting.AQUA + entityName + TextFormatting.GREEN + " 情报等级: " +
                TextFormatting.GOLD + newLevel +
                TextFormatting.GREEN + " (伤害加成: +" +
                TextFormatting.YELLOW + String.format("%.0f%%", damageBonus) +
                TextFormatting.GREEN + ")"
        ), false);

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        ResourceLocation entityId = getEntityId(stack);
        String entityName = getEntityName(stack);

        if (entityId != null) {
            tooltip.add(TextFormatting.GOLD + "情报目标: " + TextFormatting.WHITE + entityName);
            tooltip.add(TextFormatting.GRAY + "(" + entityId.toString() + ")");
            tooltip.add("");
            tooltip.add(TextFormatting.GREEN + "右键学习:");
            tooltip.add(TextFormatting.AQUA + "  永久获得对该生物 +" +
                    (int)(DAMAGE_BONUS_PER_BOOK * 100) + "% 伤害");
            tooltip.add(TextFormatting.GRAY + "  可与相同情报叠加");
        } else {
            tooltip.add(TextFormatting.RED + "损坏的情报书");
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String entityName = getEntityName(stack);
        if (entityName != null && !entityName.equals("未知生物")) {
            return entityName + "情报书";
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getEntityId(stack) != null;
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            items.add(new ItemStack(this));
        }
    }
}
