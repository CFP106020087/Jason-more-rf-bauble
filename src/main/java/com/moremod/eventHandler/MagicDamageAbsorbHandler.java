package com.moremod.eventHandler;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "moremod")
public class MagicDamageAbsorbHandler {

    private static final String TAG_SCORCH = "mm_scorch_stacks";
    private static final String TAG_LAST_HIT = "mm_scorch_last_hit";

    public static final int MAX_SCORCH = 30;
    public static final int DECAY_DELAY = 100;
    public static final int DECAY_RATE = 1;
    public static final int BURST_THRESHOLD = 20;

    // 🔥 余灼攻速加成 UUID（固定，用于识别和移除）
    private static final UUID SCORCH_ATTACK_SPEED_UUID = UUID.fromString("8f3c7e5a-2d4b-4f9a-b8c1-9e6d4a7f3b2c");
    private static final String SCORCH_MODIFIER_NAME = "Scorch Attack Speed";

    // 🔥 攻速加成配置
    private static final double ATTACK_SPEED_PER_SCORCH = 0.04; // 每层余灼 +4% 攻速
    // 30层 = +120% 攻速！

    // ===== 连击系统 =====
    private static final ThreadLocal<Boolean> IS_CHAIN_HIT = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Integer> TL_CHAIN_COUNT = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Float> TL_CHAIN_MULTIPLIER = ThreadLocal.withInitial(() -> 1.0f);

    // 🔥 指数增长配置（进一步提高）
    private static final float CHAIN_BASE_MULTIPLIER = 1.30f;  // 从 1.25 提升到 1.30
    private static final float CHAIN_MAX_MULTIPLIER = 6.0f;    // 从 5.0 提升到 6.0
    private static final int BURST_CHAIN_COUNT = 6;            // 从 5 提升到 6

    // ====================================================================
    // 公开方法
    // ====================================================================

    public static int getScorch(EntityPlayer player) {
        return getPersisted(player).getInteger(TAG_SCORCH);
    }

    public static void setScorch(EntityPlayer player, int value) {
        getPersisted(player).setInteger(TAG_SCORCH, value);
        // 🔥 余灼改变时，立即更新攻速
        updateAttackSpeed(player, value);
    }

    public static void setLastHitTime(EntityPlayer player, long time) {
        getPersisted(player).setLong(TAG_LAST_HIT, time);
    }

    public static void clearScorchTags(EntityPlayer player) {
        NBTTagCompound persisted = getPersisted(player);
        persisted.removeTag(TAG_SCORCH);
        persisted.removeTag(TAG_LAST_HIT);
        player.getEntityData().setTag("PlayerPersisted", persisted);

        // 🔥 清空余灼时，移除攻速加成
        removeAttackSpeedModifier(player);
    }

    // ====================================================================
    // 🔥 攻速加成系统
    // ====================================================================

    /**
     * 根据余灼层数更新攻速
     */
    private static void updateAttackSpeed(EntityPlayer player, int scorch) {
        IAttributeInstance attackSpeed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (attackSpeed == null) return;

        // 移除旧的修饰符
        AttributeModifier oldModifier = attackSpeed.getModifier(SCORCH_ATTACK_SPEED_UUID);
        if (oldModifier != null) {
            attackSpeed.removeModifier(oldModifier);
        }

        // 添加新的修饰符
        if (scorch > 0) {
            double bonus = scorch * ATTACK_SPEED_PER_SCORCH;
            AttributeModifier newModifier = new AttributeModifier(
                    SCORCH_ATTACK_SPEED_UUID,
                    SCORCH_MODIFIER_NAME,
                    bonus,
                    2  // 操作类型：2 = MULTIPLY_TOTAL（乘法）
            );
            attackSpeed.applyModifier(newModifier);
        }
    }

    /**
     * 移除攻速修饰符
     */
    private static void removeAttackSpeedModifier(EntityPlayer player) {
        IAttributeInstance attackSpeed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (attackSpeed == null) return;

        AttributeModifier modifier = attackSpeed.getModifier(SCORCH_ATTACK_SPEED_UUID);
        if (modifier != null) {
            attackSpeed.removeModifier(modifier);
        }
    }

    // ====================================================================
    // 连击系统（指数增长版）
    // ====================================================================
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onChainAttack(LivingHurtEvent event) {

        // ============================================
        // 阶段1：应用连击倍率
        // ============================================
        if (IS_CHAIN_HIT.get()) {
            float multiplier = TL_CHAIN_MULTIPLIER.get();
            if (multiplier > 1.0f) {
                event.setAmount(event.getAmount() * multiplier);
            }
            return;
        }

        // ============================================
        // 阶段2：检查是否触发连击链
        // ============================================
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        EntityLivingBase target = event.getEntityLiving();

        NBTTagCompound tempData = player.getEntityData();

        float chainChance = tempData.getFloat("mm_chain_chance");
        int burstChains = tempData.getInteger("mm_burst_chains");

        tempData.removeTag("mm_chain_chance");
        tempData.removeTag("mm_burst_chains");

        if (chainChance <= 0 && burstChains <= 0) return;

        // ============================================
        // 阶段3：执行连击链（指数增长）
        // ============================================
        IS_CHAIN_HIT.set(true);
        TL_CHAIN_COUNT.set(0);
        TL_CHAIN_MULTIPLIER.set(1.0f);

        try {
            // 概率连击（最多4次，增加上限）
            if (chainChance > 0) {
                for (int i = 0; i < 4; i++) {
                    if (target.isDead) break;
                    if (player.getRNG().nextFloat() < chainChance) {
                        performChainHit(player, target);
                    } else {
                        break;
                    }
                }
            }

            // 爆心连击（固定6次）
            if (burstChains > 0) {
                for (int i = 0; i < BURST_CHAIN_COUNT; i++) {
                    if (target.isDead) break;
                    performChainHit(player, target);
                }
            }

        } finally {
            IS_CHAIN_HIT.set(false);
            TL_CHAIN_COUNT.remove();
            TL_CHAIN_MULTIPLIER.remove();
        }
    }

