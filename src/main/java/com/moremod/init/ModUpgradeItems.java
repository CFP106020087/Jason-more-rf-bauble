// com/moremod/init/ModUpgradeItems.java
package com.moremod.init;

import com.moremod.upgrades.ModuleRegistry;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 模块物品注册类
 *
 * 注意：大部分升级模块现在统一在 UpgradeItemsExtended 中定义，
 * 并通过 RegisterItem.java 注册到 Forge。
 *
 * 此类保留用于：
 * - 初始化 ModuleRegistry
 * - 未来可能需要特殊处理的模块注册
 *
 * @see com.moremod.item.upgrades.UpgradeItemsExtended 模块定义
 * @see com.moremod.item.RegisterItem 统一注册入口
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ModUpgradeItems {

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        // 初始化模块系统
        ModuleRegistry.init();
    }
}
