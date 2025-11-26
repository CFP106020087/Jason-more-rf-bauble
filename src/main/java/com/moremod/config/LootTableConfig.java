package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = "moremod", name = "moremod/loot_tables")
@Mod.EventBusSubscriber(modid = "moremod")
public class LootTableConfig {

    @Config.Comment("Enable loot table modifications")
    public static boolean enableLootTables = true;

    @Config.Comment("Debug mode - prints loot table names to console")
    public static boolean debugMode = false;

    @Config.Comment("Item settings")
    public static ItemSettings items = new ItemSettings();

    @Config.Comment("Vanilla chest settings")
    public static VanillaChests vanilla = new VanillaChests();

    @Config.Comment("Modded structure settings")
    public static ModdedStructures modded = new ModdedStructures();

    @Config.Comment("Universal settings for unspecified mod chests")
    public static UniversalSettings universal = new UniversalSettings();

    /**
     * 每个物品的独立配置
     */
    public static class ItemSettings {
        @Config.Comment("Antikythera Gear settings")
        public ItemConfig antikytheraGear = new ItemConfig(true, 1.0f);

        @Config.Comment("Ancient Component settings")
        public ItemConfig ancientComponent = new ItemConfig(true, 0.5f);

        @Config.Comment("Mysterious Dust settings")
        public ItemConfig mysteriousDust = new ItemConfig(true, 2.0f);

        @Config.Comment("Rare Crystal settings")
        public ItemConfig rareCrystal = new ItemConfig(true, 0.5f);  // ✅ 修改：启用 + 0.5倍（与component一致）
    }

    /**
     * 单个物品的配置
     */
    public static class ItemConfig {
        @Config.Comment("Enable this item in loot tables")
        public boolean enabled;

        @Config.Comment("Weight multiplier for this item (0.5 = half weight, 2.0 = double weight)")
        @Config.RangeDouble(min = 0.0, max = 10.0)
        public float weightMultiplier;

        public ItemConfig() {
            this(true, 1.0f);
        }

        public ItemConfig(boolean enabled, float multiplier) {
            this.enabled = enabled;
            this.weightMultiplier = multiplier;
        }
    }

    /**
     * 原版箱子配置
     */
    public static class VanillaChests {
        @Config.Comment("Simple Dungeon")
        public ChestLootSettings simpleDungeon = new ChestLootSettings(
                15, 1, 2,  // gear
                8, 1, 1,   // component
                20, 2, 4,  // dust
                8, 1, 1    // crystal ✅ 修改：3 → 8
        );

        @Config.Comment("Abandoned Mineshaft")
        public ChestLootSettings abandonedMineshaft = new ChestLootSettings(
                10, 1, 1,  // gear
                5, 1, 1,   // component
                15, 1, 3,  // dust
                5, 1, 1    // crystal ✅ 修改：2 → 5
        );

        @Config.Comment("Stronghold (all types)")
        public ChestLootSettings stronghold = new ChestLootSettings(
                25, 1, 3,  // gear
                12, 1, 2,  // component
                20, 2, 4,  // dust
                12, 1, 2   // crystal ✅ 修改：5 → 12，掉落数 1 → 1-2
        );

        @Config.Comment("Desert Pyramid")
        public ChestLootSettings desertPyramid = new ChestLootSettings(
                20, 1, 2,  // gear
                10, 1, 1,  // component
                18, 2, 4,  // dust
                10, 1, 1   // crystal ✅ 修改：4 → 10
        );

        @Config.Comment("Jungle Temple")
        public ChestLootSettings jungleTemple = new ChestLootSettings(
                18, 1, 2,  // gear
                9, 1, 1,   // component
                16, 2, 3,  // dust
                9, 1, 1    // crystal ✅ 修改：4 → 9
        );

        @Config.Comment("End City Treasure")
        public ChestLootSettings endCity = new ChestLootSettings(
                35, 2, 4,  // gear
                20, 1, 2,  // component
                30, 3, 6,  // dust
                20, 1, 2   // crystal ✅ 修改：10 → 20
        );

        @Config.Comment("Village Blacksmith")
        public ChestLootSettings villageBlacksmith = new ChestLootSettings(
                8, 1, 1,   // gear
                4, 1, 1,   // component
                10, 1, 2,  // dust
                4, 1, 1    // crystal ✅ 修改：1 → 4
        );

        @Config.Comment("Nether Bridge")
        public ChestLootSettings netherBridge = new ChestLootSettings(
                15, 1, 2,  // gear
                8, 1, 1,   // component
                12, 2, 3,  // dust
                8, 1, 1    // crystal ✅ 修改：3 → 8
        );

        @Config.Comment("Woodland Mansion")
        public ChestLootSettings woodlandMansion = new ChestLootSettings(
                22, 1, 3,  // gear
                12, 1, 2,  // component
                18, 2, 4,  // dust
                12, 1, 2   // crystal ✅ 修改：6 → 12，掉落数改为1-2
        );
    }

    /**
     * 每个箱子类型的物品设置
     */
    public static class ChestLootSettings {
        @Config.Comment("Antikythera Gear")
        public LootSettings gear;

        @Config.Comment("Ancient Component")
        public LootSettings component;

        @Config.Comment("Mysterious Dust")
        public LootSettings dust;

        @Config.Comment("Rare Crystal")
        public LootSettings crystal;

