package com.moremod.event;

import com.moremod.item.ItemCurseSpread;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

@Mod.EventBusSubscriber(modid = "moremod")
public class CurseSpreadHandler {

    private static final Random RANDOM = new Random();
    private static final double DROP_CANCEL_CHANCE = 0.30; // 30%概率不掉落

    /**
     * 处理诅咒蔓延的伤害修改
     * 使用 LOW 优先级确保在其他修改之后处理
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.isCanceled() || event.getEntity() == null || event.getEntity().world == null)
            return;

        // 只在服务端处理
        if (event.getEntity().world.isRemote)
            return;

        EntityLivingBase victim = event.getEntityLiving();
        EntityLivingBase attacker = null;

        // 获取攻击者
        if (event.getSource().getTrueSource() instanceof EntityLivingBase) {
            attacker = (EntityLivingBase) event.getSource().getTrueSource();
        }

        float originalDamage = event.getAmount();
        float modifiedDamage = originalDamage;
        boolean damageModified = false;

        // Debug用 - 暫時開啟調試
        boolean debug = true; // 設置為true開啟調試信息

        // 1. 如果攻击者被诅咒蔓延影响，减少其造成的伤害
        if (attacker != null && !(attacker instanceof EntityPlayer)) {
            if (ItemCurseSpread.isCursedBySpread(attacker)) {
                int curseLevel = ItemCurseSpread.getCurseSpreadLevel(attacker);
                double multiplier = ItemCurseSpread.getDamageReductionMultiplier(curseLevel);
                modifiedDamage *= multiplier;
                damageModified = true;

                if (debug && victim instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) victim;
                    player.sendMessage(new TextComponentString(
                            String.format("§e攻击者被诅咒影响: %d级诅咒, 伤害 %.1f -> %.1f",
                                    curseLevel, originalDamage, modifiedDamage)
                    ));
                }
            }
        }

        // 2. 如果受害者被诅咒蔓延影响，增加其受到的伤害
        // ✅ 修复：直接获取詛咒等級，避免重複調用 isCursedBySpread 導致的競態條件
        if (!(victim instanceof EntityPlayer)) {
            int victimCurseLevel = ItemCurseSpread.getCurseSpreadLevel(victim);

            // 調試：輸出當前詛咒等級
            if (debug && attacker instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) attacker;
                player.sendMessage(new TextComponentString(
                        String.format("§7[Debug] 目標 %s 詛咒等級: %d", victim.getName(), victimCurseLevel)
                ));
            }

            if (victimCurseLevel >= 7) {
                double damageMultiplier = ItemCurseSpread.getDamageAmplificationMultiplier(victimCurseLevel);

                // 从原始伤害重新计算，避免双重乘算
                float victimModifiedDamage = originalDamage;

                // 如果攻击者也被诅咒了，先应用攻击者的削弱
                if (attacker != null && !(attacker instanceof EntityPlayer)) {
                    int attackerCurseLevel = ItemCurseSpread.getCurseSpreadLevel(attacker);
                    if (attackerCurseLevel >= 7) {
                        double attackerMultiplier = ItemCurseSpread.getDamageReductionMultiplier(attackerCurseLevel);
                        victimModifiedDamage *= attackerMultiplier;
                    }
                }

                // 然后应用受害者的增伤
                victimModifiedDamage *= damageMultiplier;
                modifiedDamage = victimModifiedDamage;
                damageModified = true;

                if (debug && attacker instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) attacker;
                    player.sendMessage(new TextComponentString(
                            String.format("§c[易傷] 目標被詛咒影響: %d級詛咒, 傷害 %.1f -> %.1f (×%.1f)",
                                    victimCurseLevel, originalDamage, modifiedDamage, damageMultiplier)
                    ));
                }
            }
        }

        // 应用修改后的伤害
        if (damageModified && modifiedDamage != originalDamage) {
            event.setAmount(modifiedDamage);
        }
    }

    /**
     * 处理被诅咒的实体掉落物
     * 30%概率不掉落任何物品
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.isCanceled() || event.getEntity() == null || event.getEntity().world == null)
            return;

        // 只在服务端处理
        if (event.getEntity().world.isRemote)
            return;

        EntityLivingBase entity = event.getEntityLiving();

        // 只对非玩家实体生效
        if (entity instanceof EntityPlayer)
            return;

        // 检查是否被诅咒蔓延影响
        if (!ItemCurseSpread.isCursedBySpread(entity))
            return;

        // 30%概率清空掉落物
        if (RANDOM.nextDouble() < DROP_CANCEL_CHANCE) {
            event.getDrops().clear();
            
            // Debug信息（可选）
            // 如果有玩家在附近，可以发送消息
            // EntityPlayer nearbyPlayer = entity.world.getClosestPlayerToEntity(entity, 32.0);
            // if (nearbyPlayer != null) {
            //     nearbyPlayer.sendMessage(new TextComponentString("§5诅咒吞噬了 " + entity.getName() + " 的掉落物"));
            // }
        }
    }

    /**
     * 定期清理过期的诅咒效果
     * 防止护甲削弱效果残留
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntity().world.isRemote)
            return;

        EntityLivingBase entity = event.getEntityLiving();

        // 每20 ticks检查一次
        if (entity.ticksExisted % 20 == 0) {
            // 检查诅咒是否过期
            if (!ItemCurseSpread.isCursedBySpread(entity)) {
                // 如果过期了但护甲削弱还在，清理它
                ItemCurseSpread.clearArmorReduction(entity);
            }
        }
    }
}