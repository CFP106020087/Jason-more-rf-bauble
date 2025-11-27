package com.moremod.system.ascension;

import com.moremod.moremod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 破碎之神 - First Aid 模组兼容层
 * Broken God - First Aid Mod Compatibility Layer
 *
 * First Aid 模组有独立的死亡逻辑：当 HEAD 或 BODY 血量到 0 时触发死亡
 * 此类通过订阅 FirstAidLivingDamageEvent 来阻止破碎之神玩家被 First Aid 杀死
 */
@Mod.EventBusSubscriber(modid = moremod.MODID)
public class BrokenGodFirstAidCompat {

    private static final Logger LOGGER = LogManager.getLogger("moremod");

    /**
     * 拦截 First Aid 的伤害事件
     * 如果是破碎之神玩家并且处于停机状态，取消伤害
     * 如果伤害会导致 HEAD 或 BODY 归零，进入停机模式
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @Optional.Method(modid = "firstaid")
    public static void onFirstAidDamage(ichttt.mods.firstaid.api.event.FirstAidLivingDamageEvent event) {
        try {
            if (!(event.getEntityPlayer() instanceof EntityPlayer)) return;
            EntityPlayer player = event.getEntityPlayer();

            if (!BrokenGodHandler.isBrokenGod(player)) return;

            // 停机期间完全免疫 First Aid 伤害
            if (BrokenGodHandler.isInShutdown(player)) {
                event.setCanceled(true);
                LOGGER.debug("[BrokenGod-FA] Cancelled First Aid damage during shutdown for {}", player.getName());
                return;
            }

            // 检查伤害后是否会导致 HEAD 或 BODY 归零
            ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel model = event.getAfterDamageModel();
            if (model != null) {
                float headHealth = model.HEAD.currentHealth;
                float bodyHealth = model.BODY.currentHealth;

                // 如果 HEAD 或 BODY 会降到 0，拦截伤害并进入停机
                if (headHealth <= 0 || bodyHealth <= 0) {
                    event.setCanceled(true);

                    // 恢复到安全血量
                    model.HEAD.currentHealth = Math.max(1.0f, model.HEAD.currentHealth);
                    model.BODY.currentHealth = Math.max(1.0f, model.BODY.currentHealth);

                    // 进入停机模式
                    if (!BrokenGodHandler.isInShutdown(player)) {
                        BrokenGodHandler.enterShutdown(player);
                        LOGGER.info("[BrokenGod-FA] Intercepted fatal First Aid damage, entering shutdown for {}", player.getName());
                    }
                    return;
                }
            }

        } catch (Throwable e) {
            LOGGER.debug("[BrokenGod-FA] Error handling First Aid event: {}", e.getMessage());
        }
    }

    /**
     * 检查 First Aid 是否已加载
     */
    public static boolean isFirstAidLoaded() {
        return Loader.isModLoaded("firstaid");
    }
}
