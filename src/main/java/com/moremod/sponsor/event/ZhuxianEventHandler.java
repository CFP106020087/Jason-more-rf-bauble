package com.moremod.sponsor.event;

import com.moremod.sponsor.item.ZhuxianSword;
import com.moremod.sponsor.util.TrueDamageHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 诛仙四剑事件处理器
 *
 * 处理:
 * - 击杀计数与形态升级
 * - 免疫伤害（陷仙形态）
 * - 流血效果（绝仙形态）
 * - 太平领域效果
 * - 为天地立心：经验加成
 * - 为生民立命：血量保护、村民保护
 * - 为往圣继绝学：村民附近敌人真伤
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ZhuxianEventHandler {

    // 防止重复处理
    private static final Set<Integer> PROCESSING_KILLS = new HashSet<>();

    // ==================== 击杀处理 ====================

    /**
     * 处理击杀事件 - 更新击杀计数
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event.getSource() == null) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        ItemStack sword = ZhuxianSword.getZhuxianSword(player);
        if (sword.isEmpty()) return;

        int entityId = event.getEntity().getEntityId();
        if (!PROCESSING_KILLS.add(entityId)) return;

        try {
            ZhuxianSword item = (ZhuxianSword) sword.getItem();
            ZhuxianSword.SwordForm form = item.getForm(sword);

            // 记录击杀前的状态
            boolean hadAoe = item.hasAoe(sword);
            float oldDamage = item.getTotalDamage(sword);

            // 增加击杀计数
            item.addKill(sword);

            // 检查是否解锁了新能力
            float newDamage = item.getTotalDamage(sword);
            boolean hasAoeNow = item.hasAoe(sword);

            if (form == ZhuxianSword.SwordForm.ZHUXIAN && newDamage > oldDamage) {
                player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GOLD + "★ 诛仙剑伤害提升! " + TextFormatting.RED + String.format("%.0f", newDamage)
                ), true);
            }

            if (form == ZhuxianSword.SwordForm.LUXIAN && !hadAoe && hasAoeNow) {
                player.sendStatusMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "★★ 戮仙剑已解锁范围伤害!"
                ), true);
            }

            if (form == ZhuxianSword.SwordForm.XIANXIAN) {
                int charges = item.getImmunityCharges(sword);
                player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GOLD + "☆ 陷仙剑: 免疫层数 +" + TextFormatting.AQUA + charges
                ), true);
            }
        } finally {
            PROCESSING_KILLS.remove(entityId);
        }
    }

    // ==================== 伤害免疫（陷仙形态） ====================

    /**
     * 陷仙形态：消耗免疫层数抵挡伤害
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerAttacked(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        ItemStack sword = ZhuxianSword.getZhuxianSword(player);
        if (sword.isEmpty()) return;

        ZhuxianSword item = (ZhuxianSword) sword.getItem();

        // 陷仙形态：消耗免疫层数
        if (item.getForm(sword) == ZhuxianSword.SwordForm.XIANXIAN ||
            item.getForm(sword) == ZhuxianSword.SwordForm.JUEXIAN) {
            if (item.consumeImmunityCharge(sword)) {
                event.setCanceled(true);
                player.sendStatusMessage(new TextComponentString(
                    TextFormatting.AQUA + "✦ 免疫伤害! 剩余: " + item.getImmunityCharges(sword)
                ), true);
                return;
            }
        }

        // 为生民立命：血量不低于20%
        if (ZhuxianSword.isPlayerSkillActive(player, ZhuxianSword.NBT_SKILL_LIMING)) {
            float currentHealth = player.getHealth();
            float minHealth = player.getMaxHealth() * 0.2f;
            float damage = event.getAmount();

            if (currentHealth - damage < minHealth) {
                // 限制伤害
                float maxDamage = currentHealth - minHealth;
                if (maxDamage <= 0) {
                    event.setCanceled(true);
                    player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GREEN + "✦ 为生民立命: 血量保护触发!"
                    ), true);
                }
                // 伤害限制在LivingHurtEvent中处理
            }
        }
    }

    /**
     * 为生民立命：限制伤害使血量不低于20%
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        if (ZhuxianSword.isPlayerSkillActive(player, ZhuxianSword.NBT_SKILL_LIMING)) {
            float currentHealth = player.getHealth();
            float minHealth = player.getMaxHealth() * 0.2f;
            float damage = event.getAmount();

            if (currentHealth - damage < minHealth) {
                float maxDamage = Math.max(0, currentHealth - minHealth);
                event.setAmount(maxDamage);
            }
        }
    }

    // ==================== 经验加成（为天地立心） ====================

    /**
     * 为天地立心：经验获取+30%
     */
    @SubscribeEvent
    public static void onPlayerPickupXp(PlayerPickupXpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        if (ZhuxianSword.isPlayerSkillActive(player, ZhuxianSword.NBT_SKILL_TIANXIN)) {
            // 单手持剑时才生效
            ItemStack sword = ZhuxianSword.getZhuxianSword(player);
            if (!sword.isEmpty() && player.getHeldItemOffhand().isEmpty()) {
                int originalXp = event.getOrb().xpValue;
                int bonusXp = (int) (originalXp * 0.3f);
                event.getOrb().xpValue = originalXp + bonusXp;
            }
        }
    }

    // ==================== Tick处理 ====================

    /**
     * 每Tick处理:
     * - 流血效果
     * - 为往圣继绝学：村民附近敌人真伤
     * - 太平领域效果
     * - 村民保护
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;

        World world = event.world;
        long worldTime = world.getTotalWorldTime();

        // 每秒处理一次
        if (worldTime % 20 != 0) return;

        // 处理所有玩家
        for (EntityPlayer player : world.playerEntities) {
            ItemStack sword = ZhuxianSword.getZhuxianSword(player);
            if (sword.isEmpty()) continue;

            ZhuxianSword item = (ZhuxianSword) sword.getItem();

            // 为往圣继绝学：村民附近敌人每秒5%真伤
            if (item.isSkillActive(sword, ZhuxianSword.NBT_SKILL_JUEXUE)) {
                processJuexueEffect(player, world);
            }

            // 为生民立命：村民无敌
            if (item.isSkillActive(sword, ZhuxianSword.NBT_SKILL_LIMING)) {
                processLimingVillagerProtection(player, world);
            }

            // 太平领域
            NBTTagCompound tag = sword.getTagCompound();
            if (tag != null) {
                long taipingEndTime = tag.getLong(ZhuxianSword.NBT_TAIPING_END_TIME);
                if (taipingEndTime > worldTime) {
                    processTaipingDomain(player, world);
                }
            }
        }

        // 处理流血效果
        processBleedingEntities(world);
    }

    /**
     * 为往圣继绝学：村民附近敌人每秒5%真伤
     */
    private static void processJuexueEffect(EntityPlayer player, World world) {
        double range = 32.0; // 检测范围

        // 找到附近所有村民
        List<EntityVillager> villagers = world.getEntitiesWithinAABB(EntityVillager.class,
            player.getEntityBoundingBox().grow(range));

        for (EntityVillager villager : villagers) {
            // 村民附近10格内的敌对生物
            List<EntityMob> mobs = world.getEntitiesWithinAABB(EntityMob.class,
                villager.getEntityBoundingBox().grow(10.0));

            for (EntityMob mob : mobs) {
                // 5%最大生命真伤
                TrueDamageHelper.dealPercentTrueDamage(player, mob, 0.05f);
            }
        }
    }

    /**
     * 为生民立命：村民获得无敌
     */
    private static void processLimingVillagerProtection(EntityPlayer player, World world) {
        double range = 16.0;

        List<EntityVillager> villagers = world.getEntitiesWithinAABB(EntityVillager.class,
            player.getEntityBoundingBox().grow(range));

        for (EntityVillager villager : villagers) {
            // 给予抗性提升效果作为无敌
            villager.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 40, 254)); // 255级抗性=无敌
            // 标记为受保护（用于交易特价，在Mixin中处理）
            villager.getEntityData().setBoolean("ZhuxianProtected", true);
        }
    }

    /**
     * 太平领域：敌对生物停止攻击
     */
    private static void processTaipingDomain(EntityPlayer player, World world) {
        double range = 5.0;

        List<EntityLiving> entities = world.getEntitiesWithinAABB(EntityLiving.class,
            player.getEntityBoundingBox().grow(range));

        for (EntityLiving entity : entities) {
            if (entity instanceof EntityMob) {
                EntityMob mob = (EntityMob) entity;

                // 清除攻击目标
                mob.setAttackTarget(null);
                mob.setRevengeTarget(null);

                // 清除导航路径
                if (mob.getNavigator() != null) {
                    mob.getNavigator().clearPath();
                }

                // 添加缓慢效果
                mob.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 9));
            }
        }

        // 尝试恢复SimpleDifficulty口渴值
        tryRefillThirst(player);
    }

    /**
     * 尝试恢复SimpleDifficulty口渴值
     */
    private static void tryRefillThirst(EntityPlayer player) {
        try {
            // 尝试通过反射调用SimpleDifficulty
            Class<?> thirstClass = Class.forName("com.charles445.simpledifficulty.api.SDCapabilities");
            // 具体实现取决于SimpleDifficulty的API
        } catch (ClassNotFoundException e) {
            // SimpleDifficulty未安装，忽略
        } catch (Exception e) {
            // 其他错误，忽略
        }
    }

    /**
     * 处理流血效果
     */
    private static void processBleedingEntities(World world) {
        long worldTime = world.getTotalWorldTime();

        for (EntityLivingBase entity : world.getEntities(EntityLivingBase.class,
            e -> e.getEntityData().getBoolean("ZhuxianBleeding"))) {

            long endTime = entity.getEntityData().getLong("ZhuxianBleedEndTime");

            if (worldTime < endTime) {
                // 每秒20%最大生命真伤
                TrueDamageHelper.dealPercentTrueDamage(null, entity, 0.20f);
            } else {
                // 流血结束
                entity.getEntityData().setBoolean("ZhuxianBleeding", false);
            }
        }
    }

    // ==================== 村民攻击保护 ====================

    /**
     * 阻止敌对生物攻击受保护的村民
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onVillagerAttacked(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityVillager)) return;

        EntityVillager villager = (EntityVillager) event.getEntityLiving();

        if (villager.getEntityData().getBoolean("ZhuxianProtected")) {
            event.setCanceled(true);
        }
    }

    /**
     * 阻止敌对生物锁定受保护的村民
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSetTarget(LivingSetAttackTargetEvent event) {
        if (!(event.getTarget() instanceof EntityVillager)) return;

        EntityVillager villager = (EntityVillager) event.getTarget();

        if (villager.getEntityData().getBoolean("ZhuxianProtected")) {
            if (event.getEntityLiving() instanceof EntityLiving) {
                ((EntityLiving) event.getEntityLiving()).setAttackTarget(null);
            }
        }
    }

    // ==================== 清理 ====================

    /**
     * 定期清理
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 每5秒清理一次
        if (System.currentTimeMillis() % 5000 < 50) {
            PROCESSING_KILLS.clear();
            TrueDamageHelper.cleanup();
        }
    }
}
