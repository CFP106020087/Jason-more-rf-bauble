package com.moremod.handler;

import com.moremod.config.LootTableConfig;
import com.moremod.config.LootTableConfig.*;
import net.minecraft.item.Item;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.RandomChance;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.SetCount;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.moremod.item.ModMaterialItems.*;
import static com.moremod.item.RegisterItem.ANTIKYTHERA_GEAR;
import static com.moremod.item.RegisterItem.BATTERY_BAUBLE;
import static com.moremod.item.RegisterItem.TEMPORAL_HEART;
import static com.moremod.item.RegisterItem.BLOODY_THIRST_MASK;
import static com.moremod.init.ModItems.SACRED_HEART;
import static com.moremod.init.ModItems.PEACE_EMBLEM;
import static com.moremod.init.ModItems.GUARDIAN_SCALE;
import static com.moremod.init.ModItems.COURAGE_BLADE;
import static com.moremod.init.ModItems.FROST_DEW;
import static com.moremod.init.ModItems.SOUL_ANCHOR;
import static com.moremod.init.ModItems.SLUMBER_SACHET;

@Mod.EventBusSubscriber(modid = "moremod")
public class LootTableHandler {

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!LootTableConfig.enableLootTables) {
            return;
        }

        String name = event.getName().toString();

        if (LootTableConfig.debugMode) {
            System.out.println("[moremod] Loading loot table: " + name);
        }

        // 对所有包含 chest 的战利品表注入稀有物品
        if (name.contains("chest") &&
                !name.contains("player") &&
                !name.contains("entities/")) {
            injectRareBaubles(event, 0.001f);        // 0.1% 概率的稀有饰品（含归一心原石）
            injectSacredRelics(event, 0.003f);       // 0.3% 概率的七咒圣物（3倍于稀有饰品）
        }

        // === 原版战利品表 ===
        if (name.equals("minecraft:chests/simple_dungeon")) {
            addMultipleItems(event, LootTableConfig.vanilla.simpleDungeon);
        }
        else if (name.equals("minecraft:chests/abandoned_mineshaft")) {
            addMultipleItems(event, LootTableConfig.vanilla.abandonedMineshaft);
        }
        else if (name.equals("minecraft:chests/stronghold_corridor") ||
                name.equals("minecraft:chests/stronghold_crossing") ||
                name.equals("minecraft:chests/stronghold_library")) {
            addMultipleItems(event, LootTableConfig.vanilla.stronghold);
        }
        else if (name.equals("minecraft:chests/desert_pyramid")) {
            addMultipleItems(event, LootTableConfig.vanilla.desertPyramid);
        }
        else if (name.equals("minecraft:chests/jungle_temple")) {
            addMultipleItems(event, LootTableConfig.vanilla.jungleTemple);
        }
        else if (name.equals("minecraft:chests/end_city_treasure")) {
            addMultipleItems(event, LootTableConfig.vanilla.endCity);
        }
        else if (name.equals("minecraft:chests/village_blacksmith")) {
            addMultipleItems(event, LootTableConfig.vanilla.villageBlacksmith);
        }
        else if (name.equals("minecraft:chests/nether_bridge")) {
            addMultipleItems(event, LootTableConfig.vanilla.netherBridge);
        }
        else if (name.equals("minecraft:chests/woodland_mansion")) {
            addMultipleItems(event, LootTableConfig.vanilla.woodlandMansion);
        }

        // === Recurrent Complex 支持 ===
        else if (LootTableConfig.modded.recurrentComplex.enabled &&
                (name.contains("reccomplex:") || name.contains("recurrentcomplex:"))) {

            if (name.contains("rare") || name.contains("treasure")) {
                addMultipleItems(event, LootTableConfig.modded.recurrentComplex.rare);
            }
            else if (name.contains("dungeon")) {
                addMultipleItems(event, LootTableConfig.modded.recurrentComplex.dungeon);
            }
            else if (name.contains("tower")) {
                addMultipleItems(event, LootTableConfig.modded.recurrentComplex.tower);
            }
            else if (name.contains("generic") || name.contains("structure")) {
                addMultipleItems(event, LootTableConfig.modded.recurrentComplex.generic);
            }
            else {
                addMultipleItems(event, LootTableConfig.modded.recurrentComplex.common);
            }
        }
        else if (LootTableConfig.modded.recurrentComplex.enabled &&
                name.contains("rc_") && (name.contains("chest") || name.contains("loot"))) {
            addMultipleItems(event, LootTableConfig.modded.recurrentComplex.generic);
        }

        // === Battle Towers 支持 ===
        else if (LootTableConfig.modded.battleTowers.enabled &&
                (name.contains("battletowers:") || name.contains("atomicstryker:battletowers"))) {

            if (name.contains("top") || name.contains("golem") || name.contains("boss")) {
                addMultipleItems(event, LootTableConfig.modded.battleTowers.top);
            }
            else if (name.contains("middle") || name.contains("floor")) {
                addMultipleItems(event, LootTableConfig.modded.battleTowers.middle);
            }
            else if (name.contains("bottom") || name.contains("common")) {
                addMultipleItems(event, LootTableConfig.modded.battleTowers.bottom);
            }
            else {
                addMultipleItems(event, LootTableConfig.modded.battleTowers.generic);
            }
        }
        else if (LootTableConfig.modded.battleTowers.enabled &&
                name.contains("tower") && (name.contains("chest") || name.contains("loot")) &&
                !name.contains("minecraft:")) {
            addMultipleItems(event, LootTableConfig.modded.battleTowers.generic);
        }

        // === 其他模组的通用支持 ===
        else if (LootTableConfig.universal.enabled &&
                (name.contains("chest") || name.contains("loot") || name.contains("treasure")) &&
                !name.contains("minecraft:") &&
                !name.contains("player") &&
                !name.contains("entities/") &&
                !name.contains("gameplay/")) {

            if (name.contains("treasure") || name.contains("rare") || name.contains("epic")) {
                addMultipleItems(event, LootTableConfig.universal.rareChests);
            }
            else {
                addMultipleItems(event, LootTableConfig.universal.commonChests);
            }
        }
    }

    /**
     * 添加多个物品到战利品表（材料与齿轮）
     */
    private static void addMultipleItems(LootTableLoadEvent event, ChestLootSettings settings) {
        LootPool pool = getOrCreatePool(event.getTable());
        if (pool == null) return;

        // 齿轮
        if (LootTableConfig.items.antikytheraGear.enabled && settings.gear.weight > 0) {
            addItemToPool(pool, ANTIKYTHERA_GEAR, settings.gear,
                    LootTableConfig.items.antikytheraGear.weightMultiplier);
        }

        // 古代组件
        if (LootTableConfig.items.ancientComponent.enabled && settings.component.weight > 0) {
            addItemToPool(pool, ANCIENT_COMPONENT, settings.component,
                    LootTableConfig.items.ancientComponent.weightMultiplier);
        }

        // 神秘粉尘
        if (LootTableConfig.items.mysteriousDust.enabled && settings.dust.weight > 0) {
            addItemToPool(pool, MYSTERIOUS_DUST, settings.dust,
                    LootTableConfig.items.mysteriousDust.weightMultiplier);
        }

        // 稀有水晶
        if (LootTableConfig.items.rareCrystal.enabled && settings.crystal.weight > 0) {
            addItemToPool(pool, RARE_CRYSTAL, settings.crystal,
                    LootTableConfig.items.rareCrystal.weightMultiplier);
        }
    }

    /**
     * 添加物品到池
     */
    private static void addItemToPool(LootPool pool, Item item, LootSettings settings, float multiplier) {
        if (item == null) return;

        int adjustedWeight = Math.round(settings.weight * multiplier);
        if (adjustedWeight <= 0) return;

        LootEntryItem entry = new LootEntryItem(
                item,
                adjustedWeight,
                2,
                new LootFunction[] {
                        new SetCount(new LootCondition[0],
                                new RandomValueRange(settings.minCount, settings.maxCount))
                },
                new LootCondition[0],
                "moremod:" + item.getRegistryName().getPath()
        );

        pool.addEntry(entry);
    }

    /**
     * 获取或创建池
     */
    private static LootPool getOrCreatePool(LootTable table) {
        String[] poolNames = {"main", "pool0", "pool1", "additions"};
        for (String poolName : poolNames) {
            LootPool pool = table.getPool(poolName);
            if (pool != null) {
                return pool;
            }
        }
        LootPool newPool = new LootPool(
                new LootEntry[0],
                new LootCondition[0],
                new RandomValueRange(0, 1),
                new RandomValueRange(0, 0),
                "moremod_loot"
        );
        table.addPool(newPool);
        return table.getPool("moremod_loot");
    }

    /**
     * 在任意箱子战利品表注入1%概率的稀有饰品
     */
    private static void injectRareBaubles(LootTableLoadEvent event, float chance) {
        String poolName = "moremod_rare_baubles";

        // 避免重复添加
        if (event.getTable().getPool(poolName) != null) return;

        // 1%概率条件
        LootCondition[] conditions = new LootCondition[]{ new RandomChance(chance) };

        // 创建新池
        LootPool pool = new LootPool(
                new LootEntry[0],
                conditions,
                new RandomValueRange(1, 1), // 触发时roll一次
                new RandomValueRange(0, 0),
                poolName
        );

        // 添加三个饰品，等权重
        if (BATTERY_BAUBLE != null) {
            pool.addEntry(new LootEntryItem(
                    BATTERY_BAUBLE,
                    1, // 权重
                    1, // quality (不受幸运影响)
                    new LootFunction[0],
                    new LootCondition[0],
                    "moremod:battery_bauble"
            ));
        }

        // 归一心原石
        if (TEMPORAL_HEART != null) {
            pool.addEntry(new LootEntryItem(
                    TEMPORAL_HEART,
                    1,
                    1,
                    new LootFunction[0],
                    new LootCondition[0],
                    "moremod:temporal_heart"
            ));
        }

        if (BLOODY_THIRST_MASK != null) {
            pool.addEntry(new LootEntryItem(
                    BLOODY_THIRST_MASK,
                    1,
                    1,
                    new LootFunction[0],
                    new LootCondition[0],
                    "moremod:bloody_thirst_mask"
            ));
        }

        event.getTable().addPool(pool);

        if (LootTableConfig.debugMode) {
            System.out.println("[moremod] Injected rare baubles (0.1%) into: " + event.getName());
        }
    }

    /**
     * 注入七件仪式圣物
     */
    private static void injectSacredRelics(LootTableLoadEvent event, float chance) {
        String poolName = "moremod_sacred_relics";

        // 避免重复添加
        if (event.getTable().getPool(poolName) != null) return;

        // 概率条件
        LootCondition[] conditions = new LootCondition[]{ new RandomChance(chance) };

        // 创建新池
        LootPool pool = new LootPool(
                new LootEntry[0],
                conditions,
                new RandomValueRange(1, 1), // 触发时roll一次
                new RandomValueRange(0, 0),
                poolName
        );

        // 七件仪式圣物（等权重）
        addRelicEntry(pool, SACRED_HEART, 1, "sacred_heart");
        addRelicEntry(pool, PEACE_EMBLEM, 1, "peace_emblem");
        addRelicEntry(pool, GUARDIAN_SCALE, 1, "guardian_scale");
        addRelicEntry(pool, COURAGE_BLADE, 1, "courage_blade");
        addRelicEntry(pool, FROST_DEW, 1, "frost_dew");
        addRelicEntry(pool, SOUL_ANCHOR, 1, "soul_anchor");
        addRelicEntry(pool, SLUMBER_SACHET, 1, "slumber_sachet");

        event.getTable().addPool(pool);

        if (LootTableConfig.debugMode) {
            System.out.println("[moremod] Injected sacred relics (0.3%) into: " + event.getName());
        }
    }

    /**
     * 添加圣物条目到池
     */
    private static void addRelicEntry(LootPool pool, Item item, int weight, String name) {
        if (item == null) return;

        pool.addEntry(new LootEntryItem(
                item,
                weight,
                1, // quality
                new LootFunction[0],
                new LootCondition[0],
                "moremod:" + name
        ));
    }
}