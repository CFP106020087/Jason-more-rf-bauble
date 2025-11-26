package com.moremod.system;

import com.moremod.client.gui.EventHUDOverlay;
import com.moremod.config.FleshRejectionConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 血肉排异HUD提示管理器
 * 统一管理所有排异相关的HUD提示信息
 */
public class FleshRejectionHUDManager {

    // 用于记录已显示过的引导信息（避免重复）
    private static final java.util.Set<String> shownGuides = new java.util.HashSet<>();
    private static final java.util.Map<EntityPlayer, Long> lastHUDTime = new java.util.WeakHashMap<>();

    // HUD 冷却时间（毫秒）
    private static final long HUD_COOLDOWN = 2000;

    /**
     * 格式化排异值显示
     * 对异常值进行处理，确保显示合理
     */
    private static String formatRejectionValue(float value) {
        // 如果值异常大，可能是计算错误
        if (value > 100) {
            // 尝试缩放
            value = value / 10f;
            if (value > 100) {
                // 还是太大，强制限制
                value = Math.min(value / 10f, 99.9f);
            }
        }

        // 根据数值大小选择格式
        if (value < 0.1f) {
            return String.format("%.2f%%", value);
        } else if (value < 1f) {
            return String.format("%.1f%%", value);
        } else if (value < 10f) {
            return String.format("%.1f%%", value);
        } else {
            return String.format("%.0f%%", value);
        }
    }

    /**
     * 检查是否可以显示HUD（避免刷屏）
     */
    private static boolean canShowHUD(EntityPlayer player) {
        Long lastTime = lastHUDTime.get(player);
        if (lastTime == null || System.currentTimeMillis() - lastTime > HUD_COOLDOWN) {
            lastHUDTime.put(player, System.currentTimeMillis());
            return true;
        }
        return false;
    }

    /**
     * 显示一次性引导提示
     */
    @SideOnly(Side.CLIENT)
    private static void showGuideOnce(String key, String message) {
        if (!shownGuides.contains(key)) {
            EventHUDOverlay.push(message);
            shownGuides.add(key);
        }
    }

    // ========== 排异值变化提示 ==========

    /**
     * 饥饿导致排异增加
     */
    @SideOnly(Side.CLIENT)
    public static void onHungerRejection(EntityPlayer player, float increase) {
        if (!player.world.isRemote) return;
        if (increase < 0.1f) return;

        showGuideOnce("hunger_first", TextFormatting.YELLOW + "💡 饥饿会加速血肉排异");

        if (canShowHUD(player)) {
            EventHUDOverlay.push(TextFormatting.RED + "🍖 饥饿 +" + formatRejectionValue(increase));
        }
    }

    /**
     * 口渴导致排异增加
     */
    @SideOnly(Side.CLIENT)
    public static void onThirstRejection(EntityPlayer player, float increase) {
        if (!player.world.isRemote) return;
        if (increase < 0.1f) return;

        showGuideOnce("thirst_first", TextFormatting.YELLOW + "💡 口渴会加速血肉排异");

        if (canShowHUD(player)) {
            EventHUDOverlay.push(TextFormatting.GOLD + "💧 口渴 +" + formatRejectionValue(increase));
        }
    }

    /**
     * 熬夜导致排异加速
     */
    @SideOnly(Side.CLIENT)
    public static void onInsomniaBoost(EntityPlayer player, int days, float boost) {
        if (!player.world.isRemote) return;
        if (boost <= 0) return;

        String message;
        TextFormatting color;

        if (days >= 3) {
            color = TextFormatting.DARK_RED;
            message = "😵 重度失眠 x" + String.format("%.1f", 1 + boost);
            showGuideOnce("insomnia_severe", TextFormatting.YELLOW + "💡 长期不睡觉会严重恶化排异");
        } else if (days >= 2) {
            color = TextFormatting.RED;
            message = "😴 中度失眠 x" + String.format("%.1f", 1 + boost);
        } else {
            color = TextFormatting.GOLD;
            message = "🌙 轻度失眠 x" + String.format("%.1f", 1 + boost);
        }

        if (canShowHUD(player)) {
            EventHUDOverlay.push(color + message);
        }
    }

    /**
     * 受伤导致排异增加
     */
    @SideOnly(Side.CLIENT)
    public static void onDamageRejection(EntityPlayer player, float damage, float increase) {
        if (!player.world.isRemote) return;
        if (increase < 0.5f) return;

        showGuideOnce("damage_first", TextFormatting.YELLOW + "💡 受伤会触发血肉排异反应");

        String severity;
        TextFormatting color;

        if (damage >= 10) {
            severity = "重创";
            color = TextFormatting.DARK_RED;
        } else if (damage >= 5) {
            severity = "受伤";
            color = TextFormatting.RED;
        } else {
            severity = "轻伤";
            color = TextFormatting.GOLD;
        }

        EventHUDOverlay.push(color + "⚠ " + severity + " +" + formatRejectionValue(increase));
    }

    /**
     * 正面药水导致排异增加
     */
    @SideOnly(Side.CLIENT)
    public static void onPotionRejection(EntityPlayer player, float increase) {
        if (!player.world.isRemote) return;
        if (increase < 0.1f) return;

        showGuideOnce("potion_first", TextFormatting.YELLOW + "💡 药水会被身体排斥");

        if (canShowHUD(player)) {
            // 药水排异值通常较大，进行特殊处理
            String displayValue = formatRejectionValue(increase);
            EventHUDOverlay.push(TextFormatting.LIGHT_PURPLE + "🧪 正面药水 +" + displayValue);
        }
    }

