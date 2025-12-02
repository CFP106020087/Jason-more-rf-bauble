package com.moremod.event.eventHandler;

import com.moremod.item.ItemMechanicalCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 简化版护甲强化事件处理器
 * 纯粹的减伤系统，无额外效果
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ArmorEnhancementEventHandler {

    // 配置常量
    private static final float MAX_TOTAL_REDUCTION = 0.90f; // 最高总减伤90%
    private static final float BASE_REDUCTION_PER_LEVEL = 0.06f; // 每级6%基础减伤
    private static final boolean DIMINISHING_RETURNS = true; // 启用递减收益
    private static final int INVULNERABLE_DURATION = 60; // 3秒无敌时间（60 ticks）
    private static final int ENERGY_PER_DAMAGE = 1000; // 每点伤害消耗1000 RF转换为黄心
    private static final int INVULNERABLE_COOLDOWN = 1200; // 无敌冷却时间：60秒（1200 ticks）

    /**
     * 处理伤害减免
     * 使用 LOWEST 优先级，在其他所有减伤之后处理
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(player);

        if (!ItemMechanicalCore.isMechanicalCore(coreStack)) {
            return;
        }

        int level = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.ARMOR_ENHANCEMENT);

        if (level <= 0) {
            return;
        }

        // 5级：检查无敌状态
        if (level >= 5 && isInvulnerable(player, coreStack)) {
            // 完全免疫伤害
            event.setCanceled(true);

            // 消耗能量转换为黄心
            float damage = event.getAmount();
            int energyCost = (int)(damage * ENERGY_PER_DAMAGE);

            if (ItemMechanicalCore.consumeEnergy(coreStack, energyCost)) {
                // 直接添加黄心（不使用药水效果，无上限）
                float absorptionHearts = damage * 0.5f; // 每点伤害转0.5黄心
                float currentAbsorption = player.getAbsorptionAmount();
                player.setAbsorptionAmount(currentAbsorption + absorptionHearts);

                // 视觉效果
                for (int i = 0; i < 10; i++) {
                    player.world.spawnParticle(
                            net.minecraft.util.EnumParticleTypes.VILLAGER_HAPPY,
                            player.posX + (player.world.rand.nextDouble() - 0.5),
                            player.posY + player.world.rand.nextDouble() * 2,
                            player.posZ + (player.world.rand.nextDouble() - 0.5),
                            0, 0.1, 0
                    );
                }

                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GOLD + "✦ 伤害吸收 → +" + String.format("%.1f", absorptionHearts/2) + " 黄心 (消耗 " + energyCost + " RF)"
                ), true);
            } else {
                // 能量不足，无敌失效
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "能量不足！无敌失效！"
                ), true);
                coreStack.getTagCompound().setLong("InvulnerableUntil", 0);
            }

            return;
        }

        // 检测已有的减伤效果
        float existingReduction = detectExistingReduction(player, event);

        // 计算我们的减伤，考虑递减收益
        float ourReduction = calculateBalancedReduction(level, event.getSource(), existingReduction);

        // 应用减伤
        float originalDamage = event.getAmount();
        float newDamage = originalDamage * (1.0f - ourReduction);

        event.setAmount(newDamage);

        // 特殊效果触发
        handleSpecialEffects(player, level, event.getSource(), originalDamage - newDamage, originalDamage, coreStack);
    }

    /**
     * 检查是否处于无敌状态
     */
    private static boolean isInvulnerable(EntityPlayer player, ItemStack coreStack) {
        if (!coreStack.hasTagCompound()) {
            return false;
        }

        long invulnerableUntil = coreStack.getTagCompound().getLong("InvulnerableUntil");
        return player.world.getTotalWorldTime() < invulnerableUntil;
    }

    /**
     * 检测已存在的减伤效果
     */
    private static float detectExistingReduction(EntityPlayer player, LivingHurtEvent event) {
        float reduction = 0.0f;

        // 检查护甲值
        int armorValue = player.getTotalArmorValue();
        if (armorValue > 0) {
            reduction += Math.min(armorValue * 0.04f, 0.8f); // 最高80%
        }

        // 检查抗性效果
        if (player.isPotionActive(net.minecraft.init.MobEffects.RESISTANCE)) {
            int amplifier = player.getActivePotionEffect(net.minecraft.init.MobEffects.RESISTANCE).getAmplifier();
            reduction += (amplifier + 1) * 0.2f; // 每级20%
        }

        // 粗略估算保护附魔（无法精确获取）
        if (armorValue >= 10) { // 假设有附魔
            reduction += 0.2f; // 估算20%额外减伤
        }

        return Math.min(reduction, 0.95f); // 上限95%
    }

    /**
     * 计算平衡的减伤比例
     */
    private static float calculateBalancedReduction(int level, DamageSource source, float existingReduction) {
        // 基础减伤，考虑递减收益
        float baseReduction;

        if (DIMINISHING_RETURNS) {
            // 递减公式：每级收益递减
            baseReduction = 0.0f;
            for (int i = 1; i <= level; i++) {
                baseReduction += BASE_REDUCTION_PER_LEVEL / (1 + (i - 1) * 0.3f);
            }
            // Level 1: 6%
            // Level 2: 6% + 4.6% = 10.6%
            // Level 3: 10.6% + 3.5% = 14.1%
            // Level 4: 14.1% + 2.8% = 16.9%
            // Level 5: 16.9% + 2.3% = 19.2%
        } else {
            baseReduction = Math.min(level * BASE_REDUCTION_PER_LEVEL, 0.30f);
        }

        // 特殊伤害类型加成
        float typeBonus = 0.0f;

        if (level >= 2) {
            if (source.isExplosion()) {
                typeBonus += 0.05f; // 爆炸额外5%
            }
            if (source.isFireDamage()) {
                typeBonus += 0.08f; // 火焰额外8%
            }
        }

        if (level >= 3) {
            if (source.isProjectile()) {
                typeBonus += 0.05f; // 投射物额外5%
            }
            if (source.isMagicDamage()) {
                typeBonus += 0.04f; // 魔法额外4%
            }
        }

        if (level >= 4) {
            if (source == DamageSource.FALL) {
                typeBonus += 0.10f; // 摔落额外10%
            }
        }

        if (level >= 5) {
            // 满级额外加成
            typeBonus += 0.03f; // 所有伤害额外3%

            // 虚空伤害减免（保持较低）
            if (source == DamageSource.OUT_OF_WORLD) {
                return 0.10f; // 只减免10%虚空伤害
            }
        }

        // 真实伤害和创造模式伤害不减免
        if (source.isDamageAbsolute() || (source.canHarmInCreative() && source != DamageSource.OUT_OF_WORLD)) {
            return 0.0f;
        }

        // 计算最终减伤，考虑已有减伤的影响
        float totalReduction = baseReduction + typeBonus;

        // 如果已有减伤很高，降低我们的减伤效果
        if (existingReduction > 0.7f) {
            totalReduction *= (1.0f - existingReduction); // 只对剩余伤害生效
        }

        // 确保总减伤不超过设定上限
        float finalDamagePercent = (1.0f - existingReduction) * (1.0f - totalReduction);
        if (finalDamagePercent < (1.0f - MAX_TOTAL_REDUCTION)) {
            // 调整我们的减伤以确保不超过总上限
            totalReduction = 1.0f - ((1.0f - MAX_TOTAL_REDUCTION) / (1.0f - existingReduction));
        }

        return Math.max(0, Math.min(totalReduction, 0.5f)); // 单独最高50%减伤
    }

    /**
     * 处理特殊效果
     */
    private static void handleSpecialEffects(EntityPlayer player, int level, DamageSource source,
                                             float reducedDamage, float originalDamage, ItemStack coreStack) {
        // 显示减伤信息
        if (reducedDamage > 1.0f && player.world.getTotalWorldTime() % 40 == 0) {
            showDamageReductionMessage(player, reducedDamage, source);
        }

        // 3级：能量爆发
        if (level >= 3 && originalDamage >= 15.0f) {
            handleEnergyBurst(player);
        }

        // 4级：致命伤害保护
        if (level >= 4) {
            float actualDamage = originalDamage - reducedDamage;
            if (player.getHealth() - actualDamage <= 0) {
                if (handleLethalProtection(player, coreStack)) {
                    return; // 保护触发，不继续处理
                }
            }
        }

        // 5级：触发无敌时间
        if (level >= 5 && originalDamage > 0) {
            triggerInvulnerability(player, coreStack);
        }
    }

    /**
     * 3级：能量爆发效果
     */
    private static void handleEnergyBurst(EntityPlayer player) {
        // 视觉效果
        for (int i = 0; i < 15; i++) {
            double offsetX = (player.world.rand.nextDouble() - 0.5) * 3;
            double offsetY = player.world.rand.nextDouble() * 2;
            double offsetZ = (player.world.rand.nextDouble() - 0.5) * 3;

            player.world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.EXPLOSION_NORMAL,
                    player.posX + offsetX,
                    player.posY + offsetY,
                    player.posZ + offsetZ,
                    offsetX * 0.1, 0.1, offsetZ * 0.1
            );
        }

        // 音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE,
                net.minecraft.util.SoundCategory.PLAYERS, 0.5F, 1.5F);

        // 击退周围实体
        player.world.getEntitiesWithinAABBExcludingEntity(player,
                        player.getEntityBoundingBox().grow(3.0))
                .forEach(entity -> {
                    double dx = entity.posX - player.posX;
                    double dz = entity.posZ - player.posZ;
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    if (distance > 0 && distance < 3.0) {
                        double force = (3.0 - distance) / 3.0; // 距离越近力量越大
                        entity.motionX += (dx / distance) * force * 1.5;
                        entity.motionZ += (dz / distance) * force * 1.5;
                        entity.motionY += 0.3 * force;
                    }
                });

        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "✦ LV3护甲模块效果触发，弹开周围实体！"
        ));
    }

    /**
     * 4级：致命伤害保护
     */
    private static boolean handleLethalProtection(EntityPlayer player, ItemStack coreStack) {
        if (!coreStack.hasTagCompound()) {
            coreStack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        }

        long lastProtection = coreStack.getTagCompound().getLong("LastLethalProtection");
        long currentTime = player.world.getTotalWorldTime();

        // 冷却时间：10分钟（12000 ticks）
        if (currentTime - lastProtection < 12000) {
            return false;
        }

        // 触发保护 - 连续设置两次健康值以绕过First Aid模组
        float targetHealth = Math.max(4.0f, player.getMaxHealth() * 0.2f); // 至少2颗心或20%生命值
        player.setHealth(targetHealth);
        player.setHealth(targetHealth); // 第二次设置，确保First Aid模组不会拦截

        // 额外的治疗以确保恢复
        player.heal(targetHealth - player.getHealth());

        coreStack.getTagCompound().setLong("LastLethalProtection", currentTime);

        // 给予短暂无敌和再生
        player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 60, 4, true, false)); // 3秒抗性V
        player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 100, 2, true, false)); // 5秒再生III

        // 特效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.ITEM_TOTEM_USE,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 1.0F);

        // 粒子效果
        for (int i = 0; i < 30; i++) {
            player.world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.TOTEM,
                    player.posX + (player.world.rand.nextDouble() - 0.5) * 2,
                    player.posY + player.world.rand.nextDouble() * 2,
                    player.posZ + (player.world.rand.nextDouble() - 0.5) * 2,
                    0, 0.2, 0
            );
        }

        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "✦✦ " + TextFormatting.RED + "LV4护甲能力触发，致命保护已触发！" +
                        TextFormatting.GRAY + " (冷却: 10分钟)"
        ));

        return true;
    }

    /**
     * 5级：触发无敌时间
     */
    private static void triggerInvulnerability(EntityPlayer player, ItemStack coreStack) {
        if (!coreStack.hasTagCompound()) {
            coreStack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        }

        // 检查是否已经处于无敌状态
        if (isInvulnerable(player, coreStack)) {
            return;
        }

        // 检查冷却
        long lastInvulnerable = coreStack.getTagCompound().getLong("LastInvulnerableTrigger");
        long currentTime = player.world.getTotalWorldTime();

        // 冷却时间：60秒（1200 ticks）
        if (currentTime - lastInvulnerable < INVULNERABLE_COOLDOWN) {
            return;
        }

        // 设置无敌时间
        coreStack.getTagCompound().setLong("InvulnerableUntil", currentTime + INVULNERABLE_DURATION);
        coreStack.getTagCompound().setLong("LastInvulnerableTrigger", currentTime);

        // 视觉效果
        player.sendMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "✦✦✦ LV5护甲模块效果触发！3秒内免疫所有伤害！"
        ));

        // 发光效果
        player.addPotionEffect(new PotionEffect(MobEffects.GLOWING, INVULNERABLE_DURATION, 0, true, false));
    }

    /**
     * 显示减伤信息
     */
    private static void showDamageReductionMessage(EntityPlayer player, float reducedDamage, DamageSource source) {
        if (reducedDamage >= 3.0f) {
            String damageType = source.isExplosion() ? "爆炸" :
                    source.isFireDamage() ? "火焰" :
                            source.isProjectile() ? "投射" :
                                    source.isMagicDamage() ? "魔法" :
                                            source == DamageSource.FALL ? "摔落" : "物理";

            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "▼ " + String.format("%.1f", reducedDamage) + " " + damageType
            ), true);
        }
    }

    /**
     * 获取护甲强化状态描述
     */
    public static String getArmorStatus(EntityPlayer player) {
        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (!ItemMechanicalCore.isMechanicalCore(coreStack)) {
            return "未装备";
        }

        int level = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.ARMOR_ENHANCEMENT);

        if (level <= 0) {
            return "未升级";
        }

        StringBuilder status = new StringBuilder();
        status.append(TextFormatting.BLUE).append("等级 ").append(level).append("/5");
        status.append(TextFormatting.GRAY).append(" | ");

        // 显示实际减伤率
        float reduction = 0.0f;
        if (DIMINISHING_RETURNS) {
            for (int i = 1; i <= level; i++) {
                reduction += BASE_REDUCTION_PER_LEVEL / (1 + (i - 1) * 0.3f);
            }
        } else {
            reduction = level * BASE_REDUCTION_PER_LEVEL;
        }

        status.append(TextFormatting.GREEN).append("基础减伤: ").append(String.format("%.1f", reduction * 100)).append("%");

        if (level >= 3) {
            status.append(TextFormatting.GRAY).append(" | ");
            status.append(TextFormatting.DARK_PURPLE).append("能量爆发");
        }

        if (level >= 4 && coreStack.hasTagCompound()) {
            long lastProtection = coreStack.getTagCompound().getLong("LastLethalProtection");
            long currentTime = player.world.getTotalWorldTime();
            long cooldown = 12000 - (currentTime - lastProtection);

            status.append(TextFormatting.GRAY).append(" | ");
            if (cooldown > 0) {
                status.append(TextFormatting.RED).append("致命保护: ")
                        .append(cooldown / 20).append("s");
            } else {
                status.append(TextFormatting.GREEN).append("致命保护就绪");
            }
        }

        if (level >= 5 && coreStack.hasTagCompound()) {
            // 检查无敌状态
            if (isInvulnerable(player, coreStack)) {
                status.append(TextFormatting.GRAY).append(" | ");
                status.append(TextFormatting.LIGHT_PURPLE).append("【无敌中】");
            } else {
                long lastInvulnerable = coreStack.getTagCompound().getLong("LastInvulnerableTrigger");
                long currentTime = player.world.getTotalWorldTime();
                long cooldown = INVULNERABLE_COOLDOWN - (currentTime - lastInvulnerable);

                if (cooldown > 0) {
                    status.append(TextFormatting.GRAY).append(" | ");
                    status.append(TextFormatting.YELLOW).append("无敌冷却: ")
                            .append(cooldown / 20).append("s");
                }
            }
        }

        return status.toString();
    }
}