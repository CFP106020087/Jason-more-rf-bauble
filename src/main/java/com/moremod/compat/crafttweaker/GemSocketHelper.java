package com.moremod.compat.crafttweaker;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

/**
 * 宝石镶嵌工具类
 *
 * ✅ 修复：平均品质计算过滤布尔词条
 */
public class GemSocketHelper {

    // 默认最大镶嵌数（其他类可引用此常量）
    public static final int MAX_SOCKETS = 6;

    // 实际使用的最大镶嵌数（可通过CraftTweaker配置）
    private static int customMaxSockets = MAX_SOCKETS;

    private static final String TAG_SOCKETED_GEMS = "SocketedGems";

    // ==========================================
    // 镶嵌功能
    // ==========================================

    /**
     * 将宝石镶嵌到物品上
     *
     * @param item 要镶嵌的物品（武器/装备）
     * @param gem 宝石物品（必须是已鉴定的宝石）
     * @return 是否镶嵌成功
     */
    public static boolean socketGem(ItemStack item, ItemStack gem) {
        if (item.isEmpty() || gem.isEmpty()) {
            return false;
        }

        // 检查是否是宝石
        if (!GemNBTHelper.isGem(gem)) {
            return false;
        }

        // 检查宝石是否已鉴定
        if (GemNBTHelper.isUnidentified(gem)) {
            return false;
        }

        // 检查是否已达到最大镶嵌数
        if (getSocketedGemCount(item) >= getMaxSockets()) {
            return false;
        }

        // 获取或创建NBT
        NBTTagCompound itemTag = item.getTagCompound();
        if (itemTag == null) {
            itemTag = new NBTTagCompound();
            item.setTagCompound(itemTag);
        }

        // 获取或创建镶嵌列表
        NBTTagList gemList;
        if (itemTag.hasKey(TAG_SOCKETED_GEMS)) {
            gemList = itemTag.getTagList(TAG_SOCKETED_GEMS, Constants.NBT.TAG_COMPOUND);
        } else {
            gemList = new NBTTagList();
        }

        // 复制宝石的NBT数据
        NBTTagCompound gemCopy = gem.writeToNBT(new NBTTagCompound());
        gemList.appendTag(gemCopy);

        // 保存回物品
        itemTag.setTag(TAG_SOCKETED_GEMS, gemList);

        return true;
    }

    /**
     * 批量镶嵌宝石
     */
    public static int socketGems(ItemStack item, ItemStack... gems) {
        int count = 0;
        for (ItemStack gem : gems) {
            if (socketGem(item, gem)) {
                count++;
            }
        }
        return count;
    }

    // ==========================================
    // 移除功能
    // ==========================================

    /**
     * 移除指定位置的宝石
     *
     * @param item 物品
     * @param index 索引（0开始）
     * @return 被移除的宝石（如果成功）
     */
    public static ItemStack removeGem(ItemStack item, int index) {
        if (item.isEmpty() || !item.hasTagCompound()) {
            return ItemStack.EMPTY;
        }

        NBTTagCompound itemTag = item.getTagCompound();
        if (!itemTag.hasKey(TAG_SOCKETED_GEMS)) {
            return ItemStack.EMPTY;
        }

        NBTTagList gemList = itemTag.getTagList(TAG_SOCKETED_GEMS, Constants.NBT.TAG_COMPOUND);

        if (index < 0 || index >= gemList.tagCount()) {
            return ItemStack.EMPTY;
        }

        // 获取宝石数据
        NBTTagCompound gemTag = gemList.getCompoundTagAt(index);
        ItemStack removedGem = new ItemStack(gemTag);

        // 移除该宝石
        NBTTagList newList = new NBTTagList();
        for (int i = 0; i < gemList.tagCount(); i++) {
            if (i != index) {
                newList.appendTag(gemList.get(i));
            }
        }

        // 更新或删除标签
        if (newList.tagCount() > 0) {
            itemTag.setTag(TAG_SOCKETED_GEMS, newList);
        } else {
            itemTag.removeTag(TAG_SOCKETED_GEMS);
        }

        return removedGem;
    }

    /**
     * 移除所有宝石
     */
    public static ItemStack[] removeAllGems(ItemStack item) {
        int count = getSocketedGemCount(item);
        if (count == 0) {
            return new ItemStack[0];
        }

        ItemStack[] removed = new ItemStack[count];
        for (int i = count - 1; i >= 0; i--) {
            removed[i] = removeGem(item, i);
        }

        return removed;
    }

