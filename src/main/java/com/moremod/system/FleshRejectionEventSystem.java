package com.moremod.system;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.system.FleshRejectionSystem;
import com.moremod.system.FleshRejectionHUDManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Flesh Rejection - Event Driven Additive System
 * 添加HUD提示功能
 */
public class FleshRejectionEventSystem {

    private static final String NBT_EVENT_REJECTION = "EventRejectionBonus";

    private static float getEventRejection(EntityPlayer player) {
        return player.getEntityData()
                .getCompoundTag("MoreMod_RejectionData")
                .getFloat(NBT_EVENT_REJECTION);
    }

    private static void setEventRejection(EntityPlayer player, float value) {
        player.getEntityData()
                .getCompoundTag("MoreMod_RejectionData")
                .setFloat(NBT_EVENT_REJECTION, Math.max(0, value));
        player.getEntityData()
                .getCompoundTag("MoreMod_RejectionData")
                .setBoolean("Dirty", true);
    }

    private static void addEventRejection(EntityPlayer player, float amount) {
        float now = getEventRejection(player);
        setEventRejection(player, now + amount);

        FleshRejectionSystem.setRejectionLevel(
                player,
                FleshRejectionSystem.getRejectionLevel(player) + amount
        );
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        if (!FleshRejectionSystem.hasMechanicalCore(player)) {
            return;
        }

        float dmg = event.getAmount();

        float adaptation = FleshRejectionSystem.getAdaptationLevel(player);
        float rejection  = FleshRejectionSystem.getRejectionLevel(player);
        boolean transcended = FleshRejectionSystem.hasTranscended(player);

        // 突破状态完全免疫
        if (transcended && adaptation >= com.moremod.config.FleshRejectionConfig.adaptationThreshold) {
            return;
        }

        // 计算事件造成的排异
        float eventGain = (float) Math.pow(dmg, 2) * 0.07f;

        // 适应减影响
        float adaptFactor = 1.0f - (adaptation /
                com.moremod.config.FleshRejectionConfig.adaptationThreshold);
        adaptFactor = Math.max(0.1f, adaptFactor);
        eventGain *= adaptFactor;

        // 组织松动效果
        if (rejection > 50) {
            eventGain *= 1.0f + (rejection - 50f) / 100f;
        }

        // 应用事件排异
        addEventRejection(player, eventGain);
        
        // 发送HUD提示（客户端）
        if (player.world.isRemote && eventGain > 0.5f) {
            FleshRejectionHUDManager.onDamageRejection(player, dmg, eventGain);
        }
    }

    public static void clearEventRejection(EntityPlayer player) {
        setEventRejection(player, 0f);
    }
}