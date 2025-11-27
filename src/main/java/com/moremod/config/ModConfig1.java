package com.moremod.config;

import com.moremod.moremod;
import net.minecraftforge.common.config.Config;

@Config(modid = moremod.MODID)
public class ModConfig1 {

    @Config.Name("Wisdom Fountain Settings")
    @Config.Comment("Settings for the Wisdom Fountain multiblock structure")
    public static WisdomFountainSettings wisdomFountain = new WisdomFountainSettings();

    public static class WisdomFountainSettings {
        @Config.Name("Effect Range")
        @Config.Comment("Range of the fountain's effect in blocks")
        @Config.RangeInt(min = 5, max = 50)
        public int range = 10;

        @Config.Name("Can Transform Professional Villagers")
        @Config.Comment("Whether already-professional villagers can be transformed")
        public boolean canTransformProfessional = false;

        @Config.Name("Emerald Price Multiplier")
        @Config.Comment("Multiplier for emerald costs of enchanted books")
        @Config.RangeDouble(min = 0.1, max = 10.0)
        public double priceMultiplier = 1.0;

        @Config.Name("Give Luck Effect")
        @Config.Comment("Whether players near the fountain get Luck effect")
        public boolean giveLuckEffect = true;

        @Config.Name("Particle Effects")
        @Config.Comment("Enable particle effects for the fountain")
        public boolean enableParticles = true;

        @Config.Name("Structure Check Interval")
        @Config.Comment("Ticks between structure validation checks")
        @Config.RangeInt(min = 20, max = 200)
        public int checkInterval = 20;
    }
}