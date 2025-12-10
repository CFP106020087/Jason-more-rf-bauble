package com.moremod.ritual.enchanting;

import com.moremod.block.BlockEnchantingBooster;
import com.moremod.init.ModBlocks;
import com.moremod.ritual.RitualInfusionAPI;
import com.moremod.ritual.RitualInfusionRecipe;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;

/**
 * 附魔增强方块的仪式配方
 * 四种类型，逐级增强
 */
public class EnchantingBoosterRituals {

    public static void registerRituals() {
        if (ModBlocks.ENCHANTING_BOOSTER == null) {
            System.err.println("[MoreMod] Cannot register enchanting booster rituals - block not registered!");
            return;
        }

        // ========== 奥术石 (Arcane Stone) ==========
        // 基础材料：圆石 + 青金石 + 红石
        // +1.0 附魔增益
        RitualInfusionAPI.RITUAL_RECIPES.add(new RitualInfusionRecipe(
                Ingredient.fromStacks(new ItemStack(Blocks.COBBLESTONE)),
                Arrays.asList(
                        Ingredient.fromStacks(new ItemStack(Items.DYE, 1, 4)), // 青金石
                        Ingredient.fromStacks(new ItemStack(Items.DYE, 1, 4)), // 青金石
                        Ingredient.fromStacks(new ItemStack(Items.REDSTONE)),
                        Ingredient.fromStacks(new ItemStack(Items.REDSTONE))
                ),
                new ItemStack(ModBlocks.ENCHANTING_BOOSTER, 1, BlockEnchantingBooster.BoosterType.ARCANE_STONE.getMeta()),
                100,   // 5秒
                500,   // 每基座500能量
                0.0f,  // 无失败率
                1      // 一阶祭坛
        ));

        // ========== 附魔书架 (Enchanted Bookshelf) ==========
        // 中级材料：书架 + 附魔书 + 青金石块
        // +2.0 附魔增益
        RitualInfusionAPI.RITUAL_RECIPES.add(new RitualInfusionRecipe(
                Ingredient.fromStacks(new ItemStack(Blocks.BOOKSHELF)),
                Arrays.asList(
                        Ingredient.fromStacks(new ItemStack(Items.ENCHANTED_BOOK)),
                        Ingredient.fromStacks(new ItemStack(Items.ENCHANTED_BOOK)),
                        Ingredient.fromStacks(new ItemStack(Blocks.LAPIS_BLOCK)),
                        Ingredient.fromStacks(new ItemStack(Items.EXPERIENCE_BOTTLE)),
                        Ingredient.fromStacks(new ItemStack(Items.EXPERIENCE_BOTTLE))
                ),
                new ItemStack(ModBlocks.ENCHANTING_BOOSTER, 1, BlockEnchantingBooster.BoosterType.ENCHANTED_BOOKSHELF.getMeta()),
                200,   // 10秒
                1000,  // 每基座1000能量
                0.05f, // 5%失败率
                1      // 一阶祭坛
        ));

        // ========== 知识水晶 (Knowledge Crystal) ==========
        // 高级材料：钻石块 + 绿宝石 + 下界石英 + 附魔之瓶
        // +3.0 附魔增益
        RitualInfusionAPI.RITUAL_RECIPES.add(new RitualInfusionRecipe(
                Ingredient.fromStacks(new ItemStack(Blocks.DIAMOND_BLOCK)),
                Arrays.asList(
                        Ingredient.fromStacks(new ItemStack(Items.EMERALD)),
                        Ingredient.fromStacks(new ItemStack(Items.EMERALD)),
                        Ingredient.fromStacks(new ItemStack(Items.QUARTZ)),
                        Ingredient.fromStacks(new ItemStack(Items.QUARTZ)),
                        Ingredient.fromStacks(new ItemStack(Items.EXPERIENCE_BOTTLE)),
                        Ingredient.fromStacks(new ItemStack(Items.EXPERIENCE_BOTTLE)),
                        Ingredient.fromStacks(new ItemStack(Items.ENCHANTED_BOOK)),
                        Ingredient.fromStacks(new ItemStack(Items.ENCHANTED_BOOK))
                ),
                new ItemStack(ModBlocks.ENCHANTING_BOOSTER, 1, BlockEnchantingBooster.BoosterType.KNOWLEDGE_CRYSTAL.getMeta()),
                400,   // 20秒
                2000,  // 每基座2000能量
                0.1f,  // 10%失败率
                2      // 二阶祭坛
        ));

        // ========== 灵魂图书馆 (Soul Library) ==========
        // 终极材料：下界之星 + 灵魂沙 + 大量附魔材料
        // +5.0 附魔增益
        RitualInfusionAPI.RITUAL_RECIPES.add(new RitualInfusionRecipe(
                Ingredient.fromStacks(new ItemStack(Items.NETHER_STAR)),
                Arrays.asList(
                        Ingredient.fromStacks(new ItemStack(Blocks.SOUL_SAND)),
                        Ingredient.fromStacks(new ItemStack(Blocks.SOUL_SAND)),
                        Ingredient.fromStacks(new ItemStack(Items.ENCHANTED_BOOK)),
                        Ingredient.fromStacks(new ItemStack(Items.ENCHANTED_BOOK)),
                        Ingredient.fromStacks(new ItemStack(Items.ENCHANTED_BOOK)),
                        Ingredient.fromStacks(new ItemStack(Items.ENCHANTED_BOOK)),
                        Ingredient.fromStacks(new ItemStack(Blocks.OBSIDIAN)),
                        Ingredient.fromStacks(new ItemStack(Blocks.OBSIDIAN))
                ),
                new ItemStack(ModBlocks.ENCHANTING_BOOSTER, 1, BlockEnchantingBooster.BoosterType.SOUL_LIBRARY.getMeta()),
                600,   // 30秒
                5000,  // 每基座5000能量
                0.15f, // 15%失败率
                3      // 三阶祭坛
        ));

        System.out.println("[MoreMod] ✨ Registered 4 enchanting booster rituals");
    }
}
