package com.moremod.tile;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.util.List;

public class TileEntitySimpleWisdomShrine extends TileEntity implements ITickable {

    private boolean isFormed = false;
    private int tickCounter = 0;
    private int range = 15;

    private static final int CHECK_INTERVAL = 20;
    private static final int UNLOCK_COOLDOWN = 100;
    private static final int GROWTH_BOOST = 6000;

    // 缓存反射字段以提高性能
    private static Field toolUsesField = null;
    private static Field maxTradeUsesField = null;
    private static boolean fieldsInitialized = false;

    @Override
    public void update() {
        if (world.isRemote) return;

        tickCounter++;

        if (tickCounter % 100 == 0) {
            checkStructure();
        }

        if (!isFormed) return;

        if (tickCounter % CHECK_INTERVAL == 0) {
            applyEffects();
            spawnParticles();
        }

        if (tickCounter % UNLOCK_COOLDOWN == 0) {
            unlockVillagerTrades();
        }
    }

    public void checkStructure() {
        World world = this.world;
        BlockPos center = this.pos;
        boolean valid = true;

        // 底层
        valid &= checkBlock(world, center.add(-1, 0, -1), Blocks.EMERALD_BLOCK);
        valid &= checkBlock(world, center.add(0, 0, -1), Blocks.BOOKSHELF);
        valid &= checkBlock(world, center.add(1, 0, -1), Blocks.EMERALD_BLOCK);
        valid &= checkBlock(world, center.add(-1, 0, 0), Blocks.BOOKSHELF);
        valid &= checkBlock(world, center.add(1, 0, 0), Blocks.BOOKSHELF);
        valid &= checkBlock(world, center.add(-1, 0, 1), Blocks.EMERALD_BLOCK);
        valid &= checkBlock(world, center.add(0, 0, 1), Blocks.BOOKSHELF);
        valid &= checkBlock(world, center.add(1, 0, 1), Blocks.EMERALD_BLOCK);

        // 中层
        valid &= checkBlock(world, center.add(-1, 1, -1), Blocks.BOOKSHELF);
        valid &= checkAir(world, center.add(0, 1, -1));
        valid &= checkBlock(world, center.add(1, 1, -1), Blocks.BOOKSHELF);
        valid &= checkAir(world, center.add(-1, 1, 0));
        valid &= checkAir(world, center.add(0, 1, 0));
        valid &= checkAir(world, center.add(1, 1, 0));
        valid &= checkBlock(world, center.add(-1, 1, 1), Blocks.BOOKSHELF);
        valid &= checkAir(world, center.add(0, 1, 1));
        valid &= checkBlock(world, center.add(1, 1, 1), Blocks.BOOKSHELF);

        // 顶层 - 1.12.2版本
        valid &= checkBlock(world, center.add(-1, 2, -1), Blocks.GOLD_BLOCK);
        valid &= checkBlock(world, center.add(0, 2, -1), Blocks.BOOKSHELF);
        valid &= checkBlock(world, center.add(1, 2, -1), Blocks.GOLD_BLOCK);
        valid &= checkBlock(world, center.add(-1, 2, 0), Blocks.BOOKSHELF);
        valid &= checkBlock(world, center.add(0, 2, 0), Blocks.ENCHANTING_TABLE);
        valid &= checkBlock(world, center.add(1, 2, 0), Blocks.BOOKSHELF);
        valid &= checkBlock(world, center.add(-1, 2, 1), Blocks.GOLD_BLOCK);
        valid &= checkBlock(world, center.add(0, 2, 1), Blocks.BOOKSHELF);
        valid &= checkBlock(world, center.add(1, 2, 1), Blocks.GOLD_BLOCK);

        if (valid && !isFormed) {
            onStructureFormed();
        } else if (!valid && isFormed) {
            onStructureBroken();
        }

        isFormed = valid;
    }

    private boolean checkBlock(World world, BlockPos pos, net.minecraft.block.Block expectedBlock) {
        return world.getBlockState(pos).getBlock() == expectedBlock;
    }

    private boolean checkAir(World world, BlockPos pos) {
        return world.isAirBlock(pos);
    }

    private void applyEffects() {
        List<EntityVillager> villagers = world.getEntitiesWithinAABB(
                EntityVillager.class,
                new AxisAlignedBB(pos).grow(range)
        );

        for (EntityVillager villager : villagers) {
            if (villager.isChild()) {
                accelerateGrowth(villager);
            }
        }
    }

