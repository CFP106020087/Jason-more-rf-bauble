package com.moremod.init;

import com.moremod.moremod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * 模組液體註冊
 *
 * 註冊原油和植物油液體，支持管道系統
 */
public class ModFluids {

    // 原油 - 黑色粘稠液體
    public static Fluid CRUDE_OIL;

    // 植物油 - 黃綠色液體
    public static Fluid PLANT_OIL;

    /**
     * 在 preInit 階段調用，註冊液體
     * 必須在 FluidRegistry.enableUniversalBucket() 之後調用
     */
    public static void registerFluids() {
        // 原油
        CRUDE_OIL = new Fluid(
                "crude_oil",
                new ResourceLocation(moremod.MODID, "blocks/crude_oil_still"),
                new ResourceLocation(moremod.MODID, "blocks/crude_oil_flow")
        ) {
            @Override
            public int getColor() {
                return 0xFF1A1A1A; // 深黑色
            }
        };
        CRUDE_OIL.setDensity(900);        // 比水輕一點
        CRUDE_OIL.setViscosity(3000);     // 粘稠
        CRUDE_OIL.setLuminosity(0);       // 不發光
        CRUDE_OIL.setTemperature(300);    // 常溫

        if (!FluidRegistry.isFluidRegistered(CRUDE_OIL)) {
            FluidRegistry.registerFluid(CRUDE_OIL);
            FluidRegistry.addBucketForFluid(CRUDE_OIL);
        } else {
            CRUDE_OIL = FluidRegistry.getFluid("crude_oil");
        }

        // 植物油
        PLANT_OIL = new Fluid(
                "plant_oil",
                new ResourceLocation(moremod.MODID, "blocks/plant_oil_still"),
                new ResourceLocation(moremod.MODID, "blocks/plant_oil_flow")
        ) {
            @Override
            public int getColor() {
                return 0xFFAACC44; // 黃綠色
            }
        };
        PLANT_OIL.setDensity(920);        // 比水輕
        PLANT_OIL.setViscosity(1500);     // 中等粘稠
        PLANT_OIL.setLuminosity(0);       // 不發光
        PLANT_OIL.setTemperature(300);    // 常溫

        if (!FluidRegistry.isFluidRegistered(PLANT_OIL)) {
            FluidRegistry.registerFluid(PLANT_OIL);
            FluidRegistry.addBucketForFluid(PLANT_OIL);
        } else {
            PLANT_OIL = FluidRegistry.getFluid("plant_oil");
        }
    }

    /**
     * 獲取原油液體
     */
    public static Fluid getCrudeOil() {
        if (CRUDE_OIL == null) {
            CRUDE_OIL = FluidRegistry.getFluid("crude_oil");
        }
        return CRUDE_OIL;
    }

    /**
     * 獲取植物油液體
     */
    public static Fluid getPlantOil() {
        if (PLANT_OIL == null) {
            PLANT_OIL = FluidRegistry.getFluid("plant_oil");
        }
        return PLANT_OIL;
    }
}
