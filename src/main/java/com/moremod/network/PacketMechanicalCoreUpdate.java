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
import com.moremod.eventHandler.EnergyPunishmentSystem;
import com.moremod.util.BaublesSyncUtil;

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

        private static final String K_ORIGINAL_MAX = "OriginalMax_";
        private static final String K_OWNED_MAX = "OwnedMax_";
        private static final String K_DAMAGE_COUNT = "DamageCount_";
        private static final String K_WAS_PUNISHED = "WasPunished_";
        private static final String K_LAST_LEVEL = "LastLevel_";
        private static final String K_IS_PAUSED = "IsPaused_";
        private static final String K_UPGRADE = "upgrade_";

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
                int actualLevel = getActualLevel(nbt, id, serverPlayer);  // ← 从 Capability 读取真实等级
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
                        setLevelEverywhere(core, id, 0, serverPlayer);

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
                    setLevelEverywhere(core, id, requested, serverPlayer);

                    if (requested > ownedMax) {
                        ensureOwnedMaxAtLeast(nbt, id, requested);

                        int originalMax = getOriginalMax(nbt, id);
                        if (requested > originalMax) {
                            nbt.setInteger(K_ORIGINAL_MAX + id, requested);
                            nbt.setInteger(K_ORIGINAL_MAX + up(id), requested);
                            nbt.setInteger(K_ORIGINAL_MAX + lo(id), requested);
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
                int finalLevel = getActualLevel(nbt, id, serverPlayer);
                boolean finalPaused = nbt.getBoolean(K_IS_PAUSED + id);
                int finalLastLevel = nbt.getInteger(K_LAST_LEVEL + id);

                System.out.println("[服务器] 最终状态 - 等级: " + finalLevel +
                        ", 暂停: " + finalPaused +
                        ", LastLevel: " + finalLastLevel);

                syncDirty(serverPlayer);
            });

            return null;
        }

        // ================= 关键新增方法 =================

        /**
         * ✅ 读取真实等级（从 Capability 读取）
         */
        private static int getActualLevel(NBTTagCompound nbt, String id, EntityPlayerMP player) {
            if (player == null) return 0;

            // 从 Capability 读取
            com.moremod.capability.IMechCoreData data = player.getCapability(
                com.moremod.capability.IMechCoreData.CAPABILITY, null);

            if (data != null) {
                String moduleId = id.toUpperCase();
                return data.getModuleLevel(moduleId);
            }

            return 0;
        }

        // 保留旧签名用于兼容（但已废弃）
        @Deprecated
        private static int getActualLevel(NBTTagCompound nbt, String id) {
            // 旧的 NBT 读取逻辑（已废弃）
            if (nbt == null) return 0;

            int lv = 0;
            lv = Math.max(lv, nbt.getInteger(K_UPGRADE + id));
            lv = Math.max(lv, nbt.getInteger(K_UPGRADE + up(id)));
            lv = Math.max(lv, nbt.getInteger(K_UPGRADE + lo(id)));

            return lv;
        }

        /**
         * ✅ 清除暂停状态
         */
        private static void clearPauseState(NBTTagCompound nbt, String id) {
            if (nbt == null) return;

            String[] variants = {id, up(id), lo(id)};

            for (String variant : variants) {
                nbt.setBoolean(K_IS_PAUSED + variant, false);
            }

            if (isWaterproofId(id)) {
                for (String wid : WATERPROOF_ALIASES) {
                    String[] wvariants = {wid, up(wid), lo(wid)};
                    for (String wv : wvariants) {
                        nbt.setBoolean(K_IS_PAUSED + wv, false);
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
                    nbt.setInteger(K_LAST_LEVEL + variant, lastLevel);
                    nbt.setBoolean("HasUpgrade_" + variant, true);
                }
                nbt.setBoolean(K_IS_PAUSED + variant, paused);
            }

            if (isWaterproofId(id)) {
                for (String wid : WATERPROOF_ALIASES) {
                    String[] wvariants = {wid, up(wid), lo(wid)};
                    for (String wv : wvariants) {
                        if (paused && lastLevel > 0) {
                            nbt.setInteger(K_LAST_LEVEL + wv, lastLevel);
                            nbt.setBoolean("HasUpgrade_" + wv, true);
                        }
                        nbt.setBoolean(K_IS_PAUSED + wv, paused);
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

            boolean wasPunished = nbt.getBoolean(K_WAS_PUNISHED + upperId) ||
                    nbt.getBoolean(K_WAS_PUNISHED + upgradeId) ||
                    nbt.getBoolean(K_WAS_PUNISHED + lowerId);

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

            nbt.setInteger(K_OWNED_MAX + upgradeId, targetLevel);
            nbt.setInteger(K_OWNED_MAX + upperId, targetLevel);
            nbt.setInteger(K_OWNED_MAX + lowerId, targetLevel);

            int damageCount = Math.max(
                    nbt.getInteger(K_DAMAGE_COUNT + upgradeId),
                    Math.max(
                            nbt.getInteger(K_DAMAGE_COUNT + upperId),
                            nbt.getInteger(K_DAMAGE_COUNT + lowerId)
                    )
            );

            if (damageCount > 0) {
                int newDamageCount = Math.max(0, damageCount - 1);
                nbt.setInteger(K_DAMAGE_COUNT + upgradeId, newDamageCount);
                nbt.setInteger(K_DAMAGE_COUNT + upperId, newDamageCount);
                nbt.setInteger(K_DAMAGE_COUNT + lowerId, newDamageCount);
            }

            if (targetLevel >= itemMax) {
                nbt.removeTag(K_WAS_PUNISHED + upgradeId);
                nbt.removeTag(K_WAS_PUNISHED + upperId);
                nbt.removeTag(K_WAS_PUNISHED + lowerId);

                nbt.removeTag(K_DAMAGE_COUNT + upgradeId);
                nbt.removeTag(K_DAMAGE_COUNT + upperId);
                nbt.removeTag(K_DAMAGE_COUNT + lowerId);
            }

            // ✅ 修复：添加 player 参数（纯 Capability 模式）
            setLevelEverywhere(core, upgradeId, targetLevel, player);

            // 清除暂停状态
            writePauseStateOnly(nbt, upgradeId, targetLevel, false);

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
            String[] keys = {K_ORIGINAL_MAX, K_WAS_PUNISHED, K_DAMAGE_COUNT, "TotalDamageCount_"};

            for (String variant : variants) {
                for (String key : keys) {
                    String fullKey = key + variant;
                    if (nbt.hasKey(fullKey)) {
                        if (key.equals(K_WAS_PUNISHED)) {
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
                                if (key.equals(K_WAS_PUNISHED)) {
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
            max = Math.max(max, nbt.getInteger(K_ORIGINAL_MAX + id));
            max = Math.max(max, nbt.getInteger(K_ORIGINAL_MAX + up(id)));
            max = Math.max(max, nbt.getInteger(K_ORIGINAL_MAX + lo(id)));
            return max;
        }

        private static void setLevelEverywhere(ItemStack core, String upgradeId, int newLevel, EntityPlayerMP player) {
            if (core == null || core.isEmpty() || player == null) return;

            // ✅ 纯 Capability 模式：只写 Capability，不写 NBT
            com.moremod.capability.IMechCoreData data = player.getCapability(
                com.moremod.capability.IMechCoreData.CAPABILITY, null);

            if (data != null) {
                // 标准化模块 ID（统一大写下划线格式）
                String moduleId = upgradeId.toUpperCase();

                // 设置模块等级
                data.setModuleLevel(moduleId, newLevel);

                // 标记为脏，触发网络同步
                data.markDirty();

                System.out.println("[Capability] 设置模块 " + moduleId + " 为 Lv." + newLevel);
            }

            // ✅ 清除旧的暂停状态（保留在 NBT 中用于兼容性）
            NBTTagCompound nbt = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
            if (!core.hasTagCompound()) core.setTagCompound(nbt);

            if (newLevel > 0) {
                clearPauseState(nbt, upgradeId);
            }
        }

        // 为了向后兼容，保留旧签名（但现在需要玩家参数）
        @Deprecated
        private static void setLevelEverywhere(ItemStack core, String upgradeId, int newLevel) {
            // 这个方法不再工作，需要使用带玩家参数的版本
            System.err.println("[警告] 调用了过时的 setLevelEverywhere，请更新代码使用 Capability");
        }


        private static void writePauseMeta(ItemStack core, String upgradeId, int lastLevel, boolean paused) {
            if (core == null || core.isEmpty()) return;

            NBTTagCompound nbt = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
            if (!core.hasTagCompound()) core.setTagCompound(nbt);

            if (isWaterproofId(upgradeId)) {
                for (String wid : WATERPROOF_ALIASES) {
                    String U = up(wid), L = lo(wid);
                    if (paused && lastLevel > 0) {
                        nbt.setInteger(K_LAST_LEVEL + wid, lastLevel);
                        nbt.setInteger(K_LAST_LEVEL + U,   lastLevel);
                        nbt.setInteger(K_LAST_LEVEL + L,   lastLevel);
                        ensureOwnedMaxAtLeast(nbt, wid, lastLevel);
                        nbt.setBoolean("HasUpgrade_" + wid, true);
                        nbt.setBoolean("HasUpgrade_" + U,   true);
                        nbt.setBoolean("HasUpgrade_" + L,   true);
                    }
                    nbt.setBoolean(K_IS_PAUSED + wid, paused);
                    nbt.setBoolean(K_IS_PAUSED + U,   paused);
                    nbt.setBoolean(K_IS_PAUSED + L,   paused);
                }
            } else {
                String U = up(upgradeId), L = lo(upgradeId);
                if (paused && lastLevel > 0) {
                    for (String k : Arrays.asList(upgradeId, U, L)) {
                        nbt.setInteger(K_LAST_LEVEL + k, lastLevel);
                        ensureOwnedMaxAtLeast(nbt, k, lastLevel);
                        nbt.setBoolean("HasUpgrade_" + k, true);
                    }
                }
                for (String k : Arrays.asList(upgradeId, U, L)) {
                    nbt.setBoolean(K_IS_PAUSED + k, paused);
                }
            }
        }

        private static int getLevelAcross(ItemStack core, String id) {
            if (core == null || core.isEmpty()) return 0;
            NBTTagCompound nbt = core.getTagCompound();
            if (nbt == null) return 0;

            if (nbt.getBoolean(K_IS_PAUSED + id) ||
                    nbt.getBoolean(K_IS_PAUSED + up(id)) ||
                    nbt.getBoolean(K_IS_PAUSED + lo(id))) {
                return 0;
            }

            return getActualLevel(nbt, id);
        }

        private static int getOwnedMax(NBTTagCompound nbt, String id) {
            if (nbt == null) return 0;
            int v = 0;
            v = Math.max(v, nbt.getInteger(K_OWNED_MAX + id));
            v = Math.max(v, nbt.getInteger(K_OWNED_MAX + up(id)));
            v = Math.max(v, nbt.getInteger(K_OWNED_MAX + lo(id)));
            return v;
        }

        private static void ensureOwnedMaxAtLeast(NBTTagCompound nbt, String id, int atLeast) {
            if (nbt == null) return;
            for (String k : Arrays.asList(id, up(id), lo(id))) {
                if (nbt.getInteger(K_OWNED_MAX + k) < atLeast) {
                    nbt.setInteger(K_OWNED_MAX + k, atLeast);
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