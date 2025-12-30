package com.moremod.init;

import com.moremod.MoreMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 菜单类型注册表 - 1.20 Forge版本
 *
 * 1.12的Container在1.20中称为Menu
 */
public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MoreMod.MOD_ID);

    // ========== GUI菜单 ==========

    // TODO: 从1.12移植以下Container:
    // - ContainerChargingStation -> ChargingStationMenu
    // - ContainerUpgradeChamber -> UpgradeChamberMenu
    // - ContainerQuantumQuarry -> QuantumQuarryMenu
    // 等...

    // 示例注册:
    // public static final RegistryObject<MenuType<ChargingStationMenu>> CHARGING_STATION_MENU =
    //         MENUS.register("charging_station_menu",
    //                 () -> IForgeMenuType.create(ChargingStationMenu::new));
}
