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
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;

@Mod.EventBusSubscriber(modid = "moremod")
public class EventHandlerCombat {

    private static final String TAG_ATTACK_COUNT = "energyring_attack_count";
    private static final String TAG_NEXT_DAMAGE_MULTIPLIER = "energyring_next_multiplier";

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        // 調試輸出 1：事件觸發
        System.out.println("[DEBUG] LivingAttackEvent triggered");

        if (event.getSource().getTrueSource() == null) {
            System.out.println("[DEBUG] Source is null");
            return;
        }

        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            System.out.println("[DEBUG] Source is not EntityPlayer: " + event.getSource().getTrueSource().getClass());
            return;
        }

        if (event.getEntityLiving() instanceof EntityPlayer) {
            System.out.println("[DEBUG] Target is EntityPlayer, skipping");
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLivingBase target = event.getEntityLiving();

        System.out.println("[DEBUG] Player: " + player.getName() + " attacking " + target.getName());

        if (player.world.isRemote) {
            System.out.println("[DEBUG] Client side, returning");
            return;
        }

        // 調試輸出 2：檢查Baubles
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) {
            System.out.println("[DEBUG] BaublesHandler is null!");
            return;
        }

        System.out.println("[DEBUG] BaublesHandler slots: " + handler.getSlots());

        // 查找並調試所有槽位
        ItemStack ringStack = ItemStack.EMPTY;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                System.out.println("[DEBUG] Slot " + i + ": " + stack.getItem().getRegistryName());
                if (stack.getItem() instanceof ItemEnergyRing) {
                    System.out.println("[DEBUG] Found EnergyRing in slot " + i);
                    ringStack = stack;
                    break;
                }
            }
        }

        if (ringStack.isEmpty()) {
            System.out.println("[DEBUG] No EnergyRing found in any slot");
            return;
        }

        // 使用 ItemEnergyRing 的方法檢查能量
        ItemEnergyRing ring = (ItemEnergyRing) ringStack.getItem();
        int currentEnergy = ring.getEnergyStored(ringStack);
        System.out.println("[DEBUG] Ring energy: " + currentEnergy + "/" + ItemEnergyRing.MAX_ENERGY);

        // 獲取並更新攻擊計數
        NBTTagCompound playerData = player.getEntityData();
        NBTTagCompound persisted = playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

        int count = persisted.getInteger(TAG_ATTACK_COUNT) + 1;
        persisted.setInteger(TAG_ATTACK_COUNT, count);
        playerData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);

        System.out.println("[DEBUG] Attack count: " + count);

        // 顯示計數
        player.sendStatusMessage(new TextComponentString(
                TextFormatting.YELLOW + "虛空充能: " + count + "/5"), true);

        // 檢查觸發條件
        if (count >= 5) {
            System.out.println("[DEBUG] Triggering void strike!");

            // 檢查能量
            if (currentEnergy < ItemEnergyRing.COST_PER_TRIGGER) {
                System.out.println("[DEBUG] Not enough energy: " + currentEnergy + " < " + ItemEnergyRing.COST_PER_TRIGGER);
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "[虛空之握] 能量不足！"));
                return;
            }

            // 使用 ItemEnergyRing 的方法扣除能量
            ring.extractEnergy(ringStack, ItemEnergyRing.COST_PER_TRIGGER, false);
            System.out.println("[DEBUG] Energy extracted, remaining: " + ring.getEnergyStored(ringStack));

            // 獲取倍率
            int multiplier = persisted.getInteger(TAG_NEXT_DAMAGE_MULTIPLIER);
            if (multiplier <= 0) multiplier = 1;

            System.out.println("[DEBUG] Damage multiplier: " + multiplier);

            // 造成傷害
            float trueDamage = 250.0F * multiplier;
            System.out.println("[DEBUG] Applying damage: " + trueDamage);

            // 創建虛空傷害源
            DamageSource voidDamage = DamageSource.OUT_OF_WORLD.setDamageBypassesArmor().setDamageIsAbsolute();
            boolean damaged = target.attackEntityFrom(voidDamage, trueDamage);

            System.out.println("[DEBUG] Damage applied: " + damaged);

            // 音效
            player.world.playSound(null, target.posX, target.posY, target.posZ,
                    net.minecraft.init.SoundEvents.ENTITY_WITHER_BREAK_BLOCK,
                    net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 0.5F);

            // 判斷擊殺
            if (target.getHealth() <= 0.0F || target.isDead) {
                persisted.removeTag(TAG_NEXT_DAMAGE_MULTIPLIER);
                System.out.println("[DEBUG] Target killed, multiplier reset");
            } else {
                persisted.setInteger(TAG_NEXT_DAMAGE_MULTIPLIER, multiplier * 2);
                System.out.println("[DEBUG] Target survived, multiplier doubled to " + (multiplier * 2));
            }

            // 重置計數
            persisted.setInteger(TAG_ATTACK_COUNT, 0);
            playerData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);

            // 顯示信息
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "[虛空之握] 觸發！傷害: " + trueDamage +
                            " | 剩餘能量: " + ring.getEnergyStored(ringStack)));
        }
    }
}