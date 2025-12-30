package com.moremod.upgrades.energy;

import com.moremod.config.EnergyBalanceConfig;
import com.moremod.item.ItemMechanicalCore;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 能源类升级效果管理器（完整整合版）
 * v2.0 - 全面接入 EnergyBalanceConfig 和 ItemMechanicalCore
 */
public class EnergyUpgradeManager {

    private static final String NS = "moremod.MechanicalCore.";
    private static final String NBT_LAST_POS_X = NS + "LastPosX";
    private static final String NBT_LAST_POS_Y = NS + "LastPosY";
    private static final String NBT_LAST_POS_Z = NS + "LastPosZ";
    private static final String NBT_KINETIC_BUFFER = NS + "KineticBuffer";
    private static final String NBT_SOLAR_TICK = NS + "SolarLastTick";
    private static final String NBT_VOID_CHARGE = NS + "VoidCharge";
    private static final String NBT_COMBAT_LASTKILL_TICK = NS + "CombatLastKillTick";

    private static final boolean DEBUG_MODE = false;

    /**
     * 工具：获取当前核心（佩戴在头部的）
     */
    private static ItemStack getCore(EntityPlayer player) {
        return ItemMechanicalCore.getCoreFromPlayer(player);
    }

    /**
     * 工具：获取能量百分比（0~1）
     */
    private static float getEnergyPercent(ItemStack core) {
        IEnergyStorage es = ItemMechanicalCore.getEnergyStorage(core);
        if (es == null || es.getMaxEnergyStored() <= 0) return 0f;
        return Math.max(0f, Math.min(1f, es.getEnergyStored() / (float) es.getMaxEnergyStored()));
    }

    /**
     * 产能侧效率加成：
     * - 若装了能量效率升级：消费侧更省，同时给予产能侧适度加成
     * - 叠加低电量惩罚
     */
    private static double generationMultiplier(EntityPlayer player, ItemStack core) {
        // 效率升级
        int effLv = ItemMechanicalCore.getEffectiveUpgradeLevel(core, "energy_efficiency");
        double consumeMul = ItemMechanicalCore.getEfficiencyMultiplier(effLv);
        double prodBoost = consumeMul > 0 ? (1.0 / consumeMul) : 1.0;
        // 上限以防爆表
        prodBoost = Math.min(prodBoost, 1.8);

        // 低能量惩罚
        float pct = getEnergyPercent(core);
        double lowMul = EnergyBalanceConfig.getLowEnergyMultiplier(pct);

        // 最终
        return prodBoost;
    }

    /**
     * 动能发电系统（移动/跳跃/疾跑、挖掘）
     */
    public static class KineticGeneratorSystem {

        public static void generateFromMovement(EntityPlayer player, ItemStack core) {
            int level = ItemMechanicalCore.getUpgradeLevel(core, "KINETIC_GENERATOR");
            if (level <= 0) return;

            // 读取上次位置
            double lastX = player.getEntityData().getDouble(NBT_LAST_POS_X);
            double lastY = player.getEntityData().getDouble(NBT_LAST_POS_Y);
            double lastZ = player.getEntityData().getDouble(NBT_LAST_POS_Z);

            // 初始化：第一次没有位置记录时仅更新位置，不产能
            if (lastX == 0 && lastY == 0 && lastZ == 0) {
                storePos(player);
                return;
            }

            double distance = player.getDistance(lastX, lastY, lastZ);
            // 过滤传送/超大位移
            if (distance > 0.1 && distance < 100.0) {
                // 基础每格
                int perBlock = EnergyBalanceConfig.KineticGenerator.ENERGY_PER_BLOCK
                        + (EnergyBalanceConfig.KineticGenerator.ENERGY_PER_LEVEL * level);
                double mult = 1.0;

                if (player.isSprinting()) {
                    mult *= EnergyBalanceConfig.KineticGenerator.SPRINT_MULTIPLIER;
                }
                if (player.isElytraFlying()) {
                    mult *= EnergyBalanceConfig.KineticGenerator.ELYTRA_MULTIPLIER;
                }
                if (!player.onGround) {
                    mult *= EnergyBalanceConfig.KineticGenerator.JUMP_MULTIPLIER;
                }

                // 产出 = 距离 * perBlock * 倍率 * 产能侧倍率
                int energy = (int) Math.floor(distance * perBlock * mult * generationMultiplier(player, core));

                // 缓冲入账
                int buffer = player.getEntityData().getInteger(NBT_KINETIC_BUFFER);
                buffer += Math.max(0, energy);

                int threshold = EnergyBalanceConfig.KineticGenerator.BUFFER_THRESHOLD;
                if (buffer >= threshold) {
                    ItemMechanicalCore.addEnergy(core, buffer);
                    player.getEntityData().setInteger(NBT_KINETIC_BUFFER, 0);

                    if (DEBUG_MODE) {
                        System.out.println("[KineticGenerator] 产生能量: " + buffer + " RF");
                    }
                } else {
                    player.getEntityData().setInteger(NBT_KINETIC_BUFFER, buffer);
                }
            }

            // 更新位置
            storePos(player);
        }