    // ========== 排异效果提示 ==========

    /**
     * 药水被阻挡
     */
    @SideOnly(Side.CLIENT)
    public static void onPotionBlocked(EntityPlayer player, float rejection) {
        if (!player.world.isRemote) return;

        if (rejection >= 80) {
            showGuideOnce("potion_block_severe", TextFormatting.RED + "💡 排异过高，药水完全失效");
        } else if (rejection >= 60) {
            showGuideOnce("potion_block_moderate", TextFormatting.GOLD + "💡 排异影响药水吸收");
        }
    }

    /**
     * 攻击失误
     */
    @SideOnly(Side.CLIENT)
    public static void onAttackMiss(EntityPlayer player, boolean selfDamage) {
        if (!player.world.isRemote) return;

        if (selfDamage) {
            showGuideOnce("attack_self", TextFormatting.DARK_RED + "💡 神经错乱导致自伤！");
        } else {
            showGuideOnce("attack_miss", TextFormatting.RED + "💡 排异影响肌肉控制");
        }
    }

    /**
     * 出血效果
     */
    @SideOnly(Side.CLIENT)
    public static void onBleeding(EntityPlayer player) {
        if (!player.world.isRemote) return;
        showGuideOnce("bleeding", TextFormatting.DARK_RED + "💡 血管脆弱，容易出血");
    }

    // ========== 恢复提示 ==========

    /**
     * 睡眠恢复
     */
    @SideOnly(Side.CLIENT)
    public static void onSleepRecovery(EntityPlayer player, float reduction) {
        if (!player.world.isRemote) return;

        showGuideOnce("sleep_recovery", TextFormatting.GREEN + "💡 睡眠能缓解排异反应");

        if (reduction > 0 && canShowHUD(player)) {
            EventHUDOverlay.push(TextFormatting.AQUA + "💤 休息恢复 -" + formatRejectionValue(reduction));
        }
    }

    /**
     * 适应度提升
     */
    @SideOnly(Side.CLIENT)
    public static void onAdaptationIncrease(EntityPlayer player, float adaptation) {
        if (!player.world.isRemote) return;

        // 里程碑提示
        if (adaptation >= 25 && adaptation < 26) {
            EventHUDOverlay.push(TextFormatting.GREEN + "🧬 适应度 25% - 初步适应");
            showGuideOnce("adapt_25", TextFormatting.YELLOW + "💡 身体开始适应机械核心");
        } else if (adaptation >= 50 && adaptation < 51) {
            EventHUDOverlay.push(TextFormatting.AQUA + "🧬 适应度 50% - 稳定融合");
            showGuideOnce("adapt_50", TextFormatting.YELLOW + "💡 排异反应开始减弱");
        } else if (adaptation >= 75 && adaptation < 76) {
            EventHUDOverlay.push(TextFormatting.LIGHT_PURPLE + "🧬 适应度 75% - 深度融合");
            showGuideOnce("adapt_75", TextFormatting.YELLOW + "💡 接近完美融合状态");
        } else if (adaptation >= 100) {
            EventHUDOverlay.push(TextFormatting.GOLD + "✨ 适应度 100% - 完美融合！");
            showGuideOnce("adapt_100", TextFormatting.GOLD + "💡 可以尝试突破血肉极限");
        }
    }

    /**
     * 突破成功
     */
    @SideOnly(Side.CLIENT)
    public static void onTranscendence(EntityPlayer player) {
        if (!player.world.isRemote) return;

        EventHUDOverlay.push(TextFormatting.GOLD + "🌟 血肉超越 - 获得免疫！");
        EventHUDOverlay.push(TextFormatting.YELLOW + "💫 不再受排异困扰");
    }

    // ========== 警告提示 ==========

    /**
     * 排异值警告
     */
    @SideOnly(Side.CLIENT)
    public static void showRejectionWarning(EntityPlayer player, float rejection) {
        if (!player.world.isRemote) return;

        // 适应度满了就不应该有排异警告
        float adaptation = FleshRejectionSystem.getAdaptationLevel(player);
        if (adaptation >= FleshRejectionConfig.adaptationThreshold) {
            return; // 适应满了，不显示警告
        }

        if (rejection >= 90) {
            if (canShowHUD(player)) {
                EventHUDOverlay.push(TextFormatting.DARK_RED + "⚠ 排异临界！");
                showGuideOnce("rejection_critical", TextFormatting.DARK_RED + "💡 立即休息或使用急救物品！");
            }
        } else if (rejection >= 70) {
            if (canShowHUD(player)) {
                EventHUDOverlay.push(TextFormatting.RED + "⚠ 排异严重");
                showGuideOnce("rejection_severe", TextFormatting.RED + "💡 需要尽快处理排异问题");
            }
        } else if (rejection >= 50) {
            showGuideOnce("rejection_moderate", TextFormatting.GOLD + "💡 排异开始影响身体机能");
        }
    }

    /**
     * 首次装备机械核心
     */
    @SideOnly(Side.CLIENT)
    public static void onFirstEquipCore(EntityPlayer player) {
        if (!player.world.isRemote) return;

        EventHUDOverlay.push(TextFormatting.YELLOW + "⚙ 机械核心激活");
        EventHUDOverlay.push(TextFormatting.GOLD + "💡 身体开始产生排异反应");
        EventHUDOverlay.push(TextFormatting.GREEN + "💡 保持饱食、充足睡眠有助适应");
    }

    /**
     * 清除某个玩家的HUD冷却（用于重要事件）
     */
    public static void clearCooldown(EntityPlayer player) {
        lastHUDTime.remove(player);
    }
}