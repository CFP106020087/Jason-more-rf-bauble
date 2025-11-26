package com.moremod.item.sawblade.potion;

import com.moremod.item.sawblade.SawBladeStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

/**
 * 鲜血欢愉 - 自定义药水效果（完整版）
 *
 * 效果：
 * - 增伤（每层15%-25%）- 在BleedEventHandler中计算
 * - 攻速提升（20%-40%）- 通过AttributeModifier实现
 * - 生命偷取（10%-20%，Lv30+）- 在BleedEventHandler中计算
 * - 红色粒子效果
 *
 * 可叠加1-3层
 */
public class PotionBloodEuphoria extends Potion {

    private static final ResourceLocation ICON = new ResourceLocation("moremod", "textures/potions/blood_euphoria.png");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("91AEAA56-376B-4498-935B-2F7F68070635");

    public PotionBloodEuphoria() {
        super(false, 0xFF0000);
        this.setPotionName("effect.moremod.blood_euphoria");
        this.setIconIndex(0, 0);
        this.setRegistryName(new ResourceLocation("moremod", "blood_euphoria"));
    }

    @Override
    public boolean isBeneficial() {
        return true;
    }

    @Override
    public int getLiquidColor() {
        return 0xCC0000;
    }

    /**
     * 每tick执行 - 粒子效果
     */
    @Override
    public void performEffect(EntityLivingBase entity, int amplifier) {
        if (entity.world.isRemote && entity.world.rand.nextFloat() < 0.3f) {
            spawnBloodParticles(entity, amplifier + 1);
        }
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return true;
    }

    /**
     * 应用属性修改器 - 攻速加成
     */
    @Override
    public void applyAttributesModifiersToEntity(EntityLivingBase entity, 
                                                 AbstractAttributeMap attributes, 
                                                 int amplifier) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            ItemStack weapon = player.getHeldItemMainhand();
            
            if (!weapon.isEmpty()) {
                float attackSpeedBonus = SawBladeStats.getBloodEuphoriaAttackSpeed(weapon);
                
                IAttributeInstance attackSpeed = 
                    attributes.getAttributeInstance(net.minecraft.entity.SharedMonsterAttributes.ATTACK_SPEED);
                
                if (attackSpeed != null) {
                    attackSpeed.removeModifier(ATTACK_SPEED_UUID);
                    
                    // 每层叠加攻速
                    float totalBonus = attackSpeedBonus * (amplifier + 1);
                    attackSpeed.applyModifier(new AttributeModifier(
                        ATTACK_SPEED_UUID,
                        "Blood Euphoria Attack Speed",
                        totalBonus,
                        1  // 乘法
                    ));
                }
            }
        }
        
        super.applyAttributesModifiersToEntity(entity, attributes, amplifier);
    }

    /**
     * 移除属性修改器
     */
    @Override
    public void removeAttributesModifiersFromEntity(EntityLivingBase entity, 
                                                    AbstractAttributeMap attributes, 
                                                    int amplifier) {
        IAttributeInstance attackSpeed = 
            attributes.getAttributeInstance(net.minecraft.entity.SharedMonsterAttributes.ATTACK_SPEED);
        
        if (attackSpeed != null) {
            attackSpeed.removeModifier(ATTACK_SPEED_UUID);
        }
        
        super.removeAttributesModifiersFromEntity(entity, attributes, amplifier);
    }

    /**
     * 渲染状态栏图标
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        mc.getTextureManager().bindTexture(ICON);
        Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 7, 0, 0, 18, 18, 18, 18);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        mc.getTextureManager().bindTexture(ICON);
        Gui.drawModalRectWithCustomSizedTexture(x + 3, y + 3, 0, 0, 18, 18, 18, 18);
    }

    /**
     * 生成血液粒子效果
     */
    @SideOnly(Side.CLIENT)
    private void spawnBloodParticles(EntityLivingBase entity, int stacks) {
        for (int i = 0; i < stacks; i++) {
            double offsetX = (entity.world.rand.nextDouble() - 0.5) * 0.5;
            double offsetY = entity.world.rand.nextDouble() * 0.5;
            double offsetZ = (entity.world.rand.nextDouble() - 0.5) * 0.5;
            
            entity.world.spawnParticle(
                net.minecraft.util.EnumParticleTypes.REDSTONE,
                entity.posX + offsetX,
                entity.posY + 1.0 + offsetY,
                entity.posZ + offsetZ,
                0, 0, 0
            );
        }
    }

    // ==================== 静态辅助方法 ====================

    public static void applyEffect(EntityPlayer player, ItemStack stack) {
        Potion potion = getPotion();
        if (potion == null) return;

        int maxStacks = SawBladeStats.getBloodEuphoriaMaxStacks(stack);
        int duration = SawBladeStats.getBloodEuphoriaDuration(stack) * 20;

        PotionEffect current = player.getActivePotionEffect(potion);
        int currentStacks = (current != null) ? current.getAmplifier() + 1 : 0;
        int newStacks = Math.min(maxStacks, currentStacks + 1);

        player.addPotionEffect(new PotionEffect(
                potion,
                duration,
                newStacks - 1,
                false,
                true
        ));
    }

    private static Potion getPotion() {
        try {
            Class<?> clazz = Class.forName("com.moremod.potion.ModPotions");
            java.lang.reflect.Field field = clazz.getDeclaredField("BLOOD_EUPHORIA");
            return (Potion) field.get(null);
        } catch (Exception e) {
            System.err.println("[BloodEuphoria] Failed to get potion from ModPotions");
            return null;
        }
    }

    public static int getStacks(EntityPlayer player) {
        Potion potion = getPotion();
        if (potion == null) return 0;

        PotionEffect effect = player.getActivePotionEffect(potion);
        if (effect == null) return 0;

        return effect.getAmplifier() + 1;
    }

    public static void removeEffect(EntityPlayer player) {
        Potion potion = getPotion();
        if (potion == null) return;
        player.removePotionEffect(potion);
    }

    public static boolean hasEffect(EntityPlayer player) {
        Potion potion = getPotion();
        if (potion == null) return false;
        return player.isPotionActive(potion);
    }
}