        private static void storePos(EntityPlayer player) {
            player.getEntityData().setDouble(NBT_LAST_POS_X, player.posX);
            player.getEntityData().setDouble(NBT_LAST_POS_Y, player.posY);
            player.getEntityData().setDouble(NBT_LAST_POS_Z, player.posZ);
        }

        @SubscribeEvent
        public static void onBlockBreak(BlockEvent.BreakEvent event) {
            EntityPlayer player = event.getPlayer();
            if (player == null || player.world.isRemote) return;

            ItemStack core = getCore(player);
            if (core.isEmpty()) return;

            int level = ItemMechanicalCore.getUpgradeLevel(core, "KINETIC_GENERATOR");
            if (level <= 0) return;

            float hardness = event.getState().getBlockHardness(player.world, event.getPos());
            if (hardness < 0) return; // 不可破坏

            int base = EnergyBalanceConfig.KineticGenerator.BLOCK_BREAK_BASE;
            int energy = (int) Math.floor(hardness * base * level * generationMultiplier(player, core));

            if (energy > 0) {
                ItemMechanicalCore.addEnergy(core, energy);

                // 视觉反馈
                BlockPos p = event.getPos();
                player.world.spawnParticle(EnumParticleTypes.CRIT,
                        p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                        0, 0, 0);

                if (DEBUG_MODE) {
                    System.out.println("[KineticGenerator] 挖掘产能: " + energy + " RF");
                }
            }
        }
    }

    /**
     * 太阳能发电系统
     */
    public static class SolarGeneratorSystem {

