package com.moremod.config;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构胶囊配置
 */
@Config(modid = "moremod", name = "moremod/structure_capsule")
public class CapsuleConfig {

    @Config.Comment("不可捕获的方块列表（除了硬度<0的方块自动排除外）")
    @Config.Name("排除的方块")
    public static String[] excludedBlocks = {
        "minecraft:bedrock",
        "minecraft:end_portal",
        "minecraft:end_portal_frame",
        "minecraft:end_gateway",
        "minecraft:portal",
        "minecraft:command_block",
        "minecraft:chain_command_block",
        "minecraft:repeating_command_block",
        "minecraft:barrier",
        "minecraft:structure_block",
        "minecraft:structure_void",
        "minecraft:mob_spawner"  // 刷怪笼通常也不应该被捕获
    };

    @Config.Comment("可以被结构覆盖的方块列表")
    @Config.Name("可覆盖的方块")
    public static String[] overridableBlocks = {
        "minecraft:air",
        "minecraft:water",
        "minecraft:flowing_water",
        "minecraft:lava",
        "minecraft:flowing_lava",
        "minecraft:tallgrass",
        "minecraft:deadbush",
        "minecraft:snow_layer",
        "minecraft:vine",
        "minecraft:double_plant"
    };

    @Config.Comment("捕获时是否播放音效")
    @Config.Name("启用音效")
    public static boolean enableSound = true;

    @Config.Comment("捕获/释放时是否显示粒子效果")
    @Config.Name("启用粒子效果")
    public static boolean enableParticles = true;

    @Config.Comment("是否允许捕获包含TileEntity的方块（如箱子）")
    @Config.Name("允许捕获容器")
    public static boolean allowCaptureTileEntities = true;

    @Config.Comment("最大捕获方块数量（超过此数量会失败，防止NBT过大崩溃）")
    @Config.Name("最大方块数量")
    @Config.RangeInt(min = 100, max = 50000)
    public static int maxBlockCount = 10000;

    @Config.Comment("最大NBT数据大小（字节，超过此大小会失败）")
    @Config.Name("最大NBT大小")
    @Config.RangeInt(min = 100000, max = 10000000)
    public static int maxNBTSize = 2000000;  // 2MB

    @Config.Comment("胶囊是否为一次性（收取和释放后消耗）")
    @Config.Name("一次性胶囊")
    public static boolean singleUse = true;

    // ============== 运行时解析的列表 ==============

    private static List<Block> parsedExcludedBlocks = null;
    private static List<Block> parsedOverridableBlocks = null;

    /**
     * 获取排除的方块列表
     */
    public static List<Block> getExcludedBlocks() {
        if (parsedExcludedBlocks == null) {
            parsedExcludedBlocks = parseBlockList(excludedBlocks);
        }
        return parsedExcludedBlocks;
    }

    /**
     * 获取可覆盖的方块列表
     */
    public static List<Block> getOverridableBlocks() {
        if (parsedOverridableBlocks == null) {
            parsedOverridableBlocks = parseBlockList(overridableBlocks);
        }
        return parsedOverridableBlocks;
    }

    /**
     * 解析方块名称列表为方块对象列表
     */
    private static List<Block> parseBlockList(String[] blockNames) {
        List<Block> blocks = new ArrayList<>();
        for (String name : blockNames) {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name));
            if (block != null) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    /**
     * 重新加载配置后清除缓存
     */
    public static void invalidateCache() {
        parsedExcludedBlocks = null;
        parsedOverridableBlocks = null;
    }

    @Mod.EventBusSubscriber(modid = "moremod")
    public static class ConfigSyncHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals("moremod")) {
                ConfigManager.sync("moremod", Config.Type.INSTANCE);
                invalidateCache();
            }
        }
    }
}