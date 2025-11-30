package com.moremod.system.ascension;

import com.moremod.config.ShambhalaConfig;
import com.moremod.moremod;
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
 * 香巴拉 - First Aid 模组兼容层
 * Shambhala - First Aid Mod Compatibility Layer
 *
 * First Aid 模组有独立的死亡逻辑：当 HEAD 或 BODY 血量到 0 时触发死亡
 * 香巴拉的核心机制：只要有能量，就能阻止致命部位归零
 *
 * 与破碎之神不同：
 * - 破碎之神：无条件阻止死亡（停机模式）
 * - 香巴拉：消耗能量阻止死亡，没能量则死亡
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class ShambhalaFirstAidCompat {

    /**
     * 拦截 First Aid 的伤害事件（第二道防线）
     * 关键：必须在伤害分配后、死亡判定前修复致命部位
     *
     * 香巴拉与破碎之神的区别：
     * - 破碎之神：停机期间无条件免疫
     * - 香巴拉：需要消耗能量，能量耗尽则死亡
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @Optional.Method(modid = "firstaid")
    public static void onFirstAidDamage(FirstAidLivingDamageEvent event) {
        try {
            EntityPlayer player = event.getEntityPlayer();
            if (player == null || player.world.isRemote) return;

            if (!ShambhalaHandler.isShambhala(player)) return;

            boolean preventedFatal = false;

            // 检查伤害后是否有致命部位（HEAD 或 BODY 会归零）
            for (AbstractDamageablePart part : event.getAfterDamage()) {
                if (part.canCauseDeath && part.currentHealth <= 0) {
                    // 计算恢复这个部位需要的能量
                    float healAmount = Math.max(1.0f, part.getMaxHealth() * 0.5f); // 恢复到50%或至少1点
                    int energyCost = (int) (healAmount * ShambhalaConfig.energyPerDamage);

                    // 尝试消耗能量
                    if (ShambhalaHandler.consumeEnergy(player, energyCost)) {
                        // 成功消耗能量，恢复部位血量
                        part.heal(healAmount, null, false);
                        preventedFatal = true;

                        // 提示玩家
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.AQUA + "☀ 能量护盾保护了致命伤害 " +
                                TextFormatting.GRAY + "(-" + energyCost + " RF)"
                        ), true);
                    } else {
                        // 能量不足，尝试用剩余能量部分恢复
                        int remaining = ShambhalaHandler.getCurrentEnergy(player);
                        if (remaining > 0) {
                            float partialHeal = (float) remaining / ShambhalaConfig.energyPerDamage;
                            if (partialHeal >= 0.5f) { // 至少能恢复0.5点血
                                ShambhalaHandler.consumeEnergy(player, remaining);
                                part.heal(partialHeal, null, false);

                                // 检查是否成功阻止死亡
                                if (part.currentHealth > 0) {
                                    preventedFatal = true;
                                }

                                player.sendStatusMessage(new TextComponentString(
                                        TextFormatting.YELLOW + "⚠ 能量耗尽！护盾破碎！"
                                ), true);
                            }
                            // 如果恢复后仍然<=0，玩家将死亡
                        }
                        // 没有能量 = 死亡（这是香巴拉的核心代价）
                    }
                }
            }

            // 第二道防线：如果成功阻止了致命伤害，取消事件防止进一步处理
            if (preventedFatal) {
                event.setCanceled(true);
            }

            // 同步到客户端
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
