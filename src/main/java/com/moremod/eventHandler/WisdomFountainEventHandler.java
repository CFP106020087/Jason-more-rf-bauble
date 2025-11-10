package com.moremod.eventHandler;

import com.moremod.util.WisdomFountainHelper;
import com.moremod.tile.TileEntityWisdomFountain;
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

        // 检查是否Shift+右键
        if (!player.isSneaking()) return;

        ItemStack mainHand = player.getHeldItem(EnumHand.MAIN_HAND);
        ItemStack offHand = player.getHeldItem(EnumHand.OFF_HAND);

        // 检查是否已经是智慧守护者
        if (WisdomFountainHelper.isWisdomKeeper(villager)) {
            // 已转化的村民功能

            // 功能1：合并两本附魔书（主手和副手各持一本）
            if (mainHand.getItem() == Items.ENCHANTED_BOOK &&
                    offHand.getItem() == Items.ENCHANTED_BOOK) {

                handleBookMerging(player, villager, mainHand, offHand);
                event.setCanceled(true);
                return;
            }

            // 功能2：添加新的附魔交易（主手持附魔书，副手空）
            if (mainHand.getItem() == Items.ENCHANTED_BOOK && offHand.isEmpty()) {
                handleAddNewTrades(player, villager, mainHand);
                event.setCanceled(true);
                return;
            }

            // 功能3：查询村民信息（空手）
            if (mainHand.isEmpty() && offHand.isEmpty()) {
                showVillagerInfo(player, villager);
                event.setCanceled(true);
                return;
            }

            return;
        }

        // 未转化的村民 - 原有的转化功能
        if (mainHand.getItem() != Items.ENCHANTED_BOOK) return;

        // 查找附近的智慧之泉
        TileEntityWisdomFountain fountain = WisdomFountainHelper.findActiveNearbyFountain(
                villager.world, villager.getPosition(), 10
        );

        if (fountain == null) {
            player.sendMessage(new TextComponentString("§c需要在激活的神碑智慧之泉附近（10格内）！"));
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
                enchantments.tagCount() + " §e种不同的附魔书"));

        event.setCanceled(true);
    }

    /**
     * 处理附魔书合并
     */
    private void handleBookMerging(EntityPlayer player, EntityVillager villager,
                                   ItemStack book1, ItemStack book2) {
        // 检查是否在智慧之泉附近
        TileEntityWisdomFountain fountain = WisdomFountainHelper.findActiveNearbyFountain(
                villager.world, villager.getPosition(), 10
        );

        if (fountain == null) {
            player.sendMessage(new TextComponentString("§c合并附魔书需要在激活的智慧之泉附近！"));
            return;
        }

        // 检查经验等级
        int xpCost = 30; // 合并消耗5级经验
        if (!player.isCreative() && player.experienceLevel < xpCost) {
            player.sendMessage(new TextComponentString("§c合并附魔书需要 " + xpCost + " 级经验！"));
            return;
        }

        // 合并附魔书
        ItemStack mergedBook = WisdomFountainHelper.mergeEnchantedBooks(book1, book2);

        // 检查合并结果
        NBTTagList mergedEnchants = ItemEnchantedBook.getEnchantments(mergedBook);
        if (mergedEnchants.tagCount() == 0) {
            player.sendMessage(new TextComponentString("§c这两本书的附魔互相冲突，无法合并！"));
            return;
        }

        // 消耗物品和经验
        if (!player.isCreative()) {
            book1.shrink(1);
            book2.shrink(1);
            player.addExperienceLevel(-xpCost);
        }

        // 给予合并后的书
        if (!player.inventory.addItemStackToInventory(mergedBook)) {
            player.dropItem(mergedBook, false);
        }

        // 播放效果
        playMergeEffects(villager);

        // 显示结果
        player.sendMessage(new TextComponentString("§a成功合并附魔书！"));
        player.sendMessage(new TextComponentString("§e合并后的附魔书包含 §6" +
                mergedEnchants.tagCount() + " §e个附魔"));

        // 显示附魔列表
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

    /**
     * 添加新的交易
     */
    private void handleAddNewTrades(EntityPlayer player, EntityVillager villager, ItemStack newBook) {
        player.sendMessage(new TextComponentString("§e[功能开发中] 为智慧守护者添加新交易"));
        player.sendMessage(new TextComponentString("§7提示：目前请重新转化一个新村民"));
    }

    /**
     * 显示村民信息
     */
    private void showVillagerInfo(EntityPlayer player, EntityVillager villager) {
        // 修复：正确获取 NBTTagList
        NBTTagCompound data = villager.getEntityData();
        NBTTagList storedEnchants = null;

        // 尝试获取存储的附魔列表
        if (data.hasKey("StoredEnchantments", 9)) { // 9 = NBTTagList
            storedEnchants = data.getTagList("StoredEnchantments", 10); // 10 = NBTTagCompound
        }

        player.sendMessage(new TextComponentString("§6=== 智慧守护者信息 ==="));

        if (storedEnchants != null && storedEnchants.tagCount() > 0) {
            player.sendMessage(new TextComponentString("§e出售的附魔种类：§6" +
                    storedEnchants.tagCount()));

            // 显示附魔列表
            player.sendMessage(new TextComponentString("§e附魔列表："));
            for (int i = 0; i < storedEnchants.tagCount(); i++) {
                NBTTagCompound enchTag = storedEnchants.getCompoundTagAt(i);
                int enchId = enchTag.getShort("id");
                int level = enchTag.getShort("lvl");
                Enchantment ench = Enchantment.getEnchantmentByID(enchId);
                if (ench != null) {
                    player.sendMessage(new TextComponentString("  §7- " +
                            ench.getTranslatedName(level) + " §e价格: §6" +
                            WisdomFountainHelper.calculateEmeraldCost(enchId, level) + " 绿宝石"));
                }
            }
        } else {
            player.sendMessage(new TextComponentString("§c未找到附魔信息"));
        }

        // 获取交易数量
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
        } catch (Exception e) {
            // 忽略错误
        }

        player.sendMessage(new TextComponentString("§7提示：主副手各持一本附魔书可以合并"));
    }

    /**
     * 播放合并特效
     */
    private void playMergeEffects(EntityVillager villager) {
        World world = villager.world;

        // 播放音效
        world.playSound(null, villager.posX, villager.posY, villager.posZ,
                SoundEvents.BLOCK_ANVIL_USE, SoundCategory.NEUTRAL, 0.5F, 1.0F);

        world.playSound(null, villager.posX, villager.posY, villager.posZ,
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 1.0F, 1.5F);

        // 生成粒子效果 - 螺旋上升
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

            // 使用工具类创建交易
            MerchantRecipeList trades = WisdomFountainHelper.createEnchantedBookTrades(enchantedBook);

            // 设置职业为图书管理员
            villager.setProfession(1);

            // 使用反射设置交易
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

            // 使用工具类标记村民
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