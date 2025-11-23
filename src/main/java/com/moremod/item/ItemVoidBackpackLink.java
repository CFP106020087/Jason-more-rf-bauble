package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.inventory.ContainerVoidBackpack;
import com.moremod.inventory.InventoryVoidBackpack;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class ItemVoidBackpackLink extends Item implements IBauble {

    // ===== 硬编码配置 =====
    private static final int REQUIRED_ACTIVE_MODULES = 12;      // 解锁需要12个激活模块
    private static final int MODULES_PER_SIZE_TIER = 4;         // 每4个模块增加容量
    private static final int SIZE_PER_TIER = 3;                 // 每层增加3格
    private static final int BASE_SIZE = 9;                     // 基础9格（1行）
    private static final int MAX_SIZE = 27;                     // 最大27格（3行）
    private static final int MODULES_FOR_AUTO_COLLECT = 8;      // 8模块解锁自动收纳

    // ===== NBT Keys =====
    private static final String NBT_CACHED_ACTIVE = "CachedActive";
    private static final String NBT_CACHED_SIZE = "CachedSize";
    private static final String NBT_HAS_AUTO_COLLECT = "HasAutoCollect";
    private static final String NBT_AUTO_COLLECT_ITEMS = "AutoCollectItems";
    private static final String NBT_LAST_UPDATE = "LastUpdateTime";

    public ItemVoidBackpackLink() {
        setRegistryName("void_backpack_link");
        setTranslationKey("void_backpack_link");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.BELT;
    }

    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer)) return false;
        EntityPlayer player = (EntityPlayer) wearer;

        CoreInfo info = analyzeMechanicalCore(player);

        if (!info.hasCore) {
            if (!player.world.isRemote) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 需要佩戴机械核心"), true);
            }
            return false;
        }

        if (info.activeModules < REQUIRED_ACTIVE_MODULES) {
            if (!player.world.isRemote) {
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ 激活模块不足（" + info.activeModules + "/" + REQUIRED_ACTIVE_MODULES + "）"), true);
            }
            return false;
        }

        return true;
    }

    @Override
    public void onEquipped(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer) || wearer.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) wearer;

        CoreInfo info = analyzeMechanicalCore(player);
        updateCache(stack, info, player.world);

        int slots = calculateSlots(info.activeModules);
        boolean hasAuto = info.activeModules >= MODULES_FOR_AUTO_COLLECT;

        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "⚡ 虚空背包链接激活"));
        player.sendMessage(new TextComponentString(
                TextFormatting.WHITE + "  空间容量: " + TextFormatting.LIGHT_PURPLE + slots + " 格"));
        if (hasAuto) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.WHITE + "  自动收纳: " + TextFormatting.GREEN + "✓"));
        }
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {
        if (wearer instanceof EntityPlayer && !wearer.world.isRemote) {
            ((EntityPlayer) wearer).sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "⚡ 虚空背包链接已断开"), true);
        }
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) wearer;
        if (player.world.isRemote) return;

        if (player.world.getTotalWorldTime() % 20 != 0) return;

        CoreInfo info = analyzeMechanicalCore(player);
        updateCache(stack, info, player.world);

        if (!info.hasCore || info.activeModules < REQUIRED_ACTIVE_MODULES) {
            ejectItem(player, stack, info);
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getCachedActive(stack) > 0;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);

        if (world.isRemote) return new ActionResult<>(EnumActionResult.PASS, held);

        if (!isEquipped(player)) {
            CoreInfo info = analyzeMechanicalCore(player);
            if (!info.hasCore || info.activeModules < REQUIRED_ACTIVE_MODULES) {
                String msg = !info.hasCore ?
                        "需要佩戴机械核心" :
                        "激活模块不足（" + info.activeModules + "/" + REQUIRED_ACTIVE_MODULES + "）";
                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ " + msg), true);
                return new ActionResult<>(EnumActionResult.FAIL, held);
            }

            IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    if (baubles.getStackInSlot(i).isEmpty() &&
                            baubles.isItemValidForSlot(i, held, player)) {

                        baubles.setStackInSlot(i, held.splitStack(1));
                        player.sendStatusMessage(new TextComponentString(
                                TextFormatting.DARK_PURPLE + "⚡ 已装备虚空背包链接，按 K 键打开"
                        ), true);
                        return new ActionResult<>(EnumActionResult.SUCCESS, held);
                    }
                }
            }
            return new ActionResult<>(EnumActionResult.PASS, held);
        } else {
            // 已装备，提示按K键打开
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.YELLOW + "按 K 键打开虚空背包"), true);
        }

        return new ActionResult<>(EnumActionResult.PASS, held);
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;

        ItemStack backpack = getEquippedBackpack(player);
        if (backpack.isEmpty()) return;

        if (!getHasAutoCollect(backpack)) return;

        ItemStack pickedItem = event.getItem().getItem();
        if (shouldAutoCollect(backpack, pickedItem)) {
            if (transferToVoid(player, pickedItem, backpack)) {
                event.setCanceled(true);
                event.getItem().setDead();
            }
        }
    }

    private static boolean shouldAutoCollect(ItemStack backpack, ItemStack item) {
        NBTTagCompound nbt = backpack.getTagCompound();
        if (nbt == null || !nbt.hasKey(NBT_AUTO_COLLECT_ITEMS)) return false;

        String[] itemIds = nbt.getString(NBT_AUTO_COLLECT_ITEMS).split(",");
        String itemId = item.getItem().getRegistryName().toString();

        for (String id : itemIds) {
            if (id.equals(itemId)) return true;
        }
        return false;
    }

    public static void setAutoCollectItem(ItemStack backpack, String itemId, boolean add) {
        NBTTagCompound nbt = backpack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            backpack.setTagCompound(nbt);
        }

        String current = nbt.getString(NBT_AUTO_COLLECT_ITEMS);
        List<String> items = new ArrayList<>();
        if (!current.isEmpty()) {
            for (String s : current.split(",")) {
                items.add(s);
            }
        }

        if (add && !items.contains(itemId)) {
            items.add(itemId);
        } else if (!add) {
            items.remove(itemId);
        }

        nbt.setString(NBT_AUTO_COLLECT_ITEMS, String.join(",", items));
    }

    private static boolean transferToVoid(EntityPlayer player, ItemStack item, ItemStack backpack) {
        InventoryVoidBackpack inv = InventoryVoidBackpack.get(player.world);
        int size = getCachedSize(backpack);

        for (int i = 0; i < size; i++) {
            ItemStack slotItem = inv.getStackInSlot(i);
            if (slotItem.isEmpty()) {
                inv.setInventorySlotContents(i, item.copy());
                item.setCount(0);
                return true;
            } else if (slotItem.isItemEqual(item) && ItemStack.areItemStackTagsEqual(slotItem, item)) {
                int space = slotItem.getMaxStackSize() - slotItem.getCount();
                if (space > 0) {
                    int transfer = Math.min(space, item.getCount());
                    slotItem.grow(transfer);
                    item.shrink(transfer);
                    if (item.isEmpty()) return true;
                }
            }
        }
        return item.isEmpty();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tip, ITooltipFlag flag) {
        tip.add("");
        tip.add(TextFormatting.DARK_PURPLE + "═══ 虚空背包链接 ═══");
        tip.add(TextFormatting.GRAY + "饰品类型: " + TextFormatting.WHITE + "腰带");
        tip.add("");

        int active = getCachedActive(stack);

        if (active > 0) {
            int slots = calculateSlots(active);
            boolean hasAuto = active >= MODULES_FOR_AUTO_COLLECT;

            tip.add(TextFormatting.LIGHT_PURPLE + "激活模块: " + TextFormatting.WHITE + active);
            tip.add(TextFormatting.LIGHT_PURPLE + "空间容量: " + TextFormatting.WHITE + slots + " 格");

            if (hasAuto) {
                tip.add(TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + "自动收纳");
            } else {
                tip.add(TextFormatting.GRAY + "✗ 自动收纳 (需要 " + MODULES_FOR_AUTO_COLLECT + " 模块)");
            }

            if (active >= 16) {
                tip.add(TextFormatting.AQUA + "✓ " + TextFormatting.WHITE + "无限制存储");
            }
        } else {
            tip.add(TextFormatting.RED + "未检测到激活模块");
        }

        tip.add("");
        tip.add(TextFormatting.YELLOW + "按 K 键: " + TextFormatting.WHITE + "打开虚空背包");
        tip.add(TextFormatting.YELLOW + "右键: " + TextFormatting.WHITE + "快速装备");
        tip.add("");
        tip.add(TextFormatting.GRAY + "佩戴需求: " + TextFormatting.WHITE +
                "激活模块 ≥ " + REQUIRED_ACTIVE_MODULES);
        tip.add("");
        tip.add(TextFormatting.DARK_PURPLE + "按住 Shift 查看详情");

        if (GuiScreen.isShiftKeyDown()) {
            tip.add("");
            tip.add(TextFormatting.GOLD + "说明:");
            tip.add(TextFormatting.GRAY + "• 全局共享的虚拟仓库");
            tip.add(TextFormatting.GRAY + "• 每 " + MODULES_PER_SIZE_TIER +
                    " 个模块增加 " + SIZE_PER_TIER + " 格空间");
            tip.add(TextFormatting.GRAY + "• " + MODULES_FOR_AUTO_COLLECT +
                    " 模块解锁自动收纳");
            tip.add(TextFormatting.GRAY + "• 16 模块解锁无限制存储");
            tip.add(TextFormatting.GRAY + "• 最大容量: " + MAX_SIZE + " 格");
            tip.add("");
            tip.add(TextFormatting.DARK_GRAY + "连接到虚空的另一端...");
        }
    }

    // ===== 核心检测系统 =====
    private static class CoreInfo {
        boolean hasCore = false;
        int activeModules = 0;
    }

    private CoreInfo analyzeMechanicalCore(EntityPlayer player) {
        CoreInfo info = new CoreInfo();

        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return info;

        ItemStack core = ItemStack.EMPTY;
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty() && isMechanicalCore(stack)) {
                core = stack;
                info.hasCore = true;
                break;
            }
        }

        if (core.isEmpty()) return info;

        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return info;

        int tempActive = 0;

        if (nbt.hasKey("Upgrades", 10)) {
            NBTTagCompound upgrades = nbt.getCompoundTag("Upgrades");
            for (String key : upgrades.getKeySet()) {
                NBTTagCompound module = upgrades.getCompoundTag(key);
                int level = module.getInteger("level");
                boolean enabled = module.getBoolean("enabled") ||
                        module.getBoolean("active") ||
                        "ON".equals(module.getString("state"));

                if (enabled && level > 0) {
                    tempActive++;
                }
            }
        }

        for (String key : nbt.getKeySet()) {
            if (key.startsWith("upgrade_")) {
                int level = nbt.getInteger(key);
                if (level > 0) {
                    String upgradeName = key.substring(8);
                    boolean disabled = nbt.getBoolean("Disabled_" + upgradeName) ||
                            nbt.getBoolean("IsPaused_" + upgradeName);
                    if (!disabled) {
                        tempActive++;
                    }
                }
            }
        }

        info.activeModules = tempActive / 2;
        return info;
    }

    private boolean isMechanicalCore(ItemStack stack) {
        if (stack.isEmpty()) return false;

        String className = stack.getItem().getClass().getName();
        if (className.contains("ItemMechanicalCore")) {
            return true;
        }

        ResourceLocation rl = stack.getItem().getRegistryName();
        if (rl != null) {
            String name = rl.toString().toLowerCase();
            return name.contains("mechanical") && name.contains("core");
        }

        return false;
    }

    private int calculateSlots(int activeModules) {
        int tiers = activeModules / MODULES_PER_SIZE_TIER;
        int slots = BASE_SIZE + (tiers * SIZE_PER_TIER);
        return Math.min(slots, MAX_SIZE);
    }

    private void updateCache(ItemStack stack, CoreInfo info, World world) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        int slots = calculateSlots(info.activeModules);
        boolean hasAuto = info.activeModules >= MODULES_FOR_AUTO_COLLECT;

        nbt.setInteger(NBT_CACHED_ACTIVE, info.activeModules);
        nbt.setInteger(NBT_CACHED_SIZE, slots);
        nbt.setBoolean(NBT_HAS_AUTO_COLLECT, hasAuto);
        nbt.setLong(NBT_LAST_UPDATE, world.getTotalWorldTime());
    }

    private void ejectItem(EntityPlayer player, ItemStack stack, CoreInfo info) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack s = baubles.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() == this) {
                baubles.setStackInSlot(i, ItemStack.EMPTY);

                if (!player.inventory.addItemStackToInventory(s.copy())) {
                    player.dropItem(s.copy(), false);
                }

                String msg = !info.hasCore ?
                        "虚空背包链接需要装备机械核心！" :
                        "虚空背包链接需要至少 " + REQUIRED_ACTIVE_MODULES +
                                " 个激活模块（当前：" + info.activeModules + "）";

                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ " + msg), true);
                break;
            }
        }
    }

    // ===== 公开静态方法（供外部调用） =====

    /**
     * 检查玩家是否装备了虚空背包链接
     */
    public static boolean isEquipped(EntityPlayer player) {
        return !getEquippedBackpack(player).isEmpty();
    }

    /**
     * 获取装备的虚空背包
     */
    public static ItemStack getEquippedBackpack(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return ItemStack.EMPTY;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemVoidBackpackLink) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 获取缓存的激活模块数
     */
    public static int getCachedActive(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getInteger(NBT_CACHED_ACTIVE);
    }

    /**
     * 获取缓存的容量（供数据包使用）
     */
    public static int getCachedSizeStatic(ItemStack stack) {
        if (!stack.hasTagCompound()) return BASE_SIZE;
        return stack.getTagCompound().getInteger(NBT_CACHED_SIZE);
    }

    // ===== 私有静态方法（类内部使用） =====

    private static int getCachedSize(ItemStack stack) {
        if (!stack.hasTagCompound()) return BASE_SIZE;
        return stack.getTagCompound().getInteger(NBT_CACHED_SIZE);
    }

    private static boolean getHasAutoCollect(ItemStack stack) {
        if (!stack.hasTagCompound()) return false;
        return stack.getTagCompound().getBoolean(NBT_HAS_AUTO_COLLECT);
    }
}