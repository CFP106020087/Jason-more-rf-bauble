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
import java.util.UUID;

/**
 * 外置内循环系统 - 生命值增强饰品
 * 根据激活模块数提供额外生命值
 */
public class ItemExternalCirculationSystem extends Item implements IBauble {

    // ===== 配置 =====
    public static int REQUIRED_ACTIVE_MODULES;
    public static float HEALTH_PER_MODULE;
    public static float MAX_BONUS_HEALTH;

    static {
        com.moremod.config.ItemConfig.ensureLoaded();
        REQUIRED_ACTIVE_MODULES = com.moremod.config.ItemConfig.ExternalCirculation.requiredActiveModules;
        HEALTH_PER_MODULE = com.moremod.config.ItemConfig.ExternalCirculation.healthPerModule;
        MAX_BONUS_HEALTH = com.moremod.config.ItemConfig.ExternalCirculation.maxBonusHealth;
    }
    // ===== NBT Keys =====
    private static final String NBT_CACHED_ACTIVE = "CachedActive";
    private static final String NBT_CACHED_HEALTH = "CachedHealth";
    private static final String NBT_LAST_UPDATE = "LastUpdateTime";

    private static final UUID HEALTH_UUID = UUID.fromString("d3f6e719-8a24-4b9a-b5c3-2f8d9e4a6c12");

    public ItemExternalCirculationSystem() {
        setRegistryName("external_circulation_system");
        setTranslationKey("external_circulation_system");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.TRINKET;  // 身体槽位
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
        updateHealthWithInfo(player, info);

        // 更新缓存
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) { nbt = new NBTTagCompound(); stack.setTagCompound(nbt); }
        nbt.setInteger(NBT_CACHED_ACTIVE, info.activeModules);
        float bonusHealth = Math.min(MAX_BONUS_HEALTH, info.activeModules * HEALTH_PER_MODULE);
        nbt.setFloat(NBT_CACHED_HEALTH, bonusHealth);
        nbt.setLong(NBT_LAST_UPDATE, player.world.getTotalWorldTime());

        player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "⚙ 内循环系统激活 " +
                        TextFormatting.WHITE + "(激活模块: " + info.activeModules +
                        ", 生命值 " + TextFormatting.RED + "+" + (int)(bonusHealth/2) + "❤)"));
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {
        if (wearer instanceof EntityPlayer && !wearer.world.isRemote) {
            removeHealth((EntityPlayer) wearer);
            ((EntityPlayer) wearer).sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "⚙ 内循环系统已停用"), true);
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
        float bonusHealth = Math.min(MAX_BONUS_HEALTH, info.activeModules * HEALTH_PER_MODULE);
        nbt.setFloat(NBT_CACHED_HEALTH, bonusHealth);
        nbt.setLong(NBT_LAST_UPDATE, player.world.getTotalWorldTime());

        // 如果没有核心或模块不足，弹出
        if (!info.hasCore || info.activeModules < REQUIRED_ACTIVE_MODULES) {
            ejectItem(player, stack, info);
            return;
        }

        // 更新生命值
        updateHealthWithInfo(player, info);
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
        tip.add(TextFormatting.AQUA + "═══ 外置内循环系统 ═══");
        tip.add(TextFormatting.GRAY + "饰品类型: " + TextFormatting.WHITE + "身体");
        tip.add("");

        int active = getCachedActive(stack);
        float health = getCachedHealth(stack);

        if (active > 0) {
            tip.add(TextFormatting.DARK_AQUA + "激活模块: " + TextFormatting.WHITE + active);
            tip.add(TextFormatting.RED + "生命值加成: " + TextFormatting.WHITE + "+" + (int)(health/2) + "❤ (+" + (int)health + " HP)");
            tip.add(TextFormatting.GRAY + "转换率: " + TextFormatting.WHITE +
                    String.format("%.1f HP/模块", HEALTH_PER_MODULE));
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
            tip.add(TextFormatting.GRAY + "• 每个激活模块提供 " + HEALTH_PER_MODULE + " 点生命值");
            tip.add(TextFormatting.GRAY + "• 生命值上限: +" + (int)MAX_BONUS_HEALTH + " HP (" + (int)(MAX_BONUS_HEALTH/2) + "❤)");
            tip.add(TextFormatting.GRAY + "• 强化你的生命系统，提升生存能力");
            tip.add("");
            tip.add(TextFormatting.DARK_GRAY + "维持生命循环中...");
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

    // ===== 生命值处理 =====
    private void updateHealthWithInfo(EntityPlayer player, CoreInfo info) {
        float bonusHealth = Math.min(MAX_BONUS_HEALTH, info.activeModules * HEALTH_PER_MODULE);

        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (attr == null) return;

        // 移除旧的
        AttributeModifier old = attr.getModifier(HEALTH_UUID);
        if (old != null) {
            attr.removeModifier(old);
        }

        // 添加新的
        if (bonusHealth > 0) {
            AttributeModifier mod = new AttributeModifier(
                    HEALTH_UUID, "ExternalCirculationHealth", bonusHealth, 0
            ).setSaved(false);
            attr.applyModifier(mod);
        }
    }

    private void removeHealth(EntityPlayer player) {
        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (attr != null) {
            AttributeModifier mod = attr.getModifier(HEALTH_UUID);
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
                removeHealth(player);

                if (!player.inventory.addItemStackToInventory(s.copy())) {
                    player.dropItem(s.copy(), false);
                }

                String msg = !info.hasCore ?
                        "内循环系统需要装备机械核心！" :
                        "内循环系统需要至少 " + REQUIRED_ACTIVE_MODULES +
                                " 种激活模块（当前：" + info.activeModules + "）";

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
                            TextFormatting.AQUA + "⚙ 已装备内循环系统"
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

    private float getCachedHealth(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getFloat(NBT_CACHED_HEALTH);
    }
}