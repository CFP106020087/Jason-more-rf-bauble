package com.moremod.event;

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

import java.util.Random;
import java.util.UUID;

/**
 * 异界纤维战斗增强系统
 * 深渊凝视：暴击能力增强
 * 异界之力：攻击倍率增幅
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class OtherworldAttackEvent {

    private static final Random RANDOM = new Random();
    private static final UUID OTHERWORLD_ATTACK_UUID = UUID.fromString("92B4E6A5-7C3F-4D9B-9E83-2A5D3F8C9A11");

    /**
     * 深渊凝视 - 暴击事件处理
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAbyssGazeCritical(CriticalHitEvent event) {
        if (!(event.getEntityPlayer() instanceof EntityPlayer)) return;

        // 添加目标类型检查
        if (!(event.getTarget() instanceof EntityLivingBase)) return;

        EntityPlayer player = event.getEntityPlayer();
        EntityLivingBase target = (EntityLivingBase) event.getTarget();  // 现在安全了

        NBTTagCompound otherworldData = getOtherworldData(player);
        if (otherworldData == null) return;

        int abyssGazeStacks = otherworldData.getInteger("AbyssGazeStacks");
        int insight = otherworldData.getInteger("Insight");
        int sanity = otherworldData.getInteger("Sanity");

        // 深渊凝视层数影响暴击
        if (abyssGazeStacks > 0) {
            // 基础暴击倍率：每层深渊凝视 +0.5倍
            float baseCritMultiplier = 1.5f + (abyssGazeStacks * 0.5f);

            // 灵视加成：每20灵视额外 +0.2倍
            float insightBonus = (insight / 20f) * 0.2f;

            // 疯狂加成：理智越低，暴击越高
            float madnessBonus = 0;
            if (sanity < 50) {
                madnessBonus = ((50f - sanity) / 50f) * 1.0f; // 最高额外 +1.0倍
            }

            // 最终暴击倍率
            float finalMultiplier = baseCritMultiplier + insightBonus + madnessBonus;

            // 设置暴击倍率
            event.setDamageModifier(finalMultiplier);
            event.setResult(Event.Result.ALLOW); // 强制暴击

            // 视觉效果
            if (player.world instanceof WorldServer && target instanceof EntityLivingBase) {
                spawnAbyssGazeCritEffect((WorldServer)player.world, (EntityLivingBase)target, abyssGazeStacks);
            }

            // 暴击触发深渊凝视叠加
            if (RANDOM.nextFloat() < 0.3f) {
                abyssGazeStacks = Math.min(abyssGazeStacks + 1, 10);
                updateOtherworldData(player, "AbyssGazeStacks", abyssGazeStacks);

                if (abyssGazeStacks >= 5) {
                    player.sendStatusMessage(new TextComponentString(
                            String.format("§5§l深渊凝视 §d[%d层] §c暴击×%.1f！", abyssGazeStacks, finalMultiplier)), true);
                }
            }

            // 高层数特殊效果
            if (abyssGazeStacks >= 7) {
                applyAbyssGazeSpecialEffect(player, (EntityLivingBase)target, abyssGazeStacks);
            }
        }
        // 即使没有深渊凝视层数，高灵视也有暴击加成
        else if (insight >= 50) {
            float critChance = (insight - 50) / 100f; // 50-100灵视 = 0-50%暴击率
            if (RANDOM.nextFloat() < critChance) {
                event.setDamageModifier(event.getDamageModifier() + 1.0f);
                event.setResult(Event.Result.ALLOW);

                // 触发深渊凝视
                if (insight >= 70) {
                    updateOtherworldData(player, "AbyssGazeStacks", 1);
                    player.sendStatusMessage(new TextComponentString("§5深渊开始凝视你..."), true);
                }
            }
        }
    }

    /**
     * 异界之力 - 攻击倍率增幅
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onOtherworldDamageAmplify(LivingDamageEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        int otherworldCount = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);

        if (otherworldCount <= 0) return;

        NBTTagCompound otherworldData = getOtherworldData(player);
        if (otherworldData == null) return;
        if (event == null) return;
        if (event.getSource() == null) return;
        if (event.getSource().getTrueSource() == null) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        int insight = otherworldData.getInteger("Insight");
        int sanity = otherworldData.getInteger("Sanity");
        int forbiddenKnowledge = otherworldData.getInteger("ForbiddenKnowledge");
        int abyssGazeStacks = otherworldData.getInteger("AbyssGazeStacks");

        // 基础攻击倍率
        float baseMultiplier = 1.0f;

        // 装备加成：每件 +25%攻击
        baseMultiplier += otherworldCount * 0.25f;

        // 灵视加成：每10灵视 +10%攻击
        float insightMultiplier = (insight / 10f) * 0.1f;
        baseMultiplier += insightMultiplier;

        // 禁忌知识加成：每点 +5%攻击
        float knowledgeMultiplier = forbiddenKnowledge * 0.05f;
        baseMultiplier += knowledgeMultiplier;

        // 深渊凝视加成：每层 +15%攻击
        float gazeMultiplier = abyssGazeStacks * 0.15f;
        baseMultiplier += gazeMultiplier;

        // 疯狂加成：理智越低，攻击越高（但也有风险）
        float madnessMultiplier = 0;
        if (sanity < 50) {
            madnessMultiplier = ((50f - sanity) / 50f) * 0.5f; // 最高 +50%
            baseMultiplier += madnessMultiplier;

            // 疯狂风险：可能反噬
            if (sanity < 20 && RANDOM.nextFloat() < 0.1f) {
                player.attackEntityFrom(DamageSource.MAGIC, event.getAmount() * 0.2f);
                player.sendStatusMessage(new TextComponentString("§5异界之力反噬了你！"), true);
            }
        }

        // 应用最终倍率
        event.setAmount(event.getAmount() * baseMultiplier);

        // 特殊效果触发
        if (baseMultiplier >= 3.0f) {
            // 高倍率时的特殊效果
            applyOtherworldSpecialEffect(player, event.getEntityLiving(), baseMultiplier);
        }

        // 显示伤害信息（每5次攻击显示一次）
        if (RANDOM.nextFloat() < 0.2f) {
            player.sendStatusMessage(new TextComponentString(
                    String.format("§5异界之力 §d×%.1f §7[灵视:%d 理智:%d 凝视:%d]",
                            baseMultiplier, insight, sanity, abyssGazeStacks)), true);
        }

        // 更新属性加成
        applyOtherworldAttackAttribute(player, baseMultiplier);
    }

    /**
     * 处理异界纤维的额外效果
     */
    @SubscribeEvent
    public static void onOtherworldHurt(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        NBTTagCompound otherworldData = getOtherworldData(player);

        if (otherworldData == null) return;

        int insight = otherworldData.getInteger("Insight");
        int sanity = otherworldData.getInteger("Sanity");

        // 每次攻击增加灵视，降低理智
        if (RANDOM.nextFloat() < 0.3f) {
            insight = Math.min(insight + 1, 100);
            sanity = Math.max(sanity - 1, 0);

            updateOtherworldData(player, "Insight", insight);
            updateOtherworldData(player, "Sanity", sanity);
        }

        // 击杀时的特殊处理
        if (event.getEntityLiving().getHealth() <= event.getAmount()) {
            onOtherworldKill(player, event.getEntityLiving(), otherworldData);
        }
    }

    /**
     * 击杀时的特殊效果
     */
    private static void onOtherworldKill(EntityPlayer player, EntityLivingBase victim, NBTTagCompound data) {
        int insight = data.getInteger("Insight");
        int forbiddenKnowledge = data.getInteger("ForbiddenKnowledge");
        int abyssGazeStacks = data.getInteger("AbyssGazeStacks");

        // 击杀增加灵视
        insight = Math.min(insight + 2, 100);
        updateOtherworldData(player, "Insight", insight);

        // 击杀强大生物增加禁忌知识
        if (victim.getMaxHealth() > 50) {
            forbiddenKnowledge++;
            updateOtherworldData(player, "ForbiddenKnowledge", forbiddenKnowledge);
            player.sendStatusMessage(new TextComponentString("§5§l获得禁忌知识！"), false);
        }

        // 深渊凝视层数衰减
        if (abyssGazeStacks > 0 && RANDOM.nextFloat() < 0.2f) {
            abyssGazeStacks = Math.max(abyssGazeStacks - 1, 0);
            updateOtherworldData(player, "AbyssGazeStacks", abyssGazeStacks);
        }
    }

    /**
     * 应用深渊凝视特殊效果
     */
    private static void applyAbyssGazeSpecialEffect(EntityPlayer player, EntityLivingBase target, int stacks) {
        if (stacks >= 10) {
            // 10层：即死效果
            if (target.getHealth() < target.getMaxHealth() * 0.2f) {
                target.setHealth(0);
                player.sendStatusMessage(new TextComponentString("§5§l深渊吞噬！"), false);

                // 即死后重置层数
                updateOtherworldData(player, "AbyssGazeStacks", 0);
            }
        } else if (stacks >= 7) {
            // 7层：真实伤害
            float trueDamage = target.getMaxHealth() * 0.1f;
            target.attackEntityFrom(DamageSource.OUT_OF_WORLD, trueDamage);
        }
    }

    /**
     * 应用异界特殊效果
     */
    private static void applyOtherworldSpecialEffect(EntityPlayer player, EntityLivingBase target, float multiplier) {
        if (!(player.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) player.world;

        // 虚空撕裂效果
        if (multiplier >= 4.0f) {
            // 造成范围伤害
            world.getEntitiesWithinAABB(EntityLivingBase.class,
                    target.getEntityBoundingBox().grow(3),
                    e -> e != player && e != target && e.isEntityAlive()
            ).forEach(entity -> {
                entity.attackEntityFrom(DamageSource.causePlayerDamage(player), 10);

                // 拉扯效果
                double dx = target.posX - entity.posX;
                double dz = target.posZ - entity.posZ;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 0) {
                    entity.motionX = dx / distance * 0.5;
                    entity.motionZ = dz / distance * 0.5;
                }
            });

            // 视觉效果
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20;
                double x = target.posX + Math.cos(angle) * 3;
                double z = target.posZ + Math.sin(angle) * 3;
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
    private static void applyOtherworldAttackAttribute(EntityPlayer player, float multiplier) {
        IAttributeInstance attackAttribute = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attackAttribute != null) {
            // 移除旧的修饰符
            attackAttribute.removeModifier(OTHERWORLD_ATTACK_UUID);

            // 添加新的修饰符
            double bonus = (multiplier - 1.0) * 10; // 转换为固定攻击力加成
            if (bonus > 0) {
                attackAttribute.applyModifier(new AttributeModifier(
                        OTHERWORLD_ATTACK_UUID,
                        "Otherworld Power",
                        bonus,
                        0 // ADD操作
                ));
            }
        }
    }

    /**
     * 生成深渊凝视暴击效果
     */
    private static void spawnAbyssGazeCritEffect(WorldServer world, EntityLivingBase target, int stacks) {
        // 深紫色螺旋上升粒子
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

        // 暴击指示器
        world.spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                target.posX, target.posY + target.height, target.posZ,
                20, 0.5, 0.5, 0.5, 0.1);

        // 音效
        world.playSound(null, target.getPosition(),
                SoundEvents.ENTITY_WITHER_HURT,
                SoundCategory.PLAYERS, 0.5F, 2.0F);
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