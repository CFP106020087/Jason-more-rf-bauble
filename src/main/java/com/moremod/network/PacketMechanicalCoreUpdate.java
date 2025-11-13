package com.moremod.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.event.EnergyPunishmentSystem;
import com.moremod.util.BaublesSyncUtil;
import com.moremod.util.UpgradeKeys;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class PacketMechanicalCoreUpdate implements IMessage {

    public enum Action {
        SET_LEVEL,
        REPAIR_UPGRADE
    }

    public Action action;
    public String upgradeId;
    public int level;
    public boolean fromClient;

    public PacketMechanicalCoreUpdate() {}

    public PacketMechanicalCoreUpdate(Action action, String upgradeId, int level, boolean fromClient) {
        this.action = action;
        this.upgradeId = upgradeId;
        this.level = level;
        this.fromClient = fromClient;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int a = buf.readInt();
        this.action = Action.values()[a];
        int len = buf.readInt();
        byte[] arr = new byte[len];
        buf.readBytes(arr);
        this.upgradeId = new String(arr, StandardCharsets.UTF_8);
        this.level = buf.readInt();
        this.fromClient = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(action.ordinal());
        byte[] arr = upgradeId.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(arr.length);
        buf.writeBytes(arr);
        buf.writeInt(level);
        buf.writeBoolean(fromClient);
    }

    public static class Handler implements IMessageHandler<PacketMechanicalCoreUpdate, IMessage> {

        private static final Set<String> WATERPROOF_ALIASES = new HashSet<>(Arrays.asList(
                "WATERPROOF_MODULE","WATERPROOF","waterproof_module","waterproof"
        ));

        @Override
        public IMessage onMessage(PacketMechanicalCoreUpdate msg, MessageContext ctx) {
            final EntityPlayerMP serverPlayer = ctx.getServerHandler().player;

            serverPlayer.getServerWorld().addScheduledTask(() -> {

                if (msg.action == Action.REPAIR_UPGRADE) {
                    handleRepair(serverPlayer, msg.upgradeId, msg.level);
                    return;
                }

                if (msg.action != Action.SET_LEVEL) return;

                String id = msg.upgradeId == null ? "" : msg.upgradeId.trim();
                if (id.isEmpty()) return;

                ItemStack core = ItemMechanicalCore.findEquippedMechanicalCore(serverPlayer);
                if (core.isEmpty() || !(core.getItem() instanceof ItemMechanicalCore)) {
                    serverPlayer.sendMessage(new TextComponentString(
                            TextFormatting.RED + "请先装备机械核心"));
                    return;
                }

                NBTTagCompound nbt = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
                if (!core.hasTagCompound()) core.setTagCompound(nbt);

                int requested = Math.max(0, msg.level);

                // ✅ 关键修复：使用忽略暂停状态的读取方法
                int actualLevel = getActualLevel(nbt, id);  // ← 读取真实等级，不管是否暂停
                int ownedMax = getOwnedMax(nbt, id);

                System.out.println("[服务器] SET_LEVEL - 模块: " + id +
                        ", 请求: " + requested +
                        ", 当前真实等级: " + actualLevel +
                        ", OwnedMax: " + ownedMax);

                Map<String, Object> repairBackup = backupRepairData(nbt, id);

                final int ABS_MAX = 64;
                if (requested > ABS_MAX) requested = ABS_MAX;

                boolean justPaused = false;
                boolean justResumed = false;
                int pausedAtLevel = 0;

                if (requested > actualLevel && isPenalizedSafe(core, id)) {
                    int cap = Math.max(1, getPenaltyCapSafe(core, id));
                    if (requested > cap) {
                        serverPlayer.sendMessage(new TextComponentString(
                                TextFormatting.LIGHT_PURPLE + "🔒 惩罚中：最高 Lv." + cap));
                        requested = cap;
                    }
                }

                // ✅ 暂停逻辑修复
                if (requested == 0) {
                    // 使用真实等级而不是 getLevelAcross
                    if (actualLevel > 0) {
                        System.out.println("[服务器] 暂停模块 " + id + " 从 Lv." + actualLevel);

                        // 先设置等级为0
                        setLevelEverywhere(core, id, 0);

                        // 再写入暂停元数据（使用真实等级）
                        writePauseMeta(core, id, actualLevel, true);

                        ensureOwnedMaxAtLeast(nbt, id, actualLevel);

                        serverPlayer.sendMessage(new TextComponentString(
                                TextFormatting.YELLOW + "⏸ 已暂停 " + prettyName(id) + " (Lv." + actualLevel + ")"));

                        justPaused = true;
                        pausedAtLevel = actualLevel;
                    } else {
                        System.out.println("[服务器] 模块 " + id + " 已经是 Lv.0，无需暂停");
                    }
                } else {
                    // ✅ 恢复/升级逻辑
                    System.out.println("[服务器] 设置模块 " + id + " 为 Lv." + requested);

                    // 先清除暂停状态
                    clearPauseState(nbt, id);

                    // 再设置等级
                    setLevelEverywhere(core, id, requested);

                    if (requested > ownedMax) {
                        ensureOwnedMaxAtLeast(nbt, id, requested);

                        int originalMax = getOriginalMax(nbt, id);
                        if (requested > originalMax) {
                            nbt.setInteger(UpgradeKeys.kOriginalMax + id, requested);
                            nbt.setInteger(UpgradeKeys.kOriginalMax + up(id), requested);
                            nbt.setInteger(UpgradeKeys.kOriginalMax + lo(id), requested);
                        }
                    }

                    serverPlayer.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "✓ " + prettyName(id) + " 设为 Lv." + requested));

                    if (actualLevel == 0 && requested > 0) {
                        justResumed = true;
                    }
                }

                restoreRepairData(nbt, repairBackup);

                // ✅ 确保暂停/恢复状态正确写入
                if (justPaused) {
                    writePauseStateOnly(nbt, id, pausedAtLevel, true);
                    System.out.println("[服务器] 确认写入暂停状态: LastLevel = " + pausedAtLevel);
                }

                if (justResumed) {
                    writePauseStateOnly(nbt, id, requested, false);
                    System.out.println("[服务器] 清除暂停状态");
                }

                // ✅ 验证写入结果
                int finalLevel = getActualLevel(nbt, id);
                boolean finalPaused = nbt.getBoolean(UpgradeKeys.kPaused + id);
                int finalLastLevel = nbt.getInteger(UpgradeKeys.kLastLevel + id);

                System.out.println("[服务器] 最终状态 - 等级: " + finalLevel +
                        ", 暂停: " + finalPaused +
                        ", LastLevel: " + finalLastLevel);

                syncDirty(serverPlayer);
            });

            return null;
        }

        // ================= 关键新增方法 =================

        /**
         * ✅ 读取真实等级（忽略暂停状态）
         */
        private static int getActualLevel(NBTTagCompound nbt, String id) {
            if (nbt == null) return 0;

            int lv = 0;
            lv = Math.max(lv, nbt.getInteger(UpgradeKeys.kUpgrade + id));
            lv = Math.max(lv, nbt.getInteger(UpgradeKeys.kUpgrade + up(id)));
            lv = Math.max(lv, nbt.getInteger(UpgradeKeys.kUpgrade + lo(id)));

            return lv;
        }

        /**
         * ✅ 清除暂停状态
         */
        private static void clearPauseState(NBTTagCompound nbt, String id) {
            if (nbt == null) return;

            String[] variants = {id, up(id), lo(id)};

            for (String variant : variants) {
                nbt.setBoolean(UpgradeKeys.kPaused + variant, false);
            }

            if (isWaterproofId(id)) {
                for (String wid : WATERPROOF_ALIASES) {
                    String[] wvariants = {wid, up(wid), lo(wid)};
                    for (String wv : wvariants) {
                        nbt.setBoolean(UpgradeKeys.kPaused + wv, false);
                    }
                }
            }
        }

        /**
         * ✅ 只写入暂停状态（不修改等级）
         */
        private static void writePauseStateOnly(NBTTagCompound nbt, String id, int lastLevel, boolean paused) {
            if (nbt == null) return;

            String[] variants = {id, up(id), lo(id)};

            for (String variant : variants) {
                if (paused && lastLevel > 0) {
                    nbt.setInteger(UpgradeKeys.kLastLevel + variant, lastLevel);
                    nbt.setBoolean("HasUpgrade_" + variant, true);
                }
                nbt.setBoolean(UpgradeKeys.kPaused + variant, paused);
            }

            if (isWaterproofId(id)) {
                for (String wid : WATERPROOF_ALIASES) {
                    String[] wvariants = {wid, up(wid), lo(wid)};
                    for (String wv : wvariants) {
                        if (paused && lastLevel > 0) {
                            nbt.setInteger(UpgradeKeys.kLastLevel + wv, lastLevel);
                            nbt.setBoolean("HasUpgrade_" + wv, true);
                        }
                        nbt.setBoolean(UpgradeKeys.kPaused + wv, paused);
                    }
                }
            }
        }

        // ================= 修复处理 =================

        private static void handleRepair(EntityPlayerMP player, String upgradeId, int levelCost) {
            ItemStack core = ItemMechanicalCore.findEquippedMechanicalCore(player);
            if (core.isEmpty() || !(core.getItem() instanceof ItemMechanicalCore)) {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "未找到机械核心！"));
                return;
            }

            NBTTagCompound nbt = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
            if (!core.hasTagCompound()) core.setTagCompound(nbt);

            String upperId = up(upgradeId);
            String lowerId = lo(upgradeId);

            boolean wasPunished = nbt.getBoolean(UpgradeKeys.kWasPunished + upperId) ||
                    nbt.getBoolean(UpgradeKeys.kWasPunished + upgradeId) ||
                    nbt.getBoolean(UpgradeKeys.kWasPunished + lowerId);

            if (!wasPunished) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 模块未损坏"));
                return;
            }

            int ownedMax = getOwnedMax(nbt, upgradeId);
            int itemMax = 0;
            try {
                itemMax = EnergyPunishmentSystem.getItemMaxLevel(core, upgradeId);
            } catch (Throwable e) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "无法获取模块最大等级！"));
                return;
            }

            if (ownedMax >= itemMax) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 模块已完全修复"));
                return;
            }

            if (!player.capabilities.isCreativeMode) {
                if (player.experienceLevel < levelCost) {
                    player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "等级不足！需要 " + levelCost + " 级 (当前 " +
                                    player.experienceLevel + " 级)"));
                    return;
                }
                player.addExperienceLevel(-levelCost);
            }

            int targetLevel = Math.min(ownedMax + 1, itemMax);

            nbt.setInteger(UpgradeKeys.kOwnedMax + upgradeId, targetLevel);
            nbt.setInteger(UpgradeKeys.kOwnedMax + upperId, targetLevel);
            nbt.setInteger(UpgradeKeys.kOwnedMax + lowerId, targetLevel);

            int damageCount = Math.max(
                    nbt.getInteger(UpgradeKeys.kDamageCount + upgradeId),
                    Math.max(
                            nbt.getInteger(UpgradeKeys.kDamageCount + upperId),
                            nbt.getInteger(UpgradeKeys.kDamageCount + lowerId)
                    )
            );

            if (damageCount > 0) {
                int newDamageCount = Math.max(0, damageCount - 1);
                nbt.setInteger(UpgradeKeys.kDamageCount + upgradeId, newDamageCount);
                nbt.setInteger(UpgradeKeys.kDamageCount + upperId, newDamageCount);
                nbt.setInteger(UpgradeKeys.kDamageCount + lowerId, newDamageCount);
            }

            if (targetLevel >= itemMax) {
                nbt.removeTag(UpgradeKeys.kWasPunished + upgradeId);
                nbt.removeTag(UpgradeKeys.kWasPunished + upperId);
                nbt.removeTag(UpgradeKeys.kWasPunished + lowerId);

                nbt.removeTag(UpgradeKeys.kDamageCount + upgradeId);
                nbt.removeTag(UpgradeKeys.kDamageCount + upperId);
                nbt.removeTag(UpgradeKeys.kDamageCount + lowerId);
            }

            setLevelEverywhere(core, upgradeId, targetLevel);