        public ChestLootSettings() {
            this(10, 1, 1, 5, 1, 1, 8, 1, 2, 2, 1, 1);
        }

        public ChestLootSettings(
                int gearWeight, int gearMin, int gearMax,
                int compWeight, int compMin, int compMax,
                int dustWeight, int dustMin, int dustMax,
                int crystalWeight, int crystalMin, int crystalMax) {

            this.gear = new LootSettings(gearWeight, gearMin, gearMax);
            this.component = new LootSettings(compWeight, compMin, compMax);
            this.dust = new LootSettings(dustWeight, dustMin, dustMax);
            this.crystal = new LootSettings(crystalWeight, crystalMin, crystalMax);
        }
    }

    /**
     * 战利品设置
     */
    public static class LootSettings {
        @Config.RangeInt(min = 0, max = 100)
        public int weight;

        @Config.RangeInt(min = 0, max = 64)
        public int minCount;

        @Config.RangeInt(min = 0, max = 64)
        public int maxCount;

        public LootSettings() {
            this(10, 1, 1);
        }

        public LootSettings(int weight, int min, int max) {
            this.weight = weight;
            this.minCount = min;
            this.maxCount = max;
        }
    }

    /**
     * 模组结构配置
     */
    public static class ModdedStructures {
        @Config.Comment("Recurrent Complex settings")
        public RecurrentComplexSettings recurrentComplex = new RecurrentComplexSettings();

        @Config.Comment("Battle Towers settings")
        public BattleTowersSettings battleTowers = new BattleTowersSettings();
    }

    /**
     * Recurrent Complex 专门配置
     */
    public static class RecurrentComplexSettings {
        @Config.Comment("Enable Recurrent Complex loot injection")
        public boolean enabled = true;

        @Config.Comment("Generic/Structure chests")
        public ChestLootSettings generic = new ChestLootSettings(
                18, 1, 2,  // gear
                10, 1, 1,  // component
                15, 2, 3,  // dust
                10, 1, 1   // crystal ✅ 修改：4 → 10
        );

        @Config.Comment("Rare/Treasure chests")
        public ChestLootSettings rare = new ChestLootSettings(
                25, 2, 3,  // gear
                15, 1, 2,  // component
                20, 3, 5,  // dust
                15, 1, 2   // crystal ✅ 修改：8 → 15
        );

        @Config.Comment("Common/Basic chests")
        public ChestLootSettings common = new ChestLootSettings(
                12, 1, 1,  // gear
                6, 1, 1,   // component
                10, 1, 2,  // dust
                6, 1, 1    // crystal ✅ 修改：2 → 6
        );

        @Config.Comment("Dungeon-type structures")
        public ChestLootSettings dungeon = new ChestLootSettings(
                20, 1, 2,  // gear
                12, 1, 1,  // component
                18, 2, 4,  // dust
                12, 1, 1   // crystal ✅ 修改：5 → 12
        );

        @Config.Comment("Tower-type structures")
        public ChestLootSettings tower = new ChestLootSettings(
                22, 1, 3,  // gear
                14, 1, 2,  // component
                16, 2, 3,  // dust
                14, 1, 2   // crystal ✅ 修改：6 → 14
        );
    }

    /**
     * Battle Towers 专门配置
     */
    public static class BattleTowersSettings {
        @Config.Comment("Enable Battle Towers loot injection")
        public boolean enabled = true;

        @Config.Comment("Top/Boss floor")
        public ChestLootSettings top = new ChestLootSettings(
                30, 2, 4,  // gear
                18, 1, 2,  // component
                25, 3, 5,  // dust
                18, 1, 2   // crystal ✅ 修改：10 → 18
        );

        @Config.Comment("Middle floors")
        public ChestLootSettings middle = new ChestLootSettings(
                20, 1, 2,  // gear
                10, 1, 1,  // component
                15, 2, 3,  // dust
                10, 1, 1   // crystal ✅ 修改：5 → 10
        );

        @Config.Comment("Bottom/Common floors")
        public ChestLootSettings bottom = new ChestLootSettings(
                10, 1, 1,  // gear
                5, 1, 1,   // component
                8, 1, 2,   // dust
                5, 1, 1    // crystal ✅ 修改：2 → 5
        );

        @Config.Comment("Generic tower chests")
        public ChestLootSettings generic = new ChestLootSettings(
                15, 1, 2,  // gear
                8, 1, 1,   // component
                12, 2, 3,  // dust
                8, 1, 1    // crystal ✅ 修改：4 → 8
        );
    }

    /**
     * 通用设置
     */
    public static class UniversalSettings {
        @Config.Comment("Enable universal injection for unknown mod chests")
        public boolean enabled = true;

        @Config.Comment("Rare/Treasure/Epic chests")
        public ChestLootSettings rareChests = new ChestLootSettings(
                10, 1, 2,  // gear
                6, 1, 1,   // component
                8, 1, 3,   // dust
                6, 1, 1    // crystal ✅ 修改：3 → 6
        );

        @Config.Comment("Common mod chests")
        public ChestLootSettings commonChests = new ChestLootSettings(
                5, 1, 1,   // gear
                3, 1, 1,   // component
                4, 1, 2,   // dust
                3, 1, 1    // crystal ✅ 修改：1 → 3
        );
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals("moremod")) {
            ConfigManager.sync("moremod", Config.Type.INSTANCE);
        }
    }
}