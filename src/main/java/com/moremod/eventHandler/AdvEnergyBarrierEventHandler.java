package com.moremod.eventHandler;

import baubles.api.BaublesApi;
import com.moremod.item.ItemadvEnergyBarrier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class AdvEnergyBarrierEventHandler {

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 遍历饰品栏寻找高级能量护盾
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemadvEnergyBarrier) {

                // 检查是否为应该格挡的伤害类型
                if (!shouldBlockDamage(event.getSource())) continue;

                // 检查是否有足够能量
                int energy = ItemadvEnergyBarrier.getEnergyStored(stack);
                if (energy >= ItemadvEnergyBarrier.COST_PER_BLOCK) {
                    // 100% 格挡成功 - 消耗能量并取消伤害
                    ItemadvEnergyBarrier.setEnergyStored(stack, energy - ItemadvEnergyBarrier.COST_PER_BLOCK);
                    event.setCanceled(true);

                    if (!player.world.isRemote) {
                        // 显示格挡成功消息
                        player.sendStatusMessage(
                                new TextComponentString(
                                        TextFormatting.BLUE + "[高级护盾] " +
                                                TextFormatting.GREEN + "完全格挡 " +
                                                getDamageTypeName(event.getSource()) +
                                                TextFormatting.YELLOW + " (剩余：" + ItemadvEnergyBarrier.getEnergyStored(stack) + " RF)"
                                ), true);

                        // 播放防护音效
                        player.world.playSound(null, player.posX, player.posY, player.posZ,
                                net.minecraft.init.SoundEvents.ITEM_SHIELD_BLOCK,
                                player.getSoundCategory(), 0.5F, 1.0F);
                    }
                } else {
                    // 能量不足时的提示
                    if (!player.world.isRemote) {
                        player.sendStatusMessage(
                                new TextComponentString(
                                        TextFormatting.RED + "[高级护盾] 能量不足！无法格挡攻击！"
                                ), true);

                        // 播放能量不足音效
                        player.world.playSound(null, player.posX, player.posY, player.posZ,
                                net.minecraft.init.SoundEvents.BLOCK_NOTE_BASS,
                                player.getSoundCategory(), 0.3F, 0.5F);
                    }
                }
                break; // 只处理第一个高级护盾
            }
        }
    }

    /**
     * 判断是否应该格挡此类型的伤害
     * 高级护盾只格挡近战攻击
     */
    public static boolean shouldBlockDamage(DamageSource source) {
        // 只格挡近战攻击
        return isMeleeDamage(source);
    }

    /**
     * 判断是否为近战伤害
     */
    public static boolean isMeleeDamage(DamageSource source) {
        return source.getImmediateSource() instanceof net.minecraft.entity.Entity &&
                !source.isProjectile() &&
                !source.isMagicDamage() &&
                !source.isExplosion() &&
                !source.isFireDamage();
    }

    /**
     * 获取伤害类型的友好名称
     */
    public static String getDamageTypeName(DamageSource source) {
        if (isMeleeDamage(source)) {
            if (source.getTrueSource() instanceof EntityPlayer) return "玩家近战攻击";
            if (source.getTrueSource() instanceof net.minecraft.entity.monster.IMob) return "怪物近战攻击";
            return "近战攻击";
        }
        return source.damageType + "伤害";
    }
}