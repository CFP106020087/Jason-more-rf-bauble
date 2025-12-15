package com.moremod.sponsor.core;

import com.moremod.accessorybox.EarlyConfigLoader;
import com.moremod.moremod;
import com.moremod.sponsor.item.ZhuxianSword;
import ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem;
import ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.api.event.FirstAidLivingDamageEvent;
import ichttt.mods.firstaid.FirstAid;
import ichttt.mods.firstaid.common.network.MessageSyncDamageModel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 诛仙剑 - First Aid 模组兼容层
 * Zhuxian Sword - First Aid Mod Compatibility Layer
 *
 * First Aid 模组有独立的死亡逻辑：当 HEAD 或 BODY 血量到 0 时触发死亡
 * 诛仙剑"为生民立命"技能：血量不能低于20%，必须阻止致命部位归零
 *
 * 与香巴拉的区别：
 * - 香巴拉：消耗能量阻止死亡，没能量则死亡
 * - 诛仙剑：只要技能激活，无条件锁血20%
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class ZhuxianFirstAidCompat {

    /** 血量保护阈值：20% */
    private static final float HEALTH_THRESHOLD = 0.2f;

    /**
     * 拦截 First Aid 的伤害事件
     * 关键：必须在伤害分配后、死亡判定前修复致命部位
     *
     * 诛仙剑"为生民立命"：无条件保护血量不低于20%
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @Optional.Method(modid = "firstaid")
    public static void onFirstAidDamage(FirstAidLivingDamageEvent event) {
        try {
            // 检查诛仙剑是否启用
            if (!EarlyConfigLoader.isZhuxianSwordEnabled()) return;

            EntityPlayer player = event.getEntityPlayer();
            if (player == null || player.world.isRemote) return;

            // 检查是否激活"为生民立命"技能
            if (!ZhuxianSword.isPlayerSkillActive(player, ZhuxianSword.NBT_SKILL_LIMING)) return;

            boolean preventedFatal = false;
            float maxHealth = player.getMaxHealth();
            float minHealth = maxHealth * HEALTH_THRESHOLD;

            // 检查所有部位，确保致命部位（HEAD/BODY）不会归零
            for (AbstractDamageablePart part : event.getAfterDamage()) {
                if (part.canCauseDeath && part.currentHealth <= 0) {
                    // 强制恢复到最小血量（20%的一部分）
                    float healAmount = Math.max(1.0f, part.getMaxHealth() * HEALTH_THRESHOLD);
                    part.heal(healAmount, null, false);
                    preventedFatal = true;
                }
            }

            // 如果成功阻止了致命伤害，取消事件防止进一步处理
            if (preventedFatal) {
                event.setCanceled(true);

                // 提示玩家
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.GOLD + "为生民立命" + TextFormatting.WHITE + " 保护了致命伤害！"
                ), true);

                // 同步到客户端
                syncDamageModel(player);
            }

            // 额外检查：确保玩家总血量不低于阈值
            if (player.getHealth() < minHealth) {
                player.setHealth(minHealth);
            }

        } catch (Throwable ignored) {
            // First Aid 未加载或其他异常，静默忽略
        }
    }

    /**
     * 同步伤害模型到客户端
     */
    @Optional.Method(modid = "firstaid")
    private static void syncDamageModel(EntityPlayer player) {
        try {
            if (player instanceof EntityPlayerMP) {
                AbstractPlayerDamageModel model = (AbstractPlayerDamageModel)
                        player.getCapability(CapabilityExtendedHealthSystem.INSTANCE, null);
                if (model != null) {
                    FirstAid.NETWORKING.sendTo(new MessageSyncDamageModel(model, false), (EntityPlayerMP) player);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * 检查 First Aid 是否已加载
     */
    public static boolean isFirstAidLoaded() {
        return Loader.isModLoaded("firstaid");
    }
}
