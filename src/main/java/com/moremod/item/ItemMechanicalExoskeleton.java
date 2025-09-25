package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.item.ItemMechanicalCore.UpgradeType;
import com.moremod.item.ItemMechanicalCoreExtended.UpgradeInfo;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 机械外骨骼（饰品）- 1.12.2 + GeckoLib 3.0.30
 * - 佩戴条件：激活模块（按等级求和）达到阈值
 * - 伤害加成：每个“激活等级”+10%，并叠加能量效率同步加成
 * - 带简单 idle 动画（动画名 animation.exoskeleton.idle）
 */
public class ItemMechanicalExoskeleton extends Item implements IBauble{

    // ===== NBT Keys (on ring) =====
    private static final String NBT_CACHED_ACTIVE = "CachedActive"; // 缓存“激活模块数（按等级合计）”
    private static final String NBT_SYNC_BONUS    = "SyncBonus";    // 同步加成（能效带来的额外%）
    private static final String NBT_LAST_UPDATE   = "LastUpdateTime";

    // 每个“激活等级”提供的伤害加成
    private static final float DAMAGE_BONUS_PER_ACTIVE_LEVEL = 0.10f; // 10%

    // ===== GeckoLib =====

    public ItemMechanicalExoskeleton() {
        setTranslationKey("mechanical_exoskeleton");
        setRegistryName("mechanical_exoskeleton");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    // ===== GeckoLib hooks =====





    // ===== Baubles =====
    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.RING;
    }

