package com.moremod.core.system;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.core.api.CoreUpgradeEntry;
import com.moremod.core.api.IMechanicalCoreData;
import com.moremod.core.capability.MechanicalCoreCapability;
import com.moremod.core.registry.UpgradeRegistry;
import com.moremod.item.ItemMechanicalCore;
import com.moremod.upgrades.energy.EnergyDepletionManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 新的惩罚系统 - 基于Capability
 *
 * 功能：
 * - 能量耗尽惩罚（DOT伤害、装备耐久损失、模块降级）
 * - 自毁倒计时系统
 * - 模块降级与修复
 * - 完全不直接操作NBT，只通过Capability API
 */
public class CorePunishmentSystem {

    public static final CorePunishmentSystem INSTANCE = new CorePunishmentSystem();

    // ===== 配置参数 =====
    private static final int DOT_DAMAGE_PER_TICK = 1;
    private static final int DEGRADE_MODULE_COUNT = 2;

    private static final long TICK_1S = 20;
    private static final long TICK_5S = 20 * 5;
    private static final long TICK_10S = 20 * 10;
    private static final long TICK_15S = 20 * 15;
    private static final long TICK_20S = 20 * 20;

    // 发电模块列表（这些模块不会被降级）
    private static final Set<String> GENERATOR_MODULES = new HashSet<>(Arrays.asList(
            "SOLAR_GENERATOR", "KINETIC_GENERATOR", "THERMAL_GENERATOR",
            "VOID_ENERGY", "COMBAT_CHARGER"
    ));

    // NBT键（仅用于存储惩罚系统的临时状态，不存储升级数据）
    private static final String K_LAST_DOT = "Punish_LastDot";
    private static final String K_LAST_DEGRADE = "Punish_LastDegrade";
    private static final String K_LAST_DURABILITY = "Punish_LastDur";
    private static final String K_CRITICAL_SINCE = "Punish_CriticalSince";
    private static final String K_SELF_DESTRUCT_DONE = "Punish_SelfDestructDone";
    private static final String K_WARNING_10S = "Punish_Warning10s";
    private static final String K_WARNING_5S = "Punish_Warning5s";
    private static final String K_SPECIAL_DEATH_FLAG = "MechanicalCoreDeath";

    // ===== 伤害源 =====

    public static class EnergyDepletionDamage extends DamageSource {
        public EnergyDepletionDamage() {
            super("energy_depletion");
            this.setDamageBypassesArmor();
        }

        @Override
        public ITextComponent getDeathMessage(EntityLivingBase entity) {
            String playerName = entity.getDisplayName().getUnformattedText();
            return new TextComponentString(TextFormatting.RED + playerName + " 因机械核心能量耗尽而死亡");
        }
    }

    public static class MechanicalCoreDeathSource extends DamageSource {
        private final String deathMessage;

        public MechanicalCoreDeathSource(String name, String deathMessage) {
            super(name);
            this.deathMessage = deathMessage;
            this.setDamageBypassesArmor();
            this.setDamageIsAbsolute();
        }

        @Override
        public ITextComponent getDeathMessage(EntityLivingBase entity) {
            String playerName = entity.getDisplayName().getUnformattedText();
            return new TextComponentString(TextFormatting.DARK_RED + playerName + deathMessage);
        }
    }

    private static final DamageSource ENERGY_DEPLETION = new EnergyDepletionDamage();
    private static final DamageSource GEAR_STOP = new MechanicalCoreDeathSource(
            "mechanical_core_selfdestruct", " 不该让齿轮停下...."
    );

    // ===== 主要方法 =====

