package com.moremod.network;

import com.moremod.capabilities.autoattack.AutoAttackComboProvider;
import com.moremod.capabilities.autoattack.IAutoAttackCombo;
import com.moremod.compat.crafttweaker.GemAffix;
import com.moremod.compat.crafttweaker.GemNBTHelper;
import com.moremod.compat.crafttweaker.GemSocketHelper;
import com.moremod.compat.crafttweaker.IdentifiedAffix;
import com.moremod.compat.crafttweaker.InvulnerabilityHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

/**
 * 客户端 → 服务器：自动攻击
 * 
 * 服务器端处理所有攻击逻辑，包括：
 * - EntityLivingBase（生物）
 * - EntityEnderCrystal（末影水晶）
 * - 其他可攻击实体
 */
public class MessageAutoAttackTrigger implements IMessage {
    
    private boolean isAttacking;
    private int targetEntityId;
    
    public MessageAutoAttackTrigger() {
        this.targetEntityId = -1;
    }
    
    public MessageAutoAttackTrigger(boolean isAttacking) {
        this.isAttacking = isAttacking;
        this.targetEntityId = -1;
    }
    
    public MessageAutoAttackTrigger(boolean isAttacking, int targetEntityId) {
        this.isAttacking = isAttacking;
        this.targetEntityId = targetEntityId;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        this.isAttacking = buf.readBoolean();
        this.targetEntityId = buf.readInt();
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(isAttacking);
        buf.writeInt(targetEntityId);
    }
    
    public static class Handler implements IMessageHandler<MessageAutoAttackTrigger, IMessage> {

        // 动态计算攻击距离上限（基于玩家触及距离属性）
        private static final double BASE_ATTACK_TOLERANCE = 1.5; // 额外容差
        private static final double ABSOLUTE_MAX_DISTANCE = 12.0; // 绝对上限，防止作弊
        
        @Override
        public IMessage onMessage(final MessageAutoAttackTrigger message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            
            player.getServer().addScheduledTask(() -> {
                // 处理攻击
                if (message.targetEntityId != -1) {
                    Entity target = player.world.getEntityByID(message.targetEntityId);
                    
                    // 验证目标有效性
                    if (target != null && !target.isDead && target.canBeAttackedWithItem()) {
                        // 动态距离检查（基于玩家触及距离属性）
                        double distance = player.getDistance(target);
                        double maxDistance = getPlayerMaxAttackDistance(player);
                        if (distance <= maxDistance) {
                            // 检查武器
                            ItemStack weapon = player.getHeldItemMainhand();
                            if (!weapon.isEmpty() && weapon.getItem() instanceof ItemSword) {
                                if (hasAutoAttackAffix(weapon)) {
                                    // 执行攻击
                                    performAttack(player, target, weapon);
                                }
                            }
                        }
                    }
                }
                
                // 更新Capability状态
                IAutoAttackCombo cap = player.getCapability(AutoAttackComboProvider.AUTO_ATTACK_CAP, null);
                if (cap != null) {
                    cap.setAutoAttacking(message.isAttacking);
                    
                    if (!message.isAttacking) {
                        cap.setComboTime(20);
                    }
                }
            });
            
            return null;
        }
        
