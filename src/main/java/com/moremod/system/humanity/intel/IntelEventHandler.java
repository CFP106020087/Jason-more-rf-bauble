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
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class IntelEventHandler {

    // 高人性门槛 (只有高人性玩家才能使用情报系统)
    private static final float HIGH_HUMANITY_THRESHOLD = 50.0f;

    // 样本掉落基础概率
    private static final float BASE_DROP_CHANCE = 0.15f; // 15%

    // 人性值加成（每1点超过50的人性值增加的掉落率）
    private static final float HUMANITY_DROP_BONUS = 0.005f; // 每1点 +0.5%

    /**
     * 击杀事件 - 处理样本掉落
     */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            EntityLivingBase target = event.getEntityLiving();

            if (player.world.isRemote) return;

            // 检查人性值系统是否激活
            if (!HumanitySpectrumSystem.isSystemActive(player)) return;

            // 获取人性值
            float humanity = HumanitySpectrumSystem.getHumanity(player);

            // 只有高人性玩家才能掉落样本
            if (humanity < HIGH_HUMANITY_THRESHOLD) return;

            // 计算掉落概率（研究协议加成：已分析生物掉落率提升）
            float dropChance = calculateDropChance(player, humanity, target);

            // 随机判定
            if (player.world.rand.nextFloat() < dropChance) {
                dropBiologicalSample(player, target);
            }
        }
    }

    /**
     * 计算掉落概率
     */
    private static float calculateDropChance(EntityPlayer player, float humanity, EntityLivingBase target) {
        float chance = BASE_DROP_CHANCE;

        // 人性值加成 (50-100之间每1点增加0.5%)
        float humanityBonus = (humanity - HIGH_HUMANITY_THRESHOLD) * HUMANITY_DROP_BONUS;
        chance += humanityBonus;

        // Boss和精英怪有更高掉落率
        ResourceLocation entityId = EntityList.getKey(target);
        if (entityId != null) {
            String path = entityId.toString().toLowerCase();
            if (com.moremod.system.humanity.BiologicalProfile.isBossEntity(path)) {
                chance += 0.50f; // Boss额外 +50%
            } else if (com.moremod.system.humanity.BiologicalProfile.isEliteEntity(path)) {
                chance += 0.20f; // 精英怪额外 +20%
            }

            // 研究协议加成：已分析的生物掉落率提升
            IHumanityData data = HumanityCapabilityHandler.getData(player);
            if (data != null && entityId != null) {
                BiologicalProfile profile = data.getProfile(entityId);
                if (profile != null) {
                    // 根据档案等级增加掉落率
                    switch (profile.getCurrentTier()) {
                        case BASIC:
                            chance += 0.20f; // 初级档案 +20%
                            break;
                        case COMPLETE:
                            chance += 0.40f; // 完整档案 +40%
                            break;
                        case MASTERED:
                            chance += 0.60f; // 精通档案 +60%
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        // 确保不超过100%
        return Math.min(1.0f, chance);
    }

    /**
     * 掉落生物样本
     */
    private static void dropBiologicalSample(EntityPlayer player, EntityLivingBase target) {
        ItemStack sample = ItemBiologicalSample.createSample(target);
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
     * 优先级设为NORMAL，在其他伤害修改之后应用
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

        // 获取情报数据 (只查询一次 Capability)
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return;

        // 检查人性值门槛
        float humanity = data.getHumanity();
        if (humanity < HIGH_HUMANITY_THRESHOLD) return;

        // 计算情报伤害加成 (直接使用已有的 data)
        float damageMultiplier = IntelDataHelper.calculateDamageMultiplier(data, target);

        // 应用加成
        if (damageMultiplier > 1.0f) {
            event.setAmount(event.getAmount() * damageMultiplier);
        }
    }

    /**
     * 掉落事件 - 研究协议掉落加成
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
        java.util.List<EntityItem> bonusDrops = new java.util.ArrayList<>();
        for (EntityItem drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty()) continue;

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
}
