package com.moremod.util;

import com.moremod.init.ModBlocks;
import com.moremod.tile.TileEntityWisdomFountain;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class WisdomFountainHelper {

    // “无限”交易次数的上限（避免太夸张的溢出，给一个非常大的值就好）
    private static final int UNLIMITED_TRADE_USES = 999999;

    /**
     * 低层工具：直接往附魔书写入 NBT（不做等级上限裁剪）
     * - 不使用 EnchantmentData.addEnchantment 的封装逻辑，避免任何潜在的 clamp
     */
    public static void addStoredEnchantmentRaw(ItemStack book, int enchId, int level) {
        if (book.isEmpty() || book.getItem() != Items.ENCHANTED_BOOK) return;
        if (level <= 0) return;

        NBTTagList list = ItemEnchantedBook.getEnchantments(book); // 读取已有 StoredEnchantments
        boolean found = false;

        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            if (tag.getShort("id") == (short) enchId) {
                // 同 ID，直接覆盖为新的等级（支持破限）
                tag.setShort("lvl", (short) level);
                found = true;
                break;
            }
        }

        if (!found) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setShort("id", (short) enchId);
            tag.setShort("lvl", (short) level);
            list.appendTag(tag);
        }

        if (!book.hasTagCompound()) {
            book.setTagCompound(new NBTTagCompound());
        }
        book.getTagCompound().setTag("StoredEnchantments", list);
    }

    // ------------------------------------------------------------------------
    //  1. 附魔书合并：改为支持破限，不再被 getMaxLevel() 卡死
    // ------------------------------------------------------------------------

    /**
     * 智能合并两本附魔书 - 破限版
     */
    public static ItemStack mergeEnchantedBooks(ItemStack book1, ItemStack book2) {
        System.out.println("================== 开始合并附魔书（破限版） ==================");

        NBTTagList enchants1 = ItemEnchantedBook.getEnchantments(book1);
        NBTTagList enchants2 = ItemEnchantedBook.getEnchantments(book2);

        if (enchants1 == null || enchants2 == null) {
            System.out.println("[错误] 无法获取附魔信息");
            return new ItemStack(Items.ENCHANTED_BOOK);
        }

        System.out.println("[合并] 书1 附魔数: " + enchants1.tagCount());
        System.out.println("[合并] 书2 附魔数: " + enchants2.tagCount());

        // 打印详情（调试用）
        System.out.println("\n[书1详情]:");
        for (int i = 0; i < enchants1.tagCount(); i++) {
            NBTTagCompound tag = enchants1.getCompoundTagAt(i);
            short id = tag.getShort("id");
            short lvl = tag.getShort("lvl");
            Enchantment ench = Enchantment.getEnchantmentByID(id);
            System.out.println("  - ID:" + id + " " +
                    (ench != null ? ench.getName() : "未知") + " Lv" + lvl);
        }

        System.out.println("\n[书2详情]:");
        for (int i = 0; i < enchants2.tagCount(); i++) {
            NBTTagCompound tag = enchants2.getCompoundTagAt(i);
            short id = tag.getShort("id");
            short lvl = tag.getShort("lvl");
            Enchantment ench = Enchantment.getEnchantmentByID(id);
            System.out.println("  - ID:" + id + " " +
                    (ench != null ? ench.getName() : "未知") + " Lv" + lvl);
        }

        // 使用 Map 存储合并结果（支持任意等级）
        Map<Integer, Integer> mergedEnchants = new HashMap<>();
        Map<Integer, Enchantment> enchantObjects = new HashMap<>();

        // 先加入书1
        System.out.println("\n[处理第一本书]:");
        for (int i = 0; i < enchants1.tagCount(); i++) {
            NBTTagCompound tag = enchants1.getCompoundTagAt(i);
            short enchId = tag.getShort("id");
            short level = tag.getShort("lvl");
            Enchantment ench = Enchantment.getEnchantmentByID(enchId);

            if (ench != null && level > 0) {
                mergedEnchants.put((int) enchId, (int) level);
                enchantObjects.put((int) enchId, ench);
                System.out.println("  添加: " + ench.getName() + " Lv" + level);
            }
        }

        // 再处理书2
        System.out.println("\n[处理第二本书]:");
        for (int i = 0; i < enchants2.tagCount(); i++) {
            NBTTagCompound tag = enchants2.getCompoundTagAt(i);
            short enchId = tag.getShort("id");
            short level = tag.getShort("lvl");
            Enchantment ench = Enchantment.getEnchantmentByID(enchId);

            if (ench == null || level <= 0) {
                System.out.println("  跳过无效附魔 ID:" + enchId + " Lv:" + level);
                continue;
            }

            System.out.println("\n  处理: " + ench.getName() + " Lv" + level);

            // 冲突检查（保持原逻辑）
            boolean hasConflict = false;
            for (Map.Entry<Integer, Enchantment> entry : enchantObjects.entrySet()) {
                Enchantment existingEnch = entry.getValue();
                if (Enchantment.getEnchantmentID(existingEnch) == Enchantment.getEnchantmentID(ench)) {
                    continue;
                }
                if (!ench.isCompatibleWith(existingEnch)) {
                    System.out.println("    冲突: " + ench.getName() + " vs " + existingEnch.getName());
                    hasConflict = true;
                    break;
                }
            }
            if (hasConflict) {
                System.out.println("    跳过冲突附魔");
                continue;
            }

            // 合并逻辑：不再使用 getMaxLevel() 限制，支持破限
            Integer existingLevel = mergedEnchants.get((int) enchId);
            if (existingLevel != null) {
                System.out.println("    发现相同附魔!");
                System.out.println("    现有等级: " + existingLevel);
                System.out.println("    新书等级: " + level);

                int newLevel;
                if (existingLevel == level) {
                    // 同级 → 直接 +1，不做上限限制
                    newLevel = existingLevel + 1;
                    System.out.println("    >>> 同级合并: Lv" + existingLevel +
                            " + Lv" + level + " = Lv" + newLevel);
                } else {
                    // 不同等级 → 取更高的那个
                    newLevel = Math.max(existingLevel, level);
                    System.out.println("    >>> 不同级，取较高: Lv" + newLevel);
                }

                mergedEnchants.put((int) enchId, newLevel);
                System.out.println("    最终等级: Lv" + newLevel);
            } else {
                mergedEnchants.put((int) enchId, (int) level);
                enchantObjects.put((int) enchId, ench);
                System.out.println("    新增附魔: " + ench.getName() + " Lv" + level);
            }
        }

        // 生成结果书（使用 Raw NBT 写入，避免任何 clamp）
        ItemStack resultBook = new ItemStack(Items.ENCHANTED_BOOK);

        System.out.println("\n[创建结果书]:");
        System.out.println("总附魔数: " + mergedEnchants.size());

        for (Map.Entry<Integer, Integer> entry : mergedEnchants.entrySet()) {
            int enchId = entry.getKey();
            int finalLevel = entry.getValue();
            Enchantment ench = Enchantment.getEnchantmentByID(enchId);

            if (ench != null && finalLevel > 0) {
                addStoredEnchantmentRaw(resultBook, enchId, finalLevel);
                System.out.println("  添加到结果: " + ench.getName() + " Lv" + finalLevel);
            }
        }

        // 验证结果
        System.out.println("\n[验证结果]:");
        NBTTagList resultEnchants = ItemEnchantedBook.getEnchantments(resultBook);
        System.out.println("结果书附魔数: " + resultEnchants.tagCount());
        for (int i = 0; i < resultEnchants.tagCount(); i++) {
            NBTTagCompound tag = resultEnchants.getCompoundTagAt(i);
            short id = tag.getShort("id");
            short lvl = tag.getShort("lvl");
            Enchantment ench = Enchantment.getEnchantmentByID(id);
            System.out.println("  - " + (ench != null ? ench.getName() : "ID:" + id) + " Lv" + lvl);
        }

        System.out.println("================== 合并完成（破限版） ==================\n");

        return resultBook;
    }

    // ------------------------------------------------------------------------
    //  2. 村民交易：支持破限等级 + 无限次数
    // ------------------------------------------------------------------------

    /**
     * 从一本“样本附魔书”创建交易列表
     * - 不再强制变成 Lv5
     * - 直接使用样本书上记录的等级（可以是破限）
     */
    public static MerchantRecipeList createEnchantedBookTrades(ItemStack enchantedBook) {
        NBTTagList enchantments = ItemEnchantedBook.getEnchantments(enchantedBook);
        return createEnchantedBookTradesFromStoredList(enchantments);
    }

    /**
     * 从 NBTTagList（通常来自 StoredEnchantments）生成交易列表
     * - 这是智慧守护者“可学习新附魔”时会调用的版本
     */
    public static MerchantRecipeList createEnchantedBookTradesFromStoredList(NBTTagList enchantments) {
        MerchantRecipeList trades = new MerchantRecipeList();

        if (enchantments == null) {
            System.out.println("[交易] 无附魔信息，无法创建交易");
            addBasicTrades(trades);
            return trades;
        }

        System.out.println("[交易] 创建附魔书交易（破限版），附魔数: " + enchantments.tagCount());

        for (int i = 0; i < enchantments.tagCount(); i++) {
            NBTTagCompound enchTag = enchantments.getCompoundTagAt(i);
            short enchId = enchTag.getShort("id");
            short level = enchTag.getShort("lvl");

            Enchantment enchantment = Enchantment.getEnchantmentByID(enchId);
            if (enchantment == null || level <= 0) continue;

            int tradeLevel = level; // 🚀 直接使用原始等级（支持破限）
            String enchName = enchantment.getName();

            // 创建只带一个附魔的书（Raw NBT 写入）
            ItemStack singleEnchantBook = new ItemStack(Items.ENCHANTED_BOOK);
            addStoredEnchantmentRaw(singleEnchantBook, enchId, tradeLevel);

            // 基于“真正等级”计算价格（内部会再 clamp 到 64）
            int emeraldCost = calculateEmeraldCost(enchId, tradeLevel);

            // 交易1：绿宝石 + 书 → 附魔书（无限次）
            MerchantRecipe recipe1 = new MerchantRecipe(
                    new ItemStack(Items.EMERALD, emeraldCost),
                    new ItemStack(Items.BOOK, 1),
                    singleEnchantBook.copy(),
                    0,
                    UNLIMITED_TRADE_USES
            );
            trades.add(recipe1);

            // 交易2：仅绿宝石（价格略高）
            if (emeraldCost < 60) {
                MerchantRecipe recipe2 = new MerchantRecipe(
                        new ItemStack(Items.EMERALD, emeraldCost + 5),
                        ItemStack.EMPTY,
                        singleEnchantBook.copy(),
                        0,
                        UNLIMITED_TRADE_USES
                );
                trades.add(recipe2);
            }

            System.out.println("[交易] 添加: " + enchName +
                    " Lv" + tradeLevel +
                    " 价格: " + emeraldCost + " 绿宝石");
        }

        // 添加基础交易
        addBasicTrades(trades);

        System.out.println("[交易] 总交易数: " + trades.size());
        return trades;
    }

    /**
     * 添加基础交易（同样改为“几乎无限次”）
     */
    private static void addBasicTrades(MerchantRecipeList trades) {
        // 纸 -> 绿宝石
        trades.add(new MerchantRecipe(
                new ItemStack(Items.PAPER, 24),
                ItemStack.EMPTY,
                new ItemStack(Items.EMERALD, 1),
                0, UNLIMITED_TRADE_USES
        ));

        // 书 -> 绿宝石
        trades.add(new MerchantRecipe(
                new ItemStack(Items.BOOK, 8),
                ItemStack.EMPTY,
                new ItemStack(Items.EMERALD, 1),
                0, UNLIMITED_TRADE_USES
        ));

        // 绿宝石 -> 书架
        trades.add(new MerchantRecipe(
                new ItemStack(Items.EMERALD, 3),
                ItemStack.EMPTY,
                new ItemStack(Blocks.BOOKSHELF, 1),
                0, UNLIMITED_TRADE_USES
        ));

        // 绿宝石 -> 经验瓶
        trades.add(new MerchantRecipe(
                new ItemStack(Items.EMERALD, 5),
                ItemStack.EMPTY,
                new ItemStack(Items.EXPERIENCE_BOTTLE, 3),
                0, UNLIMITED_TRADE_USES
        ));
    }

    /**
     * 计算附魔书价格
     * - 现在真正根据“传入的 level”计算（可以是破限）
     * - 但最终还是 clamp 到 [5, 64]，避免太离谱
     */
    public static int calculateEmeraldCost(int enchId, int level) {
        Enchantment ench = Enchantment.getEnchantmentByID(enchId);
        if (ench == null) return 5;

        if (level <= 0) level = 1;

        int baseCost;
        switch (ench.getRarity()) {
            case COMMON:
                baseCost = 10;
                break;
            case UNCOMMON:
                baseCost = 20;
                break;
            case RARE:
                baseCost = 30;
                break;
            case VERY_RARE:
                baseCost = 40;
                break;
            default:
                baseCost = 15;
        }

        String enchName = ench.getName().toLowerCase();
        if (enchName.contains("mending")) {
            baseCost = 50;
        } else if (enchName.contains("fortune") || enchName.contains("looting")) {
            baseCost += 15;
        } else if (enchName.contains("silk")) {
            baseCost += 10;
        }

        // 等级加成：基础线性 + 破限部分
        int maxLevel = ench.getMaxLevel();
        int levelBonus = (level - 1) * 3;

        if (level == maxLevel) {
            levelBonus += 5; // 满级奖励
        } else if (level > maxLevel) {
            // 超出原版上限的额外加价（但最后仍然封顶 64）
            levelBonus += (level - maxLevel) * 2;
        }

        int finalCost = baseCost + levelBonus;
        return Math.max(5, Math.min(64, finalCost));
    }

    // ------------------------------------------------------------------------
    //  3. 智慧之泉查找 & WisdomKeeper 标记等辅助逻辑
    // ------------------------------------------------------------------------

    public static TileEntityWisdomFountain findActiveNearbyFountain(World world, BlockPos pos, int range) {
        for (int x = -range; x <= range; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = pos.add(x, y, z);

                    if (world.getBlockState(checkPos).getBlock() == ModBlocks.WISDOM_FOUNTAIN_CORE) {
                        TileEntity te = world.getTileEntity(checkPos);

                        if (te instanceof TileEntityWisdomFountain) {
                            TileEntityWisdomFountain fountain = (TileEntityWisdomFountain) te;

                            if (fountain.isFormed()) {
                                double distance = Math.sqrt(pos.distanceSq(checkPos));
                                if (distance <= range) {
                                    System.out.println("[查找] 找到激活的智慧之泉，距离: " +
                                            String.format("%.1f", distance));
                                    return fountain;
                                }
                            }
                        }
                    }
                }
            }
        }

        System.out.println("[查找] 未找到激活的智慧之泉");
        return null;
    }

    public static boolean isWisdomKeeper(EntityVillager villager) {
        NBTTagCompound data = villager.getEntityData();
        return data.getBoolean("WisdomKeeper");
    }

    /**
     * 标记村民为智慧守护者
     * - 现在不再写死 Lv5 文案，而是标注“可破限”
     */
    public static void markAsWisdomKeeper(EntityVillager villager, NBTTagList enchantments) {
        NBTTagCompound data = villager.getEntityData();

        data.setBoolean("WisdomKeeper", true);
        data.setTag("StoredEnchantments", enchantments.copy());

        String name = "§6智慧守护者";
        if (enchantments.tagCount() > 0) {
            name += " §7(" + enchantments.tagCount() + "种附魔§7, §d可破限§7)";
        }
        villager.setCustomNameTag(name);
        villager.setAlwaysRenderNameTag(true);
        villager.enablePersistence();

        System.out.println("[标记] 村民已转化为智慧守护者（可出售破限附魔）");
    }

    public static boolean areEnchantmentsConflicting(Enchantment ench1, Enchantment ench2) {
        if (ench1 == null || ench2 == null) return false;
        if (Enchantment.getEnchantmentID(ench1) == Enchantment.getEnchantmentID(ench2)) return false;
        return !ench1.isCompatibleWith(ench2);
    }

    public static String getEnchantmentDisplayName(Enchantment ench, int level) {
        if (ench == null) return "未知附魔";
        return ench.getTranslatedName(level);
    }

    public static boolean isValidEnchantedBook(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Items.ENCHANTED_BOOK) {
            return false;
        }

        NBTTagList enchantments = ItemEnchantedBook.getEnchantments(stack);
        return enchantments != null && enchantments.tagCount() > 0;
    }

    public static String getEnchantedBookDescription(ItemStack book) {
        if (!isValidEnchantedBook(book)) {
            return "无效的附魔书";
        }

        NBTTagList enchantments = ItemEnchantedBook.getEnchantments(book);
        StringBuilder desc = new StringBuilder();
        desc.append("附魔书 (").append(enchantments.tagCount()).append("个附魔):\n");

        for (int i = 0; i < enchantments.tagCount(); i++) {
            NBTTagCompound tag = enchantments.getCompoundTagAt(i);
            short enchId = tag.getShort("id");
            short level = tag.getShort("lvl");
            Enchantment ench = Enchantment.getEnchantmentByID(enchId);

            if (ench != null) {
                desc.append("  - ").append(ench.getTranslatedName(level)).append("\n");
            }
        }

        return desc.toString();
    }
}