    private void accelerateGrowth(EntityVillager villager) {
        try {
            Field ageField = null;
            for (Field field : EntityVillager.class.getSuperclass().getDeclaredFields()) {
                if (field.getType() == int.class) {
                    field.setAccessible(true);
                    int value = field.getInt(villager);
                    if (value < 0) {
                        ageField = field;
                        break;
                    }
                }
            }

            if (ageField != null) {
                int currentAge = ageField.getInt(villager);
                int newAge = Math.min(0, currentAge + GROWTH_BOOST);
                ageField.set(villager, newAge);

                if (currentAge < 0 && newAge == 0) {
                    spawnGrowthParticles(villager);
                    System.out.println("[智慧之泉] 小村民成长完成！");
                }
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    /**
     * 初始化反射字段（只执行一次）
     */
    private void initReflectionFields() {
        if (fieldsInitialized) return;

        try {
            // 查找 toolUses 字段（已使用次数）
            for (Field field : MerchantRecipe.class.getDeclaredFields()) {
                if (field.getType() == int.class) {
                    field.setAccessible(true);

                    // 尝试识别 toolUses 字段（通常是第一个 int 字段）
                    if (toolUsesField == null) {
                        toolUsesField = field;
                    } else if (maxTradeUsesField == null) {
                        maxTradeUsesField = field;
                        break;
                    }
                }
            }

            fieldsInitialized = true;
            System.out.println("[智慧之泉] 反射字段初始化成功");

        } catch (Exception e) {
            System.err.println("[智慧之泉] 反射字段初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void unlockVillagerTrades() {
        // 初始化反射字段
        if (!fieldsInitialized) {
            initReflectionFields();
        }

        List<EntityVillager> villagers = world.getEntitiesWithinAABB(
                EntityVillager.class,
                new AxisAlignedBB(pos).grow(range)
        );

        int unlockedCount = 0;

        for (EntityVillager villager : villagers) {
            if (villager.isChild()) continue;

            try {
                // 获取交易列表
                Field buyingListField = null;
                for (Field field : EntityVillager.class.getDeclaredFields()) {
                    if (field.getType() == MerchantRecipeList.class) {
                        buyingListField = field;
                        break;
                    }
                }

                if (buyingListField != null) {
                    buyingListField.setAccessible(true);
                    MerchantRecipeList trades = (MerchantRecipeList) buyingListField.get(villager);

                    if (trades != null && trades.size() > 0) {
                        boolean hasUnlocked = false;

                        for (int i = 0; i < trades.size(); i++) {
                            MerchantRecipe recipe = trades.get(i);

                            // 检查交易是否被锁定
                            if (recipe.isRecipeDisabled()) {
                                // 尝试解锁交易
                                if (unlockRecipe(recipe)) {
                                    hasUnlocked = true;
                                    System.out.println("[智慧之泉] 解锁交易: " +
                                            recipe.getItemToSell().getDisplayName());
                                }
                            }
                        }

                        if (hasUnlocked) {
                            unlockedCount++;
                            spawnUnlockParticles(villager);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[智慧之泉] 解锁村民交易时出错: " + e.getMessage());
            }
        }

        if (unlockedCount > 0) {
            System.out.println("[智慧之泉] 解锁了 " + unlockedCount + " 个村民的交易");
        }
    }

    /**
     * 解锁单个交易配方
     * @param recipe 要解锁的交易配方
     * @return 是否成功解锁
     */
    private boolean unlockRecipe(MerchantRecipe recipe) {
        try {
            if (toolUsesField != null) {
                // 将使用次数重置为 0
                toolUsesField.set(recipe, 0);
                return true;
            } else {
                System.err.println("[智慧之泉] toolUsesField 未初始化");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[智慧之泉] 解锁交易失败: " + e.getMessage());
            return false;
        }
    }

    private void spawnParticles() {
        if (world.rand.nextInt(4) != 0) return;

        for (int i = 0; i < 3; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5) * 2;
            double offsetY = world.rand.nextDouble() * 2;
            double offsetZ = (world.rand.nextDouble() - 0.5) * 2;

            world.spawnParticle(
                    EnumParticleTypes.ENCHANTMENT_TABLE,
                    pos.getX() + 0.5 + offsetX,
                    pos.getY() + 1 + offsetY,
                    pos.getZ() + 0.5 + offsetZ,
                    0, 0.05, 0
            );
        }
    }

    private void spawnGrowthParticles(EntityVillager villager) {
        for (int i = 0; i < 20; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5) * 1.5;
            double offsetY = world.rand.nextDouble() * 2;
            double offsetZ = (world.rand.nextDouble() - 0.5) * 1.5;

            world.spawnParticle(
                    EnumParticleTypes.VILLAGER_HAPPY,
                    villager.posX + offsetX,
                    villager.posY + offsetY,
                    villager.posZ + offsetZ,
                    0, 0.1, 0
            );
        }
    }

    private void spawnUnlockParticles(EntityVillager villager) {
        for (int i = 0; i < 10; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5);
            double offsetY = world.rand.nextDouble() * 1.5;
            double offsetZ = (world.rand.nextDouble() - 0.5);

            world.spawnParticle(
                    EnumParticleTypes.SPELL_WITCH,
                    villager.posX + offsetX,
                    villager.posY + offsetY,
                    villager.posZ + offsetZ,
                    0, 0, 0
            );
        }
    }

    private void onStructureFormed() {
        System.out.println("[智慧之泉] 结构已形成！");

        for (int i = 0; i < 50; i++) {
            double angle = (Math.PI * 2) * i / 50;
            double radius = 2.0;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double y = pos.getY() + 1;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

            world.spawnParticle(
                    EnumParticleTypes.ENCHANTMENT_TABLE,
                    x, y, z,
                    0, 0.5, 0
            );
        }
    }

    private void onStructureBroken() {
        System.out.println("[智慧之泉] 结构已破坏！");
    }

    public void onBroken() {
        isFormed = false;
    }

    public boolean isFormed() {
        return isFormed;
    }

    public int getRange() {
        return range;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("IsFormed", isFormed);
        compound.setInteger("Range", range);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        isFormed = compound.getBoolean("IsFormed");
        range = compound.getInteger("Range");
    }
}