package com.moremod.compat;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.accessorybox.SlotLayoutHelper; // 假设这是你管理槽位布局的类
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * PotionFingers 额外槽位兼容（增强版）
 * 支持 RING 和 TRINKET 类型的额外槽位
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class PotionFingersCompatHandler {

    private static final int EFFECT_DURATION = 199;
    private static final int REFRESH_RATE = 99;

    // 缓存
    private static boolean initialized = false;
    private static boolean potionFingersPresent = false;
    private static Item potionRingItem = null;
    private static Method getPotionMethod = null;

    // 初始化反射 (保持不变)
    private static void initialize() {
        if (initialized) return;
        initialized = true;

        if (!Loader.isModLoaded("potionfingers")) {
            System.out.println("[PotionFingersCompat] PotionFingers not found.");
            return;
        }

        try {
            Class<?> itemRingClass = Class.forName("vazkii.potionfingers.ItemRing");
            potionRingItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("potionfingers", "ring"));
            if (potionRingItem == null) return;

            getPotionMethod = itemRingClass.getDeclaredMethod("getPotion", ItemStack.class);
            getPotionMethod.setAccessible(true);

            potionFingersPresent = true;
            System.out.println("[PotionFingersCompat] Initialized successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;

        if (!initialized) initialize();
        if (!potionFingersPresent) return;

        EntityPlayer player = event.player;
        if (player.ticksExisted % REFRESH_RATE != 0) return;

        handleAllRingSlots(player);
    }

    private static void handleAllRingSlots(EntityPlayer player) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        if (handler == null) return;

        // 1. 获取所有需要扫描的槽位 ID
        // 包括 RING 和 TRINKET (因为 TRINKET 也可以放戒指)
        Set<Integer> slotsToScan = new HashSet<>();

        // 添加 RING 类型的槽位
        int[] ringSlots = SlotLayoutHelper.getValidSlotsForType(BaubleType.RING);
        if (ringSlots != null) {
            for (int slot : ringSlots) slotsToScan.add(slot);
        }

        // 添加 TRINKET 类型的槽位 (你的额外饰品栏位模组可能会增加这种类型)
        int[] trinketSlots = SlotLayoutHelper.getValidSlotsForType(BaubleType.TRINKET);
        if (trinketSlots != null) {
            for (int slot : trinketSlots) slotsToScan.add(slot);
        }

        // PotionFingers 原生只处理 Slot 1 和 Slot 2
        // 我们需要把这两个槽位从"额外处理列表"里排除吗？
        // 不，为了逻辑简单，我们统计*所有*槽位的戒指，然后统一计算等级。
        // 但要注意：PotionFingers 自己的代码也会跑。
        // 如果我们不仅补漏，还去叠加 Buff，会不会导致双倍效果？
        // PotionFingers 的逻辑是：addPotionEffect(..., level, ...)
        // Minecraft 的 addPotionEffect 会覆盖旧效果（如果新效果等级更高或时间更长）。
        // 所以我们只需要算出 **全局最高等级**，然后覆盖上去即可。

        Map<Potion, Integer> totalPotionCounts = new HashMap<>();

        int handlerSize = handler.getSlots();

        for (int slot : slotsToScan) {
            // 防止越界 (有些 Helper 可能返回不存在的槽位 ID)
            if (slot < 0 || slot >= handlerSize) continue;

            ItemStack stack = handler.getStackInSlot(slot);
            if (isPotionRing(stack)) {
                Potion potion = getPotionFromRing(stack);
                if (potion != null) {
                    totalPotionCounts.put(potion, totalPotionCounts.getOrDefault(potion, 0) + 1);
                }
            }
        }

        // 应用效果
        for (Map.Entry<Potion, Integer> entry : totalPotionCounts.entrySet()) {
            Potion potion = entry.getKey();
            int count = entry.getValue();

            // 如果只有 1 个戒指，且这个戒指在原版槽位 (1 或 2)，
            // 那么 PotionFingers 自己会处理，我们不需要干涉，节省性能。
            // 但如果 count >= 2，或者那个戒指在额外槽位，我们就必须介入。
            // 为了稳妥，我们总是检查并更新。

            // PotionFingers 逻辑：2个戒指 = 2级效果 (Amplifier 1)
            int amplifier = (count >= 2) ? 1 : 0;

            PotionEffect active = player.getActivePotionEffect(potion);

            boolean needUpdate = false;

            if (active == null) {
                needUpdate = true;
            } else {
                // 如果当前等级不够高
                if (active.getAmplifier() < amplifier) {
                    needUpdate = true;
                }
                // 如果持续时间快到了 (且我们的计算结果表明应该续杯)
                // 这里加个判断：如果 PotionFingers 原版逻辑已经续杯了，我们就不动
                else if (active.getDuration() <= 10) {
                    needUpdate = true;
                }
            }

            if (needUpdate) {
                // 强行覆盖/添加 Buff
                player.addPotionEffect(new PotionEffect(potion, EFFECT_DURATION, amplifier, true, false));

                if (debugMode()) {
                    System.out.println("[PFCompat] Apply " + potion.getName() + " Amp:" + amplifier + " (Count:" + count + ")");
                }
            }
        }
    }

    // 反射工具方法 (保持不变)
    private static boolean isPotionRing(ItemStack stack) {
        if (stack.isEmpty() || potionRingItem == null) return false;
        return stack.getItem() == potionRingItem && stack.getMetadata() == 1; // Meta 1 = Active
    }

    private static Potion getPotionFromRing(ItemStack stack) {
        if (!isPotionRing(stack) || getPotionMethod == null) return null;
        try {
            return (Potion) getPotionMethod.invoke(null, stack);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean debugMode() { return false; }
}