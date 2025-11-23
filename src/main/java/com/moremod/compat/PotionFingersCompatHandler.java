package com.moremod.compat;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.accessorybox.SlotLayoutHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * PotionFingers 额外槽位兼容（反射版）
 * 不修改原模组代码，通过事件扩展药水戒指效果到额外戒指槽位
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class PotionFingersCompatHandler {
    
    private static final int EFFECT_DURATION = 199;  // 与 PotionFingers 相同
    private static final int REFRESH_RATE = 99;      // 与 PotionFingers 相同
    
    // 缓存反射对象
    private static boolean initialized = false;
    private static boolean potionFingersPresent = false;
    private static Item potionRingItem = null;
    private static Method getPotionMethod = null;
    private static Class<?> itemRingClass = null;
    
    /**
     * 初始化反射
     */
    private static void initialize() {
        if (initialized) return;
        initialized = true;
        
        // 检查 PotionFingers 是否存在
        if (!Loader.isModLoaded("potionfingers")) {
            System.out.println("[PotionFingersCompat] PotionFingers not found, compatibility disabled");
            return;
        }
        
        try {
            // 获取 ItemRing 类
            itemRingClass = Class.forName("vazkii.potionfingers.ItemRing");
            
            // 获取戒指物品
            potionRingItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("potionfingers", "ring"));
            if (potionRingItem == null) {
                System.err.println("[PotionFingersCompat] Could not find PotionFingers ring item!");
                return;
            }
            
            // 获取 getPotion 静态方法
            // public static Potion getPotion(ItemStack stack)
            getPotionMethod = itemRingClass.getDeclaredMethod("getPotion", ItemStack.class);
            getPotionMethod.setAccessible(true);
            
            potionFingersPresent = true;
            System.out.println("[PotionFingersCompat] Successfully initialized PotionFingers compatibility");
            
        } catch (Exception e) {
            System.err.println("[PotionFingersCompat] Failed to initialize:");
            e.printStackTrace();
        }
    }
    
    /**
     * 玩家 Tick 事件 - 检查额外戒指槽位的药水戒指
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.world.isRemote) return;
        
        // 延迟初始化
        if (!initialized) {
            initialize();
        }
        
        if (!potionFingersPresent) return;
        
        EntityPlayer player = event.player;
        
        // 每 99 tick 检查一次（与 PotionFingers 同步）
        if (player.ticksExisted % REFRESH_RATE != 0) return;
        
        // 处理额外戒指槽位
        handleExtraRingSlots(player);
    }
    
    /**
     * 处理额外戒指槽位的药水戒指
     */
    private static void handleExtraRingSlots(EntityPlayer player) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return;
        
        int handlerSlots = handler.getSlots();
        
        // 获取所有戒指槽位
        int[] allRingSlots = SlotLayoutHelper.getValidSlotsForType(BaubleType.RING);
        
        // 统计每种药水的戒指数量（仅额外槽位）
        Map<Potion, Integer> extraPotionCounts = new HashMap<>();
        
        // 先统计原版槽位的药水（用于判断是否需要提升等级）
        Map<Potion, Integer> vanillaPotionCounts = new HashMap<>();
        for (int slot = 1; slot <= 2; slot++) {
            if (slot < handlerSlots) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (isPotionRing(stack)) {
                    Potion potion = getPotionFromRing(stack);
                    if (potion != null) {
                        vanillaPotionCounts.put(potion, vanillaPotionCounts.getOrDefault(potion, 0) + 1);
                    }
                }
            }
        }
        
        // 检查额外戒指槽位
        for (int slot : allRingSlots) {
            // 跳过原版槽位 1 和 2（PotionFingers 自己处理）
            if (slot == 1 || slot == 2) continue;
            
            // 检查额外槽位是否在 handler 范围内
            if (slot >= handlerSlots) continue;
            
            ItemStack stack = handler.getStackInSlot(slot);
            
            // 检查是否是药水戒指
            if (isPotionRing(stack)) {
                Potion potion = getPotionFromRing(stack);
                if (potion != null) {
                    extraPotionCounts.put(potion, extraPotionCounts.getOrDefault(potion, 0) + 1);
                    
                    // 调试输出
                    if (debugMode()) {
                        System.out.println("[PotionFingersCompat] Found " + potion.getName() + 
                            " ring in extra slot " + slot);
                    }
                }
            }
        }
        
        // 应用额外槽位的药水效果
        for (Map.Entry<Potion, Integer> entry : extraPotionCounts.entrySet()) {
            Potion potion = entry.getKey();
            int extraCount = entry.getValue();
            int vanillaCount = vanillaPotionCounts.getOrDefault(potion, 0);
            
            // 总戒指数量
            int totalCount = vanillaCount + extraCount;
            
            // 计算等级（最多等级 1，与 PotionFingers 相同）
            int level = Math.min(totalCount - 1, 1);
            
            // PotionFingers 可能已经应用了原版槽位的效果
            // 我们需要更新或补充效果
            PotionEffect currentEffect = player.getActivePotionEffect(potion);
            
            // 如果当前没有效果，或者等级需要提升
            if (currentEffect == null || currentEffect.getAmplifier() < level) {
                player.addPotionEffect(new PotionEffect(potion, EFFECT_DURATION, level, true, false));
                
                if (debugMode()) {
                    System.out.println("[PotionFingersCompat] Applied/Updated " + potion.getName() + 
                        " level " + (level + 1) + " (total rings: " + totalCount + ")");
                }
            }
            // 如果效果快过期了，刷新持续时间
            else if (currentEffect.getDuration() <= 10) {
                player.addPotionEffect(new PotionEffect(potion, EFFECT_DURATION, level, true, false));
            }
        }
    }
    
    /**
     * 检查物品是否是药水戒指
     */
    private static boolean isPotionRing(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (potionRingItem == null) return false;
        
        // 检查是否是戒指物品
        if (stack.getItem() != potionRingItem) return false;
        
        // 检查是否有药水效果（metadata = 1 表示启用）
        return stack.getMetadata() == 1;
    }
    
    /**
     * 从戒指获取药水效果（通过反射）
     */
    private static Potion getPotionFromRing(ItemStack stack) {
        if (!isPotionRing(stack)) return null;
        if (getPotionMethod == null) return null;
        
        try {
            // 调用 ItemRing.getPotion(stack)
            return (Potion) getPotionMethod.invoke(null, stack);
        } catch (Exception e) {
            if (debugMode()) {
                System.err.println("[PotionFingersCompat] Failed to get potion from ring:");
                e.printStackTrace();
            }
            return null;
        }
    }
    
    /**
     * 调试模式
     */
    private static boolean debugMode() {
        return false; // 设为 true 开启调试输出
    }
}