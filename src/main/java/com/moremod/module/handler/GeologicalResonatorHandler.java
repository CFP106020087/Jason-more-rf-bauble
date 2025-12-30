package com.moremod.module.handler;

import com.moremod.module.effect.EventContext;
import com.moremod.module.effect.IModuleEventHandler;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 地质共振仪 (GEOLOGICAL_RESONATOR) 处理器 - 随机矿物生成
 * 从矿物词典中随机挑选矿物直接生成给玩家
 */
public class GeologicalResonatorHandler implements IModuleEventHandler {

    // 能耗配置
    private static final int PASSIVE_COST_PER_TICK = 50;  // 维持能耗 50 RF/t
    private static final int GENERATION_COST = 1000;       // 每次生成消耗 1000 RF

    // 生成频率控制
    private static final int GENERATION_INTERVAL = 40;     // 每2秒尝试生成一次
    private int tickCounter = 0;

    private final Random random = new Random();

    // 矿物缓存
    private static final List<ItemStack> ORE_CACHE = new ArrayList<>();
    private static boolean cacheInitialized = false;

    /**
     * 初始化矿物缓存 - 从矿物词典收集所有矿物
     */
    private static void initializeOreCache() {
        if (cacheInitialized) return;
        cacheInitialized = true;

        String[] oreNames = OreDictionary.getOreNames();
        for (String name : oreNames) {
            // 只收集以 "ore" 开头的词典条目
            if (!name.startsWith("ore")) continue;

            // 排除问题矿物
            String lowerName = name.toLowerCase();
            if (lowerName.contains("netherite") ||
                lowerName.contains("ancientdebris") ||
                lowerName.contains("ancient_debris")) {
                continue;
            }

            // 获取该词典名下的所有物品
            List<ItemStack> ores = OreDictionary.getOres(name, false);
            for (ItemStack ore : ores) {
                if (!ore.isEmpty()) {
                    // 复制一份避免修改原始数据
                    ORE_CACHE.add(ore.copy());
                }
            }
        }

        // 去重（基于物品ID和元数据）
        List<ItemStack> uniqueOres = new ArrayList<>();
        for (ItemStack ore : ORE_CACHE) {
            boolean isDuplicate = false;
            for (ItemStack existing : uniqueOres) {
                if (ItemStack.areItemsEqual(ore, existing)) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                uniqueOres.add(ore);
            }
        }
        ORE_CACHE.clear();
        ORE_CACHE.addAll(uniqueOres);
    }

    @Override
    public void onTick(EventContext ctx) {
        initializeOreCache();

        // 1. 处理维持能耗
        if (!ctx.consumeEnergy(PASSIVE_COST_PER_TICK)) {
            return;
        }

        // 2. 仅服务端执行
        if (ctx.player.world.isRemote) return;
        if (!(ctx.player.world instanceof WorldServer)) return;

        // 3. 频率控制
        tickCounter++;
        if (tickCounter < GENERATION_INTERVAL) {
            return;
        }
        tickCounter = 0;

        // 4. 检查矿物缓存
        if (ORE_CACHE.isEmpty()) return;

        // 5. 根据等级决定生成次数 (1, 2, 3)
        int attempts = ctx.level;

        for (int i = 0; i < attempts; i++) {
            // 检查能量
            if (ctx.getEnergy() < GENERATION_COST) return;

            // 随机生成矿物
            if (generateRandomOre(ctx)) {
                ctx.consumeEnergy(GENERATION_COST);
            }
        }
    }

    /**
     * 随机生成一个矿物给玩家
     */
    private boolean generateRandomOre(EventContext ctx) {
        EntityPlayer player = ctx.player;
        WorldServer world = (WorldServer) player.world;

        // 随机选择一个矿物
        ItemStack selectedOre = ORE_CACHE.get(random.nextInt(ORE_CACHE.size()));
        ItemStack toGive = selectedOre.copy();
        toGive.setCount(1);

        // 尝试放入玩家背包
        if (!player.inventory.addItemStackToInventory(toGive)) {
            // 背包满了，掉落在玩家脚下
            EntityItem entityItem = new EntityItem(world, player.posX, player.posY + 0.5, player.posZ, toGive);
            entityItem.setNoPickupDelay();
            world.spawnEntity(entityItem);
        }

        // 播放效果
        playGenerationEffect(world, player);

        return true;
    }

    /**
     * 播放生成效果
     */
    private void playGenerationEffect(WorldServer world, EntityPlayer player) {
        world.spawnParticle(
            EnumParticleTypes.VILLAGER_HAPPY,
            player.posX, player.posY + 1.0, player.posZ,
            5, 0.3, 0.3, 0.3, 0.02
        );
    }

    @Override
    public int getPassiveEnergyCost() {
        return 0; // 在 onTick 中手动处理
    }
}
