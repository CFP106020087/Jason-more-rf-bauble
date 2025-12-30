package com.moremod.event;

import com.moremod.item.ItemAstralPickaxe;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * 星芒镐的额外掉落处理
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class AstralPickaxeDropHandler {
    
    private static final Random rand = new Random();
    
    // ===== 缓存 AS 物品/方块 =====
    private static boolean cacheInitialized = false;
    private static boolean asLoaded = false;
    
    private static Item cachedCraftingComponent = null;  // ItemsAS.craftingComponent
    private static Block cachedMarbleBlock = null;       // BlocksAS.blockMarble
    private static Block cachedRockCrystalBlock = null;  // BlocksAS.rockCollectorCrystal
    private static Block cachedCelestialCrystalBlock = null; // BlocksAS.celestialCollectorCrystal
    
    // AS 的 MetaType 常量
    private static final int META_STARMETAL_INGOT = 1;  // STARMETAL_INGOT
    private static final int META_MARBLE_RAW = 0;       // RAW marble
    
    // ===== 掉落概率配置 =====
    private static final float MARBLE_DROP_CHANCE = 0.05f;      // 5% 挖石头掉大理石
    private static final float STARMETAL_DROP_CHANCE = 0.03f;   // 3% 挖铁矿掉星辉锭
    
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        EntityPlayer player = event.getHarvester();
        if (player == null) return;
        
        // 检查是否手持星芒镐
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || !(held.getItem() instanceof ItemAstralPickaxe)) {
            return;
        }
        
        // 初始化 AS 缓存
        initCache();
        
        if (!asLoaded) return; // AS 未安装
        
        IBlockState state = event.getState();
        Block block = state.getBlock();
        
        // === 挖石头 → 概率掉大理石 ===
        if (block == Blocks.STONE && cachedMarbleBlock != null) {
            if (rand.nextFloat() < MARBLE_DROP_CHANCE) {
                event.getDrops().add(new ItemStack(cachedMarbleBlock, 1, META_MARBLE_RAW));
            }
        }
        
        // === 挖铁矿 → 概率掉星辉锭 ===
        if (block == Blocks.IRON_ORE && cachedCraftingComponent != null) {
            if (rand.nextFloat() < STARMETAL_DROP_CHANCE) {
                event.getDrops().add(new ItemStack(cachedCraftingComponent, 1, META_STARMETAL_INGOT));
            }
        }
        
        // === 处理 AS 水晶掉落 ===
        handleCrystalDrops(event, block, state);
    }
    
    /**
     * 处理 AS 水晶的掉落物
     */
    private static void handleCrystalDrops(BlockEvent.HarvestDropsEvent event, Block block, IBlockState state) {
        String className = block.getClass().getName();
        
        boolean isCollectorCrystal = className.contains("BlockCollectorCrystal");
        boolean isCelestialCrystal = className.contains("BlockCelestialCrystals");
        
        if (!isCollectorCrystal && !isCelestialCrystal) return;
        
        // 如果原本没有掉落物（自然生成的水晶），尝试生成掉落
        if (event.getDrops().isEmpty() || isCollectorCrystal) {
            try {
                TileEntity te = event.getWorld().getTileEntity(event.getPos());
                if (te != null) {
                    ItemStack crystalDrop = createCrystalDropFromTile(te, block, state);
                    if (!crystalDrop.isEmpty()) {
                        // 清除原有掉落物，添加新的
                        event.getDrops().clear();
                        event.getDrops().add(crystalDrop);
                    }
                }
            } catch (Exception e) {
                // 静默处理，回退到默认行为
            }
        }
    }
    
    /**
     * 从 TileEntity 创建水晶掉落物
     */
    private static ItemStack createCrystalDropFromTile(TileEntity te, Block block, IBlockState state) {
        try {
            Class<?> tileClass = te.getClass();
            
            // 尝试方法1: 查找 getDropStack 或类似方法
            for (Method m : tileClass.getMethods()) {
                String name = m.getName().toLowerCase();
                if ((name.contains("drop") || name.contains("item") || name.contains("stack")) 
                    && m.getReturnType() == ItemStack.class 
                    && m.getParameterCount() == 0) {
                    try {
                        ItemStack result = (ItemStack) m.invoke(te);
                        if (result != null && !result.isEmpty()) {
                            return result;
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            // 尝试方法2: 对于 TileCollectorCrystal，读取属性并构造物品
            if (tileClass.getName().contains("TileCollectorCrystal")) {
                return createCollectorCrystalItem(te, block);
            }
            
            // 尝试方法3: 直接使用方块的 ItemBlock
            Item itemBlock = Item.getItemFromBlock(block);
            if (itemBlock != null) {
                int meta = block.getMetaFromState(state);
                return new ItemStack(itemBlock, 1, meta);
            }
            
        } catch (Exception e) {
            // 静默处理
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * 为 CollectorCrystal 创建带属性的物品
     */
    private static ItemStack createCollectorCrystalItem(TileEntity te, Block block) {
        try {
            // 获取基础物品
            Item itemBlock = Item.getItemFromBlock(block);
            if (itemBlock == null) return ItemStack.EMPTY;
            
            ItemStack stack = new ItemStack(itemBlock);
            
            // 尝试复制 NBT 数据（水晶属性、星座等）
            Class<?> tileClass = te.getClass();
            
            // 查找 CrystalAttributes
            for (Method m : tileClass.getMethods()) {
                if (m.getName().contains("Attributes") && m.getParameterCount() == 0) {
                    Object attributes = m.invoke(te);
                    if (attributes != null) {
                        // 尝试调用 store 方法将属性写入物品
                        for (Method storeMethod : attributes.getClass().getMethods()) {
                            if (storeMethod.getName().equals("store") 
                                && storeMethod.getParameterCount() == 1
                                && storeMethod.getParameterTypes()[0] == ItemStack.class) {
                                storeMethod.invoke(attributes, stack);
                                break;
                            }
                        }
                    }
                    break;
                }
            }
            
            // 查找星座信息
            for (Method m : tileClass.getMethods()) {
                String name = m.getName();
                if ((name.contains("Constellation") || name.contains("Attune")) && m.getParameterCount() == 0) {
                    Object constellation = m.invoke(te);
                    if (constellation != null && stack.getItem() instanceof net.minecraft.item.ItemBlock) {
                        // 尝试将星座写入物品 NBT
                        try {
                            Method setConstellation = findMethod(stack.getItem().getClass(), "setAttunedConstellation", ItemStack.class, constellation.getClass());
                            if (setConstellation != null) {
                                setConstellation.invoke(stack.getItem(), stack, constellation);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            
            return stack;
            
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
    
    /**
     * 查找方法（包括父类）
     */
    private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredMethod(name, paramTypes);
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * 初始化 AS 物品/方块缓存
     */
    private static void initCache() {
        if (cacheInitialized) return;
        cacheInitialized = true;
        
        try {
            // 反射获取 ItemsAS.craftingComponent
            Class<?> itemsAS = Class.forName("hellfirepvp.astralsorcery.common.lib.ItemsAS");
            Field craftingComponentField = itemsAS.getField("craftingComponent");
            cachedCraftingComponent = (Item) craftingComponentField.get(null);
            
            // 反射获取 BlocksAS.blockMarble
            Class<?> blocksAS = Class.forName("hellfirepvp.astralsorcery.common.lib.BlocksAS");
            Field marbleField = blocksAS.getField("blockMarble");
            cachedMarbleBlock = (Block) marbleField.get(null);
            
            // 尝试获取水晶方块（用于验证）
            try {
                Field rockCrystalField = blocksAS.getField("rockCollectorCrystal");
                cachedRockCrystalBlock = (Block) rockCrystalField.get(null);
            } catch (Exception ignored) {}
            
            try {
                Field celestialField = blocksAS.getField("celestialCollectorCrystal");
                cachedCelestialCrystalBlock = (Block) celestialField.get(null);
            } catch (Exception ignored) {}
            
            asLoaded = true;
            System.out.println("[MoreMod] ⭐ Astral Sorcery 兼容已启用");
            
        } catch (ClassNotFoundException e) {
            // AS 未安装，静默忽略
            asLoaded = false;
            System.out.println("[MoreMod] ⭐ Astral Sorcery 未检测到，跳过兼容功能");
        } catch (Exception e) {
            asLoaded = false;
            System.out.println("[MoreMod] ⭐ Astral Sorcery 兼容初始化失败: " + e.getMessage());
        }
    }
}