package com.moremod.init;

import com.moremod.MoreMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 创造模式标签页 - 1.20 Forge版本
 *
 * 1.12的CreativeTab在1.20中使用CreativeModeTab
 *
 * 1.12 -> 1.20 API变更:
 * - CreativeTab -> CreativeModeTab
 * - 使用displayItems回调添加物品
 */
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MoreMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MOREMOD_TAB = CREATIVE_TABS.register("moremod_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.moremod"))
                    .icon(() -> new ItemStack(ModItems.UPGRADE_CHAMBER_CORE.get()))
                    .displayItems((params, output) -> {
                        // ========== 机器方块 ==========
                        output.accept(ModItems.UPGRADE_CHAMBER_CORE.get());
                        output.accept(ModItems.RITUAL_CORE.get());
                        output.accept(ModItems.WISDOM_FOUNTAIN_CORE.get());
                        output.accept(ModItems.QUANTUM_QUARRY.get());
                        output.accept(ModItems.QUARRY_ACTUATOR.get());
                        output.accept(ModItems.CHARGING_STATION.get());
                        output.accept(ModItems.TEMPORAL_ACCELERATOR.get());
                        output.accept(ModItems.PEDESTAL.get());

                        // ========== 结构方块 ==========
                        output.accept(ModItems.GUARDIAN_STONE.get());
                        output.accept(ModItems.RUNED_VOID_STONE.get());

                        // TODO: 添加更多物品
                        // 机械核心
                        // 升级模组
                        // 材料
                        // 等...
                    })
                    .build());
}
