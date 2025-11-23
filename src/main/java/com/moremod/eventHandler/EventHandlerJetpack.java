package com.moremod.eventHandler;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemJetpackBauble;
import com.moremod.item.ItemCreativeJetpackBauble;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.upgrades.EnergyEfficiencyManager;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 喷气背包/机械核心 - 仅用推力的飞行实现：
 * - 不拦截客户端双击空格
 * - 不授予 allowFlying/isFlying
 * - 不强制关闭玩家的创造/原版飞行（仅管理自身设备与能量）
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class EventHandlerJetpack {

    // ========== 性能配置 ==========
    private static final class PerformanceConfig {
        public static int PARTICLE_QUALITY = 2;
        public static final double PARTICLE_RENDER_DISTANCE = 32.0;
        public static final double PARTICLE_RENDER_DISTANCE_SQ = PARTICLE_RENDER_DISTANCE * PARTICLE_RENDER_DISTANCE;
        public static final int ENERGY_CHECK_INTERVAL = 10;   // 每 10tick 检查能量
        public static final int STATUS_MESSAGE_INTERVAL = 40; // 提示频率
    }

    private static final int HUD_SYNC_INTERVAL = 2;
    private static final Map<UUID, Integer> lastSyncedEnergy = new HashMap<>();

    // ========== 键位/状态 ==========
    public static final Map<UUID, Boolean> jetpackJumping  = new ConcurrentHashMap<>();
    public static final Map<UUID, Boolean> jetpackSneaking = new ConcurrentHashMap<>();

    public static final Map<UUID, Boolean> playerFlying        = new HashMap<>();
    public static final Map<UUID, Boolean> jetpackActivelyUsed = new HashMap<>();
    public static final Map<UUID, Long>    lastJetpackUseTime  = new HashMap<>();
    public static final Map<UUID, Double>  hoverAscendSpeeds   = new HashMap<>();

    // ========== 装备缓存 ==========
    private static class PlayerCache {
        boolean  hasFlightDevice = false;
        boolean  isDeviceActive  = false;
        ItemStack activeDevice   = ItemStack.EMPTY;
        int      deviceTier      = 0;
        double   ascendSpeed     = 0;
        double   descendSpeed    = 0;
        double   moveSpeed       = 0;
        int      energyPerTick   = 0;
        long     lastUpdate      = 0;

        boolean isValid(long worldTime) { return false; } // 不使用缓存复用，每tick更新
    }
    public static final Map<UUID, PlayerCache> playerCaches = new HashMap<>();

    // ========== 能量同步 ==========
    private static void syncBaublesEnergyToClient(EntityPlayer player, ItemStack stack, int energy, long worldTime) {
        if (player.world.isRemote) return;
        if (worldTime % HUD_SYNC_INTERVAL != 0) return;

        UUID id = player.getUniqueID();
        Integer last = lastSyncedEnergy.get(id);
        if (last != null && last == energy) return;

        try {
            IBaublesItemHandler h = BaublesApi.getBaublesHandler(player);
            if (h != null) {
                for (int i = 0; i < h.getSlots(); i++) {
                    ItemStack s = h.getStackInSlot(i);
                    if (s.isEmpty()) continue;
                    if (s == stack || s.getItem() == stack.getItem()) {
                        h.setStackInSlot(i, stack.copy());
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {}
        lastSyncedEnergy.put(id, energy);
    }

    // ========== 设备强制关闭（仅修改设备 NBT；不触碰玩家飞行能力） ==========
    private static void forcedDisableMechanicalCore(ItemStack stack, EntityPlayer player) {
        if (!ItemMechanicalCore.isMechanicalCore(stack)) return;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) { nbt = new NBTTagCompound(); stack.setTagCompound(nbt); }
        nbt.setBoolean("FlightModuleEnabled", false);
        nbt.setBoolean("FlightHoverMode", false);

        if (!player.world.isRemote) {
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.RED + "⚡ 飞行模块能量耗尽，已强制关闭！"
            ), true);
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    net.minecraft.init.SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT,
                    net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 0.5F);
        }

        IEnergyStorage es = stack.getCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);
        if (es != null) {
            syncBaublesEnergyToClient(player, stack, es.getEnergyStored(), player.world.getTotalWorldTime());
        }
    }

    private static void forceDisableJetpack(ItemStack stack, EntityPlayer player) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) { tag = new NBTTagCompound(); stack.setTagCompound(tag); }
        tag.setBoolean("JetpackEnabled", false);
        tag.setBoolean("HoverEnabled", false);

        if (!player.world.isRemote) {
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                    net.minecraft.util.text.TextFormatting.RED + "⚡ 喷气背包能量耗尽，已强制关闭！"
            ), true);
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    net.minecraft.init.SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT,
                    net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 0.5F);
        }

        IEnergyStorage es = null;
        if (stack.getItem() instanceof ItemJetpackBauble) {
            es = ItemJetpackBauble.getEnergyStorage(stack);
        } else if (stack.getItem() instanceof ItemCreativeJetpackBauble) {
            es = ItemCreativeJetpackBauble.getEnergyStorage(stack);
        }
        if (es != null) {
            syncBaublesEnergyToClient(player, stack, es.getEnergyStored(), player.world.getTotalWorldTime());
        }
    }

    // ========== 能量检测 ==========
    private static boolean checkAndHandleEnergyDepletion(EntityPlayer player, ItemStack stack, long worldTime) {
        if (worldTime % PerformanceConfig.ENERGY_CHECK_INTERVAL != 0) return false;

        if (ItemMechanicalCore.isMechanicalCore(stack)) {
            int flightLevel = ItemMechanicalCore.getUpgradeLevel(stack,
                    ItemMechanicalCore.UpgradeType.FLIGHT_MODULE);
            if (flightLevel > 0) {
                NBTTagCompound nbt = stack.getTagCompound();
                if (nbt != null && nbt.getBoolean("FlightModuleEnabled")) {
                    IEnergyStorage energy = stack.getCapability(
                            net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);
                    if (energy == null || energy.getEnergyStored() <= 0) {
                        forcedDisableMechanicalCore(stack, player);
                        cleanupFlightState(player);
                        return true;
                    }
                }
            }
        }

        if (stack.getItem() instanceof ItemJetpackBauble) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null && tag.getBoolean("JetpackEnabled")) {
                IEnergyStorage energy = ItemJetpackBauble.getEnergyStorage(stack);
                if (energy == null || energy.getEnergyStored() <= 0) {
                    forceDisableJetpack(stack, player);
                    cleanupFlightState(player);
                    return true;
                }
            }
        }
        return false;
    }

    private static void onEnergyDepletionEffects(EntityPlayer player) {
        if (!player.world.isRemote) {
            for (int i = 0; i < 5; i++) {
                player.world.spawnParticle(EnumParticleTypes.LAVA,
                        player.posX + (player.world.rand.nextDouble() - 0.5) * 0.5,
                        player.posY + 1.0,
                        player.posZ + (player.world.rand.nextDouble() - 0.5) * 0.5,
                        (player.world.rand.nextDouble() - 0.5) * 0.1,
                        player.world.rand.nextDouble() * 0.2,
                        (player.world.rand.nextDouble() - 0.5) * 0.1);
            }
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    net.minecraft.init.SoundEvents.ENTITY_LIGHTNING_THUNDER,
                    net.minecraft.util.SoundCategory.PLAYERS, 0.3F, 2.0F);
        }
    }

    // ========== 仅更新推力飞行状态（不改能力位） ==========
    public static void updatePlayerFlightState(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) return;

        UUID playerId = player.getUniqueID();
        boolean jumping = Boolean.TRUE.equals(jetpackJumping.get(playerId));
        boolean sneaking = Boolean.TRUE.equals(jetpackSneaking.get(playerId));

        PlayerCache cache = playerCaches.computeIfAbsent(playerId, k -> new PlayerCache());
        updatePlayerEquipmentCache(player, cache);

        boolean shouldFly = cache.hasFlightDevice && cache.isDeviceActive && (jumping || sneaking);
        playerFlying.put(playerId, shouldFly);
    }
    public static void updatePlayerFlightState(EntityPlayerMP player) { updatePlayerFlightState((EntityPlayer) player); }

    // ========== 装备缓存更新（不触碰能力位） ==========
    private static void updatePlayerEquipmentCache(EntityPlayer player, PlayerCache cache) {
        cache.hasFlightDevice = false;
        cache.isDeviceActive  = false;
        cache.activeDevice    = ItemStack.EMPTY;
        cache.lastUpdate      = player.world.getTotalWorldTime();

        IBaublesItemHandler h = BaublesApi.getBaublesHandler(player);
        if (h == null) return;

        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack stack = h.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // 喷气背包
            if (stack.getItem() instanceof ItemJetpackBauble ||
                    stack.getItem() instanceof ItemCreativeJetpackBauble) {

                cache.hasFlightDevice = true;
                cache.activeDevice    = stack;
                NBTTagCompound tag    = stack.getTagCompound();

                cache.isDeviceActive = (tag != null && tag.getBoolean("JetpackEnabled"));

                if (cache.isDeviceActive && stack.getItem() instanceof ItemJetpackBauble) {
                    ItemJetpackBauble jetpack = (ItemJetpackBauble) stack.getItem();
                    cache.deviceTier    = jetpack.getTier();
                    cache.ascendSpeed   = jetpack.getActualAscendSpeed(stack);
                    cache.descendSpeed  = jetpack.getActualDescendSpeed(stack);
                    cache.moveSpeed     = jetpack.getActualMoveSpeed(stack);
                    cache.energyPerTick = jetpack.getActualEnergyPerTick(stack);
                }
                break;
            }

            // 机械核心
            if (ItemMechanicalCore.isMechanicalCore(stack)) {
                int flightLevel = ItemMechanicalCore.getUpgradeLevel(stack,
                        ItemMechanicalCore.UpgradeType.FLIGHT_MODULE);
                if (flightLevel > 0) {
                    cache.hasFlightDevice = true;
                    cache.activeDevice    = stack;
                    NBTTagCompound nbt    = stack.getTagCompound();

                    cache.isDeviceActive = (nbt != null && nbt.getBoolean("FlightModuleEnabled"));

                    if (cache.isDeviceActive) {
                        cache.deviceTier    = flightLevel;
                        cache.ascendSpeed   = 0.15 * flightLevel;
                        cache.descendSpeed  = 0.05 * flightLevel;
                        cache.moveSpeed     = 0.05 * flightLevel;
                        cache.energyPerTick = 30 * flightLevel;
                    } else {
                        cache.deviceTier = 0;
                        cache.ascendSpeed = cache.descendSpeed = cache.moveSpeed = 0;
                        cache.energyPerTick = 0;
                    }
                    break;
                }
            }
        }
    }

    // ========== 受伤/攻击事件：使用中保护摔落与卡墙 ==========
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        DamageSource source = event.getSource();

        UUID playerId = player.getUniqueID();
        PlayerCache cache = playerCaches.get(playerId);
        if (cache == null || !cache.hasFlightDevice || !cache.isDeviceActive) return;

        boolean jumping = Boolean.TRUE.equals(jetpackJumping.get(playerId));
        boolean sneaking = Boolean.TRUE.equals(jetpackSneaking.get(playerId));
        Boolean activelyUsed = jetpackActivelyUsed.get(playerId);
        boolean isUsing = jumping || sneaking || (activelyUsed != null && activelyUsed);

        if (!isUsing) return;

        if (source == DamageSource.FLY_INTO_WALL ||
                source == DamageSource.FALL ||
                source == DamageSource.IN_WALL) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        DamageSource source = event.getSource();

        if (source != DamageSource.FLY_INTO_WALL &&
                source != DamageSource.FALL &&
                source != DamageSource.IN_WALL) {
            return;
        }

        UUID playerId = player.getUniqueID();
        boolean jumping = Boolean.TRUE.equals(jetpackJumping.get(playerId));
        boolean sneaking = Boolean.TRUE.equals(jetpackSneaking.get(playerId));
        Boolean activelyUsed = jetpackActivelyUsed.get(playerId);
        boolean isUsing = jumping || sneaking || (activelyUsed != null && activelyUsed);
        if (!isUsing) return;

        PlayerCache cache = playerCaches.get(playerId);
        if (cache != null && cache.hasFlightDevice && cache.isDeviceActive) {
            event.setCanceled(true);
        }
    }

    // ========== 客户端：仅上报按键状态（不拦截、不中断、不过滤双击） ==========
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;

        UUID playerId = mc.player.getUniqueID();
        if (mc.world.getTotalWorldTime() % 2 != 0) return;

        // 仍然更新缓存（用于本地粒子/能量判断）
        PlayerCache cache = playerCaches.computeIfAbsent(playerId, k -> new PlayerCache());
        updatePlayerEquipmentCache(mc.player, cache);

        boolean isJumping  = mc.gameSettings.keyBindJump.isKeyDown();
        boolean isSneaking = mc.gameSettings.keyBindSneak.isKeyDown();

        Boolean lastJump  = jetpackJumping.get(playerId);
        Boolean lastSneak = jetpackSneaking.get(playerId);

        boolean jumpChanged  = (lastJump == null  || lastJump  != isJumping);
        boolean sneakChanged = (lastSneak == null || lastSneak != isSneaking);

        if (jumpChanged)  jetpackJumping.put(playerId, isJumping);
        if (sneakChanged) jetpackSneaking.put(playerId, isSneaking);

        if (jumpChanged || sneakChanged) {
            net.minecraftforge.fml.common.network.simpleimpl.IMessage jumpMsg =
                    new com.moremod.network.MessageJetpackJumping(cache.hasFlightDevice && isJumping);
            net.minecraftforge.fml.common.network.simpleimpl.IMessage sneakMsg =
                    new com.moremod.network.MessageJetpackSneaking(cache.hasFlightDevice && isSneaking);

            com.moremod.network.PacketHandler.INSTANCE.sendToServer(jumpMsg);
            com.moremod.network.PacketHandler.INSTANCE.sendToServer(sneakMsg);
        }
    }

    // ========== 服务器：逻辑主循环（仅推力，不动能力位） ==========
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        UUID playerId = player.getUniqueID();
        long worldTime = player.world.getTotalWorldTime();

        // ✅ Set player context for all upgrade reads
        ItemMechanicalCore.setPlayerContext(player);
        try {
            PlayerCache cache = playerCaches.computeIfAbsent(playerId, k -> new PlayerCache());
            updatePlayerEquipmentCache(player, cache);

            if (worldTime % PerformanceConfig.ENERGY_CHECK_INTERVAL == 0 && !cache.activeDevice.isEmpty()) {
                if (checkAndHandleEnergyDepletion(player, cache.activeDevice, worldTime)) {
                    return;
                }
            }

            boolean jumping = Boolean.TRUE.equals(jetpackJumping.get(playerId));
            boolean sneaking = Boolean.TRUE.equals(jetpackSneaking.get(playerId));

            if (!cache.hasFlightDevice || !cache.isDeviceActive) {
                playerFlying.put(playerId, false);
                jetpackActivelyUsed.put(playerId, false);

                // 恢复重力
                player.setNoGravity(false);
                return;
            }

            // 机械核心或喷气背包：统一推力逻辑（分别计算能耗/速度）
            if (ItemMechanicalCore.isMechanicalCore(cache.activeDevice)) {
                handleMechanicalCoreThrust(player, cache, jumping, sneaking, worldTime);
            } else if (cache.activeDevice.getItem() instanceof ItemJetpackBauble ||
                    cache.activeDevice.getItem() instanceof ItemCreativeJetpackBauble) {
                handleJetpackThrust(player, cache, jumping, sneaking, worldTime);
            }
        } finally {
            // ✅ Clear player context
            ItemMechanicalCore.clearPlayerContext();
        }
    }

    // ========== 机械核心推力（不授予/不关闭飞行能力位） ==========
    private static void handleMechanicalCoreThrust(EntityPlayer player, PlayerCache cache,
                                                   boolean jumping, boolean sneaking, long worldTime) {
        UUID playerId = player.getUniqueID();
        ItemStack stack = cache.activeDevice;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
            nbt.setBoolean("FlightModuleEnabled", false);
        }

        if (!nbt.getBoolean("FlightModuleEnabled")) {
            playerFlying.put(playerId, false);
            jetpackActivelyUsed.put(playerId, false);
            cache.isDeviceActive = false;
            cache.deviceTier = 0;
            player.setNoGravity(false);
            return;
        }

        int flightLevel = ItemMechanicalCore.getUpgradeLevel(stack, ItemMechanicalCore.UpgradeType.FLIGHT_MODULE);
        boolean hover = nbt.getBoolean("FlightHoverMode");
        if (flightLevel == 1) {
            hover = false;
            nbt.setBoolean("FlightHoverMode", false);
        }

        // 检查悬停状态变化
        boolean wasHovering = nbt.getBoolean("WasHovering");
        boolean currentlyHovering = hover && flightLevel > 1 && !jumping && !sneaking;

        if (wasHovering != currentlyHovering) {
            if (!currentlyHovering) {
                player.setNoGravity(false);
            }
            nbt.setBoolean("WasHovering", currentlyHovering);
        }

        if (!(jumping || sneaking || (hover && flightLevel > 1))) {
            playerFlying.put(playerId, false);
            jetpackActivelyUsed.put(playerId, false);
            player.setNoGravity(false);
            return;
        }

        IEnergyStorage energy = stack.getCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);
        if (energy == null || energy.getEnergyStored() <= 0) {
            forcedDisableMechanicalCore(stack, player);
            cleanupFlightState(player);
            return;
        }

        double ascendSpeed  = 0.15 * flightLevel;
        double descendSpeed = 0.05 * flightLevel;
        double moveSpeed    = 0.1 * flightLevel;

        int baseEnergyCost   = 30 * flightLevel;
        int actualEnergyCost = EnergyEfficiencyManager.calculateActualCost(player, baseEnergyCost);
        if (hover && !jumping && !sneaking && flightLevel > 1) {
            actualEnergyCost = EnergyEfficiencyManager.calculateActualCost(player, baseEnergyCost * 2);
        }

        if (energy.extractEnergy(actualEnergyCost, true) < actualEnergyCost) {
            if (worldTime % PerformanceConfig.STATUS_MESSAGE_INTERVAL == 0 && !player.world.isRemote) {
                player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.RED + "⚡ 飞行模块能量不足！需要 " + actualEnergyCost + " FE"
                ), true);
            }
            forcedDisableMechanicalCore(stack, player);
            cleanupFlightState(player);
            return;
        }

        energy.extractEnergy(actualEnergyCost, false);
        syncBaublesEnergyToClient(player, stack, energy.getEnergyStored(), worldTime);

        boolean isFlying = false;
        boolean isHovering = false;

        if (hover && flightLevel > 1) {
            isHovering = true;
            if (jumping && player.motionY < ascendSpeed * 2.5) {
                player.motionY += ascendSpeed;
                isFlying = true;
                player.setNoGravity(false);
            } else if (sneaking && player.motionY > -descendSpeed * 2.0) {
                player.motionY -= descendSpeed;
                isFlying = true;
                player.setNoGravity(false);
            } else if (!jumping && !sneaking) {
                player.motionY = 0;
                player.fallDistance = 0;
                player.setNoGravity(true);
                isFlying = true;
            }
        } else {
            player.setNoGravity(false);
            if (jumping && player.motionY < ascendSpeed * 2.5) {
                player.motionY += ascendSpeed;
                isFlying = true;
            }
            if (sneaking && player.motionY > -descendSpeed * 2.0) {
                player.motionY -= descendSpeed * 0.7;
                isFlying = true;
            }
        }

        applyHorizontalThrust(player, moveSpeed);

        playerFlying.put(playerId, true);
        jetpackActivelyUsed.put(playerId, true);
        lastJetpackUseTime.put(playerId, worldTime);

        if (player.world.isRemote && worldTime % 2 == 0) {
            spawnOptimizedMechanicalCoreParticles(player, stack, isHovering, isFlying,
                    ascendSpeed, energy.getEnergyStored(), energy.getMaxEnergyStored());
        }
    }

    // ========== 喷气背包推力（不授予/不关闭飞行能力位） ==========
    private static void handleJetpackThrust(EntityPlayer player, PlayerCache cache,
                                            boolean jumping, boolean sneaking, long worldTime) {
        UUID playerId = player.getUniqueID();
        ItemStack stack = cache.activeDevice;

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) { tag = new NBTTagCompound(); stack.setTagCompound(tag); }

        boolean enabled = tag.getBoolean("JetpackEnabled");
        boolean hover   = tag.getBoolean("HoverEnabled");
        if (cache.deviceTier < 2) { hover = false; tag.setBoolean("HoverEnabled", false); }

        if (!(enabled && (jumping || (hover && cache.deviceTier >= 2) || sneaking))) {
            playerFlying.put(playerId, false);
            jetpackActivelyUsed.put(playerId, false);
            player.setNoGravity(false); // 添加这行
            return;
        }

        IEnergyStorage energy = null;
        if (stack.getItem() instanceof ItemJetpackBauble) {
            energy = ItemJetpackBauble.getEnergyStorage(stack);
        } else if (stack.getItem() instanceof ItemCreativeJetpackBauble) {
            energy = ItemCreativeJetpackBauble.getEnergyStorage(stack);
        }

        boolean canUseEnergy = true;
        int actualEnergyPerTick = cache.energyPerTick;

        if (stack.getItem() instanceof ItemJetpackBauble && energy != null) {
            actualEnergyPerTick = EnergyEfficiencyManager.calculateActualCost(player, cache.energyPerTick);
            canUseEnergy = energy.extractEnergy(actualEnergyPerTick, true) >= actualEnergyPerTick;
        }

        if (!canUseEnergy) {
            if (tag.getBoolean("JetpackEnabled")) {
                forceDisableJetpack(stack, player);
            }
            cleanupFlightState(player);
            return;
        }

        if (energy != null) {
            energy.extractEnergy(actualEnergyPerTick, false);
            syncBaublesEnergyToClient(player, stack, energy.getEnergyStored(), worldTime);
        }

        double effectiveAscendSpeed = hover ? cache.ascendSpeed * 1.3 : cache.ascendSpeed;
        boolean isFlying = false, isHovering = false;
        if (hover && cache.deviceTier >= 2) {
            isHovering = true;
            if (jumping && player.motionY < effectiveAscendSpeed * 2.5) {
                player.motionY += effectiveAscendSpeed;
                isFlying = true;
            } else if (sneaking && player.motionY > -cache.descendSpeed * 2.0) {
                player.motionY -= cache.descendSpeed;
                isFlying = true;
            } else if (!jumping && !sneaking) {
                // 完全悬停
                player.motionY = 0;
                player.fallDistance = 0;
                isFlying = true;
            }
        } else {
            if (jumping && player.motionY < effectiveAscendSpeed * 2.5) { player.motionY += effectiveAscendSpeed; isFlying = true; }
            if (sneaking && player.motionY > -cache.descendSpeed * 2.0) { player.motionY -= cache.descendSpeed * 0.7; isFlying = true; }
        }

