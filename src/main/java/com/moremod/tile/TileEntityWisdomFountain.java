package com.moremod.tile;

import com.moremod.config.ModConfig1;
import com.moremod.multiblock.MultiblockWisdomFountain;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class TileEntityWisdomFountain extends TileEntity implements ITickable {

    // 状态变量
    private boolean isFormed = false;
    private boolean wasFormed = false;
    private int checkTimer = 0;
    private int particleTimer = 0;
    private int effectTimer = 0;
    private int beaconTimer = 0;
    private int xpTimer = 0;
    private int auraTimer = 0;

    // 记录玩家进入/离开
    private Set<UUID> playersInRange = new HashSet<>();
    private Map<UUID, Integer> playerBuffLevel = new HashMap<>();
    private Map<UUID, Long> playerLastHeal = new HashMap<>();
    private Map<UUID, Long> playerLastRepair = new HashMap<>();
    private Set<UUID> playersBeingCleansed = new HashSet<>(); // 防止循环清除

    // 动画变量
    private float animationProgress = 0.0F;
    private float rotationAngle = 0.0F;
    private float auraRadius = 0.0F;
    private float pulseAnimation = 0.0F;

    private Random rand = new Random();

    // 配置
    private static final int RANGE_CORE = 5;
    private static final int RANGE_MEDIUM = 10;
    private static final int RANGE_MAX = 16;

    // 村民交易解锁
    private int villagerUnlockTimer = 0;

    @Override
    public void update() {
        if (world == null || pos == null) return;

        // 结构检测
        checkTimer++;
        if (checkTimer >= 100) { // 每5秒检查一次
            checkTimer = 0;
            updateStructureStatus();
        }

        // 只有激活时才运行效果
        if (isFormed) {
            updateAnimation();

            if (!world.isRemote) {
                // 服务器端逻辑
                applyEnhancedEffects();
                updateAura();
                spawnServerParticles();

                // 解锁附近村民的交易
                villagerUnlockTimer++;
                if (villagerUnlockTimer >= 100) { // 每5秒检查一次
                    villagerUnlockTimer = 0;
                    unlockNearbyVillagerTrades();
                }

                // 经验球生成 - 修复：检查附近是否有玩家
                xpTimer++;
                if (xpTimer >= 600) { // 30秒
                    xpTimer = 0;
                    // 只有附近有玩家时才生成经验球
                    if (hasPlayersNearby()) {
                        spawnMultipleExperienceOrbs();
                    }
                }

                // 稀有奖励 - 修复：同样检查玩家
                if (world.getTotalWorldTime() % 2400 == 0) { // 2分钟
                    if (hasPlayersNearby()) {
                        grantRareReward();
                    }
                }
            } else {
                // 客户端粒子效果
                spawnClientParticles();
                spawnAmbientParticles();
            }
        }
    }

    // 新增：检查附近是否有玩家
    private boolean hasPlayersNearby() {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(RANGE_MAX);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        return !players.isEmpty();
    }

    private void updateStructureStatus() {
        wasFormed = isFormed;
        isFormed = MultiblockWisdomFountain.checkStructure(world, pos);

        if (isFormed != wasFormed) {
            markDirty();

            if (!world.isRemote) {
                world.notifyBlockUpdate(pos, world.getBlockState(pos),
                        world.getBlockState(pos), 3);

                if (isFormed) {
                    playActivationSound();
                    notifyNearbyPlayers(
                            TextFormatting.AQUA + "§l━━━━━━━━━━━━━━━━━━━━\n" +
                                    TextFormatting.GOLD + "§l     ✦ 智慧之泉已激活 ✦\n" +
                                    TextFormatting.YELLOW + "   神秘的能量开始流动...\n" +
                                    TextFormatting.AQUA + "§l━━━━━━━━━━━━━━━━━━━━");

                    // 激活特效
                    for (int i = 0; i < 100; i++) {
                        spawnActivationParticles();
                    }

                    // 给予附近玩家瞬间奖励
                    grantActivationBonus();
                } else {
                    playDeactivationSound();
                    notifyNearbyPlayers(
                            TextFormatting.RED + "§l✦ 智慧之泉的魔力消散了... ✦");
                    clearAllPlayerBuffs();
                }
            }
        }
    }

    private void updateAnimation() {
        if (animationProgress < 1.0F) {
            animationProgress += 0.02F;
        }

        rotationAngle += 2.0F;
        if (rotationAngle >= 360.0F) {
            rotationAngle -= 360.0F;
        }

        // 光环脉动动画
        pulseAnimation += 0.1F;
        auraRadius = 2.0F + (float)Math.sin(pulseAnimation) * 0.5F;
    }

    private void applyEnhancedEffects() {
        effectTimer++;
        if (effectTimer < 40) return; // 每2秒应用一次
        effectTimer = 0;

        AxisAlignedBB area = new AxisAlignedBB(pos).grow(RANGE_MAX);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);

        Set<UUID> currentPlayers = new HashSet<>();

        for (EntityPlayer player : players) {
            UUID playerID = player.getUniqueID();
            currentPlayers.add(playerID);

            // 检查是否是新进入的玩家
            if (!playersInRange.contains(playerID)) {
                onPlayerEnterRange(player);
            }

            double distance = player.getDistance(pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5);

            // 根据距离给予不同等级的效果
            int buffLevel = calculateBuffLevel(distance);

            // 应用对应等级效果
            switch(buffLevel) {
                case 2:
                    applyLegendaryEffects(player);
                    break;
                case 1:
                    applyMediumEffects(player);
                    break;
                default:
                    applyBasicEffects(player);
                    break;
            }

            // 检查buff等级变化
            Integer oldLevel = playerBuffLevel.get(playerID);
            if (oldLevel == null || oldLevel != buffLevel) {
                playerBuffLevel.put(playerID, buffLevel);
                notifyBuffChange(player, buffLevel);
            }

            // 持续效果
            applyContinuousEffects(player, distance);
        }

        // 处理离开的玩家
        handleLeavingPlayers(currentPlayers);
        playersInRange = currentPlayers;

        // 清理完成清除状态的玩家
        playersBeingCleansed.clear();
    }

    private int calculateBuffLevel(double distance) {
        if (distance <= RANGE_CORE) return 2;
        if (distance <= RANGE_MEDIUM) return 1;
        return 0;
    }

    private void applyBasicEffects(EntityPlayer player) {
        // 基础增益
        player.addPotionEffect(new PotionEffect(MobEffects.LUCK, 300, 0, true, false));
        player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 400, 0, true, false));
        player.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, 300, 0, true, false));

        // 缓慢回复饱食度
        if (player.getFoodStats().getFoodLevel() < 20) {
            if (rand.nextInt(20) == 0) {
                player.getFoodStats().addStats(1, 0.5F);
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GREEN + "✧ 饱食度恢复"), true);
            }
        }

        // 清除轻微负面效果
        if (player.isPotionActive(MobEffects.HUNGER)) {
            player.removePotionEffect(MobEffects.HUNGER);
        }
        if (player.isPotionActive(MobEffects.SLOWNESS)) {
            player.removePotionEffect(MobEffects.SLOWNESS);
        }
    }

    private void applyMediumEffects(EntityPlayer player) {
        applyBasicEffects(player);

        // 中级增益
        player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 300, 0, true, false));
        player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 300, 1, true, false));
        player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 300, 0, true, false));
        player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 300, 0, true, false));

        // 持续治疗
        Long lastHeal = playerLastHeal.get(player.getUniqueID());
        long currentTime = world.getTotalWorldTime();

        if (lastHeal == null || currentTime - lastHeal > 40) { // 每2秒
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(2.0F);
                playerLastHeal.put(player.getUniqueID(), currentTime);
                spawnHealingParticles(player);
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "♥ 生命恢复 +2"), true);
            }
        }

        // 清除中等负面效果
        clearNegativeEffects(player, false);
    }

    private void applyLegendaryEffects(EntityPlayer player) {
        applyMediumEffects(player);

        // 传奇增益
        player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 300, 1, true, false));
        player.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 300, 1, true, false));
        player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 200, 1, true, false));
        player.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 300, 1, true, false));
        player.addPotionEffect(new PotionEffect(MobEffects.SATURATION, 100, 0, true, false));

        // 快速治疗
        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(4.0F);
            spawnHealingParticles(player);
        }

        // 完全清除负面效果
        clearNegativeEffects(player, true);

        // 快速修复所有装备
        Long lastRepair = playerLastRepair.get(player.getUniqueID());
        long currentTime = world.getTotalWorldTime();

        if (lastRepair == null || currentTime - lastRepair > 100) { // 每5秒
            repairAllEquipment(player);
            playerLastRepair.put(player.getUniqueID(), currentTime);
        }

        // 经验奖励
        if (rand.nextInt(100) == 0) {
            player.addExperience(10);
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GOLD + "✦ 智慧祝福 +10经验"), true);
            spawnExperienceParticles(player);
        }
    }

    private void applyContinuousEffects(EntityPlayer player, double distance) {
        // 装备修复
        if (rand.nextInt(distance <= RANGE_CORE ? 10 : 30) == 0) {
            repairRandomEquipment(player);
        }

        // 清除疲劳
        if (player.getFoodStats().getSaturationLevel() < 5.0F) {
            player.getFoodStats().addStats(0, 1.0F);
        }

        // 移除火焰
        if (player.isBurning()) {
            player.extinguish();
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.AQUA + "☄ 火焰熄灭"), true);
        }
    }

    // 修复：完善清除负面效果方法，避免循环
    private void clearNegativeEffects(EntityPlayer player, boolean all) {
        UUID playerID = player.getUniqueID();

        // 防止循环清除
        if (playersBeingCleansed.contains(playerID)) {
            return;
        }

        playersBeingCleansed.add(playerID);

        // 负面效果列表
        Potion[] negativeEffects = {
                MobEffects.POISON,
                MobEffects.WITHER,
                MobEffects.NAUSEA,
                MobEffects.BLINDNESS,
                MobEffects.MINING_FATIGUE,
                MobEffects.WEAKNESS,
                MobEffects.HUNGER,
                MobEffects.SLOWNESS
        };

        // 额外的严重负面效果（仅在all=true时清除）
        Potion[] severeNegativeEffects = {
                MobEffects.LEVITATION,
                MobEffects.UNLUCK,
        };

        // 清除基础负面效果
        for (Potion effect : negativeEffects) {
            if (player.isPotionActive(effect)) {
                player.removePotionEffect(effect);
            }
        }

        // 如果需要清除所有负面效果
        if (all) {
            for (Potion effect : severeNegativeEffects) {
                if (player.isPotionActive(effect)) {
                    player.removePotionEffect(effect);
                }
            }

            // 清除所有其他可能的负面效果
            Collection<PotionEffect> activeEffects = new ArrayList<>(player.getActivePotionEffects());
            for (PotionEffect effect : activeEffects) {
                Potion potion = effect.getPotion();
                // 检查是否为负面效果
                if (potion.isBadEffect()) {
                    player.removePotionEffect(potion);
                }
            }
        }
    }

    private void updateAura() {
        auraTimer++;
        if (auraTimer < 60) return; // 每3秒
        auraTimer = 0;

        // 只在有玩家时更新光环
        if (!hasPlayersNearby()) {
            return;
        }

        // 环境净化效果
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(RANGE_MAX);

        // 为范围内的作物加速生长
        for (int x = -RANGE_MAX; x <= RANGE_MAX; x++) {
            for (int z = -RANGE_MAX; z <= RANGE_MAX; z++) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (rand.nextInt(10) == 0) {
                        world.scheduleBlockUpdate(checkPos,
                                world.getBlockState(checkPos).getBlock(), 1, 0);
                    }
                }
            }
        }
    }

    // 新增：解锁附近村民的交易
    private void unlockNearbyVillagerTrades() {
        if (world.isRemote) return;

        // 在智慧之泉范围内查找所有村民
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(RANGE_MAX);
        List<EntityVillager> villagers = world.getEntitiesWithinAABB(EntityVillager.class, area);

        int unlockedCount = 0;

        for (EntityVillager villager : villagers) {
            // 获取村民的交易列表
            MerchantRecipeList recipes = villager.getRecipes(null);

            if (recipes != null && !recipes.isEmpty()) {
                boolean hasUnlocked = false;

                for (Object obj : recipes) {
                    if (obj instanceof MerchantRecipe) {
                        MerchantRecipe recipe = (MerchantRecipe) obj;

                        // 强制解锁所有交易，不管状态如何
                        // 总是尝试解锁，确保交易可用
                        if (forceUnlockRecipe(recipe)) {
                            hasUnlocked = true;
                            unlockedCount++;
                        }
                    }
                }

                if (hasUnlocked) {
                    // 刷新村民的交易列表（使用反射调用私有方法）
                    refreshVillagerTrades(villager);

                    // 在村民位置生成粒子效果
                    if (world instanceof WorldServer) {
                        WorldServer ws = (WorldServer) world;
                        ws.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                                villager.posX, villager.posY + 1, villager.posZ,
                                10, 0.5, 0.5, 0.5, 0.02);

                        // 额外的魔法粒子效果
                        ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                                villager.posX, villager.posY + 1.5, villager.posZ,
                                15, 0.3, 0.3, 0.3, 0.01);
                    }
                }
            }
        }

        // 如果解锁了交易，通知附近玩家
        if (unlockedCount > 0) {
            AxisAlignedBB playerArea = new AxisAlignedBB(pos).grow(RANGE_MAX + 5);
            List<EntityPlayer> nearbyPlayers = world.getEntitiesWithinAABB(EntityPlayer.class, playerArea);

            for (EntityPlayer player : nearbyPlayers) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GREEN + "✦ 智慧之泉解锁了 " +
                                unlockedCount + " 个村民交易！"), true);
            }

            // 播放音效
            world.playSound(null, pos, SoundEvents.ENTITY_VILLAGER_YES,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.BLOCKS, 0.8F, 1.2F);
        }
    }

    // 强制解锁交易（更激进的方法）
    private boolean forceUnlockRecipe(MerchantRecipe recipe) {
        try {
            boolean needsUnlock = false;

            // 获取所有相关字段
            Field usesField = null;
            Field maxUsesField = null;
            Field rewardExpField = null;

            // 查找使用次数字段
            String[] usesFieldNames = {"toolUses", "field_77400_d", "uses", "timesUsed"};
            for (String fieldName : usesFieldNames) {
                try {
                    usesField = MerchantRecipe.class.getDeclaredField(fieldName);
                    usesField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }

            // 查找最大使用次数字段
            String[] maxUsesFieldNames = {"maxTradeUses", "field_82785_e", "maxUses"};
            for (String fieldName : maxUsesFieldNames) {
                try {
                    maxUsesField = MerchantRecipe.class.getDeclaredField(fieldName);
                    maxUsesField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }

            // 查找奖励经验字段
            String[] rewardExpFieldNames = {"rewardsExp", "field_180323_f"};
            for (String fieldName : rewardExpFieldNames) {
                try {
                    rewardExpField = MerchantRecipe.class.getDeclaredField(fieldName);
                    rewardExpField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }

            // 检查并修复使用次数
            if (usesField != null) {
                int currentUses = usesField.getInt(recipe);

                // 如果使用次数不是0（包括9999这种错误值），就需要解锁
                if (currentUses != 0) {
                    usesField.setInt(recipe, 0);
                    needsUnlock = true;
                }
            }

            // 设置合理的最大使用次数
            if (maxUsesField != null) {
                int currentMaxUses = maxUsesField.getInt(recipe);

                // 如果最大使用次数小于100或是9999，设置为一个合理的大值
                if (currentMaxUses < 100 || currentMaxUses == 9999) {
                    maxUsesField.setInt(recipe, 1000); // 设置为1000而不是MAX_VALUE，避免溢出问题
                    needsUnlock = true;
                }
            }

            // 确保交易给予经验（可选）
            if (rewardExpField != null) {
                boolean rewardsExp = rewardExpField.getBoolean(recipe);
                if (!rewardsExp) {
                    rewardExpField.setBoolean(recipe, true);
                }
            }

            // 如果反射失败或需要额外保证，使用NBT方法
            if (!needsUnlock || usesField == null) {
                NBTTagCompound nbt = recipe.writeToTags();
                int nbtUses = nbt.getInteger("uses");
                int nbtMaxUses = nbt.getInteger("maxUses");

                // 检查NBT中的值
                if (nbtUses != 0 || nbtMaxUses < 100 || nbtMaxUses == 9999) {
                    nbt.setInteger("uses", 0);
                    nbt.setInteger("maxUses", 1000);
                    nbt.setBoolean("rewardExp", true);
                    recipe.readFromTags(nbt);
                    needsUnlock = true;
                }
            }

            return needsUnlock;

        } catch (Exception e) {
            // 如果所有方法都失败，至少尝试NBT方法
            try {
                NBTTagCompound nbt = recipe.writeToTags();
                nbt.setInteger("uses", 0);
                nbt.setInteger("maxUses", 1000);
                nbt.setBoolean("rewardExp", true);
                recipe.readFromTags(nbt);
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }
    }

    // 检查交易是否被锁定
    private boolean isRecipeLocked(MerchantRecipe recipe) {
        try {
            // 首先检查是否直接被禁用
            if (recipe.isRecipeDisabled()) {
                return true;
            }

            // 尝试获取使用次数字段
            Field usesField = null;
            Field maxUsesField = null;

            // 可能的字段名（根据不同的映射版本）
            String[] usesFieldNames = {"toolUses", "field_77400_d", "uses", "timesUsed"};
            String[] maxUsesFieldNames = {"maxTradeUses", "field_82785_e", "maxUses"};

            // 查找使用次数字段
            for (String fieldName : usesFieldNames) {
                try {
                    usesField = MerchantRecipe.class.getDeclaredField(fieldName);
                    usesField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }

            // 查找最大使用次数字段
            for (String fieldName : maxUsesFieldNames) {
                try {
                    maxUsesField = MerchantRecipe.class.getDeclaredField(fieldName);
                    maxUsesField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }

            if (usesField != null) {
                int uses = usesField.getInt(recipe);

                // 如果使用次数大于0，可能需要解锁
                if (uses > 0) {
                    if (maxUsesField != null) {
                        int maxUses = maxUsesField.getInt(recipe);
                        // 检查是否达到上限或被设置为9999（被错误的无限交易模组锁定）
                        return uses >= maxUses || uses >= 7 || uses == 9999;
                    }
                    return uses >= 7; // 默认7次后锁定
                }
            }

            return false;

        } catch (Exception e) {
            // 如果出错，检查是否禁用
            return recipe.isRecipeDisabled();
        }
    }

    // 解锁交易
    private boolean unlockRecipe(MerchantRecipe recipe) {
        try {
            // 方法1：使用反射直接设置字段
            boolean success = unlockRecipeByReflection(recipe);

            // 方法2：如果反射失败，尝试使用NBT
            if (!success) {
                success = unlockRecipeByNBT(recipe);
            }

            return success;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 通过反射解锁交易
    private boolean unlockRecipeByReflection(MerchantRecipe recipe) {
        try {
            Field usesField = null;
            Field maxUsesField = null;

            // 可能的字段名
            String[] usesFieldNames = {"toolUses", "field_77400_d", "uses", "timesUsed"};
            String[] maxUsesFieldNames = {"maxTradeUses", "field_82785_e", "maxUses"};

            // 查找使用次数字段
            for (String fieldName : usesFieldNames) {
                try {
                    usesField = MerchantRecipe.class.getDeclaredField(fieldName);
                    usesField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }

            // 查找最大使用次数字段
            for (String fieldName : maxUsesFieldNames) {
                try {
                    maxUsesField = MerchantRecipe.class.getDeclaredField(fieldName);
                    maxUsesField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException ignored) {}
            }

            if (usesField != null) {
                // 重置使用次数为0
                usesField.setInt(recipe, 0);

                if (maxUsesField != null) {
                    // 设置最大使用次数为一个较大的值（不用MAX_VALUE，避免其他模组的问题）
                    maxUsesField.setInt(recipe, 9999);
                }
                return true;
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    // 通过NBT解锁交易
    private boolean unlockRecipeByNBT(MerchantRecipe recipe) {
        try {
            // 将交易写入NBT
            NBTTagCompound nbt = recipe.writeToTags();

            // 修改NBT中的使用次数
            nbt.setInteger("uses", 0);
            nbt.setInteger("maxUses", 9999);

            // 从NBT读回
            recipe.readFromTags(nbt);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 刷新村民的交易（使用反射调用私有方法）
    private void refreshVillagerTrades(EntityVillager villager) {
        try {
            // 方法1：尝试调用 populateBuyingList
            boolean success = false;
            String[] methodNames = {"populateBuyingList", "func_175554_cu", "o"};

            for (String methodName : methodNames) {
                try {
                    Method method = EntityVillager.class.getDeclaredMethod(methodName);
                    method.setAccessible(true);
                    method.invoke(villager);
                    success = true;
                    break;
                } catch (NoSuchMethodException ignored) {}
            }

            // 方法2：如果上述方法失败，尝试通过设置需要重新计算标志
            if (!success) {
                // 尝试设置 needsInitilization 或类似的标志
                String[] fieldNames = {"needsInitilization", "field_175565_bs", "careerId"};

                for (String fieldName : fieldNames) {
                    try {
                        Field field = EntityVillager.class.getDeclaredField(fieldName);
                        field.setAccessible(true);

                        if (fieldName.equals("careerId")) {
                            // 通过改变careerId来触发重新生成交易
                            int currentCareerId = field.getInt(villager);
                            field.setInt(villager, 0);
                            field.setInt(villager, currentCareerId);
                        } else {
                            // 设置需要初始化标志
                            field.setBoolean(villager, true);
                        }
                        break;
                    } catch (Exception ignored) {}
                }

                // 方法3：触发村民更新
                villager.setCustomer(null);

                // 强制村民重新初始化交易
                NBTTagCompound compound = new NBTTagCompound();
                villager.writeEntityToNBT(compound);
                compound.setInteger("Career", villager.getProfession());
                compound.setInteger("CareerLevel", 1);
                villager.readEntityFromNBT(compound);
            }

        } catch (Exception e) {
            // 如果所有方法都失败，至少确保交易列表被标记为需要更新
            try {
                // 尝试获取并修改买卖列表
                Field buyingListField = null;
                String[] fieldNames = {"buyingList", "field_70963_i"};

                for (String fieldName : fieldNames) {
                    try {
                        buyingListField = EntityVillager.class.getDeclaredField(fieldName);
                        buyingListField.setAccessible(true);
                        break;
                    } catch (NoSuchFieldException ignored) {}
                }

                if (buyingListField != null) {
                    Object buyingList = buyingListField.get(villager);
                    if (buyingList instanceof MerchantRecipeList) {
                        // 触发列表更新
                        ((MerchantRecipeList) buyingList).size(); // 访问列表触发懒加载
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void onPlayerEnterRange(EntityPlayer player) {
        playersInRange.add(player.getUniqueID());

        // 欢迎消息
        player.sendMessage(new TextComponentString(""));
        player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "§l◆━━━━━━━━━━━━━━━━━━━━◆"));
        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "    §l欢迎来到智慧之泉"));
        player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "  神秘的力量正在保护你..."));
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "  • 距离越近，效果越强"));
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "  • 自动修复装备"));
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "  • 清除负面状态"));
        player.sendMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "  • 解锁村民交易"));
        player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "§l◆━━━━━━━━━━━━━━━━━━━━◆"));
        player.sendMessage(new TextComponentString(""));

        // 播放音效
        world.playSound(null, player.getPosition(),
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                SoundCategory.PLAYERS, 0.8F, 1.2F);

        // 初始粒子爆发
        spawnWelcomeParticles(player);
    }

    private void onPlayerLeaveRange(EntityPlayer player) {
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "§o智慧之泉的祝福正在消散..."));

        // 给予临时保护
        player.addPotionEffect(new PotionEffect(
                MobEffects.RESISTANCE, 200, 0, true, false));
        player.addPotionEffect(new PotionEffect(
                MobEffects.REGENERATION, 100, 0, true, false));

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.YELLOW + "✦ 残留祝福将持续10秒"), true);
    }

    private void handleLeavingPlayers(Set<UUID> currentPlayers) {
        Set<UUID> leftPlayers = new HashSet<>(playersInRange);
        leftPlayers.removeAll(currentPlayers);

        for (UUID playerID : leftPlayers) {
            EntityPlayer player = world.getPlayerEntityByUUID(playerID);
            if (player != null) {
                onPlayerLeaveRange(player);
            }
            playerBuffLevel.remove(playerID);
            playerLastHeal.remove(playerID);
            playerLastRepair.remove(playerID);
            playersBeingCleansed.remove(playerID);
        }
    }

    private void notifyBuffChange(EntityPlayer player, int level) {
        String title = "";
        String subtitle = "";

        switch (level) {
            case 2:
                title = TextFormatting.GOLD + "§l★ 传奇祝福 ★";
                subtitle = TextFormatting.YELLOW + "你站在智慧的中心！";
                break;
            case 1:
                title = TextFormatting.YELLOW + "§l☆ 中级祝福 ☆";
                subtitle = TextFormatting.GREEN + "智慧之力环绕着你";
                break;
            default:
                title = TextFormatting.GREEN + "§l○ 基础祝福 ○";
                subtitle = TextFormatting.AQUA + "感受智慧的光芒";
                break;
        }

        player.sendStatusMessage(new TextComponentString(title + " " + subtitle), true);
    }

    private void repairRandomEquipment(EntityPlayer player) {
        List<ItemStack> damagedItems = new ArrayList<>();

        // 收集受损装备
        if (player.getHeldItemMainhand().isItemDamaged())
            damagedItems.add(player.getHeldItemMainhand());
        if (player.getHeldItemOffhand().isItemDamaged())
            damagedItems.add(player.getHeldItemOffhand());

        for (ItemStack armor : player.getArmorInventoryList()) {
            if (!armor.isEmpty() && armor.isItemDamaged()) {
                damagedItems.add(armor);
            }
        }

        if (!damagedItems.isEmpty()) {
            ItemStack toRepair = damagedItems.get(rand.nextInt(damagedItems.size()));
            int repairAmount = Math.min(toRepair.getItemDamage(), 20);
            toRepair.setItemDamage(toRepair.getItemDamage() - repairAmount);

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "✧ 修复: " + toRepair.getDisplayName()), true);

            spawnRepairParticles(player);
        }
    }

    private void repairAllEquipment(EntityPlayer player) {
        boolean repaired = false;

        // 完全修复所有装备
        if (repairItemFully(player.getHeldItemMainhand())) repaired = true;
        if (repairItemFully(player.getHeldItemOffhand())) repaired = true;

        for (ItemStack armor : player.getArmorInventoryList()) {
            if (repairItemFully(armor)) repaired = true;
        }

        if (repaired) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "✦ 所有装备已完全修复！"));
            spawnMassRepairParticles(player);
        }
    }

    private boolean repairItemFully(ItemStack stack) {
        if (!stack.isEmpty() && stack.isItemDamaged()) {
            stack.setItemDamage(0);
            return true;
        }
        return false;
    }

    // 修复：只在有玩家时生成经验球
    private void spawnMultipleExperienceOrbs() {
        if (!(world instanceof WorldServer)) return;

        // 再次确认有玩家
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(RANGE_MAX);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);

        if (players.isEmpty()) {
            return;
        }

        WorldServer ws = (WorldServer) world;

        // 生成5-8个经验球
        int count = 5 + rand.nextInt(4);
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            double radius = 1.5;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

            EntityXPOrb orb = new EntityXPOrb(world, x, pos.getY() + 2, z,
                    10 + rand.nextInt(15));
            world.spawnEntity(orb);
        }

        // 特效
        ws.spawnParticle(EnumParticleTypes.TOTEM,
                pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5,
                20, 1, 1, 1, 0.5);

        notifyNearbyPlayers(TextFormatting.GREEN +
                "✦ 智慧之泉释放了大量经验！");
    }

    // 修复：只在核心范围有玩家时给予稀有奖励
    private void grantRareReward() {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(RANGE_CORE);
        List<EntityPlayer> nearbyPlayers = world.getEntitiesWithinAABB(EntityPlayer.class, area);

        if (!nearbyPlayers.isEmpty()) {
            for (EntityPlayer player : nearbyPlayers) {
                // 给予1级经验
                player.addExperienceLevel(1);

                // 特殊效果
                player.addPotionEffect(new PotionEffect(
                        MobEffects.GLOWING, 200, 0, true, false));
                player.addPotionEffect(new PotionEffect(
                        MobEffects.HEALTH_BOOST, 1200, 1, true, false));

                player.sendMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "§l✦ 获得智慧的终极祝福！+1级经验 ✦"));
            }

            // 大型特效
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                        100, 2, 2, 2, 0.1);
            }
        }
    }

    private void grantActivationBonus() {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(RANGE_MAX);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);

        for (EntityPlayer player : players) {
            // 瞬间治疗
            player.heal(player.getMaxHealth());

            // 清除所有负面效果（安全方式）
            UUID playerID = player.getUniqueID();
            if (!playersBeingCleansed.contains(playerID)) {
                playersBeingCleansed.add(playerID);

                // 移除所有药水效果
                player.clearActivePotions();

                // 给予强力buff
                player.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 600, 2));
                player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 300, 1));
                player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 400, 1));

                playersBeingCleansed.remove(playerID);
            }

            // 饱食度满
            player.getFoodStats().addStats(20, 20.0F);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "§l✧ 智慧之泉赐予你力量！"));
        }
    }

    // ========== 粒子效果 ==========

    private void spawnServerParticles() {
        if (!(world instanceof WorldServer)) return;

        // 修复：只在有玩家时生成服务器粒子
        if (!hasPlayersNearby()) {
            return;
        }

        WorldServer ws = (WorldServer) world;

        particleTimer++;
        if (particleTimer < 5) return;
        particleTimer = 0;

        // 中心能量柱
        for (int i = 0; i < 5; i++) {
            ws.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                    pos.getX() + 0.5, pos.getY() + 1 + i * 0.5, pos.getZ() + 0.5,
                    3, 0.2, 0.1, 0.2, 0.05);
        }

        // 多层旋转光环
        double time = world.getTotalWorldTime() * 2;
        for (int ring = 0; ring < 3; ring++) {
            double radius = 1.5 + ring * 0.7;
            double height = 1.5 + ring * 0.3;
            int particles = 8 - ring * 2;

            for (int i = 0; i < particles; i++) {
                double angle = (time + ring * 120 + (360.0 / particles * i)) % 360 * Math.PI / 180;
                double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

                EnumParticleTypes type = ring == 0 ? EnumParticleTypes.PORTAL :
                        ring == 1 ? EnumParticleTypes.END_ROD :
                                EnumParticleTypes.SPELL_WITCH;

                ws.spawnParticle(type, x, pos.getY() + height, z,
                        1, 0, 0, 0, 0.01);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void spawnClientParticles() {
        if (rand.nextInt(2) != 0) return;

        // 上升粒子流
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 0.8;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 0.8;

            world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                    x, y, z, 0, 0.1, 0);
        }

        // 环境光点
        if (rand.nextInt(10) == 0) {
            double range = 3 + rand.nextDouble() * 2;
            double angle = rand.nextDouble() * Math.PI * 2;
            double x = pos.getX() + 0.5 + Math.cos(angle) * range;
            double y = pos.getY() + rand.nextDouble() * 3;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * range;

            world.spawnParticle(EnumParticleTypes.END_ROD,
                    x, y, z, 0, -0.02, 0);
        }
    }

    @SideOnly(Side.CLIENT)
    private void spawnAmbientParticles() {
        if (rand.nextInt(20) != 0) return;

        // 地面光环
        double angle = rand.nextDouble() * Math.PI * 2;
        double radius = 2 + rand.nextDouble() * 2;
        double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
        double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

        world.spawnParticle(EnumParticleTypes.SPELL_MOB,
                x, pos.getY() + 0.1, z,
                0.5, 1.0, 1.0);
    }

    private void spawnActivationParticles() {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        double x = pos.getX() + 0.5 + (rand.nextDouble() - 0.5) * 4;
        double y = pos.getY() + rand.nextDouble() * 4;
        double z = pos.getZ() + 0.5 + (rand.nextDouble() - 0.5) * 4;

        ws.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                x, y, z, 1, 0, 0.1, 0, 0.2);
    }

    private void spawnHealingParticles(EntityPlayer player) {
        if (!(world instanceof WorldServer)) return;
        ((WorldServer) world).spawnParticle(EnumParticleTypes.HEART,
                player.posX, player.posY + 1, player.posZ,
                5, 0.5, 0.5, 0.5, 0.01);
    }

    private void spawnRepairParticles(EntityPlayer player) {
        if (!(world instanceof WorldServer)) return;
        ((WorldServer) world).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                player.posX, player.posY + 1, player.posZ,
                3, 0.3, 0.3, 0.3, 0.01);
    }

    private void spawnMassRepairParticles(EntityPlayer player) {
        if (!(world instanceof WorldServer)) return;
        ((WorldServer) world).spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                player.posX, player.posY + 1, player.posZ,
                20, 0.5, 0.5, 0.5, 0.1);
    }

    private void spawnExperienceParticles(EntityPlayer player) {
        if (!(world instanceof WorldServer)) return;
        ((WorldServer) world).spawnParticle(EnumParticleTypes.TOTEM,
                player.posX, player.posY + 1, player.posZ,
                10, 0.3, 0.5, 0.3, 0.2);
    }

    private void spawnWelcomeParticles(EntityPlayer player) {
        if (!(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;

        // 螺旋上升粒子
        for (int i = 0; i < 20; i++) {
            double angle = (Math.PI * 2 * i) / 20;
            double radius = 1.5;
            double height = i * 0.2;
            double x = player.posX + Math.cos(angle) * radius;
            double z = player.posZ + Math.sin(angle) * radius;

            ws.spawnParticle(EnumParticleTypes.SPELL_INSTANT,
                    x, player.posY + height, z,
                    1, 0, 0, 0, 0.0);
        }
    }

    // ========== 声音效果 ==========

    private void playActivationSound() {
        // 多层声音
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BELL,
                SoundCategory.BLOCKS, 2.0F, 1.0F);
        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.BLOCKS, 1.5F, 1.5F);
        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                SoundCategory.BLOCKS, 1.0F, 0.8F);
    }

    private void playDeactivationSound() {
        world.playSound(null, pos, SoundEvents.BLOCK_NOTE_FLUTE,
                SoundCategory.BLOCKS, 2.0F, 0.8F);
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_HIT,
                SoundCategory.BLOCKS, 1.0F, 0.5F);
    }

    // ========== 工具方法 ==========

    private void notifyNearbyPlayers(String message) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(RANGE_MAX + 5);
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);

        for (EntityPlayer player : players) {
            player.sendMessage(new TextComponentString(message));
        }
    }

    private void clearAllPlayerBuffs() {
        playersInRange.clear();
        playerBuffLevel.clear();
        playerLastHeal.clear();
        playerLastRepair.clear();
        playersBeingCleansed.clear();
    }

    // ========== Getters ==========

    public boolean isFormed() { return isFormed; }
    public float getAnimationProgress() { return animationProgress; }
    public float getRotationAngle() { return rotationAngle; }
    public float getAuraRadius() { return auraRadius; }
    public BlockPos getCorePosition() { return pos; }

    // ========== NBT ==========

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("IsFormed", isFormed);
        compound.setFloat("AnimationProgress", animationProgress);
        compound.setFloat("RotationAngle", rotationAngle);
        compound.setFloat("AuraRadius", auraRadius);
        compound.setInteger("CheckTimer", checkTimer);
        compound.setInteger("EffectTimer", effectTimer);
        compound.setInteger("XpTimer", xpTimer);
        compound.setInteger("AuraTimer", auraTimer);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        isFormed = compound.getBoolean("IsFormed");
        animationProgress = compound.getFloat("AnimationProgress");
        rotationAngle = compound.getFloat("RotationAngle");
        auraRadius = compound.getFloat("AuraRadius");
        checkTimer = compound.getInteger("CheckTimer");
        effectTimer = compound.getInteger("EffectTimer");
        xpTimer = compound.getInteger("XpTimer");
        auraTimer = compound.getInteger("AuraTimer");
    }

    // ========== 网络同步 ==========

    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound tag = pkt.getNbtCompound();
        readFromNBT(tag);

        if (world != null) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    // ========== 渲染相关 ==========

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 4096.0D; // 64格
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos).grow(5);
    }

    // ========== 生命周期 ==========

    @Override
    public void onLoad() {
        if (world != null && !world.isRemote) {
            // 延迟检查，避免加载时块未完全加载
            world.scheduleUpdate(pos, world.getBlockState(pos).getBlock(), 20);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        isFormed = false;
        clearAllPlayerBuffs();
    }
}