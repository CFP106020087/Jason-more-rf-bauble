package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 时间撕裂手套 - 减少敌人无敌帧
 * 每5个激活模块减少2点无敌帧
 */
@Mod.EventBusSubscriber
public class ItemTemporalRiftGlove extends Item implements IBauble {

    // ===== 配置 =====
    public static int REQUIRED_ACTIVE_MODULES;
    public static int MODULES_PER_REDUCTION;
    public static int IFRAME_REDUCTION_PER_TIER;
    public static int MAX_IFRAME_REDUCTION;

    static {
        com.moremod.config.ItemConfig.ensureLoaded();
        REQUIRED_ACTIVE_MODULES = com.moremod.config.ItemConfig.TemporalRiftGlove.requiredActiveModules;
        MODULES_PER_REDUCTION = com.moremod.config.ItemConfig.TemporalRiftGlove.modulesPerReduction;
        IFRAME_REDUCTION_PER_TIER = com.moremod.config.ItemConfig.TemporalRiftGlove.iframeReductionPerTier;
        MAX_IFRAME_REDUCTION = com.moremod.config.ItemConfig.TemporalRiftGlove.maxIframeReduction;
    }
    // ===== NBT Keys =====
    private static final String NBT_CACHED_ACTIVE = "CachedActive";
    private static final String NBT_CACHED_REDUCTION = "CachedReduction";
    private static final String NBT_LAST_UPDATE = "LastUpdateTime";

    public ItemTemporalRiftGlove() {
        setRegistryName("temporal_rift_glove");
        setTranslationKey("temporal_rift_glove");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.RING;  // 手部槽位
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

        // 更新缓存
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) { nbt = new NBTTagCompound(); stack.setTagCompound(nbt); }

