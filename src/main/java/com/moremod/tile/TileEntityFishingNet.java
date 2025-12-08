package com.moremod.tile;

import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 渔网TileEntity - 自动钓鱼逻辑（增强版）
 * - 速度加倍
 * - 支持 Advanced Fishing 和 Aquaculture 模组的鱼
 */
public class TileEntityFishingNet extends TileEntity implements ITickable {

    // 内部物品存储 (9格)
    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    // 钓鱼计时器
    private int fishingTimer = 0;

    // 基础钓鱼时间 (tick) - 速度加倍：约15秒一次
    private static final int BASE_FISHING_TIME = 300;  // 原来是600
    // 随机波动范围
    private static final int FISHING_TIME_VARIANCE = 200;  // 原来是400

    // 下次钓鱼的目标时间
    private int targetFishingTime = 0;

    // 模组鱼列表缓存
    private static List<ItemStack> modFishCache = null;
    private static boolean modFishCacheInitialized = false;

    public TileEntityFishingNet() {
        resetFishingTimer();
    }

    private void resetFishingTimer() {
        fishingTimer = 0;
        if (world != null) {
            targetFishingTime = BASE_FISHING_TIME + world.rand.nextInt(FISHING_TIME_VARIANCE);
        } else {
            targetFishingTime = BASE_FISHING_TIME;
        }
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        // 检查下方是否有水
        if (!hasWaterBelow()) {
            return;
        }

        fishingTimer++;

        if (fishingTimer >= targetFishingTime) {
            doFishing();
            resetFishingTimer();
        }
    }

    /**
     * 检查下方是否有水源
     */
    public boolean hasWaterBelow() {
        if (world == null) return false;
        BlockPos below = pos.down();
        return world.getBlockState(below).getMaterial() == Material.WATER;
    }

    /**
     * 执行钓鱼，从战利品表和模组鱼获取物品
     */
    private void doFishing() {
        if (!(world instanceof WorldServer)) return;

        WorldServer ws = (WorldServer) world;
        Random rand = ws.rand;

        List<ItemStack> loot = new ArrayList<>();

        // 70% 概率使用原版钓鱼表
        if (rand.nextFloat() < 0.7f) {
            LootContext.Builder builder = new LootContext.Builder(ws);
            builder.withLuck(1); // 稍微增加幸运
            loot.addAll(ws.getLootTableManager()
                .getLootTableFromLocation(LootTableList.GAMEPLAY_FISHING)
                .generateLootForPools(rand, builder.build()));
        }
        // 30% 概率获取模组鱼
        else {
            ItemStack modFish = getRandomModFish(rand);
            if (modFish != null && !modFish.isEmpty()) {
                loot.add(modFish);
            } else {
                // 如果没有模组鱼，使用原版
                LootContext.Builder builder = new LootContext.Builder(ws);
                builder.withLuck(1);
                loot.addAll(ws.getLootTableManager()
                    .getLootTableFromLocation(LootTableList.GAMEPLAY_FISHING)
                    .generateLootForPools(rand, builder.build()));
            }
        }

        // 尝试将战利品放入库存
        for (ItemStack stack : loot) {
            ItemStack remaining = insertItem(stack);
            // 如果库存满了，掉落到世界
            if (!remaining.isEmpty()) {
                EntityItem entityItem = new EntityItem(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    remaining);
                world.spawnEntity(entityItem);
            }
        }

        // 生成粒子效果
        spawnFishingParticles();
    }

    /**
     * 获取随机的模组鱼
     */
    @Nullable
    private ItemStack getRandomModFish(Random rand) {
        initModFishCache();

        if (modFishCache == null || modFishCache.isEmpty()) {
            return null;
        }

        int index = rand.nextInt(modFishCache.size());
        return modFishCache.get(index).copy();
    }

