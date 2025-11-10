package com.moremod.shields.integrated;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemCrudeEnergyBarrier;
import com.moremod.item.ItemBasicEnergyBarrier;
import com.moremod.item.ItemadvEnergyBarrier;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.api.event.FirstAidLivingDamageEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static com.moremod.eventHandler.EnergyBarrierEventHandler.getDamageTypeName;


/**
 * 完整的护盾系统集成 - Minecraft 1.12.2 版本
 * 整合FirstAid部位伤害和EnhancedVisuals视觉效果
 * 修复版：智能伤害分配，避免假死
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class IntegratedShieldSystem {

    // ===== 配置常量 =====
    // 冷却时间（毫秒）
    private static final long CRUDE_COOLDOWN = 30000L;    // 粗劣：30秒
    private static final long BASIC_COOLDOWN = 15000L;    // 基础：15秒
    private static final long ADVANCED_COOLDOWN = 5000L;  // 高级：5秒

    // 能量消耗
    private static final int CRUDE_ENERGY_COST = 500;
    private static final int BASIC_ENERGY_COST = 300;
    private static final int ADVANCED_ENERGY_COST = 100;

    // 部位保护概率
    private static final float CRUDE_HEAD_CHANCE = 0.2F;      // 粗劣：20%保护头部
    private static final float BASIC_HEAD_CHANCE = 0.4F;      // 基础：40%保护头部
    private static final float BASIC_BODY_CHANCE = 0.3F;      // 基础：30%保护身体

    // 血量阈值
    private static final float LIMB_MIN_THRESHOLD = 1.0F;     // 四肢最低血量阈值
    private static final float VITAL_MIN_HEALTH = 2.0F;       // 要害最低保留血量（避免假死）

    // NBT标签
    private static final String NBT_COOLDOWN_END = "shieldCooldownEnd";
    private static final String NBT_LAST_ACTIVATION = "lastActivation";
    private static final String NBT_TOTAL_BLOCKED = "totalBlockedDamage";
    private static final String NBT_CONSECUTIVE_BLOCKS = "consecutiveBlocks";

    // 玩家状态追踪
    private static final Map<UUID, ShieldStatus> playerShieldStatus = new HashMap<>();
    private static final Random random = new Random();

    /**
     * 护盾状态类
     */
    public static class ShieldStatus {
        public ShieldType type;
        public long cooldownEnd;
        public long lastActivation;
        public float totalBlocked;
        public int consecutiveBlocks;
        public int limbWarningCount;  // 添加四肢警告计数

        public ShieldStatus(ShieldType type) {
            this.type = type;
            this.cooldownEnd = 0L;
            this.lastActivation = 0L;
            this.totalBlocked = 0.0F;
            this.consecutiveBlocks = 0;
            this.limbWarningCount = 0;
        }

        public boolean isOnCooldown() {
            return System.currentTimeMillis() < cooldownEnd;
        }
    }

    /**
     * 获取玩家的护盾状态（用于Tooltip显示）
     */
    public static ShieldStatus getPlayerShieldStatus(UUID playerUUID) {
        return playerShieldStatus.get(playerUUID);
    }

    public enum ShieldType {
        NONE(0),
        CRUDE(1),    // 粗劣
        BASIC(2),    // 基础
        ADVANCED(3); // 高级

        public final int level;
        ShieldType(int level) {
            this.level = level;
        }
    }

    // ===== 主要事件处理 =====

    /**
     * 处理普通伤害事件（无FirstAid时的主要处理）
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.isCanceled()) return;
        if (event.getSource() == DamageSource.OUT_OF_WORLD) return; // 虚空伤害无法格挡

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        ItemStack shield = findEquippedShield(player);

        if (shield.isEmpty()) return;

        ShieldType type = getShieldType(shield);
        if (type == ShieldType.NONE) return;

        ShieldStatus status = playerShieldStatus.computeIfAbsent(
                player.getUniqueID(),
                k -> new ShieldStatus(type)
        );

        // 检查冷却
        if (!status.isOnCooldown()) {
            // 激活时免疫任意伤害（除了虚空）
            if (tryActivateShield(player, shield, type, event.getAmount())) {
                event.setCanceled(true);
                updateShieldStatus(player, shield, type, event.getAmount());

                // 创建护盾效果
                if (!player.world.isRemote) {
                    createShieldEffect(player, type, event.getAmount());

                    // 显示格挡的伤害类型
                    String damageType = getDamageTypeName(event.getSource());
                    player.sendStatusMessage(new TextComponentString(
                            getShieldColor(type) + "[" + getShieldName(type) + "] " +
                                    TextFormatting.GREEN + "免疫 " + damageType + " 伤害！"
                    ), true);
                }
            }
        }
        // 冷却期间不提供主动防护，只有FirstAid被动效果
    }

    /**
     * FirstAid兼容 - 处理部位伤害
     */
    @Optional.Method(modid = "firstaid")
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFirstAidDamage(FirstAidLivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.isCanceled()) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        ItemStack shield = findEquippedShield(player);

        if (shield.isEmpty()) return;

        ShieldType type = getShieldType(shield);
        if (type == ShieldType.NONE) return;

        ShieldStatus status = playerShieldStatus.computeIfAbsent(
                player.getUniqueID(),
                k -> new ShieldStatus(type)
        );

        // 护盾激活状态 - 完全免疫
        if (!status.isOnCooldown()) {
            // 检查能量并激活
            if (tryActivateShieldFirstAid(player, shield, type, event)) {
                event.setCanceled(true);
                updateShieldStatus(player, shield, type, event.getUndistributedDamage());

                // 创建护盾效果
                if (!player.world.isRemote) {
                    createShieldEffect(player, type, event.getUndistributedDamage());
                }
                return;
            }
        }

        // 冷却期间的被动部位保护
        handleCooldownPartProtection(event, player, type, status);
    }

    /**
     * FirstAid专用的护盾激活
     */
    @Optional.Method(modid = "firstaid")
    private static boolean tryActivateShieldFirstAid(
            EntityPlayer player,
            ItemStack shield,
            ShieldType type,
            FirstAidLivingDamageEvent event
    ) {
        AbstractPlayerDamageModel beforeDamage = event.getBeforeDamage();

        // 检查是否为紧急情况
        boolean isEmergency = false;
        if (type == ShieldType.CRUDE) {
            // 粗劣护盾：低血量或致命伤害时自动触发
            if (beforeDamage.HEAD.currentHealth < 4.0F ||
                    beforeDamage.BODY.currentHealth < 4.0F ||
                    player.getHealth() < player.getMaxHealth() * 0.3F) {
                isEmergency = true;
            }
        }

        // 获取能量
        int energyCost = getEnergyCost(type);
        int currentEnergy = getShieldEnergy(shield, type);

        // 检查能量（紧急情况下粗劣护盾可以无能量触发）
        if (!isEmergency && currentEnergy < energyCost) {
            if (!player.world.isRemote) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "[护盾] 能量不足！需要 " + energyCost + " RF"
                ), true);
            }
            return false;
        }

        // 消耗能量
        if (!isEmergency) {
            consumeEnergy(shield, type, energyCost);
        } else if (currentEnergy >= energyCost / 2) {
            // 紧急触发消耗一半能量
            consumeEnergy(shield, type, energyCost / 2);
        }

        return true;
    }

    /**
     * 冷却期间的部位伤害保护
     */
    @Optional.Method(modid = "firstaid")
    private static void handleCooldownPartProtection(
            FirstAidLivingDamageEvent event,
            EntityPlayer player,
            ShieldType type,
            ShieldStatus status
    ) {
        AbstractPlayerDamageModel beforeDamage = event.getBeforeDamage();
        AbstractPlayerDamageModel afterDamage = event.getAfterDamage();
        float undistributedDamage = event.getUndistributedDamage();

        boolean shouldModify = false;
        String protectionMessage = "";
        TextFormatting messageColor = TextFormatting.WHITE;

        switch (type) {
            case CRUDE:
                // 粗劣：20%概率规避头部致命伤害
                if (afterDamage.HEAD.currentHealth <= 0 && beforeDamage.HEAD.currentHealth > 0) {
                    if (random.nextFloat() < CRUDE_HEAD_CHANCE) {
                        redistributeHeadDamage(afterDamage, beforeDamage, undistributedDamage);
                        protectionMessage = "头部防护触发！";
                        messageColor = TextFormatting.GRAY;
                        shouldModify = true;
                    }
                }
                break;

            case BASIC:
                // 基础：40%概率规避头部，30%概率规避身体致命伤害
                boolean headProtected = false;
                boolean bodyProtected = false;

                if (afterDamage.HEAD.currentHealth <= 0 && beforeDamage.HEAD.currentHealth > 0) {
                    if (random.nextFloat() < BASIC_HEAD_CHANCE) {
                        redistributeHeadDamage(afterDamage, beforeDamage, undistributedDamage);
                        headProtected = true;
                        protectionMessage = "头部防护触发！";
                    }
                }

                if (!headProtected && afterDamage.BODY.currentHealth <= 0 && beforeDamage.BODY.currentHealth > 0) {
                    if (random.nextFloat() < BASIC_BODY_CHANCE) {
                        redistributeBodyDamage(afterDamage, beforeDamage, undistributedDamage);
                        bodyProtected = true;
                        protectionMessage = "躯干防护触发！";
                    }
                }

                if (headProtected || bodyProtected) {
                    messageColor = TextFormatting.AQUA;
                    shouldModify = true;
                }
                break;

            case ADVANCED:
                // 高级：高概率无视要害伤害
                prioritizeLimbDamage(afterDamage, beforeDamage, undistributedDamage, player, status);

                // 检查是否触发了保护
                float headReduction = beforeDamage.HEAD.currentHealth - afterDamage.HEAD.currentHealth;
                float bodyReduction = beforeDamage.BODY.currentHealth - afterDamage.BODY.currentHealth;

                if (headReduction > 0.1F || bodyReduction > 0.1F) {
                    protectionMessage = "要害防护激活！伤害大幅减免";
                    messageColor = TextFormatting.GOLD;
                }
                break;
        }

        // 显示保护效果
        if (!protectionMessage.isEmpty() && !player.world.isRemote) {
            showProtectionEffect(player, protectionMessage, messageColor);
        }

        // 确保要害部位不会进入假死状态（血量为0但不死）
        if (afterDamage.HEAD.currentHealth > 0 && afterDamage.HEAD.currentHealth < VITAL_MIN_HEALTH) {
            afterDamage.HEAD.currentHealth = VITAL_MIN_HEALTH;
        }
        if (afterDamage.BODY.currentHealth > 0 && afterDamage.BODY.currentHealth < VITAL_MIN_HEALTH) {
            afterDamage.BODY.currentHealth = VITAL_MIN_HEALTH;
        }
    }

    /**
     * 重定向头部伤害到其他部位
     */
    @Optional.Method(modid = "firstaid")
    private static void redistributeHeadDamage(
            AbstractPlayerDamageModel afterDamage,
            AbstractPlayerDamageModel beforeDamage,
            float totalDamage
    ) {
        // 计算头部实际受到的伤害
        float headDamage = beforeDamage.HEAD.currentHealth - afterDamage.HEAD.currentHealth;

        // 恢复头部血量的70%
        afterDamage.HEAD.currentHealth = beforeDamage.HEAD.currentHealth - (headDamage * 0.3F);

        // 确保不低于最小值
        if (afterDamage.HEAD.currentHealth < VITAL_MIN_HEALTH) {
            afterDamage.HEAD.currentHealth = VITAL_MIN_HEALTH;
        }

        // 将70%的伤害分配到其他部位
        float redistributed = headDamage * 0.7F;

        // 优先分配到身体（40%）
        float bodyDamage = redistributed * 0.4F;
        afterDamage.BODY.currentHealth = Math.max(0, afterDamage.BODY.currentHealth - bodyDamage);

        // 剩余平均分配到四肢（各15%）
        float limbDamage = redistributed * 0.15F;
        afterDamage.LEFT_ARM.currentHealth = Math.max(0, afterDamage.LEFT_ARM.currentHealth - limbDamage);
        afterDamage.RIGHT_ARM.currentHealth = Math.max(0, afterDamage.RIGHT_ARM.currentHealth - limbDamage);
        afterDamage.LEFT_LEG.currentHealth = Math.max(0, afterDamage.LEFT_LEG.currentHealth - limbDamage);
        afterDamage.RIGHT_LEG.currentHealth = Math.max(0, afterDamage.RIGHT_LEG.currentHealth - limbDamage);
    }

    /**
     * 重定向身体伤害到四肢
     */
    @Optional.Method(modid = "firstaid")
    private static void redistributeBodyDamage(
            AbstractPlayerDamageModel afterDamage,
            AbstractPlayerDamageModel beforeDamage,
            float totalDamage
    ) {
        // 计算身体实际受到的伤害
        float bodyDamage = beforeDamage.BODY.currentHealth - afterDamage.BODY.currentHealth;

        // 恢复身体血量的60%
        afterDamage.BODY.currentHealth = beforeDamage.BODY.currentHealth - (bodyDamage * 0.4F);

        // 确保不低于最小值
        if (afterDamage.BODY.currentHealth < VITAL_MIN_HEALTH) {
            afterDamage.BODY.currentHealth = VITAL_MIN_HEALTH;
        }

        // 将60%的伤害分配到四肢
        float redistributed = bodyDamage * 0.6F;
        float limbDamage = redistributed * 0.25F;

        afterDamage.LEFT_ARM.currentHealth = Math.max(0, afterDamage.LEFT_ARM.currentHealth - limbDamage);
        afterDamage.RIGHT_ARM.currentHealth = Math.max(0, afterDamage.RIGHT_ARM.currentHealth - limbDamage);
        afterDamage.LEFT_LEG.currentHealth = Math.max(0, afterDamage.LEFT_LEG.currentHealth - limbDamage);
        afterDamage.RIGHT_LEG.currentHealth = Math.max(0, afterDamage.RIGHT_LEG.currentHealth - limbDamage);
    }

    /**
     * 高概率无视要害伤害 - 高级护盾（简化版）
     * 直接减免头部和身体的伤害，避免复杂的转移导致假死
     */
    @Optional.Method(modid = "firstaid")
    private static void prioritizeLimbDamage(
            AbstractPlayerDamageModel afterDamage,
            AbstractPlayerDamageModel beforeDamage,
            float totalDamage,
            EntityPlayer player,
            ShieldStatus status
    ) {
        // 定义无视概率
        final float HEAD_IGNORE_CHANCE = 0.9F;   // 90%无视头部伤害
        final float BODY_IGNORE_CHANCE = 0.8F;   // 80%无视身体伤害
        final float DAMAGE_REDUCTION = 0.1F;     // 剩余10-20%的伤害

        // 保存原始血量
        float originalHeadHealth = beforeDamage.HEAD.currentHealth;
        float originalBodyHealth = beforeDamage.BODY.currentHealth;

        // 计算各部位受到的伤害
        float headDamage = Math.max(0, originalHeadHealth - afterDamage.HEAD.currentHealth);
        float bodyDamage = Math.max(0, originalBodyHealth - afterDamage.BODY.currentHealth);

        boolean headProtected = false;
        boolean bodyProtected = false;
        String protectionMessage = "";

        // 处理头部伤害
        if (headDamage > 0) {
            if (random.nextFloat() < HEAD_IGNORE_CHANCE) {
                // 90%概率大幅减免头部伤害
                afterDamage.HEAD.currentHealth = originalHeadHealth - (headDamage * DAMAGE_REDUCTION);
                headProtected = true;

                // 确保不会因为浮点误差进入假死
                if (afterDamage.HEAD.currentHealth > 0 && afterDamage.HEAD.currentHealth < VITAL_MIN_HEALTH) {
                    afterDamage.HEAD.currentHealth = VITAL_MIN_HEALTH;
                }
            }
        }

        // 处理身体伤害
        if (bodyDamage > 0) {
            if (random.nextFloat() < BODY_IGNORE_CHANCE) {
                // 80%概率大幅减免身体伤害
                afterDamage.BODY.currentHealth = originalBodyHealth - (bodyDamage * DAMAGE_REDUCTION);
                bodyProtected = true;

                // 确保不会因为浮点误差进入假死
                if (afterDamage.BODY.currentHealth > 0 && afterDamage.BODY.currentHealth < VITAL_MIN_HEALTH) {
                    afterDamage.BODY.currentHealth = VITAL_MIN_HEALTH;
                }
            }
        }

        // 构建保护消息
        if (headProtected && bodyProtected) {
            protectionMessage = "要害完全保护！伤害减免90%";
        } else if (headProtected) {
            protectionMessage = "头部保护激活！伤害减免90%";
        } else if (bodyProtected) {
            protectionMessage = "躯干保护激活！伤害减免90%";
        }

        // 如果有保护触发，显示效果
        if (!protectionMessage.isEmpty() && !player.world.isRemote) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GOLD + "[高级护盾] " + protectionMessage
            ), true);

            // 特殊情况：如果原本会致命但被保护
            if ((headDamage > originalHeadHealth * 0.8F && headProtected) ||
                    (bodyDamage > originalBodyHealth * 0.8F && bodyProtected)) {

                // 给予少量黄心作为奖励
                float absorption = 2.0F;
                player.setAbsorptionAmount(player.getAbsorptionAmount() + absorption);
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.YELLOW + "致命伤害吸收 +" + absorption + " 黄心"
                ), true);
            }
        }

        // 对四肢的伤害保持正常（不处理）
        // 这样四肢仍会正常受伤，但要害得到强力保护
    }

    /**
     * 检查是否发生了伤害转移
     */
    @Optional.Method(modid = "firstaid")
    private static boolean checkDamageTransfer(
            AbstractPlayerDamageModel after,
            AbstractPlayerDamageModel before
    ) {
        // 比较要害部位的伤害是否被减少
        float headDamageBefore = Math.max(0, before.HEAD.currentHealth - after.HEAD.currentHealth);
        float bodyDamageBefore = Math.max(0, before.BODY.currentHealth - after.BODY.currentHealth);

        // 如果要害伤害被减少，说明发生了转移
        return headDamageBefore > 0.1F || bodyDamageBefore > 0.1F;
    }

    // ===== 护盾激活逻辑 =====

    private static boolean tryActivateShield(
            EntityPlayer player,
            ItemStack shield,
            ShieldType type,
            float damage
    ) {
        // 检查低血量自动触发（粗劣护盾特性）
        boolean isEmergency = false;
        if (type == ShieldType.CRUDE && player.getHealth() < player.getMaxHealth() * 0.3F) {
            isEmergency = true;
        }

        // 获取能量
        int energyCost = getEnergyCost(type);
        int currentEnergy = getShieldEnergy(shield, type);

        // 检查能量
        if (!isEmergency && currentEnergy < energyCost) {
            if (!player.world.isRemote) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "[护盾] 能量不足！需要 " + energyCost + " RF"
                ), true);
            }
            return false;
        }

        // 消耗能量
        if (isEmergency && currentEnergy >= energyCost / 2) {
            consumeEnergy(shield, type, energyCost / 2);
        } else if (currentEnergy >= energyCost) {
            consumeEnergy(shield, type, energyCost);
        } else if (isEmergency) {
            // 紧急情况且能量不足一半，消耗所有剩余能量
            consumeEnergy(shield, type, currentEnergy);
        }

        return true;
    }

    private static void updateShieldStatus(
            EntityPlayer player,
            ItemStack shield,
            ShieldType type,
            float blockedDamage
    ) {
        ShieldStatus status = playerShieldStatus.get(player.getUniqueID());
        if (status == null) {
            status = new ShieldStatus(type);
            playerShieldStatus.put(player.getUniqueID(), status);
        }

        long currentTime = System.currentTimeMillis();

        // 检查连续格挡（高级护盾特性）
        if (type == ShieldType.ADVANCED &&
                currentTime - status.lastActivation < 3000L) {
            status.consecutiveBlocks++;
        } else {
            status.consecutiveBlocks = 0;
        }

        status.lastActivation = currentTime;
        status.cooldownEnd = currentTime + calculateCooldown(type, status.consecutiveBlocks);
        status.totalBlocked += blockedDamage;
        status.type = type;
        status.limbWarningCount = 0; // 重置警告

        // 更新NBT
        if (!shield.hasTagCompound()) {
            shield.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = shield.getTagCompound();
        nbt.setLong(NBT_COOLDOWN_END, status.cooldownEnd);
        nbt.setFloat(NBT_TOTAL_BLOCKED, status.totalBlocked);
        nbt.setInteger(NBT_CONSECUTIVE_BLOCKS, status.consecutiveBlocks);
        nbt.setLong(NBT_LAST_ACTIVATION, status.lastActivation);

        // 显示格挡信息
        if (!player.world.isRemote) {
            showBlockMessage(player, type, blockedDamage, status.cooldownEnd - currentTime);
        }
    }

    private static long calculateCooldown(ShieldType type, int consecutiveBlocks) {
        long baseCooldown = getCooldownTime(type);

        // 高级护盾连续格挡减少冷却
        if (type == ShieldType.ADVANCED && consecutiveBlocks > 0) {
            long reduction = Math.min(consecutiveBlocks * 1000L, baseCooldown / 2); // 最多减一半
            return Math.max(1000L, baseCooldown - reduction); // 最少1秒
        }

        return baseCooldown;
    }

    // ===== 视觉效果 =====

    private static void createShieldEffect(EntityPlayer player, ShieldType type, float damage) {
        switch (type) {
            case CRUDE:
                createCrudeShieldEffect(player, damage);
                break;
            case BASIC:
                createBasicShieldEffect(player, damage);
                break;
            case ADVANCED:
                createAdvancedShieldEffect(player, damage);
                break;
            default:
                break;
        }
    }

    private static void createCrudeShieldEffect(EntityPlayer player, float damage) {
        // 红色爆炸波效果
        for (int i = 0; i < 30; i++) {
            double angle = (Math.PI * 2) * i / 30;
            double x = Math.cos(angle) * 2;
            double z = Math.sin(angle) * 2;

            player.world.spawnParticle(
                    EnumParticleTypes.REDSTONE,
                    player.posX + x, player.posY + 1, player.posZ + z,
                    1.0, 0.0, 0.0 // RGB红色
            );
        }

        // 击退周围实体
        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                player.getEntityBoundingBox().grow(3.0D),
                e -> e != player && e.isEntityAlive()
        );

        for (EntityLivingBase entity : entities) {
            double dx = entity.posX - player.posX;
            double dz = entity.posZ - player.posZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0) {
                entity.knockBack(player, 1.5F, -dx/distance, -dz/distance);
                entity.attackEntityFrom(DamageSource.causePlayerDamage(player), damage * 0.5F);
            }
        }

        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 0.5F, 1.2F);
    }

    private static void createBasicShieldEffect(EntityPlayer player, float damage) {
        // 蓝色护盾泡泡
        for (int i = 0; i < 40; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 1.5;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = random.nextDouble() * 2;

            player.world.spawnParticle(
                    EnumParticleTypes.WATER_BUBBLE,
                    player.posX + x, player.posY + y, player.posZ + z,
                    0, 0.05, 0
            );
        }

        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ITEM_SHIELD_BLOCK,
                SoundCategory.PLAYERS, 0.7F, 1.0F);
    }

    private static void createAdvancedShieldEffect(EntityPlayer player, float damage) {
        // 金色吸收效果
        for (int i = 0; i < 50; i++) {
            double angle = (Math.PI * 2) * i / 25;
            double radius = 0.5 + (i % 2) * 0.5;
            double y = (i / 25.0) * 2;
            double x = Math.cos(angle + y) * radius;
            double z = Math.sin(angle + y) * radius;

            player.world.spawnParticle(
                    EnumParticleTypes.END_ROD,
                    player.posX + x, player.posY + y, player.posZ + z,
                    0, 0.02, 0
            );
        }

        // 转换少量伤害为黄心
        float absorption = Math.min(damage * 0.1F, 2.0F); // 最多2颗黄心
        float currentAbsorption = player.getAbsorptionAmount();
        float newAbsorption = Math.min(20.0F, currentAbsorption + absorption);
        player.setAbsorptionAmount(newAbsorption);

        player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS, 0.5F, 1.5F);

        if (absorption > 0.1F) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GOLD + "+" + String.format("%.1f", absorption) + " 黄心"
            ), true);
        }
    }

    private static void showProtectionEffect(EntityPlayer player, String message, TextFormatting color) {
        if (!player.world.isRemote) {
            player.sendStatusMessage(new TextComponentString(
                    color + "[被动防护] " + message
            ), true);

            // 小型粒子效果
            for (int i = 0; i < 5; i++) {
                player.world.spawnParticle(
                        EnumParticleTypes.VILLAGER_HAPPY,
                        player.posX + (random.nextDouble() - 0.5),
                        player.posY + 1 + (random.nextDouble() * 0.5),
                        player.posZ + (random.nextDouble() - 0.5),
                        0, 0.1, 0
                );
            }
        }
    }

    private static void showBlockMessage(
            EntityPlayer player,
            ShieldType type,
            float damage,
            long cooldown
    ) {
        String shieldName = getShieldName(type);
        int cooldownSec = (int)(cooldown / 1000L);

        player.sendStatusMessage(new TextComponentString(
                getShieldColor(type) + "[" + shieldName + "] " +
                        TextFormatting.GREEN + "格挡 " + String.format("%.1f", damage) + " 伤害 " +
                        TextFormatting.YELLOW + "(冷却: " + cooldownSec + "秒)"
        ), true);
    }

    // ===== 工具方法 =====

    private static ItemStack findEquippedShield(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return ItemStack.EMPTY;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof ItemadvEnergyBarrier ||
                        stack.getItem() instanceof ItemBasicEnergyBarrier ||
                        stack.getItem() instanceof ItemCrudeEnergyBarrier) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static ShieldType getShieldType(ItemStack stack) {
        if (stack.getItem() instanceof ItemadvEnergyBarrier) return ShieldType.ADVANCED;
        if (stack.getItem() instanceof ItemBasicEnergyBarrier) return ShieldType.BASIC;
        if (stack.getItem() instanceof ItemCrudeEnergyBarrier) return ShieldType.CRUDE;
        return ShieldType.NONE;
    }

    @SideOnly(Side.CLIENT)
    public static ShieldType getEquippedShieldTypeClient(EntityPlayer player) {
        ItemStack shield = findEquippedShield(player);
        return !shield.isEmpty() ? getShieldType(shield) : ShieldType.NONE;
    }

    private static long getCooldownTime(ShieldType type) {
        switch (type) {
            case CRUDE: return CRUDE_COOLDOWN;
            case BASIC: return BASIC_COOLDOWN;
            case ADVANCED: return ADVANCED_COOLDOWN;
            default: return 0L;
        }
    }

    private static int getEnergyCost(ShieldType type) {
        switch (type) {
            case CRUDE: return CRUDE_ENERGY_COST;
            case BASIC: return BASIC_ENERGY_COST;
            case ADVANCED: return ADVANCED_ENERGY_COST;
            default: return 0;
        }
    }

    private static int getShieldEnergy(ItemStack stack, ShieldType type) {
        switch (type) {
            case CRUDE: return ItemCrudeEnergyBarrier.getEnergyStored(stack);
            case BASIC: return ItemBasicEnergyBarrier.getEnergyStored(stack);
            case ADVANCED: return ItemadvEnergyBarrier.getEnergyStored(stack);
            default: return 0;
        }
    }

    private static void consumeEnergy(ItemStack stack, ShieldType type, int amount) {
        int current = getShieldEnergy(stack, type);
        int newEnergy = Math.max(0, current - amount);
        switch (type) {
            case CRUDE:
                ItemCrudeEnergyBarrier.setEnergyStored(stack, newEnergy);
                break;
            case BASIC:
                ItemBasicEnergyBarrier.setEnergyStored(stack, newEnergy);
                break;
            case ADVANCED:
                ItemadvEnergyBarrier.setEnergyStored(stack, newEnergy);
                break;
            default:
                break;
        }
    }

    private static String getShieldName(ShieldType type) {
        switch (type) {
            case CRUDE: return "粗劣护盾";
            case BASIC: return "基础护盾";
            case ADVANCED: return "高级护盾";
            default: return "未知护盾";
        }
    }

    private static TextFormatting getShieldColor(ShieldType type) {
        switch (type) {
            case CRUDE: return TextFormatting.GRAY;
            case BASIC: return TextFormatting.AQUA;
            case ADVANCED: return TextFormatting.GOLD;
            default: return TextFormatting.WHITE;
        }
    }

    // ===== Tick事件 - 显示冷却状态 =====

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 每秒显示一次冷却状态
        if (player.world.getTotalWorldTime() % 20L == 0L) {
            ShieldStatus status = playerShieldStatus.get(player.getUniqueID());
            if (status != null && status.isOnCooldown()) {
                long remaining = status.cooldownEnd - System.currentTimeMillis();
                if (remaining > 0L && remaining < 5000L) {
                    // 最后5秒提示
                    int seconds = (int) Math.ceil(remaining / 1000.0);
                    player.sendStatusMessage(new TextComponentString(
                            getShieldColor(status.type) + "[" + getShieldName(status.type) +
                                    "] 冷却: " + seconds + "秒"
                    ), true);
                }
            }
        }
    }

    // ===== EnhancedVisuals 视觉效果免疫 =====

    /**
     * 爆炸视觉效果免疫
     * 护盾激活或冷却期间免疫爆炸视觉效果
     */
    @Optional.Method(modid = "enhancedvisuals")
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onVisualExplosion(team.creative.enhancedvisuals.api.event.VisualExplosionEvent event) {
        if (event.isCanceled()) return;

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        // 检查是否装备了护盾
        ItemStack shield = findEquippedShield(player);
        if (shield.isEmpty()) return;

        ShieldType type = getShieldType(shield);
        if (type == ShieldType.NONE) return;

        ShieldStatus status = playerShieldStatus.get(player.getUniqueID());

        // 护盾激活状态或冷却期间都免疫
        if (status != null) {
            // 不同等级护盾的免疫效果
            switch (type) {
                case CRUDE:
                    // 粗劣护盾：冷却期间30%概率免疫
                    if (!status.isOnCooldown() || random.nextFloat() < 0.3F) {
                        event.setCanceled(true);
                    }
                    break;

                case BASIC:
                    // 基础护盾：冷却期间60%概率免疫
                    if (!status.isOnCooldown() || random.nextFloat() < 0.6F) {
                        event.setCanceled(true);
                    }
                    break;

                case ADVANCED:
                    // 高级护盾：始终免疫
                    event.setCanceled(true);
                    break;
            }
        } else if (getShieldEnergy(shield, type) > 0) {
            // 有能量就提供基础免疫
            event.setCanceled(true);
        }
    }

    /**
     * 血液飞溅效果免疫
     * 护盾激活时免疫血液效果
     */
    @Optional.Method(modid = "enhancedvisuals")
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onSplashEvent(team.creative.enhancedvisuals.api.event.SplashEvent event) {
        if (event.isCanceled()) return;

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        ItemStack shield = findEquippedShield(player);
        if (shield.isEmpty()) return;

        ShieldType type = getShieldType(shield);
        if (type == ShieldType.NONE) return;

        ShieldStatus status = playerShieldStatus.get(player.getUniqueID());

        // 护盾未在冷却时免疫血液效果
        if (status == null || !status.isOnCooldown()) {
            if (getShieldEnergy(shield, type) > 0) {
                event.setCanceled(true);
            }
        } else {
            // 冷却期间根据护盾等级提供部分免疫
            switch (type) {
                case CRUDE:
                    // 不免疫
                    break;
                case BASIC:
                    // 50%免疫
                    if (random.nextFloat() < 0.5F) {
                        event.setCanceled(true);
                    }
                    break;
                case ADVANCED:
                    // 80%免疫
                    if (random.nextFloat() < 0.8F) {
                        event.setCanceled(true);
                    }
                    break;
            }
        }
    }

    /**
     * 火焰粒子效果免疫
     * 护盾提供火焰视觉保护
     */
    @Optional.Method(modid = "enhancedvisuals")
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onFireParticles(team.creative.enhancedvisuals.api.event.FireParticlesEvent event) {
        if (event.isCanceled()) return;

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        ItemStack shield = findEquippedShield(player);
        if (shield.isEmpty()) return;

        ShieldType type = getShieldType(shield);

        // 仅高级护盾免疫火焰视觉效果
        if (type == ShieldType.ADVANCED) {
            ShieldStatus status = playerShieldStatus.get(player.getUniqueID());
            if (status == null || !status.isOnCooldown()) {
                if (getShieldEnergy(shield, type) > 0) {
                    event.setCanceled(true);

                    // 显示护盾吸收火焰效果
                    if (!player.world.isRemote && player.world.getTotalWorldTime() % 40L == 0L) {
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.GOLD + "[高级护盾] 火焰防护激活"
                        ), true);
                    }
                }
            }
        }
    }

    /**
     * 低生命值心跳效果免疫
     * 护盾激活时减轻低血量视觉效果


    /**
     * 检查客户端是否需要导入EnhancedVisuals类
     * 用于避免类加载错误
     */
    @SideOnly(Side.CLIENT)
    private static void importEnhancedVisualsClient() {
        try {
            Class.forName("team.creative.enhancedvisuals.api.event.VisualExplosionEvent");
            Class.forName("team.creative.enhancedvisuals.api.event.SplashEvent");
            Class.forName("team.creative.enhancedvisuals.api.event.FireParticlesEvent");
            // 如果存在，注册一个标记
            enhancedVisualsLoaded = true;
        } catch (ClassNotFoundException e) {
            enhancedVisualsLoaded = false;
        }
    }

    private static boolean enhancedVisualsLoaded = false;
}