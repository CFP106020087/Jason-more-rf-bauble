package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * 资源磁化戒指 - 掉落物增强
 * 每3模块：吸取范围+2格
 * 每6模块：自动熔炼矿物
 * 每10模块：10%双倍掉落
 */
@Mod.EventBusSubscriber
public class ItemResourceMagnetRing extends Item implements IBauble {

    // ===== 硬编码配置 =====
    private static final int REQUIRED_ACTIVE_MODULES = 5;           // 解锁需要5个激活模块
    private static final int MODULES_PER_RANGE_TIER = 3;            // 每3个模块增加范围
    private static final double RANGE_PER_TIER = 2.0;               // 每层增加2格
    private static final double BASE_RANGE = 5.0;                   // 基础5格范围
    private static final double MAX_RANGE = 25.0;                   // 最大25格范围
    private static final int MODULES_FOR_SMELTING = 6;              // 6模块解锁熔炼
    private static final int MODULES_FOR_DOUBLE_DROP = 10;          // 10模块解锁双倍
    private static final double DOUBLE_DROP_CHANCE = 0.1;           // 10%几率

    // ===== NBT Keys =====
    private static final String NBT_CACHED_ACTIVE = "CachedActive";
    private static final String NBT_CACHED_RANGE = "CachedRange";
    private static final String NBT_HAS_SMELTING = "HasSmelting";
    private static final String NBT_HAS_DOUBLE = "HasDouble";
    private static final String NBT_LAST_UPDATE = "LastUpdateTime";

    private static final Random RANDOM = new Random();

