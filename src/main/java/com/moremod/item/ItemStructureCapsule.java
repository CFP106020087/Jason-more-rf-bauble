package com.moremod.item;

import com.moremod.entity.EntityThrownCapsule;
import com.moremod.structure.StructureData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 结构胶囊物品
 * 空状态扔出捕获结构，存储状态扔出释放结构
 * 附带物品存储功能：
 * - 小型(3×3×3)=32格
 * - 中型(7×7×7)=64格
 * - 大型(15×15×15)=64格
 * - 巨型(31×31×31)=128格
 * - 超巨型(63×63×63)=256格
 */
public class ItemStructureCapsule extends Item {

    public static final int STATE_EMPTY = 0;
    public static final int STATE_STORED = 1;

    // 捕获范围（奇数，以落点为中心）
    private final int captureSize;
    // 物品存储槽位数
    private final int inventorySize;

    public ItemStructureCapsule(String name, int size) {
        this.setTranslationKey(name);
        this.setRegistryName(name);
        this.setMaxStackSize(1);
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
        // 确保 size 是奇数
        this.captureSize = (size % 2 == 0) ? size + 1 : size;
        // 根据尺寸设置物品存储槽位数
        // 小型(3x3)=32格，中型(7x7)和大型(15x15)=64格
        // 巨型(31x31)=128格，超巨型(63x63)=256格
        if (size <= 3) {
            this.inventorySize = 32;
        } else if (size <= 15) {
            this.inventorySize = 64;
        } else if (size <= 31) {
            this.inventorySize = 128;
        } else {
            this.inventorySize = 256;
        }
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

    public int getInventorySize() {
        return inventorySize;
    }

    // ============== 物品存储管理 ==============

    /**
     * 获取存储的物品列表
     */
    public static List<ItemStack> getStoredItems(ItemStack stack) {
        List<ItemStack> items = new ArrayList<>();
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("inventory")) {
            return items;
        }
        NBTTagList invList = stack.getTagCompound().getTagList("inventory", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < invList.tagCount(); i++) {
            NBTTagCompound itemNBT = invList.getCompoundTagAt(i);
            ItemStack item = new ItemStack(itemNBT);
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }

    /**
     * 添加物品到存储（返回未能存入的物品）
     */
    public static ItemStack addItemToStorage(ItemStack capsule, ItemStack toAdd, int maxSlots) {
        if (toAdd.isEmpty()) return ItemStack.EMPTY;

        if (!capsule.hasTagCompound()) {
            capsule.setTagCompound(new NBTTagCompound());
        }

        NBTTagList invList;
        if (capsule.getTagCompound().hasKey("inventory")) {
            invList = capsule.getTagCompound().getTagList("inventory", Constants.NBT.TAG_COMPOUND);
        } else {
            invList = new NBTTagList();
        }

        // 先尝试堆叠到已有物品
        ItemStack remaining = toAdd.copy();
        for (int i = 0; i < invList.tagCount() && !remaining.isEmpty(); i++) {
            NBTTagCompound itemNBT = invList.getCompoundTagAt(i);
            ItemStack existing = new ItemStack(itemNBT);
            if (ItemStack.areItemsEqual(existing, remaining) && ItemStack.areItemStackTagsEqual(existing, remaining)) {
                int canAdd = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (canAdd > 0) {
                    existing.grow(canAdd);
                    remaining.shrink(canAdd);
                    invList.set(i, existing.writeToNBT(new NBTTagCompound()));
                }
            }
        }

        // 放入新槽位
        while (!remaining.isEmpty() && invList.tagCount() < maxSlots) {
            ItemStack toStore = remaining.splitStack(Math.min(remaining.getCount(), remaining.getMaxStackSize()));
            invList.appendTag(toStore.writeToNBT(new NBTTagCompound()));
        }

        capsule.getTagCompound().setTag("inventory", invList);
        return remaining;
    }

    /**
     * 清空存储的物品
     */
    public static void clearStoredItems(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag("inventory");
        }
    }

    /**
     * 获取存储的物品数量
     */
    public static int getStoredItemCount(ItemStack stack) {
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("inventory")) {
            return 0;
        }
        return stack.getTagCompound().getTagList("inventory", Constants.NBT.TAG_COMPOUND).tagCount();
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
        tooltip.add(TextFormatting.AQUA + "物品存储: " + inventorySize + " 格");

        if (hasStructure(stack)) {
            int blockCount = stack.getTagCompound().getInteger("blockCount");
            tooltip.add(TextFormatting.GREEN + "已存储 " + blockCount + " 个方块");
        } else {
            tooltip.add(TextFormatting.YELLOW + "空 - 投掷以捕获结构");
        }

        int itemCount = getStoredItemCount(stack);
        if (itemCount > 0) {
            tooltip.add(TextFormatting.BLUE + "存储物品: " + itemCount + " 种");
        }

        tooltip.add("");
        if (com.moremod.config.CapsuleConfig.singleUse) {
            tooltip.add(TextFormatting.RED + "⚠ 一次性：释放后胶囊消耗");
        }
        tooltip.add(TextFormatting.DARK_GRAY + "右键投掷");
        tooltip.add(TextFormatting.DARK_GRAY + "无法捕获/覆盖基岩等不可破坏方块");
        tooltip.add(TextFormatting.DARK_GRAY + "方块上限: " + com.moremod.config.CapsuleConfig.maxBlockCount);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return hasStructure(stack);
    }
}