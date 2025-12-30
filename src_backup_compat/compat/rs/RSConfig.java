package com.moremod.compat.rs;

import net.minecraftforge.common.config.Config;

@Config(modid = "moremod", name = "moremod/rs_infinity_booster")
@Config.LangKey("moremod.config.rs_infinity_booster")
public class RSConfig {
    
    @Config.Comment("Energy usage for the Infinity Card (FE/t)")
    @Config.RangeInt(min = 0, max = 100000)
    public static int infinityCardEnergyUsage = 1000;
    
    @Config.Comment("Energy usage for the Dimension Card (FE/t)")
    @Config.RangeInt(min = 0, max = 100000)
    public static int dimensionCardEnergyUsage = 2000;
    
    @Config.Comment("Enable the Infinity Card")
    public static boolean enableInfinityCard = true;
    
    @Config.Comment("Enable the Dimension Card")
    public static boolean enableDimensionCard = true;
}
