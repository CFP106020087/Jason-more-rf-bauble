package com.moremod.quarry.simulation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;

/**
 * 生物群系矿物注册表
 * 通过监听矿物生成事件和扫描世界生成器来收集矿物分布数据
 */
public class BiomeOreRegistry {
    
    private static BiomeOreRegistry INSTANCE;
    
    // 生物群系 -> 矿物条目列表
    private final Map<Biome, List<OreEntry>> biomeOres = new HashMap<>();
    
    // 通用矿物（所有生物群系都有）
    private final List<OreEntry> universalOres = new ArrayList<>();
    
    // 已初始化标记
    private boolean initialized = false;
    
    public static BiomeOreRegistry getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BiomeOreRegistry();
        }
        return INSTANCE;
    }
    
    private BiomeOreRegistry() {
        MinecraftForge.ORE_GEN_BUS.register(this);
    }
    
    /**
     * 初始化默认矿物分布
     * 在 postInit 阶段调用
     */
    public void initialize() {
        if (initialized) return;
        initialized = true;
        
        // 注册原版矿物（通用）
        registerUniversalOre(Blocks.COAL_ORE.getDefaultState(), 17, 0, 128, 20);
        registerUniversalOre(Blocks.IRON_ORE.getDefaultState(), 9, 0, 64, 20);
        registerUniversalOre(Blocks.GOLD_ORE.getDefaultState(), 9, 0, 32, 2);
        registerUniversalOre(Blocks.REDSTONE_ORE.getDefaultState(), 8, 0, 16, 8);
        registerUniversalOre(Blocks.DIAMOND_ORE.getDefaultState(), 8, 0, 16, 1);
        registerUniversalOre(Blocks.LAPIS_ORE.getDefaultState(), 7, 0, 32, 1);
        
        // 石头变种
        registerUniversalOre(Blocks.STONE.getStateFromMeta(1), 33, 0, 80, 10);  // Granite
        registerUniversalOre(Blocks.STONE.getStateFromMeta(3), 33, 0, 80, 10);  // Diorite
        registerUniversalOre(Blocks.STONE.getStateFromMeta(5), 33, 0, 80, 10);  // Andesite
        registerUniversalOre(Blocks.DIRT.getDefaultState(), 33, 0, 256, 10);
        registerUniversalOre(Blocks.GRAVEL.getDefaultState(), 33, 0, 256, 8);
        
        // 扫描模组矿物
        scanModOres();
        
        // 注册生物群系特定矿物
        registerBiomeSpecificOres();
    }
    
    /**
     * 扫描模组矿物（通过矿物词典）
     */
    private void scanModOres() {
        for (String oreName : OreDictionary.getOreNames()) {
            if (oreName.startsWith("ore")) {
                List<ItemStack> ores = OreDictionary.getOres(oreName);
                for (ItemStack oreStack : ores) {
                    if (oreStack.isEmpty()) continue;
                    
                    Block block = Block.getBlockFromItem(oreStack.getItem());
                    if (block != Blocks.AIR) {
                        IBlockState state = block.getStateFromMeta(oreStack.getMetadata());
                        
                        // 根据矿物类型设置生成参数
                        OreEntry entry = guessOreParameters(oreName, state);
                        if (entry != null && !containsOre(universalOres, state)) {
                            universalOres.add(entry);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 根据矿物名称猜测生成参数
     */
    private OreEntry guessOreParameters(String oreName, IBlockState state) {
        String lowerName = oreName.toLowerCase();
        
        // 常见模组矿物的合理默认值
        if (lowerName.contains("copper") || lowerName.contains("tin")) {
            return new OreEntry(state, 9, 0, 64, 15);
        } else if (lowerName.contains("silver") || lowerName.contains("lead")) {
            return new OreEntry(state, 9, 0, 32, 8);
        } else if (lowerName.contains("uranium") || lowerName.contains("platinum")) {
            return new OreEntry(state, 4, 0, 16, 1);
        } else if (lowerName.contains("nickel") || lowerName.contains("aluminum") || lowerName.contains("aluminium")) {
            return new OreEntry(state, 6, 0, 48, 6);
        } else if (lowerName.contains("osmium") || lowerName.contains("zinc")) {
            return new OreEntry(state, 8, 0, 48, 8);
        }
        
        // 默认参数
        return new OreEntry(state, 6, 0, 48, 5);
    }
    
    /**
     * 注册生物群系特定矿物
     */
    private void registerBiomeSpecificOres() {
        // 地狱生物群系
        for (Biome biome : ForgeRegistries.BIOMES) {
            if (biome.getRegistryName() != null && 
                biome.getRegistryName().getPath().toLowerCase().contains("hell")) {
                registerBiomeOre(biome, Blocks.QUARTZ_ORE.getDefaultState(), 14, 10, 117, 16);
                registerBiomeOre(biome, Blocks.MAGMA.getDefaultState(), 33, 27, 36, 4);
            }
        }
        
        // 末地生物群系 - 没有矿物，但可以添加自定义
    }
    
    /**
     * 注册通用矿物
     */
    public void registerUniversalOre(IBlockState state, int veinSize, int minY, int maxY, int weight) {
        universalOres.add(new OreEntry(state, veinSize, minY, maxY, weight));
    }
    
    /**
     * 为特定生物群系注册矿物
     */
    public void registerBiomeOre(Biome biome, IBlockState state, int veinSize, int minY, int maxY, int weight) {
        biomeOres.computeIfAbsent(biome, b -> new ArrayList<>())
                 .add(new OreEntry(state, veinSize, minY, maxY, weight));
    }
    
    /**
     * 获取指定生物群系的所有矿物条目
     */
    public List<OreEntry> getOresForBiome(Biome biome) {
        List<OreEntry> result = new ArrayList<>(universalOres);
        List<OreEntry> biomeSpecific = biomeOres.get(biome);
        if (biomeSpecific != null) {
            result.addAll(biomeSpecific);
        }
        return result;
    }
    
    /**
     * 获取总权重
     */
    public int getTotalWeight(Biome biome) {
        int total = 0;
        for (OreEntry entry : getOresForBiome(biome)) {
            total += entry.weight;
        }
        return total;
    }
    
    /**
     * 根据权重随机选择矿物
     */
    public OreEntry getRandomOre(Biome biome, Random rand) {
        List<OreEntry> ores = getOresForBiome(biome);
        if (ores.isEmpty()) return null;
        
        int totalWeight = getTotalWeight(biome);
        int target = rand.nextInt(totalWeight);
        
        int cumulative = 0;
        for (OreEntry entry : ores) {
            cumulative += entry.weight;
            if (target < cumulative) {
                return entry;
            }
        }
        
        return ores.get(ores.size() - 1);
    }
    
    private boolean containsOre(List<OreEntry> list, IBlockState state) {
        for (OreEntry entry : list) {
            if (entry.state.equals(state)) return true;
        }
        return false;
    }
    
    /**
     * 矿物条目
     */
    public static class OreEntry {
        public final IBlockState state;
        public final int veinSize;      // 矿脉大小
        public final int minY;          // 最小生成高度
        public final int maxY;          // 最大生成高度
        public final int weight;        // 生成权重
        
        public OreEntry(IBlockState state, int veinSize, int minY, int maxY, int weight) {
            this.state = state;
            this.veinSize = veinSize;
            this.minY = minY;
            this.maxY = maxY;
            this.weight = weight;
        }
        
        /**
         * 根据高度计算实际生成概率修正
         */
        public float getHeightMultiplier(int y) {
            if (y < minY || y > maxY) return 0f;
            
            // 越靠近最佳高度，概率越高
            int midY = (minY + maxY) / 2;
            int range = (maxY - minY) / 2;
            float distance = Math.abs(y - midY) / (float) range;
            return 1f - (distance * 0.5f);  // 边缘处概率降低50%
        }
    }
}
