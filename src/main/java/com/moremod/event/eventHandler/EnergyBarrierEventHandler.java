package com.moremod.event.eventHandler;

import baubles.api.BaublesApi;
import com.moremod.item.ItemEnergyBarrier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "moremod")
public class EnergyBarrierEventHandler {

    // 记录玩家最近一次格挡的时间
    private static final Map<UUID, Long> lastBlockTime = new HashMap<>();
    private static final long BLOCK_COOLDOWN = 100;

    // 记录玩家正在承受的持续伤害
    private static final Map<UUID, Long> continuousDamageTime = new HashMap<>();
    private static final long CONTINUOUS_DAMAGE_WINDOW = 1000;

    // 记录玩家上一次的生命值
    private static final Map<UUID, Float> lastHealth = new HashMap<>();
    private static final Map<UUID, Float> lastMaxHealth = new HashMap<>();
    private static final Map<UUID, Boolean> protectedThisTick = new HashMap<>();

    // 免费格挡的伤害阈值
    private static final float FREE_BLOCK_THRESHOLD = 10.0F;

    /**
     * 拦截所有攻击事件 - 最早触发
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 检查是否有护盾
        if (!hasBarrierWithEnergy(player)) return;

        // 检查是否应该阻挡
        if (!shouldBlockDamage(event.getSource())) return;

        // 检查攻击者
        Entity attacker = event.getSource().getTrueSource();
        if (attacker == null) attacker = event.getSource().getImmediateSource();

        if (attacker != null) {
            String attackerClass = attacker.getClass().getName();

            // 阻挡所有寄生虫攻击
            if (attackerClass.contains("scapeandrunparasites") ||
                    attackerClass.contains("srparasites") ||
                    attackerClass.contains("EntityPFeral") ||
                    attackerClass.contains("EntityParasiteBase")) {

                event.setCanceled(true);
                protectedThisTick.put(player.getUniqueID(), true);

                // 第一次消耗能量
                consumeBarrierEnergySimple(player, event.getAmount(), "寄生虫攻击");
            }
        }
    }

    /**
     * 监控玩家生命值变化 - 用于捕获直接的setHealth调用
     * 这会在FirstAid处理之前触发
     */


