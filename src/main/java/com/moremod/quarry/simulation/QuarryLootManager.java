package com.moremod.quarry.simulation;

import com.google.common.collect.ImmutableList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 量子采石场战利品表管理器
 * 支持自定义战利品表和注入现有战利品表
 */
public class QuarryLootManager {
    
    private static QuarryLootManager INSTANCE;
    
    // 自定义战利品表
    public static final ResourceLocation QUARRY_COMMON = new ResourceLocation("yourmod", "quarry/common");
    public static final ResourceLocation QUARRY_RARE = new ResourceLocation("yourmod", "quarry/rare");
    public static final ResourceLocation QUARRY_LEGENDARY = new ResourceLocation("yourmod", "quarry/legendary");
    public static final ResourceLocation QUARRY_NETHER = new ResourceLocation("yourmod", "quarry/nether");
    public static final ResourceLocation QUARRY_END = new ResourceLocation("yourmod", "quarry/end");
    
    // 可用的战利品表及其权重
    private final Map<ResourceLocation, Integer> lootTableWeights = new LinkedHashMap<>();
    
    // 注入到其他战利品表的条目
    private final Map<ResourceLocation, List<LootEntry>> injectedEntries = new HashMap<>();
    
    // 自定义条目（运行时添加）
    private final List<CustomLootEntry> customEntries = new ArrayList<>();
    
