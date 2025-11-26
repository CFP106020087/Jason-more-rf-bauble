package com.moremod.enchantment;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** moremod 附魔增强辅助类 (1.12.2) —— 修复版 */
public class EnchantmentBoostHelper {

    /* ---------------- 状态存储（UUID键） ---------------- */

    private static final Map<UUID, Long> activeBoostsEndAt = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> activeBoostLevels = new ConcurrentHashMap<>();
    private static final ThreadLocal<EntityPlayer> currentContextPlayer = new ThreadLocal<>();

    // 反射缓存
    private static Class<?> minecraftClass = null;
    private static java.lang.reflect.Method getMinecraftMethod = null;
    private static java.lang.reflect.Field playerField = null;
    private static boolean reflectionInitialized = false;

    /* ---------------- 外部 API ---------------- */

    public static void activateBoost(EntityPlayer player, int boostAmount, int durationSeconds) {
        if (player == null || boostAmount <= 0 || durationSeconds <= 0) return;
        final UUID id = player.getUniqueID();
        final long end = System.currentTimeMillis() + durationSeconds * 1000L;
        activeBoostsEndAt.put(id, end);
        activeBoostLevels.put(id, boostAmount);
        System.out.println("[moremod] Activated enchantment boost for " + safeName(player)
                + ": +" + boostAmount + " for " + durationSeconds + "s");
    }

    public static void deactivateBoost(EntityPlayer player) {
        if (player == null) return;
        final UUID id = player.getUniqueID();
        boolean hadBoost = activeBoostsEndAt.containsKey(id);
        activeBoostsEndAt.remove(id);
        activeBoostLevels.remove(id);
        if (hadBoost) {
            System.out.println("[moremod] 临时 Buff 清除 - 玩家: " + safeName(player) + ", UUID: " + id);
        }
    }

    public static boolean hasActiveBoost(EntityPlayer player) {
        if (player == null) return false;
        Long end = activeBoostsEndAt.get(player.getUniqueID());
        if (end == null) return false;
        if (System.currentTimeMillis() > end) {
            activeBoostsEndAt.remove(player.getUniqueID());
            activeBoostLevels.remove(player.getUniqueID());
            return false;
        }
        return true;
    }

    public static int getRemainingTime(EntityPlayer player) {
        if (!hasActiveBoost(player)) return 0;
        long remaining = activeBoostsEndAt.get(player.getUniqueID()) - System.currentTimeMillis();
        return (int) Math.max(0, remaining / 1000L);
    }

    public static void tickCleanup() {
        final long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Long>> it = activeBoostsEndAt.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, Long> e = it.next();
            if (e.getValue() == null || e.getValue() <= now) {
                UUID id = e.getKey();
                it.remove();
                activeBoostLevels.remove(id);
            }
        }

