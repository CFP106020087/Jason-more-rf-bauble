package com.moremod.quarry.simulation;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.*;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 怪物掉落模拟器
 * 模拟在特定生物群系击杀怪物的掉落物
 */
public class MobDropSimulator {
    
    private static MobDropSimulator INSTANCE;
    
    // 缓存：生物群系 -> 可生成的怪物列表
    private final Map<Biome, List<Biome.SpawnListEntry>> biomeMonsters = new HashMap<>();
    
    // 怪物 -> 战利品表位置
    private final Map<Class<? extends EntityLiving>, ResourceLocation> lootTableCache = new HashMap<>();
    
    public static MobDropSimulator getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MobDropSimulator();
        }
        return INSTANCE;
    }
    
    private MobDropSimulator() {
        initLootTableCache();
    }
    
    /**
     * 初始化战利品表缓存
     */
    private void initLootTableCache() {
        // 常见怪物的战利品表
        lootTableCache.put(EntityZombie.class, new ResourceLocation("entities/zombie"));
        lootTableCache.put(EntitySkeleton.class, new ResourceLocation("entities/skeleton"));
        lootTableCache.put(EntityCreeper.class, new ResourceLocation("entities/creeper"));
        lootTableCache.put(EntitySpider.class, new ResourceLocation("entities/spider"));
        lootTableCache.put(EntityEnderman.class, new ResourceLocation("entities/enderman"));
        lootTableCache.put(EntityWitch.class, new ResourceLocation("entities/witch"));
        lootTableCache.put(EntitySlime.class, new ResourceLocation("entities/slime"));
        lootTableCache.put(EntityBlaze.class, new ResourceLocation("entities/blaze"));
        lootTableCache.put(EntityGhast.class, new ResourceLocation("entities/ghast"));
        lootTableCache.put(EntityPigZombie.class, new ResourceLocation("entities/zombie_pigman"));
        lootTableCache.put(EntityWitherSkeleton.class, new ResourceLocation("entities/wither_skeleton"));
        lootTableCache.put(EntityGuardian.class, new ResourceLocation("entities/guardian"));
        lootTableCache.put(EntityShulker.class, new ResourceLocation("entities/shulker"));
    }
    
    /**
     * 获取生物群系的怪物生成列表
     */
    public List<Biome.SpawnListEntry> getMonstersForBiome(Biome biome) {
        return biomeMonsters.computeIfAbsent(biome, b -> {
            List<Biome.SpawnListEntry> monsters = new ArrayList<>();
            monsters.addAll(biome.getSpawnableList(EnumCreatureType.MONSTER));
            return monsters;
        });
    }
    
    /**
     * 模拟击杀一个随机怪物并返回掉落物
     * 
     * @param world 世界（用于战利品表上下文）
     * @param biome 生物群系
     * @param lootingLevel 抢夺等级
     * @param rand 随机数生成器
     * @return 掉落物列表
     */
    public List<ItemStack> simulateKill(WorldServer world, Biome biome, int lootingLevel, Random rand) {
        List<ItemStack> drops = new ArrayList<>();
        
        List<Biome.SpawnListEntry> monsters = getMonstersForBiome(biome);
        if (monsters.isEmpty()) {
            return drops;
        }
        
        // 随机选择一个怪物
        Biome.SpawnListEntry entry = WeightedRandom.getRandomItem(rand, monsters);
        if (entry == null) return drops;
        
        // 获取战利品表
        ResourceLocation lootTable = getLootTableForEntity(entry.entityClass);
        if (lootTable == null) return drops;
        
        // 生成掉落物
        try {
            LootTable table = world.getLootTableManager().getLootTableFromLocation(lootTable);
            LootContext.Builder contextBuilder = new LootContext.Builder(world);
            contextBuilder.withLuck(lootingLevel);
            
            // 模拟难度
            DifficultyInstance difficulty = new DifficultyInstance(
                EnumDifficulty.HARD, 
                world.getWorldTime(), 
                100000L,  // 模拟长期存在的区块
                world.getCurrentMoonPhaseFactor()
            );
            
            drops.addAll(table.generateLootForPools(rand, contextBuilder.build()));
            
            // 应用抢夺附魔效果
            if (lootingLevel > 0) {
                applyLootingBonus(drops, lootingLevel, rand);
            }
            
            // 稀有掉落
            addRareDrops(entry.entityClass, drops, lootingLevel, rand);
            
        } catch (Exception e) {
            // 如果战利品表加载失败，使用备用掉落
            addFallbackDrops(entry.entityClass, drops, rand);
        }
        
        return drops;
    }
    
    /**
     * 获取实体的战利品表
     */
    @Nullable
    private ResourceLocation getLootTableForEntity(Class<? extends EntityLiving> entityClass) {
        // 先检查缓存
        ResourceLocation cached = lootTableCache.get(entityClass);
        if (cached != null) return cached;
        
        // 尝试从注册表获取
        ResourceLocation entityId = EntityList.getKey(entityClass);
        if (entityId != null) {
            ResourceLocation lootTable = new ResourceLocation(entityId.getNamespace(),
                "entities/" + entityId.getPath());
            lootTableCache.put(entityClass, lootTable);
            return lootTable;
        }
        
        return null;
    }
    
    /**
     * 应用抢夺附魔加成
     */
    private void applyLootingBonus(List<ItemStack> drops, int lootingLevel, Random rand) {
        List<ItemStack> bonusDrops = new ArrayList<>();
        
        for (ItemStack stack : drops) {
            // 每级抢夺有25%几率额外掉落
            for (int i = 0; i < lootingLevel; i++) {
                if (rand.nextFloat() < 0.25f) {
                    bonusDrops.add(stack.copy());
                }
            }
        }
        
        drops.addAll(bonusDrops);
    }
    
    /**
     * 添加稀有掉落
     */
    private void addRareDrops(Class<? extends EntityLiving> entityClass, List<ItemStack> drops, int lootingLevel, Random rand) {
        float rareChance = 0.025f + (lootingLevel * 0.01f);  // 2.5% + 每级1%
        
        if (rand.nextFloat() < rareChance) {
            // 怪物头颅等稀有掉落
            if (entityClass == EntitySkeleton.class) {
                drops.add(new ItemStack(Items.SKULL, 1, 0));
            } else if (entityClass == EntityZombie.class) {
                drops.add(new ItemStack(Items.SKULL, 1, 2));
            } else if (entityClass == EntityCreeper.class) {
                drops.add(new ItemStack(Items.SKULL, 1, 4));
            } else if (entityClass == EntityWitherSkeleton.class) {
                drops.add(new ItemStack(Items.SKULL, 1, 1));
            } else if (entityClass == EntityEnderman.class) {
                drops.add(new ItemStack(Items.ENDER_PEARL));
            }
        }
    }
    
    /**
     * 备用掉落（当战利品表加载失败时）
     */
    private void addFallbackDrops(Class<? extends EntityLiving> entityClass, List<ItemStack> drops, Random rand) {
        // 基础经验球效果 - 转换为一些基础物品
        if (EntityZombie.class.isAssignableFrom(entityClass)) {
            if (rand.nextFloat() < 0.5f) drops.add(new ItemStack(Items.ROTTEN_FLESH, 1 + rand.nextInt(2)));
        } else if (EntitySkeleton.class.isAssignableFrom(entityClass)) {
            if (rand.nextFloat() < 0.5f) drops.add(new ItemStack(Items.BONE, 1 + rand.nextInt(2)));
            if (rand.nextFloat() < 0.5f) drops.add(new ItemStack(Items.ARROW, 1 + rand.nextInt(2)));
        } else if (entityClass == EntitySpider.class) {
            if (rand.nextFloat() < 0.5f) drops.add(new ItemStack(Items.STRING, 1 + rand.nextInt(2)));
            if (rand.nextFloat() < 0.33f) drops.add(new ItemStack(Items.SPIDER_EYE));
        } else if (entityClass == EntityCreeper.class) {
            drops.add(new ItemStack(Items.GUNPOWDER, 1 + rand.nextInt(2)));
        } else if (entityClass == EntitySlime.class) {
            drops.add(new ItemStack(Items.SLIME_BALL, 1 + rand.nextInt(2)));
        }
    }
    
    /**
     * 获取生物群系可生成的怪物类型数量
     */
    public int getMonsterVariety(Biome biome) {
        return getMonstersForBiome(biome).size();
    }
    
    /**
     * 检查生物群系是否有怪物可以生成
     */
    public boolean hasMonstersForBiome(Biome biome) {
        return !getMonstersForBiome(biome).isEmpty();
    }
}
