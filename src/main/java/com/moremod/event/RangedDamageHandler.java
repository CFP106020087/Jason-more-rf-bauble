package com.moremod.event;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.item.ItemMechanicalCoreExtended;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 远程伤害增幅处理器
 *
 * 独立事件处理器，不使用自动注册系统
 *
 * Lv1: +65% 远程伤害
 * Lv2: +80% 远程伤害
 * Lv3: +100% 远程伤害 (双倍)
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class RangedDamageHandler {

    private static final String MODULE_ID = "RANGED_DAMAGE_BOOST";

    // 每级伤害加成 (强化版: 全部 +50%)
    private static final float[] DAMAGE_MULTIPLIERS = {0f, 0.65f, 0.80f, 1.00f};

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();

        // 检查是否为远程伤害
        if (!isRangedDamage(source)) return;

        // 获取攻击者
        if (!(source.getTrueSource() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) source.getTrueSource();

        // 只在服务端处理
        if (player.world.isRemote) return;

        // 检查机械核心
        ItemStack coreStack = ItemMechanicalCore.findEquippedMechanicalCore(player);
        if (coreStack.isEmpty()) return;

        // 检查模块是否激活
        if (!ItemMechanicalCore.isUpgradeActive(coreStack, MODULE_ID)) return;

        // 获取模块等级
        int level = 0;
        try {
            level = ItemMechanicalCoreExtended.getUpgradeLevel(coreStack, MODULE_ID);
        } catch (Throwable ignored) {}

        if (level <= 0) return;

        // 计算伤害加成
        float multiplier = getMultiplier(level);
        if (multiplier <= 0) return;

        // 应用伤害加成
        float currentDamage = event.getAmount();
        float newDamage = currentDamage * (1.0f + multiplier);
        event.setAmount(newDamage);
    }

    /**
     * 判断是否为远程伤害
     */
    private static boolean isRangedDamage(DamageSource source) {
        // isProjectile() 检查箭、雪球、火焰弹等投射物
        if (source.isProjectile()) {
            return true;
        }

        // 额外检查伤害类型名称
        String type = source.getDamageType();
        if (type != null) {
            // arrow = 箭, thrown = 投掷物, trident = 三叉戟
            if (type.equals("arrow") || type.equals("thrown") || type.equals("trident")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取对应等级的伤害加成倍率
     */
    private static float getMultiplier(int level) {
        if (level < 0 || level >= DAMAGE_MULTIPLIERS.length) {
            return DAMAGE_MULTIPLIERS[DAMAGE_MULTIPLIERS.length - 1];
        }
        return DAMAGE_MULTIPLIERS[level];
    }
}
