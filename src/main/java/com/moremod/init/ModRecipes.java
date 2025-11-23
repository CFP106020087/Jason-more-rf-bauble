package com.moremod.init;

import com.moremod.moremod;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;

@Mod.EventBusSubscriber(modid = moremod.MODID)
public class ModRecipes {

    public static void init() {
        // 合成配方通过事件注册
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        // ============ 神碑智慧之泉相关配方 ============

        // 守护者石块 (9个守护者之石)
        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(moremod.MODID, "guardian_stone_block"),
                        new ItemStack(ModBlocks.GUARDIAN_STONE_BLOCK),
                        "SSS",
                        "SSS",
                        "SSS",
                        'S', ModItems.FRGUARDIAN_STONE
                ).setRegistryName("guardian_stone_block")
        );

        // 符文虚空石块 (4个符文虚空石 + 4个末影珍珠 + 1个黑曜石)
        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(moremod.MODID, "runed_void_stone_block"),
                        new ItemStack(ModBlocks.RUNED_VOID_STONE_BLOCK),
                        "VEV",
                        "EOE",
                        "VEV",
                        'V', ModItems.RUNED_VOID_STONE,
                        'E', Items.ENDER_PEARL,
                        'O', Blocks.OBSIDIAN
                ).setRegistryName("runed_void_stone_block")
        );

        // 远古核心块 (4个远古核心碎片 + 4个金锭 + 1个钻石块)
        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(moremod.MODID, "ancient_core_block"),
                        new ItemStack(ModBlocks.ANCIENT_CORE_BLOCK),
                        "FGF",
                        "GDG",
                        "FGF",
                        'F', ModItems.ANCIENT_CORE_FRAGMENT,
                        'G', Items.GOLD_INGOT,
                        'D', Blocks.DIAMOND_BLOCK
                ).setRegistryName("ancient_core_block")
        );

        // 智慧之泉核心 (1个远古核心块 + 4个符文虚空石块 + 4个附魔台)
        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(moremod.MODID, "wisdom_fountain_core"),
                        new ItemStack(ModBlocks.WISDOM_FOUNTAIN_CORE),
                        "RER",
                        "EAE",
                        "RER",
                        'A', ModBlocks.ANCIENT_CORE_BLOCK,
                        'R', ModBlocks.RUNED_VOID_STONE_BLOCK,
                        'E', Blocks.ENCHANTING_TABLE
                ).setRegistryName("wisdom_fountain_core")
        );

        // 💎💎💎 简易智慧之泉核心 (绿宝石 + 书 + 金块) 💎💎💎
        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(moremod.MODID, "simple_wisdom_shrine"),
                        new ItemStack(ModBlocks.SIMPLE_WISDOM_SHRINE),
                        "EBE",
                        "BGB",
                        "EBE",
                        'E', Items.EMERALD,
                        'B', Items.BOOK,
                        'G', Blocks.GOLD_BLOCK
                ).setRegistryName("simple_wisdom_shrine")
        );

        // ============ 反向配方：方块转回物品 (可选) ============
        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(moremod.MODID, "guardian_stone_from_block"),
                        new ItemStack(ModItems.FRGUARDIAN_STONE, 9),
                        "B",
                        'B', ModBlocks.GUARDIAN_STONE_BLOCK
                ).setRegistryName("guardian_stone_from_block")
        );

        // ============ 劍升級台（material 版）合成配方 ============
        // 图纸：
        // I O I
        // D A D
        // I O I
        // I = ingotIron（矿辞），O = 黑曜石，D = 钻石，A = 铁砧
        event.getRegistry().register(
                new ShapedOreRecipe(
                        new ResourceLocation(moremod.MODID, "sword_upgrade_station_material"),
                        new ItemStack(ModBlocks.SWORD_UPGRADE_STATION_MATERIAL),
                        "IOI",
                        "DAD",
                        "IOI",
                        'I', "ingotIron",
                        'O', Blocks.OBSIDIAN,
                        'D', Items.DIAMOND,
                        'A', Blocks.ANVIL
                ).setRegistryName("sword_upgrade_station_material")
        );
    }
}
