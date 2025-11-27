// com/moremod/init/ModUpgradeItems.java
package com.moremod.init;

import com.moremod.item.UpgradeType;
import com.moremod.item.upgrades.ItemUpgradeComponent;
import com.moremod.upgrades.ModuleRegistry;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class ModUpgradeItems {

    // =====================================================
    // 横扫模块（示例 - 你原本已有）
    // =====================================================

    // =====================================================
    // ⭐ 魔力吸收模块（Magic Absorb）⭐
    // =====================================================
    public static Item MAGIC_ABSORB_1;  // Lv1 蓝色
    public static Item MAGIC_ABSORB_2;  // Lv2 橙色
    public static Item MAGIC_ABSORB_3;  // Lv3 紫色

    // =====================================================
    // 注册所有模块物品
    // =====================================================
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {

        // -----------------------------------------------------
        // 横扫模块（SWEEP MODULE） - 示例
        // -----------------------------------------------------



        // -----------------------------------------------------
        // ⭐⭐ 魔力吸收模块（MAGIC_ABSORB）⭐⭐
        // -----------------------------------------------------

        // ----- Lv1（蓝色） -----
        MAGIC_ABSORB_1 = register(event, new ItemUpgradeComponent(
                UpgradeType.MAGIC_ABSORB,
                new String[]{
                        "§7吸收少量法伤",
                        "§7并转化为物理力量",
                        "§7叠加少量 §c余灼"
                },
                1
        ).setRegistryName("magic_absorb_1"));

        // ----- Lv2（橙色） -----
        MAGIC_ABSORB_2 = register(event, new ItemUpgradeComponent(
                UpgradeType.MAGIC_ABSORB,
                new String[]{
                        "§7更高法伤吸收率",
                        "§7更快余灼累积与更高伤害"
                },
                2
        ).setRegistryName("magic_absorb_2"));

        // ----- Lv3（紫色） -----
        MAGIC_ABSORB_3 = register(event, new ItemUpgradeComponent(
                UpgradeType.MAGIC_ABSORB,
                new String[]{
                        "§7强化吸收倍率",
                        "§c余灼满载触发『魔力爆心』",
                        "§7造成一次强力爆发伤害"
                },
                3
        ).setRegistryName("magic_absorb_3"));

        // =====================================================
        // 初始化模块系统（必须放最后）
        // =====================================================
        ModuleRegistry.init();
    }

    // =====================================================
    // 注册工具方法
    // =====================================================
    private static Item register(RegistryEvent.Register<Item> event, Item item) {
        event.getRegistry().register(item);
        return item;
    }
}
