package com.moremod.eventHandler;

import baubles.api.BaublesApi;
import com.moremod.item.ItemSpearBauble;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

@Mod.EventBusSubscriber
public class EnergyRingDamageBypassHandler {

    private static final String SRP_MODID = "srparasites";

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
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

        // 对任意生物强力穿甲和穿药水抗性
        net.minecraft.entity.EntityLivingBase target = event.getEntityLiving();

        // 重置伤害免疫时间，确保伤害能够生效
        target.hurtResistantTime = 0;

        // 强制移除护甲属性
        IAttributeInstance armor = target.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        IAttributeInstance toughness = target.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);
        if (armor != null) {
            armor.setBaseValue(0);
            // 移除所有护甲修饰符
            armor.removeAllModifiers();
        }
        if (toughness != null) {
            toughness.setBaseValue(0);
            toughness.removeAllModifiers();
        }

        // 移除抗性药水效果
        target.removePotionEffect(MobEffects.RESISTANCE);
        target.removePotionEffect(MobEffects.ABSORPTION);

        // 为了确保穿透生效，创建绝对伤害源
        DamageSource absoluteDamage = new DamageSource("armor_pierce_absolute") {
            {
                setDamageBypassesArmor();
                setDamageIsAbsolute();
            }
            @Override
            public boolean isUnblockable() {
                return true;
            }
            @Override
            public boolean isDamageAbsolute() {
                return true;
            }
        };

        // 取消原始伤害，用我们的绝对伤害替代
        float originalDamage = event.getAmount();
        event.setCanceled(true);

        // 造成绝对伤害，完全无视护甲
        target.attackEntityFrom(absoluteDamage, originalDamage);

        // 检查是否是寄生虫
        ResourceLocation regName = EntityList.getKey(target);
        boolean isSrpEntity = regName != null && SRP_MODID.equals(regName.getNamespace());

        if (isSrpEntity) {
            // 对寄生虫造成额外的追打伤害
            float bypassDamage = originalDamage; // 追打伤害等于原始伤害

            // 延迟一点时间再造成追打伤害，确保第一次伤害已经处理完成
            player.world.scheduleBlockUpdate(target.getPosition(),
                    player.world.getBlockState(target.getPosition()).getBlock(), 1, 0);

            // 使用延迟执行追打
            new Thread(() -> {
                try {
                    Thread.sleep(5); // 等待50毫秒
                    if (!target.isDead && target.world != null) {
                        target.hurtResistantTime = 0; // 再次重置免疫时间
                        target.attackEntityFrom(new DamageSource("bypass_srp") {
                            {
                                setDamageBypassesArmor();
                                setDamageIsAbsolute();
                            }
                            @Override
                            public boolean isUnblockable() {
                                return true;
                            }
                            @Override
                            public boolean isDamageAbsolute() {
                                return true;
                            }
                        }, bypassDamage);
                    }
                } catch (Exception e) {
                    // 忽略异常
                }
            }).start();
        }

        bauble.extractEnergy(baubleStack, ItemSpearBauble.COST_PER_TRIGGER, false);
    }
}