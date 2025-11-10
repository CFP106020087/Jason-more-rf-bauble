package com.moremod.upgrades.combat;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;

/**
 * 战斗类升级效果管理器 - 完整版带能量检查
 */
public class CombatUpgradeManager {

    /**
     * 伤害提升系统
     */
    public static class DamageBoostSystem {

        /**
         * 在 LivingHurtEvent 中計算傷害加成
         * @return 傷害倍率
         */
        public static float getDamageMultiplier(EntityPlayer player, ItemStack coreStack) {
            int level = ItemMechanicalCore.getUpgradeLevel(coreStack, "DAMAGE_BOOST");
            if (level <= 0) return 1.0F;

            // 检查升级是否激活
            if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "DAMAGE_BOOST")) {
                return 1.0F; // 未激活时返回基础倍率
            }

            // 消耗能量（每次攻击）
            if (!ItemMechanicalCore.consumeEnergyForUpgrade(coreStack, "DAMAGE_BOOST", 20 * level)) {
                // 能量不足提示
                if (player.world.getTotalWorldTime() % 60 == 0) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.RED + "⚡ 伤害增幅能量不足"
                    ), true);
                }
                return 1.0F; // 能量不足时返回基础倍率
            }

            // 伤害加成：50%/100%/150%/200%/250%
            return 1.0F + (0.25F * level);
        }

        // 暴击系统
        public static float applyCritical(EntityPlayer player, float damage, int level) {
            if (level <= 0) return damage;

            ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);

            // 检查升级是否激活
            if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "DAMAGE_BOOST")) {
                return damage; // 未激活时不应用暴击
            }

            // 暴击几率：10%/20%/30%/40%/50%
            float critChance = 0.1F * level;

            if (player.getRNG().nextFloat() < critChance) {
                // 暴击消耗额外能量
                if (!ItemMechanicalCore.consumeEnergy(coreStack, 10)) {
                    return damage; // 能量不足无法暴击
                }

                // 暴击伤害：2x
                damage *= 2.0F;

                // 暴击特效
                player.world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.CRIT_MAGIC,
                        player.posX, player.posY + player.getEyeHeight(), player.posZ,
                        0, 0, 0
                );

                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GOLD + "⚔ 暴击！"
                ), true);
            }

            return damage;
        }
    }

    /**
     * 攻击速度系统
     */
    public static class AttackSpeedSystem {
        private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("d8499b04-2222-4726-ab29-64469d734e0d");
        private static final String NBT_LAST_ATTACK = "MechanicalCoreLastAttack";
        private static final String NBT_SPEED_APPLIED = "MechanicalCoreSpeedApplied";

        public static void applyAttackSpeed(EntityPlayer player, ItemStack coreStack) {
            int level = ItemMechanicalCore.getUpgradeLevel(coreStack, "ATTACK_SPEED");
            if (level <= 0) {
                removeAttackSpeed(player);
                return;
            }

            // 检查升级是否激活
            if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "ATTACK_SPEED")) {
                removeAttackSpeed(player);

                // 提示信息
                if (player.getEntityData().getBoolean(NBT_SPEED_APPLIED)) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.YELLOW + "⚡ 攻击速度加成已失效（能量不足）"
                    ), true);
                    player.getEntityData().setBoolean(NBT_SPEED_APPLIED, false);
                }
                return;
            }

            // 标记速度已应用
            if (!player.getEntityData().getBoolean(NBT_SPEED_APPLIED)) {
                player.getEntityData().setBoolean(NBT_SPEED_APPLIED, true);
            }

            // 攻速加成：20%/40%/60%
            double speedBonus = 0.2 * level;

            // 修改攻击速度属性
            net.minecraft.entity.ai.attributes.IAttributeInstance attackSpeed =
                    player.getAttributeMap().getAttributeInstance(
                            net.minecraft.entity.SharedMonsterAttributes.ATTACK_SPEED
                    );

            // 移除旧的修改器
            attackSpeed.removeModifier(SPEED_MODIFIER_UUID);

            // 添加新的速度修改器
            net.minecraft.entity.ai.attributes.AttributeModifier modifier =
                    new net.minecraft.entity.ai.attributes.AttributeModifier(
                            SPEED_MODIFIER_UUID,
                            "Mechanical Core Attack Speed",
                            speedBonus,
                            2 // MULTIPLY_TOTAL
                    );
            attackSpeed.applyModifier(modifier);
        }

        public static void removeAttackSpeed(EntityPlayer player) {
            net.minecraft.entity.ai.attributes.IAttributeInstance attackSpeed =
                    player.getAttributeMap().getAttributeInstance(
                            net.minecraft.entity.SharedMonsterAttributes.ATTACK_SPEED
                    );
            attackSpeed.removeModifier(SPEED_MODIFIER_UUID);
            player.getEntityData().setBoolean(NBT_SPEED_APPLIED, false);
        }

        // 连击系统
        public static void checkCombo(EntityPlayer player, int level) {
            if (level <= 0) return;

            ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);

            // 检查升级是否激活
            if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "ATTACK_SPEED")) {
                return;
            }

            long lastAttack = player.getEntityData().getLong(NBT_LAST_ATTACK);
            long currentTime = player.world.getTotalWorldTime();

            // 连击窗口：40 tick
            if (currentTime - lastAttack < 40) {
                // 连击消耗少量能量
                if (ItemMechanicalCore.consumeEnergy(coreStack, 5)) {
                    // 连击加成
                    player.addExhaustion(-0.5F); // 减少攻击消耗

                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.YELLOW + "连击！"
                    ), true);
                }
            }

            player.getEntityData().setLong(NBT_LAST_ATTACK, currentTime);
        }
    }

    /**
     * 范围拓展系统 - 增加攻擊觸及距離
     */
    public static class RangeExtensionSystem {
        private static final UUID REACH_MODIFIER_UUID = UUID.fromString("d8499b04-3333-4726-ab29-64469d734e0d");
        private static final String NBT_EXTENDED_REACH = "MechanicalCoreExtendedReach";
        private static final String NBT_REACH_APPLIED = "MechanicalCoreReachApplied";

        public static void applyReachExtension(EntityPlayer player, ItemStack coreStack) {
            int level = ItemMechanicalCore.getUpgradeLevel(coreStack, "RANGE_EXTENSION");
            if (level <= 0) {
                removeReachExtension(player);
                return;
            }

            // 检查升级是否激活
            if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "RANGE_EXTENSION")) {
                removeReachExtension(player);

                // 提示信息
                if (player.getEntityData().getBoolean(NBT_REACH_APPLIED)) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.YELLOW + "⚡ 攻击范围扩展已失效（能量不足）"
                    ), true);
                    player.getEntityData().setBoolean(NBT_REACH_APPLIED, false);
                }
                return;
            }

            // 标记已应用
            if (!player.getEntityData().getBoolean(NBT_REACH_APPLIED)) {
                player.getEntityData().setBoolean(NBT_REACH_APPLIED, true);
            }

            // 觸及距離增加：1/2/3 格
            double reachBonus = 3.0 * level;

            // 在玩家NBT中存儲觸及距離加成
            player.getEntityData().setDouble(NBT_EXTENDED_REACH, reachBonus);

            // 使用屬性修改器增加觸及距離
            net.minecraft.entity.ai.attributes.IAttributeInstance reachDistance =
                    player.getAttributeMap().getAttributeInstance(
                            net.minecraft.entity.player.EntityPlayer.REACH_DISTANCE
                    );

            // 移除舊的修改器
            reachDistance.removeModifier(REACH_MODIFIER_UUID);

            // 添加新的觸及距離修改器
            net.minecraft.entity.ai.attributes.AttributeModifier modifier =
                    new net.minecraft.entity.ai.attributes.AttributeModifier(
                            REACH_MODIFIER_UUID,
                            "Mechanical Core Reach",
                            reachBonus,
                            0 // ADD
                    );
            reachDistance.applyModifier(modifier);
        }

        public static void removeReachExtension(EntityPlayer player) {
            player.getEntityData().removeTag(NBT_EXTENDED_REACH);
            player.getEntityData().setBoolean(NBT_REACH_APPLIED, false);

            net.minecraft.entity.ai.attributes.IAttributeInstance reachDistance =
                    player.getAttributeMap().getAttributeInstance(
                            net.minecraft.entity.player.EntityPlayer.REACH_DISTANCE
                    );
            reachDistance.removeModifier(REACH_MODIFIER_UUID);
        }

        /**
         * 檢查目標是否在擴展的觸及範圍內
         */
        public static boolean isInExtendedReach(EntityPlayer player, Entity target) {
            double reachBonus = player.getEntityData().getDouble(NBT_EXTENDED_REACH);
            double baseReach = player.isCreative() ? 5.0D : 4.5D;
            double totalReach = baseReach + reachBonus;

            return player.getDistance(target) <= totalReach;
        }

        /**
         * 顯示觸及範圍視覺效果
         */
        public static void showReachIndicator(EntityPlayer player, int level) {
            if (level <= 0 || player.world.getTotalWorldTime() % 20 != 0) return;

            ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);

            // 检查是否激活
            if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "RANGE_EXTENSION")) {
                return;
            }

            // 显示效果消耗少量能量
            if (!ItemMechanicalCore.consumeEnergy(coreStack, 2)) {
                return;
            }

            // 在玩家周圍顯示觸及範圍粒子
            double reach = player.isCreative() ? 5.0D : 4.5D;
            reach += level;

            // 顯示範圍邊界粒子
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2) * i / 16;
                double x = player.posX + Math.cos(angle) * reach;
                double z = player.posZ + Math.sin(angle) * reach;

                player.world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.ENCHANTMENT_TABLE,
                        x, player.posY + 1, z,
                        0, 0, 0
                );
            }
        }
    }

    /**
     * 追击系统
     */
    public static class PursuitSystem {
        private static final String NBT_PURSUIT_TARGET = "MechanicalCorePursuitTarget";
        private static final String NBT_PURSUIT_STACKS = "MechanicalCorePursuitStacks";
        private static final String NBT_LAST_PURSUIT = "MechanicalCoreLastPursuit";

        public static void markTarget(EntityPlayer player, Entity target, int level) {
            if (level <= 0) return;

            ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);

            // 检查升级是否激活
            if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "PURSUIT")) {
                // 清除追击标记
                player.getEntityData().removeTag(NBT_PURSUIT_TARGET);
                player.getEntityData().setInteger(NBT_PURSUIT_STACKS, 0);
                return;
            }

            // 标记目标消耗少量能量
            if (!ItemMechanicalCore.consumeEnergyForUpgrade(coreStack, "PURSUIT", 5)) {
                return;
            }

            String currentTarget = player.getEntityData().getString(NBT_PURSUIT_TARGET);
            String targetUUID = target.getUniqueID().toString();

            if (currentTarget.equals(targetUUID)) {
                // 增加追击层数
                int stacks = player.getEntityData().getInteger(NBT_PURSUIT_STACKS);
                stacks = Math.min(stacks + 1, level * 2); // 最大层数：2/4/6
                player.getEntityData().setInteger(NBT_PURSUIT_STACKS, stacks);

                if (stacks % 2 == 0) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.LIGHT_PURPLE + "追击层数: " + stacks
                    ), true);
                }
            } else {
                // 新目标，重置层数
                player.getEntityData().setString(NBT_PURSUIT_TARGET, targetUUID);
                player.getEntityData().setInteger(NBT_PURSUIT_STACKS, 1);
            }

            player.getEntityData().setLong(NBT_LAST_PURSUIT, player.world.getTotalWorldTime());
        }

        public static float getPursuitDamage(EntityPlayer player, Entity target) {
            ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);

            // 检查升级是否激活
            if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "PURSUIT")) {
                return 0;
            }

            String targetUUID = target.getUniqueID().toString();
            String markedTarget = player.getEntityData().getString(NBT_PURSUIT_TARGET);

            if (!targetUUID.equals(markedTarget)) return 0;

            // 检查追击是否过期（1秒）
            long lastPursuit = player.getEntityData().getLong(NBT_LAST_PURSUIT);
            if (player.world.getTotalWorldTime() - lastPursuit > 20) {  // 改为20 ticks (1秒)
                // 追击过期，清除标记
                player.getEntityData().removeTag(NBT_PURSUIT_TARGET);
                player.getEntityData().setInteger(NBT_PURSUIT_STACKS, 0);
                return 0;
            }

            int stacks = player.getEntityData().getInteger(NBT_PURSUIT_STACKS);
            // 每层10%额外伤害
            return stacks * 0.1F;
        }

        // 追击冲刺
        public static void dashToTarget(EntityPlayer player, Entity target, int level) {
            if (level < 2) return; // 2级以上才有冲刺

            ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);

            // 检查升级是否激活
            if (!ItemMechanicalCore.isUpgradeEnabled(coreStack, "PURSUIT")) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚡ 追击系统未激活"
                ), true);
                return;
            }

            // 冲刺需要额外能量
            if (!ItemMechanicalCore.consumeEnergyForUpgrade(coreStack, "PURSUIT", 50)) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚡ 能量不足，无法追击冲刺"
                ), true);
                return;
            }

            double distance = player.getDistance(target);
            if (distance > 2 && distance < 8) {
                // 向目标冲刺
                Vec3d direction = target.getPositionVector().subtract(player.getPositionVector()).normalize();
                player.addVelocity(direction.x * 0.8, 0.2, direction.z * 0.8);

                // 粒子轨迹
                for (int i = 0; i < 10; i++) {
                    player.world.spawnParticle(
                            net.minecraft.util.EnumParticleTypes.CRIT,
                            player.posX, player.posY + 1, player.posZ,
                            direction.x * 0.1, 0, direction.z * 0.1
                    );
                }

                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + "⚡ 追击冲刺！"
                ), true);
            }
        }
    }

    /**
     * 玩家更新事件 - 應用持續效果
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return;

        // 获取能量状态
        EnergyDepletionManager.EnergyStatus status = ItemMechanicalCore.getEnergyStatus(coreStack);

        // 如果在生命支持模式，不应用战斗升级
        if (status == EnergyDepletionManager.EnergyStatus.CRITICAL) {
            AttackSpeedSystem.removeAttackSpeed(player);
            RangeExtensionSystem.removeReachExtension(player);

            // 清除追击标记
            player.getEntityData().removeTag(PursuitSystem.NBT_PURSUIT_TARGET);
            player.getEntityData().setInteger(PursuitSystem.NBT_PURSUIT_STACKS, 0);
            return;
        }

        // 紧急模式下，只保留基础战斗加成
        if (status == EnergyDepletionManager.EnergyStatus.EMERGENCY) {
            // 保留攻击速度和伤害，但移除范围扩展
            AttackSpeedSystem.applyAttackSpeed(player, coreStack);
            RangeExtensionSystem.removeReachExtension(player);
        } else {
            // 正常或省电模式，应用所有战斗升级
            AttackSpeedSystem.applyAttackSpeed(player, coreStack);
            RangeExtensionSystem.applyReachExtension(player, coreStack);
        }

        // 顯示觸及範圍指示器（可選）
        if (player.getHeldItemMainhand().isEmpty() && player.isSneaking()) {
            int rangeLevel = ItemMechanicalCore.getUpgradeLevel(coreStack, "RANGE_EXTENSION");
            RangeExtensionSystem.showReachIndicator(player, rangeLevel);
        }

        // 清理过期的追击标记（1秒）
        long lastPursuit = player.getEntityData().getLong(PursuitSystem.NBT_LAST_PURSUIT);
        if (player.world.getTotalWorldTime() - lastPursuit > 20) {  // 改为20 ticks (1秒)
            player.getEntityData().removeTag(PursuitSystem.NBT_PURSUIT_TARGET);
            player.getEntityData().setInteger(PursuitSystem.NBT_PURSUIT_STACKS, 0);
        }
    }

    /**
     * 攻击事件处理
     */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return;

        // 檢查攻擊速度連擊
        AttackSpeedSystem.checkCombo(player, ItemMechanicalCore.getUpgradeLevel(coreStack, "ATTACK_SPEED"));

        if (event.getTarget() instanceof EntityLivingBase) {
            // 追击标记
            int pursuitLevel = ItemMechanicalCore.getUpgradeLevel(coreStack, "PURSUIT");
            PursuitSystem.markTarget(player, event.getTarget(), pursuitLevel);

            // 追击冲刺
            if (player.isSneaking()) {
                PursuitSystem.dashToTarget(player, event.getTarget(), pursuitLevel);
            }
        }
    }

    /**
     * LivingHurtEvent 處理 - LOWEST 優先級
     * 在所有其他模組處理完傷害後，最後應用傷害加成
     */
    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
    public static void onLivingHurtLowest(LivingHurtEvent event) {
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
            if (coreStack.isEmpty()) return;

            float damage = event.getAmount();

            // 應用傷害提升
            int damageLevel = ItemMechanicalCore.getUpgradeLevel(coreStack, "DAMAGE_BOOST");
            if (damageLevel > 0) {
                float multiplier = DamageBoostSystem.getDamageMultiplier(player, coreStack);
                damage *= multiplier;

                // 顯示傷害加成
                if (multiplier > 1.0F && player.world.rand.nextInt(5) == 0) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.RED + String.format("⚔ 傷害加成 +%d%%", (int)((multiplier - 1) * 100))
                    ), true);
                }
            }

            // 應用暴擊
            damage = DamageBoostSystem.applyCritical(player, damage, damageLevel);

            // 應用追擊傷害
            float pursuitBonus = PursuitSystem.getPursuitDamage(player, event.getEntity());
            if (pursuitBonus > 0) {
                damage *= (1 + pursuitBonus);

                // 顯示追擊加成
                if (player.world.rand.nextInt(3) == 0) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.LIGHT_PURPLE + String.format("⚡ 追擊加成 +%d%%", (int)(pursuitBonus * 100))
                    ), true);
                }
            }

            // 設置最終傷害
            event.setAmount(damage);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        // 移除原本的傷害處理，改為在 LivingHurtEvent LOWEST 中處理
    }
}