    /**
     * 每tick调用（从EnergyDepletionManager调用）
     */
    public static void tick(ItemStack core, EntityPlayer player, EnergyDepletionManager.EnergyStatus status) {
        if (core == null || core.isEmpty() || player == null || player.world == null || player.world.isRemote) {
            return;
        }

        // 获取核心NBT（仅用于存储惩罚系统状态）
        NBTTagCompound nbt = getOrCreateNBT(core);
        long time = player.world.getTotalWorldTime();

        if (status == EnergyDepletionManager.EnergyStatus.CRITICAL) {
            if (!nbt.hasKey(K_CRITICAL_SINCE)) {
                nbt.setLong(K_CRITICAL_SINCE, time);
                nbt.removeTag(K_WARNING_10S);
                nbt.removeTag(K_WARNING_5S);
                nbt.removeTag(K_SELF_DESTRUCT_DONE);
            }
            handleSelfDestructCountdown(core, player, nbt, time);
        } else {
            if (nbt.hasKey(K_CRITICAL_SINCE)) {
                nbt.removeTag(K_CRITICAL_SINCE);
                nbt.removeTag(K_WARNING_10S);
                nbt.removeTag(K_WARNING_5S);
                nbt.removeTag(K_SELF_DESTRUCT_DONE);
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GREEN + "✓ 自毁序列已取消"), true);
            }
        }

        applyPunishments(core, player, status, nbt, time);
    }

    /**
     * 自毁倒计时处理
     */
    private static void handleSelfDestructCountdown(ItemStack core, EntityPlayer player,
                                                    NBTTagCompound nbt, long time) {
        long criticalStart = nbt.getLong(K_CRITICAL_SINCE);
        long elapsed = time - criticalStart;

        if (elapsed >= TICK_10S && !nbt.getBoolean(K_WARNING_10S)) {
            nbt.setBoolean(K_WARNING_10S, true);
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "⚠⚠⚠ 核心自毁倒计时：10秒 ⚠⚠⚠"));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "立即补充能量以取消自毁序列！"));
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_NOTE_BASS, SoundCategory.PLAYERS, 1.0f, 0.5f);
        }

        if (elapsed >= TICK_15S && !nbt.getBoolean(K_WARNING_5S)) {
            nbt.setBoolean(K_WARNING_5S, true);
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "☠☠☠ 核心自毁倒计时：5秒！！！ ☠☠☠"));
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1.0f, 2.0f);

            for (int i = 0; i < 10; i++) {
                player.world.spawnParticle(EnumParticleTypes.REDSTONE,
                        player.posX + (Math.random() - 0.5) * 2,
                        player.posY + 1,
                        player.posZ + (Math.random() - 0.5) * 2,
                        1.0, 0.0, 0.0);
            }
        }

        if (elapsed >= TICK_20S - 60) {
            long secondsLeft = (TICK_20S - elapsed) / 20;
            if (secondsLeft <= 3 && secondsLeft > 0 && elapsed % 20 == 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "☠ " + secondsLeft + " ☠"), true);
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 1.0f, 0.5f);
            }
        }

        if (elapsed >= TICK_20S && !nbt.getBoolean(K_SELF_DESTRUCT_DONE)) {
            selfDestruct(core, player);
            nbt.setBoolean(K_SELF_DESTRUCT_DONE, true);
        }
    }

    /**
     * 应用惩罚
     */
    private static void applyPunishments(ItemStack core, EntityPlayer player,
                                        EnergyDepletionManager.EnergyStatus status,
                                        NBTTagCompound nbt, long time) {

        if (status == EnergyDepletionManager.EnergyStatus.CRITICAL) {
            // DOT伤害
            if (checkCooldown(nbt, K_LAST_DOT, time, TICK_1S)) {
                player.attackEntityFrom(ENERGY_DEPLETION, DOT_DAMAGE_PER_TICK);
                nbt.setLong(K_LAST_DOT, time);
                player.world.spawnParticle(EnumParticleTypes.DAMAGE_INDICATOR,
                        player.posX, player.posY + player.height / 2, player.posZ,
                        0.0, 0.0, 0.0);
            }

            // 装备耐久损失
            if (checkCooldown(nbt, K_LAST_DURABILITY, time, TICK_10S)) {
                damageDurability(player);
                nbt.setLong(K_LAST_DURABILITY, time);
            }

            // 模块降级
            if (checkCooldown(nbt, K_LAST_DEGRADE, time, TICK_10S)) {
                degradeRandomModules(core, player);
                nbt.setLong(K_LAST_DEGRADE, time);
            }
        }
    }

    /**
     * 降级随机模块（使用Capability）
     */
    public static void degradeRandomModules(ItemStack core, EntityPlayer player) {
        IMechanicalCoreData data = getCapabilityData(core);
        if (data == null) {
            return;
        }

        // 获取可降级的模块列表
        List<String> candidates = data.getInstalledUpgrades().stream()
                .filter(id -> !isGeneratorModule(id))           // 排除发电模块
                .filter(id -> !data.isPaused(id))               // 排除暂停的模块
                .filter(id -> data.getOwnedMax(id) > 0)         // 必须有拥有等级
                .filter(id -> data.getLevel(id) > 0)            // 必须有当前等级
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "没有可降级的活跃模块"), true);
            return;
        }

        // 随机选择要降级的模块
        Collections.shuffle(candidates);
        int degradeCount = Math.min(DEGRADE_MODULE_COUNT, candidates.size());

        for (int i = 0; i < degradeCount; i++) {
            String moduleId = candidates.get(i);

            // 使用Capability降级
            int originalMax = data.getOriginalMax(moduleId);
            int ownedMax = data.getOwnedMax(moduleId);

            // 降级1级
            data.degrade(moduleId, 1);

            int newOwnedMax = data.getOwnedMax(moduleId);
            String displayName = UpgradeRegistry.getDisplayName(moduleId);

            // 通知玩家
            if (newOwnedMax <= 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "⚠ 模块损坏: " + displayName +
                        TextFormatting.YELLOW + " [0/" + originalMax + "] (可修复)"), true);
            } else {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 模块降级: " + displayName +
                        " [" + newOwnedMax + "/" + originalMax + "]" +
                        TextFormatting.YELLOW + " (可修复)"), true);
            }
        }

        if (degradeCount > 0) {
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.6f, 0.8f);
        }

        // 保存数据到NBT
        saveCapabilityData(core, data);
    }

    /**
     * 自毁（使用Capability）
     */
    private static void selfDestruct(ItemStack core, EntityPlayer player) {
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_RED + "☠☠☠ 机械核心自毁序列启动 ☠☠☠"));

        // 特效
        player.world.createExplosion(player, player.posX, player.posY, player.posZ, 0.0f, false);
        for (int i = 0; i < 20; i++) {
            player.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE,
                    player.posX + (Math.random() - 0.5) * 2,
                    player.posY + Math.random() * 2,
                    player.posZ + (Math.random() - 0.5) * 2,
                    0, 0, 0);
        }

        IMechanicalCoreData data = getCapabilityData(core);
        if (data == null) {
            return;
        }

        List<String> damaged = new ArrayList<>();

        // 处理所有模块
        for (String moduleId : data.getInstalledUpgrades()) {
            if (isGeneratorModule(moduleId) || data.isPaused(moduleId)) {
                continue; // 发电模块和暂停的模块保留
            }

            int currentMax = data.getOwnedMax(moduleId);
            if (currentMax > 0) {
                // 降级到0
                data.degrade(moduleId, currentMax);
                damaged.add(UpgradeRegistry.getDisplayName(moduleId));
            }
        }

        // 给予应急能量
        try {
            IEnergyStorage es = core.getCapability(
                    net.minecraftforge.energy.CapabilityEnergy.ENERGY, null);
            if (es != null) {
                int emergency = es.getMaxEnergyStored() / 10; // 10%应急能量
                es.receiveEnergy(emergency, false);
            }
        } catch (Exception ignored) {}

        // 杀死玩家
        killPlayerSafely(player);

        // 通知
        if (!damaged.isEmpty()) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "模块严重损坏: " + String.join(", ", damaged)));
            player.sendMessage(new TextComponentString(
                    TextFormatting.YELLOW + "损坏的模块需要花费大量能量修复"));
        }

        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "发电模块和暂停模块未受影响"));

        // 保存数据到NBT
        saveCapabilityData(core, data);
    }

    // ===== 辅助方法 =====

    /**
     * 获取Capability数据
     */
    private static IMechanicalCoreData getCapabilityData(ItemStack core) {
        if (core == null || core.isEmpty()) {
            return null;
        }

        return core.getCapability(MechanicalCoreCapability.MECHANICAL_CORE_DATA, null);
    }

    /**
     * 保存Capability数据到NBT
     */
    private static void saveCapabilityData(ItemStack core, IMechanicalCoreData data) {
        if (core != null && !core.isEmpty() && data != null) {
            NBTTagCompound nbt = getOrCreateNBT(core);
            nbt.setTag("CoreData", data.serializeNBT());
        }
    }

    /**
     * 获取或创建NBT
     */
    private static NBTTagCompound getOrCreateNBT(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    /**
     * 检查冷却时间
     */
    private static boolean checkCooldown(NBTTagCompound nbt, String key, long now, long cooldown) {
        return !nbt.hasKey(key) || now - nbt.getLong(key) >= cooldown;
    }

    /**
     * 检查是否是发电模块
     */
    private static boolean isGeneratorModule(String id) {
        if (id == null) return false;
        String upper = id.toUpperCase(Locale.ROOT);
        return GENERATOR_MODULES.contains(upper) ||
               upper.contains("GENERATOR") ||
               upper.contains("CHARGER") ||
               upper.contains("SOLAR") ||
               upper.contains("KINETIC") ||
               upper.contains("VOID_ENERGY");
    }

    /**
     * 损坏装备耐久
     */
    private static void damageDurability(EntityPlayer player) {
        int totalDamage = 0;

        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (slot.getSlotType() == EntityEquipmentSlot.Type.ARMOR) {
                ItemStack armor = player.getItemStackFromSlot(slot);
                if (!armor.isEmpty() && armor.isItemStackDamageable()) {
                    armor.damageItem(10, player);
                    totalDamage += 10;
                }
            }
        }

        if (totalDamage > 0) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "⚠ 装备耐久度降低"), true);
        }
    }

    /**
     * 安全杀死玩家
     */
    private static void killPlayerSafely(EntityPlayer player) {
        if (player.capabilities.isCreativeMode) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.GRAY + "创造模式下免疫自毁伤害"));
            return;
        }

        try {
            player.getEntityData().setBoolean(K_SPECIAL_DEATH_FLAG, true);

            if (player instanceof EntityPlayerMP) {
                EntityPlayerMP mp = (EntityPlayerMP) player;
                mp.setHealth(0.0F);
                if (!mp.isDead) {
                    mp.onDeath(GEAR_STOP);
                }
                if (!mp.isDead) {
                    mp.isDead = true;
                    mp.world.setEntityState(mp, (byte) 3);
                }
            } else {
                player.setHealth(0.0F);
                if (!player.isDead) {
                    player.onDeath(GEAR_STOP);
                }
            }
        } catch (Exception e) {
            try {
                float damage = player.getMaxHealth() * 100;
                player.attackEntityFrom(DamageSource.OUT_OF_WORLD, damage);
            } catch (Exception ignored) {}
        } finally {
            player.getEntityData().removeTag(K_SPECIAL_DEATH_FLAG);
        }
    }

    /**
     * 查找装备的核心
     */
    public static ItemStack findEquippedCore(EntityPlayer player) {
        try {
            IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack stack = handler.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof ItemMechanicalCore) {
                        return stack;
                    }
                }
            }
        } catch (Exception ignored) {}

        return ItemStack.EMPTY;
    }
}
