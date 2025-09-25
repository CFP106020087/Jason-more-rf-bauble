package com.moremod.client;

import com.moremod.item.RegisterItem;
import com.moremod.item.upgrades.ItemUpgradeComponent;
import com.moremod.item.upgrades.UpgradeItemsExtended;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
public final class ClientModelRegistrar {
    private ClientModelRegistrar() {}

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        // 机械外骨骼 / 基础物品
        reg(RegisterItem.MECHANICAL_EXOSKELETON);
        reg(RegisterItem.COPPER_WISHBONE);
        reg(RegisterItem.CIRCULATION_SYSTEM);
        reg(RegisterItem.TEMPORAL_RIFT);
        reg(RegisterItem.UPGRADE_SELECTOR);
        reg(RegisterItem.LAW_SWORD);
        reg(RegisterItem.ENERGY_RING);
        reg(RegisterItem.SPEAR);
        reg(RegisterItem.CLEANSING_BAUBLE);
        reg(RegisterItem.ENERGY_BARRIER);
        reg(RegisterItem.ENERGY_SWORD);
        reg(RegisterItem.BASIC_ENERGY_BARRIER);
        reg(RegisterItem.CRUDE_ENERGY_BARRIER);
        reg(RegisterItem.ADV_ENERGY_BARRIER);

        // 电池
        reg(RegisterItem.BATTERY_BAUBLE);
        reg(RegisterItem.CREATIVE_BATTERY_BAUBLE);
        reg(RegisterItem.BATTERY_BASIC);
        reg(RegisterItem.BATTERY_ADVANCED);
        reg(RegisterItem.BATTERY_ELITE);
        reg(RegisterItem.BATTERY_ULTIMATE);
        reg(RegisterItem.BATTERY_QUANTUM);

        // 其他饰品/材料
        reg(RegisterItem.MECHANICAL_HEART);
        reg(RegisterItem.TEMPORAL_HEART);
        reg(RegisterItem.BLOODY_THIRST_MASK);
        reg(RegisterItem.MERCHANT_PERSUADER);
        reg(RegisterItem.VILLAGER_PROFESSION_TOOL);
        reg(RegisterItem.ANTIKYTHERA_GEAR);
        reg(RegisterItem.TOGGLE_RENDER);
        reg(RegisterItem.ITEM_RENDER);

        // SimpleDifficulty（若存在）
        reg(RegisterItem.getTemperatureRegulator());
        reg(RegisterItem.getThirstProcessor());

        // 机械核心 / 升级组件 / 喷气背包
        reg(RegisterItem.MECHANICAL_CORE);

        try {
            for (com.moremod.item.upgrades.ItemUpgradeComponent up : com.moremod.item.UpgradeItems.getAllUpgrades()) {
                reg(up);
            }
        } catch (Exception ex) {
            System.err.println("[moremod] ❌ 升级组件模型注册失败: " + ex);
        }

        for (Item jetpack : RegisterItem.JETPACKS) {
            reg(jetpack);
        }

        for (ItemUpgradeComponent up2 : UpgradeItemsExtended.getAllExtendedUpgrades()) {
            reg(up2);
        }

        System.out.println("[moremod] ⚙️ 所有模型注册完成（包括7个电池）");
    }

    private static void reg(Item item) {
        if (item == null || item.getRegistryName() == null) return;
        try {
            ModelLoader.setCustomModelResourceLocation(
                    item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory")
            );
        } catch (Exception e) {
            System.err.println("[moremod] ❌ 模型注册失败: " + item + " - " + e);
        }
    }
}
