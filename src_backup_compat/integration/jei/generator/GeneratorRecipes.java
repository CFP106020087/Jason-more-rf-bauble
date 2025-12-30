package com.moremod.integration.jei.generator;

import com.moremod.init.ModItems;
import com.moremod.item.energy.ItemOilBucket;
import com.moremod.item.energy.ItemPlantOilBucket;
import com.moremod.tile.TileEntityBioGenerator;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 发电机配方生成器
 * 用于JEI显示
 */
public class GeneratorRecipes {

    /**
     * 获取石油发电机的所有燃料
     */
    public static List<GeneratorFuel> getOilGeneratorFuels() {
        List<GeneratorFuel> fuels = new ArrayList<>();

        // 原油桶
        if (ModItems.CRUDE_OIL_BUCKET != null) {
            int burnTime = ItemOilBucket.BURN_TIME;
            int totalRF = ItemOilBucket.RF_PER_BUCKET;
            int rfPerTick = totalRF / burnTime;
            fuels.add(new GeneratorFuel(
                new ItemStack(ModItems.CRUDE_OIL_BUCKET),
                totalRF,
                burnTime,
                rfPerTick
            ));
        }

        // 植物油桶
        if (ModItems.PLANT_OIL_BUCKET != null) {
            int burnTime = ItemPlantOilBucket.BURN_TIME;
            int totalRF = ItemPlantOilBucket.RF_PER_BUCKET;
            int rfPerTick = totalRF / burnTime;
            fuels.add(new GeneratorFuel(
                new ItemStack(ModItems.PLANT_OIL_BUCKET),
                totalRF,
                burnTime,
                rfPerTick
            ));
        }

        return fuels;
    }

    /**
     * 获取生物质发电机的所有燃料
     */
    public static List<GeneratorFuel> getBioGeneratorFuels() {
        List<GeneratorFuel> fuels = new ArrayList<>();
        int rfPerTick = 40; // 生物质发电机的RF/tick

        // 种子类 - 低能量
        addBioFuel(fuels, Items.WHEAT_SEEDS, 200, rfPerTick);
        addBioFuel(fuels, Items.MELON_SEEDS, 200, rfPerTick);
        addBioFuel(fuels, Items.PUMPKIN_SEEDS, 200, rfPerTick);
        addBioFuel(fuels, Items.BEETROOT_SEEDS, 200, rfPerTick);

        // 作物类 - 中等能量
        addBioFuel(fuels, Items.WHEAT, 400, rfPerTick);
        addBioFuel(fuels, Items.CARROT, 400, rfPerTick);
        addBioFuel(fuels, Items.POTATO, 400, rfPerTick);
        addBioFuel(fuels, Items.BEETROOT, 400, rfPerTick);
        addBioFuel(fuels, Items.MELON, 300, rfPerTick);
        addBioFuel(fuels, Items.APPLE, 500, rfPerTick);
        addBioFuel(fuels, Items.REEDS, 300, rfPerTick);

        // 树苗 - 较高能量
        addBioFuelBlock(fuels, Blocks.SAPLING, 800, rfPerTick);

        // 腐肉
        addBioFuel(fuels, Items.ROTTEN_FLESH, 600, rfPerTick);

        // 其他植物
        addBioFuelBlock(fuels, Blocks.TALLGRASS, 150, rfPerTick);
        addBioFuelBlock(fuels, Blocks.RED_FLOWER, 200, rfPerTick);
        addBioFuelBlock(fuels, Blocks.YELLOW_FLOWER, 200, rfPerTick);
        addBioFuelBlock(fuels, Blocks.VINE, 250, rfPerTick);
        addBioFuelBlock(fuels, Blocks.WATERLILY, 300, rfPerTick);
        addBioFuelBlock(fuels, Blocks.CACTUS, 400, rfPerTick);
        addBioFuelBlock(fuels, Blocks.PUMPKIN, 600, rfPerTick);
        addBioFuelBlock(fuels, Blocks.MELON_BLOCK, 800, rfPerTick);

        return fuels;
    }

    private static void addBioFuel(List<GeneratorFuel> fuels, Item item, int totalRF, int rfPerTick) {
        if (item != null) {
            int burnTime = totalRF / rfPerTick;
            fuels.add(new GeneratorFuel(new ItemStack(item), totalRF, burnTime, rfPerTick));
        }
    }

    private static void addBioFuelBlock(List<GeneratorFuel> fuels, Block block, int totalRF, int rfPerTick) {
        if (block != null) {
            Item item = Item.getItemFromBlock(block);
            if (item != null && item != Items.AIR) {
                int burnTime = totalRF / rfPerTick;
                fuels.add(new GeneratorFuel(new ItemStack(item), totalRF, burnTime, rfPerTick));
            }
        }
    }
}
