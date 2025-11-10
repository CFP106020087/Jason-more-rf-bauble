package com.moremod.event;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.item.EnchantmentBoostBauble;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 修复跳动问题的纯NBT方案
 * 关键：只在服务端修改，客户端只读取
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class EnchantmentBoostHandler {

    private static final String ORIGINAL_ENCHANTS = "moremod:original_enchants";
    private static final String BOOSTED_TAG = "moremod:boosted";
    private static final String BOOST_LEVEL_TAG = "moremod:boost_level";

    private static final Map<UUID, ItemStack> lastBoostedItems = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // ✅ 关键修复1：只在服务端运行
        if (event.side == Side.CLIENT) return;

        EntityPlayer player = event.player;

        // ✅ 关键修复2：降低频率到每20 tick
        if (player.ticksExisted % 20 != 0) return;

        ItemStack mainHand = player.getHeldItemMainhand();
        UUID playerId = player.getUniqueID();

        int boostLevel = getActiveBoostLevel(player);

        if (boostLevel > 0 && !mainHand.isEmpty() && mainHand.isItemEnchanted()) {
            // ✅ 关键修复3：检查是否是同一个物品实例
            ItemStack lastBoosted = lastBoostedItems.get(playerId);
            boolean isSameItem = (lastBoosted == mainHand);

            if (!isBoosted(mainHand)) {
                // 未增强，添加增强
                addBoostToItem(mainHand, boostLevel);
                lastBoostedItems.put(playerId, mainHand);
                System.out.println("[moremod] 添加增强: " + mainHand.getDisplayName() + " (+" + boostLevel + ")");

            } else if (!isSameItem) {
                // ✅ 关键修复4：切换到新物品，移除旧物品增强
                if (lastBoosted != null && !lastBoosted.isEmpty() && isBoosted(lastBoosted)) {
                    removeBoostFromItem(lastBoosted);
                    System.out.println("[moremod] 移除旧增强: " + lastBoosted.getDisplayName());
                }

                // 检查新物品的增幅值是否匹配
                int currentBoost = getItemBoostLevel(mainHand);
                if (currentBoost != boostLevel) {
                    // 增幅值不匹配，重新增强
                    removeBoostFromItem(mainHand);
                    addBoostToItem(mainHand, boostLevel);
                    System.out.println("[moremod] 更新增强: " + mainHand.getDisplayName() + " (+" + boostLevel + ")");
                }

                lastBoostedItems.put(playerId, mainHand);
            }
            // ✅ 如果是同一个物品且已增强，不做任何操作（避免重复修改）

        } else {
            // 饰品未激活或主手无附魔物品
            if (!mainHand.isEmpty() && isBoosted(mainHand)) {
                removeBoostFromItem(mainHand);
                System.out.println("[moremod] 移除增强: " + mainHand.getDisplayName());
            }

            ItemStack lastBoosted = lastBoostedItems.get(playerId);
            if (lastBoosted != null && !lastBoosted.isEmpty() && isBoosted(lastBoosted)) {
                removeBoostFromItem(lastBoosted);
                System.out.println("[moremod] 清理旧增强: " + lastBoosted.getDisplayName());
            }

            lastBoostedItems.remove(playerId);
        }

        // 清理inventory中的增强
        cleanInventoryBoosts(player, mainHand);
    }

    /* ==================== 安全防护 ==================== */

    @SubscribeEvent
    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        cleanAllPlayerBoosts(player);
        lastBoostedItems.remove(player.getUniqueID());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;
        cleanAllPlayerBoosts(player);
        lastBoostedItems.remove(player.getUniqueID());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            cleanAllPlayerBoosts(player);
            lastBoostedItems.remove(player.getUniqueID());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        EntityPlayer player = event.getEntityPlayer();
        cleanAllPlayerBoosts(player);
    }

    /* ==================== 核心方法 ==================== */

    private static int getActiveBoostLevel(EntityPlayer player) {
        try {
            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles == null) return 0;

            for (int i = 0; i < baubles.getSlots(); i++) {
                ItemStack bauble = baubles.getStackInSlot(i);
                if (!bauble.isEmpty() && bauble.getItem() instanceof EnchantmentBoostBauble) {
                    if (!bauble.hasTagCompound()) continue;

                    NBTTagCompound tag = bauble.getTagCompound();

                    if (!tag.getBoolean("moremod:boost_active")) continue;

                    long endTime = tag.getLong("moremod:boost_end");
                    if (System.currentTimeMillis() > endTime) continue;

                    return tag.getInteger("moremod:boost_level");
                }
            }
        } catch (Exception e) {
            System.err.println("[moremod] Error getting boost level: " + e.getMessage());
        }
        return 0;
    }

    /**
     * ✅ 关键修复5：添加防重复增强检查
     */
    private static void addBoostToItem(ItemStack stack, int boostLevel) {
        if (stack.isEmpty() || !stack.isItemEnchanted() || boostLevel <= 0) return;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return;

        if (!nbt.hasKey("ench")) return;

        // ✅ 如果已有增强标记，先移除（防止重复增强）
        if (nbt.hasKey(BOOSTED_TAG)) {
            System.out.println("[moremod] 警告：物品已有增强标记，先移除");
            removeBoostFromItem(stack);
        }

        // 备份原始附魔
        NBTTagList originalEnchants = nbt.getTagList("ench", 10);
        nbt.setTag(ORIGINAL_ENCHANTS, originalEnchants.copy());

        // 创建增强后的附魔
        NBTTagList boostedEnchants = new NBTTagList();
        for (int i = 0; i < originalEnchants.tagCount(); i++) {
            NBTTagCompound enchTag = originalEnchants.getCompoundTagAt(i).copy();
            short level = enchTag.getShort("lvl");
            int originalLevel = level & 0xFFFF;

            // ✅ 关键修复6：明确记录原始值，便于调试
            System.out.println("[moremod] 附魔 " + enchTag.getShort("id") +
                    ": 原始NBT=" + originalLevel +
                    " (显示=" + (originalLevel/2) + ")");

            // NBT中lvl = 显示值×2，所以要加 boostLevel×2
            int newLevel = originalLevel + (boostLevel );
            if (newLevel > 32767) newLevel = 32767;

            System.out.println("[moremod] 增强后: 新NBT=" + newLevel +
                    " (显示=" + (newLevel/2) + ")");

            enchTag.setShort("lvl", (short) newLevel);
            boostedEnchants.appendTag(enchTag);
        }

        nbt.setTag("ench", boostedEnchants);
        nbt.setBoolean(BOOSTED_TAG, true);
        nbt.setInteger(BOOST_LEVEL_TAG, boostLevel);
    }

    private static void removeBoostFromItem(ItemStack stack) {
        if (stack.isEmpty()) return;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || !nbt.hasKey(BOOSTED_TAG)) return;

        // 验证备份
        if (!nbt.hasKey(ORIGINAL_ENCHANTS)) {
            System.err.println("[moremod] 错误：备份缺失！");
            nbt.removeTag(BOOSTED_TAG);
            nbt.removeTag(BOOST_LEVEL_TAG);
            return;
        }

        // 恢复原始附魔
        NBTTagList originalEnchants = nbt.getTagList(ORIGINAL_ENCHANTS, 10);
        if (originalEnchants.tagCount() > 0) {
            nbt.setTag("ench", originalEnchants.copy());
        }

        nbt.removeTag(BOOSTED_TAG);
        nbt.removeTag(BOOST_LEVEL_TAG);
        nbt.removeTag(ORIGINAL_ENCHANTS);
    }

    private static boolean isBoosted(ItemStack stack) {
        if (stack.isEmpty()) return false;
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey(BOOSTED_TAG);
    }

    private static int getItemBoostLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || !nbt.hasKey(BOOST_LEVEL_TAG)) return 0;
        return nbt.getInteger(BOOST_LEVEL_TAG);
    }

    private static void cleanInventoryBoosts(EntityPlayer player, ItemStack currentMainHand) {
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (!stack.isEmpty() && stack != currentMainHand && isBoosted(stack)) {
                removeBoostFromItem(stack);
            }
        }

        for (ItemStack stack : player.inventory.offHandInventory) {
            if (!stack.isEmpty() && stack != currentMainHand && isBoosted(stack)) {
                removeBoostFromItem(stack);
            }
        }

        for (ItemStack stack : player.inventory.armorInventory) {
            if (!stack.isEmpty() && isBoosted(stack)) {
                removeBoostFromItem(stack);
            }
        }
    }

    private static void cleanAllPlayerBoosts(EntityPlayer player) {
        for (ItemStack stack : player.inventory.mainInventory) {
            if (!stack.isEmpty() && isBoosted(stack)) {
                removeBoostFromItem(stack);
            }
        }

        for (ItemStack stack : player.inventory.offHandInventory) {
            if (!stack.isEmpty() && isBoosted(stack)) {
                removeBoostFromItem(stack);
            }
        }

        for (ItemStack stack : player.inventory.armorInventory) {
            if (!stack.isEmpty() && isBoosted(stack)) {
                removeBoostFromItem(stack);
            }
        }
    }
}