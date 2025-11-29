package com.moremod.system.humanity;

import com.moremod.config.HumanityConfig;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.moremod;
import com.moremod.system.FleshRejectionSystem;
import com.moremod.system.ascension.BrokenGodHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.api.event.FirstAidLivingDamageEvent;

import net.minecraft.util.DamageSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 人性值事件处理器
 * Humanity Event Handler
 *
 * 处理所有与人性值系统相关的游戏事件
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class HumanityEventHandler {

    // ========== 防止AoE递归的机制 ==========

    /** 自定义AoE伤害源 - 用于识别并跳过我们自己造成的伤害 */
    public static final DamageSource AOE_DAMAGE = new DamageSource("moremod_aoe").setDamageBypassesArmor();
    public static final DamageSource DISTORTION_DAMAGE = new DamageSource("moremod_distortion").setDamageBypassesArmor();

    /** 正在处理AoE伤害的玩家 - 防止递归 */
    private static final Set<UUID> processingAoE = new HashSet<>();

    // ========== 玩家Tick事件 ==========

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 检查是否需要激活人性值系统
        checkSystemActivation(player);

        // 更新人性值
        HumanitySpectrumSystem.updateHumanity(player);

        // 破碎之神：每tick强制锁定最大血量为10（防止被敌人减少）
        // 其他玩家：每秒更新一次MaxHP修改器
        if (BrokenGodHandler.isBrokenGod(player)) {
            HumanityEffectsManager.updateMaxHP(player);
        } else if (player.ticksExisted % 20 == 0) {
            HumanityEffectsManager.updateMaxHP(player);
        }

        // 应用异常场效果
        HumanitySpectrumSystem.applyAnomalyFieldEffect(player);

        // 同步系统
        if (player instanceof EntityPlayerMP) {
            HumanitySpectrumSystem.tickSyncSystem((EntityPlayerMP) player);
            FleshRejectionSystem.tickSyncSystem((EntityPlayerMP) player);
        }
    }

    /**
     * 检查是否需要激活人性值系统
     * 当排异系统突破时自动激活
     *
     * 前提条件：玩家必须装备机械核心
     */
    private static void checkSystemActivation(EntityPlayer player) {
        if (!HumanityConfig.enableHumanitySystem) return;

        // 必须装备机械核心
        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return;

        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || data.isSystemActive()) return;

        // 检查排异系统是否已突破
        if (FleshRejectionSystem.hasTranscended(player)) {
            HumanitySpectrumSystem.activateSystem(player);
        }
    }

    // ========== 伤害事件处理 ==========

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 跳过我们自己造成的AoE/畸变/反噬伤害，防止递归
        String damageType = event.getSource().getDamageType();
        if (damageType != null && damageType.startsWith("moremod_")) {
            return;
        }

        // 玩家造成伤害
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            handlePlayerAttack(event);
        }

        // 玩家受到伤害
        if (event.getEntityLiving() instanceof EntityPlayer) {
            handlePlayerDamaged(event);
        }
    }

    /**
     * 处理玩家攻击
     */
    private static void handlePlayerAttack(LivingHurtEvent event) {
        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLivingBase target = event.getEntityLiving();
        UUID playerId = player.getUniqueID();

        // 防止递归：如果这个玩家正在处理AoE，跳过
        if (processingAoE.contains(playerId)) {
            return;
        }

        if (!HumanitySpectrumSystem.isSystemActive(player)) return;

        // 破碎之神有独立的战斗系统，跳过人性值战斗效果
        if (BrokenGodHandler.isBrokenGod(player)) return;

        // 标记战斗状态
        HumanitySpectrumSystem.markCombat(player);

        float humanity = HumanitySpectrumSystem.getHumanity(player);
        float damageMultiplier = 1.0f;

        // 高人性：猎人协议
        if (humanity >= 40f) {
            damageMultiplier *= HumanitySpectrumSystem.calculateHunterProtocolDamageMultiplier(player, target);

            // 检查暴击
            if (HumanitySpectrumSystem.checkHunterProtocolCrit(player, target)) {
                damageMultiplier += 0.5f;
                // 暴击反馈
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        net.minecraft.init.SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                        net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 1.2f);
            }
        }

        // 低人性：异常协议
        if (humanity < 50f) {
            damageMultiplier *= HumanitySpectrumSystem.calculateAnomalyProtocolDamageMultiplier(player);

            // 检查畸变脉冲 - 使用AoE保护
            if (HumanitySpectrumSystem.checkDistortionPulse(player)) {
                processingAoE.add(playerId);
                try {
                    HumanitySpectrumSystem.triggerDistortionPulse(player);
                } finally {
                    processingAoE.remove(playerId);
                }
            }
        }

        // 崩解状态：攻击力翻倍（破碎之神不会进入崩解状态）
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        boolean isBrokenGod = BrokenGodHandler.isBrokenGod(player);
        if (data != null && data.isDissolutionActive() && !isBrokenGod) {
            damageMultiplier *= 2.0f;

            // 反噬 - 使用自定义伤害源避免触发其他事件
            float backlash = player.getMaxHealth() * 0.05f;
            player.attackEntityFrom(new DamageSource("moremod_backlash").setDamageBypassesArmor(), backlash);
        }

        event.setAmount(event.getAmount() * damageMultiplier);

        // 极低人性惩罚：攻击波及周围生物（破碎之神不受此惩罚）
        if (humanity < 10f && HumanityConfig.extremeLowHumanityAoEDamage && !isBrokenGod) {
            // 使用AoE保护防止递归
            if (!processingAoE.contains(playerId)) {
                processingAoE.add(playerId);
                try {
                    spreadDamageToNearby(player, target, event.getAmount());
                } finally {
                    processingAoE.remove(playerId);
                }
            }
        }
    }

    /**
     * 将伤害波及到周围的生物（极低人性惩罚）
     */
    private static void spreadDamageToNearby(EntityPlayer player, EntityLivingBase originalTarget, float damage) {
        double range = HumanityConfig.extremeLowHumanityAoERange;
        float aoeDamage = damage * (float) HumanityConfig.extremeLowHumanityAoEDamageRatio;

        java.util.List<EntityLivingBase> nearbyEntities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class,
                originalTarget.getEntityBoundingBox().grow(range),
                e -> e != originalTarget && e != player && e.isEntityAlive()
        );

        for (EntityLivingBase entity : nearbyEntities) {
            // 波及伤害 - 使用自定义伤害源，会在 onLivingHurt 开头被过滤
            entity.attackEntityFrom(AOE_DAMAGE, aoeDamage);
        }

        // 粒子效果
        if (!nearbyEntities.isEmpty() && player.world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer world = (net.minecraft.world.WorldServer) player.world;
            world.spawnParticle(net.minecraft.util.EnumParticleTypes.SMOKE_LARGE,
                    originalTarget.posX, originalTarget.posY + 1, originalTarget.posZ,
                    10, range * 0.5, 0.5, range * 0.5, 0.01);
        }
    }

    /**
     * 处理玩家受伤
     */
    private static void handlePlayerDamaged(LivingHurtEvent event) {
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (!HumanitySpectrumSystem.isSystemActive(player)) return;

        // 破碎之神有独立的防御系统，跳过人性值防御效果
        if (BrokenGodHandler.isBrokenGod(player)) return;

        // 标记战斗状态
        HumanitySpectrumSystem.markCombat(player);

        // 注意：无痛麻木效果（低人性伤害优先命中要害）
        // 已移至 onFirstAidDamage 方法中，使用 First Aid API 正确处理部位伤害分配

        // 检查量子叠加（灰域致命伤害时）
        if (HumanitySpectrumSystem.checkQuantumCollapse(player, event.getAmount())) {
            event.setCanceled(true);
        }
    }

    // ========== 实体死亡事件 ==========

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        // 玩家击杀实体
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            EntityLivingBase target = event.getEntityLiving();

            if (!HumanitySpectrumSystem.isSystemActive(player)) return;

            // 处理样本掉落
            HumanitySpectrumSystem.handleEntityKill(player, target);

            // 击杀被动生物消耗人性
            HumanitySpectrumSystem.onKillPassiveMob(player, target);
        }
    }

    // ========== 治疗效果 ==========

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!HumanitySpectrumSystem.isSystemActive(player)) return;

        float humanity = HumanitySpectrumSystem.getHumanity(player);

        // 低人性：治疗效果降低
        if (humanity < 50f) {
            float reduction = (float) HumanityConfig.lowHumanityHealingReduction;
            event.setAmount(event.getAmount() * (1f - reduction));
        }
    }

    // ========== First Aid 部位伤害处理（无痛麻木效果）==========

    /**
     * 无痛麻木效果 - 极低人性值时伤害优先打在头部和躯干
     *
     * 当人性值 < 10% 时，玩家因为失去痛觉而无法保护要害部位，
     * 导致伤害更容易命中头部和身体（要害）
     */
    @Optional.Method(modid = "firstaid")
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFirstAidDamage(FirstAidLivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.isCanceled()) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (!HumanitySpectrumSystem.isSystemActive(player)) return;

        float humanity = HumanitySpectrumSystem.getHumanity(player);

        // 极低人性：无痛麻木 - 伤害优先命中要害
        if (humanity < 10f && HumanityConfig.extremeLowHumanityCritChance > 0) {
            // 概率触发无痛麻木效果
            if (player.world.rand.nextFloat() < HumanityConfig.extremeLowHumanityCritChance) {
                applyPainlessNumbnessEffect(event, player);
            }
        }
    }

    /**
     * 应用无痛麻木效果 - 将四肢伤害转移到头部和躯干
     */
    @Optional.Method(modid = "firstaid")
    private static void applyPainlessNumbnessEffect(FirstAidLivingDamageEvent event, EntityPlayer player) {
        AbstractPlayerDamageModel beforeDamage = event.getBeforeDamage();
        AbstractPlayerDamageModel afterDamage = event.getAfterDamage();

        // 计算四肢受到的伤害
        float leftArmDamage = Math.max(0, beforeDamage.LEFT_ARM.currentHealth - afterDamage.LEFT_ARM.currentHealth);
        float rightArmDamage = Math.max(0, beforeDamage.RIGHT_ARM.currentHealth - afterDamage.RIGHT_ARM.currentHealth);
        float leftLegDamage = Math.max(0, beforeDamage.LEFT_LEG.currentHealth - afterDamage.LEFT_LEG.currentHealth);
        float rightLegDamage = Math.max(0, beforeDamage.RIGHT_LEG.currentHealth - afterDamage.RIGHT_LEG.currentHealth);

        float totalLimbDamage = leftArmDamage + rightArmDamage + leftLegDamage + rightLegDamage;

        // 如果四肢没有受到伤害，不需要转移
        if (totalLimbDamage <= 0.1f) return;

        // 将部分四肢伤害转移到要害（头部40%，躯干60%）
        float transferRatio = (float) (HumanityConfig.extremeLowHumanityCritMultiplier - 1.0);
        transferRatio = Math.min(0.7f, Math.max(0.3f, transferRatio)); // 限制在30%-70%

        float transferAmount = totalLimbDamage * transferRatio;
        float headTransfer = transferAmount * 0.4f;
        float bodyTransfer = transferAmount * 0.6f;

        // 恢复部分四肢血量（因为伤害被转移了）
        float recoveryPerLimb = transferAmount * 0.25f;
        afterDamage.LEFT_ARM.currentHealth = Math.min(beforeDamage.LEFT_ARM.currentHealth,
            afterDamage.LEFT_ARM.currentHealth + recoveryPerLimb);
        afterDamage.RIGHT_ARM.currentHealth = Math.min(beforeDamage.RIGHT_ARM.currentHealth,
            afterDamage.RIGHT_ARM.currentHealth + recoveryPerLimb);
        afterDamage.LEFT_LEG.currentHealth = Math.min(beforeDamage.LEFT_LEG.currentHealth,
            afterDamage.LEFT_LEG.currentHealth + recoveryPerLimb);
        afterDamage.RIGHT_LEG.currentHealth = Math.min(beforeDamage.RIGHT_LEG.currentHealth,
            afterDamage.RIGHT_LEG.currentHealth + recoveryPerLimb);

        // 将伤害转移到头部和躯干
        afterDamage.HEAD.currentHealth = Math.max(0, afterDamage.HEAD.currentHealth - headTransfer);
        afterDamage.BODY.currentHealth = Math.max(0, afterDamage.BODY.currentHealth - bodyTransfer);

        // 发送警告消息
        if (!player.world.isRemote) {
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.DARK_RED + "【无痛麻木】" +
                    net.minecraft.util.text.TextFormatting.GRAY + " 伤害命中要害..."
            ), true);
        }
    }

    // ========== 睡眠事件 ==========

    @SubscribeEvent
    public static void onPlayerSleep(PlayerSleepInBedEvent event) {
        // 睡眠开始时不做特殊处理
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        if (!HumanitySpectrumSystem.isSystemActive(player)) return;

        // 检查是否是正常睡眠（不是被打断）
        if (!event.shouldSetSpawn()) return;

        // 睡眠恢复人性值
        HumanitySpectrumSystem.onPlayerSleep(player);
    }

    // ========== 交互事件 ==========

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getWorld().isRemote) return;

        EntityPlayer player = event.getEntityPlayer();
        if (!HumanitySpectrumSystem.isSystemActive(player)) return;

        // 与村民交互限制 - 使用新的层级系统
        if (event.getTarget() instanceof EntityVillager) {
            HumanityEffectsManager.NPCInteractionLevel level =
                    HumanityEffectsManager.getNPCInteractionLevel(player);

            switch (level) {
                case INVISIBLE:
                    // 完全无法交互
                    event.setCanceled(true);
                    player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                            "§8§o村民完全无视你的存在..."), true);
                    makeVillagerFlee((EntityVillager) event.getTarget(), player);
                    break;

                case HOSTILE:
                    // 拒绝交易
                    event.setCanceled(true);
                    player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                            "§c§o村民恐惧地拒绝与你交易"), true);
                    makeVillagerFlee((EntityVillager) event.getTarget(), player);
                    break;

                case SUSPICIOUS:
                    // 可以交互，但会加价（在交易事件中处理）
                    player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                            "§e§o村民用警惕的眼神看着你..."), true);
                    break;

                case TRUSTED:
                case NORMAL:
                    // 正常交互
                    break;
            }
        }

        // 喂养动物
        if (event.getTarget() instanceof EntityAnimal) {
            EntityAnimal animal = (EntityAnimal) event.getTarget();
            if (animal.isBreedingItem(event.getItemStack())) {
                HumanitySpectrumSystem.onFeedAnimal(player);
            }
        }
    }

    /**
     * 让村民逃跑
     */
    private static void makeVillagerFlee(EntityVillager villager, EntityPlayer player) {
        // 简单实现：给村民一个向远离玩家方向的推力
        double dx = villager.posX - player.posX;
        double dz = villager.posZ - player.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > 0) {
            villager.motionX += (dx / dist) * 0.5;
            villager.motionZ += (dz / dist) * 0.5;
        }

        // 恐惧粒子
        if (villager.world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer world = (net.minecraft.world.WorldServer) villager.world;
            world.spawnParticle(
                    net.minecraft.util.EnumParticleTypes.VILLAGER_ANGRY,
                    villager.posX, villager.posY + villager.getEyeHeight(), villager.posZ,
                    3, 0.2, 0.2, 0.2, 0
            );
        }
    }

    // ========== 食物消耗事件 ==========

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!HumanitySpectrumSystem.isSystemActive(player)) return;

        ItemStack stack = event.getItem();
        if (stack.isEmpty()) return;

        // 检查是否是食物
        if (stack.getItem().getItemUseAction(stack) == net.minecraft.item.EnumAction.EAT ||
            stack.getItem().getItemUseAction(stack) == net.minecraft.item.EnumAction.DRINK) {
            HumanitySpectrumSystem.onPlayerEat(player, stack);
        }
    }

    // ========== 作物收获事件 ==========

    @SubscribeEvent
    public static void onBlockHarvest(BlockEvent.HarvestDropsEvent event) {
        if (event.getHarvester() == null) return;
        if (event.getWorld().isRemote) return;

        EntityPlayer player = event.getHarvester();
        if (!HumanitySpectrumSystem.isSystemActive(player)) return;

        // 检查是否是作物方块
        net.minecraft.block.Block block = event.getState().getBlock();
        if (block instanceof net.minecraft.block.BlockCrops ||
            block instanceof net.minecraft.block.BlockStem ||
            block instanceof net.minecraft.block.BlockMelon ||
            block instanceof net.minecraft.block.BlockPumpkin ||
            block instanceof net.minecraft.block.BlockCocoa ||
            block instanceof net.minecraft.block.BlockNetherWart) {
            // 只有成熟的作物才算收获
            if (block instanceof net.minecraft.block.BlockCrops) {
                net.minecraft.block.BlockCrops crops = (net.minecraft.block.BlockCrops) block;
                if (!crops.isMaxAge(event.getState())) return;
            }
            HumanitySpectrumSystem.onHarvestCrop(player);
        }
    }

    // ========== 玩家登录同步 ==========

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            // 延迟一点同步，确保客户端准备好
            HumanitySpectrumSystem.forceSync(event.player);
        }
    }

    // ========== 玩家退出清理 ==========

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HumanitySpectrumSystem.cleanupPlayer(event.player.getUniqueID());
        FleshRejectionSystem.cleanupPlayer(event.player.getUniqueID());
    }
}