// 悬停时需要每tick重置motionY来对抗重力
        if (isHovering && !jumping && !sneaking) {
            player.setNoGravity(true);  // 暂时禁用重力
        } else {
            player.setNoGravity(false); // 恢复重力
        }

        applyHorizontalThrust(player, cache.moveSpeed);

        playerFlying.put(playerId, true);
        jetpackActivelyUsed.put(playerId, true);
        lastJetpackUseTime.put(playerId, worldTime);

        if (player.world.isRemote && worldTime % 2 == 0) {
            handleOptimizedJetpackVisualEffects(player, stack, isHovering, isFlying, effectiveAscendSpeed);
        }
    }

    // ========== 清理（仅内部状态，不动能力位） ==========
    public static void cleanupFlightState(EntityPlayer player) {
        UUID id = player.getUniqueID();
        playerFlying.put(id, false);
        jetpackActivelyUsed.put(id, false);
        hoverAscendSpeeds.remove(id);
        lastJetpackUseTime.remove(id);

        // 恢复重力
        player.setNoGravity(false);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        UUID id = event.getEntityPlayer().getUniqueID();
        jetpackJumping.remove(id);
        jetpackSneaking.remove(id);
        playerFlying.remove(id);
        hoverAscendSpeeds.remove(id);
        jetpackActivelyUsed.remove(id);
        lastJetpackUseTime.remove(id);
        playerCaches.remove(id);
        lastSyncedEnergy.remove(id);
    }

    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;
        UUID id = player.getUniqueID();

        jetpackJumping.remove(id);
        jetpackSneaking.remove(id);
        playerFlying.remove(id);
        jetpackActivelyUsed.remove(id);
        lastJetpackUseTime.remove(id);
        hoverAscendSpeeds.remove(id);
        playerCaches.remove(id);
        lastSyncedEnergy.remove(id);
    }

    // ========== 监听交互：刷新缓存；若手持机械核心且模块关，则清理内部状态 ==========
    @SubscribeEvent
    public static void onItemNBTChange(PlayerInteractEvent event) {
        if (event.getEntityPlayer() == null) return;

        EntityPlayer player = event.getEntityPlayer();
        UUID id = player.getUniqueID();

        PlayerCache cache = playerCaches.get(id);
        if (cache != null) cache.lastUpdate = 0;

        ItemStack stack = player.getHeldItemMainhand();
        if (ItemMechanicalCore.isMechanicalCore(stack)) {
            NBTTagCompound nbt = stack.getTagCompound();
            if (nbt != null && !nbt.getBoolean("FlightModuleEnabled")) {
                cleanupFlightState(player);
            }
        }
    }

    // ========== 世界Tick：能量低/耗尽提示与设备关闭（不动能力位） ==========
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
        if (event.world.getTotalWorldTime() % 20 != 0) return;

        for (EntityPlayer player : event.world.playerEntities) {
            if (player.isSpectator()) continue;

            UUID id = player.getUniqueID();
            PlayerCache cache = playerCaches.get(id);
            if (cache == null || !cache.hasFlightDevice) continue;

            ItemStack stack = cache.activeDevice;
            if (stack.isEmpty()) continue;

            IEnergyStorage energy = null;
            if (ItemMechanicalCore.isMechanicalCore(stack)) {
                energy = stack.getCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);
            } else if (stack.getItem() instanceof ItemJetpackBauble) {
                energy = ItemJetpackBauble.getEnergyStorage(stack);
            } else if (stack.getItem() instanceof ItemCreativeJetpackBauble) {
                energy = ItemCreativeJetpackBauble.getEnergyStorage(stack);
            }

            if (energy != null) {
                int stored = energy.getEnergyStored();
                int max = energy.getMaxEnergyStored();

                if (stored > 0 && stored < max * 0.1 && event.world.getTotalWorldTime() % 100 == 0) {
                    player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                            net.minecraft.util.text.TextFormatting.YELLOW +
                                    "⚠ 飞行设备能量低于10%！剩余: " + stored + "/" + max + " FE"
                    ), true);
                    player.world.playSound(null, player.posX, player.posY, player.posZ,
                            net.minecraft.init.SoundEvents.BLOCK_NOTE_PLING,
                            net.minecraft.util.SoundCategory.PLAYERS, 0.5F, 0.5F);
                }

                if (stored <= 0) {
                    if (ItemMechanicalCore.isMechanicalCore(stack)) forcedDisableMechanicalCore(stack, player);
                    else                                             forceDisableJetpack(stack, player);
                    cleanupFlightState(player);
                    onEnergyDepletionEffects(player);
                }
            }
        }
    }

    // ========== 粒子 ==========
    private static int getParticleCount(int baseCount, double distance) {
        if (PerformanceConfig.PARTICLE_QUALITY == 0) return 0;
        float quality = PerformanceConfig.PARTICLE_QUALITY / 3.0f;
        float distMul = distance > 16 ? 0.5f : (distance > 8 ? 0.75f : 1.0f);
        return Math.max(1, (int)(baseCount * quality * distMul));
    }

    @SideOnly(Side.CLIENT)
    private static void handleOptimizedJetpackVisualEffects(EntityPlayer player, ItemStack jetpack,
                                                            boolean isHovering, boolean isFlying, double ascendSpeed) {
        if (PerformanceConfig.PARTICLE_QUALITY == 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        double distanceSq = mc.player.getDistanceSq(player);
        if (distanceSq > PerformanceConfig.PARTICLE_RENDER_DISTANCE_SQ) return;
        spawnOptimizedJetpackParticles(player, jetpack, isHovering, isFlying, ascendSpeed, Math.sqrt(distanceSq));
    }

    @SideOnly(Side.CLIENT)
    private static void spawnOptimizedJetpackParticles(EntityPlayer player, ItemStack jetpack,
                                                       boolean isHovering, boolean isFlying,
                                                       double ascendSpeed, double distance) {
        World world = player.world;
        double x = player.posX, y = player.posY, z = player.posZ;

        if (isHovering && !isFlying) {
            spawnOptimizedHoverParticles(world, player, x, y, z, distance);
        }
        if (isFlying) {
            float throttle = (float)Math.min(ascendSpeed * 2.0, 1.0);
            spawnOptimizedThrusterParticles(world, player, x, y, z, jetpack, throttle, distance);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void spawnOptimizedHoverParticles(World world, EntityPlayer player,
                                                     double x, double y, double z, double distance) {
        int particleCount = getParticleCount(2, distance);
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5) * 0.6;
            double offsetZ = (world.rand.nextDouble() - 0.5) * 0.6;
            double offsetY = world.rand.nextDouble() * 0.2;
            world.spawnParticle(EnumParticleTypes.CLOUD,
                    x + offsetX, y + 0.2 + offsetY, z + offsetZ,
                    (world.rand.nextDouble() - 0.5) * 0.02,
                    -0.05,
                    (world.rand.nextDouble() - 0.5) * 0.02);
        }
        if (PerformanceConfig.PARTICLE_QUALITY >= 2 && world.rand.nextInt(30) == 0) {
            world.spawnParticle(EnumParticleTypes.END_ROD, x, y + 0.6, z, 0, 0, 0);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void spawnOptimizedThrusterParticles(World world, EntityPlayer player,
                                                        double x, double y, double z,
                                                        ItemStack jetpack, float throttle, double distance) {
        float yaw = player.rotationYaw;
        double yawRad = Math.toRadians(yaw);

        double backOffset = 0.65;
        double heightOffset = player.height * 0.4;

        double backX = Math.sin(yawRad) * backOffset;
        double backZ = -Math.cos(yawRad) * backOffset;

        double jetpackWidth = 0.25;
        double lrX = Math.cos(yawRad);
        double lrZ = Math.sin(yawRad);

        double leftX = x + backX + lrX * jetpackWidth;
        double leftY = y + heightOffset;
        double leftZ = z + backZ + lrZ * jetpackWidth;

        double rightX = x + backX - lrX * jetpackWidth;
        double rightY = y + heightOffset;
        double rightZ = z + backZ - lrZ * jetpackWidth;

        int baseCount = Math.max(2, (int)(throttle * 4));
        int particleCount = getParticleCount(baseCount, distance);

        EnumParticleTypes flameType = EnumParticleTypes.FLAME;
        double jetVX = Math.sin(yawRad) * 0.3;
        double jetVY = -0.5;
        double jetVZ = -Math.cos(yawRad) * 0.3;

        for (int i = 0; i < particleCount; i++) {
            double r = 0.05;
            double ox = (world.rand.nextDouble() - 0.5) * r;
            double oy = (world.rand.nextDouble() - 0.5) * r;
            double oz = (world.rand.nextDouble() - 0.5) * r;

            double vv = 0.1;
            double vx = jetVX + (world.rand.nextDouble() - 0.5) * vv;
            double vy = jetVY + (world.rand.nextDouble() - 0.5) * vv * 0.5;
            double vz = jetVZ + (world.rand.nextDouble() - 0.5) * vv;

            world.spawnParticle(flameType, leftX + ox, leftY + oy, leftZ + oz,  vx, vy, vz);
            world.spawnParticle(flameType, rightX + ox, rightY + oy, rightZ + oz, vx, vy, vz);
        }

        if (PerformanceConfig.PARTICLE_QUALITY >= 2 && world.rand.nextInt(8) == 0) {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    leftX, leftY - 0.1, leftZ, jetVX * 0.2, jetVY * 0.3, jetVZ * 0.2);
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    rightX, rightY - 0.1, rightZ, jetVX * 0.2, jetVY * 0.3, jetVZ * 0.2);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void spawnOptimizedMechanicalCoreParticles(EntityPlayer player, ItemStack coreStack,
                                                              boolean isHovering, boolean isFlying,
                                                              double ascendSpeed, int currentEnergy, int maxEnergy) {
        if (PerformanceConfig.PARTICLE_QUALITY == 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        double distanceSq = mc.player.getDistanceSq(player);
        if (distanceSq > PerformanceConfig.PARTICLE_RENDER_DISTANCE_SQ) return;

        World world = player.world;
        double x = player.posX, y = player.posY, z = player.posZ;
        double distance = Math.sqrt(distanceSq);

        int flightLevel = ItemMechanicalCore.getUpgradeLevel(coreStack,
                ItemMechanicalCore.UpgradeType.FLIGHT_MODULE);
        float energyPercent = (float) currentEnergy / Math.max(1, maxEnergy);

        if (isHovering && !isFlying) {
            int ringCount = getParticleCount(6, distance);
            for (int i = 0; i < ringCount; i++) {
                double angle = (world.getTotalWorldTime() + i * (360.0 / ringCount)) * 0.1;
                double radius = 0.8;
                double ox = Math.cos(angle) * radius;
                double oz = Math.sin(angle) * radius;

                if (energyPercent > 0.6f) {
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB, x + ox, y + 0.5, z + oz, 0.0, 0.8, 1.0);
                } else if (energyPercent > 0.3f) {
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB, x + ox, y + 0.5, z + oz, 1.0, 1.0, 0.0);
                } else if (world.getTotalWorldTime() % 10 < 5) {
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB, x + ox, y + 0.5, z + oz, 1.0, 0.0, 0.0);
                }
            }
        }

        if (isFlying) {
            float yaw = player.rotationYaw;
            double yawRad = Math.toRadians(yaw);

            double backOffset = 0.7;
            double heightOffset = player.height * 0.45;

            double tx = x + Math.sin(yawRad) * backOffset;
            double ty = y + heightOffset;
            double tz = z - Math.cos(yawRad) * backOffset;

            int base = energyPercent > 0.3f ? 4 : 2;
            int count = getParticleCount(base * Math.max(1, flightLevel), distance);

            double vX = Math.sin(yawRad) * 0.4;
            double vY = -0.6;
            double vZ = -Math.cos(yawRad) * 0.4;

            for (int i = 0; i < count; i++) {
                double spread = 0.15;
                double ox = (world.rand.nextDouble() - 0.5) * spread;
                double oy = (world.rand.nextDouble() - 0.5) * spread * 0.5;
                double oz = (world.rand.nextDouble() - 0.5) * spread;

                if (energyPercent > 0.6f) {
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB, tx + ox, ty + oy, tz + oz, 0.0, 0.8, 1.0);
                } else if (energyPercent > 0.3f) {
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB, tx + ox, ty + oy, tz + oz, 1.0, 1.0, 0.0);
                } else {
                    world.spawnParticle(EnumParticleTypes.SPELL_MOB, tx + ox, ty + oy, tz + oz, 1.0, 0.0, 0.0);
                }

                if (i % 2 == 0) {
                    world.spawnParticle(EnumParticleTypes.FLAME,
                            tx + ox, ty + oy, tz + oz,
                            vX + ox * 0.05, vY + world.rand.nextDouble() * 0.1, vZ + oz * 0.05);
                }
            }

            if (PerformanceConfig.PARTICLE_QUALITY >= 2 && world.rand.nextInt(6) == 0) {
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, tx, ty - 0.2, tz, vX * 0.3, vY * 0.3, vZ * 0.3);
            }
        }
    }

    // ========== 推进水平速度 ==========
    private static void applyHorizontalThrust(EntityPlayer player, double speed) {
        float yaw = player.rotationYaw;
        double yawRad = Math.toRadians(yaw);
        double forward = player.moveForward;
        double strafe  = player.moveStrafing;
        double dz = -(forward * Math.cos(yawRad) + strafe * Math.sin(yawRad)) * speed;
        double dx =  (forward * Math.sin(yawRad) - strafe * Math.cos(yawRad)) * speed;
        player.motionX -= dx;
        player.motionZ -= dz;
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new EventHandlerJetpack());
    }
}