    public ItemResourceMagnetRing() {
        setRegistryName("resource_magnet_ring");
        setTranslationKey("resource_magnet_ring");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.RING;
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

        RingStats stats = calculateStats(info.activeModules);

        player.sendMessage(new TextComponentString(
                TextFormatting.BLUE + "⚡ 资源磁化戒指激活"));
        player.sendMessage(new TextComponentString(
                TextFormatting.WHITE + "  吸取范围: " + TextFormatting.AQUA + String.format("%.1f", stats.range) + "格"));
        if (stats.hasSmelting) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.WHITE + "  自动熔炼: " + TextFormatting.GREEN + "✓"));
        }
        if (stats.hasDoubleDrop) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.WHITE + "  双倍掉落: " + TextFormatting.GOLD +
                            String.format("%.0f%%", stats.doubleChance * 100)));
        }
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {
        if (wearer instanceof EntityPlayer && !wearer.world.isRemote) {
            ((EntityPlayer) wearer).sendStatusMessage(new TextComponentString(
                    TextFormatting.GRAY + "⚡ 资源磁化戒指已摘下"), true);
        }
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) wearer;
        if (player.world.isRemote) return;

        // 每 2 tick 吸取一次（提高吸取频率）
        if (player.world.getTotalWorldTime() % 2 != 0) return;

        // 吸取周围掉落物
        attractItems(player, stack);

        // 每秒检查一次核心状态（不需要每2tick都检查）
        if (player.world.getTotalWorldTime() % 20 == 0) {
            CoreInfo info = analyzeMechanicalCore(player);
            updateCache(stack, info, player.world);

            // 如果没有核心或模块不足，弹出
            if (!info.hasCore || info.activeModules < REQUIRED_ACTIVE_MODULES) {
                ejectItem(player, stack, info);
            }
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return getCachedActive(stack) > 0;
    }

    // ===== 掉落物吸取 =====
    private void attractItems(EntityPlayer player, ItemStack ring) {
        double range = getCachedRange(ring);
        if (range <= 0) return;

        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(range, range / 2, range);
        List<EntityItem> items = player.world.getEntitiesWithinAABB(EntityItem.class, aabb);

        for (EntityItem item : items) {
            if (item.isDead || item.cannotPickup()) continue;
            if (item.getItem().isEmpty()) continue;

            // 检查物品是否有主人且不是当前玩家
            if (item.getThrower() != null && !item.getThrower().isEmpty()) {
                if (!item.getThrower().equals(player.getName())) {
                    continue;
                }
            }

            // 计算吸取速度
            double dx = player.posX - item.posX;
            double dy = player.posY - item.posY + 0.5;
            double dz = player.posZ - item.posZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist < 1.5) {
                // 近距离直接拾取
                if (!player.world.isRemote) {
                    processItemPickup(player, item, ring);
                }
            } else {
                // 远距离吸引（加速吸取）
                double speed = 0.3 + (2.0 / dist);  // 提高基础速度和距离系数
                item.motionX = dx / dist * speed;
                item.motionY = dy / dist * speed + 0.05;  // 稍微向上提升避免卡地形
                item.motionZ = dz / dist * speed;
                item.velocityChanged = true;
            }
        }
    }

    // ===== 掉落物处理 =====
    private void processItemPickup(EntityPlayer player, EntityItem entityItem, ItemStack ring) {
        ItemStack itemStack = entityItem.getItem().copy();

        boolean hasSmelting = getHasSmelting(ring);
        boolean hasDouble = getHasDouble(ring);

        // 自动熔炼
        if (hasSmelting) {
            ItemStack smelted = FurnaceRecipes.instance().getSmeltingResult(itemStack);
            if (!smelted.isEmpty()) {
                itemStack = smelted.copy();
                itemStack.setCount(entityItem.getItem().getCount());
            }
        }

        // 双倍掉落
        if (hasDouble && RANDOM.nextDouble() < DOUBLE_DROP_CHANCE) {
            itemStack.grow(itemStack.getCount()); // 数量翻倍

            // 特效提示
            if (!player.world.isRemote) {
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        net.minecraft.init.SoundEvents.ENTITY_PLAYER_LEVELUP,
                        net.minecraft.util.SoundCategory.PLAYERS, 0.3f, 2.0f);
            }
        }

        // 添加到玩家背包
        if (player.inventory.addItemStackToInventory(itemStack)) {
            entityItem.setDead();

            // 播放拾取音效
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    net.minecraft.init.SoundEvents.ENTITY_ITEM_PICKUP,
                    net.minecraft.util.SoundCategory.PLAYERS, 0.2f,
                    ((RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.7f + 1.0f) * 2.0f);
        }
    }

    // ===== 统计信息 =====
    private static class RingStats {
        double range;
        boolean hasSmelting;
        boolean hasDoubleDrop;
        double doubleChance;
    }

    private RingStats calculateStats(int activeModules) {
        RingStats stats = new RingStats();

        // 计算吸取范围
        int rangeTiers = activeModules / MODULES_PER_RANGE_TIER;
        stats.range = Math.min(MAX_RANGE, BASE_RANGE + (rangeTiers * RANGE_PER_TIER));

        // 自动熔炼
        stats.hasSmelting = activeModules >= MODULES_FOR_SMELTING;

        // 双倍掉落
        stats.hasDoubleDrop = activeModules >= MODULES_FOR_DOUBLE_DROP;
        stats.doubleChance = stats.hasDoubleDrop ? DOUBLE_DROP_CHANCE : 0;

        return stats;
    }

    // ===== Tooltip =====
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tip, ITooltipFlag flag) {
        tip.add("");
        tip.add(TextFormatting.BLUE + "═══ 资源磁化戒指 ═══");
        tip.add(TextFormatting.GRAY + "饰品类型: " + TextFormatting.WHITE + "戒指");
        tip.add("");

        int active = getCachedActive(stack);

        if (active > 0) {
            RingStats stats = calculateStats(active);

            tip.add(TextFormatting.AQUA + "激活模块: " + TextFormatting.WHITE + active);
            tip.add(TextFormatting.AQUA + "吸取范围: " + TextFormatting.WHITE +
                    String.format("%.1f", stats.range) + " 格");

            if (stats.hasSmelting) {
                tip.add(TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + "自动熔炼矿物");
            } else {
                tip.add(TextFormatting.GRAY + "✗ 自动熔炼 (需要 " + MODULES_FOR_SMELTING + " 模块)");
            }

            if (stats.hasDoubleDrop) {
                tip.add(TextFormatting.GOLD + "✓ " + TextFormatting.WHITE + "双倍掉落 (" +
                        String.format("%.0f%%", stats.doubleChance * 100) + " 几率)");
            } else {
                tip.add(TextFormatting.GRAY + "✗ 双倍掉落 (需要 " + MODULES_FOR_DOUBLE_DROP + " 模块)");
            }
        } else {
            tip.add(TextFormatting.RED + "未检测到激活模块");
        }

        tip.add("");
        tip.add(TextFormatting.GRAY + "佩戴需求: " + TextFormatting.WHITE +
                "激活模块 ≥ " + REQUIRED_ACTIVE_MODULES);
        tip.add("");
        tip.add(TextFormatting.DARK_PURPLE + "按住 Shift 查看详情");

        if (GuiScreen.isShiftKeyDown()) {
            tip.add("");
            tip.add(TextFormatting.GOLD + "说明:");
            tip.add(TextFormatting.GRAY + "• 需要装备机械核心并激活至少 " +
                    REQUIRED_ACTIVE_MODULES + " 种模块");
            tip.add(TextFormatting.GRAY + "• 每 " + MODULES_PER_RANGE_TIER +
                    " 个模块增加 " + String.format("%.1f", RANGE_PER_TIER) + " 格范围");
            tip.add(TextFormatting.GRAY + "• " + MODULES_FOR_SMELTING +
                    " 模块解锁自动熔炼");
            tip.add(TextFormatting.GRAY + "• " + MODULES_FOR_DOUBLE_DROP +
                    " 模块解锁双倍掉落");
            tip.add(TextFormatting.GRAY + "• 最大范围: " + String.format("%.1f", MAX_RANGE) + " 格");
            tip.add("");
            tip.add(TextFormatting.DARK_GRAY + "吸引着你想要的一切...");
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

    // ===== 缓存更新 =====
    private void updateCache(ItemStack stack, CoreInfo info, World world) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        RingStats stats = calculateStats(info.activeModules);

        nbt.setInteger(NBT_CACHED_ACTIVE, info.activeModules);
        nbt.setDouble(NBT_CACHED_RANGE, stats.range);
        nbt.setBoolean(NBT_HAS_SMELTING, stats.hasSmelting);
        nbt.setBoolean(NBT_HAS_DOUBLE, stats.hasDoubleDrop);
        nbt.setLong(NBT_LAST_UPDATE, world.getTotalWorldTime());
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
                        "资源磁化戒指需要装备机械核心！" :
                        "资源磁化戒指需要至少 " + REQUIRED_ACTIVE_MODULES +
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
                            TextFormatting.BLUE + "⚡ 已装备资源磁化戒指"
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

    private static double getCachedRange(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0;
        return stack.getTagCompound().getDouble(NBT_CACHED_RANGE);
    }

    private static boolean getHasSmelting(ItemStack stack) {
        if (!stack.hasTagCompound()) return false;
        return stack.getTagCompound().getBoolean(NBT_HAS_SMELTING);
    }

    private static boolean getHasDouble(ItemStack stack) {
        if (!stack.hasTagCompound()) return false;
        return stack.getTagCompound().getBoolean(NBT_HAS_DOUBLE);
    }
}