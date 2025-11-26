// FleshRejectionEventSystem.java - 干净版本
package com.moremod.system;

import com.moremod.network.PacketHandler;
import com.moremod.network.PacketSyncRejectionData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 血肉排异事件系统
 * 处理受伤等事件对排异的影响
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class FleshRejectionEventSystem {

    public static final String REJECTION_TAG = "MoreMod_RejectionData";
    private static final String NBT_EVENT_REJECTION = "EventRejectionBonus";

    /**
     * 玩家受伤事件
     */
    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 核心检查
        if (!FleshRejectionSystem.hasMechanicalCore(player)) return;

        float damage = event.getAmount();
        float adaptation = FleshRejectionSystem.getAdaptationLevel(player);
        float rejection = FleshRejectionSystem.getRejectionLevel(player);
        boolean transcended = FleshRejectionSystem.hasTranscended(player);

        // 突破状态免疫
        if (transcended && adaptation >= com.moremod.config.FleshRejectionConfig.adaptationThreshold) {
            return;
        }

        // 计算事件造成的排异
        float eventGain = (float) Math.pow(damage, 2) * 0.07f;

        // 适应度减免
        float adaptFactor = 1.0f - (adaptation / com.moremod.config.FleshRejectionConfig.adaptationThreshold);
        adaptFactor = Math.max(0.1f, adaptFactor);
        eventGain *= adaptFactor;

        // 组合放大效果
        if (rejection > 50) {
            eventGain *= 1.0f + (rejection - 50f) / 100f;
        }

        // 应用事件排异
        addEventRejection(player, eventGain);
    }

    // ========== 工具方法 ==========

    private static void addEventRejection(EntityPlayer player, float amount) {
        NBTTagCompound data = ensureRejectionData(player);
        float current = data.getFloat(NBT_EVENT_REJECTION);
        data.setFloat(NBT_EVENT_REJECTION, Math.max(0, current + amount));
        data.setBoolean("Dirty", true);

        FleshRejectionSystem.setRejectionLevel(player,
                FleshRejectionSystem.getRejectionLevel(player) + amount);
    }

    public static NBTTagCompound ensureRejectionData(EntityPlayer player) {
        if (!player.getEntityData().hasKey(REJECTION_TAG)) {
            player.getEntityData().setTag(REJECTION_TAG, new NBTTagCompound());
        }
        return player.getEntityData().getCompoundTag(REJECTION_TAG);
    }

    public static void syncIfDirty(EntityPlayer player) {
        if (player.world.isRemote) return;

        NBTTagCompound data = ensureRejectionData(player);
        if (!data.getBoolean("Dirty")) return;

        data.setBoolean("Dirty", false);
        PacketHandler.INSTANCE.sendTo(
                new PacketSyncRejectionData(data.copy()),
                (EntityPlayerMP) player
        );
    }

    public static void clearEventRejection(EntityPlayer player) {
        NBTTagCompound data = ensureRejectionData(player);
        data.setFloat(NBT_EVENT_REJECTION, 0f);
        data.setBoolean("Dirty", true);
    }
}