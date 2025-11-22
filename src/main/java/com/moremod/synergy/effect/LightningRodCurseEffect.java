package com.moremod.synergy.effect;

import com.moremod.item.ItemMechanicalCore;
import com.moremod.synergy.api.ISynergyEffect;
import com.moremod.upgrades.WetnessSystem;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 雷击诅咒效果 - 吸引雷击但获得过载爆发
 *
 * 机制：
 * - 湿度每达到20%的倍数，雷击概率增加
 * - 雷雨天时雷击优先打击玩家
 * - 被雷击时：造成8点伤害、能量溢出50%
 * - 但同时获得10秒过载模式（力量III + 速度II + 临时解锁所有模块）
 */
public class LightningRodCurseEffect implements ISynergyEffect {

    private static final int LIGHTNING_DAMAGE = 8; // 4颗心
    private static final float ENERGY_OVERFLOW_PERCENT = 0.5F; // 50%能量溢出
    private static final int OVERLOAD_DURATION = 200; // 10秒（200 tick）

    @Override
    public String getEffectId() {
        return "lightning_rod_curse";
    }

    @Override
    @SubscribeEvent
    public void onPlayerTick(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        World world = player.world;

        if (world.isRemote) return;
        if (!world.isThundering()) return; // 只在雷雨天生效

        // 检查过载模式状态
        checkOverloadMode(player, world);

        // 随机检查是否召唤雷击（减少频率）
        if (player.ticksExisted % 20 != 0) return; // 每秒检查一次

        // 获取湿度
        int wetness = WetnessSystem.getWetness(player);

        // 计算雷击风险：每20%湿度增加风险
        int lightningRisk = wetness / 20;

        // 雷击概率：0-5的风险值，概率从0%到25%
        int chance = lightningRisk; // 最大5，即5/200 = 2.5%每秒
        if (world.rand.nextInt(200) < chance) {
            summonLightningStrike(player, world);
        }
    }

    /**
     * 召唤雷击
     */
    private void summonLightningStrike(EntityPlayer player, World world) {
        // 生成雷击
        EntityLightningBolt lightning = new EntityLightningBolt(
            world,
            player.posX,
            player.posY,
            player.posZ,
            false // 不引发火灾
        );
        world.spawnEntity(lightning);

        // 造成伤害
        player.attackEntityFrom(
            net.minecraft.util.DamageSource.LIGHTNING_BOLT,
            LIGHTNING_DAMAGE
        );

        // 能量溢出
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (!core.isEmpty()) {
            IEnergyStorage energy = core.getCapability(CapabilityEnergy.ENERGY, null);
            if (energy != null) {
                int stored = energy.getEnergyStored();
                int lost = (int) (stored * ENERGY_OVERFLOW_PERCENT);
                energy.extractEnergy(lost, false);

                player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "⚡ 雷击能量溢出！损失 " + lost + " RF"
                ));
            }
        }

        // 激活过载模式
        activateOverloadMode(player, core, world);
    }

    /**
     * 激活过载模式
     */
    private void activateOverloadMode(EntityPlayer player, ItemStack core, World world) {
        // 施加增益效果
        player.addPotionEffect(new PotionEffect(
            MobEffects.STRENGTH, OVERLOAD_DURATION, 2, false, true
        ));
        player.addPotionEffect(new PotionEffect(
            MobEffects.SPEED, OVERLOAD_DURATION, 1, false, true
        ));
        player.addPotionEffect(new PotionEffect(
            MobEffects.RESISTANCE, OVERLOAD_DURATION, 0, false, true
        ));

        // 设置过载模式标记
        if (!core.isEmpty()) {
            NBTTagCompound nbt = core.getTagCompound();
            if (nbt == null) {
                nbt = new NBTTagCompound();
                core.setTagCompound(nbt);
            }

            nbt.setBoolean("OverloadMode", true);
            nbt.setLong("OverloadEnd", world.getTotalWorldTime() + OVERLOAD_DURATION);

            // 临时移除能量锁定
            nbt.setBoolean("EmergencyMode", false);
        }

        // 通知玩家
        player.sendMessage(new TextComponentString(
            TextFormatting.LIGHT_PURPLE + "⚡⚡⚡ 雷击过载！所有系统超频运行！⚡⚡⚡"
        ));

        // 特效：紫色闪电粒子环绕
        spawnOverloadParticles(player, world);

        // 音效：扭曲的凋零音效
        world.playSound(
            null,
            player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_WITHER_SPAWN,
            SoundCategory.PLAYERS,
            0.5F, 1.5F
        );
    }

    /**
     * 检查过载模式状态
     */
    private void checkOverloadMode(EntityPlayer player, World world) {
        ItemStack core = ItemMechanicalCore.getCoreFromPlayer(player);
        if (core.isEmpty()) return;

        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return;

        if (!nbt.getBoolean("OverloadMode")) return;

        long endTime = nbt.getLong("OverloadEnd");
        long currentTime = world.getTotalWorldTime();

        if (currentTime >= endTime) {
            // 过载模式结束
            nbt.setBoolean("OverloadMode", false);
            nbt.setBoolean("EmergencyMode", true); // 恢复能量锁定

            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "过载模式结束，系统恢复正常运行"
            ));
        } else {
            // 过载模式进行中，生成粒子
            if (player.ticksExisted % 5 == 0) {
                spawnOverloadParticles(player, world);
            }
        }
    }

    /**
     * 生成过载模式粒子特效
     */
    private void spawnOverloadParticles(EntityPlayer player, World world) {
        // 紫色电火花环绕
        for (int i = 0; i < 3; i++) {
            double angle = player.ticksExisted * 0.1 + i * Math.PI * 2 / 3;
            double radius = 1.0;
            double x = player.posX + Math.cos(angle) * radius;
            double z = player.posZ + Math.sin(angle) * radius;
            double y = player.posY + 1.0 + world.rand.nextDouble() * 1.0;

            world.spawnParticle(
                EnumParticleTypes.DRAGON_BREATH,
                x, y, z,
                0, 0.1, 0
            );
        }

        // 顶部闪电
        world.spawnParticle(
            EnumParticleTypes.END_ROD,
            player.posX,
            player.posY + 2.5,
            player.posZ,
            (world.rand.nextDouble() - 0.5) * 0.2,
            -0.1,
            (world.rand.nextDouble() - 0.5) * 0.2
        );
    }
}