    /**
     * 处理伤害修改事件 - 在计算防御后触发
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 如果伤害已经是0，跳过
        if (event.getAmount() <= 0) return;

        // 检查是否有护盾
        if (!hasBarrierWithEnergy(player)) return;

        // 检查是否应该阻挡
        if (!shouldBlockDamage(event.getSource())) return;

        // 检查是否是FirstAid转换的魔法伤害
        if (event.getSource().isMagicDamage() && event.getSource().getTrueSource() == null) {
            // 这可能是FirstAid转换的伤害
            event.setAmount(0);
            protectedThisTick.put(player.getUniqueID(), true);

            if (!player.world.isRemote) {
                player.sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.GOLD + "[神域护盾] " +
                                        TextFormatting.AQUA + "拦截生命篡改"
                        ), true);
            }
        } else {
            event.setAmount(0);
            protectedThisTick.put(player.getUniqueID(), true);
        }
    }

    /**
     * 处理最终伤害事件
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        UUID playerUUID = player.getUniqueID();

        // 如果伤害为0，说明被其他机制阻挡了
        if (event.getAmount() <= 0.0F) return;

        // 检查是否有护盾
        if (!hasBarrierWithEnergy(player)) return;

        // 检查是否应该格挡
        if (!shouldBlockDamage(event.getSource())) return;

        // 检查是否在格挡冷却时间内
        Long lastBlock = lastBlockTime.get(playerUUID);
        long currentTime = System.currentTimeMillis();

        // 冷却时间内的免费格挡
        if (lastBlock != null && currentTime - lastBlock < BLOCK_COOLDOWN) {
            event.setAmount(0.0F);
            event.setCanceled(true);
            protectedThisTick.put(playerUUID, true);

            if (!player.world.isRemote) {
                player.sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.GOLD + "[神域护盾] " +
                                        TextFormatting.AQUA + "连续攻击防护激活"
                        ), true);
            }
            return;
        }

        // 消耗能量并格挡
        if (consumeBarrierEnergy(player, event.getAmount(), event.getSource())) {
            event.setAmount(0.0F);
            event.setCanceled(true);
            lastBlockTime.put(playerUUID, currentTime);
            protectedThisTick.put(playerUUID, true);
        }
    }

    /**
     * 检查玩家是否有护盾
     */
    private static boolean hasBarrierWithEnergy(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemEnergyBarrier) {
                return true;
            }
        }
        return false;
    }

    /**
     * 简化的能量消耗（用于寄生虫攻击和神秘伤害）
     */
    private static boolean consumeBarrierEnergySimple(EntityPlayer player, float damage, String damageType) {
        // 10点以下伤害免费格挡
        if (damage < FREE_BLOCK_THRESHOLD) {
            if (!player.world.isRemote) {
                player.sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.GOLD + "[神域护盾] " +
                                        TextFormatting.AQUA + "微小" + damageType + "自动抵消" +
                                        TextFormatting.GRAY + " (伤害: " + String.format("%.1f", damage) + ")"
                        ), true);

                // 播放轻微的防护音效
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        net.minecraft.init.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        player.getSoundCategory(), 0.3F, 1.2F);
            }
            return true;
        }

        // 检查冷却
        UUID playerUUID = player.getUniqueID();
        Long lastBlock = lastBlockTime.get(playerUUID);
        long currentTime = System.currentTimeMillis();

        if (lastBlock != null && currentTime - lastBlock < BLOCK_COOLDOWN) {
            // 冷却期间不消耗能量
            return true;
        }

        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemEnergyBarrier) {
                ItemEnergyBarrier barrier = (ItemEnergyBarrier) stack.getItem();

                // 寄生虫攻击和神秘伤害使用极少的能量
                int energyCost = Math.max(1, ItemEnergyBarrier.COST_PER_BLOCK / 10);

                if (barrier.getEnergyStored(stack) >= energyCost) {
                    barrier.consumeEnergy(stack, energyCost);
                    lastBlockTime.put(playerUUID, currentTime);

                    if (!player.world.isRemote) {
                        player.sendStatusMessage(
                                new TextComponentString(
                                        TextFormatting.GOLD + "[神域护盾] " +
                                                TextFormatting.RED + "阻挡" + damageType + "！" +
                                                TextFormatting.YELLOW + " (剩余：" + barrier.getEnergyStored(stack) + " RF)"
                                ), true);

                        // 播放防护音效
                        player.world.playSound(null, player.posX, player.posY, player.posZ,
                                net.minecraft.init.SoundEvents.ITEM_SHIELD_BLOCK,
                                player.getSoundCategory(), 0.6F, 0.8F);
                    }

                    return true;
                } else {
                    // 能量不足
                    if (!player.world.isRemote) {
                        player.sendStatusMessage(
                                new TextComponentString(
                                        TextFormatting.DARK_RED + "[神域护盾] 能量耗尽！无法抵御" + damageType + "！"
                                ), true);
                    }
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 消耗护盾能量（常规伤害）
     */
    private static boolean consumeBarrierEnergy(EntityPlayer player, float damageAmount, DamageSource source) {
        // 如果伤害低于10点，免费格挡
        if (damageAmount < FREE_BLOCK_THRESHOLD) {
            if (!player.world.isRemote) {
                String damageType = getDamageTypeName(source);
                player.sendStatusMessage(
                        new TextComponentString(
                                TextFormatting.GOLD + "[神域护盾] " +
                                        TextFormatting.AQUA + "微小伤害自动抵消：" + damageType +
                                        TextFormatting.GRAY + " (伤害: " + String.format("%.1f", damageAmount) + ")"
                        ), true);

                // 播放轻微的防护音效
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        net.minecraft.init.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        player.getSoundCategory(), 0.3F, 1.2F);
            }
            return true;
        }

        UUID playerUUID = player.getUniqueID();
        boolean isHighDamage = damageAmount >= 100;
        boolean isElementDamage = source.getClass().getName().contains("ElementDamageSource");
        boolean isHitscan = source.getClass().getName().contains("HitscanDamageSource") ||
                (source.getTrueSource() != null &&
                        source.getTrueSource().getClass().getName().contains("Asmodeus"));
        boolean isPoisonOrWither = "poison".equals(source.damageType) ||
                "wither".equals(source.damageType) ||
                (source.isMagicDamage() && damageAmount <= 2);

        Entity attacker = source.getTrueSource();
        if (attacker == null) attacker = source.getImmediateSource();
        boolean isParasite = attacker != null &&
                (attacker.getClass().getName().contains("scapeandrunparasites") ||
                        attacker.getClass().getName().contains("srparasites") ||
                        attacker.getClass().getName().contains("EntityPFeral") ||
                        attacker.getClass().getName().contains("EntityParasiteBase"));

        // FirstAid转换的魔法伤害
        boolean isFirstAidMagic = source.isMagicDamage() && attacker == null && "magic".equals(source.damageType);

        Long continuousDamage = continuousDamageTime.get(playerUUID);
        long currentTime = System.currentTimeMillis();

        // 遍历饰品栏寻找神域护盾
        for (int i = 0; i < BaublesApi.getBaublesHandler(player).getSlots(); i++) {
            ItemStack stack = BaublesApi.getBaublesHandler(player).getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemEnergyBarrier) {
                ItemEnergyBarrier barrier = (ItemEnergyBarrier) stack.getItem();

                // 计算能量消耗
                int energyCost = ItemEnergyBarrier.COST_PER_BLOCK;

                // FirstAid魔法伤害优化
                if (isFirstAidMagic) {
                    energyCost = Math.max(1, ItemEnergyBarrier.COST_PER_BLOCK / 10);
                }
                // 寄生虫攻击特别优化
                else if (isParasite) {
                    energyCost = Math.max(1, ItemEnergyBarrier.COST_PER_BLOCK / 10);
                }
                // 特殊情况减少能量消耗
                else if ((isHighDamage || isElementDamage || isHitscan) &&
                        continuousDamage != null &&
                        currentTime - continuousDamage < CONTINUOUS_DAMAGE_WINDOW) {
                    energyCost = ItemEnergyBarrier.COST_PER_BLOCK / 10;
                }
                // 中毒和凋零的持续伤害特别优化
                else if (isPoisonOrWither) {
                    energyCost = ItemEnergyBarrier.COST_PER_BLOCK / 20;
                }

                // 检查能量并消耗
                if (barrier.getEnergyStored(stack) >= energyCost) {
                    barrier.consumeEnergy(stack, energyCost);

                    // 记录持续伤害
                    if (isHighDamage || isElementDamage || isHitscan || isPoisonOrWither || isParasite || isFirstAidMagic) {
                        continuousDamageTime.put(playerUUID, currentTime);
                    }

                    if (!player.world.isRemote) {
                        // 特殊提示
                        String damageType = getDamageTypeName(source);
                        if (isFirstAidMagic) {
                            damageType = TextFormatting.LIGHT_PURPLE + "生命篡改";
                        } else if (isParasite) {
                            damageType = TextFormatting.DARK_RED + damageType;
                        } else if (isHitscan) {
                            damageType = TextFormatting.DARK_PURPLE + "瞬击" + damageType;
                        } else if (isHighDamage) {
                            damageType = TextFormatting.BOLD + "极限" + damageType;
                        } else if (isElementDamage) {
                            damageType = TextFormatting.LIGHT_PURPLE + "元素" + damageType;
                        } else if (isPoisonOrWither) {
                            damageType = TextFormatting.DARK_GREEN + damageType;
                        }

                        player.sendStatusMessage(
                                new TextComponentString(
                                        TextFormatting.GOLD + "[神域护盾] " +
                                                TextFormatting.GREEN + "完全抵御 " +
                                                damageType +
                                                TextFormatting.YELLOW + " (剩余：" + barrier.getEnergyStored(stack) + " RF)"
                                ), true);

                        // 播放防护音效
                        player.world.playSound(null, player.posX, player.posY, player.posZ,
                                net.minecraft.init.SoundEvents.ITEM_SHIELD_BLOCK,
                                player.getSoundCategory(), 0.6F, 0.8F);
                    }

                    return true;
                } else {
                    // 能量不足
                    if (!player.world.isRemote) {
                        player.sendStatusMessage(
                                new TextComponentString(
                                        TextFormatting.DARK_RED + "[神域护盾] 能量耗尽！无法提供全方位保护！"
                                ), true);

                        player.world.playSound(null, player.posX, player.posY, player.posZ,
                                net.minecraft.init.SoundEvents.BLOCK_ANVIL_LAND,
                                player.getSoundCategory(), 0.3F, 1.4F);
                    }
                }
                break;
            }
        }
        return false;
    }

    /**
     * 判断是否应该格挡此类型的伤害
     */
    public static boolean shouldBlockDamage(DamageSource source) {
        // 只排除创造模式伤害和虚空伤害
        if (source.canHarmInCreative()) return false;
        if ("outOfWorld".equals(source.damageType)) return false;

        return true;
    }

    /**
     * 获取伤害类型的友好名称
     */
    public static String getDamageTypeName(DamageSource source) {
        // 检查是否是SRP寄生虫
        if (source.getTrueSource() != null) {
            String entityClassName = source.getTrueSource().getClass().getName();
            if (entityClassName.contains("scapeandrunparasites") ||
                    entityClassName.contains("srparasites") ||
                    entityClassName.contains("EntityPFeral") ||
                    entityClassName.contains("EntityParasiteBase")) {
                return "寄生虫攻击";
            }
        }

        // 检查是否是ElementDamageSource
        if (source.getClass().getName().contains("ElementDamageSource")) {
            return "元素伤害";
        }

        // 检查是否是Hitscan
        if (source.getClass().getName().contains("HitscanDamageSource")) {
            return "瞬击伤害";
        }

        // 检查是否是Lycanite's Mobs的伤害
        if (source.getTrueSource() != null) {
            String entityClassName = source.getTrueSource().getClass().getName();
            if (entityClassName.contains("lycanitesmobs")) {
                // 特别检查各种BOSS
                if (entityClassName.contains("Amalgalich")) {
                    return "亡灵君主攻击";
                }
                if (entityClassName.contains("Asmodeus")) {
                    return "恶魔领主攻击";
                }
                if (entityClassName.contains("Rahovart")) {
                    return "地狱魔王攻击";
                }
                return "恐怖生物攻击";
            }
        }

        // 根据伤害类型返回中文名称
        switch (source.damageType) {
            case "mob": return "生物攻击";
            case "player": return "玩家攻击";
            case "arrow": return "箭矢伤害";
            case "poison": return "中毒伤害";
            case "magic": return "魔法伤害";
            case "wither": return "凋零伤害";
            case "fall": return "跌落伤害";
            case "drowning": return "溺水伤害";
            case "lava": return "岩浆伤害";
            case "inFire": return "火焰伤害";
            case "onFire": return "燃烧伤害";
            case "starve": return "饥饿伤害";
            default:
                return source.damageType + "伤害";
        }
    }

    /**
     * 清理过期的记录
     */
    public static void cleanupOldRecords() {
        long currentTime = System.currentTimeMillis();
        lastBlockTime.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > BLOCK_COOLDOWN * 10
        );
        continuousDamageTime.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > CONTINUOUS_DAMAGE_WINDOW * 2
        );
    }
}