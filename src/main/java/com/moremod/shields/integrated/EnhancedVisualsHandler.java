package com.moremod.shields.integrated;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.ItemCrudeEnergyBarrier;
import com.moremod.item.ItemBasicEnergyBarrier;
import com.moremod.item.ItemadvEnergyBarrier;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * EnhancedVisuals 视觉效果处理器
 * 为护盾系统提供视觉效果免疫
 */
@SideOnly(Side.CLIENT)
public class EnhancedVisualsHandler {

    // 单例实例，用于注册到事件总线
    public static EnhancedVisualsHandler instance = new EnhancedVisualsHandler();

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();

    /**
     * 爆炸视觉效果处理
     */
    @SubscribeEvent
    public void onVisualExplosion(team.creative.enhancedvisuals.api.event.VisualExplosionEvent event) {
        if (event.isCanceled() || mc.player == null) return;

        ItemStack shield = findEquippedShield(mc.player);
        if (shield.isEmpty()) return;

        IntegratedShieldSystem.ShieldType type = getShieldType(shield);
        if (type == IntegratedShieldSystem.ShieldType.NONE) return;

        IntegratedShieldSystem.ShieldStatus status =
                IntegratedShieldSystem.getPlayerShieldStatus(mc.player.getUniqueID());

        // 根据护盾类型和状态决定免疫效果
        if (status != null) {
            switch (type) {
                case CRUDE:
                    // 粗劣护盾：激活时100%免疫，冷却时30%免疫
                    if (!status.isOnCooldown() || random.nextFloat() < 0.3F) {
                        event.setCanceled(true);
                    }
                    break;

                case BASIC:
                    // 基础护盾：激活时100%免疫，冷却时60%免疫
                    if (!status.isOnCooldown() || random.nextFloat() < 0.6F) {
                        event.setCanceled(true);
                    }
                    break;

                case ADVANCED:
                    // 高级护盾：始终免疫
                    event.setCanceled(true);
                    break;
            }
        } else if (getShieldEnergy(shield, type) > 0) {
            // 有能量就提供基础免疫
            event.setCanceled(true);
        }
    }

    /**
     * 血液飞溅效果处理
     */
    @SubscribeEvent
    public void onSplash(team.creative.enhancedvisuals.api.event.SplashEvent event) {
        if (event.isCanceled() || mc.player == null) return;

        ItemStack shield = findEquippedShield(mc.player);
        if (shield.isEmpty()) return;

        IntegratedShieldSystem.ShieldType type = getShieldType(shield);
        if (type == IntegratedShieldSystem.ShieldType.NONE) return;

        IntegratedShieldSystem.ShieldStatus status =
                IntegratedShieldSystem.getPlayerShieldStatus(mc.player.getUniqueID());

        // 护盾未在冷却时免疫血液效果
        if (status == null || !status.isOnCooldown()) {
            if (getShieldEnergy(shield, type) > 0) {
                event.setCanceled(true);
            }
        } else {
            // 冷却期间根据护盾等级提供部分免疫
            switch (type) {
                case CRUDE:
                    // 不免疫
                    break;
                case BASIC:
                    // 50%免疫
                    if (random.nextFloat() < 0.5F) {
                        event.setCanceled(true);
                    }
                    break;
                case ADVANCED:
                    // 80%免疫
                    if (random.nextFloat() < 0.8F) {
                        event.setCanceled(true);
                    }
                    break;
            }
        }
    }

    /**
     * 火焰粒子效果处理
     */
    @SubscribeEvent
    public void onFireParticles(team.creative.enhancedvisuals.api.event.FireParticlesEvent event) {
        if (event.isCanceled() || mc.player == null) return;

        ItemStack shield = findEquippedShield(mc.player);
        if (shield.isEmpty()) return;

        IntegratedShieldSystem.ShieldType type = getShieldType(shield);

        // 仅高级护盾免疫火焰视觉效果
        if (type == IntegratedShieldSystem.ShieldType.ADVANCED) {
            IntegratedShieldSystem.ShieldStatus status =
                    IntegratedShieldSystem.getPlayerShieldStatus(mc.player.getUniqueID());

            if (status == null || !status.isOnCooldown()) {
                if (getShieldEnergy(shield, type) > 0) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * 低血量效果处理（如果EnhancedVisuals有此事件）
     */
    @SubscribeEvent
    public void onLowHealth(team.creative.enhancedvisuals.api.event.SplashEvent event) {
        if (event.isCanceled() || mc.player == null) return;

        ItemStack shield = findEquippedShield(mc.player);
        if (shield.isEmpty()) return;

        IntegratedShieldSystem.ShieldType type = getShieldType(shield);
        if (type == IntegratedShieldSystem.ShieldType.NONE) return;

        // 护盾激活且有能量时，减轻低血量效果
        IntegratedShieldSystem.ShieldStatus status =
                IntegratedShieldSystem.getPlayerShieldStatus(mc.player.getUniqueID());

        if (status == null || !status.isOnCooldown()) {
            if (getShieldEnergy(shield, type) > getEnergyCost(type)) {
                // 高级护盾完全消除效果
                if (type == IntegratedShieldSystem.ShieldType.ADVANCED) {
                    event.setCanceled(true);
                }
                // 其他护盾减轻效果（如果API支持）
                else {
                    // 如果事件有intensity字段，降低强度
                    // event.intensity = event.intensity * 0.5F;
                }
            }
        }
    }

    // ===== 工具方法 =====

    /**
     * 查找玩家装备的护盾
     */
    private ItemStack findEquippedShield(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return ItemStack.EMPTY;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof ItemadvEnergyBarrier ||
                        stack.getItem() instanceof ItemBasicEnergyBarrier ||
                        stack.getItem() instanceof ItemCrudeEnergyBarrier) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 获取护盾类型
     */
    private IntegratedShieldSystem.ShieldType getShieldType(ItemStack stack) {
        if (stack.getItem() instanceof ItemadvEnergyBarrier) {
            return IntegratedShieldSystem.ShieldType.ADVANCED;
        }
        if (stack.getItem() instanceof ItemBasicEnergyBarrier) {
            return IntegratedShieldSystem.ShieldType.BASIC;
        }
        if (stack.getItem() instanceof ItemCrudeEnergyBarrier) {
            return IntegratedShieldSystem.ShieldType.CRUDE;
        }
        return IntegratedShieldSystem.ShieldType.NONE;
    }

    /**
     * 获取护盾能量
     */
    private int getShieldEnergy(ItemStack stack, IntegratedShieldSystem.ShieldType type) {
        switch (type) {
            case CRUDE:
                return ItemCrudeEnergyBarrier.getEnergyStored(stack);
            case BASIC:
                return ItemBasicEnergyBarrier.getEnergyStored(stack);
            case ADVANCED:
                return ItemadvEnergyBarrier.getEnergyStored(stack);
            default:
                return 0;
        }
    }

    /**
     * 获取能量消耗
     */
    private int getEnergyCost(IntegratedShieldSystem.ShieldType type) {
        switch (type) {
            case CRUDE:
                return 500;
            case BASIC:
                return 300;
            case ADVANCED:
                return 100;
            default:
                return 0;
        }
    }
}