        List<EntityPlayerMP> online = getOnlinePlayers();
        if (online != null) {
            Set<UUID> onlineIds = new HashSet<>();
            for (EntityPlayerMP p : online) onlineIds.add(p.getUniqueID());
            activeBoostsEndAt.keySet().retainAll(onlineIds);
            activeBoostLevels.keySet().retainAll(onlineIds);
        }
    }

    /* ---------------- ✅ 修复：佩戴检测方法 ---------------- */

    /**
     * ✅ 修复：检查是否佩戴了附魔增强饰品（不考虑激活状态）
     * 直接检查物品类型，避免循环依赖
     */
    public static boolean hasBoostBauble(EntityPlayer player) {
        try {
            if (player == null) return false;
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack bauble = baubles.getStackInSlot(i);
                    // ✅ 直接检查类型，不调用 getBoostAmount()
                    if (!bauble.isEmpty() && bauble.getItem() instanceof com.moremod.item.EnchantmentBoostBauble) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[moremod] Error checking boost bauble: " + e.getMessage());
        }
        return false;
    }

    /**
     * ✅ 修复：获取原始饰品增幅值（不考虑激活状态）
     * 用于获取饰品的配置值
     */
    public static int getBaubleBoostAmount(EntityPlayer player) {
        try {
            if (player == null) return 0;
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack bauble = baubles.getStackInSlot(i);
                    if (!bauble.isEmpty() && bauble.getItem() instanceof com.moremod.item.EnchantmentBoostBauble) {
                        // ✅ 获取原始增幅值（不考虑激活状态）
                        return ((com.moremod.item.EnchantmentBoostBauble) bauble.getItem()).getRawBoostAmount();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[moremod] Error getting bauble boost amount: " + e.getMessage());
        }
        return 0;
    }

    /* ---------------- ASM 注入调用的入口 ---------------- */

    public static int getNewEnchantmentLevel(int originalLevel, Enchantment enchantment, ItemStack stack) {
        try {
            if (originalLevel <= 0) return originalLevel;
            EntityPlayer player = getCurrentPlayer();
            if (player == null) return originalLevel;
            if (!isInMainHand(player, stack)) return originalLevel;
            int extra = computeTotalBoost(player);
            if (extra <= 0) return originalLevel;
            return originalLevel + extra;
        } catch (Exception e) {
            System.err.println("[moremod] Error in getNewEnchantmentLevel: " + e.getMessage());
            return originalLevel;
        }
    }

    public static Map<Enchantment, Integer> applyNewEnchantmentLevels(Map<Enchantment, Integer> enchantments, ItemStack stack) {
        try {
            if (enchantments == null || enchantments.isEmpty()) return enchantments;
            EntityPlayer player = getCurrentPlayer();
            if (player == null) return enchantments;
            if (!isInMainHand(player, stack)) return enchantments;
            int extra = computeTotalBoost(player);
            if (extra <= 0) return enchantments;
            Map<Enchantment, Integer> out = new HashMap<>(enchantments.size());
            for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet()) {
                int base = (e.getValue() == null ? 0 : e.getValue());
                out.put(e.getKey(), base + extra);
            }
            return out;
        } catch (Exception e) {
            System.err.println("[moremod] Error in applyNewEnchantmentLevels: " + e.getMessage());
            return enchantments;
        }
    }

    public static NBTTagList modifyEnchantmentTagsForTooltip(NBTTagList enchantmentTags, ItemStack stack) {
        try {
            if (enchantmentTags == null || enchantmentTags.tagCount() == 0) return enchantmentTags;
            EntityPlayer player = getCurrentPlayer();
            if (player == null) return enchantmentTags;
            if (!isInMainHand(player, stack)) return enchantmentTags;
            int boost = computeTotalBoost(player);
            if (boost <= 0) return enchantmentTags;
            int displayBoost = boost * 2;
            return addLevelToAll(enchantmentTags, displayBoost);
        } catch (Exception e) {
            System.err.println("[moremod] Error in modifyEnchantmentTagsForTooltip: " + e.getMessage());
            return enchantmentTags;
        }
    }

    public static NBTTagList modifyEnchantmentTagsForCombat(NBTTagList enchantmentTags, ItemStack stack) {
        try {
            if (enchantmentTags == null || enchantmentTags.tagCount() == 0) return enchantmentTags;
            EntityPlayer player = getCurrentPlayer();
            if (player == null) return enchantmentTags;
            if (!isInMainHand(player, stack)) return enchantmentTags;
            int extra = computeTotalBoost(player);
            if (extra <= 0) return enchantmentTags;
            return addLevelToAll(enchantmentTags, extra);
        } catch (Exception e) {
            System.err.println("[moremod] Error in modifyEnchantmentTagsForCombat: " + e.getMessage());
            return enchantmentTags;
        }
    }

    /* ---------------- 内部逻辑 ---------------- */

    private static int computeTotalBoost(EntityPlayer player) {
        if (player == null) return 0;
        int temp = getTempBoostLevel(player);
        int bauble = getRawBaubleBoostAmount(player);
        int total = temp + bauble;
        total = total / 2;
        return total;
    }

    private static int getTempBoostLevel(EntityPlayer player) {
        final UUID id = player.getUniqueID();
        final Long end = activeBoostsEndAt.get(id);
        if (end == null) return 0;
        if (System.currentTimeMillis() > end) {
            activeBoostsEndAt.remove(id);
            activeBoostLevels.remove(id);
            return 0;
        }
        Integer v = activeBoostLevels.get(id);
        return v == null ? 0 : v;
    }

    /**
     * ✅ 保持不变：获取饰品的实际激活值（用于效果计算）
     * 这个方法会调用 getBoostAmount()，考虑激活状态
     */
    private static int getRawBaubleBoostAmount(EntityPlayer player) {
        try {
            if (player == null) return 0;
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack bauble = baubles.getStackInSlot(i);
                    if (!bauble.isEmpty() && bauble.getItem() instanceof IEnchantmentBooster) {
                        // 这里会调用 getBoostAmount()，只有激活状态才返回非零
                        return ((IEnchantmentBooster) bauble.getItem()).getBoostAmount(bauble, player);
                    }
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static NBTTagList addLevelToAll(NBTTagList src, int extra) {
        if (extra <= 0 || src == null || src.tagCount() == 0) return src;
        NBTTagList out = new NBTTagList();
        for (int i = 0; i < src.tagCount(); i++) {
            NBTTagCompound tag = src.getCompoundTagAt(i).copy();
            short level = tag.getShort("lvl");
            int sum = (level & 0xFFFF) + extra;
            if (sum > 0xFFFF) sum = 0xFFFF;
            tag.setShort("lvl", (short) sum);
            out.appendTag(tag);
        }
        return out;
    }

    private static boolean isInMainHand(EntityPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return false;
        ItemStack main = player.getHeldItemMainhand();
        if (main.isEmpty()) return false;
        return main == stack || ItemStack.areItemStacksEqual(main, stack);
    }

    /* ---------------- 上下文玩家获取 ---------------- */

    /**
     * ✅ 改进：获取当前玩家（支持服务端多人）
     */
    private static EntityPlayer getCurrentPlayer() {
        // 1. 优先使用ThreadLocal（事件中设置）
        EntityPlayer p = currentContextPlayer.get();
        if (p != null) return p;

        // 2. 客户端处理
        try {
            Side side = FMLCommonHandler.instance().getEffectiveSide();
            if (side == Side.CLIENT) {
                if (!reflectionInitialized) {
                    reflectionInitialized = true;
                    try {
                        minecraftClass = Class.forName("net.minecraft.client.Minecraft");
                        try {
                            getMinecraftMethod = minecraftClass.getMethod("getMinecraft");
                        } catch (NoSuchMethodException e) {
                            try {
                                getMinecraftMethod = minecraftClass.getMethod("func_71410_x");
                            } catch (NoSuchMethodException e2) {}
                        }
                        try {
                            playerField = minecraftClass.getField("player");
                        } catch (NoSuchFieldException e) {
                            try {
                                playerField = minecraftClass.getField("field_71439_g");
                            } catch (NoSuchFieldException e2) {}
                        }
                    } catch (Exception e) {}
                }

                if (getMinecraftMethod != null && playerField != null) {
                    try {
                        Object minecraftInstance = getMinecraftMethod.invoke(null);
                        if (minecraftInstance != null) {
                            Object playerObj = playerField.get(minecraftInstance);
                            if (playerObj instanceof EntityPlayer) {
                                return (EntityPlayer) playerObj;
                            }
                        }
                    } catch (Exception e) {}
                }
            }
        } catch (Exception e) {}

        // 3. 服务端备用逻辑（移除单人限制）
        try {
            if (FMLCommonHandler.instance().getMinecraftServerInstance() != null) {
                List<EntityPlayerMP> players =
                        FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();

                // ✅ 多人环境下，返回null让上层调用者处理
                // 因为无法确定是哪个玩家在查看物品
                if (players.size() == 1) {
                    return players.get(0);
                }

                // 在多人环境下，如果没有ThreadLocal，则无法确定玩家
                // 这种情况应该通过事件系统设置ThreadLocal来解决
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private static List<EntityPlayerMP> getOnlinePlayers() {
        try {
            if (FMLCommonHandler.instance().getMinecraftServerInstance() == null) return null;
            return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
        } catch (Throwable t) {
            return null;
        }
    }

    public static void setContextPlayer(EntityPlayer player) { currentContextPlayer.set(player); }
    public static void clearContextPlayer() { currentContextPlayer.remove(); }

    private static String safeName(EntityPlayer p) {
        try { return p.getName(); } catch (Throwable t) { return "unknown"; }
    }

    /* ---------------- 接口 ---------------- */

    public interface IEnchantmentBooster {
        int getBoostAmount(ItemStack stack, EntityPlayer player);
    }
}