package com.moremod.core;

import com.moremod.moremod;
import ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem;
import ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.api.event.FirstAidLivingDamageEvent;
import ichttt.mods.firstaid.FirstAid;
import ichttt.mods.firstaid.common.network.MessageSyncDamageModel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 七咒虚无之眸 - First Aid 模组兼容层
 * Curse Void Gaze - First Aid Mod Compatibility Layer
 *
 * First Aid 模组有独立的死亡逻辑：当 HEAD 或 BODY 血量到 0 时触发死亡
 * 通过 CommonUtils.killPlayer() 直接操作 DataManager 设置血量为 0
 * 此类通过订阅 FirstAidLivingDamageEvent 并直接修复部位血量来防止死亡
 *
 * 虚无之眸效果：
 * - 致命伤害时消耗经验抵御死亡
 * - 每次触发消耗 3 级经验
 * - 30秒冷却时间
 * - 触发后1.5秒无敌（抗性提升V，First Aid伤害也会被完全免疫）
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class CurseFirstAidCompat {

    /**
     * 拦截 First Aid 的伤害事件
     * 关键：必须在伤害分配后、死亡判定前修复致命部位
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @Optional.Method(modid = "firstaid")
    public static void onFirstAidDamage(FirstAidLivingDamageEvent event) {
        try {
            EntityPlayer player = event.getEntityPlayer();
            if (player == null || player.world.isRemote) return;

            // 检查是否有七咒之戒和虚无之眸
            if (!CurseDeathHook.hasVoidGaze(player)) return;
            if (!CurseDeathHook.hasCursedRing(player)) return;

            // 检查是否在无敌期间（抗性提升V = 100%伤害减免）
            if (isInInvincibilityPeriod(player)) {
                // 无敌期间完全免疫 First Aid 伤害，恢复所有部位到安全血量
                for (AbstractDamageablePart part : event.getAfterDamage()) {
                    if (part.currentHealth < 1.0f) {
                        part.heal(part.getMaxHealth(), null, false);
                    }
                }
                syncDamageModel(player);
                return;
            }

            // 检查伤害后是否有致命部位（HEAD 或 BODY 会归零）
            boolean hasFatalDamage = false;
            for (AbstractDamageablePart part : event.getAfterDamage()) {
                if (part.canCauseDeath && part.currentHealth <= 0) {
                    hasFatalDamage = true;
                    break;
                }
            }

            // 如果有致命伤害，尝试用虚无之眸阻止
            if (hasFatalDamage) {
                // 尝试触发死亡保护
                boolean prevented = CurseDeathHook.tryPreventDeath(player, event.getSource());

                if (prevented) {
                    // 成功触发！恢复所有致命部位
                    for (AbstractDamageablePart part : event.getAfterDamage()) {
                        if (part.canCauseDeath && part.currentHealth <= 0) {
                            // 恢复到安全血量（与 RECOVERY_HEALTH 同步）
                            part.heal(4.0f, null, false);
                        }
                    }
                    syncDamageModel(player);
                }
                // 如果未能阻止（冷却中/经验不足），让玩家正常死亡
            }

        } catch (Throwable ignored) {
        }
    }

    /**
     * 检查玩家是否在无敌期间
     * 虚无之眸触发后会给予抗性提升V（等级4）持续1.5秒
     */
    private static boolean isInInvincibilityPeriod(EntityPlayer player) {
        return player.isPotionActive(MobEffects.RESISTANCE) &&
               player.getActivePotionEffect(MobEffects.RESISTANCE).getAmplifier() >= 4;
    }

    /**
     * 同步伤害模型到客户端
     */
    @Optional.Method(modid = "firstaid")
    private static void syncDamageModel(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            AbstractPlayerDamageModel model = (AbstractPlayerDamageModel)
                    player.getCapability(CapabilityExtendedHealthSystem.INSTANCE, null);
            if (model != null) {
                FirstAid.NETWORKING.sendTo(new MessageSyncDamageModel(model, false), (EntityPlayerMP) player);
            }
        }
    }

    /**
     * 检查 First Aid 是否已加载
     */
    public static boolean isFirstAidLoaded() {
        return Loader.isModLoaded("firstaid");
    }
}
