package com.moremod.capability.module.impl;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.AbstractMechCoreModule;
import com.moremod.capability.module.ModuleContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * 经验放大器模块
 *
 * 功能：
 *  - 击杀敌人时产生额外经验
 *  - 拾取经验球时增幅经验值
 *  - 连杀系统
 *  - Lv.1: 1.5x 经验倍率, 附魔等级 +5
 *  - Lv.2: 2.0x 经验倍率, 附魔等级 +10
 *  - Lv.3: 2.5x 经验倍率, 附魔等级 +15
 *  - Lv.4: 3.0x 经验倍率, 附魔等级 +20
 *  - Lv.5: 3.5x 经验倍率, 附魔等级 +25
 *
 * 能量消耗：
 *  - 击杀消耗：baseExp * 3 RF
 *  - 拾取消耗：orbValue * 2 RF
 *
 * 特性：
 *  - 连杀系统：5秒内连续击杀 +0.1 倍率/次（最大 +1.0）
 *  - 生成独立的奖励经验球（防止无限增幅）
 */
public class ExpAmplifierModule extends AbstractMechCoreModule {

    public static final ExpAmplifierModule INSTANCE = new ExpAmplifierModule();

    // 连杀超时时间（毫秒）
    private static final long COMBO_TIMEOUT = 5000L;
    // 奖励经验球标记（防止双重增幅）
    private static final String BONUS_ORB_TAG = "MechanicalCoreBonusOrb";

    private ExpAmplifierModule() {
        super(
            "EXP_AMPLIFIER",
            "经验放大器",
            "增加经验获取并提升附魔等级",
            5  // 最大等级
        );
    }

    @Override
    public void onActivate(EntityPlayer player, IMechCoreData data, int newLevel) {
        // 初始化元数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setLong("LAST_KILL_TIME", 0);
        meta.setInteger("KILL_COMBO", 0);
    }

    @Override
    public void onDeactivate(EntityPlayer player, IMechCoreData data) {
        // 清除元数据
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setLong("LAST_KILL_TIME", 0);
        meta.setInteger("KILL_COMBO", 0);
    }

    @Override
    public void onTick(EntityPlayer player, IMechCoreData data, ModuleContext context) {
        if (context.isRemote()) return;

        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        // 每 10 秒检查一次连杀超时
        if (player.world.getTotalWorldTime() % 200 == 0) {
            NBTTagCompound meta = data.getModuleMeta(getModuleId());
            long lastKillTime = meta.getLong("LAST_KILL_TIME");
            long now = System.currentTimeMillis();

            if (lastKillTime > 0 && now - lastKillTime > COMBO_TIMEOUT) {
                // 连杀超时，重置
                int combo = meta.getInteger("KILL_COMBO");
                if (combo > 0) {
                    meta.setInteger("KILL_COMBO", 0);
                }
            }
        }
    }

    @Override
    public void onLevelChanged(EntityPlayer player, IMechCoreData data, int oldLevel, int newLevel) {
        // 等级变化时重置连杀
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setInteger("KILL_COMBO", 0);
    }

    /**
     * 击杀敌人时产生额外经验
     *
     * 此方法应该在 LivingDeathEvent 中调用
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @param killed 被击杀的实体
     */
    public void onEntityKill(EntityPlayer player, IMechCoreData data, EntityLivingBase killed) {
        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return;

        NBTTagCompound meta = data.getModuleMeta(getModuleId());

        // 计算基础经验
        int baseExp = computeBaseExperience(killed);
        if (baseExp <= 0) return;

        // 能量消耗
        int energyCost = Math.max(10, baseExp * 3);
        if (!data.consumeEnergy(energyCost)) return;

        // 连杀系统
        long now = System.currentTimeMillis();
        long lastKillTime = meta.getLong("LAST_KILL_TIME");
        int combo = 0;

        if (lastKillTime > 0 && now - lastKillTime < COMBO_TIMEOUT) {
            combo = meta.getInteger("KILL_COMBO") + 1;
            meta.setInteger("KILL_COMBO", Math.min(combo, 10));
        } else {
            meta.setInteger("KILL_COMBO", 0);
        }

        meta.setLong("LAST_KILL_TIME", now);

        // 计算倍率
        float baseMultiplier = 1.0F + (0.5F * level);  // 1.5x/2.0x/2.5x/3.0x/3.5x
        float comboBonus = combo * 0.1F;
        float totalMultiplier = baseMultiplier + comboBonus;

        // 计算奖励经验
        int bonusExp = (int) (baseExp * (totalMultiplier - 1.0F));

        if (bonusExp > 0) {
            spawnBonusExperience(player, killed, bonusExp);
            showKillBonusEffect(player, baseExp, bonusExp, combo);
        }
    }

    /**
     * 拾取经验球时增幅
     *
     * 此方法应该在 PlayerPickupXpEvent 中调用
     *
     * @param player 玩家
     * @param data 机械核心数据
     * @param orb 经验球
     * @return 增幅后的经验值（如果增幅成功）
     */
    public int amplifyXpOrb(EntityPlayer player, IMechCoreData data, EntityXPOrb orb) {
        int level = data.getModuleLevel(getModuleId());
        if (level <= 0) return orb.getXpValue();

        // 跳过奖励球（防止双重增幅）
        if (orb.getEntityData().getBoolean(BONUS_ORB_TAG)) {
            return orb.getXpValue();
        }

        // 能量消耗
        int cost = Math.max(5, orb.getXpValue() * 2);
        if (!data.consumeEnergy(cost)) {
            return orb.getXpValue();
        }

        // 计算增幅
        float multiplier = 1.0F + (0.5F * level);
        int original = orb.getXpValue();
        int newValue = (int) (original * multiplier);

        // 显示提示（降低频率避免刷屏）
        if (player.world.getTotalWorldTime() % 20 == 0) {
            int bonus = newValue - original;
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "✨ 经验增幅 +" + bonus +
                            TextFormatting.AQUA + " [x" + String.format("%.1f", multiplier) + "]"
            ), true);

