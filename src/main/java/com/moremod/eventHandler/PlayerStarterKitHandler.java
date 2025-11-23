package com.moremod.eventHandler;

import com.moremod.item.RegisterItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "moremod")
public class PlayerStarterKitHandler {

    @SubscribeEvent
    public static void onPlayerFirstJoin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;

        if (!player.world.isRemote) {
            NBTTagCompound playerData = player.getEntityData();

            // 使用 PERSISTED_NBT_TAG 确保数据在死亡后仍然保留
            NBTTagCompound persistedData;
            if (playerData.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
                persistedData = playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            } else {
                persistedData = new NBTTagCompound();
            }

            // 检查是否已经领取过新手礼包（版本化 key）
            final String kitKey = "moremod:received_starter_kit_v2";

            if (!persistedData.getBoolean(kitKey)) {
                // 第一次登录，给予礼包
                giveStarterKit(player);

                // 标记已领取
                persistedData.setBoolean(kitKey, true);
                persistedData.setLong("moremod:first_join_time", System.currentTimeMillis());

                // 保存数据
                playerData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persistedData);

                System.out.println("[moremod] 玩家 " + player.getName() + " 领取了新手礼包");
            } else {
                // 已经领取过，只发送欢迎消息
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "欢迎回来，" + player.getName() + "！"
                ));
            }
        }
    }

    /**
     * 给予新手礼包
     */
    private static void giveStarterKit(EntityPlayer player) {
        // 1. 创建机械核心（满能量）
        ItemStack mechanicalCore = createStarterMechanicalCore(player);

        // 2. 创建升级选择器
        ItemStack upgradeSelector = createCompleteUpgradeSelector(player);

        // 给予物品
        boolean coreGiven = player.inventory.addItemStackToInventory(mechanicalCore);
        boolean selectorGiven = player.inventory.addItemStackToInventory(upgradeSelector);

        // 如果背包满了，掉落在地上
        if (!coreGiven) {
            player.dropItem(mechanicalCore, false);
        }
        if (!selectorGiven) {
            player.dropItem(upgradeSelector, false);
        }

        // 发送欢迎消息
        sendWelcomeMessage(player);
    }

    /**
     * 创建新手机械核心（修复：先写 NBT，再充满能量，避免覆盖 Energy）
     */
    private static ItemStack createStarterMechanicalCore(EntityPlayer player) {
        ItemStack core = new ItemStack(RegisterItem.MECHANICAL_CORE);

        // —— 合并/设置 NBT（不要用 new 覆盖掉已有 Tag）——
        NBTTagCompound tag = core.hasTagCompound() ? core.getTagCompound() : new NBTTagCompound();
        tag.setBoolean("StarterCore", true);
        tag.setBoolean("Soulbound", true); // 灵魂绑定，死亡不掉落
        tag.setString("CoreTier", "STARTER");
        tag.setString("OriginalOwner", player.getName());
        tag.setString("OwnerUUID", player.getUniqueID().toString());
        core.setTagCompound(tag); // 先写入再充电（关键顺序）

        // —— 把能量充满（遵守传输上限，循环灌满；若需要也可直接写 Energy=N）——
        IEnergyStorage energy = core.getCapability(CapabilityEnergy.ENERGY, null);
        if (energy != null) {
            final int max = energy.getMaxEnergyStored();
            int guard = 0; // 防止意外死循环
            while (energy.getEnergyStored() < max && guard++ < 10000) {
                int need = max - energy.getEnergyStored();
                if (need <= 0) break;
                int inserted = energy.receiveEnergy(need, false);
                if (inserted <= 0) break;
            }

            // 若因为特殊实现 receiveEnergy 无法注满，可直接精确写入 NBT：
            // core.getTagCompound().setInteger("Energy", max);
        } else {
            // 兜底：若未提供 Capability（理论上不会发生），至少给个基础电量
            core.getTagCompound().setInteger("Energy", 100000);
        }

        return core;
    }

    /**
     * 创建包含所有升级的选择器
     */
    private static ItemStack createCompleteUpgradeSelector(EntityPlayer player) {
        ItemStack selector = new ItemStack(RegisterItem.UPGRADE_SELECTOR);

        // 升级选择器不依赖能量 Capability，这里新建 NBT 即可
        NBTTagCompound nbt = new NBTTagCompound();

        // 基本属性
        nbt.setString("Tier", "STARTER");
        nbt.setString("Type", "ONE_TIME_USE"); // 一次性使用
        nbt.setInteger("EnergySelections", 2);
        nbt.setInteger("OtherSelections", 1);
        nbt.setBoolean("Soulbound", true);
        nbt.setString("OriginalOwner", player.getName());
        nbt.setString("OwnerUUID", player.getUniqueID().toString());

        // 初始化选择状态
        nbt.setTag("CurrentSelections", new NBTTagCompound());

        // 完整升级列表
        NBTTagList availableUpgrades = new NBTTagList();
        for (String upgrade : getAllUpgradeNames()) {
            availableUpgrades.appendTag(new NBTTagString(upgrade));
        }
        nbt.setTag("AvailableUpgrades", availableUpgrades);

        selector.setTagCompound(nbt);
        selector.setStackDisplayName(TextFormatting.LIGHT_PURPLE + "✦ 新手升级选择器 [灵魂绑定] ✦");
        return selector;
    }

    /**
     * 获取所有升级名称列表
     */
    private static List<String> getAllUpgradeNames() {
        List<String> upgrades = new ArrayList<>();

        // ===== 基础类 BASIC =====
        upgrades.add("ENERGY_CAPACITY");
        upgrades.add("ENERGY_EFFICIENCY");
        upgrades.add("ARMOR_ENHANCEMENT");

        // ===== 生存类 SURVIVAL =====
        upgrades.add("YELLOW_SHIELD");
        upgrades.add("HEALTH_REGEN");
        upgrades.add("HUNGER_THIRST");
        upgrades.add("THORNS");
        upgrades.add("FIRE_EXTINGUISH");
        upgrades.add("TEMPERATURE_CONTROL");

        // ===== 辅助类 AUXILIARY ===== （不包含飞行模块）
        upgrades.add("ORE_VISION");
        upgrades.add("MOVEMENT_SPEED");
        upgrades.add("STEALTH");
        upgrades.add("EXP_AMPLIFIER");

        // ===== 战斗类 COMBAT =====
        upgrades.add("DAMAGE_BOOST");
        upgrades.add("ATTACK_SPEED");
        upgrades.add("RANGE_EXTENSION");
        upgrades.add("PURSUIT");

        // ===== 能源类 ENERGY =====
        upgrades.add("KINETIC_GENERATOR");
        upgrades.add("SOLAR_GENERATOR");
        upgrades.add("VOID_ENERGY");
        upgrades.add("COMBAT_CHARGER");

        return upgrades;
    }

    /**
     * 发送欢迎消息
     */
    private static void sendWelcomeMessage(EntityPlayer player) {
        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "═══════════════════════════════════════"));
        player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "         欢迎使用机械核心！"));
        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "═══════════════════════════════════════"));
        player.sendMessage(new TextComponentString(""));

        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "您已获得原厂初始化包："));
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "  • " + TextFormatting.AQUA + "机械核心 x1 [灵魂绑定]"));
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "  • " + TextFormatting.LIGHT_PURPLE + "升级选择器 x1 [一次性]"));

        player.sendMessage(new TextComponentString(""));
        player.sendMessage(new TextComponentString(
                TextFormatting.YELLOW + "【使用指南】"));
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "1. 装备机械核心到头部饰品栏"));
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "2. 右键升级选择器打开界面"));
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "3. 能源类选2个，其他各选1个"));
        player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "4. 选择后升级选择器将消失"));

        player.sendMessage(new TextComponentString(""));
        player.sendMessage(new TextComponentString(
                TextFormatting.RED + "注意：这是唯一的出厂设置，请妥善使用！"));

        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "═══════════════════════════════════════"));
    }
}
