package com.moremod.compat;

import com.moremod.compat.crafttweaker.ElementTypeRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * PotionCore 兼容处理（增强版）
 *
 * 功能：
 * 1. 阻止魔法专注药水被添加到手持元素武器的玩家
 * 2. 持续检测并移除已有的魔法专注效果
 *
 * 双重保护，确保元素伤害不会和魔法增伤叠加
 *
 * 注册方式：
 * MinecraftForge.EVENT_BUS.register(new PotionCoreCompatEnhanced());
 */
@Mod.EventBusSubscriber(modid = "moremod")

public class PotionCoreCompatEnhanced {

    private static final boolean DEBUG = true;
    private static final String POTION_CORE_MOD_ID = "potioncore";

    private static Potion magicFocusPotion = null;
    private static boolean initialized = false;

    /**
     * 初始化 - 获取魔法专注药水实例
     */
    private static void init() {
        if (initialized) return;
        initialized = true;

        // 检查PotionCore是否加载
        if (!Loader.isModLoaded(POTION_CORE_MOD_ID)) {
            if (DEBUG) {
                System.out.println("[PotionCore兼容] PotionCore未加载，兼容层禁用");
            }
            return;
        }

        try {
            // 尝试获取魔法专注药水
            magicFocusPotion = Potion.getPotionFromResourceLocation("potioncore:magic_focus");

            if (magicFocusPotion != null) {
                System.out.println("[PotionCore兼容] ✅ 成功加载，魔法专注 <-> 元素伤害 互斥已启用");
                System.out.println("[PotionCore兼容]    手持元素武器的玩家将无法获得魔法专注效果");
            } else {
                System.err.println("[PotionCore兼容] ⚠️ 无法找到魔法专注药水（potioncore:magic_focus）");
            }
        } catch (Exception e) {
            System.err.println("[PotionCore兼容] ❌ 初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 第一道防线：阻止药水添加
     *
     * 使用PotionEvent.PotionApplicableEvent（如果可用）
     * 注意：这个事件可能不是所有版本都有
     */
    @SubscribeEvent
    public void onPotionApplicable(net.minecraftforge.event.entity.living.PotionEvent.PotionApplicableEvent event) {
        // 延迟初始化
        if (!initialized) {
            init();
        }

        // 如果魔法专注药水不存在，跳过
        if (magicFocusPotion == null) {
            return;
        }

        // 只处理玩家
        if (!(event.getEntityLiving() instanceof EntityPlayer)) {
            return;
        }

        // 检查是否为魔法专注药水
        PotionEffect potionEffect = event.getPotionEffect();
        if (potionEffect == null || potionEffect.getPotion() != magicFocusPotion) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();

        // 检查是否手持元素武器
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!mainHand.isEmpty() && ElementTypeRegistry.hasElementalGems(mainHand)) {
            // 取消药水添加
            event.setResult(Event.Result.DENY);

            if (DEBUG) {
                System.out.println("╔════════════════════════════════════════╗");
                System.out.println("║   PotionCore兼容 - 阻止药水添加        ║");
                System.out.println("╠════════════════════════════════════════╣");
                System.out.println("║ 玩家: " + player.getName());
                System.out.println("║ 武器: " + mainHand.getDisplayName());
                System.out.println("║ 药水: 魔法专注");
                System.out.println("║ 操作: 已阻止添加");
                System.out.println("║ 原因: 手持元素武器");
                System.out.println("╚════════════════════════════════════════╝");
            }
        }
    }

    /**
     * 第二道防线：持续移除已有效果
     *
     * 玩家Tick事件 - 每秒检查一次
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务端处理
        if (event.side.isClient()) {
            return;
        }

        // 只在Phase.END处理
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // 每秒检查一次（20 ticks = 1秒）
        if (event.player.ticksExisted % 20 != 0) {
            return;
        }

        // 延迟初始化
        if (!initialized) {
            init();
        }

        // 如果魔法专注药水不存在，跳过
        if (magicFocusPotion == null) {
            return;
        }

        EntityPlayer player = event.player;

        // 检查玩家是否有魔法专注效果
        if (!player.isPotionActive(magicFocusPotion)) {
            return; // 没有该效果，跳过
        }

        // 检查玩家是否手持元素武器
        ItemStack mainHand = player.getHeldItemMainhand();
        if (mainHand.isEmpty() || !ElementTypeRegistry.hasElementalGems(mainHand)) {
            return; // 没有元素武器，允许保留药水效果
        }

        // 移除魔法专注效果
        player.removePotionEffect(magicFocusPotion);

        if (DEBUG) {
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║   PotionCore兼容 - 移除已有药水        ║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║ 玩家: " + player.getName());
            System.out.println("║ 武器: " + mainHand.getDisplayName());
            System.out.println("║ 药水: 魔法专注");
            System.out.println("║ 操作: 已移除效果");
            System.out.println("║ 原因: 手持元素武器");
            System.out.println("╚════════════════════════════════════════╝");
        }
    }

    /**
     * 工具方法：检查玩家是否应该被禁止魔法专注
     */
    public static boolean shouldBlockMagicFocus(EntityPlayer player) {
        if (magicFocusPotion == null) {
            return false;
        }

        ItemStack mainHand = player.getHeldItemMainhand();
        return !mainHand.isEmpty() && ElementTypeRegistry.hasElementalGems(mainHand);
    }

    /**
     * 工具方法：手动移除玩家的魔法专注效果
     */
    public static void removeMagicFocus(EntityPlayer player) {
        if (!initialized) {
            init();
        }

        if (magicFocusPotion != null && player.isPotionActive(magicFocusPotion)) {
            player.removePotionEffect(magicFocusPotion);
        }
    }
}