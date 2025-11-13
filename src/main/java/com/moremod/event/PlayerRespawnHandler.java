package com.moremod.event;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.event.EnergyPunishmentSystem;
import com.moremod.util.UpgradeKeys;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 玩家死亡/重生：核心应急能量恢复 + 状态清理 + 一次性提示
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class PlayerRespawnHandler {

    // ===== 持久化到玩家的 NBT 键 =====
    private static final String PERSISTED = "PlayerPersisted";
    private static final String KEY_DEATH_RECOVERY = "moremod_DeathRecovery";
    private static final String KEY_DIED_FROM_ENERGY = "moremod_DiedFromEnergyLoss";

    // ===== 一次性提示控制 =====
    private static final String KEY_CORE_DESTROYED = "CoreDestroyed";
    private static final String KEY_CORE_DESTROYED_NOTIFIED = "CoreDestroyedNotified";

    // 兼容读取：发电模块 ID（大小写/新旧）
    private static final Set<String> GENERATORS = new HashSet<>(Arrays.asList(
            "KINETIC_GENERATOR", "SOLAR_GENERATOR", "VOID_ENERGY", "COMBAT_CHARGER"
    ));

    // ================== 事件：死亡时拷贝/记录状态 ==================
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        EntityPlayer oldP = event.getOriginal();
        EntityPlayer newP = event.getEntityPlayer();

        // 取旧核心
        ItemStack oldCore = ItemMechanicalCore.findEquippedMechanicalCore(oldP);
        boolean hasCore = !oldCore.isEmpty();

        // 计算是否因能量极低导致的死亡（<1%）
        boolean diedFromLowEnergy = false;
        if (hasCore) {
            IEnergyStorage es = oldCore.getCapability(CapabilityEnergy.ENERGY, null);
            if (es != null && es.getMaxEnergyStored() > 0) {
                float pct = (float) es.getEnergyStored() / es.getMaxEnergyStored();
                diedFromLowEnergy = pct < 0.01f;
            }
        }

        // 写到 玩家持久 NBT（PlayerPersisted），防止物品/背包变动导致标记丢失
        NBTTagCompound newED = newP.getEntityData();
        if (!newED.hasKey(PERSISTED, 10)) newED.setTag(PERSISTED, new NBTTagCompound());
        NBTTagCompound persisted = newED.getCompoundTag(PERSISTED);
        persisted.setBoolean(KEY_DEATH_RECOVERY, true);
        if (diedFromLowEnergy) persisted.setBoolean(KEY_DIED_FROM_ENERGY, true);

        // 同时也把标记写在旧核心的 NBT（若核心被保留可直接沿用）
        if (hasCore && oldCore.hasTagCompound()) {
            NBTTagCompound n = oldCore.getTagCompound();
            n.setBoolean("DeathRecovery", true);
            if (diedFromLowEnergy) n.setBoolean("DiedFromEnergyLoss", true);
        }
    }

    // ================== 事件：重生时恢复能量 + 清理 ==================
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        // 找核心（若找不到就不做任何事）
        ItemStack core = ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (core.isEmpty()) return;

        restoreEmergencyEnergyAndClean(player, core);
    }

    // ================== 核心逻辑 ==================
    private static void restoreEmergencyEnergyAndClean(EntityPlayer player, ItemStack core) {
        if (!core.hasTagCompound()) core.setTagCompound(new NBTTagCompound());
        NBTTagCompound nbt = core.getTagCompound();

        // 读取玩家持久化标记
        NBTTagCompound ed = player.getEntityData();
        if (!ed.hasKey(PERSISTED, 10)) ed.setTag(PERSISTED, new NBTTagCompound());
        NBTTagCompound persisted = ed.getCompoundTag(PERSISTED);

        boolean deathRecovery = persisted.getBoolean(KEY_DEATH_RECOVERY) || nbt.getBoolean("DeathRecovery");
        boolean diedFromEnergy = persisted.getBoolean(KEY_DIED_FROM_ENERGY) || nbt.getBoolean("DiedFromEnergyLoss");

        IEnergyStorage es = core.getCapability(CapabilityEnergy.ENERGY, null);
        if (es != null && es.getMaxEnergyStored() > 0) {
            int max = es.getMaxEnergyStored();
            int cur = es.getEnergyStored();
            int target;

            if (diedFromEnergy) {
                // 能量耗尽死亡：恢复到 30%
                target = Math.round(max * 0.30f);
                msg(player, TextFormatting.YELLOW, "⚡ 应急协议启动：核心恢复至30%能量");
                msg(player, TextFormatting.RED, "警告：避免再次能量耗尽！");
            } else if (deathRecovery) {
                // 普通死亡：恢复到 20%
                target = Math.round(max * 0.20f);
                msg(player, TextFormatting.GREEN, "⚡ 核心重启：恢复至20%能量");
            } else {
                // 无标记：至少 10%
                target = Math.max(cur, Math.round(max * 0.10f));
                if (cur < target) msg(player, TextFormatting.AQUA, "⚡ 最低能量保护：提升至10%");
            }

            if (cur < target) ensureEnergyAtLeast(core, es, target);

            // 清除标记（物品 & 玩家持久）
            cleanupDeathFlags(nbt, persisted);

            // 清惩罚


            // 发电模块提示
            checkAndNotifyGenerators(player, nbt);

            // “核心自毁”提示仅一次
            notifyCoreDestroyedOnce(player, nbt);
        }
    }

    // ================== 工具：能量设置（优先 capability，失败写 NBT 兜底） ==================
    private static void ensureEnergyAtLeast(ItemStack core, IEnergyStorage es, int target) {
        int cur = es.getEnergyStored();
        int need = Math.max(0, target - cur);
        if (need > 0) {
            int accepted = es.receiveEnergy(need, false);
            int after = es.getEnergyStored();
            if (accepted <= 0 && after < target) {
                // 有些自定义储能不允许直接改 capability，这里写 NBT 兜底（大小写都写）
                NBTTagCompound n = core.getTagCompound();
                if (n == null) { n = new NBTTagCompound(); core.setTagCompound(n); }
                n.setInteger("Energy", target);
                n.setInteger("energy", target);
            }
        }
    }

    // ================== 工具：只提示一次的“核心自毁” ==================
    private static void notifyCoreDestroyedOnce(EntityPlayer player, NBTTagCompound nbt) {
        if (nbt.getBoolean(KEY_CORE_DESTROYED) && !nbt.getBoolean(KEY_CORE_DESTROYED_NOTIFIED)) {
            // 只显示一次
            msgRaw(player, TextFormatting.DARK_RED + "════════════════════════");
            msg(player, TextFormatting.RED, "核心曾经自毁！以下升级可能已永久丢失：");

            // 这里仍然用示例名单；如果你已有实际丢失列表，可替换成真实枚举
            String[] lost = { "护甲强化", "速度提升", "护盾发生器", "飞行模块", "黄色护盾", "生命恢复", "伤害提升", "攻击速度" };
            for (String name : lost) {
                if (Math.random() < 0.7) {
                    msgRaw(player, TextFormatting.GRAY + "  ✗ " + name);
                }
            }
            msgRaw(player, TextFormatting.DARK_RED + "════════════════════════");

            nbt.setBoolean(KEY_CORE_DESTROYED_NOTIFIED, true); // 标记已提示
        }
    }

    // ================== 工具：发电模块状态提示（统一大小写、旧键名） ==================
    private static void checkAndNotifyGenerators(EntityPlayer player, NBTTagCompound nbt) {
        boolean any = false;

        StringBuilder sb = new StringBuilder();
        for (String id : GENERATORS) {
            int lv = readLevelCompat(nbt, id);
            if (lv > 0) {
                if (!any) {
                    msg(player, TextFormatting.AQUA, "发电模块状态：");
                    any = true;
                }
                TextFormatting c = TextFormatting.WHITE;
                if ("KINETIC_GENERATOR".equals(id)) c = TextFormatting.GREEN;
                else if ("SOLAR_GENERATOR".equals(id)) c = TextFormatting.YELLOW;
                else if ("VOID_ENERGY".equals(id)) c = TextFormatting.DARK_PURPLE;
                else if ("COMBAT_CHARGER".equals(id)) c = TextFormatting.RED;

                msgRaw(player, c + "  ✓ " + dispName(id) + " Lv." + lv);
            }
        }

        if (!any) {
            msg(player, TextFormatting.RED, "⚠ 警告：未检测到发电模块！");
            msg(player, TextFormatting.YELLOW, "建议立即安装发电升级");
        }
    }

    // ================== 工具：统一键名读写 ==================
    private static String canon(String s) {
        if (s == null) return "";
        String t = s.trim().replace('-', '_').replace(' ', '_');
        return t.toUpperCase(Locale.ROOT);
    }
    private static String lower(String s) { return canon(s).toLowerCase(Locale.ROOT); }

    /** 读取升级等级：兼容 upgrade_ID / upgrade_id / 直接 ID */
    private static int readLevelCompat(NBTTagCompound nbt, String id) {
        String U = canon(id), L = lower(id);
        int a = nbt.getInteger(UpgradeKeys.kUpgrade(U));
        int b = nbt.getInteger(UpgradeKeys.kUpgrade(L));
        int c = nbt.getInteger(U); // 某些旧代码可能直接存上去了
        return Math.max(a, Math.max(b, c));
    }

    /** 清除死亡相关标记（玩家+物品，大小写也顺便清） */
    private static void cleanupDeathFlags(NBTTagCompound itemNbt, NBTTagCompound persisted) {
        // 玩家持久
        persisted.removeTag(KEY_DEATH_RECOVERY);
        persisted.removeTag(KEY_DIED_FROM_ENERGY);

        // 物品
        if (itemNbt.hasKey("DeathRecovery")) itemNbt.removeTag("DeathRecovery");
        if (itemNbt.hasKey("deathrecovery")) itemNbt.removeTag("deathrecovery");
        if (itemNbt.hasKey("DiedFromEnergyLoss")) itemNbt.removeTag("DiedFromEnergyLoss");
        if (itemNbt.hasKey("diedfromenergyloss")) itemNbt.removeTag("diedfromenergyloss");
    }

    // ================== 文本工具 ==================
    private static void msg(EntityPlayer p, TextFormatting c, String text) {
        p.sendMessage(new TextComponentString(c + text));
    }
    private static void msgRaw(EntityPlayer p, String text) {
        p.sendMessage(new TextComponentString(text));
    }

    private static String dispName(String id) {
        switch (canon(id)) {
            case "KINETIC_GENERATOR": return "动能发电";
            case "SOLAR_GENERATOR":   return "太阳能";
            case "VOID_ENERGY":       return "虚空能量";
            case "COMBAT_CHARGER":    return "战斗充能";
            default: return id;
        }
    }
}
