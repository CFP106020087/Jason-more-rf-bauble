package com.moremod.eventHandler;

import com.moremod.tile.TileEntityWisdomFountain;
import com.moremod.util.WisdomFountainHelper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.Random;

public class WisdomFountainEventHandler {
    private Random rand = new Random();

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote) return;
        if (!(event.getTarget() instanceof EntityVillager)) return;

        EntityPlayer player = event.getEntityPlayer();
        EntityVillager villager = (EntityVillager) event.getTarget();

        // 必须 Shift+右键
        if (!player.isSneaking()) return;

        ItemStack mainHand = player.getHeldItem(EnumHand.MAIN_HAND);
        ItemStack offHand = player.getHeldItem(EnumHand.OFF_HAND);

        // ================= 已是智慧守护者 =================
        if (WisdomFountainHelper.isWisdomKeeper(villager)) {

            // 功能1：合并两本附魔书（支持破限）
            if (mainHand.getItem() == Items.ENCHANTED_BOOK &&
                    offHand.getItem() == Items.ENCHANTED_BOOK) {

                handleBookMerging(player, villager, mainHand, offHand);
                event.setCanceled(true);
                return;
            }

            // 功能2：为智慧守护者“学习”新的附魔（主手附魔书，副手空）
            if (mainHand.getItem() == Items.ENCHANTED_BOOK && offHand.isEmpty()) {
                handleAddNewTrades(player, villager, mainHand);
                event.setCanceled(true);
                return;
            }

            // 功能3：查询村民信息（双手空）
            if (mainHand.isEmpty() && offHand.isEmpty()) {
                showVillagerInfo(player, villager);
                event.setCanceled(true);
                return;
            }

            return;
        }

        // ================= 尚未转化为智慧守护者 =================

        if (mainHand.getItem() != Items.ENCHANTED_BOOK) return;

        // 必须在激活的智慧之泉附近
        TileEntityWisdomFountain fountain = WisdomFountainHelper.findActiveNearbyFountain(
                villager.world, villager.getPosition(), 10
        );

        if (fountain == null) {
            player.sendMessage(new TextComponentString("§c需要在激活的智慧之泉附近（10格内）！"));
            return;
        }

        // 执行转化
        transformVillager(villager, mainHand, player);

        // 消耗附魔书
        if (!player.isCreative()) {
            mainHand.shrink(1);
        }

        // 播放效果
        playTransformEffects(villager);

        NBTTagList enchantments = ItemEnchantedBook.getEnchantments(mainHand);
        player.sendMessage(new TextComponentString("§a成功将村民转化为智慧守护者！"));
        player.sendMessage(new TextComponentString("§e该村民现在出售 §6" +
                enchantments.tagCount() + " §e种附魔（可破限等级）"));

        event.setCanceled(true);
    }

    // ------------------------------------------------------------------------
    //  1. 附魔书合并（调用破限版 Helper）
    // ------------------------------------------------------------------------
    private void handleBookMerging(EntityPlayer player, EntityVillager villager,
                                   ItemStack book1, ItemStack book2) {
        TileEntityWisdomFountain fountain = WisdomFountainHelper.findActiveNearbyFountain(
                villager.world, villager.getPosition(), 10
        );

        if (fountain == null) {
            player.sendMessage(new TextComponentString("§c合并附魔书需要在激活的智慧之泉附近！"));
            return;
        }

        int xpCost = 30;
        if (!player.isCreative() && player.experienceLevel < xpCost) {
            player.sendMessage(new TextComponentString("§c合并附魔书需要 " + xpCost + " 级经验！"));
            return;
        }

        ItemStack mergedBook = WisdomFountainHelper.mergeEnchantedBooks(book1, book2);

        NBTTagList mergedEnchants = ItemEnchantedBook.getEnchantments(mergedBook);
        if (mergedEnchants.tagCount() == 0) {
            player.sendMessage(new TextComponentString("§c这两本书的附魔互相冲突，无法合并！"));
            return;
        }

        if (!player.isCreative()) {
            book1.shrink(1);
            book2.shrink(1);
            player.addExperienceLevel(-xpCost);
        }

        if (!player.inventory.addItemStackToInventory(mergedBook)) {
            player.dropItem(mergedBook, false);
        }

        playMergeEffects(villager);

        player.sendMessage(new TextComponentString("§a成功合并附魔书！（支持破限等级）"));
        player.sendMessage(new TextComponentString("§e合并后的附魔书包含 §6" +
                mergedEnchants.tagCount() + " §e个附魔"));

        for (int i = 0; i < mergedEnchants.tagCount(); i++) {
            int id = mergedEnchants.getCompoundTagAt(i).getShort("id");
            int level = mergedEnchants.getCompoundTagAt(i).getShort("lvl");
            Enchantment ench = Enchantment.getEnchantmentByID(id);
            if (ench != null) {
                player.sendMessage(new TextComponentString("  §7- " +
                        ench.getTranslatedName(level)));
            }
        }
    }

    // ------------------------------------------------------------------------
    //  2. 为智慧守护者“学习”新的附魔并更新交易（真正实现“可学”）
    // ------------------------------------------------------------------------
    private void handleAddNewTrades(EntityPlayer player, EntityVillager villager, ItemStack newBook) {
        // 必须在激活的智慧之泉附近
        TileEntityWisdomFountain fountain = WisdomFountainHelper.findActiveNearbyFountain(
                villager.world, villager.getPosition(), 10
        );

        if (fountain == null) {
            player.sendMessage(new TextComponentString("§c为智慧守护者学习新附魔，需要在激活的智慧之泉附近！"));
            return;
        }

        NBTTagList newEnchants = ItemEnchantedBook.getEnchantments(newBook);
        if (newEnchants == null || newEnchants.tagCount() == 0) {
            player.sendMessage(new TextComponentString("§c这本书没有任何附魔！"));
            return;
        }

        NBTTagCompound data = villager.getEntityData();
        NBTTagList storedEnchants;

        if (data.hasKey("StoredEnchantments", 9)) { // 9 = List
            storedEnchants = data.getTagList("StoredEnchantments", 10);
        } else {
            storedEnchants = new NBTTagList();
        }

        int added = 0;
        int upgraded = 0;

        // 将新书中的附魔合并进 WisdomKeeper 已掌握的列表（支持破限等级）
        for (int i = 0; i < newEnchants.tagCount(); i++) {
            NBTTagCompound newTag = newEnchants.getCompoundTagAt(i);
            short newId = newTag.getShort("id");
            short newLvl = newTag.getShort("lvl");
            if (newLvl <= 0) continue;

            boolean found = false;
            for (int j = 0; j < storedEnchants.tagCount(); j++) {
                NBTTagCompound oldTag = storedEnchants.getCompoundTagAt(j);
                if (oldTag.getShort("id") == newId) {
                    found = true;
                    short oldLvl = oldTag.getShort("lvl");
                    if (newLvl > oldLvl) {
                        oldTag.setShort("lvl", newLvl);
                        upgraded++;
                    }
                    break;
                }
            }

            if (!found) {
                storedEnchants.appendTag(newTag.copy());
                added++;
            }
        }

        data.setTag("StoredEnchantments", storedEnchants);

        // 根据最新的 StoredEnchantments 重建交易列表（支持破限等级）
        MerchantRecipeList newTrades = WisdomFountainHelper.createEnchantedBookTradesFromStoredList(storedEnchants);

        try {
            Field buyingListField = null;
            for (Field field : EntityVillager.class.getDeclaredFields()) {
                if (field.getType() == MerchantRecipeList.class) {
                    buyingListField = field;
                    break;
                }
            }

            if (buyingListField != null) {
                buyingListField.setAccessible(true);
                buyingListField.set(villager, newTrades);
            }
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(new TextComponentString("§c更新智慧守护者交易失败：" + e.getMessage()));
            return;
        }

        // 消耗书本
        if (!player.isCreative()) {
            newBook.shrink(1);
        }

        // 反馈信息
        if (added == 0 && upgraded == 0) {
            player.sendMessage(new TextComponentString("§7这本附魔书的内容，智慧守护者已经完全掌握了。"));
        } else {
            player.sendMessage(new TextComponentString("§a智慧守护者学习了新的知识！"));
            if (added > 0) {
                player.sendMessage(new TextComponentString("§e新增附魔种类: §6" + added));
            }
            if (upgraded > 0) {
                player.sendMessage(new TextComponentString("§e提升已有附魔等级: §6" + upgraded));
            }
        }

        // 一些简单的特效
        playMergeEffects(villager);
    }

    // ------------------------------------------------------------------------
    //  3. 显示智慧守护者信息（会列出破限等级 + 价格）
    // ------------------------------------------------------------------------
    private void showVillagerInfo(EntityPlayer player, EntityVillager villager) {
        NBTTagCompound data = villager.getEntityData();
        NBTTagList storedEnchants = null;

        if (data.hasKey("StoredEnchantments", 9)) {
            storedEnchants = data.getTagList("StoredEnchantments", 10);
        }

        player.sendMessage(new TextComponentString("§6=== 智慧守护者信息 ==="));

        if (storedEnchants != null && storedEnchants.tagCount() > 0) {
            player.sendMessage(new TextComponentString("§e出售的附魔种类：§6" +
                    storedEnchants.tagCount()));

            player.sendMessage(new TextComponentString("§e附魔列表："));
            for (int i = 0; i < storedEnchants.tagCount(); i++) {
                NBTTagCompound enchTag = storedEnchants.getCompoundTagAt(i);
                int enchId = enchTag.getShort("id");
                int level = enchTag.getShort("lvl");
                Enchantment ench = Enchantment.getEnchantmentByID(enchId);
                if (ench != null) {
                    int cost = WisdomFountainHelper.calculateEmeraldCost(enchId, level);
                    player.sendMessage(new TextComponentString("  §7- " +
                            ench.getTranslatedName(level) + " §e价格: §6" +
                            cost + " 绿宝石"));
                }
            }
        } else {
            player.sendMessage(new TextComponentString("§c未找到附魔信息"));
        }

        // 交易数量（原逻辑保留）
        try {
            Field buyingListField = null;
            for (Field field : EntityVillager.class.getDeclaredFields()) {
                if (field.getType() == MerchantRecipeList.class) {
                    buyingListField = field;
                    break;
                }
            }

            if (buyingListField != null) {
                buyingListField.setAccessible(true);
                MerchantRecipeList trades = (MerchantRecipeList) buyingListField.get(villager);
                if (trades != null) {
                    player.sendMessage(new TextComponentString("§e交易选项数量：§6" +
                            trades.size()));
                }
            }
        } catch (Exception ignored) {}

        player.sendMessage(new TextComponentString("§7提示："));
        player.sendMessage(new TextComponentString("§7- 主副手各持一本附魔书：合并（可破限）"));
        player.sendMessage(new TextComponentString("§7- 只拿附魔书：让智慧守护者学习新的附魔"));
    }

    // ------------------------------------------------------------------------
    //  4. 村民转化 / 视觉特效
    // ------------------------------------------------------------------------
    private void playMergeEffects(EntityVillager villager) {
        World world = villager.world;

        world.playSound(null, villager.posX, villager.posY, villager.posZ,
                SoundEvents.BLOCK_ANVIL_USE, SoundCategory.NEUTRAL, 0.5F, 1.0F);

        world.playSound(null, villager.posX, villager.posY, villager.posZ,
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 1.0F, 1.5F);

        for (int i = 0; i < 40; i++) {
            double angle = (Math.PI * 2) * i / 20;
            double radius = 0.5 + i * 0.02;
            double x = villager.posX + Math.cos(angle) * radius;
            double y = villager.posY + i * 0.05;
            double z = villager.posZ + Math.sin(angle) * radius;

            world.spawnParticle(
                    EnumParticleTypes.ENCHANTMENT_TABLE,
                    x, y, z,
                    0, 0.02, 0
            );

            if (i % 5 == 0) {
                world.spawnParticle(
                        EnumParticleTypes.VILLAGER_HAPPY,
                        x, y, z,
                        0, 0, 0
                );
            }
        }
    }

    private void transformVillager(EntityVillager villager, ItemStack enchantedBook, EntityPlayer player) {
        try {
            NBTTagList enchantments = ItemEnchantedBook.getEnchantments(enchantedBook);

            // 用“样本书上的附魔列表”生成交易（支持破限）
            MerchantRecipeList trades = WisdomFountainHelper.createEnchantedBookTrades(enchantedBook);

            // 设置职业为图书管理员
            villager.setProfession(1);

            // 设置交易列表
            Field buyingListField = null;
            for (Field field : EntityVillager.class.getDeclaredFields()) {
                if (field.getType() == MerchantRecipeList.class) {
                    buyingListField = field;
                    break;
                }
            }

            if (buyingListField != null) {
                buyingListField.setAccessible(true);
                buyingListField.set(villager, trades);
            }

            // 标记为智慧守护者（会记录 StoredEnchantments）
            WisdomFountainHelper.markAsWisdomKeeper(villager, enchantments);

        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(new TextComponentString("§c转化失败：" + e.getMessage()));
        }
    }

    private void playTransformEffects(EntityVillager villager) {
        World world = villager.world;

        world.playSound(null, villager.posX, villager.posY, villager.posZ,
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 1.0F, 1.0F);

        for (int i = 0; i < 30; i++) {
            double x = villager.posX + (rand.nextDouble() - 0.5) * 2.0;
            double y = villager.posY + rand.nextDouble() * 2.0;
            double z = villager.posZ + (rand.nextDouble() - 0.5) * 2.0;

            world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE, x, y, z, 0, 0.1, 0);
        }
    }
}