    /**
     * 执行单次连击（指数增长）
     */
    private static void performChainHit(EntityPlayer player, EntityLivingBase target) {
        if (player == null || target == null) return;
        if (player.isDead || target.isDead) return;

        // ============================================
        // 🔥 指数增长计算
        // ============================================
        int chainCount = TL_CHAIN_COUNT.get() + 1;
        TL_CHAIN_COUNT.set(chainCount);

        // 指数公式：multiplier = BASE^chainCount
        // 1.30^1 = 1.30 (130%)
        // 1.30^2 = 1.69 (169%)
        // 1.30^3 = 2.20 (220%)
        // 1.30^4 = 2.86 (286%)
        // 1.30^5 = 3.71 (371%)
        // 1.30^6 = 4.83 (483%)
        float multiplier = (float) Math.pow(CHAIN_BASE_MULTIPLIER, chainCount);

        if (multiplier > CHAIN_MAX_MULTIPLIER) {
            multiplier = CHAIN_MAX_MULTIPLIER;
        }

        TL_CHAIN_MULTIPLIER.set(multiplier);

        // ============================================
        // 触发攻击
        // ============================================
        target.hurtResistantTime = 0;
        player.attackTargetEntityWithCurrentItem(target);

        // ============================================
        // 视觉效果
        // ============================================
        if (!player.world.isRemote) {
            // 连击粒子（越多越密集）
            for (int i = 0; i < chainCount; i++) {
                player.world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.CRIT_MAGIC,
                        target.posX + (player.getRNG().nextDouble() - 0.5) * target.width,
                        target.posY + target.height * 0.5,
                        target.posZ + (player.getRNG().nextDouble() - 0.5) * target.width,
                        0, 0.5, 0
                );
            }

            // 高倍率火焰特效
            if (multiplier >= 2.0f) {
                for (int i = 0; i < 3; i++) {
                    player.world.spawnParticle(
                            net.minecraft.util.EnumParticleTypes.FLAME,
                            target.posX + (player.getRNG().nextDouble() - 0.5) * target.width,
                            target.posY + target.height * 0.7,
                            target.posZ + (player.getRNG().nextDouble() - 0.5) * target.width,
                            0, 0.2, 0
                    );
                }
            }

            // 爆心巨型爆炸特效
            if (chainCount >= 6) {
                player.world.spawnParticle(
                        net.minecraft.util.EnumParticleTypes.EXPLOSION_HUGE,
                        target.posX, target.posY + target.height * 0.5, target.posZ,
                        0, 0, 0
                );

                // 爆炸音效
                player.world.playSound(null, target.getPosition(),
                        net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE,
                        net.minecraft.util.SoundCategory.PLAYERS,
                        1.5f, 0.7f);
            }
        }
    }

    // ====================================================================
    // 余灼衰减 + 攻速更新
    // ====================================================================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;

        EntityPlayer player = event.player;

        // 每秒检查一次
        if (player.ticksExisted % 20 != 0) return;

        int scorch = getScorch(player);
        if (scorch <= 0) return;

        long now = player.world.getTotalWorldTime();
        long lastHit = getLastHitTime(player);

        // 5秒未攻击，开始衰减
        if (now - lastHit > DECAY_DELAY) {
            scorch -= DECAY_RATE;
            if (scorch <= 0) {
                clearScorchTags(player);
            } else {
                setScorch(player, scorch); // 这会自动更新攻速
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clearScorchTags(event.player);
    }

    // ====================================================================
    // 私有工具
    // ====================================================================
    private static NBTTagCompound getPersisted(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey("PlayerPersisted")) {
            data.setTag("PlayerPersisted", new NBTTagCompound());
        }
        return data.getCompoundTag("PlayerPersisted");
    }

    private static long getLastHitTime(EntityPlayer player) {
        return getPersisted(player).getLong(TAG_LAST_HIT);
    }

    // ====================================================================
    // 客户端显示
    // ====================================================================
    @SideOnly(Side.CLIENT)
    public static int getClientScorch(EntityPlayer player) {
        if (player == null) return 0;
        try {
            return getScorch(player);
        } catch (Exception e) {
            return 0;
        }
    }

    @SideOnly(Side.CLIENT)
    public static boolean isBurstReady(EntityPlayer player) {
        if (player == null) return false;
        try {
            return getClientScorch(player) >= BURST_THRESHOLD;
        } catch (Exception e) {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    public static int getMaxScorch() {
        return MAX_SCORCH;
    }

    /**
     * 获取当前攻速加成（用于 GUI 显示）
     */
    @SideOnly(Side.CLIENT)
    public static double getAttackSpeedBonus(EntityPlayer player) {
        int scorch = getClientScorch(player);
        return scorch * ATTACK_SPEED_PER_SCORCH * 100; // 转换为百分比
    }
}
