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

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.util.BaublesSyncUtil;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 机械核心升级同步：GUI -> 服务器
 */
public class PacketMechanicalCoreUpdate implements IMessage {

    public enum Action { SET_LEVEL }

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
        @Override
        public IMessage onMessage(PacketMechanicalCoreUpdate msg, MessageContext ctx) {
            final EntityPlayerMP serverPlayer = ctx.getServerHandler().player;

            serverPlayer.getServerWorld().addScheduledTask(() -> {
                if (msg.action != Action.SET_LEVEL) return;

                String id = msg.upgradeId == null ? "" : msg.upgradeId.trim();
                if (id.isEmpty()) return;

                ItemStack core = ItemMechanicalCore.findEquippedMechanicalCore(serverPlayer);
                if (core.isEmpty() || !(core.getItem() instanceof ItemMechanicalCore)) {
                    serverPlayer.sendMessage(new TextComponentString(
                            TextFormatting.RED + "请先装备机械核心再进行设置。"));
                    return;
                }

                NBTTagCompound nbt = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
                if (!core.hasTagCompound()) core.setTagCompound(nbt);

                int requested = Math.max(0, msg.level);
                int current   = getLevelAcross(core, id);
                int ownedMax  = getOwnedMax(nbt, id);

                // 安全上限兜底
                final int ABS_MAX = 64;
                if (requested > ABS_MAX) requested = ABS_MAX;

                // 惩罚期约束：超过 cap 的目标会被夹回（如果你实现了 isPenalized/getPenaltyCap）
                if (requested > current && isPenalizedSafe(core, id)) {
                    int cap = Math.max(1, getPenaltyCapSafe(core, id));
                    if (requested > cap) {
                        serverPlayer.sendMessage(new TextComponentString(
                                TextFormatting.LIGHT_PURPLE + "🔒 惩罚中：最高仅允许 Lv." + cap + "，已拒绝更高设置。"));
                        requested = cap;
                    }
                }

                // 设置为 0 = 暂停（记录 LastLevel & IsPaused）
                if (requested == 0) {
                    if (current > 0) {
                        writePauseMeta(core, id, current, true);
                        setLevelEverywhere(core, id, 0);
                        ensureOwnedMaxAtLeast(nbt, id, current);
                        serverPlayer.sendMessage(new TextComponentString(
                                TextFormatting.YELLOW + "⏸ 已暂停 " + prettyName(id) + "（点击 + 可恢复）"));
                    }
                    syncDirty(serverPlayer);
                    return;
                }

                // 恢复/升级：清理暂停标记
                writePauseMeta(core, id, requested, false);

                // 抬升 OwnedMax（记录历史最高）
                if (requested > ownedMax) {
                    ensureOwnedMaxAtLeast(nbt, id, requested);
                }

                // 真正落盘（NBT + 扩展 + 基础枚举同步）
                setLevelEverywhere(core, id, requested);

                // 提示（若仍处于惩罚状态，告知会被“临时上限”限制）
                if (isPenalizedSafe(core, id)) {
                    serverPlayer.sendMessage(new TextComponentString(
                            TextFormatting.AQUA + "↑ " + prettyName(id) + " 设为 Lv." + requested +
                                    TextFormatting.LIGHT_PURPLE + "（惩罚中，超过临时上限会被限制）"));
                } else {
                    serverPlayer.sendMessage(new TextComponentString(
                            TextFormatting.GREEN + "✓ " + prettyName(id) + " 设为 Lv." + requested));
                }

                syncDirty(serverPlayer);
            });

            return null;
        }

        // ================= 工具方法 =================

        private static final Set<String> WATERPROOF_ALIASES = new HashSet<>(Arrays.asList(
                "WATERPROOF_MODULE","WATERPROOF","waterproof_module","waterproof"
        ));

        private static String up(String s){ return s == null ? "" : s.toUpperCase(); }
        private static String lo(String s){ return s == null ? "" : s.toLowerCase(); }

        private static boolean isWaterproofId(String id) {
            if (id == null) return false;
            String u = up(id);
            return WATERPROOF_ALIASES.contains(u) || u.contains("WATERPROOF");
        }

