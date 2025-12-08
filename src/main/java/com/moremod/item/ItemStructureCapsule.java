package com.moremod.item;

import com.moremod.entity.EntityThrownCapsule;
import com.moremod.structure.StructureData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 结构胶囊物品
 * 空状态扔出捕获结构，存储状态扔出释放结构
 */
public class ItemStructureCapsule extends Item {

    public static final int STATE_EMPTY = 0;
    public static final int STATE_STORED = 1;

    // 捕获范围（奇数，以落点为中心）
    private final int captureSize;

    public ItemStructureCapsule(String name, int size) {
        this.setTranslationKey(name);
        this.setRegistryName(name);
        this.setMaxStackSize(1);
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
        // 添加到 MoreMod 的创造模式物品栏
        // 确保 size 是奇数
        this.captureSize = (size % 2 == 0) ? size + 1 : size;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            // 创建投掷实体
            EntityThrownCapsule entity = new EntityThrownCapsule(world, player, stack.copy());
            entity.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, 1.5F, 0.5F);
            world.spawnEntity(entity);

            // 消耗物品
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    // ============== 状态管理 ==============

    public static int getState(ItemStack stack) {
        return stack.getItemDamage();
    }

    public static void setState(ItemStack stack, int state) {
        stack.setItemDamage(state);
    }

    public static boolean isEmpty(ItemStack stack) {
        return getState(stack) == STATE_EMPTY;
    }

    public static boolean hasStructure(ItemStack stack) {
        return getState(stack) == STATE_STORED && stack.hasTagCompound() 
               && stack.getTagCompound().hasKey("structure");
    }

    // ============== 结构数据存取 ==============

    public static void saveStructure(ItemStack stack, StructureData data) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound structureNBT = new NBTTagCompound();
        data.writeToNBT(structureNBT);
        stack.getTagCompound().setTag("structure", structureNBT);
        stack.getTagCompound().setInteger("blockCount", data.getBlockCount());
        setState(stack, STATE_STORED);
    }

    public static StructureData loadStructure(ItemStack stack) {
        if (!hasStructure(stack)) {
            return null;
        }
        NBTTagCompound structureNBT = stack.getTagCompound().getCompoundTag("structure");
        StructureData data = new StructureData();
        data.readFromNBT(structureNBT);
        return data;
    }

    public static void clearStructure(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag("structure");
            stack.getTagCompound().removeTag("blockCount");
        }
        setState(stack, STATE_EMPTY);
    }

    // ============== 属性获取 ==============

    public int getCaptureSize() {
        return captureSize;
    }

    public static String getLabel(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("label")) {
            return stack.getTagCompound().getString("label");
        }
        return null;
    }

    public static void setLabel(ItemStack stack, String label) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setString("label", label);
    }

    // ============== 显示相关 ==============

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String baseName = super.getItemStackDisplayName(stack);
        String label = getLabel(stack);

        if (isEmpty(stack)) {
            return baseName + " (空)";
        } else {
            String displayLabel = (label != null && !label.isEmpty()) 
                ? "«" + label + "»" 
                : "(已存储)";
            return baseName + " " + displayLabel;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        int size = getCaptureSize();
        tooltip.add(TextFormatting.GRAY + "捕获范围: " + size + "×" + size + "×" + size);

        if (hasStructure(stack)) {
            int blockCount = stack.getTagCompound().getInteger("blockCount");
            tooltip.add(TextFormatting.GREEN + "已存储 " + blockCount + " 个方块");
        } else {
            tooltip.add(TextFormatting.YELLOW + "空 - 投掷以捕获结构");
        }

        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "右键投掷");
        tooltip.add(TextFormatting.DARK_GRAY + "无法捕获基岩等不可破坏方块");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return hasStructure(stack);
    }
}