package com.moremod.sponsor;

import com.moremod.sponsor.item.SponsorArmor;
import com.moremod.sponsor.item.SponsorBauble;
import com.moremod.sponsor.item.SponsorSword;
import com.moremod.sponsor.item.ZhuxianSword;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 赞助者物品注册中心
 *
 * 所有赞助者武器、盔甲、饰品都在这里注册
 * 可以通过 SponsorConfig.enabled 完全禁用
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class SponsorItems {

    // ========== 物品列表（用于统一管理） ==========
    private static final List<Item> ALL_SPONSOR_ITEMS = new ArrayList<>();

    // ========== 赞助者武器 ==========
    public static Item ZHUXIAN_SWORD; // 诛仙四剑

    // ========== 示例赞助者盔甲 ==========
    // 取消注释并添加你的赞助者盔甲
    // public static Item EXAMPLE_SPONSOR_HELMET;
    // public static Item EXAMPLE_SPONSOR_CHESTPLATE;
    // public static Item EXAMPLE_SPONSOR_LEGGINGS;
    // public static Item EXAMPLE_SPONSOR_BOOTS;

    // ========== 示例赞助者饰品 ==========
    // 取消注释并添加你的赞助者饰品
    // public static Item EXAMPLE_SPONSOR_AMULET;

    /**
     * 安全创建物品（避免类加载异常）
     */
    private static <T extends Item> T newSafe(Supplier<T> supplier, String id) {
        try {
            T item = supplier.get();
            if (item != null) {
                ALL_SPONSOR_ITEMS.add(item);
            }
            return item;
        } catch (Throwable t) {
            System.err.println("[moremod/sponsor] 创建赞助者物品失败(" + id + "): " + t.getMessage());
            t.printStackTrace();
            return null;
        }
    }

    /**
     * 注册物品事件
     */
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // 检查主开关
        if (!SponsorConfig.isEnabled()) {
            System.out.println("[moremod] 赞助者物品系统已禁用，跳过注册");
            return;
        }

        System.out.println("[moremod] ========== 注册赞助者物品 ==========");

        // ========== 赞助者武器 ==========
        if (SponsorConfig.isWeaponsEnabled()) {
            // 诛仙四剑
            ZHUXIAN_SWORD = newSafe(ZhuxianSword::new, "zhuxian_sword");
        }

        // 示例：赞助者盔甲
        // if (SponsorConfig.isArmorEnabled()) {
        //     EXAMPLE_SPONSOR_HELMET = newSafe(() ->
        //         new SponsorArmor(EntityEquipmentSlot.HEAD, "example_sponsor_helmet", "赞助者示例头盔"),
        //         "example_sponsor_helmet"
        //     );
        //     EXAMPLE_SPONSOR_CHESTPLATE = newSafe(() ->
        //         new SponsorArmor(EntityEquipmentSlot.CHEST, "example_sponsor_chestplate", "赞助者示例胸甲"),
        //         "example_sponsor_chestplate"
        //     );
        //     EXAMPLE_SPONSOR_LEGGINGS = newSafe(() ->
        //         new SponsorArmor(EntityEquipmentSlot.LEGS, "example_sponsor_leggings", "赞助者示例护腿"),
        //         "example_sponsor_leggings"
        //     );
        //     EXAMPLE_SPONSOR_BOOTS = newSafe(() ->
        //         new SponsorArmor(EntityEquipmentSlot.FEET, "example_sponsor_boots", "赞助者示例靴子"),
        //         "example_sponsor_boots"
        //     );
        // }

        // 示例：赞助者饰品
        // if (SponsorConfig.isBaublesEnabled()) {
        //     EXAMPLE_SPONSOR_AMULET = newSafe(() ->
        //         new SponsorBauble("example_sponsor_amulet", "赞助者示例护符", BaubleType.AMULET),
        //         "example_sponsor_amulet"
        //     );
        // }

        // ========== 注册所有物品 ==========
        if (!ALL_SPONSOR_ITEMS.isEmpty()) {
            event.getRegistry().registerAll(ALL_SPONSOR_ITEMS.toArray(new Item[0]));
            System.out.println("[moremod] 成功注册 " + ALL_SPONSOR_ITEMS.size() + " 个赞助者物品");
        } else {
            System.out.println("[moremod] 没有赞助者物品需要注册");
        }

        System.out.println("[moremod] ========== 赞助者物品注册完成 ==========");
    }

    /**
     * 注册模型事件（客户端）
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        if (!SponsorConfig.isEnabled()) {
            return;
        }

        System.out.println("[moremod] 注册赞助者物品模型...");

        for (Item item : ALL_SPONSOR_ITEMS) {
            if (item != null && item.getRegistryName() != null) {
                ModelLoader.setCustomModelResourceLocation(
                    item, 0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory")
                );
            }
        }

        System.out.println("[moremod] 赞助者物品模型注册完成");
    }

    /**
     * 获取所有赞助者物品
     */
    public static List<Item> getAllSponsorItems() {
        return new ArrayList<>(ALL_SPONSOR_ITEMS);
    }

    /**
     * 检查物品是否是赞助者物品
     */
    public static boolean isSponsorItem(Item item) {
        return ALL_SPONSOR_ITEMS.contains(item);
    }

    /**
     * 获取赞助者物品数量
     */
    public static int getSponsorItemCount() {
        return ALL_SPONSOR_ITEMS.size();
    }
}
