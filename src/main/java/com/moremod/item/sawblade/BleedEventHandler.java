package com.moremod.item.sawblade;

import com.moremod.item.ItemSawBladeSword;
import com.moremod.item.sawblade.potion.PotionBloodEuphoria;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 锯刃剑 - 核心出血系统事件处理器
 * 
 * 处理：
 * 1. 出血累积 + 爆裂
 * 2. 撕裂打击（多段伤害）
 * 3. 猎杀本能（低血量/背刺增伤）
 * 4. 鲜血欢愉（buff触发）
 * 5. 生命偷取
 * 6. 成长系统
 */
public class BleedEventHandler {
    
    // NBT键名（存在实体上）
    private static final String KEY_BLEED = "moremod_bleed_buildup";
    private static final String KEY_DECAY_TIME = "moremod_bleed_decay";
    private static final String KEY_LACERATION_TIME = "moremod_laceration_time";
    private static final String KEY_LACERATION_COUNT = "moremod_laceration_count";
    
    // ==================== 攻击事件：出血累积 + 技能触发 ====================
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public  void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase target = event.getEntityLiving();
        DamageSource source = event.getSource();
        
        if (!(source.getTrueSource() instanceof EntityPlayer)) return;
        
        EntityPlayer player = (EntityPlayer) source.getTrueSource();
        ItemStack weapon = player.getHeldItemMainhand();
        
        if (weapon.isEmpty() || !(weapon.getItem() instanceof ItemSawBladeSword)) return;
        if (!isHostile(target)) return;
        
        // 标记战斗状态
        SawBladeNBT.markAttack(weapon, player.world.getTotalWorldTime());
        
        float baseDamage = event.getAmount();
        
        // ===== 技能3：猎杀本能（低血量+背刺增伤）=====
        float hunterMult = SawBladeStats.getHunterInstinctMultiplier(weapon, target);
        float backstabMult = SawBladeStats.getBackstabMultiplier(weapon, player, target);
        
        // ===== 技能4：鲜血欢愉（buff增伤）=====
        int euphoriaStacks = PotionBloodEuphoria.getStacks(player);
        float euphoriaMult = euphoriaStacks > 0 ? (1.0f + SawBladeStats.getBloodEuphoriaPerStack(weapon) * euphoriaStacks) : 1.0f;
        
        // 综合伤害
        float totalMult = hunterMult * backstabMult * euphoriaMult;
        event.setAmount(baseDamage * totalMult);
        
        // ===== 技能1：出血累积 =====
        addBleedBuildup(target, weapon, player, baseDamage);
        
        // ===== 技能2：撕裂打击（延迟多段伤害）=====
        scheduleLaceration(target, weapon, player, baseDamage);
        
        // ===== 生命偷取（鲜血欢愉状态下）=====
        if (euphoriaStacks > 0) {
            float lifeSteal = SawBladeStats.getBloodEuphoriaLifeSteal(weapon);
            if (lifeSteal > 0) {
                float healAmount = baseDamage * totalMult * lifeSteal;
                player.heal(healAmount);
            }
        }
        
