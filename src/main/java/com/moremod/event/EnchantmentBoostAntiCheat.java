package com.moremod.event;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * moremod —— 附魔增幅反作弊（信任为主：无 clamp / 无降级）
 *
 * 规则：
 *  - 仅在检测到 BOOSTED_TAG 时处理。
 *  - 有 ORIGIN → 回滚；无 ORIGIN → 保持现有附魔，仅移除标记。
 *  - 2×2：清输入后同帧强制重算并同步结果；3×3 工作台也在清理后强制重算。
 *  - 不触碰其他模组的超限附魔。
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class EnchantmentBoostAntiCheat {

    /* ---------------- 配置 ---------------- */

    private static final String BOOSTED_TAG       = "moremod:boosted";
    private static final String ORIGINAL_ENCHANTS = "moremod:original_enchants";
    private static final String BOOST_LEVEL_TAG   = "moremod:boost_level";

    private static final int SCAN_INTERVAL_TICKS = 5; // 背包/容器普扫频率
    private static final boolean DEBUG = false;

    // NBT 类型常量
    private static final int NBT_LIST     = 9;
    private static final int NBT_COMPOUND = 10;

    /* ---------------- 工具 ---------------- */

    private static boolean isServer(EntityPlayer p) { return p != null && !p.world.isRemote; }
    private static boolean isServer(EntityItem e)   { return e != null && !e.world.isRemote; }

    /** 只识别“自家增幅”标记 */
    private static boolean isBoosted(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey(BOOSTED_TAG);
    }

    /** 宽松校验 ORIGIN：不过度校验等级，避免误伤其他模组的超限 */
    private static boolean isValidOriginalList(NBTTagList list) {
        if (list == null) return false;
        if (list.tagCount() > 64) return false; // 简单上限防恶意 NBT
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound ench = list.getCompoundTagAt(i);
            if (!ench.hasKey("id", 99) || !ench.hasKey("lvl", 99)) return false;
            int id  = ench.getInteger("id");
            int lvl = ench.getInteger("lvl");
            if (lvl < 1) return false;
            if (Enchantment.getEnchantmentByID(id) == null) return false;
        }
        return true;
    }

    /**
     * 回滚到 ORIGIN；若缺失/非法则保持现有附魔（信任式处理）。
     * 仅对“带增幅标记”的物品调用。
     */
    private static void restoreEnchantmentsOrKeep(ItemStack stack, NBTTagCompound nbt) {
        if (nbt.hasKey(ORIGINAL_ENCHANTS, NBT_LIST)) {
            NBTTagList origin = nbt.getTagList(ORIGINAL_ENCHANTS, NBT_COMPOUND);
            if (isValidOriginalList(origin)) {
                if (stack.getItem() == Items.ENCHANTED_BOOK) {
                    nbt.setTag("StoredEnchantments", origin.copy());
                    nbt.removeTag("ench");
                } else {
                    nbt.setTag("ench", origin.copy());
                    nbt.removeTag("StoredEnchantments");
                }
                return; // 成功回滚
            }
        }
        // 无 ORIGIN 或无效 → 保持现有附魔不动
    }

    /** 统一清理：有 ORIGIN 就回滚；无 ORIGIN 只移除标记（不降级/不清空） */
    private static void cleanBoost(ItemStack stack) {
        if (stack.isEmpty()) return;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || !nbt.hasKey(BOOSTED_TAG)) return;

        restoreEnchantmentsOrKeep(stack, nbt);

        nbt.removeTag(BOOSTED_TAG);
        nbt.removeTag(BOOST_LEVEL_TAG);
        nbt.removeTag(ORIGINAL_ENCHANTS);
        stack.setTagCompound(nbt);

        if (DEBUG) System.out.println("[AntiCheat] Cleaned boost: " + stack.getDisplayName());
    }

    /* ---------------- 2×2 自带合成栏：清输入 + 同帧重算结果并同步 ---------------- */

    private static void cleanInventory2x2CraftingGrid(EntityPlayer p) {
        if (p == null) return;
        Container c = p.inventoryContainer;

        if (c instanceof ContainerPlayer) {
            ContainerPlayer cp = (ContainerPlayer) c;
            boolean cleaned = false;

            for (int s = 1; s <= 4; s++) { // 1..4 = 2×2 输入
                Slot slot = safeGetSlot(cp, s);
                if (slot == null) continue;
                ItemStack in = slot.getStack();
                if (!in.isEmpty() && isBoosted(in)) {
                    cleanBoost(in);
                    cleaned = true;
                }
            }

            // 只要清过输入，就强制重算并同步
            if (cleaned) {
                cp.onCraftMatrixChanged(cp.craftMatrix);
                cp.detectAndSendChanges();
            }

            // 结果槽若意外带标记也清掉
            Slot out = safeGetSlot(cp, 0);
            if (out != null) {
                ItemStack r = out.getStack();
                if (!r.isEmpty() && isBoosted(r)) cleanBoost(r);
            }
            return;
        }

        // 不是 ContainerPlayer（被其它模组替换时的保底清理）
        for (int s = 1; s <= 4; s++) {
            Slot slot = safeGetSlot(c, s);
            if (slot == null) continue;
            ItemStack in = slot.getStack();
            if (!in.isEmpty() && isBoosted(in)) cleanBoost(in);
        }
        Slot out = safeGetSlot(c, 0);
        if (out != null) {
            ItemStack r = out.getStack();
            if (!r.isEmpty() && isBoosted(r)) cleanBoost(r);
        }
    }

    private static Slot safeGetSlot(Container c, int index) {
        return (c != null && index >= 0 && index < c.inventorySlots.size()) ? c.getSlot(index) : null;
    }

    /* ---------------- 扫描/清理辅助 ---------------- */

    private static void cleanAllItems(EntityPlayer player) {
        for (ItemStack s : player.inventory.mainInventory) if (isBoosted(s)) cleanBoost(s);
        for (ItemStack s : player.inventory.offHandInventory) if (isBoosted(s)) cleanBoost(s);
        for (ItemStack s : player.inventory.armorInventory) if (isBoosted(s)) cleanBoost(s);

        if (Loader.isModLoaded("baubles")) {
            try {
                IBaublesItemHandler h = BaublesApi.getBaublesHandler(player);
                for (int i = 0; i < h.getSlots(); i++) {
                    ItemStack s = h.getStackInSlot(i);
                    if (!s.isEmpty() && isBoosted(s)) cleanBoost(s);
                }
            } catch (Throwable ignored) {}
        }
    }

    /**
     * 清容器中的增幅物；若是工作台并且清理过输入，强制重算并同步结果。
     */
    private static void cleanContainerBoosts(Container container, EntityPlayer player) {
        if (container == null || player == null) return;
        int mainIdx = player.inventory.currentItem;
        boolean cleaned = false;

        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = container.getSlot(i);
            if (slot == null) continue;

            // 跳过主手槽（当 slot 指向玩家背包且索引等于当前热键）
            if (slot.inventory == player.inventory && slot.getSlotIndex() == mainIdx) continue;

            ItemStack s = slot.getStack();
            if (!s.isEmpty() && isBoosted(s)) {
                cleanBoost(s);
                cleaned = true;
            }
        }

        // ★ 如果是工作台容器且确实清理过输入 → 强制重算并同步
        if (cleaned && container instanceof ContainerWorkbench) {
            ContainerWorkbench cw = (ContainerWorkbench) container;
            cw.onCraftMatrixChanged(cw.craftMatrix);
            cw.detectAndSendChanges();
        }
    }

    /* ---------------- 事件 ---------------- */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemToss(ItemTossEvent event) {
        if (!isServer(event.getEntityItem())) return;
        ItemStack stack = event.getEntityItem().getItem();
        if (isBoosted(stack)) cleanBoost(stack);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!isServer(event.getEntityPlayer())) return;
        ItemStack stack = event.getItem().getItem();
        if (isBoosted(stack)) cleanBoost(stack);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!isServer(player)) return;
        cleanAllItems(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDrops(PlayerDropsEvent event) {
        if (!isServer(event.getEntityPlayer())) return;
        for (EntityItem ei : event.getDrops()) {
            ItemStack s = ei.getItem();
            if (isBoosted(s)) cleanBoost(s);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        EntityPlayer player = event.player;
        if (!isServer(player)) return;
        cleanAllItems(player);
    }

    /**
     * 合成事件：只回滚/清除“带增幅标记”的输入，和（极少数）传播到产物上的标记。
     * 不再尝试替换 e.crafting（该字段是 final）。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemCrafted(ItemCraftedEvent e) {
        EntityPlayer p = e.player;
        if (!isServer(p)) return;

        IInventory inv = e.craftMatrix;
        if (inv != null) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack in = inv.getStackInSlot(i);
                if (!in.isEmpty() && isBoosted(in)) cleanBoost(in);
            }
        }
        // 结果若意外带上了标记（某些特殊配方复制 NBT）也清掉
        if (isBoosted(e.crafting)) cleanBoost(e.crafting);
    }

    /**
     * 铁砧预览：发现增幅输入就地回滚（不取消，允许以回滚后的输入重算预览）。
     * 若输出带增幅标记，也仅清标记，不改等级。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAnvilUpdate(AnvilUpdateEvent e) {
        ItemStack left  = e.getLeft();
        ItemStack right = e.getRight();

        if (isBoosted(left))  cleanBoost(left);
        if (isBoosted(right)) cleanBoost(right);

        ItemStack out = e.getOutput();
        if (!out.isEmpty() && isBoosted(out)) {
            cleanBoost(out);
        }
    }

    /**
     * 铁砧取出：只要结果还带你的增幅标记，就回滚（若有 ORIGIN）并清标记；其余不动。
     * 兼容 getItemResult / getOutput 两种映射。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAnvilRepair(AnvilRepairEvent e) {
        EntityPlayer p = e.getEntityPlayer();
        if (!isServer(p)) return;

        ItemStack out = getAnvilOutputCompat(e);
        if (out != null && !out.isEmpty() && isBoosted(out)) {
            cleanBoost(out);
        }
    }

    /** 打开任意容器即刻清一次（跳过主手槽；工作台会在清理后强制重算） */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!isServer(player)) return;
        cleanContainerBoosts(event.getContainer(), player);
    }

    /**
     * 每 tick：专扫 2×2 自带合成格（清输入后强制重算）；
     * 每 SCAN_INTERVAL_TICKS：普扫背包/副手/护甲/Baubles/已打开容器（跳过主手槽，工作台清理后强制重算）。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        EntityPlayer p = event.player;

        // 2×2 合成格：清输入 → 立刻重算并同步
        cleanInventory2x2CraftingGrid(p);

        // 周期普扫
        if (p.ticksExisted % SCAN_INTERVAL_TICKS != 0) return;

        int mainIdx = p.inventory.currentItem;

        for (int i = 0; i < p.inventory.mainInventory.size(); i++) {
            if (i == mainIdx) continue; // 跳过主手
            ItemStack s = p.inventory.mainInventory.get(i);
            if (!s.isEmpty() && isBoosted(s)) cleanBoost(s);
        }
        for (ItemStack s : p.inventory.offHandInventory) if (isBoosted(s)) cleanBoost(s);
        for (ItemStack s : p.inventory.armorInventory)  if (isBoosted(s)) cleanBoost(s);

        if (Loader.isModLoaded("baubles")) {
            try {
                IBaublesItemHandler h = BaublesApi.getBaublesHandler(p);
                for (int i = 0; i < h.getSlots(); i++) {
                    ItemStack s = h.getStackInSlot(i);
                    if (!s.isEmpty() && isBoosted(s)) cleanBoost(s);
                }
            } catch (Throwable ignored) {}
        }

        if (p.openContainer != null && p.openContainer != p.inventoryContainer) {
            cleanContainerBoosts(p.openContainer, p);
        }
    }

    /* ---------------- 兼容：不同映射的 Anvil 输出 ---------------- */

    private static ItemStack getAnvilOutputCompat(AnvilRepairEvent e) {
        try {
            return e.getItemResult(); // 常见映射（1.12.2）
        } catch (Throwable t) {
            try {
                return (ItemStack) AnvilRepairEvent.class.getMethod("getOutput").invoke(e);
            } catch (Throwable ignore) {
                return ItemStack.EMPTY;
            }
        }
    }
}
