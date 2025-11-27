package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = "moremod", name = "moremod/behavior_chip")
@Config.LangKey("config.moremod.behavior_chip")
public class BehaviorChipConfig {
    
    @Config.Name("分析間隔")
    @Config.Comment("重新分析玩家行為的時間間隔（小時）")
    @Config.RangeInt(min = 1, max = 24)
    public static int analysisIntervalHours = 1;
    
    @Config.Name("預測模式解鎖時間")
    @Config.Comment("需要裝備多少小時才能解鎖預測模式")
    @Config.RangeInt(min = 10, max = 500)
    public static int predictModeHours = 100;
    
    @Config.Name("數據衰減啟用")
    @Config.Comment("是否啟用數據衰減（避免數據無限增長）")
    public static boolean enableDataDecay = true;
    
    @Config.Name("數據衰減週期")
    @Config.Comment("每多少小時衰減一次數據（保留 90%）")
    @Config.RangeInt(min = 24, max = 168)
    public static int decayIntervalHours = 72;
    
    @Config.Name("增益倍率")
    @Config.Comment({
        "各玩家類型的增益倍率",
        "格式：基礎增益 / 預測模式增益"
    })
    public static BuffMultipliers buffMultipliers = new BuffMultipliers();
    
    public static class BuffMultipliers {
        @Config.Name("戰鬥型增益")
        @Config.Comment("攻擊力和攻擊速度的增益倍率")
        @Config.RangeDouble(min = 0.0, max = 2.0)
        public double combatMultiplier = 1.0;
        
        @Config.Name("探索型增益")
        @Config.Comment("移動速度的增益倍率")
        @Config.RangeDouble(min = 0.0, max = 2.0)
        public double explorerMultiplier = 1.0;
        
        @Config.Name("採礦型增益")
        @Config.Comment("挖掘速度的增益倍率")
        @Config.RangeDouble(min = 0.0, max = 2.0)
        public double minerMultiplier = 1.0;
        
        @Config.Name("建築型增益")
        @Config.Comment("建築相關的增益倍率")
        @Config.RangeDouble(min = 0.0, max = 2.0)
        public double builderMultiplier = 1.0;
        
        @Config.Name("農業型增益")
        @Config.Comment("農業相關的增益倍率")
        @Config.RangeDouble(min = 0.0, max = 2.0)
        public double farmerMultiplier = 1.0;
    }
    
    @Config.Name("特殊效果")
    @Config.Comment("各類型玩家的特殊效果開關")
    public static SpecialEffects specialEffects = new SpecialEffects();
    
    public static class SpecialEffects {
        @Config.Name("戰鬥型：敵人附近獲得力量")
        public boolean combatStrengthNearEnemy = true;
        
        @Config.Name("探索型：夜視效果")
        public boolean explorerNightVision = true;
        
        @Config.Name("採礦型：礦物額外掉落機率")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double minerExtraDropChance = 0.15;
        
        @Config.Name("建築型：方塊節省機率")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double builderBlockSaveChance = 0.10;
        
        @Config.Name("農業型：作物額外掉落機率")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double farmerExtraDropChance = 0.25;
        
        @Config.Name("農業型：飽食度恢復")
        public boolean farmerHungerRestore = true;
    }
    
    @Mod.EventBusSubscriber(modid = "moremod")
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("moremod")) {
                ConfigManager.sync("moremod", Config.Type.INSTANCE);
            }
        }
    }
}
