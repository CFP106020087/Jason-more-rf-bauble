package com.moremod.system.humanity.intel;

import com.moremod.config.HumanityConfig;
import com.moremod.moremod;
import com.moremod.system.humanity.BiologicalProfile;
import com.moremod.system.humanity.HumanityCapabilityHandler;
import com.moremod.system.humanity.HumanitySpectrumSystem;
import com.moremod.system.humanity.IHumanityData;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 情报系统事件处理器
 * Intel System Event Handler
 *
 * 处理：
 * 1. 高人性玩家击杀怪物时掉落生物样本
 * 2. 根据已学习情报应用伤害加成
 *
 * 性能优化：
 * - 使用实体类型缓存避免重复字符串操作
 * - 合并Capability查询减少开销
 * - 伤害加成使用缓存减少高频计算
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class IntelEventHandler {

    // 高人性门槛 (只有高人性玩家才能使用情报系统)
    private static final float HIGH_HUMANITY_THRESHOLD = 50.0f;

    // 样本掉落基础概率
    private static final float BASE_DROP_CHANCE = 0.05f; // 5%

    // 人性值加成（每1点超过50的人性值增加的掉落率）
    private static final float HUMANITY_DROP_BONUS = 0.005f; // 每1点 +0.5%

    // ========== 伤害加成缓存（性能优化）==========
    /** 缓存结构：玩家UUID -> (实体ID -> (加成倍率, 过期tick)) */
    private static final java.util.Map<java.util.UUID, java.util.Map<ResourceLocation, CachedMultiplier>> damageCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 缓存有效期：100 tick = 5秒 */
    private static final int CACHE_DURATION = 100;

    private static class CachedMultiplier {
        final float multiplier;
        final long expireTick;

        CachedMultiplier(float multiplier, long expireTick) {
            this.multiplier = multiplier;
            this.expireTick = expireTick;
        }
    }

    /**
     * 击杀事件 - 处理样本掉落
     * 优化：合并Capability查询，使用实体类型缓存
     */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLivingBase target = event.getEntityLiving();

        if (player.world.isRemote) return;

        // 一次性获取Capability数据（优化：从3次查询减少到1次）
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        float humanity = data.getHumanity();
        if (humanity < HIGH_HUMANITY_THRESHOLD) return;

        // 获取实体ID（只获取一次）
        ResourceLocation entityId = EntityList.getKey(target);
        if (entityId == null) return;

        // 计算掉落概率（使用缓存的实体类型）
        float dropChance = calculateDropChance(data, humanity, entityId);

        // 随机判定
        if (player.world.rand.nextFloat() < dropChance) {
            dropBiologicalSample(target, entityId);
        }
    }

    /**
     * 计算掉落概率（优化版本）
     * 使用实体类型缓存，避免重复字符串操作
     */
    private static float calculateDropChance(IHumanityData data, float humanity, ResourceLocation entityId) {
        float chance = BASE_DROP_CHANCE;

        // 人性值加成 (50-100之间每1点增加0.5%)
        chance += (humanity - HIGH_HUMANITY_THRESHOLD) * HUMANITY_DROP_BONUS;

        // 使用缓存的实体类型判断（优化：从~30μs降至~0.1μs）
        BiologicalProfile.EntityType entityType = BiologicalProfile.getEntityType(entityId);
        switch (entityType) {
            case BOSS:
                chance += 0.50f; // Boss额外 +50%
                break;
            case ELITE:
                chance += 0.20f; // 精英怪额外 +20%
                break;
            default:
                break;
        }

        // 狩猎协议加成：已分析的生物掉落率提升
        BiologicalProfile profile = data.getProfile(entityId);
        if (profile != null) {
            switch (profile.getCurrentTier()) {
                case BASIC:
                    chance += 0.20f;
                    break;
                case COMPLETE:
                    chance += 0.40f;
                    break;
                case MASTERED:
                    chance += 0.60f;
                    break;
                default:
                    break;
            }
        }

        return Math.min(1.0f, chance);
    }

    /**
     * 掉落生物样本（优化版本）
     * 接受已获取的entityId避免重复查询
     */
    private static void dropBiologicalSample(EntityLivingBase target, ResourceLocation entityId) {
        ItemStack sample = ItemBiologicalSample.createSampleFromId(target, entityId);
        if (sample.isEmpty()) return;

        // 在目标位置掉落
        EntityItem itemEntity = new EntityItem(
                target.world,
                target.posX,
                target.posY + 0.5,
                target.posZ,
                sample
        );
        itemEntity.setDefaultPickupDelay();
        target.world.spawnEntity(itemEntity);
    }

    /**
     * 伤害事件 - 应用情报加成
     * 优化：使用缓存避免每次伤害都计算
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 跳过自定义伤害源
        String damageType = event.getSource().getDamageType();
        if (damageType != null && damageType.startsWith("moremod_")) {
            return;
        }

        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLivingBase target = event.getEntityLiving();

        if (player.world.isRemote) return;

        // 获取实体ID
        ResourceLocation entityId = EntityList.getKey(target);
        if (entityId == null) return;

        // 尝试从缓存获取伤害倍率
        float damageMultiplier = getCachedDamageMultiplier(player, entityId);

        // 缓存未命中时计算
        if (damageMultiplier < 0) {
            IHumanityData data = HumanityCapabilityHandler.getData(player);
            if (data == null || !data.isSystemActive()) return;

            float humanity = data.getHumanity();
            if (humanity < HIGH_HUMANITY_THRESHOLD) return;

            damageMultiplier = IntelDataHelper.calculateDamageMultiplier(data, entityId);

            // 存入缓存
            cacheDamageMultiplier(player, entityId, damageMultiplier);
        }

        // 应用加成
        if (damageMultiplier > 1.0f) {
            event.setAmount(event.getAmount() * damageMultiplier);
        }
    }

    /**
     * 从缓存获取伤害倍率
     * @return 倍率，或 -1 表示缓存未命中
     */
    private static float getCachedDamageMultiplier(EntityPlayer player, ResourceLocation entityId) {
        java.util.Map<ResourceLocation, CachedMultiplier> playerCache = damageCache.get(player.getUniqueID());
        if (playerCache == null) return -1;

        CachedMultiplier cached = playerCache.get(entityId);
        if (cached == null) return -1;

        // 检查是否过期
        if (player.world.getTotalWorldTime() > cached.expireTick) {
            playerCache.remove(entityId);
            return -1;
        }

        return cached.multiplier;
    }

    /**
     * 缓存伤害倍率
     */
    private static void cacheDamageMultiplier(EntityPlayer player, ResourceLocation entityId, float multiplier) {
        java.util.Map<ResourceLocation, CachedMultiplier> playerCache =
                damageCache.computeIfAbsent(player.getUniqueID(), k -> new java.util.concurrent.ConcurrentHashMap<>());

        long expireTick = player.world.getTotalWorldTime() + CACHE_DURATION;
        playerCache.put(entityId, new CachedMultiplier(multiplier, expireTick));
    }

    /**
     * 清除玩家的伤害缓存（玩家登出时调用）
     */
    public static void clearPlayerCache(java.util.UUID playerUUID) {
        damageCache.remove(playerUUID);
    }

    /**
     * 掉落事件 - 狩猎协议掉落加成
     * 已分析的生物掉落物数量提升
     */
    @SubscribeEvent
    public static void onLivingDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLivingBase target = event.getEntityLiving();

        if (player.world.isRemote) return;

        // 检查人性值系统
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        float humanity = data.getHumanity();
        if (humanity < HIGH_HUMANITY_THRESHOLD) return;

        // 获取目标的档案
        ResourceLocation entityId = EntityList.getKey(target);
        if (entityId == null) return;

        BiologicalProfile profile = data.getProfile(entityId);
        if (profile == null) return;

        // 根据档案等级计算掉落倍率
        float dropMultiplier = 1.0f;
        switch (profile.getCurrentTier()) {
            case BASIC:
                dropMultiplier = 1.2f;  // +20%
                break;
            case COMPLETE:
                dropMultiplier = 1.5f;  // +50%
                break;
            case MASTERED:
                dropMultiplier = 2.0f;  // +100% (双倍掉落)
                break;
            default:
                return; // 未分析，不加成
        }

        // 遍历所有掉落物，按概率复制
        // 注意：跳过装备类物品（武器、工具、盔甲），只复制普通掉落物
        java.util.List<EntityItem> bonusDrops = new java.util.ArrayList<>();
        for (EntityItem drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty()) continue;

            // 跳过装备类物品（防止给僵尸武器后掉落两把）
            if (isEquipmentItem(stack)) continue;

            // 计算额外掉落数量
            float bonusChance = dropMultiplier - 1.0f;
            int bonusCount = 0;

            // 整数部分直接获得
            while (bonusChance >= 1.0f) {
                bonusCount += stack.getCount();
                bonusChance -= 1.0f;
            }

            // 小数部分概率获得
            if (bonusChance > 0 && player.world.rand.nextFloat() < bonusChance) {
                bonusCount += stack.getCount();
            }

            // 创建额外掉落物
            if (bonusCount > 0) {
                ItemStack bonusStack = stack.copy();
                bonusStack.setCount(bonusCount);
                EntityItem bonusItem = new EntityItem(
                        target.world,
                        target.posX,
                        target.posY + 0.5,
                        target.posZ,
                        bonusStack
                );
                bonusItem.setDefaultPickupDelay();
                bonusDrops.add(bonusItem);
            }
        }

        // 添加额外掉落物到事件
        event.getDrops().addAll(bonusDrops);
    }

    /**
     * 检查物品是否为装备类物品
     * 装备类物品不应该被双倍掉落复制
     */
    private static boolean isEquipmentItem(ItemStack stack) {
        net.minecraft.item.Item item = stack.getItem();

        // 武器类
        if (item instanceof net.minecraft.item.ItemSword) return true;
        if (item instanceof net.minecraft.item.ItemBow) return true;
        if (item instanceof net.minecraft.item.ItemShield) return true;

        // 工具类
        if (item instanceof net.minecraft.item.ItemTool) return true;
        if (item instanceof net.minecraft.item.ItemHoe) return true;
        if (item instanceof net.minecraft.item.ItemFishingRod) return true;
        if (item instanceof net.minecraft.item.ItemFlintAndSteel) return true;
        if (item instanceof net.minecraft.item.ItemShears) return true;

        // 盔甲类
        if (item instanceof net.minecraft.item.ItemArmor) return true;
        if (item instanceof net.minecraft.item.ItemElytra) return true;

        // 特殊物品（可能被怪物拾取）
        if (item instanceof net.minecraft.item.ItemSkull) return true;

        return false;
    }
}
