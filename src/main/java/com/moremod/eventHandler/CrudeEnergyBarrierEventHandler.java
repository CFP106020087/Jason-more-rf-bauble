package com.moremod.eventHandler;

import baubles.api.BaublesApi;
import com.moremod.item.ItemCrudeEnergyBarrier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class CrudeEnergyBarrierEventHandler {

    // 冷却时间常量 (20秒 = 20000毫秒)
    private static final long COOLDOWN_TIME = 20000L;
    private static final String NBT_LAST_BLOCK_TIME = "lastBlockTime";

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        // 检查是否为玩家
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 遍历所有饰品槽位
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);

            // 检查是否为粗劣能量护盾
            if (!stack.isEmpty() && stack.getItem() instanceof ItemCrudeEnergyBarrier) {

                // 检查是否为近战伤害
                if (!isMeleeDamage(event.getSource())) continue;

                // 检查是否有足够能量
                int energy = ItemCrudeEnergyBarrier.getEnergyStored(stack);
                if (energy < ItemCrudeEnergyBarrier.COST_PER_BLOCK) {
                    // 能量不足时的提示
                    if (!player.world.isRemote) {
                        player.sendStatusMessage(
                                new TextComponentString(
                                        TextFormatting.DARK_RED + "[粗劣护盾] 能量不足！"
                                ), true);
                    }
                    continue;
                }

                // 检查冷却时间
                long currentTime = System.currentTimeMillis();
                long lastBlockTime = getLastBlockTime(stack);
                long cooldownRemaining = COOLDOWN_TIME - (currentTime - lastBlockTime);

                if (cooldownRemaining > 0) {
                    // 冷却中的提示
                    if (!player.world.isRemote) {
                        int secondsRemaining = (int) Math.ceil(cooldownRemaining / 1000.0);
                        player.sendStatusMessage(
                                new TextComponentString(
                                        TextFormatting.GRAY + "[粗劣护盾] " +
                                                TextFormatting.YELLOW + "冷却中... (" + secondsRemaining + "秒)"
                                ), true);
                    }
                    continue;
                }

                // 100% 格挡成功
                ItemCrudeEnergyBarrier.setEnergyStored(stack, energy - ItemCrudeEnergyBarrier.COST_PER_BLOCK);
                setLastBlockTime(stack, currentTime);
                event.setCanceled(true);

                if (!player.world.isRemote) {
                    // 成功格挡的消息
                    player.sendStatusMessage(
                            new TextComponentString(
                                    TextFormatting.GRAY + "[粗劣护盾] " +
                                            TextFormatting.GREEN + "格挡成功！" +
                                            TextFormatting.YELLOW + " (剩余：" + ItemCrudeEnergyBarrier.getEnergyStored(stack) + " RF)" +
                                            TextFormatting.AQUA + " [冷却：20秒]"
                            ), true);

                    // 播放格挡音效
                    player.world.playSound(null, player.posX, player.posY, player.posZ,
                            SoundEvents.ITEM_SHIELD_BLOCK,
                            player.getSoundCategory(), 0.3F, 1.4F);
                }

                // 找到第一个护盾后就退出循环
                break;
            }
        }
    }

    /**
     * 获取上次格挡时间
     */
    private static long getLastBlockTime(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return 0L;
        }
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt.getLong(NBT_LAST_BLOCK_TIME);
    }

    /**
     * 设置上次格挡时间
     */
    private static void setLastBlockTime(ItemStack stack, long time) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound nbt = stack.getTagCompound();
        nbt.setLong(NBT_LAST_BLOCK_TIME, time);
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