    /**
     * 初始化模组鱼缓存
     */
    private static void initModFishCache() {
        if (modFishCacheInitialized) return;
        modFishCacheInitialized = true;
        modFishCache = new ArrayList<>();

        // Advanced Fishing 鱼类
        if (Loader.isModLoaded("advanced_fishing")) {
            addModFish("advanced_fishing", "goldfish");
            addModFish("advanced_fishing", "bass");
            addModFish("advanced_fishing", "piranha");
            addModFish("advanced_fishing", "carp");
            addModFish("advanced_fishing", "catfish");
            addModFish("advanced_fishing", "blue_jellyfish");
            addModFish("advanced_fishing", "green_jellyfish");
            addModFish("advanced_fishing", "magma_jellyfish");
            addModFish("advanced_fishing", "glowing_jellyfish");
            addModFish("advanced_fishing", "shark");
            addModFish("advanced_fishing", "angler_fish");
            addModFish("advanced_fishing", "ghost_fish");
            addModFish("advanced_fishing", "bone_fish");
            addModFish("advanced_fishing", "mud_fish");
            addModFish("advanced_fishing", "nether_fish");
            addModFish("advanced_fishing", "spectre_fish");
            addModFish("advanced_fishing", "cave_fish");
            addModFish("advanced_fishing", "spike_fish");
            addModFish("advanced_fishing", "flying_fish");
        }

        // Aquaculture 鱼类
        if (Loader.isModLoaded("aquaculture")) {
            // 淡水鱼
            addModFish("aquaculture", "fish", 0);  // Bluegill
            addModFish("aquaculture", "fish", 1);  // Perch
            addModFish("aquaculture", "fish", 2);  // Gar
            addModFish("aquaculture", "fish", 3);  // Bass
            addModFish("aquaculture", "fish", 4);  // Muskellunge
            addModFish("aquaculture", "fish", 5);  // Brown Trout
            addModFish("aquaculture", "fish", 6);  // Catfish
            addModFish("aquaculture", "fish", 7);  // Carp
            addModFish("aquaculture", "fish", 8);  // Brown Shrooma
            addModFish("aquaculture", "fish", 9);  // Red Shrooma
            addModFish("aquaculture", "fish", 10); // Boulti (Nile Tilapia)
            addModFish("aquaculture", "fish", 11); // Capitaine (Nile Perch)
            addModFish("aquaculture", "fish", 12); // Bagrid
            addModFish("aquaculture", "fish", 13); // Syndontis
            addModFish("aquaculture", "fish", 14); // Smallmouth Bass
            addModFish("aquaculture", "fish", 15); // Arapaima
            addModFish("aquaculture", "fish", 16); // Piranha
            addModFish("aquaculture", "fish", 17); // Tambaqui
            addModFish("aquaculture", "fish", 18); // Pink Salmon
            addModFish("aquaculture", "fish", 19); // Rainbow Trout
            addModFish("aquaculture", "fish", 20); // Blackfish
            // 海水鱼
            addModFish("aquaculture", "fish", 21); // Pacific Halibut
            addModFish("aquaculture", "fish", 22); // Atlantic Halibut
            addModFish("aquaculture", "fish", 23); // Atlantic Herring
            addModFish("aquaculture", "fish", 24); // Pink Salmon
            addModFish("aquaculture", "fish", 25); // Pollock
            addModFish("aquaculture", "fish", 26); // Tuna
            addModFish("aquaculture", "fish", 27); // Swordfish
            addModFish("aquaculture", "fish", 28); // Shark
            addModFish("aquaculture", "fish", 29); // Whale
            addModFish("aquaculture", "fish", 30); // Squid
            addModFish("aquaculture", "fish", 31); // Jellyfish
            // 热带鱼
            addModFish("aquaculture", "fish", 32); // Red Grouper
            addModFish("aquaculture", "fish", 33); // Tuna
            addModFish("aquaculture", "fish", 34); // Swordfish
        }

        System.out.println("[MoreMod] 渔网模组鱼缓存初始化完成，共 " + modFishCache.size() + " 种鱼");
    }

    /**
     * 添加模组鱼到缓存
     */
    private static void addModFish(String modid, String itemName) {
        addModFish(modid, itemName, 0);
    }

    private static void addModFish(String modid, String itemName, int meta) {
        try {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(modid, itemName));
            if (item != null && item != Items.AIR) {
                modFishCache.add(new ItemStack(item, 1, meta));
            }
        } catch (Exception e) {
            // 忽略不存在的物品
        }
    }

    /**
     * 尝试将物品插入库存
     */
    private ItemStack insertItem(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < inventory.getSlots() && !remaining.isEmpty(); i++) {
            remaining = inventory.insertItem(i, remaining, false);
        }
        return remaining;
    }

    /**
     * 生成钓鱼粒子效果
     */
    private void spawnFishingParticles() {
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(net.minecraft.util.EnumParticleTypes.WATER_SPLASH,
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                10, 0.3, 0.1, 0.3, 0.0);
        }
    }

    /**
     * 掉落所有库存物品
     */
    public void dropInventory() {
        if (world == null || world.isRemote) return;

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                EntityItem entityItem = new EntityItem(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    stack.copy());
                world.spawnEntity(entityItem);
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    // ========== NBT 读写 ==========

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("FishingTimer", fishingTimer);
        compound.setInteger("TargetTime", targetFishingTime);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        fishingTimer = compound.getInteger("FishingTimer");
        targetFishingTime = compound.getInteger("TargetTime");
        if (targetFishingTime <= 0) {
            targetFishingTime = BASE_FISHING_TIME;
        }
    }

    // ========== Capability (漏斗/管道支持) ==========

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }

    // ========== Getter ==========

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getFishingProgress() {
        return fishingTimer;
    }

    public int getTargetTime() {
        return targetFishingTime;
    }
}
