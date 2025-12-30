package com.moremod.event;

import com.moremod.compat.crafttweaker.*;
import com.moremod.entity.EntitySwordBeam;
import com.moremod.item.chengyue.ChengYueSweep;
import com.moremod.util.DamageSourceSwordBeam;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/**
 * Ultimate事件监听器 - 增强版 ⭐
 * 支持所有16种Trigger类型 + 无敌帧系统
 *
 * ✅ 核心功能：
 * - 从镶嵌的宝石中读取效果
 * - 通过GemNBTHelper和AffixPoolRegistry获取effectType
 * - ⭐ 新增：无敌帧穿透处理
 * - ⭐ 新增：防止剑气无限循环
 * - 详细的调试输出
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class UltimateEventHandler {

    private static final Random RANDOM = new Random();
    private static final boolean DEBUG = false;  // 调试开关

    // 连击追踪
    private static final Map<UUID, ComboTracker> COMBO_TRACKERS = new HashMap<>();

    // ==========================================
    // ⭐ 新增：检查是否是剑气伤害
    // ==========================================
    private static boolean isSwordBeamDamage(DamageSource source) {
        // 检查自定义伤害源
        if (source instanceof DamageSourceSwordBeam) {
            return true;
        }

        // 检查直接实体
        if (source.getImmediateSource() instanceof EntitySwordBeam) {
            return true;
        }

        // 检查伤害类型名称
        if ("swordbeam".equals(source.getDamageType())) {
            return true;
        }

        return false;
    }

    // ==========================================
    // ⭐ 新增：检查是否是AOE/横扫伤害（防止递归卡顿）
    // ==========================================
    private static boolean isAOEOrSweepDamage(DamageSource source) {
        // 检查澄月横扫伤害
        if (ChengYueSweep.ChengYueSweepDamage.isSweepDamage(source)) {
            return true;
        }

        // 检查AOE伤害类型
        String damageType = source.getDamageType();
        if ("aoe".equals(damageType) ||
            "sweep".equals(damageType) ||
            "chengyue_sweep".equals(damageType)) {
            return true;
        }

        return false;
    }

    // ==========================================
    // 战斗事件 - ON_DODGE, ON_BLOCK
    // ==========================================

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        Entity sourceEntity = source.getTrueSource();

        if (!(sourceEntity instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        // ⭐ 跳过AOE/横扫伤害的闪避/格挡检查
        if (isAOEOrSweepDamage(source) || isSwordBeamDamage(source)) {
            return;
        }

        EntityPlayer attacker = (EntityPlayer) sourceEntity;
        EntityLivingBase victim = event.getEntityLiving();
        ItemStack weapon = attacker.getHeldItemMainhand();

        if (weapon.isEmpty()) return;

        List<UltimateEffectHandler.EffectConfig> effects = parseEffects(weapon);
        if (effects.isEmpty()) return;

        // 检查闪避
        for (UltimateEffectHandler.EffectConfig effect : effects) {
            if ("dodge_chance".equals(effect.effectType)) {
                if (UltimateEffectHandler.triggerEffect(
                        effect, SpecialEffectTrigger.ON_DODGE,
                        attacker, victim, event.getAmount()  // ⭐ 传递source
                )) {
                    event.setCanceled(true);
                    playDodgeEffect(victim);
                    return;
                }
            }
        }

        // 检查格挡
        for (UltimateEffectHandler.EffectConfig effect : effects) {
            if ("block_chance".equals(effect.effectType)) {
                if (UltimateEffectHandler.triggerEffect(
                        effect, SpecialEffectTrigger.ON_BLOCK,
                        victim, attacker, event.getAmount()  // ⭐ 传递source
                )) {
                    victim.getEntityData().setBoolean("moremod$blocked", true);
                    playBlockEffect(victim);
                    return;
                }
            }
        }
    }

    // ==========================================
    // 伤害事件 - ON_HIT, ON_CRIT, ON_HIT_TAKEN, ON_LETHAL_DAMAGE, ON_COMBO
    // ⭐ 增强：无敌帧处理 + 防剑气循环
    // ==========================================

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity sourceEntity = source.getTrueSource();

        if (!(sourceEntity instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        // ⭐ 核心防循环：剑气伤害不触发新的剑气效果
        if (isSwordBeamDamage(source)) {
            if (DEBUG) {
                System.out.println("[UltimateEvent] ⭐ 检测到剑气伤害，跳过效果触发避免循环");
            }
            return;
        }

        // ⭐ 核心防循环：AOE/横扫伤害不触发新的AOE效果（防止严重卡顿）
        if (isAOEOrSweepDamage(source)) {
            if (DEBUG) {
                System.out.println("[UltimateEvent] ⭐ 检测到AOE/横扫伤害，跳过效果触发避免递归卡顿");
            }
            return;
        }

        EntityPlayer attacker = (EntityPlayer) sourceEntity;
        EntityLivingBase victim = event.getEntityLiving();
        ItemStack weapon = attacker.getHeldItemMainhand();

        if (weapon.isEmpty()) return;

        float damage = event.getAmount();

        // ⭐ 如果伤害为0或负数，不触发效果
        if (damage <= 0) {
            if (DEBUG) {
                System.out.println("[UltimateEvent] 伤害<=0，跳过效果触发");
            }
            return;
        }

        if (DEBUG) {
            System.out.println("╔════════════════════════════════════════════════╗");
            System.out.println("║ [UltimateEvent] 检测到攻击事件");
            System.out.println("║ 攻击者: " + attacker.getName());
            System.out.println("║ 受害者: " + victim.getName());
            System.out.println("║ 武器: " + weapon.getDisplayName());
            System.out.println("║ 伤害: " + damage);
            System.out.println("║ 伤害源: " + source.getDamageType());  // ⭐ 新增
            System.out.println("║ ⭐ 目标无敌帧: " + victim.hurtResistantTime + " ticks");
        }

        // ==========================================
        // ⭐ 处理无敌帧穿透
        // ==========================================
        if (victim.hurtResistantTime > 0) {
            // 检查是否有穿透无敌帧标记
            if (victim.getEntityData().hasKey("moremod$iframe_penetration")) {
                long expireTime = victim.getEntityData().getLong("moremod$iframe_penetration_expire");

                // 检查标记是否过期
                if (victim.world.getTotalWorldTime() < expireTime) {
                    float penetration = victim.getEntityData().getFloat("moremod$iframe_penetration");

                    // 允许穿透百分比伤害
                    float penetratedDamage = damage * penetration;
                    event.setAmount(penetratedDamage);
                    damage = penetratedDamage;

                    // 播放穿透效果
                    if (victim.world instanceof net.minecraft.world.WorldServer) {
                        ((net.minecraft.world.WorldServer) victim.world).spawnParticle(
                                net.minecraft.util.EnumParticleTypes.CRIT_MAGIC,
                                victim.posX, victim.posY + victim.height / 2, victim.posZ,
                                5, 0.3, 0.3, 0.3, 0.1
                        );
                    }

                    if (DEBUG) {
                        System.out.println("║ ⭐ [无敌帧穿透] 穿透: " + (penetration * 100) + "%");
                        System.out.println("║ ⭐ [无敌帧穿透] 原伤害: " + (damage / penetration) + " → 穿透伤害: " + damage);
                    }
                } else {
                    // 标记过期，清除
                    victim.getEntityData().removeTag("moremod$iframe_penetration");
                    victim.getEntityData().removeTag("moremod$iframe_penetration_expire");

                    if (DEBUG) {
                        System.out.println("║ ⭐ [无敌帧穿透] 标记已过期，已清除");
                    }
                }
            }
        }

        // 格挡减伤
        if (victim.getEntityData().getBoolean("moremod$blocked")) {
            event.setAmount(damage * 0.5f);
            victim.getEntityData().removeTag("moremod$blocked");
            damage = event.getAmount();

            if (DEBUG) {
                System.out.println("║ 格挡减伤: 50% → " + damage);
            }
        }

        List<UltimateEffectHandler.EffectConfig> effects = parseEffects(weapon);

        if (DEBUG) {
            System.out.println("║ 解析到的效果数量: " + effects.size());
            for (UltimateEffectHandler.EffectConfig effect : effects) {
                System.out.println("║   - " + effect.effectType + " = " + effect.value);
            }
            System.out.println("╚════════════════════════════════════════════════╝");
        }

        if (effects.isEmpty()) return;

        // 致命伤害检查
        if (victim.getHealth() - damage <= 0) {
            for (UltimateEffectHandler.EffectConfig effect : effects) {
                if (checkTrigger(effect, SpecialEffectTrigger.ON_LETHAL_DAMAGE)) {
                    UltimateEffectHandler.triggerEffect(
                            effect, SpecialEffectTrigger.ON_LETHAL_DAMAGE,
                            attacker, victim, damage  // ⭐ 传递source
                    );
                }
            }
        }

        // 暴击计算
        boolean isCrit = false;
        float critDamageBonus = 0.0f;

        for (UltimateEffectHandler.EffectConfig effect : effects) {
            if ("crit_chance".equals(effect.effectType)) {
                if (UltimateEffectHandler.triggerEffect(
                        effect, SpecialEffectTrigger.ON_CRIT,
                        attacker, victim, damage  // ⭐ 传递source
                )) {
                    isCrit = true;
                }
            }
            if ("crit_damage".equals(effect.effectType)) {
                critDamageBonus += effect.value;
            }
        }

        // 应用暴击伤害
        if (isCrit && critDamageBonus > 0) {
            damage *= (1.0f + critDamageBonus);
            event.setAmount(damage);
            playCritEffect(victim);
        }

        // 连击追踪
        ComboTracker combo = getComboTracker(attacker);
        combo.hit();

        // 连击效果
        if (combo.getComboCount() >= 3) {
            for (UltimateEffectHandler.EffectConfig effect : effects) {
                if (checkTrigger(effect, SpecialEffectTrigger.ON_COMBO)) {
                    UltimateEffectHandler.triggerEffect(
                            effect, SpecialEffectTrigger.ON_COMBO,
                            attacker, victim, damage  // ⭐ 传递source
                    );
                }
            }
        }

        // 触发命中效果
        SpecialEffectTrigger trigger = isCrit ?
                SpecialEffectTrigger.ON_CRIT : SpecialEffectTrigger.ON_HIT;

        for (UltimateEffectHandler.EffectConfig effect : effects) {
            String type = effect.effectType;

            // 跳过已处理
            if ("crit_chance".equals(type) || "crit_damage".equals(type)) continue;
            if ("dodge_chance".equals(type) || "block_chance".equals(type)) continue;

            if (checkTrigger(effect, trigger)) {
                boolean triggered = UltimateEffectHandler.triggerEffect(
                        effect, trigger, attacker, victim, damage  // ⭐ 传递source
                );

                if (DEBUG && triggered) {
                    System.out.println("[UltimateEvent] ✅ 触发效果: " + effect.effectType);

                    // ⭐ 如果是无敌帧效果，显示额外信息
                    if (type.contains("iframe") || type.equals("reduce_iframes") ||
                            type.equals("ignore_iframes") || type.equals("iframe_penetration")) {
                        System.out.println("    ⭐ 新的无敌帧: " + victim.hurtResistantTime + " ticks");
                    }
                }
            }
        }

        // 受击者的反伤
        if (victim instanceof EntityPlayer) {
            ItemStack victimWeapon = ((EntityPlayer) victim).getHeldItemMainhand();
            List<UltimateEffectHandler.EffectConfig> victimEffects = parseEffects(victimWeapon);

            for (UltimateEffectHandler.EffectConfig effect : victimEffects) {
                if (checkTrigger(effect, SpecialEffectTrigger.ON_HIT_TAKEN)) {
                    UltimateEffectHandler.triggerEffect(
                            effect, SpecialEffectTrigger.ON_HIT_TAKEN,
                            victim, attacker, damage  // ⭐ 传递source
                    );
                }
            }
        }
    }

    // ==========================================
    // 击杀事件 - ON_KILL
    // ==========================================

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingDeath(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        Entity sourceEntity = source.getTrueSource();

        if (!(sourceEntity instanceof EntityPlayer)) return;
        if (event.getEntityLiving().world.isRemote) return;

        // ⭐ AOE/横扫击杀不触发效果（防止递归）
        if (isAOEOrSweepDamage(source)) {
            if (DEBUG) {
                System.out.println("[UltimateEvent] ⭐ AOE/横扫击杀，跳过击杀效果");
            }
            return;
        }

        // ⭐ 剑气击杀不触发新剑气
        boolean isBeamKill = isSwordBeamDamage(source);

        EntityPlayer attacker = (EntityPlayer) sourceEntity;
        EntityLivingBase victim = event.getEntityLiving();
        ItemStack weapon = attacker.getHeldItemMainhand();

        if (weapon.isEmpty()) return;

        List<UltimateEffectHandler.EffectConfig> effects = parseEffects(weapon);

        for (UltimateEffectHandler.EffectConfig effect : effects) {
            if (checkTrigger(effect, SpecialEffectTrigger.ON_KILL)) {
                // ⭐ 如果是剑气效果且是剑气击杀，跳过
                if (isBeamKill && (effect.effectType.contains("beam") ||
                        effect.effectType.equals("sword_beam") ||
                        effect.effectType.equals("sword_beam_onkill"))) {
                    if (DEBUG) {
                        System.out.println("[UltimateEvent] ⭐ 剑气击杀不触发新剑气: " + effect.effectType);
                    }
                    continue;
                }

                UltimateEffectHandler.triggerEffect(
                        effect, SpecialEffectTrigger.ON_KILL,
                        attacker, victim, 0  // ⭐ 传递source
                );
            }
        }
    }

    // ==========================================
    // 右键事件 - ON_USE
    // ==========================================

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        ItemStack item = event.getItemStack();
        if (item.isEmpty()) return;

        List<UltimateEffectHandler.EffectConfig> effects = parseEffects(item);

        for (UltimateEffectHandler.EffectConfig effect : effects) {
            if (checkTrigger(effect, SpecialEffectTrigger.ON_USE)) {
                UltimateEffectHandler.triggerEffect(
                        effect, SpecialEffectTrigger.ON_USE,
                        player, null, 0  // ⭐ ON_USE没有伤害源
                );
            }
        }
    }

    // ==========================================
    // Tick事件 - ON_LOW_HEALTH, ON_FULL_HEALTH, ON_SPRINT
    // ==========================================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;
        ItemStack weapon = player.getHeldItemMainhand();

        if (weapon.isEmpty()) return;

        List<UltimateEffectHandler.EffectConfig> effects = parseEffects(weapon);
        if (effects.isEmpty()) return;

        float healthPercent = player.getHealth() / player.getMaxHealth();

        for (UltimateEffectHandler.EffectConfig effect : effects) {
            // 低血量
            if (checkTrigger(effect, SpecialEffectTrigger.ON_LOW_HEALTH)) {
                float threshold = effect.getFloat("threshold", 0.3f);
                if (healthPercent <= threshold) {
                    UltimateEffectHandler.triggerEffect(
                            effect, SpecialEffectTrigger.ON_LOW_HEALTH,
                            player, null, 0  // ⭐ Tick事件没有伤害源
                    );
                }
            }

            // 满生命值
            if (checkTrigger(effect, SpecialEffectTrigger.ON_FULL_HEALTH)) {
                if (healthPercent >= 0.99f) {
                    UltimateEffectHandler.triggerEffect(
                            effect, SpecialEffectTrigger.ON_FULL_HEALTH,
                            player, null, 0
                    );
                }
            }

            // 冲刺
            if (checkTrigger(effect, SpecialEffectTrigger.ON_SPRINT)) {
                if (player.isSprinting()) {
                    UltimateEffectHandler.triggerEffect(
                            effect, SpecialEffectTrigger.ON_SPRINT,
                            player, null, 0
                    );
                }
            }
        }

        // 更新连击
        ComboTracker combo = COMBO_TRACKERS.get(player.getUniqueID());
        if (combo != null) {
            combo.tick();
        }
    }

    // ==========================================
    // ✅ 核心修复：从镶嵌的宝石中解析效果配置
    // ==========================================

    /**
     * ✅ 从镶嵌的宝石中读取效果（而不是从武器NBT）
     */
    private static List<UltimateEffectHandler.EffectConfig> parseEffects(ItemStack weapon) {
        List<UltimateEffectHandler.EffectConfig> effects = new ArrayList<>();

        // ✅ 检查是否有镶嵌宝石
        if (!GemSocketHelper.hasSocketedGems(weapon)) {
            return effects;
        }

        // ✅ 获取所有镶嵌的宝石
        ItemStack[] gems = GemSocketHelper.getAllSocketedGems(weapon);

        if (DEBUG) {
            System.out.println("║ 镶嵌宝石数量: " + gems.length);
        }

        // ✅ 遍历每个宝石，提取其词条
        for (int i = 0; i < gems.length; i++) {
            ItemStack gem = gems[i];
            if (gem.isEmpty() || !GemNBTHelper.isIdentified(gem)) {
                if (DEBUG) {
                    System.out.println("║ 宝石 " + (i+1) + ": 空或未鉴定");
                }
                continue;
            }

            if (DEBUG) {
                System.out.println("║ 宝石 " + (i+1) + ": " + gem.getDisplayName());
            }

            // ✅ 获取宝石的词条
            List<IdentifiedAffix> affixes = GemNBTHelper.getAffixes(gem);

            if (DEBUG) {
                System.out.println("║   词条数量: " + affixes.size());
            }

            for (IdentifiedAffix affix : affixes) {
                GemAffix.AffixType type = affix.getAffix().getType();

                // 只处理 SPECIAL_EFFECT 类型
                if (type != GemAffix.AffixType.SPECIAL_EFFECT) {
                    continue;
                }

                // ✅ 获取效果类型和数值（从GemAffix的parameters中，不是从NBT）
                String effectType = (String) affix.getAffix().getParameter("effectType");
                if (effectType == null) {
                    if (DEBUG) {
                        System.out.println("║     ⚠️ 词条缺少 effectType 参数");
                    }
                    continue;
                }

                float value = affix.getValue();

                // 创建效果配置
                UltimateEffectHandler.EffectConfig config =
                        new UltimateEffectHandler.EffectConfig(effectType, value);

                // ✅ 复制所有参数（从GemAffix的parameters中）
                Map<String, Object> params = affix.getAffix().getParameters();
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    config.param(entry.getKey(), entry.getValue());
                }

                effects.add(config);

                if (DEBUG) {
                    System.out.println("║     ✅ 效果: " + effectType + " = " + value);
                }
            }
        }

        return effects;
    }

    /**
     * 检查trigger是否匹配
     */
    private static boolean checkTrigger(UltimateEffectHandler.EffectConfig effect,
                                        SpecialEffectTrigger trigger) {
        String effectTrigger = effect.getString("trigger", "ON_HIT");
        try {
            SpecialEffectTrigger configuredTrigger =
                    SpecialEffectTrigger.valueOf(effectTrigger);
            return configuredTrigger == trigger;
        } catch (IllegalArgumentException e) {
            return trigger == SpecialEffectTrigger.ON_HIT; // 默认触发器
        }
    }

    /**
     * 获取连击追踪器
     */
    private static ComboTracker getComboTracker(EntityPlayer player) {
        UUID uuid = player.getUniqueID();
        return COMBO_TRACKERS.computeIfAbsent(uuid, k -> new ComboTracker());
    }

    // ==========================================
    // ⭐ 调试工具
    // ==========================================

    /**
     * 用于测试无敌帧效果的辅助方法
     * 可以通过命令或其他方式调用
     */
    public static void debugIframes(EntityLivingBase entity) {
        if (entity == null) return;

        System.out.println("========== 无敌帧调试信息 ==========");
        System.out.println("实体: " + entity.getName());
        System.out.println("当前无敌时间: " + entity.hurtResistantTime + " ticks");
        System.out.println("受伤时间: " + entity.hurtTime + " ticks");
        System.out.println("最大无敌时间: " + entity.maxHurtResistantTime + " ticks");

        if (entity.getEntityData().hasKey("moremod$iframe_penetration")) {
            float pen = entity.getEntityData().getFloat("moremod$iframe_penetration");
            long expire = entity.getEntityData().getLong("moremod$iframe_penetration_expire");
            long current = entity.world.getTotalWorldTime();
            System.out.println("穿透标记: " + (pen * 100) + "%");
            System.out.println("过期时间: " + expire + " (当前: " + current + ")");
            System.out.println("剩余时间: " + (expire - current) + " ticks");
        } else {
            System.out.println("穿透标记: 无");
        }

        System.out.println("===================================");
    }

    /**
     * 清理过期的无敌帧标记
     * 定期调用以防止内存泄漏
     */
    public static void cleanupExpiredIframeMarks(EntityLivingBase entity) {
        if (entity == null) return;

        if (entity.getEntityData().hasKey("moremod$iframe_penetration_expire")) {
            long expireTime = entity.getEntityData().getLong("moremod$iframe_penetration_expire");
            long currentTime = entity.world.getTotalWorldTime();

            if (currentTime >= expireTime) {
                entity.getEntityData().removeTag("moremod$iframe_penetration");
                entity.getEntityData().removeTag("moremod$iframe_penetration_expire");
            }
        }
    }

    // ==========================================
    // 特效方法
    // ==========================================

    private static void playDodgeEffect(EntityLivingBase entity) {
        entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
                net.minecraft.init.SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                net.minecraft.util.SoundCategory.PLAYERS, 0.5f, 1.5f);

        if (entity.world instanceof net.minecraft.world.WorldServer) {
            ((net.minecraft.world.WorldServer) entity.world).spawnParticle(
                    net.minecraft.util.EnumParticleTypes.CLOUD,
                    entity.posX, entity.posY + 1, entity.posZ,
                    10, 0.3, 0.5, 0.3, 0.05
            );
        }
    }

    private static void playBlockEffect(EntityLivingBase entity) {
        entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
                net.minecraft.init.SoundEvents.ITEM_SHIELD_BLOCK,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 1.0f);

        if (entity.world instanceof net.minecraft.world.WorldServer) {
            ((net.minecraft.world.WorldServer) entity.world).spawnParticle(
                    net.minecraft.util.EnumParticleTypes.CRIT,
                    entity.posX, entity.posY + 1, entity.posZ,
                    5, 0.3, 0.3, 0.3, 0.0
            );
        }
    }

    private static void playCritEffect(EntityLivingBase entity) {
        entity.world.playSound(null, entity.posX, entity.posY, entity.posZ,
                net.minecraft.init.SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                net.minecraft.util.SoundCategory.PLAYERS, 1.0f, 1.0f);

        if (entity.world instanceof net.minecraft.world.WorldServer) {
            ((net.minecraft.world.WorldServer) entity.world).spawnParticle(
                    net.minecraft.util.EnumParticleTypes.CRIT,
                    entity.posX, entity.posY + entity.height / 2, entity.posZ,
                    20, 0.5, 0.5, 0.5, 0.1
            );
        }
    }

    // ==========================================
    // 连击追踪器
    // ==========================================

    private static class ComboTracker {
        private int comboCount = 0;
        private int ticksSinceLastHit = 0;
        private static final int COMBO_TIMEOUT = 60; // 3秒

        public void hit() {
            if (ticksSinceLastHit > COMBO_TIMEOUT) {
                comboCount = 1;
            } else {
                comboCount++;
            }
            ticksSinceLastHit = 0;
        }

        public void tick() {
            ticksSinceLastHit++;
            if (ticksSinceLastHit > COMBO_TIMEOUT) {
                comboCount = 0;
            }
        }

        public int getComboCount() {
            return comboCount;
        }
    }
}