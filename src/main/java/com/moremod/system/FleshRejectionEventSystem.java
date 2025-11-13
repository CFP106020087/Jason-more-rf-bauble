package com.moremod.system;

import com.moremod.system.FleshRejectionSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * =========================================================
 *  Flesh Rejection - Event Driven Additive System
 *  (獨立事件排異系統)
 *
 *  功能：
 *   ✔ 依照傷害量平方提升排異（重傷更嚴重）
 *   ✔ 適應越滿，排異提升越弱
 *   ✔ 適應滿 + transcended → 完全禁止事件提升排異
 *   ✔ 所有事件造成的排異變化儲存在「事件排異欄位」
 *   ✔ 死亡後事件排異完全清除（你原系統 patch）
 * =========================================================
 */
public class FleshRejectionEventSystem {

    /** 事件排異欄位名稱（放在玩家 EntityData 內） */
    private static final String NBT_EVENT_REJECTION = "EventRejectionBonus";

    /** 取得事件排異（純 bonus，不含自然排異） */
    private static float getEventRejection(EntityPlayer player) {
        return player.getEntityData()
                .getCompoundTag("MoreMod_RejectionData")
                .getFloat(NBT_EVENT_REJECTION);
    }

    /** 設置事件排異 */
    private static void setEventRejection(EntityPlayer player, float value) {
        player.getEntityData()
                .getCompoundTag("MoreMod_RejectionData")
                .setFloat(NBT_EVENT_REJECTION, Math.max(0, value));
        // 標記 dirty → 交給 FleshRejectionSystem 做同步
        player.getEntityData()
                .getCompoundTag("MoreMod_RejectionData")
                .setBoolean("Dirty", true);
    }

    /** 增加事件排異（都透過這裡進入） */
    private static void addEventRejection(EntityPlayer player, float amount) {
        float now = getEventRejection(player);
        setEventRejection(player, now + amount);

        // 直接把排異也跟著提升（這是你現有的排異系統主值）
        FleshRejectionSystem.setRejectionLevel(
                player,
                FleshRejectionSystem.getRejectionLevel(player) + amount
        );
    }

    // ============================================================
    // 🔥 事件主邏輯：受傷時（適用所有傷害來源）
    // ============================================================
    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        float dmg = event.getAmount();

        // 適應度 & 排異 & 突破狀態
        float adaptation = FleshRejectionSystem.getAdaptationLevel(player);
        float rejection  = FleshRejectionSystem.getRejectionLevel(player);
        boolean transcended = FleshRejectionSystem.hasTranscended(player);

        // ============================================================
        // 1. transcend 狀態 → 完全免疫事件排異（你要求）
        // ============================================================
        if (transcended && adaptation >= com.moremod.config.FleshRejectionConfig.adaptationThreshold) {
            return; // 完全不改排異
        }

        // ============================================================
        // 2. 計算事件造成的排異（平方曲線，提高惡性循環）
        //
        //    疼痛因子 = dmg^2 * 0.07    ← 可調
        // ============================================================
        float eventGain = (float) Math.pow(dmg, 2) * 0.07f;

        // ============================================================
        // 3. 適應減影響（越接近滿，事件排異越小）
        //
        //    對應關係：
        //      適應 0% → 完全吃滿事件排異
        //      適應 50% → 只吃一半
        //      適應 100% → 只吃 10%
        // ============================================================
        float adaptFactor = 1.0f - (adaptation /
                com.moremod.config.FleshRejectionConfig.adaptationThreshold);

        adaptFactor = Math.max(0.1f, adaptFactor); // 最低保留 10% 事件排異（才有壓力來源）

        eventGain *= adaptFactor;

        // ============================================================
        // 4. 組織鬆動效果（排異越高，越容易惡化）
        //
        //    讓排異 >50 後事件排異成長加速 → 更像病變
        // ============================================================
        if (rejection > 50) {
            eventGain *= 1.0f + (rejection - 50f) / 100f; // 最多 +100%
        }

        // ============================================================
        // 5. 套用事件排異
        // ============================================================
        addEventRejection(player, eventGain);
    }

    // ============================================================
    // 🔥 提供給死亡時 reset 使用（由你原本的死亡邏輯呼叫）
    // ============================================================
    public static void clearEventRejection(EntityPlayer player) {
        setEventRejection(player, 0f);
    }
}
