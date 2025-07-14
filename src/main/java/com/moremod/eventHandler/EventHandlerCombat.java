package com.moremod.eventHandler;

import com.moremod.item.ItemEnergyRing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import baubles.api.BaublesApi;

@Mod.EventBusSubscriber
public class EventHandlerCombat {

    private static final String TAG_ATTACK_COUNT = "energyring_attack_count";
    private static final String TAG_NEXT_DAMAGE_MULTIPLIER = "energyring_next_multiplier";

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        if((event.getEntityLiving() instanceof EntityPlayer )) return;
        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLivingBase target = event.getEntityLiving();

        if (player.world.isRemote) return;

        // 查找佩戴的能量戒指
        ItemStack ringStack = findEquippedEnergyRing(player);
        if (ringStack.isEmpty()) return;

        ItemEnergyRing ring = (ItemEnergyRing) ringStack.getItem();
        NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

        int count = persisted.getInteger(TAG_ATTACK_COUNT) + 1;
        persisted.setInteger(TAG_ATTACK_COUNT, count);
        player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);

        // 检查是否达到触发条件且能量足够
        if (count >= 5 && ring.getEnergyStored(ringStack) >= ItemEnergyRing.COST_PER_TRIGGER) {
            // 获取倍率（若之前未成功击杀）
            int multiplier = persisted.getInteger(TAG_NEXT_DAMAGE_MULTIPLIER);
            if (multiplier <= 0) multiplier = 1;

            // 扣除能量
            ring.extractEnergy(ringStack, ItemEnergyRing.COST_PER_TRIGGER, false);

            // 造成虚空伤害
            float trueDamage = 250 * multiplier;
            target.attackEntityFrom(DamageSource.OUT_OF_WORLD, trueDamage);

            // 判断是否击杀
            if (target.getHealth() > 0.0F) {
                persisted.setInteger(TAG_NEXT_DAMAGE_MULTIPLIER, multiplier * 2); // 下次翻倍
            } else {
                persisted.removeTag(TAG_NEXT_DAMAGE_MULTIPLIER); // 重置倍率
            }

            // 重置攻击计数
            persisted.setInteger(TAG_ATTACK_COUNT, 0);
            player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);

            // 显示信息
            int remainingEnergy = ring.getEnergyStored(ringStack);
            int triggersLeft = remainingEnergy / ItemEnergyRing.COST_PER_TRIGGER;
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "[虚空之握] 你感受到虚空的力量撕裂了对面！倍率: " + multiplier +
                            "，剩余能量：" + remainingEnergy + " RF，可触发次数：" + triggersLeft
            ));
        }
    }

    // 查找是否佩戴了 Energy Ring 饰品
    private static ItemStack findEquippedEnergyRing(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemEnergyRing) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
