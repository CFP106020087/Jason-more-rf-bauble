package com.moremod.tile;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.*;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 动物喂食器TileEntity - 自动喂养动物使其繁殖
 */
public class TileEntityAnimalFeeder extends TileEntity implements ITickable {

    // 食物存储 (9格)
    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    // 喂食冷却
    private int feedCooldown = 0;
    private static final int FEED_COOLDOWN_TIME = 200; // 10秒冷却

    // 搜索范围
    private static final int RANGE = 8;

    // 动物-食物映射
    private static final Map<Class<? extends EntityAnimal>, Set<Item>> ANIMAL_FOODS = new HashMap<>();

    static {
        // 牛、羊 - 小麦
        Set<Item> wheatAnimals = new HashSet<>();
        wheatAnimals.add(Items.WHEAT);
        ANIMAL_FOODS.put(EntityCow.class, wheatAnimals);
        ANIMAL_FOODS.put(EntitySheep.class, wheatAnimals);
        ANIMAL_FOODS.put(EntityMooshroom.class, wheatAnimals);

        // 猪 - 胡萝卜、土豆、甜菜
        Set<Item> pigFood = new HashSet<>();
        pigFood.add(Items.CARROT);
        pigFood.add(Items.POTATO);
        pigFood.add(Items.BEETROOT);
        ANIMAL_FOODS.put(EntityPig.class, pigFood);

        // 鸡 - 种子
        Set<Item> chickenFood = new HashSet<>();
        chickenFood.add(Items.WHEAT_SEEDS);
        chickenFood.add(Items.MELON_SEEDS);
        chickenFood.add(Items.PUMPKIN_SEEDS);
        chickenFood.add(Items.BEETROOT_SEEDS);
        ANIMAL_FOODS.put(EntityChicken.class, chickenFood);

        // 兔子 - 胡萝卜、蒲公英
        Set<Item> rabbitFood = new HashSet<>();
        rabbitFood.add(Items.CARROT);
        rabbitFood.add(Items.GOLDEN_CARROT);
        ANIMAL_FOODS.put(EntityRabbit.class, rabbitFood);

        // 狼 - 肉
        Set<Item> wolfFood = new HashSet<>();
        wolfFood.add(Items.BEEF);
        wolfFood.add(Items.COOKED_BEEF);
        wolfFood.add(Items.PORKCHOP);
        wolfFood.add(Items.COOKED_PORKCHOP);
        wolfFood.add(Items.CHICKEN);
        wolfFood.add(Items.COOKED_CHICKEN);
        wolfFood.add(Items.MUTTON);
        wolfFood.add(Items.COOKED_MUTTON);
        wolfFood.add(Items.RABBIT);
        wolfFood.add(Items.COOKED_RABBIT);
        ANIMAL_FOODS.put(EntityWolf.class, wolfFood);

        // 猫 - 鱼
        Set<Item> catFood = new HashSet<>();
        catFood.add(Items.FISH);
        catFood.add(Items.COOKED_FISH);
        ANIMAL_FOODS.put(EntityOcelot.class, catFood);
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        // 冷却中
        if (feedCooldown > 0) {
            feedCooldown--;
            return;
        }

        // 尝试喂食
        if (tryFeedAnimals()) {
            feedCooldown = FEED_COOLDOWN_TIME;
        }
    }

    /**
     * 尝试喂食周围的动物
     */
    private boolean tryFeedAnimals() {
        AxisAlignedBB searchBox = new AxisAlignedBB(pos).grow(RANGE);
        List<EntityAnimal> animals = world.getEntitiesWithinAABB(EntityAnimal.class, searchBox);

        // 过滤可繁殖的动物
        List<EntityAnimal> breedableAnimals = new ArrayList<>();
        for (EntityAnimal animal : animals) {
            if (animal.getGrowingAge() == 0 && !animal.isInLove()) {
                breedableAnimals.add(animal);
            }
        }

        if (breedableAnimals.size() < 2) return false;

        // 按类型分组
        Map<Class<? extends EntityAnimal>, List<EntityAnimal>> animalsByType = new HashMap<>();
        for (EntityAnimal animal : breedableAnimals) {
            Class<? extends EntityAnimal> type = animal.getClass();
            animalsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(animal);
        }

        // 尝试为每种动物找到配对
        for (Map.Entry<Class<? extends EntityAnimal>, List<EntityAnimal>> entry : animalsByType.entrySet()) {
            List<EntityAnimal> sameType = entry.getValue();
            if (sameType.size() < 2) continue;

            // 找到对应的食物
            Set<Item> validFoods = ANIMAL_FOODS.get(entry.getKey());
            if (validFoods == null) continue;

            // 检查库存是否有足够的食物
            int foodSlot = findFoodSlot(validFoods);
            if (foodSlot == -1) continue;

            ItemStack foodStack = inventory.getStackInSlot(foodSlot);
            if (foodStack.getCount() < 2) continue;

            // 喂食两只动物
            EntityAnimal animal1 = sameType.get(0);
            EntityAnimal animal2 = sameType.get(1);

            animal1.setInLove(null);
            animal2.setInLove(null);

            // 消耗食物
            foodStack.shrink(2);
            if (foodStack.isEmpty()) {
                inventory.setStackInSlot(foodSlot, ItemStack.EMPTY);
            }

            // 粒子效果
            spawnLoveParticles(animal1);
            spawnLoveParticles(animal2);

            markDirty();
            return true;
        }

        return false;
    }

    /**
     * 查找包含指定食物的槽位
     */
    private int findFoodSlot(Set<Item> validFoods) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && validFoods.contains(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 生成爱心粒子
     */
    private void spawnLoveParticles(EntityAnimal animal) {
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            ws.spawnParticle(EnumParticleTypes.HEART,
                animal.posX, animal.posY + animal.height + 0.5, animal.posZ,
                5, 0.3, 0.3, 0.3, 0.0);
        }
    }

    /**
     * 添加食物
     */
    public ItemStack addFood(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < inventory.getSlots() && !remaining.isEmpty(); i++) {
            remaining = inventory.insertItem(i, remaining, false);
        }
        markDirty();
        return remaining;
    }

    /**
     * 获取食物总数
     */
    public int getFoodCount() {
        int count = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            count += inventory.getStackInSlot(i).getCount();
        }
        return count;
    }

    /**
     * 获取冷却秒数
     */
    public int getCooldownSeconds() {
        return feedCooldown / 20;
    }

    /**
     * 掉落库存
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
            }
        }
    }

    // ========== NBT ==========

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Inventory", inventory.serializeNBT());
        compound.setInteger("Cooldown", feedCooldown);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        inventory.deserializeNBT(compound.getCompoundTag("Inventory"));
        feedCooldown = compound.getInteger("Cooldown");
    }

    // ========== Capability ==========

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }
}