    // 佩戴前置限制：激活模块数达不到阈值时，禁止佩戴
    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return false;
        EntityPlayer p = (EntityPlayer) entity;
        ItemStack core = ItemMechanicalCore.findEquippedMechanicalCore(p);
        if (!ItemMechanicalCore.isMechanicalCore(core)) {
            if (!p.world.isRemote) p.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 需要佩戴机械核心"), true);
            return false;
        }
        int active = countActiveModules(p, core, ExoConfig.countGenerators());
        int need = ExoConfig.minActive();
        boolean ok = active >= need;
        if (!ok && !p.world.isRemote) {
            p.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "⚠ 激活模块不足（" + active + "/" + need + "）"), true);
        }
        return ok;
    }

    @Override
    public void onWornTick(ItemStack ring, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer p = (EntityPlayer) entity;
        if (p.world.isRemote) return;

        // 每秒刷新一次可视化缓存（供 tooltip 用）
        if (p.world.getTotalWorldTime() % 20 == 0) {
            ItemStack core = ItemMechanicalCore.findEquippedMechanicalCore(p);
            NBTTagCompound nbt = ring.getTagCompound();
            if (nbt == null) { nbt = new NBTTagCompound(); ring.setTagCompound(nbt); }

            if (ItemMechanicalCore.isMechanicalCore(core)) {
                int active = countActiveModules(p, core, ExoConfig.countGenerators());
                float sync = calculateSyncBonus(core);
                nbt.setInteger(NBT_CACHED_ACTIVE, active);
                nbt.setFloat(NBT_SYNC_BONUS, sync);
                nbt.setLong(NBT_LAST_UPDATE, p.world.getTotalWorldTime());
            } else {
                nbt.setInteger(NBT_CACHED_ACTIVE, 0);
                nbt.setFloat(NBT_SYNC_BONUS, 0f);
            }
        }
    }

    @Override
    public void onEquipped(ItemStack ring, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote) return;
        EntityPlayer p = (EntityPlayer) entity;

        ItemStack core = ItemMechanicalCore.findEquippedMechanicalCore(p);
        if (!ItemMechanicalCore.isMechanicalCore(core)) {
            p.sendMessage(new TextComponentString(TextFormatting.RED + "⚠ 需要佩戴机械核心"));
            return;
        }

        int active = countActiveModules(p, core, ExoConfig.countGenerators());
        float sync = calculateSyncBonus(core);
        float total = (active * DAMAGE_BONUS_PER_ACTIVE_LEVEL + sync) * 100f;

        // 写入缓存供 tooltip 用
        NBTTagCompound nbt = ring.getTagCompound();
        if (nbt == null) { nbt = new NBTTagCompound(); ring.setTagCompound(nbt); }
        nbt.setInteger(NBT_CACHED_ACTIVE, active);
        nbt.setFloat(NBT_SYNC_BONUS, sync);

        p.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "⚔ 外骨骼激活 "
                        + TextFormatting.WHITE + "(激活模块: " + active
                        + TextFormatting.GRAY + " / 阈值 " + ExoConfig.minActive()
                        + TextFormatting.WHITE + ", 增益 "
                        + TextFormatting.YELLOW + "+" + String.format(Locale.ROOT,"%.0f%%", total) + ")"));
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer) || entity.world.isRemote) return;
        ((EntityPlayer) entity).sendStatusMessage(new TextComponentString(
                TextFormatting.GRAY + "⚔ 机械外骨骼已停用"), true);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getCachedActive(stack) > 0;
    }

    // ===== Tooltip =====
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tip, ITooltipFlag flag) {
        tip.add("");
        tip.add(TextFormatting.GOLD + "═══ 机械外骨骼 ═══");
        tip.add(TextFormatting.GRAY + "饰品类型: " + TextFormatting.WHITE + "手部");
        tip.add("");

        int active = getCachedActive(stack);
        float sync = getSyncBonus(stack);
        int need = ExoConfig.minActive();

        if (active > 0) {
            float dmg = active * DAMAGE_BONUS_PER_ACTIVE_LEVEL * 100f;
            float total = dmg + sync * 100f;
            tip.add(TextFormatting.AQUA + "激活模块(按等级): " + TextFormatting.WHITE + active);
            tip.add(TextFormatting.YELLOW + "基础伤害加成: " + TextFormatting.WHITE + "+" + fmt(dmg) + "%");
            if (sync > 0) tip.add(TextFormatting.GREEN + "能效同步加成: " + TextFormatting.WHITE + "+" + fmt(sync * 100f) + "%");
            tip.add(TextFormatting.GOLD + "总伤害加成: " + TextFormatting.WHITE + "+" + fmt(total) + "%");
        } else {
            tip.add(TextFormatting.RED + "未检测到激活模块");
        }

        tip.add(TextFormatting.GRAY + "佩戴需求: " + TextFormatting.WHITE + "激活模块 ≥ " + need
                + TextFormatting.DARK_GRAY + " (发电计入: " + (ExoConfig.countGenerators() ? "是" : "否") + ")");
        tip.add("");
        tip.add(TextFormatting.DARK_PURPLE + "按住 Shift 查看详情");
        if (GuiScreen.isShiftKeyDown()) {
            tip.add("");
            tip.add(TextFormatting.GOLD + "说明:");
            tip.add(TextFormatting.GRAY + "• 只统计“激活”的模块等级：未启用/被暂停/被能量档位关闭的不计入");
            tip.add(TextFormatting.GRAY + "• 伤害加成与其他来源可叠加");
            tip.add(TextFormatting.GRAY + "• 阈值、是否计入发电可在 config 配置");
        }
    }

    // ===== Helpers =====
    private static String fmt(float v) { return String.format(Locale.ROOT,"%.0f", v); }
    private static String U(String s){ return s==null? "" : s.toUpperCase(Locale.ROOT); }

    /** 计算“激活模块数”（把各模块等级相加） */
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

        // 扩展（排除 BASIC，避免重复计入）
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

    private static boolean isGenerator(String id) {
        String u = U(id);
        return u.contains("GENERATOR") || u.contains("CHARGER") ||
                u.equals("SOLAR_GENERATOR") || u.equals("KINETIC_GENERATOR") ||
                u.equals("THERMAL_GENERATOR") || u.equals("VOID_ENERGY") ||
                u.equals("COMBAT_CHARGER");
    }

    private static boolean isPaused(ItemStack core, String id) {
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return false;
        return nbt.getBoolean("IsPaused_" + id) || nbt.getBoolean("IsPaused_" + U(id));
    }

    /** 与 GUI 一致的能量档位开关规则 */
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

    /** 能量效率带来的额外%加成（每级 +2%） */
    private static float calculateSyncBonus(ItemStack core) {
        int eff = ItemMechanicalCore.getUpgradeLevel(core, UpgradeType.ENERGY_EFFICIENCY);
        return eff * 0.02f;
    }

    public static int getCachedActive(ItemStack ring) {
        if (!ring.hasTagCompound()) return 0;
        return ring.getTagCompound().getInteger(NBT_CACHED_ACTIVE);
    }

    public static float getSyncBonus(ItemStack ring) {
        if (!ring.hasTagCompound()) return 0f;
        return ring.getTagCompound().getFloat(NBT_SYNC_BONUS);
    }

    // ===== 简易配置载入（专用于外骨骼） =====
    public static final class ExoConfig {
        private static boolean loaded = false;
        private static int minActive = 6;
        private static boolean countGenerators = false;

        public static int minActive() {
            ensureLoaded();
            return minActive;
        }
        public static boolean countGenerators() {
            ensureLoaded();
            return countGenerators;
        }

        private static void ensureLoaded() {
            if (loaded) return;
            loaded = true;
            try {
                File cfgDir = Loader.instance().getConfigDir();
                File f = new File(cfgDir, "moremod_exoskeleton.cfg");
                net.minecraftforge.common.config.Configuration cfg =
                        new net.minecraftforge.common.config.Configuration(f);
                cfg.load();
                minActive = Math.max(0, cfg.getInt(
                        "minActiveModules", "general", 6,
                        0, 999, "佩戴外骨骼需要的最低“激活模块等级总和”"));
                countGenerators = cfg.getBoolean(
                        "countGeneratorsInActive", "general", false,
                        "是否把发电/充能类模块计入“激活模块数”");
                if (cfg.hasChanged()) cfg.save();
            } catch (Throwable ignored) {}
        }
    }
}