            // 粒子效果
            for (int i = 0; i < 3; i++) {
                player.world.spawnParticle(
                        EnumParticleTypes.VILLAGER_HAPPY,
                        player.posX + (Math.random() - 0.5),
                        player.posY + 1.0 + Math.random(),
                        player.posZ + (Math.random() - 0.5),
                        0, 0.1, 0
                );
            }
        }

        return newValue;
    }

    /**
     * 获取附魔等级加成
     *
     * @param level 模块等级
     * @return 附魔等级加成
     */
    public static int getEnchantmentBonus(int level) {
        if (level <= 0) return 0;
        return level * 5;  // 5/10/15/20/25
    }

    /**
     * 计算实体基础经验
     */
    private int computeBaseExperience(EntityLivingBase entity) {
        // Boss
        if (entity instanceof net.minecraft.entity.boss.EntityWither ||
                entity instanceof net.minecraft.entity.boss.EntityDragon) {
            return 50;
        }

        // 怪物
        if (entity instanceof EntityMob) {
            // 特殊怪物
            if (entity instanceof net.minecraft.entity.monster.EntityEnderman ||
                    entity instanceof net.minecraft.entity.monster.EntityCreeper ||
                    entity instanceof net.minecraft.entity.monster.EntityWitch ||
                    entity instanceof net.minecraft.entity.monster.EntityBlaze) {
                return 10;
            }
            // 普通怪物
            return 5;
        }

        // 动物
        if (entity instanceof EntityAnimal) {
            return 1;
        }

        // 村民（不给经验）
        if (entity instanceof EntityVillager) {
            return 0;
        }

        return 0;
    }

    /**
     * 生成奖励经验球
     */
    private void spawnBonusExperience(EntityPlayer player, EntityLivingBase killed, int totalExp) {
        while (totalExp > 0) {
            int orbExp = Math.min(totalExp, 10);
            EntityXPOrb xpOrb = new EntityXPOrb(
                    killed.world,
                    killed.posX,
                    killed.posY,
                    killed.posZ,
                    orbExp
            );

            // 标记为奖励球（防止双重增幅）
            xpOrb.getEntityData().setBoolean(BONUS_ORB_TAG, true);

            // 给经验球一个初速度
            xpOrb.motionX = (player.world.rand.nextDouble() - 0.5) * 0.2;
            xpOrb.motionY = 0.3 + player.world.rand.nextDouble() * 0.2;
            xpOrb.motionZ = (player.world.rand.nextDouble() - 0.5) * 0.2;

            killed.world.spawnEntity(xpOrb);
            totalExp -= orbExp;
        }
    }

    /**
     * 显示击杀奖励效果
     */
    private void showKillBonusEffect(EntityPlayer player, int baseExp, int bonusExp, int combo) {
        StringBuilder msg = new StringBuilder();
        msg.append(TextFormatting.GOLD).append("⚔ 击杀奖励 +").append(bonusExp).append(" EXP");

        if (combo > 0) {
            msg.append(TextFormatting.LIGHT_PURPLE).append(" 连杀x").append(combo + 1);
        }

        player.sendStatusMessage(new TextComponentString(msg.toString()), true);

        // 音效（连杀时音调提高）
        float pitch = 1.0F + (combo * 0.1F);
        player.world.playSound(
                null,
                player.posX, player.posY, player.posZ,
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS,
                0.5F, pitch
        );
    }

    /**
     * 获取玩家连杀数
     */
    public int getPlayerCombo(IMechCoreData data) {
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        return meta.getInteger("KILL_COMBO");
    }

    /**
     * 重置玩家连杀数
     */
    public void resetPlayerCombo(IMechCoreData data) {
        NBTTagCompound meta = data.getModuleMeta(getModuleId());
        meta.setInteger("KILL_COMBO", 0);
        meta.setLong("LAST_KILL_TIME", 0);
    }

    @Override
    public int getPassiveEnergyCost(int level) {
        // 经验放大器无被动消耗（仅在击杀/拾取时消耗）
        return 0;
    }

    @Override
    public boolean canExecute(EntityPlayer player, IMechCoreData data) {
        // 总是可以执行（能量检查在具体方法中）
        return true;
    }

    @Override
    public NBTTagCompound getDefaultMeta() {
        NBTTagCompound meta = new NBTTagCompound();
        meta.setLong("LAST_KILL_TIME", 0);
        meta.setInteger("KILL_COMBO", 0);
        return meta;
    }

    @Override
    public boolean validateMeta(NBTTagCompound meta) {
        if (!meta.hasKey("LAST_KILL_TIME")) {
            meta.setLong("LAST_KILL_TIME", 0);
        }
        if (!meta.hasKey("KILL_COMBO")) {
            meta.setInteger("KILL_COMBO", 0);
        }
        return true;
    }
}
