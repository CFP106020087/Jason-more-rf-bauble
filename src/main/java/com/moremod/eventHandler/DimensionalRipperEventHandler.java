package com.moremod.handler;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemDimensionalRipper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/**
 * 维度撕裂者的事件处理器
 * 处理传送门维护、实体传送冷却等
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class DimensionalRipperEventHandler {

    // 实体传送冷却管理
    private static final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private static final int TELEPORT_COOLDOWN = 40; // 2秒冷却

    // 传送门保护
    private static final Map<BlockPos, Long> protectedPortalBlocks = new HashMap<>();

    /**
     * 处理实体传送冷却
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();

        // 重置NoGravity标记（用作临时传送标记）
        if (entity.hasNoGravity()) {
            UUID uuid = entity.getUniqueID();
            Long cooldownTime = teleportCooldowns.get(uuid);

            if (cooldownTime == null) {
                // 设置冷却
                teleportCooldowns.put(uuid, entity.world.getTotalWorldTime() + TELEPORT_COOLDOWN);
                return;
            }

            // 检查冷却是否结束
            if (entity.world.getTotalWorldTime() >= cooldownTime) {
                entity.setNoGravity(false);
                teleportCooldowns.remove(uuid);
            }
        }
    }

    /**
     * 防止传送门方块被破坏
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockPos pos = event.getPos();

        // 检查是否是受保护的传送门方块
        Long protectionTime = protectedPortalBlocks.get(pos);
        if (protectionTime != null) {
            long currentTime = event.getWorld().getTotalWorldTime();

            if (currentTime < protectionTime) {
                // 传送门还在保护期内
                event.setCanceled(true);

                if (event.getPlayer() != null) {
                    event.getPlayer().sendStatusMessage(
                            new net.minecraft.util.text.TextComponentString(
                                    net.minecraft.util.text.TextFormatting.RED +
                                            "⚠ 维度裂隙无法被破坏"), true);
                }
            } else {
                // 保护期已过，移除记录
                protectedPortalBlocks.remove(pos);
            }
        }
    }

    /**
     * 处理维度传送事件
     */
    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();

        // 检查是否正在通过维度撕裂者传送
        if (entity.hasNoGravity()) { // 使用我们的标记
            // 允许传送
            return;
        }

        // 检查维度锁定（如果玩家装备了某些特殊物品）
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;

            // 检查是否有维度锁定效果
            if (hasDimensionLock(player)) {
                event.setCanceled(true);
                player.sendStatusMessage(
                        new net.minecraft.util.text.TextComponentString(
                                net.minecraft.util.text.TextFormatting.DARK_PURPLE +
                                        "⟐ 维度锁定激活，传送被阻止"), true);
            }
        }
    }

    /**
     * 定期清理过期数据
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 每5秒清理一次
        if (event.side.isServer() &&
                net.minecraftforge.fml.common.FMLCommonHandler.instance()
                        .getMinecraftServerInstance().getTickCounter() % 100 == 0) {

            long currentTime = net.minecraftforge.fml.common.FMLCommonHandler.instance()
                    .getMinecraftServerInstance().getWorld(0).getTotalWorldTime();

            // 清理过期的传送冷却
            teleportCooldowns.entrySet().removeIf(entry -> currentTime >= entry.getValue());

            // 清理过期的传送门保护
            protectedPortalBlocks.entrySet().removeIf(entry -> currentTime >= entry.getValue());
        }
    }

    /**
     * 玩家登出时清理数据
     */
    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        // 关闭玩家的所有传送门
        EntityPlayer player = event.player;
        ItemStack ripper = findDimensionalRipper(player);

        if (!ripper.isEmpty() && ripper.getItem() instanceof ItemDimensionalRipper) {
            // 调用物品的关闭传送门方法
            // ((ItemDimensionalRipper) ripper.getItem()).closeAllPortals(player);

            // 清理玩家相关的冷却
            teleportCooldowns.remove(player.getUniqueID());
        }
    }

    // ===== 辅助方法 =====

    /**
     * 查找玩家是否装备了维度撕裂者
     */
    private static ItemStack findDimensionalRipper(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack stack = baubles.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemDimensionalRipper) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 检查玩家是否有维度锁定
     */
    private static boolean hasDimensionLock(EntityPlayer player) {
        ItemStack ripper = findDimensionalRipper(player);
        if (!ripper.isEmpty()) {
            NBTTagCompound nbt = ripper.getTagCompound();
            if (nbt != null && nbt.getBoolean("DimensionLock")) {
                return true;
            }
        }

        // 也可以检查其他提供维度锁定的物品
        // 比如香巴拉等

        return false;
    }

    /**
     * 添加传送门方块保护
     */
    public static void protectPortalBlock(BlockPos pos, long duration) {
        long protectionTime = net.minecraftforge.fml.common.FMLCommonHandler.instance()
                .getMinecraftServerInstance().getWorld(0).getTotalWorldTime() + duration;
        protectedPortalBlocks.put(pos, protectionTime);
    }

    /**
     * 检查实体是否在传送冷却中
     */
    public static boolean isOnTeleportCooldown(Entity entity) {
        Long cooldownTime = teleportCooldowns.get(entity.getUniqueID());
        if (cooldownTime == null) return false;

        long currentTime = entity.world.getTotalWorldTime();
        return currentTime < cooldownTime;
    }

    /**
     * 设置实体传送冷却
     */
    public static void setTeleportCooldown(Entity entity, int ticks) {
        long cooldownTime = entity.world.getTotalWorldTime() + ticks;
        teleportCooldowns.put(entity.getUniqueID(), cooldownTime);
    }
}