    // ==========================================
    // 查询功能
    // ==========================================

    /**
     * 获取已镶嵌的宝石数量
     */
    public static int getSocketedGemCount(ItemStack item) {
        if (item.isEmpty() || !item.hasTagCompound()) {
            return 0;
        }

        NBTTagCompound itemTag = item.getTagCompound();
        if (!itemTag.hasKey(TAG_SOCKETED_GEMS)) {
            return 0;
        }

        return itemTag.getTagList(TAG_SOCKETED_GEMS, Constants.NBT.TAG_COMPOUND).tagCount();
    }

    /**
     * 获取剩余可镶嵌数量
     */
    public static int getRemainingSocketCount(ItemStack item) {
        return getMaxSockets() - getSocketedGemCount(item);
    }

    /**
     * 检查是否可以镶嵌更多宝石
     */
    public static boolean canSocketMore(ItemStack item) {
        return getSocketedGemCount(item) < getMaxSockets();
    }

    /**
     * 检查物品是否有镶嵌的宝石
     */
    public static boolean hasSocketedGems(ItemStack item) {
        return getSocketedGemCount(item) > 0;
    }

    /**
     * 获取指定位置的宝石
     */
    public static ItemStack getSocketedGem(ItemStack item, int index) {
        if (item.isEmpty() || !item.hasTagCompound()) {
            return ItemStack.EMPTY;
        }

        NBTTagCompound itemTag = item.getTagCompound();
        if (!itemTag.hasKey(TAG_SOCKETED_GEMS)) {
            return ItemStack.EMPTY;
        }

        NBTTagList gemList = itemTag.getTagList(TAG_SOCKETED_GEMS, Constants.NBT.TAG_COMPOUND);

        if (index < 0 || index >= gemList.tagCount()) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(gemList.getCompoundTagAt(index));
    }

    /**
     * 获取所有镶嵌的宝石
     */
    public static ItemStack[] getAllSocketedGems(ItemStack item) {
        int count = getSocketedGemCount(item);
        if (count == 0) {
            return new ItemStack[0];
        }

        ItemStack[] gems = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            gems[i] = getSocketedGem(item, i);
        }

        return gems;
    }

    // ==========================================
    // 统计功能
    // ==========================================

    /**
     * ✅ 计算所有镶嵌宝石的平均品质（过滤布尔词条）
     *
     * 修复：只计算非布尔词条（5%-95%）的品质
     * 全是布尔词条的宝石不计入平均
     */
    public static int getAverageGemQuality(ItemStack item) {
        ItemStack[] gems = getAllSocketedGems(item);
        if (gems.length == 0) {
            return 0;
        }

        int totalQuality = 0;
        int validGemCount = 0;

        for (ItemStack gem : gems) {
            if (GemNBTHelper.isIdentified(gem)) {
                java.util.List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
                if (!affixes.isEmpty()) {
                    // ✅ 只计算非布尔词条的品质
                    int numericQualitySum = 0;
                    int numericCount = 0;

                    for (IdentifiedAffix affix : affixes) {
                        int quality = affix.getQuality();

                        // 过滤布尔词条（0%或100%）
                        if (quality > 5 && quality < 95) {
                            numericQualitySum += quality;
                            numericCount++;
                        }
                    }

                    // 只有存在非布尔词条时才计入平均
                    if (numericCount > 0) {
                        int gemQuality = numericQualitySum / numericCount;
                        totalQuality += gemQuality;
                        validGemCount++;
                    }
                    // 如果全是布尔词条，这个宝石不计入平均
                }
            }
        }

        return validGemCount > 0 ? totalQuality / validGemCount : 0;
    }

    /**
     * 获取所有宝石的总词条数
     */
    public static int getTotalAffixCount(ItemStack item) {
        ItemStack[] gems = getAllSocketedGems(item);
        int total = 0;

        for (ItemStack gem : gems) {
            if (GemNBTHelper.isIdentified(gem)) {
                total += GemNBTHelper.getAffixes(gem).size();
            }
        }

        return total;
    }

    // ==========================================
    // 配置
    // ==========================================

    /**
     * 设置最大镶嵌数量（CraftTweaker使用）
     * 最大值限制为6
     */
    public static void setMaxSockets(int max) {
        if (max > 0 && max <= MAX_SOCKETS) {
            customMaxSockets = max;
        }
    }

    public static int getMaxSockets() {
        return customMaxSockets;
    }
}
