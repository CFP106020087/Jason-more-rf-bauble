package com.moremod.recipe;

import com.moremod.item.RegisterItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;

@Mod.EventBusSubscriber(modid = "moremod")
public class BatteryRecipes {

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        System.out.println("[moremod] 注册电池合成配方...");

        // 基础电池
        event.getRegistry().register(new ShapedOreRecipe(
                new ResourceLocation("moremod", "battery_basic"),
                new ItemStack(RegisterItem.BATTERY_BASIC),
                "RIR",
                "ICI",
                "RIR",
                'R', "dustRedstone",
                'I', "ingotIron",
                'C', "blockRedstone"
        ).setRegistryName("battery_basic_recipe"));

        // 高级电池 - 只需要1个基础电池
        event.getRegistry().register(new ShapedOreRecipe(
                new ResourceLocation("moremod", "battery_advanced"),
                new ItemStack(RegisterItem.BATTERY_ADVANCED),
                "GGG",
                "CBC",
                "GGG",
                'G', "ingotGold",
                'B', new ItemStack(RegisterItem.BATTERY_BASIC),
                'C', "blockGold"
        ).setRegistryName("battery_advanced_recipe"));

        // 精英电池 - 只需要1个高级电池
        event.getRegistry().register(new ShapedOreRecipe(
                new ResourceLocation("moremod", "battery_elite"),
                new ItemStack(RegisterItem.BATTERY_ELITE),
                "DDD",
                "DAD",
                "DED",
                'D', "gemDiamond",
                'A', new ItemStack(RegisterItem.BATTERY_ADVANCED),
                'E', "blockDiamond"
        ).setRegistryName("battery_elite_recipe"));

        // 终极电池 - 只需要1个精英电池
        event.getRegistry().register(new ShapedOreRecipe(
                new ResourceLocation("moremod", "battery_ultimate"),
                new ItemStack(RegisterItem.BATTERY_ULTIMATE),
                "NNN",
                "ESE",
                "NNN",
                'N', Items.NETHER_STAR,
                'E', new ItemStack(RegisterItem.BATTERY_ELITE),
                'S', "blockEmerald"
        ).setRegistryName("battery_ultimate_recipe"));

        // 量子电池 - 只需要1个终极电池
        event.getRegistry().register(new ShapedOreRecipe(
                new ResourceLocation("moremod", "battery_quantum"),
                new ItemStack(RegisterItem.BATTERY_QUANTUM),
                "PEP",
                "UOU",
                "PEP",
                'P', Items.ENDER_PEARL,
                'U', new ItemStack(RegisterItem.BATTERY_ULTIMATE),
                'O', Items.ENDER_EYE,
                'E', Items.ENDER_EYE
        ).setRegistryName("battery_quantum_recipe"));

        System.out.println("[moremod] 电池合成配方注册完成！");
    }
}