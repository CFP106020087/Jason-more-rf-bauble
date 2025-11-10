package com.moremod.eventHandler;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCore.UpgradeType;
import com.moremod.item.ItemMechanicalCoreExtended;
import com.moremod.item.ItemMechanicalCoreExtended.UpgradeInfo;
import com.moremod.item.ItemMechanicalExoskeleton;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/**
 * 机械外骨骼事件处理器（按"激活模块数"工作）
 * - 伤害加成 = (激活等级合计 * 配置倍率) + 同步加成
 * - 激活数低于阈值时，自动从 Baubles 弹出饰品
 * - 阈值与是否计入发电模块可在 config/moremod.cfg 配置
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class MechanicalExoskeletonEventHandler {

    // ===== 配置值（通过静态块加载）=====
    private static float DAMAGE_BONUS_PER_ACTIVE_LEVEL;
    private static float ENERGY_SYNC_BONUS;

    static {
        com.moremod.config.ItemConfig.ensureLoaded();
        DAMAGE_BONUS_PER_ACTIVE_LEVEL = com.moremod.config.ItemConfig.MechanicalExoskeleton.damageBonus;
        ENERGY_SYNC_BONUS = com.moremod.config.ItemConfig.MechanicalExoskeleton.energySyncBonus;
    }

    // ===== 缓存每个玩家的"激活模块数/倍率" =====
    private static final Map<UUID, Cached> CACHE = new HashMap<>();

    private static class Cached {
        int activeLevels;      // 激活模块"等级"合计
        float syncBonus;       // 能效同步加成（0.02/级）
        float totalMultiplier = 1.0f; // ✅ 修复：默认值设为 1.0，避免初始化为0导致伤害归零
        long lastUpdate;
        boolean hasExo;
        boolean hasCore;
    }

    // ===== 周期刷新缓存 =====
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.world.isRemote) return;
        EntityPlayer p = e.player;
        if (p.world.getTotalWorldTime() % 20 != 0) return; // 每秒一次
        updateCache(p);
    }

    private static void updateCache(EntityPlayer p) {
        UUID id = p.getUniqueID();
        Cached c = CACHE.computeIfAbsent(id, k -> new Cached());

        ItemStack exo = findExo(p);
        c.hasExo = !exo.isEmpty();

        if (c.hasExo) {
            ItemStack core = ItemMechanicalCore.findEquippedMechanicalCore(p);
            c.hasCore = ItemMechanicalCore.isMechanicalCore(core);
            if (c.hasCore) {
                int act = countActiveModules(p, core, Cfg.countGenerators);
                float sync = calcSync(core);
                c.activeLevels = act;
                c.syncBonus = sync;
                c.totalMultiplier = 1.0f + act * DAMAGE_BONUS_PER_ACTIVE_LEVEL + sync;

                // 写入饰品 NBT 供 tooltip
                writeExoNbt(exo, act, sync);

                // 不足阈值 -> 弹出
                if (act < Cfg.minActive) {
                    ejectExo(p);
                }
            } else {
                c.activeLevels = 0;
                c.syncBonus = 0;
                c.totalMultiplier = 1.0f;
                writeExoNbt(exo, 0, 0f);
            }
        } else {
            c.activeLevels = 0;
            c.syncBonus = 0;
            c.totalMultiplier = 1.0f;
            c.hasCore = false;
        }

        c.lastUpdate = p.world.getTotalWorldTime();
    }

    // ===== 伤害加成（最后执行） =====
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingDamageEvent e) {
        if (!(e.getSource().getTrueSource() instanceof EntityPlayer)) return;
        EntityPlayer p = (EntityPlayer) e.getSource().getTrueSource();

        // ✅ 修复：在伤害事件中立即更新缓存，确保数据实时性
        updateCache(p);

        Cached c = CACHE.get(p.getUniqueID());
        if (c == null || !c.hasExo || !c.hasCore || c.activeLevels <= 0) return;

        float finalDamage = e.getAmount() * c.totalMultiplier;
        e.setAmount(finalDamage);

        // 华丽一点的高伤害特效（可选）
        if (!p.world.isRemote && finalDamage > 20f) {
            for (int i = 0; i < 5; i++) {
                double ox = (p.world.rand.nextDouble() - 0.5) * 0.5;
                double oy = p.world.rand.nextDouble() * 0.5;
                double oz = (p.world.rand.nextDouble() - 0.5) * 0.5;
                e.getEntity().world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.CRIT_MAGIC,
                        e.getEntity().posX + ox, e.getEntity().posY + e.getEntity().height / 2 + oy,
                        e.getEntity().posZ + oz, ox * 0.1, 0.1, oz * 0.1);
            }
        }
    }

    // ===== 统计"激活模块等级合计" =====
    private static int countActiveModules(EntityPlayer p, ItemStack core, boolean countGenerators) {
        int total = 0;

        // 基础
        for (UpgradeType t : UpgradeType.values()) {
            String id = t.getKey();
            if (!countGenerators && isGenerator(id)) continue;

            int lv = ItemMechanicalCore.getUpgradeLevel(core, t);
            if (lv <= 0) continue;
            if (isPaused(core, id)) continue;
            if (!allowedByEnergy(core, id)) continue;

            total += lv;
        }

        // 扩展（排除 BASIC）
        try {
            for (Map.Entry<String, UpgradeInfo> e : ItemMechanicalCoreExtended.getAllUpgrades().entrySet()) {
                String id = e.getKey();
                UpgradeInfo info = e.getValue();
                if (info == null) continue;
                if (info.category == ItemMechanicalCoreExtended.UpgradeCategory.BASIC) continue;
                if (!countGenerators && isGenerator(id)) continue;

                int lv = ItemMechanicalCoreExtended.getUpgradeLevel(core, id);
                if (lv <= 0) continue;
                if (isPaused(core, id)) continue;
                if (!allowedByEnergy(core, id)) continue;

                total += lv;
            }
        } catch (Throwable ignored) {}

        return total;
    }

    // ===== 工具：开关判断与分类 =====
    private static String U(String s){ return s==null? "" : s.toUpperCase(Locale.ROOT); }

    private static boolean isPaused(ItemStack core, String id) {
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return false;
        return nbt.getBoolean("IsPaused_" + id) || nbt.getBoolean("IsPaused_" + U(id));
    }

    private static boolean isGenerator(String id) {
        String u = U(id);
        return u.contains("GENERATOR") || u.contains("CHARGER")
                || u.equals("SOLAR_GENERATOR") || u.equals("KINETIC_GENERATOR")
                || u.equals("THERMAL_GENERATOR") || u.equals("VOID_ENERGY")
                || u.equals("COMBAT_CHARGER");
    }

    private static boolean allowedByEnergy(ItemStack core, String id) {
        EnergyDepletionManager.EnergyStatus st = EnergyDepletionManager.getCurrentEnergyStatus(core);
        String u = U(id);
        switch (st) {
            case NORMAL: return true;
            case POWER_SAVING:
                return !(u.equals("ORE_VISION") || u.equals("STEALTH") || u.equals("FLIGHT_MODULE")
                        || u.equals("KINETIC_GENERATOR") || u.equals("SOLAR_GENERATOR"));
            case EMERGENCY:
                return u.equals("HEALTH_REGEN") || u.equals("REGENERATION")
                        || u.equals("YELLOW_SHIELD") || u.equals("SHIELD_GENERATOR")
                        || u.equals("DAMAGE_BOOST") || u.equals("ARMOR_ENHANCEMENT")
                        || u.contains("WATERPROOF");
            case CRITICAL:
                return u.equals("HEALTH_REGEN") || u.equals("REGENERATION")
                        || u.equals("FIRE_EXTINGUISH") || u.equals("THORNS")
                        || u.contains("WATERPROOF");
            default: return true;
        }
    }

    private static float calcSync(ItemStack core) {
        int eff = ItemMechanicalCore.getUpgradeLevel(core, UpgradeType.ENERGY_EFFICIENCY);
        return eff * ENERGY_SYNC_BONUS;
    }

    // ===== 从 Baubles 找饰品、写入 NBT、弹出饰品 =====
    private static ItemStack findExo(EntityPlayer p) {
        IBaublesItemHandler h = BaublesApi.getBaublesHandler(p);
        if (h != null) {
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack s = h.getStackInSlot(i);
                if (!s.isEmpty() && s.getItem().getRegistryName() != null
                        && "mechanical_exoskeleton".equals(s.getItem().getRegistryName().getPath())) {
                    return s;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static void writeExoNbt(ItemStack exo, int active, float sync) {
        NBTTagCompound nbt = exo.getTagCompound();
        if (nbt == null) { nbt = new NBTTagCompound(); exo.setTagCompound(nbt); }
        nbt.setInteger("CachedActive", active);
        nbt.setFloat("SyncBonus", sync);
    }

    private static void ejectExo(EntityPlayer p) {
        IBaublesItemHandler h = BaublesApi.getBaublesHandler(p);
        if (h == null) return;
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (s.isEmpty()) continue;
            if (s.getItem().getRegistryName() != null
                    && "mechanical_exoskeleton".equals(s.getItem().getRegistryName().getPath())) {
                ItemStack copy = s.copy();
                h.setStackInSlot(i, ItemStack.EMPTY);

                boolean stored = p.inventory.addItemStackToInventory(copy);
                if (!stored) p.dropItem(copy, true);

                p.world.playSound(null, p.posX, p.posY, p.posZ,
                        SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.7f, 1.2f);
                p.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 激活模块不足（需 ≥ " + Cfg.minActive + "），外骨骼已解除佩戴"), true);
                break;
            }
        }
    }

    // ===== 配置辅助类（使用ItemConfig） =====
    private static final class Cfg {
        static int minActive;
        static boolean countGenerators;

        static {
            com.moremod.config.ItemConfig.ensureLoaded();
            minActive = com.moremod.config.ItemConfig.MechanicalExoskeleton.minActiveModules;
            countGenerators = com.moremod.config.ItemConfig.MechanicalExoskeleton.countGenerators;
        }
    }
}