// 推荐
            writePauseStateOnly(nbt, upgradeId, targetLevel, false);

// 或者至少
// clearPauseState(nbt, upgradeId);

            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);

            if (targetLevel >= itemMax) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 模块完全修复！" + prettyName(upgradeId) +
                                " 已恢复到 Lv." + targetLevel +
                                TextFormatting.GRAY + " (-" + levelCost + " 级)"));
            } else {
                int repairsLeft = itemMax - targetLevel;
                player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + "⚒ 模块部分修复：" + prettyName(upgradeId) +
                                " Lv." + targetLevel + "/" + itemMax +
                                TextFormatting.GRAY + " (还需 " + repairsLeft + " 次, -" + levelCost + " 级)"));
            }

            syncDirty(player);
        }

        // ================= 备份保护 =================

        private static Map<String, Object> backupRepairData(NBTTagCompound nbt, String upgradeId) {
            Map<String, Object> backup = new HashMap<>();
            String[] variants = {upgradeId, up(upgradeId), lo(upgradeId)};
            String[] keys = {UpgradeKeys.kOriginalMax, UpgradeKeys.kWasPunished, UpgradeKeys.kDamageCount, "TotalDamageCount_"};

            for (String variant : variants) {
                for (String key : keys) {
                    String fullKey = key + variant;
                    if (nbt.hasKey(fullKey)) {
                        if (key.equals(UpgradeKeys.kWasPunished)) {
                            backup.put(fullKey, nbt.getBoolean(fullKey));
                        } else {
                            backup.put(fullKey, nbt.getInteger(fullKey));
                        }
                    }
                }
            }

            if (isWaterproofId(upgradeId)) {
                for (String wid : WATERPROOF_ALIASES) {
                    String[] wvariants = {wid, up(wid), lo(wid)};
                    for (String wv : wvariants) {
                        for (String key : keys) {
                            String fullKey = key + wv;
                            if (nbt.hasKey(fullKey) && !backup.containsKey(fullKey)) {
                                if (key.equals(UpgradeKeys.kWasPunished)) {
                                    backup.put(fullKey, nbt.getBoolean(fullKey));
                                } else {
                                    backup.put(fullKey, nbt.getInteger(fullKey));
                                }
                            }
                        }
                    }
                }
            }

            return backup;
        }

        private static void restoreRepairData(NBTTagCompound nbt, Map<String, Object> backup) {
            if (backup.isEmpty()) return;

            for (Map.Entry<String, Object> entry : backup.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Boolean) {
                    nbt.setBoolean(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    nbt.setInteger(key, (Integer) value);
                }
            }
        }

        // ================= 工具方法 =================

        private static String up(String s){ return s == null ? "" : s.toUpperCase(); }
        private static String lo(String s){ return s == null ? "" : s.toLowerCase(); }

        private static boolean isWaterproofId(String id) {
            if (id == null) return false;
            String u = up(id);
            return WATERPROOF_ALIASES.contains(u) || u.contains("WATERPROOF");
        }

        private static int getOriginalMax(NBTTagCompound nbt, String id) {
            int max = 0;
            max = Math.max(max, nbt.getInteger(UpgradeKeys.kOriginalMax + id));
            max = Math.max(max, nbt.getInteger(UpgradeKeys.kOriginalMax + up(id)));
            max = Math.max(max, nbt.getInteger(UpgradeKeys.kOriginalMax + lo(id)));
            return max;
        }

        private static void setLevelEverywhere(ItemStack core, String upgradeId, int newLevel) {
            if (core == null || core.isEmpty()) return;

            NBTTagCompound nbt = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
            if (!core.hasTagCompound()) core.setTagCompound(nbt);

            if (isWaterproofId(upgradeId)) {
                for (String wid : WATERPROOF_ALIASES) {
                    String U = up(wid), L = lo(wid);
                    nbt.setInteger(UpgradeKeys.kUpgrade + wid, newLevel);
                    nbt.setInteger(UpgradeKeys.kUpgrade + U,   newLevel);
                    nbt.setInteger(UpgradeKeys.kUpgrade + L,   newLevel);
                    if (newLevel > 0) {
                        nbt.setBoolean("HasUpgrade_" + wid, true);
                        nbt.setBoolean("HasUpgrade_" + U,   true);
                        nbt.setBoolean("HasUpgrade_" + L,   true);
                    }
                    try {
                        ItemMechanicalCoreExtended.setUpgradeLevel(core, wid, newLevel);
                        ItemMechanicalCoreExtended.setUpgradeLevel(core, U,   newLevel);
                        ItemMechanicalCoreExtended.setUpgradeLevel(core, L,   newLevel);
                    } catch (Throwable ignored) {}
                }
                try {
                    for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
                        if (isWaterproofId(t.getKey())) {
                            ItemMechanicalCore.setUpgradeLevel(core, t, newLevel);
                        }
                    }
                } catch (Throwable ignored) {}
            } else {
                String U = up(upgradeId), L = lo(upgradeId);
                nbt.setInteger(UpgradeKeys.kUpgrade + upgradeId, newLevel);
                nbt.setInteger(UpgradeKeys.kUpgrade + U,         newLevel);
                nbt.setInteger(UpgradeKeys.kUpgrade + L,         newLevel);
                if (newLevel > 0) {
                    nbt.setBoolean("HasUpgrade_" + upgradeId, true);
                    nbt.setBoolean("HasUpgrade_" + U,         true);
                    nbt.setBoolean("HasUpgrade_" + L,         true);
                }
                try {
                    ItemMechanicalCoreExtended.setUpgradeLevel(core, upgradeId, newLevel);
                    ItemMechanicalCoreExtended.setUpgradeLevel(core, U,        newLevel);
                    ItemMechanicalCoreExtended.setUpgradeLevel(core, L,        newLevel);
                } catch (Throwable ignored) {}
                try {
                    for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
                        if (t.getKey().equalsIgnoreCase(upgradeId)) {
                            ItemMechanicalCore.setUpgradeLevel(core, t, newLevel);
                            break;
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // ✅ 统一兜底：只要把等级设为 >0，就清掉一切 IsPaused_（含别名）
            if (newLevel > 0) {
                clearPauseState(nbt, upgradeId);
            }
        }


        private static void writePauseMeta(ItemStack core, String upgradeId, int lastLevel, boolean paused) {
            if (core == null || core.isEmpty()) return;

            NBTTagCompound nbt = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
            if (!core.hasTagCompound()) core.setTagCompound(nbt);

            if (isWaterproofId(upgradeId)) {
                for (String wid : WATERPROOF_ALIASES) {
                    String U = up(wid), L = lo(wid);
                    if (paused && lastLevel > 0) {
                        nbt.setInteger(UpgradeKeys.kLastLevel + wid, lastLevel);
                        nbt.setInteger(UpgradeKeys.kLastLevel + U,   lastLevel);
                        nbt.setInteger(UpgradeKeys.kLastLevel + L,   lastLevel);
                        ensureOwnedMaxAtLeast(nbt, wid, lastLevel);
                        nbt.setBoolean("HasUpgrade_" + wid, true);
                        nbt.setBoolean("HasUpgrade_" + U,   true);
                        nbt.setBoolean("HasUpgrade_" + L,   true);
                    }
                    nbt.setBoolean(UpgradeKeys.kPaused + wid, paused);
                    nbt.setBoolean(UpgradeKeys.kPaused + U,   paused);
                    nbt.setBoolean(UpgradeKeys.kPaused + L,   paused);
                }
            } else {
                String U = up(upgradeId), L = lo(upgradeId);
                if (paused && lastLevel > 0) {
                    for (String k : Arrays.asList(upgradeId, U, L)) {
                        nbt.setInteger(UpgradeKeys.kLastLevel + k, lastLevel);
                        ensureOwnedMaxAtLeast(nbt, k, lastLevel);
                        nbt.setBoolean("HasUpgrade_" + k, true);
                    }
                }
                for (String k : Arrays.asList(upgradeId, U, L)) {
                    nbt.setBoolean(UpgradeKeys.kPaused + k, paused);
                }
            }
        }

        private static int getLevelAcross(ItemStack core, String id) {
            if (core == null || core.isEmpty()) return 0;
            NBTTagCompound nbt = core.getTagCompound();
            if (nbt == null) return 0;

            if (nbt.getBoolean(UpgradeKeys.kPaused + id) ||
                    nbt.getBoolean(UpgradeKeys.kPaused + up(id)) ||
                    nbt.getBoolean(UpgradeKeys.kPaused + lo(id))) {
                return 0;
            }

            return getActualLevel(nbt, id);
        }

        private static int getOwnedMax(NBTTagCompound nbt, String id) {
            if (nbt == null) return 0;
            int v = 0;
            v = Math.max(v, nbt.getInteger(UpgradeKeys.kOwnedMax + id));
            v = Math.max(v, nbt.getInteger(UpgradeKeys.kOwnedMax + up(id)));
            v = Math.max(v, nbt.getInteger(UpgradeKeys.kOwnedMax + lo(id)));
            return v;
        }

        private static void ensureOwnedMaxAtLeast(NBTTagCompound nbt, String id, int atLeast) {
            if (nbt == null) return;
            for (String k : Arrays.asList(id, up(id), lo(id))) {
                if (nbt.getInteger(UpgradeKeys.kOwnedMax + k) < atLeast) {
                    nbt.setInteger(UpgradeKeys.kOwnedMax + k, atLeast);
                }
            }
        }

        private static void syncDirty(EntityPlayerMP p) {
            try {
                p.inventory.markDirty();
                p.inventoryContainer.detectAndSendChanges();
                if (!p.world.isRemote) {
                    BaublesSyncUtil.safeSyncAll(p);
                }
            } catch (Throwable ignored) {}
        }

        private static String prettyName(String id) {
            String s = id.replace('_', ' ').toLowerCase();
            if (s.isEmpty()) return id;
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }

        private static boolean isPenalizedSafe(ItemStack core, String id) {
            try {
                return ItemMechanicalCore.isPenalized(core, id);
            } catch (Throwable ignored) {
                return false;
            }
        }

        private static int getPenaltyCapSafe(ItemStack core, String id) {
            try {
                return ItemMechanicalCore.getPenaltyCap(core, id);
            } catch (Throwable ignored) {
                return Integer.MAX_VALUE;
            }
        }
    }
}