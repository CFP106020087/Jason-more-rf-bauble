package com.moremod.system.humanity;

import com.moremod.config.BrokenGodConfig;
import com.moremod.config.HumanityConfig;
import com.moremod.moremod;
import com.moremod.util.DirectHealthHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

/**
 * 人性值效果管理器
 * Humanity Effects Manager
 *
 * 管理人性值带来的各种效果：
 * - MaxHP 修改（低人性减少最大生命值）
 * - 药水免疫（破碎之神状态）
 * - 访问控制（Synergy Station等）
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class HumanityEffectsManager {

    // ========== MaxHP 修改系统 ==========

    /** MaxHP 修改器的 UUID */
    private static final UUID HUMANITY_HP_MODIFIER_UUID = UUID.fromString("a5b6c7d8-e9f0-1234-5678-9abcdef01234");

    /** 修改器名称 */
    private static final String HUMANITY_HP_MODIFIER_NAME = "Humanity MaxHP Modifier";

    /** 破碎之神当前血量上限（不修改 maxHealth，只限制 currentHealth） */
    private static final double BROKEN_GOD_MAX_HEALTH = 10.0;

    /**
     * 更新玩家的 MaxHP 基于人性值
     * 在 PlayerTickEvent 中每秒调用
     */
    public static void updateMaxHP(EntityPlayer player) {
        if (player.world.isRemote) return;

        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) {
            // 系统未激活，移除修改器
            removeHPModifier(player);
            removeBrokenGodHPLock(player);
            return;
        }

        // 破碎之神：强制锁定最大血量为 10
        if (data.getAscensionRoute() == AscensionRoute.BROKEN_GOD) {
            removeHPModifier(player);
            applyBrokenGodHPLock(player);
            return;
        }

        // 非破碎之神：移除锁定修改器
        removeBrokenGodHPLock(player);

        float humanity = data.getHumanity();

        // 计算 HP 减少百分比
        float hpReduction = calculateHPReduction(humanity, data);

        // 应用修改器
        applyHPModifier(player, hpReduction);
    }

    /**
     * 应用破碎之神当前血量锁定
     * 不修改 maxHealth 属性，只限制 currentHealth 不超过 BROKEN_GOD_MAX_HEALTH
     * 这样可以保留其他模组对 maxHealth 的修改，同时限制实际血量
     */
    private static void applyBrokenGodHPLock(EntityPlayer player) {
        // 不再修改 maxHealth 属性，只限制当前血量

        // 获取当前血量上限（取 maxHealth 和 BROKEN_GOD_MAX_HEALTH 的较小值）
        // 防御性检查：如果 maxHealth 被其他模组设为0或负数，使用 BROKEN_GOD_MAX_HEALTH 作为下限
        double playerMaxHealth = player.getMaxHealth();
        double effectiveMaxHealth;
        if (playerMaxHealth <= 0) {
            // maxHealth 异常，使用固定值作为安全下限
            effectiveMaxHealth = BROKEN_GOD_MAX_HEALTH;
        } else {
            effectiveMaxHealth = Math.min(playerMaxHealth, BROKEN_GOD_MAX_HEALTH);
        }

        // 确保当前血量不超过有效上限
        if (player.getHealth() > effectiveMaxHealth) {
            DirectHealthHelper.setHealthDirect(player, (float) effectiveMaxHealth);
        }

        // 确保当前血量不为0或负数（防止其他模组直接设置血量为0）
        if (player.getHealth() <= 0) {
            // 检查是否在停机状态，停机状态血量应为1
            if (com.moremod.system.ascension.BrokenGodHandler.isInShutdown(player)) {
                DirectHealthHelper.setHealthDirect(player, 1.0f);
            } else {
                DirectHealthHelper.setHealthDirect(player, (float) effectiveMaxHealth);
            }
        }
    }

    /**
     * 移除破碎之神血量锁定
     * 注：当前实现不修改 maxHealth 属性，所以此方法仅用于兼容性
     */
    private static void removeBrokenGodHPLock(EntityPlayer player) {
        // 新实现不再添加属性修改器，所以无需移除
        // 保留此方法以保持 API 兼容性
    }

    /**
     * 计算生命值减少百分比
     */
    private static float calculateHPReduction(float humanity, IHumanityData data) {
        // 破碎之神：HP由破碎_心核遗物管理，此处不做修改
        if (data.getAscensionRoute() == AscensionRoute.BROKEN_GOD) {
            return 0f;
        }


        // 基于人性值的减少
        if (humanity <= 10f) {
            return 0.50f; // -50% MaxHP (临界崩解)
        } else if (humanity <= 25f) {
            return 0.30f; // -30% MaxHP (深度异化)
        } else if (humanity <= 40f) {
            return 0.15f; // -15% MaxHP (异常协议)
        }

        return 0f; // 40%+ 人性：无减少
    }

    /**
     * 应用 HP 修改器
     */
    private static void applyHPModifier(EntityPlayer player, float reduction) {
        IAttributeInstance maxHealthAttr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        // 移除旧修改器
        AttributeModifier existingMod = maxHealthAttr.getModifier(HUMANITY_HP_MODIFIER_UUID);
        if (existingMod != null) {
            maxHealthAttr.removeModifier(existingMod);
        }

        // 如果没有减少，不添加修改器
        if (reduction == 0f) return;

        // 创建新修改器
        // Operation 2 = 乘法 (最终值 * (1 + amount))
        // 减少 50% 意味着 amount = -0.5
        double amount = -reduction;

        AttributeModifier newMod = new AttributeModifier(
                HUMANITY_HP_MODIFIER_UUID,
                HUMANITY_HP_MODIFIER_NAME,
                amount,
                2 // Operation: Multiply
        );

        maxHealthAttr.applyModifier(newMod);

        // 如果当前生命值超过最大值，调整
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * 移除 HP 修改器
     */
    private static void removeHPModifier(EntityPlayer player) {
        IAttributeInstance maxHealthAttr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        AttributeModifier existingMod = maxHealthAttr.getModifier(HUMANITY_HP_MODIFIER_UUID);
        if (existingMod != null) {
            maxHealthAttr.removeModifier(existingMod);
        }
    }

    // ========== 药水免疫系统（破碎之神）==========

    /**
     * 处理药水应用事件
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPotionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        IHumanityData data = HumanityCapabilityHandler.getData(player);

        if (data == null || !data.isSystemActive()) return;

        // 破碎之神：完全免疫所有药水
        if (data.getAscensionRoute() == AscensionRoute.BROKEN_GOD) {
            event.setResult(Event.Result.DENY);
            return;
        }

        // 深度异化状态（人性 < 25%）：负面药水效果减半
        // 这在 PotionAddedEvent 中处理
    }

    /**
     * 处理药水添加事件（用于修改持续时间）
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPotionAdded(PotionEvent.PotionAddedEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        IHumanityData data = HumanityCapabilityHandler.getData(player);

        if (data == null || !data.isSystemActive()) return;

        // 深度异化效果在其他地方处理（如 HumanitySpectrumSystem.applyHumanityEffects）
        // 这里仅做事件监听，不修改药水效果
    }

    // ========== 访问控制系统 ==========

    /**
     * 检查玩家是否可以使用 Synergy Station
     */
    public static boolean canUseSynergyStation(EntityPlayer player) {
        if (!HumanityConfig.enableHumanitySystem) return true;

        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return true;

        // 破碎之神：无法使用
        if (data.getAscensionRoute() == AscensionRoute.BROKEN_GOD) {
            return false;
        }


        // 普通状态：需要 60%+ 人性值
        float humanity = data.getHumanity();
        return humanity >= HumanityConfig.synergyStationThreshold;
    }

    /**
     * 检查玩家是否可以与 NPC 交易
     */
    public static NPCInteractionLevel getNPCInteractionLevel(EntityPlayer player) {
        if (!HumanityConfig.enableHumanitySystem) return NPCInteractionLevel.NORMAL;

        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return NPCInteractionLevel.NORMAL;

        // 破碎之神：完全无法交互
        if (data.getAscensionRoute() == AscensionRoute.BROKEN_GOD) {
            return NPCInteractionLevel.INVISIBLE;
        }


        // 基于人性值
        float humanity = data.getHumanity();

        if (humanity >= 80f) {
            return NPCInteractionLevel.TRUSTED;     // 折扣
        } else if (humanity >= 60f) {
            return NPCInteractionLevel.NORMAL;      // 正常
        } else if (humanity >= 40f) {
            return NPCInteractionLevel.SUSPICIOUS;  // 加价 +50%
        } else if (humanity >= 25f) {
            return NPCInteractionLevel.HOSTILE;     // 拒绝交易
        } else {
            return NPCInteractionLevel.INVISIBLE;   // 完全无视
        }
    }

    /**
     * 获取交易价格倍率
     */
    public static float getTradePriceMultiplier(EntityPlayer player) {
        NPCInteractionLevel level = getNPCInteractionLevel(player);

        switch (level) {
            case TRUSTED:
                return 0.70f; // -30% 折扣
            case NORMAL:
                return 1.0f;
            case SUSPICIOUS:
                return 1.5f; // +50% 加价
            default:
                return 999f; // 不可交易
        }
    }

    /**
     * NPC 交互等级
     */
    public enum NPCInteractionLevel {
        TRUSTED,      // 信任（折扣）
        NORMAL,       // 正常
        SUSPICIOUS,   // 怀疑（加价）
        HOSTILE,      // 敌对（拒绝交易）
        INVISIBLE     // 无视（完全无法交互）
    }

    // ========== 升格路线检查 ==========

    /**
     * 检查是否可以升格为破碎之神
     */
    public static boolean canAscendToBrokenGod(EntityPlayer player) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null || !data.isSystemActive()) return false;

        // 已经升格
        if (data.getAscensionRoute() != AscensionRoute.NONE) return false;

        // 条件：
        // 1. 人性值 <= 阈值 (5%)
        // 2. 低人性累计时间 >= 配置值 (30分钟)
        // 3. 激活模块数 >= 配置值

        float humanity = data.getHumanity();
        if (humanity > BrokenGodConfig.ascensionHumanityThreshold) return false;

        long lowHumanitySeconds = data.getLowHumanityTicks() / 20;
        if (lowHumanitySeconds < BrokenGodConfig.requiredLowHumanitySeconds) return false;

        // 检查模块安装数量
        ItemStack core = com.moremod.item.ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (!com.moremod.item.ItemMechanicalCore.isMechanicalCore(core)) return false;

        int activeModules = com.moremod.item.ItemMechanicalCore.getTotalActiveUpgradeLevel(core);
        if (activeModules < BrokenGodConfig.requiredModuleCount) return false;

        return true;
    }

    /**
     * 计算激活模块数（用于升格检查）
     * 参考 ItemMechanicalExoskeleton.countActiveModules()
     * 统计所有模块的等级总和，而非安装种类数
     */
    public static int countActiveModulesForAscension(EntityPlayer player, ItemStack core) {
        int total = 0;

        // 基础模块
        for (com.moremod.item.ItemMechanicalCore.UpgradeType t : com.moremod.item.ItemMechanicalCore.UpgradeType.values()) {
            int lv = com.moremod.item.ItemMechanicalCore.getUpgradeLevel(core, t);
            if (lv > 0) {
                total += lv;
            }
        }

        // 扩展模块
        try {
            for (java.util.Map.Entry<String, com.moremod.item.ItemMechanicalCoreExtended.UpgradeInfo> e :
                    com.moremod.item.ItemMechanicalCoreExtended.getAllUpgrades().entrySet()) {
                com.moremod.item.ItemMechanicalCoreExtended.UpgradeInfo info = e.getValue();
                if (info == null) continue;
                if (info.category == com.moremod.item.ItemMechanicalCoreExtended.UpgradeCategory.BASIC) continue;

                int lv = com.moremod.item.ItemMechanicalCoreExtended.getUpgradeLevel(core, e.getKey());
                if (lv > 0) {
                    total += lv;
                }
            }
        } catch (Throwable ignored) {}

        return total;
    }

    /**
     * 执行升格
     */
    public static void performAscension(EntityPlayer player, AscensionRoute route) {
        IHumanityData data = HumanityCapabilityHandler.getData(player);
        if (data == null) return;

        // 目前只支持破碎之神
        if (route != AscensionRoute.BROKEN_GOD) return;

        data.setAscensionRoute(route);

        // 发送消息
        String message = "§5═══════════════════════════════\n" +
                "§5§l【破碎之神】\n" +
                "§8齿轮转动，但不知为何而转。\n" +
                "§8神在运行，但不理解目的。\n" +
                "§c你已获得药水免疫、存在干扰等能力。\n" +
                "§c但你失去了与人类世界的连接。\n" +
                "§5═══════════════════════════════";

        player.sendMessage(new net.minecraft.util.text.TextComponentString(message));

        // 粒子效果
        if (player.world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer world = (net.minecraft.world.WorldServer) player.world;
            for (int i = 0; i < 100; i++) {
                double angle = (i / 100.0) * Math.PI * 2;
                double radius = 3.0 + (i % 10) * 0.3;
                double x = player.posX + Math.cos(angle) * radius;
                double z = player.posZ + Math.sin(angle) * radius;
                double y = player.posY + (i / 100.0) * 5;

                world.spawnParticle(net.minecraft.util.EnumParticleTypes.PORTAL, x, y, z, 1, 0, 0, 0, 0.05);
            }
        }

        // 音效
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                net.minecraft.init.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * 获取玩家的激活模块数（公开方法，供外部使用）
     */
    public static int getActiveModuleCount(EntityPlayer player) {
        ItemStack core = com.moremod.item.ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (!com.moremod.item.ItemMechanicalCore.isMechanicalCore(core)) return 0;
        return countActiveModulesForAscension(player, core);
    }
}
