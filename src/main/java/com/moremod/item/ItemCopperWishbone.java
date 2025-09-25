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
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 铜质许愿骨 - 幸运饰品
 * 每个等级≥3的激活模块提供 +1 幸运
 */
public class ItemCopperWishbone extends Item implements IBauble {

    // ===== 配置 =====
    public static final int REQUIRED_ACTIVE_MODULES = 4;  // 需要4个激活模块才能佩戴
    public static final int MIN_LEVEL_FOR_LUCK = 3;       // 等级≥3的模块才提供幸运
    public static final int MAX_LUCK_POINTS = 100;        // 幸运上限

    // ===== NBT Keys =====
    private static final String NBT_CACHED_ACTIVE = "CachedActive";
    private static final String NBT_CACHED_QUALIFIED = "CachedQualified";  // 等级≥3的模块数
    private static final String NBT_LAST_UPDATE = "LastUpdateTime";

    private static final UUID LUCK_UUID = UUID.fromString("ec8b3a86-2f5d-4a9d-8f4b-89dbab2a889b");

    public ItemCopperWishbone() {
        setRegistryName("copper_wishbone");
        setTranslationKey("copper_wishbone");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.CHARM;
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
        updateLuckWithInfo(player, info);

        // 更新缓存
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) { nbt = new NBTTagCompound(); stack.setTagCompound(nbt); }
        nbt.setInteger(NBT_CACHED_ACTIVE, info.activeModules);
        nbt.setInteger(NBT_CACHED_QUALIFIED, info.qualifiedModules);
        nbt.setLong(NBT_LAST_UPDATE, player.world.getTotalWorldTime());

        int luck = Math.min(MAX_LUCK_POINTS, info.qualifiedModules);
        player.sendMessage(new TextComponentString(
                TextFormatting.GOLD + "✦ 许愿骨激活 " +
                        TextFormatting.WHITE + "(激活模块: " + info.activeModules +
                        ", 幸运 " + TextFormatting.YELLOW + "+" + luck + ")"));
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {
        if (wearer instanceof EntityPlayer && !wearer.world.isRemote) {
            removeLuck((EntityPlayer) wearer);
            ((EntityPlayer) wearer).sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "✦ 许愿骨已摘下"), true);
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
        nbt.setInteger(NBT_CACHED_ACTIVE, info.activeModules);
        nbt.setInteger(NBT_CACHED_QUALIFIED, info.qualifiedModules);
        nbt.setLong(NBT_LAST_UPDATE, player.world.getTotalWorldTime());

        // 如果没有核心或模块不足，弹出
        if (!info.hasCore || info.activeModules < REQUIRED_ACTIVE_MODULES) {
            ejectItem(player, stack, info);
            return;
        }

