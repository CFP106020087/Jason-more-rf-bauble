package com.moremod.energy;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 血液能量系统 - 事件处理器
 * 玩家攻击/击杀生物时，武器会累积血液能量和肉块组织
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class BloodEnergyHandler {

    // NBT 键名
    public static final String NBT_BLOOD_ENERGY = "BloodEnergy";
    public static final String NBT_FLESH_CHUNKS = "FleshChunks";
    public static final String NBT_BOSS_KILLS = "BossKills";
    public static final String NBT_LAST_HIT_TIME = "LastHitTime";

    // 能量配置
    public static final int BASE_ATTACK_ENERGY = 200;      // 基础攻击能量
    public static final int KILL_ENERGY_NORMAL = 2000;     // 普通怪物击杀
    public static final int KILL_ENERGY_ELITE = 10000;     // 精英怪物击杀
    public static final int KILL_ENERGY_BOSS = 100000;     // Boss击杀
    public static final int FLESH_ENERGY = 50000;          // 每个肉块转换能量

    public static final int MAX_BLOOD_ENERGY = 10000000;   // 最大血液能量 (1000万)
    public static final int MAX_FLESH_CHUNKS = 100;        // 最大肉块数量

    // 攻击节流 (防止高攻速武器刷血)
    private static final long HIT_COOLDOWN_MS = 500;       // 同一目标0.5秒冷却
    private static final Map<UUID, Long> lastHitTimes = new HashMap<>();

    /**
     * 攻击事件 - 累积血液能量
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            EntityLivingBase target = event.getEntityLiving();

            // 检查手持物品是否是武器
            ItemStack weapon = player.getHeldItemMainhand();
            if (!isValidWeapon(weapon)) return;

            // 节流检查
            UUID targetId = target.getUniqueID();
            long currentTime = System.currentTimeMillis();
            Long lastHit = lastHitTimes.get(targetId);
            if (lastHit != null && currentTime - lastHit < HIT_COOLDOWN_MS) {
                return;
            }
            lastHitTimes.put(targetId, currentTime);

            // 计算血液能量
            float damage = event.getAmount();
            int bloodEnergy = (int) (BASE_ATTACK_ENERGY * (1 + damage / 10f));

            // 暴击加成 (基于伤害判断)
            if (damage > 10) {
                bloodEnergy = (int) (bloodEnergy * 1.5f);
            }

            // 累积血液能量
            addBloodEnergy(weapon, bloodEnergy);
        }
    }

    /**
     * 击杀事件 - 累积肉块组织
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            EntityLivingBase target = event.getEntityLiving();

            ItemStack weapon = player.getHeldItemMainhand();
            if (!isValidWeapon(weapon)) return;

            // 根据生物类型决定奖励
            int bloodEnergy;
            int fleshChunks;
            boolean isBoss = false;

            if (isBossEntity(target)) {
                bloodEnergy = KILL_ENERGY_BOSS;
                fleshChunks = 10;
                isBoss = true;
            } else if (isEliteEntity(target)) {
                bloodEnergy = KILL_ENERGY_ELITE;
                fleshChunks = 3;
            } else {
                bloodEnergy = KILL_ENERGY_NORMAL;
                fleshChunks = 1;
            }

            // 根据生物最大生命值额外加成
            float maxHealth = target.getMaxHealth();
            if (maxHealth > 20) {
                bloodEnergy += (int) (maxHealth * 100);
                if (maxHealth > 100) {
                    fleshChunks += (int) (maxHealth / 50);
                }
            }

            addBloodEnergy(weapon, bloodEnergy);
            addFleshChunks(weapon, fleshChunks);

            if (isBoss) {
                incrementBossKills(weapon);
                // 击杀Boss时显示消息
                player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "★ " +
                    TextFormatting.RED + "Boss之血注入武器！" +
                    TextFormatting.GOLD + " +" + bloodEnergy + " 血液能量"
                ));
            }

            // 清理节流缓存
            lastHitTimes.remove(target.getUniqueID());
        }
    }

    /**
     * 检查是否是有效武器
     */
    public static boolean isValidWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // 剑和斧可以累积血液
        return stack.getItem() instanceof ItemSword ||
               stack.getItem() instanceof ItemAxe ||
               stack.getItem().getTranslationKey().contains("sword") ||
               stack.getItem().getTranslationKey().contains("blade") ||
               stack.getItem().getTranslationKey().contains("axe") ||
               stack.getItem().getTranslationKey().contains("katana") ||
               stack.getItem().getTranslationKey().contains("scythe");
    }

    /**
     * 检查是否是Boss
     */
    private static boolean isBossEntity(EntityLivingBase entity) {
        return entity instanceof EntityDragon ||
               entity instanceof EntityWither ||
               entity instanceof EntityElderGuardian ||
               entity.getMaxHealth() >= 200;
    }

    /**
     * 检查是否是精英怪物
     */
    private static boolean isEliteEntity(EntityLivingBase entity) {
        return entity instanceof EntityEnderman ||
               entity instanceof EntityBlaze ||
               entity instanceof EntityGhast ||
               entity instanceof EntityGuardian ||
               entity instanceof EntityWitch ||
               entity instanceof EntityEvoker ||
               entity instanceof EntityVindicator ||
               entity.getMaxHealth() >= 40;
    }

    // ========== NBT 操作方法 ==========

    public static void addBloodEnergy(ItemStack stack, int amount) {
        NBTTagCompound nbt = getOrCreateTag(stack);
        int current = nbt.getInteger(NBT_BLOOD_ENERGY);
        nbt.setInteger(NBT_BLOOD_ENERGY, Math.min(current + amount, MAX_BLOOD_ENERGY));
    }

    public static void addFleshChunks(ItemStack stack, int amount) {
        NBTTagCompound nbt = getOrCreateTag(stack);
        int current = nbt.getInteger(NBT_FLESH_CHUNKS);
        nbt.setInteger(NBT_FLESH_CHUNKS, Math.min(current + amount, MAX_FLESH_CHUNKS));
    }

    public static void incrementBossKills(ItemStack stack) {
        NBTTagCompound nbt = getOrCreateTag(stack);
        int current = nbt.getInteger(NBT_BOSS_KILLS);
        nbt.setInteger(NBT_BOSS_KILLS, current + 1);
    }

    public static int getBloodEnergy(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getInteger(NBT_BLOOD_ENERGY);
    }

    public static int getFleshChunks(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getInteger(NBT_FLESH_CHUNKS);
    }

    public static int getBossKills(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getInteger(NBT_BOSS_KILLS);
    }

    /**
     * 计算总能量值
     */
    public static int getTotalEnergy(ItemStack stack) {
        int blood = getBloodEnergy(stack);
        int flesh = getFleshChunks(stack) * FLESH_ENERGY;
        int bossBonus = getBossKills(stack) * 50000; // Boss击杀额外加成
        return blood + flesh + bossBonus;
    }

    /**
     * 清除所有血液数据
     */
    public static void clearBloodData(ItemStack stack) {
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            nbt.removeTag(NBT_BLOOD_ENERGY);
            nbt.removeTag(NBT_FLESH_CHUNKS);
            nbt.removeTag(NBT_BOSS_KILLS);
            nbt.removeTag(NBT_LAST_HIT_TIME);
        }
    }

    /**
     * 检查是否有血液数据
     */
    public static boolean hasBloodData(ItemStack stack) {
        return getBloodEnergy(stack) > 0 || getFleshChunks(stack) > 0;
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
