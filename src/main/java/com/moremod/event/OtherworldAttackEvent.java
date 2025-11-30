package com.moremod.event;

import com.moremod.combat.TrueDamageHelper;
import com.moremod.fabric.data.UpdatedFabricPlayerData;
import com.moremod.fabric.system.FabricWeavingSystem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.*;

/**
 * 异界纤维战斗增强系统 - 凹函数增伤版本
 * 使用凹函数使得只有在极限状态下才能获得最高增伤
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class OtherworldAttackEvent {

    private static final Random RANDOM = new Random();
    private static final UUID OTHERWORLD_ATTACK_UUID = UUID.fromString("92B4E6A5-7C3F-4D9B-9E83-2A5D3F8C9A11");

    // 玩家装备状态追踪
    private static final Map<UUID, Integer> PLAYER_EQUIPMENT_TRACKER = new ConcurrentHashMap<>();

    /**
     * 凹函数计算器 - 使用对数函数实现平滑增长
     * @param value 当前值
     * @param max 最大值
     * @param scale 缩放系数（越小增长越慢）
     * @return 0到1之间的凹函数值
     */
    private static double concaveFunction(double value, double max, double scale) {
        if (value <= 0) return 0;
        if (value >= max) value = max;
        double normalized = value / max;
        // 使用 log(1 + x * scale) / log(1 + scale) 的凹函数
        return Math.log(1 + normalized * scale) / Math.log(1 + scale);
    }

    /**
     * 极限状态凹函数 - 只有接近极限时才快速增长
     */
    private static double extremeConcaveFunction(double value, double max) {
        if (value <= 0) return 0;
        if (value >= max) value = max;
        double normalized = value / max;
        // 使用 x^3 函数，早期增长极慢，后期快速增长
        return Math.pow(normalized, 3);
    }

    /**
     * 玩家Tick事件 - 监控装备变化
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        if (event.player.ticksExisted % 10 != 0) return;

        EntityPlayer player = event.player;
        UUID playerId = player.getUniqueID();

        int currentCount = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);
        Integer previousCount = PLAYER_EQUIPMENT_TRACKER.get(playerId);

        if (previousCount == null || previousCount != currentCount) {
            PLAYER_EQUIPMENT_TRACKER.put(playerId, currentCount);

            if (currentCount == 0) {
                cleanupOtherworldAttributes(player);

                if (previousCount != null && previousCount > 0) {
                    player.sendStatusMessage(new TextComponentString("§5异界之力消散..."), true);
                }
            }
        }

        if (currentCount == 0) {
            IAttributeInstance attackAttribute = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
            if (attackAttribute != null && attackAttribute.getModifier(OTHERWORLD_ATTACK_UUID) != null) {
                attackAttribute.removeModifier(OTHERWORLD_ATTACK_UUID);
            }
        }
    }

    /**
     * 深渊凝视 - 暴击事件处理（改进版）
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAbyssGazeCritical(CriticalHitEvent event) {
        if (!(event.getEntityPlayer() instanceof EntityPlayer)) return;
        if (!(event.getTarget() instanceof EntityLivingBase)) return;

        EntityPlayer player = event.getEntityPlayer();

        int otherworldCount = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);
        if (otherworldCount <= 0) {
            cleanupOtherworldAttributes(player);
            return;
        }

        EntityLivingBase target = (EntityLivingBase) event.getTarget();

        NBTTagCompound otherworldData = getOtherworldData(player);
        if (otherworldData == null) return;

        int abyssGazeStacks = otherworldData.getInteger("AbyssGazeStacks");
        int insight = otherworldData.getInteger("Insight");
        int sanity = otherworldData.getInteger("Sanity");
        int forbiddenKnowledge = otherworldData.getInteger("ForbiddenKnowledge");

        if (abyssGazeStacks > 0) {
            // 基础暴击倍率 - 使用凹函数
            float baseMultiplier = 1.5f;

            // 深渊凝视加成 - 使用凹函数，最高贡献2.5
            float gazeBonus = (float)(concaveFunction(abyssGazeStacks, 10, 5) * 2.5);

            // 灵视加成 - 使用凹函数，最高贡献1.0
            float insightBonus = (float)(concaveFunction(insight, 100, 3) * 1.0);

            // 疯狂加成 - 使用极限凹函数，只有接近疯狂时才显著，最高贡献2.0
            float madnessValue = sanity < 50 ? (50 - sanity) : 0;
            float madnessBonus = (float)(extremeConcaveFunction(madnessValue, 50) * 2.0);

            // 禁忌知识加成 - 使用极限凹函数，需要大量积累才有显著效果，最高贡献2.5
            float knowledgeBonus = (float)(extremeConcaveFunction(forbiddenKnowledge, 50) * 2.5);

            // 最终暴击倍率：1.5 + 2.5 + 1.0 + 2.0 + 2.5 = 最高9.5倍
            float finalMultiplier = baseMultiplier + gazeBonus + insightBonus + madnessBonus + knowledgeBonus;

            event.setDamageModifier(finalMultiplier);
            event.setResult(Event.Result.ALLOW);

            if (player.world instanceof WorldServer) {
                spawnAbyssGazeCritEffect((WorldServer)player.world, target, abyssGazeStacks);
            }

            // 层数增长变慢
            if (RANDOM.nextFloat() < (0.3f - abyssGazeStacks * 0.02f)) {
                abyssGazeStacks = min(abyssGazeStacks + 1, 10);
                updateOtherworldData(player, "AbyssGazeStacks", abyssGazeStacks);
            }

            // 显示详细信息
            if (abyssGazeStacks >= 3 || finalMultiplier >= 3.0f) {
                player.sendStatusMessage(new TextComponentString(
                        String.format("§5§l深渊凝视 §d[%d层] §c暴击×%.1f §7[禁忌:%d 疯狂:%.0f%%]",
                                abyssGazeStacks, finalMultiplier, forbiddenKnowledge, madnessValue * 2)), true);
            }

            if (abyssGazeStacks >= 7) {
                applyAbyssGazeSpecialEffect(player, target, abyssGazeStacks);
            }
        }
        else if (insight >= 30) {
            // 灵视触发深渊凝视的门槛提高
            float critChance = (float)concaveFunction(insight - 30, 70, 2) * 0.5f;
            if (RANDOM.nextFloat() < critChance) {
                event.setDamageModifier(event.getDamageModifier() + 0.5f);
                event.setResult(Event.Result.ALLOW);

                if (insight >= 70 && RANDOM.nextFloat() < 0.3f) {
                    updateOtherworldData(player, "AbyssGazeStacks", 1);
                    player.sendStatusMessage(new TextComponentString("§5深渊开始凝视你..."), true);
                }
            }
        }
    }

    /**
     * 异界之力 - 攻击倍率增幅（改进版）
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onOtherworldDamageAmplify(LivingDamageEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        int otherworldCount = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);

        if (otherworldCount <= 0) {
            IAttributeInstance attackAttribute = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
            if (attackAttribute != null && attackAttribute.getModifier(OTHERWORLD_ATTACK_UUID) != null) {
                attackAttribute.removeModifier(OTHERWORLD_ATTACK_UUID);
            }
            return;
        }

        NBTTagCompound otherworldData = getOtherworldData(player);
        if (otherworldData == null) return;

        int insight = otherworldData.getInteger("Insight");
        int sanity = otherworldData.getInteger("Sanity");
        int forbiddenKnowledge = otherworldData.getInteger("ForbiddenKnowledge");
        int abyssGazeStacks = otherworldData.getInteger("AbyssGazeStacks");

        // 基础倍率
        float baseMultiplier = 1.0f;

        // 装备加成 - 使用凹函数，最高贡献0.6
        float equipmentBonus = (float)(concaveFunction(otherworldCount, 4, 3) * 0.6);

        // 灵视加成 - 使用凹函数，最高贡献0.8
        float insightMultiplier = (float)(concaveFunction(insight, 100, 4) * 0.8);

        // 深渊凝视加成 - 使用凹函数，最高贡献1.0
        float gazeMultiplier = (float)(concaveFunction(abyssGazeStacks, 10, 4) * 1.0);

        // 禁忌知识加成 - 使用极限凹函数，需要大量积累，最高贡献1.6
        float knowledgeMultiplier = (float)(extremeConcaveFunction(forbiddenKnowledge, 50) * 1.6);

        // 疯狂加成 - 使用极限凹函数，只在极度疯狂时显著，最高贡献1.0
        float madnessValue = sanity < 50 ? (50 - sanity) : 0;
        float madnessMultiplier = (float)(extremeConcaveFunction(madnessValue, 50) * 1.0);

        // 综合倍率：1.0 + 0.6 + 0.8 + 1.0 + 1.6 + 1.0 = 最高6.0倍
        float totalMultiplier = baseMultiplier + equipmentBonus + insightMultiplier +
                gazeMultiplier + knowledgeMultiplier + madnessMultiplier;

        // 极限协同加成：当禁忌知识和疯狂都很高时，额外加成
        if (forbiddenKnowledge >= 30 && madnessValue >= 40) {
            float synergyBonus = (float)(extremeConcaveFunction(forbiddenKnowledge, 50) *
                    extremeConcaveFunction(madnessValue, 50) * 0.5);
            totalMultiplier += synergyBonus;
        }

        event.setAmount(event.getAmount() * totalMultiplier);

        // 疯狂反噬
        if (sanity < 30 && RANDOM.nextFloat() < (0.05f + madnessMultiplier * 0.1f)) {
            float backlash = event.getAmount() * (0.1f + madnessMultiplier * 0.15f);
            player.attackEntityFrom(DamageSource.MAGIC, backlash);
            player.sendStatusMessage(new TextComponentString(
                    String.format("§5异界之力反噬！§c -%.0f HP", backlash)), true);
        }

        // 特效触发门槛提高
        if (totalMultiplier >= 4.0f) {
            applyOtherworldSpecialEffect(player, event.getEntityLiving(), totalMultiplier);
        }

        // 信息显示
        if (RANDOM.nextFloat() < 0.15f || totalMultiplier >= 3.5f) {
            player.sendStatusMessage(new TextComponentString(
                    String.format("§5异界之力 §d×%.1f §7[灵视:%d 理智:%d 禁忌:%d 凝视:%d]",
                            totalMultiplier, insight, sanity, forbiddenKnowledge, abyssGazeStacks)), true);
        }

        applyOtherworldAttackAttribute(player, totalMultiplier, otherworldCount);
    }

    /**
     * 处理异界纤维的额外效果（调整版）
     */
    @SubscribeEvent
    public static void onOtherworldHurt(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

        int otherworldCount = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);
        if (otherworldCount <= 0) {
            cleanupOtherworldAttributes(player);
            return;
        }

        NBTTagCompound otherworldData = getOtherworldData(player);
        if (otherworldData == null) return;

        int insight = otherworldData.getInteger("Insight");
        int sanity = otherworldData.getInteger("Sanity");

        // 灵视和理智的变化速度降低，使得达到极限更困难
        if (RANDOM.nextFloat() < 0.2f) {
            // 灵视增长变慢
            if (insight < 50) {
                insight = min(insight + 1, 100);
            } else if (insight < 80 && RANDOM.nextFloat() < 0.5f) {
                insight = min(insight + 1, 100);
            } else if (RANDOM.nextFloat() < 0.2f) {
                insight = min(insight + 1, 100);
            }

            // 理智下降变慢
            if (sanity > 30) {
                sanity = Math.max(sanity - 1, 0);
            } else if (sanity > 10 && RANDOM.nextFloat() < 0.5f) {
                sanity = Math.max(sanity - 1, 0);
            } else if (RANDOM.nextFloat() < 0.3f) {
                sanity = Math.max(sanity - 1, 0);
            }

            updateOtherworldData(player, "Insight", insight);
            updateOtherworldData(player, "Sanity", sanity);
        }

        if (event.getEntityLiving().getHealth() <= event.getAmount()) {
            onOtherworldKill(player, event.getEntityLiving(), otherworldData);
        }
    }

    /**
     * 清理异界属性
     */
    public static void cleanupOtherworldAttributes(EntityPlayer player) {
        IAttributeInstance attackAttribute = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attackAttribute != null) {
            attackAttribute.removeModifier(OTHERWORLD_ATTACK_UUID);
        }
        PLAYER_EQUIPMENT_TRACKER.remove(player.getUniqueID());
    }

    /**
     * 击杀时的特殊效果（调整版）
     */
    private static void onOtherworldKill(EntityPlayer player, EntityLivingBase victim, NBTTagCompound data) {
        int insight = data.getInteger("Insight");
        int forbiddenKnowledge = data.getInteger("ForbiddenKnowledge");
        int abyssGazeStacks = data.getInteger("AbyssGazeStacks");
        int sanity = data.getInteger("Sanity");

        // 灵视增长基于当前值递减
        int insightGain = insight < 50 ? 2 : (insight < 80 ? 1 : 0);
        if (insightGain > 0 && (insight < 80 || RANDOM.nextFloat() < 0.3f)) {
            insight = min(insight + insightGain, 100);
            updateOtherworldData(player, "Insight", insight);
        }

        // 禁忌知识只从强大敌人获得，且增长缓慢
        if (victim.getMaxHealth() > 50) {
            // 获取禁忌知识的概率随已有知识递减
            float knowledgeChance = forbiddenKnowledge < 10 ? 0.8f :
                    (forbiddenKnowledge < 20 ? 0.5f :
                            (forbiddenKnowledge < 30 ? 0.3f : 0.1f));

            if (RANDOM.nextFloat() < knowledgeChance) {
                forbiddenKnowledge++;
                updateOtherworldData(player, "ForbiddenKnowledge", forbiddenKnowledge);
                player.sendStatusMessage(new TextComponentString("§5§l获得禁忌知识！§r §7[" + forbiddenKnowledge + "]"), false);

                // 获得禁忌知识时理智大幅下降
                sanity = Math.max(sanity - 5, 0);
                updateOtherworldData(player, "Sanity", sanity);
            }
        }

        // 深渊凝视层数缓慢衰减
        if (abyssGazeStacks > 0 && RANDOM.nextFloat() < 0.1f) {
            abyssGazeStacks = Math.max(abyssGazeStacks - 1, 0);
            updateOtherworldData(player, "AbyssGazeStacks", abyssGazeStacks);
        }

        // 击杀时有小概率恢复理智
        if (RANDOM.nextFloat() < 0.05f) {
            sanity = min(sanity + 1, 100);
            updateOtherworldData(player, "Sanity", sanity);
            player.sendStatusMessage(new TextComponentString("§a理智略微恢复..."), true);
        }
    }

    /**
     * 应用深渊凝视特殊效果
     */
    private static void applyAbyssGazeSpecialEffect(EntityPlayer player, EntityLivingBase target, int stacks) {
        if (stacks >= 10) {
            if (target.getHealth() < target.getMaxHealth() * 0.2f) {
                TrueDamageHelper.triggerVanillaDeathChain(target);
                player.sendStatusMessage(new TextComponentString("§5§l深渊吞噬！"), false);
                updateOtherworldData(player, "AbyssGazeStacks", Math.max(stacks - 3, 0));
            }
        } else if (stacks >= 7) {
            float trueDamage = target.getMaxHealth() * (0.05f + stacks * 0.01f);
            target.attackEntityFrom(DamageSource.OUT_OF_WORLD, trueDamage);
        }
    }

    /**
     * 应用异界特殊效果
     */
    private static void applyOtherworldSpecialEffect(EntityPlayer player, EntityLivingBase target, float multiplier) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        if (multiplier >= 4.0f) {
            float damageRadius = 3.0f + (multiplier - 4.0f) * 0.5f;
            float damageAmount = 10.0f + (multiplier - 4.0f) * 5.0f;

            world.getEntitiesWithinAABB(EntityLivingBase.class,
                    target.getEntityBoundingBox().grow(damageRadius),
                    e -> e != player && e != target && e.isEntityAlive()
            ).forEach(entity -> {
                entity.attackEntityFrom(DamageSource.causePlayerDamage(player), damageAmount);

                double dx = target.posX - entity.posX;
                double dz = target.posZ - entity.posZ;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 0) {
                    entity.motionX = dx / distance * 0.5;
                    entity.motionZ = dz / distance * 0.5;
                }
            });

            for (int i = 0; i < 20 + (int)(multiplier * 2); i++) {
                double angle = (Math.PI * 2 * i) / 20;
                double x = target.posX + Math.cos(angle) * damageRadius;
                double z = target.posZ + Math.sin(angle) * damageRadius;
                world.spawnParticle(EnumParticleTypes.PORTAL,
                        x, target.posY + 1, z,
                        1, 0, 0.1, 0, 0.05);
            }

            world.playSound(null, target.getPosition(),
                    SoundEvents.ENTITY_ENDERMEN_TELEPORT,
                    SoundCategory.PLAYERS, 1.0F, 0.5F);
        }
    }

    /**
     * 应用异界攻击属性
     */
    private static void applyOtherworldAttackAttribute(EntityPlayer player, float multiplier, int equipmentCount) {
        IAttributeInstance attackAttribute = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attackAttribute != null) {
            attackAttribute.removeModifier(OTHERWORLD_ATTACK_UUID);

            if (equipmentCount > 0 && multiplier > 1.0f) {
                double bonus = (multiplier - 1.0) * 10;
                if (bonus > 0) {
                    attackAttribute.applyModifier(new AttributeModifier(
                            OTHERWORLD_ATTACK_UUID,
                            "Otherworld Power",
                            bonus,
                            0
                    ));
                }
            }
        }
    }

    /**
     * 生成深渊凝视暴击效果
     */
    private static void spawnAbyssGazeCritEffect(WorldServer world, EntityLivingBase target, int stacks) {
        for (int i = 0; i < stacks * 5; i++) {
            double angle = (Math.PI * 2 * i) / 10;
            double radius = 1.5 - (i * 0.05);
            double height = i * 0.1;

            double x = target.posX + Math.cos(angle) * radius;
            double z = target.posZ + Math.sin(angle) * radius;
            double y = target.posY + height;

            world.spawnParticle(EnumParticleTypes.DRAGON_BREATH,
                    x, y, z,
                    1, 0, 0.05, 0, 0.02);
        }

        world.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                target.posX, target.posY + target.height, target.posZ,
                20 + stacks * 5, 0.5, 0.5, 0.5, 0.1);

        world.playSound(null, target.getPosition(),
                SoundEvents.ENTITY_WITHER_HURT,
                SoundCategory.PLAYERS, 0.5F, 2.0F - stacks * 0.05F);
    }

    /**
     * 获取玩家的异界布料数据
     */
    private static NBTTagCompound getOtherworldData(EntityPlayer player) {
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (FabricWeavingSystem.getFabricType(armor) == UpdatedFabricPlayerData.FabricType.OTHERWORLD) {
                return FabricWeavingSystem.getFabricData(armor);
            }
        }
        return null;
    }

    /**
     * 更新异界数据到装备
     */
    private static void updateOtherworldData(EntityPlayer player, String key, int value) {
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (FabricWeavingSystem.getFabricType(armor) == UpdatedFabricPlayerData.FabricType.OTHERWORLD) {
                NBTTagCompound data = FabricWeavingSystem.getFabricData(armor);
                data.setInteger(key, value);
                FabricWeavingSystem.updateFabricData(armor, data);
                break;
            }
        }
    }
}