        /**
         * 对任意实体执行攻击
         */
        private void performAttack(EntityPlayerMP player, Entity target, ItemStack weapon) {
            // ⭐ 使用InvulnerabilityHelper清除无敌帧，允许快速连击
            if (target instanceof EntityLivingBase) {
                InvulnerabilityHelper.removeInvulnerability((EntityLivingBase) target);
            }

            // 计算基础伤害
            float damage = (float) player.getEntityAttribute(
                SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
            
            // 附魔伤害（对生物实体）
            float enchantBonus = 0;
            if (target instanceof EntityLivingBase) {
                enchantBonus = EnchantmentHelper.getModifierForCreature(
                    weapon, ((EntityLivingBase) target).getCreatureAttribute());
            }
            
            // 药水效果
            if (player.isPotionActive(MobEffects.STRENGTH)) {
                int amp = player.getActivePotionEffect(MobEffects.STRENGTH).getAmplifier();
                damage += 3.0F * (amp + 1);
            }
            if (player.isPotionActive(MobEffects.WEAKNESS)) {
                int amp = player.getActivePotionEffect(MobEffects.WEAKNESS).getAmplifier();
                damage -= 4.0F * (amp + 1);
            }
            
            // 连击加成（仅对生物）
            if (target instanceof EntityLivingBase) {
                IAutoAttackCombo cap = player.getCapability(AutoAttackComboProvider.AUTO_ATTACK_CAP, null);
                if (cap != null) {
                    damage *= cap.getComboPower();
                    
                    // 更新连击
                    int newCombo = cap.getComboCount() + 1;
                    cap.setComboCount(newCombo);
                    float newPower = 1.0f + (newCombo / 10) * 0.1f;
                    cap.setComboPower(Math.min(newPower, 20.0f));
                    cap.setLastAttackTime(player.world.getTotalWorldTime());
                }
            }
            
            // 最终伤害
            damage = Math.max(damage + enchantBonus, 1.0f);
            
            // 造成伤害
            boolean didDamage = target.attackEntityFrom(
                DamageSource.causePlayerDamage(player), damage);
            
            if (didDamage) {
                // 击退（对生物）
                if (target instanceof EntityLivingBase) {
                    int knockback = EnchantmentHelper.getKnockbackModifier(player);
                    if (knockback > 0) {
                        ((EntityLivingBase) target).knockBack(player, knockback * 0.5F,
                            player.posX - target.posX, player.posZ - target.posZ);
                    }
                    
                    // 火焰附加
                    int fireAspect = EnchantmentHelper.getFireAspectModifier(player);
                    if (fireAspect > 0) {
                        target.setFire(fireAspect * 4);
                    }
                }
                
                // 音效
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, player.getSoundCategory(),
                    1.0F, 1.0F);
                
                // 粒子
                if (player.world instanceof WorldServer) {
                    ((WorldServer) player.world).spawnParticle(
                        EnumParticleTypes.DAMAGE_INDICATOR,
                        target.posX, target.posY + target.height * 0.5, target.posZ,
                        (int)(damage * 0.5), 0.1, 0, 0.1, 0.2);
                }
                
                // 耐久消耗
                weapon.damageItem(1, player);
            }
        }
        
        /**
         * 获取玩家的最大攻击距离（基于触及距离属性）
         */
        private double getPlayerMaxAttackDistance(EntityPlayerMP player) {
            double reachDistance = 3.0; // 默认生存模式触及距离

            try {
                net.minecraft.entity.ai.attributes.IAttributeInstance reachAttr =
                    player.getEntityAttribute(net.minecraft.entity.player.EntityPlayer.REACH_DISTANCE);
                if (reachAttr != null) {
                    reachDistance = reachAttr.getAttributeValue();
                }
            } catch (Exception e) {
                // 属性不存在，使用默认值
            }

            // 加上容差，但不超过绝对上限
            return Math.min(reachDistance + BASE_ATTACK_TOLERANCE, ABSOLUTE_MAX_DISTANCE);
        }

        private boolean hasAutoAttackAffix(ItemStack weapon) {
            if (!GemSocketHelper.hasSocketedGems(weapon)) {
                return false;
            }
            
            ItemStack[] gems = GemSocketHelper.getAllSocketedGems(weapon);
            
            for (ItemStack gem : gems) {
                if (gem.isEmpty() || !GemNBTHelper.isIdentified(gem)) {
                    continue;
                }
                
                List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);
                
                for (IdentifiedAffix affix : affixes) {
                    if (affix.getAffix().getType() != GemAffix.AffixType.SPECIAL_EFFECT) {
                        continue;
                    }
                    
                    String effectType = (String) affix.getAffix().getParameter("effectType");
                    if ("auto_attack".equals(effectType)) {
                        return true;
                    }
                }
            }
            
            return false;
        }
    }
}