        public static void generateFromSunlight(EntityPlayer player, ItemStack core) {
            int level = ItemMechanicalCore.getUpgradeLevel(core, "SOLAR_GENERATOR");
            if (level <= 0) return;

            // 能见天、白天、光照阈值
            BlockPos pos = player.getPosition();
            if (!player.world.canSeeSky(pos)) return;
            if (!player.world.isDaytime()) return;

            int minSky = EnergyBalanceConfig.SolarGenerator.MIN_SKY_LIGHT;
            int skyLight = player.world.getLightFor(EnumSkyBlock.SKY, pos);
            if (skyLight < minSky) return;

            // 周期：每 20 tick
            long last = player.getEntityData().getLong(NBT_SOLAR_TICK);
            long now = player.world.getTotalWorldTime();
            if (now - last < 20) return;

            // 基础：每级产能
            int base = EnergyBalanceConfig.SolarGenerator.ENERGY_PER_LEVEL * level;

            // 高度加成（>100 线性增长），封顶
            double heightBonus = 1.0;
            if (player.posY > 100.0) {
                heightBonus = 1.0 + (player.posY - 100.0) / 100.0;
                heightBonus = Math.min(heightBonus, EnergyBalanceConfig.SolarGenerator.HEIGHT_BONUS_MAX);
            }

            // 天气惩罚（雷暴优先于雨）
            double weather = 1.0;
            if (player.world.isThundering()) {
                weather = EnergyBalanceConfig.SolarGenerator.STORM_PENALTY;
            } else if (player.world.isRaining()) {
                weather = EnergyBalanceConfig.SolarGenerator.RAIN_PENALTY;
            }

            int energy = (int) Math.floor(base * heightBonus * weather * generationMultiplier(player, core));

            if (energy > 0) {
                ItemMechanicalCore.addEnergy(core, energy);
                player.getEntityData().setLong(NBT_SOLAR_TICK, now);

                // 视觉效果
                if (energy >= 10) {
                    for (int i = 0; i < 2; i++) {
                        player.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                                player.posX + (player.getRNG().nextDouble() - 0.5),
                                player.posY + 1.8,
                                player.posZ + (player.getRNG().nextDouble() - 0.5),
                                0, -0.05, 0);
                    }
                }

                if (DEBUG_MODE) {
                    System.out.println("[SolarGenerator] 太阳能产能: " + energy + " RF");
                }
            }
        }
    }

    /**
     * 虚空能量系统（末地/低 Y）
     * 越接近地底能量越高：
     * - Y >= deepYLevel: 不发电
     * - Y < deepYLevel: 开始发电，越深倍率越高
     * - Y <= voidYLevel: 最大倍率
     */
    public static class VoidEnergySystem {

        public static void generateFromVoid(EntityPlayer player, ItemStack core) {
            int level = ItemMechanicalCore.getUpgradeLevel(core, "VOID_ENERGY");
            if (level <= 0) return;

            boolean inVoidZone = false;
            float zoneMult = 1;

            int deepY = EnergyBalanceConfig.VoidEnergy.DEEP_Y_LEVEL;  // 开始发电高度 (64)
            int voidY = EnergyBalanceConfig.VoidEnergy.VOID_Y_LEVEL;  // 最大发电高度 (16)
            float maxMult = 5.0f;  // 最大深度倍率

            // 末地：直接获得末地倍率
            if (player.dimension == 1) {
                inVoidZone = true;
                zoneMult = EnergyBalanceConfig.VoidEnergy.END_MULTIPLIER;
            }

            // 深度发电：越深能量越高
            double playerY = player.posY;
            if (playerY < deepY) {
                inVoidZone = true;

                if (playerY <= voidY) {
                    // 最深处：最大倍率
                    zoneMult = Math.max(zoneMult, maxMult);
                } else {
                    // 线性插值：deepY -> voidY 对应 1.0 -> maxMult
                    float depthRatio = (float)(deepY - playerY) / (float)(deepY - voidY);
                    float depthMult = 1.0f + (maxMult - 1.0f) * depthRatio;
                    zoneMult = Math.max(zoneMult, depthMult);
                }
            }

            if (!inVoidZone) return;

            // 每 tick 充能
            int perTick = EnergyBalanceConfig.VoidEnergy.CHARGE_PER_TICK;
            int charge = player.getEntityData().getInteger(NBT_VOID_CHARGE);
            charge += perTick * level * zoneMult;

            // 转换：每满 100 充能 → CHARGE_CONVERSION RF
            int batches = charge / 100;
            if (batches > 0) {
                int rf = (int) Math.floor(batches * EnergyBalanceConfig.VoidEnergy.CHARGE_CONVERSION
                        * generationMultiplier(player, core));

                if (rf > 0) {
                    ItemMechanicalCore.addEnergy(core, rf);

                    // 视觉效果
                    for (int i = 0; i < Math.min(6, batches * 2); i++) {
                        player.world.spawnParticle(EnumParticleTypes.PORTAL,
                                player.posX + (player.getRNG().nextDouble() - 0.5) * 2,
                                player.posY + player.getRNG().nextDouble() * 2,
                                player.posZ + (player.getRNG().nextDouble() - 0.5) * 2,
                                0, 0, 0);
                    }

                    if (DEBUG_MODE) {
                        System.out.println("[VoidEnergy] 虚空充能: " + rf + " RF");
                    }
                }
                charge = charge % 100;
            }
            player.getEntityData().setInteger(NBT_VOID_CHARGE, charge);

            // 末地额外奖励：每 100 tick
            if (player.dimension == 1 && player.world.getTotalWorldTime() % 100 == 0) {
                int bonus = (int) Math.floor(EnergyBalanceConfig.VoidEnergy.END_BONUS * level
                        * generationMultiplier(player, core));

                if (bonus > 0) {
                    ItemMechanicalCore.addEnergy(core, bonus);

                    if (DEBUG_MODE) {
                        System.out.println("[VoidEnergy] 末地奖励: " + bonus + " RF");
                    }
                }
            }
        }
    }

    /**
     * 战斗充能系统（击杀）
     */
    public static class CombatChargerSystem {
        private static final Map<UUID, Integer> combatStreak = new HashMap<>();
        private static final Map<UUID, Long> lastKillTime = new HashMap<>();

        @SubscribeEvent
        public static void onEntityKill(LivingDeathEvent event) {
            if (event.getSource().getTrueSource() == null) return;
            if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
            if (player.world.isRemote) return;

            ItemStack core = getCore(player);
            if (core.isEmpty()) return;

            int level = ItemMechanicalCore.getUpgradeLevel(core, "COMBAT_CHARGER");
            if (level <= 0) return;

            float maxHP = event.getEntityLiving().getMaxHealth();
            double base = maxHP * EnergyBalanceConfig.CombatCharger.ENERGY_PER_HP * level;

            // Boss / 小Boss 倍率
            double bossMul = 1.0;
            if (event.getEntityLiving() instanceof net.minecraft.entity.boss.EntityDragon
                    || event.getEntityLiving() instanceof net.minecraft.entity.boss.EntityWither) {
                bossMul = EnergyBalanceConfig.CombatCharger.BOSS_MULTIPLIER;
            } else if (!event.getEntityLiving().isNonBoss()) {
                bossMul = EnergyBalanceConfig.CombatCharger.MINIBOSS_MULTIPLIER;
            }

            // 连杀系统
            UUID id = player.getUniqueID();
            long currentTime = player.world.getTotalWorldTime();
            Long lastKill = lastKillTime.get(id);

            // 检查连杀是否超时
            if (lastKill != null && currentTime - lastKill > EnergyBalanceConfig.CombatCharger.STREAK_TIMEOUT) {
                combatStreak.remove(id);
            }

            int streak = combatStreak.getOrDefault(id, 0) + 1;
            combatStreak.put(id, streak);
            lastKillTime.put(id, currentTime);

            double streakMul = Math.min(1.0 + 0.1 * streak, EnergyBalanceConfig.CombatCharger.MAX_STREAK_BONUS);

            int energy = (int) Math.floor(base * bossMul * streakMul * generationMultiplier(player, core));

            if (energy > 0) {
                ItemMechanicalCore.addEnergy(core, energy);

                // 视觉效果
                for (int i = 0; i < 12; i++) {
                    player.world.spawnParticle(EnumParticleTypes.SPELL_MOB,
                            event.getEntityLiving().posX + (player.getRNG().nextDouble() - 0.5) * 2,
                            event.getEntityLiving().posY + player.getRNG().nextDouble() * 2,
                            event.getEntityLiving().posZ + (player.getRNG().nextDouble() - 0.5) * 2,
                            1.0, 0.0, 0.0);
                }

                // Boss/小Boss 掉落能量精华
                if (bossMul > 1.0) {
                    ItemStack orb = new ItemStack(net.minecraft.init.Items.REDSTONE, Math.max(1, level));
                    orb.setStackDisplayName("§c能量精华");
                    EntityItem drop = new EntityItem(event.getEntityLiving().world,
                            event.getEntityLiving().posX,
                            event.getEntityLiving().posY,
                            event.getEntityLiving().posZ,
                            orb);
                    drop.setDefaultPickupDelay();
                    event.getEntityLiving().world.spawnEntity(drop);
                }

                if (DEBUG_MODE) {
                    System.out.println("[CombatCharger] 战斗充能: " + energy + " RF (连杀: x" + streak + ")");
                }
            }

            // 记录最后击杀时间
            player.getEntityData().setLong(NBT_COMBAT_LASTKILL_TICK, currentTime);
        }

        public static void resetStreak(UUID id) {
            combatStreak.remove(id);
            lastKillTime.remove(id);
        }

        public static int getStreak(UUID id) {
            return combatStreak.getOrDefault(id, 0);
        }
    }

    /**
     * 主 Tick：驱动各能源系统
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        ItemStack core = getCore(player);
        if (core.isEmpty()) return;

        // 只有升级安装且有效（等级>0）的情况下才驱动
        if (ItemMechanicalCore.getUpgradeLevel(core, "KINETIC_GENERATOR") > 0) {
            KineticGeneratorSystem.generateFromMovement(player, core);
        }

        if (ItemMechanicalCore.getUpgradeLevel(core, "SOLAR_GENERATOR") > 0) {
            SolarGeneratorSystem.generateFromSunlight(player, core);
        }

        if (ItemMechanicalCore.getUpgradeLevel(core, "VOID_ENERGY") > 0) {
            VoidEnergySystem.generateFromVoid(player, core);
        }

        // 连杀重置检查
        long now = player.world.getTotalWorldTime();
        long lastKill = player.getEntityData().getLong(NBT_COMBAT_LASTKILL_TICK);
        if (now - lastKill > EnergyBalanceConfig.CombatCharger.STREAK_TIMEOUT) {
            CombatChargerSystem.resetStreak(player.getUniqueID());
        }

        // 动能缓冲溢出保护
        int buffer = player.getEntityData().getInteger(NBT_KINETIC_BUFFER);
        int threshold = EnergyBalanceConfig.KineticGenerator.BUFFER_THRESHOLD;
        if (buffer >= threshold * 2) { // 防止缓冲区过大
            ItemMechanicalCore.addEnergy(core, buffer);
            player.getEntityData().setInteger(NBT_KINETIC_BUFFER, 0);

            if (DEBUG_MODE) {
                System.out.println("[KineticGenerator] 缓冲区溢出保护: " + buffer + " RF");
            }
        }

        // 定期状态报告（调试用）
        if (DEBUG_MODE && now % 100 == 0) {
            IEnergyStorage energy = ItemMechanicalCore.getEnergyStorage(core);
            if (energy != null) {
                System.out.println("[EnergyUpgradeManager] 当前能量: " +
                        energy.getEnergyStored() + "/" + energy.getMaxEnergyStored() + " RF");

                if (buffer > 0) {
                    System.out.println("  动能缓冲: " + buffer + " RF");
                }

                int voidCharge = player.getEntityData().getInteger(NBT_VOID_CHARGE);
                if (voidCharge > 0) {
                    System.out.println("  虚空充能: " + voidCharge + "/100");
                }

                int streak = CombatChargerSystem.getStreak(player.getUniqueID());
                if (streak > 0) {
                    System.out.println("  连杀: x" + streak);
                }
            }
        }
    }

    /**
     * 获取所有能源发电状态（用于显示）
     */
    public static String getEnergyGenerationStatus(EntityPlayer player, ItemStack core) {
        StringBuilder status = new StringBuilder();

        // 动能发电
        int kineticLevel = ItemMechanicalCore.getUpgradeLevel(core, "KINETIC_GENERATOR");
        if (kineticLevel > 0) {
            int buffer = player.getEntityData().getInteger(NBT_KINETIC_BUFFER);
            status.append("§b动能: Lv.").append(kineticLevel);
            if (buffer > 0) {
                status.append(" (缓冲: ").append(buffer).append(" RF)");
            }
            status.append("\n");
        }

        // 太阳能发电
        int solarLevel = ItemMechanicalCore.getUpgradeLevel(core, "SOLAR_GENERATOR");
        if (solarLevel > 0) {
            status.append("§e太阳能: Lv.").append(solarLevel);
            if (player.world.isDaytime() && player.world.canSeeSky(player.getPosition())) {
                status.append(" §a[激活]");
            } else {
                status.append(" §7[待机]");
            }
            status.append("\n");
        }

        // 虚空能量
        int voidLevel = ItemMechanicalCore.getUpgradeLevel(core, "VOID_ENERGY");
        if (voidLevel > 0) {
            int charge = player.getEntityData().getInteger(NBT_VOID_CHARGE);
            status.append("§5虚空: Lv.").append(voidLevel);
            status.append(" (充能: ").append(charge).append("/100)");
            status.append("\n");
        }

        // 战斗充能
        int combatLevel = ItemMechanicalCore.getUpgradeLevel(core, "COMBAT_CHARGER");
        if (combatLevel > 0) {
            int streak = CombatChargerSystem.getStreak(player.getUniqueID());
            status.append("§c战斗: Lv.").append(combatLevel);
            if (streak > 0) {
                status.append(" (连杀: x").append(streak).append(")");
            }
            status.append("\n");
        }

        return status.toString();
    }

    /**
     * 重置玩家的能源发电数据（用于死亡等情况）
     */
    public static void resetPlayerData(EntityPlayer player) {
        // 清除位置数据
        player.getEntityData().removeTag(NBT_LAST_POS_X);
        player.getEntityData().removeTag(NBT_LAST_POS_Y);
        player.getEntityData().removeTag(NBT_LAST_POS_Z);

        // 清除缓冲数据
        player.getEntityData().removeTag(NBT_KINETIC_BUFFER);
        player.getEntityData().removeTag(NBT_VOID_CHARGE);

        // 清除时间数据
        player.getEntityData().removeTag(NBT_SOLAR_TICK);
        player.getEntityData().removeTag(NBT_COMBAT_LASTKILL_TICK);

        // 重置连杀
        CombatChargerSystem.resetStreak(player.getUniqueID());

        if (DEBUG_MODE) {
            System.out.println("[EnergyUpgradeManager] 重置玩家 " + player.getName() + " 的能源数据");
        }
    }
}