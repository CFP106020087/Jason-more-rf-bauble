package com.moremod.event.eventHandler;

import baubles.api.BaublesApi;
import com.moremod.item.ItemSpearBauble;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "moremod")
public class EnergyRingDamageBypassHandler {

    private static final String SRP_MODID = "srparasites";
    private static final String BYPASS_DAMAGE_TYPE = "bypass_srp";

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {

        if (BYPASS_DAMAGE_TYPE.equals(event.getSource().damageType)) return;

        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        ItemStack baubleStack = ItemStack.EMPTY;
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemSpearBauble) {
                baubleStack = stack;
                break;
            }
        }
        if (baubleStack.isEmpty()) return;

        ItemSpearBauble bauble = (ItemSpearBauble) baubleStack.getItem();
        if (bauble.getEnergyStored(baubleStack) < ItemSpearBauble.COST_PER_TRIGGER) return;

        net.minecraft.entity.EntityLivingBase target = event.getEntityLiving();

        // 检查是否是末影龙 - 对末影龙不做任何处理
        if (target instanceof EntityDragon) {
            // 对末影龙保持正常伤害，不穿甲
            bauble.extractEnergy(baubleStack, ItemSpearBauble.COST_PER_TRIGGER, false);
            return;
        }


        double originalArmor = 0;
        double originalToughness = 0;

        IAttributeInstance armor = target.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        IAttributeInstance toughness = target.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);

        if (armor != null) {
            originalArmor = armor.getAttributeValue();
            armor.setBaseValue(0);
        }
        if (toughness != null) {
            originalToughness = toughness.getAttributeValue();
            toughness.setBaseValue(0);
        }

        // 移除抗性效果实现真实穿甲
        target.removePotionEffect(MobEffects.RESISTANCE);
        target.removePotionEffect(MobEffects.ABSORPTION);


        ResourceLocation regName = EntityList.getKey(target);
        if (regName != null && SRP_MODID.equals(regName.getNamespace())) {
            target.hurtResistantTime = 0;
            float bypassDamage = event.getAmount(); // 使用当前伤害
            target.attackEntityFrom(new DamageSource(BYPASS_DAMAGE_TYPE) {
                {
                    setDamageBypassesArmor();
                }
                @Override
                public boolean isUnblockable() {
                    return true;
                }
                @Override
                public net.minecraft.entity.Entity getTrueSource() {
                    return player;
                }
            }, bypassDamage);


            if (!player.world.isRemote) {
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        net.minecraft.util.text.TextFormatting.RED + "银枪对污秽之物造成双倍伤害！"));
            }
        }

        bauble.extractEnergy(baubleStack, ItemSpearBauble.COST_PER_TRIGGER, false);
    }
}