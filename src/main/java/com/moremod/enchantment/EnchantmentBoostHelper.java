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

/** moremod 附魔增强辅助类 (1.12.2) —— 修复 & 兼容旧调用 */
public class EnchantmentBoostHelper {

    /* ---------------- 状态存储（UUID键） ---------------- */

    // 临时 Buff 结束时间戳 ms
    private static final Map<UUID, Long> activeBoostsEndAt = new ConcurrentHashMap<>();
    // 临时 Buff 强度
    private static final Map<UUID, Integer> activeBoostLevels = new ConcurrentHashMap<>();
    // 按键是否激活
    private static final Map<UUID, Boolean> keyActive = new ConcurrentHashMap<>();

    // 上下文玩家（Transformer注入方法里可设定）
    private static final ThreadLocal<EntityPlayer> currentContextPlayer = new ThreadLocal<>();

    /* ---------------- 外部 API ---------------- */

    /** 启用一个持续 timeSeconds 的临时增幅（可与按键饰品叠加） */
    public static void activateBoost(EntityPlayer player, int boostAmount, int durationSeconds) {
        if (player == null || boostAmount <= 0 || durationSeconds <= 0) return;
        final UUID id = player.getUniqueID();
        final long end = System.currentTimeMillis() + durationSeconds * 1000L;
        activeBoostsEndAt.put(id, end);
        activeBoostLevels.put(id, boostAmount);
        System.out.println("[moremod] Activated enchantment boost for " + safeName(player)
                + ": +" + boostAmount + " for " + durationSeconds + "s");
    }

    /** 是否存在仍在持续的临时增幅 */
    public static boolean hasActiveBoost(EntityPlayer player) {
        if (player == null) return false;
        Long end = activeBoostsEndAt.get(player.getUniqueID());
        if (end == null) return false;
        if (System.currentTimeMillis() > end) {
            // 过期清理
            activeBoostsEndAt.remove(player.getUniqueID());
            activeBoostLevels.remove(player.getUniqueID());
            return false;
        }
        return true;
    }

    /** 临时增幅剩余时间（秒） */
    public static int getRemainingTime(EntityPlayer player) {
        if (!hasActiveBoost(player)) return 0;
        long remaining = activeBoostsEndAt.get(player.getUniqueID()) - System.currentTimeMillis();
        return (int) Math.max(0, remaining / 1000L);
    }

    /** 供按键层设置：R键按下/松开 */
    public static void setKeyActive(EntityPlayer player, boolean active) {
        if (player == null) return;
        keyActive.put(player.getUniqueID(), active);
    }

    /** 查询R键是否被判定为激活 */
    public static boolean isKeyActive(EntityPlayer player) {
        if (player == null) return false;
        Boolean v = keyActive.get(player.getUniqueID());
        return v != null && v.booleanValue();
    }

    /** 建议周期性清理（每 10~20 tick 调用一次） */
    public static void tickCleanup() {
        final long now = System.currentTimeMillis();

        // 清理过期临时 Buff
        for (Iterator<Map.Entry<UUID, Long>> it = activeBoostsEndAt.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, Long> e = it.next();
            if (e.getValue() == null || e.getValue() <= now) {
                UUID id = e.getKey();
                it.remove();
                activeBoostLevels.remove(id);
            }
        }

        // 可选：清理离线玩家的按键状态
        List<EntityPlayerMP> online = getOnlinePlayers();
        if (online != null) {
            Set<UUID> onlineIds = new HashSet<>();
            for (EntityPlayerMP p : online) onlineIds.add(p.getUniqueID());
            keyActive.keySet().retainAll(onlineIds);
        }
    }

    /* ---------------- 兼容旧调用点的方法（保留签名） ---------------- */

    /** 是否佩戴了可提供增幅的饰品（不考虑R键） */
    public static boolean hasBoostBauble(EntityPlayer player) {
        return getBaubleBoostAmount(player) > 0;
    }

    /**
     * 返回“原始饰品数值”（不考虑R键开关），用于一次性60s Buff。
     * 实战计算时是否加入，由 computeTotalBoost 决定（需R键激活）。
     */
    public static int getBaubleBoostAmount(EntityPlayer player) {
        return getRawBaubleBoostAmount(player);
    }

    /* ---------------- ASM 注入调用的入口 ---------------- */

    // Astral 风格：返回最终等级（原值 + 增幅）
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

    // Map 批量叠加
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

    // NBT 显示层 - Tooltip
    public static NBTTagList modifyEnchantmentTagsForTooltip(NBTTagList enchantmentTags, ItemStack stack) {
        try {
            if (enchantmentTags == null || enchantmentTags.tagCount() == 0) return enchantmentTags;

            EntityPlayer player = getCurrentPlayer();
            if (player == null) return enchantmentTags;

            if (!isInMainHand(player, stack)) return enchantmentTags;

            int extra = computeTotalBoost(player);
            if (extra <= 0) return enchantmentTags;

            return addLevelToAll(enchantmentTags, extra);
        } catch (Exception e) {
            System.err.println("[moremod] Error in modifyEnchantmentTagsForTooltip: " + e.getMessage());
            return enchantmentTags;
        }
    }

    // NBT 显示层 - 战斗计算
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

    /** 统一计算总增幅：临时Buff + （R键按住？饰品数值：0） */
    private static int computeTotalBoost(EntityPlayer player) {
        if (player == null) return 0;

        int temp = getTempBoostLevel(player);                 // 需 activateBoost 才有
        int bauble = isKeyActive(player) ? getRawBaubleBoostAmount(player) : 0;
        int total = temp + bauble;

        // 需要可以在这里做上限，例如不超过 10：
        // if (total > 10) total = 10;

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

    /** 原始饰品数值（忽略R键，仅读取是否佩戴对应饰品） */
    private static int getRawBaubleBoostAmount(EntityPlayer player) {
        try {
            if (player == null) return 0;
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack bauble = baubles.getStackInSlot(i);
                    if (!bauble.isEmpty() && bauble.getItem() instanceof IEnchantmentBooster) {
                        return ((IEnchantmentBooster) bauble.getItem()).getBoostAmount(bauble, player);
                    }
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    /** 为全部附魔 tag +extra 级（拷贝写入，含溢出保护） */
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

    /** 仅主手判断（同实例或内容等价） */
    private static boolean isInMainHand(EntityPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return false;
        ItemStack main = player.getHeldItemMainhand();
        if (main.isEmpty()) return false;
        return main == stack || ItemStack.areItemStacksEqual(main, stack);
    }

    /* ---------------- 上下文玩家获取 ---------------- */

    private static EntityPlayer getCurrentPlayer() {
        EntityPlayer p = currentContextPlayer.get();
        if (p != null) return p;

        try {
            Side side = FMLCommonHandler.instance().getEffectiveSide();
            if (side == Side.CLIENT) {
                p = net.minecraft.client.Minecraft.getMinecraft().player;
                if (p != null) return p;
            }
        } catch (Throwable ignored) {}

        try {
            if (FMLCommonHandler.instance().getMinecraftServerInstance() != null) {
                List<EntityPlayerMP> players =
                        FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
                if (players.size() == 1) return players.get(0);
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

    /* ---------------- 接口保持不变 ---------------- */

    public interface IEnchantmentBooster {
        int getBoostAmount(ItemStack stack, EntityPlayer player);
    }
}
