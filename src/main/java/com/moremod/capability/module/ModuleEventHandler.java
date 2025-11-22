package com.moremod.capability.module;

import com.moremod.capability.IMechCoreData;
import com.moremod.capability.module.impl.*;
import com.moremod.item.ItemMechanicalCore;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 模块事件处理器
 *
 * 处理需要监听 Forge 事件的模块逻辑
 */
@Mod.EventBusSubscriber
public class ModuleEventHandler {

    /**
     * 处理玩家受伤事件 - 反伤系统
     *
     * 优先级：NORMAL
     * 在伤害计算后、护盾耗尽检测前触发
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onPlayerHurt(LivingHurtEvent event) {
        // 只处理玩家受伤
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote) return;

        // 获取机械核心
        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return;

        // 获取 Capability 数据
        IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);
        if (data == null) return;

        // 检查攻击者
        if (event.getSource().getTrueSource() instanceof EntityLivingBase) {
            EntityLivingBase attacker = (EntityLivingBase) event.getSource().getTrueSource();

            // 应用反伤效果
            ThornsModule.INSTANCE.applyThorns(player, data, attacker, event.getAmount());
        }
    }

    /**
     * 处理玩家造成伤害事件 - 伤害加成、暴击、追击
     *
     * 优先级：LOWEST
     * 在所有其他 mod 处理完伤害后，最后应用伤害加成
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtLowest(LivingHurtEvent event) {
        // 只处理玩家造成的伤害
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        // 获取机械核心
        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return;

        // 获取 Capability 数据
        IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);
        if (data == null) return;

        float damage = event.getAmount();

        // 应用伤害提升
        int damageLevel = data.getModuleLevel("DAMAGE_BOOST");
        if (damageLevel > 0) {
            float multiplier = DamageBoostModule.INSTANCE.getDamageMultiplier(player, data);
            damage *= multiplier;

            // 显示伤害加成
            if (multiplier > 1.0F && player.world.rand.nextInt(5) == 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + String.format("⚔ 伤害加成 +%d%%", (int)((multiplier - 1) * 100))
                ), true);
            }
        }

        // 应用暴击
        damage = DamageBoostModule.INSTANCE.applyCritical(player, data, damage);

        // 应用追击伤害
        float pursuitBonus = PursuitModule.INSTANCE.getPursuitDamage(player, data, event.getEntity());
        if (pursuitBonus > 0) {
            damage *= (1 + pursuitBonus);

            // 显示追击加成
            if (player.world.rand.nextInt(3) == 0) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.LIGHT_PURPLE + String.format("⚡ 追击加成 +%d%%", (int)(pursuitBonus * 100))
                ), true);
            }
        }

        // 设置最终伤害
        event.setAmount(damage);
    }

    /**
     * 处理攻击事件 - 连击、追击标记
     *
     * 优先级：NORMAL
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onAttackEntity(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        // 获取机械核心
        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return;

        // 获取 Capability 数据
        IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);
        if (data == null) return;

        // 检查攻击速度连击
        AttackSpeedModule.INSTANCE.checkCombo(player, data);

        if (event.getTarget() instanceof EntityLivingBase) {
            // 追击标记
            PursuitModule.INSTANCE.markTarget(player, data, event.getTarget());

            // 追击冲刺（潜行时触发）
            if (player.isSneaking()) {
                PursuitModule.INSTANCE.dashToTarget(player, data, event.getTarget());
            }
        }
    }

    /**
     * 处理方块破坏事件 - 动能发电
     *
     * 优先级：NORMAL
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world.isRemote) return;

        // 获取机械核心
        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return;

        // 获取 Capability 数据
        IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);
        if (data == null) return;

        // 动能发电：挖掘产能
        float hardness = event.getState().getBlockHardness(player.world, event.getPos());
        KineticGeneratorModule.INSTANCE.generateFromBlockBreak(player, data, hardness);
    }

    /**
     * 处理实体死亡事件 - 战斗充能
     *
     * 优先级：NORMAL
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onEntityDeath(LivingDeathEvent event) {
        // 只处理玩家击杀
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();
        if (player.world.isRemote) return;

        // 获取机械核心
        ItemStack coreStack = ItemMechanicalCore.getCoreFromPlayer(player);
        if (coreStack.isEmpty()) return;

        // 获取 Capability 数据
        IMechCoreData data = player.getCapability(IMechCoreData.CAPABILITY, null);
        if (data == null) return;

        // 战斗充能
        CombatChargerModule.INSTANCE.onEntityKill(player, data, event.getEntityLiving());
    }
}
