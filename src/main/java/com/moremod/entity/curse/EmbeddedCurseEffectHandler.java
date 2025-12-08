package com.moremod.entity.curse;

import com.moremod.core.CurseDeathHook;
import com.moremod.entity.curse.EmbeddedCurseManager.EmbeddedCurseType;
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
 * 嵌入诅咒效果处理器
 *
 * 七咒之戒的七项诅咒与对应的嵌入饰品：
 *
 * 1. 受到伤害加倍 → 虚无之眸 (VOID_GAZE) 抵消
 * 2. 中立生物主动攻击 → 饕餮指骨 (GLUTTONOUS_PHALANX) 抵消
 * 3. 护甲效力降低30% → 荆棘碎片 (THORN_SHARD) 抵消
 * 4. 对怪物伤害降低50% → 怨念结晶 (CRYSTALLIZED_RESENTMENT) 抵消
 * 5. 着火永燃 → 绞王之索 (NOOSE_OF_HANGED_KING) 抵消
 * 6. 死亡灵魂破碎 → 第五幕剧本 (SCRIPT_OF_FIFTH_ACT) 抵消
 * 7. 失眠症 → 需要嵌入任意3个饰品后解除
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class EmbeddedCurseEffectHandler {

    // ========== 1. 受到伤害加倍 → 虚无之眸抵消 ==========

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了虚无之眸
        if (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.VOID_GAZE)) {
            // 七咒会让伤害翻倍，嵌入后抵消这个效果
            // 如果伤害被翻倍了，我们减半恢复原值
            // 注意：这是在七咒效果之后执行的，所以我们需要将伤害减半
            float currentDamage = event.getAmount();
            event.setAmount(currentDamage * 0.5f);
        }
    }

    // ========== 2. 中立生物主动攻击 → 饕餮指骨抵消 ==========

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (!(event.getTarget() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getTarget();

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了饕餮指骨
        if (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.GLUTTONOUS_PHALANX)) {
            // 如果是中立生物（非敌对怪物），取消攻击目标
            if (!(event.getEntityLiving() instanceof EntityMob)) {
                if (event.getEntityLiving() instanceof EntityAnimal ||
                    event.getEntityLiving() instanceof EntityLiving) {
                    // 无法直接取消，但可以在tick中处理
                }
            }
        }
    }

    // ========== 3. 护甲效力降低30% → 荆棘碎片抵消 ==========
    // 这个效果通过属性修改实现，需要在佩戴检测中处理
    // 荆棘碎片嵌入后，护甲效力恢复正常

    // ========== 4. 对怪物伤害降低50% → 怨念结晶抵消 ==========

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查目标是否是怪物
        if (!(event.getEntityLiving() instanceof EntityMob)) return;

        // 检查是否嵌入了怨念结晶
        if (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.CRYSTALLIZED_RESENTMENT)) {
            // 七咒会让对怪物伤害减半，嵌入后抵消这个效果
            // 将伤害恢复（乘以2）
            float currentDamage = event.getAmount();
            event.setAmount(currentDamage * 2.0f);
        }
    }

    // ========== 5. 着火永燃 → 绞王之索抵消 ==========

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查是否嵌入了绞王之索
        if (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.NOOSE_OF_HANGED_KING)) {
            // 如果玩家着火，正常熄灭（抵消永燃效果）
            // 七咒会阻止火焰自然熄灭，嵌入后恢复正常
            // 这里我们不需要特别处理，因为嵌入后七咒的永燃效果应该被抵消
        }
    }

    // ========== 6. 死亡灵魂破碎 → 第五幕剧本抵消 ==========
    // 这个效果需要在死亡事件中处理
    // 第五幕剧本嵌入后，死亡不会导致额外的惩罚

    // ========== 7. 失眠症 → 嵌入3个以上饰品解除 ==========

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerSleep(PlayerSleepInBedEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        // 检查是否有七咒之戒
        if (!CurseDeathHook.hasCursedRing(player)) return;

        // 检查嵌入数量
        int embeddedCount = EmbeddedCurseManager.getEmbeddedCount(player);

        // 嵌入3个以上饰品可以解除失眠
        if (embeddedCount >= 3) {
            // 允许睡觉（不做任何阻止）
            // 七咒的失眠效果在其他地方实现，这里只是说明嵌入后可以抵消
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 获取玩家当前抵消的诅咒数量
     */
    public static int getCounteredCurseCount(EntityPlayer player) {
        int count = 0;

        if (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.VOID_GAZE)) count++;
        if (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.GLUTTONOUS_PHALANX)) count++;
        if (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.THORN_SHARD)) count++;
        if (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.CRYSTALLIZED_RESENTMENT)) count++;
        if (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.NOOSE_OF_HANGED_KING)) count++;
        if (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.SCRIPT_OF_FIFTH_ACT)) count++;

        // 失眠症需要3个饰品
        if (EmbeddedCurseManager.getEmbeddedCount(player) >= 3) count++;

        return count;
    }

    /**
     * 获取诅咒抵消状态描述
     */
    public static String[] getCurseStatus(EntityPlayer player) {
        return new String[] {
            "1.伤害加倍: " + (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.VOID_GAZE) ? "§a已抵消" : "§c生效中"),
            "2.中立生物攻击: " + (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.GLUTTONOUS_PHALANX) ? "§a已抵消" : "§c生效中"),
            "3.护甲降低30%: " + (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.THORN_SHARD) ? "§a已抵消" : "§c生效中"),
            "4.伤害降低50%: " + (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.CRYSTALLIZED_RESENTMENT) ? "§a已抵消" : "§c生效中"),
            "5.永燃: " + (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.NOOSE_OF_HANGED_KING) ? "§a已抵消" : "§c生效中"),
            "6.灵魂破碎: " + (EmbeddedCurseManager.hasEmbeddedCurse(player, EmbeddedCurseType.SCRIPT_OF_FIFTH_ACT) ? "§a已抵消" : "§c生效中"),
            "7.失眠症: " + (EmbeddedCurseManager.getEmbeddedCount(player) >= 3 ? "§a已抵消" : "§c生效中")
        };
    }
}