        private static void setLevelEverywhere(ItemStack core, String upgradeId, int newLevel) {
            if (core == null || core.isEmpty()) return;

            NBTTagCompound nbt = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
            if (!core.hasTagCompound()) core.setTagCompound(nbt);

            if (isWaterproofId(upgradeId)) {
                for (String wid : WATERPROOF_ALIASES) {
                    String U = up(wid), L = lo(wid);
                    nbt.setInteger("upgrade_" + wid, newLevel);
                    nbt.setInteger("upgrade_" + U,   newLevel);
                    nbt.setInteger("upgrade_" + L,   newLevel);
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

                // 基础枚举中若存在也同步
                try {
                    for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
                        if (isWaterproofId(t.getKey())) {
                            ItemMechanicalCore.setUpgradeLevel(core, t, newLevel);
                        }
                    }
                } catch (Throwable ignored) {}
            } else {
                String U = up(upgradeId), L = lo(upgradeId);

                nbt.setInteger("upgrade_" + upgradeId, newLevel);
                nbt.setInteger("upgrade_" + U,         newLevel);
                nbt.setInteger("upgrade_" + L,         newLevel);
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

                // 若是基础枚举升级，也同步
                try {
                    for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
                        if (t.getKey().equalsIgnoreCase(upgradeId)) {
                            ItemMechanicalCore.setUpgradeLevel(core, t, newLevel);
                            break;
                        }
                    }
                } catch (Throwable ignored) {}
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
                        nbt.setInteger("LastLevel_" + wid, lastLevel);
                        nbt.setInteger("LastLevel_" + U,   lastLevel);
                        nbt.setInteger("LastLevel_" + L,   lastLevel);
                        ensureOwnedMaxAtLeast(nbt, wid, lastLevel);
                        nbt.setBoolean("HasUpgrade_" + wid, true);
                        nbt.setBoolean("HasUpgrade_" + U,   true);
                        nbt.setBoolean("HasUpgrade_" + L,   true);
                    }
                    nbt.setBoolean("IsPaused_" + wid, paused);
                    nbt.setBoolean("IsPaused_" + U,   paused);
                    nbt.setBoolean("IsPaused_" + L,   paused);
                }
            } else {
                String U = up(upgradeId), L = lo(upgradeId);
                if (paused && lastLevel > 0) {
                    for (String k : Arrays.asList(upgradeId, U, L)) {
                        nbt.setInteger("LastLevel_" + k, lastLevel);
                        ensureOwnedMaxAtLeast(nbt, k, lastLevel);
                        nbt.setBoolean("HasUpgrade_" + k, true);
                    }
                }
                for (String k : Arrays.asList(upgradeId, U, L)) {
                    nbt.setBoolean("IsPaused_" + k, paused);
                }
            }
        }

        private static int getLevelAcross(ItemStack core, String id) {
            if (core == null || core.isEmpty()) return 0;
            NBTTagCompound nbt = core.getTagCompound();
            int lv = 0;

            if (nbt != null) {
                // 暂停视作 0
                if (nbt.getBoolean("IsPaused_" + id) ||
                        nbt.getBoolean("IsPaused_" + up(id)) ||
                        nbt.getBoolean("IsPaused_" + lo(id))) {
                    return 0;
                }
                lv = Math.max(lv, nbt.getInteger("upgrade_" + id));
                lv = Math.max(lv, nbt.getInteger("upgrade_" + up(id)));
                lv = Math.max(lv, nbt.getInteger("upgrade_" + lo(id)));
            }

            try {
                lv = Math.max(lv, ItemMechanicalCoreExtended.getUpgradeLevel(core, id));
                lv = Math.max(lv, ItemMechanicalCoreExtended.getUpgradeLevel(core, up(id)));
                lv = Math.max(lv, ItemMechanicalCoreExtended.getUpgradeLevel(core, lo(id)));
            } catch (Throwable ignored) {}

            try {
                for (ItemMechanicalCore.UpgradeType t : ItemMechanicalCore.UpgradeType.values()) {
                    if (t.getKey().equalsIgnoreCase(id)) {
                        lv = Math.max(lv, ItemMechanicalCore.getUpgradeLevel(core, t));
                        break;
                    }
                }
            } catch (Throwable ignored) {}

            return lv;
        }

        private static int getOwnedMax(NBTTagCompound nbt, String id) {
            if (nbt == null) return 0;
            int v = 0;
            v = Math.max(v, nbt.getInteger("OwnedMax_" + id));
            v = Math.max(v, nbt.getInteger("OwnedMax_" + up(id)));
            v = Math.max(v, nbt.getInteger("OwnedMax_" + lo(id)));
            return v;
        }

        private static void ensureOwnedMaxAtLeast(NBTTagCompound nbt, String id, int atLeast) {
            if (nbt == null) return;
            for (String k : Arrays.asList(id, up(id), lo(id))) {
                if (nbt.getInteger("OwnedMax_" + k) < atLeast) {
                    nbt.setInteger("OwnedMax_" + k, atLeast);
                }
            }
        }

        private static void syncDirty(EntityPlayerMP p) {
            try {
                // 背包与容器
                p.inventory.markDirty();
                p.inventoryContainer.detectAndSendChanges();
                // Baubles 同步（兼容不同版本）
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

        // ======= 惩罚期安全调用（若项目未实现相应方法，则视为不在惩罚期） =======
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
