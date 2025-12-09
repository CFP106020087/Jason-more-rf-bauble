package com.moremod.quarry.simulation;

import com.moremod.quarry.QuarryConfig;
import com.moremod.quarry.QuarryMode;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;

/**
 * 虚拟挖掘模拟器
 * 核心类：根据工作模式模拟产出
 */
public class VirtualMiningSimulator {
    
    private static VirtualMiningSimulator INSTANCE;
    
    private final BiomeOreRegistry oreRegistry;
    private final MobDropSimulator mobDropSimulator;
    private final QuarryLootManager lootManager;
    
    // 统计数据
    private long totalBlocksMined = 0;
    private long totalMobsKilled = 0;
    private long totalLootGenerated = 0;
    
    public static VirtualMiningSimulator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VirtualMiningSimulator();
        }
        return INSTANCE;
    }
    
    private VirtualMiningSimulator() {
        this.oreRegistry = BiomeOreRegistry.getInstance();
        this.mobDropSimulator = MobDropSimulator.getInstance();
        this.lootManager = QuarryLootManager.getInstance();
    }
    
    /**
     * 初始化（在 postInit 调用）
     */
    public void initialize() {
        oreRegistry.initialize();
    }
    
    /**
     * 执行一次模拟操作
     * 
     * @param world 世界
     * @param mode 工作模式
     * @param biome 目标生物群系
     * @param enchantedBook 附魔书（可为空）
     * @param rand 随机数生成器
     * @return 产出的物品列表
     */
    public List<ItemStack> simulate(WorldServer world, QuarryMode mode, Biome biome, 
                                    ItemStack enchantedBook, Random rand) {
        // 解析附魔
        int fortuneLevel = getEnchantmentLevel(enchantedBook, Enchantments.FORTUNE);
        int silkTouchLevel = getEnchantmentLevel(enchantedBook, Enchantments.SILK_TOUCH);
        int lootingLevel = getEnchantmentLevel(enchantedBook, Enchantments.LOOTING);
        float luckLevel = getEnchantmentLevel(enchantedBook, Enchantments.LUCK_OF_THE_SEA);
        
        switch (mode) {
            case MINING:
                return simulateMining(world, biome, fortuneLevel, silkTouchLevel > 0, rand);
            case MOB_DROPS:
                return simulateMobKill(world, biome, lootingLevel, rand);
            case LOOT_TABLE:
                return simulateLoot(world, luckLevel, rand);
            default:
                return Collections.emptyList();
        }
    }
    
    /**
     * 模拟挖矿
     */
    private List<ItemStack> simulateMining(WorldServer world, Biome biome, 
                                           int fortuneLevel, boolean silkTouch, Random rand) {
        List<ItemStack> drops = new ArrayList<>();
        
        // 模拟挖掘多个方块
        int blocksToMine = QuarryConfig.BLOCKS_PER_OPERATION;
        
        for (int i = 0; i < blocksToMine; i++) {
            // 模拟在随机高度挖掘
            int y = rand.nextInt(QuarryConfig.VIRTUAL_WORLD_HEIGHT);
            
            // 大部分是石头
            if (rand.nextFloat() < 0.85f) {
                // 85% 概率是普通石头，不产出（或产出圆石）
                if (silkTouch && rand.nextFloat() < 0.1f) {
                    drops.add(new ItemStack(Blocks.STONE));
                }
                continue;
            }
            
            // 选择矿物
            BiomeOreRegistry.OreEntry oreEntry = oreRegistry.getRandomOre(biome, rand);
            if (oreEntry == null) continue;
            
            // 检查高度是否合适
            float heightMult = oreEntry.getHeightMultiplier(y);
            if (rand.nextFloat() > heightMult) continue;
            
            // 获取掉落物
            List<ItemStack> oreDrops = getBlockDrops(oreEntry.state, fortuneLevel, silkTouch, rand);
            drops.addAll(oreDrops);
        }
        
        totalBlocksMined += blocksToMine;
        
        // 合并相同物品
        return mergeStacks(drops);
    }
    
    /**
     * 获取方块掉落物
     */
    private List<ItemStack> getBlockDrops(IBlockState state, int fortuneLevel, boolean silkTouch, Random rand) {
        List<ItemStack> drops = new ArrayList<>();
        Block block = state.getBlock();
        
        if (silkTouch) {
            // 精准采集 - 直接掉落方块本身
            Item item = Item.getItemFromBlock(block);
            if (item != null) {
                drops.add(new ItemStack(item, 1, block.getMetaFromState(state)));
            }
        } else {
            // 正常掉落
            NonNullList<ItemStack> blockDrops = NonNullList.create();
            
            try {
                // 尝试获取方块的掉落物
                block.getDrops(blockDrops, null, null, state, fortuneLevel);
                drops.addAll(blockDrops);
            } catch (Exception e) {
                // 如果失败，使用简单掉落
                drops.add(new ItemStack(block, 1, block.getMetaFromState(state)));
            }
            
            // 时运加成（对于某些矿物）
            if (fortuneLevel > 0 && isFortuneAffected(block)) {
                for (int i = 0; i < drops.size(); i++) {
                    ItemStack stack = drops.get(i);
                    int bonus = rand.nextInt(fortuneLevel + 1);
                    if (bonus > 0) {
                        stack.grow(bonus);
                    }
                }
            }
        }
        
        return drops;
    }
    
    /**
     * 检查方块是否受时运影响
     */
    private boolean isFortuneAffected(Block block) {
        return block == Blocks.COAL_ORE ||
               block == Blocks.DIAMOND_ORE ||
               block == Blocks.EMERALD_ORE ||
               block == Blocks.LAPIS_ORE ||
               block == Blocks.REDSTONE_ORE ||
               block == Blocks.LIT_REDSTONE_ORE ||
               block == Blocks.QUARTZ_ORE ||
               isModOreAffectedByFortune(block);
    }
    
    /**
     * 检查模组矿物是否受时运影响
     */
    private boolean isModOreAffectedByFortune(Block block) {
        // 检查矿物词典
        ItemStack stack = new ItemStack(block);
        if (stack.isEmpty()) return false;
        
        int[] ids = OreDictionary.getOreIDs(stack);
        for (int id : ids) {
            String name = OreDictionary.getOreName(id);
            // 宝石类矿物通常受时运影响
            if (name.startsWith("gem") || name.contains("Crystal") || name.contains("crystal")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 模拟击杀怪物
     */
    private List<ItemStack> simulateMobKill(WorldServer world, Biome biome, int lootingLevel, Random rand) {
        List<ItemStack> drops = mobDropSimulator.simulateKill(world, biome, lootingLevel, rand);
        totalMobsKilled++;
        return drops;
    }
    
    /**
     * 模拟战利品生成
     */
    private List<ItemStack> simulateLoot(WorldServer world, float luck, Random rand) {
        List<ItemStack> drops = lootManager.generateLoot(world, luck, rand);
        totalLootGenerated++;
        return drops;
    }
    
    /**
     * 获取附魔等级
     */
    private int getEnchantmentLevel(ItemStack book, Enchantment enchantment) {
        if (book == null || book.isEmpty()) return 0;
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(book);
        return enchantments.getOrDefault(enchantment, 0);
    }
    
    /**
     * 合并相同物品堆
     */
    public static List<ItemStack> mergeStacks(List<ItemStack> input) {
        Map<String, ItemStack> merged = new LinkedHashMap<>();
        
        for (ItemStack stack : input) {
            if (stack.isEmpty()) continue;
            
            String key = stack.getItem().getRegistryName() + "@" + stack.getMetadata();
            if (stack.hasTagCompound()) {
                key += "@" + stack.getTagCompound().toString();
            }
            
            ItemStack existing = merged.get(key);
            if (existing != null) {
                existing.grow(stack.getCount());
            } else {
                merged.put(key, stack.copy());
            }
        }
        
        return new ArrayList<>(merged.values());
    }
    
    /**
     * 计算操作间隔（考虑效率附魔）
     */
    public int calculateOperationTicks(ItemStack enchantedBook) {
        int baseTicks = QuarryConfig.BASE_TICKS_PER_OPERATION;
        int efficiencyLevel = getEnchantmentLevel(enchantedBook, Enchantments.EFFICIENCY);
        
        if (efficiencyLevel > 0) {
            float reduction = efficiencyLevel * QuarryConfig.EFFICIENCY_SPEED_BONUS;
            baseTicks = (int) (baseTicks * (1f - reduction));
        }
        
        return Math.max(QuarryConfig.MIN_TICKS_PER_OPERATION, baseTicks);
    }
    
    /**
     * 计算能量消耗（考虑效率附魔 - 效率越高消耗越多）
     */
    public int calculateEnergyPerOperation(ItemStack enchantedBook) {
        int baseEnergy = QuarryConfig.ENERGY_PER_OPERATION;
        int efficiencyLevel = getEnchantmentLevel(enchantedBook, Enchantments.EFFICIENCY);
        
        // 效率附魔增加能量消耗（平衡速度提升）
        if (efficiencyLevel > 0) {
            baseEnergy += baseEnergy * efficiencyLevel * 0.2;
        }
        
        return baseEnergy;
    }
    
    // 统计数据访问
    public long getTotalBlocksMined() { return totalBlocksMined; }
    public long getTotalMobsKilled() { return totalMobsKilled; }
    public long getTotalLootGenerated() { return totalLootGenerated; }
}