        int reduction = calculateIframeReduction(info.activeModules);
        nbt.setInteger(NBT_CACHED_ACTIVE, info.activeModules);
        nbt.setInteger(NBT_CACHED_REDUCTION, reduction);
        nbt.setLong(NBT_LAST_UPDATE, player.world.getTotalWorldTime());

        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_PURPLE + "⏱ 时间撕裂手套激活 " +
                        TextFormatting.WHITE + "(激活模块: " + info.activeModules +
                        ", 无敌帧削减 " + TextFormatting.LIGHT_PURPLE + "-" + reduction + ")"));
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {
        if (wearer instanceof EntityPlayer && !wearer.world.isRemote) {
            ((EntityPlayer) wearer).sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "⏱ 时间撕裂手套已摘下"), true);
        }
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) wearer;
        if (player.world.isRemote) return;

        // 每秒检查一次
        if (player.world.getTotalWorldTime() % 20 != 0) return;

        CoreInfo info = analyzeMechanicalCore(player);

        // 更新缓存
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) { nbt = new NBTTagCompound(); stack.setTagCompound(nbt); }

        int reduction = calculateIframeReduction(info.activeModules);
        nbt.setInteger(NBT_CACHED_ACTIVE, info.activeModules);
        nbt.setInteger(NBT_CACHED_REDUCTION, reduction);
        nbt.setLong(NBT_LAST_UPDATE, player.world.getTotalWorldTime());

        // 如果没有核心或模块不足，弹出
        if (!info.hasCore || info.activeModules < REQUIRED_ACTIVE_MODULES) {
            ejectItem(player, stack, info);
            return;
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getCachedActive(stack) > 0;
    }

    // ===== 战斗事件处理 =====
    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getSource().getTrueSource();

            // 检查玩家是否装备了时间撕裂手套
            if (hasTemporalRiftGlove(player)) {
                ItemStack glove = getEquippedGlove(player);
                if (!glove.isEmpty()) {
                    int reduction = getCachedReduction(glove);
                    if (reduction > 0) {
                        // 减少目标的无敌帧
                        EntityLivingBase target = event.getEntityLiving();
                        int currentIframes = target.hurtResistantTime;
                        int newIframes = Math.max(0, currentIframes - reduction);
                        target.hurtResistantTime = newIframes;
                        target.maxHurtResistantTime = Math.max(1, target.maxHurtResistantTime - reduction);
                    }
                }
            }
        }
    }

    private static boolean hasTemporalRiftGlove(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return false;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemTemporalRiftGlove) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack getEquippedGlove(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return ItemStack.EMPTY;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemTemporalRiftGlove) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // ===== Tooltip =====
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tip, ITooltipFlag flag) {
        tip.add("");
        tip.add(TextFormatting.DARK_PURPLE + "═══ 时间撕裂手套 ═══");
        tip.add(TextFormatting.GRAY + "饰品类型: " + TextFormatting.WHITE + "手部");
        tip.add("");

        int active = getCachedActive(stack);
        int reduction = getCachedReduction(stack);

        if (active > 0) {
            tip.add(TextFormatting.LIGHT_PURPLE + "激活模块: " + TextFormatting.WHITE + active);
            tip.add(TextFormatting.LIGHT_PURPLE + "无敌帧削减: " + TextFormatting.WHITE + "-" + reduction + " tick");
            tip.add(TextFormatting.GRAY + "削减效率: " + TextFormatting.WHITE +
                    IFRAME_REDUCTION_PER_TIER + " tick/" + MODULES_PER_REDUCTION + "模块");
        } else {
            tip.add(TextFormatting.RED + "未检测到激活模块");
        }

        tip.add(TextFormatting.GRAY + "佩戴需求: " + TextFormatting.WHITE + "激活模块 ≥ " + REQUIRED_ACTIVE_MODULES);
        tip.add("");
        tip.add(TextFormatting.DARK_PURPLE + "按住 Shift 查看详情");

        if (GuiScreen.isShiftKeyDown()) {
            tip.add("");
            tip.add(TextFormatting.GOLD + "说明:");
            tip.add(TextFormatting.GRAY + "• 需要装备机械核心并激活至少 " + REQUIRED_ACTIVE_MODULES + " 种模块");
            tip.add(TextFormatting.GRAY + "• 每 " + MODULES_PER_REDUCTION + " 个激活模块减少 " +
                    IFRAME_REDUCTION_PER_TIER + " 点无敌帧");
            tip.add(TextFormatting.GRAY + "• 最大削减: " + MAX_IFRAME_REDUCTION + " tick");
            tip.add(TextFormatting.GRAY + "• 撕裂时间的连续性，让敌人更易受伤");
            tip.add("");
            tip.add(TextFormatting.DARK_GRAY + "时间在你的掌控之中...");
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

        // 查找机械核心
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

        // 分析核心的模块
        NBTTagCompound nbt = core.getTagCompound();
        if (nbt == null) return info;

        int tempActive = 0;

        // 方法1：检查 Upgrades 结构
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

        // 方法2：检查简单的 upgrade_ 前缀
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

        // 除以2处理重复计算
        info.activeModules = tempActive / 2;

        return info;
    }

    // 简单的核心判定
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

    // ===== 无敌帧计算 =====
    private int calculateIframeReduction(int activeModules) {
        int tiers = activeModules / MODULES_PER_REDUCTION;
        int reduction = tiers * IFRAME_REDUCTION_PER_TIER;
        return Math.min(reduction, MAX_IFRAME_REDUCTION);
    }

    // ===== 弹出处理 =====
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
                        "时间撕裂手套需要装备机械核心！" :
                        "时间撕裂手套需要至少 " + REQUIRED_ACTIVE_MODULES +
                                " 个激活模块（当前：" + info.activeModules + "）";

                player.sendStatusMessage(new TextComponentString(
                        TextFormatting.RED + "⚠ " + msg
                ), true);
                break;
            }
        }
    }

    // ===== 右键快速装备 =====
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (world.isRemote) return new ActionResult<>(EnumActionResult.PASS, held);

        // 先检查条件
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
                            TextFormatting.DARK_PURPLE + "⏱ 已装备时间撕裂手套"
                    ), true);
                    return new ActionResult<>(EnumActionResult.SUCCESS, held);
                }
            }
        }
        return new ActionResult<>(EnumActionResult.PASS, held);
    }

    // ===== Helper =====
    private static int getCachedActive(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getInteger(NBT_CACHED_ACTIVE);
    }

    private static int getCachedReduction(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getInteger(NBT_CACHED_REDUCTION);
    }
}