        // 更新幸运值
        updateLuckWithInfo(player, info);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getCachedActive(stack) > 0;
    }

    // ===== Tooltip =====
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tip, ITooltipFlag flag) {
        tip.add("");
        tip.add(TextFormatting.GOLD + "═══ 铜质许愿骨 ═══");
        tip.add(TextFormatting.GRAY + "饰品类型: " + TextFormatting.WHITE + "护符");
        tip.add("");

        int active = getCachedActive(stack);
        int qualified = getCachedQualified(stack);

        if (active > 0) {
            int luck = Math.min(MAX_LUCK_POINTS, qualified);
            tip.add(TextFormatting.AQUA + "激活模块: " + TextFormatting.WHITE + active);
            tip.add(TextFormatting.AQUA + "合格模块 (Lv≥" + MIN_LEVEL_FOR_LUCK + "): " + TextFormatting.WHITE + qualified);
            tip.add(TextFormatting.YELLOW + "幸运加成: " + TextFormatting.WHITE + "+" + luck);
        } else {
            tip.add(TextFormatting.RED + "未检测到激活模块");
        }

        tip.add(TextFormatting.GRAY + "佩戴需求: " + TextFormatting.WHITE + "激活模块 ≥ " + REQUIRED_ACTIVE_MODULES);
        tip.add("");
        tip.add(TextFormatting.DARK_PURPLE + "按住 Shift 查看详情");

        if (GuiScreen.isShiftKeyDown()) {
            tip.add("");
            tip.add(TextFormatting.GOLD + "说明:");
            tip.add(TextFormatting.GRAY + "• 需要装备机械核心并激活至少 " + REQUIRED_ACTIVE_MODULES + " 个模块");
            tip.add(TextFormatting.GRAY + "• 每个等级≥" + MIN_LEVEL_FOR_LUCK + " 的激活模块提供 +1 幸运");
            tip.add(TextFormatting.GRAY + "• 幸运上限: " + MAX_LUCK_POINTS);
            tip.add("");
            tip.add(TextFormatting.DARK_GRAY + "许个愿吧~");
        }
    }

    // ===== 核心检测系统 =====
    private static class CoreInfo {
        boolean hasCore = false;
        int activeModules = 0;        // 所有激活的模块数
        int qualifiedModules = 0;     // 等级≥3的激活模块数
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
                    info.activeModules++;
                    if (level >= MIN_LEVEL_FOR_LUCK) {
                        info.qualifiedModules++;
                    }
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
                        info.activeModules++;
                        if (level >= MIN_LEVEL_FOR_LUCK) {
                            info.qualifiedModules++;
                        }
                    }
                }
            }
        }

        return info;
    }

    // 简单的核心判定
    private boolean isMechanicalCore(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // 检查类名
        String className = stack.getItem().getClass().getName();
        if (className.contains("ItemMechanicalCore")) {
            return true;
        }

        // 检查注册名
        ResourceLocation rl = stack.getItem().getRegistryName();
        if (rl != null) {
            String name = rl.toString().toLowerCase();
            return name.contains("mechanical") && name.contains("core");
        }

        return false;
    }

    // ===== 幸运值处理 =====
    private void updateLuckWithInfo(EntityPlayer player, CoreInfo info) {
        int luckPoints = Math.min(MAX_LUCK_POINTS, info.qualifiedModules);

        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.LUCK);
        if (attr == null) return;

        // 移除旧的
        AttributeModifier old = attr.getModifier(LUCK_UUID);
        if (old != null) {
            attr.removeModifier(old);
        }

        // 添加新的
        if (luckPoints > 0) {
            AttributeModifier mod = new AttributeModifier(
                    LUCK_UUID, "CopperWishboneLuck", luckPoints, 0
            ).setSaved(false);
            attr.applyModifier(mod);
        }
    }

    private void removeLuck(EntityPlayer player) {
        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.LUCK);
        if (attr != null) {
            AttributeModifier mod = attr.getModifier(LUCK_UUID);
            if (mod != null) {
                attr.removeModifier(mod);
            }
        }
    }

    // ===== 弹出处理 =====
    private void ejectItem(EntityPlayer player, ItemStack stack, CoreInfo info) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return;

        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack s = baubles.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() == this) {
                baubles.setStackInSlot(i, ItemStack.EMPTY);
                removeLuck(player);

                if (!player.inventory.addItemStackToInventory(s.copy())) {
                    player.dropItem(s.copy(), false);
                }

                String msg = !info.hasCore ?
                        "许愿骨需要装备机械核心！" :
                        "许愿骨需要至少 " + REQUIRED_ACTIVE_MODULES +
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
                            TextFormatting.GOLD + "✦ 已装备许愿骨"
                    ), true);
                    return new ActionResult<>(EnumActionResult.SUCCESS, held);
                }
            }
        }
        return new ActionResult<>(EnumActionResult.PASS, held);
    }

    // ===== Helper =====
    private int getCachedActive(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getInteger(NBT_CACHED_ACTIVE);
    }

    private int getCachedQualified(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getInteger(NBT_CACHED_QUALIFIED);
    }
}