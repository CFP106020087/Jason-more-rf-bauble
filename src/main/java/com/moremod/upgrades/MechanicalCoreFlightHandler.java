package com.moremod.upgrades;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.eventHandler.EventHandlerJetpack;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.UUID;

/**
 * 机械核心飞行模块处理器（仅推力版）
 * - 不拦截双击空格、不授予/取消 allowFlying 和 isFlying
 * - 只根据按键与能量对玩家施加物理推力，并更新 EventHandlerJetpack 的状态位
 */
public class MechanicalCoreFlightHandler {

    // 基础参数
    private static final double BASE_ASCEND_SPEED = 0.08;
    private static final double BASE_DESCEND_SPEED = 0.15;
    private static final double BASE_MOVE_SPEED   = 0.03;
    private static final int    BASE_ENERGY_COST  = 50; // FE/tick

    /**
     * 由 UpgradeEffectManager 每tick调用
     */
    public static void handleFlightModule(EntityPlayer player, ItemStack coreStack) {
        if (player == null || coreStack == null || coreStack.isEmpty()) return;

        int flightLevel = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE);
        if (flightLevel <= 0) return;

        IEnergyStorage energy = coreStack.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy == null) return;

        // 协同升级
        int speedLevel      = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.SPEED_BOOST);
        int efficiencyLevel = ItemMechanicalCore.getUpgradeLevel(coreStack, ItemMechanicalCore.UpgradeType.ENERGY_EFFICIENCY);

        // 键位状态来自 EventHandlerJetpack（客户端每隔2tick上报）
        UUID pid = player.getUniqueID();
        Boolean jumping  = EventHandlerJetpack.jetpackJumping.get(pid);
        Boolean sneaking = EventHandlerJetpack.jetpackSneaking.get(pid);
        if (jumping == null || sneaking == null) return;

        // NBT（是否启用/悬停）
        NBTTagCompound nbt = coreStack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            coreStack.setTagCompound(nbt);
        }
        boolean flightEnabled = nbt.getBoolean("FlightModuleEnabled");
        boolean hoverMode     = nbt.getBoolean("FlightHoverMode");

        // 若未写入过启用位，沿用原逻辑：默认启用一次
        if (!nbt.hasKey("FlightModuleEnabled")) {
            nbt.setBoolean("FlightModuleEnabled", true);
            flightEnabled = true;
        }

        // 能耗：能效每级 -10%
        float efficiencyMul = Math.max(0.0f, 1.0f - (efficiencyLevel * 0.1f));
        int energyPerTick = Math.max(1, (int)(BASE_ENERGY_COST * efficiencyMul));

        // 悬停静止时半耗
        if (hoverMode && !jumping && !sneaking) {
            energyPerTick = Math.max(1, (int)(energyPerTick * 0.5f));
        }

        // 条件满足才推力
        if (flightEnabled && (jumping || sneaking || hoverMode)) {
            // 先试探能量
            if (energy.extractEnergy(energyPerTick, true) >= energyPerTick) {
                // 扣能
                energy.extractEnergy(energyPerTick, false);

                // 速度：加速每级 +30% 水平&上升；下降不变（更易控）
                double speedMul   = 1.0 + (speedLevel * 0.3);
                double ascendSpd  = BASE_ASCEND_SPEED  * speedMul;
                double descendSpd = BASE_DESCEND_SPEED;
                double moveSpd    = BASE_MOVE_SPEED    * speedMul;

                if (hoverMode) ascendSpd *= 1.5;

                // 施加物理推力（不改能力位）
                applyFlightPhysics(player, jumping, sneaking, hoverMode, ascendSpd, descendSpd, moveSpd);

                // 更新状态（供其他逻辑/特效使用）
                EventHandlerJetpack.playerFlying.put(pid, true);
                EventHandlerJetpack.jetpackActivelyUsed.put(pid, true);

                // 粒子
                if (player.world.isRemote) {
                    spawnMechanicalFlightParticles(player, jumping || sneaking, hoverMode,
                            energy.getEnergyStored(), energy.getMaxEnergyStored());
                }
            } else {
                // 能量不足：限频提示 + 清理内部状态（不碰能力位）
                if (!player.world.isRemote && player.world.getTotalWorldTime() % 40 == 0) {
                    player.sendStatusMessage(new TextComponentString(
                            TextFormatting.RED + "⚡ 飞行模块能量不足！"), true);
                }
                disableFlightStateOnly(player, pid);
            }
        } else {
            // 未激活或无按键
            disableFlightStateOnly(player, pid);
        }
    }

    /**
     * 仅施加推力，不修改 allowFlying/isFlying
     */
    private static void applyFlightPhysics(EntityPlayer player, boolean jumping, boolean sneaking,
                                           boolean hoverMode, double ascendSpeed, double descendSpeed,
                                           double moveSpeed) {
        // 垂直推力
        if (hoverMode) {
            if (jumping && player.motionY < ascendSpeed * 2.5) {
                player.motionY += ascendSpeed;
            } else if (sneaking && player.motionY > -descendSpeed * 2.0) {
                player.motionY -= descendSpeed;
            } else if (!jumping && !sneaking) {
                // 精准悬停：抵消下落并缓和上升
                if (player.motionY < 0) {
                    player.motionY += 0.08;
                    if (player.motionY > 0) player.motionY = 0;
                } else if (player.motionY > 0.02) {
                    player.motionY *= 0.9;
                }
            }
        } else {
            if (jumping && player.motionY < ascendSpeed * 2.5) {
                player.motionY += ascendSpeed;
            }
            if (sneaking && player.motionY > -descendSpeed * 2.0) {
                player.motionY -= descendSpeed * 0.7;
            }
        }

        // 水平推力
        float yaw = player.rotationYaw;
        double yawRad  = Math.toRadians(yaw);
        double forward = player.moveForward;
        double strafe  = player.moveStrafing;

        double dz = -(forward * Math.cos(yawRad) + strafe * Math.sin(yawRad)) * moveSpeed;
        double dx =  (forward * Math.sin(yawRad) - strafe * Math.cos(yawRad)) * moveSpeed;

        player.motionX -= dx;
        player.motionZ -= dz;

        // 高空小保护
        if (player.posY > 200) {
            player.addPotionEffect(new net.minecraft.potion.PotionEffect(
                    net.minecraft.init.MobEffects.RESISTANCE, 100, 0, true, false));
        }
    }

    /**
     * 清理内部飞行状态（不触碰玩家飞行能力位）
     */
    private static void disableFlightStateOnly(EntityPlayer player, UUID playerId) {
        EventHandlerJetpack.playerFlying.put(playerId, false);
        EventHandlerJetpack.jetpackActivelyUsed.put(playerId, false);
    }

    /**
     * 机械核心粒子（能量色）
     */
    private static void spawnMechanicalFlightParticles(EntityPlayer player, boolean thrust,
                                                       boolean hover, int currentEnergy, int maxEnergy) {
        World world = player.world;
        double x = player.posX, y = player.posY, z = player.posZ;

        float energyPercent = maxEnergy > 0 ? (float) currentEnergy / maxEnergy : 0f;

        if (hover && !thrust) {
            for (int i = 0; i < 6; i++) {
                double angle = (world.getTotalWorldTime() + i * 60) * 0.1;
                double radius = 0.8;
                double ox = Math.cos(angle) * radius;
                double oz = Math.sin(angle) * radius;

                EnumParticleTypes type = energyPercent > 0.6f ? EnumParticleTypes.VILLAGER_HAPPY :
                        (energyPercent > 0.3f ? EnumParticleTypes.SPELL : EnumParticleTypes.REDSTONE);

                world.spawnParticle(type, x + ox, y + 0.5, z + oz, 0, -0.02, 0);
            }
        }

        if (thrust) {
            float yaw = player.rotationYaw;
            double yawRad = Math.toRadians(yaw);

            double tx = x - Math.sin(yawRad) * 0.3;
            double ty = y + 0.8;
            double tz = z + Math.cos(yawRad) * 0.3;

            int count = energyPercent > 0.3f ? 8 : 4;

            for (int i = 0; i < count; i++) {
                double spread = 0.2;
                double ox = (world.rand.nextDouble() - 0.5) * spread;
                double oy = (world.rand.nextDouble() - 0.5) * spread;
                double oz = (world.rand.nextDouble() - 0.5) * spread;

                if (energyPercent > 0.6f) {
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB, tx + ox, ty + oy, tz + oz, 0.0, 0.8, 1.0);
                } else if (energyPercent > 0.3f) {
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB, tx + ox, ty + oy, tz + oz, 1.0, 1.0, 0.0);
                } else {
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB, tx + ox, ty + oy, tz + oz, 1.0, 0.0, 0.0);
                }

                if (world.rand.nextInt(3) == 0) {
                    world.spawnParticle(EnumParticleTypes.ENCHANTMENT_TABLE,
                            tx, ty, tz, -Math.sin(yawRad) * 0.1, -0.2, Math.cos(yawRad) * 0.1);
                }
            }
        }
    }
}