    public static QuarryLootManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new QuarryLootManager();
        }
        return INSTANCE;
    }
    
    private QuarryLootManager() {
        MinecraftForge.EVENT_BUS.register(this);
        initDefaultLootTables();
    }
    
    /**
     * 初始化默认战利品表
     */
    private void initDefaultLootTables() {
        // 原版宝箱战利品表
        lootTableWeights.put(new ResourceLocation("chests/simple_dungeon"), 20);
        lootTableWeights.put(new ResourceLocation("chests/village_blacksmith"), 15);
        lootTableWeights.put(new ResourceLocation("chests/abandoned_mineshaft"), 15);
        lootTableWeights.put(new ResourceLocation("chests/desert_pyramid"), 10);
        lootTableWeights.put(new ResourceLocation("chests/jungle_temple"), 10);
        lootTableWeights.put(new ResourceLocation("chests/igloo_chest"), 8);
        lootTableWeights.put(new ResourceLocation("chests/stronghold_corridor"), 5);
        lootTableWeights.put(new ResourceLocation("chests/stronghold_crossing"), 5);
        lootTableWeights.put(new ResourceLocation("chests/stronghold_library"), 5);
        lootTableWeights.put(new ResourceLocation("chests/woodland_mansion"), 3);
        lootTableWeights.put(new ResourceLocation("chests/end_city_treasure"), 2);
        lootTableWeights.put(new ResourceLocation("chests/nether_bridge"), 5);
        
        // 自定义战利品表
        lootTableWeights.put(QUARRY_COMMON, 25);
        lootTableWeights.put(QUARRY_RARE, 5);
        lootTableWeights.put(QUARRY_LEGENDARY, 1);
    }
    
    /**
     * 处理战利品表加载事件 - 注入自定义条目
     */
    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        
        // 注入到指定的战利品表
        List<LootEntry> toInject = injectedEntries.get(name);
        if (toInject != null && !toInject.isEmpty()) {
            LootPool mainPool = event.getTable().getPool("main");
            if (mainPool != null) {
                for (LootEntry entry : toInject) {
                    mainPool.addEntry(entry);
                }
            }
        }
        
        // 处理自定义战利品表
        if (name.equals(QUARRY_COMMON)) {
            setupCommonLootTable(event.getTable());
        } else if (name.equals(QUARRY_RARE)) {
            setupRareLootTable(event.getTable());
        } else if (name.equals(QUARRY_LEGENDARY)) {
            setupLegendaryLootTable(event.getTable());
        }
    }
    
    /**
     * 设置普通战利品表
     */
    private void setupCommonLootTable(LootTable table) {
        // 这个方法会在 JSON 不存在时被调用
        // 你可以在 assets/yourmod/loot_tables/quarry/common.json 中定义
        // 或者在这里用代码添加
    }
    
    private void setupRareLootTable(LootTable table) {
        // 稀有战利品
    }
    
    private void setupLegendaryLootTable(LootTable table) {
        // 传说战利品
    }
    
    /**
     * 从战利品表生成物品
     * 
     * @param world 世界
     * @param luck 幸运值
     * @param rand 随机数
     * @return 生成的物品列表
     */
    public List<ItemStack> generateLoot(WorldServer world, float luck, Random rand) {
        List<ItemStack> result = new ArrayList<>();
        
        // 选择战利品表
        ResourceLocation selectedTable = selectLootTable(rand);
        if (selectedTable == null) return result;
        
        try {
            LootTable table = world.getLootTableManager().getLootTableFromLocation(selectedTable);
            LootContext.Builder contextBuilder = new LootContext.Builder(world);
            contextBuilder.withLuck(luck);
            
            result.addAll(table.generateLootForPools(rand, contextBuilder.build()));
            
            // 添加自定义条目
            for (CustomLootEntry customEntry : customEntries) {
                if (rand.nextFloat() < customEntry.chance) {
                    result.add(customEntry.stack.copy());
                }
            }
            
        } catch (Exception e) {
            // 战利品表加载失败，跳过
        }
        
        return result;
    }
    
    /**
     * 根据权重选择战利品表
     */
    @Nullable
    private ResourceLocation selectLootTable(Random rand) {
        int totalWeight = lootTableWeights.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) return null;
        
        int target = rand.nextInt(totalWeight);
        int cumulative = 0;
        
        for (Map.Entry<ResourceLocation, Integer> entry : lootTableWeights.entrySet()) {
            cumulative += entry.getValue();
            if (target < cumulative) {
                return entry.getKey();
            }
        }
        
        return lootTableWeights.keySet().iterator().next();
    }
    
    /**
     * 注册新的战利品表
     * 
     * @param location 战利品表位置
     * @param weight 权重
     */
    public void registerLootTable(ResourceLocation location, int weight) {
        lootTableWeights.put(location, weight);
    }
    
    /**
     * 移除战利品表
     */
    public void removeLootTable(ResourceLocation location) {
        lootTableWeights.remove(location);
    }
    
    /**
     * 设置战利品表权重
     */
    public void setLootTableWeight(ResourceLocation location, int weight) {
        if (lootTableWeights.containsKey(location)) {
            lootTableWeights.put(location, weight);
        }
    }
    
    /**
     * 注入条目到现有战利品表
     * 
     * @param targetTable 目标战利品表
     * @param entry 要注入的条目
     */
    public void injectEntry(ResourceLocation targetTable, LootEntry entry) {
        injectedEntries.computeIfAbsent(targetTable, k -> new ArrayList<>()).add(entry);
    }
    
    /**
     * 添加简单的物品掉落注入
     */
    public void injectItemDrop(ResourceLocation targetTable, ItemStack stack, int weight) {
        LootEntry entry = new LootEntryItem(
            stack.getItem(),
            weight,
            0,
            new LootFunction[0],
            new LootCondition[0],
            "quarry_injected_" + stack.getItem().getRegistryName().getPath()
        );
        injectEntry(targetTable, entry);
    }
    
    /**
     * 添加自定义运行时掉落
     */
    public void addCustomDrop(ItemStack stack, float chance) {
        customEntries.add(new CustomLootEntry(stack, chance));
    }
    
    /**
     * 清除自定义掉落
     */
    public void clearCustomDrops() {
        customEntries.clear();
    }
    
    /**
     * 获取所有注册的战利品表
     */
    public Set<ResourceLocation> getRegisteredLootTables() {
        return Collections.unmodifiableSet(lootTableWeights.keySet());
    }
    
    /**
     * 获取战利品表权重
     */
    public int getLootTableWeight(ResourceLocation location) {
        return lootTableWeights.getOrDefault(location, 0);
    }
    
    /**
     * 自定义掉落条目
     */
    private static class CustomLootEntry {
        final ItemStack stack;
        final float chance;
        
        CustomLootEntry(ItemStack stack, float chance) {
            this.stack = stack.copy();
            this.chance = chance;
        }
    }
    
    /**
     * API: 从其他模组注入战利品
     * 在 FMLPostInitializationEvent 中调用
     */
    public static void addQuarryLoot(ItemStack stack, float chance) {
        getInstance().addCustomDrop(stack, chance);
    }
    
    /**
     * API: 注册新的战利品表供采石场使用
     */
    public static void registerQuarryLootTable(ResourceLocation location, int weight) {
        getInstance().registerLootTable(location, weight);
    }
}
