package com.moremod.util;

import com.moremod.init.ModBlocks;
import com.moremod.tile.TileEntityWisdomFountain;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
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

    /**
     * 智能合并两本附魔书 - 完整版
     */
    public static ItemStack mergeEnchantedBooks(ItemStack book1, ItemStack book2) {
        System.out.println("================== 开始合并附魔书 ==================");

        // 获取两本书的附魔
        NBTTagList enchants1 = ItemEnchantedBook.getEnchantments(book1);
        NBTTagList enchants2 = ItemEnchantedBook.getEnchantments(book2);

        if (enchants1 == null || enchants2 == null) {
            System.out.println("[错误] 无法获取附魔信息");
            return new ItemStack(Items.ENCHANTED_BOOK);
        }

        System.out.println("[合并] 书1 附魔数: " + enchants1.tagCount());
        System.out.println("[合并] 书2 附魔数: " + enchants2.tagCount());

        // 打印详细信息
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

        // 使用Map存储合并后的附魔
        Map<Integer, Integer> mergedEnchants = new HashMap<>();
        Map<Integer, Enchantment> enchantObjects = new HashMap<>();

        // 第一步：添加第一本书的所有附魔
        System.out.println("\n[处理第一本书]:");
        for (int i = 0; i < enchants1.tagCount(); i++) {
            NBTTagCompound tag = enchants1.getCompoundTagAt(i);
            short enchId = tag.getShort("id");
            short level = tag.getShort("lvl");
            Enchantment ench = Enchantment.getEnchantmentByID(enchId);

            if (ench != null) {
                mergedEnchants.put((int)enchId, (int)level);
                enchantObjects.put((int)enchId, ench);
                System.out.println("  添加: " + ench.getName() + " Lv" + level +
                        " (最大等级: " + ench.getMaxLevel() + ")");
            }
        }

        // 第二步：处理第二本书的附魔
        System.out.println("\n[处理第二本书]:");
        for (int i = 0; i < enchants2.tagCount(); i++) {
            NBTTagCompound tag = enchants2.getCompoundTagAt(i);
            short enchId = tag.getShort("id");
            short level = tag.getShort("lvl");
            Enchantment ench = Enchantment.getEnchantmentByID(enchId);

            if (ench == null) {
                System.out.println("  跳过无效附魔 ID:" + enchId);
                continue;
            }

            System.out.println("\n  处理: " + ench.getName() + " Lv" + level);

            // 检查是否与现有附魔冲突
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

            // 处理附魔合并
            Integer existingLevel = mergedEnchants.get((int)enchId);
            if (existingLevel != null) {
                System.out.println("    发现相同附魔!");
                System.out.println("    现有等级: " + existingLevel);
                System.out.println("    新书等级: " + level);
                System.out.println("    最大等级: " + ench.getMaxLevel());

                int newLevel;
                if (existingLevel == level) {
                    newLevel = Math.min(existingLevel + 1, ench.getMaxLevel());
                    System.out.println("    >>> 同级合并: Lv" + existingLevel +
                            " + Lv" + level + " = Lv" + newLevel);
                } else {
                    newLevel = Math.max(existingLevel, level);
                    System.out.println("    >>> 不同级，取较高: Lv" + newLevel);
                }

                mergedEnchants.put((int)enchId, newLevel);
                System.out.println("    最终等级: Lv" + newLevel);
            } else {
                mergedEnchants.put((int)enchId, (int)level);
                enchantObjects.put((int)enchId, ench);
                System.out.println("    新增附魔: " + ench.getName() + " Lv" + level);
            }
        }

        // 创建结果书
        ItemStack resultBook = new ItemStack(Items.ENCHANTED_BOOK);

        System.out.println("\n[创建结果书]:");
        System.out.println("总附魔数: " + mergedEnchants.size());

        for (Map.Entry<Integer, Integer> entry : mergedEnchants.entrySet()) {
            int enchId = entry.getKey();
            int finalLevel = entry.getValue();
            Enchantment ench = Enchantment.getEnchantmentByID(enchId);

            if (ench != null && finalLevel > 0) {
                ItemEnchantedBook.addEnchantment(resultBook,
                        new EnchantmentData(ench, finalLevel));
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

        System.out.println("================== 合并完成 ==================\n");

        return resultBook;
    }

    /**
     * 创建附魔书交易列表
     * ✅ 修改：无论输入的附魔书等级如何，村民只出售 Lv5 版本
     */
    public static MerchantRecipeList createEnchantedBookTrades(ItemStack enchantedBook) {
        MerchantRecipeList trades = new MerchantRecipeList();
        NBTTagList enchantments = ItemEnchantedBook.getEnchantments(enchantedBook);

        System.out.println("[交易] 创建附魔书交易，附魔数: " + enchantments.tagCount());

        // 为每个附魔创建独立的交易
        for (int i = 0; i < enchantments.tagCount(); i++) {
            NBTTagCompound enchTag = enchantments.getCompoundTagAt(i);
            short enchId = enchTag.getShort("id");
            short originalLevel = enchTag.getShort("lvl");

            Enchantment enchantment = Enchantment.getEnchantmentByID(enchId);
            if (enchantment == null) continue;

            // ========== ✅ 核心修改：始终创建 Lv5 的附魔书 ==========
            // 无论原始附魔书是什么等级，村民只出售 Lv5 版本
            short tradeLevel = 5;  // ✅ 固定为等级 5

            // 如果附魔的最大等级小于5，则使用最大等级
            int maxLevel = enchantment.getMaxLevel();
            if (maxLevel < 5) {
                tradeLevel = (short) maxLevel;
                System.out.println("[交易] " + enchantment.getName() +
                        " 最大等级为 " + maxLevel + "，使用最大等级");
            }

            // 创建 Lv5 (或最大等级) 的附魔书
            ItemStack singleEnchantBook = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantedBook.addEnchantment(singleEnchantBook,
                    new EnchantmentData(enchantment, tradeLevel));

            // 计算价格 - 基于 Lv5 的价格
            int emeraldCost = calculateEmeraldCost(enchId, tradeLevel);

            // 交易方式1：绿宝石 + 书 -> 附魔书 Lv5
            MerchantRecipe recipe1 = new MerchantRecipe(
                    new ItemStack(Items.EMERALD, emeraldCost),
                    new ItemStack(Items.BOOK, 1),
                    singleEnchantBook.copy(),
                    0, // 使用次数
                    5  // ✅ 最大使用次数改为5次
            );
            trades.add(recipe1);

            // 交易方式2：仅绿宝石（价格稍高）
            if (emeraldCost < 60) {
                MerchantRecipe recipe2 = new MerchantRecipe(
                        new ItemStack(Items.EMERALD, emeraldCost + 5),
                        ItemStack.EMPTY,
                        singleEnchantBook.copy(),
                        0,
                        5  // ✅ 最大使用次数改为5次
                );
                trades.add(recipe2);
            }

            System.out.println("[交易] 添加: " + enchantment.getName() +
                    " (原始 Lv" + originalLevel + " -> 出售 Lv" + tradeLevel + ")" +
                    " 价格: " + emeraldCost + " 绿宝石");
        }

        // 添加基础交易
        addBasicTrades(trades);

        System.out.println("[交易] 总交易数: " + trades.size());
        return trades;
    }

    /**
     * 添加基础交易
     */
    private static void addBasicTrades(MerchantRecipeList trades) {
        // 纸 -> 绿宝石
        trades.add(new MerchantRecipe(
                new ItemStack(Items.PAPER, 24),
                ItemStack.EMPTY,
                new ItemStack(Items.EMERALD, 1),
                0, 5  // ✅ 统一改为5次
        ));

        // 书 -> 绿宝石
        trades.add(new MerchantRecipe(
                new ItemStack(Items.BOOK, 8),
                ItemStack.EMPTY,
                new ItemStack(Items.EMERALD, 1),
                0, 5  // ✅ 统一改为5次
        ));

        // 绿宝石 -> 书架
        trades.add(new MerchantRecipe(
                new ItemStack(Items.EMERALD, 3),
                ItemStack.EMPTY,
                new ItemStack(Blocks.BOOKSHELF, 1),
                0, 5  // ✅ 统一改为5次
        ));

        // 绿宝石 -> 经验瓶
        trades.add(new MerchantRecipe(
                new ItemStack(Items.EMERALD, 5),
                ItemStack.EMPTY,
                new ItemStack(Items.EXPERIENCE_BOTTLE, 3),
                0, 5  // ✅ 统一改为5次
        ));
    }

    /**
     * 计算附魔书价格
     * ✅ 基于 Lv5 计算价格
     */
    public static int calculateEmeraldCost(int enchId, int level) {
        Enchantment ench = Enchantment.getEnchantmentByID(enchId);
        if (ench == null) return 5;

        // 基础价格根据稀有度
        int baseCost;
        switch (ench.getRarity()) {
            case COMMON:
                baseCost = 10;   // 普通附魔 Lv5 基础价格
                break;
            case UNCOMMON:
                baseCost = 20;   // 罕见附魔 Lv5 基础价格
                break;
            case RARE:
                baseCost = 30;   // 稀有附魔 Lv5 基础价格
                break;
            case VERY_RARE:
                baseCost = 40;   // 非常稀有 Lv5 基础价格
                break;
            default:
                baseCost = 15;
        }

        // 特殊附魔调整
        String enchName = ench.getName().toLowerCase();
        if (enchName.contains("mending")) {
            baseCost = 50; // 经验修补特殊价格
        } else if (enchName.contains("fortune") || enchName.contains("looting")) {
            baseCost += 15; // 时运/掠夺额外加价
        } else if (enchName.contains("silk")) {
            baseCost += 10; // 精准采集额外加价
        }

        // 等级加成（Lv5 的加成）
        int levelBonus = (level - 1) * 3;  // 每级+3绿宝石
        if (level == ench.getMaxLevel()) {
            levelBonus += 5; // 满级额外奖励
        }

        int finalCost = baseCost + levelBonus;

        // 限制价格范围（Lv5 的价格会更高）
        return Math.max(5, Math.min(64, finalCost));
    }

    /**
     * 查找附近激活的智慧之泉
     */
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

    /**
     * 检查村民是否已转化为智慧守护者
     */
    public static boolean isWisdomKeeper(EntityVillager villager) {
        NBTTagCompound data = villager.getEntityData();
        return data.getBoolean("WisdomKeeper");
    }

    /**
     * 标记村民为智慧守护者
     * ✅ 修改：显示名称中标注出售的是 Lv5 版本
     */
    public static void markAsWisdomKeeper(EntityVillager villager, NBTTagList enchantments) {
        NBTTagCompound data = villager.getEntityData();

        // 设置标记
        data.setBoolean("WisdomKeeper", true);

        // 存储附魔信息
        data.setTag("StoredEnchantments", enchantments.copy());

        // ✅ 设置显示名称 - 标注出售 Lv5 附魔
        String name = "§6智慧守护者";
        if (enchantments.tagCount() > 0) {
            name += " §7(" + enchantments.tagCount() + "种附魔 §dLv5§7)";  // ✅ 改为Lv5
        }
        villager.setCustomNameTag(name);
        villager.setAlwaysRenderNameTag(true);

        // 防止村民消失
        villager.enablePersistence();

        System.out.println("[标记] 村民已转化为智慧守护者（出售 Lv5 附魔）");
    }

    /**
     * 检查两个附魔是否冲突
     */
    public static boolean areEnchantmentsConflicting(Enchantment ench1, Enchantment ench2) {
        if (ench1 == null || ench2 == null) return false;
        if (Enchantment.getEnchantmentID(ench1) == Enchantment.getEnchantmentID(ench2)) return false;
        return !ench1.isCompatibleWith(ench2);
    }

    /**
     * 获取附魔的中文名称
     */
    public static String getEnchantmentDisplayName(Enchantment ench, int level) {
        if (ench == null) return "未知附魔";
        return ench.getTranslatedName(level);
    }

    /**
     * 验证附魔书是否有效
     */
    public static boolean isValidEnchantedBook(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Items.ENCHANTED_BOOK) {
            return false;
        }

        NBTTagList enchantments = ItemEnchantedBook.getEnchantments(stack);
        return enchantments != null && enchantments.tagCount() > 0;
    }

    /**
     * 获取附魔书的描述信息
     */
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