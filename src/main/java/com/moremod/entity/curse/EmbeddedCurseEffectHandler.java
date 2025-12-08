package com.moremod.entity.curse;

import com.moremod.core.CurseDeathHook;
import com.moremod.entity.curse.EmbeddedCurseManager.EmbeddedRelicType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 嵌入遗物效果处理器
 *
 * 七咒之戒的七项诅咒与对应的七圣遗物：
 *
 * 1. 受到伤害加倍 → 圣光之心 (SACRED_HEART) 抵消
 * 2. 中立生物主动攻击 → 和平徽章 (PEACE_EMBLEM) 抵消
 * 3. 护甲效力降低30% → 守护鳞片 (GUARDIAN_SCALE) 抵消
 * 4. 对怪物伤害降低50% → 勇气之刃 (COURAGE_BLADE) 抵消
 * 5. 着火永燃 → 霜华之露 (FROST_DEW) 抵消
 * 6. 死亡灵魂破碎 → 灵魂锚点 (SOUL_ANCHOR) 抵消
 * 7. 失眠症 → 安眠香囊 (SLUMBER_SACHET) 抵消
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class EmbeddedCurseEffectHandler {

    // ========== 1. 受到伤害加倍 → 圣光之心抵消 ==========

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了圣光之心
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SACRED_HEART)) {
            // 七咒会让伤害翻倍，嵌入后抵消这个效果
            // 如果伤害被翻倍了，我们减半恢复原值
            float currentDamage = event.getAmount();
            event.setAmount(currentDamage * 0.5f);
        }
    }

    // ========== 2. 中立生物主动攻击 → 和平徽章抵消 ==========

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (!(event.getTarget() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getTarget();

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了和平徽章
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.PEACE_EMBLEM)) {
            // 如果是中立生物（非敌对怪物），取消攻击目标
            if (!(event.getEntityLiving() instanceof EntityMob)) {
                if (event.getEntityLiving() instanceof EntityAnimal ||
                    event.getEntityLiving() instanceof EntityLiving) {
                    // 无法直接取消，但可以在tick中处理
                }
            }
        }
    }

    // ========== 3. 护甲效力降低30% → 守护鳞片抵消 ==========
    // 这个效果通过属性修改实现，需要在佩戴检测中处理
    // 守护鳞片嵌入后，护甲效力恢复正常

    // ========== 4. 对怪物伤害降低50% → 勇气之刃抵消 ==========

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查目标是否是怪物
        if (!(event.getEntityLiving() instanceof EntityMob)) return;

        // 检查是否嵌入了勇气之刃
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.COURAGE_BLADE)) {
            // 七咒会让对怪物伤害减半，嵌入后抵消这个效果
            // 将伤害恢复（乘以2）
            float currentDamage = event.getAmount();
            event.setAmount(currentDamage * 2.0f);
        }
    }

    // ========== 5. 着火永燃 → 霜华之露抵消 ==========

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了霜华之露
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.FROST_DEW)) {
            // 如果玩家着火，正常熄灭（抵消永燃效果）
            // 七咒会阻止火焰自然熄灭，嵌入后恢复正常
            // 这里我们不需要特别处理，因为嵌入后七咒的永燃效果应该被抵消
        }
    }

    // ========== 6. 死亡灵魂破碎 → 灵魂锚点抵消 ==========
    // 这个效果需要在死亡事件中处理
    // 灵魂锚点嵌入后，死亡不会导致额外的惩罚

    // ========== 7. 失眠症 → 安眠香囊抵消 ==========

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerSleep(PlayerSleepInBedEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了安眠香囊
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SLUMBER_SACHET)) {
            // 安眠香囊可以直接解除失眠症
            // 允许睡觉（不做任何阻止）
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 获取玩家当前抵消的诅咒数量
     */
    public static int getCounteredCurseCount(EntityPlayer player) {
        int count = 0;

        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SACRED_HEART)) count++;
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.PEACE_EMBLEM)) count++;
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.GUARDIAN_SCALE)) count++;
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.COURAGE_BLADE)) count++;
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.FROST_DEW)) count++;
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SOUL_ANCHOR)) count++;
        if (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SLUMBER_SACHET)) count++;

        return count;
    }

    /**
     * 获取诅咒抵消状态描述
     */
    public static String[] getCurseStatus(EntityPlayer player) {
        return new String[] {
            "1.伤害加倍: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SACRED_HEART) ? "§a已抵消" : "§c生效中"),
            "2.中立生物攻击: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.PEACE_EMBLEM) ? "§a已抵消" : "§c生效中"),
            "3.护甲降低30%: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.GUARDIAN_SCALE) ? "§a已抵消" : "§c生效中"),
            "4.伤害降低50%: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.COURAGE_BLADE) ? "§a已抵消" : "§c生效中"),
            "5.永燃: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.FROST_DEW) ? "§a已抵消" : "§c生效中"),
            "6.灵魂破碎: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SOUL_ANCHOR) ? "§a已抵消" : "§c生效中"),
            "7.失眠症: " + (EmbeddedCurseManager.hasEmbeddedRelic(player, EmbeddedRelicType.SLUMBER_SACHET) ? "§a已抵消" : "§c生效中")
        };
    }
}