        // 统计伤害
        SawBladeNBT.addDamage(weapon, baseDamage * totalMult);
    }
    
    // ==================== 出血累积 ====================
    
    private void addBleedBuildup(EntityLivingBase target, ItemStack weapon, EntityPlayer player, float damage) {
        NBTTagCompound data = target.getEntityData();
        float current = data.getFloat(KEY_BLEED);
        
        // 基础累积
        float buildup = SawBladeStats.getBaseBleedBuildUp(weapon);
        
        // 暴击加成
        boolean isCrit = player.fallDistance > 0.0f && !player.onGround;
        if (isCrit) {
            buildup *= SawBladeStats.getCritBleedMultiplier(weapon);
        }
        
        // 连击加成
        int combo = SawBladeNBT.addCombo(weapon, player.world.getTotalWorldTime());
        if (combo >= 3) {
            buildup *= 1.2f;  // 连击3次以上+20%
        }
        
        float newBleed = Math.min(100.0f, current + buildup);
        data.setFloat(KEY_BLEED, newBleed);
        data.setLong(KEY_DECAY_TIME, player.world.getTotalWorldTime());
        
        // 触发爆裂
        if (newBleed >= 100.0f) {
            triggerBleedBurst(target, weapon, player);
        }
    }
    
    // ==================== 出血爆裂 ====================
    
    private void triggerBleedBurst(EntityLivingBase target, ItemStack weapon, EntityPlayer player) {
        // 计算伤害
        float percent = SawBladeStats.getBleedBurstDamagePercent(weapon);
        float maxHP = target.getMaxHealth();
        float damage = maxHP * percent;
        
        // 保护：不致死
        float currentHP = target.getHealth();
        if (currentHP - damage < 1.0f) {
            damage = Math.max(0, currentHP - 1.0f);
        }
        
        // 造成真实伤害
        if (damage > 0) {
            target.attackEntityFrom(DamageSource.MAGIC, damage);
        }
        
        // 重置出血
        target.getEntityData().setFloat(KEY_BLEED, 0.0f);
        
        // 统计
        SawBladeNBT.addBleedProc(weapon);
        
        // ===== 触发鲜血欢愉buff =====
        PotionBloodEuphoria.applyEffect(player, weapon);
        
        // 视觉效果
        spawnBurstEffect(target);
        
        // 音效
        target.world.playSound(null, target.posX, target.posY, target.posZ,
            SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.HOSTILE, 0.8f, 0.5f);
        
        // 反馈
        if (player.isSneaking()) {
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.RED + "出血爆裂！" + TextFormatting.GRAY + " (" + (int)damage + " 真实伤害)"
            ), true);
        }
    }
    
    // ==================== 撕裂打击（多段伤害）====================
    
    private void scheduleLaceration(EntityLivingBase target, ItemStack weapon, EntityPlayer player, float baseDamage) {
        NBTTagCompound data = target.getEntityData();
        
        // 撕裂参数
        int hits = SawBladeStats.getLacerationHits(weapon);
        int interval = SawBladeStats.getLacerationInterval(weapon);
        float damagePercent = SawBladeStats.getLacerationDamagePercent(weapon);
        
        // 记录撕裂数据
        data.setLong(KEY_LACERATION_TIME, player.world.getTotalWorldTime());
        data.setInteger(KEY_LACERATION_COUNT, hits);
        data.setFloat("moremod_laceration_damage", baseDamage * damagePercent);
        data.setInteger("moremod_laceration_interval", interval);
        data.setString("moremod_laceration_attacker", player.getUniqueID().toString());
    }
    
    // ==================== 实体Update：处理撕裂和衰减 ====================
    
    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote) return;
        
        NBTTagCompound data = entity.getEntityData();
        long currentTime = entity.world.getTotalWorldTime();
        
        // 处理撕裂打击
        if (data.hasKey(KEY_LACERATION_COUNT)) {
            processLaceration(entity, data, currentTime);
        }
        
        // 处理出血衰减
        if (data.hasKey(KEY_BLEED)) {
            processBleedDecay(entity, data, currentTime);
        }
    }
    
    private void processLaceration(EntityLivingBase entity, NBTTagCompound data, long currentTime) {
        long lastTime = data.getLong(KEY_LACERATION_TIME);
        int interval = data.getInteger("moremod_laceration_interval");
        
        if (currentTime - lastTime >= interval) {
            int count = data.getInteger(KEY_LACERATION_COUNT);
            
            if (count > 0) {
                // 造成撕裂伤害
                float damage = data.getFloat("moremod_laceration_damage");
                entity.attackEntityFrom(DamageSource.MAGIC, damage);
                
                // 小幅出血累积
                float bleed = data.getFloat(KEY_BLEED);
                data.setFloat(KEY_BLEED, Math.min(100.0f, bleed + 2.0f));
                
                // 更新计数
                data.setInteger(KEY_LACERATION_COUNT, count - 1);
                data.setLong(KEY_LACERATION_TIME, currentTime);
                
                // 粒子效果
                if (entity.world instanceof net.minecraft.world.WorldServer) {
                    ((net.minecraft.world.WorldServer)entity.world).spawnParticle(
                        net.minecraft.util.EnumParticleTypes.DAMAGE_INDICATOR,
                        entity.posX, entity.posY + entity.height * 0.5, entity.posZ,
                        3, 0.2, 0.2, 0.2, 0.0
                    );
                }
            } else {
                // 清理数据
                data.removeTag(KEY_LACERATION_COUNT);
                data.removeTag("moremod_laceration_damage");
                data.removeTag("moremod_laceration_interval");
                data.removeTag("moremod_laceration_attacker");
            }
        }
    }
    
    private void processBleedDecay(EntityLivingBase entity, NBTTagCompound data, long currentTime) {
        float bleed = data.getFloat(KEY_BLEED);
        if (bleed <= 0) {
            data.removeTag(KEY_BLEED);
            data.removeTag(KEY_DECAY_TIME);
            return;
        }
        
        long lastDecay = data.getLong(KEY_DECAY_TIME);
        
        // 每秒衰减检查
        if (currentTime - lastDecay >= 20) {
            // 获取衰减速率（需要找到对应的武器，这里使用默认值）
            float decayRate = 8.0f;  // 默认衰减速率
            
            float newBleed = Math.max(0, bleed - decayRate);
            data.setFloat(KEY_BLEED, newBleed);
            data.setLong(KEY_DECAY_TIME, currentTime);
            
            if (newBleed <= 0) {
                data.removeTag(KEY_BLEED);
                data.removeTag(KEY_DECAY_TIME);
            }
        }
    }
    
    // ==================== 击杀事件：成长系统 ====================
    
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onLivingDeath(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getTrueSource() instanceof EntityPlayer)) return;
        
        EntityPlayer player = (EntityPlayer) source.getTrueSource();
        ItemStack weapon = player.getHeldItemMainhand();
        
        if (weapon.isEmpty() || !(weapon.getItem() instanceof ItemSawBladeSword)) return;
        
        EntityLivingBase killed = event.getEntityLiving();
        
        // 判断击杀类型
        boolean isBoss = SawBladeStats.isBoss(killed);
        boolean isBleed = killed.getEntityData().getFloat(KEY_BLEED) > 50.0f;
        boolean isBackstab = SawBladeStats.isBackstab(player, killed);
        
        // 添加击杀统计
        SawBladeNBT.addKill(weapon, isBoss, isBleed, isBackstab);
        
        // 升级提示
        int oldLevel = SawBladeNBT.getLevel(weapon);
        boolean leveledUp = SawBladeNBT.addExp(weapon, 10, isBoss, isBleed, isBackstab);
        
        if (leveledUp) {
            int newLevel = SawBladeNBT.getLevel(weapon);
            player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "【锯刃剑】" +
                TextFormatting.WHITE + " 升级至 Lv." + newLevel + "！"
            ));
            
            // 升级音效
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.5f);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private boolean isHostile(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) return false;
        return entity instanceof IMob;
    }
    
    private void spawnBurstEffect(EntityLivingBase target) {
        if (!(target.world instanceof net.minecraft.world.WorldServer)) return;
        
        net.minecraft.world.WorldServer world = (net.minecraft.world.WorldServer) target.world;
        
        // 红色粒子爆发
        for (int i = 0; i < 20; i++) {
            double offsetX = (world.rand.nextDouble() - 0.5) * 0.5;
            double offsetY = world.rand.nextDouble() * target.height;
            double offsetZ = (world.rand.nextDouble() - 0.5) * 0.5;
            
            world.spawnParticle(
                net.minecraft.util.EnumParticleTypes.REDSTONE,
                target.posX + offsetX,
                target.posY + offsetY,
                target.posZ + offsetZ,
                1, 0, 0, 0, 0.0
            );
        }
        
        // 虚弱debuff
        target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 40, 0));
    }
}
