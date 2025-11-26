package com.moremod.eventHandler;

import com.moremod.item.ItemEnergyRing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
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

    // 创建自定义虚空伤害源
    public static class VoidGraspDamageSource extends EntityDamageSource {
        public VoidGraspDamageSource(EntityPlayer player) {
            super("void_grasp", player);
            this.setDamageBypassesArmor();
            this.setDamageIsAbsolute();
            this.setMagicDamage();
        }

        @Override
        public boolean isUnblockable() {
            return true;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getTrueSource() == null) {
            return;
        }

        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            return;
        }

        if (event.getEntityLiving() instanceof EntityPlayer) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLivingBase target = event.getEntityLiving();

        if (player.world.isRemote) {
            return;
        }

        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) {
            return;
        }

        ItemStack ringStack = ItemStack.EMPTY;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemEnergyRing) {
                ringStack = stack;
                break;
            }
        }

        if (ringStack.isEmpty()) {
            return;
        }

        ItemEnergyRing ring = (ItemEnergyRing) ringStack.getItem();
        int currentEnergy = ring.getEnergyStored(ringStack);

        NBTTagCompound playerData = player.getEntityData();
        NBTTagCompound persisted = playerData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

        int count = persisted.getInteger(TAG_ATTACK_COUNT) + 1;
        persisted.setInteger(TAG_ATTACK_COUNT, count);
        playerData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);

        player.sendStatusMessage(new TextComponentString(
                TextFormatting.YELLOW + "虛空充能: " + count + "/5"), true);

        if (count >= 5) {
            if (currentEnergy < ItemEnergyRing.COST_PER_TRIGGER) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "[虛空之握] 能量不足！"));
                return;
            }

            ring.extractEnergy(ringStack, ItemEnergyRing.COST_PER_TRIGGER, false);

            int multiplier = persisted.getInteger(TAG_NEXT_DAMAGE_MULTIPLIER);
            if (multiplier <= 0) multiplier = 1;

            float trueDamage = 250.0F * multiplier;

            // 使用自定义虚空伤害源，确保getTrueSource()返回玩家
            VoidGraspDamageSource voidDamage = new VoidGraspDamageSource(player);

            // 重置伤害免疫时间以确保伤害生效
            int originalHurtTime = target.hurtResistantTime;
            target.hurtResistantTime = 0;

            boolean damaged = target.attackEntityFrom(voidDamage, trueDamage);

            if (!damaged) {
                // 如果伤害失败，恢复免疫时间
                target.hurtResistantTime = originalHurtTime;
            }

            player.world.playSound(null, target.posX, target.posY, target.posZ,
                    net.minecraft.init.SoundEvents.ENTITY_WITHER_BREAK_BLOCK,
                    net.minecraft.util.SoundCategory.PLAYERS, 1.0F, 0.5F);

            if (target.getHealth() <= 0.0F || target.isDead) {
                persisted.removeTag(TAG_NEXT_DAMAGE_MULTIPLIER);
            } else {
                persisted.setInteger(TAG_NEXT_DAMAGE_MULTIPLIER, multiplier * 2);
            }

            persisted.setInteger(TAG_ATTACK_COUNT, 0);
            playerData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "[虛空之握] 觸發！傷害: " + trueDamage +
                            " | 剩餘能量: " + ring.getEnergyStored(ringStack)));
        }
    }
}