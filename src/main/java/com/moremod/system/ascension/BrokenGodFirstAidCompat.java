package com.moremod.system.ascension;

import com.moremod.moremod;
import ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem;
import ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.api.event.FirstAidLivingDamageEvent;
import ichttt.mods.firstaid.FirstAid;
import ichttt.mods.firstaid.common.network.MessageSyncDamageModel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 破碎之神 - First Aid 模组兼容层
 * Broken God - First Aid Mod Compatibility Layer
 *
 * First Aid 模组有独立的死亡逻辑：当 HEAD 或 BODY 血量到 0 时触发死亡
 * 通过 CommonUtils.killPlayer() 直接操作 DataManager 设置血量为 0
 * 此类通过订阅 FirstAidLivingDamageEvent 并直接修复部位血量来防止死亡
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class BrokenGodFirstAidCompat {

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

            if (!BrokenGodHandler.isBrokenGod(player)) return;

            // 停机期间完全免疫 First Aid 伤害
            if (BrokenGodHandler.isInShutdown(player)) {
                // 恢复所有部位到安全血量
                for (AbstractDamageablePart part : event.getAfterDamage()) {
                    if (part.currentHealth < 1.0f) {
                        part.heal(part.getMaxHealth(), null, false);
                    }
                }
                return;
            }

            // 检查伤害后是否有致命部位（HEAD 或 BODY 会归零）
            boolean hasFatalDamage = false;
            for (AbstractDamageablePart part : event.getAfterDamage()) {
                if (part.canCauseDeath && part.currentHealth <= 0) {
                    hasFatalDamage = true;
                    // 立即恢复该部位到 1 点血量，阻止死亡判定
                    part.heal(1.0f, null, false);
                }
            }

            // 如果有致命伤害，进入停机模式
            if (hasFatalDamage) {
                if (!BrokenGodHandler.isInShutdown(player)) {
                    BrokenGodHandler.enterShutdown(player);
                }

                // 同步到客户端 - 从 Capability 获取 model
                if (player instanceof EntityPlayerMP) {
                    AbstractPlayerDamageModel model = (AbstractPlayerDamageModel)
                            player.getCapability(CapabilityExtendedHealthSystem.INSTANCE, null);
                    if (model != null) {
                        FirstAid.NETWORKING.sendTo(new MessageSyncDamageModel(model, false), (EntityPlayerMP) player);
                    }
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